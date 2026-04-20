package com.kidslearn.common.constants;

/**
 * Redis Key 常量
 */
public class RedisConstants {

    private static final String PREFIX = "kidslearn:";

    /** 用户Token前缀 */
    public static final String USER_TOKEN = PREFIX + "token:user:";
    /** 管理员Token前缀 */
    public static final String ADMIN_TOKEN = PREFIX + "token:admin:";
    /** 验证码 */
    public static final String SMS_CODE = PREFIX + "sms:";
    /** 每日统计 */
    public static final String DAILY_STATS = PREFIX + "stats:daily:";
    /** 排行榜 */
    public static final String LEADERBOARD = PREFIX + "leaderboard:";
    /** 接口限流 */
    public static final String RATE_LIMIT = PREFIX + "rate:";

    /** Token过期时间（秒） */
    public static final long TOKEN_EXPIRE = 7200;
    /** 刷新Token过期时间（秒） */
    public static final long REFRESH_TOKEN_EXPIRE = 604800;
}
