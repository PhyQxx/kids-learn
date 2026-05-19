package com.kidslearn.api.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kidslearn.api.entity.ChallengeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface ChallengeRecordMapper extends BaseMapper<ChallengeRecord> {
    
    @Select("SELECT c.challenge_type, COUNT(DISTINCT cr.user_id) as player_count " +
            "FROM challenge_record cr " +
            "JOIN challenge c ON cr.challenge_id = c.id " +
            "WHERE c.status = 1 " +
            "GROUP BY c.challenge_type")
    List<Map<String, Object>> countPlayersByChallengeType();
}
