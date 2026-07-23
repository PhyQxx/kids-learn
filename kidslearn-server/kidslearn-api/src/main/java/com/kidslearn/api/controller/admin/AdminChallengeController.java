package com.kidslearn.api.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidslearn.api.entity.Challenge;
import com.kidslearn.api.mapper.ChallengeMapper;
import com.kidslearn.api.service.impl.AdminOperationLogService;
import com.kidslearn.common.result.PageResult;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理后台-挑战管理")
@RestController
@RequestMapping("/api/v1/admin/challenge")
@RequiredArgsConstructor
public class AdminChallengeController {

    private final ChallengeMapper challengeMapper;
    private final AdminOperationLogService adminOperationLogService;

    @Operation(summary = "挑战列表")
    @GetMapping("/list")
    public R<PageResult<Challenge>> list(@RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "20") Integer pageSize,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Integer challengeType,
                                         @RequestParam(required = false) Long subjectId,
                                         @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Challenge> wrapper = new LambdaQueryWrapper<Challenge>()
            .like(keyword != null && !keyword.isEmpty(), Challenge::getChallengeName, keyword)
            .eq(challengeType != null, Challenge::getChallengeType, challengeType)
            .eq(subjectId != null, Challenge::getSubjectId, subjectId)
            .eq(status != null, Challenge::getStatus, status)
            .orderByDesc(Challenge::getCreateTime);
        Page<Challenge> p = challengeMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @Operation(summary = "新增/编辑挑战")
    @PostMapping("/save")
    public R<Void> save(@RequestBody Challenge challenge) {
        if (challenge.getId() == null) challengeMapper.insert(challenge);
        else challengeMapper.updateById(challenge);
        return R.ok();
    }

    @Operation(summary = "删除挑战")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        challengeMapper.deleteById(id);
        adminOperationLogService.write("challenge", "delete", "challenge", id, "delete challenge");
        return R.ok();
    }
}
