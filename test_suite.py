#!/usr/bin/env python3
"""
Intelligent Grading System - Comprehensive Test Suite
Advanced Python testing script with detailed reporting
"""

import requests
import json
import time
import sys
from pathlib import Path
from datetime import datetime
from typing import Dict, List, Tuple, Optional

# Configuration
BACKEND_URL = "http://localhost:8080"
FRONTEND_URL = "http://localhost:8081"
TEST_FILES_DIR = Path("./test_files")

# Test accounts
TEST_ACCOUNTS = {
    "student": {"username": "student", "password": "password", "role": "STUDENT"},
    "teacher": {"username": "teacher", "password": "password", "role": "TEACHER"},
    "admin": {"username": "admin", "password": "password", "role": "ADMIN"}
}

# Color codes for terminal output
class Colors:
    HEADER = '\033[95m'
    BLUE = '\033[94m'
    CYAN = '\033[96m'
    GREEN = '\033[92m'
    YELLOW = '\033[93m'
    RED = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'

class TestResult:
    def __init__(self):
        self.total = 0
        self.passed = 0
        self.failed = 0
        self.skipped = 0
        self.errors: List[Tuple[str, str]] = []
        self.start_time = datetime.now()

    def add_pass(self, test_name: str):
        self.passed += 1
        self.total += 1
        print(f"{Colors.GREEN}✓ PASSED: {test_name}{Colors.ENDC}")

    def add_fail(self, test_name: str, error: str):
        self.failed += 1
        self.total += 1
        self.errors.append((test_name, error))
        print(f"{Colors.RED}✗ FAILED: {test_name}{Colors.ENDC}")
        print(f"{Colors.RED}  Error: {error}{Colors.ENDC}")

    def add_skip(self, test_name: str, reason: str):
        self.skipped += 1
        self.total += 1
        print(f"{Colors.YELLOW}⊘ SKIPPED: {test_name} - {reason}{Colors.ENDC}")

    def print_summary(self):
        end_time = datetime.now()
        duration = (end_time - self.start_time).total_seconds()

        print("\n" + "=" * 60)
        print(f"{Colors.BOLD}Test Execution Summary{Colors.ENDC}")
        print("=" * 60)
        print(f"Total Tests:  {Colors.BLUE}{self.total}{Colors.ENDC}")
        print(f"Passed Tests: {Colors.GREEN}{self.passed}{Colors.ENDC}")
        print(f"Failed Tests: {Colors.RED}{self.failed}{Colors.ENDC}")
        print(f"Skipped:      {Colors.YELLOW}{self.skipped}{Colors.ENDC}")

        if self.total > 0:
            pass_rate = (self.passed / self.total) * 100
            print(f"Pass Rate:    {Colors.CYAN}{pass_rate:.1f}%{Colors.ENDC}")

        print(f"Duration:     {duration:.2f} seconds")

        if self.errors:
            print(f"\n{Colors.RED}Failed Tests:{Colors.ENDC}")
            for test_name, error in self.errors:
                print(f"{Colors.RED}  - {test_name}: {error}{Colors.ENDC}")

        print("=" * 60)

    def save_report(self, filename: str = "test_report.json"):
        report = {
            "timestamp": datetime.now().isoformat(),
            "total": self.total,
            "passed": self.passed,
            "failed": self.failed,
            "skipped": self.skipped,
            "pass_rate": (self.passed / self.total * 100) if self.total > 0 else 0,
            "errors": [{"test": name, "error": error} for name, error in self.errors]
        }

        with open(filename, 'w') as f:
            json.dump(report, f, indent=2)

        print(f"\n{Colors.BLUE}Report saved to: {filename}{Colors.ENDC}")

