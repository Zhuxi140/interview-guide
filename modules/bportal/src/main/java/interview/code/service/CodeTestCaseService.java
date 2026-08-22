package interview.code.service;

import com.baomidou.mybatisplus.extension.service.IService;
import interview.code.model.entity.CodeTestCase;
import interview.code.model.req.CodeTestCaseCreateReq;
import interview.code.model.req.CodeTestCaseUpdateReq;
import interview.code.model.vo.CodeTestCaseCreateVO;
import interview.code.model.vo.CodeTestCaseUpdateVO;

/**
 * 编程题测试用例服务：平台全局题与企业私有题的用例维护。
 */
public interface CodeTestCaseService extends IService<CodeTestCase> {

    /**
     * 为平台全局题添加测试用例
     * @param questionId 题目 ID
     * @param req 创建请求
     * @return 创建结果
     */
    CodeTestCaseCreateVO createPlatformTestCase(Long questionId, CodeTestCaseCreateReq req);

    /**
     * 更新平台全局题测试用例
     * @param questionId 题目 ID
     * @param caseId 用例 ID
     * @param req 更新请求（含期望版本）
     * @return 更新结果
     */
    CodeTestCaseUpdateVO updatePlatformTestCase(Long questionId, Long caseId, CodeTestCaseUpdateReq req);

    /**
     * 删除平台全局题测试用例（乐观锁 + 逻辑删除）
     * @param questionId 题目 ID
     * @param caseId 用例 ID
     * @param expectedVersion 期望版本号（If-Match）
     */
    void deletePlatformTestCase(Long questionId, Long caseId, Integer expectedVersion);

    /**
     * 为企业私有题添加测试用例
     * @param enterpriseId 企业 ID（服务端确定，不接受请求体传入）
     * @param questionId 题目 ID
     * @param req 创建请求
     * @return 创建结果
     */
    CodeTestCaseCreateVO createEnterpriseTestCase(Long enterpriseId, Long questionId, CodeTestCaseCreateReq req);

    /**
     * 更新企业私有题测试用例
     * @param enterpriseId 企业 ID
     * @param questionId 题目 ID
     * @param caseId 用例 ID
     * @param req 更新请求（含期望版本）
     * @return 更新结果
     */
    CodeTestCaseUpdateVO updateEnterpriseTestCase(Long enterpriseId, Long questionId, Long caseId, CodeTestCaseUpdateReq req);

    /**
     * 删除企业私有题测试用例（乐观锁 + 逻辑删除）
     * @param enterpriseId 企业 ID
     * @param questionId 题目 ID
     * @param caseId 用例 ID
     * @param expectedVersion 期望版本号（If-Match）
     */
    void deleteEnterpriseTestCase(Long enterpriseId, Long questionId, Long caseId, Integer expectedVersion);
}
