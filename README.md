# Intelligent Grading System

[中文版](./README_ZH.md) | English

## Project Overview

An AI-powered intelligent grading system designed to automate student assignment grading. The system supports multiple file formats (PDF, Word, images), uses OpenAI GPT-4 models for intelligent scoring, and provides comprehensive analytics capabilities.

## System Architecture

- **Frontend**: React 18 + Ant Design
- **Backend**: Spring Boot 3.x (Java 17)
- **Database**: PostgreSQL 15
- **Storage**: Aliyun OSS
- **AI**: OpenAI GPT-4o-mini (Text + Vision)
- **Deployment**: Docker + Docker Compose

## Core Features

### Student Portal
- ✅ Upload assignments (supports PDF, Word, images)
- ✅ Preview submissions
- ✅ View grades and feedback
- ✅ Appeal grades
- ✅ Track learning progress
- ✅ View personalized learning plans

### Teacher Portal
- ✅ Create assignments and grading rubrics
- ✅ Review AI grading results
- ✅ Adjust scores and feedback
- ✅ Publish grades with version control
- ✅ Handle student appeals
- ✅ View class analytics dashboard
  - Grade distribution charts
  - AI-powered error heatmaps
  - Knowledge point statistics
  - Difficulty/discrimination analysis

### Technical Team
- ✅ Create and manage courses
- ✅ Manage classes and enrollments
- ✅ Assign teachers and students
- ✅ View audit logs
- ✅ User management

## AI Agent Capabilities

### 1. Intelligent Text Extraction
- PDF/Word document OCR processing
- Image content recognition (OpenAI Vision API)
- Mathematical formula recognition
- Automatic anonymization of student information

### 2. Intelligent Scoring
- Objective grading based on rubric criteria
- Subjective question evaluation with suggestions
- Confidence score assessment
- Low-confidence flagging for teacher review

### 3. Intelligent Feedback Generation
- Personalized feedback (identifying issues, causes, improvement suggestions)
- Knowledge point correlation
- Improvement verification methods
- Structured JSON feedback format

### 4. Intelligent Data Analytics
- **AI-powered class error analysis** (error heatmaps)
- Difficulty and discrimination calculation
- Student progress tracking
- Common misconception identification

### 5. Asynchronous Processing & Retry Mechanism
- Automatic notification retry (up to 3 attempts)
- Background async processing (OCR, scoring)
- Exponential backoff strategy
- Task scheduling with Spring @Scheduled

## Quick Start

### Prerequisites

- Docker and Docker Compose
- OpenAI API key
- Aliyun OSS account (optional, can run in mock mode)

### 1. Configure Environment Variables

Copy the example environment file and configure your settings:

```bash
# Copy the example file
cp .env.example .env.local

# Edit with your actual credentials
nano .env.local  # or use your preferred editor
```

**Required configuration in `.env.local`:**

```env
# Aliyun OSS Configuration
ALIYUN_OSS_ENDPOINT=oss-cn-beijing.aliyuncs.com
ALIYUN_OSS_ACCESS_KEY=your-aliyun-access-key-here
ALIYUN_OSS_SECRET_KEY=your-aliyun-secret-key-here
ALIYUN_OSS_BUCKET=your-bucket-name

# OpenAI Configuration
OPENAI_API_KEY=sk-proj-your-openai-api-key-here
OPENAI_MODEL=gpt-4o-mini
OPENAI_VISION_ENABLED=true
OPENAI_VISION_MODEL=gpt-4o-mini

# Database Configuration
POSTGRES_USER=admin
POSTGRES_PASSWORD=admin123
POSTGRES_DB=grading_system
```

