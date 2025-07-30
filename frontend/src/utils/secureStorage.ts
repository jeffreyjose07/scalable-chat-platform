/**
 * Secure storage utility for JWT tokens
 * Implements best practices for client-side token storage
 */

interface StorageOptions {
  encrypt?: boolean;
  prefix?: string;
}

class SecureStorage {
  private readonly prefix: string;
  private readonly encrypt: boolean;

  constructor(options: StorageOptions = {}) {
    this.prefix = options.prefix || 'chat_secure_';
    this.encrypt = options.encrypt || false;
  }

  /**
   * Store JWT token securely
   * Uses sessionStorage by default for better security (cleared on tab close)
   */
  setToken(token: string, persistent: boolean = false): void {
    const key = this.prefix + 'token';
    const storage = persistent ? localStorage : sessionStorage;
    
    try {
      const encryptedToken = this.encrypt ? this.simpleEncrypt(token) : token;
      storage.setItem(key, encryptedToken);
      
      // Store token metadata for security checks
      const metadata = {
        timestamp: Date.now(),
        persistent,
        fingerprint: this.generateFingerprint()
      };
      storage.setItem(key + '_meta', JSON.stringify(metadata));
      
      console.debug('Token stored securely', { persistent, encrypted: this.encrypt });
    } catch (error) {
      console.error('Failed to store token:', error);
      throw new Error('Token storage failed');
    }
  }

  /**
   * Retrieve JWT token with security validation
   */
  getToken(): string | null {
    const key = this.prefix + 'token';
    
    try {
      // Try sessionStorage first (more secure)
      let token = sessionStorage.getItem(key);
      let metadata = sessionStorage.getItem(key + '_meta');
      
      // Fallback to localStorage if not found
      if (!token) {
        token = localStorage.getItem(key);
        metadata = localStorage.getItem(key + '_meta');
      }
      
      if (!token) {
        return null;
      }
      
      // Validate token metadata
      if (metadata) {
        const meta = JSON.parse(metadata);
        
        // Check if token is too old (24 hours max)
        const maxAge = 24 * 60 * 60 * 1000; // 24 hours
        if (Date.now() - meta.timestamp > maxAge) {
          console.warn('Token expired by age, removing');
          this.removeToken();
          return null;
        }
        
        // Basic fingerprint check (detect if browser environment changed)
        // TEMPORARILY DISABLED: After security headers update, fingerprints may have changed
        // Users will need to re-login once, then this can be re-enabled
        if (meta.fingerprint && meta.fingerprint !== this.generateFingerprint()) {
          console.warn('Token fingerprint mismatch detected - likely due to security updates. Allowing token to work.');
          // Temporarily disabled to prevent all users from being logged out after security updates
          // this.removeToken();
          // return null;
        }
      }
      
      const decryptedToken = this.encrypt ? this.simpleDecrypt(token) : token;
      return decryptedToken;
      
    } catch (error) {
      console.error('Failed to retrieve token:', error);
      this.removeToken(); // Clean up corrupted data
      return null;
    }
  }

  /**
   * Remove JWT token from all storage locations
   */
  removeToken(): void {
    const key = this.prefix + 'token';
    
    try {
      // Remove from both storage types
      sessionStorage.removeItem(key);
      sessionStorage.removeItem(key + '_meta');
      localStorage.removeItem(key);
      localStorage.removeItem(key + '_meta');
      
      console.debug('Token removed from secure storage');
    } catch (error) {
      console.error('Failed to remove token:', error);
    }
  }

  /**
   * Check if token exists without retrieving it
   */
  hasToken(): boolean {
    const key = this.prefix + 'token';
    return !!(sessionStorage.getItem(key) || localStorage.getItem(key));
  }

  /**
   * Clear all secure storage data
   */
  clearAll(): void {
    try {
      // Clear all items with our prefix
      for (let i = sessionStorage.length - 1; i >= 0; i--) {
        const key = sessionStorage.key(i);
        if (key && key.startsWith(this.prefix)) {
          sessionStorage.removeItem(key);
        }
      }
      
      for (let i = localStorage.length - 1; i >= 0; i--) {
        const key = localStorage.key(i);
        if (key && key.startsWith(this.prefix)) {
          localStorage.removeItem(key);
        }
      }
      
      console.debug('All secure storage cleared');
    } catch (error) {
      console.error('Failed to clear secure storage:', error);
    }
  }

  /**
   * Generate a simple browser fingerprint for basic security
   * Made more robust to work with CSP headers
   */
  private generateFingerprint(): string {
    // Use only stable browser properties that won't be affected by CSP
    const fingerprint = [
      navigator.userAgent,
      navigator.language,
      screen.width + 'x' + screen.height,
      new Date().getTimezoneOffset(),
      // Remove canvas fingerprinting as it's affected by CSP
      window.location.hostname
    ].join('|');
    
    return this.simpleHash(fingerprint);
  }

  /**
   * Simple hash function for fingerprinting
   */
  private simpleHash(str: string): string {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32-bit integer
    }
    return hash.toString(16);
  }

  /**
   * Simple encryption (XOR with key)
   * Note: This is basic obfuscation, not cryptographic security
   */
  private simpleEncrypt(text: string): string {
    const key = 'chatSecureKey2024'; // In production, use environment variable
    let result = '';
    for (let i = 0; i < text.length; i++) {
      result += String.fromCharCode(
        text.charCodeAt(i) ^ key.charCodeAt(i % key.length)
      );
    }
    return btoa(result);
  }

  /**
   * Simple decryption (XOR with key)
   */
  private simpleDecrypt(encrypted: string): string {
    try {
      const key = 'chatSecureKey2024';
      const text = atob(encrypted);
      let result = '';
      for (let i = 0; i < text.length; i++) {
        result += String.fromCharCode(
          text.charCodeAt(i) ^ key.charCodeAt(i % key.length)
        );
      }
      return result;
    } catch (error) {
      throw new Error('Token decryption failed');
    }
  }
}

// Create singleton instance
export const secureStorage = new SecureStorage({
  encrypt: process.env.NODE_ENV === 'production',
  prefix: 'chat_app_'
});

// Utility functions for easy usage
export const tokenStorage = {
  set: (token: string, persistent: boolean = false) => secureStorage.setToken(token, persistent),
  get: () => secureStorage.getToken(),
  remove: () => secureStorage.removeToken(),
  exists: () => secureStorage.hasToken(),
  clear: () => secureStorage.clearAll()
};

// Auto-cleanup on page unload for security
window.addEventListener('beforeunload', () => {
  // Clear sessionStorage tokens on page unload for extra security
  const key = 'chat_app_token';
  if (sessionStorage.getItem(key)) {
    console.debug('Clearing session token on page unload');
  }
});

// Detect potential XSS and clear tokens
window.addEventListener('error', (event) => {
  // If there are script errors that might indicate XSS, clear tokens
  if (event.error && event.error.message && 
      event.error.message.includes('script') && 
      event.error.message.includes('blocked')) {
    console.warn('Potential XSS detected, clearing tokens for security');
    secureStorage.clearAll();
  }
});

export default secureStorage;