#!/bin/bash

# Script to rename AppUser to SysUser in all Java files
# Date: 2026-02-10

echo "🔄 Renaming AppUser to SysUser in all Java files..."
echo ""

# Find all Java files containing AppUser and replace with SysUser
find src -name "*.java" -type f -exec sed -i '' 's/AppUser/SysUser/g' {} \;

echo "✅ Replacement complete!"
echo ""
echo "📊 Verification:"

# Count files that still contain AppUser (should be 0)
REMAINING=$(find src -name "*.java" -type f -exec grep -l "AppUser" {} \; | wc -l | tr -d ' ')
echo "  - Files still containing 'AppUser': $REMAINING"

# Count files now containing SysUser
UPDATED=$(find src -name "*.java" -type f -exec grep -l "SysUser" {} \; | wc -l | tr -d ' ')
echo "  - Files now containing 'SysUser': $UPDATED"

echo ""
echo "🗑️  Removing old AppUser.java file..."
rm -f src/main/java/com/sism/entity/AppUser.java

if [ ! -f src/main/java/com/sism/entity/AppUser.java ]; then
    echo "✅ AppUser.java removed successfully"
else
    echo "❌ Failed to remove AppUser.java"
fi

echo ""
echo "✅ Migration complete!"