**Important:**
- Never commit `.env.local` to version control (it's already in `.gitignore`)
- Keep your API keys and credentials secure
- See `.env.example` for all available configuration options

### 2. Start the System

```bash
# One-click startup (automatically builds images)
docker-compose up -d --build

# Check status
docker-compose ps

# View logs
docker-compose logs -f backend
```

### 3. Access the System

- **Frontend**: http://localhost:8081
- **Backend API**: http://localhost:8080

### 4. Default Accounts

The system comes with pre-configured test accounts:

| Role | Username | Password | Description |
|------|----------|----------|-------------|
| Student | student | password | Student account |
| Teacher | teacher | password | Teacher account |
| Admin | admin | password | Administrator account |

## Demo Workflow

### Complete Workflow Demonstration

1. **Technical Team Creates Course**
   - Login with `admin/password`
   - Create course (e.g., ELEC5620)
   - Create classes for the course
   - Add teachers and students to classes

2. **Teacher Creates Assignment**
   - Login with `teacher/password`
   - Create assignment (title, description, grading criteria)
   - Set due date and total marks
   - Define rubrics for each question

3. **Student Submits Assignment**
   - Login with `student/password`
   - Select course and assignment
   - Upload file (images work best for testing)
   - Preview submission content
   - Confirm submission

4. **AI Automatic Grading** (background process)
   - OCR text extraction
   - Anonymization processing
   - AI scoring (objective + subjective questions)
   - Feedback generation
   - Confidence calculation

5. **Teacher Review**
   - View AI grading results
   - Adjust scores (if needed)
   - Add comments or audio annotations
   - Publish grades with snapshots

6. **Student Appeals**
   - View grades and feedback
   - Submit appeal with reasoning
   - Wait for teacher processing
   - Receive notification on resolution

7. **Teacher Handles Appeals**
   - Review appeal reasoning
   - Re-evaluate submission
   - Approve/reject appeal with explanation
   - Update grade if approved (creates new version)

8. **Data Analytics**
   - View class analytics dashboard
   - **Error Heatmap**: AI analyzes common class issues
   - Grade distribution and difficulty analysis
   - Export reports

## Advanced Technical Features

### 1. AI Intelligent Analysis (Demo Highlight)

**Error Heatmap - AI-Powered Analysis**:
- Collects all students' AI feedback
- Uses OpenAI to analyze common issues
- Dynamically generates 5-10 most common problems
- Automatic sorting and statistics

**Code Location**: `backend/src/main/java/com/intelligentmarker/service/AnalyticsService.java`

### 2. Asynchronous Processing

- Uses `@Async` annotation
- OCR and scoring execute in background
- Non-blocking user operations
- Task executor configuration

### 3. Notification Retry Mechanism

- Uses `@Scheduled` for periodic tasks
- Failed notifications auto-retry (max 3 times)
- Exponential backoff strategy
- Notification status tracking

### 4. Grade Version Control

- Snapshot-based versioning
- Immutable grade history
- Appeal-triggered republishing
- Audit trail for all changes

### 5. Docker Containerization

- Frontend/backend separation
- Database persistence
- One-click start/stop
- Health check monitoring

### 6. CI/CD Pipeline

- GitHub Actions automation
- Code quality checks
- Automated testing
- Docker image building

## Project Structure

```
5620/
├── backend/                      # Spring Boot backend
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/intelligentmarker/
│   │       │       ├── controller/       # REST API controllers
│   │       │       ├── service/          # Business logic
│   │       │       │   ├── ScoringService.java
│   │       │       │   ├── AnalyticsService.java
│   │       │       │   ├── AppealService.java
│   │       │       │   ├── NotificationService.java
│   │       │       │   └── AnonymizationService.java
│   │       │       ├── model/            # Data models
│   │       │       │   ├── User.java
│   │       │       │   ├── Assignment.java
│   │       │       │   ├── Submission.java
│   │       │       │   ├── Grade.java
│   │       │       │   ├── GradeSnapshot.java
│   │       │       │   └── Appeal.java
│   │       │       └── repository/       # Data access
│   │       └── resources/
│   │           ├── application.yml       # Application config
│   │           └── data.sql             # Initialization data
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                     # React frontend
│   ├── src/
│   │   ├── pages/               # Page components
│   │   │   ├── Login.jsx
│   │   │   ├── UploadAssignment.jsx
│   │   │   ├── TeacherReview.jsx
│   │   │   ├── StudentResults.jsx
│   │   │   ├── AppealList.jsx
│   │   │   ├── Dashboard.jsx
│   │   │   └── ClassManagement.jsx
│   │   └── App.jsx              # Main application
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
├── .github/workflows/            # CI/CD configuration
├── docker-compose.yml            # Docker orchestration
├── .env                          # Environment variables
├── START.sh                      # Startup script
└── README.md
```

## Key Files Explanation

### AI Scoring Logic
- `ScoringService.java` - Complete AI scoring workflow implementation
- Supports confidence threshold configuration
- Handles both objective and subjective questions
- Generates structured feedback in JSON format

### Vision API Integration
- `OpenAIVisionAdapter.java` - Image recognition and text extraction
- Mathematical formula recognition capability
- Fallback to mock data when API unavailable

### Intelligent Analytics
- `AnalyticsService.java` - AI-powered error heatmap analysis
- Grade distribution statistics
- Difficulty and discrimination calculations

### Appeal Workflow
- `AppealService.java` - Complete appeal handling process
- Grade version control with snapshots
- Status management (PENDING → APPROVED/REJECTED)

### Anonymization
- `AnonymizationService.java` - Student information removal
- Pattern-based PII detection (IDs, emails, phones)
- Preview generation for student confirmation

## System Administration

### View Logs

```bash
# View all service logs
docker-compose logs -f

# View specific service
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f postgres
```

### Restart Services

```bash
# Restart all services
docker-compose restart

# Restart specific service
docker-compose restart backend
```

### Stop and Clean Up

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (clear database)
docker-compose down -v

# Clean Docker cache
docker system prune -f
```

### Database Operations

```bash
# Access database
docker exec -it intelligent-marker-db psql -U admin -d grading_system

# View tables
\dt

# View users
SELECT id, username, role FROM users;

# View submissions
SELECT id, status, file_type FROM submissions ORDER BY id DESC;

# View grades
SELECT id, ai_score, teacher_score, status FROM grades;

# Exit
\q
```

## Troubleshooting

### Issue 1: Startup Failure

**Solution**:
```bash
docker-compose down -v
docker system prune -f
docker-compose up -d --build
```

### Issue 2: OpenAI API Call Failure

**Checklist**:
1. Verify `OPENAI_API_KEY` in `.env` is correct
2. Check OpenAI account balance
3. View backend logs: `docker logs intelligent-marker-backend -f`
4. System will fallback to mock mode if API fails

### Issue 3: Image Recognition Using Mock Data

**Solution**:
1. Confirm `OPENAI_VISION_MODEL=gpt-4o-mini` in `.env`
2. Restart backend: `docker-compose restart backend`
3. Re-submit image

### Issue 4: Database Connection Failure

**Solution**:
```bash
# Check database status
docker-compose ps postgres

# Restart database
docker-compose restart postgres

# Wait 30 seconds then restart backend
docker-compose restart backend
```

## Technology Stack

### Backend Technologies
- Spring Boot 3.2.0
- Spring Data JPA (Hibernate)
- Spring Security
- PostgreSQL 15
- Lombok
- Jackson (JSON processing)
- PDFBox (PDF processing)
- Apache POI (Word processing)
- RestTemplate (HTTP client)

### Frontend Technologies
- React 18.2
- Ant Design 5.x
- Axios (HTTP client)
- Recharts (data visualization)

### DevOps
- Docker 24.x
- Docker Compose 2.x
- GitHub Actions
- Nginx

### AI Integration
- OpenAI GPT-4o-mini (text understanding and scoring)
- OpenAI Vision API (image recognition)
- Custom prompt engineering
- Mock adapters for offline testing

## Performance Optimizations

1. **Database Indexing**: Indexes on frequently queried fields
2. **Async Processing**: OCR and scoring don't block main flow
3. **Connection Pooling**: Optimized database connection pool
4. **Batch Processing**: Bulk notification retry queries
5. **Lazy Loading**: JPA relationships loaded on demand

## Security Features

1. **Password Encryption**: BCrypt hashing for all passwords
2. **Role-Based Access Control**: STUDENT, TEACHER, ADMIN, TECHNICAL_TEAM
3. **Audit Logging**: Records all critical operations
4. **Data Anonymization**: PII removal during grading
5. **Input Validation**: Server-side validation for all inputs

## Future Enhancements

- [ ] Multi-language support (i18n)
- [ ] Text-to-speech feedback
- [ ] Intelligent question bank generation
- [ ] Code assignment analysis (syntax checking, plagiarism detection)
- [ ] Mobile application
- [ ] Real-time collaborative grading
- [ ] Advanced analytics dashboard
- [ ] Integration with LMS platforms

## Development Team

- **Stage 1**: System design and architecture
- **Stage 2**: Prototype implementation and demonstration
- **Demo Time**: Week 13

## License

This project is for academic demonstration purposes only. Not for commercial use.

---

## Pre-Demo Checklist

- ✅ Docker environment running
- ✅ OpenAI API Key valid with sufficient credits
- ✅ Aliyun OSS configured (or mock mode ready)
- ✅ Test data prepared
- ✅ Demo workflow rehearsed
- ✅ Backup plan ready (mock mode)

**Good luck with your demo!** 🎉

## Support

For issues or questions:
- Check the [Troubleshooting](#troubleshooting) section
- Review logs: `docker-compose logs -f`
- Verify environment variables in `.env`
- Restart services: `docker-compose restart`

---

**Version**: 1.0.0
**Last Updated**: 2024
