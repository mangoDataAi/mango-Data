# 贡献指南 - 芒果数据中台

感谢您有兴趣为芒果数据中台做出贡献！本文档提供了贡献的指南和说明。

## 目录

- [行为准则](#行为准则)
- [开发环境设置](#开发环境设置)
  - [前提条件](#前提条件)
  - [前端开发环境](#前端开发环境)
  - [后端开发环境](#后端开发环境)
  - [数据库设置](#数据库设置)
- [开发流程](#开发流程)
  - [分支管理](#分支管理)
  - [代码提交规范](#代码提交规范)
  - [代码审查流程](#代码审查流程)
  - [发布周期](#发布周期)
- [编码指南](#编码指南)
  - [JavaScript/Vue样式指南](#javascriptvue样式指南)
  - [Java编码规范](#java编码规范)
  - [CSS/SCSS指南](#cssscss指南)
  - [提交消息](#提交消息)
- [项目结构](#项目结构)
- [测试](#测试)
  - [前端测试](#前端测试)
  - [后端测试](#后端测试)
  - [端到端测试](#端到端测试)
- [文档](#文档)
- [版本控制](#版本控制)
- [常见问题](#常见问题)

## 行为准则

请阅读并遵守我们的[行为准则](CODE_OF_CONDUCT.md)。我们希望所有贡献者都能遵循这些准则，以确保项目社区健康、友好和包容。

## 开发环境设置

### 前提条件

- **Node.js** - v14.0.0或更高版本
- **npm** - v6.0.0或更高版本
- **Java JDK** - 1.8
- **Maven** - 3.6.0或更高版本
- **MySQL** - 5.7或更高版本 / **PostgreSQL** - 12或更高版本
- **Redis** - 5.0或更高版本
- **Git** - 2.0或更高版本

### 前端开发环境

1. **克隆仓库**
   ```bash
   git clone https://github.com/mangoDataAi/mango-Data.git
   cd mango-data-platform/mango-web
   ```

2. **安装依赖**
   ```bash
   npm install
   ```

3. **配置环境变量**
   ```bash
   # 复制配置文件模板
   cp .env.example .env.local
   
   # 编辑.env.local，配置API地址等
   # VUE_APP_API_BASE_URL=http://localhost:8080/api
   # VUE_APP_MOCK=false
   ```

4. **启动开发服务器**
   ```bash
   npm run dev
   ```
   现在可以通过 http://localhost:3000 访问前端应用。

5. **构建生产环境**
   ```bash
   npm run build
   ```
   构建完成后，生成的文件将位于`dist`目录中。

### 后端开发环境

1. **克隆仓库（如果尚未克隆）**
   ```bash
   git clone https://github.com/mangoDataAi/mango-Data.git
   cd mango-Data/mango-server
   ```

2. **配置数据库**
   - 创建MySQL数据库
   - 运行`sql/schema.sql`初始化数据库结构
   - 运行`sql/data.sql`导入初始数据

3. **配置应用属性**
   ```bash
   # 复制配置文件
   cp src/main/resources/application.example.yml src/main/resources/application.yml
   
   # 编辑application.yml，配置数据库连接等
   ```

4. **编译运行**
   ```bash
   # 编译项目
   mvn clean package -DskipTests
   
   # 运行应用
   java -jar target/mango-server.jar
   ```
   后端服务现在在 http://localhost:8080 上运行。

### 数据库设置

#### MySQL配置示例

1. **创建数据库**
   ```sql
   CREATE DATABASE mango4 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE mango_file CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. **配置application.yml**
   ```yaml
     datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/MANGO
    username: root
    password: 123456
   ```
## 项目结构

```
mango-data-platform/
├── docs/                          # 文档
│   ├── images/                    # 文档图片
│   ├── api/                       # API文档
│   └── guide/                     # 用户指南
├── mango-web/                     # 前端代码
│   ├── src/                       # 源代码
│   │   ├── assets/                # 静态资源
│   │   ├── components/            # 可复用组件
│   │   │   ├── common/            # 通用组件
│   │   │   ├── business/          # 业务组件
│   │   │   └── layout/            # 布局组件
│   │   ├── composables/           # 组合式函数
│   │   ├── router/                # 路由配置
│   │   │   ├── index.js           # 路由入口
│   │   │   └── modules/           # 模块路由
│   │   ├── store/                 # 状态管理
│   │   │   ├── index.js           # Store入口
│   │   │   └── modules/           # 模块Store
│   │   ├── views/                 # 页面组件
│   │   │   ├── dashboard/         # 仪表盘
│   │   │   ├── data-integration/  # 数据集成
│   │   │   ├── data-quality/      # 数据质量
│   │   │   └── ...                # 其他模块
│   │   ├── utils/                 # 工具函数
│   │   ├── api/                   # API调用
│   │   ├── styles/                # 全局样式
│   │   ├── App.vue                # 主应用组件
│   │   └── main.js                # 入口文件
│   ├── public/                    # 公共静态资源
│   ├── tests/                     # 测试文件
│   ├── package.json               # 依赖定义
│   └── vite.config.js             # Vite配置
├── mango-server/                  # 后端代码
│   ├── src/                       # 源代码
│   │   ├── main/                  # 主代码
│   │   │   ├── java/com/mango/    # Java代码
│   │   │   │   ├── common/        # 通用代码
│   │   │   │   ├── config/        # 配置类
│   │   │   │   ├── controller/    # 控制器
│   │   │   │   ├── entity/        # 实体类
│   │   │   │   ├── mapper/        # MyBatis映射
│   │   │   │   ├── service/       # 服务层
│   │   │   │   │   ├── impl/      # 服务实现
│   │   │   │   │   └── *.java     # 服务接口
│   │   │   │   └── utils/         # 工具类
│   │   │   └── resources/         # 资源文件
│   │   │       ├── mapper/        # MyBatis XML
│   │   │       ├── static/        # 静态资源
│   │   │       └── application.yml # 配置文件
│   │   └── test/                  # 测试代码
│   ├── pom.xml                    # Maven配置
│   └── sql/                       # SQL脚本
├── docker/                        # Docker配置
│   ├── docker-compose.yml         # Docker Compose
│   └── Dockerfile                 # Dockerfile
├── scripts/                       # 脚本
├── .gitignore                     # Git忽略文件
├── .editorconfig                  # 编辑器配置
├── README.md                      # 项目说明
├── CONTRIBUTING.md                # 贡献指南
├── LICENSE                        # 许可证
└── ACKNOWLEDGEMENTS.md            # 致谢名单
```

感谢您为芒果数据中台做出贡献！如有任何问题，请随时联系项目维护者或在GitHub上提出issue。 