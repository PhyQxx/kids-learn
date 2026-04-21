package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.*;
import com.kidslearn.api.mapper.*;
import com.kidslearn.api.service.AchievementService;
import com.kidslearn.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

    private final AchievementMapper achievementMapper;
    private final AchievementTierMapper achievementTierMapper;
    private final UserAchievementMapper userAchievementMapper;
    private final StickerMapper stickerMapper;
    private final StickerSeriesMapper stickerSeriesMapper;
    private final UserStickerMapper userStickerMapper;
    private final TitleMapper titleMapper;
    private final UserTitleMapper userTitleMapper;
    private final UserMapper userMapper;
    private final RewardLogMapper rewardLogMapper;

    @Override
    public List<Map<String, Object>> getAchievements(Long userId, Integer type) {
        LambdaQueryWrapper<Achievement> wrapper = new LambdaQueryWrapper<Achievement>()
            .eq(Achievement::getStatus, 1)
            .eq(type != null, Achievement::getAchieveType, type)
            .orderByAsc(Achievement::getSortOrder);
        List<Achievement> achievements = achievementMapper.selectList(wrapper);

        return achievements.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("achieveCode", a.getAchieveCode());
            map.put("achieveName", a.getAchieveName());
            map.put("achieveDesc", a.getAchieveDesc());
            map.put("achieveIcon", a.getAchieveIcon());
            map.put("achieveType", a.getAchieveType());
            map.put("isTiered", a.getIsTiered());

            // user progress
            UserAchievement ua = userAchievementMapper.selectOne(
                new LambdaQueryWrapper<UserAchievement>()
                    .eq(UserAchievement::getUserId, userId)
                    .eq(UserAchievement::getAchieveId, a.getId())
            );
            map.put("currentValue", ua != null ? ua.getCurrentValue() : 0);
            map.put("isCompleted", ua != null && ua.getIsCompleted() == 1);
            map.put("isReceived", ua != null && ua.getIsReceived() == 1);

            // tiers
            if (a.getIsTiered() == 1) {
                List<AchievementTier> tiers = achievementTierMapper.selectList(
                    new LambdaQueryWrapper<AchievementTier>()
                        .eq(AchievementTier::getAchieveId, a.getId())
                        .orderByAsc(AchievementTier::getTierLevel)
                );
                List<Map<String, Object>> tierList = tiers.stream().map(t -> {
                    Map<String, Object> tm = new HashMap<>();
                    tm.put("tierLevel", t.getTierLevel());
                    tm.put("tierName", t.getTierName());
                    return tm;
                }).collect(Collectors.toList());
                map.put("tiers", tierList);
            }
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getMyProgress(Long userId) {
        Map<String, Object> result = new HashMap<>();
        Long total = achievementMapper.selectCount(
            new LambdaQueryWrapper<Achievement>().eq(Achievement::getStatus, 1)
        );
        Long completed = userAchievementMapper.selectCount(
            new LambdaQueryWrapper<UserAchievement>()
                .eq(UserAchievement::getUserId, userId)
                .eq(UserAchievement::getIsCompleted, 1)
        );
        result.put("totalAchievements", total);
        result.put("completedAchievements", completed);

        Long totalStickers = stickerMapper.selectCount(
            new LambdaQueryWrapper<Sticker>().eq(Sticker::getStatus, 1)
        );
        Long myStickerTypes = userStickerMapper.selectCount(
            new LambdaQueryWrapper<UserSticker>().eq(UserSticker::getUserId, userId)
        );
        result.put("totalStickers", totalStickers);
        result.put("collectedStickers", myStickerTypes);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> receiveReward(Long userId, Long achievementId) {
        UserAchievement ua = userAchievementMapper.selectOne(
            new LambdaQueryWrapper<UserAchievement>()
                .eq(UserAchievement::getUserId, userId)
                .eq(UserAchievement::getAchieveId, achievementId)
        );
        if (ua == null || ua.getIsCompleted() != 1) {
            throw new BusinessException("成就未完成");
        }
        if (ua.getIsReceived() == 1) {
            throw new BusinessException("奖励已领取");
        }

        // get tier rewards
        AchievementTier tier = achievementTierMapper.selectOne(
            new LambdaQueryWrapper<AchievementTier>()
                .eq(AchievementTier::getAchieveId, achievementId)
                .eq(AchievementTier::getTierLevel, 1) // simplified: just first tier
                .last("LIMIT 1")
        );

        int goldReward = 50; // default
        if (tier != null && tier.getRewardJson() != null) {
            // parse reward json for gold amount (simplified)
            if (tier.getRewardJson().contains("\"value\":100")) goldReward = 100;
            else if (tier.getRewardJson().contains("\"value\":50")) goldReward = 50;
        }

        User user = userMapper.selectById(userId);
        user.setGold(user.getGold() + goldReward);
        userMapper.updateById(user);

        ua.setIsReceived(1);
        userAchievementMapper.updateById(ua);

        RewardLog log = new RewardLog();
        log.setUserId(userId);
        log.setRewardType(1);
        log.setQuantity(goldReward);
        log.setSourceType("ACHIEVEMENT");
        log.setSourceId(achievementId);
        log.setDescription("成就奖励");
        rewardLogMapper.insert(log);

        Map<String, Object> result = new HashMap<>();
        result.put("gold", goldReward);
        result.put("message", "奖励领取成功！");
        return result;
    }

    @Override
    public List<Map<String, Object>> getStickers(Long userId, Long seriesId) {
        LambdaQueryWrapper<Sticker> wrapper = new LambdaQueryWrapper<Sticker>()
            .eq(Sticker::getStatus, 1)
            .eq(seriesId != null, Sticker::getSeriesId, seriesId);
        List<Sticker> stickers = stickerMapper.selectList(wrapper);

        List<UserSticker> myStickers = userStickerMapper.selectList(
            new LambdaQueryWrapper<UserSticker>().eq(UserSticker::getUserId, userId)
        );
        Map<Long, Integer> myMap = myStickers.stream()
            .collect(Collectors.toMap(UserSticker::getStickerId, UserSticker::getQuantity, (a, b) -> a));

        return stickers.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("stickerCode", s.getStickerCode());
            map.put("stickerName", s.getStickerName());
            map.put("stickerUrl", s.getStickerUrl());
            map.put("rarity", s.getRarity());
            map.put("seriesName", s.getSeriesName());
            map.put("description", s.getDescription());
            map.put("quantity", myMap.getOrDefault(s.getId(), 0));
            map.put("collected", myMap.containsKey(s.getId()));
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getTitles(Long userId) {
        List<UserTitle> userTitles = userTitleMapper.selectList(
            new LambdaQueryWrapper<UserTitle>().eq(UserTitle::getUserId, userId)
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserTitle ut : userTitles) {
            Title title = titleMapper.selectById(ut.getTitleId());
            if (title == null) continue;
            Map<String, Object> map = new HashMap<>();
            map.put("id", title.getId());
            map.put("titleCode", title.getTitleCode());
            map.put("titleName", title.getTitleName());
            map.put("titleColor", title.getTitleColor());
            map.put("titleIcon", title.getTitleIcon());
            map.put("isActive", ut.getIsActive());
            result.add(map);
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> activateTitle(Long userId, Long titleId) {
        // deactivate all
        List<UserTitle> all = userTitleMapper.selectList(
            new LambdaQueryWrapper<UserTitle>().eq(UserTitle::getUserId, userId)
        );
        for (UserTitle ut : all) {
            ut.setIsActive(0);
            userTitleMapper.updateById(ut);
        }
        // activate target
        UserTitle target = userTitleMapper.selectOne(
            new LambdaQueryWrapper<UserTitle>()
                .eq(UserTitle::getUserId, userId)
                .eq(UserTitle::getTitleId, titleId)
        );
        if (target != null) {
            target.setIsActive(1);
            userTitleMapper.updateById(target);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("message", "称号佩戴成功！");
        return result;
    }
}
