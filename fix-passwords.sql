-- 修复用户密码为BCrypt加密的"password"
-- 密码hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy = "password"

UPDATE users SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' 
WHERE username IN ('teacher@uni.edu', 'student@uni.edu', 'student2@uni.edu', 'admin@uni.edu');

-- 验证更新
SELECT username, 
       CASE 
         WHEN password LIKE '$2a$%' THEN '✅ BCrypt加密'
         ELSE '❌ 未加密'
       END as password_status
FROM users
WHERE username IN ('teacher@uni.edu', 'student@uni.edu', 'student2@uni.edu', 'admin@uni.edu');

