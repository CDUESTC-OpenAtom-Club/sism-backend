#!/bin/bash
# ============================================================================
# 生成OpenAPI文档脚本
# ============================================================================

set -e

echo "========================================="
echo "OpenAPI文档生成工具"
echo "========================================="
echo ""

# 配置
SERVER_URL="http://localhost:8080/api"
OUTPUT_DIR="docs/openapi"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# 创建输出目录
mkdir -p "$OUTPUT_DIR"

echo "📍 步骤1: 检查服务状态..."
if curl -s "$SERVER_URL/v3/api-docs" > /dev/null 2>&1; then
    echo "✓ 服务正在运行: $SERVER_URL"
else
    echo "✗ 服务未运行，尝试启动..."
    echo "请运行: mvn spring-boot:run -pl sism-main"
    echo "或者: java -jar sism-main/target/sism-main-1.0.0.jar"
    exit 1
fi

echo ""
echo "📍 步骤2: 生成OpenAPI JSON文档..."
curl -s "$SERVER_URL/v3/api-docs" | jq '.' > "$OUTPUT_DIR/openapi-$TIMESTAMP.json"
echo "✓ 已生成: $OUTPUT_DIR/openapi-$TIMESTAMP.json"

echo ""
echo "📍 步骤3: 生成OpenAPI YAML文档..."
if command -v yq &> /dev/null; then
    yq eval -P "$OUTPUT_DIR/openapi-$TIMESTAMP.json" > "$OUTPUT_DIR/openapi-$TIMESTAMP.yaml"
    echo "✓ 已生成: $OUTPUT_DIR/openapi-$TIMESTAMP.yaml"
else
    echo "⚠️  未安装yq工具，跳过YAML生成"
    echo "   安装: brew install yq"
fi

echo ""
echo "📍 步骤4: 创建最新版本链接..."
ln -sf "openapi-$TIMESTAMP.json" "$OUTPUT_DIR/openapi-latest.json"
echo "✓ 已创建: $OUTPUT_DIR/openapi-latest.json"

echo ""
echo "📍 步骤5: 生成接口端点清单..."
echo "# API端点清单" > "$OUTPUT_DIR/api-endpoints.md"
echo "" >> "$OUTPUT_DIR/api-endpoints.md"
echo "生成时间: $(date '+%Y-%m-%d %H:%M:%S')" >> "$OUTPUT_DIR/api-endpoints.md"
echo "" >> "$OUTPUT_DIR/api-endpoints.md"
echo "## 所有接口" >> "$OUTPUT_DIR/api-endpoints.md"
echo "" >> "$OUTPUT_DIR/api-endpoints.md"
jq -r '.paths | to_entries[] | "\(.key): \(.value | keys | join(", "))"' "$OUTPUT_DIR/openapi-latest.json" >> "$OUTPUT_DIR/api-endpoints.md"
echo "✓ 已生成: $OUTPUT_DIR/api-endpoints.md"

echo ""
echo "========================================="
echo "生成完成！"
echo "========================================="
echo ""
echo "文档位置:"
echo "  JSON: $OUTPUT_DIR/openapi-$TIMESTAMP.json"
echo "  YAML: $OUTPUT_DIR/openapi-$TIMESTAMP.yaml"
echo "  最新: $OUTPUT_DIR/openapi-latest.json"
echo ""
echo "Swagger UI: $SERVER_URL/swagger-ui/index.html"
echo "在线文档: $SERVER_URL/v3/api-docs"
echo ""

# 显示统计信息
echo "📊 接口统计:"
ENDPOINTS=$(jq '.paths | length' "$OUTPUT_DIR/openapi-latest.json")
TAGS=$(jq '.[] | .tags | length' "$OUTPUT_DIR/openapi-latest.json" 2>/dev/null | wc -l | tr -d ' ')
echo "  - 总路径数: $ENDPOINTS"
echo "  - 控制器数: $(jq '.tags | length' "$OUTPUT_DIR/openapi-latest.json")"
echo ""
