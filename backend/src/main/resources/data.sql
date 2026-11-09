INSERT INTO email_templates (id, name, view_name, subject_template, active, follow_up, created_at, updated_at)
SELECT 1, 'Standard Cold Outreach', 'email/cold-email', 'Application for {{jobTitle}}', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM email_templates WHERE id = 1);

INSERT INTO email_templates (id, name, view_name, subject_template, active, follow_up, created_at, updated_at)
SELECT 2, 'Direct Introduction', 'email/cold-email', 'Interested in opportunities - {{candidateName}}', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM email_templates WHERE id = 2);

INSERT INTO email_templates (id, name, view_name, subject_template, active, follow_up, created_at, updated_at)
SELECT 3, 'Follow-up', 'email/follow-up-email', 'Following up: {{jobTitle}}', true, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM email_templates WHERE id = 3);

INSERT INTO email_templates (id, name, view_name, subject_template, active, follow_up, created_at, updated_at)
SELECT 4, 'Specific Opening', 'email/specific-opening-email', 'Application for {{jobTitle}}', true, false, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM email_templates WHERE id = 4);
