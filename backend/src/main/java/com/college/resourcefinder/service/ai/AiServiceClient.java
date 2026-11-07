package com.college.resourcefinder.service.ai;

import com.college.resourcefinder.dto.ai.*;
import java.util.List;

/**
 * Replaceable AI service interface decoupling the Spring Boot application from specific AI providers.
 */
public interface AiServiceClient {

    AiQueryParseResponse parseQuery(String query, String userDeptCode);

    AiSummarizeResponse summarize(String filePath, int maxSentences, String provider);

    List<AiSemanticCandidate> semanticSearch(String query, List<AiSemanticCandidate> candidates);

    AiChatResponse chat(String message, List<AiChatMessageDto> history, String userRole, String userDeptCode);

    AiFileSaveParseResponse parseFileSave(String prompt);

    boolean isAvailable();
}
