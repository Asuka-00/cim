/** 物料。基础计量单位用 mes_uom。 */
export interface Material {
  id: string;
  materialCode: string;
  materialName: string;
  materialType: string;
  spec?: string;
  baseUomId: string;
  isCritical: number;
  shelfLifeDays?: number | null;
  needIncomingInspect: number;
  remark?: string;
  enabled: number;
}

/** 新增或修改物料。 */
export interface MaterialSaveRequest {
  materialCode: string;
  materialName: string;
  materialType?: string;
  spec?: string;
  baseUomId: string;
  isCritical?: number;
  shelfLifeDays?: number | null;
  needIncomingInspect?: number;
  remark?: string;
  enabled?: number;
}
