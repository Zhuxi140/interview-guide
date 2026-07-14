package interview.resume.controller;

import interview.common.constant.ApiVersion;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 候选人画像聚合表（供雷达图读取） 前端控制器
 * </p>
 *
 * @author zhuxi
 * @since 2026-07-13
 */
@RestController
@RequestMapping(ApiVersion.V1 +"/candidateProfile")
public class CandidateProfileController {

}
