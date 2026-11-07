"""
Natural Language Parser for Controlled File Organization Requests.
Converts user phrases into structured, allowlisted operation parameters.
Enforces strict filesystem path traversal defenses and schema allowlists.
"""

import os
import re
from typing import Dict, Any, Optional
from ollama_client import query_ollama_json

ALLOWED_CATEGORIES = {
    "lecture notes": "LECTURE_NOTES",
    "notes": "LECTURE_NOTES",
    "lecture": "LECTURE_NOTES",
    "question papers": "QUESTION_PAPERS",
    "question paper": "QUESTION_PAPERS",
    "exam papers": "QUESTION_PAPERS",
    "questions": "QUESTION_PAPERS",
    "assignments": "ASSIGNMENTS",
    "assignment": "ASSIGNMENTS",
    "homework": "ASSIGNMENTS",
    "lab manuals": "LAB_MANUALS",
    "lab manual": "LAB_MANUALS",
    "manual": "LAB_MANUALS",
    "study materials": "STUDY_MATERIALS",
    "study material": "STUDY_MATERIALS",
    "materials": "STUDY_MATERIALS",
    "syllabus": "SYLLABUS",
    "curriculum": "SYLLABUS",
    "archives": "ARCHIVES",
    "archive": "ARCHIVES"
}

ALLOWED_CATEGORY_CODES = {
    "LECTURE_NOTES",
    "QUESTION_PAPERS",
    "ASSIGNMENTS",
    "LAB_MANUALS",
    "STUDY_MATERIALS",
    "SYLLABUS",
    "ARCHIVES"
}

SAFE_TAG_REGEX = re.compile(r"^[a-zA-Z0-9_-]{1,50}$")


def sanitize_subfolder_tag(tag: Optional[str]) -> Optional[str]:
    """Validates that subfolder tag adheres strictly to regex ^[a-zA-Z0-9_-]{1,50}$."""
    if not tag:
        return None
    clean = tag.strip()
    # Reject any directory traversal sequence
    if ".." in clean or "/" in clean or "\\" in clean:
        return None
    if SAFE_TAG_REGEX.match(clean):
        return clean
    return None


def parse_deterministic_file_save(prompt: str) -> Dict[str, Any]:
    """
    Deterministic rule-based parser for file saving requests.
    Zero-latency offline baseline.
    """
    if not prompt or not prompt.strip():
        return {
            "valid": False,
            "error": "Empty prompt provided.",
            "resource_id": None,
            "destination_category": None,
            "subfolder_tag": None
        }

    raw = prompt.strip()
    working = raw.lower()

    # 1. Resource ID extraction
    res_id = None
    res_match = re.search(r"\b(?:resource|res|id|item)[\s#:]*(\d+)\b", working)
    if res_match:
        res_id = int(res_match.group(1))

    # 2. Destination Category extraction
    target_category = None
    for phrase, cat_code in sorted(ALLOWED_CATEGORIES.items(), key=lambda x: -len(x[0])):
        pattern = r"\b" + re.escape(phrase) + r"\b"
        if re.search(pattern, working):
            target_category = cat_code
            break

    # 3. Subfolder tag extraction from raw prompt
    subfolder = None
    folder_match = re.search(r"\b(?:subfolder|folder|tag|directory|dir)[\s:=]+([a-zA-Z0-9_-]{1,50})\b", raw, re.IGNORECASE)
    if folder_match:
        subfolder = sanitize_subfolder_tag(folder_match.group(1))

    if not res_id:
        return {
            "valid": False,
            "error": "Could not identify target Resource ID from prompt (e.g., 'resource #15').",
            "resource_id": None,
            "destination_category": target_category,
            "subfolder_tag": subfolder
        }

    if not target_category:
        return {
            "valid": False,
            "error": "Could not identify an approved destination category (e.g., 'lecture notes', 'question papers', 'assignments').",
            "resource_id": res_id,
            "destination_category": None,
            "subfolder_tag": subfolder
        }

    return {
        "valid": True,
        "resource_id": res_id,
        "destination_category": target_category,
        "subfolder_tag": subfolder,
        "explanation": f"Organize resource #{res_id} into '{target_category}'" + (f" with subfolder '{subfolder}'" if subfolder else "")
    }


def parse_file_save_intent(prompt: str) -> Dict[str, Any]:
    """
    Hybrid file save intent parser.
    Uses deterministic parser as high-confidence baseline.
    If baseline fails and LLM is enabled, uses few-shot structured JSON parsing.
    """
    baseline = parse_deterministic_file_save(prompt)
    if baseline.get("valid") or os.environ.get("AI_DISABLE_LLM", "false").lower() == "true":
        return baseline

    # If deterministic parsing failed (e.g. creative student/faculty phrasing), try Qwen3-0.6B
    system_prompt = (
        "Extract file-saving parameters from the user prompt. Return JSON with keys: "
        "'resource_id' (integer), 'destination_category' (one of: 'LECTURE_NOTES', 'QUESTION_PAPERS', "
        "'ASSIGNMENTS', 'LAB_MANUALS', 'STUDY_MATERIALS', 'SYLLABUS', 'ARCHIVES' or null), "
        "and 'subfolder_tag' (string matching [a-zA-Z0-9_-] or null)."
    )

    llm_output = query_ollama_json(
        prompt=prompt.strip(),
        system_prompt=system_prompt,
        timeout=5.0
    )

    if llm_output and isinstance(llm_output, dict):
        res_id = llm_output.get("resource_id")
        dest_cat = llm_output.get("destination_category")
        tag = sanitize_subfolder_tag(llm_output.get("subfolder_tag"))

        if isinstance(res_id, int) and res_id > 0 and dest_cat in ALLOWED_CATEGORY_CODES:
            return {
                "valid": True,
                "resource_id": res_id,
                "destination_category": dest_cat,
                "subfolder_tag": tag,
                "explanation": f"Organize resource #{res_id} into '{dest_cat}'" + (f" with subfolder '{tag}'" if tag else "")
            }

    return baseline
