"""
Natural Language Query Parser for Academic Resource Discovery.
Implements a Hybrid Parsing Strategy optimized for local Qwen3-0.6B LLM with
strict deterministic regex fallback, few-shot prompt guidance, and a schema validation layer.
"""

import os
import re
from typing import Optional, Dict, Any, List
from ollama_client import query_ollama_json

STOPWORDS = {
    "find", "show", "get", "search", "give", "list", "display", "please",
    "for", "of", "the", "in", "a", "an", "on", "to", "me", "my", "with",
    "all", "about", "and", "or", "from", "can", "you", "need", "i", "want"
}

ALLOWED_CATEGORIES = [
    "Lecture Notes",
    "Previous Examination Papers",
    "Assignments",
    "Study Materials",
    "E-books and Reference Documents",
    "Other Approved Academic Resources"
]

VALID_DEPARTMENTS = {"MCA", "CS", "IT", "BCA", "BSC_CS", "MCA_AI"}

SUBJECT_SYNONYMS = {
    "dbms": "DBMS",
    "database": "DBMS",
    "database management system": "DBMS",
    "database management": "DBMS",
    "java": "Java",
    "core java": "Java",
    "advanced java": "Java",
    "os": "Operating Systems",
    "operating system": "Operating Systems",
    "operating systems": "Operating Systems",
    "cn": "Computer Networks",
    "network": "Computer Networks",
    "computer networks": "Computer Networks",
    "ds": "Data Structures",
    "data structure": "Data Structures",
    "data structures": "Data Structures",
    "python": "Python",
    "python programming": "Python",
    "web tech": "Web Technologies",
    "web technology": "Web Technologies",
    "cloud": "Cloud Computing",
    "cloud computing": "Cloud Computing",
    "se": "Software Engineering",
    "software engineering": "Software Engineering"
}

CATEGORY_SYNONYMS = {
    "notes": "Lecture Notes",
    "lecture notes": "Lecture Notes",
    "lecture": "Lecture Notes",
    "slides": "Lecture Notes",
    "ppt": "Lecture Notes",
    "question paper": "Previous Examination Papers",
    "question papers": "Previous Examination Papers",
    "questions": "Previous Examination Papers",
    "exam paper": "Previous Examination Papers",
    "exam papers": "Previous Examination Papers",
    "previous paper": "Previous Examination Papers",
    "model paper": "Previous Examination Papers",
    "assignment": "Assignments",
    "assignments": "Assignments",
    "homework": "Assignments",
    "lab": "Study Materials",
    "lab manual": "Study Materials",
    "practical": "Study Materials",
    "manual": "Study Materials",
    "study material": "Study Materials",
    "study materials": "Study Materials",
    "material": "Study Materials",
    "materials": "Study Materials",
    "book": "E-books and Reference Documents",
    "books": "E-books and Reference Documents",
    "ebook": "E-books and Reference Documents",
    "ebooks": "E-books and Reference Documents",
    "e-book": "E-books and Reference Documents",
    "e-books": "E-books and Reference Documents",
    "reference": "E-books and Reference Documents",
    "syllabus": "Other Approved Academic Resources",
    "curriculum": "Other Approved Academic Resources"
}

DEPARTMENT_SYNONYMS = {
    "mca": "MCA",
    "computer application": "MCA",
    "computer applications": "MCA",
    "cs": "CS",
    "computer science": "CS",
    "bsc cs": "CS",
    "it": "IT",
    "information technology": "IT"
}

# Few-shot prompts to guide Qwen3-0.6B to emit strictly valid JSON
FEW_SHOT_QUERY_EXAMPLES = [
    {
        "role": "user",
        "content": "Find DBMS notes for Unit 3"
    },
    {
        "role": "assistant",
        "content": '{"subject": "DBMS", "semester": null, "unit": 3, "category": "Lecture Notes", "department": null, "keywords": ""}'
    },
    {
        "role": "user",
        "content": "Show Java question papers for Semester 2 in MCA"
    },
    {
        "role": "assistant",
        "content": '{"subject": "Java", "semester": 2, "unit": null, "category": "Previous Examination Papers", "department": "MCA", "keywords": ""}'
    },
    {
        "role": "user",
        "content": "Operating systems process scheduling algorithms"
    },
    {
        "role": "assistant",
        "content": '{"subject": "Operating Systems", "semester": null, "unit": null, "category": null, "department": null, "keywords": "process scheduling algorithms"}'
    }
]

SYSTEM_PROMPT = (
    "You are a strict academic query parser for a college resource finder. "
    "Given a user search prompt, extract search entities and return a JSON object with keys: "
    "'subject', 'semester' (1-8 or null), 'unit' (1-10 or null), 'category' (one of: 'Lecture Notes', "
    "'Previous Examination Papers', 'Assignments', 'Study Materials', 'E-books and Reference Documents', "
    "'Other Approved Academic Resources' or null), 'department' (or null), and 'keywords' (string). "
    "Do not include any Markdown or explanatory text outside the JSON object."
)


