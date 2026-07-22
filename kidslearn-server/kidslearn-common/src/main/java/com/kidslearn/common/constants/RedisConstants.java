package com.kidslearn.common.constants;

/**
 * Redis Key 常量
 */
public class RedisConstants {

    private static final String PREFIX = "kidslearn:";

    /** 用户Token前缀（所有角色共用，多设备登录用Set存储） */
    public static final String USER_TOKEN = PREFIX + "tokens:user:";
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
    /** 滑动续期阈值：access token 剩余有效期（秒）小于此值时，拦截器签发新 token 下发到响应头 */
    public static final long TOKEN_REFRESH_THRESHOLD = 1800;
    /** 滑动续期节流前缀：SET NX EX 防止同一 token 在窗口期内被重复续期 */
    public static final String TOKEN_REFRESH_LOCK = PREFIX + "tokens:refresh-lock:";
    /** 滑动续期节流窗口（秒）：同一 access token 在该时间内只续期一次 */
    public static final long TOKEN_REFRESH_LOCK_SECONDS = 60;
}
