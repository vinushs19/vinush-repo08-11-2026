package com.college.resourcefinder.service.ai;

import com.college.resourcefinder.config.AiProperties;
import com.college.resourcefinder.dto.ai.*;
import com.college.resourcefinder.dto.filesave.ControlledFileSaveRequest;
import com.college.resourcefinder.dto.filesave.ControlledFileSaveResponse;
import com.college.resourcefinder.dto.resource.ResourceResponse;
import com.college.resourcefinder.dto.search.SearchQueryDto;
import com.college.resourcefinder.dto.search.SearchResultResponse;
import com.college.resourcefinder.entity.AuditLog;
import com.college.resourcefinder.entity.ChatInteraction;
import com.college.resourcefinder.entity.Resource;
import com.college.resourcefinder.entity.User;
import com.college.resourcefinder.exception.ResourceNotFoundException;
import com.college.resourcefinder.exception.StorageException;
import com.college.resourcefinder.repository.AuditLogRepository;
import com.college.resourcefinder.repository.ChatInteractionRepository;
import com.college.resourcefinder.repository.ResourceRepository;
import com.college.resourcefinder.service.ControlledFileSaveService;
import com.college.resourcefinder.service.SearchService;
import com.college.resourcefinder.service.StorageService;
import com.college.resourcefinder.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core AI/NLP Orchestrator Service.
 * Manages provider failover, rate limiting, request validation, security guardrails,
 * document text summarization, semantic cosine vector search, and chat assistant dispatching.
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final AiProperties aiProperties;
    private final FastApiAiServiceClient fastApiClient;
    private final FallbackLocalAiService fallbackClient;
    private final ResourceRepository resourceRepository;
    private final ChatInteractionRepository chatInteractionRepository;
    private final AuditLogRepository auditLogRepository;
    private final StorageService storageService;
    private final SearchService searchService;
    private final ControlledFileSaveService controlledFileSaveService;

    // Rate limiting tracking: userId -> list of request timestamps (epoch ms)
    private final Map<Long, List<Long>> userRequestTimestamps = new ConcurrentHashMap<>();

    public AiService(AiProperties aiProperties,
                     FastApiAiServiceClient fastApiClient,
                     FallbackLocalAiService fallbackClient,
                     ResourceRepository resourceRepository,
                     ChatInteractionRepository chatInteractionRepository,
                     AuditLogRepository auditLogRepository,
                     StorageService storageService,
                     SearchService searchService,
                     ControlledFileSaveService controlledFileSaveService) {
        this.aiProperties = aiProperties;
        this.fastApiClient = fastApiClient;
        this.fallbackClient = fallbackClient;
        this.resourceRepository = resourceRepository;
        this.chatInteractionRepository = chatInteractionRepository;
        this.auditLogRepository = auditLogRepository;
        this.storageService = storageService;
        this.searchService = searchService;
        this.controlledFileSaveService = controlledFileSaveService;
    }

    /**
     * Checks client rate limit and validates input length.
     */
    private void enforceRateLimitAndSize(User user, String input) {
        if (input != null && input.length() > aiProperties.getMaxInputChars()) {
            throw new IllegalArgumentException(
                    "Input exceeds maximum allowed size of " + aiProperties.getMaxInputChars() + " characters."
            );
        }

        if (user == null || user.getUserId() == null) return;

        long now = System.currentTimeMillis();
        long windowStart = now - 60_000L; // 1-minute window

        userRequestTimestamps.compute(user.getUserId(), (id, timestamps) -> {
            if (timestamps == null) {
                timestamps = new ArrayList<>();
            }
            // Remove timestamps older than 60s
            timestamps.removeIf(ts -> ts < windowStart);
            if (timestamps.size() >= aiProperties.getRateLimitPerMinute()) {
                throw new IllegalArgumentException(
                        "Rate limit exceeded: You have submitted " + timestamps.size() +
                        " requests in the last minute. Maximum allowed is " + aiProperties.getRateLimitPerMinute() + "."
                );
            }
            timestamps.add(now);
            return timestamps;
        });
    }

    /**
     * Obtains the active AI client with automatic failover to local fallback.
     */
    private AiServiceClient getClient() {
        if (aiProperties.isEnabled() && fastApiClient.isAvailable()) {
            return fastApiClient;
        }
        return fallbackClient;
    }

    /**
     * Parses an unstructured natural language search query.
     */
    public AiQueryParseResponse parseNaturalLanguageQuery(String query, User currentUser) {
        enforceRateLimitAndSize(currentUser, query);
        String deptCode = (currentUser != null && currentUser.getDepartment() != null) ?
                currentUser.getDepartment().getDepartmentCode() : null;

        try {
            return getClient().parseQuery(query, deptCode);
        } catch (Exception ex) {
            log.warn("FastAPI query parser failed ({}), falling back to internal Java NLP", ex.getMessage());
            return fallbackClient.parseQuery(query, deptCode);
        }
    }

    /**
     * Executes AI-enhanced search by merging natural language entity extraction with structured database queries.
     */
    @Transactional
    public SearchResultResponse searchWithAi(SearchQueryDto queryDto, User currentUser, Pageable pageable) {
        String rawQuery = (queryDto != null && queryDto.getQuery() != null) ? queryDto.getQuery().trim() : "";
        enforceRateLimitAndSize(currentUser, rawQuery);

        AiQueryParseResponse aiParsed = parseNaturalLanguageQuery(rawQuery, currentUser);

        // Pre-populate queryDto with AI-extracted entities if not explicitly overridden by client
        if (queryDto != null) {
            if (queryDto.getSemester() == null && aiParsed.getSemester() != null) {
                queryDto.setSemester(aiParsed.getSemester());
            }
            if (queryDto.getResourceType() == null && aiParsed.getCategory() != null) {
                queryDto.setResourceType(aiParsed.getCategory());
            }
        }

        return searchService.search(queryDto, currentUser, pageable);
    }

    /**
     * Executes semantic vector search across repository resources.
     */
    @Transactional(readOnly = true)
    public AiSemanticSearchResponse executeSemanticSearch(AiSemanticSearchRequest request, User currentUser) {
        String query = request.getQuery() != null ? request.getQuery().trim() : "";
        enforceRateLimitAndSize(currentUser, query);

        // Fetch candidate active resources based on optional filters
        Specification<Resource> spec = (root, q, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.isTrue(root.get("isActive")));
            predicates.add(cb.equal(root.get("status"), "ACTIVE"));

            if (request.getDepartmentId() != null) {
                predicates.add(cb.equal(root.get("subject").get("department").get("departmentId"), request.getDepartmentId()));
            }
            if (request.getSemester() != null) {
                predicates.add(cb.equal(root.get("subject").get("semester"), request.getSemester()));
            }
            if (request.getSubjectId() != null) {
                predicates.add(cb.equal(root.get("subject").get("subjectId"), request.getSubjectId()));
            }
            if (request.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("categoryId"), request.getCategoryId()));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        List<Resource> candidates = resourceRepository.findAll(spec);
        if (candidates.isEmpty()) {
            return new AiSemanticSearchResponse(query, Collections.emptyList());
        }

        List<AiSemanticCandidate> candidateDtos = new ArrayList<>();
        for (Resource r : candidates) {
            candidateDtos.add(new AiSemanticCandidate(
                    r.getResourceId(),
                    r.getTitle(),
                    r.getDescription(),
                    r.getSummary(),
                    r.getSubject() != null ? r.getSubject().getSubjectName() : "",
                    r.getCategory() != null ? r.getCategory().getCategoryName() : ""
            ));
        }

        List<AiSemanticCandidate> rankedCandidates;
        try {
            rankedCandidates = getClient().semanticSearch(query, candidateDtos);
        } catch (Exception ex) {
            log.warn("FastAPI semantic search failed ({}), falling back to local Java vector scoring", ex.getMessage());
            rankedCandidates = fallbackClient.semanticSearch(query, candidateDtos);
        }

        // Map ranked candidates back to ResourceResponse
        Map<Long, Resource> resourceMap = new HashMap<>();
        for (Resource r : candidates) {
            resourceMap.put(r.getResourceId(), r);
        }

        int limit = Math.max(1, Math.min(request.getLimit(), 50));
        List<ResourceResponse> results = new ArrayList<>();

        for (AiSemanticCandidate c : rankedCandidates) {
            if (c.getSemanticSimilarity() != null && c.getSemanticSimilarity() > 0.05) {
                Resource entity = resourceMap.get(c.getResourceId());
                if (entity != null) {
                    ResourceResponse resp = ResourceResponse.fromEntity(entity);
                    results.add(resp);
                    if (results.size() >= limit) break;
                }
            }
        }

        // If semantic scoring yielded no confident matches, execute deterministic SQL search fallback
        if (results.isEmpty() && !query.isBlank()) {
            SearchQueryDto fallbackQuery = new SearchQueryDto();
            fallbackQuery.setQuery(query);
            fallbackQuery.setDepartmentId(request.getDepartmentId());
            fallbackQuery.setSemester(request.getSemester());
            fallbackQuery.setSubjectId(request.getSubjectId());
            fallbackQuery.setCategoryId(request.getCategoryId());
            SearchResultResponse sqlFallback = searchService.search(fallbackQuery, currentUser, PageRequest.of(0, limit));
            if (sqlFallback != null && sqlFallback.getResults() != null) {
                results = sqlFallback.getResults();
            }
        }

        return new AiSemanticSearchResponse(query, results);
    }

    /**
     * Extracts text and generates or retrieves a concise summary of an approved academic document.
     */
    @Transactional
    public AiSummarizeResponse summarizeResource(Long resourceId,
                                                 AiSummarizeRequest request,
                                                 User currentUser,
                                                 HttpServletRequest httpRequest) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", resourceId));

        if (!Boolean.TRUE.equals(resource.getIsActive())) {
            throw new ResourceNotFoundException("The requested resource is inactive or deleted.");
        }

        Path absolutePath = storageService.resolveAbsolutePath(resource.getFilePath());
        if (!Files.exists(absolutePath)) {
            throw new ResourceNotFoundException("The underlying physical file is not available on the server: " + resource.getFileName());
        }

        String fileName = resource.getFileName() != null ? resource.getFileName().toLowerCase() : "";
        String fileType = resource.getFileType() != null ? resource.getFileType().toLowerCase() : "";
        boolean isSupported = fileName.endsWith(".pdf") || fileName.endsWith(".docx") || fileName.endsWith(".txt")
                || "pdf".equals(fileType) || "docx".equals(fileType) || "txt".equals(fileType);
        if (!isSupported) {
            throw new IllegalArgumentException(
                    "Text summarization is currently supported only for PDF, DOCX, and TXT documents. " +
                    "Presentation and spreadsheet files (.pptx, .xlsx) are not supported."
            );
        }

        // Cache verification: if summary exists and file has not been modified
        if (resource.getSummary() != null && !resource.getSummary().isBlank() && resource.getFileHash() != null) {
            AiSummarizeResponse cachedResp = new AiSummarizeResponse();
            cachedResp.setResourceId(resource.getResourceId());
            cachedResp.setSummary(resource.getSummary());
            cachedResp.setFileHash(resource.getFileHash());
            cachedResp.setGeneratedAt(resource.getSummaryUpdatedAt() != null ? resource.getSummaryUpdatedAt() : resource.getUpdatedAt());
            cachedResp.setMethod("cached_database");
            cachedResp.setCached(true);
            cachedResp.setWordCount(resource.getSummary().split("\\s+").length);
            return cachedResp;
        }

        int maxSentences = (request != null && request.getMaxSentences() > 0) ? request.getMaxSentences() : 4;
        String provider = (request != null) ? request.getProvider() : null;

        AiSummarizeResponse summaryResp;
        try {
            summaryResp = getClient().summarize(absolutePath.toString(), maxSentences, provider);
        } catch (Exception ex) {
            log.warn("FastAPI summarizer failed ({}), falling back to local Java text summarizer", ex.getMessage());
            summaryResp = fallbackClient.summarize(absolutePath.toString(), maxSentences, provider);
        }

        summaryResp.setResourceId(resource.getResourceId());
        summaryResp.setGeneratedAt(LocalDateTime.now());
        summaryResp.setCached(false);

        // Only persist and cache valid generated summaries (never cache fallback messages or errors)
        boolean shouldPersist = summaryResp.getMethod() != null
                && !summaryResp.getMethod().startsWith("fallback")
                && !"unsupported_format".equals(summaryResp.getMethod())
                && summaryResp.getFileHash() != null
                && summaryResp.getSummary() != null
                && !summaryResp.getSummary().contains("requires the active AI");

        if (shouldPersist) {
            resource.setSummary(summaryResp.getSummary());
            resource.setSummaryUpdatedAt(summaryResp.getGeneratedAt());
            resource.setFileHash(summaryResp.getFileHash());
            resourceRepository.save(resource);
        }

        logAudit(currentUser, "RESOURCE_SUMMARIZE", "SUCCESS", httpRequest,
                "Generated " + summaryResp.getMethod() + " summary for resource #" + resourceId +
                " (" + summaryResp.getWordCount() + " words)");

        return summaryResp;
    }

    /**
     * Conversational interface dispatching natural-language requests into validated backend operations.
     */
    @Transactional
    public AiChatResponse chat(AiChatRequest request, User currentUser, HttpServletRequest httpRequest) {
        String msg = (request != null && request.getMessage() != null) ? request.getMessage().trim() : "";
        enforceRateLimitAndSize(currentUser, msg);

        long start = System.currentTimeMillis();
        String deptCode = (currentUser != null && currentUser.getDepartment() != null) ?
                currentUser.getDepartment().getDepartmentCode() : null;
        String role = (currentUser != null) ? currentUser.getRole() : "STUDENT";

        AiChatResponse response;
        try {
            response = getClient().chat(msg, request.getConversationHistory(), role, deptCode);
        } catch (Exception ex) {
            log.warn("FastAPI chat engine failed ({}), using fallback Java assistant", ex.getMessage());
            response = fallbackClient.chat(msg, request.getConversationHistory(), role, deptCode);
        }

        // Execute action if model planned a backend operation
        if ("SEARCH_RESOURCES".equalsIgnoreCase(response.getActionType()) && response.getActionData() != null) {
            SearchQueryDto searchDto = new SearchQueryDto();
            Map<String, Object> data = response.getActionData();

            if (data.get("keywords") != null) searchDto.setQuery((String) data.get("keywords"));
            if (data.get("semester") != null) searchDto.setSemester(((Number) data.get("semester")).intValue());
            if (data.get("category") != null) searchDto.setResourceType((String) data.get("category"));

            SearchResultResponse searchResults = searchService.search(searchDto, currentUser, PageRequest.of(0, 5));
            response.setResourceResults(searchResults.getResults());
        } else if ("SECURITY_REJECTION".equalsIgnoreCase(response.getActionType())) {
            logAudit(currentUser, "AI_PROMPT_INJECTION_REJECTED", "DENIED", httpRequest,
                    "Rejected suspicious prompt: " + (msg.length() > 60 ? msg.substring(0, 60) + "..." : msg));
        }

        long latency = System.currentTimeMillis() - start;
        response.setLatencyMs(latency);
        response.setSessionId(request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString());

        // Record interaction in chat_interactions table
        try {
            ChatInteraction interaction = new ChatInteraction(
                    currentUser,
                    response.getSessionId(),
                    msg,
                    response.getActionType(),
                    response.getReply(),
                    "SUCCESS",
                    (int) latency
            );
            chatInteractionRepository.save(interaction);
        } catch (Exception ex) {
            log.warn("Failed to persist chat interaction: {}", ex.getMessage());
        }

        return response;
    }

    /**
     * Translates a natural language instruction into a structured controlled file save request.
     */
    public AiFileSaveParseResponse parseFileSaveIntent(String prompt, User currentUser) {
        enforceRateLimitAndSize(currentUser, prompt);
        if (!"FACULTY".equalsIgnoreCase(currentUser.getRole()) && !"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            throw new AccessDeniedException("File organization workflows are reserved for faculty and administrators.");
        }

        try {
            return getClient().parseFileSave(prompt);
        } catch (Exception ex) {
            log.warn("FastAPI file save parser failed ({}), falling back to local Java parser", ex.getMessage());
            return fallbackClient.parseFileSave(prompt);
        }
    }

    /**
     * Health check verifying AI microservice status and fallback readiness.
     */
    public AiHealthResponse checkHealth() {
        AiHealthResponse health = new AiHealthResponse();
        health.setServiceUrl(aiProperties.getServiceUrl());

        long start = System.currentTimeMillis();
        boolean fastApiUp = fastApiClient.isAvailable();
        long latency = System.currentTimeMillis() - start;

        health.setLatencyMs(latency);
        if (fastApiUp) {
            health.setStatus("UP");
            health.setActiveProvider("FASTAPI_PYTHON_SERVICE");
            health.setFallbackActive(false);
            health.setDetails("Python FastAPI service online on " + aiProperties.getServiceUrl());
        } else {
            health.setStatus("UP"); // Spring Boot is UP and operating via deterministic in-JVM fallback
            health.setActiveProvider("LOCAL_JAVA_FALLBACK");
            health.setFallbackActive(true);
            health.setDetails("FastAPI service offline or unconfigured. Deterministic in-JVM AI engine active.");
        }
        return health;
    }

    private void logAudit(User user, String action, String status, HttpServletRequest request, String details) {
        try {
            AuditLog audit = new AuditLog();
            audit.setUser(user);
            audit.setAction(action);
            audit.setStatus(status);
            audit.setResourceType("AI_SERVICE");
            audit.setResourceIdentifier(user != null ? user.getEmail() : "ANONYMOUS");
            audit.setIpAddress(request != null ? SecurityUtils.getClientIp(request) : "LOCAL");
            audit.setDetails(details);
            auditLogRepository.save(audit);
        } catch (Exception ex) {
            log.warn("Failed to record AI audit log: {}", ex.getMessage());
        }
    }
}
