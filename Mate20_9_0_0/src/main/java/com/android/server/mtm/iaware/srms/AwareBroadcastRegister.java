package com.android.server.mtm.iaware.srms;

import android.app.mtm.iaware.appmng.AppMngConstant.AppMngFeature;
import android.rms.iaware.AwareLog;
import com.android.server.am.HwActivityManagerService;
import com.android.server.mtm.iaware.appmng.DecisionMaker;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class AwareBroadcastRegister {
    private static final String ITEM_REPORT_THRESHOLD = "brreg_report_threshold";
    private static final String MISC_ASSEMBLE_CONDITION = "brreg_assemble_condition";
    private static final String MISC_BIGDATA_THRESHOLD = "bigdata_threshold";
    private static final String SPLIT_VALUE = ",";
    private static final String TAG = "AwareBroadcastRegister brreg";
    private static AwareBroadcastRegister mBroadcastRegister = null;
    private final HashMap<String, HashMap<String, String>> mBRAssembleConditons;
    private final HashMap<String, Integer> mBRCounts;
    private int mBrRegisterReportThreshold;
    private final Object mConfigLock;
    private HwActivityManagerService mHwAMS;
    private final HashMap<String, HashMap<String, String>> mPkgBRsWithCondition;

    public static synchronized AwareBroadcastRegister getInstance() {
        AwareBroadcastRegister awareBroadcastRegister;
        synchronized (AwareBroadcastRegister.class) {
            if (mBroadcastRegister == null) {
                mBroadcastRegister = new AwareBroadcastRegister();
                mBroadcastRegister.updateConfigData();
            }
            awareBroadcastRegister = mBroadcastRegister;
        }
        return awareBroadcastRegister;
    }

    private AwareBroadcastRegister() {
        this.mBRCounts = new HashMap();
        this.mPkgBRsWithCondition = new HashMap();
        this.mBRAssembleConditons = new HashMap();
        this.mHwAMS = null;
        this.mConfigLock = new Object();
        this.mBrRegisterReportThreshold = 30;
        this.mHwAMS = HwActivityManagerService.self();
    }

    public void updateConfigData() {
        String str;
        StringBuilder stringBuilder;
        if (this.mHwAMS == null) {
            AwareLog.e(TAG, "failed to get HwAMS");
            return;
        }
        DecisionMaker.getInstance().updateRule(AppMngFeature.BROADCAST, this.mHwAMS.getUiContext());
        synchronized (this.mConfigLock) {
            int size;
            int i;
            String item;
            String[] configList;
            ArrayList<String> defaultThresholdList = DecisionMaker.getInstance().getRawConfig(AppMngFeature.BROADCAST.getDesc(), MISC_BIGDATA_THRESHOLD);
            int i2 = 0;
            int i3 = 2;
            int i4 = 1;
            if (defaultThresholdList != null) {
                size = defaultThresholdList.size();
                for (i = 0; i < size; i++) {
                    item = (String) defaultThresholdList.get(i);
                    if (!(item == null || item.isEmpty())) {
                        configList = item.split(",");
                        if (configList.length < 2) {
                            String str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("invalid config: ");
                            stringBuilder2.append(item);
                            AwareLog.e(str2, stringBuilder2.toString());
                        } else {
                            try {
                                if (configList[0].trim().equals(ITEM_REPORT_THRESHOLD)) {
                                    this.mBrRegisterReportThreshold = Integer.parseInt(configList[1].trim());
                                }
                            } catch (NumberFormatException e) {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("invalid config: ");
                                stringBuilder.append(item);
                                AwareLog.e(str, stringBuilder.toString());
                            }
                        }
                    }
                }
            }
            this.mBRAssembleConditons.clear();
            this.mPkgBRsWithCondition.clear();
            ArrayList<String> brAssembleCondition = DecisionMaker.getInstance().getRawConfig(AppMngFeature.BROADCAST.getDesc(), MISC_ASSEMBLE_CONDITION);
            if (brAssembleCondition != null) {
                size = 0;
                i = brAssembleCondition.size();
                while (size < i) {
                    item = (String) brAssembleCondition.get(size);
                    if (!(item == null || item.isEmpty())) {
                        configList = item.split(",");
                        if (configList.length < i3) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("invalid config: ");
                            stringBuilder.append(item);
                            AwareLog.e(str, stringBuilder.toString());
                        } else {
                            str = configList[i2].trim();
                            String condition = configList[i4].trim();
                            String action = getActionFromAssembleCondition(condition);
                            String pkg = getPackageNameFromId(str);
                            String receiver = getReceiverFromAcID(str);
                            if (!(receiver == null || receiver.startsWith("."))) {
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(".");
                                stringBuilder3.append(receiver);
                                receiver = stringBuilder3.toString();
                            }
                            if (!(action == null || pkg == null)) {
                                HashMap<String, String> receiverMap = (HashMap) this.mPkgBRsWithCondition.get(pkg);
                                if (receiverMap == null) {
                                    receiverMap = new HashMap();
                                    this.mPkgBRsWithCondition.put(pkg, receiverMap);
                                }
                                receiverMap.put(receiver, str);
                                HashMap<String, String> innerMap = (HashMap) this.mBRAssembleConditons.get(str);
                                if (innerMap == null) {
                                    innerMap = new HashMap();
                                    this.mBRAssembleConditons.put(str, innerMap);
                                }
                                innerMap.put(action, condition);
                            }
                        }
                    }
                    size++;
                    i2 = 0;
                    i3 = 2;
                    i4 = 1;
                }
            }
        }
    }

    public int getBRRegisterReportThreshold() {
        int i;
        synchronized (this.mConfigLock) {
            i = this.mBrRegisterReportThreshold;
        }
        return i;
    }

    public HashMap<String, Integer> getBRCounts() {
        HashMap<String, Integer> hashMap;
        synchronized (this.mBRCounts) {
            hashMap = this.mBRCounts;
        }
        return hashMap;
    }

    public int countReceiverRegister(boolean isRegister, String brId) {
        int intValue;
        synchronized (this.mBRCounts) {
            Integer count;
            if (isRegister) {
                if (brId != null) {
                    if (!brId.isEmpty()) {
                        count = (Integer) this.mBRCounts.get(brId);
                        if (count == null) {
                            this.mBRCounts.put(brId, Integer.valueOf(1));
                        } else {
                            this.mBRCounts.put(brId, Integer.valueOf(count.intValue() + 1));
                        }
                    }
                }
            } else if (!(brId == null || brId.isEmpty())) {
                count = (Integer) this.mBRCounts.get(brId);
                if (count != null) {
                    Integer newCount = Integer.valueOf(count.intValue() - 1);
                    if (newCount.intValue() == 0) {
                        this.mBRCounts.remove(brId);
                    } else {
                        this.mBRCounts.put(brId, newCount);
                    }
                }
            }
            intValue = this.mBRCounts.get(brId) == null ? 0 : ((Integer) this.mBRCounts.get(brId)).intValue();
        }
        return intValue;
    }

    public static String removeBRIdUncommonData(String brId) {
        int pidPositionEnd;
        StringBuffer sb = new StringBuffer(brId);
        int pidPositionStart = sb.indexOf("+");
        if (pidPositionStart != -1) {
            pidPositionEnd = sb.indexOf("+", pidPositionStart + 1);
            if (pidPositionEnd != -1) {
                sb.delete(pidPositionStart, pidPositionEnd);
            }
        }
        pidPositionEnd = sb.indexOf("@");
        if (pidPositionEnd != -1) {
            int objectAddressPositionEnd = sb.indexOf("+", pidPositionEnd);
            if (objectAddressPositionEnd != -1) {
                sb.delete(pidPositionEnd, objectAddressPositionEnd);
            }
        }
        return sb.toString();
    }

    /* JADX WARNING: Missing block: B:29:0x007a, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String findMatchedAssembleConditionId(String brId) {
        if (brId == null) {
            return null;
        }
        String pkg = getPackageNameFromId(brId);
        if (pkg == null || pkg.isEmpty()) {
            return null;
        }
        synchronized (this.mConfigLock) {
            HashMap<String, String> receiverMap = (HashMap) this.mPkgBRsWithCondition.get(pkg);
            if (receiverMap != null) {
                for (Entry<String, String> entry : receiverMap.entrySet()) {
                    String receiver = (String) entry.getKey();
                    if (receiver != null && !receiver.isEmpty() && brId.contains(receiver)) {
                        String str;
                        if (AwareBroadcastDebug.getDebugDetail()) {
                            str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Hit !  brId:");
                            stringBuilder.append(brId);
                            stringBuilder.append(" match  acId: ");
                            stringBuilder.append((String) entry.getValue());
                            AwareLog.i(str, stringBuilder.toString());
                        }
                        str = (String) entry.getValue();
                        return str;
                    }
                }
            }
        }
    }

    public String getBRAssembleCondition(String acId, String action) {
        if (acId == null || action == null) {
            return null;
        }
        synchronized (this.mConfigLock) {
            HashMap<String, String> conditions = (HashMap) this.mBRAssembleConditons.get(acId);
            if (conditions != null) {
                String str = (String) conditions.get(action);
                return str;
            }
            return null;
        }
    }

    private static String getPackageNameFromId(String brId) {
        if (brId == null) {
            return null;
        }
        int pkgPositionEnd = brId.indexOf("+");
        if (pkgPositionEnd != -1) {
            return brId.substring(0, pkgPositionEnd);
        }
        return null;
    }

    private static String getReceiverFromAcID(String acId) {
        if (acId == null) {
            return null;
        }
        int receiverPositionStart = acId.lastIndexOf("+");
        if (receiverPositionStart == -1) {
            return null;
        }
        try {
            return acId.substring(receiverPositionStart + 1, acId.length());
        } catch (IndexOutOfBoundsException e) {
            AwareLog.e(TAG, "acId process error");
            return null;
        }
    }

    private static String getActionFromAssembleCondition(String condition) {
        if (condition == null) {
            return null;
        }
        String action = null;
        int actionEndPosition = condition.indexOf("@");
        if (actionEndPosition != -1) {
            action = condition.substring(0, actionEndPosition);
        }
        return action;
    }

    public void dumpBRRegConfig(PrintWriter pw) {
        updateConfigData();
        synchronized (this.mConfigLock) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("brreg_report_threshold: ");
            stringBuilder.append(this.mBrRegisterReportThreshold);
            pw.println(stringBuilder.toString());
            pw.println("Assemble conditions:");
            for (Entry<String, HashMap<String, String>> entry : this.mBRAssembleConditons.entrySet()) {
                if (entry.getValue() != null) {
                    for (Entry<String, String> innerEntry : ((HashMap) entry.getValue()).entrySet()) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append((String) entry.getKey());
                        stringBuilder2.append(", ");
                        stringBuilder2.append((String) innerEntry.getValue());
                        pw.println(stringBuilder2.toString());
                    }
                }
            }
        }
    }

    public void dumpIawareBRRegInfo(PrintWriter pw) {
        synchronized (this.mBRCounts) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("BR register ID and count:");
            stringBuilder.append(this.mBRCounts.size());
            pw.println(stringBuilder.toString());
            for (Entry<String, Integer> entry : this.mBRCounts.entrySet()) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("");
                stringBuilder2.append((String) entry.getKey());
                stringBuilder2.append(",");
                stringBuilder2.append(entry.getValue());
                pw.println(stringBuilder2.toString());
            }
        }
    }
}
