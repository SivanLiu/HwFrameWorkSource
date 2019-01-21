package android.zrhung.appeye;

import android.zrhung.ZrHungData;
import android.zrhung.ZrHungImpl;

public final class AppEyeCL extends ZrHungImpl {
    private static final String KEYWORD = "CL";
    private static final String TAG = "ZrHung.AppEyeCL";
    private static AppEyeCL mSingleton;

    public AppEyeCL(String wpName) {
        super(wpName);
    }

    public static synchronized AppEyeCL getInstance(String wpName) {
        AppEyeCL appEyeCL;
        synchronized (AppEyeCL.class) {
            if (mSingleton == null) {
                mSingleton = new AppEyeCL(wpName);
            }
            appEyeCL = mSingleton;
        }
        return appEyeCL;
    }

    public boolean sendEvent(ZrHungData args) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CL:");
        stringBuilder.append(args.getString("packageName"));
        return sendAppEyeEvent((short) 265, args, null, stringBuilder.toString());
    }
}
