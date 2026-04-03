# Mutation Testing Plan

## ChangeLog

| Version | Change Date | By | Description |
| --- | --- | --- | --- |
| 1 | 26th March 2026 | Opeyemi Ogundimu | Initial mutation testing plan aligned with the Sprint 2 Test Plan |
| 2 | 26th March 2026 | Opeyemi Ogundimu | Added Feature 3 enrollment and discovery mutation scope, implemented test titles, and final mutation results |

## 1 Introduction

### 1.1 Scope

CourseHub is a course platform project. This mutation testing plan supplements the Sprint 2 Test Plan by defining how mutation testing will be used to assess the strength of the backend unit test suite for the implemented core features.

**Functional Scope (Sprint 2 only)**

- User Authentication & Role Management: mutation testing will validate the service-layer logic for registration, login, session retrieval, logout, input normalization, password verification, and role-aware dashboard routing.
- Course Creation & Management: mutation testing will validate the service-layer logic for course creation, update, deletion, ownership checks, course retrieval, search behavior, and link normalization/validation.
- Enrollment and Discovery: mutation testing will validate the service-layer logic for course tag filtering, active enrollment creation and reactivation, unenrollment, enrolled-course retrieval, enrollment checks, and enrolled-student access behavior.
- Mutation target scope: the focus of this document is backend service-layer logic exercised through JUnit 5 unit tests.
- Mutation execution scope: feature-specific PIT runs may target selected service classes such as `RegisterService`, `AuthService`, `CourseService`, and `EnrollmentService`, depending on the feature being analyzed.
- Out of scope for this document: controller-only API testing, frontend component testing, manual acceptance testing, and broader integration flows, which remain covered by the main Test Plan.

**Non-Functional Scope (Sprint 2 only)**

- Test Strength: mutation testing will be used to confirm that the unit tests detect meaningful code changes rather than only achieving line coverage.
- Fault Detection: the team will use mutation testing to expose weak assertions, missing branch coverage, and insufficient negative-path tests.
- Reliability: mutation results must be reproducible when re-run locally and in CI for the selected feature scope.
- Maintainability: mutation testing will be kept focused on feature-level service classes so that surviving mutants are easier to analyze and fix.
- Release Readiness: mutation analysis is intended to strengthen confidence in the backend services before merge and release.

### 1.2 Roles and Responsibilities

| Name | Net ID | GitHub username | Role |
| --- | --- | --- | --- |
| Pankaj Jhanji | jhanjip | fruitlesspeak | Full-Stack Developer |
| Jessie Ttito Tello | ttitotej | jessiettito | Full-Stack Developer |
| Mohamed Hammam | hammamm | mhammam2 | Full-Stack Developer |
| Opeyemi Ogundimu | ogundimo | ogundimo | Full-Stack Developer |

**Full-Stack Developers**

**Roles**

- Build end-to-end features from UI to API to database and strengthen the associated automated tests.

**Responsibilities**

- Build and maintain mutation-oriented unit tests for the selected backend service classes.
- Review surviving mutants, improve assertions and edge-case coverage, and re-run PIT until the required outcome is reached.
- Document mutation scope, target classes, and feature-level results in a way that is traceable to the Sprint 2 Test Plan.
- Keep the Maven/PIT configuration and CI workflow functional so mutation analysis can be reproduced by the team.

## 2 Mutation Test Methodology

Mutation testing will be conducted on backend service-layer logic by running PIT against the selected feature classes and then executing the related JUnit 5 test suites. PIT creates small changes in the compiled code, re-runs the tests, and reports whether those tests were strong enough to detect the injected fault.

For this document:

- A mutant is **killed** if one or more tests fail after the mutation is introduced.
- A mutant **survives** if all related tests still pass.
- A mutant is **equivalent** if it does not change observable behavior and therefore cannot be killed by a valid test.

Equivalent mutants must be reviewed carefully and only excluded when there is a clear justification. The target outcome for each selected feature is a 100% mutation score across all non-equivalent mutants.

### CORE FEATURE 1: User Authentication & Role Management

Secure sign-up/login functionality supporting distinct roles (Professor/Student) to control access to specific features.

**Mutation Tests**

**A) Registration Logic (Service) {3 tests}**

