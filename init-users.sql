-- Quick user setup for demo
INSERT INTO users (id, username, email, password, full_name, role, student_id, created_at, updated_at) VALUES
(1, 'teacher@uni.edu', 'teacher@uni.edu', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Teacher Zhang', 'TEACHER', NULL, NOW(), NOW()),
(2, 'student@uni.edu', 'student@uni.edu', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Student Li', 'STUDENT', 'SID2023001', NOW(), NOW()),
(3, 'student2@uni.edu', 'student2@uni.edu', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Student Wang', 'STUDENT', 'SID2023002', NOW(), NOW()),
(4, 'admin@uni.edu', 'admin@uni.edu', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Admin User', 'ADMIN', NULL, NOW(), NOW());

-- Assignments
INSERT INTO assignments (id, title, description, teacher_id, course_code, due_date, total_marks, instructions, status, created_at, updated_at) VALUES
(1, 'Algorithm Design - Assignment 1', 'Quick sort algorithm implementation', 1, 'COMP5005', NOW() + INTERVAL '7 days', 100, 'Please submit complete implementation', 'PUBLISHED', NOW(), NOW()),
(2, 'Data Structure - Assignment 2', 'Binary search tree implementation', 1, 'COMP5006', NOW() + INTERVAL '14 days', 100, 'Implement insert, delete, search operations', 'PUBLISHED', NOW(), NOW());

-- Rubrics (Grading Criteria)
INSERT INTO rubrics (id, assignment_id, question_id, criteria, weight, key_points, sample_answer, question_type, created_at, updated_at) VALUES
(1, 1, 'Q1', 'Algorithm Implementation', 50, 'Correct partition function\nCorrect recursive logic\nHandle edge cases', 'Implement partition and quickSort functions correctly', 'SUBJECTIVE', NOW(), NOW()),
(2, 1, 'Q2', 'Complexity Analysis', 30, 'Time complexity analysis\nSpace complexity analysis\nBest/worst case discussion', 'Average O(n log n), worst O(n^2)', 'SUBJECTIVE', NOW(), NOW()),
(3, 1, 'Q3', 'Code Quality', 20, 'Code readability\nVariable naming\nComments', 'Clear function structure and naming', 'SUBJECTIVE', NOW(), NOW()),
(4, 2, 'Q1', 'BST Implementation', 60, 'Correct insert operation\nCorrect delete operation\nCorrect search operation', 'Implement BST with all operations', 'SUBJECTIVE', NOW(), NOW()),
(5, 2, 'Q2', 'Code Quality', 40, 'Code structure\nError handling\nTest cases', 'Well-structured code with tests', 'SUBJECTIVE', NOW(), NOW());

-- Set sequences
SELECT setval('users_id_seq', 10);
SELECT setval('assignments_id_seq', 10);
SELECT setval('rubrics_id_seq', 10);

