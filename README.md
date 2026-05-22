<div align="center">

# 🏋️ Fitness-Web

### AI 智能健身助手

**Spring Boot + React 全栈项目 | AI 对话 | 训练计划 | 饮食管理 | 数据追踪**

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://react.dev/)
[![AI](https://img.shields.io/badge/AI-Multi_Model-purple.svg)]()

</div>

---

## ✨ 项目简介

Fitness-Web 是一款基于 AI 的智能健身助手平台。通过自然语言对话，用户可以轻松管理训练计划、记录饮食运动、获取健身知识。AI 教练 "Tatan" 会根据用户画像提供个性化的训练和饮食建议。

**在线体验：[zhouzhou.cn](https://zhouzhou.cn)**

---

## 🛠️ 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.x + MyBatis-Plus |
| 前端框架 | React 18 + Umi 4 + Ant Design |
| AI 模型 | 通义千问 Qwen / DeepSeek / 智谱 GLM（多模型切换） |
| 知识库 | Chroma 向量数据库 + RAG 检索增强 |
| 数据库 | MySQL 8.0 |
| 文件存储 | 腾讯云 COS |
| 部署 | Docker Compose + Nginx 反向代理 |

---

## 🎯 核心功能

### 💬 AI 智能教练对话
- **流式对话**：SSE 实时逐字输出，体验流畅
- **意图识别**：自动识别用户意图（查计划/查记录/保存记录/生成计划/知识问答）
- **多模型切换**：支持通义千问、DeepSeek、智谱 GLM，按需切换
- **RAG 知识库**：基于向量数据库的健身知识检索，回答专业健身问题

### 📋 训练管理
- **训练计划**：自定义训练模板 + 循环机制，自动排期
- **查看其他训练日**：一键切换查看每天的训练安排
- **动作库**：按肌群分类的完整动作数据库，支持收藏

### 🥗 饮食管理
- **饮食计划**：按餐次管理（早餐/午餐/晚餐/加餐），支持模板循环
- **食物库**：系统 + 自定义食物，含热量/蛋白质/碳水/脂肪数据
- **一键记录**：对话中直接保存饮食记录，无需手动填表

### 📊 数据追踪
- **体重趋势图**：可视化体重变化曲线
- **热量日历**：每日摄入/消耗热量追踪
- **训练/饮食记录**：完整的运动饮食历史记录

### 👤 用户系统
- 登录/注册 + 验证码
- 个人信息管理（身高/体重/年龄/目标）
- 头像上传（COS 云存储）
- 管理员后台（用户管理/食物管理）

---

## 📁 项目结构

```
Fitness-Web/
├── Fitness-backend/          # Spring Boot 后端
│   ├── src/main/java/
│   │   └── com.zz.usercenter/
│   │       ├── controller/   # 接口层（10个Controller）
│   │       ├── service/      # 业务层（17个Service）
│   │       ├── mapper/       # 数据层（MyBatis-Plus）
│   │       ├── model/        # 实体/请求/VO
│   │       ├── config/       # AI模型配置/跨域/COS
│   │       └── util/         # 工具类
│   └── src/main/resources/
│       ├── application.yml           # 主配置（环境变量占位）
│       └── application.example.yml   # 配置模板
│
├── Fitness-frontend/          # React 前端
│   └── src/
│       ├── pages/            # 页面（对话/动作库/收藏/个人中心/管理）
│       ├── components/       # 通用组件
│       ├── services/         # API 调用
│       ├── hooks/            # 自定义 Hooks
│       └── utils/            # 工具函数
│
├── nginx/                    # Nginx 配置
├── docker-compose.example.yml
└── .gitignore
```

---

## 🚀 快速开始

### 环境要求
- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Python 3.10+（Chroma 向量库，可选）

### 后端

```bash
cd Fitness-backend

# 1. 配置环境变量（参考 application.example.yml）
#    QWEN_API_KEY / DEEPSEEK_API_KEY / GLM_API_KEY / COS_SECRET_ID / COS_SECRET_KEY

# 2. 修改数据库连接
#    application.yml 中 datasource.url / username / password

# 3. 启动
./mvnw spring-boot:run
```

后端默认运行在 `http://localhost:8080/api`

### 前端

```bash
cd Fitness-frontend

npm install
npm run dev
```

前端默认运行在 `http://localhost:8000`

---

## 🔐 安全说明

本项目所有密钥（API Key、数据库密码、云存储密钥）均通过**环境变量**注入，不存入代码仓库。

- `application.yml` — 仅包含 `${ENV_NAME}` 占位符
- `application-prod.yml` — 生产配置，已在 `.gitignore` 中排除
- `docker-compose.yml` — 含真实密钥，已在 `.gitignore` 中排除
- `application.example.yml` — 配置模板，可作为参考

---

## 📝 开发日志

- [x] 用户系统（注册/登录/个人信息/头像上传）
- [x] 动作库（按肌群分类/收藏）
- [x] 训练计划管理（模板+循环）
- [x] 饮食计划管理（模板+循环）
- [x] 运动/饮食记录追踪
- [x] AI 对话（流式SSE/多模型/RAG知识库）
- [x] AI 意图识别（查计划/查记录/保存记录/生成计划）
- [x] 体重趋势/热量日历可视化
- [x] 管理员后台
- [x] Docker 部署 + Nginx HTTPS

---

<div align="center">

**Made with ❤️ by Zz**

</div>
