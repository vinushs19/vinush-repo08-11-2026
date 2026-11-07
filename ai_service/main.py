"""
AI-Based College Resource Finder — Dedicated Python FastAPI AI Service.
Optimized for local Qwen3-0.6B running via Ollama on resource-constrained hardware (e.g., 8GB RAM).
Provides hybrid NLP query parsing, document text extraction & summarization,
semantic vector scoring, and conversational academic assistant capabilities.
"""

import os
import time
import logging
from typing import Optional, List, Dict, Any
from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

from ollama_client import get_ollama_status, LLM_MODEL, LLM_HOST
# Legacy aliases for backward compatibility
OLLAMA_MODEL = LLM_MODEL
OLLAMA_HOST = LLM_HOST
from nlp_parser import parse_natural_language_query
from summarizer import summarize_document
from semantic_engine import rank_candidates_semantically, get_embedding_provider_name
from chat_engine import process_chat_message
from file_save_parser import parse_file_save_intent

# Configure safe logging (no sensitive contents logged)
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("ai_service")

MAX_INPUT_CHARS = int(os.environ.get("MAX_INPUT_CHARS", "50000"))

app = FastAPI(
    title="College Resource Finder AI Microservice",
    description="NLP Query Processing, Document Summarization, Semantic Search, and Academic Assistant (Qwen3-0.6B)",
    version="1.0.0"
)

# CORS configuration for local inter-service calls
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.middleware("http")
async def request_size_and_timing_guard(request: Request, call_next):
    """Guards against oversized requests and records latency safely."""
    start_time = time.time()

    # Content length guard
    content_length = request.headers.get("content-length")
    if content_length and int(content_length) > (MAX_INPUT_CHARS * 4):
        raise HTTPException(
            status_code=413,
            detail=f"Payload exceeds maximum allowed size ({MAX_INPUT_CHARS} characters)."
        )

    response = await call_next(request)
    duration_ms = int((time.time() - start_time) * 1000)
    logger.info("Path: %s | Status: %s | Latency: %sms", request.url.path, response.status_code, duration_ms)
    return response


# ---- Pydantic Request Models ----

class QueryParseRequest(BaseModel):
    query: str = Field(..., max_length=MAX_INPUT_CHARS, description="Unstructured natural-language user query")
    department: Optional[str] = Field(None, max_length=50, description="Current user's academic department code")


class SummarizeRequest(BaseModel):
    file_path: str = Field(..., max_length=1000, description="Absolute path to physical document on server")
    max_sentences: int = Field(4, description="Target number of summary sentences", ge=1, le=10)
    provider: Optional[str] = Field("qwen3", description="AI provider (qwen3, ollama, local_extractive)")


class SemanticCandidate(BaseModel):
    resource_id: int
    title: str = Field(..., max_length=500)
    description: Optional[str] = ""
    summary: Optional[str] = ""
    subject_name: Optional[str] = ""
    category_name: Optional[str] = ""


class SemanticSearchRequest(BaseModel):
    query: str = Field(..., max_length=1000)
    candidates: List[SemanticCandidate]


class ChatMessage(BaseModel):
    role: str = Field(..., max_length=20)
    content: str = Field(..., max_length=10000)


class ChatRequest(BaseModel):
    message: str = Field(..., max_length=MAX_INPUT_CHARS)
    conversation_history: Optional[List[ChatMessage]] = []
    user_role: Optional[str] = "STUDENT"
    user_department: Optional[str] = None


class FileSaveParseRequest(BaseModel):
    prompt: str = Field(..., max_length=MAX_INPUT_CHARS)


# ---- Endpoints ----

@app.get("/health")
def health():
    """
    Health check endpoint reporting Python microservice status,
    Ollama local connectivity, target model availability, and active embedding provider.
    """
    ollama_info = get_ollama_status()
    embed_provider = get_embedding_provider_name()

    return {
        "status": "UP",
        "service": "ai-college-resource-finder",
        "version": "1.0.0",
        "llm_provider": ollama_info.get("provider", "unknown"),
        "llm_host": ollama_info.get("host", ""),
        "target_model": LLM_MODEL,
        "llm_connected": ollama_info["reachable"],
        "model_available": ollama_info["model_available"],
        "available_models": ollama_info.get("available_models", []),
        "embedding_provider": embed_provider,
        "fallback_ready": True,
        "hardware_profile": "8GB_RAM_OPTIMIZED"
    }


@app.post("/api/v1/ai/parse-query")
def parse_query(request: QueryParseRequest):
    try:
        result = parse_natural_language_query(request.query, request.department)
        return {"success": True, "data": result}
    except Exception as ex:
        logger.error("parse_query error: %s", str(ex))
        raise HTTPException(status_code=500, detail="Query parsing failed")


@app.post("/api/v1/ai/summarize")
def summarize(request: SummarizeRequest):
    try:
        result = summarize_document(request.file_path, request.max_sentences, request.provider or "qwen3")
        return {"success": True, "data": result}
    except FileNotFoundError as fnf:
        raise HTTPException(status_code=404, detail=str(fnf))
    except ValueError as ve:
        raise HTTPException(status_code=400, detail=str(ve))
    except Exception as ex:
        logger.error("summarize error: %s", str(ex))
        raise HTTPException(status_code=500, detail=f"Summarization failed: {str(ex)}")


@app.post("/api/v1/ai/semantic-search")
def semantic_search(request: SemanticSearchRequest):
    try:
        candidate_dicts = [c.model_dump() for c in request.candidates]
        ranked = rank_candidates_semantically(request.query, candidate_dicts)
        return {"success": True, "data": ranked}
    except Exception as ex:
        logger.error("semantic_search error: %s", str(ex))
        raise HTTPException(status_code=500, detail="Semantic search failed")


@app.post("/api/v1/ai/chat")
def chat(request: ChatRequest):
    try:
        history_dicts = [{"role": m.role, "content": m.content} for m in (request.conversation_history or [])]
        result = process_chat_message(
            message=request.message,
            conversation_history=history_dicts,
            user_role=request.user_role or "STUDENT",
            user_dept_code=request.user_department
        )
        return {"success": True, "data": result}
    except Exception as ex:
        logger.error("chat error: %s", str(ex))
        raise HTTPException(status_code=500, detail="Chat processing failed")


@app.post("/api/v1/ai/parse-file-save")
def parse_file_save(request: FileSaveParseRequest):
    try:
        result = parse_file_save_intent(request.prompt)
        return {"success": True, "data": result}
    except Exception as ex:
        logger.error("parse_file_save error: %s", str(ex))
        raise HTTPException(status_code=500, detail="File save parsing failed")


if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("AI_SERVICE_PORT", "8000"))
    uvicorn.run("main:app", host="127.0.0.1", port=port, reload=False)
