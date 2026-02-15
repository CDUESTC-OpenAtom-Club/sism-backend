# OWASP Dependency Check Security Scanning Guide

## Overview

This document provides comprehensive guidance on using OWASP Dependency Check for security vulnerability scanning in the SISM backend project.

## What is OWASP Dependency Check?

OWASP Dependency Check is a Software Composition Analysis (SCA) tool that identifies known vulnerabilities in project dependencies by checking them against the National Vulnerability Database (NVD).

## Setup

### 1. Get an NVD API Key (Highly Recommended)

Without an API key, the NVD update process can take a VERY long time and may fail due to rate limiting.

**Steps to get an API key:**

1. Visit: https://nvd.nist.gov/developers/request-an-api-key
2. Fill out the request form with your email address
3. Check your email for the API key
4. Add the API key to your environment

**Add to `.env` file:**
```bash
NVD_API_KEY=your-api-key-here
```

**Or export as environment variable:**
```bash
export NVD_API_KEY=your-api-key-here
```

### 2. Configuration

The OWASP Dependency Check plugin is already configured in `pom.xml` with the following settings:

- **Fail Build Threshold**: CVSS score >= 7 (High severity)
- **Report Formats**: HTML, JSON, XML
- **Suppressions File**: `owasp-suppressions.xml`
- **Analyzers**: Assembly, Archive (Node.js and RetireJS disabled for Java project)

## Usage

### Running Security Scan

**Basic scan:**
```bash
mvn dependency-check:check
```

**Scan with tests skipped:**
```bash
mvn dependency-check:check -DskipTests
```

**Update NVD database only:**
```bash
mvn dependency-check:update-only
```

**Aggregate scan (for multi-module projects):**
```bash
mvn dependency-check:aggregate
```

### Viewing Reports

After running the scan, reports are generated in:
- **HTML Report**: `target/dependency-check-report.html` (Open in browser)
- **JSON Report**: `target/dependency-check-report.json`
- **XML Report**: `target/dependency-check-report.xml`

### Understanding Results

The report includes:

1. **Dependency Information**
   - File name and path
   - Package URL (PURL)
   - Identifiers (CPE, Maven coordinates)

2. **Vulnerability Details**
   - CVE ID
   - CVSS Score (0-10 scale)
   - Severity (Low, Medium, High, Critical)
   - Description
   - References and links

3. **Severity Levels**
   - **Critical**: CVSS 9.0-10.0
   - **High**: CVSS 7.0-8.9
   - **Medium**: CVSS 4.0-6.9
   - **Low**: CVSS 0.1-3.9

## Handling Vulnerabilities

### 1. Update Dependencies

The best solution is to update vulnerable dependencies to patched versions:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>vulnerable-lib</artifactId>
    <version>2.0.0</version> <!-- Updated from 1.0.0 -->
</dependency>
```

### 2. Suppress False Positives

If a vulnerability is a false positive or doesn't apply to your usage, add a suppression to `owasp-suppressions.xml`:

```xml
<suppress>
    <notes><![CDATA[
        False positive: This CVE affects a different component with the same name.
        Our usage of this library doesn't expose the vulnerable code path.
    ]]></notes>
    <cve>CVE-2023-12345</cve>
</suppress>
```

**Suppress by file name:**
```xml
<suppress>
    <notes><![CDATA[
        This is a test dependency not used in production.
    ]]></notes>
    <filePath regex="true">.*test-library.*\.jar</filePath>
</suppress>
```

**Suppress by GAV (Group, Artifact, Version):**
```xml
<suppress>
    <notes><![CDATA[
        Vulnerability doesn't affect our usage pattern.
    ]]></notes>
    <gav regex="true">com\.example:.*:.*</gav>
    <cve>CVE-2023-12345</cve>
</suppress>
```

### 3. Accept Risk

For vulnerabilities that cannot be fixed immediately:

1. Document the risk in `owasp-suppressions.xml`
2. Create a tracking issue
3. Plan remediation in next sprint
4. Add suppression with expiration date:

```xml
<suppress until="2026-03-31">
    <notes><![CDATA[
        Accepted risk: No patch available yet.
        Tracking issue: SISM-123
        Mitigation: Input validation implemented at application layer.
    ]]></notes>
    <cve>CVE-2023-12345</cve>
</suppress>
```

## CI/CD Integration

### GitHub Actions

Add to `.github/workflows/security-scan.yml`:

```yaml
name: Security Scan

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]
  schedule:
    # Run weekly on Monday at 2 AM
    - cron: '0 2 * * 1'

