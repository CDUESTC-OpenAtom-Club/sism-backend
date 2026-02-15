# OWASP Dependency Check - Setup Summary

## Quick Start

### 1. Get NVD API Key (Required)

Visit: https://nvd.nist.gov/developers/request-an-api-key

### 2. Configure Environment

Add to `.env` file:
```bash
NVD_API_KEY=your-api-key-here
```

### 3. Run Security Scan

```bash
cd sism-backend
mvn dependency-check:check -DskipTests
```

### 4. View Report

Open: `target/dependency-check-report.html` in your browser

## What Was Implemented

### Maven Plugin Configuration

- **Plugin**: OWASP Dependency Check 9.0.9
- **Location**: `pom.xml`
- **Fail Threshold**: CVSS >= 7 (High/Critical)
- **Reports**: HTML, JSON, XML

### Files Created

1. **pom.xml** - Plugin configuration
2. **owasp-suppressions.xml** - False positive suppressions template
3. **docs/security/owasp-dependency-check-guide.md** - Complete documentation
4. **.env.example** - NVD_API_KEY configuration

### Key Features

✅ Automated vulnerability scanning
✅ NVD database integration
✅ Multiple report formats
✅ Suppression support for false positives
✅ CI/CD ready
✅ Configurable fail thresholds

## Common Commands

```bash
# Run security scan
mvn dependency-check:check

# Update NVD database only
mvn dependency-check:update-only

# Skip tests during scan
mvn dependency-check:check -DskipTests

# Generate aggregate report (multi-module)
mvn dependency-check:aggregate
```

## Handling Vulnerabilities

### 1. Update Dependencies (Preferred)

Update to patched version in `pom.xml`:
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>library</artifactId>
    <version>2.0.0</version> <!-- Updated -->
</dependency>
```

### 2. Suppress False Positives

Add to `owasp-suppressions.xml`:
```xml
<suppress>
    <notes><![CDATA[
        Reason for suppression with clear documentation
    ]]></notes>
    <cve>CVE-2023-12345</cve>
</suppress>
```

### 3. Accept Risk (Temporary)

Document and track:
```xml
<suppress until="2026-03-31">
    <notes><![CDATA[
        Accepted risk: No patch available
        Tracking: SISM-123
        Mitigation: Input validation at app layer
    ]]></notes>
    <cve>CVE-2023-12345</cve>
</suppress>
```

## CI/CD Integration

### GitHub Actions Example

```yaml
- name: Run OWASP Dependency Check
  env:
    NVD_API_KEY: ${{ secrets.NVD_API_KEY }}
  run: |
    cd sism-backend
    mvn dependency-check:check -DskipTests
```

### Add Secret to GitHub

1. Go to: Settings → Secrets and variables → Actions
2. Click: "New repository secret"
3. Name: `NVD_API_KEY`
4. Value: Your NVD API key
5. Save

## Troubleshooting

### Issue: 403/404 Error from NVD

**Solution**: Get and configure NVD API key

### Issue: Scan Takes Too Long

**Solution**: Use NVD API key (speeds up significantly)

### Issue: False Positives

**Solution**: Add suppressions to `owasp-suppressions.xml`

## Next Steps

1. ✅ Plugin configured
2. ⏳ Get NVD API key
3. ⏳ Run initial scan
4. ⏳ Review and fix vulnerabilities
5. ⏳ Add to CI/CD pipeline
6. ⏳ Schedule weekly scans

## Resources

- **Full Documentation**: `docs/security/owasp-dependency-check-guide.md`
- **OWASP Project**: https://jeremylong.github.io/DependencyCheck/
- **NVD API Key**: https://nvd.nist.gov/developers/request-an-api-key
- **CVE Database**: https://cve.mitre.org/

## Security Checklist

- [ ] NVD API key obtained and configured
- [ ] Initial security scan completed
- [ ] High/critical vulnerabilities addressed
- [ ] Suppressions documented
- [ ] CI/CD pipeline updated
- [ ] Team trained on process
- [ ] Weekly scan schedule established

---

**Status**: ✅ Setup Complete
**Date**: 2026-02-14
**Next Action**: Obtain NVD API key and run first scan
