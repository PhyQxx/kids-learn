package com.kidslearn.api.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kidslearn.api.entity.Question;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {}
