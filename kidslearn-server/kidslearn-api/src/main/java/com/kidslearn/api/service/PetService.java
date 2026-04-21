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
}
