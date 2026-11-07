package com.college.resourcefinder.controller;

import com.college.resourcefinder.dto.response.ApiResponse;
import com.college.resourcefinder.dto.search.*;
import com.college.resourcefinder.entity.User;
import com.college.resourcefinder.service.AuthService;
import com.college.resourcefinder.service.SearchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for database-backed academic resource search and user search history.
 * Base path: {@code /api/v1/search}
 */
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;
    private final AuthService authService;

    public SearchController(SearchService searchService, AuthService authService) {
        this.searchService = searchService;
        this.authService = authService;
    }

    /**
     * Executes deterministic resource search with multi-faceted filtering.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<SearchResultResponse>> searchResources(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false, defaultValue = "relevance") String sortBy,
            @PageableDefault(size = 12) Pageable pageable) {

        String effectiveQuery = (query != null && !query.isBlank()) ? query : q;

        SearchQueryDto dto = new SearchQueryDto();
        dto.setQuery(effectiveQuery);
        dto.setDepartmentId(departmentId);
        dto.setSubjectId(subjectId);
        dto.setSemester(semester);
        dto.setUnitId(unitId);
        dto.setCategoryId(categoryId);
        dto.setResourceType(resourceType);
        dto.setFileType(fileType);
        dto.setSortBy(sortBy);

        User currentUser = null;
        try {
            currentUser = authService.getCurrentAuthenticatedUser();
        } catch (Exception ignored) {}

        SearchResultResponse response = searchService.search(dto, currentUser, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Retrieves recent search history for the authenticated user.
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<SearchHistoryResponse>>> getSearchHistory(
            @PageableDefault(size = 10, sort = "searchDate", direction = Sort.Direction.DESC) Pageable pageable) {

        User currentUser = authService.getCurrentAuthenticatedUser();
        Page<SearchHistoryResponse> historyPage = searchService.getUserSearchHistory(currentUser, pageable);
        return ResponseEntity.ok(ApiResponse.success(historyPage));
    }

    /**
     * Deletes a specific search history entry owned by the current user.
     */
    @DeleteMapping("/history/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHistoryItem(@PathVariable Long id) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        searchService.deleteSearchHistoryItem(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Search history entry deleted.", null));
    }

    /**
     * Clears all search history for the authenticated user.
     */
    @DeleteMapping("/history")
    public ResponseEntity<ApiResponse<Void>> clearHistory() {
        User currentUser = authService.getCurrentAuthenticatedUser();
        searchService.clearUserSearchHistory(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Search history cleared.", null));
    }
}
