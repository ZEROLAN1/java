# CentOS服务器部署指南

本指南将帮助您在CentOS 7/8服务器上部署Java云存储应用。

---

## 📋 准备工作

### 服务器要求
- **操作系统**: CentOS 7 或 CentOS 8
- **内存**: 至少 2GB RAM
- **硬盘**: 至少 10GB 可用空间
- **网络**: 公网IP地址，开放80端口（HTTP）

### 需要安装的软件
- Docker
- Docker Compose
- Git

---

## 🚀 方法一：使用Docker部署（推荐）

### 步骤1: 连接到服务器

```bash
# 使用SSH连接到你的CentOS服务器
ssh root@你的服务器IP地址
# 输入密码
```

### 步骤2: 安装Docker

```bash
# 安装依赖
yum install -y yum-utils device-mapper-persistent-data lvm2

# 添加Docker仓库
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo

# 安装Docker
yum install -y docker-ce docker-ce-cli containerd.io

# 启动Docker服务
systemctl start docker
systemctl enable docker

# 验证Docker安装
docker --version
```

### 步骤3: 安装Docker Compose

```bash
# 下载Docker Compose
curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

# 添加执行权限
chmod +x /usr/local/bin/docker-compose

# 验证安装
docker-compose --version
```

### 步骤4: 安装Git

```bash
yum install -y git
git --version
```

### 步骤5: 上传项目到服务器

**方法A: 使用Git（推荐）**

```bash
# 进入工作目录
cd /opt

# 克隆项目（替换为你的仓库地址）
git clone https://github.com/你的用户名/java-cloud-storage.git

# 进入项目目录
cd java-cloud-storage
```

**方法B: 使用SCP上传**

在你的本地电脑上运行：
```bash
# 压缩项目
tar -czf java-cloud-storage.tar.gz java-cloud-storage/

# 上传到服务器
scp java-cloud-storage.tar.gz root@你的服务器IP:/opt/

# 在服务器上解压
ssh root@你的服务器IP
cd /opt
tar -xzf java-cloud-storage.tar.gz
cd java-cloud-storage
```

### 步骤6: 配置生产环境

```bash
# 编辑docker-compose.yml，修改敏感信息
vi docker-compose.yml

# 重要：修改以下内容
# 1. MySQL密码（第27行和第42行）
#    - SPRING_DATASOURCE_PASSWORD=修改为强密码
#    - MYSQL_ROOT_PASSWORD=修改为强密码
# 2. 前端端口（第9行，可选）
#    - "80:80"  # 改为80端口，生产环境更方便
```

示例修改：
```yaml
services:
  frontend:
    ports:
      - "80:80"  # 使用80端口
  
  backend:
    environment:
      - SPRING_DATASOURCE_PASSWORD=你的强密码  # 修改
  
  db:
    environment:
      - MYSQL_ROOT_PASSWORD=你的强密码  # 修改
```

### 步骤7: 启动服务

```bash
# 构建并启动所有服务
docker-compose up -d

# 查看启动状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 步骤8: 配置防火墙

```bash
# CentOS 7 使用 firewalld
systemctl start firewalld
systemctl enable firewalld

# 开放80端口
firewall-cmd --permanent --add-port=80/tcp
firewall-cmd --reload

# 查看开放的端口
firewall-cmd --list-ports
```

如果使用阿里云/腾讯云等云服务器，还需要在控制台的安全组中开放80端口。

### 步骤9: 访问应用

在浏览器中访问：
```
http://你的服务器IP
```

---

## 🔧 方法二：手动部署（不使用Docker）

### 步骤1: 安装Java 11

```bash
# 安装OpenJDK 11
yum install -y java-11-openjdk java-11-openjdk-devel

# 验证安装
java -version
```

### 步骤2: 安装MySQL 8.0

```bash
# 安装MySQL YUM仓库
rpm -Uvh https://dev.mysql.com/get/mysql80-community-release-el7-3.noarch.rpm

# 安装MySQL
yum install -y mysql-community-server

# 启动MySQL
systemctl start mysqld
systemctl enable mysqld

# 获取临时密码
grep 'temporary password' /var/log/mysqld.log

# 修改root密码
mysql -uroot -p
# 输入临时密码，然后执行：
ALTER USER 'root'@'localhost' IDENTIFIED BY '你的强密码';
CREATE DATABASE cloud_storage CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
exit;
```

### 步骤3: 安装Node.js和npm

```bash
# 安装Node.js 18
curl -fsSL https://rpm.nodesource.com/setup_18.x | bash -
yum install -y nodejs

# 验证安装
node -v
npm -v
```

### 步骤4: 安装Nginx

```bash
# 安装Nginx
yum install -y nginx

# 启动Nginx
systemctl start nginx
systemctl enable nginx
```

### 步骤5: 部署后端

```bash
# 进入项目目录
cd /opt/java-cloud-storage

# 修改配置文件
vi src/main/resources/application-prod.properties

# 确保数据库密码与MySQL中设置的一致

# 编译项目
./mvnw clean package -DskipTests

# 创建服务目录
mkdir -p /opt/cloud-storage/backend
cp target/cloud-storage-0.0.1-SNAPSHOT.jar /opt/cloud-storage/backend/app.jar
mkdir -p /opt/cloud-storage/uploads

