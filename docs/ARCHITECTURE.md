# 系统架构说明

## 📐 整体架构

本项目采用**简化的微服务架构**，基于单体Spring Boot应用进行模块化设计：

```
┌─────────────────────────────────────────────┐
│           React Frontend (Port 3000)        │
│  (Ant Design UI + Axios + React Router)    │
└──────────────────┬──────────────────────────┘
                   │ REST API
┌──────────────────▼──────────────────────────┐
│      Spring Boot Backend (Port 8080)        │
│  ┌──────────────────────────────────────┐  │
│  │   Controller Layer (REST APIs)       │  │
│  ├──────────────────────────────────────┤  │
│  │   Service Layer (Business Logic)     │  │
│  │   - SubmissionService (@Async)       │  │
│  │   - ScoringService (AI Integration)  │  │
│  │   - NotificationService (@Scheduled) │  │
│  │   - PublishService (Snapshots)       │  │
│  │   - AppealService                    │  │
│  │   - AnalyticsService                 │  │
│  ├──────────────────────────────────────┤  │
│  │   Repository Layer (Data Access)     │  │
│  │   - JPA Repositories                 │  │
│  └──────────────────────────────────────┘  │
└──────────────────┬──────────────────────────┘
                   │
         ┌─────────┼─────────┐
         │         │         │
         ▼         ▼         ▼
    PostgreSQL  阿里云OSS  OpenAI API
    (数据存储)  (文件存储)  (AI评分)
```

---

## 🏛️ 4+1视图

### 1️⃣ Logical View (逻辑视图)

#### 核心模块

```
com.intelligentmarker
├── model/              # 实体类 (8个核心表)
│   ├── User            # 用户 (学生/教师/管理员)
│   ├── Assignment      # 作业
│   ├── Submission      # 提交
│   ├── Grade           # 评分
│   ├── GradeSnapshot   # 不可变快照
│   ├── Appeal          # 申诉
│   ├── NotificationAttempt  # 通知重试记录
│   └── AuditLog        # 审计日志
│
├── repository/         # 数据访问层
│   └── *Repository.java
│
├── service/            # 业务逻辑层
│   ├── SubmissionService       # 协调上传→OCR→匿名→评分
│   ├── OCRService              # 文本提取 (Tesseract)
│   ├── AnonymizationService    # PII移除
│   ├── ScoringService          # AI评分
│   ├── OpenAiService           # OpenAI API封装
│   ├── PublishService          # 快照发布
│   ├── NotificationService     # 通知+重试
│   ├── AppealService           # 申诉处理
│   ├── AnalyticsService        # 数据分析
│   ├── AliyunOssService        # 阿里云OSS
│   └── AuditLogService         # 审计日志
│
└── controller/         # REST API层
    ├── SubmissionController
    ├── GradeController
    ├── AppealController
    ├── AnalyticsController
    ├── AssignmentController
    └── AuthController
```

---

### 2️⃣ Process View (流程视图)

#### 主流程：提交→评分→发布

```
学生上传 
    ↓
【异步处理 @Async】
    ├── OCR提取文本
    ├── 匿名化处理
    └── AI评分
         ├── 置信度 >= 0.85 → HIGH_CONFIDENCE
         └── 置信度 < 0.85 → NEEDS_REVIEW
    ↓
教师审核 (如需要)
    ↓
发布成绩
    ├── 创建不可变快照
    ├── 通知学生
    │    ├── 发送成功 → SENT
    │    └── 发送失败 → FAILED
    │         └── 【定时任务 @Scheduled 每60秒重试】
    │              ├── 重试1 (60秒后)
    │              ├── 重试2 (120秒后)
    │              └── 重试3 (最后一次)
    └── 记录审计日志
```

#### 状态机

```
Submission状态流转:
UPLOADED → OCR_PROCESSING → ANONYMIZING → SCORING → SCORED → PUBLISHED

Grade状态流转:
HIGH_CONFIDENCE / NEEDS_REVIEW → APPROVED → PUBLISHED → APPEALED

NotificationAttempt状态流转:
PENDING → SENT
        ↓ (失败)
      FAILED → SENT (重试成功)
        ↓ (3次都失败)
      EXHAUSTED
```

