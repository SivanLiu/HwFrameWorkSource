package com.android.server.radar;

import android.common.HwFrameworkFactory;
import android.util.LogException;
import android.util.Slog;
import java.util.HashMap;

public class FrameworkRadar {
    private static final int BODY_MAX_SIZE = 512;
    private static boolean DEBUG = false;
    public static final int LEVEL_A = 65;
    public static final int LEVEL_B = 66;
    public static final int LEVEL_C = 67;
    private static final long ONE_DAY_MILL_SEC = (DEBUG ? 1000 : 86400000);
    public static final int RADAR_FWK_ERR_APP_CRASH_AT_START = 2802;
    public static final int RADAR_FWK_ERR_INSTALL_SD = 2700;
    public static final int RADAR_FWK_ERR_INSTALL_SHARED_UID = 2701;
    private static final String TAG = "FrameworkRadar";
    private static LogException mLogException = HwFrameworkFactory.getLogException();
    private static HashMap<Integer, Long> mLogTime = new HashMap();

    /* JADX WARNING: Missing block: B:16:0x003f, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean isIntervalLargeEnough(int scene) {
        synchronized (mLogTime) {
            if (mLogTime.get(Integer.valueOf(scene)) != null) {
                if (System.currentTimeMillis() - (mLogTime.get(Integer.valueOf(scene)) == null ? 0 : ((Long) mLogTime.get(Integer.valueOf(scene))).longValue()) < ONE_DAY_MILL_SEC) {
                    return false;
                }
            }
        }
    }

    private static void updateSceneTimestamp(int scene) {
        synchronized (mLogTime) {
            mLogTime.put(Integer.valueOf(scene), Long.valueOf(System.currentTimeMillis()));
        }
    }

    public static void msg(RadarHeader radarHeader, String reason) {
        if (radarHeader != null && mLogException != null && reason != null) {
            int scene = radarHeader.getScene();
            if (isIntervalLargeEnough(scene)) {
                String header = radarHeader.getRadarHeader();
                String body = new StringBuilder(512);
                body.append("Reason:");
                body.append(reason);
                body.append("\n");
                body = body.toString();
                mLogException.msg("framework", radarHeader.getLevel(), header, body);
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(header);
                    stringBuilder.append(body);
                    Slog.w(str, stringBuilder.toString());
                }
                updateSceneTimestamp(scene);
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0068, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void msg(int level, int scene, String func, String reason) {
        if (func != null && mLogException != null && isIntervalLargeEnough(scene)) {
            String header = new RadarHeader("PMS", "0", 100, scene, level).getRadarHeader();
            String body = new StringBuilder(512);
            body.append("Failfunc:");
            body.append(func);
            body.append(";");
            body.append("Reason:");
            body.append(reason);
            body.append("\n");
            body = body.toString();
            mLogException.msg("framework", level, header, body);
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(header);
                stringBuilder.append(body);
                Slog.w(str, stringBuilder.toString());
            }
            updateSceneTimestamp(scene);
        }
    }
}
