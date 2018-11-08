package huawei.android.common;

import android.common.HwFrameworkMonitor;
import android.content.Intent;
import android.os.Bundle;
import android.util.IMonitor;
import android.util.IMonitor.EventStream;
import android.util.Log;
import android.util.Slog;
import huawei.android.security.facerecognition.FaceCamera;

public class HwFrameworkMonitorImpl implements HwFrameworkMonitor {
    private static final int MAX_REASON_LEN = 512;
    private static final String TAG = "HwFrameworkMonitor";
    private static HwFrameworkMonitorImpl mInstance = null;

    private HwFrameworkMonitorImpl() {
    }

    public static synchronized HwFrameworkMonitorImpl getInstance() {
        HwFrameworkMonitorImpl hwFrameworkMonitorImpl;
        synchronized (HwFrameworkMonitorImpl.class) {
            if (mInstance == null) {
                mInstance = new HwFrameworkMonitorImpl();
            }
            hwFrameworkMonitorImpl = mInstance;
        }
        return hwFrameworkMonitorImpl;
    }

    public boolean monitor(int sceneId, Bundle params) {
        EventStream eventStream = IMonitor.openEventStream(sceneId);
        if (eventStream == null) {
            return false;
        }
        if (params != null) {
            switch (sceneId) {
                case 907034001:
                    int errorType = params.getInt("errorType", FaceCamera.RET_OPEN_CAMERA_FAILED);
                    eventStream.setParam((short) 0, errorType);
                    Exception e = (Exception) params.getSerializable("reason");
                    if (e == null) {
                        e = new Exception();
                    }
                    String reason = Log.getStackTraceString(e).trim();
                    if (reason.length() > 512) {
                        reason = reason.substring(0, 512);
                    }
                    eventStream.setParam((short) 1, reason);
                    Slog.i(TAG, "monitorCheckPassword: errorType=" + errorType + ", reason=" + reason);
                    break;
                case 907400000:
                    eventStream.setParam((short) 0, params.getString("package", "unknown"));
                    eventStream.setParam((short) 1, params.getString("versionName", "unknown"));
                    eventStream.setParam((short) 3, params.getString("extra", "unknown"));
                    break;
                case 907400002:
                    eventStream.setParam((short) 0, params.getString("package", "unknown"));
                    eventStream.setParam((short) 1, params.getString("versionName", "unknown"));
                    eventStream.setParam((short) 3, params.getString("action", "unknown"));
                    eventStream.setParam((short) 4, params.getInt("actionCount", 0));
                    eventStream.setParam((short) 5, Boolean.valueOf(params.getBoolean("mmsFlag", false)));
                    eventStream.setParam((short) 6, params.getString("receiver", "unknown"));
                    eventStream.setParam((short) 7, params.getString("package", "unknown"));
                    break;
                case 907400003:
                    eventStream.setParam((short) 0, params.getString("package", "unknown"));
                    eventStream.setParam((short) 1, params.getString("versionName", "unknown"));
                    eventStream.setParam((short) 3, params.getString("action", "unknown"));
                    eventStream.setParam((short) 4, params.getFloat("receiveTime", 0.0f));
                    eventStream.setParam((short) 5, params.getString("receiver", "unknown"));
                    Object objIntent = params.getParcelable("intent");
                    if (objIntent != null) {
                        eventStream.setParam((short) 6, ((Intent) objIntent).toString());
                        break;
                    }
                    break;
                case 907400016:
                    eventStream.setParam((short) 0, params.getString("cpuState", "unknown"));
                    eventStream.setParam((short) 1, params.getString("cpuTime", "unknown"));
                    eventStream.setParam((short) 2, params.getString("extra", "unknown"));
                    break;
                case 907400018:
                    eventStream.setParam((short) 0, params.getString("component", "unknown"));
                    eventStream.setParam((short) 1, params.getString("reason", "unknown"));
                    break;
            }
        }
        boolean result = IMonitor.sendEvent(eventStream);
        Slog.i(TAG, "Monitor for " + sceneId + ", result=" + result);
        IMonitor.closeEventStream(eventStream);
        return result;
    }
}
