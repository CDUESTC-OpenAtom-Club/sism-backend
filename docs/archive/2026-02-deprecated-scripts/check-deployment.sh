#!/bin/bash
# ============================================
# SISM Backend Deployment Check Script
# ============================================
# Purpose: Verify deployment without requiring sudo
# Usage: ./check-deployment.sh [jar-name]
# ============================================

set -e

# Configuration
SISM_HOME="/opt/sism"
JAR_NAME="${1:-sism-backend-1.0.0.jar}"
HEALTH_URL="http://localhost:8080/api/actuator/health"
MAX_RETRIES=10
RETRY_DELAY=10

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

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# Check if JAR file exists
log_info "📦 Checking JAR file..."
if [ -f "$SISM_HOME/backend/$JAR_NAME" ]; then
    JAR_SIZE=$(stat -c%s "$SISM_HOME/backend/$JAR_NAME" 2>/dev/null || stat -f%z "$SISM_HOME/backend/$JAR_NAME")
    log_success "✅ JAR file exists: $JAR_SIZE bytes"
else
    log_error "❌ JAR file not found: $SISM_HOME/backend/$JAR_NAME"
    exit 1
fi

# Wait a bit for service to potentially restart
log_info "⏳ Waiting for service to stabilize (30 seconds)..."
sleep 30

# Health check with retries
log_info "🏥 Performing health checks..."
for i in $(seq 1 $MAX_RETRIES); do
    if curl -sf "$HEALTH_URL" > /dev/null 2>&1; then
        log_success "✅ Backend is healthy!"
        
        # Get health details
        HEALTH_STATUS=$(curl -s "$HEALTH_URL" 2>/dev/null || echo '{"status":"UNKNOWN"}')
        echo "Health Status: $HEALTH_STATUS"
        
        # Test Cycles API
        log_info "🧪 Testing Cycles API..."
        CYCLES_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/cycles -H "Authorization: Bearer test" 2>/dev/null || echo "000")
        if [ "$CYCLES_RESPONSE" = "401" ] || [ "$CYCLES_RESPONSE" = "403" ]; then
            log_success "✅ Cycles API endpoint is accessible (auth required: $CYCLES_RESPONSE)"
        elif [ "$CYCLES_RESPONSE" = "200" ]; then
            log_success "✅ Cycles API endpoint is accessible"
        else
            log_warn "⚠️  Cycles API returned: $CYCLES_RESPONSE"
        fi
        
        exit 0
    fi
    log_info "⏳ Health check retry $i/$MAX_RETRIES..."
    sleep "$RETRY_DELAY"
done

log_error "❌ Health check failed after $MAX_RETRIES attempts"
log_warn "⚠️  This script cannot restart the service (requires sudo)"
log_info "📋 To manually restart the service, run:"
echo "    sudo systemctl restart sism-backend"
echo "    sudo journalctl -u sism-backend -f"

exit 1
