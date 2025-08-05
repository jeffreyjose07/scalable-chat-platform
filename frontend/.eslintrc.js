module.exports = {
  extends: [
    'react-app',
    'react-app/jest',
    'prettier'
  ],
  rules: {
    // Console logs are acceptable in development builds
    'no-console': 'off',
    'prefer-const': 'error',
    'no-var': 'error',
    'react/react-in-jsx-scope': 'off',
    '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
    // Allow any types temporarily - these should be fixed in development iterations
    '@typescript-eslint/no-explicit-any': 'warn',
    // Allow missing dependencies as warnings - these should be addressed but not block builds
    'react-hooks/exhaustive-deps': 'warn',
  },
  ignorePatterns: [
    'build/',
    'node_modules/',
    '*.config.js'
  ]
};