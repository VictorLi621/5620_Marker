# Batch Chinese to English Translation Script for Frontend
# Run this in PowerShell from the project root directory

$translations = @{
    # Common UI elements
    "请输入" = "Please enter"
    "请选择" = "Please select"
    "提交" = "Submit"
    "取消" = "Cancel"
    "确认" = "Confirm"
    "删除" = "Delete"
    "编辑" = "Edit"
    "查看" = "View"
    "返回" = "Back"
    "保存" = "Save"
    "上传" = "Upload"
    "下载" = "Download"
    "搜索" = "Search"
    "筛选" = "Filter"
    "刷新" = "Refresh"
    "加载中" = "Loading"
    "加载失败" = "Failed to load"
    "操作成功" = "Operation successful"
    "操作失败" = "Operation failed"
    
    # Roles
    "学生" = "Student"
    "教师" = "Teacher"
    "管理员" = "Admin"
    "技术团队" = "Technical Team"
    
    # Pages
    "提交作业" = "Submit Assignment"
    "查看成绩" = "View Results"
    "分析仪表板" = "Analytics Dashboard"
    "申诉管理" = "Appeal Management"
    "用户管理" = "User Management"
    "课程管理" = "Course Management"
    "作业管理" = "Assignment Management"
    
    # Assignment related
    "作业" = "Assignment"
    "作业列表" = "Assignment List"
    "创建作业" = "Create Assignment"
    "作业标题" = "Assignment Title"
    "作业描述" = "Assignment Description"
    "截止日期" = "Due Date"
    "总分" = "Total Marks"
    "评分标准" = "Grading Rubric"
    
    # Submission related
    "提交" = "Submission"
    "已提交" = "Submitted"
    "处理中" = "Processing"
    "已评分" = "Graded"
    "待审核" = "Pending Review"
    
    # Grading related
    "分数" = "Score"
    "评分" = "Grading"
    "反馈" = "Feedback"
    "置信度" = "Confidence"
    "AI评分" = "AI Grading"
    "教师评分" = "Teacher Score"
    "发布成绩" = "Publish Grade"
    
    # Appeal related
    "申诉" = "Appeal"
    "申诉理由" = "Appeal Reason"
    "批准" = "Approve"
    "拒绝" = "Reject"
    "处理申诉" = "Process Appeal"
    
    # Analytics
    "成绩分布" = "Score Distribution"
    "平均分" = "Mean Score"
    "中位数" = "Median"
    "标准差" = "Standard Deviation"
    "样本数量" = "Sample Size"
    "常见问题" = "Common Issues"
    "出现次数" = "Occurrences"
    "占比" = "Percentage"
    
    # Course related
    "课程" = "Course"
    "课程代码" = "Course Code"
    "课程名称" = "Course Name"
    "选课" = "Enrollment"
    "添加课程" = "Add Course"
    
    # Messages
    "登录成功" = "Login successful"
    "登录失败" = "Login failed"
    "注册成功" = "Registration successful"
    "注册失败" = "Registration failed"
    "上传成功" = "Upload successful"
    "上传失败" = "Upload failed"
    "处理完成" = "Processing complete"
    "处理失败" = "Processing failed"
    "您还未选修任何课程" = "You have not enrolled in any courses"
    "请联系技术团队" = "Please contact the technical team"
}

Write-Host "This script would translate Chinese text to English in frontend files."
Write-Host "Note: For complete translation, it's recommended to manually edit each file"
Write-Host "as automatic replacement may cause issues with context-specific translations."
Write-Host ""
Write-Host "Files to translate:"
Write-Host "- StudentResults.jsx"
Write-Host "- TeacherReview.jsx"
Write-Host "- AppealList.jsx"
Write-Host "- CreateAssignment.jsx"
Write-Host "- TechManagement.jsx"
Write-Host ""
Write-Host "Recommendation: Use this as a reference and manually translate remaining files."

