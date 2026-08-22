package interview.code.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.code.model.entity.CodeQuestion;
import interview.code.model.req.AdminCodeQuestionSearchReq;
import interview.code.model.req.CodeQuestionCreateReq;
import interview.code.model.req.CodeQuestionUpdateReq;
import interview.code.model.req.EnterpriseCodeQuestionSearchReq;
import interview.code.model.vo.CodeQuestionCreateVO;
import interview.code.model.vo.CodeQuestionDetailVO;
import interview.code.model.vo.CodeQuestionListItemVO;
import interview.code.model.vo.CodeQuestionUpdateVO;
import interview.code.model.vo.SessionCodeQuestionVO;

import java.util.List;
import java.util.Map;

/**
 * 编程题库服务：平台全局题管理、企业私有题管理与候选人会话题目读取。
 */
public interface CodeQuestionService extends IService<CodeQuestion> {

    /**
     * 创建平台全局编程题
     * @param req 创建请求
     * @return 创建结果（可见性固定为 GLOBAL）
     */
    CodeQuestionCreateVO createPlatformQuestion(CodeQuestionCreateReq req);

    /**
     * 分页查询平台全局编程题
     * @param req 分页查询参数
     * @return 全局题分页
     */
    IPage<CodeQuestionListItemVO> pagePlatformQuestions(AdminCodeQuestionSearchReq req);

    /**
     * 查询平台全局编程题详情（含全部测试用例）
     * @param questionId 题目 ID
     * @return 题目详情
     */
    CodeQuestionDetailVO getPlatformQuestionDetail(Long questionId);

    /**
     * 部分更新平台全局编程题
     * @param questionId 题目 ID
     * @param req 更新请求（含期望版本）
     * @return 更新结果
     */
    CodeQuestionUpdateVO updatePlatformQuestion(Long questionId, CodeQuestionUpdateReq req);

    /**
     * 删除平台全局编程题（乐观锁 + 逻辑删除）
     * @param questionId 题目 ID
     * @param expectedVersion 期望版本号（If-Match）
     */
    void deletePlatformQuestion(Long questionId, Integer expectedVersion);

    /**
     * 创建企业私有编程题
     * @param enterpriseId 企业 ID（服务端确定，不接受请求体传入）
     * @param req 创建请求
     * @return 创建结果（可见性固定为 PRIVATE）
     */
    CodeQuestionCreateVO createEnterpriseQuestion(Long enterpriseId, CodeQuestionCreateReq req);

    /**
     * 分页查询企业可管理的编程题（本企业私有题 + 可用全局题）
     * @param enterpriseId 企业 ID
     * @param req 分页查询参数
     * @return 题目分页
     */
    IPage<CodeQuestionListItemVO> pageEnterpriseQuestions(Long enterpriseId, EnterpriseCodeQuestionSearchReq req);

    /**
     * 查询企业有权管理的题目详情（含全部测试用例）
     * @param enterpriseId 企业 ID
     * @param questionId 题目 ID
     * @return 题目详情
     */
    CodeQuestionDetailVO getEnterpriseQuestionDetail(Long enterpriseId, Long questionId);

    /**
     * 部分更新企业私有编程题（全局题仅平台管理端可维护）
     * @param enterpriseId 企业 ID
     * @param questionId 题目 ID
     * @param req 更新请求（含期望版本）
     * @return 更新结果
     */
    CodeQuestionUpdateVO updateEnterpriseQuestion(Long enterpriseId, Long questionId, CodeQuestionUpdateReq req);

    /**
     * 删除企业私有编程题（乐观锁 + 逻辑删除）
     * @param enterpriseId 企业 ID
     * @param questionId 题目 ID
     * @param expectedVersion 期望版本号（If-Match）
     */
    void deleteEnterpriseQuestion(Long enterpriseId, Long questionId, Integer expectedVersion);

    /**
     * 候选人读取当前会话分配的题目（公开用例可见，隐藏用例不返回）
     * @param sessionId 会话 ID
     * @param questionId 题目 ID
     * @return 会话题目
     */
    SessionCodeQuestionVO getSessionQuestion(Long sessionId, Long questionId);

    /**
     * 批量查询题目标题（单表 IN 查询，供提交列表补齐展示字段）
     * @param questionIds 题目 ID 列表
     * @return 题目 ID → 标题映射
     */
    Map<Long, String> getQuestionTitlesByIds(List<Long> questionIds);
}
