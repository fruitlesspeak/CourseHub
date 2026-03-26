package com.example.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "important_dates",
        indexes = {
                @Index(name = "idx_important_dates_course_due_at", columnList = "course_id,due_at"),
                @Index(name = "idx_important_dates_created_by_user", columnList = "created_by_user_id")
        }
)
public class ImportantDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "course_id", nullable = false)
    private Integer courseId;

    @Column(name = "created_by_user_id", nullable = false)
    private Integer createdByUserId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "due_at", nullable = false)
    private OffsetDateTime dueAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public ImportantDate() {}

    public ImportantDate(Integer id, Integer courseId, Integer createdByUserId, String title,
                         String description, OffsetDateTime dueAt, OffsetDateTime createdAt,
                         OffsetDateTime updatedAt) {
        this.id = id;
        this.courseId = courseId;
        this.createdByUserId = createdByUserId;
        this.title = title;
        this.description = description;
        this.dueAt = dueAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Integer getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Integer createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OffsetDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(OffsetDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer id;
        private Integer courseId;
        private Integer createdByUserId;
        private String title;
        private String description;
        private OffsetDateTime dueAt;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public Builder id(Integer id) {
            this.id = id;
            return this;
        }

        public Builder courseId(Integer courseId) {
            this.courseId = courseId;
            return this;
        }

        public Builder createdByUserId(Integer createdByUserId) {
            this.createdByUserId = createdByUserId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder dueAt(OffsetDateTime dueAt) {
            this.dueAt = dueAt;
            return this;
        }

        public Builder createdAt(OffsetDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(OffsetDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ImportantDate build() {
            return new ImportantDate(id, courseId, createdByUserId, title, description, dueAt, createdAt, updatedAt);
        }
    }
}
