# Core Functional Features

1.  **User Authentication & Role Management:** Secure sign-up/login functionality supporting distinct roles (Professor/Student) to control access to specific features.
2.  **Course Creation & Management:** A set of tools for Professors to create, manage, and delete courses.
3.  **Enrollment & Progress Tracking:** Functionality for students to browse courses via query/tags, enroll, and access materials.
4.  **Rating & Review System:** A feedback feature allowing students to rate courses and write reviews, which are aggregated and displayed on course detail pages.
5.  **Analytics Dashboards:** Personalized dashboards for students to track upcoming due dates and progress, and Professors to view completion rates and student metrics.
6.  **Assignment & Submission (Stretch Feature):** A system where Professors can post assignments, and students can upload submissions for review.

# Technologies

| Component               | Technology & Details                                                                                             |
| ----------------------- | ---------------------------------------------------------------------------------------------------------------- |
| Frontend                | - Vue.js: gentle learning curve, similar to HTML <br> - Bootstrap: for UI components <br> - Axios: for API calls |
| Backend                 | - Java Spring Boot (MVC pattern): industry relevance, extensive documentation, familiarity from previous courses |
| Database                | - PostgreSQL <br> - Hibernate: integration with Spring Boot                                                      |
| DevOps & Infrastructure | - Docker: containerization <br> - GitHub Actions: CI/CD pipelines <br> - Maven: build management                 |

# Testing:

- Backend: JUnit, MockMvc, and Spring Boot Test.
- Frontend: Vitest or Jest.
- Load Testing: JMeter.
