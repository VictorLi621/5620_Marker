import React, { useState, useEffect } from 'react';
import { Card, Select, Row, Col, Statistic, Table, message, Empty, Spin } from 'antd';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import axios from 'axios';

const Dashboard = ({ user }) => {
  const [courses, setCourses] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [selectedCourse, setSelectedCourse] = useState(null);
  const [selectedAssignment, setSelectedAssignment] = useState(null);
  const [distribution, setDistribution] = useState(null);
  const [heatmap, setHeatmap] = useState([]);
  const [loading, setLoading] = useState(false);

  // Load teacher's courses
  useEffect(() => {
    if (user && user.id && user.role === 'TEACHER') {
      fetchTeacherCourses();
    }
  }, [user]);

  // When a course is selected, load its assignments
  useEffect(() => {
    if (selectedCourse) {
      fetchAssignmentsByCourse();
    }
  }, [selectedCourse]);

  useEffect(() => {
    if (selectedAssignment) {
      fetchAnalytics();
    }
  }, [selectedAssignment]);

  const fetchTeacherCourses = async () => {
    try {
      setLoading(true);
      console.log('📚 Fetching courses for teacher:', user.id);
      const response = await axios.get(`/api/courses/teacher/${user.id}`);
      console.log('📚 Teacher courses:', response.data);
      setCourses(response.data || []);
      
      // Automatically select the first course
      if (response.data && response.data.length > 0) {
        setSelectedCourse(response.data[0].courseCode);
      }
    } catch (error) {
      console.error('Failed to fetch teacher courses:', error);
      message.error('Failed to load courses');
    } finally {
      setLoading(false);
    }
  };

  const fetchAssignmentsByCourse = async () => {
    try {
      setLoading(true);
      console.log('📝 Fetching assignments for course:', selectedCourse);
      const response = await axios.get(`/api/assignments?page=0&size=100`);
      const allAssignments = response.data.content || response.data || [];
      
      // Filter assignments for the current course
      const courseAssignments = allAssignments.filter(a => a.courseCode === selectedCourse);
      console.log('📝 Course assignments:', courseAssignments);
      setAssignments(courseAssignments);
      
      // Automatically select the first assignment
      if (courseAssignments.length > 0) {
        setSelectedAssignment(courseAssignments[0].id);
      } else {
        setSelectedAssignment(null);
      }
    } catch (error) {
      console.error('Failed to fetch assignments:', error);
      message.error('Failed to load assignments');
    } finally {
      setLoading(false);
    }
  };

  const fetchAnalytics = async () => {
    try {
      const distResponse = await axios.get(`/api/analytics/assignments/${selectedAssignment}/distribution`);
      setDistribution(distResponse.data);

      const heatmapResponse = await axios.get(`/api/analytics/assignments/${selectedAssignment}/heatmap`);
      setHeatmap(heatmapResponse.data);
    } catch (error) {
      console.error('Failed to fetch analytics', error);
    }
  };

  // Convert real score data to chart format
  const scoreChartData = React.useMemo(() => {
    if (!distribution || !distribution.scores || distribution.scores.length === 0) {
      // Return empty array when no data
      return [];
    }
    
    // Group scores into ranges
    const ranges = [
      { range: '0-20', min: 0, max: 20, count: 0 },
      { range: '21-40', min: 21, max: 40, count: 0 },
      { range: '41-60', min: 41, max: 60, count: 0 },
      { range: '61-80', min: 61, max: 80, count: 0 },
      { range: '81-100', min: 81, max: 100, count: 0 },
    ];
    
    distribution.scores.forEach(score => {
      for (const range of ranges) {
        if (score >= range.min && score <= range.max) {
          range.count++;
          break;
        }
      }
    });
    
    return ranges.map(({ range, count }) => ({ range, count }));
  }, [distribution]);

  const heatmapColumns = [
    {
      title: 'Common Issues',
      dataIndex: 'issue',
      key: 'issue',
    },
    {
      title: 'Occurrences',
      dataIndex: 'count',
      key: 'count',
      sorter: (a, b) => a.count - b.count,
    },
    {
      title: 'Percentage',
      dataIndex: 'percentage',
      key: 'percentage',
      render: (val) => `${val.toFixed(1)}%`,
    },
  ];

  if (user.role !== 'TEACHER') {
    return (
      <Card>
        <Empty description="Only teachers can view the analytics dashboard" />
      </Card>
    );
  }

  if (loading && courses.length === 0) {
    return (
      <Card>
        <Spin tip="Loading..." />
      </Card>
    );
  }

  if (courses.length === 0) {
    return (
      <Card>
        <Empty 
          description={
            <div>
              <p>You don't have any courses yet</p>
              <p style={{ fontSize: '12px', color: '#999' }}>
                Please contact the technical team to add you to a course, or be assigned as the main instructor when creating a course
              </p>
            </div>
          } 
        />
      </Card>
    );
  }

  return (
    <div>
      <h2>📊 Analytics Dashboard</h2>

      <Card style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', gap: '16px', alignItems: 'center', flexWrap: 'wrap' }}>
          <div>
            <label style={{ fontWeight: 'bold', marginRight: 8 }}>Select Course:</label>
            <Select
              style={{ width: 250 }}
              value={selectedCourse}
              onChange={setSelectedCourse}
              placeholder="Select a course"
            >
              {courses.map(course => (
                <Select.Option key={course.courseCode} value={course.courseCode}>
                  {course.courseCode} - {course.courseName}
                </Select.Option>
              ))}
            </Select>
          </div>
          
          <div>
            <label style={{ fontWeight: 'bold', marginRight: 8 }}>Select Assignment:</label>
            <Select
              style={{ width: 300 }}
              value={selectedAssignment}
              onChange={setSelectedAssignment}
              placeholder="Select an assignment"
              disabled={!selectedCourse || assignments.length === 0}
              notFoundContent={
                !selectedCourse ? 'Please select a course first' : 
                assignments.length === 0 ? 'No assignments for this course' : null
              }
            >
              {assignments.map(assignment => (
                <Select.Option key={assignment.id} value={assignment.id}>
                  {assignment.title}
                </Select.Option>
              ))}
            </Select>
          </div>
        </div>
      </Card>

      {!selectedAssignment && (
        <Card>
          <Empty description="Please select a course and assignment to view analytics" />
        </Card>
      )}

      {selectedAssignment && (
        <>
          <Row gutter={16} style={{ marginBottom: 16 }}>
            <Col span={6}>
              <Card>
                <Statistic
                  title="Sample Size"
                  value={distribution?.sampleSize || 45}
                  suffix="submissions"
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="Mean Score"
                  value={distribution?.mean || 72.3}
                  precision={1}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="Median"
                  value={distribution?.median || 75.0}
                  precision={1}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="Std. Deviation"
                  value={distribution?.stdDev || 12.5}
                  precision={1}
                />
              </Card>
            </Col>
          </Row>

          <Card title="📈 Score Distribution" style={{ marginBottom: 16 }}>
            {scoreChartData.length > 0 ? (
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={scoreChartData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="range" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="count" fill="#1890ff" name="Number of Students" />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <Empty 
                description="No submissions yet"
                style={{ padding: '40px 0' }}
              />
            )}
          </Card>

          <Card title="🔥 Error Heatmap (Common Issues)">
            <Table
              columns={heatmapColumns}
              dataSource={heatmap.length > 0 ? heatmap : [
                { key: 1, issue: 'Missing complexity analysis', count: 28, percentage: 62.2 },
                { key: 2, issue: 'Insufficient boundary condition handling', count: 23, percentage: 51.1 },
                { key: 3, issue: 'Missing error handling', count: 19, percentage: 42.2 },
                { key: 4, issue: 'Insufficient test coverage', count: 15, percentage: 33.3 },
                { key: 5, issue: 'Missing code comments', count: 12, percentage: 26.7 },
              ]}
              pagination={false}
            />
          </Card>
        </>
      )}
    </div>
  );
};

export default Dashboard;
