import React, { useState, useEffect } from 'react';
import { Card, Form, Input, InputNumber, Button, message, DatePicker, Select } from 'antd';
import axios from 'axios';

const { TextArea } = Input;

const CreateAssignment = ({ user }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [courses, setCourses] = useState([]);
  const [loadingCourses, setLoadingCourses] = useState(false);

  // Load teacher's courses
  useEffect(() => {
    const fetchTeacherCourses = async () => {
      try {
        setLoadingCourses(true);
        console.log('🔍 Fetching courses for teacher:', user.id);
        const response = await axios.get(`/api/courses/teacher/${user.id}`);
        console.log('📚 Teacher courses response:', response.data);
        setCourses(response.data || []);
        
        if (!response.data || response.data.length === 0) {
          console.warn('⚠️ Teacher has no courses!');
          console.log('User info:', { id: user.id, role: user.role, username: user.username });
        }
      } catch (error) {
        console.error('❌ Failed to fetch teacher courses:', error);
        console.error('Error details:', error.response?.data);
        message.warning('Unable to load your course list. You can manually enter the course code');
      } finally {
        setLoadingCourses(false);
      }
    };

    if (user && user.id) {
      fetchTeacherCourses();
    }
  }, [user]);

  const handleSubmit = async (values) => {
    setLoading(true);
    try {
      const payload = {
        ...values,
        teacherId: user.id,
        dueDate: values.dueDate ? values.dueDate.format('YYYY-MM-DD HH:mm:ss') : null
      };

      const response = await axios.post('/api/assignments/create', payload);

      if (response.data.success) {
        message.success('✅ Assignment created successfully!');
        form.resetFields();
      } else {
        message.error('Creation failed: ' + response.data.error);
      }
    } catch (error) {
      console.error('Create assignment failed:', error);
      message.error('Creation failed: ' + (error.response?.data?.error || error.message));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h2>📝 Create Assignment</h2>
      
      <Card>
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{
            totalMarks: 100
          }}
        >
          <Form.Item
            label="Course Code"
            name="courseCode"
            rules={[{ required: true, message: 'Please select a course' }]}
            extra={courses.length === 0 ? "If you don't see any courses, please contact the technical team to assign you to courses" : `You have ${courses.length} course(s)`}
          >
            <Select
              showSearch
              placeholder={loadingCourses ? "Loading courses..." : "Select a course you teach"}
              loading={loadingCourses}
              disabled={loadingCourses}
              optionFilterProp="children"
              filterOption={(input, option) =>
                (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
              }
              options={courses.map(course => ({
                value: course.courseCode,
                label: `${course.courseCode} - ${course.courseName}`
              }))}
              notFoundContent={loadingCourses ? "Loading..." : "No courses found, please contact the technical team"}
            />
          </Form.Item>

          <Form.Item
            label="Assignment Title"
            name="title"
            rules={[{ required: true, message: 'Please enter assignment title' }]}
          >
            <Input placeholder="e.g., Algorithm Design - Assignment 1" />
          </Form.Item>

          <Form.Item
            label="Assignment Description"
            name="description"
            rules={[{ required: true, message: 'Please enter assignment description' }]}
          >
            <TextArea 
              rows={4} 
              placeholder="Describe the assignment requirements, grading criteria, etc..."
            />
          </Form.Item>

          <Form.Item
            label="Total Marks"
            name="totalMarks"
            rules={[{ required: true, message: 'Please enter total marks' }]}
          >
            <InputNumber 
              min={1} 
              max={1000} 
              style={{ width: '200px' }}
              placeholder="100"
            />
          </Form.Item>

          <Form.Item
            label="Due Date (Optional)"
            name="dueDate"
          >
            <DatePicker 
              showTime 
              format="YYYY-MM-DD HH:mm:ss"
              style={{ width: '100%' }}
              placeholder="Select due date"
            />
          </Form.Item>

          <Form.Item>
            <Button 
              type="primary" 
              htmlType="submit" 
              loading={loading}
              size="large"
            >
              Create Assignment
            </Button>
          </Form.Item>
        </Form>
      </Card>

      <Card title="💡 Tips" style={{ marginTop: 16 }}>
        <ul>
          <li>✅ You can only create assignments for courses you teach</li>
          <li>If you don't see any courses, please contact the technical team to assign you to courses in the "Course Table"</li>
          <li>After creating an assignment, students enrolled in that course can see and submit it on the "Submit Assignment" page</li>
          <li>You can view student submissions and grade them on the "Review Submissions" page</li>
          <li>Grading criteria can be detailed in the description, and AI will grade based on it</li>
        </ul>
      </Card>
    </div>
  );
};

export default CreateAssignment;
