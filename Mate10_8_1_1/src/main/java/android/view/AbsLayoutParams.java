package android.view;

public abstract class AbsLayoutParams {
    public static final int FLAG_DESTORY_SURFACE = 2;
    public static final int FLAG_DIM_BEHIND_REMOVED = 1048576;
    public static final int FLAG_EMUI_LIGHT_STYLE = 16;
    public static final int FLAG_EXCLUDE_TRANSFER_EVENT = 131072;
    public static final int FLAG_GESTURE_NAV_DISABLE_BACK = 262144;
    public static final int FLAG_GESTURE_NAV_DISABLE_BOTTOM = 524288;
    public static final int FLAG_GESTURE_NAV_WINDOW = 2097152;
    public static final int FLAG_KEYEVENT_PASS_TO_USER_HOME = Integer.MIN_VALUE;
    public static final int FLAG_MMI_TEST_DEFAULT_SHAPE = 16384;
    public static final int FLAG_MMI_TEST_VOLUME_UP_DOWN = 8;
    public static final int FLAG_NOTCH_SUPPORT = 65536;
    public static final int FLAG_SECURE_SCREENCAP = 8192;
    public static final int FLAG_SECURE_SCREENSHOT = 4096;
    public static final int FLAG_SHARE_DIALOG = 1;
    public static final int FLAG_STATUS_BAR_PANEL_EXPANDED = 4;
    public static final int FLAG_WINDOW_CHANGED = 32768;
    public static final int SYSTEM_UI_FLAG_IMMERSIVE_GESTURE_ISOLATED = 512;

    public int getHwFlags() {
        return 0;
    }

    public void addHwFlags(int hwFlags) {
    }

    public void clearHwFlags(int hwFlags) {
    }
}
