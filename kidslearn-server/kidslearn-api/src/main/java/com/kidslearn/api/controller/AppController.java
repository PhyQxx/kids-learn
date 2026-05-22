package com.kidslearn.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.AppVersion;
import com.kidslearn.api.mapper.AppVersionMapper;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "应用接口")
@RestController
@RequestMapping("/api/v1/public/app")
@RequiredArgsConstructor
public class AppController {

    private final AppVersionMapper appVersionMapper;

    @Operation(summary = "检查应用更新")
    @GetMapping("/check-update")
    public R<AppVersion> checkUpdate(
            @RequestParam String platform,
            @RequestParam Integer versionCode) {
        LambdaQueryWrapper<AppVersion> wrapper = new LambdaQueryWrapper<AppVersion>()
                .eq(AppVersion::getPlatform, platform)
                .gt(AppVersion::getVersionCode, versionCode)
                .orderByDesc(AppVersion::getVersionCode)
                .last("LIMIT 1");
        AppVersion latest = appVersionMapper.selectOne(wrapper);
        return R.ok(latest);
    }
}
