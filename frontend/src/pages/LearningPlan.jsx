import React, { useState, useEffect } from 'react';
import { Card, Button, Timeline, Tag, Spin, Empty, message, List, Progress } from 'antd';
import { BookOutlined, ClockCircleOutlined, CheckCircleOutlined, TrophyOutlined } from '@ant-design/icons';
import axios from 'axios';

const LearningPlan = ({ user, gradeId, assignmentTitle, onNavigate }) => {
  const [loading, setLoading] = useState(true);
  const [learningPlan, setLearningPlan] = useState(null);
  const [allPlans, setAllPlans] = useState([]);
  const [selectedPlan, setSelectedPlan] = useState(null);

  useEffect(() => {
    if (gradeId) {
      // Generate new learning plan, then fetch all plans
      generateLearningPlan(gradeId);
    } else {
      // Load all existing learning plans
      fetchAllLearningPlans();
    }
  }, [gradeId, user.id]);

  const fetchAllLearningPlans = async () => {
    try {
      const response = await axios.get(`/api/learning-plans/student/${user.id}`);
      setAllPlans(response.data || []);

      if (response.data && response.data.length > 0) {
        setSelectedPlan(response.data[0]);
      }
    } catch (error) {
      console.error('Failed to fetch learning plans:', error);
      message.error('Failed to load learning plans: ' + (error.response?.data?.error || error.message));
    } finally {
      setLoading(false);
    }
  };

  const generateLearningPlan = async (gradeId) => {
    setLoading(true);
    try {
      const response = await axios.post('/api/learning-plans/generate', {
        gradeId: gradeId,
        studentId: user.id
      });

      if (response.data.success) {
        const newPlan = response.data.learningPlan;
        setLearningPlan(newPlan);
        setSelectedPlan(newPlan);
        message.success('✅ Learning plan generated successfully!');

        // Fetch all plans to update the list
        await fetchAllLearningPlans();
      } else {
        message.error('Failed to generate learning plan: ' + response.data.error);
        setLoading(false);
      }
    } catch (error) {
      console.error('Failed to generate learning plan:', error);
      message.error('Failed to generate learning plan: ' + (error.response?.data?.error || error.message));
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" tip="Generating personalized learning plan..." />
      </div>
    );
  }

  if (!selectedPlan && allPlans.length === 0) {
    return (
      <div>
        <h2>📚 My Learning Plans</h2>
        <Empty description="No learning plans yet. Complete assignments and view results to generate learning plans." />
        <div style={{ textAlign: 'center', marginTop: 16 }}>
          <Button type="primary" onClick={() => onNavigate && onNavigate('results')}>
            Go to View Results
          </Button>
        </div>
      </div>
    );
  }

  // Parse the learning plan content
  let planContent = selectedPlan;
  if (typeof selectedPlan.planContent === 'string') {
    try {
      planContent = { ...selectedPlan, ...JSON.parse(selectedPlan.planContent) };
    } catch (e) {
      console.error('Failed to parse plan content:', e);
    }
  }

  const phases = planContent.phases || [];
  const totalTasks = phases.reduce((sum, phase) => sum + (phase.tasks?.length || 0), 0);
  const completedTasks = 0; // TODO: Implement task completion tracking

  return (
    <div>
      <h2>📚 My Learning Plans</h2>

      {/* Plan list selection */}
      {allPlans.length > 1 && (
        <Card title="📋 Learning Plan History" style={{ marginBottom: 16 }}>
          <List
            dataSource={allPlans}
            renderItem={(plan) => (
              <List.Item
                key={plan.id}
                onClick={() => setSelectedPlan(plan)}
                style={{
                  cursor: 'pointer',
                  background: selectedPlan?.id === plan.id ? '#e6f7ff' : 'white',
                  padding: '12px',
                  borderRadius: '4px',
                  border: selectedPlan?.id === plan.id ? '2px solid #1890ff' : '1px solid #f0f0f0',
                  marginBottom: '8px'
                }}
              >
                <div style={{ width: '100%' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <strong>{plan.assignmentTitle || 'Learning Plan'}</strong>
                    <Tag color="blue">{plan.status || 'ACTIVE'}</Tag>
                  </div>
                  <div style={{ fontSize: '12px', color: '#888', marginTop: '4px' }}>
                    Created: {plan.createdAt ? new Date(plan.createdAt).toLocaleString('en-US') : 'Unknown'}
                  </div>
                </div>
              </List.Item>
            )}
          />
        </Card>
      )}

      {/* Plan Overview */}
      <Card
        title={
          <span>
            <TrophyOutlined style={{ marginRight: '8px' }} />
            Personalized Learning Plan - {planContent.assignmentTitle || assignmentTitle}
          </span>
        }
        extra={<Tag color="green">Generated on {planContent.createdAt ? new Date(planContent.createdAt).toLocaleDateString('en-US') : 'Today'}</Tag>}
        style={{ marginBottom: 16 }}
      >
        <div style={{ marginBottom: 16 }}>
          <h4>🎯 Learning Objectives</h4>
          <p style={{ color: '#666', lineHeight: '1.8' }}>
            {planContent.objective || 'Based on your assignment feedback, this personalized learning plan targets the key areas identified for improvement. Follow this structured approach to strengthen your understanding and skills.'}
          </p>
        </div>

        <div style={{ marginBottom: 16 }}>
          <h4>📊 Progress Overview</h4>
          <Progress
            percent={Math.round((completedTasks / totalTasks) * 100) || 0}
            status="active"
            format={(percent) => `${completedTasks}/${totalTasks} tasks completed`}
          />
        </div>

        <div>
          <h4>⏱️ Estimated Duration</h4>
          <p>
            <ClockCircleOutlined style={{ marginRight: '8px' }} />
            {planContent.estimatedDuration || '4-6 weeks'} (Flexible based on your pace)
          </p>
        </div>
      </Card>

      {/* Learning Plan Timeline */}
      <Card title="📅 Learning Roadmap" style={{ marginBottom: 16 }}>
        <Timeline mode="left">
          {phases.map((phase, phaseIdx) => (
            <Timeline.Item
              key={phaseIdx}
              color={phaseIdx === 0 ? 'green' : 'blue'}
              dot={phaseIdx === 0 ? <CheckCircleOutlined /> : <BookOutlined />}
            >
              <Card
                size="small"
                title={
                  <span>
                    <strong>{phase.phase}</strong>
                    <Tag color="blue" style={{ marginLeft: '8px' }}>{phase.duration}</Tag>
                  </span>
                }
                style={{ marginBottom: '16px' }}
              >
                <div style={{ marginBottom: '12px' }}>
                  <strong style={{ color: '#1890ff' }}>🎯 Focus Area:</strong>
                  <p style={{ margin: '4px 0 0 0', color: '#666' }}>{phase.focusArea}</p>
                </div>

                <div style={{ marginBottom: '12px' }}>
                  <strong style={{ color: '#52c41a' }}>✅ Tasks & Activities:</strong>
                  <ul style={{ marginTop: '8px', paddingLeft: '24px' }}>
                    {(phase.tasks || []).map((task, taskIdx) => (
                      <li key={taskIdx} style={{ marginBottom: '8px', lineHeight: '1.6', color: '#333' }}>
                        {task}
                      </li>
                    ))}
                  </ul>
                </div>

                {phase.resources && phase.resources.length > 0 && (
                  <div style={{ marginBottom: '12px' }}>
                    <strong style={{ color: '#7b1fa2' }}>📚 Recommended Resources:</strong>
                    <ul style={{ marginTop: '8px', paddingLeft: '24px' }}>
                      {phase.resources.map((resource, resIdx) => (
                        <li key={resIdx} style={{ marginBottom: '4px', color: '#666' }}>
                          {resource}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {phase.milestones && phase.milestones.length > 0 && (
                  <div>
                    <strong style={{ color: '#fa8c16' }}>🏆 Success Criteria:</strong>
                    <ul style={{ marginTop: '8px', paddingLeft: '24px' }}>
                      {phase.milestones.map((milestone, mileIdx) => (
                        <li key={mileIdx} style={{ marginBottom: '4px', color: '#666' }}>
                          {milestone}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </Card>
            </Timeline.Item>
          ))}
        </Timeline>
      </Card>

      {/* Tips & Motivation */}
      <Card
        title="💡 Tips for Success"
        style={{
          marginBottom: 16,
          background: 'linear-gradient(135deg, #fff9e6 0%, #ffe7ba 100%)',
          border: '2px solid #faad14'
        }}
      >
        <ul style={{ marginBottom: 0, paddingLeft: '24px', lineHeight: '2' }}>
          <li><strong>Pace yourself:</strong> This plan is flexible - adjust the timeline based on your schedule and learning speed.</li>
          <li><strong>Practice consistently:</strong> Regular practice (even 30 minutes daily) is more effective than occasional long sessions.</li>
          <li><strong>Track your progress:</strong> Keep notes on what you learn and challenges you overcome.</li>
          <li><strong>Seek help when needed:</strong> Don't hesitate to ask questions in office hours or discussion forums.</li>
          <li><strong>Apply your knowledge:</strong> Try to implement concepts in small projects or exercises as you learn.</li>
        </ul>
      </Card>

      {/* Action Buttons */}
      <Card>
        <div style={{ textAlign: 'center' }}>
          <Button
            type="primary"
            size="large"
            onClick={() => {
              if (onNavigate) {
                // Pass gradeId to navigate back to the specific assignment
                onNavigate('results', { selectedGradeId: selectedPlan?.gradeId });
              }
            }}
          >
            ← Back to Results
          </Button>
        </div>
      </Card>
    </div>
  );
};

export default LearningPlan;
