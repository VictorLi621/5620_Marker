import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Tag, Space, message, Spin, Descriptions, Statistic, Row, Col, Modal, Empty } from 'antd';
import { ArrowLeftOutlined, FileTextOutlined, CheckCircleOutlined, ClockCircleOutlined, EditOutlined } from '@ant-design/icons';
import axios from 'axios';

const AssignmentDetails = ({ user, onNavigate, assignmentId }) => {
  const [assignment, setAssignment] = useState(null);
  const [submissions, setSubmissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [previewModalVisible, setPreviewModalVisible] = useState(false);
  const [selectedSubmission, setSelectedSubmission] = useState(null);
  const [submissionContent, setSubmissionContent] = useState(null);
  const [loadingContent, setLoadingContent] = useState(false);

  useEffect(() => {
    fetchAssignmentDetails();
  }, [assignmentId]);

  const fetchAssignmentDetails = async () => {
    try {
      setLoading(true);
      const response = await axios.get(`/api/assignments/${assignmentId}/details`);
      setAssignment(response.data);
      setSubmissions(response.data.submissions || []);
    } catch (error) {
      console.error('Failed to fetch assignment details:', error);
      message.error('Failed to load assignment details: ' + (error.response?.data?.error || error.message));
    } finally {
      setLoading(false);
    }
  };

  const handleViewSubmission = async (submission) => {
    setSelectedSubmission(submission);
    setLoadingContent(true);
    setPreviewModalVisible(true);

    try {
      const response = await axios.get(`/api/submissions/${submission.submissionId}/content`);
      setSubmissionContent(response.data);
    } catch (error) {
      console.error('Failed to load submission content:', error);
      message.error('Failed to load submission content');
      setPreviewModalVisible(false);
    } finally {
      setLoadingContent(false);
    }
  };

  const handleReviewSubmission = (submission) => {
    // Navigate to review page
    onNavigate('review', { submissionId: submission.submissionId });
  };

  const columns = [
    {
      title: 'Student',
      dataIndex: 'studentName',
      key: 'studentName',
      render: (name, record) => (
        <Space direction="vertical" size={0}>
          <strong>{name}</strong>
          <span style={{ fontSize: '12px', color: '#888' }}>ID: {record.studentId}</span>
        </Space>
      ),
    },
    {
      title: 'File Name',
      dataIndex: 'originalFileName',
      key: 'originalFileName',
      ellipsis: true,
    },
    {
      title: 'Submitted At',
      dataIndex: 'submittedAt',
      key: 'submittedAt',
      render: (date) => new Date(date).toLocaleString('en-US'),
      sorter: (a, b) => new Date(a.submittedAt) - new Date(b.submittedAt),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        const colorMap = {
          UPLOADED: 'blue',
          OCR_PROCESSING: 'cyan',
          ANONYMIZING: 'purple',
          SCORING: 'orange',
          SCORED: 'green',
          REVIEWED: 'lime',
          PUBLISHED: 'success',
          FAILED: 'red'
        };
        return <Tag color={colorMap[status] || 'default'}>{status}</Tag>;
      },
      filters: [
        { text: 'Uploaded', value: 'UPLOADED' },
        { text: 'Processing', value: 'OCR_PROCESSING' },
        { text: 'Scored', value: 'SCORED' },
        { text: 'Published', value: 'PUBLISHED' },
      ],
      onFilter: (value, record) => record.status === value,
    },
    {
      title: 'AI Score',
      dataIndex: 'aiScore',
      key: 'aiScore',
      align: 'center',
      render: (score) => score ? <Tag color="blue">{score}</Tag> : '-',
      sorter: (a, b) => (a.aiScore || 0) - (b.aiScore || 0),
    },
    {
      title: 'Teacher Score',
      dataIndex: 'teacherScore',
      key: 'teacherScore',
      align: 'center',
      render: (score) => score ? <Tag color="green">{score}</Tag> : '-',
      sorter: (a, b) => (a.teacherScore || 0) - (b.teacherScore || 0),
    },
    {
      title: 'Final Score',
      dataIndex: 'finalScore',
      key: 'finalScore',
      align: 'center',
      render: (score) => score ? <strong style={{ fontSize: '16px', color: '#1890ff' }}>{score}</strong> : '-',
      sorter: (a, b) => (a.finalScore || 0) - (b.finalScore || 0),
    },
    {
      title: 'Grade Status',
      dataIndex: 'gradeStatus',
      key: 'gradeStatus',
      render: (status) => {
        if (!status) return '-';
        const colorMap = {
          HIGH_CONFIDENCE: 'green',
          NEEDS_REVIEW: 'orange',
          APPROVED: 'blue',
          PUBLISHED: 'success',
          APPEALED: 'red'
        };
        return <Tag color={colorMap[status] || 'default'}>{status}</Tag>;
      },
    },
    {
      title: 'Actions',
      key: 'actions',
      align: 'center',
      render: (_, record) => (
        <Space>
          <Button
            size="small"
            icon={<FileTextOutlined />}
            onClick={() => handleViewSubmission(record)}
          >
            View
          </Button>
          {record.gradeId && (
            <Button
              size="small"
              type="primary"
              icon={<EditOutlined />}
              onClick={() => handleReviewSubmission(record)}
            >
              Review
            </Button>
          )}
        </Space>
      ),
    },
  ];

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" tip="Loading assignment details..." />
      </div>
    );
  }

  if (!assignment) {
    return (
      <Card>
        <Empty description="Assignment not found" />
      </Card>
    );
  }

  return (
    <div>
      <Button
        icon={<ArrowLeftOutlined />}
        onClick={() => onNavigate('assignments')}
        style={{ marginBottom: 16 }}
      >
        Back to Assignments
      </Button>

      {/* Assignment Information */}
      <Card title="Assignment Information" style={{ marginBottom: 16 }}>
        <Descriptions bordered column={2}>
          <Descriptions.Item label="Title" span={2}>
            <strong style={{ fontSize: '16px' }}>{assignment.title}</strong>
          </Descriptions.Item>
          <Descriptions.Item label="Course Code">
            <Tag color="blue">{assignment.courseCode}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Total Marks">
            <Tag color="purple">{assignment.totalMarks} points</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Status">
            <Tag color={assignment.status === 'PUBLISHED' ? 'green' : 'orange'}>
              {assignment.status}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Due Date">
            {assignment.dueDate ? new Date(assignment.dueDate).toLocaleString('en-US') : 'No due date'}
          </Descriptions.Item>
          <Descriptions.Item label="Teacher" span={2}>
            {assignment.teacherName}
          </Descriptions.Item>
          <Descriptions.Item label="Description" span={2}>
            {assignment.description || 'No description'}
          </Descriptions.Item>
          {assignment.instructions && (
            <Descriptions.Item label="Instructions" span={2}>
              {assignment.instructions}
            </Descriptions.Item>
          )}
        </Descriptions>
      </Card>

      {/* Statistics */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="Total Submissions"
              value={assignment.totalSubmissions}
              prefix={<FileTextOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Scored"
              value={assignment.scoredCount}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Published"
              value={assignment.publishedCount}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Pending Review"
              value={assignment.pendingCount}
              prefix={<ClockCircleOutlined />}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Submissions Table */}
      <Card title="Student Submissions">
        {submissions.length === 0 ? (
          <Empty description="No submissions yet" />
        ) : (
          <Table
            columns={columns}
            dataSource={submissions}
            rowKey="submissionId"
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showTotal: (total) => `Total ${total} submissions`,
            }}
          />
        )}
      </Card>

      {/* Preview Modal */}
      <Modal
        title={`View Submission - ${submissionContent?.originalFileName || ''}`}
        open={previewModalVisible}
        onCancel={() => {
          setPreviewModalVisible(false);
          setSubmissionContent(null);
          setSelectedSubmission(null);
        }}
        footer={[
          <Button key="close" onClick={() => setPreviewModalVisible(false)}>
            Close
          </Button>,
          selectedSubmission?.gradeId && (
            <Button
              key="review"
              type="primary"
              onClick={() => {
                setPreviewModalVisible(false);
                handleReviewSubmission(selectedSubmission);
              }}
            >
              Go to Review
            </Button>
          ),
        ]}
        width={900}
      >
        {loadingContent ? (
          <div style={{ textAlign: 'center', padding: '40px' }}>
            <Spin tip="Loading..." />
          </div>
        ) : submissionContent ? (
          <div>
            <Descriptions bordered size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="File Name" span={3}>
                {submissionContent.originalFileName}
              </Descriptions.Item>
              <Descriptions.Item label="Student" span={3}>
                {selectedSubmission?.studentName}
              </Descriptions.Item>
            </Descriptions>

            <h4>Original Submission Content (OCR Extracted):</h4>
            <div style={{
              maxHeight: '300px',
              overflow: 'auto',
              padding: '12px',
              background: '#f5f5f5',
              borderRadius: '4px',
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-word',
              fontFamily: 'monospace',
              fontSize: '13px',
              marginBottom: '16px'
            }}>
              {submissionContent.ocrText || submissionContent.originalText || '(No text content)'}
            </div>

            <h4 style={{ color: '#1890ff' }}>Anonymized Content (used for AI grading):</h4>
            <div style={{
              maxHeight: '300px',
              overflow: 'auto',
              padding: '12px',
              background: '#e6f7ff',
              borderRadius: '4px',
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-word',
              fontFamily: 'monospace',
              fontSize: '13px',
              border: '1px solid #91d5ff'
            }}>
              {submissionContent.anonymizedText || '(No anonymized text)'}
            </div>

            {submissionContent.originalFileUrl && (
              <div style={{ marginTop: 16 }}>
                <Button
                  type="link"
                  href={submissionContent.originalFileUrl}
                  target="_blank"
                >
                  Download Original File
                </Button>
              </div>
            )}
          </div>
        ) : (
          <Empty description="No content" />
        )}
      </Modal>
    </div>
  );
};

export default AssignmentDetails;
