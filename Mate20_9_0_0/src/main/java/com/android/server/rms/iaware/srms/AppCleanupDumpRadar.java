package com.android.server.rms.iaware.srms;

import android.app.mtm.iaware.appmng.AppMngConstant.AppCleanSource;
import android.app.mtm.iaware.appmng.AppMngConstant.CleanReason;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.rms.iaware.AwareConstant;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import com.android.internal.os.SomeArgs;
import com.android.server.mtm.iaware.appmng.AwareProcessBlockInfo;
import com.android.server.mtm.iaware.appmng.rule.RuleParserUtil.AppMngTag;
import com.android.server.mtm.taskstatus.ProcessCleaner.CleanType;
import com.android.server.mtm.utils.AppStatusUtils.Status;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.json.JSONException;
import org.json.JSONObject;

public class AppCleanupDumpRadar {
    private static boolean DEBUG = false;
    private static final int MSG_MEMORY_DATA = 1;
    private static final String TAG = "AppCleanupDumpRadar";
    private static final boolean isBetaUser = (AwareConstant.CURRENT_USER_TYPE == 3);
    private static volatile AppCleanupDumpRadar mCleanDumpRadar;
    private ArrayMap<String, CleanupData> mCleanupDataList = new ArrayMap();
    private long mCrashStartTime = this.mPGStartTime;
    private Handler mHandler;
    private long mMemStartTime = this.mPGStartTime;
    private long mPGStartTime = System.currentTimeMillis();
    private long mSMStartTime = this.mPGStartTime;
    private long mSmartStartTime = this.mPGStartTime;
    private long mThermalStartTime = this.mPGStartTime;

    /* renamed from: com.android.server.rms.iaware.srms.AppCleanupDumpRadar$1 */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppCleanSource = new int[AppCleanSource.values().length];

