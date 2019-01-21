package android.zrhung.appeye;

import android.os.FreezeScreenScene;
import android.rms.utils.Utils;
import android.util.Log;
import android.util.ZRHung.HungConfig;
import android.zrhung.ZrHungData;
import android.zrhung.ZrHungImpl;

public final class AppEyeANR extends ZrHungImpl {
    private static final int MANR_ARRAY_SIZE = 16;
    private static final int MANR_MILLIS_PER_SECOND = 1000;
    private static final int MANR_THRESHOLD_MAX = 10;
    private static final int MANR_THRESHOLD_MIN = 1;
    private static final String TAG = "ZrHung.AppEyeANR";
    private String[] mAnrPkgNames = new String[16];
    private long[] mAnrTimes = new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private boolean mConfiged = false;
    private int mCount = 0;
    private boolean mEnabled = false;
    private boolean mEnabledFastANR = false;
    private int mEnd = 0;
    private int mIndex = 0;
    private int mInterval = 0;
    private String[] mPackageList = null;
    private int mStart = 0;
    private int mThreshold = 0;
    private long mUploadTime = 0;

    public AppEyeANR(String wpName) {
        super(wpName);
    }

    private int parseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("parseInt NumberFormatException e = ");
            stringBuilder.append(e.getMessage());
            Log.e(str2, stringBuilder.toString());
            return -1;
        }
    }

    private boolean getCoreANRConfig() {
        boolean z = false;
        if (this.mConfiged) {
            if (this.mEnabled && this.mInterval > 0 && this.mPackageList != null && this.mThreshold > 1 && this.mThreshold < 10) {
                z = true;
            }
            return z;
        }
        HungConfig cfg = getConfig();
        if (cfg == null) {
            return false;
        }
        if (cfg.status != 0) {
            if (cfg.status != 1) {
                this.mConfiged = true;
            }
            return false;
        } else if (cfg.value == null) {
            this.mConfiged = true;
            return false;
        } else {
            this.mPackageList = cfg.value.split(",");
            if (this.mPackageList.length < 5) {
                this.mConfiged = true;
                return false;
            }
            this.mEnabled = this.mPackageList[0].trim().equals("1");
            this.mThreshold = parseInt(this.mPackageList[1].trim());
            this.mInterval = parseInt(this.mPackageList[2].trim());
            this.mEnabledFastANR = this.mPackageList[3].trim().equals("1");
            this.mConfiged = true;
            if (this.mEnabled && this.mInterval > 0 && this.mThreshold > 1 && this.mThreshold < 10) {
                z = true;
            }
            return z;
        }
    }

    private boolean isMANR(String anrInfo) {
        long currentTime = System.currentTimeMillis();
        this.mAnrTimes[this.mIndex] = currentTime;
        this.mAnrPkgNames[this.mIndex] = anrInfo;
        this.mIndex = (this.mIndex + 1) % 16;
        this.mCount++;
        if (this.mCount < this.mThreshold) {
            return false;
        }
        this.mEnd = ((this.mStart + this.mThreshold) - 1) % 16;
        if (this.mAnrTimes[this.mStart] + ((long) (this.mInterval * 1000)) < this.mAnrTimes[this.mEnd] || (this.mUploadTime != 0 && this.mUploadTime + ((long) (this.mInterval * 1000)) > this.mAnrTimes[this.mStart])) {
            this.mCount--;
            this.mStart = (this.mStart + 1) % 16;
            return false;
        }
        this.mCount = 0;
        this.mStart = (this.mStart + this.mThreshold) % 16;
        this.mUploadTime = currentTime;
        return true;
    }

    private boolean isCoreApp(String packageName) {
        for (String trim : this.mPackageList) {
            if (trim.trim().equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    public int init(ZrHungData args) {
        getCoreANRConfig();
        return 0;
    }

    public boolean check(ZrHungData args) {
        if (!getCoreANRConfig()) {
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("check:");
        stringBuilder.append(this.mEnabledFastANR);
        Log.i(str, stringBuilder.toString());
        return this.mEnabledFastANR;
    }

    /* JADX WARNING: Missing block: B:21:0x00c9, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean sendEvent(ZrHungData args) {
        if (args == null || !isZrHungDataValid(args) || !getCoreANRConfig()) {
            return false;
        }
        String packageName = args.getString("packageName");
        String activityName = args.getString("activityName");
        StringBuilder anrInfo = new StringBuilder();
        anrInfo.append(" PackageName:");
        anrInfo.append(packageName);
        anrInfo.append(" ActivityName:");
        anrInfo.append(activityName);
        StringBuilder stringBuilder;
        if ("APP_CRASH".equals(args.getString("WpName"))) {
            Log.i(TAG, "send APP_CRASH events");
            stringBuilder = new StringBuilder();
            stringBuilder.append("APP_CRASH:");
            stringBuilder.append(anrInfo.toString());
            return sendAppEyeEvent((short) 287, args, null, stringBuilder.toString());
        } else if (isCoreApp(packageName)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("CANR:");
            stringBuilder.append(anrInfo.toString());
            return sendAppEyeEvent((short) 269, args, null, stringBuilder.toString());
        } else if (isMANR(anrInfo.toString())) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("MANR:");
            stringBuilder.append(getCurrentMANRInfo());
            return sendAppEyeEvent((short) 268, args, null, stringBuilder.toString());
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("ANR:");
            stringBuilder.append(anrInfo.toString());
            return sendAppEyeEvent((short) 267, args, null, stringBuilder.toString());
        }
    }

    private String getCurrentMANRInfo() {
        StringBuilder multAnrInfo = new StringBuilder();
        int start = (this.mStart - this.mThreshold) % 16;
        int end = this.mEnd;
        if (start < 0) {
            start += 16;
        }
        while (true) {
            multAnrInfo.append("\n");
            multAnrInfo.append("time:");
            long time = this.mAnrTimes[start];
            if (time > 0) {
                multAnrInfo.append(Utils.getDateFormatValue(time, Utils.DATE_FORMAT_DETAIL));
            }
            multAnrInfo.append(this.mAnrPkgNames[start]);
            if (start == end) {
                return multAnrInfo.toString();
            }
            start = (start + 1) % 16;
        }
    }

    private boolean isZrHungDataValid(ZrHungData args) {
        if (args.getString("packageName") != null) {
            return true;
        }
        String processName = args.getString(FreezeScreenScene.PROCESS_NAME);
        if (processName == null) {
            return false;
        }
        args.putString("packageName", processName);
        return true;
    }
}
