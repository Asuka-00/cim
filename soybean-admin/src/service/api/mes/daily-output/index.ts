import { request } from '@/service/request';

export interface DailyOutput {
  lotCount: number;
  qty: number;
  scrapQty: number;
}

export interface ProductOutput {
  productId?: string | null;
  lotCount: number;
  qty: number;
  scrapQty: number;
}

/** 汇总某一天完成的 Lot 数、数量和报废数量。 */
export const getDailyOutput = (date: string) => {
  return request<DailyOutput>({ url: '/api/v1/mes/reports/daily-output', method: 'get', params: { date } });
};

/** 按产品汇总某一天的完工量。 */
export const getDailyOutputByProduct = (date: string) => {
  return request<ProductOutput[]>({ url: '/api/v1/mes/reports/daily-output/by-product', method: 'get', params: { date } });
};
