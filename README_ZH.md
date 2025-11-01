# 智能评分系统

中文版 | [English](./README.md)

## 项目简介

这是一个基于AI的智能评分系统，用于自动化批改学生作业。系统支持多种文件格式（PDF、Word、图片），使用OpenAI GPT-4模型进行智能评分，并提供丰富的分析功能。

## 系统架构

- **前端**: React 18 + Ant Design
- **后端**: Spring Boot 3.x (Java 17)
- **数据库**: PostgreSQL 15
- **存储**: 阿里云OSS
- **AI**: OpenAI GPT-4o-mini (文本 + Vision)
- **部署**: Docker + Docker Compose

## 核心功能

### 学生端
- ✅ 上传作业（支持PDF、Word、图片）
- ✅ 预览提交内容
- ✅ 查看评分和反馈
- ✅ 申诉评分
- ✅ 查看个人进度
- ✅ 查看个性化学习计划

### 教师端
- ✅ 创建作业和评分标准
- ✅ 审核AI评分结果
- ✅ 调整分数和反馈
- ✅ 发布成绩（带版本控制）
- ✅ 处理学生申诉
- ✅ 查看班级分析仪表盘
  - 成绩分布图
  - AI智能错误热图
  - 知识点统计
  - 难度/区分度分析

### 技术团队
- ✅ 创建和管理课程
- ✅ 管理班级和学生注册
- ✅ 分配教师和学生到课程
- ✅ 查看审计日志
- ✅ 用户管理

## AI Agent 能力展示

### 1. 智能文本提取
- PDF/Word文档OCR处理
- 图片内容识别（OpenAI Vision API）
- 数学公式识别
- 学生信息自动匿名化

### 2. 智能评分
- 基于评分标准的客观评分
- 主观题评价和建议
- 置信度评估
- 低置信度提醒教师审核

### 3. 智能反馈生成
- 个性化反馈（指出问题、原因、改进建议）
- 知识点关联
- 改进验证方法
- 结构化JSON格式反馈

### 4. 智能数据分析
- **AI分析班级共性问题**（错误热图）
- 难度和区分度计算
- 学生进步趋势
- 常见误解识别

### 5. 异步处理和重试机制
- 通知自动重试（最多3次）
- 后台异步处理（OCR、评分）
- 指数退避策略
- Spring @Scheduled 任务调度

## 快速开始

### 前置要求

- Docker 和 Docker Compose
- OpenAI API密钥
- 阿里云OSS账号（可选，可以运行在模拟模式）

### 1. 配置环境变量

创建 `.env` 文件：

```env
# 数据库配置
POSTGRES_USER=admin
POSTGRES_PASSWORD=admin123
POSTGRES_DB=grading_system

# 阿里云OSS配置（可选）
ALIYUN_OSS_ENDPOINT=your-endpoint
ALIYUN_OSS_ACCESS_KEY_ID=your-access-key
ALIYUN_OSS_ACCESS_KEY_SECRET=your-secret-key
ALIYUN_OSS_BUCKET_NAME=your-bucket-name

# OpenAI配置
OPENAI_API_KEY=sk-your-api-key-here
OPENAI_MODEL=gpt-4o-mini
OPENAI_VISION_MODEL=gpt-4o-mini
OPENAI_VISION_ENABLED=true
```

### 2. 启动系统

```bash
# 一键启动（会自动构建镜像）
docker-compose up -d --build

# 查看启动状态
docker-compose ps

# 查看日志
docker-compose logs -f backend
```

### 3. 访问系统

- **前端地址**: http://localhost:8081
- **后端API**: http://localhost:8080

### 4. 默认账号

系统预置了三个测试账号：

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 学生 | student | password | 学生账号 |
| 教师 | teacher | password | 教师账号 |
| 管理员 | admin | password | 管理员账号 |

## 演示流程

### 完整工作流演示

1. **技术团队创建课程**
   - 登录 `admin/password`
   - 创建课程（如：ELEC5620）
   - 为课程创建班级
   - 添加教师和学生到班级

2. **教师创建作业**
   - 登录 `teacher/password`
   - 创建作业（标题、描述、评分标准）
   - 设置截止日期和总分
   - 定义每个问题的评分细则

