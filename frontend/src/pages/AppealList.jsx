import React, { useState, useEffect } from 'react';
import { Card, Button, Modal, Input, InputNumber, message, Tag, Space, Descriptions, List, Spin, Empty } from 'antd';
import axios from 'axios';

const { TextArea } = Input;

const AppealList = ({ user }) => {
  const [appeals, setAppeals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [resolveModalVisible, setResolveModalVisible] = useState(false);
  const [currentAppeal, setCurrentAppeal] = useState(null);
  const [resolution, setResolution] = useState('');
  const [newScore, setNewScore] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [previewModalVisible, setPreviewModalVisible] = useState(false);
  const [submissionContent, setSubmissionContent] = useState(null);
  const [loadingContent, setLoadingContent] = useState(false);

  useEffect(() => {
    fetchAppeals();
  }, []);

  const fetchAppeals = async () => {
    try {
      const response = await axios.get('/api/appeals/pending?page=0&size=50');
      console.log('Fetched appeals:', response.data);
      // Adapt to new pagination format
      const appealsData = response.data.content || response.data || [];
      setAppeals(appealsData);
    } catch (error) {
      console.error('Failed to fetch appeals:', error);
      message.error('Failed to load appeal list');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenResolve = (appeal) => {
    setCurrentAppeal(appeal);
    setNewScore(appeal.currentScore);
    setResolveModalVisible(true);
  };

  const handleResolve = async (status) => {
    if (!resolution.trim()) {
      message.warning('Please enter resolution details');
      return;
    }

    // No new score needed for rejection, but required for approval
    if (status === 'APPROVED' && (newScore === null || newScore === undefined)) {
      message.warning('New score is required when approving an appeal');
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        teacherId: user.id,
        status,
        resolution
      };
      
      // Only send newScore when approving
      if (status === 'APPROVED') {
        payload.newScore = newScore;
      }
      
      const response = await axios.put(`/api/appeals/${currentAppeal.id}/resolve`, payload);

      if (response.data.success) {
        message.success(`✅ Appeal ${status === 'APPROVED' ? 'approved' : 'rejected'}`);
        setResolveModalVisible(false);
        setResolution('');
        setNewScore(null);
        setCurrentAppeal(null);
        fetchAppeals(); // Refresh list
      } else {
        message.error('Processing failed: ' + response.data.error);
      }
    } catch (error) {
      console.error('Resolve appeal failed:', error);
      message.error('Processing failed: ' + (error.response?.data?.error || error.message));
    } finally {
      setSubmitting(false);
    }
  };

  const handleViewSubmission = async (submissionId) => {
    setLoadingContent(true);
    setPreviewModalVisible(true);
    
    try {
      const response = await axios.get(`/api/submissions/${submissionId}/content`);
      setSubmissionContent(response.data);
    } catch (error) {
      console.error('Failed to load submission content:', error);
      message.error('Failed to load submission content');
      setPreviewModalVisible(false);
    } finally {
      setLoadingContent(false);
    }
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" tip="Loading appeal list..." />
      </div>
    );
  }

  if (appeals.length === 0) {
    return (
      <div>
        <h2>📋 Process Appeals</h2>
        <Empty description="No pending appeals" />
      </div>
    );
  }

  return (
    <div>
      <h2>📋 Process Appeals</h2>

      <List
        dataSource={appeals}
        renderItem={(appeal) => (
          <Card 
            key={appeal.id}
            title={`${appeal.studentName} - ${appeal.assignmentTitle || 'Assignment'}`}
            extra={<Tag color="orange">Pending</Tag>}
            style={{ marginBottom: 16 }}
          >
            <Descriptions bordered column={2}>
              <Descriptions.Item label="Current Score">
                <span style={{ fontSize: '20px', fontWeight: 'bold' }}>
                  {appeal.currentScore}
                </span> / 100
              </Descriptions.Item>
              <Descriptions.Item label="Appeal Time">
                {appeal.createdAt ? new Date(appeal.createdAt).toLocaleString('en-US') : 'Unknown'}
              </Descriptions.Item>
              <Descriptions.Item label="Submission ID" span={2}>
                Submission #{appeal.submissionId}
              </Descriptions.Item>
              <Descriptions.Item label="Appeal Reason" span={2}>
                <div style={{ padding: '12px', background: '#f5f5f5', borderRadius: '4px' }}>
                  {appeal.reason}
                </div>
              </Descriptions.Item>
            </Descriptions>

            <div style={{ marginTop: 16 }}>
              <Space>
                <Button 
                  type="primary"
                  onClick={() => handleOpenResolve(appeal)}
                >
                  ✅ Approve Appeal
                </Button>
                <Button 
                  danger
                  onClick={() => {
                    setCurrentAppeal(appeal);
                    setNewScore(null); // Clear score when rejecting
                    setResolveModalVisible(true);
                  }}
                >
                  ❌ Reject Appeal
                </Button>
                <Button 
                  type="default"
                  onClick={() => handleViewSubmission(appeal.submissionId)}
                >
                  📄 View Submission Details
                </Button>
              </Space>
            </div>
          </Card>
        )}
      />

      {/* Resolve appeal modal */}
      <Modal
        title={`Process Appeal - ${currentAppeal?.studentName || ''}`}
        open={resolveModalVisible}
        onCancel={() => {
          setResolveModalVisible(false);
          setCurrentAppeal(null);
          setResolution('');
          setNewScore(null);
        }}
        footer={[
          <Button key="reject" danger onClick={() => handleResolve('REJECTED')} loading={submitting}>
            ❌ Reject Appeal
          </Button>,
          <Button key="approve" type="primary" onClick={() => handleResolve('APPROVED')} loading={submitting}>
            ✅ Approve Appeal
          </Button>,
        ]}
      >
        {currentAppeal && (
          <>
            <p><strong>Current Score:</strong> {currentAppeal.currentScore}</p>
            <p><strong>Appeal Reason:</strong> {currentAppeal.reason}</p>
            
            <div style={{ marginTop: 16 }}>
              <label style={{ fontWeight: 'bold' }}>New Score (Required when approving):</label>
              <InputNumber
                min={0}
                max={100}
                value={newScore}
                onChange={setNewScore}
                style={{ width: '100%', marginTop: 8 }}
                placeholder="Enter adjusted score (optional for rejection)"
              />
            </div>

            <div style={{ marginTop: 16 }}>
              <label style={{ fontWeight: 'bold', color: 'red' }}>* Resolution Details (Required):</label>
              <TextArea
                rows={4}
                value={resolution}
                onChange={(e) => setResolution(e.target.value)}
                placeholder={`Please enter resolution details (required)\n\nApproval example: After review, your complexity analysis does cover the key points. Score adjusted from 75 to 85.\n\nRejection example: After review, your answer has XXX issues. The grading is reasonable and will not be adjusted.`}
                style={{ marginTop: 8 }}
              />
            </div>
          </>
        )}
      </Modal>

      {/* Preview submission content modal */}
      <Modal
        title={`📄 View Submission - ${submissionContent?.originalFileName || ''}`}
        open={previewModalVisible}
        onCancel={() => {
          setPreviewModalVisible(false);
          setSubmissionContent(null);
        }}
        footer={[
          <Button key="close" onClick={() => setPreviewModalVisible(false)}>
            Close
          </Button>
        ]}
        width={900}
      >
        {loadingContent ? (
          <div style={{ textAlign: 'center', padding: '40px' }}>
            <Spin tip="Loading..." />
          </div>
        ) : submissionContent ? (
          <div>
            <h4>📝 Submission Content (OCR Extracted):</h4>
            <div style={{
              maxHeight: '400px',
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
            
            <h4 style={{ color: '#1890ff' }}>🔒 Anonymized Content:</h4>
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
          </div>
        ) : (
          <Empty description="No content" />
        )}
      </Modal>
    </div>
  );
};

export default AppealList;
