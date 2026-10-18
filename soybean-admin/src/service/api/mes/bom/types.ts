/** BOM 主数据。版本和行另查。 */
export interface Bom {
  id: string;
  bomCode: string;
  bomName: string;
  description?: string;
  enabled: number;
}

/** BOM 保存请求。 */
export interface BomSaveRequest {
  bomCode: string;
  bomName: string;
  description?: string;
  enabled: number;
}
