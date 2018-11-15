package com.android.commands.monkey;

import android.app.IActivityManager;
import android.util.Log;
import android.view.IWindowManager;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MonkeyGetFrameRateEvent extends MonkeyEvent {
    private static final String LOG_FILE = "/sdcard/avgFrameRateOut.txt";
    private static final Pattern NO_OF_FRAMES_PATTERN = Pattern.compile(".*\\(([a-f[A-F][0-9]].*?)\\s.*\\)");
    private static final String TAG = "MonkeyGetFrameRateEvent";
    private static float mDuration;
    private static int mEndFrameNo;
    private static long mEndTime;
    private static int mStartFrameNo;
    private static long mStartTime;
    private static String mTestCaseName = null;
    private String GET_FRAMERATE_CMD = "service call SurfaceFlinger 1013";
    private String mStatus;

    public MonkeyGetFrameRateEvent(String status, String testCaseName) {
        super(4);
        this.mStatus = status;
        mTestCaseName = testCaseName;
    }

    public MonkeyGetFrameRateEvent(String status) {
        super(4);
        this.mStatus = status;
    }

    private float getAverageFrameRate(int totalNumberOfFrame, float duration) {
        if (duration > 0.0f) {
            return ((float) totalNumberOfFrame) / duration;
        }
        return 0.0f;
    }

    private void writeAverageFrameRate() {
        StringBuilder stringBuilder;
        FileWriter writer = null;
        int totalNumberOfFrame = 0;
        try {
            writer = new FileWriter(LOG_FILE, true);
            float avgFrameRate = getAverageFrameRate(mEndFrameNo - mStartFrameNo, mDuration);
            writer.write(String.format("%s:%.2f\n", new Object[]{mTestCaseName, Float.valueOf(avgFrameRate)}));
            writer.close();
            try {
                writer.close();
            } catch (IOException e) {
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("IOException ");
                stringBuilder2.append(e.toString());
                Log.e(str, stringBuilder2.toString());
            }
        } catch (IOException e2) {
            Log.w(TAG, "Can't write sdcard log file", e2);
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e22) {
                    String str2 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("IOException ");
                    stringBuilder.append(e22.toString());
                    Log.e(str2, stringBuilder.toString());
                }
            }
        } catch (Throwable th) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e3) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("IOException ");
                    stringBuilder.append(e3.toString());
                    Log.e(TAG, stringBuilder.toString());
                }
            }
        }
    }

    private String getNumberOfFrames(String input) {
        Matcher m = NO_OF_FRAMES_PATTERN.matcher(input);
        if (m.matches()) {
            return m.group(1);
        }
        return null;
    }

    public int injectEvent(IWindowManager iwm, IActivityManager iam, int verbose) {
        Process p = null;
        BufferedReader result = null;
        try {
            p = Runtime.getRuntime().exec(this.GET_FRAMERATE_CMD);
            if (p.waitFor() != 0) {
                Logger.err.println(String.format("// Shell command %s status was %s", new Object[]{this.GET_FRAMERATE_CMD, Integer.valueOf(status)}));
            }
            result = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String output = result.readLine();
            if (output != null) {
                if (this.mStatus == "start") {
                    mStartFrameNo = Integer.parseInt(getNumberOfFrames(output), 16);
                    mStartTime = System.currentTimeMillis();
                } else if (this.mStatus == "end") {
                    mEndFrameNo = Integer.parseInt(getNumberOfFrames(output), 16);
                    mEndTime = System.currentTimeMillis();
                    mDuration = (float) (((double) (mEndTime - mStartTime)) / 1000.0d);
                    writeAverageFrameRate();
                }
            }
            try {
                result.close();
                if (p != null) {
                    p.destroy();
                }
            } catch (IOException e) {
                Logger.err.println(e.toString());
            }
        } catch (Exception e2) {
            Logger logger = Logger.err;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("// Exception from ");
            stringBuilder.append(this.GET_FRAMERATE_CMD);
            stringBuilder.append(":");
            logger.println(stringBuilder.toString());
            Logger.err.println(e2.toString());
            if (result != null) {
                result.close();
            }
            if (p != null) {
                p.destroy();
            }
        } catch (Throwable th) {
            if (result != null) {
                try {
                    result.close();
                } catch (IOException e3) {
                    Logger.err.println(e3.toString());
                }
            }
            if (p != null) {
                p.destroy();
            }
        }
        return 1;
    }
}
