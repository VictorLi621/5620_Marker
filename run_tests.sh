#!/bin/bash

# Intelligent Grading System - Automated Test Script
# This script runs comprehensive API tests for the system

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BACKEND_URL="http://localhost:8080"
FRONTEND_URL="http://localhost:8081"
TEST_FILES_DIR="./test_files"

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Test results array
declare -a FAILED_TEST_NAMES=()

# Functions
print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_test() {
    echo -e "${YELLOW}Running: $1${NC}"
}

print_pass() {
    echo -e "${GREEN}✓ PASSED: $1${NC}"
    ((PASSED_TESTS++))
    ((TOTAL_TESTS++))
}

print_fail() {
    echo -e "${RED}✗ FAILED: $1${NC}"
    echo -e "${RED}  Error: $2${NC}"
    ((FAILED_TESTS++))
    ((TOTAL_TESTS++))
    FAILED_TEST_NAMES+=("$1")
}

check_response() {
    local test_name=$1
    local response=$2
    local expected_code=$3
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')

    if [ "$http_code" == "$expected_code" ]; then
        print_pass "$test_name"
        return 0
    else
        print_fail "$test_name" "Expected HTTP $expected_code, got $http_code"
        return 1
    fi
}

# Check if system is running
check_system() {
    print_header "Checking System Health"

    print_test "Backend health check"
    if curl -s "$BACKEND_URL/actuator/health" > /dev/null 2>&1; then
        print_pass "Backend is running"
    else
        print_fail "Backend health check" "Backend is not responding"
        echo -e "${RED}Please start the system with: docker-compose up -d${NC}"
        exit 1
    fi

    print_test "Frontend health check"
    if curl -s "$FRONTEND_URL" > /dev/null 2>&1; then
        print_pass "Frontend is running"
    else
        print_fail "Frontend health check" "Frontend is not responding"
    fi

    echo ""
}

# Authentication Tests
test_authentication() {
    print_header "Authentication Tests"

    # TEST-AUTH-001: Login - Student
    print_test "TEST-AUTH-001: Login as Student"
    response=$(curl -s -w "\n%{http_code}" -X POST "$BACKEND_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username": "student", "password": "password"}')
    check_response "TEST-AUTH-001" "$response" "200"

    # TEST-AUTH-002: Login - Teacher
    print_test "TEST-AUTH-002: Login as Teacher"
    response=$(curl -s -w "\n%{http_code}" -X POST "$BACKEND_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username": "teacher", "password": "password"}')
    check_response "TEST-AUTH-002" "$response" "200"

    # TEST-AUTH-003: Login - Invalid Credentials
    print_test "TEST-AUTH-003: Login with invalid credentials"
    response=$(curl -s -w "\n%{http_code}" -X POST "$BACKEND_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username": "invalid", "password": "wrong"}')
    check_response "TEST-AUTH-003" "$response" "400"

    echo ""
}

# Course Management Tests
test_courses() {
    print_header "Course Management Tests"

    # TEST-COURSE-001: Create Course
    print_test "TEST-COURSE-001: Create course"
    response=$(curl -s -w "\n%{http_code}" -X POST "$BACKEND_URL/api/courses/create" \
        -H "Content-Type: application/json" \
        -d '{
            "courseCode": "AUTOTEST'$(date +%s)'",
            "courseName": "Automated Test Course",
            "description": "Created by automated test",
            "createdBy": 3
        }')
    check_response "TEST-COURSE-001" "$response" "200"

    # TEST-COURSE-002: Get All Courses
    print_test "TEST-COURSE-002: Get all courses"
    response=$(curl -s -w "\n%{http_code}" "$BACKEND_URL/api/courses")
    check_response "TEST-COURSE-002" "$response" "200"

    echo ""
}

# Assignment Tests
test_assignments() {
    print_header "Assignment Tests"

    # TEST-ASSIGN-002: Get Teacher Assignments
    print_test "TEST-ASSIGN-002: Get teacher assignments"
    response=$(curl -s -w "\n%{http_code}" "$BACKEND_URL/api/assignments?teacherId=2&page=0&size=20")
    check_response "TEST-ASSIGN-002" "$response" "200"

    echo ""
}

# Submission Tests
test_submissions() {
    print_header "Submission Tests"

    # Check if test files exist
    if [ ! -f "$TEST_FILES_DIR/Anonymization test2.docx" ]; then
        print_fail "TEST-SUB-001" "Test file not found: $TEST_FILES_DIR/Anonymization test2.docx"
    else
        # TEST-SUB-001: Upload Word Document
        print_test "TEST-SUB-001: Upload Word document"
        response=$(curl -s -w "\n%{http_code}" -X POST "$BACKEND_URL/api/submissions" \
            -F "studentId=1" \
            -F "assignmentId=1" \
            -F "file=@$TEST_FILES_DIR/Anonymization test2.docx")

        http_code=$(echo "$response" | tail -n1)
        if [ "$http_code" == "200" ]; then
            submission_id=$(echo "$response" | sed '$d' | grep -o '"submissionId":[0-9]*' | grep -o '[0-9]*')
            print_pass "TEST-SUB-001 (Submission ID: $submission_id)"

            # Wait a bit for processing
            sleep 2

            # TEST-SUB-003: Get Submission Status
            print_test "TEST-SUB-003: Get submission status"
            response=$(curl -s -w "\n%{http_code}" "$BACKEND_URL/api/submissions/$submission_id/status")
            check_response "TEST-SUB-003" "$response" "200"

            # TEST-SUB-004: Get Submission Details
            print_test "TEST-SUB-004: Get submission details"
            response=$(curl -s -w "\n%{http_code}" "$BACKEND_URL/api/submissions/$submission_id")
            check_response "TEST-SUB-004" "$response" "200"

            # TEST-SUB-005: Get Submission Content
            print_test "TEST-SUB-005: Get submission content"
            response=$(curl -s -w "\n%{http_code}" "$BACKEND_URL/api/submissions/$submission_id/content")
            check_response "TEST-SUB-005" "$response" "200"
        else
            print_fail "TEST-SUB-001" "HTTP $http_code"
        fi
    fi

    echo ""
}

