/** Time Link。约束从起点 Track-Out 到终点 Track-In 的最长间隔。 */
export interface RouteTimeLink {
  id: string;
  routeVersionId: string;
  fromSeqNo: number;
  toSeqNo: number;
  maxMinutes: number;
  warnMinutes?: number | null;
  action: string;
  reasonCode?: string;
}

/** Time Link 保存请求。HOLD 必须带原因码。 */
export interface RouteTimeLinkSaveRequest {
  fromSeqNo: number | null;
  toSeqNo: number | null;
  maxMinutes: number | null;
  warnMinutes?: number | null;
  action: string;
  reasonCode?: string;
}
