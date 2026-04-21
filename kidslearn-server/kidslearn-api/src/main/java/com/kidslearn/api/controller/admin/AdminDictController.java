package com.kidslearn.api.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidslearn.api.entity.DictData;
import com.kidslearn.api.entity.DictType;
import com.kidslearn.api.mapper.DictDataMapper;
import com.kidslearn.api.mapper.DictTypeMapper;
import com.kidslearn.common.result.PageResult;
import com.kidslearn.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理后台-字典管理")
@RestController
@RequestMapping("/api/v1/admin/dict")
@RequiredArgsConstructor
public class AdminDictController {

    private final DictTypeMapper dictTypeMapper;
    private final DictDataMapper dictDataMapper;

    // ==================== 字典类型 ====================

    @Operation(summary = "字典类型列表")
    @GetMapping("/type/list")
    public R<PageResult<DictType>> dictTypeList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<DictType> wrapper = new LambdaQueryWrapper<DictType>()
            .and(keyword != null && !keyword.isEmpty(), w -> w
                .like(DictType::getDictName, keyword)
                .or()
                .like(DictType::getDictType, keyword))
            .orderByDesc(DictType::getCreateTime);
        Page<DictType> p = dictTypeMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @Operation(summary = "所有字典类型（下拉用）")
    @GetMapping("/type/all")
    public R<List<DictType>> dictTypeAll() {
        return R.ok(dictTypeMapper.selectList(
            new LambdaQueryWrapper<DictType>().eq(DictType::getStatus, 1).orderByAsc(DictType::getId)));
    }

    @Operation(summary = "新增/编辑字典类型")
    @PostMapping("/type/save")
    public R<Void> dictTypeSave(@RequestBody DictType dictType) {
        if (dictType.getId() == null) dictTypeMapper.insert(dictType);
        else dictTypeMapper.updateById(dictType);
        return R.ok();
    }

    @Operation(summary = "删除字典类型")
    @DeleteMapping("/type/{id}")
    public R<Void> dictTypeDelete(@PathVariable Long id) {
        dictTypeMapper.deleteById(id);
        dictDataMapper.delete(new LambdaQueryWrapper<DictData>().eq(DictData::getDictTypeId, id));
        return R.ok();
    }

    // ==================== 字典数据 ====================

    @Operation(summary = "字典数据列表")
    @GetMapping("/data/list")
    public R<PageResult<DictData>> dictDataList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long dictTypeId) {
        LambdaQueryWrapper<DictData> wrapper = new LambdaQueryWrapper<DictData>()
            .eq(dictTypeId != null, DictData::getDictTypeId, dictTypeId)
            .orderByAsc(DictData::getSortOrder);
        Page<DictData> p = dictDataMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return R.ok(new PageResult<>(p.getRecords(), p.getTotal(), page, pageSize));
    }

    @Operation(summary = "根据字典类型代码获取数据")
    @GetMapping("/data/{dictType}")
    public R<List<DictData>> dictDataByType(@PathVariable String dictType) {
        DictType type = dictTypeMapper.selectOne(
            new LambdaQueryWrapper<DictType>().eq(DictType::getDictType, dictType));
        if (type == null) return R.ok(List.of());
        List<DictData> list = dictDataMapper.selectList(
            new LambdaQueryWrapper<DictData>()
                .eq(DictData::getDictTypeId, type.getId())
                .eq(DictData::getStatus, 1)
                .orderByAsc(DictData::getSortOrder));
        return R.ok(list);
    }

    @Operation(summary = "新增/编辑字典数据")
    @PostMapping("/data/save")
    public R<Void> dictDataSave(@RequestBody DictData dictData) {
        if (dictData.getId() == null) dictDataMapper.insert(dictData);
        else dictDataMapper.updateById(dictData);
        return R.ok();
    }

    @Operation(summary = "删除字典数据")
    @DeleteMapping("/data/{id}")
    public R<Void> dictDataDelete(@PathVariable Long id) {
        dictDataMapper.deleteById(id);
        return R.ok();
    }
}
