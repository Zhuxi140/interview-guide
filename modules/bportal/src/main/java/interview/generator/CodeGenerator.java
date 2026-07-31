package interview.generator;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.fill.Column;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.Collections;

/**
 * @author zhuxi
 * @apiNote 代码生成器 — bportal 模块（Phase 2 简历 & 投递域）
 */
public class CodeGenerator {

    static final String URL = "jdbc:postgresql://localhost:5432/interview_guide?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8";
    static final String USERNAME = "root";
    static final String PASSWORD = "123456";

    public static void main(String[] args) {
        String baseDir = System.getProperty("user.dir") + "/modules/bportal";

        // ---- 简历域 ----
        generate(baseDir, "resume",
                "resumes", "resume_analyses",
                "candidate_skill_scores", "candidate_ai_profiles");

        // ---- 匹配域 ----
        generate(baseDir, "matching",
                "job_applications");
    }

    static void generate(String baseDir, String domain, String... tables) {
        FastAutoGenerator.create(URL, USERNAME, PASSWORD)
                .globalConfig(builder -> builder
                        .author("zhuxi")
                        .outputDir(baseDir + "/src/main/java")
                        .disableOpenDir())
                .packageConfig(builder -> builder
                        .parent("interview")
                        .entity(domain + ".model.entity")
                        .service(domain + ".service")
                        .serviceImpl(domain + ".service")
                        .mapper(domain + ".mapper")
                        .controller(domain + ".controller")
                        .pathInfo(Collections.singletonMap(OutputFile.xml,
                                baseDir + "/src/main/resources/mapper")))
                .strategyConfig(builder -> {
                    builder.addInclude(tables);

                    builder.entityBuilder()
                            .enableLombok()
                            .idType(IdType.ASSIGN_ID)
                            .addTableFills(
                                    new Column("created_at", FieldFill.INSERT),
                                    new Column("updated_at", FieldFill.INSERT_UPDATE),
                                    new Column("updated_by", FieldFill.INSERT_UPDATE),
                                    new Column("trace_id", FieldFill.INSERT_UPDATE)
                            )
                            .naming(NamingStrategy.underline_to_camel)
                            .columnNaming(NamingStrategy.underline_to_camel)
                            .logicDeleteColumnName("is_deleted");

                    builder.mapperBuilder()
                            .formatMapperFileName("%sMapper")
                            .formatXmlFileName("%sMapper");

                    builder.serviceBuilder()
                            .formatServiceFileName("%sService")
                            .formatServiceImplFileName("%sServiceImpl");

                    builder.controllerBuilder()
                            .enableRestStyle()
                            .formatFileName("%sController");
                })
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }
}
