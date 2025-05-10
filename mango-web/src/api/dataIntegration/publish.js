import request from '@/utils/request';

/**
 * 获取版本详情
 * @param {String} versionId 版本ID
 * @returns {Promise} 请求Promise
 */
export function getVersionDetail(versionId) {
    return request({
        url: `/api/materialize/detail`,
        method: 'get',
        params: {
            versionId
        }
    });
}