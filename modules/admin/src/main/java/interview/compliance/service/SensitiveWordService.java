package interview.compliance.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import interview.compliance.model.entity.SensitiveWord;
import interview.compliance.model.req.SensitiveWordCreateReq;
import interview.compliance.model.req.SensitiveWordSearchReq;
import interview.compliance.model.req.SensitiveWordUpdateReq;
import interview.compliance.model.vo.SensitiveWordCreateVO;
import interview.compliance.model.vo.SensitiveWordListItemVO;
import interview.compliance.model.vo.SensitiveWordUpdateVO;

/**
 * @author zhuxi
 * @apiNote 平台端敏感词库管理服务
 */
public interface SensitiveWordService extends IService<SensitiveWord> {

    /**
     * 分页查询敏感词库
     * @param req 分页与类别筛选条件
     * @return 敏感词分页
     */
    IPage<SensitiveWordListItemVO> pageSensitiveWords(SensitiveWordSearchReq req);

    /**
     * 添加敏感词（词本体唯一）
     * @param req 添加请求
     * @return 添加结果
     */
    SensitiveWordCreateVO createSensitiveWord(SensitiveWordCreateReq req);

    /**
     * 编辑敏感词（半量更新 + 乐观锁）
     * @param id 敏感词 ID
     * @param req 编辑请求
     * @return 编辑结果
     */
    SensitiveWordUpdateVO updateSensitiveWord(Long id, SensitiveWordUpdateReq req);

    /**
     * 删除敏感词（逻辑删除 + If-Match 版本校验）
     * @param id 敏感词 ID
     * @param expectedVersion 期望版本号
     */
    void deleteSensitiveWord(Long id, Integer expectedVersion);
}
