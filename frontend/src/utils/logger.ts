/**
 * Production-safe logging utility
 * Automatically disables console output in production builds
 */
class Logger {
  private isDevelopment = process.env.NODE_ENV === 'development';

  debug(message: string, ...args: unknown[]): void {
    if (this.isDevelopment) {
      console.debug(`[DEBUG] ${message}`, ...args);
    }
  }

  info(message: string, ...args: unknown[]): void {
    if (this.isDevelopment) {
      console.info(`[INFO] ${message}`, ...args);
    }
  }

  warn(message: string, ...args: unknown[]): void {
    if (this.isDevelopment) {
      console.warn(`[WARN] ${message}`, ...args);
    }
  }

  error(message: string, ...args: unknown[]): void {
    // Always log errors, even in production
    console.error(`[ERROR] ${message}`, ...args);
  }

  log(message: string, ...args: unknown[]): void {
    if (this.isDevelopment) {
      console.log(`[LOG] ${message}`, ...args);
    }
  }
}

export const logger = new Logger();