/*
 * @Description:
 */
import {postRequest, getRequest, getDownload} from '/@/lib/axios';

export const goodsApi = {
  // 添加商品
  addGoods: (param) => {
    return postRequest('/goods/add', param);
  },
  // 删除
  deleteGoods: (goodsId) => {
    return getRequest(`/goods/delete/${goodsId}`);
  },
  // 批量
  batchDelete: (goodsIdList) => {
    return postRequest('/goods/batchDelete', goodsIdList);
  },
  // 分页查询
  queryGoodsList: (param) => {
    return postRequest('/goods/query', param);
  },
  // 更新商品
  updateGoods: (param) => {
    return postRequest('/goods/update', param);
  },

  // 导入
  importGoods : (file) =>{
    return postRequest('/goods/importGoods',file);
  },

  // 导出
  exportGoods : () =>{
    return getDownload('/goods/exportGoods');
  }
};
