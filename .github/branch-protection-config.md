# Branch Protection Configuration

To enable branch protection for the `main`/`master` branch, go to your GitHub repository settings and configure the following:

## Settings → Branches → Add Rule

### Branch name pattern: `main` (or `master`)

### Protection Rules:
- ✅ **Require a pull request before merging**
  - ✅ Require approvals: 1
  - ✅ Dismiss stale PR approvals when new commits are pushed
  - ✅ Require review from code owners (if CODEOWNERS file exists)

- ✅ **Require status checks to pass before merging**
  - ✅ Require branches to be up to date before merging
  - **Required status checks:**
    - `Backend Build & Test`
    - `Frontend Build & Test`
    - `Build Status Check`

- ✅ **Require conversation resolution before merging**

- ✅ **Require signed commits** (optional but recommended)

- ✅ **Include administrators** (applies rules to repo admins too)

- ✅ **Restrict pushes that create files, edit files, or delete files**
  - This prevents direct pushes to main branch

### Additional Settings:
- ✅ **Allow force pushes: Everyone** (set to disabled)
- ✅ **Allow deletions** (set to disabled)

## Alternative: Use GitHub CLI

You can also configure branch protection using GitHub CLI:

```bash
# Install GitHub CLI if not already installed
# https://cli.github.com/

# Configure branch protection
gh api repos/:owner/:repo/branches/main/protection \
  --method PUT \
  --field required_status_checks='{"strict":true,"contexts":["Backend Build & Test","Frontend Build & Test","Build Status Check"]}' \
  --field enforce_admins=true \
  --field required_pull_request_reviews='{"required_approving_review_count":1,"dismiss_stale_reviews":true}' \
  --field restrictions=null
```

## Verification

After setting up branch protection:

1. Try to push directly to main branch (should fail)
2. Create a PR with failing tests (should prevent merge)
3. Create a PR with passing tests (should allow merge after approval)

## CODEOWNERS File (Optional)

Create `.github/CODEOWNERS` to automatically request reviews from specific people:

```
# Global owners
* @your-username

# Backend specific
/backend/ @backend-team-lead @your-username

# Frontend specific  
/frontend/ @frontend-team-lead @your-username

# CI/CD workflows
/.github/ @devops-team @your-username
```