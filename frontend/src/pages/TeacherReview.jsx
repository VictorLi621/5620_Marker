import React, { useState, useEffect } from 'react';
import { Card, Button, InputNumber, Input, Modal, message, Descriptions, Tag, Space, List, Spin, Empty } from 'antd';
import axios from 'axios';

const { TextArea } = Input;

const TeacherReview = ({ user }) => {
  const [submissions, setSubmissions] = useState([]);
  const [selectedSubmission, setSelectedSubmission] = useState(null);
  const [selectedGrade, setSelectedGrade] = useState(null);
  const [teacherScore, setTeacherScore] = useState(null);
  const [comments, setComments] = useState('');
  const [loading, setLoading] = useState(true);
  const [reviewing, setReviewing] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [previewModalVisible, setPreviewModalVisible] = useState(false);
  const [submissionContent, setSubmissionContent] = useState(null);
  const [loadingContent, setLoadingContent] = useState(false);

  useEffect(() => {
    fetchSubmissions();
  }, []);

  const fetchSubmissions = async () => {
    try {
      // ✅ Use teacher-specific API to only get submissions for their own assignments
      const response = await axios.get(`/api/grades/teacher/${user.id}/pending?page=0&size=50`);
      
      // Adapt to new pagination format
      const pendingGrades = response.data.content || response.data || [];
      console.log('Teacher pending grades:', pendingGrades);
      
      // Transform to submission format for UI compatibility
      const transformedSubmissions = pendingGrades.map(grade => ({
        id: grade.submissionId,
        studentId: grade.studentId,
        studentName: grade.studentName,
        status: 'SCORED',
        createdAt: grade.submittedAt,
        assignmentTitle: grade.assignmentTitle,
        gradeId: grade.gradeId,
        aiScore: grade.aiScore,
        aiConfidence: grade.aiConfidence,
        teacherScore: grade.teacherScore,
        published: grade.published
      }));
      
      setSubmissions(transformedSubmissions);
      
      if (transformedSubmissions.length > 0) {
        selectSubmission(transformedSubmissions[0]);
      }
    } catch (error) {
      console.error('Failed to fetch submissions:', error);
      message.error('Failed to load submission list: ' + (error.response?.data?.error || error.message));
    } finally {
      setLoading(false);
    }
  };

  const selectSubmission = async (submission) => {
    setSelectedSubmission(submission);
    
    // Fetch grade information for this submission
    try {
      const gradeRes = await axios.get(`/api/grades/submission/${submission.id}`);
      setSelectedGrade(gradeRes.data);
      setTeacherScore(gradeRes.data.teacherScore || gradeRes.data.aiScore);
      setComments(gradeRes.data.teacherComments || '');
    } catch (error) {
      console.error('Failed to fetch grade:', error);
    }
  };

  const handleReview = async () => {
    if (!selectedGrade || teacherScore === null) {
      message.warning('Please enter teacher score');
      return;
    }

    setReviewing(true);
    try {
      const response = await axios.put(`/api/grades/${selectedGrade.id}/review`, {
        teacherId: user.id,
        teacherScore,
        comments
      });

      if (response.data.success) {
        message.success('Review saved successfully!');

        // Refresh the grade details to show updated scores
        const gradeRes = await axios.get(`/api/grades/submission/${selectedSubmission.id}`);
        setSelectedGrade(gradeRes.data);
        setTeacherScore(gradeRes.data.teacherScore || gradeRes.data.aiScore);
        setComments(gradeRes.data.teacherComments || '');

        fetchSubmissions(); // Refresh list
      } else {
        message.error('Review failed: ' + response.data.error);
      }
    } catch (error) {
      message.error('Review failed: ' + (error.response?.data?.error || error.message));
    } finally {
      setReviewing(false);
    }
  };

  const handleViewSubmission = async () => {
    if (!selectedSubmission) {
      message.error('Please select a submission');
      return;
    }

    setLoadingContent(true);
    setPreviewModalVisible(true);
    
    try {
      const response = await axios.get(`/api/submissions/${selectedSubmission.id}/content`);
      setSubmissionContent(response.data);
    } catch (error) {
      console.error('Failed to load submission content:', error);
      message.error('Failed to load submission content');
      setPreviewModalVisible(false);
    } finally {
      setLoadingContent(false);
    }
  };

  const handlePublish = async () => {
    if (!selectedSubmission) {
      message.warning('Please select a submission');
      return;
    }

    setPublishing(true);
    try {
      const response = await axios.post(`/api/grades/publish/${selectedSubmission.id}`, {
        teacherId: user.id,
        notes: 'Grade published'
      });

      if (response.data.success) {
        message.success('✅ Grade published! Student will be notified');
        fetchSubmissions(); // Refresh list
      } else {
        message.error('Publish failed: ' + response.data.error);
      }
    } catch (error) {
      message.error('Publish failed: ' + (error.response?.data?.error || error.message));
    } finally {
      setPublishing(false);
    }
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" tip="Loading submission list..." />
      </div>
    );
  }

  if (submissions.length === 0) {
    return (
      <div>
        <h2>👨‍🏫 Review Submissions</h2>
        <Empty description="No pending submissions to review" />
      </div>
    );
  }

  // Parse AI feedback
  let aiFeedback = { strengths: [], weaknesses: [], breakdown: [] };
  try {
    if (selectedGrade && selectedGrade.aiFeedback) {
      const parsed = JSON.parse(selectedGrade.aiFeedback);
      aiFeedback = parsed.feedback || parsed;
    }
  } catch (e) {
    console.error('Failed to parse AI feedback:', e);
  }

  return (
    <div>
      <h2>👨‍🏫 Review Submissions</h2>

      <div style={{ display: 'grid', gridTemplateColumns: '300px 1fr', gap: '16px' }}>
        {/* Left: Submission list */}
        <Card title="Submission List" style={{ height: 'fit-content' }}>
          <List
            dataSource={submissions}
            renderItem={(sub) => (
              <List.Item
                key={sub.id}
                onClick={() => selectSubmission(sub)}
                style={{
                  cursor: 'pointer',
                  background: selectedSubmission?.id === sub.id ? '#e6f7ff' : 'white',
                  padding: '12px',
                  marginBottom: '8px',
                  borderRadius: '4px',
                  border: selectedSubmission?.id === sub.id ? '2px solid #1890ff' : '1px solid #d9d9d9'
                }}
              >
                <div style={{ width: '100%' }}>
                  <div><strong>{sub.studentName || `Student ${sub.studentId}`}</strong></div>
                  <div style={{ fontSize: '12px', color: '#888' }}>
                    {sub.originalFileName}
                  </div>
                  <Tag color={sub.status === 'SCORED' ? 'green' : 'orange'} style={{ marginTop: '4px' }}>
                    {sub.status}
                  </Tag>
                </div>
              </List.Item>
            )}
          />
        </Card>

        {/* Right: Details */}
        <div>
          {selectedSubmission && selectedGrade ? (
            <>
              <Card 
                title={`Submission Details - ${selectedSubmission.originalFileName}`}
                extra={
                  <Space>
                    <Tag color="blue">ID: {selectedSubmission.id}</Tag>
                    <Button size="small" onClick={handleViewSubmission}>
                      📄 View Submission Content
                    </Button>
                  </Space>
                }
                style={{ marginBottom: 16 }}
              >
                <Descriptions bordered column={2}>
                  <Descriptions.Item label="AI Score">
                    <span style={{ fontSize: '20px', fontWeight: 'bold' }}>
                      {selectedGrade.aiScore || 0}
                    </span> / 100
                  </Descriptions.Item>
                  <Descriptions.Item label="AI Confidence">
                    <Tag color={(selectedGrade.aiConfidence || 0) >= 0.85 ? 'green' : 'orange'}>
                      {((selectedGrade.aiConfidence || 0) * 100).toFixed(0)}%
                    </Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="Submission Time" span={2}>
                    {selectedSubmission.createdAt ? new Date(selectedSubmission.createdAt).toLocaleString('en-US') : 'Unknown'}
                  </Descriptions.Item>
                  <Descriptions.Item label="Status" span={2}>
                    <Tag>{selectedGrade.status}</Tag>
                  </Descriptions.Item>
                </Descriptions>

                {aiFeedback.strengths?.length > 0 && (
                  <div style={{ marginTop: 16, padding: '12px', background: '#f6ffed', border: '1px solid #b7eb8f', borderRadius: '4px' }}>
                    <h4 style={{ color: '#52c41a', marginBottom: '12px' }}>✅ Strengths:</h4>
                    <ul style={{ marginBottom: 0, paddingLeft: '20px' }}>
                      {aiFeedback.strengths.map((s, i) => (
                        <li key={i} style={{ marginBottom: '8px', lineHeight: '1.6', color: '#333' }}>{s}</li>
                      ))}
                    </ul>
                  </div>
                )}

                {aiFeedback.weaknesses?.length > 0 && (
                  <div style={{ marginTop: 16, padding: '12px', background: '#fffbe6', border: '1px solid #ffe58f', borderRadius: '4px' }}>
                    <h4 style={{ color: '#faad14', marginBottom: '12px' }}>⚠️ Areas for Improvement:</h4>
                    <ul style={{ marginBottom: 0, paddingLeft: '20px' }}>
                      {aiFeedback.weaknesses.map((w, i) => (
                        <li key={i} style={{ marginBottom: '8px', lineHeight: '1.6', color: '#333' }}>{w}</li>
                      ))}
                    </ul>
                  </div>
                )}

                {aiFeedback.suggestions?.length > 0 && (
                  <div style={{ marginTop: 16, padding: '12px', background: '#e6f7ff', border: '1px solid #91d5ff', borderRadius: '4px' }}>
                    <h4 style={{ color: '#1890ff', marginBottom: '12px' }}>💡 Detailed Improvement Suggestions:</h4>
                    {aiFeedback.suggestions.map((suggestion, i) => (
                      <div key={i} style={{
                        marginBottom: '16px',
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
                        <div style={{ marginBottom: '8px' }}>
                          <strong style={{ color: '#7b1fa2' }}>Why This Matters:</strong>
                          <p style={{ margin: '4px 0 0 0', color: '#555', lineHeight: '1.6', fontStyle: 'italic' }}>{suggestion.why}</p>
                        </div>
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
                      </div>
                    ))}
                  </div>
                )}
              </Card>

              <Card title="Teacher Review">
                <div style={{ marginBottom: 16 }}>
                  <label style={{ fontWeight: 'bold', marginRight: 8 }}>Teacher Score:</label>
                  <InputNumber
                    min={0}
                    max={100}
                    value={teacherScore}
                    onChange={setTeacherScore}
                    placeholder="Enter final score"
                    size="large"
                    style={{ width: 200 }}
                  />
                  <span style={{ marginLeft: 8, color: '#888' }}>
                    (AI suggested: {selectedGrade.aiScore || 0})
                  </span>
                </div>

                <div style={{ marginBottom: 16 }}>
                  <label style={{ fontWeight: 'bold', marginRight: 8 }}>Teacher Comments:</label>
                  <TextArea
                    rows={4}
                    value={comments}
                    onChange={(e) => setComments(e.target.value)}
                    placeholder="Add your comments and suggestions..."
                  />
                </div>

                <Space>
                  <Button 
                    type="default" 
                    onClick={handleReview}
                    loading={reviewing}
                    disabled={teacherScore === null}
                  >
                    💾 Save Review (save only, not publish)
                  </Button>
                  <Button 
                    type="primary"
                    onClick={handlePublish}
                    loading={publishing}
                  >
                    📢 Publish Grade (notify student)
                  </Button>
                </Space>
                <p style={{ marginTop: 8, color: '#888', fontSize: '12px' }}>
                  Tip: First "Save Review" to update your score and comments, then "Publish Grade" to notify the student. Publishing creates a grade snapshot.
                </p>
              </Card>
            </>
          ) : (
            <Card>
              <Empty description="Please select a submission from the left" />
            </Card>
          )}
        </div>
      </div>

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
            <Descriptions bordered size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="File Name" span={3}>
                {submissionContent.originalFileName}
              </Descriptions.Item>
            </Descriptions>
            
            <h4>📝 Original Submission Content (OCR Extracted):</h4>
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
            
            <h4 style={{ color: '#1890ff' }}>🔒 Anonymized Content (used for AI grading):</h4>
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
            
            <p style={{ marginTop: 8, color: '#888', fontSize: '12px' }}>
              💡 Tip: AI grading uses anonymized content to avoid bias from names, student IDs, etc.
            </p>
            
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

export default TeacherReview;
