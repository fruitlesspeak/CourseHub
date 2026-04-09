# Project Summary and Vision

**CourseHub** is a course platform for students that brings learning and community feedback into one platform. CourseHub targets a common problem: students often rely on scattered external resources, but learning platforms deliver content that lack transparent peer reviews, rating platforms provide opinions but don’t deliver courses, and campus LMS tools have limited discovery and no rating system.

**CourseHub combines the three core capabilities into one platform:**

- Course creation and delivery
- Community ratings and reviews
- Personalized progress tracking

### Our vision

Help students discover high quality learning content while empowering instructors to share knowledge.

## Team Members

- Jessie Ttito Tello
- Pankaj Jhanji
- Mohamed Hammam
- Opeyemi Ogundimu

# Core Functional Features

1.  **User Authentication & Role Management:** Secure sign-up/login functionality supporting distinct roles (Professor/Student) to control access to specific features.
2.  **Course Creation & Management:** A set of tools for Professors to create, manage, and delete courses.
3.  **Enrollment & Progress Tracking:** Functionality for students to browse courses via query/tags, enroll, and access materials.
4.  **Rating & Review System:** A feedback feature allowing students to rate courses and write reviews, which are aggregated and displayed on course detail pages.
5.  **Analytics Dashboards:** Personalized dashboards for students to track upcoming due dates and progress, and Professors to view completion rates and student metrics.
6.  **Assignment & Submission (Stretch Feature):** A system where Professors can post assignments, and students can upload submissions for review.

[Here is a link to our Core Functional Features](https://github.com/fruitlesspeak/CourseHub/blob/main/CoreFeatures.md#core-functional-features)

# Technologies

| Component               | Technology & Details                                                                                             |
| ----------------------- | ---------------------------------------------------------------------------------------------------------------- |
| Frontend                | - Vue.js: gentle learning curve, similar to HTML <br> - Bootstrap: for UI components <br> - Axios: for API calls |
| Backend                 | - Java Spring Boot (MVC pattern): industry relevance, extensive documentation, familiarity from previous courses |
| Database                | - PostgreSQL <br> - Hibernate: integration with Spring Boot                                                      |
| DevOps & Infrastructure | - Docker: containerization <br> - GitHub Actions: CI/CD pipelines <br> - Maven: build management                 |

### Testing:

- Backend: JUnit, MockMvc, and Spring Boot Test.
- Frontend: Vitest or Jest.
- Load Testing: JMeter.

[Here is a link to our Technologies](https://github.com/fruitlesspeak/CourseHub/blob/main/CoreFeatures.md#technologies)

# User Stories

### User Authentication & Role Management

- As a guest user, I want to register with email/password, so that I can create an account to access CourseHub features.
- As a registered user, I want to log in with my credentials so that I can access my personalized CourseHub experience.

**Acceptance Criteria**

- Given that I am on the registration page, when I enter a valid email and password, a new account should be created for me.
- Given that I am on the login page, when I enter valid credentials, I should be redirected to my specific dashboard.

### Course Creation & Management

- As a Professor, I want to create new courses so that I can share my teaching content with students.
- As a Professor, I want to edit/delete my existing courses, so that I can update content or remove outdated material.

**Acceptance Criteria**

- Given that I am logged in as a professor, when I fill in the required fields and click “Create” , then the course should appear in the catalog, and students should be able to enroll.
- Given that I am viewing my created course, when I click "Edit", then I should be able to modify the course description, tags, material, and due dates.
- Given I select "Delete" on a course I created, when I confirm the action, then the course should be permanently removed from the platform.

### Enrollment & Progress Tracking

- As a Student, I want to browse and enroll in available courses so that I can start learning new content.
- As a Student, I want to track my course progress so that I know my status.

**Acceptance Criteria**

- Given that I am a student viewing the course catalog, when I click "Enroll" on a course, I should be added to the course student list and granted access to materials.
- Given I am an enrolled Student, when I complete submissions and get graded, then my progress bar on the dashboard should update to reflect the new percentage grade.
- Given I search for a course by tag (e.g., "Java"), when I submit the query, then only courses containing that tag should be displayed.

### Rating & Reviews

- As a Student, I want to rate completed courses so that I can help future students make decisions.
- As a Student, I want to see course ratings before enrolling, so that I can assess quality.

**Acceptance Criteria**

- Given that I have completed a course, when I submit a rating and a review, the review should be appended to the course reviews page.
- Given that I am browsing courses, when I view a course card, I should see an average rating and individual reviews from other students.

### Dashboards

- As a Professor, I want a dashboard showing my teaching analytics so that I can monitor course performance.
- As a Professor, I want a dashboard showing my reviews, so that I can see what I did well and what I may need to improve on.

**Acceptance Criteria**

- Given that I am a professor, when I view the analytics section, I should see useful metrics about my students, such as the number of students enrolled, submission rates, and students' progress.
- Given that I am a professor, when I access the reviews tab, I should see a list of the most recent reviews left by students.

### Assignments & Submission (Stretch goal)

- As a Professor, I want to create assignments for my course lessons so that I can assess student understanding.
- As a Student, I want to submit assignments and view feedback so that I can demonstrate my work and receive grades.

**Acceptance Criteria**

- Given that I am a professor editing a course, when I post an assignment with a due date, it should be visible to all enrolled students.
- Given that I am a student, when I upload and submit my assignment, the system should confirm the upload and mark the assignment as "Submitted".
- Given that I am a professor viewing a student's submission, when I enter a numeric grade and written feedback, then the student should be able to view that grade and feedback on their dashboard.

[Here is a link to our User Stories](https://github.com/fruitlesspeak/CourseHub/blob/main/UserStories.md)

# Block diagram

[Here is a link to our block diagram](https://github.com/fruitlesspeak/CourseHub/blob/main/BlockDiagram.md)

---

_You can find our meeting minutes on our Wiki Page or in MeetingMinutes.md_

## SonarCloud

Track open code quality issues here: [SonarCloud Issues](https://sonarcloud.io/project/issues?issueStatuses=OPEN%2CCONFIRMED&id=fruitlesspeak_CourseHub)

# Run Locally

To run the project locally with Docker:

1. Update `frontend/nginx.conf.template` so the backend upstream points to the Docker service instead of Railway:

   Replace:
    ```nginx
    set $backend_upstream http://backend.railway.internal:8080;
    ```
   with:
    ```nginx
    set $backend_upstream http://backend:8080;
    ```



2. Fill in the values in `.env.example`, then copy the same values into `.env` because `docker compose` reads environment variables from `.env`.

3. Start the required services from the project root:

   ```bash
   docker compose up --build db frontend backend
   ```
