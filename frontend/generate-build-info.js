const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

// Generate build information
const generateBuildInfo = () => {
  const packageJson = JSON.parse(fs.readFileSync('package.json', 'utf8'));
  
  let gitCommit = 'unknown';
  let gitBranch = 'unknown';
  
  try {
    gitCommit = execSync('git rev-parse HEAD', { encoding: 'utf8' }).trim();
    gitBranch = execSync('git rev-parse --abbrev-ref HEAD', { encoding: 'utf8' }).trim();
  } catch (error) {
    console.warn('Could not get git information:', error.message);
    // Try to get from environment variables (common in CI/CD)
    gitCommit = process.env.RENDER_GIT_COMMIT || process.env.GITHUB_SHA || gitCommit;
    gitBranch = process.env.RENDER_GIT_BRANCH || process.env.GITHUB_REF_NAME || gitBranch;
  }
  
  const buildInfo = {
    version: packageJson.version,
    buildTime: new Date().toISOString(),
    gitCommit,
    gitBranch,
    nodeVersion: process.version,
    environment: process.env.NODE_ENV || 'development'
  };
  
  // Create .env.local file with build info
  const envContent = [
    `REACT_APP_VERSION=${buildInfo.version}`,
    `REACT_APP_BUILD_TIME=${buildInfo.buildTime}`,
    `REACT_APP_GIT_COMMIT=${buildInfo.gitCommit}`,
    `REACT_APP_GIT_BRANCH=${buildInfo.gitBranch}`,
    `REACT_APP_NODE_VERSION=${buildInfo.nodeVersion}`,
    `REACT_APP_BUILD_ENV=${buildInfo.environment}`
  ].join('\n');
  
  fs.writeFileSync('.env.local', envContent);
  
  console.log('Build information generated:');
  console.log(JSON.stringify(buildInfo, null, 2));
};

generateBuildInfo();