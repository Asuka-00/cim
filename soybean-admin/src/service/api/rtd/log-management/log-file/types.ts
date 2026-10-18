export type LogFileType = 'app' | 'audit' | 'sql' | 'error' | 'slow';
export type LogPreviewLineCount = number | 'ALL';

export interface LogFileInfoVO {
  fileName: string;
  type: LogFileType;
  size: number;
  lastModifiedAt: string;
  compressed: boolean;
}

export interface LogTailVO {
  fileName: string;
  type: LogFileType;
  lines: number;
  content: string[];
}
