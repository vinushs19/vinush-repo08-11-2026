package com.college.resourcefinder.controller;

import com.college.resourcefinder.dto.ai.*;
import com.college.resourcefinder.dto.response.ApiResponse;
import com.college.resourcefinder.dto.search.SearchQueryDto;
import com.college.resourcefinder.dto.search.SearchResultResponse;
import com.college.resourcefinder.entity.User;
import com.college.resourcefinder.service.AuthService;
import com.college.resourcefinder.service.ai.AiService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for AI and NLP capabilities.
 * Base path: {@code /api/v1/ai}
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiService aiService;
    private final AuthService authService;

    public AiController(AiService aiService, AuthService authService) {
        this.aiService = aiService;
        this.authService = authService;
    }

    private User getCurrentUserOrNull() {
        try {
            return authService.getCurrentAuthenticatedUser();
        } catch (Exception e) {
            return null;
        }
    }

    private User getRequiredCurrentUser() {
        return authService.getCurrentAuthenticatedUser();
    }

    /**
     * Diagnostic endpoint reporting AI microservice health and active provider status.
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<AiHealthResponse>> checkHealth() {
        AiHealthResponse health = aiService.checkHealth();
        return ResponseEntity.ok(ApiResponse.success(health));
    }

    /**
     * Parses an unstructured natural language search query into structured criteria.
     */
    @PostMapping("/parse-query")
    public ResponseEntity<ApiResponse<AiQueryParseResponse>> parseQuery(
            @Valid @RequestBody AiQueryParseRequest request) {
        User currentUser = getCurrentUserOrNull();
        AiQueryParseResponse parsed = aiService.parseNaturalLanguageQuery(request.getQuery(), currentUser);
        return ResponseEntity.ok(ApiResponse.success("Query parsed successfully.", parsed));
    }

    /**
     * Executes AI-augmented search combining natural language criteria extraction with database filters.
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<SearchResultResponse>> searchWithAi(
            @RequestBody(required = false) SearchQueryDto queryDto,
            @PageableDefault(size = 15) Pageable pageable) {
        User currentUser = getCurrentUserOrNull();
        SearchResultResponse response = aiService.searchWithAi(queryDto, currentUser, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Performs semantic vector search using cosine similarity across resource content.
     */
    @PostMapping("/semantic-search")
    public ResponseEntity<ApiResponse<AiSemanticSearchResponse>> semanticSearch(
            @Valid @RequestBody AiSemanticSearchRequest request) {
        User currentUser = getCurrentUserOrNull();
        AiSemanticSearchResponse response = aiService.executeSemanticSearch(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Summarizes an approved academic document (PDF, DOCX, TXT) with caching and version validation.
     */
    @PostMapping("/resources/{id}/summarize")
    public ResponseEntity<ApiResponse<AiSummarizeResponse>> summarizeResource(
            @PathVariable Long id,
            @RequestBody(required = false) AiSummarizeRequest request,
            HttpServletRequest httpRequest) {
        User currentUser = getRequiredCurrentUser();
        AiSummarizeResponse summary = aiService.summarizeResource(id, request, currentUser, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Document summarized successfully.", summary));
    }

    /**
     * Conversational academic assistant interface.
     */
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @Valid @RequestBody AiChatRequest request,
            HttpServletRequest httpRequest) {
        User currentUser = getRequiredCurrentUser();
        AiChatResponse response = aiService.chat(request, currentUser, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Parses natural language file organization requests (Restricted to Faculty and Administrators).
     */
    @PostMapping("/parse-file-save")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<ApiResponse<AiFileSaveParseResponse>> parseFileSave(
            @RequestBody Map<String, String> body) {
        String prompt = body != null ? body.get("prompt") : "";
        User currentUser = getRequiredCurrentUser();
        AiFileSaveParseResponse response = aiService.parseFileSaveIntent(prompt, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
