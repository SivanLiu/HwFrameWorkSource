package com.android.commands.monkey;

import android.app.IActivityManager;
import android.os.Environment;
import android.util.Log;
import android.view.IWindowManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MonkeyGetAppFrameRateEvent extends MonkeyEvent {
    private static final String LOG_FILE = new File(Environment.getExternalStorageDirectory(), "avgAppFrameRateOut.txt").getAbsolutePath();
    private static final Pattern NO_OF_FRAMES_PATTERN = Pattern.compile(".* ([0-9]*) frames rendered");
    private static final String TAG = "MonkeyGetAppFrameRateEvent";
    private static String sActivityName = null;
    private static float sDuration;
    private static int sEndFrameNo;
    private static long sEndTime;
    private static int sStartFrameNo;
    private static long sStartTime;
    private static String sTestCaseName = null;
    private String GET_APP_FRAMERATE_TMPL = "dumpsys gfxinfo %s";
    private String mStatus;

    public MonkeyGetAppFrameRateEvent(String status, String activityName, String testCaseName) {
        super(4);
        this.mStatus = status;
        sActivityName = activityName;
        sTestCaseName = testCaseName;
    }

    public MonkeyGetAppFrameRateEvent(String status, String activityName) {
        super(4);
        this.mStatus = status;
        sActivityName = activityName;
    }

    public MonkeyGetAppFrameRateEvent(String status) {
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
        FileWriter writer = null;
        int totalNumberOfFrame = 0;
        String str;
        StringBuilder stringBuilder;
        try {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("file: ");
            stringBuilder.append(LOG_FILE);
            Log.w(str, stringBuilder.toString());
            writer = new FileWriter(LOG_FILE, true);
            float avgFrameRate = getAverageFrameRate(sEndFrameNo - sStartFrameNo, sDuration);
            writer.write(String.format("%s:%.2f\n", new Object[]{sTestCaseName, Float.valueOf(avgFrameRate)}));
            try {
                writer.close();
            } catch (IOException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("IOException ");
                stringBuilder2.append(e.toString());
                Log.e(str2, stringBuilder2.toString());
            }
        } catch (IOException e2) {
            Log.w(TAG, "Can't write sdcard log file", e2);
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e22) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("IOException ");
                    stringBuilder.append(e22.toString());
                    Log.e(str, stringBuilder.toString());
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

    private String getNumberOfFrames(BufferedReader reader) throws IOException {
        while (true) {
            String readLine = reader.readLine();
            String line = readLine;
            if (readLine == null) {
                return null;
            }
            Matcher m = NO_OF_FRAMES_PATTERN.matcher(line);
            if (m.matches()) {
                return m.group(1);
            }
        }
    }

    public int injectEvent(IWindowManager iwm, IActivityManager iam, int verbose) {
        Process p = null;
        BufferedReader result = null;
        String cmd = String.format(this.GET_APP_FRAMERATE_TMPL, new Object[]{sActivityName});
        try {
            p = Runtime.getRuntime().exec(cmd);
            if (p.waitFor() != 0) {
                Logger.err.println(String.format("// Shell command %s status was %s", new Object[]{cmd, Integer.valueOf(status)}));
            }
            result = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String output = getNumberOfFrames(result);
            if (output != null) {
                if ("start".equals(this.mStatus)) {
                    sStartFrameNo = Integer.parseInt(output);
                    sStartTime = System.currentTimeMillis();
                } else if ("end".equals(this.mStatus)) {
                    sEndFrameNo = Integer.parseInt(output);
                    sEndTime = System.currentTimeMillis();
                    sDuration = (float) (((double) (sEndTime - sStartTime)) / 1000.0d);
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
            stringBuilder.append(cmd);
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
