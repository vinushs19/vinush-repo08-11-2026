"""
LLM Client for Local AI Model Integration.
Supports LM Studio (OpenAI-compatible API) and Ollama.
Engineered specifically for low-resource hardware (e.g., 8GB RAM laptops).
Enforces strict timeouts, low temperature, and defensive fallbacks.
"""

import os
import re
import json
import time
import logging
from typing import Dict, Any, List, Optional, Tuple
import httpx

logger = logging.getLogger("ai_service.llm")

# ---------------------------------------------------------------------------
# Provider detection: LM Studio (OpenAI-compatible) vs Ollama
# ---------------------------------------------------------------------------
# LM Studio default: http://localhost:1234  (OpenAI-compatible /v1/... API)
# Ollama default:    http://localhost:11434  (Native /api/... API)
#
# Set LLM_PROVIDER=lmstudio or LLM_PROVIDER=ollama to override auto-detection.
# Set LLM_HOST to override the base URL for either provider.
# ---------------------------------------------------------------------------

LLM_PROVIDER = os.environ.get("LLM_PROVIDER", "auto").lower()  # auto, lmstudio, ollama
LLM_HOST = os.environ.get("LLM_HOST", "").rstrip("/")
LLM_MODEL = os.environ.get("LLM_MODEL", os.environ.get("OLLAMA_MODEL", "qwen3-0.6b"))
LLM_TIMEOUT = float(os.environ.get("LLM_TIMEOUT", os.environ.get("OLLAMA_TIMEOUT", "15.0")))
LLM_NUM_CTX = int(os.environ.get("LLM_NUM_CTX", os.environ.get("OLLAMA_NUM_CTX", "2048")))
LLM_EMBED_MODEL = os.environ.get("LLM_EMBED_MODEL",
                                  os.environ.get("OLLAMA_EMBED_MODEL",
                                                 "text-embedding-nomic-embed-text-v1.5"))

# Legacy env var aliases (backward compatible)
OLLAMA_HOST = os.environ.get("OLLAMA_HOST", "http://127.0.0.1:11434").rstrip("/")
OLLAMA_MODEL = LLM_MODEL
OLLAMA_TIMEOUT = LLM_TIMEOUT
OLLAMA_KEEP_ALIVE = os.environ.get("OLLAMA_KEEP_ALIVE", "2m")
OLLAMA_NUM_CTX = LLM_NUM_CTX
OLLAMA_EMBED_MODEL = LLM_EMBED_MODEL

# LM Studio defaults
LMSTUDIO_HOST = os.environ.get("LMSTUDIO_HOST", "http://127.0.0.1:1234").rstrip("/")

# Simple in-memory cache to prevent blocking health checks
_STATUS_CACHE: Dict[str, Any] = {}
_LAST_STATUS_CHECK = 0.0
_STATUS_CACHE_TTL = 5.0  # 5 seconds


def _detect_provider() -> Tuple[str, str]:
    """
    Auto-detect which LLM backend is running.
    Priority: explicit env var > LM Studio on :1234 > Ollama on :11434
    Returns (provider_name, base_url).
    """
    if LLM_HOST:
        # User specified explicit host
        provider = LLM_PROVIDER if LLM_PROVIDER != "auto" else "lmstudio"
        return provider, LLM_HOST

    if LLM_PROVIDER == "lmstudio":
        return "lmstudio", LMSTUDIO_HOST
    elif LLM_PROVIDER == "ollama":
        return "ollama", OLLAMA_HOST

    # Auto-detect: try LM Studio first (port 1234), then Ollama (port 11434)
    try:
        with httpx.Client(timeout=1.5) as client:
            resp = client.get(f"{LMSTUDIO_HOST}/v1/models")
            if resp.status_code == 200:
                return "lmstudio", LMSTUDIO_HOST
    except Exception:
        pass

    try:
        with httpx.Client(timeout=1.5) as client:
            resp = client.get(f"{OLLAMA_HOST}/api/tags")
            if resp.status_code == 200:
                return "ollama", OLLAMA_HOST
    except Exception:
        pass

    # Default to LM Studio
    return "lmstudio", LMSTUDIO_HOST


