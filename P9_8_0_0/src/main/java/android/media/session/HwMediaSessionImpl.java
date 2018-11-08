package android.media.session;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioSystem;
import android.util.Log;

public class HwMediaSessionImpl implements HwMediaSessionManager {
    private static final String TAG = "HwMediaSessionImpl";
    private static HwMediaSessionManager mHwMediaSessionManager = new HwMediaSessionImpl();

    private HwMediaSessionImpl() {
    }

    public static HwMediaSessionManager getDefault() {
        return mHwMediaSessionManager;
    }

    public void updateTargetInService(PendingIntent mbr, Context context) {
        String creatorPackage = mbr != null ? mbr.getCreatorPackage() : null;
        if (creatorPackage != null && isWiredHeadsetOn() && context != null) {
            Intent intent = new Intent("com.huawei.internetaudioservice.autoaction");
            intent.setClassName("com.huawei.internetaudioservice", "com.huawei.internetaudioservice.InternetAudioService");
            Log.i(TAG, "newReceiver:" + creatorPackage);
            intent.putExtra("new_target_selected", creatorPackage);
            context.startService(intent);
        }
    }

    private boolean isWiredHeadsetOn() {
        if (AudioSystem.getDeviceConnectionState(4, "") == 0 && AudioSystem.getDeviceConnectionState(8, "") == 0) {
            return false;
        }
        return true;
    }
}
