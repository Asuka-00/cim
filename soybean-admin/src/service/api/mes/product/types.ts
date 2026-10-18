/** 产品。id 和外键为雪花短整型，接口按 JSON 数字返回。 */
export interface Product {
  id: string;
  productCode: string;
  productName: string;
  productType: string;
  familyId?: string | null;
  customerId?: string | null;
  customerPartNo?: string;
  waferSizeMm?: number | null;
  grossDiePerWafer?: number | null;
  baseUnitId: string;
  remark?: string;
  enabled: number;
}

/** 新增或修改产品。 */
export interface ProductSaveRequest {
  productCode: string;
  productName: string;
  productType?: string;
  familyId?: string | null;
  customerId?: string | null;
  customerPartNo?: string;
  waferSizeMm?: number | null;
  grossDiePerWafer?: number | null;
  baseUnitId: string;
  remark?: string;
  enabled?: number;
}
