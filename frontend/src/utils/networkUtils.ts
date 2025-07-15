/**
 * Network utilities for detecting local IP and configuring API endpoints
 */

/**
 * Detects if the current hostname is a local IP address
 */
export const isLocalIP = (hostname: string): boolean => {
  // Check for private IP ranges
  const privateIPPatterns = [
    /^192\.168\.\d{1,3}\.\d{1,3}$/,     // 192.168.x.x
    /^10\.\d{1,3}\.\d{1,3}\.\d{1,3}$/,  // 10.x.x.x
    /^172\.(1[6-9]|2[0-9]|3[01])\.\d{1,3}\.\d{1,3}$/ // 172.16.x.x - 172.31.x.x
  ];
  
  return privateIPPatterns.some(pattern => pattern.test(hostname));
};

/**
 * Gets the appropriate API base URL based on current environment
 */
export const getApiBaseUrl = (): string => {
  // If environment variable is set, use it
  if (process.env.REACT_APP_API_URL) {
    return process.env.REACT_APP_API_URL;
  }
  
  const hostname = window.location.hostname;
  
  // If accessing via local IP, use the same IP for backend
  if (isLocalIP(hostname)) {
    return `http://${hostname}:8080`;
  }
  
  // Default to localhost for local development
  return 'http://localhost:8080';
};

/**
 * Gets the appropriate WebSocket URL based on current environment
 */
export const getWebSocketUrl = (): string => {
  // Check for runtime configuration first (Docker)
  if (typeof window !== 'undefined' && (window as any)._env_?.REACT_APP_WS_URL) {
    return (window as any)._env_.REACT_APP_WS_URL;
  }
  
  // If environment variable is set, use it
  if (process.env.REACT_APP_WS_URL) {
    return process.env.REACT_APP_WS_URL;
  }
  
  const hostname = window.location.hostname;
  
  // If accessing via local IP, use the same IP for WebSocket
  if (isLocalIP(hostname)) {
    return `ws://${hostname}:8080`;
  }
  
  // Default to localhost for local development
  return 'ws://localhost:8080';
};

/**
 * Gets the current network configuration info for debugging
 */
export const getNetworkInfo = () => {
  const hostname = window.location.hostname;
  const port = window.location.port;
  const isLocal = isLocalIP(hostname);
  
  return {
    hostname,
    port,
    isLocalIP: isLocal,
    apiUrl: getApiBaseUrl(),
    wsUrl: getWebSocketUrl(),
    fullUrl: window.location.href
  };
};