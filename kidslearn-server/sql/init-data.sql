-- ============================================================
-- 趣学星球(KidsLearn) 初始数据
-- ============================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------
-- 管理员角色
-- -----------------------------------------------------------
INSERT INTO `admin_role` (`id`, `role_name`, `role_code`, `permissions`, `description`) VALUES
(1, '超级管理员', 'SUPER_ADMIN', '["*"]', '拥有所有权限'),
(2, '内容运营', 'CONTENT_ADMIN', '["subject:*","course:*","level:*","question:*","audit:*"]', '管理学习内容'),
(3, '游戏运营', 'GAME_ADMIN', '["pet:*","item:*","decoration:*","achievement:*","sticker:*","title:*"]', '管理游戏化内容'),
(4, '数据分析师', 'DATA_ANALYST', '["dashboard:read","stats:read","log:read"]', '查看数据统计'),
(5, '审核员', 'AUDITOR', '["audit:read","audit:review"]', '内容审核');

-- -----------------------------------------------------------
-- 管理员账号 (密码: admin123, MD5: 0192023a7bbd73250516f069df18b500)
-- -----------------------------------------------------------
INSERT INTO `admin_user` (`id`, `username`, `password`, `real_name`, `role_id`, `status`) VALUES
(1, 'admin', '{MD5}0192023a7bbd73250516f069df18b500', '超级管理员', 1, 1);

-- -----------------------------------------------------------
-- 学科
-- -----------------------------------------------------------
INSERT INTO `subject` (`id`, `subject_code`, `subject_name`, `icon_url`, `color`, `sort_order`, `status`) VALUES
(1, 'CHINESE', '语文', '/static/subject/chinese.png', '#FF6B6B', 1, 1),
(2, 'MATH', '数学', '/static/subject/math.png', '#4ECDC4', 2, 1),
(3, 'ENGLISH', '英语', '/static/subject/english.png', '#FFE66D', 3, 1),
(4, 'LOGIC', '逻辑', '/static/subject/logic.png', '#A78BFA', 4, 1),
(5, 'SCIENCE', '科学', '/static/subject/science.png', '#34D399', 5, 1),
(6, 'MUSIC', '音乐', '/static/subject/music.png', '#F472B6', 6, 1);

-- -----------------------------------------------------------
-- 年级
-- -----------------------------------------------------------
INSERT INTO `grade_level` (`id`, `level_code`, `level_name`, `age_group`, `min_age`, `max_age`) VALUES
(1, 1, '幼儿园小班', 1, 3, 4),
(2, 2, '幼儿园中班', 1, 4, 5),
(3, 3, '幼儿园大班', 1, 5, 6),
(4, 4, '小学一年级', 2, 6, 7),
(5, 5, '小学二年级', 2, 7, 8),
(6, 6, '小学三年级', 2, 8, 9),
(7, 7, '小学四年级', 3, 9, 10),
(8, 8, '小学五年级', 3, 10, 11),
(9, 9, '小学六年级', 3, 11, 12);

-- -----------------------------------------------------------
-- 默认宠物
-- -----------------------------------------------------------
INSERT INTO `pet` (`id`, `pet_code`, `pet_name`, `pet_type`, `base_image_url`, `evolve_levels`, `unlock_cost`, `is_default`, `status`) VALUES
(1, 'CAT_001', '星小猫', 1, '/static/pet/cat_001_base.png', '3,5,7,9', 0, 1, 1),
(2, 'DOG_001', '萌小犬', 1, '/static/pet/dog_001_base.png', '3,5,7,9', 0, 0, 1),
(3, 'RABBIT_001', '绒小兔', 1, '/static/pet/rabbit_001_base.png', '3,5,7,9', 200, 0, 1),
(4, 'DRAGON_001', '焰小龙', 3, '/static/pet/dragon_001_base.png', '3,5,7,9', 500, 0, 1);

-- 宠物进化配置 (以星小猫为例)
INSERT INTO `pet_evolution` (`pet_id`, `evolve_level`, `image_url`, `description`) VALUES
(1, 3, '/static/pet/cat_001_lv3.png', '星小猫学会了撒娇，变得更强壮了'),
(1, 5, '/static/pet/cat_001_lv5.png', '星小猫成长为少年形态'),
(1, 7, '/static/pet/cat_001_lv7.png', '星小猫进入青年形态，更加威风'),
(1, 9, '/static/pet/cat_001_lv9.png', '星小猫完全体，闪耀着智慧光芒');

