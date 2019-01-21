package android.cover;

import android.content.Context;
import android.cover.ICoverManager.Stub;
import android.os.IBinder;
import android.os.ServiceManager;
import android.view.View;
import android.view.WindowManager.LayoutParams;

@Deprecated
public class CoverManager implements IHwCoverManager {
    public static final String COVER_COVER_CLOCK_ACTION = "com.huawei.android.start.CoverClock";
    public static final String COVER_NAME_PREFIX = "Cover:";
    public static final String COVER_SERVICE = "cover";
    public static final String COVER_STATE = "coverOpen";
    public static final String COVER_STATE_CHANGED_ACTION = "com.huawei.android.cover.STATE";
    public static final int DEFAULT_COLOR = -16777216;
    public static final String HALL_STATE_RECEIVER_ASSOCIATED = "associated";
    public static final String HALL_STATE_RECEIVER_AUDIO = "audioserver";
    public static final String HALL_STATE_RECEIVER_CAMERA = "cameraserver";
    public static final String HALL_STATE_RECEIVER_DEFINE = "android";
    public static final String HALL_STATE_RECEIVER_FACE = "facerecognize";
    public static final String HALL_STATE_RECEIVER_GETSTATE = "getstate";
    public static final String HALL_STATE_RECEIVER_PHONE = "com.android.phone";
    private static final String KEYGUARD_PERMISSION = "android.permission.CONTROL_KEYGUARD";
    private static final String TAG = "CoverManger";
    private static final Object mInstanceSync = new Object();
    private static ICoverManager sCoverManagerService;
    private static volatile CoverManager sSelf = null;
    private Context mContext;
    private LayoutParams mCoverItemparams;

    private static ICoverManager getCoverManagerService() {
        synchronized (mInstanceSync) {
            if (sCoverManagerService != null) {
                ICoverManager iCoverManager = sCoverManagerService;
                return iCoverManager;
            }
            sCoverManagerService = Stub.asInterface(ServiceManager.getService(COVER_SERVICE));
            ICoverManager iCoverManager2 = sCoverManagerService;
            return iCoverManager2;
        }
    }

    public static CoverManager getDefault() {
        throw new RuntimeException("Stub!");
    }

    public boolean isCoverOpen() {
        throw new RuntimeException("Stub!");
    }

    public boolean setCoverForbiddened(boolean forbiddened) {
        throw new RuntimeException("Stub!");
    }

    public void addCoverItemView(View view, boolean isNeed) {
        throw new RuntimeException("Stub!");
    }

    public void addCoverItemView(View view, boolean isNeed, int activTime) {
        throw new RuntimeException("Stub!");
    }

    public void addCoverItemView(View view, boolean isNeed, boolean mDisablePower) {
        throw new RuntimeException("Stub!");
    }

    public void addCoverItemView(View view, boolean isNeed, boolean mDisablePower, int activTime) {
        throw new RuntimeException("Stub!");
    }

    public void removeCoverItemView(View view) {
        throw new RuntimeException("Stub!");
    }

    public void setCoverViewBinder(IBinder binder, Context context) {
        throw new RuntimeException("Stub!");
    }

    public int getHallState(int hallType) {
        throw new RuntimeException("Stub!");
    }

    public boolean registerHallCallback(String receiverName, int hallType, IHallCallback callback) {
        throw new RuntimeException("Stub!");
    }

    public boolean unRegisterHallCallback(String receiverName, int hallType) {
        throw new RuntimeException("Stub!");
    }

    public boolean unRegisterHallCallbackEx(int hallType, IHallCallback callback) {
        throw new RuntimeException("Stub!");
    }
}
