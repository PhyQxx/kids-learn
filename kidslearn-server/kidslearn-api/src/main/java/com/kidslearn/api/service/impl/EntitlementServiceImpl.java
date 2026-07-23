package com.kidslearn.api.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.AppConfig;
import com.kidslearn.api.mapper.AppConfigMapper;
import com.kidslearn.api.service.EntitlementService;
import com.kidslearn.api.service.SubscriptionService;
import com.kidslearn.common.exception.BusinessException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor
public class EntitlementServiceImpl implements EntitlementService {
    private final SubscriptionService subscriptionService;
    private final AppConfigMapper appConfigMapper;
    private final StringRedisTemplate redisTemplate;
    public boolean has(Long userId, Code code) { return rule(userId, code).enabled; }
    public void require(Long userId, Code code) { if (!has(userId, code)) throw new BusinessException("需要会员权益：" + code.name()); }
    public void consume(Long userId, Code code, int amount) {
        if (amount <= 0) return; Rule rule = rule(userId, code); if (!rule.enabled) require(userId, code);
        quota(userId, code, "daily", LocalDate.now().toString(), rule.dailyQuota, amount, 2, TimeUnit.DAYS);
        quota(userId, code, "monthly", YearMonth.now().toString(), rule.monthlyQuota, amount, 40, TimeUnit.DAYS);
    }
    public Map<String, Object> getUserEntitlements(Long userId) {
        Map<String,Object> out = new LinkedHashMap<>();
        for (Code code : Code.values()) { Rule r=rule(userId,code); out.put(code.name(), Map.of("enabled",r.enabled,"dailyQuota",r.dailyQuota,"monthlyQuota",r.monthlyQuota)); }
        return out;
    }
    private Rule rule(Long userId, Code code) {
        boolean vip=Boolean.TRUE.equals(subscriptionService.getCurrentSubscription(userId).get("active"));
        String plan=vip?"VIP":"FREE"; boolean enabled=defaults(vip,code); int daily=0,monthly=0;
        String prefix="entitlement."+plan+"."+code.name()+".";
        for (AppConfig c:appConfigMapper.selectList(new LambdaQueryWrapper<AppConfig>().likeRight(AppConfig::getConfigKey,prefix))) {
            if ((prefix+"enabled").equals(c.getConfigKey())) enabled=Boolean.parseBoolean(c.getConfigValue());
            if ((prefix+"dailyQuota").equals(c.getConfigKey())) daily=number(c.getConfigValue());
            if ((prefix+"monthlyQuota").equals(c.getConfigKey())) monthly=number(c.getConfigValue());
        }
        return new Rule(enabled,daily,monthly);
    }
    private void quota(Long uid,Code code,String period,String bucket,int limit,int amount,long ttl,TimeUnit unit) {
        if(limit<=0)return; String key="quota:"+period+":"+bucket+":"+uid+":"+code; Long used=redisTemplate.opsForValue().increment(key,amount);
        if(used!=null&&used==amount)redisTemplate.expire(key,ttl,unit); if(used!=null&&used>limit){redisTemplate.opsForValue().increment(key,-amount);throw new BusinessException("权益额度已用完："+code.name());}
    }
    private static boolean defaults(boolean vip,Code code){return switch(code){case COURSE_BASIC,OFFLINE_CONTENT->true;default->vip;};}
    private static int number(String v){try{return Math.max(0,Integer.parseInt(v));}catch(Exception e){return 0;}}
    private record Rule(boolean enabled,int dailyQuota,int monthlyQuota){}
}