def _validate_and_sanitize_schema(parsed: Dict[str, Any]) -> Dict[str, Any]:
    """
    Validation Layer: Enforces database integrity constraints on LLM output.
    Rejects or clamps invalid semester (must be 1-8), unit (1-10), and unknown categories.
    """
    sanitized = {}

    # 1. Semester validation (1 to 8 only)
    sem = parsed.get("semester")
    if sem is not None:
        try:
            sem_int = int(sem)
            if 1 <= sem_int <= 8:
                sanitized["semester"] = sem_int
            else:
                sanitized["semester"] = None
        except (ValueError, TypeError):
            sanitized["semester"] = None
    else:
        sanitized["semester"] = None

    # 2. Unit validation (1 to 10 only)
    unit = parsed.get("unit")
    if unit is not None:
        try:
            unit_int = int(unit)
            if 1 <= unit_int <= 10:
                sanitized["unit"] = unit_int
            else:
                sanitized["unit"] = None
        except (ValueError, TypeError):
            sanitized["unit"] = None
    else:
        sanitized["unit"] = None

    # 3. Category validation (must be in ALLOWED_CATEGORIES or mapped synonym)
    cat = parsed.get("category")
    if cat and isinstance(cat, str):
        cat_lower = cat.strip().lower()
        if cat in ALLOWED_CATEGORIES:
            sanitized["category"] = cat
        elif cat_lower in CATEGORY_SYNONYMS:
            sanitized["category"] = CATEGORY_SYNONYMS[cat_lower]
        else:
            sanitized["category"] = None
    else:
        sanitized["category"] = None

    # 4. Department validation
    dept = parsed.get("department")
    if dept and isinstance(dept, str):
        dept_clean = dept.strip().upper()
        if dept_clean in VALID_DEPARTMENTS or dept.lower() in DEPARTMENT_SYNONYMS:
            sanitized["department"] = DEPARTMENT_SYNONYMS.get(dept.lower(), dept_clean)
        else:
            sanitized["department"] = None
    else:
        sanitized["department"] = None

    # 5. Subject
    subj = parsed.get("subject")
    if subj and isinstance(subj, str):
        subj_clean = subj.strip()
        subj_lower = subj_clean.lower()
        sanitized["subject"] = SUBJECT_SYNONYMS.get(subj_lower, subj_clean)
    else:
        sanitized["subject"] = None

    # 6. Keywords
    kw = parsed.get("keywords", "")
    sanitized["keywords"] = str(kw).strip() if kw else ""

    return sanitized


def parse_deterministic(query: str, user_dept_code: Optional[str] = None) -> Dict[str, Any]:
    """
    Deterministic regex and rule-based query parser.
    Zero latency and 100% offline baseline.
    """
    if not query or not query.strip():
        return {
            "original_query": "",
            "normalized_keywords": "",
            "subject": None,
            "semester": None,
            "unit": None,
            "category": None,
            "department": user_dept_code,
            "department_scoped": False,
            "confidence": 0.0,
            "explanation": "Empty query provided.",
            "source": "deterministic"
        }

    raw = query.strip()
    working = raw.lower()
    extracted_items = []

    # 1. Department Scope
    dept_scoped = False
    target_dept = None
    if re.search(r"\bmy\s+(?:department|dept)\b", working):
        dept_scoped = True
        target_dept = user_dept_code
        working = re.sub(r"\bmy\s+(?:department|dept)\b", " ", working)
        extracted_items.append(f"Department: current user's department ({user_dept_code or 'Not set'})")
    else:
        for term, code in DEPARTMENT_SYNONYMS.items():
            pattern = r"\b" + re.escape(term) + r"\b"
            if re.search(pattern, working):
                target_dept = code
                working = re.sub(pattern, " ", working)
                extracted_items.append(f"Department: {code}")
                break

    # 2. Semester extraction (1 to 8)
    extracted_sem = None
    sem_match = re.search(r"\b(?:semester|sem|s)[\s\-_]*([1-8])\b", working)
    if sem_match:
        extracted_sem = int(sem_match.group(1))
        working = sem_match.re.sub(" ", working, count=1)
        extracted_items.append(f"Semester {extracted_sem}")

    # 3. Unit extraction (1 to 10)
    extracted_unit = None
    unit_match = re.search(r"\b(?:unit|module|u)[\s\-_]*([1-9]|10)\b", working)
    if unit_match:
        extracted_unit = int(unit_match.group(1))
        working = unit_match.re.sub(" ", working, count=1)
        extracted_items.append(f"Unit {extracted_unit}")

    # 4. Category extraction
    extracted_category = None
    for term in sorted(CATEGORY_SYNONYMS.keys(), key=lambda t: -len(t)):
        pattern = r"\b" + re.escape(term) + r"\b"
        if re.search(pattern, working):
            extracted_category = CATEGORY_SYNONYMS[term]
            working = re.sub(pattern, " ", working, count=1)
            extracted_items.append(f"Category: {extracted_category}")
            break

    # 5. Subject extraction
    extracted_subject = None
    for term in sorted(SUBJECT_SYNONYMS.keys(), key=lambda t: -len(t)):
        pattern = r"\b" + re.escape(term) + r"\b"
        if re.search(pattern, working):
            extracted_subject = SUBJECT_SYNONYMS[term]
            working = re.sub(pattern, " ", working, count=1)
            extracted_items.append(f"Subject: {extracted_subject}")
            break

    # 6. Residual Keywords filtering
    tokens = re.split(r"[\s,;:\.?!]+", working)
    residual = [t for t in tokens if t and t not in STOPWORDS and len(t) > 1]
    normalized_keywords = " ".join(residual).strip()

    weights = [
        (extracted_subject, 0.35),
        (extracted_unit, 0.20),
        (extracted_sem, 0.20),
        (extracted_category, 0.15),
        (target_dept, 0.10)
    ]
    confidence = sum(w for val, w in weights if val is not None)
    if not extracted_items and normalized_keywords:
        confidence = 0.30
    confidence = min(round(confidence, 2), 1.0)

    if extracted_items:
        explanation = "Extracted criteria: " + ", ".join(extracted_items)
        if normalized_keywords:
            explanation += f" (Keywords: '{normalized_keywords}')"
    elif normalized_keywords:
        explanation = f"Performing keyword search on '{normalized_keywords}'"
    else:
        explanation = "Broad search without specific filters."

    return {
        "original_query": raw,
        "normalized_keywords": normalized_keywords,
        "subject": extracted_subject,
        "semester": extracted_sem,
        "unit": extracted_unit,
        "category": extracted_category,
        "department": target_dept,
        "department_scoped": dept_scoped,
        "confidence": confidence,
        "explanation": explanation,
        "source": "deterministic"
    }


