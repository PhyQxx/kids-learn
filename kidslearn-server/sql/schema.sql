-- ============================================================
-- 趣学星球(KidsLearn) 数据库建表脚本
-- Database: kids_learn
-- Charset: utf8mb4
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------
-- 用户与认证
-- -----------------------------------------------------------

CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '密码（加密）',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `user_type` tinyint NOT NULL DEFAULT 1 COMMENT '用户类型：1孩子 2家长',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `total_exp` int NOT NULL DEFAULT 0 COMMENT '总经验值',
  `level` int NOT NULL DEFAULT 1 COMMENT '等级',
  `gold` int NOT NULL DEFAULT 0 COMMENT '金币余额',
  `diamond` int NOT NULL DEFAULT 0 COMMENT '钻石余额',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户主表';

CREATE TABLE IF NOT EXISTS `child_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `grade_level` tinyint DEFAULT NULL COMMENT '年级：1-13（幼儿园小班~高三）',
  `gender` tinyint NOT NULL DEFAULT 0 COMMENT '性别：0未知 1男 2女',
  `school_name` varchar(100) DEFAULT NULL COMMENT '学校名称',
  `class_name` varchar(50) DEFAULT NULL COMMENT '班级',
  `learn_age_group` tinyint NOT NULL DEFAULT 1 COMMENT '学习年龄段：1幼幼组 2低年龄组 3高年龄组',
  `pet_id` bigint DEFAULT NULL COMMENT '当前宠物ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='孩子档案表';

CREATE TABLE IF NOT EXISTS `parent_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `relationship` varchar(20) DEFAULT NULL COMMENT '与孩子关系：父亲/母亲/其他',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家长档案表';

CREATE TABLE IF NOT EXISTS `family` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `family_code` varchar(20) NOT NULL COMMENT '家庭邀请码',
  `family_name` varchar(50) DEFAULT NULL COMMENT '家庭名称',
  `parent_user_id` bigint NOT NULL COMMENT '家长用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_family_code` (`family_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭组表';

CREATE TABLE IF NOT EXISTS `family_child` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `family_id` bigint NOT NULL COMMENT '家庭ID',
  `child_user_id` bigint NOT NULL COMMENT '孩子用户ID',
  `relation_name` varchar(20) DEFAULT NULL COMMENT '关系称谓',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_family_child` (`family_id`, `child_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭孩子关联表';

CREATE TABLE IF NOT EXISTS `user_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `login_type` tinyint NOT NULL COMMENT '登录类型：1孩子 2家长',
  `device_type` varchar(20) DEFAULT NULL COMMENT '设备类型',
  `ip` varchar(50) DEFAULT NULL COMMENT '登录IP',
  `login_time` datetime NOT NULL COMMENT '登录时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录日志表';

-- -----------------------------------------------------------
-- 学习内容
-- -----------------------------------------------------------

CREATE TABLE IF NOT EXISTS `subject` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subject_code` varchar(20) NOT NULL COMMENT '学科代码',
  `subject_name` varchar(50) NOT NULL COMMENT '学科名称',
  `icon_url` varchar(255) DEFAULT NULL COMMENT '图标URL',
  `color` varchar(20) DEFAULT NULL COMMENT '主题色',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subject_code` (`subject_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学科表';

CREATE TABLE IF NOT EXISTS `grade_level` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `level_code` tinyint NOT NULL COMMENT '年级代码：1-13',
  `level_name` varchar(20) NOT NULL COMMENT '年级名称',
  `age_group` tinyint NOT NULL COMMENT '年龄组：1幼幼组 2低年龄组 3高年龄组',
  `min_age` int NOT NULL DEFAULT 3 COMMENT '最小年龄',
  `max_age` int NOT NULL DEFAULT 6 COMMENT '最大年龄',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_level_code` (`level_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年级表';

