# API & Flow Documentation

Full request/response payloads for every endpoint, plus how the Thymeleaf pages work in the
browser. All REST endpoints are under `/api/**` and return/accept JSON; the Thymeleaf pages are
plain server-rendered HTML under `/users`, `/companies`, `/outreach`.

## Entities at a glance

```text
User (candidate)  1 ---- N  Outreach  N ---- 1  Company
                              |
                              +-- templateId --> EmailTemplate
```

- **User**: the candidate an outreach email is sent on behalf of.
- **Company**: the target org, with optional `hrEmail` and `careerEmail`.
- **EmailTemplate**: a Thymeleaf view name (`viewName`) + a subject line with `{{placeholders}}`
  (`subjectTemplate`). Rows are seeded via `data.sql`.
- **Outreach**: one send attempt — always created, whether the send succeeds or fails.

---

## 1. Users API — `/api/users`

Candidate profile CRUD.

### `POST /api/users` — create a candidate

Request:
```json
{
  "name": "Gourav Kumar",
  "email": "gourav@example.com",
  "phone": "+91-9876543210",
  "location": "Bengaluru, India",
  "linkedinUrl": "https://linkedin.com/in/gourav",
  "githubUrl": "https://github.com/Gouravkumar08",
  "portfolioUrl": "https://gourav.dev",
  "experience": "3 years as a backend engineer, Java/Spring",
  "skills": "Java, Spring Boot, MySQL, Kafka",
  "education": "B.Tech CS, 2022",
  "noticePeriod": "30 days",
  "expectedSalary": "18 LPA",
  "resumePath": "classpath:resume/resume.pdf",
  "resumeDriveLink": "https://drive.google.com/file/d/xxxx/view"
}
```
Only `name` and `email` are required (Bean Validation `@NotBlank`/`@Email`). Everything else is optional.

