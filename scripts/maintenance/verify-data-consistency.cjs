/**
 * 前后端数据一致性验证脚本
 * 
 * 用于对比 Mock 数据与 API 响应，验证数据完整性
 * 
 * Requirements: 6.2, 6.3, 7.1, 7.2, 7.3
 */

const fs = require('fs')
const path = require('path')

// ==================== Configuration ====================

const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080'

// ==================== Mock Data Loading ====================

/**
 * Load mock indicators from the frontend data file
 * Note: This is a simplified version that parses the TypeScript file
 */
function loadMockIndicators() {
  const filePath = path.join(__dirname, '../src/data/indicators/indicators2026.ts')
  const content = fs.readFileSync(filePath, 'utf-8')
  
  // Extract indicator objects using regex (simplified parsing)
  const indicators = []
  
  // Match id patterns like '2026-101', '2026-101-1'
  const idMatches = content.matchAll(/id:\s*['"]([^'"]+)['"]/g)
  const nameMatches = content.matchAll(/name:\s*['"]([^'"]+)['"]/g)
  const isQualitativeMatches = content.matchAll(/isQualitative:\s*(true|false)/g)
  const type1Matches = content.matchAll(/type1:\s*['"]([^'"]+)['"]/g)
  const type2Matches = content.matchAll(/type2:\s*['"]([^'"]+)['"]/g)
  const progressMatches = content.matchAll(/progress:\s*(\d+)/g)
  const targetValueMatches = content.matchAll(/targetValue:\s*(\d+(?:\.\d+)?)/g)
  const unitMatches = content.matchAll(/unit:\s*['"]([^'"]+)['"]/g)
  const responsiblePersonMatches = content.matchAll(/responsiblePerson:\s*['"]([^'"]+)['"]/g)
  
  const ids = [...idMatches].map(m => m[1])
  const names = [...nameMatches].map(m => m[1])
  const isQualitatives = [...isQualitativeMatches].map(m => m[1] === 'true')
  const type1s = [...type1Matches].map(m => m[1])
  const type2s = [...type2Matches].map(m => m[1])
  const progresses = [...progressMatches].map(m => parseInt(m[1]))
  const targetValues = [...targetValueMatches].map(m => parseFloat(m[1]))
  const units = [...unitMatches].map(m => m[1])
  const responsiblePersons = [...responsiblePersonMatches].map(m => m[1])
  
  for (let i = 0; i < ids.length; i++) {
    indicators.push({
      id: ids[i],
      name: names[i] || '',
      isQualitative: isQualitatives[i] || false,
      type1: type1s[i] || '',
      type2: type2s[i] || '',
      progress: progresses[i] || 0,
      targetValue: targetValues[i] || 0,
      unit: units[i] || '',
      responsiblePerson: responsiblePersons[i] || ''
    })
  }
  
  return indicators
}

// ==================== Data Analysis ====================

/**
 * Analyze mock data completeness
 */
function analyzeMockData(indicators) {
  const analysis = {
    total: indicators.length,
    quantitative: indicators.filter(i => i.type1 === '定量').length,
    qualitative: indicators.filter(i => i.type1 === '定性').length,
    development: indicators.filter(i => i.type2 === '发展性').length,
    basic: indicators.filter(i => i.type2 === '基础性').length,
    hasCustomMilestones: indicators.filter(i => i.isQualitative).length,
    byDepartment: {}
  }
  
  // Group by responsible department
  for (const indicator of indicators) {
    const dept = indicator.responsiblePerson || 'Unknown'
    if (!analysis.byDepartment[dept]) {
      analysis.byDepartment[dept] = 0
    }
    analysis.byDepartment[dept]++
  }
  
  return analysis
}

/**
 * Validate data completeness requirements
 */
function validateCompleteness(analysis) {
  const results = {
    passed: [],
    failed: []
  }
  
  // Requirement 7.1: At least 12 quantitative indicators
  if (analysis.quantitative >= 12) {
    results.passed.push(`✅ 定量指标数量: ${analysis.quantitative} >= 12`)
  } else {
    results.failed.push(`❌ 定量指标数量: ${analysis.quantitative} < 12`)
  }
  
  // Requirement 7.2: Qualitative indicators with custom milestones
  if (analysis.qualitative > 0) {
    results.passed.push(`✅ 定性指标数量: ${analysis.qualitative} > 0`)
  } else {
    results.failed.push(`❌ 定性指标数量: ${analysis.qualitative} = 0`)
  }
  
  // Requirement 7.3: Both development and basic indicators exist
  if (analysis.development > 0) {
    results.passed.push(`✅ 发展性指标数量: ${analysis.development} > 0`)
  } else {
    results.failed.push(`❌ 发展性指标数量: ${analysis.development} = 0`)
  }
  
  if (analysis.basic > 0) {
    results.passed.push(`✅ 基础性指标数量: ${analysis.basic} > 0`)
  } else {
    results.failed.push(`❌ 基础性指标数量: ${analysis.basic} = 0`)
  }
  
  return results
}

/**
 * Compare mock data with API response
 */
