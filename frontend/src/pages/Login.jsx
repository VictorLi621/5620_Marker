import React, { useState } from 'react';
import { Form, Input, Button, Card, message, Space } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import axios from 'axios';

const Login = ({ onLogin, onShowRegister }) => {
  const [loading, setLoading] = useState(false);

  const onFinish = async (values) => {
    setLoading(true);
    try {
      const response = await axios.post('/api/auth/login', {
        username: values.username,
        password: values.password
      });
      
      if (response.data.success) {
        message.success('Login successful!');
        onLogin(response.data.user);
      } else {
        message.error('Login failed: ' + response.data.error);
      }
    } catch (error) {
      message.error('Login failed: ' + (error.response?.data?.error || error.message));
    } finally {
      setLoading(false);
    }
  };

  const quickLogin = (username) => {
    onFinish({ username, password: 'password' });
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
        title="Intelligent Grading System - Login" 
        style={{ width: 400, boxShadow: '0 4px 12px rgba(0,0,0,0.15)' }}
      >
        <Form
          name="login"
          initialValues={{ remember: true }}
          onFinish={onFinish}
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: 'Please enter your username!' }]}
          >
            <Input 
              prefix={<UserOutlined />} 
              placeholder="Username" 
              size="large"
            />
          </Form.Item>
          
          <Form.Item
            name="password"
            rules={[{ required: true, message: 'Please enter your password!' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="Password"
              size="large"
            />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block size="large">
              Login
            </Button>
          </Form.Item>
        </Form>

        <div style={{ marginTop: 16, textAlign: 'center' }}>
          <p style={{ color: '#888', fontSize: '12px', marginBottom: 8 }}>Quick Demo Login:</p>
          <Space>
            <Button size="small" onClick={() => quickLogin('student@uni.edu')}>
              Student
            </Button>
            <Button size="small" onClick={() => quickLogin('teacher@uni.edu')}>
              Teacher
            </Button>
            <Button size="small" onClick={() => quickLogin('admin@uni.edu')}>
              Admin
            </Button>
          </Space>
          <p style={{ color: '#aaa', fontSize: '11px', marginTop: 8 }}>Default password: password</p>
          
          <div style={{ marginTop: 16, borderTop: '1px solid #eee', paddingTop: 16 }}>
            <Button 
              type="link" 
              onClick={onShowRegister}
              block
            >
              Don't have an account? Register now
            </Button>
          </div>
        </div>
      </Card>
    </div>
  );
};

export default Login;

