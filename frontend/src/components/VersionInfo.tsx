import React, { useState } from 'react';

interface VersionInfoProps {
  className?: string;
  showFullInfo?: boolean;
  clickable?: boolean;
}

const VersionInfo: React.FC<VersionInfoProps> = ({ className = '', showFullInfo = false, clickable = false }) => {
  const [showDetails, setShowDetails] = useState(false);
  const version = process.env.REACT_APP_VERSION || '1.0.0';
  const buildTime = process.env.REACT_APP_BUILD_TIME || new Date().toISOString();
  const gitCommit = process.env.REACT_APP_GIT_COMMIT || 'unknown';
  const gitBranch = process.env.REACT_APP_GIT_BRANCH || 'unknown';
  const environment = process.env.REACT_APP_BUILD_ENV || process.env.NODE_ENV || 'development';

  if (!showFullInfo && !showDetails) {
    if (clickable) {
      return (
        <button 
          onClick={() => setShowDetails(true)}
          className={`text-xs text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300 transition-colors ${className}`}
          title="Click for build details"
        >
          v{version}
        </button>
      );
    }
    
    return (
      <span className={`text-xs text-gray-500 dark:text-gray-400 ${className}`}>
        v{version}
      </span>
    );
  }

  return (
    <div className={`text-xs text-gray-500 dark:text-gray-400 ${className}`}>
      {clickable && showDetails && (
        <div className="absolute right-0 top-8 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-600 rounded-lg shadow-lg p-3 z-50 min-w-48">
          <div className="flex justify-between items-center mb-2">
            <span className="font-medium text-gray-900 dark:text-gray-100">Build Info</span>
            <button 
              onClick={() => setShowDetails(false)}
              className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <div className="space-y-1">
            <div>Version: {version}</div>
            <div>Build: {new Date(buildTime).toLocaleString()}</div>
            <div>Commit: {gitCommit.substring(0, 8)}</div>
            <div>Branch: {gitBranch}</div>
            <div>Environment: {environment}</div>
          </div>
        </div>
      )}
      {!clickable && (
        <div className="space-y-1">
          <div>Version: {version}</div>
          <div>Build: {new Date(buildTime).toLocaleString()}</div>
          <div>Commit: {gitCommit.substring(0, 8)}</div>
          <div>Branch: {gitBranch}</div>
          <div>Environment: {environment}</div>
        </div>
      )}
    </div>
  );
};

export default VersionInfo;