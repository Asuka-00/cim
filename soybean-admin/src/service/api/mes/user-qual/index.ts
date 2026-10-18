import { request } from '@/service/request';

export interface UserQual {
  id: string;
  userName: string;
  targetType: string;
  targetId: string;
  status: string;
  validTo?: string | null;
}

/** 列出全部人员资质。 */
export const getUserQuals = () => {
  return request<UserQual[]>({ url: '/api/v1/mes/user-quals', method: 'get' });
};

/** 新建有效资质。生效止空表示不限期。 */
export const createUserQual = (data: { userName: string; targetType: string; targetId: string; validTo?: string | null }) => {
  return request<boolean>({ url: '/api/v1/mes/user-quals', method: 'post', data });
};

/** 把已过生效止的有效资质标成过期。不传时间则用服务器当前时间。 */
export const expireUserQuals = () => {
  return request<number>({ url: '/api/v1/mes/user-quals/expire-due', method: 'post' });
};
