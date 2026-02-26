#!/bin/bash

# SISM Database Check Script
# This script analyzes the database and generates a report

echo "============================================"
echo "SISM Database Analysis Tool"
echo "============================================"
echo ""

# Database connection details
DB_HOST="175.24.139.148"
DB_PORT="8386"
DB_NAME="strategic"
DB_USER="postgres"
DB_PASS="64378561huaW"

echo "Connecting to database: $DB_HOST:$DB_PORT/$DB_NAME"
echo ""

# Check if psql is available
if command -v psql &> /dev/null; then
    echo "Using psql client..."

    # Run analysis script
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << 'EOF'
-- Table counts
SELECT
    'TABLE_COUNTS' AS check_type,
    table_name,
    (SELECT COUNT(*) FROM information_schema.columns WHERE table_name = t.table_name AND table_schema = 'public') as column_count
FROM information_schema.tables t
WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- Record counts
DO $$
DECLARE
    table_rec RECORD;
    total_records BIGINT := 0;
BEGIN
    RAISE NOTICE '=== 表记录统计 ===';
    FOR table_rec IN
        SELECT table_name FROM information_schema.tables
        WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
        ORDER BY table_name
    LOOP
        EXECUTE format('SELECT COUNT(*) as count FROM %I', table_rec.table_name) INTO total_records;
        RAISE NOTICE '%-30s %,6d 条记录', table_rec.table_name, total_records;
    END LOOP;
END $$;
    EOF
else
    echo "psql not found. Using HTTP API instead..."
    echo ""
    echo "Starting application in background..."

    # Start application
    ./mvnw spring-boot:run > /tmp/sism-app.log 2>&1 &
    APP_PID=$!

    echo "Waiting for application to start (30 seconds)..."
    sleep 30

    # Check if application is running
    if curl -s http://localhost:8080/actuator/health > /dev/null; then
        echo "Application is running!"
        echo ""

        # Get database report
        echo "Fetching database report..."
        curl -s http://localhost:8080/admin/database/report | python3 -m json.tool

        echo ""
        echo "Shutting down application..."
        kill $APP_PID 2>/dev/null
    else
        echo "Failed to start application. Check /tmp/sism-app.log for details."
        kill $APP_PID 2>/dev/null
    fi
fi

echo ""
echo "============================================"
echo "Analysis complete!"
echo "============================================"