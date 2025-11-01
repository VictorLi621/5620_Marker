# Testing Suite - Complete Overview

This document provides an overview of the comprehensive testing suite for the Intelligent Grading System.

## 📦 What's Included

A complete testing framework with:
- ✅ Detailed test plan documentation
- ✅ Automated Bash test script
- ✅ Advanced Python test suite
- ✅ Sample test files
- ✅ Quick reference guides
- ✅ Troubleshooting documentation

---

## 📚 Documentation Files

### 1. TEST_PLAN.md (Comprehensive Test Plan)
**What it is:** Complete test plan with all test cases, expected results, and test procedures.

**Contains:**
- API test specifications
- Functional test cases
- End-to-end test scenarios
- Performance test guidelines
- Test execution checklist

**When to use:**
- Planning test execution
- Understanding what needs to be tested
- Reference for test case details
- Manual testing guidance

**Key sections:**
- Authentication tests (3 tests)
- Course management tests (3 tests)
- Assignment tests (4 tests)
- Submission tests (6 tests)
- Grade tests (6 tests)
- Appeal tests (5 tests)
- Analytics tests (3 tests)
- E2E scenarios (4 scenarios)

---

### 2. TESTING_GUIDE.md (How-To Guide)
**What it is:** Step-by-step guide on how to run tests and interpret results.

**Contains:**
- Quick start instructions
- Test script usage
- Category-specific testing
- Manual testing procedures
- Troubleshooting steps

**When to use:**
- First time running tests
- Learning how to use test scripts
- Troubleshooting test failures
- Understanding test results

---

### 3. QUICK_TEST_REFERENCE.md (Cheat Sheet)
**What it is:** One-page quick reference for common test commands and scenarios.

**Contains:**
- Quick commands
- Test categories table
- Common scenarios
- Emergency fixes

**When to use:**
- Quick lookup during testing
- Before demo/presentation
- As a desktop reference

---

## 🛠️ Test Scripts

### 1. run_tests.sh (Bash Script)
**What it is:** Automated API testing script using curl.

**Features:**
- Fast execution
- Color-coded output
- No dependencies (just curl)
- Category-based testing
- Text report generation

**How to use:**
```bash
# Make executable (first time only)
chmod +x run_tests.sh

# Run all tests
./run_tests.sh

# Run specific category
./run_tests.sh auth
./run_tests.sh submissions
./run_tests.sh grades
```

**Outputs:**
- Console output with colors
- `test_report.txt` - summary report

**Best for:**
- Quick testing
- CI/CD integration
- Basic validation

---

### 2. test_suite.py (Python Script)
**What it is:** Advanced testing script with session management and detailed reporting.

**Features:**
- Session management
- File upload testing
- JSON report output
- Detailed error messages
- Workflow testing

**Dependencies:**
```bash
pip3 install requests
```

**How to use:**
```bash
# Make executable (first time only)
chmod +x test_suite.py

# Run all tests
python3 test_suite.py

# Run specific category
python3 test_suite.py auth
python3 test_suite.py submissions
python3 test_suite.py grades
```

**Outputs:**
- Console output with colors
- `test_report.json` - detailed JSON report

**Best for:**
- Detailed testing
- Debugging
- Integration testing
- Complex workflows

---

## 📁 Test Files

### test_files/ Directory
Contains sample files for submission testing:

1. **Anonymization test2.docx** (12KB)
   - Word document
   - Tests OCR extraction
   - Tests anonymization

2. **ELEC5620 Project Stage 2 Marking Criteria.pdf** (107KB)
   - PDF document
   - Tests PDF processing
   - Tests multi-page handling

**Usage:** Automatically used by test scripts, or manually upload via UI.

See `test_files/README.md` for details.

---

## 🎯 Test Coverage

### What's Tested

| Component | Coverage | Test Count | Method |
|-----------|----------|------------|--------|
| **Authentication** | 100% | 3 | Automated |
| **Course Management** | 80% | 3 | Automated |
| **Class Management** | 70% | 2 | Automated |
| **Assignments** | 90% | 4 | Automated |
| **Submissions** | 85% | 6 | Automated + Manual |
| **Grades** | 90% | 6 | Automated + Manual |
| **Appeals** | 80% | 5 | Automated + Manual |
| **Analytics** | 50% | 3 | Manual |

**Total Automated Tests:** ~30 API tests
**Total Manual Test Scenarios:** ~10 UI workflows

---

## 🚀 Quick Start Guide

