/**
 * 系统更新日志 api 封装
 */
import { uploadRequest } from '@/lib/smart-request';

export const fileApi = {
  upload: (file, folder) => {
    return uploadRequest(file, folder);
  },
};
