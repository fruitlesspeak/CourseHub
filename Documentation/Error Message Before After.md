# Error Message Before vs After

This document captures the user-facing error message updates implemented on the `fix/improve-user-facing-errors` branch.

## Frontend Messages

| Error case | Before | After |
|---|---|---|
| Backend connection failed with non-OK response | `HTTP <status>` | `We couldn't reach the server. Please try again.` |
| Backend connection failed unexpectedly | `Unknown error` | `Something went wrong while contacting the server. Please try again.` |
| Backend connection fallback message slot | `Failed to load` | `Unable to load data right now.` |
| Login form incomplete | `Email and password are required.` | `Email and password are required.` |
| Login rejected | `Invalid email or password.` | `Invalid email or password.` |
| Login validation error from API | `Please verify your input and try again.` | Field-specific validation message when available, for example `Email is required.`, `Password is required.`, or `Enter a valid email address.` Fallback: `Please review your email and password and try again.` |
| Login server or network failure | `Unable to sign in right now. Please try again.` | `We couldn't sign you in right now. Please try again in a moment.` |
| Register missing first name | `First name is required.` | `First name is required.` |
| Register missing last name | `Last name is required.` | `Last name is required.` |
| Register missing email | `Email is required.` | `Email is required.` |
| Register short password | `Password must be at least 8 characters.` | `Password must be at least 8 characters.` |
| Register password mismatch | `Passwords do not match.` | `Passwords do not match.` |
| Register missing role | `Role is required.` | `Role is required.` |
| Register duplicate email | `Email already in use.` | `An account with this email already exists. Try signing in instead.` |
| Register validation error from API | `Invalid input. Please check your fields.` | Field-specific validation message when available, for example `First name is required.`, `Email is required.`, or `Password must be between 8 and 72 characters.` Fallback: `Please review your details and try again.` |
| Register failed | `Registration failed. Try again.` | `We couldn't create your account right now. Please try again.` |
| Automatic sign-in after registration failed | `Account created, but automatic sign-in failed. Please sign in.` | `Your account was created, but we couldn't sign you in automatically. Please sign in.` |
| Course catalog requires sign-in | `You must be logged in to browse courses.` | `Please sign in to browse courses.` |
| Course catalog load failed | `Failed to load courses. Please try again.` | `We couldn't load the course list. Please try again.` |
| Enrollment already exists | `You are already enrolled in this course.` | `You are already enrolled in this course.` |
| Enrollment requires sign-in | `Please log in to enroll in courses.` | `Please log in to enroll in courses.` |
| Enrollment blocked by role | `You are not authorized to enroll in this course.` | `Only students can enroll in courses.` |
| Enrollment target missing | `Course not found — it may have been removed.` | `Course not found — it may have been removed.` |
| Enrollment generic failure | `Enrollment failed. Please try again.` | `We couldn't enroll you in this course. Please try again.` |
| Course detail missing | `Course not found.` | `Course not found.` |
| Course detail load failed | `Failed to load course. Please try again.` | `We couldn't load this course right now. Please try again.` |
| Course detail enroll failed | `Enrollment failed. Please try again.` | `We couldn't enroll you in this course. Please try again.` |
| Professor course link must start with secure URL | `Link must start with https:// or www.` | `Enter a valid course link starting with https:// or www.` |
| Professor course link invalid | `Link is invalid.` | `Enter a valid course link.` |
| Professor course due date invalid | `Due date is invalid.` | `Enter a valid due date and time.` |
| Professor course save failed | `Unable to save this course right now.` | `We couldn't save this course right now. Please try again.` |
| Important date due date invalid | `Due date is invalid.` | `Enter a valid due date and time.` |
| Important date added before course exists | `Course is not ready yet.` | `Save the course before adding important dates.` |
| Important date not found locally | `Important date not found.` | `Important date not found.` |
| Important date save failed | `Unable to save this important date right now.` | `We couldn't save this important date right now. Please try again.` |
| Important date delete failed | `Unable to delete this important date right now.` | `We couldn't delete this important date right now. Please try again.` |
| Edit-course screen load failed | `Unable to load course details.` | `We couldn't load this course right now. Please try again.` |
| Professor dashboard delete failed | `Unable to delete this course right now.` | `We couldn't delete this course right now. Please try again.` |
| Student dashboard drop failed | `Failed to drop course.` | `We couldn't drop this course right now. Please try again.` |
| Generic course-store load failure | `Failed to load courses.` | `We couldn't load courses right now. Please try again.` |
| Generic important-date-store load failure | `Failed to load important dates.` | `We couldn't load important dates right now. Please try again.` |
| Generic user-store load failure | `Failed to load users.` | `We couldn't load users right now. Please try again.` |
| Admin course form generic failure | `An error occurred.` | `We couldn't save this course right now. Please review the details and try again.` |
| Admin course delete failure | `Delete failed.` | `We couldn't delete this course right now. Please try again.` |
| Admin user form generic failure | `An error occurred.` | `We couldn't save this user right now. Please review the details and try again.` |
| Admin user delete failure | `Delete failed.` | `We couldn't delete this user right now. Please try again.` |