---

### 3️⃣ Development View (开发视图)

#### 技术栈

| 层次 | 技术 | 说明 |
|------|------|------|
| **前端** | React 18 + Ant Design 5 + Vite | 现代化UI |
| **后端** | Spring Boot 3.2 + Java 17 | REST API |
| **数据库** | PostgreSQL 15 | ACID保证 |
| **文件存储** | 阿里云OSS | 对象存储 |
| **AI** | OpenAI GPT-4 | 智能评分 |
| **OCR** | Tesseract | 文本提取 |
| **容器化** | Docker + Docker Compose | 一键部署 |
| **CI/CD** | GitHub Actions | 自动化构建 |

#### 依赖管理

- **后端：** Maven (`pom.xml`)
- **前端：** npm (`package.json`)

#### 异步处理

- **方式：** Spring `@Async` (不用RabbitMQ，简化架构)
- **配置：** `AsyncConfig.java` (线程池：5-10)
- **状态查询：** 前端轮询 `GET /api/submissions/{id}/status` (每2秒)

#### 定时任务

- **方式：** Spring `@Scheduled`
- **重试通知：** 每60秒扫描失败通知并重试
- **配置：** `NotificationService.retryFailedNotifications()`

---

### 4️⃣ Deployment View (部署视图)

#### Docker Compose架构

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    ports: 5432:5432
    volumes: postgres_data:/var/lib/postgresql/data
  
  backend:
    build: ./backend
    ports: 8080:8080
    depends_on: postgres
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/grading_system
      - ALIYUN_OSS_*
      - OPENAI_API_KEY
  
  frontend:
    build: ./frontend
    ports: 3000:80
    depends_on: backend
```

#### 部署步骤

```bash
# 1. 启动所有服务
docker-compose up -d

# 2. 等待启动 (约30秒)
docker-compose logs -f

# 3. 访问
open http://localhost:3000
```

---

### 5️⃣ Scenario View (场景视图)

#### Use Case 1: 学生提交作业

```
学生 → [上传文件]
    ↓
SubmissionController.uploadSubmission()
    ↓
SubmissionService.createSubmission()
    ├── AliyunOssService.uploadFile()  # 上传原始文件到OSS
    └── SubmissionService.processSubmissionAsync()  # 异步处理
         ├── OCRService.extractText()  # Step 1: OCR
         ├── AnonymizationService.anonymize()  # Step 2: 匿名化
         └── ScoringService.scoreSubmission()  # Step 3: AI评分
              └── OpenAiService.chat()  # 调用GPT-4
```

#### Use Case 2: 教师发布成绩

```
教师 → [点击"发布成绩"]
    ↓
GradeController.publishGrade()
    ↓
PublishService.publishGrade()
    ├── 创建GradeSnapshot (不可变快照)
    ├── NotificationService.notifyStudentGradePublished()
    │    ↓
    │   NotificationAttempt (PENDING)
    │    ↓
    │   sendNotification()
    │    ├── SUCCESS → SENT
    │    └── FAIL → FAILED (等待重试)
    └── AuditLogService.log()  # 记录审计
```

#### Use Case 3: 通知自动重试

```
【后台定时任务，每60秒执行】

@Scheduled(fixedRate = 60000)
NotificationService.retryFailedNotifications()
    ↓
查询 notification_attempts 表
WHERE status = 'FAILED' 
  AND attemptCount < 3 
  AND nextRetryAt <= NOW()
    ↓
对每条失败通知:
    attemptCount++
    nextRetryAt = NOW() + (60秒 * attemptCount)  # 指数退避
    sendNotification()
    ↓
    ├── 成功 → status = 'SENT'
    └── 失败 → status = 'FAILED' (下次继续重试)
         └── attemptCount == 3 → status = 'EXHAUSTED'