def get_ollama_status() -> Dict[str, Any]:
    """
    Checks whether the local LLM backend (LM Studio or Ollama) is reachable
    and whether the target model is loaded.
    Safe and non-blocking with a short timeout and 5-second TTL cache.
    """
    global _STATUS_CACHE, _LAST_STATUS_CHECK
    now = time.time()
    if _STATUS_CACHE and (now - _LAST_STATUS_CHECK) < _STATUS_CACHE_TTL:
        return dict(_STATUS_CACHE)

    provider, host = _detect_provider()

    status = {
        "reachable": False,
        "provider": provider,
        "host": host,
        "target_model": LLM_MODEL,
        "model_available": False,
        "available_models": [],
        "keep_alive": OLLAMA_KEEP_ALIVE if provider == "ollama" else "n/a"
    }

    try:
        with httpx.Client(timeout=2.0) as client:
            if provider == "lmstudio":
                resp = client.get(f"{host}/v1/models")
                if resp.status_code == 200:
                    data = resp.json()
                    models = [m.get("id", "") for m in data.get("data", [])]
                    model_present = any(
                        LLM_MODEL in m or m.startswith(LLM_MODEL.split(":")[0])
                        for m in models
                    )
                    status.update({
                        "reachable": True,
                        "model_available": model_present,
                        "available_models": models
                    })
            else:
                # Ollama
                resp = client.get(f"{host}/api/tags")
                if resp.status_code == 200:
                    data = resp.json()
                    models = [m.get("name", "") for m in data.get("models", [])]
                    model_present = any(
                        LLM_MODEL in m or m.startswith(LLM_MODEL.split(":")[0])
                        for m in models
                    )
                    status.update({
                        "reachable": True,
                        "model_available": model_present,
                        "available_models": models
                    })
    except Exception as ex:
        logger.debug("LLM status check failed (%s): %s", provider, str(ex))

    _STATUS_CACHE = status
    _LAST_STATUS_CHECK = now
    return dict(_STATUS_CACHE)


def clean_json_response(raw_text: str) -> Optional[Dict[str, Any]]:
    """
    Strips markdown code fences (```json ... ```) or extracts the first valid JSON object.
    Defends against small 0.6B LLMs wrapping JSON in explanatory prose.
    """
    if not raw_text or not raw_text.strip():
        return None

    cleaned = raw_text.strip()

    # Strip <think>...</think> blocks that Qwen3 may produce
    cleaned = re.sub(r"<think>.*?</think>", "", cleaned, flags=re.DOTALL).strip()

    # 1. Direct parse attempt
    try:
        return json.loads(cleaned)
    except Exception:
        pass

    # 2. Extract from markdown code fence
    fence_match = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", cleaned, re.DOTALL)
    if fence_match:
        try:
            return json.loads(fence_match.group(1))
        except Exception:
            pass

    # 3. Find outermost braces
    brace_match = re.search(r"(\{.*\})", cleaned, re.DOTALL)
    if brace_match:
        try:
            return json.loads(brace_match.group(1))
        except Exception:
            pass

    return None


def _clean_text_response(raw_text: str) -> str:
    """Strip <think>...</think> reasoning blocks from Qwen3 text responses."""
    if not raw_text:
        return ""
    cleaned = re.sub(r"<think>.*?</think>", "", raw_text, flags=re.DOTALL).strip()
    return cleaned


# ---------------------------------------------------------------------------
# LM Studio (OpenAI-compatible) functions
# ---------------------------------------------------------------------------

def _lmstudio_chat(messages: List[Dict[str, str]],
                   host: str,
                   response_format: Optional[Dict] = None,
                   max_tokens: int = 256,
                   temperature: float = 0.1,
                   timeout: float = 15.0) -> Optional[str]:
    """Send a chat completion request to LM Studio's OpenAI-compatible API."""
    payload: Dict[str, Any] = {
        "model": LLM_MODEL,
        "messages": messages,
        "temperature": temperature,
        "max_tokens": max_tokens,
        "stream": False,
    }
    if response_format:
        payload["response_format"] = response_format

    try:
        with httpx.Client(timeout=timeout) as client:
            resp = client.post(f"{host}/v1/chat/completions", json=payload)
            if resp.status_code == 200:
                result = resp.json()
                content = result.get("choices", [{}])[0].get("message", {}).get("content", "")
                return content
            else:
                logger.warning("LM Studio returned status %d: %s", resp.status_code, resp.text[:200])
    except httpx.TimeoutException:
        logger.warning("LM Studio request timed out after %.1fs", timeout)
    except httpx.ConnectError:
        logger.debug("LM Studio is not running on %s", host)
    except Exception as ex:
        logger.warning("LM Studio chat error: %s", str(ex))

    return None


def _lmstudio_embedding(text: str, host: str, model: Optional[str] = None) -> Optional[List[float]]:
    """Get embeddings from LM Studio's OpenAI-compatible embeddings endpoint."""
    embed_model = model or LLM_EMBED_MODEL
    try:
        with httpx.Client(timeout=5.0) as client:
            resp = client.post(
                f"{host}/v1/embeddings",
                json={"model": embed_model, "input": text}
            )
            if resp.status_code == 200:
                data = resp.json()
                embedding = data.get("data", [{}])[0].get("embedding")
                if embedding and isinstance(embedding, list):
                    return embedding
    except Exception as ex:
        logger.debug("LM Studio embedding unavailable: %s", str(ex))
    return None


# ---------------------------------------------------------------------------
# Ollama-native functions
# ---------------------------------------------------------------------------