# 创建systemd服务
cat > /etc/systemd/system/cloud-storage-backend.service << EOF
[Unit]
Description=Cloud Storage Backend Service
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/cloud-storage/backend
ExecStart=/usr/bin/java -jar /opt/cloud-storage/backend/app.jar --spring.profiles.active=prod
Environment="SPRING_DATASOURCE_PASSWORD=你的MySQL密码"
Environment="JWT_SECRET=$(openssl rand -base64 32)"
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# 启动后端服务
systemctl daemon-reload
systemctl start cloud-storage-backend
systemctl enable cloud-storage-backend

# 查看状态
systemctl status cloud-storage-backend
```

### 步骤6: 部署前端

```bash
# 进入前端目录
cd /opt/java-cloud-storage/frontend

# 安装依赖
npm install

# 构建生产版本
npm run build

# 复制到Nginx目录
cp -r dist/* /usr/share/nginx/html/

# 配置Nginx
cat > /etc/nginx/conf.d/cloud-storage.conf << 'EOF'
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    # 前端路由
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端API代理
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 文件上传大小限制
    client_max_body_size 10M;
}
EOF

# 测试Nginx配置
nginx -t

# 重启Nginx
systemctl restart nginx
```

### 步骤7: 配置防火墙

```bash
# 开放80端口
firewall-cmd --permanent --add-port=80/tcp
firewall-cmd --reload
```

### 步骤8: 访问应用

在浏览器中访问：
```
http://你的服务器IP
```

---

## 🔐 安全加固（可选但推荐）

### 1. 配置HTTPS证书

```bash
# 安装Certbot
yum install -y certbot python3-certbot-nginx

# 获取证书（需要先配置域名）
certbot --nginx -d 你的域名.com

# 自动续期
systemctl enable certbot-renew.timer
```

### 2. 修改默认密码

确保修改以下默认密码：
- MySQL root密码
- JWT密钥
- 应用管理员密码

### 3. 配置防火墙规则

```bash
# 只允许必要的端口
firewall-cmd --permanent --add-service=http
firewall-cmd --permanent --add-service=https
firewall-cmd --permanent --add-service=ssh
firewall-cmd --reload
```

### 4. 设置SELinux（CentOS默认启用）

```bash
# 查看SELinux状态
sestatus

# 如果需要配置SELinux策略
setsebool -P httpd_can_network_connect 1
```

---

## 📊 运维管理

### 查看日志

**Docker部署：**
```bash
# 查看所有日志
docker-compose logs -f

# 查看后端日志
docker-compose logs -f backend

# 查看数据库日志
docker-compose logs -f db
```

**手动部署：**
```bash
# 查看后端日志
journalctl -u cloud-storage-backend -f

# 查看Nginx日志
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log
```

### 重启服务

**Docker部署：**
```bash
# 重启所有服务
docker-compose restart

# 重启单个服务
docker-compose restart backend
```

**手动部署：**
```bash
# 重启后端
systemctl restart cloud-storage-backend

# 重启Nginx
systemctl restart nginx
```

### 备份数据库

**Docker部署：**
```bash
# 备份
docker exec cloud-storage-db mysqldump -uroot -p你的密码 cloud_storage > backup_$(date +%Y%m%d).sql

# 恢复
docker exec -i cloud-storage-db mysql -uroot -p你的密码 cloud_storage < backup_20240101.sql
```

**手动部署：**
```bash
# 备份
mysqldump -uroot -p你的密码 cloud_storage > backup_$(date +%Y%m%d).sql

# 恢复
mysql -uroot -p你的密码 cloud_storage < backup_20240101.sql
```

### 更新应用

**Docker部署：**
```bash
cd /opt/java-cloud-storage
git pull
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

**手动部署：**
```bash
cd /opt/java-cloud-storage
git pull

# 更新后端
./mvnw clean package -DskipTests
systemctl stop cloud-storage-backend
cp target/cloud-storage-0.0.1-SNAPSHOT.jar /opt/cloud-storage/backend/app.jar
systemctl start cloud-storage-backend

# 更新前端
cd frontend
npm install
npm run build
cp -r dist/* /usr/share/nginx/html/
systemctl restart nginx
```

---

## ❓ 常见问题

### 1. 无法访问应用

**检查服务状态：**
```bash
# Docker
docker-compose ps

# 手动部署
systemctl status cloud-storage-backend
systemctl status nginx
```

**检查防火墙：**
```bash
firewall-cmd --list-ports
```

**检查端口占用：**
```bash
netstat -tlnp | grep 80
netstat -tlnp | grep 8080
```

### 2. 数据库连接失败

**检查MySQL状态：**
```bash
# Docker
docker-compose logs db

# 手动部署
systemctl status mysqld
```

**测试连接：**
```bash
mysql -uroot -p -h localhost
```

### 3. 文件上传失败

**检查uploads目录权限：**
```bash
# Docker
docker exec cloud-storage-backend ls -la /app/uploads

# 手动部署
ls -la /opt/cloud-storage/uploads
chmod 755 /opt/cloud-storage/uploads
```

### 4. 内存不足

**查看内存使用：**
```bash
free -h
docker stats  # Docker部署
```

**优化Java内存：**
```bash
# 在启动命令中添加内存限制
java -Xmx512m -Xms256m -jar app.jar
```

---

## 📞 技术支持

如遇到问题，请：
1. 查看日志文件排查错误
2. 检查防火墙和安全组配置
3. 确认所有服务都正常运行
4. 参考本文档的常见问题部分

**祝您部署顺利！** 🎉
