#!/bin/bash
# ============================================
# Auto Setup Sudoers (with password)
# ============================================
# Purpose: Configure sudoers using user's password
# Usage: echo "your-password" | ./auto-setup-sudoers.sh <username>
# ============================================

set -e

DEPLOY_USER="${1}"

if [ -z "$DEPLOY_USER" ]; then
    echo "Usage: echo 'password' | $0 <deploy-username>"
    exit 1
fi

# Read password from stdin
read -r PASSWORD

# Create sudoers configuration
SUDOERS_CONTENT="# SISM Backend Deployment - Passwordless sudo
$DEPLOY_USER ALL=(ALL) NOPASSWD: /usr/bin/systemctl restart sism-backend
$DEPLOY_USER ALL=(ALL) NOPASSWD: /usr/bin/systemctl status sism-backend
$DEPLOY_USER ALL=(ALL) NOPASSWD: /usr/bin/systemctl is-active sism-backend
$DEPLOY_USER ALL=(ALL) NOPASSWD: /usr/bin/systemctl stop sism-backend
$DEPLOY_USER ALL=(ALL) NOPASSWD: /usr/bin/systemctl start sism-backend
$DEPLOY_USER ALL=(ALL) NOPASSWD: /usr/bin/journalctl -u sism-backend*
$DEPLOY_USER ALL=(ALL) NOPASSWD: /usr/bin/ln -sf /opt/sism/backend/* /opt/sism/backend/sism-backend.jar"

# Use sudo with password from stdin
echo "$PASSWORD" | sudo -S bash -c "
    echo '$SUDOERS_CONTENT' > /etc/sudoers.d/sism-deploy
    chmod 0440 /etc/sudoers.d/sism-deploy
    visudo -c -f /etc/sudoers.d/sism-deploy
"

if [ $? -eq 0 ]; then
    echo "✅ Sudoers configured successfully"
    
    # Setup directory permissions
    echo "$PASSWORD" | sudo -S bash -c "
        mkdir -p /opt/sism/backend
        groupadd -f sism
        usermod -aG sism $DEPLOY_USER
        chown -R root:sism /opt/sism
        chmod -R 775 /opt/sism/backend
    "
    
    echo "✅ Directory permissions configured"
    echo "✅ Setup complete!"
else
    echo "❌ Failed to configure sudoers"
    exit 1
fi
