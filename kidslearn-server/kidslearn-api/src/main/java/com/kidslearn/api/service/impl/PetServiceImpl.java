package com.kidslearn.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.kidslearn.api.entity.*;
import com.kidslearn.api.mapper.*;
import com.kidslearn.api.realtime.RealtimeEventPublisher;
import com.kidslearn.api.service.PetService;
import com.kidslearn.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final PetEvolutionMapper evolutionMapper;
    private final UserDecorationInventoryMapper decoInventoryMapper;
    private final UserMapper userMapper;
    private final RealtimeEventPublisher realtimeEventPublisher;

    @Override
    public Map<String, Object> getMyPet(Long userId) {
        UserPet userPet = userPetMapper.selectOne(
            new LambdaQueryWrapper<UserPet>().eq(UserPet::getUserId, userId).last("LIMIT 1")
        );
        if (userPet == null) {
            return initPet(userId);
        }

        // Apply passive decay
        List<LocalDateTime> times = Arrays.asList(
            userPet.getLastFeedTime(), userPet.getLastPlayTime(), userPet.getLastBathTime()
        );
        LocalDateTime lastActive = times.stream()
            .filter(Objects::nonNull)
            .max(LocalDateTime::compareTo)
            .orElse(null);
        if (lastActive != null) {
            PetEvolutionEngine.DecayResult decay = PetEvolutionEngine.applyPassiveDecay(
                userPet.getHunger(), userPet.getMood(),
                userPet.getEnergy() != null ? userPet.getEnergy() : 100,
                lastActive);
            if (decay.changed()) {
                userPet.setHunger(decay.hunger());
                userPet.setMood(decay.mood());
                userPet.setEnergy(decay.energy());
                userPetMapper.updateById(userPet);
            }
        }

        Pet pet = petMapper.selectById(userPet.getPetId());

        // Find current evolution image
        String evolutionImage = userPet.getCurrentImageUrl();
        String evolutionName = "";
        List<PetEvolution> evolutions = evolutionMapper.selectList(
            new LambdaQueryWrapper<PetEvolution>()
                .eq(PetEvolution::getPetId, userPet.getPetId())
                .orderByDesc(PetEvolution::getEvolveLevel)
        );
        for (PetEvolution evo : evolutions) {
            if (userPet.getCurrentLevel() >= evo.getEvolveLevel()) {
                evolutionImage = evo.getImageUrl();
                evolutionName = evo.getDescription();
                break;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", userPet.getId());
        result.put("petId", userPet.getPetId());
        result.put("petName", pet != null ? pet.getPetName() : "");
        result.put("petType", pet != null ? pet.getPetType() : 0);
        result.put("currentLevel", userPet.getCurrentLevel());
        result.put("currentExp", userPet.getCurrentExp());
        result.put("hunger", userPet.getHunger());
        result.put("mood", userPet.getMood());
        result.put("energy", userPet.getEnergy() != null ? userPet.getEnergy() : 100);
        result.put("currentImageUrl", evolutionImage);
        result.put("evolutionName", evolutionName);

        int expInLevel = PetEvolutionEngine.expInCurrentLevel(userPet.getCurrentExp(), userPet.getCurrentLevel());
        int expForNext = PetEvolutionEngine.expForNextLevel(userPet.getCurrentLevel());
        int expNeeded = PetEvolutionEngine.expNeededForNextLevel(userPet.getCurrentExp(), userPet.getCurrentLevel());
        result.put("expInCurrentLevel", expInLevel);
        result.put("nextLevelExp", expForNext);
        result.put("expNeeded", expNeeded);

        User user = userMapper.selectById(userId);
        result.put("gold", user != null && user.getGold() != null ? user.getGold() : 0);
        result.put("diamond", user != null && user.getDiamond() != null ? user.getDiamond() : 0);

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

        UserPet userPet = findUserPetOrThrow(userId);

        if (item.getItemType() == 1) {
            userPet.setHunger(Math.min(100, userPet.getHunger() + item.getEffectValue()));
            int curEnergy = userPet.getEnergy() != null ? userPet.getEnergy() : 100;
            userPet.setEnergy(Math.min(100, curEnergy + item.getEffectValue() / 3));
        } else if (item.getItemType() == 2) {
            userPet.setMood(Math.min(4, userPet.getMood() + item.getEffectValue()));
        } else {
            userPet.setHunger(100);
            userPet.setEnergy(100);
            userPet.setMood(4);
        }

        inventory.setQuantity(inventory.getQuantity() - 1);
        if (inventory.getQuantity() <= 0) {
            inventoryMapper.deleteById(inventory.getId());
        } else {
            inventoryMapper.updateById(inventory);
        }

        userPet.setLastFeedTime(LocalDateTime.now());
        userPet.setCurrentExp(userPet.getCurrentExp() + PetEvolutionEngine.FEED_EXP);
        checkAndApplyLevelUp(userPet);
        userPetMapper.updateById(userPet);

        Map<String, Object> result = new HashMap<>();
        result.put("hunger", userPet.getHunger());
        result.put("mood", userPet.getMood());
        result.put("energy", userPet.getEnergy());
        result.put("exp", userPet.getCurrentExp());
        result.put("level", userPet.getCurrentLevel());
        result.put("message", item.getItemName() + " 使用成功！");
        publishPetStatus(userId);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> playWithPet(Long userId) {
        UserPet userPet = findUserPetOrThrow(userId);

        int energy = userPet.getEnergy() != null ? userPet.getEnergy() : 100;
        if (energy < 10) {
            throw new BusinessException("宠物太累了，需要休息或吃点东西");
        }

        userPet.setEnergy(Math.max(0, energy - 15));
        userPet.setMood(Math.min(4, userPet.getMood() + 1));
        userPet.setHunger(Math.max(0, userPet.getHunger() - 5));
        userPet.setLastPlayTime(LocalDateTime.now());
        userPet.setCurrentExp(userPet.getCurrentExp() + PetEvolutionEngine.PLAY_EXP);
        checkAndApplyLevelUp(userPet);
        userPetMapper.updateById(userPet);

        Map<String, Object> result = new HashMap<>();
        result.put("mood", userPet.getMood());
        result.put("energy", userPet.getEnergy());
        result.put("hunger", userPet.getHunger());
        result.put("exp", userPet.getCurrentExp());
        result.put("level", userPet.getCurrentLevel());
        result.put("message", "和宠物玩耍了一会儿！");
        publishPetStatus(userId);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> bathPet(Long userId) {
        UserPet userPet = findUserPetOrThrow(userId);

        userPet.setMood(Math.min(4, userPet.getMood() + 1));
        int curEnergy = userPet.getEnergy() != null ? userPet.getEnergy() : 100;
        userPet.setEnergy(Math.min(100, curEnergy + 10));
        userPet.setHunger(Math.max(0, userPet.getHunger() - 3));
        userPet.setLastBathTime(LocalDateTime.now());
        userPet.setCurrentExp(userPet.getCurrentExp() + PetEvolutionEngine.BATH_EXP);
        checkAndApplyLevelUp(userPet);
        userPetMapper.updateById(userPet);

        Map<String, Object> result = new HashMap<>();
        result.put("mood", userPet.getMood());
        result.put("energy", userPet.getEnergy());
        result.put("hunger", userPet.getHunger());
        result.put("exp", userPet.getCurrentExp());
        result.put("level", userPet.getCurrentLevel());
        result.put("message", "洗了个舒服的热水澡！");
        publishPetStatus(userId);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> dressPet(Long userId, List<Long> decorationIds) {
        UserPet userPet = findUserPetOrThrow(userId);

        if (decorationIds != null && !decorationIds.isEmpty()) {
            for (Long decoId : decorationIds) {
                UserDecorationInventory inv = decoInventoryMapper.selectOne(
                    new LambdaQueryWrapper<UserDecorationInventory>()
                        .eq(UserDecorationInventory::getUserId, userId)
                        .eq(UserDecorationInventory::getDecorationId, decoId)
                );
                if (inv == null || inv.getQuantity() <= 0) {
                    PetDecoration deco = decorationMapper.selectById(decoId);
                    throw new BusinessException("还未拥有装饰: " + (deco != null ? deco.getDecoName() : decoId));
                }
            }
        }

        userPet.setWearDecorationIds(decorationIds != null && !decorationIds.isEmpty()
            ? decorationIds.stream().map(String::valueOf).collect(Collectors.joining(","))
            : "");
        userPetMapper.updateById(userPet);

        Map<String, Object> result = new HashMap<>();
        result.put("wearDecorationIds", decorationIds != null ? decorationIds : Collections.emptyList());
        result.put("message", "换装成功！");
        publishPetStatus(userId);
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
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("购买数量必须大于0");
        }
        PetItem item = petItemMapper.selectById(itemId);
        if (item == null) throw new BusinessException("道具不存在");

        int totalCost = item.getPrice() * quantity;
        User user = userMapper.selectById(userId);
        int currentGold = user.getGold() != null ? user.getGold() : 0;
        if (currentGold < totalCost) throw new BusinessException("金币不足");

        // Atomic update to prevent race condition
        int updated = userMapper.update(null, new UpdateWrapper<User>()
            .eq("id", userId)
            .ge("gold", totalCost)
            .setSql("gold = gold - " + totalCost));
        if (updated == 0) throw new BusinessException("金币不足");
        user.setGold(currentGold - totalCost);

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
        realtimeEventPublisher.publishBalance(userId, user.getGold(), user.getDiamond());
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
            map.put("effectValue", item.getEffectValue());
            map.put("effectDesc", item.getEffectDesc());
            map.put("price", item.getPrice());
            result.add(map);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getDecorations(String slot) {
        LambdaQueryWrapper<PetDecoration> wrapper = new LambdaQueryWrapper<PetDecoration>()
            .eq(PetDecoration::getStatus, 1)
            .eq(slot != null && !slot.isEmpty(), PetDecoration::getSlot, slot);
        List<PetDecoration> decos = decorationMapper.selectList(wrapper);
        return decos.stream().map(d -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", d.getId());
            map.put("decoCode", d.getDecoCode());
            map.put("decoName", d.getDecoName());
            map.put("slot", d.getSlot());
            map.put("imageUrl", d.getImageUrl());
            map.put("layerOrder", d.getLayerOrder());
            map.put("priceGold", d.getPriceGold());
            map.put("priceDiamond", d.getPriceDiamond());
            map.put("rarity", d.getRarity());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> buyDecoration(Long userId, Long decorationId) {
        PetDecoration deco = decorationMapper.selectById(decorationId);
        if (deco == null) throw new BusinessException("装饰品不存在");

        User user = userMapper.selectById(userId);
        if (deco.getPriceGold() > 0) {
            int currentGold = user.getGold() != null ? user.getGold() : 0;
            if (currentGold < deco.getPriceGold()) throw new BusinessException("金币不足");
            int updated = userMapper.update(null, new UpdateWrapper<User>()
                .eq("id", userId)
                .ge("gold", deco.getPriceGold())
                .setSql("gold = gold - " + deco.getPriceGold()));
            if (updated == 0) throw new BusinessException("金币不足");
            user.setGold(currentGold - deco.getPriceGold());
        } else if (deco.getPriceDiamond() > 0) {
            int currentDiamond = user.getDiamond() != null ? user.getDiamond() : 0;
            if (currentDiamond < deco.getPriceDiamond()) throw new BusinessException("钻石不足");
            int updated = userMapper.update(null, new UpdateWrapper<User>()
                .eq("id", userId)
                .ge("diamond", deco.getPriceDiamond())
                .setSql("diamond = diamond - " + deco.getPriceDiamond()));
            if (updated == 0) throw new BusinessException("钻石不足");
            user.setDiamond(currentDiamond - deco.getPriceDiamond());
        } else {
            userMapper.updateById(user);
        }

        UserDecorationInventory inv = decoInventoryMapper.selectOne(
            new LambdaQueryWrapper<UserDecorationInventory>()
                .eq(UserDecorationInventory::getUserId, userId)
                .eq(UserDecorationInventory::getDecorationId, decorationId)
        );
        if (inv != null) {
            inv.setQuantity(inv.getQuantity() + 1);
            decoInventoryMapper.updateById(inv);
        } else {
            inv = new UserDecorationInventory();
            inv.setUserId(userId);
            inv.setDecorationId(decorationId);
            inv.setQuantity(1);
            decoInventoryMapper.insert(inv);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("gold", user.getGold());
        result.put("diamond", user.getDiamond());
        result.put("message", "购买成功！");
        realtimeEventPublisher.publishBalance(userId, user.getGold(), user.getDiamond());
        return result;
    }

    @Override
    public List<Map<String, Object>> getDecorationInventory(Long userId) {
        List<UserDecorationInventory> invList = decoInventoryMapper.selectList(
            new LambdaQueryWrapper<UserDecorationInventory>()
                .eq(UserDecorationInventory::getUserId, userId)
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserDecorationInventory inv : invList) {
            PetDecoration deco = decorationMapper.selectById(inv.getDecorationId());
            if (deco == null) continue;
            Map<String, Object> map = new HashMap<>();
            map.put("id", inv.getId());
            map.put("decorationId", deco.getId());
            map.put("decoCode", deco.getDecoCode());
            map.put("decoName", deco.getDecoName());
            map.put("slot", deco.getSlot());
            map.put("imageUrl", deco.getImageUrl());
            map.put("rarity", deco.getRarity());
            map.put("quantity", inv.getQuantity());
            result.add(map);
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> initPet(Long userId) {
        UserPet existing = userPetMapper.selectOne(
            new LambdaQueryWrapper<UserPet>().eq(UserPet::getUserId, userId).last("LIMIT 1")
        );
        if (existing != null) {
            return getMyPet(userId);
        }

        Pet defaultPet = petMapper.selectOne(
            new LambdaQueryWrapper<Pet>().eq(Pet::getIsDefault, 1).eq(Pet::getStatus, 1).last("LIMIT 1")
        );
        if (defaultPet == null) {
            throw new BusinessException("系统暂无默认宠物");
        }

        UserPet userPet = new UserPet();
        userPet.setUserId(userId);
        userPet.setPetId(defaultPet.getId());
        userPet.setCurrentLevel(1);
        userPet.setCurrentExp(0);
        userPet.setHunger(80);
        userPet.setMood(3);
        userPet.setEnergy(100);
        userPet.setCurrentImageUrl(defaultPet.getBaseImageUrl());
        userPet.setLastFeedTime(LocalDateTime.now());
        userPet.setLastPlayTime(LocalDateTime.now());
        userPet.setLastBathTime(LocalDateTime.now());
        userPetMapper.insert(userPet);

        return getMyPet(userId);
    }

    private UserPet findUserPetOrThrow(Long userId) {
        UserPet userPet = userPetMapper.selectOne(
            new LambdaQueryWrapper<UserPet>().eq(UserPet::getUserId, userId).last("LIMIT 1")
        );
        if (userPet == null) throw new BusinessException("没有宠物，请先初始化");
        return userPet;
    }

    private void checkAndApplyLevelUp(UserPet userPet) {
        int newLevel = PetEvolutionEngine.calculateLevel(userPet.getCurrentExp());
        if (newLevel > userPet.getCurrentLevel()) {
            userPet.setCurrentLevel(newLevel);

            List<PetEvolution> evolutions = evolutionMapper.selectList(
                new LambdaQueryWrapper<PetEvolution>()
                    .eq(PetEvolution::getPetId, userPet.getPetId())
                    .orderByDesc(PetEvolution::getEvolveLevel)
            );
            for (PetEvolution evo : evolutions) {
                if (newLevel >= evo.getEvolveLevel()) {
                    userPet.setCurrentImageUrl(evo.getImageUrl());
                    break;
                }
            }
        }
    }

    @Override
    @Transactional
    public void addPetExp(Long userId, int exp) {
        UserPet userPet = userPetMapper.selectOne(
            new LambdaQueryWrapper<UserPet>().eq(UserPet::getUserId, userId).last("LIMIT 1")
        );
        if (userPet == null) return;
        userPet.setCurrentExp(userPet.getCurrentExp() + exp);
        checkAndApplyLevelUp(userPet);
        userPetMapper.updateById(userPet);
    }

    private void publishPetStatus(Long userId) {
        realtimeEventPublisher.publishPetStatus(userId, getMyPet(userId));
    }

    @Override
    public List<Map<String, Object>> getAvailablePets() {
        List<Pet> pets = petMapper.selectList(
            new LambdaQueryWrapper<Pet>().eq(Pet::getStatus, 1).orderByAsc(Pet::getId)
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (Pet pet : pets) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", pet.getId());
            map.put("petCode", pet.getPetCode());
            map.put("petName", pet.getPetName());
            map.put("petType", pet.getPetType());
            map.put("imageUrl", pet.getBaseImageUrl() != null ? pet.getBaseImageUrl() : "🐱");
            result.add(map);
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> selectPet(Long userId, Long petId) {
        Pet pet = petMapper.selectById(petId);
        if (pet == null || pet.getStatus() != 1) {
            throw new BusinessException("宠物不存在或已下架");
        }

        UserPet userPet = userPetMapper.selectOne(
            new LambdaQueryWrapper<UserPet>().eq(UserPet::getUserId, userId).last("LIMIT 1")
        );
        if (userPet == null) {
            userPet = new UserPet();
            userPet.setUserId(userId);
            userPet.setCurrentLevel(1);
            userPet.setCurrentExp(0);
            userPet.setHunger(80);
            userPet.setMood(3);
            userPet.setEnergy(100);
            userPet.setLastFeedTime(LocalDateTime.now());
            userPet.setLastPlayTime(LocalDateTime.now());
            userPet.setLastBathTime(LocalDateTime.now());
        }
        userPet.setPetId(petId);
        userPet.setCurrentImageUrl(pet.getBaseImageUrl() != null ? pet.getBaseImageUrl() : "🐱");

        if (userPet.getId() == null) {
            userPetMapper.insert(userPet);
        } else {
            userPetMapper.updateById(userPet);
        }
        return getMyPet(userId);
    }
}
