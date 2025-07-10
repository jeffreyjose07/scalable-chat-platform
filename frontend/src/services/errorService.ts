import toast from 'react-hot-toast';

export enum ErrorType {
  NETWORK = 'NetworkError',
  AUTHENTICATION = 'AuthenticationError',
  VALIDATION = 'ValidationError',
  PERMISSION = 'PermissionError',
  NOT_FOUND = 'NotFoundError',
  SERVER = 'ServerError',
  UNKNOWN = 'UnknownError'
}

export interface ApiError {
  message: string;
  code: string;
  type: ErrorType;
  details?: Record<string, unknown>;
  statusCode?: number;
}

export class ErrorService {
  /**
   * Handle errors with user notification and logging
   */
  static handleError(error: Error | ApiError, context: string): void {
    // Log error for debugging
    console.error(`Error in ${context}:`, error);

    // Determine error type and show appropriate message
    const userMessage = this.getUserFriendlyMessage(error);
    
    // Show toast notification
    toast.error(userMessage, {
      duration: 5000,
      position: 'top-right',
    });

    // Report error in production
    if (process.env.NODE_ENV === 'production') {
      this.reportError(error, context);
    }
  }

  /**
   * Handle errors silently (no user notification)
   */
  static logError(error: Error | ApiError, context: string): void {
    console.error(`Silent error in ${context}:`, error);
    
    if (process.env.NODE_ENV === 'production') {
      this.reportError(error, context);
    }
  }

  /**
   * Convert technical errors to user-friendly messages
   */
  private static getUserFriendlyMessage(error: Error | ApiError): string {
    if (this.isApiError(error)) {
      switch (error.type) {
        case ErrorType.NETWORK:
          return 'Connection problem. Please check your internet and try again.';
        case ErrorType.AUTHENTICATION:
          return 'Please log in again to continue.';
        case ErrorType.VALIDATION:
          return error.message || 'Please check your input and try again.';
        case ErrorType.PERMISSION:
          return "You don't have permission to perform this action.";
        case ErrorType.NOT_FOUND:
          return 'The requested information could not be found.';
        case ErrorType.SERVER:
          return 'Server error. Please try again in a moment.';
        default:
          return 'Something went wrong. Please try again.';
      }
    }

    // Handle standard JavaScript errors
    if (error.name === 'TypeError') {
      return 'Something went wrong. Please refresh the page.';
    }
    
    if (error.name === 'NetworkError' || error.message.includes('fetch')) {
      return 'Connection problem. Please check your internet and try again.';
    }

    return 'An unexpected error occurred. Please try again.';
  }

  /**
   * Create API error from response
   */
  static createApiError(
    message: string,
    type: ErrorType = ErrorType.UNKNOWN,
    statusCode?: number,
    details?: Record<string, unknown>
  ): ApiError {
    return {
      message,
      code: type,
      type,
      statusCode,
      details,
    };
  }

  /**
   * Parse error from fetch response
   */
  static async parseApiError(response: Response): Promise<ApiError> {
    try {
      const data = await response.json();
      return this.createApiError(
        data.message || 'API Error',
        this.mapStatusToErrorType(response.status),
        response.status,
        data
      );
    } catch {
      return this.createApiError(
        `HTTP ${response.status}: ${response.statusText}`,
        this.mapStatusToErrorType(response.status),
        response.status
      );
    }
  }

  /**
   * Map HTTP status codes to error types
   */
  private static mapStatusToErrorType(status: number): ErrorType {
    if (status >= 400 && status < 500) {
      switch (status) {
        case 401:
          return ErrorType.AUTHENTICATION;
        case 403:
          return ErrorType.PERMISSION;
        case 404:
          return ErrorType.NOT_FOUND;
        case 422:
          return ErrorType.VALIDATION;
        default:
          return ErrorType.VALIDATION;
      }
    }
    
    if (status >= 500) {
      return ErrorType.SERVER;
    }
    
    return ErrorType.UNKNOWN;
  }

  /**
   * Type guard for API errors
   */
  private static isApiError(error: Error | ApiError): error is ApiError {
    return 'type' in error && 'code' in error;
  }

  /**
   * Report error to monitoring service (placeholder)
   */
  private static reportError(error: Error | ApiError, context: string): void {
    // In production, integrate with error monitoring service like Sentry
    // Example:
    // Sentry.captureException(error, {
    //   tags: { context },
    //   level: 'error'
    // });
    
    console.warn('Error reporting not configured:', { error, context });
  }

  /**
   * Create error handler for async operations
   */
  static createAsyncErrorHandler(context: string) {
    return (error: Error | ApiError) => {
      this.handleError(error, context);
      throw error; // Re-throw for component handling if needed
    };
  }

  /**
   * Wrapper for API calls with automatic error handling
   */
  static async handleApiCall<T>(
    apiCall: () => Promise<T>,
    context: string,
    options: { silent?: boolean } = {}
  ): Promise<T> {
    try {
      return await apiCall();
    } catch (error) {
      if (options.silent) {
        this.logError(error as Error, context);
      } else {
        this.handleError(error as Error, context);
      }
      throw error;
    }
  }
}

// Utility hooks for error handling
export const useErrorHandler = (context: string) => {
  return {
    handleError: (error: Error | ApiError) => ErrorService.handleError(error, context),
    logError: (error: Error | ApiError) => ErrorService.logError(error, context),
    createAsyncHandler: () => ErrorService.createAsyncErrorHandler(context),
  };
};