#!/bin/bash
# ============================================
# SISM 快速配置脚本 - blackevil.cn
# 服务器 IP: 175.24.139.148
# ============================================

set -e

echo "=========================================="
echo "SISM 服务器快速配置"
echo "域名: blackevil.cn"
echo "=========================================="

# 1. 创建部署用户
echo "[1/6] 创建部署用户..."
if ! id "deploy" &>/dev/null; then
    sudo useradd -m -s /bin/bash deploy
    echo "✅ 用户 deploy 创建成功"
else
    echo "ℹ️  用户 deploy 已存在"
fi

# 2. 配置 sudo 权限
echo "[2/6] 配置 sudo 权限..."
sudo tee /etc/sudoers.d/deploy << 'EOF'
deploy ALL=(ALL) NOPASSWD: /bin/systemctl restart sism-backend
deploy ALL=(ALL) NOPASSWD: /bin/systemctl status sism-backend *
deploy ALL=(ALL) NOPASSWD: /bin/systemctl reload nginx
deploy ALL=(ALL) NOPASSWD: /usr/sbin/nginx -t
deploy ALL=(ALL) NOPASSWD: /bin/chown -R www-data\:www-data /var/www/sism
deploy ALL=(ALL) NOPASSWD: /bin/ln -sf /opt/sism/backend/sism-backend-1.0.0.jar /opt/sism/backend/sism-backend.jar
EOF
sudo chmod 440 /etc/sudoers.d/deploy
echo "✅ sudo 权限配置完成"

# 3. 创建目录结构
echo "[3/6] 创建目录结构..."
sudo mkdir -p /opt/sism/backend
sudo mkdir -p /opt/sism/scripts
sudo mkdir -p /var/www/sism
sudo mkdir -p /var/log/sism

# 设置权限
sudo chown -R deploy:deploy /opt/sism/backend
sudo chown -R deploy:deploy /var/www/sism
sudo chown -R sism:sism /var/log/sism 2>/dev/null || sudo chown -R deploy:deploy /var/log/sism
echo "✅ 目录创建完成"

# 4. 配置 SSH
echo "[4/6] 配置 SSH..."
sudo mkdir -p /home/deploy/.ssh
sudo chmod 700 /home/deploy/.ssh
sudo touch /home/deploy/.ssh/authorized_keys
sudo chmod 600 /home/deploy/.ssh/authorized_keys
sudo chown -R deploy:deploy /home/deploy/.ssh
echo "✅ SSH 目录配置完成"
echo ""
echo "⚠️  请手动添加 GitHub Actions 的公钥到:"
echo "   /home/deploy/.ssh/authorized_keys"
echo ""

# 5. 创建后端环境配置
echo "[5/6] 创建后端环境配置..."
if [[ ! -f /opt/sism/backend/.env ]]; then
    sudo tee /opt/sism/backend/.env << 'EOF'
# 数据库配置
DB_HOST=175.24.139.148
DB_PORT=8386
DB_NAME=strategic
DB_USERNAME=postgres
DB_PASSWORD=64378561huaW

# JWT 配置
JWT_SECRET=sism-secret-key-change-in-production-environment-must-be-at-least-256-bits

# 服务器配置
SERVER_PORT=8080
LOG_PATH=/var/log/sism

# CORS 配置
ALLOWED_ORIGINS=https://blackevil.cn

# Swagger 配置
SWAGGER_ENABLED=false
EOF
    sudo chmod 600 /opt/sism/backend/.env
    sudo chown deploy:deploy /opt/sism/backend/.env
    echo "✅ 环境配置创建完成"
else
    echo "ℹ️  环境配置已存在"
fi

# 6. 安装 Systemd 服务
echo "[6/6] 安装 Systemd 服务..."
sudo tee /etc/systemd/system/sism-backend.service << 'EOF'
[Unit]
Description=SISM Backend Service
After=network.target

[Service]
Type=simple
User=deploy
Group=deploy
WorkingDirectory=/opt/sism/backend
EnvironmentFile=/opt/sism/backend/.env
ExecStart=/usr/bin/java -Xms512m -Xmx2g -jar /opt/sism/backend/sism-backend.jar --spring.profiles.active=prod
ExecStop=/bin/kill -TERM $MAINPID
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=sism-backend

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable sism-backend
echo "✅ Systemd 服务安装完成"

echo ""
echo "=========================================="
echo "✅ 基础配置完成！"
echo "=========================================="
echo ""
echo "下一步操作:"
echo ""
echo "1. 添加 SSH 公钥到服务器:"
echo "   echo '你的公钥' >> /home/deploy/.ssh/authorized_keys"
echo ""
echo "2. 配置 Nginx (复制配置文件):"
echo "   sudo cp docs/nginx/sism.conf /etc/nginx/sites-available/sism"
echo "   sudo ln -s /etc/nginx/sites-available/sism /etc/nginx/sites-enabled/"
echo "   sudo nginx -t && sudo systemctl reload nginx"
echo ""
echo "3. 申请 SSL 证书:"
echo "   sudo certbot --nginx -d blackevil.cn"
echo ""
echo "4. 在 GitHub 添加 Secrets:"
echo "   SERVER_HOST = 175.24.139.148"
echo "   SERVER_USER = deploy"
echo "   SERVER_SSH_KEY = (你的私钥内容)"
echo ""
