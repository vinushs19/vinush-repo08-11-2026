"""
Semantic Vector Search Engine.
Engineered with architectural separation from the 0.6B generative model.
Supports dedicated embedding models (e.g. all-MiniLM-L6-v2, Ollama nomic-embed-text)
with an ultra-lightweight, zero-memory subword TF-IDF vector cosine similarity baseline.
"""

import os
import math
import re
import logging
from typing import List, Dict, Any, Optional
from ollama_client import query_ollama_embedding

logger = logging.getLogger("ai_service.semantic")

# Optional Tier 1: HuggingFace SentenceTransformers (all-MiniLM-L6-v2)
SENTENCE_TRANSFORMER_MODEL = None
ST_LOAD_ATTEMPTED = False


def _get_sentence_transformer():
    global SENTENCE_TRANSFORMER_MODEL, ST_LOAD_ATTEMPTED
    if not ST_LOAD_ATTEMPTED:
        ST_LOAD_ATTEMPTED = True
        try:
            from sentence_transformers import SentenceTransformer
            model_name = os.environ.get("EMBEDDING_MODEL_NAME", "all-MiniLM-L6-v2")
            SENTENCE_TRANSFORMER_MODEL = SentenceTransformer(model_name)
            logger.info("Loaded dedicated SentenceTransformer model: %s", model_name)
        except Exception as ex:
            logger.debug("SentenceTransformer not available (%s), using lightweight vector baseline", str(ex))
            SENTENCE_TRANSFORMER_MODEL = None
    return SENTENCE_TRANSFORMER_MODEL


def get_embedding_provider_name() -> str:
    """Returns the name of the active embedding engine."""
    if _get_sentence_transformer() is not None:
        return "sentence_transformers_all_minilm_l6_v2"
    if os.environ.get("USE_OLLAMA_EMBEDDINGS", "false").lower() == "true":
        return "ollama_nomic_embed_text"
    return "subword_tfidf_cosine_engine"


# --- Built-in Lightweight Vector Space Model (Tier 3 Baseline) ---

def tokenize(text: str) -> List[str]:
    if not text:
        return []
    words = re.findall(r"\b[a-zA-Z0-9]{2,}\b", text.lower())
    # Sub-word character 3-grams for morphological flexibility and synonym closeness
    trigrams = []
    for w in words:
        if len(w) >= 3:
            trigrams.extend([w[i:i+3] for i in range(len(w) - 2)])
    return words + trigrams


def build_tf_vector(tokens: List[str]) -> Dict[str, float]:
    vec: Dict[str, float] = {}
    for t in tokens:
        vec[t] = vec.get(t, 0.0) + 1.0
    max_count = max(vec.values()) if vec else 1.0
    for k in vec:
        vec[k] /= max_count
    return vec


def cosine_similarity(vec1: Dict[str, float], vec2: Dict[str, float]) -> float:
    common_keys = set(vec1.keys()) & set(vec2.keys())
    if not common_keys:
        return 0.0

    dot_product = sum(vec1[k] * vec2[k] for k in common_keys)
    norm1 = math.sqrt(sum(v * v for v in vec1.values()))
    norm2 = math.sqrt(sum(v * v for v in vec2.values()))

    if norm1 == 0.0 or norm2 == 0.0:
        return 0.0

    return dot_product / (norm1 * norm2)


def dense_cosine_similarity(vec1: List[float], vec2: List[float]) -> float:
    if not vec1 or not vec2 or len(vec1) != len(vec2):
        return 0.0
    dot = sum(a * b for a, b in zip(vec1, vec2))
    norm1 = math.sqrt(sum(a * a for a in vec1))
    norm2 = math.sqrt(sum(b * b for b in vec2))
    if norm1 == 0.0 or norm2 == 0.0:
        return 0.0
    return dot / (norm1 * norm2)


def rank_candidates_semantically(query: str, candidates: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """
    Computes semantic similarity between the user search query and authorized resource metadata.
    Does not use the 0.6B generative model directly.
    """
    if not query or not query.strip() or not candidates:
        return candidates

    st_model = _get_sentence_transformer()

    # Tier 1: Dense embeddings with SentenceTransformers
    if st_model is not None:
        try:
            texts = [
                f"{c.get('title', '')} {c.get('description', '')} {c.get('summary', '')} {c.get('subject_name', '')} {c.get('category_name', '')}"
                for c in candidates
            ]
            query_emb = st_model.encode(query, convert_to_numpy=True).tolist()
            doc_embs = st_model.encode(texts, convert_to_numpy=True).tolist()

            results = []
            for candidate, doc_emb in zip(candidates, doc_embs):
                sim = dense_cosine_similarity(query_emb, doc_emb)
                item = dict(candidate)
                item["semantic_similarity"] = round(min(max(sim, 0.0), 1.0), 4)
                results.append(item)

            results.sort(key=lambda x: x["semantic_similarity"], reverse=True)
            return results
        except Exception as ex:
            logger.warning("Dense embedding search failed (%s), falling back to vector baseline", str(ex))

    # Tier 2: Ollama Dedicated Embedding Model (if enabled)
    if os.environ.get("USE_OLLAMA_EMBEDDINGS", "false").lower() == "true":
        query_emb = query_ollama_embedding(query)
        if query_emb:
            results = []
            for candidate in candidates:
                corpus = f"{candidate.get('title', '')} {candidate.get('description', '')} {candidate.get('summary', '')}"
                doc_emb = query_ollama_embedding(corpus)
                sim = dense_cosine_similarity(query_emb, doc_emb) if doc_emb else 0.0
                item = dict(candidate)
                item["semantic_similarity"] = round(min(max(sim, 0.0), 1.0), 4)
                results.append(item)

            results.sort(key=lambda x: x["semantic_similarity"], reverse=True)
            return results

    # Tier 3: Zero-overhead, offline subword TF-IDF Vector Cosine Engine
    query_tokens = tokenize(query)
    query_vec = build_tf_vector(query_tokens)

    results = []
    for candidate in candidates:
        text_corpus = " ".join([
            str(candidate.get("title", "")),
            str(candidate.get("description", "")),
            str(candidate.get("summary", "")),
            str(candidate.get("subject_name", "")),
            str(candidate.get("category_name", ""))
        ])

        cand_tokens = tokenize(text_corpus)
        cand_vec = build_tf_vector(cand_tokens)

        sim_score = cosine_similarity(query_vec, cand_vec)
        score = round(min(max(sim_score, 0.0), 1.0), 4)

        item = dict(candidate)
        item["semantic_similarity"] = score
        results.append(item)

    results.sort(key=lambda x: x["semantic_similarity"], reverse=True)
    return results
