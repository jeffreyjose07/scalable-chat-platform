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
    const wsUrl = (window as any)._env_.REACT_APP_WS_URL;
    console.log('🔌 Using runtime WebSocket URL:', wsUrl);
    return wsUrl;
  }
  
  // If environment variable is set and not empty, use it
  if (process.env.REACT_APP_WS_URL && process.env.REACT_APP_WS_URL.trim()) {
    console.log('🔌 Using build-time WebSocket URL:', process.env.REACT_APP_WS_URL);
    return process.env.REACT_APP_WS_URL;
  }
  
  const hostname = window.location.hostname;
  
  // If accessing via local IP, use the same IP for WebSocket
  if (isLocalIP(hostname)) {
    const wsUrl = `ws://${hostname}:8080`;
    console.log('🔌 Using local IP WebSocket URL:', wsUrl);
    return wsUrl;
  }
  
  // Default to localhost for local development
  const wsUrl = 'ws://localhost:8080';
  console.log('🔌 Using default WebSocket URL:', wsUrl);
  return wsUrl;
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