CREATE TABLE IF NOT EXISTS `course` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subject_id` bigint NOT NULL COMMENT '学科ID',
  `grade_level_id` bigint DEFAULT NULL COMMENT '年级ID（主年级）',
  `course_name` varchar(100) NOT NULL COMMENT '课程名称',
  `course_desc` varchar(500) DEFAULT NULL COMMENT '课程描述',
  `cover_url` varchar(255) DEFAULT NULL COMMENT '封面图',
  `total_levels` int NOT NULL DEFAULT 0 COMMENT '总关卡数',
  `difficulty` tinyint NOT NULL DEFAULT 1 COMMENT '难度：1简单 2普通 3困难',
  `is_elite` tinyint NOT NULL DEFAULT 0 COMMENT '是否精英关：0否 1是',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_subject_id` (`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

CREATE TABLE IF NOT EXISTS `course_grade` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `grade_level_id` bigint NOT NULL COMMENT '年级ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程年级关联表';

CREATE TABLE IF NOT EXISTS `course_level` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `level_num` int NOT NULL COMMENT '关卡序号',
  `level_name` varchar(100) NOT NULL COMMENT '关卡名称',
  `level_desc` varchar(500) DEFAULT NULL COMMENT '关卡描述',
  `cover_url` varchar(255) DEFAULT NULL COMMENT '封面图',
  `total_questions` int NOT NULL DEFAULT 0 COMMENT '题目数量',
  `pass_score` int NOT NULL DEFAULT 60 COMMENT '及格分数',
  `star_thresholds` varchar(50) DEFAULT '60,80,100' COMMENT '星级门槛',
  `exp_reward` int NOT NULL DEFAULT 10 COMMENT '经验奖励',
  `gold_reward` int NOT NULL DEFAULT 10 COMMENT '金币奖励',
  `sticker_id` bigint DEFAULT NULL COMMENT '通关奖励贴纸ID',
  `is_unlock` tinyint NOT NULL DEFAULT 0 COMMENT '是否解锁：0否 1是',
  `unlock_condition` varchar(255) DEFAULT NULL COMMENT '解锁条件JSON',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程关卡表';

CREATE TABLE IF NOT EXISTS `question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_level_id` bigint NOT NULL COMMENT '关卡ID',
  `question_type` tinyint NOT NULL DEFAULT 1 COMMENT '题型：1选择 2判断 3填空 4连线 5拖拽',
  `question_content` text COMMENT '题目内容（JSON富文本）',
  `difficulty` tinyint NOT NULL DEFAULT 1 COMMENT '难度：1简单 2普通 3困难',
  `score` int NOT NULL DEFAULT 10 COMMENT '分值',
  `time_limit` int NOT NULL DEFAULT 0 COMMENT '限时（秒），0不限时',
  `analysis` text COMMENT '解析',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_course_level_id` (`course_level_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

CREATE TABLE IF NOT EXISTS `question_option` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `option_label` varchar(10) NOT NULL COMMENT '选项标签：A/B/C/D',
  `option_content` text COMMENT '选项内容',
  `is_correct` tinyint NOT NULL DEFAULT 0 COMMENT '是否正确答案：0否 1是',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目选项表';

CREATE TABLE IF NOT EXISTS `learning_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `course_level_id` bigint NOT NULL COMMENT '关卡ID',
  `score` int NOT NULL DEFAULT 0 COMMENT '得分',
  `stars` int NOT NULL DEFAULT 0 COMMENT '获得星星数',
  `answer_time` int NOT NULL DEFAULT 0 COMMENT '答题用时（秒）',
  `wrong_count` int NOT NULL DEFAULT 0 COMMENT '错题数',
  `is_pass` tinyint NOT NULL DEFAULT 0 COMMENT '是否通关：0否 1是',
  `play_time` datetime DEFAULT NULL COMMENT '游玩时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_level` (`user_id`, `course_level_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习记录表';

CREATE TABLE IF NOT EXISTS `wrong_topic` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `wrong_answer` varchar(100) DEFAULT NULL COMMENT '错误答案',
  `correct_answer` varchar(100) DEFAULT NULL COMMENT '正确答案',
  `times` int NOT NULL DEFAULT 1 COMMENT '错误次数',
  `last_wrong_time` datetime DEFAULT NULL COMMENT '最后错误时间',
  `is_mastered` tinyint NOT NULL DEFAULT 0 COMMENT '是否已掌握：0否 1是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本表';

-- -----------------------------------------------------------
-- 游戏化系统
-- -----------------------------------------------------------

CREATE TABLE IF NOT EXISTS `pet` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pet_code` varchar(20) NOT NULL COMMENT '宠物代码',
  `pet_name` varchar(50) NOT NULL COMMENT '宠物名称',
  `pet_type` tinyint NOT NULL DEFAULT 1 COMMENT '宠物类型：1动物 2植物 3幻想',
  `base_image_url` varchar(255) DEFAULT NULL COMMENT '基础形象URL',
  `evolve_levels` varchar(50) DEFAULT NULL COMMENT '进化等级',
  `unlock_condition` varchar(255) DEFAULT NULL COMMENT '解锁条件JSON',
  `unlock_cost` int NOT NULL DEFAULT 0 COMMENT '解锁金币价格',
  `is_default` tinyint NOT NULL DEFAULT 0 COMMENT '是否默认宠物',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pet_code` (`pet_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物定义表';

CREATE TABLE IF NOT EXISTS `pet_evolution` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pet_id` bigint NOT NULL COMMENT '宠物ID',
  `evolve_level` int NOT NULL COMMENT '进化触发等级',
  `image_url` varchar(255) DEFAULT NULL COMMENT '进化后形象URL',
  `effect_url` varchar(255) DEFAULT NULL COMMENT '进化特效资源URL',
  `description` varchar(200) DEFAULT NULL COMMENT '形态描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pet_id` (`pet_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物进化配置表';

CREATE TABLE IF NOT EXISTS `pet_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `item_code` varchar(20) NOT NULL COMMENT '道具代码',
  `item_name` varchar(50) NOT NULL COMMENT '道具名称',
  `item_type` tinyint NOT NULL DEFAULT 1 COMMENT '道具类型：1食物 2玩具',
  `effect_desc` varchar(100) DEFAULT NULL COMMENT '效果描述',
  `image_url` varchar(255) DEFAULT NULL COMMENT '图片URL',
  `price` int NOT NULL DEFAULT 0 COMMENT '价格（金币）',
  `rarity` tinyint NOT NULL DEFAULT 1 COMMENT '稀有度：1普通 2稀有 3传说',
  `effect_value` int NOT NULL DEFAULT 0 COMMENT '效果值',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_code` (`item_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物道具表';

CREATE TABLE IF NOT EXISTS `pet_item_inventory` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `pet_item_id` bigint NOT NULL COMMENT '道具ID',
  `quantity` int NOT NULL DEFAULT 0 COMMENT '数量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_item` (`user_id`, `pet_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='背包表';

CREATE TABLE IF NOT EXISTS `pet_decoration` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `deco_code` varchar(20) NOT NULL COMMENT '装饰代码',
  `deco_name` varchar(50) NOT NULL COMMENT '装饰名称',
  `slot` varchar(20) NOT NULL DEFAULT 'HEAD' COMMENT '部位：HEAD/BODY/FACE/BACK/FOOT/EFFECT',
  `image_url` varchar(255) DEFAULT NULL COMMENT '装饰图片URL',
  `layer_order` int NOT NULL DEFAULT 0 COMMENT '叠加层级',
  `applicable_pets` varchar(255) DEFAULT NULL COMMENT '适配宠物ID列表',
  `price_gold` int NOT NULL DEFAULT 0 COMMENT '金币价格',
  `price_diamond` int NOT NULL DEFAULT 0 COMMENT '钻石价格',
  `rarity` tinyint NOT NULL DEFAULT 1 COMMENT '稀有度：1普通 2稀有 3传说',
  `is_limited` tinyint NOT NULL DEFAULT 0 COMMENT '是否限定',
  `limited_start` datetime DEFAULT NULL COMMENT '限定开始时间',
  `limited_end` datetime DEFAULT NULL COMMENT '限定结束时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_deco_code` (`deco_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物装饰表';

CREATE TABLE IF NOT EXISTS `user_pet` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `pet_id` bigint NOT NULL COMMENT '宠物ID',
  `current_level` int NOT NULL DEFAULT 1 COMMENT '当前等级',
  `current_exp` int NOT NULL DEFAULT 0 COMMENT '当前经验',
  `hunger` int NOT NULL DEFAULT 80 COMMENT '饱食度 0-100',
  `mood` tinyint NOT NULL DEFAULT 3 COMMENT '心情：1难过 2一般 3开心 4兴奋',
  `current_image_url` varchar(255) DEFAULT NULL COMMENT '当前形象URL',
  `wear_decoration_ids` varchar(100) DEFAULT NULL COMMENT '佩戴装饰ID列表',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户宠物表';

CREATE TABLE IF NOT EXISTS `achievement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `achieve_code` varchar(20) NOT NULL COMMENT '成就代码',
  `achieve_name` varchar(100) NOT NULL COMMENT '成就名称',
  `achieve_desc` varchar(200) DEFAULT NULL COMMENT '成就描述',
  `achieve_icon` varchar(255) DEFAULT NULL COMMENT '成就图标',
  `achieve_type` tinyint NOT NULL DEFAULT 1 COMMENT '成就类型：1学习 2收集 3社交 4时长 5特殊',
  `is_tiered` tinyint NOT NULL DEFAULT 0 COMMENT '是否分级',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_achieve_code` (`achieve_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成就定义表';

CREATE TABLE IF NOT EXISTS `achievement_tier` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `achieve_id` bigint NOT NULL COMMENT '成就ID',
  `tier_level` int NOT NULL COMMENT '等级序号（1铜/2银/3金）',
  `tier_name` varchar(20) NOT NULL COMMENT '等级名称',
  `condition_json` text COMMENT '条件JSON',
  `reward_json` text COMMENT '奖励JSON',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_achieve_id` (`achieve_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成就等级表';

CREATE TABLE IF NOT EXISTS `user_achievement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `achieve_id` bigint NOT NULL COMMENT '成就ID',
  `current_value` int NOT NULL DEFAULT 0 COMMENT '当前进度值',
  `target_value` int NOT NULL DEFAULT 0 COMMENT '目标值',
  `is_completed` tinyint NOT NULL DEFAULT 0 COMMENT '是否完成',
  `completed_time` datetime DEFAULT NULL COMMENT '完成时间',
  `is_received` tinyint NOT NULL DEFAULT 0 COMMENT '是否领取',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_achieve` (`user_id`, `achieve_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户成就进度表';

CREATE TABLE IF NOT EXISTS `sticker_series` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `series_code` varchar(20) NOT NULL COMMENT '系列代码',
  `series_name` varchar(50) NOT NULL COMMENT '系列名称',
  `series_icon` varchar(255) DEFAULT NULL COMMENT '系列封面图URL',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `is_limited` tinyint NOT NULL DEFAULT 0 COMMENT '是否限定系列',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_series_code` (`series_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='贴纸系列表';

CREATE TABLE IF NOT EXISTS `sticker` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sticker_code` varchar(20) NOT NULL COMMENT '贴纸代码',
  `sticker_name` varchar(50) NOT NULL COMMENT '贴纸名称',
  `sticker_url` varchar(255) DEFAULT NULL COMMENT '贴纸图片URL',
  `rarity` tinyint NOT NULL DEFAULT 1 COMMENT '稀有度：1普通 2稀有 3传说',
  `series_id` bigint DEFAULT NULL COMMENT '系列ID',
  `series_name` varchar(50) DEFAULT NULL COMMENT '系列名称',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sticker_code` (`sticker_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='贴纸表';

CREATE TABLE IF NOT EXISTS `user_sticker` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `sticker_id` bigint NOT NULL COMMENT '贴纸ID',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户贴纸表';

CREATE TABLE IF NOT EXISTS `reward_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `reward_type` tinyint NOT NULL COMMENT '奖励类型：1金币 2经验 3钻石 4贴纸 5道具',
  `reward_item_id` bigint DEFAULT NULL COMMENT '物品ID',
  `quantity` int NOT NULL DEFAULT 0 COMMENT '数量',
  `source_type` varchar(30) DEFAULT NULL COMMENT '来源类型',
  `source_id` bigint DEFAULT NULL COMMENT '来源ID',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖励流水表';

CREATE TABLE IF NOT EXISTS `title` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title_code` varchar(20) NOT NULL COMMENT '称号代码',
  `title_name` varchar(50) NOT NULL COMMENT '称号名称',
  `title_color` varchar(20) DEFAULT NULL COMMENT '展示颜色',
  `title_icon` varchar(255) DEFAULT NULL COMMENT '称号图标URL',
  `obtain_type` tinyint NOT NULL DEFAULT 1 COMMENT '获取方式：1成就解锁 2活动奖励 3手动发放',
  `related_achieve_id` bigint DEFAULT NULL COMMENT '关联成就ID',
  `is_timed` tinyint NOT NULL DEFAULT 0 COMMENT '是否限时',
  `valid_start` datetime DEFAULT NULL COMMENT '有效期开始',
  `valid_end` datetime DEFAULT NULL COMMENT '有效期结束',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_title_code` (`title_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='称号表';

CREATE TABLE IF NOT EXISTS `user_title` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `title_id` bigint NOT NULL COMMENT '称号ID',
  `is_active` tinyint NOT NULL DEFAULT 0 COMMENT '是否佩戴中',
  `obtain_time` datetime NOT NULL COMMENT '获得时间',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户称号表';

-- -----------------------------------------------------------
-- 社交与互动
-- -----------------------------------------------------------

CREATE TABLE IF NOT EXISTS `friend` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `friend_id` bigint NOT NULL COMMENT '好友ID',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0待确认 1已添加 2已拒绝',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表';

CREATE TABLE IF NOT EXISTS `leaderboard` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `rank_type` tinyint NOT NULL COMMENT '排行类型：1好友 2班级 3全国',
  `rank_value` bigint NOT NULL DEFAULT 0 COMMENT '排行值',
  `rank_week` varchar(10) DEFAULT NULL COMMENT '排行周期',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_type_week` (`rank_type`, `rank_week`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排行榜表';

CREATE TABLE IF NOT EXISTS `challenge` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `challenge_name` varchar(100) DEFAULT NULL COMMENT '挑战赛名称',
  `challenge_type` tinyint NOT NULL DEFAULT 1 COMMENT '类型：1好友对战 2排位赛',
  `subject_id` bigint DEFAULT NULL COMMENT '学科ID',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0未开始 1进行中 2已结束',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挑战赛表';

CREATE TABLE IF NOT EXISTS `challenge_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `challenge_id` bigint NOT NULL COMMENT '挑战赛ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `opponent_id` bigint DEFAULT NULL COMMENT '对手用户ID',
  `user_score` int NOT NULL DEFAULT 0 COMMENT '用户得分',
  `opponent_score` int NOT NULL DEFAULT 0 COMMENT '对手得分',
  `is_winner` tinyint NOT NULL DEFAULT 2 COMMENT '是否获胜：0负 1胜 2平',
  `reward_gold` int NOT NULL DEFAULT 0 COMMENT '奖励金币',
  `play_time` datetime DEFAULT NULL COMMENT '对战时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_challenge_id` (`challenge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挑战记录表';

-- -----------------------------------------------------------
-- 系统配置
-- -----------------------------------------------------------

CREATE TABLE IF NOT EXISTS `app_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_key` varchar(50) NOT NULL COMMENT '配置键',
  `config_value` text COMMENT '配置值',
  `config_type` tinyint NOT NULL DEFAULT 1 COMMENT '配置类型：1系统 2运营',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用配置表';

CREATE TABLE IF NOT EXISTS `push_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_code` varchar(30) NOT NULL COMMENT '模板代码',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `title_template` varchar(200) DEFAULT NULL COMMENT '标题模板',
  `content_template` text COMMENT '内容模板',
  `push_type` tinyint NOT NULL DEFAULT 1 COMMENT '推送类型：1系统 2运营',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推送模板表';

CREATE TABLE IF NOT EXISTS `time_control` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `family_id` bigint NOT NULL COMMENT '家庭ID',
  `child_user_id` bigint NOT NULL COMMENT '孩子用户ID',
  `daily_limit` int NOT NULL DEFAULT 0 COMMENT '每日时长限制（分钟）',
  `forbidden_start` time DEFAULT NULL COMMENT '禁止时段开始',
  `forbidden_end` time DEFAULT NULL COMMENT '禁止时段结束',
  `is_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_family_child` (`family_id`, `child_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='时间控制表';

CREATE TABLE IF NOT EXISTS `usage_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `app_name` varchar(50) DEFAULT NULL COMMENT '应用名称',
  `duration` int NOT NULL DEFAULT 0 COMMENT '使用时长（秒）',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `date` date DEFAULT NULL COMMENT '日期',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`, `date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='使用记录表';

CREATE TABLE IF NOT EXISTS `daily_stats` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `learn_minutes` int NOT NULL DEFAULT 0 COMMENT '学习时长（分钟）',
  `completed_levels` int NOT NULL DEFAULT 0 COMMENT '完成关卡数',
  `earned_gold` int NOT NULL DEFAULT 0 COMMENT '获得金币',
  `earned_exp` int NOT NULL DEFAULT 0 COMMENT '获得经验',
  `login_count` int NOT NULL DEFAULT 0 COMMENT '登录次数',
  `pet_feed_count` int NOT NULL DEFAULT 0 COMMENT '喂食次数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日统计表';

-- -----------------------------------------------------------
-- 付费相关
-- -----------------------------------------------------------

CREATE TABLE IF NOT EXISTS `subscription` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID（家长）',
  `plan_type` tinyint NOT NULL DEFAULT 1 COMMENT '套餐类型：1月度 2年度 3永久',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0未生效 1生效中 2已过期 3已取消',
  `start_time` datetime DEFAULT NULL COMMENT '生效时间',
  `end_time` datetime DEFAULT NULL COMMENT '到期时间',
  `auto_renew` tinyint NOT NULL DEFAULT 0 COMMENT '是否自动续费',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅/会员表';

CREATE TABLE IF NOT EXISTS `order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) NOT NULL COMMENT '订单编号',
  `user_id` bigint NOT NULL COMMENT '用户ID（家长）',
  `product_type` tinyint NOT NULL DEFAULT 1 COMMENT '商品类型：1订阅 2单次购买',
  `product_id` bigint DEFAULT NULL COMMENT '商品ID',
  `amount` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '订单金额',
  `pay_channel` varchar(20) DEFAULT NULL COMMENT '支付渠道',
  `pay_status` tinyint NOT NULL DEFAULT 0 COMMENT '支付状态：0待支付 1已支付 2已退款',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- -----------------------------------------------------------
-- 后台管理
-- -----------------------------------------------------------

CREATE TABLE IF NOT EXISTS `admin_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '密码（加密）',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `role_id` bigint DEFAULT NULL COMMENT '角色ID',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员用户表';

CREATE TABLE IF NOT EXISTS `admin_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_code` varchar(20) NOT NULL COMMENT '角色代码',
  `permissions` text COMMENT '权限列表JSON',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员角色表';

CREATE TABLE IF NOT EXISTS `content_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `target_type` varchar(20) NOT NULL COMMENT '审核对象类型',
  `target_id` bigint NOT NULL COMMENT '对象ID',
  `action` varchar(20) NOT NULL COMMENT '操作',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0草稿 1待审核 2已通过 3已驳回 4已发布 5已下线',
  `submitter_id` bigint DEFAULT NULL COMMENT '提交人ID',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核人ID',
  `review_comment` varchar(500) DEFAULT NULL COMMENT '审核意见',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `review_time` datetime DEFAULT NULL COMMENT '审核时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容审核表';

CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_user_id` bigint NOT NULL COMMENT '操作人ID',
  `module` varchar(30) DEFAULT NULL COMMENT '操作模块',
  `action` varchar(30) DEFAULT NULL COMMENT '操作动作',
  `target_type` varchar(30) DEFAULT NULL COMMENT '对象类型',
  `target_id` bigint DEFAULT NULL COMMENT '对象ID',
  `detail` text COMMENT '操作详情JSON',
  `ip` varchar(50) DEFAULT NULL COMMENT '操作IP',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_admin_user_id` (`admin_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

SET FOREIGN_KEY_CHECKS = 1;
