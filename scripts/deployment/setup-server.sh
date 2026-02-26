#!/bin/bash
# ============================================
# Setup Server for SISM Deployment
# ============================================
# Purpose: One-time server configuration
# Usage: sudo ./setup-server.sh <deploy-username>
# ============================================

set -e

if [ "$EUID" -ne 0 ]; then
    echo "❌ This script must be run as root"
    echo "Usage: sudo $0 <deploy-username>"
    exit 1
fi

DEPLOY_USER="${1}"

if [ -z "$DEPLOY_USER" ]; then
    echo "Usage: sudo $0 <deploy-username>"
    echo ""
    echo "Example: sudo $0 github-deploy"
    exit 1
fi

if ! id "$DEPLOY_USER" &>/dev/null; then
    echo "❌ User '$DEPLOY_USER' does not exist"
    echo ""
    echo "Create user first:"
    echo "  sudo useradd -m -s /bin/bash $DEPLOY_USER"
    exit 1
fi

echo "🔧 SISM Backend - Server Setup"
echo "==============================="
echo "User: $DEPLOY_USER"
echo ""

# 1. Configure sudoers
echo "📝 Step 1: Configuring sudoers..."
SUDOERS_FILE="/etc/sudoers.d/sism-deploy"

cat > "$SUDOERS_FILE" << EOL
# SISM Backend Deployment - Passwordless sudo
# Created: $(date)
# User: $DEPLOY_USER

$DEPLOY_USER ALL=(ALL) NOPASSWD: /usr/bin/systemctl restart sism-backend
$DEPLOY_USER ALL=(ALL) NOPASSWD: /usr/bin/systemctl status sism-backend
$DEPLOY_USER ALL=(ALL) NOPASSWD: /usr/bin/systemctl is-active sism-backend
$DEPLOY_USER ALL=(ALL) NOPASSWD: /usr/bin/systemctl stop sism-backend
$DEPLOY_USER ALL=(ALL) NOPASSWD: /usr/bin/systemctl start sism-backend
$DEPLOY_USER ALL=(ALL) NOPASSWD: /usr/bin/journalctl -u sism-backend*
$DEPLOY_USER ALL=(ALL) NOPASSWD: /usr/bin/ln -sf /opt/sism/backend/* /opt/sism/backend/sism-backend.jar
EOL

chmod 0440 "$SUDOERS_FILE"

if visudo -c -f "$SUDOERS_FILE" >/dev/null 2>&1; then
    echo "✅ Sudoers configured: $SUDOERS_FILE"
else
    echo "❌ Sudoers syntax error! Removing file..."
    rm -f "$SUDOERS_FILE"
    exit 1
fi

# 2. Set directory permissions
echo ""
echo "📝 Step 2: Setting directory permissions..."
SISM_HOME="/opt/sism"

if [ ! -d "$SISM_HOME" ]; then
    mkdir -p "$SISM_HOME/backend"
fi

if ! getent group sism >/dev/null; then
    groupadd sism
    echo "✅ Created group: sism"
fi

usermod -aG sism "$DEPLOY_USER"
echo "✅ Added $DEPLOY_USER to sism group"

chown -R root:sism "$SISM_HOME"
chmod -R 775 "$SISM_HOME/backend"
echo "✅ Directory permissions set: $SISM_HOME"

# 3. Test configuration
echo ""
echo "📝 Step 3: Testing configuration..."

if sudo -u "$DEPLOY_USER" sudo -n systemctl status sism-backend >/dev/null 2>&1; then
    echo "✅ Sudo access works"
else
    echo "⚠️  Sudo test inconclusive (service may not exist yet)"
fi

if sudo -u "$DEPLOY_USER" touch "$SISM_HOME/backend/.test" 2>/dev/null; then
    rm -f "$SISM_HOME/backend/.test"
    echo "✅ Directory write access works"
else
    echo "❌ Directory write access failed"
    exit 1
fi

# 4. Summary
echo ""
echo "==============================="
echo "✅ Server setup complete!"
echo "==============================="
echo ""
echo "Configuration:"
echo "  • User: $DEPLOY_USER"
echo "  • Group: sism"
echo "  • Sudoers: $SUDOERS_FILE"
echo "  • Deploy directory: $SISM_HOME/backend"
echo ""
echo "Next steps:"
echo "  1. Configure SSH key for $DEPLOY_USER"
echo "  2. Test deployment: ./deploy.sh"
echo "  3. Monitor logs: sudo journalctl -u sism-backend -f"
echo ""
