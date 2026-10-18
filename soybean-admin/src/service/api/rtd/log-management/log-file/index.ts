import type { LogFileInfoVO, LogPreviewLineCount, LogFileType, LogTailVO } from './types';
import { request } from '@/service/request';

export type { LogFileInfoVO, LogPreviewLineCount, LogFileType, LogTailVO } from './types';

/** 查询日志文件列表 */
export const getLogFileList = (type: LogFileType) => {
  return request<LogFileInfoVO[]>({ url: '/api/v1/audit/log-files', method: 'get', params: { type } });
};

/** 查看日志文件尾部 */
export const getLogFileTail = (type: LogFileType, fileName: string, lines: LogPreviewLineCount) => {
  return request<LogTailVO>({
    url: `/api/v1/audit/log-files/${type}/tail`,
    method: 'get',
    params: { fileName, lines }
  });
};

/** 下载日志文件 */
export const downloadLogFile = (type: LogFileType, fileName: string) => {
  return request<Blob, 'blob'>({
    url: `/api/v1/audit/log-files/${type}/download`,
    method: 'get',
    params: { fileName },
    responseType: 'blob'
  });
};
