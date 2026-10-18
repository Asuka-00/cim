/** 产品版本。绑定 Route 和 BOM 主数据，不绑定具体版本。 */
export interface ProductVersion {
  id: string;
  productId: string;
  versionNo: number;
  status: string;
  routeId?: string | null;
  bomId?: string | null;
  remark?: string;
}

/** 产品版本草稿保存请求。 */
export interface ProductVersionSaveRequest {
  routeId?: string | null;
  bomId?: string | null;
  remark?: string;
}
