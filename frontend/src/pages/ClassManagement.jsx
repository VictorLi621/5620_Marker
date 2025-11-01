import React, { useState, useEffect } from 'react';
import { Card, Form, Input, InputNumber, Button, Select, message, Table, Space, Modal, Tag, Descriptions } from 'antd';
import axios from 'axios';

const ClassManagement = ({ user }) => {
  const [form] = Form.useForm();
  const [addStudentForm] = Form.useForm();
  const [classes, setClasses] = useState([]);
  const [teachers, setTeachers] = useState([]);
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedClass, setSelectedClass] = useState(null);
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [addStudentModalVisible, setAddStudentModalVisible] = useState(false);

  useEffect(() => {
    fetchClasses();
    // In real application, need to call API to fetch teacher and student lists
  }, []);

  const fetchClasses = async () => {
    try {
      setLoading(true);
      const response = await axios.get('/api/classes?page=0&size=100');
      const classesData = response.data.content || response.data || [];
      setClasses(classesData);
    } catch (error) {
      console.error('Failed to fetch classes:', error);
      message.error('Failed to load class list');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateClass = async (values) => {
    setLoading(true);
    try {
      const response = await axios.post('/api/classes/create', values);

      if (response.data.success) {
        message.success('✅ Class created successfully!');
        form.resetFields();
        fetchClasses();
      } else {
        message.error('Creation failed: ' + response.data.error);
      }
    } catch (error) {
      console.error('Create class failed:', error);
      message.error('Creation failed: ' + (error.response?.data?.error || error.message));
    } finally {
      setLoading(false);
    }
  };

  const handleViewDetails = async (classItem) => {
    try {
      const response = await axios.get(`/api/classes/${classItem.id}`);
      setSelectedClass(response.data);
      setDetailModalVisible(true);
    } catch (error) {
      console.error('Failed to load class details:', error);
      message.error('Failed to load class details');
    }
  };

  const handleAddStudent = () => {
    if (!selectedClass) {
      message.error('Please select a class first');
      return;
    }
    setAddStudentModalVisible(true);
  };

  const handleAddStudentSubmit = async (values) => {
    try {
      const response = await axios.post(`/api/classes/${selectedClass.id}/add-student`, {
        studentId: parseInt(values.studentId)
      });

      if (response.data.success) {
        message.success('✅ Student added to class successfully!');
        addStudentForm.resetFields();
        setAddStudentModalVisible(false);
        // Refresh class details
        handleViewDetails({ id: selectedClass.id });
      } else {
        message.error('Add failed: ' + response.data.error);
      }
    } catch (error) {
      console.error('Add student failed:', error);
      message.error('Add failed: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleRemoveStudent = async (studentId) => {
    try {
      const response = await axios.delete(`/api/classes/${selectedClass.id}/remove-student/${studentId}`);

      if (response.data.success) {

        message.success('✅ Student removed from class successfully!');
        // Refresh class details
        handleViewDetails({ id: selectedClass.id });
      } else {
        message.error('Remove failed: ' + response.data.error);
      }
    } catch (error) {
      console.error('Remove student failed:', error);
      message.error('Remove failed: ' + (error.response?.data?.error || error.message));
    }
  };

  const columns = [
    {
      title: 'Class ID',
      dataIndex: 'classId',
      key: 'classId',
      width: 200,
    },
    {
      title: 'Class Name',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: 'Course Code',
      dataIndex: 'courseCode',
      key: 'courseCode',
      width: 120,
    },
    {
      title: 'Semester',
      dataIndex: 'semester',
      key: 'semester',
      width: 120,
    },
    {
      title: 'Instructor',
      dataIndex: 'teacherName',
      key: 'teacherName',
      width: 120,
    },
    {
      title: 'Students',
      key: 'size',
      width: 100,
      render: (_, record) => (
        <span>
          {record.currentSize} {record.capacity ? `/ ${record.capacity}` : ''}
        </span>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'active',
      key: 'active',
      width: 80,
      render: (active) => (
        <Tag color={active ? 'green' : 'red'}>
          {active ? 'Active' : 'Inactive'}
        </Tag>
      ),
    },
    {
      title: 'Actions',
      key: 'action',
      width: 120,
      render: (_, record) => (
        <Button type="link" size="small" onClick={() => handleViewDetails(record)}>
          View Details
        </Button>
      ),
    },
  ];

  const studentColumns = [
    {
      title: 'Student ID',
      dataIndex: 'id',
      key: 'id',
    },
    {
      title: 'Name',
      dataIndex: 'fullName',
      key: 'fullName',
    },
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: 'Actions',
      key: 'action',
      render: (_, record) => (
        <Button 
          type="link" 
          danger 
          size="small"
          onClick={() => {
            Modal.confirm({
              title: 'Confirm Removal',
              content: `Are you sure you want to remove ${record.fullName} from the class?`,
              onOk: () => handleRemoveStudent(record.id),
            });
          }}
        >
          Remove
        </Button>
      ),
    },
  ];

  return (
    <div>
      <h2>🏫 Class Management</h2>
      
      <Card title="➕ Create New Class" style={{ marginBottom: 16 }}>
        <Form
          form={form}
          layout="vertical"
          onFinish={handleCreateClass}
        >
          <Form.Item
            label="Class ID"
            name="classId"
            rules={[{ required: true, message: 'Please enter class ID' }]}
            extra="Format: {course code}-{semester}-{class number}, e.g.: ELEC5620-2024-S1-A"
          >
            <Input placeholder="e.g.: ELEC5620-2024-S1-A" />
          </Form.Item>

          <Form.Item
            label="Course Code"
            name="courseCode"
            rules={[{ required: true, message: 'Please enter course code' }]}
          >
            <Input placeholder="e.g.: ELEC5620" />
          </Form.Item>

          <Form.Item
            label="Class Name"
            name="name"
            rules={[{ required: true, message: 'Please enter class name' }]}
          >
            <Input placeholder="e.g.: Algorithm Design - Class A" />
          </Form.Item>

          <Form.Item
            label="Semester"
            name="semester"
          >
            <Input placeholder="e.g.: 2024-S1" />
          </Form.Item>

          <Form.Item
            label="Instructor ID"
            name="teacherId"
            rules={[{ required: true, message: 'Please enter instructor ID' }]}
            extra="Tip: Instructor ID can be found after user registration"
          >
            <Input placeholder="e.g.: 2" type="number" />
          </Form.Item>

          <Form.Item
            label="Class Description"
            name="description"
          >
            <Input.TextArea rows={3} placeholder="Class description (optional)" />
          </Form.Item>

          <Form.Item
            label="Class Capacity"
            name="capacity"
          >
            <InputNumber min={1} placeholder="e.g.: 50" style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading}>
              Create Class
            </Button>
          </Form.Item>
        </Form>
      </Card>

      <Card title="📋 Class List">
        <Table
          columns={columns}
          dataSource={classes}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      {/* Class Details Modal */}
      <Modal
        title="Class Details"
        open={detailModalVisible}
        onCancel={() => {
          setDetailModalVisible(false);
          setSelectedClass(null);
        }}
        footer={[
          <Button key="addStudent" type="primary" onClick={handleAddStudent}>
            Add Student
          </Button>,
          <Button key="close" onClick={() => setDetailModalVisible(false)}>
            Close
          </Button>,
        ]}
        width={800}
      >
        {selectedClass && (
          <div>
            <Descriptions bordered column={2} style={{ marginBottom: 16 }}>
              <Descriptions.Item label="Class ID" span={2}>
                {selectedClass.classId}
              </Descriptions.Item>
              <Descriptions.Item label="Class Name" span={2}>
                {selectedClass.name}
              </Descriptions.Item>
              <Descriptions.Item label="Course Code">
                {selectedClass.courseCode}
              </Descriptions.Item>
              <Descriptions.Item label="Semester">
                {selectedClass.semester || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Instructor">
                {selectedClass.teacherName}
              </Descriptions.Item>
              <Descriptions.Item label="Students">
                {selectedClass.currentSize} {selectedClass.capacity && `/ ${selectedClass.capacity}`}
              </Descriptions.Item>
              <Descriptions.Item label="Description" span={2}>
                {selectedClass.description || '-'}
              </Descriptions.Item>
            </Descriptions>

            <h3 style={{ marginTop: 16, marginBottom: 12 }}>👥 Class Students</h3>
            <Table
              columns={studentColumns}
              dataSource={selectedClass.students || []}
              rowKey="id"
              pagination={false}
              size="small"
            />
          </div>
        )}
      </Modal>

      {/* Add Student Modal */}
      <Modal
        title="Add Student to Class"
        open={addStudentModalVisible}
        onCancel={() => {
          setAddStudentModalVisible(false);
          addStudentForm.resetFields();
        }}
        footer={null}
      >
        <Form
          form={addStudentForm}
          layout="vertical"
          onFinish={handleAddStudentSubmit}
        >
          <Form.Item
            label="Student ID"
            name="studentId"
            rules={[{ required: true, message: 'Please enter student ID' }]}
            extra="Tip: Student ID can be found after user registration"
          >
            <Input placeholder="e.g.: 1" type="number" />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                Add
              </Button>
              <Button onClick={() => setAddStudentModalVisible(false)}>
                Cancel
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      <Card title="💡 Usage Instructions" style={{ marginTop: 16 }}>
        <ul>
          <li><strong>Class ID Format</strong>: Recommended format {`{course code}-{semester}-{class number}`}, e.g. ELEC5620-2024-S1-A</li>
          <li><strong>Creation Process</strong>: Admin creates class → Add students to class → Instructor selects class when creating assignments</li>
          <li><strong>Access Control</strong>: Students can only see assignments for their own class, instructors can only see submissions for their own class</li>
          <li><strong>Class Capacity</strong>: Optional field, when set the system will limit the number of students in the class</li>
        </ul>
      </Card>
    </div>
  );
};

export default ClassManagement;

