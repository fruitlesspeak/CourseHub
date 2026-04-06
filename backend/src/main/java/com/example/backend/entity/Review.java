package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "course_id", nullable = false)
    private Integer courseId;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public Review() {
    }

    public Review(Integer id, Integer userId, Integer courseId, Integer rating, String comment, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.courseId = courseId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Integer          getId()                                  { return id; }
    public void             setId(Integer id)                        { this.id = id; }

    public Integer          getUserId()                              { return userId; }
    public void             setUserId(Integer userId)                { this.userId = userId; }

    public Integer          getCourseId()                            { return courseId; }
    public void             setCourseId(Integer courseId)            { this.courseId = courseId; }

    public Integer          getRating()                              { return rating; }
    public void             setRating(Integer rating)                { this.rating = rating; }

    public String           getComment()                             { return comment; }
    public void             setComment(String comment)               { this.comment = comment; }

    public OffsetDateTime   getCreatedAt()                           { return createdAt; }
    public void             setCreatedAt(OffsetDateTime createdAt)   { this.createdAt = createdAt; }

    public OffsetDateTime   getUpdatedAt() { return updatedAt; }
    public void             setUpdatedAt(OffsetDateTime updatedAt)   { this.updatedAt = updatedAt; }
}