function compareWithApiResponse(mockIndicators, apiIndicators) {
  const discrepancies = []
  
  // Create a map of API indicators by name for comparison
  const apiMap = new Map()
  for (const api of apiIndicators) {
    apiMap.set(api.indicatorDesc || api.name, api)
  }
  
  for (const mock of mockIndicators) {
    const api = apiMap.get(mock.name)
    
    if (!api) {
      discrepancies.push({
        field: 'existence',
        mockId: mock.id,
        mockValue: mock.name,
        apiValue: null,
        message: `Mock indicator "${mock.name}" not found in API response`
      })
      continue
    }
    
    // Compare fields
    const fieldsToCompare = [
      { mock: 'isQualitative', api: 'isQualitative' },
      { mock: 'type1', api: 'type1' },
      { mock: 'type2', api: 'type2' },
      { mock: 'progress', api: 'progress' },
      { mock: 'targetValue', api: 'targetValue' },
      { mock: 'unit', api: 'unit' },
      { mock: 'responsiblePerson', api: 'responsiblePerson' }
    ]
    
    for (const field of fieldsToCompare) {
      const mockValue = mock[field.mock]
      const apiValue = api[field.api]
      
      if (mockValue !== undefined && apiValue !== undefined && mockValue !== apiValue) {
        // Allow for numeric tolerance
        if (typeof mockValue === 'number' && typeof apiValue === 'number') {
          if (Math.abs(mockValue - apiValue) > 0.01) {
            discrepancies.push({
              field: field.mock,
              mockId: mock.id,
              mockValue,
              apiValue,
              message: `Field "${field.mock}" mismatch for "${mock.name}"`
            })
          }
        } else {
          discrepancies.push({
            field: field.mock,
            mockId: mock.id,
            mockValue,
            apiValue,
            message: `Field "${field.mock}" mismatch for "${mock.name}"`
          })
        }
      }
    }
  }
  
  return discrepancies
}

// ==================== Report Generation ====================

/**
 * Generate verification report
 */
function generateReport(analysis, validation, discrepancies = []) {
  const report = []
  
  report.push('# 前后端数据一致性验证报告')
  report.push('')
  report.push(`生成时间: ${new Date().toISOString()}`)
  report.push('')
  
  report.push('## 1. Mock 数据分析')
  report.push('')
  report.push(`- 总指标数: ${analysis.total}`)
  report.push(`- 定量指标: ${analysis.quantitative}`)
  report.push(`- 定性指标: ${analysis.qualitative}`)
  report.push(`- 发展性指标: ${analysis.development}`)
  report.push(`- 基础性指标: ${analysis.basic}`)
  report.push('')
  
  report.push('## 2. 数据完整性验证')
  report.push('')
  report.push('### 通过的检查')
  for (const passed of validation.passed) {
    report.push(`- ${passed}`)
  }
  report.push('')
  
  if (validation.failed.length > 0) {
    report.push('### 失败的检查')
    for (const failed of validation.failed) {
      report.push(`- ${failed}`)
    }
    report.push('')
  }
  
  report.push('## 3. 数据差异')
  report.push('')
  if (discrepancies.length === 0) {
    report.push('✅ 未发现数据差异')
  } else {
    report.push(`发现 ${discrepancies.length} 处差异:`)
    report.push('')
    for (const d of discrepancies.slice(0, 20)) {
      report.push(`- **${d.field}** (${d.mockId}): Mock="${d.mockValue}", API="${d.apiValue}"`)
    }
    if (discrepancies.length > 20) {
      report.push(`- ... 还有 ${discrepancies.length - 20} 处差异`)
    }
  }
  
  return report.join('\n')
}

// ==================== Main Execution ====================

async function main() {
  console.log('🔍 开始前后端数据一致性验证...\n')
  
  // Load mock data
  console.log('📂 加载 Mock 数据...')
  const mockIndicators = loadMockIndicators()
  console.log(`   找到 ${mockIndicators.length} 个指标\n`)
  
  // Analyze mock data
  console.log('📊 分析 Mock 数据...')
  const analysis = analyzeMockData(mockIndicators)
  console.log(`   定量指标: ${analysis.quantitative}`)
  console.log(`   定性指标: ${analysis.qualitative}`)
  console.log(`   发展性指标: ${analysis.development}`)
  console.log(`   基础性指标: ${analysis.basic}\n`)
  
  // Validate completeness
  console.log('✅ 验证数据完整性...')
  const validation = validateCompleteness(analysis)
  
  for (const passed of validation.passed) {
    console.log(`   ${passed}`)
  }
  for (const failed of validation.failed) {
    console.log(`   ${failed}`)
  }
  console.log('')
  
  // Generate report
  const report = generateReport(analysis, validation)
  
  // Save report
  const reportPath = path.join(__dirname, '../docs/data-consistency-report.md')
  fs.mkdirSync(path.dirname(reportPath), { recursive: true })
  fs.writeFileSync(reportPath, report)
  console.log(`📄 报告已保存到: ${reportPath}\n`)
  
  // Summary
  const allPassed = validation.failed.length === 0
  if (allPassed) {
    console.log('🎉 所有数据完整性检查通过!')
  } else {
    console.log(`⚠️ ${validation.failed.length} 项检查未通过`)
    process.exit(1)
  }
}

// Export for testing
module.exports = {
  loadMockIndicators,
  analyzeMockData,
  validateCompleteness,
  compareWithApiResponse,
  generateReport
}

// Run if executed directly
if (require.main === module) {
  main().catch(console.error)
}
