#!/usr/bin/env node

/**
 * 执行 V1.6 迁移：为 indicator 表添加 owner_org_id 和 target_org_id
 */

const { Client } = require('pg');
const fs = require('fs');
const path = require('path');

const client = new Client({
  host: '175.24.139.148',
  port: 8386,
  database: 'strategic',
  user: 'muzimu',
  password: '64378561huaW'
});

async function runMigration() {
  try {
    await client.connect();
    console.log('✓ 已连接到数据库\n');

    // 读取迁移脚本
    const migrationPath = path.join(__dirname, '../migrations/V1.6__add_org_foreign_keys_to_indicator.sql');
    const sql = fs.readFileSync(migrationPath, 'utf8');

    console.log('开始执行 V1.6 迁移...\n');

    // 执行迁移（分步执行以便更好地处理错误）
    const steps = [
      {
        name: '添加新列',
        sql: `
          ALTER TABLE indicator
          ADD COLUMN IF NOT EXISTS owner_org_id BIGINT,
          ADD COLUMN IF NOT EXISTS target_org_id BIGINT;
        `
      },
      {
        name: '从 owner_dept 映射到 owner_org_id',
        sql: `
          UPDATE indicator i
          SET owner_org_id = so.id
          FROM sys_org so
          WHERE i.owner_dept IS NOT NULL
            AND i.owner_dept = so.name
            AND i.owner_org_id IS NULL;
        `
      },
      {
        name: '从 responsible_dept 映射到 target_org_id',
        sql: `
          UPDATE indicator i
          SET target_org_id = so.id
          FROM sys_org so
          WHERE i.responsible_dept IS NOT NULL
            AND i.responsible_dept = so.name
            AND i.target_org_id IS NULL;
        `
      },
      {
        name: '设置默认值',
        sql: `
          DO $$
          DECLARE
              default_org_id BIGINT;
          BEGIN
              SELECT id INTO default_org_id
              FROM sys_org
              WHERE type = 'FUNCTIONAL_DEPT'
              AND is_active = true
              ORDER BY sort_order
              LIMIT 1;

              IF default_org_id IS NOT NULL THEN
                  UPDATE indicator
                  SET owner_org_id = default_org_id
                  WHERE owner_org_id IS NULL;

                  UPDATE indicator
                  SET target_org_id = default_org_id
                  WHERE target_org_id IS NULL;
                  
                  RAISE NOTICE '已将 NULL 值设置为默认组织 ID: %', default_org_id;
              ELSE
                  RAISE EXCEPTION '无法找到默认组织（FUNCTIONAL_DEPT）';
              END IF;
          END $$;
        `
      },
      {
        name: '验证没有 NULL 值',
        sql: `
          SELECT 
            (SELECT COUNT(*) FROM indicator WHERE owner_org_id IS NULL) as null_owner,
            (SELECT COUNT(*) FROM indicator WHERE target_org_id IS NULL) as null_target;
        `
      },
      {
        name: '添加 NOT NULL 约束',
        sql: `
          ALTER TABLE indicator
          ALTER COLUMN owner_org_id SET NOT NULL,
          ALTER COLUMN target_org_id SET NOT NULL;
        `
      },
      {
        name: '添加外键约束',
        sql: `
          ALTER TABLE indicator
          DROP CONSTRAINT IF EXISTS fk_indicator_owner_org,
          DROP CONSTRAINT IF EXISTS fk_indicator_target_org;
          
          ALTER TABLE indicator
          ADD CONSTRAINT fk_indicator_owner_org
              FOREIGN KEY (owner_org_id)
              REFERENCES sys_org(id)
              ON DELETE RESTRICT,
          ADD CONSTRAINT fk_indicator_target_org
              FOREIGN KEY (target_org_id)
              REFERENCES sys_org(id)
              ON DELETE RESTRICT;
        `
      },
      {
        name: '添加索引',
        sql: `
          CREATE INDEX IF NOT EXISTS idx_indicator_owner_org ON indicator(owner_org_id);
          CREATE INDEX IF NOT EXISTS idx_indicator_target_org ON indicator(target_org_id);
        `
      },
      {
        name: '添加 level 列',
        sql: `
          ALTER TABLE indicator
          ADD COLUMN IF NOT EXISTS level VARCHAR(20);
        `
      },
      {
        name: '设置 level 默认值',
        sql: `
          UPDATE indicator
          SET level = CASE
              WHEN parent_indicator_id IS NULL THEN 'PRIMARY'
              ELSE 'SECONDARY'
          END
          WHERE level IS NULL;
        `
      },
      {
        name: '设置 level NOT NULL',
        sql: `
          ALTER TABLE indicator
          ALTER COLUMN level SET NOT NULL;
        `
      }
    ];

    for (const step of steps) {
      try {
        console.log(`执行: ${step.name}...`);
        const result = await client.query(step.sql);
        
        if (step.name === '验证没有 NULL 值' && result.rows.length > 0) {
          const { null_owner, null_target } = result.rows[0];
          if (parseInt(null_owner) > 0 || parseInt(null_target) > 0) {
            throw new Error(`仍有 NULL 值: owner_org_id=${null_owner}, target_org_id=${null_target}`);
          }
          console.log(`  ✓ 验证通过：无 NULL 值`);
        } else if (result.rowCount !== undefined && result.rowCount > 0) {
          console.log(`  ✓ 影响 ${result.rowCount} 行`);
        } else {
          console.log(`  ✓ 完成`);
        }
      } catch (error) {
        if (error.message.includes('already exists') || error.message.includes('does not exist')) {
          console.log(`  ⚠ 跳过（已存在或不存在）`);
        } else {
          throw error;
        }
      }
    }

    console.log('\n✓ V1.6 迁移完成');

  } catch (error) {
    console.error('\n✗ 迁移失败:', error.message);
    throw error;
  } finally {
    await client.end();
    console.log('✓ 数据库连接已关闭\n');
  }
}

// 执行迁移
runMigration()
  .then(() => {
    console.log('✓ 迁移成功完成');
    process.exit(0);
  })
  .catch(error => {
    console.error('✗ 迁移失败:', error);
    process.exit(1);
  });
