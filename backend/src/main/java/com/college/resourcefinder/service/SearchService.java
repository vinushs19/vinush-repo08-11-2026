package com.college.resourcefinder.service;

import com.college.resourcefinder.dto.resource.ResourceResponse;
import com.college.resourcefinder.dto.search.*;
import com.college.resourcefinder.entity.*;
import com.college.resourcefinder.exception.ResourceNotFoundException;
import com.college.resourcefinder.repository.*;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service managing deterministic academic resource search, keyword normalization,
 * multi-faceted filtering, relevance ranking, and user search history tracking.
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private static final Set<String> STOPWORDS = Set.of(
            "find", "show", "get", "search", "give", "list", "display", "please",
            "for", "of", "the", "in", "a", "an", "on", "to", "me", "my", "with",
            "all", "about", "and", "or", "from"
    );

    private final ResourceRepository resourceRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final SubjectRepository subjectRepository;
    private final DepartmentRepository departmentRepository;
    private final CategoryRepository categoryRepository;

    public SearchService(ResourceRepository resourceRepository,
                         SearchHistoryRepository searchHistoryRepository,
                         SubjectRepository subjectRepository,
                         DepartmentRepository departmentRepository,
                         CategoryRepository categoryRepository) {
        this.resourceRepository = resourceRepository;
        this.searchHistoryRepository = searchHistoryRepository;
        this.subjectRepository = subjectRepository;
        this.departmentRepository = departmentRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Executes deterministic resource search with keyword normalization,
     * multi-faceted criteria merging, and search history recording.
     */
    @Transactional
    public SearchResultResponse search(SearchQueryDto queryDto, User currentUser, Pageable pageable) {
        String rawQuery = (queryDto != null && queryDto.getQuery() != null) ? queryDto.getQuery().trim() : "";

        DetectedCriteriaDto detected = normalizeAndExtractCriteria(rawQuery, currentUser);

        // Merge explicit criteria with detected criteria (explicit takes precedence)
        Long targetDeptId = (queryDto != null && queryDto.getDepartmentId() != null) ?
                queryDto.getDepartmentId() :
                (detected.isDepartmentScopeApplied() && currentUser != null && currentUser.getDepartment() != null ?
                        currentUser.getDepartment().getDepartmentId() : null);

        Integer targetSemester = (queryDto != null && queryDto.getSemester() != null) ?
                queryDto.getSemester() : detected.getExtractedSemester();

        Long targetSubjectId = (queryDto != null && queryDto.getSubjectId() != null) ?
                queryDto.getSubjectId() : null;

        Long targetUnitId = (queryDto != null && queryDto.getUnitId() != null) ?
                queryDto.getUnitId() : null;

        Integer targetUnitNumber = (targetUnitId == null) ? detected.getExtractedUnitNumber() : null;

        Long targetCategoryId = (queryDto != null && queryDto.getCategoryId() != null) ?
                queryDto.getCategoryId() : null;

        String targetResourceType = (queryDto != null && queryDto.getResourceType() != null && !queryDto.getResourceType().isBlank()) ?
                queryDto.getResourceType() : detected.getExtractedResourceType();

        String targetFileType = (queryDto != null && queryDto.getFileType() != null && !queryDto.getFileType().isBlank()) ?
                queryDto.getFileType().trim().toLowerCase() : null;

        String residualKeywords = detected.getNormalizedKeywords();
        String subjectToken = detected.getExtractedSubjectToken();

        // Build JPA Specification
        Specification<Resource> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always enforce active status
            predicates.add(cb.isTrue(root.get("isActive")));
            predicates.add(cb.equal(root.get("status"), "ACTIVE"));

            if (targetDeptId != null) {
                predicates.add(cb.equal(root.get("subject").get("department").get("departmentId"), targetDeptId));
            }

            if (targetSemester != null) {
                predicates.add(cb.equal(root.get("subject").get("semester"), targetSemester));
            }

            if (targetSubjectId != null) {
                predicates.add(cb.equal(root.get("subject").get("subjectId"), targetSubjectId));
            } else if (subjectToken != null && !subjectToken.isBlank()) {
                String subPattern = "%" + subjectToken.toLowerCase() + "%";
                Predicate subjCodeMatch = cb.like(cb.lower(root.get("subject").get("subjectCode")), subPattern);
                Predicate subjNameMatch = cb.like(cb.lower(root.get("subject").get("subjectName")), subPattern);
                predicates.add(cb.or(subjCodeMatch, subjNameMatch));
            }

            if (targetUnitId != null) {
                predicates.add(cb.equal(root.get("unit").get("unitId"), targetUnitId));
            } else if (targetUnitNumber != null) {
                predicates.add(cb.equal(root.get("unit").get("unitNumber"), targetUnitNumber));
            }

            if (targetCategoryId != null) {
                predicates.add(cb.equal(root.get("category").get("categoryId"), targetCategoryId));
            } else if (detected.getExtractedCategory() != null) {
                predicates.add(cb.like(cb.lower(root.get("category").get("categoryName")), "%" + detected.getExtractedCategory().toLowerCase() + "%"));
            }

            if (targetResourceType != null && !targetResourceType.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("resourceType")), "%" + targetResourceType.toLowerCase() + "%"));
            }

            if (targetFileType != null && !targetFileType.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("fileType")), targetFileType));
            }

            if (residualKeywords != null && !residualKeywords.isBlank()) {
                List<Predicate> keywordPredicates = new ArrayList<>();
                for (String term : residualKeywords.split("\\s+")) {
                    if (term.length() < 2) continue;
                    String pattern = "%" + term.toLowerCase() + "%";
                    Predicate inTitle = cb.like(cb.lower(root.get("title")), pattern);
                    Predicate inDesc = cb.like(cb.lower(root.get("description")), pattern);
                    Predicate inSubjName = cb.like(cb.lower(root.get("subject").get("subjectName")), pattern);
                    Predicate inSubjCode = cb.like(cb.lower(root.get("subject").get("subjectCode")), pattern);
                    keywordPredicates.add(cb.or(inTitle, inDesc, inSubjName, inSubjCode));
                }
                if (!keywordPredicates.isEmpty()) {
                    predicates.add(cb.and(keywordPredicates.toArray(new Predicate[0])));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Determine sorting
        Sort sort = resolveSort(queryDto != null ? queryDto.getSortBy() : "relevance");
        Pageable resolvedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<Resource> entityPage = resourceRepository.findAll(spec, resolvedPageable);
        Page<ResourceResponse> responsePage = entityPage.map(ResourceResponse::fromEntity);

        // Record search history if user is authenticated and query is non-empty
        if (!rawQuery.isBlank() && currentUser != null) {
            recordSearchHistory(currentUser, rawQuery, (int) entityPage.getTotalElements());
        }

        return new SearchResultResponse(rawQuery, detected, responsePage);
    }

    /**
     * Normalizes natural language queries and deterministically extracts semantic criteria.
     */
    public DetectedCriteriaDto normalizeAndExtractCriteria(String rawQuery, User currentUser) {
        DetectedCriteriaDto dto = new DetectedCriteriaDto();
        dto.setOriginalQuery(rawQuery);

        if (rawQuery == null || rawQuery.isBlank()) {
            dto.setNormalizedKeywords("");
            return dto;
        }

        String working = rawQuery.toLowerCase();

        // 1. Semester extraction: "semester 2", "sem 2", "s2"
        Matcher semMatcher = Pattern.compile("\\b(?:semester|sem|s)[\\s\\-_]*([1-8])\\b").matcher(working);
        if (semMatcher.find()) {
            dto.setExtractedSemester(Integer.parseInt(semMatcher.group(1)));
            working = semMatcher.replaceFirst(" ");
        }

        // 2. Unit extraction: "unit 3", "u3"
        Matcher unitMatcher = Pattern.compile("\\b(?:unit|u)[\\s\\-_]*([1-9]|10)\\b").matcher(working);
        if (unitMatcher.find()) {
            dto.setExtractedUnitNumber(Integer.parseInt(unitMatcher.group(1)));
            working = unitMatcher.replaceFirst(" ");
        }

        // 3. Category & Resource Type extraction
        if (working.matches(".*\\b(notes|lecture notes|slides|ppt|presentation)\\b.*")) {
            dto.setExtractedCategory("Lecture Notes");
            dto.setExtractedResourceType("Lecture notes");
            working = working.replaceAll("\\b(lecture notes|notes|slides|ppt|presentation)\\b", " ");
        } else if (working.matches(".*\\b(question papers?|qp|pyq|exams?|model papers?|question bank)\\b.*")) {
            dto.setExtractedCategory("Question Papers");
            dto.setExtractedResourceType("Previous examination papers");
            working = working.replaceAll("\\b(question papers?|qp|pyq|exams?|model papers?|question bank)\\b", " ");
        } else if (working.matches(".*\\b(assignments?|homework|lab exercises?)\\b.*")) {
            dto.setExtractedCategory("Assignments");
            dto.setExtractedResourceType("Assignments");
            working = working.replaceAll("\\b(assignments?|homework|lab exercises?)\\b", " ");
        } else if (working.matches(".*\\b(study materials?|handbook|guide)\\b.*")) {
            dto.setExtractedCategory("Study Material");
            dto.setExtractedResourceType("Study materials");
            working = working.replaceAll("\\b(study materials?|handbook|guide)\\b", " ");
        } else if (working.matches(".*\\b(books?|e-?books?|textbooks?)\\b.*")) {
            dto.setExtractedCategory("E-Books");
            dto.setExtractedResourceType("E-books and reference documents");
            working = working.replaceAll("\\b(books?|e-?books?|textbooks?)\\b", " ");
        }

        // 4. Department scope extraction: "my department"
        if (working.contains("my department")) {
            dto.setDepartmentScopeApplied(true);
            working = working.replace("my department", " ");
        }

        // 5. Common subject token detection: "dbms", "database", "java", "os", "operating system"
        if (working.matches(".*\\b(dbms|database|sql)\\b.*")) {
            dto.setExtractedSubjectToken("DBMS");
            working = working.replaceAll("\\b(dbms|database|sql)\\b", " ");
        } else if (working.matches(".*\\b(java|advanced java)\\b.*")) {
            dto.setExtractedSubjectToken("Java");
            working = working.replaceAll("\\b(java|advanced java)\\b", " ");
        } else if (working.matches(".*\\b(os|operating systems?)\\b.*")) {
            dto.setExtractedSubjectToken("Operating System");
            working = working.replaceAll("\\b(operating systems?|os)\\b", " ");
        } else if (working.matches(".*\\b(dsa|data structures?)\\b.*")) {
            dto.setExtractedSubjectToken("Data Structures");
            working = working.replaceAll("\\b(data structures?|dsa)\\b", " ");
        }

        // 6. Stopwords cleanup for residual keywords
        String[] words = working.trim().split("\\s+");
        List<String> meaningfulTokens = new ArrayList<>();
        for (String w : words) {
            String clean = w.replaceAll("[^a-zA-Z0-9]", "");
            if (clean.length() >= 2 && !STOPWORDS.contains(clean)) {
                meaningfulTokens.add(clean);
            }
        }

        dto.setExtractedTokens(meaningfulTokens);
        dto.setNormalizedKeywords(String.join(" ", meaningfulTokens));

        return dto;
    }

    private Sort resolveSort(String sortBy) {
        if (sortBy == null) return Sort.by(Sort.Direction.DESC, "uploadDate");
        return switch (sortBy.toLowerCase()) {
            case "date_asc" -> Sort.by(Sort.Direction.ASC, "uploadDate");
            case "title_asc" -> Sort.by(Sort.Direction.ASC, "title");
            case "title_desc" -> Sort.by(Sort.Direction.DESC, "title");
            case "size_desc" -> Sort.by(Sort.Direction.DESC, "fileSize");
            case "relevance", "date_desc" -> Sort.by(Sort.Direction.DESC, "uploadDate");
            default -> Sort.by(Sort.Direction.DESC, "uploadDate");
        };
    }

    // =========================================================================
    // SEARCH HISTORY MANAGEMENT
    // =========================================================================

    /**
     * Records a search query in the authenticated user's private history.
     */
    @Transactional
    public void recordSearchHistory(User user, String query, int resultCount) {
        if (user == null || query == null || query.isBlank()) return;

        try {
            String sanitized = query.trim();
            if (sanitized.length() > 500) {
                sanitized = sanitized.substring(0, 500);
            }

            SearchHistory history = new SearchHistory();
            history.setUser(user);
            history.setSearchQuery(sanitized);
            history.setResultCount(resultCount);

            searchHistoryRepository.save(history);
        } catch (Exception e) {
            log.warn("Failed to record search history: {}", e.getMessage());
        }
    }

    /**
     * Retrieves recent search history for the authenticated user with pagination.
     */
    @Transactional(readOnly = true)
    public Page<SearchHistoryResponse> getUserSearchHistory(User currentUser, Pageable pageable) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication required to view search history.");
        }
        return searchHistoryRepository.findByUser_UserId(currentUser.getUserId(), pageable)
                .map(SearchHistoryResponse::fromEntity);
    }

    /**
     * Deletes a specific search history item owned by the current user.
     */
    @Transactional
    public void deleteSearchHistoryItem(Long searchId, User currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication required.");
        }

        SearchHistory history = searchHistoryRepository.findBySearchIdAndUser_UserId(searchId, currentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Search history entry not found with ID: " + searchId));

        searchHistoryRepository.delete(history);
    }

    /**
     * Clears all search history for the authenticated user.
     */
    @Transactional
    public void clearUserSearchHistory(User currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication required.");
        }
        searchHistoryRepository.deleteByUser_UserId(currentUser.getUserId());
    }
}
