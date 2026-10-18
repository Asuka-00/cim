import { request } from '@/service/request';

export interface UnitSerial {
  id: string;
  serialNo: string;
  lotId?: string;
  stripId?: string | null;
  containerId?: string | null;
  position?: string;
  status?: string;
}

/** 列出某个 Lot 的序列号。 */
export const getUnitSerials = (lotId: string) => {
  return request<UnitSerial[]>({ url: `/api/v1/mes/lots/${lotId}/unit-serials`, method: 'get' });
};

/** 登记一个序列号。产品未打开跟踪时会被拒绝。 */
export const assignUnitSerial = (
  lotId: string,
  data: { serialNo: string; stripId?: string | null; containerId?: string | null; position?: string }
) => {
  return request<boolean>({ url: `/api/v1/mes/lots/${lotId}/unit-serials`, method: 'post', data });
};
