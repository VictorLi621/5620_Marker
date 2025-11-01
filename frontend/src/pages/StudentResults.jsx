import React, { useState, useEffect } from 'react';
import { Card, Button, Modal, Input, message, Descriptions, Tag, Spin, Empty, List } from 'antd';
import axios from 'axios';

const { TextArea } = Input;

const StudentResults = ({ user, onNavigate, selectedGradeId }) => {
  const [appealModalVisible, setAppealModalVisible] = useState(false);
  const [appealReason, setAppealReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(true);
  const [grades, setGrades] = useState([]);
  const [selectedGrade, setSelectedGrade] = useState(null);
  const [appeals, setAppeals] = useState([]);
  const [previewModalVisible, setPreviewModalVisible] = useState(false);
  const [submissionContent, setSubmissionContent] = useState(null);
  const [loadingContent, setLoadingContent] = useState(false);

  useEffect(() => {
    fetchGrades();
  }, [user.id]);

  // Select grade based on selectedGradeId parameter (from navigation)
  useEffect(() => {
    if (selectedGradeId && grades.length > 0) {
      const targetGrade = grades.find(g => g.id === selectedGradeId);
      if (targetGrade) {
        setSelectedGrade(targetGrade);
        fetchAppeals(targetGrade.submissionId);
      }
    }
  }, [selectedGradeId, grades]);

  const fetchGrades = async () => {
    try {
      const response = await axios.get(`/api/grades/student/${user.id}`);
      console.log('Fetched grades:', response.data);
      
      if (response.data && Array.isArray(response.data)) {
        setGrades(response.data);
        if (response.data.length > 0) {
          setSelectedGrade(response.data[0]); // Default to first grade
          fetchAppeals(response.data[0].submissionId); // Fetch appeal records
        }
      } else {
        console.warn('Invalid grades data:', response.data);
        setGrades([]);
      }
    } catch (error) {
      console.error('Failed to fetch grades:', error);
      message.error('Failed to load grades: ' + (error.response?.data?.error || error.message));
      setGrades([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchAppeals = async (submissionId) => {
    if (!submissionId) return;
    
    try {
      const response = await axios.get(`/api/appeals/submission/${submissionId}`);
      console.log('Fetched appeals:', response.data);
      setAppeals(response.data || []);
    } catch (error) {
      console.error('Failed to fetch appeals:', error);
      setAppeals([]);
    }
  };

  const handleAcknowledge = async () => {
    if (!selectedGrade) return;
    
    try {
      const response = await axios.post(`/api/grades/${selectedGrade.id}/acknowledge`, {
        studentId: user.id
      });
      
      if (response.data.success) {
        message.success('✅ Grade acknowledged! System has recorded your confirmation.');
      } else {
        message.error('Acknowledgment failed');
      }
    } catch (error) {
      console.error('Acknowledge failed:', error);
      message.error('Acknowledgment failed: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleViewSubmission = async () => {
    if (!selectedGrade || !selectedGrade.submissionId) {
      message.error('Unable to retrieve submission information');
      return;
    }

    setLoadingContent(true);
    setPreviewModalVisible(true);
    
    try {
      const response = await axios.get(`/api/submissions/${selectedGrade.submissionId}/content`);
      setSubmissionContent(response.data);
    } catch (error) {
      console.error('Failed to load submission content:', error);
      message.error('Failed to load submission content');
      setPreviewModalVisible(false);
    } finally {
      setLoadingContent(false);
    }
  };

  const handleAppeal = async () => {
    if (!appealReason.trim() || !selectedGrade) {
      message.warning('Please enter appeal reason');
      return;
    }

    setSubmitting(true);
    try {
      const response = await axios.post('/api/appeals', {
        submissionId: selectedGrade.submissionId,
        studentId: user.id,
        reason: appealReason
      });

      if (response.data.success) {
        message.success('Appeal submitted successfully! Awaiting teacher review');
        setAppealModalVisible(false);
        setAppealReason('');
        fetchGrades(); // Refresh data
      } else {
        message.error('Appeal failed: ' + response.data.error);
      }
    } catch (error) {
      message.error('Appeal failed: ' + (error.response?.data?.error || error.message));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" tip="Loading grades..." />
      </div>
    );
  }

  if (!selectedGrade) {
    return (
      <div>
        <h2>📊 My Grades</h2>
        <Empty description="No published grades yet" />
      </div>
    );
  }

  // Parse AI feedback JSON
  let feedback = { strengths: [], weaknesses: [], suggestions: [] };
  try {
    if (selectedGrade.aiFeedback) {
      const parsed = JSON.parse(selectedGrade.aiFeedback);
      feedback = parsed.feedback || parsed;
    }
  } catch (e) {
    console.error('Failed to parse AI feedback:', e);
  }

  return (
    <div>
      <h2>📊 My Grades</h2>

      {/* Grade list selection */}
      {grades.length > 1 && (
        <Card title="📋 Assignment List" style={{ marginBottom: 16 }}>
          <List
            dataSource={grades}
            renderItem={(grade) => (
              <List.Item
                key={grade.id}
                onClick={() => {
                  setSelectedGrade(grade);
                  fetchAppeals(grade.submissionId);
                }}
                style={{
                  cursor: 'pointer',
                  background: selectedGrade?.id === grade.id ? '#e6f7ff' : 'white',
                  padding: '12px',
                  borderRadius: '4px',
                  border: selectedGrade?.id === grade.id ? '2px solid #1890ff' : '1px solid #f0f0f0',
                  marginBottom: '8px'
                }}
              >
                <div style={{ width: '100%' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <strong>{grade.assignmentTitle || 'Assignment'}</strong>
                    <Tag color="green">{grade.teacherScore || grade.aiScore || 0} pts</Tag>
                  </div>
                  <div style={{ fontSize: '12px', color: '#888', marginTop: '4px' }}>
                    Published: {grade.publishedAt ? new Date(grade.publishedAt).toLocaleString('en-US') : 'Unknown'}
                  </div>
                </div>
              </List.Item>
            )}
          />
        </Card>
      )}

      <Card 
        title={selectedGrade.assignmentTitle || 'Assignment Grade'}
        extra={<Tag color="green">Published</Tag>}
        style={{ marginBottom: 16 }}
      >
        <Descriptions bordered column={2}>
          <Descriptions.Item label="Final Grade">
            <span style={{ fontSize: '24px', fontWeight: 'bold', color: '#1890ff' }}>
              {selectedGrade.teacherScore || selectedGrade.aiScore || 0}
            </span> / 100
          </Descriptions.Item>
          <Descriptions.Item label="AI Score">
            {selectedGrade.aiScore || 0} 
            {selectedGrade.aiConfidence && ` (Confidence: ${(selectedGrade.aiConfidence * 100).toFixed(0)}%)`}
          </Descriptions.Item>
          <Descriptions.Item label="Published Date" span={2}>
            {selectedGrade.publishedAt ? new Date(selectedGrade.publishedAt).toLocaleString('en-US') : 'Not published'}
          </Descriptions.Item>
          <Descriptions.Item label="Teacher Comments" span={2}>
            {selectedGrade.teacherComments || 'No comments'}
          </Descriptions.Item>
        </Descriptions>

        <div style={{ marginTop: 24 }}>
          <Button type="primary" onClick={handleAcknowledge} style={{ marginRight: 8 }}>
            ✅ Acknowledge
          </Button>
          <Button onClick={handleViewSubmission} style={{ marginRight: 8 }}>
            📄 View Submission
          </Button>
          <Button onClick={() => setAppealModalVisible(true)} style={{ marginRight: 8 }}>
            📝 Appeal Grade
          </Button>
          <Button
            type="primary"
            onClick={() => {
              if (onNavigate && selectedGrade) {
                onNavigate('learning-plan', { gradeId: selectedGrade.id, assignmentTitle: selectedGrade.assignmentTitle });
              }
            }}
            disabled={!feedback.suggestions || feedback.suggestions.length === 0}
          >
            📚 Generate Learning Plan
          </Button>
        </div>
      </Card>

      {(feedback.strengths?.length > 0 || feedback.weaknesses?.length > 0 || feedback.suggestions?.length > 0) && (
        <Card title="💡 Detailed Feedback" style={{ marginBottom: 16 }}>
          {feedback.strengths?.length > 0 && (
            <div style={{ marginBottom: 16, padding: '12px', background: '#f6ffed', border: '1px solid #b7eb8f', borderRadius: '4px' }}>
              <h4 style={{ color: '#52c41a', marginBottom: '12px' }}>✅ Strengths:</h4>
              <ul style={{ marginBottom: 0, paddingLeft: '20px' }}>
                {feedback.strengths.map((s, i) => (
                  <li key={i} style={{ marginBottom: '8px', lineHeight: '1.6', color: '#333' }}>{s}</li>
                ))}
              </ul>
            </div>
          )}

          {feedback.weaknesses?.length > 0 && (
            <div style={{ marginBottom: 16, padding: '12px', background: '#fffbe6', border: '1px solid #ffe58f', borderRadius: '4px' }}>
              <h4 style={{ color: '#faad14', marginBottom: '12px' }}>⚠️ Areas for Improvement:</h4>
              <ul style={{ marginBottom: 0, paddingLeft: '20px' }}>
                {feedback.weaknesses.map((w, i) => (
                  <li key={i} style={{ marginBottom: '8px', lineHeight: '1.6', color: '#333' }}>{w}</li>
                ))}
              </ul>
            </div>
          )}

          {feedback.suggestions?.length > 0 && (
            <div style={{ padding: '12px', background: '#e6f7ff', border: '1px solid #91d5ff', borderRadius: '4px' }}>
              <h4 style={{ color: '#1890ff', marginBottom: '12px' }}>💡 Detailed Improvement Suggestions:</h4>
              {feedback.suggestions.map((suggestion, i) => (
                <div key={i} style={{
                  marginBottom: i < feedback.suggestions.length - 1 ? '16px' : 0,
                  padding: '12px',
                  background: 'white',
                  borderLeft: '3px solid #1890ff',
                  borderRadius: '4px'
                }}>
                  <div style={{ marginBottom: '8px' }}>
                    <strong style={{ color: '#d32f2f' }}>Issue:</strong>
                    <p style={{ margin: '4px 0 0 0', color: '#333', lineHeight: '1.6' }}>{suggestion.issue}</p>
                  </div>
                  <div style={{ marginBottom: '8px' }}>
                    <strong style={{ color: '#1976d2' }}>Suggestion:</strong>
                    <p style={{ margin: '4px 0 0 0', color: '#333', lineHeight: '1.6' }}>{suggestion.suggestion}</p>
                  </div>
                  {suggestion.why && (
                    <div style={{ marginBottom: '8px' }}>
                      <strong style={{ color: '#7b1fa2' }}>Why This Matters:</strong>
                      <p style={{ margin: '4px 0 0 0', color: '#555', lineHeight: '1.6', fontStyle: 'italic' }}>{suggestion.why}</p>
                    </div>
                  )}
                  {suggestion.howToImprove && (
                    <div>
                      <strong style={{ color: '#388e3c' }}>How to Improve:</strong>
                      <p style={{
                        margin: '4px 0 0 0',
                        color: '#333',
                        lineHeight: '1.6',
                        padding: '8px',
                        background: '#f5f5f5',
                        borderRadius: '4px',
                        fontFamily: 'monospace',
                        fontSize: '13px',
                        whiteSpace: 'pre-wrap'
                      }}>{suggestion.howToImprove}</p>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </Card>
      )}

      {/* Appeal history */}
      {appeals.length > 0 && (
        <Card title="📋 Appeal Records" style={{ marginBottom: 16 }}>
          {appeals.map((appeal, idx) => (
            <Card key={appeal.id} size="small" style={{ marginBottom: 8 }}>
              <Descriptions column={2} size="small">
                <Descriptions.Item label="Appeal Time" span={2}>
                  {appeal.createdAt ? new Date(appeal.createdAt).toLocaleString('en-US') : 'Unknown'}
                </Descriptions.Item>
                <Descriptions.Item label="Status">
                  <Tag color={
                    appeal.status === 'PENDING' ? 'orange' :
                    appeal.status === 'APPROVED' ? 'green' :
                    appeal.status === 'REJECTED' ? 'red' : 'default'
                  }>
                    {appeal.status === 'PENDING' ? 'Pending' :
                     appeal.status === 'APPROVED' ? 'Approved' :
                     appeal.status === 'REJECTED' ? 'Rejected' : appeal.status}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="Resolved Time">
                  {appeal.resolvedAt ? new Date(appeal.resolvedAt).toLocaleString('en-US') : 'Pending'}
                </Descriptions.Item>
                <Descriptions.Item label="Appeal Reason" span={2}>
                  <div style={{ padding: '8px', background: '#f5f5f5', borderRadius: '4px' }}>
                    {appeal.reason}
                  </div>
                </Descriptions.Item>
                {appeal.resolution && (
                  <Descriptions.Item label="Resolution" span={2}>
                    <div style={{ 
                      padding: '8px', 
                      background: appeal.status === 'APPROVED' ? '#f6ffed' : '#fff1f0', 
                      borderRadius: '4px',
                      border: `1px solid ${appeal.status === 'APPROVED' ? '#b7eb8f' : '#ffa39e'}`
                    }}>
                      {appeal.resolution}
                    </div>
                  </Descriptions.Item>
                )}
              </Descriptions>
            </Card>
          ))}
        </Card>
      )}

      <Modal
        title="Appeal Grade"
        open={appealModalVisible}
        onOk={handleAppeal}
        onCancel={() => setAppealModalVisible(false)}
        confirmLoading={submitting}
      >
        <p>Please state your appeal reason:</p>
        <TextArea
          rows={4}
          value={appealReason}
          onChange={(e) => setAppealReason(e.target.value)}
          placeholder="e.g., I believe my complexity analysis covered the key points and would like the teacher to review again..."
        />
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
        width={800}
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
            </Descriptions>
            
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
              fontSize: '13px'
            }}>
              {submissionContent.ocrText || submissionContent.originalText || '(No text content)'}
            </div>
            
            {submissionContent.originalFileUrl && (
              <div style={{ marginTop: 16 }}>
                <Button 
                  type="link" 
                  href={submissionContent.originalFileUrl} 
                  target="_blank"
                >
                  🔗 Download Original File
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

export default StudentResults;
