import { request } from '@/service/request';

export interface Shift {
  id: string;
  shiftCode: string;
  shiftName: string;
  startTime: string;
  endTime: string;
  crossDay?: number;
}

/** 列出全部班次。 */
export const getShifts = () => {
  return request<Shift[]>({ url: '/api/v1/mes/shifts', method: 'get' });
};

/** 新建班次。结束时间不晚于开始时间时记为跨日。 */
export const createShift = (data: { shiftCode: string; shiftName: string; startTime: string; endTime: string }) => {
  return request<boolean>({ url: '/api/v1/mes/shifts', method: 'post', data });
};
