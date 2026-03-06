package com.example.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "is_professor", nullable = false)
    private boolean isProfessor = false;

    @Column(name = "professor_id")
    private Integer professorId;

    @Column(name = "student_id", length = 50)
    private String studentId;

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


    public User() {}

    public User(Integer id, UUID uuid, String email, String passwordHash,
                String firstName, String lastName, boolean isProfessor,
                Integer professorId, String studentId,
                OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id           = id;
        this.uuid         = uuid;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.firstName    = firstName;
        this.lastName     = lastName;
        this.isProfessor  = isProfessor;
        this.professorId  = professorId;
        this.studentId    = studentId;
        this.createdAt    = createdAt;
        this.updatedAt    = updatedAt;
    }


    public Integer        getId()                          { return id; }
    public void           setId(Integer id)                { this.id = id; }

    public UUID           getUuid()                        { return uuid; }
    public void           setUuid(UUID uuid)               { this.uuid = uuid; }

    public String         getEmail()                       { return email; }
    public void           setEmail(String email)           { this.email = email; }

    public String         getPasswordHash()                { return passwordHash; }
    public void           setPasswordHash(String h)        { this.passwordHash = h; }

    public String         getFirstName()                   { return firstName; }
    public void           setFirstName(String v)           { this.firstName = v; }

    public String         getLastName()                    { return lastName; }
    public void           setLastName(String v)            { this.lastName = v; }

    public boolean        isProfessor()                    { return isProfessor; }
    public void           setProfessor(boolean v)          { this.isProfessor = v; }

    public Integer        getProfessorId()                 { return professorId; }
    public void           setProfessorId(Integer v)        { this.professorId = v; }

    public String         getStudentId()                   { return studentId; }
    public void           setStudentId(String v)           { this.studentId = v; }

    public OffsetDateTime getCreatedAt()                   { return createdAt; }
    public void           setCreatedAt(OffsetDateTime t)   { this.createdAt = t; }

    public OffsetDateTime getUpdatedAt()                   { return updatedAt; }
    public void           setUpdatedAt(OffsetDateTime t)   { this.updatedAt = t; }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Integer id;
        private UUID uuid;
        private String email, passwordHash, firstName, lastName, studentId;
        private boolean isProfessor = false;
        private Integer professorId;
        private OffsetDateTime createdAt, updatedAt;

        public Builder id(Integer id)                    { this.id = id;                   return this; }
        public Builder uuid(UUID uuid)                   { this.uuid = uuid;               return this; }
        public Builder email(String v)                   { this.email = v;                 return this; }
        public Builder passwordHash(String v)            { this.passwordHash = v;          return this; }
        public Builder firstName(String v)               { this.firstName = v;             return this; }
        public Builder lastName(String v)                { this.lastName = v;              return this; }
        public Builder isProfessor(boolean v)            { this.isProfessor = v;           return this; }
        public Builder professorId(Integer v)            { this.professorId = v;           return this; }
        public Builder studentId(String v)               { this.studentId = v;             return this; }
        public Builder createdAt(OffsetDateTime v)       { this.createdAt = v;             return this; }
        public Builder updatedAt(OffsetDateTime v)       { this.updatedAt = v;             return this; }

        public User build() {
            return new User(id, uuid, email, passwordHash, firstName, lastName,
                            isProfessor, professorId, studentId, createdAt, updatedAt);
        }
    }
}
