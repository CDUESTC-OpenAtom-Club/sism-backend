/**
 * 为 2026 年度指标添加有意义的备注
 * 根据指标类型和战略任务分类编写
 */
import pg from 'pg';
const { Pool } = pg;

const pool = new Pool({
  host: process.env.DB_HOST || '175.24.139.148',
  port: parseInt(process.env.DB_PORT || '8386'),
  database: process.env.DB_NAME || 'strategic',
  user: process.env.DB_USER || 'postgres',
  password: process.env.DB_PASSWORD || '64378561huaW'
});

// 备注映射表：根据指标描述的关键词匹配备注
const remarkMappings = [
  // 产学研相关
  { pattern: '产学研基地建设数', remark: '统计本年度新建或升级的产学研合作基地数量，含校企联合实验室、实训中心等' },
  { pattern: '企业订单班开设数', remark: '统计与企业合作开设的订单培养班级数量，需签订正式合作协议' },
  
  // 国际化相关
  { pattern: '国际合作项目数量', remark: '统计与境外高校或机构签署的合作项目数，含交换生、联合培养等' },
  { pattern: '留学生招生人数', remark: '统计本年度招收的学历留学生和非学历留学生总人数' },
  { pattern: '本学院国际交流项目', remark: '统计学院层面开展的国际交流活动数量，含学术会议、师生互访等' },
  
  // 教学质量相关
  { pattern: '提升教学质量指标', remark: '综合评估教学质量提升情况，包括课程建设、教学改革、教学成果等' },
  { pattern: '本学院教学评估得分', remark: '汇总学院在校内教学评估中的综合得分，含督导评价、学生评教等' },
  { pattern: '本学院学生满意度', remark: '通过问卷调查统计学生对教学、管理、服务的综合满意度评分' },
  
  // 科研相关
  { pattern: '加强科研创新指标', remark: '综合评估科研创新能力提升情况，包括项目立项、成果转化、平台建设等' },
  { pattern: '本学院专利申请数', remark: '统计本学院师生申请的发明专利、实用新型、外观设计专利数量' },
  { pattern: '本学院科研项目数', remark: '统计学院获批的各级各类科研项目数量，含纵向和横向课题' },
  { pattern: '本学院论文发表数', remark: '统计学院师生发表的学术论文数量，按期刊级别分类统计' },
  { pattern: '科研项目立项数量', remark: '统计本年度获批立项的各级各类科研项目数量' },
  { pattern: '科研经费到账金额', remark: '统计本年度实际到账的科研经费总额，单位为万元' },
  
  // 师资相关
  { pattern: '本学院师资培训完成率', remark: '统计教师参加各类培训的完成比例，含教学能力、科研能力培训等' },
  { pattern: '教师培训覆盖率', remark: '统计参加培训的教师占专任教师总数的比例' },
  { pattern: '双师型教师比例', remark: '统计具有双师素质教师占专任教师总数的比例' },
  { pattern: '高层次人才引进数', remark: '统计引进的高层次人才数量，含国家级、省级人才计划入选者等' },
  
  // 就业相关
  { pattern: '本学院就业率指标', remark: '统计应届毕业生初次就业率，含升学、就业、创业等去向' },
  { pattern: '毕业生就业率', remark: '统计全校应届毕业生的综合就业率，按教育部口径计算' },
  
  // 校企合作相关
  { pattern: '本学院校企合作数', remark: '统计学院与企业签订的合作协议数量，含实习、就业、科研合作等' },
  { pattern: '本学院实践基地数量', remark: '统计学院签约的校外实习实训基地数量，需有正式协议且在有效期内' },
  { pattern: '实践教学基地建设数量', remark: '统计新建或升级的实践教学基地数量，含校内实训室、校外基地等' },
  
  // 课程改革相关
  { pattern: '本学院课程改革完成数', remark: '统计完成课程改革的课程数量，含课程内容更新、教学方法改革等' },
  { pattern: '完成课程改革项目数量', remark: '统计完成验收的课程改革项目数量，含精品课程、在线课程等' },
  
  // 信息化相关
  { pattern: '本学院信息化建设进度', remark: '评估学院信息化建设完成情况，含教学平台、管理系统、数据治理等' },
  { pattern: '数据治理完成度', remark: '评估数据标准化、数据质量、数据安全等治理工作的完成进度' },
  { pattern: '信息系统建设项目', remark: '统计完成建设或升级的信息系统数量，含教学、科研、管理系统等' },
  
  // 学术交流相关
  { pattern: '学术交流活动次数', remark: '统计组织或参与的学术会议、讲座、研讨会等活动次数' },
  
  // 制度建设相关
  { pattern: '制度建设完成率', remark: '统计年度制度建设任务的完成比例，含新建、修订、废止制度等' },
  { pattern: '管理流程优化项目', remark: '统计完成的管理流程优化项目数量，含审批流程简化、服务效率提升等' },
  
  // 研究生培养相关
  { pattern: '研究生培养质量指标', remark: '综合评估研究生培养质量，含学位论文质量、就业去向、学术成果等' }
];

async function updateRemarks() {
  const client = await pool.connect();
  
  try {
    console.log('开始更新指标备注...\n');
    
    let totalUpdated = 0;
    
    for (const mapping of remarkMappings) {
      const result = await client.query(
        `UPDATE indicator 
         SET remark = $1 
         WHERE indicator_desc LIKE $2 
         AND year = 2026 
         AND (remark IS NULL OR remark = '')`,
        [mapping.remark, `%${mapping.pattern}%`]
      );
      
      if (result.rowCount > 0) {
        console.log(`✓ "${mapping.pattern}" - 更新了 ${result.rowCount} 条记录`);
        totalUpdated += result.rowCount;
      }
    }
    
    console.log(`\n总计更新了 ${totalUpdated} 条指标备注`);
    
    // 验证结果
    const verifyResult = await client.query(
      `SELECT COUNT(*) as total, 
              COUNT(CASE WHEN remark IS NOT NULL AND remark != '' THEN 1 END) as with_remark
       FROM indicator WHERE year = 2026`
    );
    
    const { total, with_remark } = verifyResult.rows[0];
    console.log(`\n2026年度指标统计: 总计 ${total} 条, 有备注 ${with_remark} 条`);
    
  } catch (err) {
    console.error('更新失败:', err);
    throw err;
  } finally {
    client.release();
    await pool.end();
  }
}

updateRemarks().catch(console.error);
