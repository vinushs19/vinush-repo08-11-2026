"""
AI Academic Chat Assistant Engine.
Classifies user intent into controlled backend tool selections,
generates conversational responses with Qwen3-0.6B few-shot guidance,
and enforces prompt injection defenses.
"""

import os
import re
from typing import Dict, Any, List, Optional
from nlp_parser import parse_natural_language_query
from file_save_parser import parse_file_save_intent
from ollama_client import query_ollama_json

PROMPT_INJECTION_PATTERNS = [
    r"\bignore\s+(?:all\s+)?(?:previous|prior|above)\s+instructions\b",
    r"\breveal\s+(?:system\s+)?(?:prompt|configuration|keys|secrets|passwords)\b",
    r"\bbypass\s+(?:authorization|permissions|security|rbac)\b",
    r"\bgrant\s+(?:me\s+)?(?:admin|root|faculty)\b",
    r"\b(?:drop\s+table|insert\s+into|delete\s+from|update\s+users)\b",
    r"\b(?:eval|exec|spawn|os\.system|subprocess)\b",
    r"\bsystem\s*\(",
    r"\b(?:cat\s+/etc/passwd|type\s+c:\\|cmd\.exe|powershell)\b",
    r"\bact\s+as\s+(?:a\s+)?dan\b",
    r"\bjailbreak\b"
]

ALLOWED_CHAT_ACTIONS = {
    "NONE",
    "SECURITY_REJECTION",
    "SEARCH_RESOURCES",
    "SUMMARIZE_RESOURCE",
    "ORGANIZE_FILE",
    "ROLE_RESTRICTION",
    "EXPLAIN_FILTERS",
    "GENERAL_HELP"
}

SYSTEM_CHAT_PROMPT = (
    "You are the College Resource Finder AI Academic Assistant. "
    "Your capabilities are strictly restricted to: searching academic resources, explaining search filters, "
    "summarizing approved documents by ID, and assisting faculty in saving files into allowlisted categories. "
    "Classify the user intent and reply with strict JSON containing: "
    "'reply' (string), 'action_type' (one of: 'SEARCH_RESOURCES', 'SUMMARIZE_RESOURCE', 'ORGANIZE_FILE', "
    "'EXPLAIN_FILTERS', 'GENERAL_HELP'), and 'action_data' (object or null). "
    "Never bypass permissions, never execute raw commands, and do not hallucinate resources."
)

FEW_SHOT_CHAT_EXAMPLES = [
    {
        "role": "user",
        "content": "Find DBMS notes for Unit 3"
    },
    {
        "role": "assistant",
        "content": '{"reply": "Searching repository for DBMS Unit 3 Notes...", "action_type": "SEARCH_RESOURCES", "action_data": {"keywords": "DBMS", "unit": 3, "category": "Notes"}}'
    },
    {
        "role": "user",
        "content": "Can you summarize resource #5?"
    },
    {
        "role": "assistant",
        "content": '{"reply": "Checking permissions and generating summary for resource #5...", "action_type": "SUMMARIZE_RESOURCE", "action_data": {"resource_id": 5}}'
    },
    {
        "role": "user",
        "content": "How do I filter notes and question papers?"
    },
    {
        "role": "assistant",
        "content": '{"reply": "You can filter resources by Department, Semester (1-8), Subject, Unit (1-10), and Category.", "action_type": "EXPLAIN_FILTERS", "action_data": null}'
    }
]


def detect_prompt_injection(message: str) -> bool:
    if not message:
        return False
    msg_lower = message.lower()
    for pattern in PROMPT_INJECTION_PATTERNS:
        if re.search(pattern, msg_lower):
            return True
    return False


