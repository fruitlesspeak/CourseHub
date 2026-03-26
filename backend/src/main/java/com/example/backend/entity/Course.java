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

    @Column(nullable = false, length = 50)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 1000)
    private String link;

    @Column(length = 1000)
    private String tags;

    @Column(columnDefinition = "TEXT")
    private String material;

    @Column(name = "due_date")
    private OffsetDateTime dueDate;

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

    public Course(Integer id, UUID uuid, String title, String code, String description, String link,
                  String tags, String material, OffsetDateTime dueDate, Integer professorId,
                  OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id          = id;
        this.uuid        = uuid;
        this.title       = title;
        this.code        = code;
        this.description = description;
        this.link        = link;
        this.tags        = tags;
        this.material    = material;
        this.dueDate     = dueDate;
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

    public String         getCode()                        { return code; }
    public void           setCode(String v)                { this.code = v; }

    public String         getDescription()                 { return description; }
    public void           setDescription(String v)         { this.description = v; }

    public String         getLink()                        { return link; }
    public void           setLink(String v)                { this.link = v; }

    public String         getTags()                        { return tags; }
    public void           setTags(String v)                { this.tags = v; }

    public String         getMaterial()                    { return material; }
    public void           setMaterial(String v)            { this.material = v; }

    public OffsetDateTime getDueDate()                     { return dueDate; }
    public void           setDueDate(OffsetDateTime v)     { this.dueDate = v; }

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
        private String title;
        private String code;
        private String description;
        private String link;
        private String tags;
        private String material;
        private OffsetDateTime dueDate;
        private Integer professorId;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public Builder id(Integer id)                    { this.id = id;           return this; }
        public Builder uuid(UUID uuid)                   { this.uuid = uuid;       return this; }
        public Builder title(String v)                   { this.title = v;         return this; }
        public Builder code(String v)                    { this.code = v;          return this; }
        public Builder description(String v)             { this.description = v;   return this; }
        public Builder link(String v)                    { this.link = v;          return this; }
        public Builder tags(String v)                    { this.tags = v;          return this; }
        public Builder material(String v)                { this.material = v;      return this; }
        public Builder dueDate(OffsetDateTime v)         { this.dueDate = v;       return this; }
        public Builder professorId(Integer v)            { this.professorId = v;   return this; }
        public Builder createdAt(OffsetDateTime v)       { this.createdAt = v;     return this; }
        public Builder updatedAt(OffsetDateTime v)       { this.updatedAt = v;     return this; }

        public Course build() {
            return new Course(id, uuid, title, code, description, link, tags, material, dueDate,
                    professorId, createdAt, updatedAt);
        }
    }
}
