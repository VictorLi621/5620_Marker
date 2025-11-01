-- Create learning_plans table
CREATE TABLE IF NOT EXISTS learning_plans (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    grade_id BIGINT NOT NULL,
    assignment_title VARCHAR(500),
    plan_content TEXT,
    objective TEXT,
    estimated_duration VARCHAR(100),
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_learning_plan_student FOREIGN KEY (student_id) REFERENCES users(id),
    CONSTRAINT fk_learning_plan_grade FOREIGN KEY (grade_id) REFERENCES grades(id)
);

-- Create index for faster queries
CREATE INDEX idx_learning_plans_student_id ON learning_plans(student_id);
CREATE INDEX idx_learning_plans_grade_id ON learning_plans(grade_id);
CREATE INDEX idx_learning_plans_status ON learning_plans(status);
