# 云存储系统 - 前后端分离版本

一个现代化的云存储系统，采用前后端分离架构开发。

## 技术栈

### 后端
- **Spring Boot 2.7.5** - Java Web 框架
- **Spring Security + JWT** - 身份认证和授权
- **Spring Data JPA** - 数据持久化
- **MySQL 8.0** - 关系型数据库
- **Maven** - 项目构建工具

### 前端
- **React 18** - 前端框架
- **Vite** - 构建工具
- **TailwindCSS** - CSS 框架
- **Axios** - HTTP 客户端
- **React Router** - 路由管理
- **Lucide React** - 图标库

### 部署
- **Docker & Docker Compose** - 容器化部署
- **Nginx** - 前端静态文件服务器和反向代理

## 功能特性

- ✅ 用户注册与登录（JWT 认证）
- ✅ 文件上传（支持最大 10MB）
- ✅ 文件下载
- ✅ 文件删除
- ✅ 文件列表展示
- ✅ 响应式设计
- ✅ 现代化 UI 界面

## 本地开发

### 前置要求

- JDK 11+
- Node.js 18+
- Maven 3.6+
- MySQL 8.0+

### 后端开发

1. **配置数据库**

创建 MySQL 数据库：
```sql
CREATE DATABASE cloud_storage;
```

2. **修改配置文件**

编辑 `src/main/resources/application.properties`：
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/cloud_storage
spring.datasource.username=your_username
spring.datasource.password=your_password
```

3. **运行后端**

```bash
# 使用 Maven
mvn spring-boot:run

# 或者使用 IDE 直接运行 CloudStorageApplication.java
```

后端将在 http://localhost:8080 运行

### 前端开发

1. **安装依赖**

```bash
cd frontend
npm install
```

2. **启动开发服务器**

```bash
npm run dev
```

前端将在 http://localhost:3000 运行

## Docker 部署

### 方式一：使用 Docker Compose（推荐）

这是最简单的部署方式，会自动启动前端、后端和数据库三个容器。

1. **确保已安装 Docker 和 Docker Compose**

2. **启动所有服务**

```bash
# 构建并启动所有容器
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down

# 完全清理（包括数据）
docker-compose down -v
```

3. **访问应用**

打开浏览器访问：http://localhost

### 方式二：分别构建容器

#### 后端容器

```bash
# 构建镜像
docker build -t cloud-storage-backend .

# 运行容器（需要先启动 MySQL）
docker run -d \
  --name cloud-storage-backend \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/cloud_storage \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=password \
  -v $(pwd)/uploads:/app/uploads \
  cloud-storage-backend
```

#### 前端容器

```bash
# 进入前端目录
cd frontend

# 构建镜像
docker build -t cloud-storage-frontend .

# 运行容器
docker run -d \
  --name cloud-storage-frontend \
  -p 80:80 \
  cloud-storage-frontend
```

## 生产环境部署

### 1. 云服务器部署（推荐）

适用于阿里云、腾讯云、AWS 等云服务器。

#### 准备工作

- 一台 Linux 服务器（Ubuntu 20.04+ 或 CentOS 7+）
- 至少 2GB 内存
- 安装 Docker 和 Docker Compose

#### 部署步骤

1. **安装 Docker**

```bash
# Ubuntu
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 安装 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

2. **上传项目到服务器**

```bash
# 使用 git
git clone <your-repo-url>
cd java-cloud-storage

# 或使用 scp
scp -r . user@server:/path/to/deployment
```

3. **修改配置（可选）**

编辑 `docker-compose.yml`，修改数据库密码等敏感信息：

```yaml
environment:
  - MYSQL_ROOT_PASSWORD=your_strong_password
```

4. **启动服务**

```bash
docker-compose up -d
```

5. **配置防火墙**

```bash
# 开放 80 端口（HTTP）
sudo ufw allow 80/tcp

# 如果需要 HTTPS
sudo ufw allow 443/tcp
```

6. **访问应用**

在浏览器中输入：`http://你的服务器IP`

### 2. 配置 HTTPS（推荐）

