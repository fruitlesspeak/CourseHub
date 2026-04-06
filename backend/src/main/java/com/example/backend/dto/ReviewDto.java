package com.example.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public class ReviewDto {

    public static class CreateRequest {

        @NotNull(message = "Rating is required.")
        @Min(value = 1, message = "Rating must be between 1 and 5.")
        @Max(value = 5, message = "Rating must be between 1 and 5.")
        private Integer rating;

        private String comment;

        public CreateRequest() {}

        public Integer getRating()          { return rating; }
        public void    setRating(Integer v) { this.rating = v; }
        public String  getComment()         { return comment; }
        public void    setComment(String v) { this.comment = v; }
    }

    public static class Response {

        private Integer        id;
        private Integer        rating;
        private String         comment;
        private String         reviewerFirstName;
        private String         reviewerLastName;
        private Integer        courseId;
        private String         courseTitle;
        private OffsetDateTime createdAt;

        public Response() {}

        private Response(Builder b) {
            this.id                = b.id;
            this.rating            = b.rating;
            this.comment           = b.comment;
            this.reviewerFirstName = b.reviewerFirstName;
            this.reviewerLastName  = b.reviewerLastName;
            this.courseId          = b.courseId;
            this.courseTitle       = b.courseTitle;
            this.createdAt         = b.createdAt;
        }

        public Integer        getId()                { return id; }
        public Integer        getRating()            { return rating; }
        public String         getComment()           { return comment; }
        public String         getReviewerFirstName() { return reviewerFirstName; }
        public String         getReviewerLastName()  { return reviewerLastName; }
        public Integer        getCourseId()          { return courseId; }
        public String         getCourseTitle()       { return courseTitle; }
        public OffsetDateTime getCreatedAt()         { return createdAt; }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private Integer        id;
            private Integer        rating;
            private String         comment;
            private String         reviewerFirstName;
            private String         reviewerLastName;
            private Integer        courseId;
            private String         courseTitle;
            private OffsetDateTime createdAt;

            public Builder id(Integer v)               { this.id = v;                return this; }
            public Builder rating(Integer v)           { this.rating = v;            return this; }
            public Builder comment(String v)           { this.comment = v;           return this; }
            public Builder reviewerFirstName(String v) { this.reviewerFirstName = v; return this; }
            public Builder reviewerLastName(String v)  { this.reviewerLastName = v;  return this; }
            public Builder courseId(Integer v)         { this.courseId = v;          return this; }
            public Builder courseTitle(String v)       { this.courseTitle = v;       return this; }
            public Builder createdAt(OffsetDateTime v) { this.createdAt = v;         return this; }

            public Response build() { return new Response(this); }
        }
    }
}
