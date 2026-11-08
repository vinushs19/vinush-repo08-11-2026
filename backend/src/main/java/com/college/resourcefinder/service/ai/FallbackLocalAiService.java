package com.college.resourcefinder.service.ai;

import com.college.resourcefinder.dto.ai.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic in-JVM AI/NLP fallback implementation.
 * Ensures the platform operates reliably even if external AI providers or Python microservices are unavailable.
 */
@Service
public class FallbackLocalAiService implements AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(FallbackLocalAiService.class);

    private static final Set<String> STOPWORDS = Set.of(
            "find", "show", "get", "search", "give", "list", "display", "please",
            "for", "of", "the", "in", "a", "an", "on", "to", "me", "my", "with",
            "all", "about", "and", "or", "from", "can", "you", "need", "i", "want"
    );

    private static final Map<String, String> SUBJECT_ALIASES = Map.ofEntries(
            Map.entry("dbms", "DBMS"),
            Map.entry("database", "DBMS"),
            Map.entry("java", "Java"),
            Map.entry("os", "Operating Systems"),
            Map.entry("operating systems", "Operating Systems"),
            Map.entry("operating system", "Operating Systems"),
            Map.entry("cn", "Computer Networks"),
            Map.entry("network", "Computer Networks"),
            Map.entry("ds", "Data Structures"),
            Map.entry("python", "Python"),
            Map.entry("cloud", "Cloud Computing")
    );

    private static final Map<String, String> CATEGORY_ALIASES = Map.ofEntries(
            Map.entry("notes", "Notes"),
            Map.entry("lecture notes", "Notes"),
            Map.entry("question paper", "Question Papers"),
            Map.entry("question papers", "Question Papers"),
            Map.entry("questions", "Question Papers"),
            Map.entry("exam paper", "Question Papers"),
            Map.entry("assignments", "Assignments"),
            Map.entry("assignment", "Assignments"),
            Map.entry("homework", "Assignments"),
            Map.entry("study materials", "Notes"),
            Map.entry("study material", "Notes"),
            Map.entry("material", "Notes"),
            Map.entry("materials", "Notes"),
            Map.entry("lab manual", "Lab Manuals"),
            Map.entry("lab", "Lab Manuals"),
            Map.entry("book", "E-Books"),
            Map.entry("books", "E-Books"),
            Map.entry("ebook", "E-Books"),
            Map.entry("syllabus", "Syllabus")
    );

    private static final List<String> PROMPT_INJECTION_PATTERNS = List.of(
            "ignore\\s+(?:all\\s+)?(?:previous|prior|above)\\s+instructions",
            "reveal\\s+(?:system\\s+)?(?:prompt|configuration|keys|secrets|passwords)",
            "bypass\\s+(?:authorization|permissions|security|rbac)",
            "grant\\s+(?:me\\s+)?(?:admin|root|faculty)",
            "(?:drop\\s+table|insert\\s+into|delete\\s+from|update\\s+users)",
            "(?:eval|exec|spawn|os\\.system|subprocess)",
            "system\\s*\\(",
            "(?:cat\\s+/etc/passwd|type\\s+c:\\\\|cmd\\.exe|powershell)"
    );

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public AiQueryParseResponse parseQuery(String query, String userDeptCode) {
        AiQueryParseResponse resp = new AiQueryParseResponse();
        resp.setOriginalQuery(query != null ? query : "");

        if (query == null || query.isBlank()) {
            resp.setNormalizedKeywords("");
            resp.setConfidence(0.0);
            resp.setExplanation("Empty query string.");
            return resp;
        }

        String working = query.toLowerCase();
        List<String> detected = new ArrayList<>();

        // 1. Department
        if (working.contains("my department") || working.contains("my dept")) {
            resp.setDepartmentScoped(true);
            resp.setDepartment(userDeptCode);
            working = working.replaceAll("\\bmy\\s+(?:department|dept)\\b", " ");
            detected.add("Department: current user's department");
        } else if (working.contains("mca")) {
            resp.setDepartment("MCA");
            working = working.replaceAll("\\bmca\\b", " ");
            detected.add("Department: MCA");
        } else if (working.contains("cs")) {
            resp.setDepartment("CS");
            working = working.replaceAll("\\bcs\\b", " ");
            detected.add("Department: CS");
        }

        // 2. Semester
        Matcher semMatcher = Pattern.compile("\\b(?:semester|sem|s)[\\s\\-_]*([1-8])\\b").matcher(working);
        if (semMatcher.find()) {
            resp.setSemester(Integer.parseInt(semMatcher.group(1)));
            detected.add("Semester " + resp.getSemester());
            working = semMatcher.replaceFirst(" ");
        }

        // 3. Unit
        Matcher unitMatcher = Pattern.compile("\\b(?:unit|module|u)[\\s\\-_]*([1-9]|10)\\b").matcher(working);
        if (unitMatcher.find()) {
            resp.setUnit(Integer.parseInt(unitMatcher.group(1)));
            detected.add("Unit " + resp.getUnit());
            working = unitMatcher.replaceFirst(" ");
        }

        // 4. Category
        for (Map.Entry<String, String> entry : CATEGORY_ALIASES.entrySet()) {
            if (working.contains(entry.getKey())) {
                resp.setCategory(entry.getValue());
                detected.add("Category: " + entry.getValue());
                working = working.replace(entry.getKey(), " ");
                break;
            }
        }

        // 5. Subject
        for (Map.Entry<String, String> entry : SUBJECT_ALIASES.entrySet()) {
            if (working.contains(entry.getKey())) {
                resp.setSubject(entry.getValue());
                detected.add("Subject: " + entry.getValue());
                working = working.replace(entry.getKey(), " ");
                break;
            }
        }

        // 6. Keywords
        String[] tokens = working.split("[\\s,;:.?!]+");
        List<String> keywords = new ArrayList<>();
        for (String t : tokens) {
            if (t.length() > 1 && !STOPWORDS.contains(t)) {
                keywords.add(t);
            }
        }
        resp.setNormalizedKeywords(String.join(" ", keywords).trim());

        double conf = (resp.getSubject() != null ? 0.35 : 0.0) +
                      (resp.getUnit() != null ? 0.20 : 0.0) +
                      (resp.getSemester() != null ? 0.20 : 0.0) +
                      (resp.getCategory() != null ? 0.15 : 0.0) +
                      (resp.getDepartment() != null ? 0.10 : 0.0);
        if (detected.isEmpty() && !resp.getNormalizedKeywords().isEmpty()) {
            conf = 0.30;
        }
        resp.setConfidence(Math.min(1.0, conf));

        if (!detected.isEmpty()) {
            resp.setExplanation("Extracted criteria: " + String.join(", ", detected) +
                    (resp.getNormalizedKeywords().isEmpty() ? "" : " (Keywords: '" + resp.getNormalizedKeywords() + "')"));
        } else {
            resp.setExplanation("Keyword search: '" + resp.getNormalizedKeywords() + "'");
        }

        return resp;
    }

    @Override
    public AiSummarizeResponse summarize(String filePath, int maxSentences, String provider) {
        AiSummarizeResponse resp = new AiSummarizeResponse();
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("Physical document not found on server: " + file.getName());
        }

        String fileHash = computeSha256(file);
        resp.setFileHash(fileHash);
        resp.setGeneratedAt(LocalDateTime.now());

        String name = file.getName().toLowerCase();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf(".")) : "";

        if (!Set.of(".txt", ".pdf", ".docx").contains(ext)) {
            resp.setSummary("Text summarization is not supported for " + (ext.isEmpty() ? "this" : ext.toUpperCase()) + " format. Supported formats: PDF, DOCX, TXT.");
            resp.setMethod("unsupported_format");
            resp.setWordCount(0);
            resp.setOriginalWordCount(0);
            resp.setReductionRatio(0.0);
            return resp;
        }

        // For binary document formats (.pdf, .docx), do not read raw bytes as UTF-8
        if (".pdf".equals(ext) || ".docx".equals(ext)) {
            resp.setSummary("Text extraction for " + ext.toUpperCase() + " documents requires the active AI text extraction microservice.");
            resp.setMethod("fallback_unavailable");
            resp.setWordCount(0);
            resp.setOriginalWordCount(0);
            resp.setReductionRatio(0.0);
            return resp;
        }

        resp.setMethod("local_java_extractive");
        String text = extractTextDefensively(file);
        int words = countWords(text);
        resp.setOriginalWordCount(words);

        if (words < 20) {
            resp.setSummary(text.isBlank() ? "Document contains minimal extractable text." : text);
            resp.setWordCount(words);
            resp.setReductionRatio(1.0);
            return resp;
        }

        String summary = generateExtractiveSummary(text, maxSentences > 0 ? maxSentences : 4);
        resp.setSummary(summary);
        int summaryWords = countWords(summary);
        resp.setWordCount(summaryWords);
        resp.setReductionRatio(Math.round((double) summaryWords / Math.max(words, 1) * 1000.0) / 1000.0);

        return resp;
    }

    @Override
    public List<AiSemanticCandidate> semanticSearch(String query, List<AiSemanticCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Double> queryVec = buildVector(tokenize(query));
        List<AiSemanticCandidate> scored = new ArrayList<>();

        for (AiSemanticCandidate c : candidates) {
            String corpus = (c.getTitle() != null ? c.getTitle() : "") + " " +
                            (c.getDescription() != null ? c.getDescription() : "") + " " +
                            (c.getSummary() != null ? c.getSummary() : "") + " " +
                            (c.getSubjectName() != null ? c.getSubjectName() : "") + " " +
                            (c.getCategoryName() != null ? c.getCategoryName() : "");

            Map<String, Double> docVec = buildVector(tokenize(corpus));
            double sim = computeCosine(queryVec, docVec);
            c.setSemanticSimilarity(Math.round(sim * 10000.0) / 10000.0);
            scored.add(c);
        }

        scored.sort((a, b) -> Double.compare(b.getSemanticSimilarity(), a.getSemanticSimilarity()));
        return scored;
    }

    @Override
    public AiChatResponse chat(String message, List<AiChatMessageDto> history, String userRole, String userDeptCode) {
        AiChatResponse resp = new AiChatResponse();
        if (message == null || message.isBlank()) {
            resp.setReply("Hello! I am your College Academic Assistant. How can I help with your course resources today?");
            resp.setActionType("NONE");
            return resp;
        }

        // Defend against prompt injection
        if (isPromptInjection(message)) {
            resp.setReply("I cannot fulfill this request. My operations are strictly restricted to academic resource discovery, document summarization, and authorized file management.");
            resp.setActionType("SECURITY_REJECTION");
            resp.setActionData(Map.of("flag", "PROMPT_INJECTION_DETECTED"));
            return resp;
        }

        String lower = message.trim().toLowerCase();

        // Check Summarize intent
        Matcher sumMatcher = Pattern.compile("\\b(?:summarize|summary|overview|tldr)\\b.*?(?:resource|id|#)?\\s*(\\d+)\\b").matcher(lower);
        if (sumMatcher.find()) {
            long resId = Long.parseLong(sumMatcher.group(1));
            resp.setReply("I can summarize resource #" + resId + " for you. Checking permissions and generating the summary...");
            resp.setActionType("SUMMARIZE_RESOURCE");
            resp.setActionData(Map.of("resourceId", resId));
            return resp;
        }

        // Check Organize File intent
        if (lower.contains("organize") || lower.contains("save file") || lower.contains("move file")) {
            AiFileSaveParseResponse fileSave = parseFileSave(message);
            if (fileSave.isValid()) {
                if ("STUDENT".equalsIgnoreCase(userRole)) {
                    resp.setReply("File organization workflows are restricted to faculty and administrators. As a student, you can search, preview, and download resources.");
                    resp.setActionType("ROLE_RESTRICTION");
                    return resp;
                }
                resp.setReply("Preparing to " + fileSave.getExplanation() + ". Validating server-side permissions...");
                resp.setActionType("ORGANIZE_FILE");
                resp.setActionData(Map.of(
                        "resourceId", fileSave.getResourceId(),
                        "destinationCategory", fileSave.getDestinationCategory(),
                        "subfolderTag", fileSave.getSubfolderTag() != null ? fileSave.getSubfolderTag() : ""
                ));
                return resp;
            }
        }

        // Check Filter explanation
        if (lower.contains("how do i filter") || lower.contains("how to search") || lower.contains("filter options")) {
            resp.setReply("You can filter resources by Department, Semester (1-8), Subject, Unit (1-10), and Category (Notes, Question Papers, Assignments, Study Materials). You can also type natural requests like: 'Find DBMS notes for Unit 3' or 'Show Java question papers for Semester 2'.");
            resp.setActionType("EXPLAIN_FILTERS");
            return resp;
        }

        // Resource search intent
        AiQueryParseResponse parsed = parseQuery(message, userDeptCode);
        if (parsed.getConfidence() >= 0.25 || lower.contains("find") || lower.contains("show") || lower.contains("notes") || lower.contains("paper")) {
            resp.setReply("Searching academic repository: " + parsed.getExplanation());
            resp.setActionType("SEARCH_RESOURCES");
            Map<String, Object> data = new HashMap<>();
            if (parsed.getSubject() != null) data.put("subject", parsed.getSubject());
            if (parsed.getSemester() != null) data.put("semester", parsed.getSemester());
            if (parsed.getUnit() != null) data.put("unit", parsed.getUnit());
            if (parsed.getCategory() != null) data.put("category", parsed.getCategory());
            if (parsed.getDepartment() != null) data.put("department", parsed.getDepartment());
            if (parsed.getNormalizedKeywords() != null) data.put("keywords", parsed.getNormalizedKeywords());
            resp.setActionData(data);
            return resp;
        }

        resp.setReply("I'm here to assist with finding academic notes, previous question papers, assignments, and study materials. Try asking: 'Find DBMS notes for Unit 3' or 'Summarize resource #1'.");
        resp.setActionType("GENERAL_HELP");
        return resp;
    }

    @Override
    public AiFileSaveParseResponse parseFileSave(String prompt) {
        AiFileSaveParseResponse resp = new AiFileSaveParseResponse();
        if (prompt == null || prompt.isBlank()) {
            resp.setValid(false);
            resp.setError("Empty file organization prompt.");
            return resp;
        }

        String lower = prompt.toLowerCase();

        // 1. Resource ID
        Matcher idMatcher = Pattern.compile("\\b(?:resource|res|id|item)[\\s#:]*(\\d+)\\b").matcher(lower);
        if (idMatcher.find()) {
            resp.setResourceId(Long.parseLong(idMatcher.group(1)));
        }

        // 2. Destination Category
        Map<String, String> cats = Map.of(
                "lecture notes", "LECTURE_NOTES",
                "notes", "LECTURE_NOTES",
                "question papers", "QUESTION_PAPERS",
                "question paper", "QUESTION_PAPERS",
                "assignments", "ASSIGNMENTS",
                "assignment", "ASSIGNMENTS",
                "lab manuals", "LAB_MANUALS",
                "study materials", "STUDY_MATERIALS",
                "syllabus", "SYLLABUS",
                "archives", "ARCHIVES"
        );
        for (Map.Entry<String, String> e : cats.entrySet()) {
            if (lower.contains(e.getKey())) {
                resp.setDestinationCategory(e.getValue());
                break;
            }
        }

        // 3. Subfolder
        Matcher folderMatcher = Pattern.compile("\\b(?:subfolder|folder|tag|dir)[\\s:=]+([a-zA-Z0-9_-]{1,50})\\b", Pattern.CASE_INSENSITIVE).matcher(prompt);
        if (folderMatcher.find()) {
            resp.setSubfolderTag(folderMatcher.group(1));
        }

        if (resp.getResourceId() == null) {
            resp.setValid(false);
            resp.setError("Target resource ID not specified (e.g. 'resource #12').");
            return resp;
        }

        if (resp.getDestinationCategory() == null) {
            resp.setValid(false);
            resp.setError("Approved destination category not recognized (e.g. 'question papers', 'lecture notes').");
            return resp;
        }

        resp.setValid(true);
        resp.setExplanation("Organize resource #" + resp.getResourceId() + " into '" + resp.getDestinationCategory() + "'" +
                (resp.getSubfolderTag() != null ? " with subfolder '" + resp.getSubfolderTag() + "'" : ""));
        return resp;
    }

    private boolean isPromptInjection(String msg) {
        String lower = msg.toLowerCase();
        for (String pat : PROMPT_INJECTION_PATTERNS) {
            if (Pattern.compile(pat).matcher(lower).find()) {
                return true;
            }
        }
        return false;
    }

    private String extractTextDefensively(File file) {
        try (InputStream in = new FileInputStream(file)) {
            byte[] bytes = in.readNBytes(50000);
            return new String(bytes, StandardCharsets.UTF_8).replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        } catch (Exception e) {
            return "Unable to read document contents: " + e.getMessage();
        }
    }

    private String generateExtractiveSummary(String text, int targetSentences) {
        String[] raw = text.split("(?<=[.!?])\\s+");
        List<String> clean = new ArrayList<>();
        for (String s : raw) {
            String trimmed = s.trim();
            if (trimmed.length() > 20) {
                clean.add(trimmed);
            }
        }

        if (clean.size() <= targetSentences) {
            return String.join(" ", clean);
        }

        // Calculate word frequencies
        Map<String, Integer> freq = new HashMap<>();
        for (String w : text.toLowerCase().split("\\W+")) {
            if (w.length() >= 3 && !STOPWORDS.contains(w)) {
                freq.put(w, freq.getOrDefault(w, 0) + 1);
            }
        }
        int maxFreq = freq.values().stream().max(Integer::compare).orElse(1);

        // Score sentences
        List<Map.Entry<Integer, Double>> scored = new ArrayList<>();
        for (int i = 0; i < clean.size(); i++) {
            String[] words = clean.get(i).toLowerCase().split("\\W+");
            double score = 0;
            for (String w : words) {
                score += (double) freq.getOrDefault(w, 0) / maxFreq;
            }
            score /= Math.max(words.length, 1);
            if (i == 0) score *= 1.5;
            else if (i < 3) score *= 1.25;
            scored.add(Map.entry(i, score));
        }

        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<Integer> selectedIndices = new ArrayList<>();
        for (int i = 0; i < Math.min(targetSentences, scored.size()); i++) {
            selectedIndices.add(scored.get(i).getKey());
        }
        Collections.sort(selectedIndices);

        List<String> summaryParts = new ArrayList<>();
        for (int idx : selectedIndices) {
            summaryParts.add(clean.get(idx));
        }
        return String.join(" ", summaryParts);
    }

    private List<String> tokenize(String text) {
        if (text == null) return Collections.emptyList();
        List<String> tokens = new ArrayList<>();
        String[] words = text.toLowerCase().split("\\W+");
        for (String w : words) {
            if (w.length() >= 2) {
                tokens.add(w);
                if (w.length() >= 3) {
                    for (int i = 0; i < w.length() - 2; i++) {
                        tokens.add(w.substring(i, i + 3));
                    }
                }
            }
        }
        return tokens;
    }

    private Map<String, Double> buildVector(List<String> tokens) {
        Map<String, Double> vec = new HashMap<>();
        for (String t : tokens) {
            vec.put(t, vec.getOrDefault(t, 0.0) + 1.0);
        }
        double maxCount = vec.values().stream().max(Double::compare).orElse(1.0);
        for (Map.Entry<String, Double> e : vec.entrySet()) {
            e.setValue(e.getValue() / maxCount);
        }
        return vec;
    }

    private double computeCosine(Map<String, Double> v1, Map<String, Double> v2) {
        Set<String> common = new HashSet<>(v1.keySet());
        common.retainAll(v2.keySet());
        if (common.isEmpty()) return 0.0;

        double dot = 0.0;
        for (String k : common) {
            dot += v1.get(k) * v2.get(k);
        }

        double norm1 = 0.0;
        for (double v : v1.values()) norm1 += v * v;

        double norm2 = 0.0;
        for (double v : v2.values()) norm2 += v * v;

        if (norm1 == 0.0 || norm2 == 0.0) return 0.0;
        return dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private String computeSha256(File file) {
        try (InputStream in = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            byte[] hash = digest.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return "hash_unavailable";
        }
    }

    private int countWords(String s) {
        if (s == null || s.isBlank()) return 0;
        return s.trim().split("\\s+").length;
    }
}
