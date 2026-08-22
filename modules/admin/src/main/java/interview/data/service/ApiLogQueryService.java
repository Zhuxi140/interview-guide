package interview.data.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import interview.data.model.vo.ApiLogListItemVO;

/**
 * API 访问日志查询服务（热表与归档冷表共用）。
 *
 * @author zhuxi
 */
public interface ApiLogQueryService {

    /**
     * 分页查询 API 访问日志热表。
     *
     * @param page         页码
     * @param size         每页条数
     * @param userId       操作人筛选，可为空
     * @param apiPath      接口路径筛选，可为空
     * @param method       HTTP 动词筛选，可为空
     * @param status       响应状态码筛选，可为空
     * @param startTime    开始时间（ISO-8601 带时区），可为空
     * @param endTime      结束时间（ISO-8601 带时区），可为空
     * @return API 日志分页结果
     */
    IPage<ApiLogListItemVO> pageApiLogs(Integer page, Integer size, Long userId,
                                        String apiPath, String method, Integer status,
                                        String startTime, String endTime);

    /**
     * 查询单条 API 访问日志详情。
     *
     * @param id 日志 ID
     * @return 日志详情
     */
    ApiLogListItemVO getApiLogDetail(Long id);

    /**
     * 分页查询归档 API 日志冷表。
     *
     * @param page      页码
     * @param size      每页条数
     * @param userId    操作人筛选，可为空
     * @param apiPath   接口路径筛选，可为空
     * @param method    HTTP 动词筛选，可为空
     * @param status    响应状态码筛选，可为空
     * @param startTime 开始时间（ISO-8601 带时区），可为空
     * @param endTime   结束时间（ISO-8601 带时区），可为空
     * @return 归档日志分页结果
     */
    IPage<ApiLogListItemVO> pageApiLogArchives(Integer page, Integer size, Long userId,
                                               String apiPath, String method, Integer status,
                                               String startTime, String endTime);
}
