package com.college.resourcefinder.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA entity recording conversational interactions with the AI Academic Assistant.
 * Mapped to the {@code chat_interactions} table.
 */
@Entity
@Table(name = "chat_interactions")
public class ChatInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interaction_id")
    private Long interactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Column(name = "user_message", nullable = false, columnDefinition = "TEXT")
    private String userMessage;

    @Column(name = "detected_intent", nullable = false, length = 100)
    private String detectedIntent;

    @Column(name = "assistant_reply", nullable = false, columnDefinition = "TEXT")
    private String assistantReply;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "SUCCESS";

    @Column(name = "latency_ms", nullable = false)
    private Integer latencyMs = 0;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
        if (this.status == null) {
            this.status = "SUCCESS";
        }
        if (this.latencyMs == null) {
            this.latencyMs = 0;
        }
    }

    public ChatInteraction() {}

    public ChatInteraction(User user, String sessionId, String userMessage,
                           String detectedIntent, String assistantReply,
                           String status, Integer latencyMs) {
        this.user = user;
        this.sessionId = sessionId;
        this.userMessage = userMessage;
        this.detectedIntent = detectedIntent;
        this.assistantReply = assistantReply;
        this.status = status;
        this.latencyMs = latencyMs;
    }

    public Long getInteractionId() { return interactionId; }
    public void setInteractionId(Long interactionId) { this.interactionId = interactionId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserMessage() { return userMessage; }
    public void setUserMessage(String userMessage) { this.userMessage = userMessage; }

    public String getDetectedIntent() { return detectedIntent; }
    public void setDetectedIntent(String detectedIntent) { this.detectedIntent = detectedIntent; }

    public String getAssistantReply() { return assistantReply; }
    public void setAssistantReply(String assistantReply) { this.assistantReply = assistantReply; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatInteraction that = (ChatInteraction) o;
        return Objects.equals(interactionId, that.interactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interactionId);
    }
}
