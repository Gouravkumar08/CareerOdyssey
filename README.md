# CareerOdyssey

A personal career-ops platform: candidate profiles, companies, cold-email outreach via Gmail
SMTP with resume attachments, a DSA practice tracker with sheets/progress/planner, and more to
come (automated job discovery). Spring Boot backend backed by MySQL, React/Vite frontend.

## Stack

- Java 21, Spring Boot 4
- Spring Web MVC, Spring Data JPA, Thymeleaf, Spring Mail, Bean Validation
- MySQL
- Maven

## Running locally

1. Have MySQL running locally (default expected at `localhost:3306`, database `outreach_db`
   auto-created on first connect).
2. Optionally override DB credentials (defaults to `root`/`root`):

   ```bash
   export DB_USERNAME=root
   export DB_PASSWORD=root
   ```

3. Run the app:

   ```bash
   ./mvnw spring-boot:run
   ```

4. Open `http://localhost:8080` — redirects to the Outreach UI.

Gmail credentials are **not** set via environment variables — each candidate profile stores its
own Gmail address + [app password](https://myaccount.google.com/apppasswords) (never the real
account password), and outreach for that candidate is sent from their own account. Set these on
the "Gmail sending credentials" section of the candidate form at `/users/new` or `/users/{id}/edit`.
There is currently no login for this app — anyone who can reach it can read/write any candidate's
data, so only run this against a local/trusted MySQL instance, not a shared or internet-exposed one.

## MVP flow

1. Create a candidate at `/users/new` (or `POST /api/users`), including their Gmail address and
   app password.
2. Create a company at `/companies/new` (or `POST /api/companies`).
3. Send a cold email — from `/outreach/new` you can pick one company (single send) or several at
   once (bulk send to the same candidate/template), optionally attaching a resume file for just
   this send. Via the API it's a multipart request (not JSON), since it accepts an optional file:

   ```bash
   curl -X POST http://localhost:8080/api/outreach/send \
     -F userId=1 \
     -F companyIds=1 -F companyIds=2 \
     -F templateId=1 \
     -F resumeFile=@/path/to/resume.pdf
   ```

   This fetches the candidate and each company, auto-resolves the recipient (HR email if set,
   otherwise career email), renders the template with candidate + company details, attaches
   `resumeFile` if provided or falls back to the candidate's default resume
   (`app.resume.path`/`app.resume.filename`), sends via the candidate's own Gmail SMTP
   credentials, and persists one `Outreach` record per company regardless of outcome
   (`SENT` or `FAILED`).
4. From the Outreach History page, each record has **Resend** and **Follow up** actions — both
   open an editable copy of the send form (same candidate/company/job details, template swapped
   to the follow-up template for "Follow up") so you can tweak anything, or attach a different
   resume, before it actually sends. Nothing sends until you submit that form.

## API & flow documentation

See [API_DOCUMENTATION.md](API_DOCUMENTATION.md) for full request/response payloads for every
endpoint, the exact send-flow logic behind `POST /api/outreach/send`, and how the Thymeleaf
pages work in the browser. Note: that document predates the per-candidate Gmail credentials and
bulk-send changes described above — treat this README as the source of truth for those until it's
refreshed.

## Notes

- The bundled resume PDF is a placeholder — swap it out, or point `app.resume.path`/`app.resume.filename`
  in `application.yml` at a different classpath/file location.
- `User.resumeDriveLink` can hold a Google Drive link to the resume as an alternative/companion
  to the attached file; it is not yet used for sending, but is rendered on the candidate view page.
- `email_templates` rows are seeded via `data.sql`; template bodies live as Thymeleaf views
  under `src/main/resources/templates/email/`.
- Deleting a candidate or company that already has outreach history is blocked with a clear
  error (foreign key), both from the UI and the API (`409 Conflict`) — delete the outreach
  records first if you really want to remove one.
