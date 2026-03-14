/**
 * 组织机构同步阶段
 * 将前端定义的组织机构同步到数据库
 * 
 * Requirements: 1.1, 1.4
 */

// 前端定义的职能部门
const FUNCTIONAL_DEPARTMENTS = [
  '党委办公室 | 党委统战部',
  '纪委办公室 | 监察处',
  '党委宣传部 | 宣传策划部',
  '党委组织部 | 党委教师工作部',
  '人力资源部',
  '党委学工部 | 学生工作处',
  '党委保卫部 | 保卫处',
  '学校综合办公室',
  '教务处',
  '科技处',
  '财务部',
  '招生工作处',
  '就业创业指导中心',
  '实验室建设管理处',
  '数字校园建设办公室',
  '图书馆 | 档案馆',
  '后勤资产处',
  '继续教育部',
  '国际合作与交流处'
];

// 前端定义的二级学院
const COLLEGES = [
  '马克思主义学院',
  '工学院',
  '计算机学院',
  '商学院',
  '文理学院',
  '艺术与科技学院',
  '航空学院',
  '国际教育学院'
];

/**
 * 执行组织机构同步
 * @param {import('../sync-context.js').SyncContext} ctx - 同步上下文
 * @returns {Promise<import('../sync-context.js').PhaseResult>}
 */
export async function syncOrg(ctx) {
  const client = await ctx.getClient();
  
  try {
    console.log('正在同步组织机构...');
    
    // 1. 查询现有组织
    const existingOrgs = await client.query('SELECT org_id, org_name FROM org');
    const existingNames = new Set(existingOrgs.rows.map(r => r.org_name));
    
    // 建立现有组织的 ID 映射
    existingOrgs.rows.forEach(org => {
      ctx.maps.org.set(org.org_name, org.org_id);
    });
    
    console.log(`数据库现有组织: ${existingNames.size} 个`);
    
    // 2. 开始事务
    await client.query('BEGIN');
    
    // 3. 同步战略发展部
    if (!existingNames.has('战略发展部')) {
      const result = await client.query(
        'INSERT INTO org (org_name, org_type, sort_order) VALUES ($1, $2, $3) RETURNING org_id',
        ['战略发展部', 'STRATEGY_DEPT', 1]
      );
      ctx.maps.org.set('战略发展部', result.rows[0].org_id);
      ctx.recordInsert('org');
      console.log('✅ 新增: 战略发展部');
    } else {
      ctx.recordSkip('org');
    }
    
    // 4. 同步职能部门
    for (let i = 0; i < FUNCTIONAL_DEPARTMENTS.length; i++) {
      const dept = FUNCTIONAL_DEPARTMENTS[i];
      if (!existingNames.has(dept)) {
        const result = await client.query(
          'INSERT INTO org (org_name, org_type, sort_order) VALUES ($1, $2, $3) RETURNING org_id',
          [dept, 'FUNCTION_DEPT', 20 + i]
        );
        ctx.maps.org.set(dept, result.rows[0].org_id);
        ctx.recordInsert('org');
        console.log(`✅ 新增: ${dept}`);
      } else {
        ctx.recordSkip('org');
      }
    }
    
    // 5. 同步二级学院
    for (let i = 0; i < COLLEGES.length; i++) {
      const college = COLLEGES[i];
      if (!existingNames.has(college)) {
        const result = await client.query(
          'INSERT INTO org (org_name, org_type, sort_order) VALUES ($1, $2, $3) RETURNING org_id',
          [college, 'COLLEGE', 50 + i]
        );
        ctx.maps.org.set(college, result.rows[0].org_id);
        ctx.recordInsert('org');
        console.log(`✅ 新增: ${college}`);
      } else {
        ctx.recordSkip('org');
      }
    }
    
    // 6. 提交事务
    await client.query('COMMIT');
    
    // 7. 输出映射信息
    console.log(`\n组织机构 ID 映射已建立: ${ctx.maps.org.size} 个`);
    
    return {
      success: true,
      inserted: ctx.getPhaseStats('org').inserted,
      skipped: ctx.getPhaseStats('org').skipped
    };
    
  } catch (err) {
    await client.query('ROLLBACK');
    ctx.recordError('org');
    return {
      success: false,
      inserted: 0,
      skipped: 0,
      error: err
    };
  } finally {
    client.release();
  }
}

export default { syncOrg };
