import React from 'react';
import VersionInfo from './VersionInfo';

const Footer: React.FC = () => {
  return (
    <footer className="bg-white/60 dark:bg-gray-900/60 backdrop-blur-sm border-t border-gray-200/30 dark:border-gray-700/30">
      <div className="max-w-7xl mx-auto px-4 py-2">
        <div className="flex justify-center items-center space-x-4 text-xs text-gray-500 dark:text-gray-400">
          <span>© 2025 Chat Platform</span>
          <span>•</span>
          <VersionInfo />
          <span>•</span>
          <a 
            href="https://jeffreyjose07.github.io" 
            target="_blank" 
            rel="noopener noreferrer"
            className="hover:text-gray-700 dark:hover:text-gray-300 transition-colors"
          >
            Jeffrey Jose
          </a>
        </div>
      </div>
    </footer>
  );
};

export default Footer;