1. `mutation_registerWithValidPayloadSavesUserAndReturnsResponse`: verifies valid registration normalizes input, hashes password, saves the user, assigns the expected role, and returns the expected response fields.
2. `mutation_registerWithExistingEmailThrowsConflictException`: verifies duplicate email registration throws `EmailAlreadyInUseException`, skips password encoding, and prevents persistence.
3. `mutation_registerWithProfessorRoleSavesProfessorAndReturnsProfessorResponse`: verifies professor registration stores the professor role correctly, normalizes the email, hashes the password, and returns the expected professor response data.

**B) Login/Auth Logic (Service) {9 tests}**

1. `mutation_loginWithValidStudentCredentialsCreatesSessionAndReturnsStudentDashboard`: verifies valid student login creates the session, stores the correct session attributes, and returns the expected student dashboard path.
2. `mutation_loginWithValidProfessorCredentialsCreatesSessionAndReturnsProfessorDashboard`: verifies valid professor login creates the session and returns the expected professor dashboard path.
3. `mutation_loginWithUnknownEmailThrowsInvalidCredentials`: verifies unknown email login throws `InvalidCredentialsException` and does not create a session.
4. `mutation_loginWithWrongPasswordThrowsInvalidCredentials`: verifies invalid password login throws `InvalidCredentialsException` and does not authenticate the user.
5. `mutation_getCurrentSessionWhenAuthenticatedAsStudentReturnsProfile`: verifies an authenticated student session returns the expected current-session profile and dashboard path.
6. `mutation_getCurrentSessionWhenAuthenticatedAsProfessorReturnsProfessorDashboard`: verifies an authenticated professor session returns the expected role-aware dashboard path.
7. `mutation_getCurrentSessionWithInvalidSessionUserIdReturnsEmptyAndInvalidatesSession`: verifies an invalid session user id is handled safely by invalidating the session and returning an empty result.
8. `mutation_getCurrentSessionWithoutSessionReturnsEmpty`: verifies the service returns an empty result when no HTTP session exists.
9. `mutation_logoutInvalidatesSession`: verifies logout invalidates the active session and kills mutants that skip session invalidation.

**Feature-Level Mutation Goal**

- At least 10 non-equivalent mutants should be generated across the selected authentication service scope.
- All non-equivalent mutants should be killed before the feature is considered complete for mutation testing.
- Any surviving mutant must lead to either a stronger test case or a documented equivalent-mutant justification.

### CORE FEATURE 2: Course Creation & Management

A set of tools for Professors to create, manage, and delete courses.

**Mutation Tests**

**A) Course Creation & Management Logic (Service) {20 tests}**

1. `mutation_createWithHttpsLinkSavesLinkAsIs`: verifies a valid `https://` course link is preserved exactly during creation.
2. `mutation_createWithWwwLinkNormalizesToHttps`: verifies a `www.` course link is normalized to `https://...` before save and response mapping.
3. `mutation_createWithHttpLinkThrowsValidationError`: verifies insecure `http://` links are rejected and the course is not saved.
4. `mutation_createWithMalformedLinkThrowsValidationError`: verifies malformed links are rejected and the course is not saved.
5. `mutation_createWithBlankLinkStoresNull`: verifies a blank link is normalized to `null`.
6. `mutation_createWithMissingProfessorThrowsError`: verifies course creation fails when the professor does not exist.
7. `mutation_createWithExistingNonProfessorUserThrowsError`: verifies a non-professor user cannot create a course and the save path is blocked.
8. `mutation_createWithBlankOptionalTextFieldsStoresNulls`: verifies blank optional text fields are normalized to `null`.
9. `mutation_createWithNullOptionalFieldsStoresNulls`: verifies `null` optional fields remain `null` throughout create and response mapping.
10. `mutation_findAllReturnsMappedCourses`: verifies `findAll()` returns mapped course response objects rather than an empty or unmapped result.
11. `mutation_findByUuidReturnsMappedCourse`: verifies `findByUuid()` returns the expected mapped course response.
12. `mutation_findByUuidWhenCourseMissingThrowsNotFound`: verifies a missing UUID throws the expected not-found exception.
13. `mutation_findByProfessorReturnsMappedCourses`: verifies professor-specific retrieval returns the correct mapped courses.
14. `mutation_searchReturnsMappedCourses`: verifies search returns the expected filtered and mapped course results.
15. `mutation_updateCourseAsOwnerUpdatesEditableFieldsAndReturnsResponse`: verifies the course owner can update editable fields and receives the correct updated response.
16. `mutation_updateCourseAsOwnerUpdatesTitleAndCode`: verifies the owner can update course title and code successfully.
17. `mutation_updateCourseAsOwnerBlankOptionalFieldsBecomeNull`: verifies blank optional fields are normalized to `null` during update operations.
18. `mutation_updateCourseAsNonOwnerThrowsForbiddenAndDoesNotSave`: verifies a non-owner professor cannot update another professor's course and no save occurs.
19. `mutation_deleteCourseAsOwnerDeletesCourse`: verifies the owner can delete the course successfully.
20. `mutation_deleteCourseAsNonOwnerThrowsForbiddenAndDoesNotDelete`: verifies a non-owner professor cannot delete another professor's course and no delete occurs.

