package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public class CourseDto {


    public static class CreateRequest {

        @NotBlank @Size(max = 255)
        private String title;

        private String description;

        @NotNull
        private Integer professorId;

        public CreateRequest() {}

        public String  getTitle()                              { return title; }
        public void    setTitle(String v)                      { this.title = v; }
        public String  getDescription()                        { return description; }
        public void    setDescription(String v)                { this.description = v; }
        public Integer getProfessorId()                        { return professorId; }
        public void    setProfessorId(Integer v)               { this.professorId = v; }
    }



    public static class UpdateRequest {

        @Size(max = 255)
        private String  title;
        private String  description;
        private Integer professorId;

        public UpdateRequest() {}

        public String  getTitle()                              { return title; }
        public void    setTitle(String v)                      { this.title = v; }
        public String  getDescription()                        { return description; }
        public void    setDescription(String v)                { this.description = v; }
        public Integer getProfessorId()                        { return professorId; }
        public void    setProfessorId(Integer v)               { this.professorId = v; }
    }


    public static class Response {
        private Integer        id;
        private UUID           uuid;
        private String         title;
        private String         description;
        private Integer        professorId;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public Response() {}

        public Response(Integer id, UUID uuid, String title, String description,
                        Integer professorId, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
            this.id          = id;
            this.uuid        = uuid;
            this.title       = title;
            this.description = description;
            this.professorId = professorId;
            this.createdAt   = createdAt;
            this.updatedAt   = updatedAt;
        }

        public Integer        getId()          { return id; }
        public UUID           getUuid()        { return uuid; }
        public String         getTitle()       { return title; }
        public String         getDescription() { return description; }
        public Integer        getProfessorId() { return professorId; }
        public OffsetDateTime getCreatedAt()   { return createdAt; }
        public OffsetDateTime getUpdatedAt()   { return updatedAt; }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private Integer id; private UUID uuid;
            private String title, description;
            private Integer professorId;
            private OffsetDateTime createdAt, updatedAt;

            public Builder id(Integer v)              { this.id = v;          return this; }
            public Builder uuid(UUID v)               { this.uuid = v;        return this; }
            public Builder title(String v)            { this.title = v;       return this; }
            public Builder description(String v)      { this.description = v; return this; }
            public Builder professorId(Integer v)     { this.professorId = v; return this; }
            public Builder createdAt(OffsetDateTime v){ this.createdAt = v;   return this; }
            public Builder updatedAt(OffsetDateTime v){ this.updatedAt = v;   return this; }

            public Response build() {
                return new Response(id, uuid, title, description, professorId, createdAt, updatedAt);
            }
        }
    }
}