import { request } from '@/service/request';

export interface Ecn {
  id: string;
  ecnNo: string;
  title: string;
  changeType: string;
  reason?: string;
  status: string;
  applicant?: string;
  approver?: string;
}

/** 列出全部变更申请。 */
export const getEcns = () => {
  return request<Ecn[]>({ url: '/api/v1/mes/ecns', method: 'get' });
};

/** 新建草稿。 */
export const createEcn = (data: { ecnNo: string; title: string; changeType: string; reason: string; applicant?: string }) => {
  return request<boolean>({ url: '/api/v1/mes/ecns', method: 'post', data });
};

/** 把草稿提交成评审中。 */
export const submitEcn = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/ecns/${id}/submit`, method: 'post' });
};

/** 批准评审中的申请。 */
export const approveEcn = (id: string, data: { approver: string }) => {
  return request<boolean>({ url: `/api/v1/mes/ecns/${id}/approve`, method: 'post', data });
};

/** 驳回评审中的申请。 */
export const rejectEcn = (id: string, data: { approver: string }) => {
  return request<boolean>({ url: `/api/v1/mes/ecns/${id}/reject`, method: 'post', data });
};

/** 让已批准的申请生效。 */
export const effectEcn = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/ecns/${id}/effect`, method: 'post' });
};

/** 取消草稿。 */
export const cancelEcn = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/ecns/${id}/cancel`, method: 'post' });
};
