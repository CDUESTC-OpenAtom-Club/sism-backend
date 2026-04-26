#!/bin/bash
# ============================================
# SISM Backend Deploy Script (Universal)
# ============================================
# Purpose: Universal deployment script with auto-detection
# Usage: ./deploy.sh [jar-name]
# ============================================

set -e

# Configuration
SISM_HOME="/opt/sism"
JAR_NAME="${1:-sism-backend-1.0.0.jar}"
SERVICE_NAME="sism-backend"
HEALTH_URL="http://localhost:8080/api/v1/actuator/health"
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

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Detect sudo capability
check_sudo() {
    # Check if running in CI environment
    if [ -n "$CI" ] || [ -n "$GITHUB_ACTIONS" ]; then
        # In CI, only try passwordless sudo
        if sudo -n true 2>/dev/null; then
            echo "passwordless"
        else
            echo "none"
        fi
    else
        # In interactive environment, can try password sudo
        if sudo -n true 2>/dev/null; then
            echo "passwordless"
        elif sudo -v 2>/dev/null; then
            echo "password"
        else
            echo "none"
        fi
    fi
}

# Update symlink
update_symlink() {
    log_info "🔗 Updating symlink..."
    
    local SUDO_MODE=$(check_sudo)
    
    if [ "$SUDO_MODE" = "passwordless" ]; then
        sudo ln -sf "$SISM_HOME/backend/$JAR_NAME" "$SISM_HOME/backend/sism-backend.jar"
        log_success "Symlink updated (with sudo)"
    elif ln -sf "$SISM_HOME/backend/$JAR_NAME" "$SISM_HOME/backend/sism-backend.jar" 2>/dev/null; then
        log_success "Symlink updated (no sudo needed)"
    else
        log_error "Failed to update symlink"
        log_info "Try: sudo ln -sf $SISM_HOME/backend/$JAR_NAME $SISM_HOME/backend/sism-backend.jar"
        return 1
    fi
}

# Restart service
restart_service() {
    log_info "🔄 Restarting $SERVICE_NAME service..."
    
    local SUDO_MODE=$(check_sudo)
    
    # Try passwordless sudo first
    if [ "$SUDO_MODE" = "passwordless" ]; then
        sudo systemctl restart "$SERVICE_NAME"
        log_success "Service restarted (with sudo)"
        return 0
    fi
    
    # Try systemctl --user
    if systemctl --user restart "$SERVICE_NAME" 2>/dev/null; then
        log_success "Service restarted (systemctl --user)"
        return 0
    fi
    
    # In CI environment, don't try password sudo
    if [ -n "$CI" ] || [ -n "$GITHUB_ACTIONS" ]; then
        log_error "Cannot restart service in CI without passwordless sudo"
        log_warning "⚠️  Server configuration required:"
        log_warning "    1. SSH to server: ssh user@server"
        log_warning "    2. Run: sudo ./setup-server.sh <deploy-username>"
        log_warning "    3. Re-run this deployment"
        return 1
    fi
    
    # Try with password (interactive only)
    if [ "$SUDO_MODE" = "password" ]; then
        sudo systemctl restart "$SERVICE_NAME"
        log_success "Service restarted (with password)"
        return 0
    fi
    
    # Fallback: create restart signal
    log_warning "Cannot restart service directly"
    local RESTART_SIGNAL="$SISM_HOME/backend/.restart-signal"
    if touch "$RESTART_SIGNAL" 2>/dev/null; then
        log_info "Created restart signal: $RESTART_SIGNAL"
        log_warning "Service should restart automatically"
        return 0
    else
        log_error "Cannot restart service"
        log_info "Manual restart required: sudo systemctl restart $SERVICE_NAME"
        return 1
    fi
}

# Check service status
check_service_status() {
    log_info "🔍 Checking service status..."
    
    local SUDO_MODE=$(check_sudo)
    
    if [ "$SUDO_MODE" = "passwordless" ]; then
        SERVICE_STATUS=$(sudo systemctl is-active "$SERVICE_NAME")
        log_info "Service status: $SERVICE_STATUS"
    elif systemctl --user is-active "$SERVICE_NAME" >/dev/null 2>&1; then
        SERVICE_STATUS=$(systemctl --user is-active "$SERVICE_NAME")
        log_info "Service status: $SERVICE_STATUS"
    else
        log_warning "Cannot check service status (will verify via health check)"
    fi
}

# Health check
health_check() {
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
                log_info "API response: $API_RESPONSE"
            fi
            
            return 0
        fi
        
        log_info "⏳ Health check retry $i/$MAX_RETRIES (HTTP $HTTP_CODE)..."
        sleep "$RETRY_DELAY"
    done
    
    log_error "❌ Health check failed after $MAX_RETRIES attempts"
    return 1
}

# Main deployment flow
main() {
    log_info "🚀 Starting SISM Backend Deployment"
    log_info "JAR: $JAR_NAME"
    echo ""
    
    # Update symlink
    if ! update_symlink; then
        exit 1
    fi
    
    # Restart service
    if ! restart_service; then
        exit 1
    fi
    
    # Wait for startup
    log_info "⏳ Waiting for service to start ($STARTUP_WAIT seconds)..."
    sleep "$STARTUP_WAIT"
    
    # Check service status
    check_service_status
    
    # Health check
    if health_check; then
        log_success "🎉 Deployment completed successfully!"
        exit 0
    else
        log_error "❌ Deployment failed"
        log_info "Check logs: sudo journalctl -u $SERVICE_NAME -n 100"
        exit 1
    fi
}

# Run main
main