-- -----------------------------------------------------------
-- 宠物道具（食物）
-- -----------------------------------------------------------
INSERT INTO `pet_item` (`item_code`, `item_name`, `item_type`, `effect_desc`, `image_url`, `price`, `rarity`, `effect_value`, `status`) VALUES
('FOOD_APPLE', '红苹果', 1, '恢复饱食度+30', '/static/item/apple.png', 10, 1, 30, 1),
('FOOD_CAKE', '小蛋糕', 1, '恢复饱食度+50', '/static/item/cake.png', 25, 1, 50, 1),
('FOOD_FISH', '烤鱼干', 1, '恢复饱食度+80', '/static/item/fish.png', 50, 2, 80, 1),
('FOOD_STAR', '星星糖', 1, '恢复饱食度+100', '/static/item/star_candy.png', 100, 3, 100, 1),
('TOY_BALL', '小皮球', 2, '心情+1', '/static/item/ball.png', 15, 1, 1, 1),
('TOY_KITE', '小风筝', 2, '心情+2', '/static/item/kite.png', 40, 2, 2, 1);

-- -----------------------------------------------------------
-- 宠物装饰
-- -----------------------------------------------------------
INSERT INTO `pet_decoration` (`deco_code`, `deco_name`, `slot`, `image_url`, `layer_order`, `price_gold`, `price_diamond`, `rarity`, `status`) VALUES
('DEC_CROWN', '小皇冠', 'HEAD', '/static/deco/crown.png', 10, 200, 0, 2, 1),
('DEC_BOW', '蝴蝶结', 'HEAD', '/static/deco/bow.png', 10, 80, 0, 1, 1),
('DEC_GLASSES', '墨镜', 'FACE', '/static/deco/glasses.png', 20, 150, 0, 2, 1),
('DEC_CAPE', '超人披风', 'BODY', '/static/deco/cape.png', 5, 300, 0, 2, 1),
('DEC_WINGS', '小翅膀', 'BACK', '/static/deco/wings.png', 3, 0, 10, 3, 1),
('DEC_SNEAKERS', '运动鞋', 'FOOT', '/static/deco/sneakers.png', 1, 100, 0, 1, 1);

-- -----------------------------------------------------------
-- 贴纸系列与贴纸
-- -----------------------------------------------------------
INSERT INTO `sticker_series` (`series_code`, `series_name`, `series_icon`, `description`, `is_limited`) VALUES
('SERIES_ANIMAL', '动物乐园', '/static/sticker/series_animal.png', '可爱的小动物们', 0),
('SERIES_SPACE', '星际探险', '/static/sticker/series_space.png', '遨游太空的冒险', 0),
('SERIES_OCEAN', '海底世界', '/static/sticker/series_ocean.png', '神秘的海洋生物', 0);

INSERT INTO `sticker` (`sticker_code`, `sticker_name`, `sticker_url`, `rarity`, `series_id`, `series_name`, `description`) VALUES
('STK_KITTEN', '小猫咪', '/static/sticker/kitten.png', 1, 1, '动物乐园', '一只可爱的小猫咪'),
('STK_PUPPY', '小狗狗', '/static/sticker/puppy.png', 1, 1, '动物乐园', '活泼的小狗狗'),
('STK_BUNNY', '小兔子', '/static/sticker/bunny.png', 2, 1, '动物乐园', '乖巧的小兔子'),
('STK_UNICORN', '独角兽', '/static/sticker/unicorn.png', 3, 1, '动物乐园', '传说中的独角兽'),
('STK_ROCKET', '小火箭', '/static/sticker/rocket.png', 1, 2, '星际探险', '嗖嗖飞上天'),
('STK_STAR', '大星星', '/static/sticker/star.png', 1, 2, '星际探险', '闪闪发光的星星'),
('STK_ALIEN', '小外星人', '/static/sticker/alien.png', 2, 2, '星际探险', '来自远方的朋友'),
('STK_PLANET', '彩色星球', '/static/sticker/planet.png', 3, 2, '星际探险', '彩虹般的星球'),
('STK_FISH', '小丑鱼', '/static/sticker/fish.png', 1, 3, '海底世界', '海底的小丑鱼'),
('STK_WHALE', '大鲸鱼', '/static/sticker/whale.png', 2, 3, '海底世界', '温柔的大鲸鱼'),
('STK_MERMAID', '美人鱼', '/static/sticker/mermaid.png', 3, 3, '海底世界', '美丽的人鱼公主');

