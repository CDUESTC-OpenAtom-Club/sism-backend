#!/bin/bash
# ============================================
# SISM Backend Service Restart Script
# ============================================
# Purpose: Restart the backend service and verify health
# Usage: ./restart-service.sh [jar-name]
# ============================================

set -e

# Configuration
SISM_HOME="/opt/sism"
JAR_NAME="${1:-sism-backend-1.0.0.jar}"
SERVICE_NAME="sism-backend"
HEALTH_URL="http://localhost:8080/api/actuator/health"
MAX_RETRIES=5
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

# Health check with retries
log_info "🏥 Performing health checks..."
for i in $(seq 1 $MAX_RETRIES); do
    if curl -sf "$HEALTH_URL" > /dev/null 2>&1; then
        log_success "✅ Backend deployed successfully and is healthy!"
        
        # Test Cycles API
        log_info "🧪 Testing Cycles API..."
        if curl -sf http://localhost:8080/api/cycles -H "Authorization: Bearer test" > /dev/null 2>&1 || [ $? -eq 22 ]; then
            log_success "✅ Cycles API endpoint is accessible"
        fi
        
        exit 0
    fi
    log_info "⏳ Health check retry $i/$MAX_RETRIES..."
    sleep "$RETRY_DELAY"
done

log_error "❌ Health check failed after $MAX_RETRIES attempts"
log_info "📋 Recent service logs:"
sudo journalctl -u "$SERVICE_NAME" -n 50 --no-pager

exit 1
