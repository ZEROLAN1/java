# 部署指南

本文档详细介绍如何将云存储系统部署到生产环境。

## 快速开始

### 最简单的部署方式（5分钟内完成）

1. **准备服务器**
   - 任何支持 Docker 的 Linux 服务器
   - 至少 2GB 内存，10GB 硬盘空间

2. **安装 Docker**
   ```bash
   curl -fsSL https://get.docker.com -o get-docker.sh
   sudo sh get-docker.sh
   sudo usermod -aG docker $USER
   ```

3. **安装 Docker Compose**
   ```bash
   sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
   sudo chmod +x /usr/local/bin/docker-compose
   ```

4. **克隆项目**
   ```bash
   git clone <your-repository>
   cd java-cloud-storage
   ```

5. **启动服务**
   ```bash
   docker-compose up -d
   ```

6. **访问应用**
   - 打开浏览器，访问：`http://服务器IP`

## 详细部署步骤

### 一、阿里云/腾讯云部署

#### 1. 购买服务器

推荐配置：
- CPU: 2核
- 内存: 4GB
- 硬盘: 40GB
- 带宽: 5Mbps
- 操作系统: Ubuntu 20.04 LTS

#### 2. 安全组配置

开放以下端口：
- 22 (SSH)
- 80 (HTTP)
- 443 (HTTPS，可选)

#### 3. 连接服务器

```bash
ssh root@你的服务器IP
```

#### 4. 安装必要软件

```bash
# 更新系统
apt-get update
apt-get upgrade -y

# 安装 Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

# 安装 Docker Compose
curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# 验证安装
docker --version
docker-compose --version
```

#### 5. 部署应用

```bash
# 克隆项目（或使用 scp 上传）
git clone <your-repository>
cd java-cloud-storage

# 修改数据库密码（重要！）
nano docker-compose.yml
# 修改 MYSQL_ROOT_PASSWORD 为强密码

# 启动服务
docker-compose up -d

# 查看启动状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

#### 6. 配置防火墙

```bash
# Ubuntu UFW
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable
```

### 二、配置域名和 HTTPS

#### 1. 域名解析

在域名服务商处配置 A 记录：
```
类型    主机记录    记录值
A       @          你的服务器IP
A       www        你的服务器IP
```

#### 2. 安装 SSL 证书（Let's Encrypt）

```bash
# 安装 Certbot
apt-get install certbot -y

# 停止前端容器
docker-compose stop frontend

# 获取证书
certbot certonly --standalone -d yourdomain.com -d www.yourdomain.com

# 重启前端容器
docker-compose start frontend
```

#### 3. 配置 Nginx SSL

编辑 `frontend/nginx.conf`：

```nginx
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name yourdomain.com www.yourdomain.com;
    
    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;
    
    # SSL 优化
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

#### 4. 更新 Docker Compose

修改 `docker-compose.yml`：

```yaml
frontend:
  build:
    context: ./frontend
    dockerfile: Dockerfile
  container_name: cloud-storage-frontend
  ports:
    - "80:80"
    - "443:443"
  volumes:
    - /etc/letsencrypt:/etc/letsencrypt:ro
  depends_on:
    - backend
  restart: always
```

#### 5. 重新构建和启动

```bash
docker-compose down
docker-compose build --no-cache frontend
docker-compose up -d
```

#### 6. 自动续期证书

```bash
# 添加定时任务
crontab -e

# 添加以下行（每月1号凌晨3点自动续期）
0 3 1 * * certbot renew --quiet && docker-compose restart frontend
```

### 三、生产环境优化

#### 1. 修改默认密码

编辑 `docker-compose.yml`：

```yaml
environment:
  - MYSQL_ROOT_PASSWORD=your_strong_password_here
```

编辑 `src/main/resources/application.properties`：

```properties
jwt.secret=your_very_long_and_random_secret_key_here
```

