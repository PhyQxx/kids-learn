package com.kidslearn.api.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kidslearn.api.entity.Pet;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface PetMapper extends BaseMapper<Pet> {}