使用 Let's Encrypt 免费证书：

1. **安装 Certbot**

```bash
sudo apt-get update
sudo apt-get install certbot
```

2. **获取证书**

```bash
sudo certbot certonly --standalone -d yourdomain.com
```

3. **修改 Nginx 配置**

编辑 `frontend/nginx.conf`，添加 SSL 配置：

```nginx
server {
    listen 443 ssl;
    server_name yourdomain.com;
    
    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;
    
    # 其他配置...
}

server {
    listen 80;
    server_name yourdomain.com;
    return 301 https://$server_name$request_uri;
}
```

4. **更新 docker-compose.yml**

```yaml
frontend:
  volumes:
    - /etc/letsencrypt:/etc/letsencrypt:ro
  ports:
    - "80:80"
    - "443:443"
```

### 3. 域名配置

1. 在域名服务商处添加 A 记录，指向服务器 IP
2. 等待 DNS 解析生效（通常 10-30 分钟）
3. 使用域名访问应用

## 系统架构

```
┌─────────────┐
│   浏览器     │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Nginx     │  (前端静态文件 + API 反向代理)
│   :80       │
└──────┬──────┘
       │
       ├──────────────┐
       │              │
       ▼              ▼
┌─────────────┐  ┌─────────────┐
│   React     │  │ Spring Boot │
│   前端      │  │   后端 API  │
└─────────────┘  │   :8080     │
                 └──────┬──────┘
                        │
                        ▼
                 ┌─────────────┐
                 │   MySQL     │
                 │   :3306     │
                 └─────────────┘
```

## API 接口文档

### 认证接口

#### 用户注册
- **POST** `/api/auth/register`
- 请求体：
```json
{
  "username": "string",
  "email": "string",
  "password": "string"
}
```

#### 用户登录
- **POST** `/api/auth/login`
- 请求体：
```json
{
  "username": "string",
  "password": "string"
}
```

### 文件接口

所有文件接口都需要在 Header 中携带 JWT Token：
```
Authorization: Bearer <token>
```

#### 获取文件列表
- **GET** `/api/files`

#### 上传文件
- **POST** `/api/files/upload`
- Content-Type: `multipart/form-data`
- 参数：`file` (文件)

#### 下载文件
- **GET** `/api/files/download/{id}`

#### 删除文件
- **DELETE** `/api/files/{id}`

## 常见问题

### 1. 端口被占用

如果 80 或 8080 端口被占用，可以修改 `docker-compose.yml` 中的端口映射：

```yaml
ports:
  - "8080:80"  # 将前端映射到 8080 端口
```

### 2. 数据库连接失败

检查 MySQL 容器是否正常启动：
```bash
docker-compose logs db
```

### 3. 文件上传失败

确保 uploads 目录有写入权限：
```bash
chmod 777 uploads
```

### 4. 前端无法连接后端

检查 Nginx 配置中的代理设置，确保 `backend` 主机名正确。

## 项目结构

```
java-cloud-storage/
├── src/                          # 后端源码
│   ├── main/
│   │   ├── java/
│   │   │   └── com/cloudstorage/
│   │   │       ├── config/       # 配置类（JWT、Security）
│   │   │       ├── controller/   # REST 控制器
│   │   │       ├── dto/          # 数据传输对象
│   │   │       ├── model/        # 实体类
│   │   │       ├── repository/   # 数据访问层
│   │   │       └── service/      # 业务逻辑层
│   │   └── resources/
│   │       └── application.properties
├── frontend/                     # 前端源码
│   ├── src/
│   │   ├── pages/               # 页面组件
│   │   ├── context/             # React Context
│   │   ├── App.jsx              # 主应用
│   │   └── main.jsx             # 入口文件
│   ├── Dockerfile               # 前端容器配置
│   └── nginx.conf               # Nginx 配置
├── pom.xml                      # Maven 配置
├── Dockerfile                   # 后端容器配置
├── docker-compose.yml           # Docker Compose 配置
└── README.md                    # 项目文档
```

## 开发团队

如有问题，请提交 Issue 或 Pull Request。

## 许可证

MIT License
