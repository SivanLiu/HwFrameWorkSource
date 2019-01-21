package android.zrhung.appeye;

import android.util.Slog;
import android.util.ZRHung;
import android.util.ZRHung.HungConfig;
import android.zrhung.ZrHungData;
import android.zrhung.ZrHungImpl;

public final class AppBootFail extends ZrHungImpl {
    private static final String KEYWORD = "AppBootFail:";
    private static final String TAG = "ZrHung.AppBootFail";
    private static AppBootFail mAppBootFail = null;
    private static boolean mConfiged = false;
    private static boolean mEnabled = false;

    private AppBootFail(String wpName) {
        super(wpName);
    }

    public static AppBootFail getInstance(String wpName) {
        if (mAppBootFail == null) {
            mAppBootFail = new AppBootFail(wpName);
        }
        return mAppBootFail;
    }

    private boolean isEnabled() {
        if (mConfiged) {
            return mEnabled;
        }
        HungConfig cfg = ZRHung.getHungConfig((short) 264);
        if (cfg == null || cfg.value == null) {
            Slog.e(TAG, "Failed to get config from zrhung");
            return false;
        }
        mEnabled = cfg.value.split(",")[0].trim().equals("1");
        mConfiged = true;
        return mEnabled;
    }

    /* JADX WARNING: Missing block: B:15:0x0055, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean sendEvent(ZrHungData args) {
        boolean ret = false;
        if (args == null || args.getString("packageName") == null || !isEnabled()) {
            return false;
        }
        String packageName = args.getString("packageName");
        StringBuilder stringBuilder;
        try {
            stringBuilder = new StringBuilder();
            stringBuilder.append(KEYWORD);
            stringBuilder.append(packageName);
            ret = sendAppEyeEvent((short) 264, args, null, stringBuilder.toString());
            if (!ret) {
                Slog.e(TAG, " sendAppFreezeEvent failed!");
            }
        } catch (Exception ex) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("exception info ex:");
            stringBuilder.append(ex);
            Slog.e(str, stringBuilder.toString());
        }
        return ret;
    }
}
