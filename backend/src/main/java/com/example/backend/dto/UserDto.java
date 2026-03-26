package com.example.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public class UserDto {

    public static class CreateRequest {

        @NotBlank @Email
        private String email;

        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        @NotBlank
        private String firstName;

        @NotBlank
        private String lastName;

        private boolean isProfessor = false;
        private Integer professorId;
        private String  studentId;

        public CreateRequest() {}

        public String  getEmail()                              { return email; }
        public void    setEmail(String v)                      { this.email = v; }
        public String  getPassword()                           { return password; }
        public void    setPassword(String v)                   { this.password = v; }
        public String  getFirstName()                          { return firstName; }
        public void    setFirstName(String v)                  { this.firstName = v; }
        public String  getLastName()                           { return lastName; }
        public void    setLastName(String v)                   { this.lastName = v; }
        public boolean isProfessor()                           { return isProfessor; }
        public void    setProfessor(boolean v)                 { this.isProfessor = v; }
        public Integer getProfessorId()                        { return professorId; }
        public void    setProfessorId(Integer v)               { this.professorId = v; }
        public String  getStudentId()                          { return studentId; }
        public void    setStudentId(String v)                  { this.studentId = v; }
    }

    public static class UpdateRequest {

        @Email
        private String  email;
        private String  firstName;
        private String  lastName;
        private Boolean isProfessor;
        private Integer professorId;
        private String  studentId;

        public UpdateRequest() {}

        public String  getEmail()                              { return email; }
        public void    setEmail(String v)                      { this.email = v; }
        public String  getFirstName()                          { return firstName; }
        public void    setFirstName(String v)                  { this.firstName = v; }
        public String  getLastName()                           { return lastName; }
        public void    setLastName(String v)                   { this.lastName = v; }
        public Boolean getIsProfessor()                        { return isProfessor; }
        public void    setIsProfessor(Boolean v)               { this.isProfessor = v; }
        public Integer getProfessorId()                        { return professorId; }
        public void    setProfessorId(Integer v)               { this.professorId = v; }
        public String  getStudentId()                          { return studentId; }
        public void    setStudentId(String v)                  { this.studentId = v; }
    }

    public static class Response {
        private Integer        id;
        private UUID           uuid;
        private String         email;
        private String         firstName;
        private String         lastName;
        private boolean        isProfessor;
        private Integer        professorId;
        private String         studentId;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public Response() {}

        public Response(Integer id, UUID uuid, String email, String firstName, String lastName,
                        boolean isProfessor, Integer professorId, String studentId,
                        OffsetDateTime createdAt, OffsetDateTime updatedAt) {
            this.id          = id;
            this.uuid        = uuid;
            this.email       = email;
            this.firstName   = firstName;
            this.lastName    = lastName;
            this.isProfessor = isProfessor;
            this.professorId = professorId;
            this.studentId   = studentId;
            this.createdAt   = createdAt;
            this.updatedAt   = updatedAt;
        }

        public Integer        getId()          { return id; }
        public UUID           getUuid()        { return uuid; }
        public String         getEmail()       { return email; }
        public String         getFirstName()   { return firstName; }
        public String         getLastName()    { return lastName; }
        public boolean        isProfessor()    { return isProfessor; }
        public Integer        getProfessorId() { return professorId; }
        public String         getStudentId()   { return studentId; }
        public OffsetDateTime getCreatedAt()   { return createdAt; }
        public OffsetDateTime getUpdatedAt()   { return updatedAt; }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private Integer id; private UUID uuid;
            private String email, firstName, lastName, studentId;
            private boolean isProfessor;
            private Integer professorId;
            private OffsetDateTime createdAt, updatedAt;

            public Builder id(Integer v)            { this.id = v;          return this; }
            public Builder uuid(UUID v)             { this.uuid = v;        return this; }
            public Builder email(String v)          { this.email = v;       return this; }
            public Builder firstName(String v)      { this.firstName = v;   return this; }
            public Builder lastName(String v)       { this.lastName = v;    return this; }
            public Builder isProfessor(boolean v)   { this.isProfessor = v; return this; }
            public Builder professorId(Integer v)   { this.professorId = v; return this; }
            public Builder studentId(String v)      { this.studentId = v;   return this; }
            public Builder createdAt(OffsetDateTime v) { this.createdAt = v; return this; }
            public Builder updatedAt(OffsetDateTime v) { this.updatedAt = v; return this; }

            public Response build() {
                return new Response(id, uuid, email, firstName, lastName, isProfessor, professorId, studentId, createdAt, updatedAt);
            }
        }
    }
}