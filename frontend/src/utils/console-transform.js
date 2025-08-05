/**
 * Build-time console transformation utility
 * This script replaces console statements with production-safe alternatives
 */

const isProduction = process.env.NODE_ENV === 'production';

// Transform console.log, console.debug, console.info to no-ops in production
// Keep console.warn and console.error for important messages
const transformConsole = (content) => {
  if (!isProduction) {
    return content;
  }

  // Replace console.log, console.debug, console.info with no-ops
  return content
    .replace(/console\.(log|debug|info)\s*\([^;]*\);?/g, '/* $& */') // Comment out
    .replace(/console\.(log|debug|info)\s*\([^)]*\)/g, 'void 0'); // Replace with void 0
};

// This would be integrated into the build process
module.exports = { transformConsole };