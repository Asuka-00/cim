/** BOM 版本。复制时 BOM 行一并带走。 */
export interface BomVersion {
  id: string;
  bomId: string;
  versionNo: number;
  status: string;
  remark?: string;
}

/** BOM 版本草稿只改备注。 */
export interface BomVersionSaveRequest {
  remark?: string;
}