-- -----------------------------------------------------------
-- 成就定义
-- -----------------------------------------------------------
INSERT INTO `achievement` (`achieve_code`, `achieve_name`, `achieve_desc`, `achieve_type`, `is_tiered`, `sort_order`) VALUES
('ACH_FIRST_LEVEL', '初次闯关', '完成第一个关卡', 1, 0, 1),
('ACH_DAILY_LOGIN', '坚持不懈', '连续登录学习', 1, 1, 2),
('ACH_LEVEL_MASTER', '关卡达人', '累计完成关卡数', 1, 1, 3),
('ACH_STAR_COLLECTOR', '星星收集者', '累计获得星星数', 1, 1, 4),
('ACH_FULL_STAR', '完美主义', '单关卡获得3星', 1, 0, 5),
('ACH_STICKER_FAN', '贴纸爱好者', '收集贴纸数', 2, 1, 6),
('ACH_PET_LOVER', '宠物达人', '宠物达到指定等级', 1, 1, 7),
('ACH_SPEED_LEARNER', '速答王', '快速完成关卡', 1, 0, 8),
('ACH_SUBJECT_M', '数学小达人', '完成数学指定关卡', 1, 0, 9),
('ACH_SUBJECT_C', '语文小达人', '完成语文指定关卡', 1, 0, 10),
('ACH_FRIEND_5', '社交达人', '添加5个好友', 3, 0, 11),
('ACH_CHALLENGE_WIN', '挑战高手', '挑战赛获胜', 3, 0, 12);

-- 成就分级 (坚持不懈: 铜3天/银7天/金30天)
INSERT INTO `achievement_tier` (`achieve_id`, `tier_level`, `tier_name`, `condition_json`, `reward_json`) VALUES
(2, 1, '铜级', '{"logic":"AND","conditions":[{"type":"DAILY_LOGIN","value":3}]}', '[{"type":"GOLD","value":50}]'),
(2, 2, '银级', '{"logic":"AND","conditions":[{"type":"DAILY_LOGIN","value":7}]}', '[{"type":"GOLD","value":100},{"type":"STICKER","itemId":3}]'),
(2, 3, '金级', '{"logic":"AND","conditions":[{"type":"DAILY_LOGIN","value":30}]}', '[{"type":"GOLD","value":500},{"type":"STICKER","itemId":4},{"type":"TITLE","itemId":1}]');

-- 关卡达人: 铜10关/银50关/金100关
INSERT INTO `achievement_tier` (`achieve_id`, `tier_level`, `tier_name`, `condition_json`, `reward_json`) VALUES
(3, 1, '铜级', '{"logic":"AND","conditions":[{"type":"COMPLETE_LEVEL","value":10}]}', '[{"type":"GOLD","value":100}]'),
(3, 2, '银级', '{"logic":"AND","conditions":[{"type":"COMPLETE_LEVEL","value":50}]}', '[{"type":"GOLD","value":300},{"type":"STICKER","itemId":5}]'),
(3, 3, '金级', '{"logic":"AND","conditions":[{"type":"COMPLETE_LEVEL","value":100}]}', '[{"type":"GOLD","value":1000},{"type":"DIAMOND","value":10}]');

-- 星星收集者
INSERT INTO `achievement_tier` (`achieve_id`, `tier_level`, `tier_name`, `condition_json`, `reward_json`) VALUES
(4, 1, '铜级', '{"logic":"AND","conditions":[{"type":"GET_STAR","value":30}]}', '[{"type":"GOLD","value":80}]'),
(4, 2, '银级', '{"logic":"AND","conditions":[{"type":"GET_STAR","value":100}]}', '[{"type":"GOLD","value":200}]'),
(4, 3, '金级', '{"logic":"AND","conditions":[{"type":"GET_STAR","value":300}]}', '[{"type":"GOLD","value":500},{"type":"DIAMOND","value":5}]');