**Feature-Level Mutation Goal**

- At least 10 non-equivalent mutants should be generated across the selected course-management service scope.
- All non-equivalent mutants should be killed before the feature is considered complete for mutation testing.
- Any surviving mutant must lead to either a stronger test case or a documented equivalent-mutant justification.

### CORE FEATURE 3: Enrollment and Discovery

Functionality for students to browse courses via query/tags, enroll, and access their enrolled courses.

**Mutation Tests**

**A) Enrollment Lifecycle Logic (Service) {14 tests}**

1. `mutation_enrollCreatesNewActiveEnrollmentWithStudentAndCourseIds`: verifies a new enrollment stores the correct student id and course id and is persisted as an active enrollment.
2. `mutation_reactivateInactiveEnrollmentMarksItActiveAndSaves`: verifies an existing inactive enrollment is reactivated and saved rather than duplicated.
3. `mutation_enrollWhenAlreadyActiveThrowsDuplicateErrorAndDoesNotSave`: verifies duplicate active enrollment attempts throw an error and do not save a new or changed enrollment.
4. `mutation_deactivateActiveEnrollmentMarksItInactiveAndSaves`: verifies unenrollment deactivates an active enrollment and persists the updated state.
5. `mutation_deactivateWhenAlreadyInactiveThrowsAndDoesNotSave`: verifies deactivation fails for an already inactive enrollment and does not save.
6. `mutation_deactivateWhenEnrollmentMissingThrowsNotFound`: verifies deactivation throws the expected not-found exception when no enrollment record exists.
7. `mutation_enrollWhenStudentMissingThrowsNotFound`: verifies enrollment fails when the student account does not exist.
8. `mutation_enrollWhenProfessorIdPassedThrowsNoStudentFound`: verifies a professor account cannot be treated as an eligible student enrollment target.
9. `mutation_enrollWhenCourseMissingThrowsNotFound`: verifies enrollment fails when the target course does not exist.
10. `mutation_findByUserReturnsActiveEnrollments`: verifies student-specific enrollment lookup returns the expected active enrollments.
11. `mutation_findByCourseReturnsActiveEnrollments`: verifies course-specific enrollment lookup returns the expected active enrollments.
12. `mutation_isEnrolledReturnsTrueForActiveEnrollment`: verifies active enrollment status is reported as `true`.
13. `mutation_isEnrolledReturnsFalseForInactiveEnrollment`: verifies inactive enrollments are not treated as active access.
14. `mutation_isEnrolledReturnsFalseWhenEnrollmentMissing`: verifies missing enrollment records return `false`.

**B) Discovery and Enrolled-Course Access Logic (Service) {11 tests}**

