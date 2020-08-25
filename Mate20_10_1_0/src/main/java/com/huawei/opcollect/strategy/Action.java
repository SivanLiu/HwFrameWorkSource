package com.huawei.opcollect.strategy;

import android.content.Context;
import android.os.SystemClock;
import com.huawei.nb.query.Query;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.strategy.OdmfActionManager;
import com.huawei.opcollect.utils.OPCollectLog;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Action {
    private static final Object LOCK = new Object();
    private static final String TAG = "Action";
    private static volatile long mObjectNum = 0;
    private static Map<String, Integer> objectMap = new HashMap();
    private List<String> actionTriggerList;
    private List<EventTimerTrigger> eventTimerTriggerList;
    private List<String> eventTriggerList;
    /* access modifiers changed from: protected */
    public Context mContext = null;
    private int mDailyRecordNum = 0;
    private boolean mEnable = false;
    private long mIntervalMin = -1;
    private long mLastExecuteTime = 0;
    private int mMaxRecordOneDay = -1;
    private String mName;
    private boolean mTimeDisable = false;
    private List<TimeDisable> timeDisableList;
    private List<ITimerTrigger> timerTriggerList;

    public Action(Context context, String name) {
        if (context != null) {
            this.mContext = context.getApplicationContext();
        }
        this.mName = name;
        objectNumPlus();
        OPCollectLog.i(TAG, "Action index " + mObjectNum);
        if (objectMap.containsKey(this.mName)) {
            objectMap.put(this.mName, Integer.valueOf(objectMap.get(this.mName).intValue() + 1));
            return;
        }
        objectMap.put(this.mName, 1);
    }

    /* access modifiers changed from: protected */
    public void finalize() throws Throwable {
        objectMinus();
        OPCollectLog.i(TAG, "Action remains " + mObjectNum);
        if (objectMap.containsKey(this.mName)) {
            int val = objectMap.get(this.mName).intValue() - 1;
            if (val <= 0) {
                objectMap.remove(this.mName);
            } else {
                objectMap.put(this.mName, Integer.valueOf(val));
            }
        } else {
            OPCollectLog.e(TAG, this.mName + " will destroy an object that is not found in the objectMap");
        }
        super.finalize();
    }

    public boolean destroy() {
        if (this.mEnable) {
            disable();
        }
        OPCollectLog.i(this.mName, "destroy");
        return true;
    }

    private static void objectNumPlus() {
        synchronized (LOCK) {
            mObjectNum++;
        }
    }

    private static void objectMinus() {
        synchronized (LOCK) {
            mObjectNum--;
        }
    }

    private void parseFromJson(String jsonData) throws JSONException {
        if (jsonData != null && !jsonData.trim().equals("")) {
            JSONObject rootJson = new JSONObject(jsonData);
            JSONArray policyJson = rootJson.optJSONArray("LoopTimerTrigger");
            if (policyJson != null) {
                addTimerTrigger(LoopTimerTrigger.fromJson(policyJson));
            }
            JSONArray policyJson2 = rootJson.optJSONArray("OneShotTimerTrigger");
            if (policyJson2 != null) {
                addTimerTrigger(OneShotTimerTrigger.fromJson(policyJson2));
            }
            JSONArray policyJson3 = rootJson.optJSONArray("SpanTimerTrigger");
            if (policyJson3 != null) {
                addTimerTrigger(SpanTimerTrigger.fromJson(policyJson3));
            }
            JSONArray policyJson4 = rootJson.optJSONArray("EventTimerTrigger");
            if (policyJson4 != null) {
                if (this.eventTimerTriggerList == null) {
                    this.eventTimerTriggerList = new ArrayList();
                }
                this.eventTimerTriggerList.addAll(EventTimerTrigger.fromJson(policyJson4));
            }
            JSONArray policyJson5 = rootJson.optJSONArray("EventTrigger");
            if (policyJson5 != null) {
                if (this.eventTriggerList == null) {
                    this.eventTriggerList = new ArrayList();
                }
                this.eventTriggerList.addAll(parseStringArray(policyJson5));
            }
            JSONArray policyJson6 = rootJson.optJSONArray("ActionTrigger");
            if (policyJson6 != null) {
                if (this.actionTriggerList == null) {
                    this.actionTriggerList = new ArrayList();
                }
                this.actionTriggerList.addAll(parseStringArray(policyJson6));
            }
            JSONArray policyJson7 = rootJson.optJSONArray("TimeDisable");
            if (policyJson7 != null) {
                if (this.timeDisableList == null) {
                    this.timeDisableList = new ArrayList();
                }
                this.timeDisableList.addAll(TimeDisable.fromJson(policyJson7));
            }
        }
    }

    private List<String> parseStringArray(JSONArray jsonObj) throws JSONException {
        List<String> list = new ArrayList<>();
        int length = jsonObj.length();
        for (int i = 0; i < length; i++) {
            list.add(jsonObj.getString(i));
        }
        return list;
    }

    public void setCollectPolicy(String jsonPolicy) {
        clearPolicy();
        try {
            parseFromJson(jsonPolicy);
        } catch (JSONException e) {
            OPCollectLog.e(TAG, "Error while parsing json " + e);
        }
    }

    private void clearPolicy() {
        this.timeDisableList = null;
        this.timerTriggerList = null;
        this.eventTimerTriggerList = null;
        this.eventTriggerList = null;
        this.actionTriggerList = null;
    }

    private void addTimerTrigger(List<ITimerTrigger> list) {
        if (list != null) {
            if (this.timerTriggerList == null) {
                this.timerTriggerList = new ArrayList();
            }
            this.timerTriggerList.addAll(list);
        }
    }

    public String getName() {
        return this.mName;
    }

    public void enable() {
        this.mEnable = true;
    }

    public void disable() {
        OPCollectLog.i(this.mName, "disable");
        this.mEnable = false;
    }

    public boolean isEnable() {
        return !this.mTimeDisable && this.mEnable;
    }

    public void setMaxRecordOneday(int maxNum) {
        this.mMaxRecordOneDay = maxNum;
    }

    public void setIntervalMin(int minInterval) {
        this.mIntervalMin = (long) minInterval;
    }

    public void setDailyRecordNum(int dailyRecordNum) {
        this.mDailyRecordNum = dailyRecordNum;
    }

    public int getMaxRecordOneday() {
        return this.mMaxRecordOneDay;
    }

    public int getIntervalMin() {
        return (int) this.mIntervalMin;
    }

    public int getDailyRecordNum() {
        return this.mDailyRecordNum;
    }

    /* access modifiers changed from: protected */
    public <T extends AManagedObject> int queryDailyRecordNum(Class<T> clazz) {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        Date midnight = calendar.getTime();
        OPCollectLog.d(TAG, "midnight: " + midnight.toString());
        return (int) OdmfCollectScheduler.getInstance().getOdmfHelper().queryManageObjectCount(Query.select(clazz).greaterThanOrEqualTo("mTimeStamp", midnight).count("*"));
    }

    public void onNewDay(Calendar calNow) {
        this.mDailyRecordNum = 0;
    }

    /* access modifiers changed from: protected */
    public boolean execute() {
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean executeWithArgs(AbsActionParam absActionParam) {
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean performWithArgs(AbsActionParam absActionParam) {
        if (!isEnable()) {
            OPCollectLog.w(TAG, getName() + " is disable");
            return false;
        } else if (this.mMaxRecordOneDay <= 0 || this.mDailyRecordNum < this.mMaxRecordOneDay || absActionParam == null || !absActionParam.isCheckMaxRecordOneDay()) {
            long nowRealTime = SystemClock.elapsedRealtime() / 1000;
            if (this.mIntervalMin > 0 && nowRealTime - this.mLastExecuteTime < this.mIntervalMin && absActionParam != null && absActionParam.isCheckMinInterval()) {
                OPCollectLog.i(TAG, getName() + " executed frequently");
                return false;
            } else if (!executeWithArgs(absActionParam)) {
                OPCollectLog.e(TAG, getName() + " execution failed");
                return false;
            } else {
                OPCollectLog.i(TAG, getName() + " executed successfully");
                if (absActionParam != null && absActionParam.isCheckMaxRecordOneDay()) {
                    this.mDailyRecordNum++;
                }
                if (absActionParam != null && absActionParam.isCheckMinInterval()) {
                    this.mLastExecuteTime = nowRealTime;
                }
                return true;
            }
        } else {
            OPCollectLog.w(TAG, getName() + " is overlimit, current:" + this.mDailyRecordNum + ", max:" + this.mMaxRecordOneDay);
            return false;
        }
    }

    public boolean perform() {
        if (!isEnable()) {
            OPCollectLog.w(TAG, getName() + " is disable");
            return false;
        } else if (this.mMaxRecordOneDay <= 0 || this.mDailyRecordNum < this.mMaxRecordOneDay) {
            long nowRealTime = SystemClock.elapsedRealtime() / 1000;
            if (this.mIntervalMin > 0 && nowRealTime - this.mLastExecuteTime < this.mIntervalMin) {
                OPCollectLog.i(TAG, getName() + " executed frequently");
                return false;
            } else if (!execute()) {
                OPCollectLog.e(TAG, getName() + " execution failed");
                return false;
            } else {
                OPCollectLog.i(TAG, getName() + " executed successfully");
                this.mDailyRecordNum++;
                this.mLastExecuteTime = nowRealTime;
                return true;
            }
        } else {
            OPCollectLog.w(TAG, getName() + " is overlimit, current:" + this.mDailyRecordNum + ", max:" + this.mMaxRecordOneDay);
            return false;
        }
    }

    private void updateTimeDisable(Calendar calNow, long secondOfDay, OdmfActionManager.NextTimer nxtTimer) {
        boolean disable = false;
        if (this.timeDisableList != null) {
            int listSize = this.timeDisableList.size();
            for (int i = 0; i < listSize; i++) {
                if (this.timeDisableList.get(i).checkDisable(calNow, secondOfDay, nxtTimer)) {
                    disable = true;
                }
            }
            this.mTimeDisable = disable;
        }
    }

    public boolean checkTimerTriggers(Calendar calNow, long secondOfDay, long rtNow, OdmfActionManager.NextTimer nxtTimer) {
        boolean active = false;
        updateTimeDisable(calNow, secondOfDay, nxtTimer);
        if (!isEnable()) {
            return false;
        }
        if (this.timerTriggerList != null) {
            int listSize = this.timerTriggerList.size();
            for (int i = 0; i < listSize; i++) {
                if (this.timerTriggerList.get(i).checkTrigger(calNow, secondOfDay, rtNow, nxtTimer)) {
                    active = true;
                }
            }
        }
        if (this.eventTimerTriggerList != null) {
            int listSize2 = this.eventTimerTriggerList.size();
            for (int i2 = 0; i2 < listSize2; i2++) {
                if (this.eventTimerTriggerList.get(i2).checkTrigger(calNow, secondOfDay, rtNow, nxtTimer)) {
                    active = true;
                }
            }
        }
        return active;
    }

    public boolean checkEventTriggers(String eventName) {
        if (this.eventTimerTriggerList != null) {
            int listSize = this.eventTimerTriggerList.size();
            for (int i = 0; i < listSize; i++) {
                this.eventTimerTriggerList.get(i).startTimer(eventName);
            }
        }
        if (this.eventTriggerList == null || !isEnable()) {
            return false;
        }
        return this.eventTriggerList.contains(eventName);
    }

    public boolean checkActionTriggers(String actionName) {
        if (this.actionTriggerList == null || !isEnable()) {
            return false;
        }
        return this.actionTriggerList.contains(actionName);
    }

    public static void dump(String prefix, PrintWriter pw) {
        String prefix2 = "" + prefix + "\\-";
        StringBuilder sb = new StringBuilder(prefix).append("<--Action Object(").append(objectMap.size()).append(")-->");
        for (Map.Entry<String, Integer> entry : objectMap.entrySet()) {
            sb.append(System.lineSeparator()).append(prefix2).append(entry.getKey()).append(": ").append(entry.getValue().intValue());
        }
        pw.println(sb.toString());
    }

    public void dump(int indentNum, PrintWriter pw) {
        if (pw != null) {
            String prefix = String.format(Locale.ROOT, "%" + indentNum + "s\\-", " ");
            StringBuilder sb = new StringBuilder("<--").append(this.mName).append("-->");
            sb.append(System.lineSeparator()).append(prefix).append("mEnable: ").append(this.mEnable);
            sb.append(System.lineSeparator()).append(prefix).append("mTimeDisable: ").append(this.mTimeDisable);
            sb.append(System.lineSeparator()).append(prefix).append("mIntervalMin: ").append(this.mIntervalMin);
            sb.append(System.lineSeparator()).append(prefix).append("mMaxRecordOneday: ").append(this.mMaxRecordOneDay);
            sb.append(System.lineSeparator()).append(prefix).append("mLastExecuteTime: ").append(this.mLastExecuteTime);
            sb.append(System.lineSeparator()).append(prefix).append("mDailyRecordNum: ").append(this.mDailyRecordNum);
            connectString(prefix, "" + prefix, sb);
            pw.println(sb.toString());
        }
    }

    private void connectString(String prefix, String prefix2, StringBuilder sb) {
        if (this.timeDisableList == null) {
            sb.append(System.lineSeparator()).append(prefix).append("timeDisableList is null");
        } else {
            sb.append(System.lineSeparator()).append(prefix).append("timeDisableList(").append(this.timeDisableList.size()).append("):");
            for (TimeDisable td : this.timeDisableList) {
                sb.append(System.lineSeparator()).append(td.toString(prefix2));
            }
        }
        if (this.timerTriggerList == null) {
            sb.append(System.lineSeparator()).append(prefix).append("timerTriggerList is null");
        } else {
            sb.append(System.lineSeparator()).append(prefix).append("timerTriggerList(").append(this.timerTriggerList.size()).append("):");
            for (ITimerTrigger tt : this.timerTriggerList) {
                sb.append(System.lineSeparator()).append(tt.toString(prefix2));
            }
        }
        if (this.eventTimerTriggerList == null) {
            sb.append(System.lineSeparator()).append(prefix).append("eventTimerTriggerList is null");
        } else {
            sb.append(System.lineSeparator()).append(prefix).append("eventTimerTriggerList(").append(this.eventTimerTriggerList.size()).append("):");
            for (EventTimerTrigger ett : this.eventTimerTriggerList) {
                sb.append(System.lineSeparator()).append(ett.toString(prefix2));
            }
        }
        if (this.eventTriggerList == null) {
            sb.append(System.lineSeparator()).append(prefix).append("eventTriggerList is null");
        } else {
            sb.append(System.lineSeparator()).append(prefix).append("eventTriggerList(").append(this.eventTriggerList.size()).append("):");
            for (String et : this.eventTriggerList) {
                sb.append(System.lineSeparator()).append(prefix2).append(et);
            }
        }
        if (this.actionTriggerList == null) {
            sb.append(System.lineSeparator()).append(prefix).append("actionTriggerList is null");
            return;
        }
        sb.append(System.lineSeparator()).append(prefix).append("actionTriggerList(").append(this.actionTriggerList.size()).append("):");
        for (String at : this.actionTriggerList) {
            sb.append(System.lineSeparator()).append(prefix2).append(at);
        }
    }
}
