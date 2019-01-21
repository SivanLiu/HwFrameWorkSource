package android.zrhung.appeye;

import android.os.FreezeScreenScene;
import android.util.ZRHung;
import android.zrhung.ZrHungData;
import android.zrhung.ZrHungImpl;

public final class AppEyeXcollie extends ZrHungImpl {
    private static final String TAG = "ZrHung.AppEyeXcollie";
    private static AppEyeXcollie mSingleton;

    public AppEyeXcollie(String wpName) {
        super(wpName);
    }

    public static synchronized AppEyeXcollie getInstance(String wpName) {
        AppEyeXcollie appEyeXcollie;
        synchronized (AppEyeXcollie.class) {
            if (mSingleton == null) {
                mSingleton = new AppEyeXcollie(wpName);
            }
            appEyeXcollie = mSingleton;
        }
        return appEyeXcollie;
    }

    public boolean sendEvent(ZrHungData args) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("p=");
        stringBuilder.append(args.getInt(FreezeScreenScene.PID_PARAM));
        return ZRHung.sendHungEvent(ZRHung.XCOLLIE_FWK_SERVICE, stringBuilder.toString(), args.getString("stackTrace"));
    }
}