1. `mutation_findByTagReturnsMatchingCourses`: verifies tag-based course filtering returns only the expected matching courses.
2. `mutation_findMyCoursesReturnsActiveEnrolledCourses`: verifies a student’s active enrollments are mapped into the correct enrolled-course response list.
3. `mutation_findMyCoursesWithNoEnrollmentsReturnsEmptyList`: verifies students with no active enrollments receive an empty course list.
4. `mutation_findMyCoursesWithMissingCoursesStillReturnsEmptySafely`: verifies stale enrollment references do not break the enrolled-course lookup path.
5. `mutation_findByUuidIncludesStudentsInCourseResponse`: verifies a course response includes the mapped enrolled student list and count.
6. `mutation_findByUuidWithMissingStudentInEnrollmentThrowsNotFound`: verifies the service fails safely if an active enrollment points to a missing user record.
7. `mutation_findByUuidReflectsEnrollmentInCourseStudents`: verifies active enrollment data is reflected in the course’s returned student list.
8. `mutation_findByUuidWithNoEnrollmentsReturnsEmptyStudentsList`: verifies courses with no active enrollments return an empty student list and zero enrolled count.
9. `mutation_findByUuidWithMultipleStudentsReturnsAllStudents`: verifies multiple active enrollments are fully reflected in the returned course response.
10. `mutation_ensureStudentEnrolledAllowsActiveEnrollment`: verifies enrolled students pass the course-access guard.
11. `mutation_ensureStudentEnrolledRejectsMissingEnrollment`: verifies unenrolled students are blocked by the course-access guard with the expected exception.

**Feature-Level Mutation Goal**

- At least 10 non-equivalent mutants should be generated across the selected enrollment and discovery service scope.
- All non-equivalent mutants should be killed before the feature is considered complete for mutation testing.
- Any surviving mutant must lead to either a stronger test case or a documented equivalent-mutant justification.

**Feature-Level Mutation Result**

- PIT scope: `EnrollmentService` and the enrollment/discovery paths in `CourseService`.
- Final result: `70/70` mutants killed.
- Final mutation score: `100%`.
- Final test strength: `100%`.

## 2.1 Mutation Test Execution Approach

- Mutation testing will be run locally from the backend module using PIT Maven goals.
- Feature-level runs should keep `targetClasses` and `targetTests` narrow so that reports are easier to interpret and document.
- Surviving mutants must be reviewed before new tests are added so the team can determine whether the failure is due to a real gap or an equivalent mutant.
- Mutation testing should be repeated after test improvements until the selected feature reaches the required non-equivalent mutant kill rate.

### 2.1.1 Tooling

- PIT (`pitest-maven`) for mutation generation and execution.
- `pitest-junit5-plugin` for JUnit 5 support.
- JUnit 5 for backend unit testing.
- Mockito for isolating service dependencies.
- GitHub Actions for CI execution and artifact upload of mutation reports.

### 2.1.2 CI/CD Mutation Workflow

- The repository includes mutation-testing support in `backend/pom.xml`.
- The repository also includes a GitHub Actions workflow at `.github/workflows/mutation.yml`.
- The workflow compiles backend tests, runs PIT for the configured target classes, and uploads the HTML mutation report as a build artifact.
- Mutation workflow scope can be adjusted per feature so that the selected classes in CI match the feature currently being analyzed and documented.

## 2.2 Mutation Test Level Summary

| Test Level | Scope & Requirement | Methodology (How will you do this?) |
| --- | --- | --- |
| Mutation Testing | At least 10 non-equivalent mutants for each selected feature-specific service scope. Target outcome: 100% kill rate for non-equivalent mutants. | PIT will generate mutants for backend service classes. JUnit 5 and Mockito-based unit tests will be executed against those mutants. Surviving mutants will be analyzed, stronger tests will be added, and PIT will be re-run until the required result is achieved. |
| Regression Mutation Testing | Mutation checks should be reproducible before merge/release and available for review in CI artifacts. | The team will use the configured Maven/PIT setup and GitHub Actions workflow to execute mutation runs for selected backend service classes and review the generated HTML report. |

## 3 Terms/Acronyms

| TERM/ACRONYM | DEFINITION |
| --- | --- |
| PIT | A mutation testing tool for Java that generates code mutants and evaluates whether the test suite detects them. |
| AUT | Application Under Test. |
| Mutant | A modified version of the original program created to simulate a small fault. |
| Killed Mutant | A mutant detected by one or more failing tests. |
| Survived Mutant | A mutant that does not cause any related test to fail. |
| Equivalent Mutant | A mutant that changes the code syntactically but not the observable behavior. |
| CI/CD | Continuous Integration / Continuous Delivery. |