# Grade Tests
test_grades() {
    print_header "Grade Tests"

    # TEST-GRADE-001: Get Teacher Pending Grades
    print_test "TEST-GRADE-001: Get teacher pending grades"
    response=$(curl -s -w "\n%{http_code}" "$BACKEND_URL/api/grades/teacher/2/pending?page=0&size=20")
    check_response "TEST-GRADE-001" "$response" "200"

    # TEST-GRADE-005: Get Student Grades
    print_test "TEST-GRADE-005: Get student grades"
    response=$(curl -s -w "\n%{http_code}" "$BACKEND_URL/api/grades/student/1")
    check_response "TEST-GRADE-005" "$response" "200"

    echo ""
}

# Appeal Tests
test_appeals() {
    print_header "Appeal Tests"

    # TEST-APPEAL-002: Get Student Appeals
    print_test "TEST-APPEAL-002: Get student appeals"
    response=$(curl -s -w "\n%{http_code}" "$BACKEND_URL/api/appeals/student/1")
    check_response "TEST-APPEAL-002" "$response" "200"

    # TEST-APPEAL-003: Get Teacher Pending Appeals
    print_test "TEST-APPEAL-003: Get teacher pending appeals"
    response=$(curl -s -w "\n%{http_code}" "$BACKEND_URL/api/appeals/teacher/2/pending")
    check_response "TEST-APPEAL-003" "$response" "200"

    echo ""
}

# Class Management Tests
test_classes() {
    print_header "Class Management Tests"

    # TEST-CLASS-002: Add Student to Class
    print_test "TEST-CLASS-002: Add student to class (may fail if already added)"
    response=$(curl -s -w "\n%{http_code}" -X POST "$BACKEND_URL/api/classes/1/add-student" \
        -H "Content-Type: application/json" \
        -d '{"studentId": 1}')
    # This might fail if student already in class, that's okay
    http_code=$(echo "$response" | tail -n1)
    if [ "$http_code" == "200" ] || [ "$http_code" == "400" ]; then
        print_pass "TEST-CLASS-002 (status: $http_code)"
        ((TOTAL_TESTS++))
        ((PASSED_TESTS++))
    else
        print_fail "TEST-CLASS-002" "Unexpected status: $http_code"
    fi

    echo ""
}

# Generate Report
generate_report() {
    print_header "Test Execution Summary"

    echo -e "Total Tests:  ${BLUE}$TOTAL_TESTS${NC}"
    echo -e "Passed Tests: ${GREEN}$PASSED_TESTS${NC}"
    echo -e "Failed Tests: ${RED}$FAILED_TESTS${NC}"

    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}All tests passed! ✓${NC}"
        PASS_RATE=100
    else
        PASS_RATE=$((PASSED_TESTS * 100 / TOTAL_TESTS))
        echo -e "Pass Rate:    ${YELLOW}$PASS_RATE%${NC}"
        echo ""
        echo -e "${RED}Failed Tests:${NC}"
        for test_name in "${FAILED_TEST_NAMES[@]}"; do
            echo -e "${RED}  - $test_name${NC}"
        done
    fi

    echo ""
    echo "Test execution completed at: $(date)"

    # Save report to file
    {
        echo "Test Execution Report"
        echo "Date: $(date)"
        echo "Environment: Local Development"
        echo ""
        echo "Summary:"
        echo "- Total Tests: $TOTAL_TESTS"
        echo "- Passed: $PASSED_TESTS"
        echo "- Failed: $FAILED_TESTS"
        echo "- Pass Rate: $PASS_RATE%"
        echo ""
        if [ $FAILED_TESTS -gt 0 ]; then
            echo "Failed Tests:"
            for test_name in "${FAILED_TEST_NAMES[@]}"; do
                echo "  - $test_name"
            done
        fi
    } > test_report.txt

    echo -e "${BLUE}Report saved to: test_report.txt${NC}"
}

# Main execution
main() {
    echo -e "${BLUE}"
    echo "╔════════════════════════════════════════════════╗"
    echo "║  Intelligent Grading System - Test Suite      ║"
    echo "║  Automated API Testing                         ║"
    echo "╚════════════════════════════════════════════════╝"
    echo -e "${NC}"

    # Check command line arguments
    TEST_CATEGORY=${1:-all}

    # Always check system first
    check_system

    # Run tests based on category
    case $TEST_CATEGORY in
        all)
            test_authentication
            test_courses
            test_classes
            test_assignments
            test_submissions
            test_grades
            test_appeals
            ;;
        auth)
            test_authentication
            ;;
        courses)
            test_courses
            ;;
        classes)
            test_classes
            ;;
        assignments)
            test_assignments
            ;;
        submissions)
            test_submissions
            ;;
        grades)
            test_grades
            ;;
        appeals)
            test_appeals
            ;;
        *)
            echo -e "${RED}Unknown test category: $TEST_CATEGORY${NC}"
            echo "Available categories: all, auth, courses, classes, assignments, submissions, grades, appeals"
            exit 1
            ;;
    esac

    # Generate final report
    generate_report

    # Exit with appropriate code
    if [ $FAILED_TESTS -eq 0 ]; then
        exit 0
    else
        exit 1
    fi
}

# Run main function
main "$@"
