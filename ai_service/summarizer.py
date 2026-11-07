"""
Document Text Extraction and Academic Summarization Service.
Optimized for local Qwen3-0.6B LLM with context window safeguards,
chunking / extractive pre-filtering, and deterministic offline fallback.
"""

import os
import re
import hashlib
from typing import Dict, Any, List, Optional
from ollama_client import query_ollama_text

try:
    import fitz  # PyMuPDF
    PYMUPDF_AVAILABLE = True
except ImportError:
    PYMUPDF_AVAILABLE = False

try:
    import pdfplumber
    PDFPLUMBER_AVAILABLE = True
except ImportError:
    PDFPLUMBER_AVAILABLE = False

try:
    import docx
    DOCX_AVAILABLE = True
except ImportError:
    DOCX_AVAILABLE = False


MAX_EXTRACT_CHARS = 100_000
MIN_TEXT_FOR_SUMMARY = 50
MAX_CHUNK_WORDS_FOR_06B = 550  # Keep within Qwen3-0.6B's optimal 2048-token context budget


def compute_sha256(file_path: str) -> str:
    hasher = hashlib.sha256()
    with open(file_path, "rb") as f:
        while chunk := f.read(65536):
            hasher.update(chunk)
    return hasher.hexdigest()


def extract_text_from_file(file_path: str) -> Dict[str, Any]:
    """
    Extracts plain text from physical PDF, DOCX, or TXT documents.
    Defensively handles empty, scanned, or corrupted files.
    """
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"Physical file not found on server: {os.path.basename(file_path)}")

    ext = os.path.splitext(file_path)[1].lower()
    extracted_text = ""

    if ext == ".pdf":
        # 1. Try PyMuPDF (fitz)
        if PYMUPDF_AVAILABLE:
            try:
                doc = fitz.open(file_path)
                pages_text = []
                for page_num in range(len(doc)):
                    page = doc[page_num]
                    t = page.get_text("text")
                    if t:
                        pages_text.append(t)
                    if sum(len(p) for p in pages_text) > MAX_EXTRACT_CHARS:
                        break
                doc.close()
                extracted_text = "\n".join(pages_text).strip()
            except Exception as ex:
                raise ValueError(f"Corrupted or invalid PDF file: {str(ex)}")

        # 2. Fallback to pdfplumber if fitz unavailable or returned empty
        if not extracted_text and PDFPLUMBER_AVAILABLE:
            try:
                with pdfplumber.open(file_path) as pdf:
                    pages_text = [p.extract_text() or "" for p in pdf.pages]
                    extracted_text = "\n".join(pages_text).strip()
            except Exception as ex:
                raise ValueError(f"PDF extraction failed: {str(ex)}")

        if not extracted_text:
            raise ValueError("The PDF appears to be scanned or contains only images; no extractable text found.")

    elif ext == ".docx":
        if not DOCX_AVAILABLE:
            raise RuntimeError("python-docx is not available for DOCX text extraction.")
        try:
            doc = docx.Document(file_path)
            paragraphs = [p.text for p in doc.paragraphs if p.text.strip()]
            extracted_text = "\n".join(paragraphs).strip()
        except Exception as ex:
            raise ValueError(f"Corrupted or invalid DOCX document: {str(ex)}")

        if not extracted_text:
            raise ValueError("The DOCX document is empty.")

    elif ext == ".txt":
        try:
            with open(file_path, "r", encoding="utf-8", errors="replace") as f:
                extracted_text = f.read(MAX_EXTRACT_CHARS).strip()
        except Exception as ex:
            raise ValueError(f"Failed to read text file: {str(ex)}")
    else:
        raise ValueError(f"Unsupported file format for summarization: '{ext}'. Allowed: .pdf, .docx, .txt")

    if len(extracted_text) > MAX_EXTRACT_CHARS:
        extracted_text = extracted_text[:MAX_EXTRACT_CHARS]

    file_hash = compute_sha256(file_path)
    word_count = len(re.findall(r"\b\w+\b", extracted_text))

    return {
        "text": extracted_text,
        "file_hash": file_hash,
        "word_count": word_count
    }


