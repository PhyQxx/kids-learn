package com.kidslearn.api.dto.learn;

import lombok.Data;
import java.util.List;

@Data
public class DailyTaskVO {
    private String date;
    private Integer totalTime;
    private List<TaskItemVO> tasks;
    private Object petStatus;

    @Data
    public static class TaskItemVO {
        private String subject;
        private String subjectName;
        private String subjectIcon;
        private Integer todayMinutes;
        private Integer targetMinutes;
        private Integer progress;
        private String status;
    }
}
