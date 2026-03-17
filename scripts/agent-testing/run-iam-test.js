const fs = require('fs');
const path = require('path');

// 加载配置
const config = JSON.parse(fs.readFileSync('./config/test-users.json', 'utf8'));

// 加载Agent
const IamAgent = require('./agents/iam-agent');

// 创建并执行Agent
async function runTest() {
  console.log('\n========================================');
  console.log('🧪 IAM认证Agent 测试');
  console.log('========================================\n');

  const agent = new IamAgent(config);

  try {
    const result = await agent.execute();
    console.log('\n✅ 测试完成');
    console.log('结果:', JSON.stringify(result.results, null, 2));
  } catch (error) {
    console.error('\n❌ 测试失败:', error.message);
    console.error(error.stack);
    process.exit(1);
  }
}

runTest();
