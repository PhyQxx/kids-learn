package com.kidslearn.api.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidslearn.api.entity.ChallengeSeason;
import com.kidslearn.api.mapper.ChallengeSeasonMapper;
import com.kidslearn.api.service.impl.AdminOperationLogService;
import com.kidslearn.common.exception.BusinessException;
import com.kidslearn.common.result.PageResult;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理后台-排位赛赛季")
@RestController
@RequestMapping("/api/v1/admin/challenge-season")
@RequiredArgsConstructor
public class AdminChallengeSeasonController {

    private final ChallengeSeasonMapper challengeSeasonMapper;
    private final AdminOperationLogService adminOperationLogService;

    @Operation(summary = "赛季列表")
    @GetMapping("/list")
    public R<PageResult<ChallengeSeason>> list(@RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "20") Integer pageSize,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String status) {
        LambdaQueryWrapper<ChallengeSeason> wrapper = new LambdaQueryWrapper<ChallengeSeason>()
            .and(keyword != null && !keyword.isEmpty(), w -> w
                .like(ChallengeSeason::getSeasonKey, keyword)
                .or().like(ChallengeSeason::getSeasonName, keyword))
            .eq(status != null && !status.isEmpty(), ChallengeSeason::getStatus, status)
            .orderByDesc(ChallengeSeason::getStartDate);
        Page<ChallengeSeason> p = challengeSeasonMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @Operation(summary = "新增/编辑赛季")
    @PostMapping("/save")
    public R<Void> save(@RequestBody ChallengeSeason season) {
        if (season.getSeasonKey() == null || season.getSeasonKey().isEmpty())
            throw new BusinessException("赛季 key 不能为空");
        if (season.getStartDate() == null || season.getEndDate() == null)
            throw new BusinessException("起止日期不能为空");
        if (season.getEndDate().isBefore(season.getStartDate()))
            throw new BusinessException("结束日期不能早于开始日期");
        if (season.getStatus() == null || season.getStatus().isEmpty()) season.setStatus("DRAFT");
        if (season.getId() == null) challengeSeasonMapper.insert(season);
        else challengeSeasonMapper.updateById(season);
        return R.ok();
    }

    @Operation(summary = "删除赛季")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        challengeSeasonMapper.deleteById(id);
        adminOperationLogService.write("challenge-season", "delete", "challenge_season", id, "delete season");
        return R.ok();
    }
}
