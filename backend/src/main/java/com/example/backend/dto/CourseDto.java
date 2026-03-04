package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public class CourseDto {


    public static class CreateRequest {

        @NotBlank
        @Size(max = 255)
        private String title;

        @NotBlank
        @Size(max = 50)
        private String code;

        private String description;

        @Size(max = 1000)
        private String link;

        @Size(max = 1000)
        private String tags;

        private String material;

        private OffsetDateTime dueDate;

        public CreateRequest() {}

        public String  getTitle()                              { return title; }
        public void    setTitle(String v)                      { this.title = v; }
        public String  getCode()                               { return code; }
        public void    setCode(String v)                       { this.code = v; }
        public String  getDescription()                        { return description; }
        public void    setDescription(String v)                { this.description = v; }
        public String  getLink()                               { return link; }
        public void    setLink(String v)                       { this.link = v; }
        public String  getTags()                               { return tags; }
        public void    setTags(String v)                       { this.tags = v; }
        public String  getMaterial()                           { return material; }
        public void    setMaterial(String v)                   { this.material = v; }
        public OffsetDateTime getDueDate()                     { return dueDate; }
        public void    setDueDate(OffsetDateTime v)            { this.dueDate = v; }
    }



    public static class UpdateRequest {

        @Size(max = 255)
        private String  title;

        @Size(max = 50)
        private String  code;

        private String  description;

        @Size(max = 1000)
        private String  link;

        @Size(max = 1000)
        private String tags;

        private String material;

        private OffsetDateTime dueDate;

        public UpdateRequest() {}

        public String  getTitle()                              { return title; }
        public void    setTitle(String v)                      { this.title = v; }
        public String  getCode()                               { return code; }
        public void    setCode(String v)                       { this.code = v; }
        public String  getDescription()                        { return description; }
        public void    setDescription(String v)                { this.description = v; }
        public String  getLink()                               { return link; }
        public void    setLink(String v)                       { this.link = v; }
        public String  getTags()                               { return tags; }
        public void    setTags(String v)                       { this.tags = v; }
        public String  getMaterial()                           { return material; }
        public void    setMaterial(String v)                   { this.material = v; }
        public OffsetDateTime getDueDate()                     { return dueDate; }
        public void    setDueDate(OffsetDateTime v)            { this.dueDate = v; }
    }


    public static class Response {
        private Integer        id;
        private UUID           uuid;
        private String         title;
        private String         code;
        private String         description;
        private String         link;
        private String         tags;
        private String         material;
        private OffsetDateTime dueDate;
        private Integer        professorId;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public Response() {}

        public Response(Integer id, UUID uuid, String title, String code, String description, String link,
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

        public Integer        getId()          { return id; }
        public UUID           getUuid()        { return uuid; }
        public String         getTitle()       { return title; }
        public String         getCode()        { return code; }
        public String         getDescription() { return description; }
        public String         getLink()        { return link; }
        public String         getTags()        { return tags; }
        public String         getMaterial()    { return material; }
        public OffsetDateTime getDueDate()     { return dueDate; }
        public Integer        getProfessorId() { return professorId; }
        public OffsetDateTime getCreatedAt()   { return createdAt; }
        public OffsetDateTime getUpdatedAt()   { return updatedAt; }

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

            public Builder id(Integer v)               { this.id = v;          return this; }
            public Builder uuid(UUID v)                { this.uuid = v;        return this; }
            public Builder title(String v)             { this.title = v;       return this; }
            public Builder code(String v)              { this.code = v;        return this; }
            public Builder description(String v)       { this.description = v; return this; }
            public Builder link(String v)              { this.link = v;        return this; }
            public Builder tags(String v)              { this.tags = v;        return this; }
            public Builder material(String v)          { this.material = v;    return this; }
            public Builder dueDate(OffsetDateTime v)   { this.dueDate = v;     return this; }
            public Builder professorId(Integer v)      { this.professorId = v; return this; }
            public Builder createdAt(OffsetDateTime v) { this.createdAt = v;   return this; }
            public Builder updatedAt(OffsetDateTime v) { this.updatedAt = v;   return this; }

            public Response build() {
                return new Response(id, uuid, title, code, description, link, tags, material, dueDate,
                        professorId, createdAt, updatedAt);
            }
        }
    }
}
