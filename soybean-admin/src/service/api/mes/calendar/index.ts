import { request } from '@/service/request';

export interface FactoryCalendar {
  id: string;
  calDate: string;
  isWorkday: number;
  shiftCodes?: string | null;
}

/** 按日期查询。这一天还没有日历时接口会报错。 */
export const getFactoryCalendar = (date: string) => {
  return request<FactoryCalendar>({ url: '/api/v1/mes/calendars', method: 'get', params: { date } });
};

/** 新建一天。同一天只能有一行。 */
export const createFactoryCalendar = (data: { calDate: string; isWorkday: number }) => {
  return request<boolean>({ url: '/api/v1/mes/calendars', method: 'post', data });
};

/** 给这一天安排班次。编码必须已经存在，逗号分隔。 */
export const assignCalendarShifts = (id: string, data: { shiftCodes: string }) => {
  return request<boolean>({ url: `/api/v1/mes/calendars/${id}/shifts`, method: 'post', data });
};
