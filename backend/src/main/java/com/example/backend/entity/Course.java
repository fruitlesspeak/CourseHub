package com.example.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "professor_id", nullable = false)
    private Integer professorId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null) uuid = UUID.randomUUID();
    }


    public Course() {}

    public Course(Integer id, UUID uuid, String title, String description,
                  Integer professorId, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id          = id;
        this.uuid        = uuid;
        this.title       = title;
        this.description = description;
        this.professorId = professorId;
        this.createdAt   = createdAt;
        this.updatedAt   = updatedAt;
    }


    public Integer        getId()                          { return id; }
    public void           setId(Integer id)                { this.id = id; }

    public UUID           getUuid()                        { return uuid; }
    public void           setUuid(UUID uuid)               { this.uuid = uuid; }

    public String         getTitle()                       { return title; }
    public void           setTitle(String v)               { this.title = v; }

    public String         getDescription()                 { return description; }
    public void           setDescription(String v)         { this.description = v; }

    public Integer        getProfessorId()                 { return professorId; }
    public void           setProfessorId(Integer v)        { this.professorId = v; }

    public OffsetDateTime getCreatedAt()                   { return createdAt; }
    public void           setCreatedAt(OffsetDateTime t)   { this.createdAt = t; }

    public OffsetDateTime getUpdatedAt()                   { return updatedAt; }
    public void           setUpdatedAt(OffsetDateTime t)   { this.updatedAt = t; }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Integer id;
        private UUID uuid;
        private String title, description;
        private Integer professorId;
        private OffsetDateTime createdAt, updatedAt;

        public Builder id(Integer id)                    { this.id = id;           return this; }
        public Builder uuid(UUID uuid)                   { this.uuid = uuid;       return this; }
        public Builder title(String v)                   { this.title = v;         return this; }
        public Builder description(String v)             { this.description = v;   return this; }
        public Builder professorId(Integer v)            { this.professorId = v;   return this; }
        public Builder createdAt(OffsetDateTime v)       { this.createdAt = v;     return this; }
        public Builder updatedAt(OffsetDateTime v)       { this.updatedAt = v;     return this; }

        public Course build() {
            return new Course(id, uuid, title, description, professorId, createdAt, updatedAt);
        }
    }
}