def parse_natural_language_query(query: str,
                                 user_dept_code: Optional[str] = None,
                                 use_llm: bool = True) -> Dict[str, Any]:
    """
    Hybrid Query Parsing:
    Combines deterministic regex/keyword extraction with local Qwen3-0.6B LLM intent refinement.
    Deterministic high-confidence extractions always anchor the result.
    If LLM is offline or output fails schema validation, falls back with zero latency.
    """
    base_result = parse_deterministic(query, user_dept_code)
    if not query or not query.strip():
        return base_result

    # If LLM disabled or deterministic confidence is already very high (e.g. subject + unit + category)
    if not use_llm or os.environ.get("AI_DISABLE_LLM", "false").lower() == "true":
        return base_result

    # Query Qwen3-0.6B via Ollama for semantic refinement
    llm_raw = query_ollama_json(
        prompt=query.strip(),
        system_prompt=SYSTEM_PROMPT,
        few_shot_examples=FEW_SHOT_QUERY_EXAMPLES,
        timeout=6.0
    )

    if not llm_raw or not isinstance(llm_raw, dict):
        return base_result

    # Run through schema validation layer
    validated_llm = _validate_and_sanitize_schema(llm_raw)

    # Hybrid merging: deterministic exact regex holds higher authority for exact numbers
    merged_sem = base_result["semester"] if base_result["semester"] is not None else validated_llm["semester"]
    merged_unit = base_result["unit"] if base_result["unit"] is not None else validated_llm["unit"]
    merged_cat = base_result["category"] if base_result["category"] is not None else validated_llm["category"]
    merged_dept = base_result["department"] if base_result["department"] is not None else validated_llm["department"]
    merged_subj = base_result["subject"] if base_result["subject"] is not None else validated_llm["subject"]

    # Keywords: combine or prioritize
    merged_kw = base_result["normalized_keywords"]
    if not merged_kw and validated_llm["keywords"]:
        merged_kw = validated_llm["keywords"]

    # Recompute explanation and confidence
    extracted_items = []
    if merged_dept:
        extracted_items.append(f"Department: {merged_dept}")
    if merged_sem:
        extracted_items.append(f"Semester {merged_sem}")
    if merged_unit:
        extracted_items.append(f"Unit {merged_unit}")
    if merged_cat:
        extracted_items.append(f"Category: {merged_cat}")
    if merged_subj:
        extracted_items.append(f"Subject: {merged_subj}")

    weights = [
        (merged_subj, 0.35),
        (merged_unit, 0.20),
        (merged_sem, 0.20),
        (merged_cat, 0.15),
        (merged_dept, 0.10)
    ]
    confidence = min(round(sum(w for val, w in weights if val is not None), 2), 1.0)
    if not extracted_items and merged_kw:
        confidence = 0.30

    explanation = "Extracted criteria: " + ", ".join(extracted_items) if extracted_items else f"Keyword search: '{merged_kw}'"
    if extracted_items and merged_kw:
        explanation += f" (Keywords: '{merged_kw}')"

    return {
        "original_query": query.strip(),
        "normalized_keywords": merged_kw,
        "subject": merged_subj,
        "semester": merged_sem,
        "unit": merged_unit,
        "category": merged_cat,
        "department": merged_dept,
        "department_scoped": base_result["department_scoped"],
        "confidence": confidence,
        "explanation": explanation,
        "source": "hybrid_qwen3"
    }
