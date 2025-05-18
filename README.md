# AI辅助旅行规划应用

## 项目概述

AITravel是一个基于安卓平台的智能旅行规划应用，集成了AI聊天助手功能，帮助用户创建、优化和管理旅行行程。该应用利用高德地图API提供位置服务，并通过自建的AI服务提供餐厅推荐、景点推荐等智能功能。

## 核心功能

- **行程创建与管理**：创建多日行程，添加景点和餐厅，设置访问顺序和交通方式
- **AI聊天助手**：通过自然语言与AI助手交互，获取旅行建议
- **智能推荐**：AI根据行程上下文提供餐厅和景点推荐
- **地图显示**：在地图上直观展示行程路线和地点
- **行程详情查看**：查看行程的详细信息，包括每日安排、景点详情
- **行程编辑**：修改已创建的行程，调整行程顺序、交通方式等

## 项目结构

```
trave_v6-showmap/           # 安卓客户端
├── app/
│   ├── src/main/
│   │   ├── java/com/example/trave/
│   │   │   ├── Activities/       # UI界面
│   │   │   ├── Adapters/         # RecyclerView适配器
│   │   │   ├── Domains/          # 数据模型
│   │   │   ├── Services/         # 业务逻辑服务
│   │   │   └── DatabaseHelper.java  # 数据库管理
│   │   └── res/                  # 资源文件
│   └── build.gradle.kts          # 依赖管理
├── Summary_of_Changes.md         # 重构说明
└── Restaurant_Recommendations_Flow.md  # 餐厅推荐流程

4.27ver/                    # 后端服务
├── app.py                  # Flask API服务
├── main.py                 # 主逻辑处理
├── travel_tools/           # 工具类
└── models/                 # AI模型

amap_poi_details.py        # 高德POI数据提取脚本
```

## 技术栈

- **前端**：
  - Android原生开发 (Java)
  - RecyclerView、CardView等UI组件
  - SQLite本地数据库

- **后端**：
  - Flask Web框架
  - Python AI处理逻辑
  - RESTful API设计

- **外部API**：
  - 高德地图API

## 安装与运行

### 安卓客户端

1. 使用Android Studio打开`trave_v6-showmap`目录
2. 同步Gradle依赖
3. 构建并运行项目到Android设备或模拟器

### 后端服务

1. 进入`4.27ver`目录
2. 安装依赖：`pip install -r requirements.txt`
3. 启动Flask服务：`python app.py`

## 环境配置

- 将高德API密钥配置到环境变量：
  - 创建系统环境变量`AMAP_KEY`，值为您的高德API密钥
  - 重启应用后，系统将自动使用环境变量中的API密钥

## 使用流程

1. 打开应用，登录或创建账户
2. 创建新行程，设置目的地和天数
3. 添加景点到行程中，设置访问顺序
4. 使用AI聊天功能获取餐厅推荐
5. 选择您喜欢的餐厅，添加到行程
6. 查看地图上的完整行程路线
7. 编辑或保存行程

## 开发者须知

- 数据库结构在`DatabaseHelper.java`中定义
- AI功能与后端交互逻辑在`AIService.java`中实现
- 推荐功能的处理流程在单独的文档中详细说明
