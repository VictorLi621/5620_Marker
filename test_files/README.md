# Test Files Directory

This directory contains sample files used for testing the Intelligent Grading System.

## Available Test Files

### 1. Anonymization test2.docx
- **Type:** Microsoft Word Document (.docx)
- **Purpose:** Test document submission and OCR extraction
- **Use Cases:**
  - Testing Word document upload
  - Testing text extraction from Word files
  - Testing anonymization of student information
  - Testing AI grading on Word documents

### 2. ELEC5620 Project Stage 2 Marking Criteria.pdf
- **Type:** PDF Document (.pdf)
- **Purpose:** Test PDF submission and processing
- **Use Cases:**
  - Testing PDF upload
  - Testing PDF text extraction
  - Testing OCR on PDF files
  - Testing AI grading on PDF documents

## How to Use Test Files

### Automated Testing

Both test scripts automatically use these files:

```bash
# Bash script uses these files
./run_tests.sh submissions

# Python script uses these files
python3 test_suite.py submissions
```

### Manual Testing

1. **Login to system** as student (username: `student`, password: `password`)
2. **Navigate to** "Upload Assignment"
3. **Select file** from this directory
4. **Upload** and wait for processing
5. **Check results** in "View Results"

### API Testing with curl

```bash
# Upload Word document
curl -X POST http://localhost:8080/api/submissions \
  -F "studentId=1" \
  -F "assignmentId=1" \
  -F "file=@test_files/Anonymization test2.docx"

# Upload PDF
curl -X POST http://localhost:8080/api/submissions \
  -F "studentId=1" \
  -F "assignmentId=1" \
  -F "file=@test_files/ELEC5620 Project Stage 2 Marking Criteria.pdf"
```

## Adding Your Own Test Files

You can add additional test files to this directory for testing:

### Supported File Types
- **PDF** (.pdf)
- **Word** (.docx, .doc)
- **Images** (.jpg, .jpeg, .png) - for Vision API testing

### File Naming Conventions
- Use descriptive names
- Avoid spaces (use underscores or hyphens)
- Include file type in name if helpful

**Examples:**
- `student_essay_sample.docx`
- `math_homework_test.pdf`
- `handwritten_assignment.jpg`

## Test File Requirements

For best testing results:
- **File size:** Keep under 10MB for faster processing
- **Content:** Include some text content for OCR
- **Format:** Use standard formats (not corrupted files)
- **PII:** Can include sample student names/IDs to test anonymization

## Expected Processing Flow

When you upload a test file:

1. **Upload** → File saved to storage (Aliyun OSS or local)
2. **OCR Processing** → Text extraction from PDF/Word/Image
3. **Anonymization** → Remove student PII (names, IDs, emails)
4. **Scoring** → AI analyzes content and assigns score
5. **Feedback** → AI generates detailed feedback
6. **Complete** → Submission marked as SCORED

This typically takes 10-30 seconds depending on file size and AI API response time.

## Troubleshooting

### Issue: File not found during tests

**Solution:**
```bash
# Verify files exist
ls -la test_files/

# Check permissions
chmod 644 test_files/*
```

### Issue: Upload fails with size error

**Solution:** Reduce file size or check backend configuration for max upload size.

### Issue: OCR extraction fails

**Solution:**
- Ensure file is not corrupted
- Check backend logs: `docker-compose logs backend`
- Verify OpenAI Vision API is enabled

## Testing Different Scenarios

### Scenario 1: Test OCR Quality
Use `Anonymization test2.docx` which has clear text for good OCR results.

### Scenario 2: Test PDF Processing
Use the PDF marking criteria which has formatted text and tables.

### Scenario 3: Test Large Files
Add a larger PDF (5-10MB) to test performance.

### Scenario 4: Test Image Processing
Add a handwritten assignment image to test Vision API.

## File Metadata

| File | Size | Pages/Length | Best For |
|------|------|--------------|----------|
| Anonymization test2.docx | ~12KB | Short | Quick testing, anonymization |
| ELEC5620 Project...pdf | ~107KB | Multi-page | PDF processing, complex layout |

## Integration with Test Suite

These files are referenced in:
- **TEST_PLAN.md** → Test cases TEST-SUB-001, TEST-SUB-002
- **run_tests.sh** → Submission tests
- **test_suite.py** → Submission workflow tests

## Version Control Note

These files are committed to git for testing purposes. In a production environment, actual student submissions should NOT be committed to version control.

---

**Need more test files?**
Contact the team or add your own following the guidelines above.
