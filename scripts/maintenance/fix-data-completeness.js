/**
 * 数据完整性修复脚本
 * 修复以下问题：
 * 1. milestone.target_progress 为0或null的记录
 * 2. indicator.responsible_person 为null的记录
 * 
 * 运行: node scripts/fix-data-completeness.js
 */

import pg from 'pg';
import dotenv from 'dotenv';

dotenv.config();

const { Pool } = pg;

const pool = new Pool({
  host: process.env.DB_HOST,
  port: parseInt(process.env.DB_PORT || '5432'),
  database: process.env.DB_NAME,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: false } : false
});

// 责任人映射表（根据部门类型和名称）
const responsiblePersonMap = {
  // 二级学院
  '计算机学院': '赵院长',
  '商学院': '钱院长',
  '工学院': '李院长',
  '文理学院': '吴院长',
  '艺术与科技学院': '孙院长',
  '航空学院': '周院长',
  '国际教育学院': '郑院长',
  '马克思主义学院': '王院长',
  
  // 职能部门
  '就业创业指导中心': '张主任',
  '招生工作处': '王主任',
  '教务处': '刘主任',
  '学校综合办公室': '陈主任',
  '人力资源部': '杨主任',
  '财务部': '黄主任',
  '科技处': '林主任',
  '后勤资产处': '何主任',
  '实验室建设管理处': '罗主任',
  '数字校园建设办公室': '郭主任',
  '国际合作与交流处': '高主任',
  '继续教育部': '梁主任',
  '图书馆 | 档案馆': '谢主任',
  '党委办公室 | 党委统战部': '韩主任',
  '党委保卫部 | 保卫处': '唐主任',
  '党委学生工作部 | 学生处': '冯主任',
  '党委宣传部': '董主任',
  '纪检监察室': '萧主任',
  '工会': '程主任',
  '团委': '曹主任',
  '质量管理与评估处': '袁主任',
  '发展规划处': '邓主任',
  
  // 战略发展部
  '战略发展部': '管理员'
};

async function fixMilestoneTargetProgress() {
  console.log('\n📊 修复里程碑目标进度 (target_progress)...');
  
  const client = await pool.connect();
  try {
    // 季度里程碑：Q1=25, Q2=50, Q3=75, Q4=100
    const quarterlyUpdate = await client.query(`
      UPDATE milestone 
      SET target_progress = CASE 
        WHEN milestone_name = 'Q1季度目标' THEN 25
        WHEN milestone_name = 'Q2季度目标' THEN 50
        WHEN milestone_name = 'Q3季度目标' THEN 75
        WHEN milestone_name = 'Q4季度目标' THEN 100
        ELSE target_progress
      END
      WHERE milestone_name IN ('Q1季度目标', 'Q2季度目标', 'Q3季度目标', 'Q4季度目标')
        AND (target_progress = 0 OR target_progress IS NULL)
    `);
    console.log(`  ✅ 季度里程碑更新: ${quarterlyUpdate.rowCount} 条`);

    // 月度里程碑：按月份累计进度
    // 假设从5月开始，每月约8.33%，累计到年底100%
    const monthlyUpdate = await client.query(`
      UPDATE milestone 
      SET target_progress = CASE 
        WHEN milestone_name = '5月目标' THEN 42
        WHEN milestone_name = '6月目标' THEN 50
        WHEN milestone_name = '7月目标' THEN 58
        WHEN milestone_name = '8月目标' THEN 67
        WHEN milestone_name = '9月目标' THEN 75
        WHEN milestone_name = '10月目标' THEN 83
        WHEN milestone_name = '11月目标' THEN 92
        WHEN milestone_name = '12月目标' THEN 100
        ELSE target_progress
      END
      WHERE milestone_name LIKE '%月目标'
        AND (target_progress = 0 OR target_progress IS NULL)
    `);
    console.log(`  ✅ 月度里程碑更新: ${monthlyUpdate.rowCount} 条`);

    // 其他里程碑：根据 sort_order 和 weight_percent 计算累计进度
    const otherUpdate = await client.query(`
      UPDATE milestone m
      SET target_progress = (
        SELECT LEAST(100, ROUND(SUM(m2.weight_percent)::numeric))::integer
        FROM milestone m2 
        WHERE m2.indicator_id = m.indicator_id 
          AND m2.sort_order <= m.sort_order
      )
      WHERE (target_progress = 0 OR target_progress IS NULL)
        AND milestone_name NOT IN ('Q1季度目标', 'Q2季度目标', 'Q3季度目标', 'Q4季度目标')
        AND milestone_name NOT LIKE '%月目标'
    `);
    console.log(`  ✅ 其他里程碑更新: ${otherUpdate.rowCount} 条`);

  } finally {
    client.release();
  }
}