def chunk_and_prefilter_text(text: str, max_words: int = MAX_CHUNK_WORDS_FOR_06B) -> str:
    """
    Extractive Pre-Filtering / Chunking Strategy for 0.6B Context Limits.
    For documents exceeding the small model's context capacity, extracts
    lead sentences, key section headings, and conclusion sentences so the
    0.6B LLM receives high-signal, non-redundant context bounded strictly by max_words.
    """
    raw_sentences = [s.strip() for s in re.split(r"(?<=[.!?])\s+", text) if len(s.strip()) > 15]
    if not raw_sentences:
        return text[:max_words * 6]

    total_words = sum(len(s.split()) for s in raw_sentences)
    if total_words <= max_words:
        return text

    selected: List[str] = []
    current_words = 0

    # 1. Lead introductory sentences
    intro_budget = max(20, int(max_words * 0.4))
    idx = 0
    while idx < len(raw_sentences) and current_words < intro_budget:
        s = raw_sentences[idx]
        w_count = len(s.split())
        if current_words + w_count <= intro_budget:
            selected.append(s)
            current_words += w_count
        else:
            break
        idx += 1

    # 2. Key middle sentences evenly sampled
    conclusion_budget = max(20, int(max_words * 0.3))
    mid_end = max(idx, len(raw_sentences) - 3)
    if mid_end > idx:
        step = max(1, (mid_end - idx) // 5)
        for m_idx in range(idx, mid_end, step):
            s = raw_sentences[m_idx]
            w_count = len(s.split())
            if current_words + w_count <= (max_words - conclusion_budget):
                selected.append(s)
                current_words += w_count

    # 3. Concluding sentences
    for c_idx in range(max(mid_end, len(raw_sentences) - 3), len(raw_sentences)):
        s = raw_sentences[c_idx]
        w_count = len(s.split())
        if current_words + w_count <= max_words:
            selected.append(s)
            current_words += w_count

    return " ".join(selected)


def generate_extractive_summary(text: str, target_sentences: int = 4) -> str:
    """
    Deterministic extractive summarizer using sentence position, length,
    and term-frequency scoring. 100% offline, reproducible, and zero-latency.
    """
    raw_sentences = re.split(r"(?<=[.!?])\s+", text)
    clean_sentences = [s.strip() for s in raw_sentences if len(s.strip()) > 20]

    if not clean_sentences:
        return text[:300] + "..." if len(text) > 300 else text

    if len(clean_sentences) <= target_sentences:
        return " ".join(clean_sentences)

    # Term frequencies
    words = re.findall(r"\b[a-zA-Z]{3,}\b", text.lower())
    freq: Dict[str, int] = {}
    for w in words:
        freq[w] = freq.get(w, 0) + 1

    max_freq = max(freq.values()) if freq else 1

    # Sentence scoring
    scored_sentences = []
    for idx, sentence in enumerate(clean_sentences):
        sentence_words = re.findall(r"\b[a-zA-Z]{3,}\b", sentence.lower())
        if not sentence_words:
            continue

        raw_score = sum(freq.get(w, 0) / max_freq for w in sentence_words)
        normalized_score = raw_score / len(sentence_words)

        # Boost lead sentence and early paragraphs
        if idx == 0:
            normalized_score *= 1.5
        elif idx < 3:
            normalized_score *= 1.25

        scored_sentences.append((normalized_score, idx, sentence))

    # Pick top N sentences maintaining chronological ordering
    scored_sentences.sort(key=lambda item: item[0], reverse=True)
    top_n = scored_sentences[:target_sentences]
    top_n.sort(key=lambda item: item[1])

    summary = " ".join(item[2] for item in top_n)
    return summary


def summarize_document(file_path: str,
                       max_sentences: int = 4,
                       provider: str = "qwen3") -> Dict[str, Any]:
    """
    Summarizes an approved physical academic document.
    Coordinates text extraction, 0.6B context chunking, generative synthesis via Ollama,
    and graceful fallback to deterministic extractive summarization.
    """
    extracted = extract_text_from_file(file_path)
    text = extracted["text"]
    original_words = extracted["word_count"]
    file_hash = extracted["file_hash"]

    if original_words < MIN_TEXT_FOR_SUMMARY:
        return {
            "summary": text,
            "word_count": original_words,
            "original_word_count": original_words,
            "reduction_ratio": 1.0,
            "file_hash": file_hash,
            "method": "verbatim_short"
        }

    summary_text = ""
    method = "local_extractive"

    # Check if local Qwen3-0.6B LLM generation is requested
    use_qwen = provider in ["qwen3", "ollama", "auto"] and os.environ.get("AI_DISABLE_LLM", "false").lower() != "true"

    if use_qwen:
        # Pre-filter / chunk text to avoid overflowing 0.6B context window
        condensed_text = chunk_and_prefilter_text(text, max_words=MAX_CHUNK_WORDS_FOR_06B)
        system_prompt = (
            "You are an academic summarizer. Summarize the following course document in 3-4 concise, "
            "factual sentences highlighting key definitions, concepts, and takeaways. "
            "Do not hallucinate, speculate, or add information not present in the document."
        )
        prompt = f"Document Content:\n{condensed_text}\n\nConcise Academic Summary:"

        llm_summary = query_ollama_text(
            prompt=prompt,
            system_prompt=system_prompt,
            max_tokens=220,
            temperature=0.1,
            timeout=8.0
        )

        if llm_summary and len(llm_summary.strip()) >= 25:
            summary_text = llm_summary.strip()
            method = "qwen3_ollama_abstractive"

    # Deterministic fallback if generative LLM was not requested, failed, timed out, or produced empty output
    if not summary_text:
        summary_text = generate_extractive_summary(text, target_sentences=max_sentences)
        method = "extractive_offline_fallback" if use_qwen else "local_extractive"

    summary_words = len(re.findall(r"\b\w+\b", summary_text))
    reduction_ratio = round(summary_words / max(original_words, 1), 3)

    return {
        "summary": summary_text,
        "word_count": summary_words,
        "original_word_count": original_words,
        "reduction_ratio": reduction_ratio,
        "file_hash": file_hash,
        "method": method
    }
