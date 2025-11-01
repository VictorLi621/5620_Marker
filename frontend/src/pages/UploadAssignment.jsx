import React, { useState, useEffect } from 'react';
import { Upload, Button, Select, Card, message, Progress, Tag } from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import axios from 'axios';

const { Dragger } = Upload;

const UploadAssignment = ({ user, onNavigate }) => {
  const [assignments, setAssignments] = useState([]);
  const [selectedAssignment, setSelectedAssignment] = useState(null);
  const [fileList, setFileList] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [submissionId, setSubmissionId] = useState(null);
  const [processingStatus, setProcessingStatus] = useState(null);

  useEffect(() => {
    fetchAssignments();
  }, []);

  useEffect(() => {
    if (submissionId && processingStatus !== 'SCORED' && processingStatus !== 'FAILED') {
      const interval = setInterval(pollSubmissionStatus, 2000);
      return () => clearInterval(interval);
    }
  }, [submissionId, processingStatus]);

  const fetchAssignments = async () => {
    try {
      // Fetch all assignments
      const response = await axios.get('/api/assignments?page=0&size=100');
      const assignmentsData = response.data.content || response.data || [];
      
      // ✅ Only check enrollment for STUDENT role
      if (user.role === 'STUDENT') {
        // Fetch student's enrollment list
        const enrollmentsRes = await axios.get(`/api/enrollments/student/${user.id}`);
        const enrollments = enrollmentsRes.data || [];
        const enrolledCourseCodes = enrollments.map(e => e.courseCode);
        
        console.log('🎓 Student enrolled courses:', enrolledCourseCodes);
        console.log('📚 All assignments:', assignmentsData.map(a => ({
          id: a.id,
          title: a.title,
          courseCode: a.courseCode
        })));
        
        // Only show assignments from enrolled courses
        const filteredAssignments = assignmentsData.filter(a => 
          enrolledCourseCodes.includes(a.courseCode)
        );
        
        console.log('✅ Filtered assignments:', filteredAssignments);
        console.log('❌ Excluded assignments:', assignmentsData.filter(a => 
          !enrolledCourseCodes.includes(a.courseCode)
        ).map(a => ({
          id: a.id,
          title: a.title,
          courseCode: a.courseCode,
          reason: `Course code mismatch: "${a.courseCode}" not in [${enrolledCourseCodes.join(', ')}]`
        })));
        
        setAssignments(filteredAssignments);
        
        if (filteredAssignments.length === 0 && assignmentsData.length > 0) {
          message.warning('You have not enrolled in any courses. Please contact the technical team to add course enrollment.');
        }
      } else {
        // Admin/Technical Team/Teacher: show all assignments
        setAssignments(assignmentsData);
      }
    } catch (error) {
      console.error('Failed to fetch assignments:', error);
      message.error('Failed to load assignment list: ' + (error.response?.data?.error || error.message));
    }
  };

  const pollSubmissionStatus = async () => {
    try {
      const response = await axios.get(`/api/submissions/${submissionId}/status`);
      setProcessingStatus(response.data.status);
      
      if (response.data.status === 'SCORED') {
        message.success('✅ Assignment processing complete! AI grading generated');
      } else if (response.data.status === 'FAILED') {
        message.error('Processing failed: ' + response.data.error);
      }
    } catch (error) {
      console.error('Status poll failed', error);
    }
  };

  const uploadProps = {
    name: 'file',
    multiple: false,
    fileList,
    beforeUpload: (file) => {
      setFileList([file]);
      return false;
    },
    onRemove: () => {
      setFileList([]);
    },
  };

  const handleUpload = async () => {
    if (!selectedAssignment) {
      message.warning('Please select an assignment');
      return;
    }
    if (fileList.length === 0) {
      message.warning('Please select a file');
      return;
    }

    const formData = new FormData();
    formData.append('file', fileList[0]);
    formData.append('studentId', user.id);
    formData.append('assignmentId', selectedAssignment);

    setUploading(true);
    try {
      const response = await axios.post('/api/submissions', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      
      if (response.data.success) {
        message.success('Upload successful! Processing...');
        setSubmissionId(response.data.submissionId);
        setProcessingStatus(response.data.status);
        setFileList([]);
      } else {
        message.error('Upload failed: ' + response.data.error);
      }
    } catch (error) {
      message.error('Upload failed: ' + (error.response?.data?.error || error.message));
    } finally {
      setUploading(false);
    }
  };

  const getStatusDisplay = () => {
    const statusMap = {
      'UPLOADED': { text: 'Uploaded', percent: 20, status: 'active' },
      'OCR_PROCESSING': { text: 'OCR Processing', percent: 40, status: 'active' },
      'ANONYMIZING': { text: 'Anonymizing', percent: 60, status: 'active' },
      'SCORING': { text: 'AI Scoring', percent: 80, status: 'active' },
      'SCORED': { text: 'Grading Complete', percent: 100, status: 'success' },
      'FAILED': { text: 'Processing Failed', percent: 0, status: 'exception' },
    };
    return statusMap[processingStatus] || { text: 'Unknown', percent: 0, status: 'normal' };
  };

  return (
    <div>
      <h2>📤 Submit Assignment</h2>
      
      <Card style={{ marginBottom: 16 }}>
        <div style={{ marginBottom: 16 }}>
          <label style={{ fontWeight: 'bold', marginRight: 8 }}>Select Assignment:</label>
          <Select
            style={{ width: 400 }}
            placeholder="Select an assignment"
            onChange={setSelectedAssignment}
            value={selectedAssignment}
          >
            {assignments.map(a => (
              <Select.Option key={a.id} value={a.id}>
                {a.courseCode} - {a.title} (Total: {a.totalMarks})
              </Select.Option>
            ))}
          </Select>
        </div>

        <Dragger {...uploadProps} accept=".pdf,.doc,.docx,.jpg,.png">
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">Click or drag file to this area to upload</p>
          <p className="ant-upload-hint">
            Supports PDF, Word, and image formats. Files will be automatically processed with OCR and anonymization.
          </p>
        </Dragger>

        <Button
          type="primary"
          onClick={handleUpload}
          loading={uploading}
          disabled={fileList.length === 0 || !selectedAssignment}
          style={{ marginTop: 16 }}
          size="large"
        >
          {uploading ? 'Uploading...' : 'Submit Assignment'}
        </Button>
      </Card>

      {processingStatus && (
        <Card title="Processing Progress">
          <Progress 
            percent={getStatusDisplay().percent} 
            status={getStatusDisplay().status}
          />
          <div style={{ marginTop: 16 }}>
            <Tag color="blue">{getStatusDisplay().text}</Tag>
            {submissionId && <span style={{ marginLeft: 8 }}>Submission ID: {submissionId}</span>}
          </div>
          {processingStatus === 'SCORED' && (
            <div style={{ marginTop: 16 }}>
              <p>✅ Processing complete! AI has finished grading, awaiting teacher review.</p>
              <Button
                type="primary"
                onClick={() => {
                  if (onNavigate) {
                    onNavigate('submission-success', { submissionId });
                  }
                }}
              >
                View Submission Details
              </Button>
            </div>
          )}
        </Card>
      )}
    </div>
  );
};

export default UploadAssignment;
