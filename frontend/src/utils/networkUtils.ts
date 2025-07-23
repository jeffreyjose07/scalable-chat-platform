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
  // Check for runtime configuration first (Docker/Render deployment)
  if (typeof window !== 'undefined' && (window as any)._env_?.REACT_APP_API_URL) {
    const baseUrl = (window as any)._env_.REACT_APP_API_URL;
    console.log('🌐 Using runtime API base URL:', baseUrl);
    return baseUrl;
  }
  
  // If environment variable is set and not empty, use it
  if (process.env.REACT_APP_API_URL && process.env.REACT_APP_API_URL.trim()) {
    console.log('🌐 Using build-time API base URL:', process.env.REACT_APP_API_URL);
    return process.env.REACT_APP_API_URL;
  }
  
  const hostname = window.location.hostname;
  
  // For production deployment (render.com, etc), use relative URL
  if (hostname !== 'localhost' && !hostname.startsWith('192.168') && !hostname.startsWith('10.') && !hostname.startsWith('172.')) {
    console.log('🌐 Using relative API base URL for single service deployment');
    return ''; // Relative path for single service deployment
  }
  
  // If accessing via local IP, use the same IP for backend
  if (isLocalIP(hostname)) {
    console.log('🌐 Using local IP API base URL:', `http://${hostname}:8080`);
    return `http://${hostname}:8080`;
  }
  
  // Default to localhost for local development
  console.log('🌐 Using localhost API base URL for development');
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
  
  // For single service deployment, use relative WebSocket URL
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsUrl = `${protocol}//${window.location.host}`;
  console.log('🔌 Using single-service WebSocket URL:', wsUrl);
  return wsUrl;
};

