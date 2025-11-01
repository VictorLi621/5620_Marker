import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Tag, Space, message, Spin, Empty, Statistic, Row, Col } from 'antd';
import { FileTextOutlined, EyeOutlined, PlusOutlined } from '@ant-design/icons';
import axios from 'axios';

const TeacherAssignments = ({ user, onNavigate }) => {
  const [assignments, setAssignments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({
    total: 0,
    published: 0,
    draft: 0,
    closed: 0
  });

  useEffect(() => {
    fetchAssignments();
  }, []);

  const fetchAssignments = async () => {
    try {
      setLoading(true);
      const response = await axios.get(`/api/assignments?teacherId=${user.id}&page=0&size=100`);
      const assignmentList = response.data.content || [];
      setAssignments(assignmentList);

      // Calculate statistics
      const published = assignmentList.filter(a => a.status === 'PUBLISHED').length;
      const draft = assignmentList.filter(a => a.status === 'DRAFT').length;
      const closed = assignmentList.filter(a => a.status === 'CLOSED').length;

      setStats({
        total: assignmentList.length,
        published,
        draft,
        closed
      });
    } catch (error) {
      console.error('Failed to fetch assignments:', error);
      message.error('Failed to load assignments: ' + (error.response?.data?.error || error.message));
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    {
      title: 'Assignment Title',
      dataIndex: 'title',
      key: 'title',
      render: (text, record) => (
        <Space>
          <FileTextOutlined />
          <strong>{text}</strong>
        </Space>
      ),
    },
    {
      title: 'Course Code',
      dataIndex: 'courseCode',
      key: 'courseCode',
      render: (code) => <Tag color="blue">{code}</Tag>,
    },
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
      render: (text) => text || 'No description',
    },
    {
      title: 'Total Marks',
      dataIndex: 'totalMarks',
      key: 'totalMarks',
      align: 'center',
      render: (marks) => <Tag color="purple">{marks} points</Tag>,
    },
    {
      title: 'Due Date',
      dataIndex: 'dueDate',
      key: 'dueDate',
      render: (date) => date ? new Date(date).toLocaleString('en-US') : 'No due date',
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        const colorMap = {
          PUBLISHED: 'green',
          DRAFT: 'orange',
          CLOSED: 'red'
        };
        return <Tag color={colorMap[status] || 'default'}>{status}</Tag>;
      },
    },
    {
      title: 'Actions',
      key: 'actions',
      align: 'center',
      render: (_, record) => (
        <Space>
          <Button
            type="primary"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => onNavigate('assignment-details', { assignmentId: record.id })}
          >
            View Details
          </Button>
        </Space>
      ),
    },
  ];

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" tip="Loading assignments..." />
      </div>
    );
  }

  return (
    <div>
      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2>My Assignments</h2>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => onNavigate('create')}
        >
          Create New Assignment
        </Button>
      </div>

      {/* Statistics Cards */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="Total Assignments"
              value={stats.total}
              valueStyle={{ color: '#1890ff' }}
              prefix={<FileTextOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Published"
              value={stats.published}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Draft"
              value={stats.draft}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Closed"
              value={stats.closed}
              valueStyle={{ color: '#f5222d' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Assignments Table */}
      <Card>
        {assignments.length === 0 ? (
          <Empty
            description="No assignments found"
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          >
            <Button type="primary" onClick={() => onNavigate('create')}>
              Create Your First Assignment
            </Button>
          </Empty>
        ) : (
          <Table
            columns={columns}
            dataSource={assignments}
            rowKey="id"
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showTotal: (total) => `Total ${total} assignments`,
            }}
          />
        )}
      </Card>
    </div>
  );
};

export default TeacherAssignments;
