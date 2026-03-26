package com.example.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "course_id", nullable = false)
    private Integer courseId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;


    public Enrollment() {}

    public Enrollment(Integer id, Integer userId, Integer courseId, Boolean isActive, OffsetDateTime createdAt) {
        this.id        = id;
        this.userId    = userId;
        this.courseId  = courseId;
        this.isActive  = isActive;
        this.createdAt = createdAt;
    }

    public Integer        getId()                                  { return id; }
    public void           setId(Integer id)                        { this.id = id; }

    public Integer        getUserId()                              { return userId;}
    public void           setUserId(Integer userId)                { this.userId = userId; }

    public Integer        getCourseId()                            { return courseId;}
    public void           setCourseId(Integer courseId)            { this.courseId = courseId; }

    public Boolean        getIsActive()                            { return isActive; }
    public void           setIsActive(Boolean active)              { isActive = active; }

    public OffsetDateTime getCreatedAt()                           { return createdAt; }
    public void           setCreatedAt(OffsetDateTime createdAt)   { this.createdAt = createdAt;}
}