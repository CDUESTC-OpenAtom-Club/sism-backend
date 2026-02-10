#!/bin/bash

# Script to update all Org references to SysOrg in Java files
# This script performs the migration from org table to sys_org table

echo "🔄 Starting Org to SysOrg migration in Java files..."

# Find all Java files that import Org
FILES=$(grep -rl "import com.sism.entity.Org;" src/main/java src/test/java 2>/dev/null)

if [ -z "$FILES" ]; then
    echo "✅ No files found with Org imports"
    exit 0
fi

echo "📝 Found $(echo "$FILES" | wc -l) files to update"

# Backup
echo "💾 Creating backup..."
tar -czf org-to-sysorg-backup-$(date +%Y%m%d_%H%M%S).tar.gz src/

# Replace imports
echo "🔧 Updating imports..."
for file in $FILES; do
    sed -i '' 's/import com\.sism\.entity\.Org;/import com.sism.entity.SysOrg;/g' "$file"
    sed -i '' 's/import com\.sism\.repository\.OrgRepository;/import com.sism.repository.SysOrgRepository;/g' "$file"
    sed -i '' 's/import com\.sism\.service\.OrgService;/import com.sism.service.SysOrgService;/g' "$file"
    sed -i '' 's/import com\.sism\.vo\.OrgVO;/import com.sism.vo.SysOrgVO;/g' "$file"
done

# Replace variable declarations and types
echo "🔧 Updating variable declarations..."
for file in $FILES; do
    # Replace type declarations
    sed -i '' 's/\bOrg org\b/SysOrg org/g' "$file"
    sed -i '' 's/\bOrg \([a-zA-Z][a-zA-Z0-9]*\)\b/SysOrg \1/g' "$file"
    sed -i '' 's/OrgRepository orgRepository/SysOrgRepository sysOrgRepository/g' "$file"
    sed -i '' 's/OrgService orgService/SysOrgService sysOrgService/g' "$file"
    sed -i '' 's/OrgVO orgVO/SysOrgVO orgVO/g' "$file"
    
    # Replace method calls
    sed -i '' 's/\.getOrgId()/\.getId()/g' "$file"
    sed -i '' 's/\.setOrgId(/\.setId(/g' "$file"
    sed -i '' 's/\.getOrgName()/\.getName()/g' "$file"
    sed -i '' 's/\.setOrgName(/\.setName(/g' "$file"
    sed -i '' 's/\.getOrgType()/\.getType()/g' "$file"
    sed -i '' 's/\.setOrgType(/\.setType(/g' "$file"
    
    # Replace JPA query paths
    sed -i '' 's/\.org\.orgId/.org.id/g' "$file"
    sed -i '' 's/\.org\.orgName/.org.name/g' "$file"
    sed -i '' 's/\.org\.orgType/.org.type/g' "$file"
    sed -i '' 's/Org_OrgId/Org_Id/g' "$file"
done

echo "✅ Migration completed!"
echo "📊 Updated files:"
echo "$FILES" | while read file; do
    echo "   - $file"
done

echo ""
echo "⚠️  Please review the changes and run tests:"
echo "   mvn clean test"
