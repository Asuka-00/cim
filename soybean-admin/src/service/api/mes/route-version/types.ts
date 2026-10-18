/** Route 版本。步骤、Time Link 和步骤 Recipe 随复制一起带走。 */
export interface RouteVersion {
  id: string;
  routeId: string;
  versionNo: number;
  status: string;
  remark?: string;
}

/** Route 版本草稿只改备注。 */
export interface RouteVersionSaveRequest {
  remark?: string;
}
