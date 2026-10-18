import { request } from '@/service/request';

export interface MesFile {
  id: string;
  fileName: string;
  sizeBytes: number;
  sha256: string;
}

/** 列出挂在这个 Lot 上的文件。 */
export const getLotFiles = (lotId: string) => {
  return request<MesFile[]>({ url: '/api/v1/mes/files', method: 'get', params: { lotId } });
};

/** 登记文件元数据。摘要必须是 64 位十六进制，大小不能为负。 */
export const registerLotFile = (data: {
  fileName: string;
  contentType?: string;
  sizeBytes: number;
  sha256: string;
  lotId: string;
}) => {
  return request<boolean>({ url: '/api/v1/mes/files', method: 'post', data });
};

/** 写入内容。字节数和 SHA-256 必须与登记一致。 */
export const storeLotFile = (id: string, body: Blob) => {
  return request<boolean>({
    url: `/api/v1/mes/files/${id}/content`,
    method: 'put',
    data: body,
    headers: { 'Content-Type': 'application/octet-stream' }
  });
};