async function fixIndicatorResponsiblePerson() {
  console.log('\n👤 修复指标责任人 (responsible_person)...');
  
  const client = await pool.connect();
  try {
    // 获取所有需要更新的指标及其目标部门
    const indicators = await client.query(`
      SELECT i.indicator_id, o.org_name, o.org_type
      FROM indicator i
      JOIN org o ON i.target_org_id = o.org_id
      WHERE i.responsible_person IS NULL OR i.responsible_person = ''
    `);

    console.log(`  📋 需要更新的指标数: ${indicators.rows.length}`);

    let updatedCount = 0;
    for (const row of indicators.rows) {
      const responsiblePerson = responsiblePersonMap[row.org_name] || 
        (row.org_type === 'COLLEGE' ? '院长' : '主任');
      
      await client.query(
        'UPDATE indicator SET responsible_person = $1 WHERE indicator_id = $2',
        [responsiblePerson, row.indicator_id]
      );
      updatedCount++;
    }

    console.log(`  ✅ 责任人更新完成: ${updatedCount} 条`);

  } finally {
    client.release();
  }
}

async function verifyFixes() {
  console.log('\n🔍 验证修复结果...');
  
  const client = await pool.connect();
  try {
    // 验证里程碑
    const milestoneCheck = await client.query(`
      SELECT COUNT(*) as total,
             COUNT(CASE WHEN target_progress > 0 THEN 1 END) as has_progress,
             COUNT(CASE WHEN target_progress = 0 OR target_progress IS NULL THEN 1 END) as zero_progress
      FROM milestone
    `);
    const m = milestoneCheck.rows[0];
    console.log(`  📊 里程碑: 总数=${m.total}, 有进度=${m.has_progress}, 无进度=${m.zero_progress}`);

    // 验证指标
    const indicatorCheck = await client.query(`
      SELECT COUNT(*) as total,
             COUNT(CASE WHEN responsible_person IS NOT NULL AND responsible_person != '' THEN 1 END) as has_person,
             COUNT(CASE WHEN responsible_person IS NULL OR responsible_person = '' THEN 1 END) as no_person
      FROM indicator
    `);
    const i = indicatorCheck.rows[0];
    console.log(`  👤 指标: 总数=${i.total}, 有责任人=${i.has_person}, 无责任人=${i.no_person}`);

    // 显示样本数据
    console.log('\n📋 修复后数据样本:');
    
    const milestoneSample = await client.query(`
      SELECT milestone_name, target_progress, status 
      FROM milestone 
      WHERE milestone_name LIKE 'Q%' 
      ORDER BY sort_order 
      LIMIT 4
    `);
    console.log('  里程碑样本:');
    milestoneSample.rows.forEach(r => {
      console.log(`    - ${r.milestone_name}: target_progress=${r.target_progress}, status=${r.status}`);
    });

    const indicatorSample = await client.query(`
      SELECT i.indicator_desc, i.responsible_person, o.org_name
      FROM indicator i
      JOIN org o ON i.target_org_id = o.org_id
      WHERE i.year = 2026
      LIMIT 5
    `);
    console.log('  指标样本:');
    indicatorSample.rows.forEach(r => {
      console.log(`    - ${r.indicator_desc.substring(0, 30)}... -> ${r.responsible_person} (${r.org_name})`);
    });

  } finally {
    client.release();
  }
}

async function main() {
  console.log('🚀 数据完整性修复脚本启动');
  console.log('=' .repeat(50));
  
  try {
    await fixMilestoneTargetProgress();
    await fixIndicatorResponsiblePerson();
    await verifyFixes();
    
    console.log('\n' + '=' .repeat(50));
    console.log('✅ 数据修复完成!');
    
  } catch (error) {
    console.error('❌ 修复失败:', error);
    process.exit(1);
  } finally {
    await pool.end();
  }
}

main();