3. **学生提交作业**
   - 登录 `student/password`
   - 选择课程和作业
   - 上传文件（测试图片效果最佳）
   - 预览提交内容
   - 确认提交

4. **AI自动评分**（后台自动进行）
   - OCR文本提取
   - 匿名化处理
   - AI评分（客观题+主观题）
   - 生成反馈
   - 计算置信度

5. **教师审核**
   - 查看AI评分结果
   - 调整分数（如需要）
   - 添加评语或音频批注
   - 发布成绩（创建快照）

6. **学生申诉**
   - 查看评分和反馈
   - 提交申诉理由
   - 等待教师处理
   - 收到处理结果通知

7. **教师处理申诉**
   - 查看申诉理由
   - 重新评估提交
   - 批准/拒绝申诉并说明理由
   - 批准后更新分数（创建新版本）

8. **数据分析**
   - 查看班级分析仪表盘
   - **错误热图**：AI智能分析全班共性问题
   - 成绩分布、难度分析
   - 导出报告

## 高级技术特性

### 1. AI智能分析（演示重点）

**错误热图 - AI智能分析**：
- 收集所有学生的AI反馈
- 使用OpenAI分析共性问题
- 动态生成5-10个最常见问题
- 自动排序和统计

**代码位置**: `backend/src/main/java/com/intelligentmarker/service/AnalyticsService.java`

### 2. 异步处理

- 使用 `@Async` 注解
- OCR和评分在后台执行
- 不阻塞用户操作
- 任务执行器配置

### 3. 通知重试机制

- 使用 `@Scheduled` 定时任务
- 失败通知自动重试（最多3次）
- 指数退避策略
- 通知状态跟踪

### 4. 成绩版本控制

- 基于快照的版本管理
- 不可变的成绩历史
- 申诉触发的重新发布
- 所有变更的审计跟踪

### 5. Docker容器化

- 前后端分离
- 数据库持久化
- 一键启动/停止
- 健康检查监控

### 6. CI/CD流水线

- GitHub Actions自动化
- 代码质量检查
- 自动化测试
- Docker镜像构建

## 项目结构

```
5620/
├── backend/                      # Spring Boot后端
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/intelligentmarker/
│   │       │       ├── controller/       # REST API控制器
│   │       │       ├── service/          # 业务逻辑
│   │       │       │   ├── ScoringService.java
│   │       │       │   ├── AnalyticsService.java
│   │       │       │   ├── AppealService.java
│   │       │       │   ├── NotificationService.java
│   │       │       │   └── AnonymizationService.java
│   │       │       ├── model/            # 数据模型
│   │       │       │   ├── User.java
│   │       │       │   ├── Assignment.java
│   │       │       │   ├── Submission.java
│   │       │       │   ├── Grade.java
│   │       │       │   ├── GradeSnapshot.java
│   │       │       │   └── Appeal.java
│   │       │       └── repository/       # 数据访问
│   │       └── resources/
│   │           ├── application.yml       # 应用配置
│   │           └── data.sql             # 初始化数据
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                     # React前端
│   ├── src/
│   │   ├── pages/               # 页面组件
│   │   │   ├── Login.jsx
│   │   │   ├── UploadAssignment.jsx
│   │   │   ├── TeacherReview.jsx
│   │   │   ├── StudentResults.jsx
│   │   │   ├── AppealList.jsx
│   │   │   ├── Dashboard.jsx
│   │   │   └── ClassManagement.jsx
│   │   └── App.jsx              # 主应用
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
├── .github/workflows/            # CI/CD配置
├── docker-compose.yml            # Docker编排
├── .env                          # 环境变量
├── START.sh                      # 启动脚本
└── README.md
```

## 关键文件说明

### AI评分逻辑
- `ScoringService.java` - 完整的AI评分流程实现
- 支持置信度阈值配置
- 处理客观题和主观题
- 生成结构化JSON格式反馈

### Vision API集成
- `OpenAIVisionAdapter.java` - 图片识别和文本提取
- 数学公式识别能力
- API不可用时回退到模拟数据

### 智能分析
- `AnalyticsService.java` - AI智能错误热图分析
- 成绩分布统计
- 难度和区分度计算

### 申诉工作流
- `AppealService.java` - 完整的申诉处理流程
- 基于快照的成绩版本控制
- 状态管理（PENDING → APPROVED/REJECTED）

