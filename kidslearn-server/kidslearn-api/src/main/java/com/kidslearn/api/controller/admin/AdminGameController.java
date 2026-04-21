package com.kidslearn.api.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidslearn.api.entity.*;
import com.kidslearn.api.mapper.*;
import com.kidslearn.common.result.PageResult;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "管理后台-游戏化管理")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminGameController {

    private final PetMapper petMapper;
    private final PetEvolutionMapper petEvolutionMapper;
    private final PetItemMapper petItemMapper;
    private final PetDecorationMapper petDecorationMapper;
    private final AchievementMapper achievementMapper;
    private final AchievementTierMapper achievementTierMapper;
    private final StickerMapper stickerMapper;
    private final StickerSeriesMapper stickerSeriesMapper;
    private final TitleMapper titleMapper;

    // ==================== 宠物管理 ====================

    @GetMapping("/pet/list")
    public R<PageResult<Pet>> petList(@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<Pet> p = petMapper.selectPage(new Page<>(page, pageSize),
            new LambdaQueryWrapper<Pet>().orderByAsc(Pet::getId));
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @PostMapping("/pet/save")
    public R<Void> petSave(@RequestBody Pet pet) {
        if (pet.getId() == null) petMapper.insert(pet);
        else petMapper.updateById(pet);
        return R.ok();
    }

    @DeleteMapping("/pet/{id}")
    public R<Void> petDelete(@PathVariable Long id) {
        petMapper.deleteById(id);
        return R.ok();
    }

    @GetMapping("/pet/{petId}/evolutions")
    public R<List<PetEvolution>> petEvolutions(@PathVariable Long petId) {
        return R.ok(petEvolutionMapper.selectList(
            new LambdaQueryWrapper<PetEvolution>().eq(PetEvolution::getPetId, petId).orderByAsc(PetEvolution::getEvolveLevel)));
    }

    @PostMapping("/pet/evolution/save")
    public R<Void> petEvolutionSave(@RequestBody PetEvolution evolution) {
        if (evolution.getId() == null) petEvolutionMapper.insert(evolution);
        else petEvolutionMapper.updateById(evolution);
        return R.ok();
    }

    // ==================== 道具管理 ====================

    @GetMapping("/pet-item/list")
    public R<PageResult<PetItem>> petItemList(@RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<PetItem> p = petItemMapper.selectPage(new Page<>(page, pageSize),
            new LambdaQueryWrapper<PetItem>().orderByAsc(PetItem::getId));
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @PostMapping("/pet-item/save")
    public R<Void> petItemSave(@RequestBody PetItem item) {
        if (item.getId() == null) petItemMapper.insert(item);
        else petItemMapper.updateById(item);
        return R.ok();
    }

    @DeleteMapping("/pet-item/{id}")
    public R<Void> petItemDelete(@PathVariable Long id) {
        petItemMapper.deleteById(id);
        return R.ok();
    }

    // ==================== 装饰管理 ====================

    @GetMapping("/decoration/list")
    public R<PageResult<PetDecoration>> decorationList(@RequestParam(defaultValue = "1") Integer page,
                                                        @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<PetDecoration> p = petDecorationMapper.selectPage(new Page<>(page, pageSize),
            new LambdaQueryWrapper<PetDecoration>().orderByAsc(PetDecoration::getId));
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @PostMapping("/decoration/save")
    public R<Void> decorationSave(@RequestBody PetDecoration deco) {
        if (deco.getId() == null) petDecorationMapper.insert(deco);
        else petDecorationMapper.updateById(deco);
        return R.ok();
    }

    @DeleteMapping("/decoration/{id}")
    public R<Void> decorationDelete(@PathVariable Long id) {
        petDecorationMapper.deleteById(id);
        return R.ok();
    }

    // ==================== 成就管理 ====================

    @GetMapping("/achievement/list")
    public R<PageResult<Achievement>> achievementList(@RequestParam(defaultValue = "1") Integer page,
                                                       @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<Achievement> p = achievementMapper.selectPage(new Page<>(page, pageSize),
            new LambdaQueryWrapper<Achievement>().orderByAsc(Achievement::getSortOrder));
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @PostMapping("/achievement/save")
    public R<Void> achievementSave(@RequestBody Map<String, Object> body) {
        Achievement achievement = new Achievement();
        achievement.setId(body.get("id") != null ? Long.valueOf(body.get("id").toString()) : null);
        achievement.setAchieveCode(body.getOrDefault("achieveCode", "").toString());
        achievement.setAchieveName(body.getOrDefault("achieveName", "").toString());
        achievement.setAchieveDesc(body.getOrDefault("achieveDesc", "").toString());
        achievement.setAchieveIcon(body.getOrDefault("achieveIcon", "").toString());
        achievement.setAchieveType(Integer.valueOf(body.getOrDefault("achieveType", 1).toString()));
        achievement.setIsTiered(Integer.valueOf(body.getOrDefault("isTiered", 0).toString()));
        achievement.setSortOrder(Integer.valueOf(body.getOrDefault("sortOrder", 0).toString()));
        achievement.setStatus(Integer.valueOf(body.getOrDefault("status", 1).toString()));

        if (achievement.getId() == null) {
            achievementMapper.insert(achievement);
        } else {
            achievementMapper.updateById(achievement);
        }

        // save tiers if present
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tiers = (List<Map<String, Object>>) body.get("tiers");
        if (tiers != null) {
            // delete old tiers
            achievementTierMapper.delete(
                new LambdaQueryWrapper<AchievementTier>().eq(AchievementTier::getAchieveId, achievement.getId())
            );
            for (Map<String, Object> t : tiers) {
                AchievementTier tier = new AchievementTier();
                tier.setAchieveId(achievement.getId());
                tier.setTierLevel(Integer.valueOf(t.getOrDefault("tierLevel", 1).toString()));
                tier.setTierName(t.getOrDefault("tierName", "").toString());
                tier.setConditionJson(t.getOrDefault("conditionJson", "").toString());
                tier.setRewardJson(t.getOrDefault("rewardJson", "").toString());
                achievementTierMapper.insert(tier);
            }
        }
        return R.ok();
    }

    @DeleteMapping("/achievement/{id}")
    public R<Void> achievementDelete(@PathVariable Long id) {
        achievementMapper.deleteById(id);
        achievementTierMapper.delete(new LambdaQueryWrapper<AchievementTier>().eq(AchievementTier::getAchieveId, id));
        return R.ok();
    }

    // ==================== 贴纸管理 ====================

    @GetMapping("/sticker/list")
    public R<PageResult<Sticker>> stickerList(@RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<Sticker> p = stickerMapper.selectPage(new Page<>(page, pageSize),
            new LambdaQueryWrapper<Sticker>().orderByAsc(Sticker::getId));
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @PostMapping("/sticker/save")
    public R<Void> stickerSave(@RequestBody Sticker sticker) {
        if (sticker.getId() == null) stickerMapper.insert(sticker);
        else stickerMapper.updateById(sticker);
        return R.ok();
    }

    @DeleteMapping("/sticker/{id}")
    public R<Void> stickerDelete(@PathVariable Long id) {
        stickerMapper.deleteById(id);
        return R.ok();
    }

    @GetMapping("/sticker-series/list")
    public R<List<StickerSeries>> stickerSeriesList() {
        return R.ok(stickerSeriesMapper.selectList(
            new LambdaQueryWrapper<StickerSeries>().orderByAsc(StickerSeries::getId)));
    }

    // ==================== 称号管理 ====================

    @GetMapping("/title/list")
    public R<PageResult<Title>> titleList(@RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<Title> p = titleMapper.selectPage(new Page<>(page, pageSize),
            new LambdaQueryWrapper<Title>().orderByAsc(Title::getId));
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @PostMapping("/title/save")
    public R<Void> titleSave(@RequestBody Title title) {
        if (title.getId() == null) titleMapper.insert(title);
        else titleMapper.updateById(title);
        return R.ok();
    }

    @DeleteMapping("/title/{id}")
    public R<Void> titleDelete(@PathVariable Long id) {
        titleMapper.deleteById(id);
        return R.ok();
    }
}
