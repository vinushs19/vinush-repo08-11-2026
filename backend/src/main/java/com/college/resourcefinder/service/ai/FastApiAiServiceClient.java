package com.college.resourcefinder.service.ai;

import com.college.resourcefinder.config.AiProperties;
import com.college.resourcefinder.dto.ai.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.*;

/**
 * AI Service Client communicating over HTTP REST with the local Python FastAPI microservice.
 */
@Service
public class FastApiAiServiceClient implements AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(FastApiAiServiceClient.class);

    private final AiProperties aiProperties;
    private final RestClient restClient;

    public FastApiAiServiceClient(AiProperties aiProperties) {
        this.aiProperties = aiProperties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(aiProperties.getTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(aiProperties.getTimeoutMs()));

        this.restClient = RestClient.builder()
                .baseUrl(aiProperties.getServiceUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public boolean isAvailable() {
        try {
            Map<?, ?> health = restClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(Map.class);
            return health != null && "UP".equalsIgnoreCase(String.valueOf(health.get("status")));
        } catch (Exception ex) {
            log.debug("FastAPI AI microservice is currently unreachable: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public AiQueryParseResponse parseQuery(String query, String userDeptCode) {
        Map<String, String> body = new HashMap<>();
        body.put("query", query);
        if (userDeptCode != null) body.put("department", userDeptCode);

        Map<String, Object> resp = restClient.post()
                .uri("/api/v1/ai/parse-query")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (resp == null || !Boolean.TRUE.equals(resp.get("success"))) {
            throw new RuntimeException("AI service failed to parse query");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.get("data");

        AiQueryParseResponse parsed = new AiQueryParseResponse();
        parsed.setOriginalQuery((String) data.get("original_query"));
        parsed.setNormalizedKeywords((String) data.get("normalized_keywords"));
        parsed.setSubject((String) data.get("subject"));
        if (data.get("semester") != null) parsed.setSemester(((Number) data.get("semester")).intValue());
        if (data.get("unit") != null) parsed.setUnit(((Number) data.get("unit")).intValue());
        parsed.setCategory((String) data.get("category"));
        parsed.setDepartment((String) data.get("department"));
        parsed.setDepartmentScoped(Boolean.TRUE.equals(data.get("department_scoped")));
        if (data.get("confidence") != null) parsed.setConfidence(((Number) data.get("confidence")).doubleValue());
        parsed.setExplanation((String) data.get("explanation"));
        return parsed;
    }

    @Override
    public AiSummarizeResponse summarize(String filePath, int maxSentences, String provider) {
        Map<String, Object> body = new HashMap<>();
        body.put("file_path", filePath);
        body.put("max_sentences", maxSentences);
        body.put("provider", provider != null ? provider : "local_extractive");

        Map<String, Object> resp = restClient.post()
                .uri("/api/v1/ai/summarize")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (resp == null || !Boolean.TRUE.equals(resp.get("success"))) {
            throw new RuntimeException("AI service document summarization failed");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.get("data");

        AiSummarizeResponse summaryResp = new AiSummarizeResponse();
        summaryResp.setSummary((String) data.get("summary"));
        if (data.get("word_count") != null) summaryResp.setWordCount(((Number) data.get("word_count")).intValue());
        if (data.get("original_word_count") != null) summaryResp.setOriginalWordCount(((Number) data.get("original_word_count")).intValue());
        if (data.get("reduction_ratio") != null) summaryResp.setReductionRatio(((Number) data.get("reduction_ratio")).doubleValue());
        summaryResp.setFileHash((String) data.get("file_hash"));
        summaryResp.setMethod((String) data.get("method"));
        return summaryResp;
    }

    @Override
    public List<AiSemanticCandidate> semanticSearch(String query, List<AiSemanticCandidate> candidates) {
        List<Map<String, Object>> candidateList = new ArrayList<>();
        for (AiSemanticCandidate c : candidates) {
            Map<String, Object> map = new HashMap<>();
            map.put("resource_id", c.getResourceId());
            map.put("title", c.getTitle());
            map.put("description", c.getDescription());
            map.put("summary", c.getSummary());
            map.put("subject_name", c.getSubjectName());
            map.put("category_name", c.getCategoryName());
            candidateList.add(map);
        }

        Map<String, Object> body = Map.of(
                "query", query,
                "candidates", candidateList
        );

        Map<String, Object> resp = restClient.post()
                .uri("/api/v1/ai/semantic-search")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (resp == null || !Boolean.TRUE.equals(resp.get("success"))) {
            throw new RuntimeException("AI service semantic search failed");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) resp.get("data");

        List<AiSemanticCandidate> ranked = new ArrayList<>();
        for (Map<String, Object> row : data) {
            AiSemanticCandidate c = new AiSemanticCandidate();
            c.setResourceId(((Number) row.get("resource_id")).longValue());
            c.setTitle((String) row.get("title"));
            c.setDescription((String) row.get("description"));
            c.setSummary((String) row.get("summary"));
            c.setSubjectName((String) row.get("subject_name"));
            c.setCategoryName((String) row.get("category_name"));
            if (row.get("semantic_similarity") != null) {
                c.setSemanticSimilarity(((Number) row.get("semantic_similarity")).doubleValue());
            }
            ranked.add(c);
        }
        return ranked;
    }

    @Override
    public AiChatResponse chat(String message, List<AiChatMessageDto> history, String userRole, String userDeptCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        body.put("conversation_history", history != null ? history : Collections.emptyList());
        body.put("user_role", userRole != null ? userRole : "STUDENT");
        if (userDeptCode != null) body.put("user_department", userDeptCode);

        Map<String, Object> resp = restClient.post()
                .uri("/api/v1/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (resp == null || !Boolean.TRUE.equals(resp.get("success"))) {
            throw new RuntimeException("AI service chat failed");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.get("data");

        AiChatResponse chatResp = new AiChatResponse();
        chatResp.setReply((String) data.get("reply"));
        chatResp.setActionType((String) data.get("action_type"));
        @SuppressWarnings("unchecked")
        Map<String, Object> actionData = (Map<String, Object>) data.get("action_data");
        chatResp.setActionData(actionData);
        return chatResp;
    }

    @Override
    public AiFileSaveParseResponse parseFileSave(String prompt) {
        Map<String, String> body = Map.of("prompt", prompt);

        Map<String, Object> resp = restClient.post()
                .uri("/api/v1/ai/parse-file-save")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (resp == null || !Boolean.TRUE.equals(resp.get("success"))) {
            throw new RuntimeException("AI service file-save parsing failed");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.get("data");

        AiFileSaveParseResponse saveResp = new AiFileSaveParseResponse();
        saveResp.setValid(Boolean.TRUE.equals(data.get("valid")));
        if (data.get("resource_id") != null) {
            saveResp.setResourceId(((Number) data.get("resource_id")).longValue());
        }
        saveResp.setDestinationCategory((String) data.get("destination_category"));
        saveResp.setSubfolderTag((String) data.get("subfolder_tag"));
        saveResp.setExplanation((String) data.get("explanation"));
        saveResp.setError((String) data.get("error"));
        return saveResp;
    }
}
