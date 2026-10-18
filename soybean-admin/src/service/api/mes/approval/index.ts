import { request } from '@/service/request';

/** 待我审批的提交。已通过或驳回的不再出现。 */
export interface ApprovalTodo {
  id: string;
  bizType: string;
  bizId: string;
  assignee?: string;
  operator?: string;
  comment?: string;
  actionAt?: string;
}

/** 查询当前用户待审批的提交。 */
export const getMyApprovals = () => {
  return request<ApprovalTodo[]>({ url: '/api/v1/mes/approvals', method: 'get' });
};
