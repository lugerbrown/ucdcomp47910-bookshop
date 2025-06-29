-- SQL Script to create an Admin user
-- This script creates an admin user in both the users and admins tables
-- due to the JOINED inheritance strategy used in the JPA entities

-- First, insert into the base users table
-- Username: admin
-- Password: password (hashed using bcrypt) (https://bcrypt-generator.com/)
INSERT INTO users (username, password, role)
VALUES ('admin', '$2a$12$PPj/kgR5Ssm2ZJirdV1zWOM5N/zGIUn5GWpeISyaMZ6b7oeoo8012', 'ADMIN');

-- Then, insert into the admins table with the same ID
-- Note: The ID should match the one generated in the users table
-- You may need to adjust the ID value based on your database's auto-increment sequence
INSERT INTO admins (id)
VALUES (LAST_INSERT_ID());

-- Alternative approach if LAST_INSERT_ID() doesn't work in your database:
-- First find the max ID from users table and use it
-- INSERT INTO admins (id)
-- VALUES ((SELECT MAX(id) FROM users WHERE username = 'admin'));

-- Verify the admin user was created correctly
SELECT u.id, u.username, u.role, a.id as admin_id
FROM users u
LEFT JOIN admins a ON u.id = a.id
WHERE u.username = 'admin';