## Backend or API Messages Surfaced in the UI

These messages are returned by the backend and may be shown directly by extractor-based frontend views.

| Error case | Before | After |
|---|---|---|
| Authentication required | `Authentication required.` | `Please sign in to continue.` |
| Spring default `401` payload shown in extractor-based views | `Unauthorized` | `Please sign in to continue.` |
| Wrong role for course management | `Only professors can manage courses.` | `Only professors can create, update, or delete courses.` |
| Wrong role for important date management | `Only professors can manage important dates.` | `Only professors can add, update, or delete important dates.` |
| Wrong role for enrollment | `Only students can enroll.` | `Only students can enroll in courses.` |
| Access to course materials blocked because student is not enrolled | `You must be enrolled to access course materials.` | `You must be enrolled to access course materials.` |
| Professor editing another professor's course | `You can only modify your own courses.` | `You can only edit or delete courses you created.` |
| Professor editing another professor's important dates | `You can only manage important dates for your own courses.` | `You can only manage important dates for courses you created.` |
| Accessing another user's profile | `You can only access your own user profile.` | `You can only view or update your own profile.` |
| User creation blocked outside registration | `User creation is only available through registration.` | `Accounts can only be created through the sign-up page.` |
| User listing blocked | `User listing is not available.` | `You do not have access to view the user list.` |
| User deletion blocked | `User deletion is not available.` | `User accounts cannot be deleted here.` |
| Missing course resource | `Course not found: <uuid/id>` | `This course could not be found.` |
| Missing important date resource | `Important date not found: <id>` | `This important date could not be found.` |
| Missing user resource | `User not found: <uuid>` | `This user could not be found.` |
| Missing professor reference | `No professor found with id: <id>` | `The selected professor could not be found.` |
| Invalid course link prefix | `Course link must start with https:// or www.` | `Enter a valid course link starting with https:// or www.` |
| Invalid course link format | `Course link is invalid.` | `Enter a valid course link.` |
| Duplicate email from backend | `Email already in use` or `Email already in use: <email>` | `An account with this email already exists.` |
| Invalid credentials | `Invalid email or password.` | `Invalid email or password.` |
| Professor account assigned a student ID | `A professor cannot have a student_id.` | `A professor account cannot have a student ID.` |
| Already enrolled conflict | `User already enrolled in this course` | `You are already enrolled in this course.` |
| Already dropped enrollment conflict | `Enrollment already inactive` | `You have already dropped this course.` |
| Missing enrollment record | `Enrollment not found` | `This enrollment could not be found.` |
| Missing student account | `No student found with id: <id>` | `This student account could not be found.` |

## Validation Message Examples

The backend now returns more user-friendly field-level validation messages instead of generic framework defaults in several places.

| Validation field | Before | After |
|---|---|---|
| Blank course title | `must not be blank` | `Title is required.` |
| Blank course code | `must not be blank` | `Code is required.` |
| Blank important-date title | `must not be blank` | `Title is required.` |
| Missing important-date due date | `must not be null` | `Due date is required.` |
| Blank login email | `must not be blank` | `Email is required.` |
| Blank login password | `must not be blank` | `Password is required.` |
| Invalid email | default framework email message | `Enter a valid email address.` |
| Blank register first name | `must not be blank` | `First name is required.` |
| Blank register last name | `must not be blank` | `Last name is required.` |
| Blank register email | `must not be blank` | `Email is required.` |
| Missing register role | `must not be null` | `Role is required.` |