### 匿名化
- `AnonymizationService.java` - 学生信息移除
- 基于模式的PII检测（学号、邮箱、电话）
- 为学生生成确认预览

## 系统管理

### 查看日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f postgres
```

### 重启服务

```bash
# 重启所有服务
docker-compose restart

# 重启特定服务
docker-compose restart backend
```

### 停止和清理

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷（清空数据库）
docker-compose down -v

# 清理Docker缓存
docker system prune -f
```

### 数据库操作

```bash
# 进入数据库
docker exec -it intelligent-marker-db psql -U admin -d grading_system

# 查看表
\dt

# 查看用户
SELECT id, username, role FROM users;

# 查看提交
SELECT id, status, file_type FROM submissions ORDER BY id DESC;

# 查看成绩
SELECT id, ai_score, teacher_score, status FROM grades;

# 退出
\q
```

## 故障排除

### 问题1: 无法启动

**解决方案**:
```bash
docker-compose down -v
docker system prune -f
docker-compose up -d --build
```

### 问题2: OpenAI API调用失败

**检查步骤**:
1. 确认`.env`中的`OPENAI_API_KEY`正确
2. 检查OpenAI账号余额
3. 查看后端日志：`docker logs intelligent-marker-backend -f`
4. API失败时系统会回退到模拟模式

### 问题3: 图片识别使用Mock数据

**解决方案**:
1. 确认`.env`中`OPENAI_VISION_MODEL=gpt-4o-mini`
2. 重启backend: `docker-compose restart backend`
3. 重新提交图片

### 问题4: 数据库连接失败

**解决方案**:
```bash
# 检查数据库状态
docker-compose ps postgres

# 重启数据库
docker-compose restart postgres

# 等待30秒后重启backend
docker-compose restart backend
```

## 技术栈详情

### 后端技术
- Spring Boot 3.2.0
- Spring Data JPA (Hibernate)
- Spring Security
- PostgreSQL 15
- Lombok
- Jackson (JSON处理)
- PDFBox (PDF处理)
- Apache POI (Word处理)
- RestTemplate (HTTP客户端)

### 前端技术
- React 18.2
- Ant Design 5.x
- Axios (HTTP客户端)
- Recharts (数据可视化)

### DevOps
- Docker 24.x
- Docker Compose 2.x
- GitHub Actions
- Nginx

### AI集成
- OpenAI GPT-4o-mini (文本理解和评分)
- OpenAI Vision API (图片识别)
- 自定义Prompt工程
- 离线测试的Mock适配器

## 性能优化

1. **数据库索引**：在常用查询字段上创建索引
2. **异步处理**：OCR和评分不阻塞主流程
3. **连接池**：优化数据库连接池
4. **批量处理**：批量查询通知重试
5. **懒加载**：JPA关系按需加载

## 安全特性

1. **密码加密**：所有密码使用BCrypt哈希
2. **基于角色的访问控制**：STUDENT, TEACHER, ADMIN, TECHNICAL_TEAM
3. **审计日志**：记录所有关键操作
4. **数据匿名化**：评分时移除PII
5. **输入验证**：所有输入的服务器端验证

## 未来扩展

- [ ] 多语言支持（国际化）
- [ ] 文本转语音反馈
- [ ] 智能题库生成
- [ ] 代码作业分析（语法检查、抄袭检测）
- [ ] 移动端应用
- [ ] 实时协作批改
- [ ] 高级分析仪表盘
- [ ] 与LMS平台集成

## 开发团队

- **Stage 1**: 系统设计和架构
- **Stage 2**: 原型实现和演示
- **演示时间**: Week 13

## 许可证

本项目仅用于学术演示，请勿用于商业用途。

---

## 演示准备清单

- ✅ Docker环境正常
- ✅ OpenAI API Key有效且有余额
- ✅ 阿里云OSS配置正确（或模拟模式准备就绪）
- ✅ 测试数据准备就绪
- ✅ 演示流程熟悉
- ✅ 备用方案（模拟模式）

**祝演示成功！** 🎉

## 支持

如有问题：
- 查看[故障排除](#故障排除)部分
- 查看日志：`docker-compose logs -f`
- 验证`.env`中的环境变量
- 重启服务：`docker-compose restart`

---

**版本**: 1.0.0
**最后更新**: 2024