### Step 1: Ensure System is Running
```bash
docker-compose up -d
docker-compose ps  # All services should be "Up"
```

### Step 2: Choose Your Testing Method

**Option A: Fast Bash Testing**
```bash
./run_tests.sh
```

**Option B: Detailed Python Testing**
```bash
python3 test_suite.py
```

### Step 3: Review Results
```bash
# Bash results
cat test_report.txt

# Python results
cat test_report.json
```

---

## 📊 Understanding Test Results

### Success Indicators
- ✅ All services running
- ✅ Pass rate > 90%
- ✅ No critical failures
- ✅ Submissions process successfully

### Common Issues
| Issue | Cause | Fix |
|-------|-------|-----|
| Connection refused | System not running | `docker-compose up -d` |
| File not found | Test files missing | Check `test_files/` |
| Tests timeout | Slow processing | Wait longer, check logs |
| Random failures | Database state | Reset: `docker-compose down -v` |

---

## 🎓 Test Categories Explained

### 1. Authentication (TEST-AUTH-*)
Tests login functionality for all user roles.
- Student login
- Teacher login
- Invalid credentials

### 2. Courses (TEST-COURSE-*)
Tests course creation and management.
- Create course
- Get all courses
- Get course by code

### 3. Classes (TEST-CLASS-*)
Tests class management and student enrollment.
- Create class
- Add students
- Get class details

### 4. Assignments (TEST-ASSIGN-*)
Tests assignment creation and retrieval.
- Create assignment
- Get teacher assignments
- Get assignment details with statistics

### 5. Submissions (TEST-SUB-*)
Tests file upload and processing workflow.
- Upload Word/PDF/Image
- Check processing status
- Get submission content

### 6. Grades (TEST-GRADE-*)
Tests grading and score modification.
- Get pending grades
- Review and modify scores
- Publish grades
- Get student grades

### 7. Appeals (TEST-APPEAL-*)
Tests student appeal workflow.
- Submit appeal
- Get appeals
- Process (approve/reject)

---

## 🔄 Testing Workflows

### Workflow 1: Automated API Testing
```bash
./run_tests.sh          # Run all API tests
cat test_report.txt     # Review results
```
**Time:** ~2 minutes
**Covers:** All API endpoints

### Workflow 2: Submission Testing
```bash
python3 test_suite.py submissions
```
**Time:** ~3-5 minutes (includes file upload and processing)
**Covers:** Complete submission workflow

### Workflow 3: Manual UI Testing
```
1. Login as each user type
2. Test key workflows
3. Verify UI functionality
```
**Time:** ~10-15 minutes
**Covers:** User experience and UI

### Workflow 4: End-to-End Testing
```
Follow E2E scenarios in TEST_PLAN.md
```
**Time:** ~20-30 minutes
**Covers:** Complete system flows

---

## 🛡️ Pre-Demo Checklist

Use this before presenting or demonstrating:

- [ ] System running: `docker-compose ps`
- [ ] Run tests: `./run_tests.sh`
- [ ] Pass rate > 90%
- [ ] Test files present: `ls test_files/`
- [ ] Manual login works for all users
- [ ] Sample submission processes correctly
- [ ] No errors in logs: `docker-compose logs backend`
- [ ] Frontend loads: http://localhost:8081
- [ ] Backend healthy: http://localhost:8080/actuator/health

---

## 📈 Continuous Integration

### Add to GitHub Actions

```yaml
# .github/workflows/test.yml
name: Test Suite
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Start services
        run: docker-compose up -d
      - name: Wait for startup
        run: sleep 60
      - name: Run tests
        run: ./run_tests.sh
      - name: Upload results
        uses: actions/upload-artifact@v2
        with:
          name: test-results
          path: test_report.txt
```

---

## 🐛 Troubleshooting

### Quick Fixes

**Problem:** Tests won't run
```bash
chmod +x run_tests.sh test_suite.py
```

**Problem:** System not responding
```bash
docker-compose restart
sleep 30
```

**Problem:** All tests fail
```bash
docker-compose down -v
docker-compose up -d
sleep 60
./run_tests.sh
```

**Problem:** Some tests fail randomly
```bash
# Usually timing issues, retry
./run_tests.sh
```

### Detailed Troubleshooting

See `TESTING_GUIDE.md` → Troubleshooting section for comprehensive solutions.

---

## 📞 Getting Help

