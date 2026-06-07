DROP TABLE IF EXISTS user_profile;

CREATE TABLE user_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    fitness_goal VARCHAR(255) COMMENT '健身目标',
    activity_level VARCHAR(32) COMMENT '活动等级',
    activity_factor DOUBLE COMMENT '活动系数',
    daily_calorie_burn DOUBLE COMMENT '日消耗热量(kcal)',
    custom_daily_calories DOUBLE COMMENT '自定义每日目标热量(kcal)',
    target_weight DOUBLE COMMENT '目标体重(kg)',
    experience_level VARCHAR(32) COMMENT '训练水平',
    preferred_equipment VARCHAR(512) COMMENT '可用器械(逗号分隔)',
    weekly_training_days INT COMMENT '每周训练天数(1-7)',
    training_duration INT COMMENT '每次训练时长(分钟)',
    occupation VARCHAR(64) COMMENT '职业',
    personality VARCHAR(32) COMMENT '性格特征',
    medical_history TEXT COMMENT '伤病/疾病史',
    diet_preference TEXT COMMENT '饮食偏好/忌口',
    training_preference TEXT COMMENT '训练偏好',
    user_profile_text TEXT COMMENT 'AI生成的画像摘要',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户详细画像';

-- 数据迁移：从 user 表把画像字段搬到 user_profile
