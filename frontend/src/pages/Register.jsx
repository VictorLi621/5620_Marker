import React, { useState } from 'react';
import { Form, Input, Button, Card, Radio, message } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, IdcardOutlined } from '@ant-design/icons';
import axios from 'axios';

const Register = ({ onRegisterSuccess, onBackToLogin }) => {
  const [loading, setLoading] = useState(false);
  const [role, setRole] = useState('STUDENT');

  const onFinish = async (values) => {
    setLoading(true);
    try {
      const response = await axios.post('/api/auth/register', {
        username: values.username,
        email: values.email,
        password: values.password,
        fullName: values.fullName,
        role: role,
        studentId: role === 'STUDENT' ? values.studentId : null
      });
      
      if (response.data.success) {
        message.success('Registration successful! Please log in');
        onBackToLogin();
      } else {
        message.error('Registration failed: ' + response.data.error);
      }
    } catch (error) {
      message.error('Registration failed: ' + (error.response?.data?.error || error.message));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ 
      height: '100vh', 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
    }}>
      <Card 
        title="Intelligent Grading System - Register" 
        style={{ width: 450, boxShadow: '0 4px 12px rgba(0,0,0,0.15)' }}
      >
        <Form
          name="register"
          onFinish={onFinish}
          initialValues={{ role: 'STUDENT' }}
        >
          <Form.Item>
            <Radio.Group 
              value={role} 
              onChange={(e) => setRole(e.target.value)}
              buttonStyle="solid"
              style={{ width: '100%' }}
            >
              <Radio.Button value="STUDENT" style={{ width: '50%', textAlign: 'center' }}>
                Student Registration
              </Radio.Button>
              <Radio.Button value="TEACHER" style={{ width: '50%', textAlign: 'center' }}>
                Teacher Registration
              </Radio.Button>
            </Radio.Group>
          </Form.Item>

          <Form.Item
            name="fullName"
            rules={[{ required: true, message: 'Please enter your full name' }]}
          >
            <Input 
              prefix={<UserOutlined />} 
              placeholder="Full Name" 
              size="large"
            />
          </Form.Item>

          <Form.Item
            name="username"
            rules={[{ required: true, message: 'Please enter your username' }]}
          >
            <Input 
              prefix={<UserOutlined />} 
              placeholder="Username" 
              size="large"
            />
          </Form.Item>

          <Form.Item
            name="email"
            rules={[
              { required: true, message: 'Please enter your email' },
              { type: 'email', message: 'Please enter a valid email' }
            ]}
          >
            <Input 
              prefix={<MailOutlined />} 
              placeholder="Email" 
              size="large"
            />
          </Form.Item>

          {role === 'STUDENT' && (
            <Form.Item
              name="studentId"
              rules={[{ required: true, message: 'Please enter your Student ID' }]}
            >
              <Input 
                prefix={<IdcardOutlined />} 
                placeholder="Student ID" 
                size="large"
              />
            </Form.Item>
          )}

          <Form.Item
            name="password"
            rules={[{ required: true, message: 'Please enter your password' }, { min: 6, message: 'Password must be at least 6 characters' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="Password (at least 6 characters)"
              size="large"
            />
          </Form.Item>

          <Form.Item
            name="confirmPassword"
            dependencies={['password']}
            rules={[
              { required: true, message: 'Please confirm your password' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('Passwords do not match'));
                },
              }),
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="Confirm Password"
              size="large"
            />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block size="large">
              Register
            </Button>
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'center' }}>
            <Button type="link" onClick={onBackToLogin} block>
              Already have an account? Log in
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default Register;
