package com.kidslearn.api.service;

import com.kidslearn.common.result.PageResult;

import java.util.List;
import java.util.Map;

public interface PetService {

    Map<String, Object> getMyPet(Long userId);

    Map<String, Object> feedPet(Long userId, Long petItemId);

    Map<String, Object> playWithPet(Long userId);

    Map<String, Object> dressPet(Long userId, List<Long> decorationIds);

    List<Map<String, Object>> getShopItems(Integer itemType);

    Map<String, Object> buyItem(Long userId, Long itemId, Integer quantity);

    List<Map<String, Object>> getInventory(Long userId);

    Map<String, Object> bathPet(Long userId);

    List<Map<String, Object>> getDecorations(String slot);

    Map<String, Object> buyDecoration(Long userId, Long decorationId);

    List<Map<String, Object>> getDecorationInventory(Long userId);

    Map<String, Object> initPet(Long userId);

    void addPetExp(Long userId, int exp);

    List<Map<String, Object>> getAvailablePets();

    Map<String, Object> selectPet(Long userId, Long petId);
}
