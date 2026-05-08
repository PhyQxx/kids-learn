package com.kidslearn.api.realtime;

public final class RealtimeMessageType {

    public static final String PET_STATUS_UPDATE = "PET_STATUS_UPDATE";
    public static final String USER_BALANCE_UPDATE = "USER_BALANCE_UPDATE";
    public static final String CHILD_ACTIVITY_UPDATE = "CHILD_ACTIVITY_UPDATE";
    public static final String PARENT_MONITOR_UPDATE = "PARENT_MONITOR_UPDATE";
    public static final String ACHIEVEMENT_UNLOCKED = "ACHIEVEMENT_UNLOCKED";
    public static final String NOTIFICATION = "NOTIFICATION";
    public static final String PONG = "PONG";

    private RealtimeMessageType() {
    }
}
