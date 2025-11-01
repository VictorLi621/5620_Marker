import React, { useState } from 'react';
import { Layout, Menu, theme } from 'antd';
import {
  UploadOutlined,
  UserOutlined,
  DashboardOutlined,
  FileTextOutlined,
  WarningOutlined,
  LoginOutlined,
  PlusOutlined,
  SettingOutlined,
  BookOutlined,
  FolderOutlined
} from '@ant-design/icons';
import Login from './pages/Login';
import Register from './pages/Register';
import UploadAssignment from './pages/UploadAssignment';
import TeacherReview from './pages/TeacherReview';
import Dashboard from './pages/Dashboard';
import StudentResults from './pages/StudentResults';
import AppealList from './pages/AppealList';
import CreateAssignment from './pages/CreateAssignment';
import TechManagement from './pages/TechManagement';
import SubmissionSuccess from './pages/SubmissionSuccess';
import LearningPlan from './pages/LearningPlan';
import TeacherAssignments from './pages/TeacherAssignments';
import AssignmentDetails from './pages/AssignmentDetails';
import './index.css';

const { Header, Content, Sider } = Layout;

function App() {
  const [currentUser, setCurrentUser] = useState(null);
  const [selectedMenu, setSelectedMenu] = useState('upload');
  const [showRegister, setShowRegister] = useState(false);
  const [navigationParams, setNavigationParams] = useState({});

  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken();

  // Navigation handler with optional parameters
  const handleNavigate = (menuKey, params = {}) => {
    setSelectedMenu(menuKey);
    setNavigationParams(params);
  };

  // Set default menu based on user role (must be before conditional rendering)
  React.useEffect(() => {
    if (currentUser) {
      if (currentUser.role === 'TEACHER') {
        setSelectedMenu('review'); // Teachers default to review interface
      } else if (currentUser.role === 'TECHNICAL_TEAM' || currentUser.role === 'ADMIN') {
        setSelectedMenu('tech'); // Admins/Technical team default to technical management
      } else {
        setSelectedMenu('upload'); // Students default to upload interface
      }
    }
  }, [currentUser]);

  if (!currentUser) {
    if (showRegister) {
      return <Register 
        onRegisterSuccess={() => setShowRegister(false)}
        onBackToLogin={() => setShowRegister(false)}
      />;
    }
    return <Login 
      onLogin={setCurrentUser}
      onShowRegister={() => setShowRegister(true)}
    />;
  }

  const studentMenuItems = [
    {
      key: 'upload',
      icon: <UploadOutlined />,
      label: 'Upload Assignment',
    },
    {
      key: 'results',
      icon: <FileTextOutlined />,
      label: 'View Results',
    },
    {
      key: 'learning-plan',
      icon: <BookOutlined />,
      label: 'Learning Plan',
    },
  ];

  const teacherMenuItems = [
    {
      key: 'assignments',
      icon: <FolderOutlined />,
      label: 'My Assignments',
    },
    {
      key: 'create',
      icon: <PlusOutlined />,
      label: 'Create Assignment',
    },
    {
      key: 'review',
      icon: <UserOutlined />,
      label: 'Review Submissions',
    },
    {
      key: 'appeals',
      icon: <WarningOutlined />,
      label: 'Handle Appeals',
    },
    {
      key: 'dashboard',
      icon: <DashboardOutlined />,
      label: 'Analytics Dashboard',
    },
  ];

  const adminMenuItems = [
    {
      key: 'tech',
      icon: <SettingOutlined />,
      label: 'Technical Management',
    },
    {
      key: 'dashboard',
      icon: <DashboardOutlined />,
      label: 'System Overview',
    },
  ];

  // Select menu based on role
  let menuItems;
  if (currentUser.role === 'STUDENT') {
    menuItems = studentMenuItems;
  } else if (currentUser.role === 'TECHNICAL_TEAM' || currentUser.role === 'ADMIN') {
    menuItems = adminMenuItems;
  } else {
    menuItems = teacherMenuItems;
  }

  const renderContent = () => {
    switch (selectedMenu) {
      case 'upload':
        return <UploadAssignment user={currentUser} onNavigate={handleNavigate} />;
      case 'submission-success':
        return <SubmissionSuccess
          submissionId={navigationParams.submissionId}
          onNavigate={handleNavigate}
        />;
      case 'results':
        return <StudentResults
          user={currentUser}
          onNavigate={handleNavigate}
          selectedGradeId={navigationParams.selectedGradeId}
        />;
      case 'learning-plan':
        return <LearningPlan
          user={currentUser}
          gradeId={navigationParams.gradeId}
          assignmentTitle={navigationParams.assignmentTitle}
          onNavigate={handleNavigate}
        />;
      case 'assignments':
        return <TeacherAssignments user={currentUser} onNavigate={handleNavigate} />;
      case 'assignment-details':
        return <AssignmentDetails
          user={currentUser}
          onNavigate={handleNavigate}
          assignmentId={navigationParams.assignmentId}
        />;
      case 'create':
        return <CreateAssignment user={currentUser} />;
      case 'review':
        return <TeacherReview user={currentUser} />;
      case 'appeals':
        return <AppealList user={currentUser} />;
      case 'dashboard':
        return <Dashboard user={currentUser} />;
      case 'tech':
        return <TechManagement user={currentUser} />;
      default:
        return <div>Select a menu item</div>;
    }
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ color: 'white', fontSize: '20px', fontWeight: 'bold' }}>
          🎓 Intelligent Grading System
        </div>
        <div style={{ color: 'white' }}>
          {currentUser.fullName} ({currentUser.role})
          <LoginOutlined 
            style={{ marginLeft: 16, cursor: 'pointer' }} 
            onClick={() => setCurrentUser(null)} 
          />
        </div>
      </Header>
      <Layout>
        <Sider
          width={200}
          style={{
            background: colorBgContainer,
          }}
        >
          <Menu
            mode="inline"
            selectedKeys={[selectedMenu]}
            onClick={({ key }) => setSelectedMenu(key)}
            style={{
              height: '100%',
              borderRight: 0,
            }}
            items={menuItems}
          />
        </Sider>
        <Layout style={{ padding: '24px' }}>
          <Content
            style={{
              padding: 24,
              margin: 0,
              minHeight: 280,
              background: colorBgContainer,
              borderRadius: borderRadiusLG,
            }}
          >
            {renderContent()}
          </Content>
        </Layout>
      </Layout>
    </Layout>
  );
}

export default App;