def _ollama_chat(messages: List[Dict[str, str]],
                 host: str,
                 use_json_format: bool = False,
                 max_tokens: int = 256,
                 temperature: float = 0.1,
                 timeout: float = 8.0) -> Optional[str]:
    """Send a chat request to Ollama's native API."""
    payload: Dict[str, Any] = {
        "model": LLM_MODEL,
        "messages": messages,
        "stream": False,
        "options": {
            "temperature": temperature,
            "num_predict": max_tokens,
            "num_ctx": OLLAMA_NUM_CTX
        },
        "keep_alive": OLLAMA_KEEP_ALIVE
    }
    if use_json_format:
        payload["format"] = "json"

    try:
        with httpx.Client(timeout=timeout) as client:
            resp = client.post(f"{host}/api/chat", json=payload)
            if resp.status_code == 200:
                result = resp.json()
                content = result.get("message", {}).get("content", "")
                return content
    except httpx.TimeoutException:
        logger.warning("Ollama request timed out after %.1fs", timeout)
    except httpx.ConnectError:
        logger.debug("Ollama is not running on %s", host)
    except Exception as ex:
        logger.warning("Ollama chat error: %s", str(ex))

    return None


def _ollama_embedding(text: str, host: str, model: Optional[str] = None) -> Optional[List[float]]:
    """Get embeddings from Ollama's native embeddings endpoint."""
    embed_model = model or OLLAMA_EMBED_MODEL
    try:
        with httpx.Client(timeout=4.0) as client:
            resp = client.post(
                f"{host}/api/embeddings",
                json={"model": embed_model, "prompt": text}
            )
            if resp.status_code == 200:
                data = resp.json()
                embedding = data.get("embedding")
                if embedding and isinstance(embedding, list):
                    return embedding
    except Exception as ex:
        logger.debug("Ollama embedding retrieval unavailable: %s", str(ex))
    return None


# ---------------------------------------------------------------------------
# Unified public API (auto-detects LM Studio vs Ollama)
# ---------------------------------------------------------------------------

def query_ollama_json(prompt: str,
                      system_prompt: str,
                      few_shot_examples: Optional[List[Dict[str, str]]] = None,
                      timeout: Optional[float] = None) -> Optional[Dict[str, Any]]:
    """
    Dispatches a structured JSON query to the local LLM (LM Studio or Ollama).
    Returns parsed dictionary or None on timeout, connection error, or malformed JSON.
    """
    provider, host = _detect_provider()

    messages = []
    if system_prompt:
        messages.append({"role": "system", "content": system_prompt})
    if few_shot_examples:
        for ex in few_shot_examples:
            messages.append({"role": ex.get("role", "user"), "content": ex.get("content", "")})
    messages.append({"role": "user", "content": prompt})

    req_timeout = timeout or LLM_TIMEOUT

    if provider == "lmstudio":
        content = _lmstudio_chat(
            messages, host,
            response_format=None,  # LM Studio handles JSON via prompt instruction
            max_tokens=256,
            temperature=0.1,
            timeout=req_timeout
        )
    else:
        content = _ollama_chat(
            messages, host,
            use_json_format=True,
            max_tokens=256,
            temperature=0.1,
            timeout=req_timeout
        )

    if content:
        cleaned = _clean_text_response(content)
        parsed = clean_json_response(cleaned)
        if parsed is not None:
            return parsed
        logger.warning("LLM output was not valid JSON: %s", (cleaned or "")[:100])

    return None


def query_ollama_text(prompt: str,
                      system_prompt: str,
                      max_tokens: int = 300,
                      temperature: float = 0.1,
                      timeout: Optional[float] = None) -> Optional[str]:
    """
    Queries local LLM for natural language text (summaries, conversational chat).
    Returns string response or None on failure/timeout.
    """
    provider, host = _detect_provider()

    messages = []
    if system_prompt:
        messages.append({"role": "system", "content": system_prompt})
    messages.append({"role": "user", "content": prompt})

    req_timeout = timeout or LLM_TIMEOUT

    if provider == "lmstudio":
        content = _lmstudio_chat(
            messages, host,
            max_tokens=max_tokens,
            temperature=temperature,
            timeout=req_timeout
        )
    else:
        content = _ollama_chat(
            messages, host,
            max_tokens=max_tokens,
            temperature=temperature,
            timeout=req_timeout
        )

    if content:
        cleaned = _clean_text_response(content)
        if cleaned and len(cleaned.strip()) >= 10:
            return cleaned.strip()

    return None


def query_ollama_embedding(text: str, model: Optional[str] = None) -> Optional[List[float]]:
    """
    Retrieves dense vector embeddings from the local LLM backend.
    Auto-detects LM Studio vs Ollama and uses the appropriate endpoint.
    """
    provider, host = _detect_provider()

    if provider == "lmstudio":
        return _lmstudio_embedding(text, host, model)
    else:
        return _ollama_embedding(text, host, model)
