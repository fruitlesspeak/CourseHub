package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public class ImportantDateDto {

    public static class CreateRequest {

        @NotBlank(message = "Title is required.")
        @Size(max = 255, message = "Title must be 255 characters or fewer.")
        private String title;

        private String description;

        @NotNull(message = "Due date is required.")
        private OffsetDateTime dueAt;

        public CreateRequest() {}

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
    }

    public static class UpdateRequest {

        @Size(max = 255, message = "Title must be 255 characters or fewer.")
        private String title;

        private String description;

        private OffsetDateTime dueAt;

        public UpdateRequest() {}

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
    }

    public static class Response {
        private Integer id;
        private Integer courseId;
        private Integer createdByUserId;
        private String title;
        private String description;
        private OffsetDateTime dueAt;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public Response() {}

        public Response(Integer id, Integer courseId, Integer createdByUserId, String title,
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

        public Integer getCourseId() {
            return courseId;
        }

        public Integer getCreatedByUserId() {
            return createdByUserId;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public OffsetDateTime getDueAt() {
            return dueAt;
        }

        public OffsetDateTime getCreatedAt() {
            return createdAt;
        }

        public OffsetDateTime getUpdatedAt() {
            return updatedAt;
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

            public Response build() {
                return new Response(id, courseId, createdByUserId, title, description, dueAt, createdAt, updatedAt);
            }
        }
    }
}
