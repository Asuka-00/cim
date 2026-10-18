import { request } from '@/service/request';
import type { PageDTO } from '@/service/api/common';

export interface MesTxn {
  id: string;
  txnNo?: string;
  txnType?: string;
  objectType?: string;
  objectId?: string;
  objectCode?: string;
  source?: string;
  reasonCode?: string;
  operator?: string;
  result?: string;
  errorMessage?: string;
  payloadJson?: string;
  remark?: string;
  txnTime?: string;
}

/** 分页查询事务流水。对象主键留空表示不按对象过滤。 */
export const getTxnPage = (params: { page: number; pageSize: number; txnType?: string; objectType?: string; objectId?: string }) => {
  return request<PageDTO<MesTxn>>({ url: '/api/v1/mes/transactions', method: 'get', params });
};

/** 按主键查询一笔事务。不存在会报错。 */
export const getTxn = (id: string) => {
  return request<MesTxn>({ url: `/api/v1/mes/transactions/${id}`, method: 'get' });
};
