export interface OperationLogVO {
  id: string;
  tenantId: string;
  traceId: string;
  userName: string;
  operation: string;
  className: string;
  methodName: string;
  requestUri: string;
  requestMethod: string;
  requestIp: string;
  userAgent: string;
  requestPayload: string;
  responsePayload: string;
  success: boolean;
  durationMs: number;
  errorType: string;
  errorMessage: string;
  createBy: string;
  createAt: string;
}

export interface OperationLogQuery {
  traceId?: string;
  userName?: string;
  operation?: string;
  className?: string;
  methodName?: string;
  requestUri?: string;
  requestMethod?: string;
  success?: boolean;
  createAtStart?: string;
  createAtEnd?: string;
}