Response — `201 Created`:
```json
{
  "id": 1,
  "name": "Gourav Kumar",
  "email": "gourav@example.com",
  "phone": "+91-9876543210",
  "location": "Bengaluru, India",
  "linkedinUrl": "https://linkedin.com/in/gourav",
  "githubUrl": "https://github.com/Gouravkumar08",
  "portfolioUrl": "https://gourav.dev",
  "experience": "3 years as a backend engineer, Java/Spring",
  "skills": "Java, Spring Boot, MySQL, Kafka",
  "education": "B.Tech CS, 2022",
  "noticePeriod": "30 days",
  "expectedSalary": "18 LPA",
  "resumePath": "classpath:resume/resume.pdf",
  "resumeDriveLink": "https://drive.google.com/file/d/xxxx/view",
  "createdAt": "2026-08-19T08:00:00Z",
  "updatedAt": "2026-08-19T08:00:00Z"
}
```
- `400` if `name`/`email` missing or `email` malformed — body is a field-error map (see [Error shape](#error-response-shape)).

### `GET /api/users/{id}` — fetch one
Response `200` — same shape as create response. `404` if the id doesn't exist.

### `GET /api/users` — list all
`200` — a JSON array of the same shape, unpaginated.

### `PUT /api/users/{id}` — update
Same request body as create (full replace, not a partial patch). `200` with the updated record, or `404`/`400`.

### `DELETE /api/users/{id}` — delete
`204 No Content` on success, `404` if missing. Fails at the DB level if any `Outreach` row still
references this user (the foreign key isn't cascading).

---

## 2. Companies API — `/api/companies`

Company/contact CRUD, plus a filtered email lookup.

### `POST /api/companies` — create a company
Request:
```json
{
  "companyName": "Acme Corp",
  "location": "Bengaluru, India",
  "website": "https://acme.com",
  "careerSite": "https://acme.com/careers",
  "careerEmail": "careers@acme.com",
  "hrEmail": "hr@acme.com",
  "hrName": "Priya Sharma",
  "companyDescription": "B2B SaaS company building developer tools.",
  "linkedinUrl": "https://linkedin.com/company/acme",
  "notes": "Referred by a friend, prefers concise emails"
}
```
Only `companyName` is required. `careerEmail`/`hrEmail` are validated as emails if present, but a
company can legally have neither set — it just means any outreach sent to it will fail cleanly
(see the Outreach flow below).

Response — `201 Created`: same shape plus `id`, `createdAt`, `updatedAt`.

### `GET /api/companies/{id}` / `GET /api/companies` / `PUT /api/companies/{id}` / `DELETE /api/companies/{id}`
Mirror the Users API exactly (same status codes, same full-replace semantics on `PUT`, same FK
restriction on `DELETE` if `Outreach` rows reference the company).

### `GET /api/companies/emails` — filtered contact lookup
Query params (all optional, combine with AND):

| Param | Type | Meaning |
|---|---|---|
| `location` | string | case-insensitive exact match on `location` |
| `hasHrEmail` | boolean | `true` = only companies with a non-null `hrEmail`, `false` = only companies missing one |
| `hasCareerEmail` | boolean | same, for `careerEmail` |

Example: `GET /api/companies/emails?location=Bengaluru&hasHrEmail=true`

Response `200` — slim projection, not the full company:
```json
[
  { "id": 25, "companyName": "Acme Corp", "location": "Bengaluru, India", "careerEmail": "careers@acme.com", "hrEmail": "hr@acme.com" }
]
```

---

## 3. Outreach API — `/api/outreach` (the core send flow)

### `POST /api/outreach/send` — send a cold email

Request:
```json
{
  "userId": 1,
  "companyId": 25,
  "recipientType": "HR",
  "templateId": 1,
  "jobTitle": "Backend Engineer",
  "jobUrl": "https://acme.com/careers/backend-engineer"
}
```
`userId`, `companyId`, `recipientType` (`HR` or `CAREER`), and `templateId` are required.
`jobTitle`/`jobUrl` are optional and just get interpolated into the subject/body if present.

**Step-by-step, inside `OutreachService.send`:**

```text
POST /api/outreach/send
    |
    v
Load User by userId ---------------------------> 404 if missing
    |
Load Company by companyId ----------------------> 404 if missing
    |
Load EmailTemplate by templateId ---------------> 404 if missing
    |
Resolve recipient email:
    recipientType == HR      -> company.hrEmail
    recipientType == CAREER  -> company.careerEmail
    |
    +-- blank/missing? -----> save Outreach{status=FAILED, notes="No valid ... email configured"}
    |                         and RETURN (never attempts to send)
    v
Render subject: EmailTemplate.subjectTemplate, {{placeholders}} filled from
    candidateName / companyName / hrName / jobTitle
    |
Render HTML body: Thymeleaf view named by EmailTemplate.viewName (e.g. "email/cold-email"),
    context = candidate fields + company fields + jobTitle/jobUrl
    |
Build MimeMessage, attach resume.pdf via MimeMessageHelper
    |
JavaMailSender.send() over Gmail SMTP
    |
    +-- success --> Outreach{status=SENT, sentAt=now}
    +-- throws  --> Outreach{status=FAILED, notes="Send failed: <exception message>"}
    |
Save Outreach row (always happens, regardless of branch taken above)
    |
    v
Return 200 with the saved Outreach record
```

Response — `200 OK` (note: **200 even on a failed send** — the HTTP call itself succeeded in
recording the attempt; check the `status` field to know if the email actually went out):
```json
{
  "id": 101,
  "userId": 1, "userName": "Gourav Kumar",
  "companyId": 25, "companyName": "Acme Corp",
  "recipientEmail": "hr@acme.com", "recipientName": "Priya Sharma",
  "recipientType": "HR", "templateId": 1,
  "subject": "Application for Backend Engineer at Acme Corp",
  "jobTitle": "Backend Engineer", "jobUrl": "https://acme.com/careers/backend-engineer",
  "status": "SENT",
  "sentAt": "2026-08-19T08:15:00Z",
  "replyReceived": false, "repliedAt": null, "notes": null,
  "createdAt": "2026-08-19T08:15:00Z", "updatedAt": "2026-08-19T08:15:00Z"
}
```

Failure example (no HR email on the company) — still `200 OK`:
```json
{
  "id": 102,
  "userId": 1, "userName": "Gourav Kumar",
  "companyId": 30, "companyName": "Beta Inc",
  "recipientEmail": "", "recipientName": null,
  "recipientType": "HR", "templateId": 1,
  "subject": null, "jobTitle": null, "jobUrl": null,
  "status": "FAILED",
  "sentAt": null,
  "replyReceived": false, "repliedAt": null,
  "notes": "No valid HR email configured for company 30",
  "createdAt": "...", "updatedAt": "..."
}
```

`404` if `userId`/`companyId`/`templateId` don't exist. `400` if required fields are missing from the request.

### `GET /api/outreach/{id}` — fetch one outreach record
`200` with the same shape as above, `404` if missing.

### `GET /api/outreach` — search/filter history
All query params optional, combine with AND:

| Param | Type | Meaning |
|---|---|---|
| `userId` | long | outreach sent for this candidate |
| `companyId` | long | outreach sent to this company |
| `status` | enum | `DRAFT` \| `SENT` \| `REPLIED` \| `NO_REPLY` \| `FAILED` |
| `recipientType` | enum | `HR` \| `CAREER` |
| `replyReceived` | boolean | whether a reply has come back |
| `sentFrom` | ISO-8601 instant | lower bound on `sentAt`, e.g. `2026-08-01T00:00:00Z` |
| `sentTo` | ISO-8601 instant | upper bound on `sentAt` |

Example: `GET /api/outreach?companyId=25&status=SENT&sentFrom=2026-08-01T00:00:00Z`

Response `200` — array of the same shape as the send response.

---

## Error response shape

Thrown by `GlobalExceptionHandler` for `ResourceNotFoundException` (404), `InvalidRecipientException`
(422 — not currently thrown by the send flow, reserved for future use), and validation errors (400):
```json
{
  "timestamp": "2026-08-19T08:15:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": { "email": "must be a well-formed email address" }
}
```
`fieldErrors` is `null` for non-validation errors (404/422), which just carry a plain `message`.

---

## 4. The Thymeleaf UI (browser flow)

No JS framework — every interaction is a full page load or form `POST`; the server renders HTML
each time via the same services the REST API calls.

```text
GET /                     -> redirect -> GET /outreach

GET /users                -> table of all candidates
    each row links to     -> GET /users/{id}            (read-only view)
                                links to -> GET /users/{id}/edit  (form, pre-filled)
                                                POST /users/{id}  -> update, redirect to /users/{id}
GET /users/new            -> empty form
                                POST /users               -> create, redirect to /users/{newId}

GET /companies            -> same list/view/edit/new pattern as Users

GET /outreach              -> table of outreach history (candidate, company, recipient, status, sentAt)
GET /outreach/new          -> send form:
                                 dropdowns: Candidate, Company, Recipient Type, Email Template
                                 optional: Job Title, Job URL
                                 button "Send Email" -> POST /outreach/send
                                     -> calls OutreachService.send() (same as the REST API)
                                     -> re-renders the SAME form with a result banner:
                                          green if status == SENT, red if status == FAILED
                                     -> you can immediately send another without navigating away
```

Validation errors on the Users/Companies forms re-render the same form with inline error state
(via Spring's `BindingResult`) instead of a JSON error body — otherwise behavior (what's required,
what 404s) is identical to the REST API, since both layers call the same service classes.

---

## Running the app / trying it out

See [README.md](README.md) for local setup (MySQL, Gmail app password env vars, `./mvnw spring-boot:run`).
Once running:
- Browser UI: `http://localhost:8080/`
- REST API base: `http://localhost:8080/api`