jobs:
  security-scan:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven
      
      - name: Run OWASP Dependency Check
        env:
          NVD_API_KEY: ${{ secrets.NVD_API_KEY }}
        run: |
          cd sism-backend
          mvn dependency-check:check -DskipTests
      
      - name: Upload Security Report
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: dependency-check-report
          path: sism-backend/target/dependency-check-report.html
      
      - name: Check for High/Critical Vulnerabilities
        run: |
          cd sism-backend
          if [ -f target/dependency-check-report.json ]; then
            HIGH_COUNT=$(jq '[.dependencies[].vulnerabilities[] | select(.severity == "HIGH" or .severity == "CRITICAL")] | length' target/dependency-check-report.json)
            if [ "$HIGH_COUNT" -gt 0 ]; then
              echo "Found $HIGH_COUNT high/critical vulnerabilities!"
              exit 1
            fi
          fi
```

### Add NVD API Key to GitHub Secrets

1. Go to repository Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Name: `NVD_API_KEY`
4. Value: Your NVD API key
5. Click "Add secret"

## Configuration Options

### Fail Build Threshold

Adjust the CVSS score threshold in `pom.xml`:

```xml
<configuration>
    <!-- Fail build if CVSS score >= 7 (High severity) -->
    <failBuildOnCVSS>7</failBuildOnCVSS>
    
    <!-- Or fail on specific severity levels -->
    <failBuildOnAnyVulnerability>false</failBuildOnAnyVulnerability>
</configuration>
```

### Scope Configuration

Control which dependencies are scanned:

```xml
<configuration>
    <!-- Skip test dependencies -->
    <skipTestScope>true</skipTestScope>
    
    <!-- Skip provided dependencies -->
    <skipProvidedScope>true</skipProvidedScope>
    
    <!-- Skip runtime dependencies -->
    <skipRuntimeScope>false</skipRuntimeScope>
</configuration>
```

### Analyzer Configuration

Enable/disable specific analyzers:

```xml
<configuration>
    <!-- Assembly analyzer (for JAR files) -->
    <assemblyAnalyzerEnabled>true</assemblyAnalyzerEnabled>
    
    <!-- Archive analyzer -->
    <archiveAnalyzerEnabled>true</archiveAnalyzerEnabled>
    
    <!-- Node.js analyzer (disabled for Java project) -->
    <nodeAnalyzerEnabled>false</nodeAnalyzerEnabled>
    
    <!-- Retire.js analyzer (for JavaScript) -->
    <retireJsAnalyzerEnabled>false</retireJsAnalyzerEnabled>
</configuration>
```

## Troubleshooting

### Issue: NVD Update Fails with 403/404 Error

**Solution**: Get an NVD API key (see Setup section above)

### Issue: Scan Takes Too Long

**Solutions**:
1. Use an NVD API key to speed up updates
2. Cache NVD data between runs
3. Run updates separately: `mvn dependency-check:update-only`

### Issue: False Positives

**Solution**: Add suppressions to `owasp-suppressions.xml` with clear documentation

### Issue: Build Fails on Known Vulnerabilities

**Temporary workaround** (not recommended for production):
```bash
mvn dependency-check:check -DfailBuildOnCVSS=11
```

**Proper solution**: Fix or suppress vulnerabilities

## Best Practices

1. **Run Regularly**: Schedule weekly scans in CI/CD
2. **Use API Key**: Always use an NVD API key for faster updates
3. **Document Suppressions**: Add clear notes explaining why vulnerabilities are suppressed
4. **Review Reports**: Don't just suppress - understand the risks
5. **Update Dependencies**: Keep dependencies up-to-date
6. **Track Issues**: Create tickets for vulnerabilities that can't be fixed immediately
7. **Set Expiration**: Use `until` attribute on suppressions to force periodic review
8. **Test After Updates**: Run full test suite after updating dependencies

## Resources

- **OWASP Dependency Check**: https://jeremylong.github.io/DependencyCheck/
- **NVD API Key**: https://nvd.nist.gov/developers/request-an-api-key
- **CVE Database**: https://cve.mitre.org/
- **CVSS Calculator**: https://nvd.nist.gov/vuln-metrics/cvss/v3-calculator
- **Suppression File Schema**: https://jeremylong.github.io/DependencyCheck/general/suppression.html

## Example Workflow

1. **Run scan**: `mvn dependency-check:check`
2. **Review HTML report**: Open `target/dependency-check-report.html`
3. **For each vulnerability**:
   - Check if update available → Update dependency
   - If false positive → Add suppression with documentation
   - If real but no fix → Document risk, create issue, add temporary suppression
4. **Commit changes**: Update `pom.xml` and `owasp-suppressions.xml`
5. **Verify**: Run scan again to confirm issues resolved

## Security Scan Checklist

- [ ] NVD API key configured
- [ ] Scan runs successfully
- [ ] No high/critical vulnerabilities (or all suppressed with documentation)
- [ ] Suppressions have clear justification
- [ ] Suppressions have expiration dates where appropriate
- [ ] CI/CD pipeline includes security scan
- [ ] Team trained on handling vulnerabilities
- [ ] Process documented for vulnerability response

---

**Last Updated**: 2026-02-14
**Version**: 1.0
**Maintainer**: Security Team
