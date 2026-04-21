package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidslearn.api.entity.*;
import com.kidslearn.api.mapper.*;
import com.kidslearn.api.service.PetService;
import com.kidslearn.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService {

    private final UserPetMapper userPetMapper;
    private final PetMapper petMapper;
    private final PetItemMapper petItemMapper;
    private final PetItemInventoryMapper inventoryMapper;
    private final PetDecorationMapper decorationMapper;
    private final UserMapper userMapper;

    @Override
    public Map<String, Object> getMyPet(Long userId) {
        UserPet userPet = userPetMapper.selectOne(
            new LambdaQueryWrapper<UserPet>().eq(UserPet::getUserId, userId).last("LIMIT 1")
        );
        if (userPet == null) {
            return null;
        }
        Pet pet = petMapper.selectById(userPet.getPetId());

        Map<String, Object> result = new HashMap<>();
        result.put("id", userPet.getId());
        result.put("petId", userPet.getPetId());
        result.put("petName", pet != null ? pet.getPetName() : "");
        result.put("petType", pet != null ? pet.getPetType() : 0);
        result.put("currentLevel", userPet.getCurrentLevel());
        result.put("currentExp", userPet.getCurrentExp());
        result.put("hunger", userPet.getHunger());
        result.put("mood", userPet.getMood());
        result.put("currentImageUrl", userPet.getCurrentImageUrl());

        List<Long> wearIds = new ArrayList<>();
        if (userPet.getWearDecorationIds() != null && !userPet.getWearDecorationIds().isEmpty()) {
            wearIds = Arrays.stream(userPet.getWearDecorationIds().split(","))
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong).collect(Collectors.toList());
        }
        result.put("wearDecorationIds", wearIds);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> feedPet(Long userId, Long petItemId) {
        PetItemInventory inventory = inventoryMapper.selectOne(
            new LambdaQueryWrapper<PetItemInventory>()
                .eq(PetItemInventory::getUserId, userId)
                .eq(PetItemInventory::getPetItemId, petItemId)
        );
        if (inventory == null || inventory.getQuantity() <= 0) {
            throw new BusinessException("没有该道具");
        }

        PetItem item = petItemMapper.selectById(petItemId);
        if (item == null) throw new BusinessException("道具不存在");

        UserPet userPet = userPetMapper.selectOne(
            new LambdaQueryWrapper<UserPet>().eq(UserPet::getUserId, userId).last("LIMIT 1")
        );
        if (userPet == null) throw new BusinessException("没有宠物");

        // apply effect
        if (item.getItemType() == 1) { // food
            userPet.setHunger(Math.min(100, userPet.getHunger() + item.getEffectValue()));
        } else { // toy
            userPet.setMood(Math.min(4, userPet.getMood() + item.getEffectValue()));
        }

        // consume item
        inventory.setQuantity(inventory.getQuantity() - 1);
        if (inventory.getQuantity() <= 0) {
            inventoryMapper.deleteById(inventory.getId());
        } else {
            inventoryMapper.updateById(inventory);
        }

        // add pet exp
        userPet.setCurrentExp(userPet.getCurrentExp() + 5);
        userPetMapper.updateById(userPet);

        Map<String, Object> result = new HashMap<>();
        result.put("hunger", userPet.getHunger());
        result.put("mood", userPet.getMood());
        result.put("exp", userPet.getCurrentExp());
        result.put("message", item.getItemName() + " 使用成功！");
        return result;
    }

    @Override
    public Map<String, Object> playWithPet(Long userId) {
        UserPet userPet = userPetMapper.selectOne(
            new LambdaQueryWrapper<UserPet>().eq(UserPet::getUserId, userId).last("LIMIT 1")
        );
        if (userPet == null) throw new BusinessException("没有宠物");

        userPet.setMood(Math.min(4, userPet.getMood() + 1));
        userPet.setCurrentExp(userPet.getCurrentExp() + 3);
        userPet.setHunger(Math.max(0, userPet.getHunger() - 5));
        userPetMapper.updateById(userPet);

        Map<String, Object> result = new HashMap<>();
        result.put("mood", userPet.getMood());
        result.put("exp", userPet.getCurrentExp());
        result.put("hunger", userPet.getHunger());
        result.put("message", "和宠物玩耍了一会儿！");
        return result;
    }

    @Override
    public Map<String, Object> dressPet(Long userId, List<Long> decorationIds) {
        UserPet userPet = userPetMapper.selectOne(
            new LambdaQueryWrapper<UserPet>().eq(UserPet::getUserId, userId).last("LIMIT 1")
        );
        if (userPet == null) throw new BusinessException("没有宠物");

        userPet.setWearDecorationIds(decorationIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        userPetMapper.updateById(userPet);

        Map<String, Object> result = new HashMap<>();
        result.put("wearDecorationIds", decorationIds);
        result.put("message", "换装成功！");
        return result;
    }

    @Override
    public List<Map<String, Object>> getShopItems(Integer itemType) {
        LambdaQueryWrapper<PetItem> wrapper = new LambdaQueryWrapper<PetItem>()
            .eq(PetItem::getStatus, 1)
            .eq(itemType != null, PetItem::getItemType, itemType);
        List<PetItem> items = petItemMapper.selectList(wrapper);
        return items.stream().map(item -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("itemCode", item.getItemCode());
            map.put("itemName", item.getItemName());
            map.put("itemType", item.getItemType());
            map.put("effectDesc", item.getEffectDesc());
            map.put("imageUrl", item.getImageUrl());
            map.put("price", item.getPrice());
            map.put("rarity", item.getRarity());
            map.put("effectValue", item.getEffectValue());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> buyItem(Long userId, Long itemId, Integer quantity) {
        PetItem item = petItemMapper.selectById(itemId);
        if (item == null) throw new BusinessException("道具不存在");

        int totalCost = item.getPrice() * quantity;
        User user = userMapper.selectById(userId);
        if (user.getGold() < totalCost) throw new BusinessException("金币不足");

        user.setGold(user.getGold() - totalCost);
        userMapper.updateById(user);

        PetItemInventory inventory = inventoryMapper.selectOne(
            new LambdaQueryWrapper<PetItemInventory>()
                .eq(PetItemInventory::getUserId, userId)
                .eq(PetItemInventory::getPetItemId, itemId)
        );
        if (inventory != null) {
            inventory.setQuantity(inventory.getQuantity() + quantity);
            inventoryMapper.updateById(inventory);
        } else {
            inventory = new PetItemInventory();
            inventory.setUserId(userId);
            inventory.setPetItemId(itemId);
            inventory.setQuantity(quantity);
            inventoryMapper.insert(inventory);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("gold", user.getGold());
        result.put("quantity", quantity);
        result.put("message", "购买成功！");
        return result;
    }

    @Override
    public List<Map<String, Object>> getInventory(Long userId) {
        List<PetItemInventory> inventories = inventoryMapper.selectList(
            new LambdaQueryWrapper<PetItemInventory>().eq(PetItemInventory::getUserId, userId)
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (PetItemInventory inv : inventories) {
            PetItem item = petItemMapper.selectById(inv.getPetItemId());
            if (item == null) continue;
            Map<String, Object> map = new HashMap<>();
            map.put("id", inv.getId());
            map.put("itemId", item.getId());
            map.put("itemCode", item.getItemCode());
            map.put("itemName", item.getItemName());
            map.put("itemType", item.getItemType());
            map.put("imageUrl", item.getImageUrl());
            map.put("quantity", inv.getQuantity());
            result.add(map);
        }
        return result;
    }
}
