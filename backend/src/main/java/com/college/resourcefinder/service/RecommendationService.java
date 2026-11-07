package com.college.resourcefinder.service;

import com.college.resourcefinder.dto.recommendation.RecommendationResponse;
import com.college.resourcefinder.dto.resource.ResourceResponse;
import com.college.resourcefinder.entity.Resource;
import com.college.resourcefinder.exception.ResourceNotFoundException;
import com.college.resourcefinder.repository.ResourceRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Rule-based recommendation engine for academic resources.
 *
 * <p>Computes a deterministic relevance score for each candidate resource
 * based on documented, weighted metadata-similarity rules.  Every
 * recommendation is repeatable for identical inputs and uses only MySQL
 * records — no AI, no embeddings, no external services.</p>
 *
 * <h3>Scoring Weights (deterministic)</h3>
 * <table>
 *     <tr><th>Criterion</th><th>Weight</th></tr>
 *     <tr><td>Same Subject</td><td>+40</td></tr>
 *     <tr><td>Same Unit</td><td>+20</td></tr>
 *     <tr><td>Same Department</td><td>+15</td></tr>
 *     <tr><td>Same Category</td><td>+10</td></tr>
 *     <tr><td>Same Semester</td><td>+10</td></tr>
 *     <tr><td>Same Resource Type</td><td>+5</td></tr>
 *     <tr><td>Shared Title Keyword</td><td>+5 each (max 25)</td></tr>
 * </table>
 *
 * <p>This service is designed so that a future AI module can enhance or
 * replace the scoring internals without changing the public API contract.</p>
 */
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    // ---- Documented scoring weights ----
    static final int WEIGHT_SAME_SUBJECT       = 40;
    static final int WEIGHT_SAME_UNIT           = 20;
    static final int WEIGHT_SAME_DEPARTMENT     = 15;
    static final int WEIGHT_SAME_CATEGORY       = 10;
    static final int WEIGHT_SAME_SEMESTER       = 10;
    static final int WEIGHT_SAME_RESOURCE_TYPE  = 5;
    static final int WEIGHT_SHARED_KEYWORD      = 5;
    static final int MAX_KEYWORD_SCORE          = 25;  // cap: max 5 keywords counted

    /** Maximum number of candidates fetched from the database for in-memory scoring. */
    private static final int MAX_CANDIDATES = 50;

    /** Default number of recommendations returned if the caller does not specify a limit. */
    static final int DEFAULT_LIMIT = 10;

    /** Hard ceiling on the number of recommendations that can be requested. */
    static final int MAX_LIMIT = 20;

    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "and", "or", "of", "for", "in", "on", "to",
            "with", "by", "at", "from", "is", "are", "was", "were", "be",
            "been", "being", "have", "has", "had", "do", "does", "did",
            "will", "would", "shall", "should", "may", "might", "can",
            "could", "must", "not", "no", "but", "if", "then", "else",
            "when", "where", "which", "who", "whom", "how", "what", "why",
            "this", "that", "these", "those", "it", "its"
    );

    private final ResourceRepository resourceRepository;

    public RecommendationService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    /**
     * Returns up to {@code limit} related-resource recommendations for
     * the given resource, ranked by deterministic relevance score.
     *
     * <p>Only active resources with status {@code ACTIVE} are returned.
     * The source resource itself is always excluded.</p>
     *
     * @param resourceId the ID of the source resource
     * @param limit      maximum number of recommendations (clamped to {@link #MAX_LIMIT})
     * @return a list of scored recommendations, possibly empty
     * @throws ResourceNotFoundException if the source resource does not exist
     */
    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendations(Long resourceId, int limit) {
        limit = Math.max(1, Math.min(limit, MAX_LIMIT));

        // 1. Load source resource
        Resource source = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resource not found with ID: " + resourceId));

        // 2. Extract source dimensions for scoring
        Long sourceSubjectId  = safeSubjectId(source);
        Long sourceDeptId     = safeDepartmentId(source);
        Long sourceCategoryId = source.getCategory() != null
                ? source.getCategory().getCategoryId() : null;
        Integer sourceSemester = source.getSubject() != null
                ? source.getSubject().getSemester() : null;
        Long sourceUnitId     = source.getUnit() != null
                ? source.getUnit().getUnitId() : null;
        String sourceType     = source.getResourceType();
        Set<String> sourceKeywords = extractKeywords(source.getTitle());

        // 3. Build candidate specification: active + not self + shares ≥1 dimension
        Specification<Resource> spec = buildCandidateSpec(
                resourceId, sourceSubjectId, sourceDeptId, sourceCategoryId, sourceSemester);

        // 4. Fetch bounded candidates
        List<Resource> candidates = resourceRepository.findAll(spec,
                PageRequest.of(0, MAX_CANDIDATES,
                        Sort.by(Sort.Direction.DESC, "uploadDate")))
                .getContent();

        if (candidates.isEmpty()) {
            log.debug("No recommendation candidates found for resource #{}", resourceId);
            return Collections.emptyList();
        }

        // 5. Score, rank and return the top-N
        List<RecommendationResponse> ranked = candidates.stream()
                .map(candidate -> scoreCandidate(candidate,
                        sourceSubjectId, sourceDeptId, sourceCategoryId,
                        sourceSemester, sourceUnitId, sourceType, sourceKeywords))
                .filter(rec -> rec.getRelevanceScore() > 0)
                .sorted(Comparator
                        .comparingInt(RecommendationResponse::getRelevanceScore).reversed()
                        .thenComparing(rec -> rec.getResource().getUploadDate(),
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .collect(Collectors.toList());

        log.debug("Generated {} recommendations for resource #{} from {} candidates",
                ranked.size(), resourceId, candidates.size());

        return ranked;
    }

    // ------------------------------------------------------------------ //
    //  Candidate Query
    // ------------------------------------------------------------------ //

    /**
     * Builds a JPA Specification that finds active resources sharing at least
     * one taxonomic dimension with the source resource.  This pre-filters
     * at the database level so that only plausible candidates are loaded
     * into memory for detailed scoring.
     */
    private Specification<Resource> buildCandidateSpec(
            Long excludeId, Long subjectId, Long deptId,
            Long categoryId, Integer semester) {

        return (root, query, cb) -> {
            List<Predicate> must = new ArrayList<>();
            List<Predicate> dimensions = new ArrayList<>();

            // Must be active
            must.add(cb.isTrue(root.get("isActive")));
            must.add(cb.equal(root.get("status"), "ACTIVE"));

            // Exclude the source resource
            must.add(cb.notEqual(root.get("resourceId"), excludeId));

            // At least one shared dimension (OR)
            if (subjectId != null) {
                dimensions.add(cb.equal(
                        root.get("subject").get("subjectId"), subjectId));
            }
            if (deptId != null) {
                dimensions.add(cb.equal(
                        root.get("subject").get("department").get("departmentId"), deptId));
            }
            if (categoryId != null) {
                dimensions.add(cb.equal(
                        root.get("category").get("categoryId"), categoryId));
            }
            if (semester != null) {
                dimensions.add(cb.equal(
                        root.get("subject").get("semester"), semester));
            }

            // If there are no dimensions at all, no candidates can match
            if (dimensions.isEmpty()) {
                must.add(cb.disjunction());  // always false
            } else {
                must.add(cb.or(dimensions.toArray(new Predicate[0])));
            }

            return cb.and(must.toArray(new Predicate[0]));
        };
    }

    // ------------------------------------------------------------------ //
    //  Scoring
    // ------------------------------------------------------------------ //

    /**
     * Computes a weighted relevance score for a single candidate resource
     * against the source resource's extracted dimensions.
     */
    private RecommendationResponse scoreCandidate(
            Resource candidate,
            Long sourceSubjectId, Long sourceDeptId,
            Long sourceCategoryId, Integer sourceSemester,
            Long sourceUnitId, String sourceResourceType,
            Set<String> sourceKeywords) {

        int score = 0;
        List<String> matched = new ArrayList<>();

        // Same Subject (+40)
        if (sourceSubjectId != null && sourceSubjectId.equals(safeSubjectId(candidate))) {
            score += WEIGHT_SAME_SUBJECT;
            matched.add("Same subject");
        }

        // Same Unit (+20)
        if (sourceUnitId != null && candidate.getUnit() != null
                && sourceUnitId.equals(candidate.getUnit().getUnitId())) {
            score += WEIGHT_SAME_UNIT;
            matched.add("Same unit");
        }

        // Same Department (+15)
        if (sourceDeptId != null && sourceDeptId.equals(safeDepartmentId(candidate))) {
            score += WEIGHT_SAME_DEPARTMENT;
            matched.add("Same department");
        }

        // Same Category (+10)
        if (sourceCategoryId != null && candidate.getCategory() != null
                && sourceCategoryId.equals(candidate.getCategory().getCategoryId())) {
            score += WEIGHT_SAME_CATEGORY;
            matched.add("Same category");
        }

        // Same Semester (+10)
        if (sourceSemester != null && candidate.getSubject() != null
                && sourceSemester.equals(candidate.getSubject().getSemester())) {
            score += WEIGHT_SAME_SEMESTER;
            matched.add("Same semester");
        }

        // Same Resource Type (+5)
        if (sourceResourceType != null
                && sourceResourceType.equalsIgnoreCase(candidate.getResourceType())) {
            score += WEIGHT_SAME_RESOURCE_TYPE;
            matched.add("Same resource type");
        }

        // Shared Title Keywords (+5 each, max 25)
        Set<String> candidateKeywords = extractKeywords(candidate.getTitle());
        int keywordHits = 0;
        for (String kw : sourceKeywords) {
            if (candidateKeywords.contains(kw)) {
                keywordHits++;
            }
        }
        int keywordScore = Math.min(keywordHits * WEIGHT_SHARED_KEYWORD, MAX_KEYWORD_SCORE);
        if (keywordScore > 0) {
            score += keywordScore;
            matched.add("Shared keywords (" + keywordHits + ")");
        }

        return new RecommendationResponse(
                ResourceResponse.fromEntity(candidate), score, matched);
    }

    // ------------------------------------------------------------------ //
    //  Helpers
    // ------------------------------------------------------------------ //

    /**
     * Extracts non-stopword, lowercase keyword tokens from a title.
     * Tokens shorter than 2 characters are discarded.
     */
    Set<String> extractKeywords(String title) {
        if (title == null || title.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(title.toLowerCase()
                        .split("[\\s\\-_.,;:!?()\\[\\]{}\"']+"))
                .filter(t -> t.length() >= 2)
                .filter(t -> !STOPWORDS.contains(t))
                .collect(Collectors.toSet());
    }

    private Long safeSubjectId(Resource r) {
        return r.getSubject() != null ? r.getSubject().getSubjectId() : null;
    }

    private Long safeDepartmentId(Resource r) {
        return (r.getSubject() != null && r.getSubject().getDepartment() != null)
                ? r.getSubject().getDepartment().getDepartmentId() : null;
    }
}
