-- Inserisci gli utenti allineati con Keycloak
INSERT INTO users (id, full_name, email, organization, role, department, storage_quota, notes, created_at, updated_at)
VALUES
    ('a0864d47-95f9-4928-a9b8-6a6a89cd35ce', 'Alice Rossi', 'alice_manager@email.com', 'MyCompany', 'EDITOR', 'Management', NULL, NULL, NOW(), NOW()),
    ('84a93fae-27d1-4998-86b9-2f7f830b920b', 'Bob Verdi', 'bob_admin@email.com', 'MyCompany', 'ADMIN', 'Organization', NULL, NULL, NOW(), NOW()),
    ('037e908a-f967-498d-a42a-4e80999dc0cd', 'Jack Bianchi', 'jack_staff@email.com', 'MyCompany', 'VIEWER', 'Management', NULL, NULL, NOW(), NOW()),
    ('f875e228-4001-41d1-be8d-1175128bcaa2', 'Alessia Bruno', 'alessia_staff@email.com', 'MyCompany', 'VIEWER', 'Engineering', NULL, NULL, NOW(), NOW()),
    ('9c7dbfef-9fd6-41fb-aa81-e652930aa0b5', 'Emiliana Neri', 'emiliana_manager@email.com', 'MyCompany', 'EDITOR', 'Engineering', NULL, NULL, NOW(), NOW()),
    ('21ebf766-9594-40d4-8c1f-d8b49cc510a8', 'Omar Gialli', 'omar_manager@email.com', 'MyCompany', 'EDITOR', 'Management', NULL, NULL, NOW(), NOW())

ON CONFLICT (id) DO NOTHING;
