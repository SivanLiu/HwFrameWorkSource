package com.android.server.mtm.iaware.appmng;

import android.app.mtm.iaware.appmng.AppMngConstant.BroadcastSource;
import android.app.mtm.iaware.appmng.AppMngConstant.CleanReason;
import android.content.Intent;
import android.text.TextUtils;
import android.util.ArrayMap;
import com.android.server.mtm.iaware.appmng.AwareAppMngSort.ClassRate;
import com.android.server.mtm.iaware.appmng.policy.BrFilterPolicy;
import com.android.server.mtm.iaware.appmng.rule.BroadcastMngRule;
import com.android.server.mtm.iaware.appmng.rule.RuleNode;
import com.android.server.mtm.iaware.appmng.rule.RuleParserUtil.AppMngTag;
import com.android.server.mtm.taskstatus.ProcessCleaner.CleanType;
import com.android.server.mtm.taskstatus.ProcessInfo;
import com.android.server.mtm.taskstatus.ProcessInfoCollector;
import com.android.server.mtm.utils.AppStatusUtils.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class AwareProcessInfo {
    private boolean isFas = false;
    private boolean isFreeze = false;
    private boolean isRare = false;
    private boolean isStandBy = false;
    private ArrayMap<String, BrFilterPolicy> mBrPolicys = new ArrayMap();
    public int mClassRate;
    public CleanType mCleanType;
    public Map<String, Integer> mDetailedReason;
    public boolean mHasShownUi;
    public int mImportance;
    public int mMemGroup;
    public int mPid;
    public ProcessInfo mProcInfo;
    public String mReason;
    public boolean mRestartFlag = false;
    private int mState = 10;
    public int mSubClassRate;
    public int mTaskId = -1;
    public XmlConfig mXmlConfig;

    public static class XmlConfig {
        public int mCfgDefaultGroup;
        public boolean mFrequentlyUsed;
        public boolean mResCleanAllow;
        public boolean mRestartFlag;

        public XmlConfig(int groupId, boolean frequentUsed, boolean allowCleanRes, boolean restartFlag) {
            this.mCfgDefaultGroup = groupId;
            this.mFrequentlyUsed = frequentUsed;
            this.mResCleanAllow = allowCleanRes;
            this.mRestartFlag = restartFlag;
        }
    }

    public boolean getRestartFlag() {
        if (this.mRestartFlag) {
            return true;
        }
        if (this.mXmlConfig == null) {
            return false;
        }
        return this.mXmlConfig.mRestartFlag;
    }

    public AwareProcessInfo(int pid, int memGroup, int importance, int classRate, ProcessInfo procInfo) {
        this.mPid = pid;
        this.mMemGroup = memGroup;
        this.mImportance = importance;
        this.mClassRate = classRate;
        this.mSubClassRate = 0;
        this.mProcInfo = procInfo;
    }

    public AwareProcessInfo(int pid, ProcessInfo procInfo) {
        this.mPid = pid;
        this.mProcInfo = procInfo;
    }

    public String getProcessStatus() {
        if (this.mDetailedReason == null) {
            return null;
        }
        Status[] values = Status.values();
        Integer statType = (Integer) this.mDetailedReason.get(AppMngTag.STATUS.getDesc());
        if (statType == null || values.length <= statType.intValue() || statType.intValue() < 0) {
            return null;
        }
        return values[statType.intValue()].description();
    }

    public boolean isForegroundApp() {
        if (this.mDetailedReason == null) {
            return false;
        }
        Integer statType = (Integer) this.mDetailedReason.get(AppMngTag.STATUS.getDesc());
        if (statType == null) {
            return false;
        }
        if (Status.TOP_ACTIVITY.ordinal() == statType.intValue() || Status.FOREGROUND_APP.ordinal() == statType.intValue()) {
            return true;
        }
        return false;
    }

    public boolean isVisibleApp(int visibleAppAdj) {
        if (this.mDetailedReason == null || this.mProcInfo == null || this.mProcInfo.mCurAdj != visibleAppAdj) {
            return false;
        }
        Integer statType = (Integer) this.mDetailedReason.get(AppMngTag.STATUS.getDesc());
        if (statType != null && Status.VISIBLE_APP.ordinal() == statType.intValue()) {
            return true;
        }
        return false;
    }

    public boolean isAwareProtected() {
        if (TextUtils.isEmpty(this.mReason)) {
            return false;
        }
        return CleanReason.LIST.getCode().equals(this.mReason);
    }

    public String getStatisticsInfo() {
        StringBuilder build = new StringBuilder();
        build.append("proc info pid:");
        build.append(this.mPid);
        build.append(",classRate:");
        build.append(this.mClassRate);
        build.append(",");
        build.append(getClassRateStr());
        build.append(",importance:");
        build.append(this.mImportance);
        if (this.mProcInfo != null) {
            if (this.mProcInfo.mPackageName != null) {
                build.append(",packageName:");
                int size = this.mProcInfo.mPackageName.size();
                for (int i = 0; i < size; i++) {
                    String pkg = (String) this.mProcInfo.mPackageName.get(i);
                    build.append(" ");
                    build.append(pkg);
                }
            }
            build.append(",procName:");
            build.append(this.mProcInfo.mProcessName);
            build.append(",adj:");
            build.append(this.mProcInfo.mCurAdj);
        }
        return build.toString();
    }

    public String getClassRateStr() {
        return AwareAppMngSort.getClassRateStr(this.mClassRate);
    }

    public static List<AwareProcessInfo> getAwareProcInfosList() {
        List<AwareProcessInfo> awareProcList = new ArrayList();
        ProcessInfoCollector processInfoCollector = ProcessInfoCollector.getInstance();
        if (processInfoCollector == null) {
            return awareProcList;
        }
        List<ProcessInfo> procList = processInfoCollector.getProcessInfoList();
        if (procList.isEmpty()) {
            return awareProcList;
        }
        int size = procList.size();
        for (int i = 0; i < size; i++) {
            ProcessInfo procInfo = (ProcessInfo) procList.get(i);
            awareProcList.add(new AwareProcessInfo(procInfo.mPid, 0, 0, ClassRate.NORMAL.ordinal(), procInfo));
        }
        return awareProcList;
    }

    /* JADX WARNING: Missing block: B:16:0x004a, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static ArrayList<AwareProcessInfo> getAwareProcInfosFromPackage(String packageName, int userId) {
        ArrayList<AwareProcessInfo> awareProcList = new ArrayList();
        if (packageName == null || userId < 0 || packageName.equals("")) {
            return awareProcList;
        }
        ProcessInfoCollector processInfoCollector = ProcessInfoCollector.getInstance();
        if (processInfoCollector == null) {
            return awareProcList;
        }
        ArrayList<ProcessInfo> procList = processInfoCollector.getProcessInfosFromPackage(packageName, userId);
        if (procList.isEmpty()) {
            return awareProcList;
        }
        int size = procList.size();
        for (int i = 0; i < size; i++) {
            ProcessInfo procInfo = (ProcessInfo) procList.get(i);
            awareProcList.add(new AwareProcessInfo(procInfo.mPid, 0, 0, ClassRate.NORMAL.ordinal(), procInfo));
        }
        return awareProcList;
    }

    /* JADX WARNING: Missing block: B:19:0x006f, code skipped:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static ArrayList<AwareProcessInfo> getAwareProcInfosFromPackageList(List<String> packlist, List<Integer> uidList) {
        List<String> list = packlist;
        List<Integer> list2 = uidList;
        ArrayList<AwareProcessInfo> awareProcList = new ArrayList();
        if (list == null || list2 == null || packlist.size() != uidList.size()) {
            return awareProcList;
        }
        ProcessInfoCollector processInfoCollector = ProcessInfoCollector.getInstance();
        if (processInfoCollector == null) {
            return awareProcList;
        }
        ArrayMap<String, Integer> packMap = new ArrayMap();
        int listSize = packlist.size();
        for (int i = 0; i < listSize; i++) {
            packMap.put((String) list.get(i), (Integer) list2.get(i));
        }
        ArrayList<ProcessInfo> procList = processInfoCollector.getProcessInfosFromPackageMap(packMap);
        if (procList.isEmpty()) {
            return awareProcList;
        }
        int size = procList.size();
        for (int i2 = 0; i2 < size; i2++) {
            ProcessInfo procInfo = (ProcessInfo) procList.get(i2);
            awareProcList.add(new AwareProcessInfo(procInfo.mPid, 0, 0, ClassRate.NORMAL.ordinal(), procInfo));
        }
        return awareProcList;
    }

    public static ArrayList<AwareProcessInfo> getAwareProcInfosFromTask(int taskId, int userId) {
        ArrayList<AwareProcessInfo> awareProcList = new ArrayList();
        if (taskId < 0 || userId < 0) {
            return awareProcList;
        }
        ProcessInfoCollector processInfoCollector = ProcessInfoCollector.getInstance();
        if (processInfoCollector == null) {
            return awareProcList;
        }
        ArrayList<ProcessInfo> procList = processInfoCollector.getProcessInfosFromTask(taskId, userId);
        if (procList.isEmpty()) {
            return awareProcList;
        }
        int size = procList.size();
        for (int i = 0; i < size; i++) {
            ProcessInfo procInfo = (ProcessInfo) procList.get(i);
            AwareProcessInfo awareProcInfo = new AwareProcessInfo(procInfo.mPid, 0, 0, ClassRate.NORMAL.ordinal(), procInfo);
            awareProcInfo.mTaskId = taskId;
            awareProcList.add(awareProcInfo);
        }
        return awareProcList;
    }

    public static ArrayList<AwareProcessInfo> getAwareProcInfosFromTaskList(List<Integer> taskIdList, List<Integer> userIdList) {
        if (taskIdList == null || userIdList == null || taskIdList.size() != userIdList.size()) {
            return null;
        }
        ArrayList<AwareProcessInfo> awareProcList = new ArrayList();
        int listSize = taskIdList.size();
        for (int i = 0; i < listSize; i++) {
            awareProcList.addAll(getAwareProcInfosFromTask(((Integer) taskIdList.get(i)).intValue(), ((Integer) userIdList.get(i)).intValue()));
        }
        return awareProcList;
    }

    public int getState() {
        if (this.mState == 1) {
            return this.mState;
        }
        if (this.mProcInfo == null || (!this.mProcInfo.mForegroundActivities && !this.mProcInfo.mForegroundServices && !this.mProcInfo.mForceToForeground)) {
            return this.mState;
        }
        return 9;
    }

    public void setState(int state) {
        switch (state) {
            case 1:
                this.isFreeze = true;
                break;
            case 2:
                this.isFreeze = false;
                break;
            case 3:
                this.isStandBy = true;
                break;
            case 4:
                this.isStandBy = false;
                break;
            case 5:
                this.isRare = true;
                break;
            case 6:
                this.isRare = false;
                break;
            case 7:
                this.isFas = true;
                break;
            case 8:
                this.isFas = false;
                break;
        }
        resetState();
    }

    private void resetState() {
        if (this.isFreeze) {
            this.mState = 1;
        } else if (this.isFas) {
            this.mState = 7;
        } else if (this.isRare) {
            this.mState = 5;
        } else if (this.isStandBy) {
            this.mState = 3;
        } else {
            this.mState = 10;
        }
    }

    public void updateProcessInfo(ProcessInfo procInfo) {
        this.mProcInfo = procInfo;
    }

    public void updateBrPolicy() {
        BroadcastMngRule brRule = DecisionMaker.getInstance().getBroadcastMngRule(BroadcastSource.BROADCAST_FILTER);
        if (brRule != null) {
            ArrayMap<String, RuleNode> rules = brRule.getBrRules();
            synchronized (this.mBrPolicys) {
                for (Entry<String, RuleNode> ent : rules.entrySet()) {
                    String action = (String) ent.getKey();
                    RuleNode node = (RuleNode) ent.getValue();
                    BrFilterPolicy brPolicy = (BrFilterPolicy) this.mBrPolicys.get(action);
                    String id = (String) this.mProcInfo.mPackageName.get(0);
                    Intent intent = new Intent(action);
                    if (brPolicy == null) {
                        brPolicy = new BrFilterPolicy(id, 0, getState());
                        this.mBrPolicys.put(action, brPolicy);
                    }
                    BrFilterPolicy brPolicy2 = brPolicy;
                    brPolicy2.setPolicy(brRule.updateApply(BroadcastSource.BROADCAST_FILTER, id, intent, this, node));
                    brPolicy2.setStae(getState());
                }
            }
        }
    }

    public BrFilterPolicy getBrPolicy(Intent intent) {
        synchronized (this.mBrPolicys) {
            if (intent == null) {
                try {
                    return null;
                } catch (Throwable th) {
                }
            } else {
                BrFilterPolicy brFilterPolicy = (BrFilterPolicy) this.mBrPolicys.get(intent.getAction());
                return brFilterPolicy;
            }
        }
    }

    public ArrayMap<String, BrFilterPolicy> getBrFilterPolicyMap() {
        ArrayMap arrayMap;
        synchronized (this.mBrPolicys) {
            arrayMap = new ArrayMap(this.mBrPolicys);
        }
        return arrayMap;
    }
}
