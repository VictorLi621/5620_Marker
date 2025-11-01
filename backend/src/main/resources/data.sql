-- Seed data for demo

-- Users (password is "password" - BCrypt encrypted)
-- Use ON CONFLICT to ensure repeatable deployment during demos and auto-reset passwords
-- ✅ Using verified BCrypt hash
INSERT INTO users (id, username, email, password, full_name, role, student_id, created_at, updated_at) VALUES
(1, 'teacher@uni.edu', 'teacher@uni.edu', '$2a$10$xw/.7ZSk7ToNXeJ9Kp5bdO9q26AhORDntiUNLaSMow0jmgCY12TVG', 'Prof. Zhang', 'TEACHER', NULL, NOW(), NOW()),
(2, 'student@uni.edu', 'student@uni.edu', '$2a$10$xw/.7ZSk7ToNXeJ9Kp5bdO9q26AhORDntiUNLaSMow0jmgCY12TVG', 'Li Ming', 'STUDENT', 'SID2023001', NOW(), NOW()),
(3, 'student2@uni.edu', 'student2@uni.edu', '$2a$10$xw/.7ZSk7ToNXeJ9Kp5bdO9q26AhORDntiUNLaSMow0jmgCY12TVG', 'Wang Fang', 'STUDENT', 'SID2023002', NOW(), NOW()),
(4, 'admin@uni.edu', 'admin@uni.edu', '$2a$10$xw/.7ZSk7ToNXeJ9Kp5bdO9q26AhORDntiUNLaSMow0jmgCY12TVG', 'System Admin', 'ADMIN', NULL, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET 
  password = EXCLUDED.password,
  updated_at = NOW();

-- Assignments
INSERT INTO assignments (id, title, description, teacher_id, course_code, due_date, total_marks, instructions, status, created_at, updated_at) VALUES
(1, 'Algorithm Design - Assignment 1', 'Implement QuickSort algorithm and analyze time complexity', 1, 'COMP5005', NOW() + INTERVAL '7 days', 100, 'Please submit complete code implementation including:\n1. Algorithm implementation\n2. Time complexity analysis\n3. Test cases', 'PUBLISHED', NOW(), NOW()),
(2, 'Data Structures - Assignment 2', 'Implementation and Application of Binary Search Tree', 1, 'COMP5006', NOW() + INTERVAL '14 days', 100, 'Implement insert, delete, and search operations for BST', 'PUBLISHED', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Rubrics
INSERT INTO rubrics (id, assignment_id, question_id, criteria, weight, key_points, sample_answer, question_type, created_at, updated_at) VALUES
(1, 1, 'Q1', 'Algorithm Implementation Correctness', 50, 'Correctly implement partition function\nCorrectly implement recursive logic\nHandle boundary conditions', 'Implement partition and quickSort functions', 'SUBJECTIVE', NOW(), NOW()),
(2, 1, 'Q2', 'Complexity Analysis', 30, 'Analyze time complexity\nAnalyze space complexity\nDiscuss best/worst cases', 'Average time complexity O(n log n), worst case O(n^2)', 'SUBJECTIVE', NOW(), NOW()),
(3, 1, 'Q3', 'Code Quality', 20, 'Code readability\nVariable naming conventions\nComment completeness', 'Clear function structure and variable naming', 'SUBJECTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Submissions (sample data)
INSERT INTO submissions (id, student_id, assignment_id, original_doc_url, anonymized_doc_url, ocr_text, anonymized_text, status, original_file_name, file_type, created_at, updated_at) VALUES
(1, 2, 1, 'https://example-oss.com/original/submission1.pdf', 'https://example-oss.com/anonymized/submission1.txt',
'Name: Li Ming\nStudent ID: SID2023001\n\nQuick Sort Implementation:\n\ndef quicksort(arr):\n    if len(arr) <= 1:\n        return arr\n    pivot = arr[len(arr) // 2]\n    left = [x for x in arr if x < pivot]\n    middle = [x for x in arr if x == pivot]\n    right = [x for x in arr if x > pivot]\n    return quicksort(left) + middle + quicksort(right)\n\nTime Complexity: O(n log n) average case',
'Name: [STUDENT_NAME]\nStudent ID: [STUDENT_ID]\n\nQuick Sort Implementation:\n\ndef quicksort(arr):\n    if len(arr) <= 1:\n        return arr\n    pivot = arr[len(arr) // 2]\n    left = [x for x in arr if x < pivot]\n    middle = [x for x in arr if x == pivot]\n    right = [x for x in arr if x > pivot]\n    return quicksort(left) + middle + quicksort(right)\n\nTime Complexity: O(n log n) average case',
'SCORED', 'quicksort_assignment.pdf', 'pdf', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day')
ON CONFLICT (id) DO NOTHING;

-- Grades
INSERT INTO grades (id, submission_id, ai_score, ai_confidence, teacher_score, ai_feedback, teacher_comments, status, reviewed_by, published_at, created_at, updated_at) VALUES
(1, 1, 72.50, 0.88, 75.00,
'{"totalScore": 72.5, "confidence": 0.88, "breakdown": [{"questionId": "Q1", "score": 35, "maxScore": 50}, {"questionId": "Q2", "score": 22.5, "maxScore": 30}, {"questionId": "Q3", "score": 15, "maxScore": 20}], "feedback": {"strengths": ["Correct algorithm implementation", "Clean and concise code"], "weaknesses": ["Missing boundary condition discussion", "Insufficient complexity analysis"], "suggestions": [{"issue": "Missing worst-case analysis", "suggestion": "Add explanation of O(n^2) worst-case scenario"}]}}',
'Overall well done, code implementation is correct. Recommend adding more detailed complexity analysis.',
'PUBLISHED', 1, NOW() - INTERVAL '1 day', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day')
ON CONFLICT (id) DO NOTHING;

-- Grade Snapshots
INSERT INTO grade_snapshots (id, submission_id, final_score, feedback, detailed_breakdown, published_by, version_number, snapshot_at, publish_notes) VALUES
(1, 1, 75.00,
'=== AI Scoring Feedback ===\nScore: 72.5\nConfidence: 0.88\n\n=== Teacher Comments ===\nOverall well done, code implementation is correct. Recommend adding more detailed complexity analysis.\n\nFinal Score (Teacher Adjusted): 75.0',
'{"aiScore": 72.50, "aiConfidence": 0.88, "teacherScore": 75.00, "teacherComments": "Overall well done, code implementation is correct. Recommend adding more detailed complexity analysis."}',
1, 1, NOW() - INTERVAL '1 day', 'Initial publication')
ON CONFLICT (id) DO NOTHING;

-- Audit Logs
INSERT INTO audit_logs (id, actor_id, action, entity_type, entity_id, details, change_description, timestamp) VALUES
(1, 2, 'UPLOAD', 'SUBMISSION', 1, '{"fileName": "quicksort_assignment.pdf", "fileSize": 52480}', 'UPLOAD SUBMISSION #1: fileName=quicksort_assignment.pdf, fileSize=52480', NOW() - INTERVAL '2 days'),
(2, NULL, 'AI_SCORE', 'GRADE', 1, '{"aiScore": 72.5, "confidence": 0.88}', 'AI_SCORE GRADE #1: aiScore=72.5, confidence=0.88', NOW() - INTERVAL '2 days'),
(3, 1, 'REVIEW_GRADE', 'GRADE', 1, '{"teacherScore": 75.0}', 'REVIEW_GRADE GRADE #1: teacherScore=75.0', NOW() - INTERVAL '1 day'),
(4, 1, 'PUBLISH_GRADE', 'GRADE_SNAPSHOT', 1, '{"submissionId": 1, "finalScore": 75.0, "versionNumber": 1}', 'PUBLISH_GRADE GRADE_SNAPSHOT #1: submissionId=1, finalScore=75.0', NOW() - INTERVAL '1 day')
ON CONFLICT (id) DO NOTHING;

-- Notification Attempts
INSERT INTO notification_attempts (id, user_id, notification_type, reference_id, message, status, attempt_count, last_attempt_at, created_at, updated_at) VALUES
(1, 2, 'GRADE_PUBLISHED', 1, 'Your grade for ''Algorithm Design - Assignment 1'' has been published. Click to view feedback.', 'SENT', 1, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day')
ON CONFLICT (id) DO NOTHING;

-- Courses (required for demo)
INSERT INTO courses (id, course_code, course_name, teacher, description, created_at, updated_at) VALUES
(1, '5620', 'COMP5620 - Intelligent Marker System', 1, 'Advanced software architecture and intelligent grading system development', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Course Enrollments (required for demo: let demo users see the course)
INSERT INTO course_enrollments (id, user_id, course_code, created_at) VALUES
(1, 1, '5620', NOW()),  -- Teacher joins 5620
(2, 2, '5620', NOW()),  -- Student 1 joins 5620
(3, 3, '5620', NOW())   -- Student 2 joins 5620
ON CONFLICT (id) DO NOTHING;

-- Update sample assignments' course_code to 5620
UPDATE assignments SET course_code = '5620' WHERE id IN (1, 2);

-- Set sequences (set to max ID + 1 to avoid conflicts)
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM users));
SELECT setval('assignments_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM assignments));
SELECT setval('rubrics_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM rubrics));
SELECT setval('submissions_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM submissions));
SELECT setval('grades_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM grades));
SELECT setval('grade_snapshots_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM grade_snapshots));
SELECT setval('audit_logs_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM audit_logs));
SELECT setval('notification_attempts_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM notification_attempts));
SELECT setval('courses_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM courses));
SELECT setval('course_enrollments_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM course_enrollments));

