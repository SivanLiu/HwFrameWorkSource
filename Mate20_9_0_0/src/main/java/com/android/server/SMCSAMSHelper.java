package com.android.server;

import android.os.Parcel;
import android.os.SystemProperties;
import android.util.Log;
import com.android.server.am.HwActivityManagerService;
import com.huawei.android.smcs.STProcessRecord;
import com.huawei.android.smcs.SmartTrimProcessEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public final class SMCSAMSHelper {
    private static final boolean DBG = SystemProperties.getBoolean("ro.enable.st_debug", false);
    private static final boolean DBG_PERFORMANCE = DBG;
    static final int SMCS_AMS_MOVE_SELF_AUTO_TRIMED_PROCS = 5;
    static final String SMCS_TRIM_TYPE_SELF_AUTO = "trimer_self_auto";
    static final String SMCS_TRIM_TYPE_USER_ONE_SHOOT = "trimer_user_one_shoot";
    private static final int STP_EVENT_MAX_NUM = SystemProperties.getInt("ro.smart_trim.stpe_num", 200);
    private static final String TAG = "SMCSAMSHelper";
    private static SMCSAMSHelper mSelf = null;
    HwActivityManagerService mAms = HwActivityManagerService.self();
    private long mSelfAutoTrimId = 0;
    private HashSet<String> mSelfAutoTrimedProcs = null;

    private SMCSAMSHelper() {
    }

    public static SMCSAMSHelper getInstance() {
        if (mSelf == null) {
            mSelf = new SMCSAMSHelper();
        }
        return mSelf;
    }

    void setAlarmService(AlarmManagerService alarmService) {
    }

    public boolean handleTransact(Parcel data, Parcel reply, int flag) {
        boolean res = true;
        if (data == null) {
            return false;
        }
        int iEvent = data.readInt();
        if (DBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SMCSAMSHelper.handleTransact: ");
            stringBuilder.append(iEvent);
            Log.v(str, stringBuilder.toString());
        }
        if (iEvent != 5) {
            res = false;
        } else {
            handleMoveSelfAutoTrimedProcs(data, reply);
        }
        return res;
    }

    public void trimProcessPostProcess(String trimProc, int uid, String trimType, HashSet<String> pkgList) {
        String str;
        StringBuilder stringBuilder;
        long timeStart = -1;
        if (DBG) {
            timeStart = System.currentTimeMillis();
        }
        if (trimType.equals(SMCS_TRIM_TYPE_SELF_AUTO)) {
            handleSelfAutoTrimPostProcess(trimProc, uid, trimType, pkgList);
        } else if (DBG) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("SMCSAMSHelper.trimProcessPostProcess: unkonw trim type ");
            stringBuilder.append(trimType);
            Log.e(str, stringBuilder.toString());
        }
        if (DBG) {
            long timeCost = System.currentTimeMillis() - timeStart;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("SMCSAMSHelper.trimProcessPostProcess: total cost ");
            stringBuilder.append(timeCost);
            stringBuilder.append(" ms.");
            Log.v(str, stringBuilder.toString());
        }
    }

    private void handleSelfAutoTrimPostProcess(String trimProc, int uid, String trimType, HashSet<String> pkgList) {
        if (trimProc != null && trimProc.length() > 0) {
            if (this.mSelfAutoTrimedProcs == null) {
                this.mSelfAutoTrimedProcs = new HashSet();
            }
            this.mSelfAutoTrimedProcs.add(trimProc);
        }
        informTrimAlarm(pkgList);
    }

    private void informTrimAlarm(HashSet<String> hashSet) {
    }

    private void handleMoveSelfAutoTrimedProcs(Parcel data, Parcel reply) {
        long trimedId = data.readLong();
        if (trimedId != this.mSelfAutoTrimId) {
            if (DBG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("SMCSAMSHelper.handleMoveSelfAutoTrimedProcs: trimed id is different ");
                stringBuilder.append(trimedId);
                stringBuilder.append(" ");
                stringBuilder.append(this.mSelfAutoTrimId);
                Log.v(str, stringBuilder.toString());
            }
            return;
        }
        if (this.mSelfAutoTrimedProcs != null) {
            reply.writeInt(this.mSelfAutoTrimedProcs.size());
            Iterator<String> it = this.mSelfAutoTrimedProcs.iterator();
            while (it.hasNext()) {
                reply.writeString((String) it.next());
            }
            this.mSelfAutoTrimedProcs.clear();
        }
        this.mSelfAutoTrimId = -1;
    }

    private HashSet<String> stringChangeA2H(ArrayList<String> src) {
        if (src == null || src.size() == 0) {
            return null;
        }
        HashSet<String> dst = new HashSet();
        Iterator<String> it = src.iterator();
        while (it.hasNext()) {
            String sPkg = (String) it.next();
            if (sPkg != null && sPkg.length() > 0) {
                dst.add(sPkg);
            }
        }
        return dst;
    }

    private ArrayList<String> stringChangeH2A(HashSet<String> src) {
        if (src == null || src.size() == 0) {
            return null;
        }
        ArrayList<String> dst = new ArrayList();
        Iterator<String> it = src.iterator();
        while (it.hasNext()) {
            dst.add((String) it.next());
        }
        return dst;
    }

    private void dumpStrings(ArrayList<String> strs, String sLog) {
        StringBuffer sb = new StringBuffer();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(sLog);
        stringBuilder.append(":\n");
        sb.append(stringBuilder.toString());
        if (strs != null) {
            Iterator<String> it = strs.iterator();
            while (it.hasNext()) {
                String s = (String) it.next();
                stringBuilder = new StringBuilder();
                stringBuilder.append("    ");
                stringBuilder.append(s);
                stringBuilder.append("\n");
                sb.append(stringBuilder.toString());
            }
            Log.v(TAG, sb.toString());
        }
    }

    private void dumpSmartTrimProcessEvent(ArrayList<SmartTrimProcessEvent> events, String sLog) {
        StringBuffer sb = new StringBuffer();
        StringBuilder stringBuilder;
        if (events == null || events.size() == 0) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(sLog);
            stringBuilder.append(": empty events");
            Log.v(str, stringBuilder.toString());
            return;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(sLog);
        stringBuilder2.append("\n");
        sb.append(stringBuilder2.toString());
        int size = events.size();
        stringBuilder = new StringBuilder();
        stringBuilder.append(sLog);
        stringBuilder.append(": total ");
        stringBuilder.append(size);
        stringBuilder.append(" events.");
        sb.append(stringBuilder.toString());
        for (int i = 0; i < size; i++) {
            sb.append(((SmartTrimProcessEvent) events.get(i)).toString());
            sb.append("\n");
        }
        Log.v(TAG, sb.toString());
    }

    private void dumpSTProcessRecords(ArrayList<STProcessRecord> stProcessRecords, String sLog) {
        if (stProcessRecords == null || stProcessRecords.size() == 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(sLog);
            stringBuilder.append(" empty process records.");
            Log.v(str, stringBuilder.toString());
            return;
        }
        StringBuffer sb = new StringBuffer();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(sLog);
        stringBuilder2.append("\n");
        sb.append(stringBuilder2.toString());
        Iterator<STProcessRecord> it = stProcessRecords.iterator();
        while (it.hasNext()) {
            STProcessRecord stpr = (STProcessRecord) it.next();
            if (stpr != null) {
                sb.append(stpr.toString());
                sb.append("\n");
            }
        }
        Log.v(TAG, sb.toString());
    }
}
