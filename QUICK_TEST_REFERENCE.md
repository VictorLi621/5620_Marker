# Quick Test Reference

## 🚀 Quick Start

```bash
# 1. Start system
docker-compose up -d

# 2. Run tests (choose one)
./run_tests.sh              # Bash - fast & simple
python3 test_suite.py       # Python - detailed
```

---

## 📁 Test Files Overview

| File | Purpose | How to Use |
|------|---------|------------|
| **TEST_PLAN.md** | Complete test plan with all test cases | Reference document |
| **run_tests.sh** | Bash automated test script | `./run_tests.sh [category]` |
| **test_suite.py** | Python advanced test script | `python3 test_suite.py [category]` |
| **TESTING_GUIDE.md** | Detailed testing guide | Read for comprehensive info |
| **test_files/** | Sample files for testing | Used automatically by scripts |

---

## 🎯 Quick Commands

### Run All Tests
```bash
./run_tests.sh              # All tests (Bash)
python3 test_suite.py       # All tests (Python)
```

### Run Specific Category
```bash
./run_tests.sh auth         # Authentication only
./run_tests.sh submissions  # Submissions only
./run_tests.sh grades       # Grades only

python3 test_suite.py auth
python3 test_suite.py submissions
```

### View Results
```bash
cat test_report.txt         # Bash results
cat test_report.json        # Python results (formatted)
```

---

## 🧪 Test Categories

| Category | Command | Tests |
|----------|---------|-------|
| **Authentication** | `./run_tests.sh auth` | Login flows (3 tests) |
| **Courses** | `./run_tests.sh courses` | Course management (2 tests) |
| **Classes** | `./run_tests.sh classes` | Class management (2 tests) |
| **Assignments** | `./run_tests.sh assignments` | Assignment management (3 tests) |
| **Submissions** | `./run_tests.sh submissions` | File uploads & processing (5 tests) |
| **Grades** | `./run_tests.sh grades` | Grading workflow (5 tests) |
| **Appeals** | `./run_tests.sh appeals` | Appeal workflow (4 tests) |

---

## 👥 Test Accounts

| Username | Password | Role | Use For |
|----------|----------|------|---------|
| `student` | `password` | Student | Submit assignments, view grades |
| `teacher` | `password` | Teacher | Review, grade, publish |
| `admin` | `password` | Admin | Manage courses, classes |

---

## 📝 Common Test Scenarios

### Scenario 1: Test Complete Submission Flow
```bash
# Automated
python3 test_suite.py submissions

# Manual
1. Login as student
2. Upload test_files/Anonymization test2.docx
3. Wait for processing
4. Check grade in "View Results"
```

### Scenario 2: Test Teacher Review
```bash
# Manual (requires existing submission)
1. Login as teacher
2. Go to "My Assignments" → View Details
3. Click "Review" on a submission
4. Modify score
5. Save review
6. Publish grade
```

### Scenario 3: Test Appeal Workflow
```bash
# Manual (requires published grade)
1. Login as student → View Results
2. Click "Appeal Grade"
3. Enter reason, submit
4. Login as teacher → Handle Appeals
5. Process appeal (approve/reject)
6. Login as student → View updated grade
```

---

## 🔍 Verify Test Files

```bash
# Check test files exist
ls -la test_files/

# Should show:
# Anonymization test2.docx
# ELEC5620 Project Stage 2 Marking Criteria.pdf
```

---

## 🐛 Quick Troubleshooting

### System Not Running
```bash
docker-compose up -d
sleep 30  # Wait for startup
docker-compose ps  # Verify all services "Up"
```

### Tests Fail
```bash
# 1. Check logs
docker-compose logs backend

# 2. Reset database
docker-compose down -v
docker-compose up -d
sleep 60

# 3. Retry
./run_tests.sh
```

### File Upload Fails
```bash
# Check test files exist
ls test_files/

# Check file permissions
chmod 644 test_files/*
```

---

## 📊 Understanding Results

### Bash Output
```
✓ PASSED: TEST-AUTH-001: Login as Student    # Success
✗ FAILED: TEST-AUTH-003: Invalid credentials  # Failure
Total Tests: 20, Passed: 18, Failed: 2
Pass Rate: 90%
```

### Python Output
```json
{
  "total": 20,
  "passed": 18,
  "failed": 2,
  "pass_rate": 90.0
}
```

---

## 🔧 Useful Database Queries

```bash
# Access database
docker exec -it intelligent-marker-db psql -U admin -d grading_system

# Check submissions
SELECT id, status FROM submissions ORDER BY id DESC LIMIT 5;

# Check grades
SELECT id, ai_score, teacher_score FROM grades ORDER BY id DESC LIMIT 5;

# Exit
\q
```

---

## 📚 Full Documentation

| Document | Description |
|----------|-------------|
| **TEST_PLAN.md** | Complete test specifications |
| **TESTING_GUIDE.md** | Comprehensive testing guide |
| **README.md** | System documentation |
| **This file** | Quick reference |

---

## 🎯 Test Checklist for Demo

Before presenting/demoing:

- [ ] System is running (`docker-compose ps`)
- [ ] Run all tests: `./run_tests.sh`
- [ ] Check pass rate > 90%
- [ ] Test files are in place
- [ ] Test accounts work (login manually)
- [ ] Sample submission processes successfully
- [ ] No errors in logs

---

## 💡 Pro Tips

1. **Run tests before demo** to catch issues early
2. **Check logs** if tests fail: `docker-compose logs backend`
3. **Reset database** between test runs for consistency
4. **Test files** are reusable - upload multiple times
5. **Python script** provides more detailed feedback

---

## 🆘 Emergency Quick Fix

If everything breaks:
```bash
# Nuclear option - full reset
docker-compose down -v
docker system prune -f
docker-compose up -d --build
sleep 90  # Wait for full initialization
./run_tests.sh
```

---

## 📞 Quick Help

**Problem:** Tests won't run
**Solution:** Make scripts executable
```bash
chmod +x run_tests.sh
chmod +x test_suite.py
```

**Problem:** No test files
**Solution:** Check directory
```bash
mkdir -p test_files
# Add your test files to test_files/
```

**Problem:** All tests fail
**Solution:** System not running
```bash
docker-compose up -d
sleep 30
```

---

**For detailed information, see TESTING_GUIDE.md or TEST_PLAN.md**
