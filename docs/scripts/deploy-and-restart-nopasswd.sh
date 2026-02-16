#!/bin/bash
# ============================================
# SISM Backend Deploy and Restart Script (No Password)
# ============================================
# Purpose: Deploy JAR and restart service without password prompt
# Requires: sudoers configured for NOPASSWD access
# Usage: ./deploy-and-restart-nopasswd.sh [jar-name]
# ============================================

set -e

# Configuration
SISM_HOME="/opt/sism"
JAR_NAME="${1:-sism-backend-1.0.0.jar}"
SERVICE_NAME="sism-backend"
HEALTH_URL="http://localhost:8080/api/actuator/health"
MAX_RETRIES=10
RETRY_DELAY=10
STARTUP_WAIT=30

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Update symlink
log_info "🔗 Updating symlink..."
sudo ln -sf "$SISM_HOME/backend/$JAR_NAME" "$SISM_HOME/backend/sism-backend.jar"

# Restart service
log_info "🔄 Restarting $SERVICE_NAME service..."
sudo systemctl restart "$SERVICE_NAME"

# Wait for startup
log_info "⏳ Waiting for service to start ($STARTUP_WAIT seconds)..."
sleep "$STARTUP_WAIT"

# Check service status
log_info "🔍 Checking service status..."
SERVICE_STATUS=$(sudo systemctl is-active "$SERVICE_NAME")
log_info "Service status: $SERVICE_STATUS"

if [ "$SERVICE_STATUS" = "activating" ] || [ "$SERVICE_STATUS" = "active" ]; then
    log_info "✅ Service is $SERVICE_STATUS"
else
    log_error "❌ Service status is: $SERVICE_STATUS"
    log_info "📋 Service status details:"
    sudo systemctl status "$SERVICE_NAME" --no-pager
    log_info "📋 Recent service logs:"
    sudo journalctl -u "$SERVICE_NAME" -n 100 --no-pager
    exit 1
fi

# Health check with retries
log_info "🏥 Performing health checks..."
for i in $(seq 1 $MAX_RETRIES); do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_URL" 2>/dev/null || echo "000")
    
    if [ "$HTTP_CODE" = "200" ]; then
        log_success "✅ Backend deployed successfully and is healthy!"
        
        # Get health details
        HEALTH_STATUS=$(curl -s "$HEALTH_URL" 2>/dev/null || echo '{"status":"UNKNOWN"}')
        echo "Health Status: $HEALTH_STATUS"
        
        # Test API endpoint
        log_info "🧪 Testing API endpoint..."
        API_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/cycles -H "Authorization: Bearer test" 2>/dev/null || echo "000")
        if [ "$API_RESPONSE" = "401" ] || [ "$API_RESPONSE" = "403" ]; then
            log_success "✅ API endpoint is accessible (auth required: $API_RESPONSE)"
        elif [ "$API_RESPONSE" = "200" ]; then
            log_success "✅ API endpoint is accessible"
        else
            echo "API response: $API_RESPONSE"
        fi
        
        exit 0
    fi
    
    log_info "⏳ Health check retry $i/$MAX_RETRIES (HTTP $HTTP_CODE)..."
    sleep "$RETRY_DELAY"
done

log_error "❌ Health check failed after $MAX_RETRIES attempts"
log_info "📋 Service status:"
sudo systemctl status "$SERVICE_NAME" --no-pager
log_info "📋 Recent service logs:"
sudo journalctl -u "$SERVICE_NAME" -n 100 --no-pager

exit 1
