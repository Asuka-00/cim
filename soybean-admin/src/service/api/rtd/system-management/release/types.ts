export interface ReleaseInfoVO {
  version: string;
  buildTime?: string;
  gitCommit?: string;
  jarSha256?: string;
  jarSizeBytes?: number;
  current?: boolean;
  previous?: boolean;
}

export interface ReleaseCurrentVO {
  currentVersion?: string;
  buildTime?: string;
  gitCommit?: string;
  jarSha256?: string;
  previousVersion?: string;
  runningVersion?: string;
}

export interface ReleaseActivateResultVO {
  version: string;
  previousVersion?: string;
  restartDelayMs: number;
  message?: string;
}
