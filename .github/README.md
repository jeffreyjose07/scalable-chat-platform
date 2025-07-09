# GitHub Actions CI/CD Setup

This directory contains GitHub Actions workflows and configurations for the Chat Platform project.

## 🚀 Workflows

### 1. CI Pipeline (`ci.yml`)
**Triggers**: Push to main/master/develop, Pull Requests to main/master

**Jobs**:
- **Backend Build & Test**: 
  - Compiles Java code with Maven
  - Runs unit tests with H2 in-memory database
  - Uses embedded/mocked services for testing
  - Generates test reports
  - Future integration tests will use Testcontainers
  
- **Frontend Build & Test**:
  - Installs Node.js dependencies
  - Runs ESLint for code quality
  - Performs TypeScript checking
  - Executes Jest tests with coverage
  - Builds production bundle
  
- **Security Scan**:
  - Runs Trivy vulnerability scanner
  - Uploads results to GitHub Security tab
  
- **Build Status Check**:
  - Aggregates all job results
  - Provides single status for branch protection

### 2. Deploy Pipeline (`deploy.yml`)
**Triggers**: Push to main/master, version tags, manual dispatch

**Features**:
- Builds production artifacts
- Creates deployment packages
- Supports staging/production environments
- Uploads artifacts for deployment

## 🔧 Configuration Files

### `dependabot.yml`
- Automated dependency updates
- Weekly schedule for Maven, npm, and GitHub Actions
- Creates PRs for security and feature updates

### `pull_request_template.md`
- Standardized PR descriptions
- Checklist for code quality
- Testing requirements

### `branch-protection-config.md`
- Instructions for setting up branch protection
- Required status checks configuration
- GitHub CLI commands for automation

## 🛡️ Branch Protection

To enable merge protection:

1. Go to Repository Settings → Branches
2. Add protection rule for `main` branch
3. Enable required status checks:
   - ✅ Backend Build & Test
   - ✅ Frontend Build & Test
   - ✅ Build Status Check
4. Require PR reviews (minimum 1)
5. Enable "Require branches to be up to date"

## 🔍 Status Checks

The CI pipeline ensures:
- ✅ Backend compiles without errors
- ✅ All backend tests pass
- ✅ Frontend builds successfully
- ✅ TypeScript compilation succeeds
- ✅ ESLint checks pass
- ✅ Jest tests execute
- ✅ No critical security vulnerabilities

## 🚨 Troubleshooting

### Maven Corporate Repository Issues
If you encounter Maven repository issues:
1. The CI workflow overrides settings.xml to use Maven Central
2. Local development may need VPN/corporate network access
3. Consider using Maven wrapper for consistent builds

### Node.js Version Compatibility
- CI uses Node.js 18 LTS
- Ensure local development uses compatible version
- Update `.nvmrc` file if needed

### Test Database Issues
- Unit tests use H2 in-memory database (no external dependencies)
- Integration tests will use Testcontainers when needed
- Check H2 dependency and test configuration if tests fail

## 📊 Monitoring

- **Test Reports**: Available in Actions summary
- **Coverage Reports**: Uploaded to Codecov
- **Security Scans**: Results in Security tab
- **Artifacts**: Build outputs stored for 30 days

## 🔄 Deployment

### Staging Deployment
- Automatic on main branch push
- Triggered after successful CI

### Production Deployment
- Manual trigger via workflow dispatch
- Tag-based deployment (v*.*.*)
- Requires manual approval (when environments configured)

## 📝 Best Practices

1. **Never commit directly to main**
2. **Always create PRs for changes**
3. **Ensure all tests pass locally**
4. **Update tests for new features**
5. **Follow conventional commit messages**
6. **Review security scan results**