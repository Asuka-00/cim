import { request } from '@/service/request';

export interface ImportTask {
  id: string;
  taskNo: string;
  importType: string;
  mode?: string;
  onError?: string;
  status: string;
  totalRows?: number;
  successRows?: number;
  failRows?: number;
  fileRef?: string;
  operator?: string;
  startedAt?: string;
}

/** 列出正在运行的任务，按任务号排列。 */
export const getRunningImportTasks = () => {
  return request<ImportTask[]>({ url: '/api/v1/mes/import-tasks/running', method: 'get' });
};

/** 按操作人列出任务。状态留空表示该操作人的全部任务。 */
export const getImportTasksByOperator = (operator: string, status?: string) => {
  return request<ImportTask[]>({
    url: '/api/v1/mes/import-tasks/by-operator',
    method: 'get',
    params: status ? { operator, status } : { operator }
  });
};

/** 按状态列出任务。类型留空表示该状态的全部类型。 */
export const getImportTasksByStatus = (status: string, importType?: string) => {
  return request<ImportTask[]>({
    url: '/api/v1/mes/import-tasks/by-status',
    method: 'get',
    params: importType ? { status, importType } : { status }
  });
};

/** 按任务号查询一条导入任务。任务号不能超过 64 字。 */
export const getImportTask = (taskNo: string) => {
  return request<ImportTask>({ url: '/api/v1/mes/import-tasks', method: 'get', params: { taskNo } });
};

/** 作废草稿。不是草稿会拒绝。 */
export const cancelImportTask = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/import-tasks/${id}/cancel`, method: 'post' });
};

/** 撤回待处理任务，变回草稿。不是待处理会拒绝。 */
export const withdrawImportTask = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/import-tasks/${id}/withdraw`, method: 'post' });
};

/** 结束运行中的任务。成功行数加失败行数必须等于总行数。 */
export const finishImportTask = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/import-tasks/${id}/finish`, method: 'post' });
};

/** 给运行中的任务记一条失败行。同一行号不能记两次。 */
export const recordImportFailure = (
  id: string,
  data: { rowNo: number; message: string; errorCode?: string; rawData?: string }
) => {
  return request<boolean>({ url: `/api/v1/mes/import-tasks/${id}/failures`, method: 'post', data });
};

/** 给运行中的任务记一条成功行。只校验的任务会拒绝。 */
export const recordImportSuccess = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/import-tasks/${id}/successes`, method: 'post' });
};

export interface WipImportRow {
  woId: string;
  rowNo: number;
  row: { lotNo: string; qty: number; seqNo: number; stepId: string };
}

/** 往运行中的导入任务写一行在制。 */
export const importWipRow = (taskId: string, data: WipImportRow) => {
  return request<boolean>({ url: `/api/v1/mes/import-tasks/${taskId}/wip-rows`, method: 'post', data });
};

/** 一次写入多行。某一行失败时，ROLLBACK 任务会停在这一行。 */
export const importWipRows = (taskId: string, rows: WipImportRow[]) => {
  return request<boolean>({ url: `/api/v1/mes/import-tasks/${taskId}/wip-rows/batch`, method: 'post', data: rows });
};

export interface ImportErrorRow {
  id: string;
  taskId?: string;
  rowNo?: number;
  message?: string;
  errorCode?: string;
  rawData?: string;
}

/** 列出这个任务的失败行，按行号排列。 */
export const getImportFailures = (taskId: string) => {
  return request<ImportErrorRow[]>({ url: `/api/v1/mes/import-tasks/${taskId}/failures`, method: 'get' });
};

/** 按主键查询一条失败行。 */
export const getImportFailure = (errorId: string) => {
  return request<ImportErrorRow>({ url: `/api/v1/mes/import-tasks/errors/${errorId}`, method: 'get' });
};

/** 按类型列出导入任务。 */
export const getImportTasksByType = (importType: string) => {
  return request<ImportTask[]>({ url: '/api/v1/mes/import-tasks/by-type', method: 'get', params: { importType } });
};

/** 新建导入草稿。 */
export const createImportTask = (data: {
  taskNo: string;
  importType: string;
  mode?: string;
  onError?: string;
  fileRef?: string;
}) => {
  return request<boolean>({ url: '/api/v1/mes/import-tasks', method: 'post', data });
};

/** 提交草稿。 */
export const submitImportTask = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/import-tasks/${id}/submit`, method: 'post' });
};

/** 开始待处理任务。 */
export const startImportTask = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/import-tasks/${id}/start`, method: 'post' });
};

/** 按任务上的 CSV 文件导入期初在制。任务须为 RUNNING。 */
export const importWipFile = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/import-tasks/${id}/wip-file`, method: 'post' });
};
