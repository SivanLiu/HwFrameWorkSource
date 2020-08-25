package com.android.server.fsm;

import android.util.Slog;

public class SensorPostureProcess {
    private static final int ANGLE_INDEX = 6;
    private static final float EXPAND_LOWER_THRESHOLD = 50.0f;
    private static final float FOLDED_UPPER_THRESHOLD = 35.0f;
    private static final float G = 9.8f;
    private static final int G_X_MAIN_INDEX = 0;
    private static final int G_X_SUB_INDEX = 3;
    private static final int G_Y_MAIN_INDEX = 1;
    private static final int G_Y_SUB_INDEX = 4;
    private static final int G_Z_MAIN_INDEX = 2;
    private static final int G_Z_SUB_INDEX = 5;
    private static final float HALF_FOLDED_LOWER_THRESHOLD = 20.0f;
    private static final float HALF_FOLDED_UPPER_THRESHOLD = 65.0f;
    private static final float INIT_LAY_FLAT_THRESHOLD = 6.4f;
    private static final float LAY_FLAT_THRESHOLD = 0.1f;
    private static final float PORTAIT_THRESHOLD = 9.5f;
    private static final String TAG = "Fsm_SensorPostureProcess";
    private static float mThreshold = INIT_LAY_FLAT_THRESHOLD;

    public static boolean isPortraitState(float[] data) {
        float gravityMainY = data[1];
        float gravitySubY = data[4];
        if ((gravityMainY < PORTAIT_THRESHOLD || gravitySubY < PORTAIT_THRESHOLD) && (gravityMainY > -9.5f || gravitySubY > -9.5f)) {
            return false;
        }
        return true;
    }

    public static int handlePostureSensor(float[] data, int currentPosture) {
        float angle = data[6];
        if (currentPosture == 100) {
            mThreshold = INIT_LAY_FLAT_THRESHOLD;
        } else {
            mThreshold = 0.1f;
        }
        if (angle >= HALF_FOLDED_UPPER_THRESHOLD) {
            return 109;
        }
        if (angle <= HALF_FOLDED_LOWER_THRESHOLD) {
            return getFoldedPosture(data);
        }
        if (angle >= FOLDED_UPPER_THRESHOLD && angle <= EXPAND_LOWER_THRESHOLD) {
            return 106;
        }
        if (angle <= EXPAND_LOWER_THRESHOLD || angle >= HALF_FOLDED_UPPER_THRESHOLD) {
            if (currentPosture == 109 || currentPosture == 106) {
                return 106;
            }
            return getFoldedPosture(data);
        } else if (currentPosture == 109) {
            return 109;
        } else {
            return 106;
        }
    }

    private static int getFoldedPosture(float[] data) {
        float gravityMainZ = data[2];
        float gravitySubZ = data[5];
        if (isProbablyEqual(gravityMainZ, G) && isProbablyEqual(gravitySubZ, -9.8f)) {
            return 101;
        }
        if (!isProbablyEqual(gravityMainZ, -9.8f) || !isProbablyEqual(gravitySubZ, G)) {
            return 103;
        }
        return 102;
    }

    private static boolean isProbablyEqual(float data, float standardData) {
        return Math.abs(data - standardData) < mThreshold;
    }

    public static int getFoldableState(float[] data, int currentState) {
        float angle = data[6];
        if (angle <= HALF_FOLDED_LOWER_THRESHOLD) {
            return 2;
        }
        if (angle >= HALF_FOLDED_UPPER_THRESHOLD) {
            return 1;
        }
        if (angle >= FOLDED_UPPER_THRESHOLD && angle <= EXPAND_LOWER_THRESHOLD) {
            return 3;
        }
        if (angle <= HALF_FOLDED_LOWER_THRESHOLD || angle >= FOLDED_UPPER_THRESHOLD) {
            if (currentState == 1) {
                return 1;
            }
            return 3;
        } else if (currentState == 2) {
            return 2;
        } else {
            return 3;
        }
    }

    protected static void printPostureSensor(float[] data) {
        StringBuilder builder = new StringBuilder("onSensorChanged: ");
        int len = data.length;
        for (int i = 0; i < len - 1; i++) {
            builder.append(data[i]);
            builder.append(", ");
        }
        builder.append(data[len - 1]);
        Slog.i("Fsm_SensorPostureProcess", builder.toString());
    }
}
