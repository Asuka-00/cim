import { request } from '@/service/request';

export interface PrintTemplate {
  id: string;
  tplCode: string;
  tplName: string;
  tplType: string;
  content?: string;
  isDefault?: number;
  enabled?: number;
}

/** 按类型列出模板。 */
export const getPrintTemplates = (type: string) => {
  return request<PrintTemplate[]>({ url: '/api/v1/mes/print-templates', method: 'get', params: { type } });
};

/** 新建模板。新建时启用，不是默认。 */
export const createPrintTemplate = (data: { tplCode: string; tplName: string; tplType: string; content: string }) => {
  return request<boolean>({ url: '/api/v1/mes/print-templates', method: 'post', data });
};

/** 设成该类型的默认模板。停用的不能设。 */
export const setDefaultPrintTemplate = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/print-templates/${id}/default`, method: 'post' });
};

/** 停用模板，并取消默认。 */
export const disablePrintTemplate = (id: string) => {
  return request<boolean>({ url: `/api/v1/mes/print-templates/${id}/disable`, method: 'post' });
};

export interface PrintTemplateDefault {
  tplCode: string;
  tplName: string;
  content: string;
}

/** 按类型查出已启用的默认模板。这个类型没有默认模板时会报错。 */
export const getDefaultPrintTemplate = (type: string) => {
  return request<PrintTemplateDefault>({ url: '/api/v1/mes/print-templates/default', method: 'get', params: { type } });
};