-- -----------------------------------------------------------
-- 称号
-- -----------------------------------------------------------
INSERT INTO `title` (`title_code`, `title_name`, `title_color`, `obtain_type`, `related_achieve_id`) VALUES
('TITLE_EXPLORER', '小小探索家', '#4ECDC4', 1, 2),
('TITLE_MASTER', '学习小达人', '#FF6B6B', 1, 3),
('TITLE_STAR', '星星之王', '#FFE66D', 1, 4),
('TITLE_VIP', '尊贵会员', '#A78BFA', 2, NULL);

-- -----------------------------------------------------------
-- 示例课程 (数学 - 认识数字1-10, 适合幼幼组)
-- -----------------------------------------------------------
INSERT INTO `course` (`id`, `subject_id`, `grade_level_id`, `course_name`, `course_desc`, `difficulty`, `is_elite`, `sort_order`, `total_levels`) VALUES
(1, 2, 1, '认识数字1-10', '学习1到10的数字认识，通过趣味游戏认识数字', 1, 0, 1, 3),
(2, 2, 1, '比较大小', '学习比较数字大小和物品多少', 1, 0, 2, 2),
(3, 1, 1, '认字入门', '学习最基础的常见汉字', 1, 0, 1, 3),
(4, 3, 1, '英语字母', '学习26个英文字母', 1, 0, 1, 3);

-- 课程年级关联
INSERT INTO `course_grade` (`course_id`, `grade_level_id`) VALUES
(1, 1), (1, 2), (1, 3),
(2, 2), (2, 3),
(3, 1), (3, 2),
(4, 1), (4, 2);

-- 关卡 (课程1: 认识数字1-10)
INSERT INTO `course_level` (`course_id`, `level_num`, `level_name`, `level_desc`, `total_questions`, `pass_score`, `star_thresholds`, `exp_reward`, `gold_reward`, `is_unlock`, `status`) VALUES
(1, 1, '认识数字1-3', '学习数字1、2、3', 3, 60, '60,80,100', 10, 10, 1, 1),
(1, 2, '认识数字4-6', '学习数字4、5、6', 3, 60, '60,80,100', 10, 15, 0, 1),
(1, 3, '认识数字7-10', '学习数字7、8、9、10', 4, 60, '60,80,100', 15, 20, 0, 1);

-- 题目 (关卡1: 认识数字1-3)
INSERT INTO `question` (`course_level_id`, `question_type`, `question_content`, `difficulty`, `score`, `time_limit`, `analysis`, `sort_order`) VALUES
(1, 1, '这是几个苹果？🍎', 1, 10, 30, '图中有1个苹果，所以答案是A：1个', 1),
(1, 1, '这是几个香蕉？🍌🍌', 1, 10, 30, '图中有2个香蕉，所以答案是B：2个', 2),
(1, 1, '这是几个橘子？🍊🍊🍊', 1, 10, 30, '图中有3个橘子，所以答案是C：3个', 3);

-- 题目选项
INSERT INTO `question_option` (`question_id`, `option_label`, `option_content`, `is_correct`, `sort_order`) VALUES
(1, 'A', '1个', 1, 1), (1, 'B', '2个', 0, 2), (1, 'C', '3个', 0, 3), (1, 'D', '4个', 0, 4),
(2, 'A', '1个', 0, 1), (2, 'B', '2个', 1, 2), (2, 'C', '3个', 0, 3), (2, 'D', '4个', 0, 4),
(3, 'A', '1个', 0, 1), (3, 'B', '2个', 0, 2), (3, 'C', '3个', 1, 3), (3, 'D', '4个', 0, 4);

-- -----------------------------------------------------------
-- 系统配置
-- -----------------------------------------------------------
INSERT INTO `app_config` (`config_key`, `config_value`, `config_type`, `description`) VALUES
('APP_VERSION', '1.0.0', 1, '应用版本号'),
('DAILY_LOGIN_GOLD', '5', 2, '每日首次登录奖励金币'),
('CONTINUOUS_LOGIN_DAYS_7_GOLD', '50', 2, '连续登录7天奖励金币'),
('DEFAULT_DAILY_LIMIT', '30', 2, '默认每日使用时长限制（分钟）'),
('LEVEL_UP_EXP_BASE', '100', 2, '每级升级所需经验基数'),
('PET_HUNGER_DECAY_RATE', '5', 2, '宠物饱食度每小时下降值');
