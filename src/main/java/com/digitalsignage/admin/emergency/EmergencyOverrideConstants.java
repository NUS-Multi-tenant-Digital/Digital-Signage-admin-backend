package com.digitalsignage.admin.emergency;

/**
 * Emergency overrides are implemented as high-priority {@code schedule} rows (no dedicated table).
 */
public final class EmergencyOverrideConstants {

    public static final String SCHEDULE_NAME_PREFIX = "EMERGENCY:";

    public static final int PRIORITY = 999_999;

    public static final int DEFAULT_DURATION_MINUTES = 60;

    public static final int MAX_DURATION_MINUTES = 24 * 60;

    public static final int DEFAULT_RESOLUTION_WIDTH = 1920;

    public static final int DEFAULT_RESOLUTION_HEIGHT = 1080;

    private EmergencyOverrideConstants() {
    }

    public static String scheduleName(String displayName) {
        return SCHEDULE_NAME_PREFIX + displayName.trim();
    }

    public static boolean isEmergencyScheduleName(String name) {
        return name != null && name.startsWith(SCHEDULE_NAME_PREFIX);
    }

    public static String displayNameFromSchedule(String scheduleName) {
        if (!isEmergencyScheduleName(scheduleName)) {
            return scheduleName;
        }
        return scheduleName.substring(SCHEDULE_NAME_PREFIX.length());
    }
}