class TestSuite:
    def __init__(self):
        self.result = TestResult()
        self.session = requests.Session()
        self.current_user = None

    def print_header(self, text: str):
        print(f"\n{Colors.HEADER}{Colors.BOLD}{'=' * 60}{Colors.ENDC}")
        print(f"{Colors.HEADER}{Colors.BOLD}{text}{Colors.ENDC}")
        print(f"{Colors.HEADER}{Colors.BOLD}{'=' * 60}{Colors.ENDC}")

    def check_system_health(self) -> bool:
        """Check if backend and frontend are running"""
        self.print_header("System Health Check")

        # Check backend
        try:
            response = requests.get(f"{BACKEND_URL}/actuator/health", timeout=5)
            if response.status_code == 200:
                self.result.add_pass("Backend health check")
            else:
                self.result.add_fail("Backend health check", f"Status: {response.status_code}")
                return False
        except requests.exceptions.RequestException as e:
            self.result.add_fail("Backend health check", str(e))
            print(f"{Colors.RED}Please start the system with: docker-compose up -d{Colors.ENDC}")
            return False

        # Check frontend
        try:
            response = requests.get(FRONTEND_URL, timeout=5)
            if response.status_code == 200:
                self.result.add_pass("Frontend health check")
            else:
                self.result.add_fail("Frontend health check", f"Status: {response.status_code}")
        except requests.exceptions.RequestException as e:
            self.result.add_fail("Frontend health check", str(e))

        return True

    def login(self, role: str) -> Optional[Dict]:
        """Login with specified role"""
        account = TEST_ACCOUNTS.get(role)
        if not account:
            return None

        try:
            response = self.session.post(
                f"{BACKEND_URL}/api/auth/login",
                json={"username": account["username"], "password": account["password"]},
                timeout=10
            )

            if response.status_code == 200:
                self.current_user = response.json()
                return self.current_user
            return None
        except Exception as e:
            print(f"Login error: {e}")
            return None

    def test_authentication(self):
        """Test authentication endpoints"""
        self.print_header("Authentication Tests")

        # Test student login
        test_name = "TEST-AUTH-001: Student login"
        try:
            response = self.session.post(
                f"{BACKEND_URL}/api/auth/login",
                json={"username": "student", "password": "password"},
                timeout=10
            )

            if response.status_code == 200:
                data = response.json()
                if data.get("role") == "STUDENT":
                    self.result.add_pass(test_name)
                else:
                    self.result.add_fail(test_name, f"Expected STUDENT role, got {data.get('role')}")
            else:
                self.result.add_fail(test_name, f"Status: {response.status_code}")
        except Exception as e:
            self.result.add_fail(test_name, str(e))

        # Test teacher login
        test_name = "TEST-AUTH-002: Teacher login"
        try:
            response = self.session.post(
                f"{BACKEND_URL}/api/auth/login",
                json={"username": "teacher", "password": "password"},
                timeout=10
            )

            if response.status_code == 200:
                data = response.json()
                if data.get("role") == "TEACHER":
                    self.result.add_pass(test_name)
                else:
                    self.result.add_fail(test_name, f"Expected TEACHER role, got {data.get('role')}")
            else:
                self.result.add_fail(test_name, f"Status: {response.status_code}")
        except Exception as e:
            self.result.add_fail(test_name, str(e))

        # Test invalid login
        test_name = "TEST-AUTH-003: Invalid credentials"
        try:
            response = self.session.post(
                f"{BACKEND_URL}/api/auth/login",
                json={"username": "invalid", "password": "wrong"},
                timeout=10
            )

            if response.status_code == 400:
                self.result.add_pass(test_name)
            else:
                self.result.add_fail(test_name, f"Expected 400, got {response.status_code}")
        except Exception as e:
            self.result.add_fail(test_name, str(e))

    def test_courses(self):
        """Test course management"""
        self.print_header("Course Management Tests")

        # Login as admin
        if not self.login("admin"):
            self.result.add_skip("Course tests", "Admin login failed")
            return

        # Test get all courses
        test_name = "TEST-COURSE-002: Get all courses"
        try:
            response = self.session.get(f"{BACKEND_URL}/api/courses", timeout=10)

            if response.status_code == 200:
                courses = response.json()
                if isinstance(courses, list):
                    self.result.add_pass(f"{test_name} (Found {len(courses)} courses)")
                else:
                    self.result.add_fail(test_name, "Response is not a list")
            else:
                self.result.add_fail(test_name, f"Status: {response.status_code}")
        except Exception as e:
            self.result.add_fail(test_name, str(e))

    def test_assignments(self):
        """Test assignment management"""
        self.print_header("Assignment Tests")

        # Login as teacher
        if not self.login("teacher"):
            self.result.add_skip("Assignment tests", "Teacher login failed")
            return

        # Get teacher assignments
        test_name = "TEST-ASSIGN-002: Get teacher assignments"
        try:
            teacher_id = self.current_user.get("id")
            response = self.session.get(
                f"{BACKEND_URL}/api/assignments",
                params={"teacherId": teacher_id, "page": 0, "size": 20},
                timeout=10
            )

            if response.status_code == 200:
                data = response.json()
                assignments = data.get("content", [])
                self.result.add_pass(f"{test_name} (Found {len(assignments)} assignments)")
            else:
                self.result.add_fail(test_name, f"Status: {response.status_code}")
        except Exception as e:
            self.result.add_fail(test_name, str(e))

    def test_submission_workflow(self):
        """Test complete submission workflow"""
        self.print_header("Submission Workflow Tests")

        # Check if test files exist
        test_file = TEST_FILES_DIR / "Anonymization test2.docx"
        if not test_file.exists():
            self.result.add_skip("Submission tests", f"Test file not found: {test_file}")
            return

        # Login as student
        if not self.login("student"):
            self.result.add_skip("Submission tests", "Student login failed")
            return

        # Upload submission
        test_name = "TEST-SUB-001: Upload Word document"
        try:
            with open(test_file, 'rb') as f:
                files = {'file': ('Anonymization test2.docx', f, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document')}
                data = {
                    'studentId': self.current_user.get("id"),
                    'assignmentId': 1
                }
                response = self.session.post(
                    f"{BACKEND_URL}/api/submissions",
                    files=files,
                    data=data,
                    timeout=30
                )

                if response.status_code == 200:
                    result_data = response.json()
                    submission_id = result_data.get("submissionId")
                    self.result.add_pass(f"{test_name} (ID: {submission_id})")

                    # Wait for processing
                    print(f"{Colors.CYAN}Waiting for submission processing...{Colors.ENDC}")
                    time.sleep(3)

                    # Check submission status
                    self.check_submission_status(submission_id)
                else:
                    self.result.add_fail(test_name, f"Status: {response.status_code}, Response: {response.text}")
        except FileNotFoundError:
            self.result.add_skip(test_name, f"File not found: {test_file}")
        except Exception as e:
            self.result.add_fail(test_name, str(e))

    def check_submission_status(self, submission_id: int):
        """Check status of a submission"""
        test_name = f"TEST-SUB-003: Get submission {submission_id} status"
        try:
            response = self.session.get(
                f"{BACKEND_URL}/api/submissions/{submission_id}/status",
                timeout=10
            )

            if response.status_code == 200:
                data = response.json()
                status = data.get("status")
                self.result.add_pass(f"{test_name} (Status: {status})")
            else:
                self.result.add_fail(test_name, f"Status: {response.status_code}")
        except Exception as e:
            self.result.add_fail(test_name, str(e))

    def test_grades(self):
        """Test grade management"""
        self.print_header("Grade Management Tests")

        # Login as teacher
        if not self.login("teacher"):
            self.result.add_skip("Grade tests", "Teacher login failed")
            return

        # Get pending grades
        test_name = "TEST-GRADE-001: Get teacher pending grades"
        try:
            teacher_id = self.current_user.get("id")
            response = self.session.get(
                f"{BACKEND_URL}/api/grades/teacher/{teacher_id}/pending",
                params={"page": 0, "size": 20},
                timeout=10
            )

            if response.status_code == 200:
                data = response.json()
                grades = data.get("content", [])
                self.result.add_pass(f"{test_name} (Found {len(grades)} pending)")
            else:
                self.result.add_fail(test_name, f"Status: {response.status_code}")
        except Exception as e:
            self.result.add_fail(test_name, str(e))

        # Get student grades (switch to student)
        if self.login("student"):
            test_name = "TEST-GRADE-005: Get student grades"
            try:
                student_id = self.current_user.get("id")
                response = self.session.get(
                    f"{BACKEND_URL}/api/grades/student/{student_id}",
                    timeout=10
                )

                if response.status_code == 200:
                    grades = response.json()
                    self.result.add_pass(f"{test_name} (Found {len(grades)} grades)")
                else:
                    self.result.add_fail(test_name, f"Status: {response.status_code}")
            except Exception as e:
                self.result.add_fail(test_name, str(e))

    def test_appeals(self):
        """Test appeal workflow"""
        self.print_header("Appeal Tests")

        # Login as student
        if not self.login("student"):
            self.result.add_skip("Appeal tests", "Student login failed")
            return

        # Get student appeals
        test_name = "TEST-APPEAL-002: Get student appeals"
        try:
            student_id = self.current_user.get("id")
            response = self.session.get(
                f"{BACKEND_URL}/api/appeals/student/{student_id}",
                timeout=10
            )

            if response.status_code == 200:
                appeals = response.json()
                self.result.add_pass(f"{test_name} (Found {len(appeals)} appeals)")
            else:
                self.result.add_fail(test_name, f"Status: {response.status_code}")
        except Exception as e:
            self.result.add_fail(test_name, str(e))

        # Get teacher pending appeals
        if self.login("teacher"):
            test_name = "TEST-APPEAL-003: Get teacher pending appeals"
            try:
                teacher_id = self.current_user.get("id")
                response = self.session.get(
                    f"{BACKEND_URL}/api/appeals/teacher/{teacher_id}/pending",
                    timeout=10
                )

                if response.status_code == 200:
                    appeals = response.json()
                    self.result.add_pass(f"{test_name} (Found {len(appeals)} pending)")
                else:
                    self.result.add_fail(test_name, f"Status: {response.status_code}")
            except Exception as e:
                self.result.add_fail(test_name, str(e))

    def run_all_tests(self):
        """Run all test suites"""
        print(f"{Colors.BLUE}")
        print("╔════════════════════════════════════════════════╗")
        print("║  Intelligent Grading System - Test Suite      ║")
        print("║  Python Automated Testing                     ║")
        print("╚════════════════════════════════════════════════╝")
        print(f"{Colors.ENDC}")

        # Check system health first
        if not self.check_system_health():
            print(f"\n{Colors.RED}System health check failed. Aborting tests.{Colors.ENDC}")
            return False

        # Run all test suites
        self.test_authentication()
        self.test_courses()
        self.test_assignments()
        self.test_submission_workflow()
        self.test_grades()
        self.test_appeals()

        # Print summary
        self.result.print_summary()
        self.result.save_report("test_report.json")

        return self.result.failed == 0

def main():
    """Main entry point"""
    suite = TestSuite()

    # Check command line arguments
    if len(sys.argv) > 1:
        test_category = sys.argv[1]
        if test_category == "auth":
            suite.test_authentication()
        elif test_category == "courses":
            suite.test_courses()
        elif test_category == "assignments":
            suite.test_assignments()
        elif test_category == "submissions":
            suite.test_submission_workflow()
        elif test_category == "grades":
            suite.test_grades()
        elif test_category == "appeals":
            suite.test_appeals()
        else:
            print(f"{Colors.RED}Unknown test category: {test_category}{Colors.ENDC}")
            print("Available: auth, courses, assignments, submissions, grades, appeals")
            sys.exit(1)

        suite.result.print_summary()
        suite.result.save_report("test_report.json")
    else:
        # Run all tests
        success = suite.run_all_tests()
        sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()
