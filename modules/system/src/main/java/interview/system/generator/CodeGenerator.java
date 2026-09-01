package interview.system.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.Collections;

/**
 * @author zhuxi
 * @apiNote 代码生成器
 */
public class CodeGenerator {

    public static void main(String[] args) {
        // ======================== 数据库连接配置 ========================
        String url = "jdbc:postgresql://localhost:5432/interview_guide?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8";
        String username = "root";
        String password = "123456";

        // ======================== 项目路径（自动适应运行目录） ========================
        String projectPath = System.getProperty("user.dir");
        String baseDir = projectPath + "/modules/system";

        FastAutoGenerator.create(url, username, password)
                // ======================== 全局配置 ========================
                .globalConfig(builder -> {
                    builder.author("zhuxi")                        // 作者名，会写到类注释 @author 上
                            .outputDir(baseDir + "/src/main/java") // 生成的 Java 文件输出目录
                            .disableOpenDir();                     // 生成后不自动打开文件夹
                })
                // ======================== 包名配置 ========================
                .packageConfig(builder -> {
                    builder.parent("interview.system")            // 父包名，所有生成的类都在这个包下
                            .entity("entity")                      // 实体类子包 → interview.system.entity
                            .service("service")                    // Service 接口子包 → interview.system.service
                            .serviceImpl("service")                // ServiceImpl 与接口保持在同一 service 包
                            .mapper("mapper")                      // Mapper 接口子包 → interview.system.mapper
                            .controller("controller")              // Controller 子包 → interview.system.controller
                            .pathInfo(Collections.singletonMap(
                                    OutputFile.xml,                // Mapper XML 单独指定到 resources/mapper
                                    baseDir + "/src/main/resources/mapper"
                            ));
                })
                // ======================== 生成策略配置（核心） ========================
                .strategyConfig(builder -> {
                    // ---- 需要生成哪些表（支持多表，用逗号分隔） ----
                    builder.addInclude(
                            // Phase 1：地基搭建
                            "sys_users", "user_tokens",
                            "sys_roles", "sys_user_roles",
                            "sys_permissions", "sys_role_permissions",
                            "enterprises", "enterprise_team_members"
                    )
                            .addTablePrefix("sys_");  // 生成实体时自动去掉 sys_ 前缀（如 sys_users → SysUser）

                    // ---- Entity（手动编写了，已注释掉，启用时去掉 /* */） ----
                    /* 若启用会覆盖手动编写的实体类，请谨慎
                    builder.entityBuilder()
                            .superClass(BaseEntity.class)            // 公共父类（含 id、createdAt、updatedAt 等）
                            .enableLombok()                          // 使用 @Data 代替手写 getter/setter
                            .enableTableFieldAnnotation()            // 每个字段加 @TableField 注解
                            .naming(NamingStrategy.underline_to_camel)    // 表名 → 类名（下划线转驼峰）
                            .columnNaming(NamingStrategy.underline_to_camel) // 字段名 → 属性名（下划线转驼峰）
                            .logicDeleteColumnName("is_deleted")    // 指定逻辑删除字段（配合 @TableLogic）
                            .versionColumnName("version");           // 指定乐观锁字段（配合 @Version）
                    */

                    // ---- Mapper 接口 ----
                    builder.mapperBuilder()
                            .formatMapperFileName("%sMapper")        // 生成的 XxxMapper.java 文件名格式
                            .formatXmlFileName("%sMapper");          // 生成的 XxxMapper.xml 文件名格式

                    // ---- Service & ServiceImpl ----
                    builder.serviceBuilder()
                            .formatServiceFileName("%sService")      // 生成的 XxxService 接口名
                            .formatServiceImplFileName("%sServiceImpl"); // 生成的 XxxServiceImpl 实现类名

                    // ---- Controller ----
                    builder.controllerBuilder()
                            .enableRestStyle()                       // 使用 @RestController 而非 @Controller
                            .formatFileName("%sController");         // 生成的 XxxController 文件名
                })
                // ======================== 模板引擎 ========================
                .templateEngine(new FreemarkerTemplateEngine()) // 使用 Freemarker（已在 system/pom.xml 添加依赖）
                .execute();
    }
}
