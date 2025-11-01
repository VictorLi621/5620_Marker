import React, { useState, useEffect } from 'react';
import { Card, Form, Input, Button, Select, message, Table, Space, Popconfirm, Tabs, Tag, Modal, Typography, Tooltip } from 'antd';
import { CopyOutlined, SearchOutlined } from '@ant-design/icons';
import axios from 'axios';

const { TabPane } = Tabs;
const { Text } = Typography;

const TechManagement = ({ user }) => {
  const [form] = Form.useForm();
  const [students, setStudents] = useState([]);
  const [enrollments, setEnrollments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedCourse, setSelectedCourse] = useState('');
  const [allUsers, setAllUsers] = useState([]);
  const [filteredUsers, setFilteredUsers] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [loadingUsers, setLoadingUsers] = useState(false);
  const [loadingLogs, setLoadingLogs] = useState(false);
  const [searchText, setSearchText] = useState('');
  const [selectedRole, setSelectedRole] = useState(null);
  const [courses, setCourses] = useState([]);
  const [loadingCourses, setLoadingCourses] = useState(false);
  const [courseModalVisible, setCourseModalVisible] = useState(false);
  const [courseForm] = Form.useForm();
  const [selectedEnrollmentRole, setSelectedEnrollmentRole] = useState(null);

  useEffect(() => {
    fetchAllUsers();
    fetchAuditLogs(); // Auto-load logs
    fetchAllCourses(); // Auto-load course list
  }, []);

  const fetchAllUsers = async () => {
    try {
      setLoadingUsers(true);
      const response = await axios.get('/api/admin/users');
      const users = response.data || [];
      setAllUsers(users);
      setFilteredUsers(users); // Initialize filtered user list
    } catch (error) {
      console.error('Failed to fetch users:', error);
      message.error('Failed to load user list');
    } finally {
      setLoadingUsers(false);
    }
  };

  // Filter and search users
  const filterUsers = (role, search) => {
    let filtered = [...allUsers];
    
    // Filter by role
    if (role) {
      filtered = filtered.filter(u => u.role === role);
    }
    
    // Filter by search text
    if (search) {
      const searchLower = search.toLowerCase();
      filtered = filtered.filter(u => 
        u.username.toLowerCase().includes(searchLower) ||
        u.fullName.toLowerCase().includes(searchLower) ||
        u.email.toLowerCase().includes(searchLower) ||
        u.id.toString().includes(searchLower)
      );
    }
    
    setFilteredUsers(filtered);
  };

  // Role filter
  const handleRoleFilter = (value) => {
    setSelectedRole(value);
    filterUsers(value, searchText);
  };

  // Search
  const handleSearch = (value) => {
    setSearchText(value);
    filterUsers(selectedRole, value);
  };

  // Copy to clipboard
  const copyToClipboard = (text, label) => {
    navigator.clipboard.writeText(text).then(() => {
      message.success(`${label} copied: ${text}`);
    }).catch(() => {
      message.error('Copy failed');
    });
  };

  // Fetch all courses
  const fetchAllCourses = async () => {
    try {
      setLoadingCourses(true);
      const response = await axios.get('/api/courses');
      setCourses(response.data || []);
    } catch (error) {
      console.error('Failed to fetch courses:', error);
      message.error('Failed to load course list');
    } finally {
      setLoadingCourses(false);
    }
  };

  // Create course
  const handleCreateCourse = async (values) => {
    try {
      setLoading(true);
      const response = await axios.post('/api/courses/create', values);
      
      if (response.data.success) {
        message.success('✅ Course created successfully');
        courseForm.resetFields();
        setCourseModalVisible(false);
        fetchAllCourses();
      } else {
        message.error(response.data.error || 'Creation failed');
      }
    } catch (error) {
      console.error('Create course failed:', error);
      message.error('Creation failed: ' + (error.response?.data?.error || error.message));
    } finally {
      setLoading(false);
    }
  };

  // Delete course
  const handleDeleteCourse = async (courseId) => {
    try {
      const response = await axios.delete(`/api/courses/${courseId}`);
      if (response.data.success) {
        message.success('✅ Course deleted');
        fetchAllCourses();
      } else {
        message.error('Deletion failed');
      }
    } catch (error) {
      console.error('Delete course failed:', error);
      message.error('Deletion failed: ' + (error.response?.data?.error || error.message));
    }
  };

  const fetchAuditLogs = async () => {
    try {
      setLoadingLogs(true);
      const response = await axios.get('/api/admin/audit-logs?page=0&size=100');
      const logsData = response.data.content || response.data || [];
      setAuditLogs(logsData);
    } catch (error) {
      console.error('Failed to fetch audit logs:', error);
      message.error('Failed to load audit logs');
    } finally {
      setLoadingLogs(false);
    }
  };

  const handleDeleteUser = async (userId) => {
    try {
      const response = await axios.delete(`/api/admin/users/${userId}`);
      if (response.data.success) {
        message.success('✅ User deleted');
        fetchAllUsers();
      } else {
        message.error('Deletion failed');
      }
    } catch (error) {
      console.error('Delete user failed:', error);
      message.error('Deletion failed: ' + (error.response?.data?.error || error.message));
    }
  };

  useEffect(() => {
    if (selectedCourse) {
      fetchCourseEnrollments();
    }
  }, [selectedCourse]);

  const fetchUsers = async () => {
    try {
      // Assuming there is an API to get user list (needs to be created in reality)
      // const response = await axios.get('/api/users?role=STUDENT');
      // setStudents(response.data);
      
      // Temporary: Show message
      console.log('Tech management loaded');
    } catch (error) {
      console.error('Failed to fetch users:', error);
    }
  };

  const fetchCourseEnrollments = async () => {
    try {
      const response = await axios.get(`/api/enrollments/course/${selectedCourse}`);
      setEnrollments(response.data || []);
    } catch (error) {
      console.error('Failed to fetch enrollments:', error);
      message.error('Failed to load enrollment list');
    }
  };

  const handleEnrollStudent = async (values) => {
    setLoading(true);
    try {
      const response = await axios.post('/api/enrollments/enroll', {
        studentId: parseInt(values.studentId),
        courseCode: values.courseCode,
        teacherId: values.teacherId ? parseInt(values.teacherId) : null
      });

      if (response.data.success) {
        message.success('✅ User added to course!');
        form.resetFields();
        if (selectedCourse === values.courseCode) {
          fetchCourseEnrollments();
        }
      } else {
        message.error('Addition failed: ' + response.data.error);
      }
    } catch (error) {
      console.error('Enroll failed:', error);
      message.error('Addition failed: ' + (error.response?.data?.error || error.message));
    } finally {
      setLoading(false);
    }
  };

  const handleDropCourse = async (enrollmentId) => {
    try {
      const response = await axios.delete(`/api/enrollments/${enrollmentId}`);
      
      if (response.data.success) {
        message.success('✅ Course dropped');
        fetchCourseEnrollments();
      } else {
        message.error('Drop failed');
      }
    } catch (error) {
      console.error('Drop course failed:', error);
      message.error('Drop failed: ' + (error.response?.data?.error || error.message));
    }
  };

  return (
    <div>
      <h2>🔧 Technical Team Management</h2>
      
      <Tabs defaultActiveKey="1" style={{ marginTop: 16 }}>
        <TabPane tab="📚 Course Table" key="0">
          <Card 
            title="Course List" 
            extra={
              <Button 
                type="primary" 
                onClick={() => setCourseModalVisible(true)}
              >
                ➕ Create Course
              </Button>
            }
          >
            <Table
              dataSource={courses}
              rowKey="id"
              loading={loadingCourses}
              pagination={{ pageSize: 10 }}
              columns={[
                {
                  title: 'Course Code',
                  dataIndex: 'courseCode',
                  key: 'courseCode',
                  width: 120,
                  render: (code) => (
                    <Tag color="blue" style={{ fontSize: '13px' }}>{code}</Tag>
                  ),
                },
                {
                  title: 'Course Name',
                  dataIndex: 'courseName',
                  key: 'courseName',
                },
                {
                  title: 'Instructor',
                  key: 'teacher',
                  render: (_, record) => record.teacherName || '-',
                },
                {
                  title: 'Semester',
                  dataIndex: 'semester',
                  key: 'semester',
                  width: 100,
                },
                {
                  title: 'Capacity/Enrolled',
                  key: 'capacity',
                  width: 100,
                  render: (_, record) => (
                    <Text>
                      {record.enrolled || 0} / {record.capacity || '-'}
                    </Text>
                  ),
                },
                {
                  title: 'Status',
                  dataIndex: 'status',
                  key: 'status',
                  width: 100,
                  render: (status) => {
                    const colorMap = {
                      'ACTIVE': 'green',
                      'COMPLETED': 'orange',
                      'ARCHIVED': 'gray'
                    };
                    const textMap = {
                      'ACTIVE': 'Active',
                      'COMPLETED': 'Completed',
                      'ARCHIVED': 'Archived'
                    };
                    return <Tag color={colorMap[status]}>{textMap[status] || status}</Tag>;
                  }
                },
                {
                  title: 'Created At',
                  dataIndex: 'createdAt',
                  key: 'createdAt',
                  width: 180,
                  render: (date) => date ? new Date(date).toLocaleString('en-US') : '-',
                },
                {
                  title: 'Actions',
                  key: 'action',
                  width: 80,
                  render: (_, record) => (
                    <Popconfirm
                      title="Are you sure to delete this course?"
                      onConfirm={() => handleDeleteCourse(record.id)}
                      okText="Yes"
                      cancelText="No"
                    >
                      <Button type="link" danger size="small">Delete</Button>
                    </Popconfirm>
                  ),
                },
              ]}
            />
          </Card>

          {/* Create course modal */}
          <Modal
            title="Create New Course"
            open={courseModalVisible}
            onCancel={() => {
              setCourseModalVisible(false);
              courseForm.resetFields();
            }}
            footer={null}
            width={600}
          >
            <Form
              form={courseForm}
              layout="vertical"
              onFinish={handleCreateCourse}
            >
              <Form.Item
                label={<Text strong>Course Code</Text>}
                name="courseCode"
                rules={[{ required: true, message: 'Please enter course code' }]}
                extra={<Text type="secondary">Unique identifier, e.g., ELEC5620</Text>}
              >
                <Input placeholder="e.g., ELEC5620" />
              </Form.Item>

              <Form.Item
                label={<Text strong>Course Name</Text>}
                name="courseName"
                rules={[{ required: true, message: 'Please enter course name' }]}
              >
                <Input placeholder="e.g., Recursive Usability Engineering" />
              </Form.Item>

              <Form.Item
                label="Course Description"
                name="description"
              >
                <Input.TextArea 
                  rows={3} 
                  placeholder="Course introduction, learning objectives, etc."
                />
              </Form.Item>

              <Form.Item
                label={<Text strong>Semester</Text>}
                name="semester"
                rules={[{ required: true, message: 'Please enter semester' }]}
              >
                <Input placeholder="e.g., 2024-S1" />
              </Form.Item>

              <Form.Item
                label={<Text strong>Instructor (Main Teacher)</Text>}
                name="teacherId"
                extra={<Text type="secondary">Select one teacher as the main instructor for this course</Text>}
              >
                <Select
                  placeholder="Select instructor"
                  showSearch
                  allowClear
                  optionFilterProp="children"
                  filterOption={(input, option) =>
                    (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
                  }
                  options={
                    allUsers
                      .filter(u => u.role === 'TEACHER')
                      .map(u => ({
                        value: u.id,
                        label: `${u.fullName} (${u.username}) - ID: ${u.id}`
                      }))
                  }
                  notFoundContent={
                    <div style={{ padding: '12px', textAlign: 'center' }}>
                      <Text type="secondary">⚠️ No teacher users found</Text>
                      <br />
                      <Text type="secondary" style={{ fontSize: '12px' }}>
                        Please create teacher accounts in User Management first
                      </Text>
                    </div>
                  }
                />
              </Form.Item>

              <Form.Item
                label="Course Capacity"
                name="capacity"
                initialValue={50}
              >
                <Input placeholder="e.g., 50" type="number" />
              </Form.Item>

              <Form.Item>
                <Space>
                  <Button type="primary" htmlType="submit" loading={loading}>
                    Create Course
                  </Button>
                  <Button onClick={() => {
                    setCourseModalVisible(false);
                    courseForm.resetFields();
                  }}>
                    Cancel
                  </Button>
                </Space>
              </Form.Item>
            </Form>
          </Modal>
        </TabPane>

        <TabPane tab="👥 User Management" key="1">
          <Card title="All Users" extra={
            <Space>
              <Input
                placeholder="Search by username/name/email/ID"
                prefix={<SearchOutlined />}
                style={{ width: 250 }}
                allowClear
                value={searchText}
                onChange={(e) => handleSearch(e.target.value)}
              />
              <Select
                placeholder="Filter by role"
                style={{ width: 150 }}
                allowClear
                value={selectedRole}
                onChange={handleRoleFilter}
              >
                <Select.Option value="STUDENT">Student</Select.Option>
                <Select.Option value="TEACHER">Teacher</Select.Option>
                <Select.Option value="TECHNICAL_TEAM">Technical Team</Select.Option>
              </Select>
              <Button onClick={fetchAllUsers}>Refresh</Button>
            </Space>
          }>
            <Table
              dataSource={filteredUsers}
              rowKey="id"
              loading={loadingUsers}
              pagination={{ pageSize: 10 }}
              columns={[
                {
                  title: 'User ID',
                  dataIndex: 'id',
                  key: 'id',
                  width: 100,
                  render: (id) => (
                    <Space>
                      <Text strong style={{ color: '#1890ff', fontSize: '14px' }}>{id}</Text>
                      <Tooltip title="Click to copy ID">
                        <Button 
                          type="link" 
                          size="small" 
                          icon={<CopyOutlined />}
                          onClick={() => copyToClipboard(id, 'ID')}
                        />
                      </Tooltip>
                    </Space>
                  ),
                },
                {
                  title: 'Username',
                  dataIndex: 'username',
                  key: 'username',
                },
                {
                  title: 'Full Name',
                  dataIndex: 'fullName',
                  key: 'fullName',
                },
                {
                  title: 'Email',
                  dataIndex: 'email',
                  key: 'email',
                },
                {
                  title: 'Role',
                  dataIndex: 'role',
                  key: 'role',
                  render: (role) => {
                    const colorMap = {
                      'STUDENT': 'blue',
                      'TEACHER': 'green',
                      'TECHNICAL_TEAM': 'orange',
                      'ADMIN': 'red'
                    };
                    const labelMap = {
                      'STUDENT': 'Student',
                      'TEACHER': 'Teacher',
                      'TECHNICAL_TEAM': 'Technical Team',
                      'ADMIN': 'Administrator'
                    };
                    return <Tag color={colorMap[role]}>{labelMap[role] || role}</Tag>;
                  }
                },
                {
                  title: 'Registration Date',
                  dataIndex: 'createdAt',
                  key: 'createdAt',
                  render: (date) => date ? new Date(date).toLocaleString('en-US') : '-',
                },
                {
                  title: 'Actions',
                  key: 'action',
                  render: (_, record) => (
                    <Popconfirm
                      title="Are you sure to delete this user?"
                      onConfirm={() => handleDeleteUser(record.id)}
                      okText="Yes"
                      cancelText="No"
                    >
                      <Button type="link" danger size="small">Delete</Button>
                    </Popconfirm>
                  ),
                },
              ]}
            />
          </Card>
        </TabPane>

        <TabPane tab="📚 Course Management" key="2">
          <div>
      
      <Card 
        title="➕ Add User to Course" 
        style={{ marginBottom: 16 }}
      >
        <div style={{ 
          background: '#e6f7ff', 
          padding: '12px', 
          borderRadius: '4px', 
          marginBottom: '16px',
          border: '1px solid #91d5ff'
        }}>
          <Space direction="vertical">
            <Text strong>💡 How to get user ID:</Text>
            <Text>1. Switch to "User Management" tab</Text>
            <Text>2. Look at the "User ID" column (blue numbers)</Text>
            <Text>3. Click the copy icon 📋 next to the ID</Text>
            <Text>4. Return to this page and paste into the input field below</Text>
            <Text strong style={{ color: '#ff4d4f', marginTop: '8px' }}>⚠️ Only students or teachers can be added to courses, not technical team members</Text>
          </Space>
        </div>
        
        <Form
          form={form}
          layout="vertical"
          onFinish={handleEnrollStudent}
        >
          <Form.Item
            label={<Text strong>User ID (Student or Teacher)</Text>}
            name="studentId"
            rules={[{ required: true, message: 'Please enter user ID' }]}
            extra={<Text type="secondary">Copy student or teacher ID from "User Management" tab</Text>}
          >
            <Input 
              placeholder="Paste user ID (e.g., 1)" 
              type="number"
              style={{ fontSize: '14px' }}
            />
          </Form.Item>

          <Form.Item
            label={<Text strong>Course Code (Class)</Text>}
            name="courseCode"
            rules={[{ required: true, message: 'Please select or enter course code' }]}
            extra={<Text type="secondary">Select from dropdown or enter course code directly</Text>}
          >
            <Select
              showSearch
              placeholder="Select course or enter course code"
              style={{ fontSize: '14px' }}
              optionFilterProp="children"
              filterOption={(input, option) =>
                (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
              }
              options={courses.map(course => ({
                value: course.courseCode,
                label: `${course.courseCode} - ${course.courseName}`
              }))}
              dropdownRender={(menu) => (
                <>
                  {menu}
                  <div style={{ padding: '8px', borderTop: '1px solid #f0f0f0' }}>
                    <Text type="secondary" style={{ fontSize: '12px' }}>
                      💡 If the course is not in the list, please create it first in the "Course Table" tab
                    </Text>
                  </div>
                </>
              )}
            />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} size="large">
              Add User to Course
            </Button>
          </Form.Item>
          
          <div style={{ 
            background: '#e6f7ff', 
            padding: '12px', 
            borderRadius: '4px', 
            marginTop: '16px',
            border: '1px solid #91d5ff'
          }}>
            <Space direction="vertical" size="small">
              <Text strong style={{ color: '#1890ff' }}>📚 Two Types of Teachers:</Text>
              <Text type="secondary" style={{ fontSize: '12px' }}>
                <span style={{ fontWeight: 'bold' }}>Main Instructor</span>: Specified when creating course in "Course Table", responsible for course management and assignment creation
              </Text>
              <Text type="secondary" style={{ fontSize: '12px' }}>
                <span style={{ fontWeight: 'bold' }}>Teaching Assistant</span>: Added here, can view the course and grade assignments, but not the main instructor
              </Text>
              <Text type="secondary" style={{ fontSize: '12px' }}>
                💡 Both types of teachers can see the course and grade assignments
              </Text>
            </Space>
          </div>
        </Form>
      </Card>

      <Card title="📋 View Course Enrollment Status">
        <Space style={{ marginBottom: 16 }}>
          <Form.Item label="Select Course" style={{ marginBottom: 0 }}>
            <Select
              showSearch
              placeholder="Select course to view enrollment"
              style={{ width: 300 }}
              value={selectedCourse || undefined}
              onChange={(value) => {
                setSelectedCourse(value);
                setSelectedEnrollmentRole(null); // Reset role filter
              }}
              allowClear
              onClear={() => {
                setSelectedCourse('');
                setSelectedEnrollmentRole(null);
              }}
              optionFilterProp="children"
              filterOption={(input, option) =>
                (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
              }
              options={courses.map(course => ({
                value: course.courseCode,
                label: `${course.courseCode} - ${course.courseName}`
              }))}
            />
          </Form.Item>
          
          {selectedCourse && (
            <Form.Item label="Filter Role" style={{ marginBottom: 0 }}>
              <Select
                placeholder="All"
                style={{ width: 150 }}
                value={selectedEnrollmentRole}
                onChange={setSelectedEnrollmentRole}
                allowClear
              >
                <Select.Option value="STUDENT">Student</Select.Option>
                <Select.Option value="TEACHER">Teacher</Select.Option>
              </Select>
            </Form.Item>
          )}
        </Space>

        {selectedCourse && (
          <>
            <h3>
              Course: {selectedCourse}
              {selectedEnrollmentRole && (
                <Tag color="blue" style={{ marginLeft: 8 }}>
                  {selectedEnrollmentRole === 'STUDENT' ? 'Student' : 'Teacher'}
                </Tag>
              )}
            </h3>
            <Table
              columns={[
                {
                  title: 'User ID',
                  dataIndex: 'studentId',
                  key: 'studentId',
                  width: 80,
                },
                {
                  title: 'Username',
                  dataIndex: 'username',
                  key: 'username',
                },
                {
                  title: 'Student ID',
                  dataIndex: 'studentIdNumber',
                  key: 'studentIdNumber',
                  render: (id) => id || <Text type="secondary">-</Text>,
                },
                {
                  title: 'Full Name',
                  dataIndex: 'studentName',
                  key: 'studentName',
                },
                {
                  title: 'Identity',
                  key: 'identity',
                  render: (_, record) => {
                    if (record.isMainTeacher) {
                      return (
                        <Space>
                          <Tag color="gold">👨‍🏫 Main Instructor</Tag>
                          <Text type="secondary">(Specified when creating course)</Text>
                        </Space>
                      );
                    }
                    const colorMap = {
                      'STUDENT': 'blue',
                      'TEACHER': 'green',
                      'TECHNICAL_TEAM': 'orange',
                    };
                    const labelMap = {
                      'STUDENT': 'Student',
                      'TEACHER': 'Teaching Assistant',
                      'TECHNICAL_TEAM': 'Technical Team',
                    };
                    return <Tag color={colorMap[record.role]}>{labelMap[record.role] || record.role}</Tag>;
                  }
                },
                {
                  title: 'Email',
                  dataIndex: 'email',
                  key: 'email',
                },
                {
                  title: 'Enrolled At',
                  dataIndex: 'enrolledAt',
                  key: 'enrolledAt',
                  render: (date) => date ? new Date(date).toLocaleString('en-US') : '',
                },
                {
                  title: 'Actions',
                  key: 'action',
                  render: (_, record) => {
                    if (record.isMainTeacher) {
                      return (
                        <Text type="secondary" style={{ fontSize: '12px' }}>
                          Main instructor cannot be removed
                        </Text>
                      );
                    }
                    return (
                      <Popconfirm
                        title="Are you sure to drop this course?"
                        onConfirm={() => handleDropCourse(record.enrollmentId)}
                        okText="Yes"
                        cancelText="No"
                      >
                        <Button type="link" danger size="small">
                          Drop
                        </Button>
                      </Popconfirm>
                    );
                  },
                },
              ]}
              dataSource={
                selectedEnrollmentRole 
                  ? enrollments.filter(e => {
                      // If filtering teachers, include both main instructors and teaching assistants
                      if (selectedEnrollmentRole === 'TEACHER') {
                        return e.role === 'TEACHER' || e.isMainTeacher;
                      }
                      // If filtering students, only include students
                      return e.role === selectedEnrollmentRole;
                    })
                  : enrollments
              }
              rowKey={(record) => record.enrollmentId || `main-teacher-${record.studentId}`}
              pagination={{ pageSize: 10 }}
            />
          </>
        )}
      </Card>

      <Card title="💡 Usage Guide" style={{ marginTop: 16 }}>
        <ul>
          <li>📚 <Text strong>Course Creation</Text>: Create courses in "Course Table" tab and specify <Text strong type="success">main instructor</Text></li>
          <li>✅ <Text strong>Add Users</Text>: Add students or teachers (teaching assistants) to specified courses</li>
          <li>👨‍🏫 <Text strong>Teacher Permissions</Text>: Both main instructors and teaching assistants can view courses and grade assignments</li>
          <li>🎓 <Text strong>Student Permissions</Text>: Can only view and submit assignments for enrolled courses</li>
          <li>❌ <Text strong type="danger">Restrictions</Text>: Technical team and administrators cannot be added to courses</li>
          <li>🔒 <Text strong>Role Validation</Text>: When creating courses, only users with <Text type="success">TEACHER role</Text> can be selected as main instructor</li>
          <li>📋 <Text strong>Filter Function</Text>: View enrollment status can be filtered by role (Student/Teacher)</li>
          <li>🆔 <Text strong>User ID</Text>: Can be viewed and copied in the "User Management" tab</li>
        </ul>
      </Card>
          </div>
        </TabPane>

        <TabPane tab="📋 Audit Logs" key="3">
          <Card 
            title="System Audit Logs" 
            extra={
              <Space>
                <Text type="secondary">Auto-loaded</Text>
                <Button onClick={fetchAuditLogs}>Refresh Logs</Button>
              </Space>
            }
          >
            <div style={{ 
              background: '#fff7e6', 
              padding: '12px', 
              borderRadius: '4px', 
              marginBottom: '16px',
              border: '1px solid #ffd591'
            }}>
              <Space direction="vertical">
                <Text strong>📋 Audit Log Description:</Text>
                <Text>• Auto-records all system operations (login, register, create assignment, submit work, grade, publish grade, appeal, etc.)</Text>
                <Text>• Shows operator, time, operation type, related entity and details</Text>
                <Text>• Used for system monitoring, issue tracking and security audit</Text>
                <Text>• Auto-loads latest 100 logs on page load, click "Refresh Logs" to reload</Text>
              </Space>
            </div>
            <Table
              dataSource={auditLogs}
              rowKey="id"
              loading={loadingLogs}
              pagination={{ pageSize: 20 }}
              scroll={{ x: 1200 }}
              columns={[
                {
                  title: 'Time',
                  dataIndex: 'createdAt',
                  key: 'createdAt',
                  width: 180,
                  render: (date) => date ? new Date(date).toLocaleString('en-US') : '-',
                },
                {
                  title: 'User',
                  dataIndex: 'username',
                  key: 'username',
                  width: 120,
                },
                {
                  title: 'Role',
                  dataIndex: 'userRole',
                  key: 'userRole',
                  width: 100,
                  render: (role) => {
                    const colorMap = {
                      'STUDENT': 'blue',
                      'TEACHER': 'green',
                      'TECHNICAL_TEAM': 'orange',
                      'ADMIN': 'red',
                      'SYSTEM': 'gray'
                    };
                    return <Tag color={colorMap[role]}>{role}</Tag>;
                  }
                },
                {
                  title: 'Action',
                  dataIndex: 'action',
                  key: 'action',
                  width: 150,
                  render: (action) => {
                    const colorMap = {
                      'LOGIN': 'blue',
                      'REGISTER': 'green',
                      'CREATE_ASSIGNMENT': 'cyan',
                      'SUBMIT_WORK': 'purple',
                      'AI_SCORE': 'orange',
                      'REVIEW_GRADE': 'gold',
                      'PUBLISH_GRADE': 'lime',
                      'CREATE_APPEAL': 'volcano',
                      'RESOLVE_APPEAL': 'magenta',
                    };
                    return <Tag color={colorMap[action] || 'default'}>{action}</Tag>;
                  }
                },
                {
                  title: 'Entity Type',
                  dataIndex: 'entityType',
                  key: 'entityType',
                  width: 120,
                },
                {
                  title: 'Entity ID',
                  dataIndex: 'entityId',
                  key: 'entityId',
                  width: 80,
                },
                {
                  title: 'Details',
                  dataIndex: 'details',
                  key: 'details',
                  ellipsis: true,
                  render: (details) => {
                    try {
                      const parsed = JSON.parse(details);
                      return <pre style={{ fontSize: '11px', margin: 0 }}>{JSON.stringify(parsed, null, 2)}</pre>;
                    } catch (e) {
                      return details;
                    }
                  }
                },
              ]}
            />
          </Card>
        </TabPane>
      </Tabs>
    </div>
  );
};

export default TechManagement;
