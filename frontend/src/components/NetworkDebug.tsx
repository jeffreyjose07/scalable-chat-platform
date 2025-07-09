import React, { useState } from 'react';
import { getNetworkInfo } from '../utils/networkUtils';

const NetworkDebug: React.FC = () => {
  const [showDebug, setShowDebug] = useState(false);
  const networkInfo = getNetworkInfo();

  if (!showDebug) {
    return (
      <button
        onClick={() => setShowDebug(true)}
        className="fixed bottom-4 right-4 bg-gray-600 text-white px-3 py-1 rounded text-xs opacity-50 hover:opacity-100"
      >
        Network Info
      </button>
    );
  }

  return (
    <div className="fixed bottom-4 right-4 bg-white border border-gray-300 rounded-lg p-4 shadow-lg max-w-sm text-xs">
      <div className="flex justify-between items-start mb-2">
        <h3 className="font-semibold text-gray-800">Network Configuration</h3>
        <button
          onClick={() => setShowDebug(false)}
          className="text-gray-500 hover:text-gray-700"
        >
          ✕
        </button>
      </div>
      
      <div className="space-y-2 text-gray-600">
        <div>
          <strong>Frontend URL:</strong> {networkInfo.fullUrl}
        </div>
        <div>
          <strong>Hostname:</strong> {networkInfo.hostname}
        </div>
        <div>
          <strong>Is Local IP:</strong> {networkInfo.isLocalIP ? 'Yes' : 'No'}
        </div>
        <div>
          <strong>API URL:</strong> {networkInfo.apiUrl}
        </div>
        <div>
          <strong>WebSocket URL:</strong> {networkInfo.wsUrl}
        </div>
      </div>
      
      <div className="mt-3 pt-2 border-t border-gray-200">
        <div className="text-xs text-gray-500">
          URLs are automatically detected based on your current network
        </div>
      </div>
    </div>
  );
};

export default NetworkDebug;