重新构建：
```bash
docker-compose down
docker-compose build --no-cache backend
docker-compose up -d
```

#### 2. 配置文件存储限制

编辑 `src/main/resources/application.properties`：

```properties
# 根据需求调整
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
```

#### 3. 数据库备份

创建备份脚本 `backup.sh`：

```bash
#!/bin/bash
BACKUP_DIR="/backup/mysql"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

docker exec cloud-storage-db mysqldump \
  -uroot -ppassword \
  cloud_storage > $BACKUP_DIR/backup_$DATE.sql

# 保留最近7天的备份
find $BACKUP_DIR -name "backup_*.sql" -mtime +7 -delete
```

设置定时备份：
```bash
chmod +x backup.sh

# 每天凌晨2点自动备份
crontab -e
0 2 * * * /path/to/backup.sh
```

#### 4. 配置日志

创建 `docker-compose.override.yml`：

```yaml
version: '3.8'
services:
  backend:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
  
  frontend:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

#### 5. 资源限制

修改 `docker-compose.yml`：

```yaml
backend:
  deploy:
    resources:
      limits:
        cpus: '1'
        memory: 1G
      reservations:
        cpus: '0.5'
        memory: 512M

frontend:
  deploy:
    resources:
      limits:
        cpus: '0.5'
        memory: 256M
```

### 四、监控和维护

#### 1. 查看容器状态

```bash
# 查看运行状态
docker-compose ps

# 查看实时日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f backend

# 查看资源使用
docker stats
```

#### 2. 重启服务

```bash
# 重启所有服务
docker-compose restart

# 重启特定服务
docker-compose restart backend

# 完全重新部署
docker-compose down
docker-compose up -d
```

#### 3. 更新应用

```bash
# 拉取最新代码
git pull

# 重新构建并启动
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

#### 4. 清理磁盘空间

```bash
# 清理未使用的镜像
docker image prune -a

# 清理未使用的容器
docker container prune

# 清理未使用的卷
docker volume prune

# 一键清理所有
docker system prune -a --volumes
```

### 五、故障排查

#### 问题 1: 容器无法启动

```bash
# 查看详细日志
docker-compose logs

# 检查端口占用
netstat -tulpn | grep 80
netstat -tulpn | grep 8080

# 重新构建
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

#### 问题 2: 数据库连接失败

```bash
# 进入数据库容器
docker exec -it cloud-storage-db mysql -uroot -p

# 检查数据库是否存在
SHOW DATABASES;

# 如果数据库不存在，创建它
CREATE DATABASE cloud_storage;
```

#### 问题 3: 前端无法访问后端 API

```bash
# 检查后端是否正常运行
curl http://localhost:8080/api/auth/login

# 检查 Nginx 配置
docker exec cloud-storage-frontend cat /etc/nginx/conf.d/default.conf

# 重启前端容器
docker-compose restart frontend
```

#### 问题 4: 文件上传失败

```bash
# 检查上传目录权限
ls -la uploads/

# 修复权限
chmod 777 uploads/

# 检查磁盘空间
df -h
```

### 六、安全建议

1. **修改默认密码**
   - 数据库密码
   - JWT 密钥

2. **启用防火墙**
   ```bash
   ufw enable
   ufw allow 22/tcp
   ufw allow 80/tcp
   ufw allow 443/tcp
   ```

3. **定期更新系统**
   ```bash
   apt-get update
   apt-get upgrade -y
   ```

4. **配置 fail2ban**
   ```bash
   apt-get install fail2ban -y
   systemctl enable fail2ban
   systemctl start fail2ban
   ```

5. **限制文件上传大小**
   - 在 application.properties 中配置
   - 在 Nginx 中配置

6. **定期备份数据**
   - 数据库备份
   - 上传文件备份

## 总结

按照本指南操作，您应该能够成功将云存储系统部署到生产环境。如有问题，请查看日志或提交 Issue。
