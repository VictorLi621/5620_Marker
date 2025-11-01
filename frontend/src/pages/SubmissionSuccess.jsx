import React, { useState, useEffect } from 'react';
import { Card, Button, Descriptions, Tag, Spin, Empty, message } from 'antd';
import { CheckCircleOutlined, FileTextOutlined } from '@ant-design/icons';
import axios from 'axios';

const SubmissionSuccess = ({ submissionId, onNavigate }) => {
  const [submission, setSubmission] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (submissionId) {
      fetchSubmissionDetails();
    }
  }, [submissionId]);

  const fetchSubmissionDetails = async () => {
    try {
      const response = await axios.get(`/api/submissions/${submissionId}/content`);
      setSubmission(response.data);
    } catch (error) {
      console.error('Failed to fetch submission details:', error);
      message.error('Failed to load submission details');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" tip="Loading submission details..." />
      </div>
    );
  }

  if (!submission) {
    return (
      <div>
        <h2>Submission Success</h2>
        <Empty description="Submission not found" />
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '1000px', margin: '0 auto' }}>
      {/* Success Header */}
      <Card
        style={{
          marginBottom: 24,
          textAlign: 'center',
          background: 'linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%)',
          border: '2px solid #52c41a'
        }}
      >
        <CheckCircleOutlined style={{ fontSize: '64px', color: '#52c41a', marginBottom: '16px' }} />
        <h1 style={{ color: '#52c41a', marginBottom: '8px' }}>Assignment Submitted Successfully!</h1>
        <p style={{ fontSize: '16px', color: '#666', marginBottom: '8px' }}>
          Your assignment has been received and is being processed.
        </p>
        <Tag color="green" style={{ fontSize: '14px', padding: '4px 12px' }}>
          Submission ID: {submissionId}
        </Tag>
      </Card>

      {/* Submission Details */}
      <Card
        title={
          <span>
            <FileTextOutlined style={{ marginRight: '8px' }} />
            Submission Details
          </span>
        }
        style={{ marginBottom: 24 }}
      >
        <Descriptions bordered column={1}>
          <Descriptions.Item label="File Name">
            {submission.originalFileName}
          </Descriptions.Item>
          <Descriptions.Item label="File Type">
            <Tag color="blue">{submission.fileType?.toUpperCase() || 'UNKNOWN'}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Submitted At">
            {submission.createdAt ? new Date(submission.createdAt).toLocaleString('en-US', {
              year: 'numeric',
              month: 'long',
              day: 'numeric',
              hour: '2-digit',
              minute: '2-digit'
            }) : 'Unknown'}
          </Descriptions.Item>
          <Descriptions.Item label="Status">
            <Tag color={submission.status === 'SCORED' ? 'green' : 'processing'}>
              {submission.status || 'PROCESSING'}
            </Tag>
          </Descriptions.Item>
        </Descriptions>

        {submission.originalFileUrl && (
          <div style={{ marginTop: 16 }}>
            <Button
              type="primary"
              icon={<FileTextOutlined />}
              href={submission.originalFileUrl}
              target="_blank"
              size="large"
            >
              Download Original File
            </Button>
          </div>
        )}
      </Card>

      {/* File Preview */}
      <Card
        title="File Preview (Extracted Text)"
        style={{ marginBottom: 24 }}
      >
        <div style={{
          maxHeight: '400px',
          overflow: 'auto',
          padding: '16px',
          background: '#fafafa',
          borderRadius: '4px',
          border: '1px solid #d9d9d9'
        }}>
          {submission.ocrText || submission.originalText ? (
            <pre style={{
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-word',
              fontFamily: 'monospace',
              fontSize: '13px',
              lineHeight: '1.6',
              margin: 0
            }}>
              {submission.ocrText || submission.originalText}
            </pre>
          ) : (
            <Empty description="No text content available" />
          )}
        </div>
      </Card>

      {/* What's Next Section */}
      <Card
        title="What Happens Next?"
        style={{ marginBottom: 24 }}
      >
        <div style={{ lineHeight: '2' }}>
          <p>
            <strong>1. AI Grading:</strong> Your submission is currently being analyzed by our AI grading system.
          </p>
          <p>
            <strong>2. Teacher Review:</strong> After AI grading, your teacher will review the results and may adjust the score.
          </p>
          <p>
            <strong>3. Notification:</strong> You will be notified when your grade is published and available to view.
          </p>
          <p>
            <strong>4. View Results:</strong> Once published, you can view your detailed feedback and grade in the "View Results" section.
          </p>
        </div>
      </Card>

      {/* Action Buttons */}
      <Card>
        <div style={{ textAlign: 'center' }}>
          <Button
            type="default"
            size="large"
            onClick={() => onNavigate && onNavigate('upload')}
            style={{ marginRight: '12px' }}
          >
            Submit Another Assignment
          </Button>
          <Button
            type="primary"
            size="large"
            onClick={() => onNavigate && onNavigate('results')}
          >
            View My Results
          </Button>
        </div>
        <div style={{ marginTop: 16, textAlign: 'center', color: '#888', fontSize: '13px' }}>
          Note: Grades are not immediately visible. You will be notified when your grade is published.
        </div>
      </Card>
    </div>
  );
};

export default SubmissionSuccess;
