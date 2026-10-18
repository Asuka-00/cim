/** Route 主数据。版本在抽屉里另查。 */
export interface Route {
  id: string;
  routeCode: string;
  routeName: string;
  description?: string;
  enabled: number;
}

/** Route 保存请求。 */
export interface RouteSaveRequest {
  routeCode: string;
  routeName: string;
  description?: string;
  enabled: number;
}
