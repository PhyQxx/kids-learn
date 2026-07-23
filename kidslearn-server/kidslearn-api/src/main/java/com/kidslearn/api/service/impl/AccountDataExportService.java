package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.*;
import com.kidslearn.api.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service @RequiredArgsConstructor
public class AccountDataExportService {
    private final UserMapper userMapper; private final ChildProfileMapper childProfileMapper;
    private final ParentProfileMapper parentProfileMapper; private final LearningRecordMapper learningRecordMapper;
    private final WrongTopicMapper wrongTopicMapper; private final NotificationMapper notificationMapper;
    private final ChallengeParticipantMapper participantMapper; private final ChallengeRewardLogMapper rewardMapper;
    private final ParentPinService parentPinService;

    public Map<String, Object> export(Long userId, String parentPin) {
        parentPinService.verify(userId, parentPin);
        User user = userMapper.selectById(userId);
        Map<String, Object> account = new LinkedHashMap<>();
        if (user != null) {
            account.put("id", user.getId()); account.put("username", user.getUsername()); account.put("nickname", user.getNickname());
            account.put("avatar", user.getAvatar()); account.put("status", user.getStatus()); account.put("level", user.getLevel());
            account.put("totalExp", user.getTotalExp()); account.put("gold", user.getGold()); account.put("createTime", user.getCreateTime());
        }
        ParentProfile parent = parentProfileMapper.selectOne(new LambdaQueryWrapper<ParentProfile>().eq(ParentProfile::getUserId, userId).last("LIMIT 1"));
        Map<String, Object> contact = new LinkedHashMap<>();
        if (parent != null) { contact.put("phone", parent.getPhone()); contact.put("relationship", parent.getRelationship());
            contact.put("consentVersion", parent.getConsentVersion()); contact.put("consentTime", parent.getConsentTime()); }
        Map<String, Object> result = new LinkedHashMap<>(); result.put("exportedAt", LocalDateTime.now()); result.put("account", account);
        result.put("contactAndConsent", contact);
        result.put("learningProfile", childProfileMapper.selectOne(new LambdaQueryWrapper<ChildProfile>().eq(ChildProfile::getUserId, userId).last("LIMIT 1")));
        result.put("learningRecords", learningRecordMapper.selectList(new LambdaQueryWrapper<LearningRecord>().eq(LearningRecord::getUserId, userId)));
        result.put("wrongTopics", wrongTopicMapper.selectList(new LambdaQueryWrapper<WrongTopic>().eq(WrongTopic::getUserId, userId)));
        result.put("notifications", notificationMapper.selectList(new LambdaQueryWrapper<Notification>().eq(Notification::getUserId, userId)));
        result.put("challengeParticipations", participantMapper.selectList(new LambdaQueryWrapper<ChallengeParticipant>().eq(ChallengeParticipant::getUserId, userId)));
        result.put("challengeRewards", rewardMapper.selectList(new LambdaQueryWrapper<ChallengeRewardLog>().eq(ChallengeRewardLog::getUserId, userId)));
        return result;
    }
}
