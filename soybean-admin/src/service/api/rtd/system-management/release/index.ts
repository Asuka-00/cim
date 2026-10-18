import type { ReleaseActivateResultVO, ReleaseCurrentVO, ReleaseInfoVO } from './types';
import { request } from '@/service/request';

export type { ReleaseActivateResultVO, ReleaseCurrentVO, ReleaseInfoVO } from './types';

/** 当前激活版本 */
export const getCurrentRelease = () => {
  return request<ReleaseCurrentVO>({ url: '/api/v1/system/releases/current', method: 'get' });
};

/** 已安装版本列表 */
export const getReleaseList = () => {
  return request<ReleaseInfoVO[]>({ url: '/api/v1/system/releases', method: 'get' });
};

/** 上传 zip 发布包 */
export const uploadReleasePackage = (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return request<ReleaseInfoVO>({
    url: '/api/v1/system/releases',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 600000
  });
};

/** 激活指定版本（会触发进程重启） */
export const activateRelease = (version: string) => {
  return request<ReleaseActivateResultVO>({ url: `/api/v1/system/releases/${version}/activate`, method: 'post' });
};

/** 回滚到 previous */
export const rollbackRelease = () => {
  return request<ReleaseActivateResultVO>({ url: '/api/v1/system/releases/rollback', method: 'post' });
};
