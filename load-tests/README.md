# Load Tests

These assets are the first implementation pass for the CourseHub load testing plan and are intended to be developed on the dedicated `load-testing` branch.

## Structure

- `jmeter/auth.jmx`: guest registration, login, session lookup, logout
- `jmeter/course-management.jmx`: professor register/login, course CRUD, important date CRUD, logout
- `jmeter/discovery-enrollment.jmx`: professor creates a course, student discovers it, enrolls, accesses content, drops it, and logs out
- `jmeter/mixed-baseline-200rpm.jmx`: sustained mixed workload that targets 20 total virtual users and 200 requests per minute for at least 60 seconds
- `data/course-seeds.csv`: rotating course title/code/tag seed data

## Prerequisites

- JMeter 5.6+ installed locally
- CourseHub backend running on `http://localhost:8080`
- Database initialized through the existing Docker/Postgres setup

## Notes

- The current JMeter plans are self-seeding for the primary flows. They create their own users and, where needed, their own courses, so they do not depend on hard-coded UUIDs.
- The student discovery plan creates a course during the same thread flow so the student can exercise search, enrollment, and content access against known data.
- Output files should go under `load-tests/results/`, which is gitignored.
- Dashboard-only and endurance plans are still pending. This first implementation covers the three highest-signal areas from the load testing plan.
- The mixed baseline plan is the first explicit acceptance-style check for the target of `20` virtual users generating `200` requests per minute. It is intentionally fixed at `2` auth users / `20 RPM`, `5` professor users / `50 RPM`, and `13` student/discovery users / `130 RPM`, which sums to `20` users and `200 RPM` for `60` seconds, with a `1` second ramp so the minute is spent under sustained load instead of startup.

## Example Commands

Run from the project root.

```powershell
jmeter -n -t load-tests/jmeter/auth.jmx -l load-tests/results/auth.jtl -e -o load-tests/results/auth-report
```

```powershell
jmeter -n -t load-tests/jmeter/course-management.jmx -l load-tests/results/course-management.jtl -e -o load-tests/results/course-management-report
```

```powershell
jmeter -n -t load-tests/jmeter/discovery-enrollment.jmx -l load-tests/results/discovery-enrollment.jtl -e -o load-tests/results/discovery-enrollment-report
```

```powershell
jmeter -n -t load-tests/jmeter/mixed-baseline-200rpm.jmx -l load-tests/results/mixed-baseline-200rpm.jtl -e -o load-tests/results/mixed-baseline-200rpm-report
```

## Useful Overrides

Each plan supports JMeter property overrides for host, port, protocol, and thread settings.

```powershell
jmeter -n -t load-tests/jmeter/auth.jmx `
  -Jprotocol=http `
  -Jhost=localhost `
  -Jport=8080 `
  -Jauth.threads=20 `
  -Jauth.ramp=20 `
  -Jauth.loops=5 `
  -l load-tests/results/auth.jtl
```

```powershell
jmeter -n -t load-tests/jmeter/course-management.jmx `
  -Jcourse.threads=10 `
  -Jcourse.ramp=15 `
  -Jcourse.loops=3 `
  -l load-tests/results/course-management.jtl
```

```powershell
jmeter -n -t load-tests/jmeter/discovery-enrollment.jmx `
  -Jdiscovery.threads=25 `
  -Jdiscovery.ramp=20 `
  -Jdiscovery.loops=4 `
  -l load-tests/results/discovery-enrollment.jtl
```

## Verification Status

- The `.jmx` files have been written against the current backend API contract in this repository.
- XML structure can be validated locally without JMeter.
- JMeter execution itself has not yet been verified in this workspace because JMeter is not part of the repository.
- The mixed baseline plan is configured to enforce the `20 users / 200 RPM / 60 seconds` target by configuration rather than inferring it from completed sample counts. The RPM split is fixed in the plan at `20 + 50 + 130 = 200 RPM`.
