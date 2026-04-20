package com.kidslearn.generator;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.fill.Column;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * MyBatis-Plus 代码生成器
 * 运行main方法生成entity/mapper/service/controller代码
 */
public class CodeGenerator {

    private static final String URL = "jdbc:mysql://localhost:3306/kidslearn?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "root";

    private static final String PARENT_PACKAGE = "com.kidslearn.api";
    private static final String OUTPUT_DIR = System.getProperty("user.dir") + "/kidslearn-api/src/main/java";
    private static final String MAPPER_XML_DIR = System.getProperty("user.dir") + "/kidslearn-api/src/main/resources/mapper";

    public static void main(String[] args) {
        List<String> tables = Arrays.asList(
                "user", "child_profile", "parent_profile", "family", "family_child", "user_login_log",
                "subject", "grade_level", "course", "course_grade", "course_level",
                "question", "question_option", "learning_record", "wrong_topic",
                "pet", "pet_evolution", "pet_item", "pet_decoration", "pet_item_inventory", "user_pet",
                "achievement", "achievement_tier", "user_achievement",
                "sticker", "sticker_series", "user_sticker",
                "reward_log", "title", "user_title",
                "friend", "leaderboard", "challenge", "challenge_record",
                "app_config", "push_template", "time_control", "usage_log", "daily_stats",
                "subscription", "`order`"
        );

        FastAutoGenerator.create(URL, USERNAME, PASSWORD)
                .globalConfig(builder -> builder
                        .author("kidslearn")
                        .outputDir(OUTPUT_DIR)
                        .commentDate("yyyy-MM-dd")
                        .dateType(DateType.TIME_PACK)
                        .disableOpenDir()
                )
                .packageConfig(builder -> builder
                        .parent(PARENT_PACKAGE)
                        .entity("entity")
                        .mapper("mapper")
                        .service("service")
                        .serviceImpl("service.impl")
                        .controller("controller")
                        .pathConfig(Collections.singletonMap(OutputFile.xml, MAPPER_XML_DIR))
                )
                .strategyConfig(builder -> builder
                        .addInclude(tables)
                        .addTablePrefix("")
                        .entityBuilder()
                        .superClass("com.kidslearn.common.entity.BaseEntity")
                        .superEntityColumns("id", "create_time", "update_time")
                        .idType(IdType.AUTO)
                        .enableLombok()
                        .logicDeleteColumnName("deleted")
                        .addTableFills(new Column("create_time", FieldFill.INSERT))
                        .addTableFills(new Column("update_time", FieldFill.INSERT_UPDATE))
                        .controllerBuilder()
                        .enableRestStyle()
                        .mapperBuilder()
                        .enableBaseResultMap()
                        .enableBaseColumnList()
                        .serviceBuilder()
                        .formatServiceFileName("%sService")
                        .formatServiceImplFileName("%sServiceImpl")
                )
                .columnConfig(builder -> builder
                        .columnType("tinyint", DbColumnType.INTEGER)
                        .columnType("bigint", DbColumnType.LONG)
                )
                .execute();
    }
}