        static {
            $SwitchMap$com$android$server$mtm$iaware$appmng$rule$RuleParserUtil$AppMngTag = new int[AppMngTag.values().length];
            try {
                $SwitchMap$com$android$server$mtm$iaware$appmng$rule$RuleParserUtil$AppMngTag[AppMngTag.POLICY.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$mtm$iaware$appmng$rule$RuleParserUtil$AppMngTag[AppMngTag.LEVEL.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$server$mtm$iaware$appmng$rule$RuleParserUtil$AppMngTag[AppMngTag.STATUS.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$server$mtm$iaware$appmng$rule$RuleParserUtil$AppMngTag[AppMngTag.OVERSEA.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppCleanSource[AppCleanSource.POWER_GENIE.ordinal()] = 1;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppCleanSource[AppCleanSource.SYSTEM_MANAGER.ordinal()] = 2;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppCleanSource[AppCleanSource.CRASH.ordinal()] = 3;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppCleanSource[AppCleanSource.SMART_CLEAN.ordinal()] = 4;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppCleanSource[AppCleanSource.MEMORY.ordinal()] = 5;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppCleanSource[AppCleanSource.THERMAL.ordinal()] = 6;
            } catch (NoSuchFieldError e10) {
            }
        }
    }

    private static class CleanupData {
        private static boolean DEBUG = false;
        private static final int INVALID_VALUE = -1;
        private Map<String, Integer> mCrashCleanup;
        private int mCrashTotal;
        private Map<String, Integer> mMemoryCleanup;
        private int mMemoryTotal;
        private Map<String, Integer> mPGCleanup;
        private Map<String, Integer> mPGNCleanup;
        private int mPGTotal;
        private String mPackageName;
        private Map<String, Integer> mSMCleanup;
        private int mSMTotal;
        private Map<String, Integer> mSmartCleanup;
        private int mSmartTotal;
        private Map<String, Integer> mThermalCleanup;
        private int mThermalTotal;

        /* synthetic */ CleanupData(String x0, AnonymousClass1 x1) {
            this(x0);
        }

        private CleanupData(String pkg) {
            this.mPackageName = pkg;
            this.mPGTotal = 0;
            this.mSMTotal = 0;
            this.mCrashTotal = 0;
            this.mSmartTotal = 0;
            this.mMemoryTotal = 0;
            this.mThermalTotal = 0;
            this.mPGCleanup = new LinkedHashMap();
            this.mPGNCleanup = new LinkedHashMap();
            this.mSMCleanup = new LinkedHashMap();
            this.mCrashCleanup = new LinkedHashMap();
            this.mSmartCleanup = new LinkedHashMap();
            this.mMemoryCleanup = new LinkedHashMap();
            this.mThermalCleanup = new LinkedHashMap();
            this.mPGCleanup.put("lvl0", Integer.valueOf(0));
            this.mPGCleanup.put("lvl1", Integer.valueOf(0));
            this.mPGNCleanup.put("nlvl0", Integer.valueOf(0));
            this.mPGNCleanup.put("nlvl1", Integer.valueOf(0));
        }

        private void increase(AppCleanSource source, Map<String, Integer> data) {
            if (data != null && source != null) {
                if (DEBUG) {
                    for (Entry<String, Integer> entry : data.entrySet()) {
                        String str = AppCleanupDumpRadar.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("key = ");
                        stringBuilder.append((String) entry.getKey());
                        stringBuilder.append(", value = ");
                        stringBuilder.append(entry.getValue());
                        AwareLog.i(str, stringBuilder.toString());
                    }
                }
                switch (AnonymousClass1.$SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppCleanSource[source.ordinal()]) {
                    case 1:
                        increasePG(data);
                        break;
                    case 2:
                        increaseSM(data);
                        break;
                    case 3:
                        increaseCrash(data);
                        break;
                    case 4:
                        increaseSmart(data);
                        break;
                    case 5:
                        increaseMemory(data);
                        break;
                    case 6:
                        increaseThermal(data);
                        break;
                    default:
                        return;
                }
            }
        }

        private void addTimes(Map<String, Integer> cleanup, String key) {
            if (cleanup.containsKey(key)) {
                cleanup.put(key, Integer.valueOf(((Integer) cleanup.get(key)).intValue() + 1));
            } else {
                cleanup.put(key, Integer.valueOf(1));
            }
        }

        private String getCondition(Map<String, Integer> data) {
            StringBuilder condition = new StringBuilder("");
            Integer spec = (Integer) data.get("spec");
            if (spec != null && spec.intValue() >= 0 && spec.intValue() < CleanReason.values().length) {
                return condition.append(CleanReason.values()[spec.intValue()].getAbbr()).toString();
            }
            for (AppMngTag enums : AppMngTag.values()) {
                Integer value = (Integer) data.get(enums.getDesc());
                if (!(value == null || -1 == value.intValue())) {
                    switch (enums) {
                        case POLICY:
                        case LEVEL:
                            break;
                        case STATUS:
                            if (value.intValue() >= 0 && value.intValue() < Status.values().length) {
                                condition = condition.append(Status.values()[value.intValue()].description());
                                break;
                            }
                        case OVERSEA:
                            condition.append("|");
                            condition = condition.append(enums.getUploadBDTag());
                            break;
                        default:
                            condition.append("|");
                            condition.append(enums.getUploadBDTag());
                            condition.append(":");
                            condition = condition.append(String.valueOf(value));
                            break;
                    }
                }
            }
            return condition.toString();
        }

        private void increasePG(Map<String, Integer> data) {
            Integer policy = (Integer) data.get("policy");
            Integer level = (Integer) data.get(MemoryConstant.MEM_FILECACHE_ITEM_LEVEL);
            if (policy != null && level != null) {
                String str;
                if (CleanType.NONE.ordinal() == policy.intValue()) {
                    increasePGN(data);
                } else if (CleanType.NONE.ordinal() >= policy.intValue() || policy.intValue() >= CleanType.values().length) {
                    str = AppCleanupDumpRadar.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("policy = ");
                    stringBuilder.append(policy);
                    AwareLog.e(str, stringBuilder.toString());
                } else {
                    str = new StringBuilder();
                    str.append("lvl");
                    str.append(level);
                    str = str.toString();
                    addTimes(this.mPGCleanup, CleanType.values()[policy.intValue()].description());
                    addTimes(this.mPGCleanup, str);
                    this.mPGTotal++;
                }
            }
        }

        private void increasePGN(Map<String, Integer> data) {
            String condition = getCondition(data);
            if (condition.equals("")) {
                addTimes(this.mPGNCleanup, "others");
            } else {
                addTimes(this.mPGNCleanup, condition);
            }
            Integer level = (Integer) data.get(MemoryConstant.MEM_FILECACHE_ITEM_LEVEL);
            String strLevel = new StringBuilder();
            strLevel.append("nlvl");
            strLevel.append(level);
            addTimes(this.mPGNCleanup, strLevel.toString());
            this.mPGTotal++;
        }

        private void increaseSM(Map<String, Integer> data) {
            Integer policy = (Integer) data.get("policy");
            if (policy != null) {
                if (CleanType.NONE.ordinal() >= policy.intValue() || policy.intValue() >= CleanType.values().length) {
                    String str = AppCleanupDumpRadar.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("policy = ");
                    stringBuilder.append(policy);
                    AwareLog.e(str, stringBuilder.toString());
                } else {
                    addTimes(this.mSMCleanup, CleanType.values()[policy.intValue()].description());
                    this.mSMTotal++;
                }
            }
        }

        private void increaseCrash(Map<String, Integer> data) {
            Integer policy = (Integer) data.get("policy");
            if (policy != null) {
                String condition;
                if (CleanType.NONE.ordinal() == policy.intValue()) {
                    condition = getCondition(data);
                    if (condition.equals("")) {
                        addTimes(this.mCrashCleanup, "others");
                    } else {
                        addTimes(this.mCrashCleanup, condition);
                    }
                    this.mCrashTotal++;
                } else if (CleanType.NONE.ordinal() >= policy.intValue() || policy.intValue() >= CleanType.values().length) {
                    condition = AppCleanupDumpRadar.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("policy = ");
                    stringBuilder.append(policy);
                    AwareLog.e(condition, stringBuilder.toString());
                } else {
                    addTimes(this.mCrashCleanup, CleanType.values()[policy.intValue()].description());
                    this.mCrashTotal++;
                }
            }
        }

        private void increaseSmart(Map<String, Integer> data) {
            Integer policy = (Integer) data.get("policy");
            if (policy != null) {
                String condition;
                if (CleanType.NONE.ordinal() == policy.intValue()) {
                    condition = getCondition(data);
                    if (condition.equals("")) {
                        addTimes(this.mSmartCleanup, "others");
                    } else {
                        addTimes(this.mSmartCleanup, condition);
                    }
                    this.mSmartTotal++;
                } else if (CleanType.NONE.ordinal() >= policy.intValue() || policy.intValue() >= CleanType.values().length) {
                    condition = AppCleanupDumpRadar.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("policy = ");
                    stringBuilder.append(policy);
                    AwareLog.e(condition, stringBuilder.toString());
                } else {
                    addTimes(this.mSmartCleanup, CleanType.values()[policy.intValue()].description());
                    this.mSmartTotal++;
                }
            }
        }

        private void increaseMemory(Map<String, Integer> data) {
            Integer policy = (Integer) data.get("policy");
            if (policy != null) {
                StringBuilder stringBuilder;
                Integer level = (Integer) data.get(MemoryConstant.MEM_FILECACHE_ITEM_LEVEL);
                if (!(level == null || -1 == level.intValue())) {
                    Map map = this.mMemoryCleanup;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("lvl");
                    stringBuilder.append(level);
                    addTimes(map, stringBuilder.toString());
                }
                String condition;
                if (CleanType.NONE.ordinal() == policy.intValue()) {
                    condition = getCondition(data);
                    if (condition.equals("")) {
                        addTimes(this.mMemoryCleanup, "others");
                    } else {
                        addTimes(this.mMemoryCleanup, condition);
                    }
                    this.mMemoryTotal++;
                } else if (CleanType.NONE.ordinal() >= policy.intValue() || policy.intValue() >= CleanType.values().length) {
                    condition = AppCleanupDumpRadar.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("policy = ");
                    stringBuilder.append(policy);
                    AwareLog.e(condition, stringBuilder.toString());
                } else {
                    addTimes(this.mMemoryCleanup, CleanType.values()[policy.intValue()].description());
                    this.mMemoryTotal++;
                }
            }
        }

        private void increaseThermal(Map<String, Integer> data) {
            Integer policy = (Integer) data.get("policy");
            if (policy != null) {
                String condition;
                if (CleanType.NONE.ordinal() == policy.intValue()) {
                    condition = getCondition(data);
                    if (condition.equals("")) {
                        addTimes(this.mThermalCleanup, "others");
                    } else {
                        addTimes(this.mThermalCleanup, condition);
                    }
                    this.mThermalTotal++;
                } else if (CleanType.NONE.ordinal() >= policy.intValue() || policy.intValue() >= CleanType.values().length) {
                    condition = AppCleanupDumpRadar.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("policy = ");
                    stringBuilder.append(policy);
                    AwareLog.e(condition, stringBuilder.toString());
                } else {
                    addTimes(this.mThermalCleanup, CleanType.values()[policy.intValue()].description());
                    this.mThermalTotal++;
                }
            }
        }

        private JSONObject makeJson(int total, Map<String, Integer> cleanup) {
            JSONObject jsonObj = new JSONObject();
            if (total > 0) {
                try {
                    jsonObj.put("pkg", this.mPackageName);
                    jsonObj.put("total", total);
                    for (Entry<String, Integer> entry : cleanup.entrySet()) {
                        jsonObj.put((String) entry.getKey(), entry.getValue());
                    }
                } catch (JSONException e) {
                    String str = AppCleanupDumpRadar.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("makeJson catch JSONException e: ");
                    stringBuilder.append(e);
                    AwareLog.e(str, stringBuilder.toString());
                }
            }
            return jsonObj;
        }
    }

    private final class DumpRadarHandler extends Handler {
        public DumpRadarHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                SomeArgs args = msg.obj;
                AppCleanupDumpRadar.this.processMemoryData(args.arg1, ((Integer) args.arg2).intValue());
            }
        }
    }

    public static AppCleanupDumpRadar getInstance() {
        if (mCleanDumpRadar == null) {
            synchronized (AppCleanupDumpRadar.class) {
                if (mCleanDumpRadar == null) {
                    mCleanDumpRadar = new AppCleanupDumpRadar();
                }
            }
        }
        return mCleanDumpRadar;
    }

    private String makeCleanJson(AppCleanSource source) {
        StringBuilder cleanupStat = new StringBuilder("");
        for (Entry<String, CleanupData> item : this.mCleanupDataList.entrySet()) {
            CleanupData cleanup = (CleanupData) item.getValue();
            JSONObject smJson;
            switch (AnonymousClass1.$SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppCleanSource[source.ordinal()]) {
                case 1:
                    Map<String, Integer> pgCleanup = new LinkedHashMap();
                    pgCleanup.putAll(cleanup.mPGCleanup);
                    pgCleanup.putAll(cleanup.mPGNCleanup);
                    JSONObject pjJson = cleanup.makeJson(cleanup.mPGTotal, pgCleanup);
                    if (pjJson.length() == 0) {
                        break;
                    }
                    cleanupStat.append("\n");
                    cleanupStat.append(pjJson.toString());
                    break;
                case 2:
                    smJson = cleanup.makeJson(cleanup.mSMTotal, cleanup.mSMCleanup);
                    if (smJson.length() == 0) {
                        break;
                    }
                    cleanupStat.append("\n");
                    cleanupStat.append(smJson.toString());
                    break;
                case 3:
                    smJson = cleanup.makeJson(cleanup.mCrashTotal, cleanup.mCrashCleanup);
                    if (smJson.length() == 0) {
                        break;
                    }
                    cleanupStat.append("\n");
                    cleanupStat.append(smJson.toString());
                    break;
                case 4:
                    smJson = cleanup.makeJson(cleanup.mSmartTotal, cleanup.mSmartCleanup);
                    if (smJson.length() == 0) {
                        break;
                    }
                    cleanupStat.append("\n");
                    cleanupStat.append(smJson.toString());
                    break;
                case 5:
                    smJson = cleanup.makeJson(cleanup.mMemoryTotal, cleanup.mMemoryCleanup);
                    if (smJson.length() == 0) {
                        break;
                    }
                    cleanupStat.append("\n");
                    cleanupStat.append(smJson.toString());
                    break;
                case 6:
                    smJson = cleanup.makeJson(cleanup.mThermalTotal, cleanup.mThermalCleanup);
                    if (smJson.length() == 0) {
                        break;
                    }
                    cleanupStat.append("\n");
                    cleanupStat.append(smJson.toString());
                    break;
                default:
                    break;
            }
        }
        return cleanupStat.toString();
    }

    public String saveCleanBigData(boolean clear) {
        String stringBuilder;
        synchronized (this.mCleanupDataList) {
            long updateTime = System.currentTimeMillis();
            StringBuilder data = new StringBuilder("");
            String pgStr = makeCleanJson(AppCleanSource.POWER_GENIE);
            if (!pgStr.equals("")) {
                data.append("\n[iAwareAppPGClean_Start]");
                data.append("\nstartTime: ");
                data.append(String.valueOf(this.mPGStartTime));
                data.append(pgStr);
                data.append("\nendTime: ");
                data.append(String.valueOf(updateTime));
                data.append("\n[iAwareAppPGClean_End]");
                this.mPGStartTime = clear ? updateTime : this.mPGStartTime;
            }
            String smStr = makeCleanJson(AppCleanSource.SYSTEM_MANAGER);
            if (!smStr.equals("")) {
                data.append("\n[iAwareAppSMClean_Start]");
                data.append("\nstartTime: ");
                data.append(String.valueOf(this.mSMStartTime));
                data.append(smStr);
                data.append("\nendTime: ");
                data.append(String.valueOf(updateTime));
                data.append("\n[iAwareAppSMClean_End]");
                this.mSMStartTime = clear ? updateTime : this.mSMStartTime;
            }
            String crashStr = makeCleanJson(AppCleanSource.CRASH);
            if (!crashStr.equals("")) {
                data.append("\n[iAwareAppCrashClean_Start]");
                data.append("\nstartTime: ");
                data.append(String.valueOf(this.mCrashStartTime));
                data.append(crashStr);
                data.append("\nendTime: ");
                data.append(String.valueOf(updateTime));
                data.append("\n[iAwareAppCrashClean_End]");
                this.mCrashStartTime = clear ? updateTime : this.mCrashStartTime;
            }
            String smartStr = makeCleanJson(AppCleanSource.SMART_CLEAN);
            if (!smartStr.equals("")) {
                data.append("\n[iAwareAppSmartClean_Start]");
                data.append("\nstartTime: ");
                data.append(String.valueOf(this.mSmartStartTime));
                data.append(smartStr);
                data.append("\nendTime: ");
                data.append(String.valueOf(updateTime));
                data.append("\n[iAwareAppSmartClean_End]");
                this.mSmartStartTime = clear ? updateTime : this.mSmartStartTime;
            }
            String memStr = makeCleanJson(AppCleanSource.MEMORY);
            if (!memStr.equals("")) {
                data.append("\n[iAwareAppMemoryClean_Start]");
                data.append("\nstartTime: ");
                data.append(String.valueOf(this.mMemStartTime));
                data.append(memStr);
                data.append("\nendTime: ");
                data.append(String.valueOf(updateTime));
                data.append("\n[iAwareAppMemoryClean_End]");
                this.mMemStartTime = clear ? updateTime : this.mMemStartTime;
            }
            String thermalStr = makeCleanJson(AppCleanSource.THERMAL);
            if (!thermalStr.equals("")) {
                data.append("\n[iAwareAppThermalClean_Start]");
                data.append("\nstartTime: ");
                data.append(String.valueOf(this.mThermalStartTime));
                data.append(thermalStr);
                data.append("\nendTime: ");
                data.append(String.valueOf(updateTime));
                data.append("\n[iAwareAppThermalClean_End]");
                this.mThermalStartTime = clear ? updateTime : this.mThermalStartTime;
            }
            if (clear) {
                this.mCleanupDataList.clear();
            }
            stringBuilder = data.toString();
        }
        return stringBuilder;
    }

    public void updateCleanData(String pkg, AppCleanSource source, Map<String, Integer> data) {
        if (isBetaUser) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateCleanData pkg = ");
                stringBuilder.append(pkg);
                stringBuilder.append(", source = ");
                stringBuilder.append(source);
                stringBuilder.append(", data = ");
                stringBuilder.append(data);
                AwareLog.i(str, stringBuilder.toString());
            }
            if (pkg != null && !pkg.isEmpty()) {
                synchronized (this.mCleanupDataList) {
                    CleanupData cleanup = (CleanupData) this.mCleanupDataList.get(pkg);
                    if (cleanup != null) {
                        cleanup.increase(source, data);
                    } else {
                        cleanup = new CleanupData(pkg, null);
                        cleanup.increase(source, data);
                    }
                    this.mCleanupDataList.put(pkg, cleanup);
                }
            }
        }
    }

    public void dumpBigData(PrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("");
        stringBuilder.append(saveCleanBigData(false));
        pw.println(stringBuilder.toString());
    }

    public void setHandler(Handler handler) {
        if (handler != null) {
            this.mHandler = new DumpRadarHandler(handler.getLooper());
        }
    }

    private void processMemoryData(List<AwareProcessBlockInfo> blockInfos, int position) {
        int size = blockInfos.size();
        int i = 0;
        while (i < size) {
            AwareProcessBlockInfo info = (AwareProcessBlockInfo) blockInfos.get(i);
            if (info != null) {
                if (i <= position) {
                    info.mCleanType = CleanType.NONE;
                    info.mReason = CleanReason.MEMORY_ENOUGH.getCode();
                    HashMap<String, Integer> detailedReason = new HashMap();
                    detailedReason.put(AppMngTag.POLICY.getDesc(), Integer.valueOf(CleanType.NONE.ordinal()));
                    detailedReason.put("spec", Integer.valueOf(CleanReason.MEMORY_ENOUGH.ordinal()));
                    info.mDetailedReason = detailedReason;
                }
                updateCleanData(info.mPackageName, AppCleanSource.MEMORY, info.mDetailedReason);
                i++;
            } else {
                return;
            }
        }
    }

    public void reportMemoryData(List<AwareProcessBlockInfo> blockInfos, int position) {
        if (blockInfos != null && position < blockInfos.size() && this.mHandler != null) {
            Message msg = this.mHandler.obtainMessage();
            msg.what = 1;
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = blockInfos;
            args.arg2 = Integer.valueOf(position);
            msg.obj = args;
            this.mHandler.sendMessage(msg);
        }
    }
}
