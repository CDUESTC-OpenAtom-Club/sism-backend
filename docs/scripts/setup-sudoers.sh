#!/bin/bash
# ============================================
# Setup Sudoers for SISM Deployment
# ============================================
# Purpose: Configure sudoers to allow deployment without password
# Usage: sudo ./setup-sudoers.sh <deploy-username>
# ============================================

set -e

if [ "$EUID" -ne 0 ]; then
    echo "❌ This script must be run as root"
    echo "Usage: sudo $0 <deploy-username>"
    exit 1
fi

DEPLOY_USER="${1}"

if [ -z "$DEPLOY_USER" ]; then
    echo "❌ Please provide the deployment username"
    echo "Usage: sudo $0 <deploy-username>"
    exit 1
fi

# Check if user exists
if ! id "$DEPLOY_USER" &>/dev/null; then
    echo "❌ User '$DEPLOY_USER' does not exist"
    exit 1
fi

echo "🔧 Setting up sudoers for user: $DEPLOY_USER"

# Create sudoers configuration
SUDOERS_FILE="/etc/sudoers.d/sism-deploy"

cat > "$SUDOERS_FILE" << EOL
# SISM Backend Deployment Configuration
# Created: $(date)
# User: $DEPLOY_USER
#
# Allow specific systemctl and journalctl commands without password
# for automated deployment from GitHub Actions

$DEPLOY_USER ALL=(ALL) NOPASSWD: /usr/bin/systemctl restart sism-backend
$DEPLOY_USER ALL=(ALL) NOPASSWD: /usr/bin/systemctl status sism-backend
$DEPLOY_USER ALL=(ALL) NOPASSWD: /usr/bin/systemctl is-active sism-backend
$DEPLOY_USER ALL=(ALL) NOPASSWD: /usr/bin/systemctl is-enabled sism-backend
$DEPLOY_USER ALL=(ALL) NOPASSWD: /usr/bin/journalctl -u sism-backend*
$DEPLOY_USER ALL=(ALL) NOPASSWD: /usr/bin/ln -sf /opt/sism/backend/* /opt/sism/backend/sism-backend.jar
EOL

# Set correct permissions
chmod 0440 "$SUDOERS_FILE"

echo "✅ Sudoers file created: $SUDOERS_FILE"

# Validate sudoers syntax
if visudo -c -f "$SUDOERS_FILE"; then
    echo "✅ Sudoers syntax is valid"
else
    echo "❌ Sudoers syntax error! Removing file..."
    rm -f "$SUDOERS_FILE"
    exit 1
fi

# Test sudo access
echo ""
echo "🧪 Testing sudo access for user $DEPLOY_USER..."
echo "Run this command as $DEPLOY_USER to test:"
echo "  sudo systemctl status sism-backend"
echo ""
echo "It should NOT ask for a password."
echo ""
echo "✅ Setup complete!"