```

---

## 🤖 AI Agent能力设计

### 1. 置信度驱动决策

```java
if (confidence >= 0.85) {
    grade.setStatus(GradeStatus.HIGH_CONFIDENCE);
    // 建议教师可直接发布，但仍有审核权
} else {
    grade.setStatus(GradeStatus.NEEDS_REVIEW);
    notifyTeacher();  // 强制人工审核
}
```

### 2. Rubric感知评分

```java
String prompt = buildScoringPrompt(studentAnswer, rubrics, assignment);
// Prompt包含:
// - 评分标准
// - 关键得分点
// - 参考答案
// - 要求输出JSON格式反馈
```

### 3. 可解释性反馈

```json
{
  "totalScore": 72.5,
  "confidence": 0.88,
  "breakdown": [...],
  "feedback": {
    "strengths": ["优点1", "优点2"],
    "weaknesses": ["不足1"],
    "suggestions": [
      {
        "issue": "具体问题",
        "suggestion": "改进建议",
        "why": "为什么重要",
        "howToImprove": "具体怎么做"
      }
    ]
  }
}
```

### 4. 自动重试机制

- **策略：** 指数退避 (60s → 120s → 180s)
- **最大次数：** 3次
- **日志记录：** 每次尝试都记录在`notification_attempts`表

---

## 🔐 安全与隐私设计

### 1. 数据隐私

- **匿名化：** 移除姓名、学号、邮箱、手机号
- **加密存储：** 原始文件存储在OSS (支持加密)
- **访问控制：** Spring Security RBAC

### 2. 审计追踪

所有敏感操作记录在`audit_logs`表：
- `UPLOAD` - 上传作业
- `AI_SCORE` - AI评分
- `REVIEW_GRADE` - 教师审核
- `PUBLISH_GRADE` - 发布成绩
- `CREATE_APPEAL` - 创建申诉
- `RESOLVE_APPEAL` - 处理申诉

### 3. 不可变快照

- 成绩一旦发布，创建`GradeSnapshot`（版本号递增）
- 历史版本不可修改，确保可追溯

---

## 📊 数据库设计

### ER图（简化）

```
users (用户)
  ├─ assignments (作业) [1:N]
  │   ├─ submissions (提交) [1:N]
  │   │   ├─ grades (评分) [1:1]
  │   │   │   └─ grade_snapshots (快照) [1:N]
  │   │   └─ appeals (申诉) [1:N]
  │   └─ rubrics (评分标准) [1:N]
  └─ notification_attempts (通知) [1:N]

audit_logs (审计日志) [独立表]
```

### 关键索引

- `submissions(student_id, assignment_id)` - 联合唯一索引
- `grades(submission_id)` - 唯一索引
- `notification_attempts(status, nextRetryAt)` - 定时任务查询优化
- `audit_logs(entity_type, entity_id)` - 审计查询优化

---

## 🚀 性能优化

### 1. 异步处理

- OCR、匿名化、评分全部异步
- 不阻塞主线程
- 前端轮询状态

### 2. 数据库连接池

- HikariCP (Spring Boot默认)
- 最大连接数：10

### 3. 文件存储

- 阿里云OSS (云端存储)
- 不占用本地磁盘

### 4. 缓存（可选扩展）

- 可添加Redis缓存热点数据
- 当前版本未启用（保持简单）

---

## 📈 可扩展性

### 水平扩展

- **Backend：** 可部署多个实例 (需共享PostgreSQL)
- **Frontend：** 静态资源，易于CDN分发
- **Database：** PostgreSQL支持主从复制

### 垂直扩展

- 增加CPU/内存
- 增大数据库连接池

---

## 🧪 测试策略

### 单元测试

- Service层：Mock Repository
- Controller层：MockMvc

### 集成测试

- Testcontainers (PostgreSQL)
- 端到端API测试

### 手动测试

- Demo脚本 (`docs/demo-script.md`)
- 涵盖所有核心流程

---

## 📚 代码规范

- **命名：** 驼峰命名
- **注释：** Javadoc + 行内注释
- **日志：** SLF4J + Lombok `@Slf4j`
- **异常处理：** 统一返回JSON格式错误

---

**架构设计完成！如有疑问，请参考代码或联系开发团队。**

