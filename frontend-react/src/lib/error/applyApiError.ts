import { type FieldValues, type UseFormSetError } from "react-hook-form";

export interface ApiFieldError {
  field: string;
  message: string;
}

export interface ApiErrorPayload {
  code: string;
  message: string;
  fieldErrors?: ApiFieldError[];
  traceId?: string;
}

export function applyApiError<T extends FieldValues>(
  error: unknown,
  setError?: UseFormSetError<T>,
): string {
  if (!error || typeof error !== "object") {
    return "Đã xảy ra lỗi không xác định.";
  }

  const errObj = error as { response?: { data?: ApiErrorPayload } };
  const data = errObj.response?.data;

  if (data?.fieldErrors && Array.isArray(data.fieldErrors) && setError) {
    for (const fe of data.fieldErrors) {
      if (fe.field && fe.message) {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        setError(fe.field as any, {
          type: "server",
          message: fe.message,
        });
      }
    }
  }

  return data?.message || "Hệ thống gặp sự cố. Vui lòng thử lại sau.";
}