### Check Documentation
1. **TESTING_GUIDE.md** - Comprehensive how-to
2. **TEST_PLAN.md** - Detailed specifications
3. **QUICK_TEST_REFERENCE.md** - Quick lookup
4. **README.md** - System documentation

### Check System Health
```bash
# View logs
docker-compose logs backend
docker-compose logs postgres

# Check database
docker exec -it intelligent-marker-db psql -U admin -d grading_system
```

### Reset Everything
```bash
docker-compose down -v
docker system prune -f
docker-compose up -d --build
sleep 90
./run_tests.sh
```

---

## 🎯 Testing Best Practices

1. **Run tests before making changes** - Establish baseline
2. **Run tests after changes** - Verify nothing broke
3. **Check logs on failure** - Understand what went wrong
4. **Reset database between major test runs** - Ensure consistency
5. **Keep test files updated** - Maintain sample data
6. **Document new test cases** - Update TEST_PLAN.md
7. **Automate what you can** - Use scripts
8. **Test manually occasionally** - Catch UI issues

---

## 📝 Adding New Tests

### To add a new test case:

1. **Document it** in `TEST_PLAN.md`
   ```markdown
   ### TEST-NEW-001: Description
   **Steps**: ...
   **Expected**: ...
   ```

2. **Add to Bash script** in `run_tests.sh`
   ```bash
   test_new_feature() {
     print_test "TEST-NEW-001: ..."
     response=$(curl ...)
     check_response "TEST-NEW-001" "$response" "200"
   }
   ```

3. **Add to Python script** in `test_suite.py`
   ```python
   def test_new_feature(self):
     test_name = "TEST-NEW-001: ..."
     response = self.session.get(...)
     if response.status_code == 200:
       self.result.add_pass(test_name)
   ```

4. **Update this README** with new category if needed

---

## 📦 File Structure Summary

```
5620/
├── TEST_PLAN.md                    # Comprehensive test plan
├── TESTING_GUIDE.md                # How-to guide
├── TESTING_README.md               # This file
├── QUICK_TEST_REFERENCE.md         # Quick reference
├── run_tests.sh                    # Bash test script
├── test_suite.py                   # Python test script
├── test_files/
│   ├── README.md                   # Test files documentation
│   ├── Anonymization test2.docx    # Word test file
│   └── ELEC5620...pdf              # PDF test file
├── test_report.txt                 # Bash test results (generated)
└── test_report.json                # Python test results (generated)
```

---

## 🎉 Success Criteria

Your testing suite is working correctly when:

✅ All test scripts execute without errors
✅ Pass rate is consistently > 90%
✅ File uploads process successfully
✅ Grades can be modified and saved
✅ Reports are generated correctly
✅ System performs well under test load
✅ No critical errors in logs

---

## 🔮 Future Enhancements

Potential additions to test suite:

- [ ] Load testing scripts (Apache Bench, JMeter)
- [ ] Performance benchmarking
- [ ] Security testing (OWASP)
- [ ] Accessibility testing
- [ ] Cross-browser testing
- [ ] Mobile responsiveness testing
- [ ] API fuzzing
- [ ] Stress testing

---

## 📊 Test Metrics

Track these metrics over time:

- **Pass Rate:** Target > 95%
- **Test Execution Time:** < 5 minutes for full suite
- **Code Coverage:** Target > 80%
- **Bug Discovery Rate:** Tests should catch bugs before production
- **Flaky Test Rate:** < 5% (tests failing randomly)

---

## 🤝 Contributing

To contribute to the test suite:

1. Add test cases to `TEST_PLAN.md`
2. Implement in `run_tests.sh` and/or `test_suite.py`
3. Test your tests!
4. Update documentation
5. Submit PR with test results

---

## 📜 Version History

- **v1.0** - Initial test suite creation
  - 30+ automated API tests
  - 10+ manual test scenarios
  - 4 comprehensive documentation files
  - 2 test scripts (Bash + Python)

---

## 🎓 Learning Resources

### For Beginners
Start with: `QUICK_TEST_REFERENCE.md` → `TESTING_GUIDE.md`

### For Advanced Testing
Read: `TEST_PLAN.md` → Implement custom tests

### For CI/CD
Use: `run_tests.sh` in your pipelines

---

**Ready to test? Start here:**

```bash
# Quick test
./run_tests.sh

# Full test
python3 test_suite.py

# See results
cat test_report.txt
```

---

**Happy Testing!** 🧪

For questions or issues, refer to:
- TESTING_GUIDE.md (detailed help)
- TEST_PLAN.md (test specifications)
- README.md (system documentation)
