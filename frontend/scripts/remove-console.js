#!/usr/bin/env node

/**
 * Post-build script to remove console statements from production bundles
 * This is a standard practice for production React applications
 */

const fs = require('fs');
const path = require('path');

const buildDir = path.join(__dirname, '..', 'build');

function findJSFiles(dir, files = []) {
  const items = fs.readdirSync(dir);
  
  for (const item of items) {
    const fullPath = path.join(dir, item);
    const stat = fs.statSync(fullPath);
    
    if (stat.isDirectory()) {
      findJSFiles(fullPath, files);
    } else if (item.endsWith('.js')) {
      files.push(fullPath);
    }
  }
  
  return files;
}

function removeConsoleFromFile(filePath) {
  try {
    let content = fs.readFileSync(filePath, 'utf8');
    
    // Remove console.log, console.debug, console.info (keep console.warn and console.error)
    // This regex handles most common console statement patterns
    content = content
      .replace(/console\.(log|debug|info)\s*\([^)]*\)\s*;?/g, '')
      .replace(/console\.(log|debug|info)\s*\([^)]*\)/g, 'void 0');
    
    fs.writeFileSync(filePath, content, 'utf8');
    console.log(`✅ Cleaned console statements from: ${path.basename(filePath)}`);
  } catch (error) {
    console.warn(`⚠️  Failed to clean ${filePath}:`, error.message);
  }
}

// Only run if build directory exists
if (!fs.existsSync(buildDir)) {
  console.log('📁 Build directory does not exist, skipping console cleanup');
  process.exit(0);
}

// Find all JavaScript files in the build directory
const jsFiles = findJSFiles(buildDir);

if (jsFiles.length === 0) {
  console.log('📄 No JavaScript files found in build directory');
  process.exit(0);
}

console.log(`🧹 Cleaning console statements from ${jsFiles.length} JavaScript files...`);

jsFiles.forEach(file => {
  removeConsoleFromFile(file);
});

console.log('✨ Console cleanup completed successfully!');