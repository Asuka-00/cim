import { request } from '@/service/request';

export interface UdaDef {
  id: string;
  objectType: string;
  attrCode: string;
  attrName: string;
  dataType: string;
  optionsJson?: string;
  required?: number;
  enabled?: number;
}

/** 列出某个对象类型的扩展字段定义，按编码排序。 */
export const getUdaDefs = (objectType: string) => {
  return request<UdaDef[]>({ url: '/api/v1/mes/uda-defs', method: 'get', params: { objectType } });
};

/** 新建定义。数据类型空按 TEXT。枚举选项用逗号分隔。 */
export const createUdaDef = (data: {
  objectType: string;
  attrCode: string;
  attrName: string;
  dataType?: string;
  options?: string;
  required?: number;
}) => {
  return request<boolean>({ url: '/api/v1/mes/uda-defs', method: 'post', data });
};