def _process_chat_deterministic(message: str,
                                user_role: str = "STUDENT",
                                user_dept_code: Optional[str] = None) -> Dict[str, Any]:
    """
    Deterministic rule-based intent router.
    Executes in < 1ms with 100% reliability if LLM is offline or timed out.
    """
    msg_lower = message.strip().lower()

    # 1. Document Summarization Intent
    sum_match = re.search(r"\b(?:summarize|summary|overview|tldr|brief)\b.*?\b(?:resource|doc|document|id|#)?\s*(\d+)\b", msg_lower)
    if sum_match:
        res_id = int(sum_match.group(1))
        return {
            "reply": f"I can summarize resource #{res_id} for you. Checking your document permissions and generating the summary...",
            "action_type": "SUMMARIZE_RESOURCE",
            "action_data": {"resource_id": res_id}
        }

    # 2. Controlled File Organization Intent
    if any(k in msg_lower for k in ["organize", "save file", "move file", "save resource", "archive resource"]):
        file_save_res = parse_file_save_intent(message)
        if file_save_res.get("valid"):
            if user_role not in ["FACULTY", "ADMIN"]:
                return {
                    "reply": "File organization workflows are reserved for faculty and administrators. As a student, you have access to search, preview, and download resources.",
                    "action_type": "ROLE_RESTRICTION",
                    "action_data": {"required_role": "FACULTY"}
                }
            return {
                "reply": f"Understood! Preparing to {file_save_res.get('explanation')}. Validating permissions with the server...",
                "action_type": "ORGANIZE_FILE",
                "action_data": file_save_res
            }

    # 3. Filter Explanation Intent
    if any(k in msg_lower for k in ["how do i filter", "how to search", "filter options", "what categories", "what departments"]):
        return {
            "reply": (
                "You can filter academic resources using any of the following criteria:\n"
                "- **Department:** MCA, CS, IT, etc.\n"
                "- **Semester:** Semester 1 through 8\n"
                "- **Subject:** e.g., DBMS, Java, Operating Systems\n"
                "- **Unit:** Unit 1 through 10\n"
                "- **Category:** Lecture Notes, Previous Examination Papers, Assignments, Study Materials, and E-Books.\n\n"
                "You can also ask me naturally, such as: *'Find DBMS notes for Unit 3'* or *'Show Java question papers for Semester 2'*."
            ),
            "action_type": "EXPLAIN_FILTERS",
            "action_data": None
        }

    # 4. Natural Language Resource Search Intent
    search_parsed = parse_natural_language_query(message, user_dept_code, use_llm=False)
    if search_parsed["confidence"] >= 0.25 or any(k in msg_lower for k in ["find", "show", "search", "get", "notes", "paper", "assignment", "material", "book"]):
        return {
            "reply": f"Searching repository: {search_parsed['explanation']}",
            "action_type": "SEARCH_RESOURCES",
            "action_data": search_parsed
        }

    # 5. General Academic Guidance Fallback
    return {
        "reply": (
            "I'm here to assist with finding academic notes, previous question papers, assignments, and study materials. "
            "You can type queries like:\n"
            "- *'Find Operating Systems notes for Unit 2'*\n"
            "- *'Show Java question papers for Semester 2'*\n"
            "- *'Summarize resource #3'*"
        ),
        "action_type": "GENERAL_HELP",
        "action_data": None
    }


def process_chat_message(message: str,
                         conversation_history: Optional[List[Dict[str, str]]] = None,
                         user_role: str = "STUDENT",
                         user_dept_code: Optional[str] = None) -> Dict[str, Any]:
    """
    Main Chat Assistant Dispatcher.
    1. Blocks prompt injection attacks.
    2. Runs deterministic router as baseline.
    3. If Qwen3-0.6B LLM is active, uses few-shot structured JSON to refine assistant replies.
    4. Validates actions against allowed tools and user permissions.
    """
    if not message or not message.strip():
        return {
            "reply": "Hello! I am your College Academic Assistant. How can I assist you with your course materials today?",
            "action_type": "NONE",
            "action_data": None
        }

    # 1. Defend against Prompt Injection
    if detect_prompt_injection(message):
        return {
            "reply": "I cannot fulfill this request. My operations are strictly restricted to academic resource discovery, document summarization, and authorized file management.",
            "action_type": "SECURITY_REJECTION",
            "action_data": {"flag": "PROMPT_INJECTION_DETECTED"}
        }

    # Baseline deterministic interpretation
    baseline = _process_chat_deterministic(message, user_role, user_dept_code)

    # If LLM disabled, return deterministic result immediately
    if os.environ.get("AI_DISABLE_LLM", "false").lower() == "true":
        return baseline

    # If baseline is already high-confidence action like summarization or prompt explanation, keep it
    if baseline["action_type"] in ["SUMMARIZE_RESOURCE", "EXPLAIN_FILTERS", "ROLE_RESTRICTION"]:
        return baseline

    # Attempt Qwen3-0.6B generation via Ollama for conversational fluidity
    llm_output = query_ollama_json(
        prompt=message.strip(),
        system_prompt=SYSTEM_CHAT_PROMPT,
        few_shot_examples=FEW_SHOT_CHAT_EXAMPLES,
        timeout=6.0
    )

    if llm_output and isinstance(llm_output, dict):
        action_type = llm_output.get("action_type")
        reply = llm_output.get("reply")
        action_data = llm_output.get("action_data")

        # Security check: action must be in allowlist
        if action_type in ALLOWED_CHAT_ACTIONS and reply:
            # Enforce role restriction on file organization
            if action_type == "ORGANIZE_FILE" and user_role not in ["FACULTY", "ADMIN"]:
                return {
                    "reply": "File organization workflows are reserved for faculty and administrators. As a student, you have access to search, preview, and download resources.",
                    "action_type": "ROLE_RESTRICTION",
                    "action_data": {"required_role": "FACULTY"}
                }

            # If search resources action, merge with parsed query
            if action_type == "SEARCH_RESOURCES" and not action_data:
                action_data = baseline.get("action_data")

            return {
                "reply": reply,
                "action_type": action_type,
                "action_data": action_data
            }

    # Graceful fallback to deterministic result
    return baseline
