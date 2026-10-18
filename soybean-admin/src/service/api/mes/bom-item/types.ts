/** BOM 行。routeSeqNo 为 0 表示该站点每次经过都消耗。 */
export interface BomItem {
  id: string;
  bomVersionId: string;
  lineNo: number;
  materialId: string;
  qtyPer: number;
  uomId: string;
  consumeMode: string;
  stepId: string;
  routeSeqNo: number;
  backflush: number;
  substituteGroup?: string;
  substitutePolicy?: string;
}

/** BOM 行保存请求。用量必须大于 0。 */
export interface BomItemSaveRequest {
  lineNo: number | null;
  materialId: string | null;
  qtyPer: number | null;
  uomId: string | null;
  consumeMode: string;
  stepId: string | null;
  routeSeqNo: number | null;
  backflush: number;
  substituteGroup?: string;
  substitutePolicy: string;
}
