declare namespace Api {
  namespace Common {
    type Result<T> = {
      code: number;
      message: string;
      data: T;
      errorParams?: Record<string, string>;
    };

    type PageDTO<T> = {
      records: T[];
      total: number;
      size: number;
      current: number;
      pages: number;
    };

    /** Alias expected by the shared `useTable` hook */
    type PaginatingQueryRecord<T> = PageDTO<T>;

    interface BaseVO {
      id: string;
      tenantId: string;
      createBy: string;
      createAt: string;
      updateBy: string;
      updateAt: string;
    }

    type CommonState = 'ENABLED' | 'DISABLED';
    type AdminFlag = 'NOT_ADMIN' | 'IS_ADMIN' | 'SUPER_ADMIN';
    type PermissionType = 'MENU' | 'BUTTON' | 'API';
    type VisibilityState = 'HIDDEN' | 'VISIBLE';
  }
}
