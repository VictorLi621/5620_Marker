#!/bin/bash

echo "🚀 启动智能批改系统..."
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ 错误：Docker未运行，请先启动Docker Desktop"
    exit 1
fi

echo "✅ Docker检查通过"
echo ""

# Start services
echo "📦 启动所有服务 (PostgreSQL + Backend + Frontend)..."
docker-compose up -d

echo ""
echo "⏳ 等待服务启动 (约30秒)..."
sleep 30

echo ""
echo "🔍 检查服务状态..."
docker ps | grep intelligent-marker

echo ""
echo "✅ 系统启动完成！"
echo ""
echo "📝 访问地址："
echo "  - 前端: http://localhost:8081"
echo "  - 后端: http://localhost:8081"
echo ""
echo "👤 Demo账号："
echo "  学生: student@uni.edu / password"
echo "  教师: teacher@uni.edu / password"
echo "  管理员: admin@uni.edu / password"
echo ""
echo "📖 查看日志: docker-compose logs -f"
echo "🛑 停止服务: docker-compose down"
echo ""
echo "🎉 准备就绪！打开浏览器访问 http://localhost:8081"

