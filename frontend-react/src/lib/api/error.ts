import axios from "axios";

export interface ApiFieldError {
  field: string;
  code?: string;
  message: string;
}

export interface ApiErrorResponse {
  code?: string;
  message?: string;
  fieldErrors?: ApiFieldError[];
}

export interface ApplyApiErrorOptions {
  setFieldError?: (field: string, message: string) => void;
  onToast?: (message: string) => void;
  fallbackMessage?: string;
}

export function extractApiError(error: unknown): ApiErrorResponse | null {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data;
    if (data && typeof data === "object") {
      return data as ApiErrorResponse;
    }
  }
  if (error && typeof error === "object" && "response" in error) {
    const response = (error as { response?: { data?: unknown } }).response;
    if (response?.data && typeof response.data === "object") {
      return response.data as ApiErrorResponse;
    }
  }
  if (error && typeof error === "object" && ("code" in error || "message" in error || "fieldErrors" in error)) {
    return error as ApiErrorResponse;
  }
  return null;
}

export function applyApiError(error: unknown, options: ApplyApiErrorOptions = {}): string {
  const { setFieldError, onToast, fallbackMessage = "Đã xảy ra lỗi. Vui lòng thử lại." } = options;
  const parsed = extractApiError(error);

  if (parsed?.fieldErrors && Array.isArray(parsed.fieldErrors) && setFieldError) {
    for (const item of parsed.fieldErrors) {
      if (item.field && item.message) {
        setFieldError(item.field, item.message);
      }
    }
  }

  const message =
    parsed?.message ||
    (error instanceof Error && error.message ? error.message : null) ||
    fallbackMessage;

  if (onToast) {
    onToast(message);
  }
  return message;
}
