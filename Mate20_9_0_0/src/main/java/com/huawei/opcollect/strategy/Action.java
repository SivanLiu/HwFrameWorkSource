package com.huawei.opcollect.strategy;

import android.content.Context;
import android.os.SystemClock;
import com.huawei.nb.query.Query;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectUtils;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Action {
    private static final String TAG = "Action";
    private static volatile long mObjectNum = 0;
    private static Map<String, Integer> objectMap = new HashMap();
    private List<String> actiontriger_list;
    private List<EventTimerTrigger> eventtimertriger_list;
    private List<String> eventtriger_list;
    protected Context mContext = null;
    private int mDailyRecordNum = 0;
    private boolean mEnable = false;
    private long mIntervalMin = -1;
    private long mLastExecuteTime = 0;
    private int mMaxRecordOneday = -1;
    private String mName;
    private boolean mTimeDisable = false;
    private List<TimeDisable> timedisable_list;
    private List<ITimerTrigger> timertriger_list;

    public Action(Context context, String name) {
        if (context != null) {
            this.mContext = context.getApplicationContext();
        }
        this.mName = name;
        objectNumPlus();
        OPCollectLog.i(TAG, "Action index " + mObjectNum);
        if (objectMap.containsKey(this.mName)) {
            objectMap.put(this.mName, Integer.valueOf(((Integer) objectMap.get(this.mName)).intValue() + 1));
            return;
        }
        objectMap.put(this.mName, Integer.valueOf(1));
    }

    protected void finalize() throws Throwable {
        objectMinus();
        OPCollectLog.i(TAG, "Action remains " + mObjectNum);
        if (objectMap.containsKey(this.mName)) {
            int val = ((Integer) objectMap.get(this.mName)).intValue() - 1;
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

    private static synchronized void objectNumPlus() {
        synchronized (Action.class) {
            mObjectNum++;
        }
    }

    private static synchronized void objectMinus() {
        synchronized (Action.class) {
            mObjectNum--;
        }
    }

    private void parseFromjson(String jsonData) throws JSONException {
        if (jsonData != null && !jsonData.trim().equals("")) {
            List<String> list;
            JSONObject root_json = new JSONObject(jsonData);
            JSONArray policy_json = root_json.optJSONArray("LoopTimerTrigger");
            if (policy_json != null) {
                addTimerTriger(LoopTimerTrigger.fromJson(policy_json));
            }
            policy_json = root_json.optJSONArray("OneShotTimerTrigger");
            if (policy_json != null) {
                addTimerTriger(OneShotTimerTrigger.fromJson(policy_json));
            }
            policy_json = root_json.optJSONArray("SpanTimerTrigger");
            if (policy_json != null) {
                addTimerTriger(SpanTimerTrigger.fromJson(policy_json));
            }
            policy_json = root_json.optJSONArray("EventTimerTrigger");
            if (policy_json != null) {
                List<EventTimerTrigger> list2 = EventTimerTrigger.fromJson(policy_json);
                if (list2 != null) {
                    if (this.eventtimertriger_list == null) {
                        this.eventtimertriger_list = new ArrayList();
                    }
                    this.eventtimertriger_list.addAll(list2);
                }
            }
            policy_json = root_json.optJSONArray("EventTrigger");
            if (policy_json != null) {
                list = parseStringArray(policy_json);
                if (this.eventtriger_list == null) {
                    this.eventtriger_list = new ArrayList();
                }
                this.eventtriger_list.addAll(list);
            }
            policy_json = root_json.optJSONArray("ActionTrigger");
            if (policy_json != null) {
                list = parseStringArray(policy_json);
                if (this.actiontriger_list == null) {
                    this.actiontriger_list = new ArrayList();
                }
                this.actiontriger_list.addAll(list);
            }
            policy_json = root_json.optJSONArray("TimeDisable");
            if (policy_json != null) {
                List<TimeDisable> list3 = TimeDisable.fromJson(policy_json);
                if (list3 != null) {
                    if (this.timedisable_list == null) {
                        this.timedisable_list = new ArrayList();
                    }
                    this.timedisable_list.addAll(list3);
                }
            }
        }
    }

    private List<String> parseStringArray(JSONArray json_obj) throws JSONException {
        List<String> list = new ArrayList();
        int length = json_obj.length();
        for (int i = 0; i < length; i++) {
            list.add(json_obj.getString(i));
        }
        return list;
    }

    public void setCollectPolicy(String jsonPolicy) {
        clearPolicy();
        try {
            parseFromjson(jsonPolicy);
        } catch (JSONException e) {
            OPCollectLog.e(TAG, "Error while parsing json\n" + e);
        }
    }

    private void clearPolicy() {
        this.timedisable_list = null;
        this.timertriger_list = null;
        this.eventtimertriger_list = null;
        this.eventtriger_list = null;
        this.actiontriger_list = null;
    }

    private void addTimerTriger(List<ITimerTrigger> list) {
        if (list != null) {
            if (this.timertriger_list == null) {
                this.timertriger_list = new ArrayList();
            }
            this.timertriger_list.addAll(list);
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
        this.mMaxRecordOneday = maxNum;
    }

    public void setIntervalMin(int minInterval) {
        this.mIntervalMin = (long) minInterval;
    }

    public void setDailyRecordNum(int dailyRecordNum) {
        this.mDailyRecordNum = dailyRecordNum;
    }

    public int getMaxRecordOneday() {
        return this.mMaxRecordOneday;
    }

    public int getIntervalMin() {
        return (int) this.mIntervalMin;
    }

    public int getDailyRecordNum() {
        return this.mDailyRecordNum;
    }

    protected <T extends AManagedObject> int queryDailyRecordNum(Class<T> clazz) {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        Date midnight = calendar.getTime();
        OPCollectLog.d(TAG, "midnight: " + midnight.toString());
        int num = (int) OdmfCollectScheduler.getInstance().getOdmfHelper().queryManageObjectCount(Query.select(clazz).greaterThanOrEqualTo("mTimeStamp", midnight).count("*"));
        OPCollectLog.r(TAG, getName() + " num: " + num);
        return num;
    }

    public void onNewDay(Calendar calNow) {
        this.mDailyRecordNum = 0;
    }

    public void onNewMonth(Calendar calNow) {
    }

    public void onNewYear(Calendar calNow) {
    }

    protected boolean execute() {
        return false;
    }

    protected boolean executeWithArgs(AbsActionParam absActionParam) {
        return false;
    }

    protected boolean performWithArgs(AbsActionParam absActionParam) {
        if (!isEnable()) {
            OPCollectLog.w(TAG, getName() + " is disable");
            return false;
        } else if (this.mMaxRecordOneday <= 0 || this.mDailyRecordNum < this.mMaxRecordOneday || absActionParam == null || !absActionParam.isCheckMaxRecordOneDay()) {
            long nowRealTime = SystemClock.elapsedRealtime() / 1000;
            if (this.mIntervalMin > 0 && nowRealTime - this.mLastExecuteTime < this.mIntervalMin && absActionParam != null && absActionParam.isCheckMinInterval()) {
                OPCollectLog.i(TAG, getName() + " executed frequently");
                return false;
            } else if (executeWithArgs(absActionParam)) {
                OPCollectLog.i(TAG, getName() + " executed successfully");
                if (absActionParam != null && absActionParam.isCheckMaxRecordOneDay()) {
                    this.mDailyRecordNum++;
                }
                if (absActionParam != null && absActionParam.isCheckMinInterval()) {
                    this.mLastExecuteTime = nowRealTime;
                }
                return true;
            } else {
                OPCollectLog.e(TAG, getName() + " execution failed");
                return false;
            }
        } else {
            OPCollectLog.w(TAG, getName() + " is overlimit, current:" + this.mDailyRecordNum + ", max:" + this.mMaxRecordOneday);
            return false;
        }
    }

    public boolean perform() {
        if (!isEnable()) {
            OPCollectLog.w(TAG, getName() + " is disable");
            return false;
        } else if (this.mMaxRecordOneday <= 0 || this.mDailyRecordNum < this.mMaxRecordOneday) {
            long nowRealTime = SystemClock.elapsedRealtime() / 1000;
            if (this.mIntervalMin > 0 && nowRealTime - this.mLastExecuteTime < this.mIntervalMin) {
                OPCollectLog.i(TAG, getName() + " executed frequently");
                return false;
            } else if (execute()) {
                OPCollectLog.i(TAG, getName() + " executed successfully");
                this.mDailyRecordNum++;
                this.mLastExecuteTime = nowRealTime;
                return true;
            } else {
                OPCollectLog.e(TAG, getName() + " execution failed");
                return false;
            }
        } else {
            OPCollectLog.w(TAG, getName() + " is overlimit, current:" + this.mDailyRecordNum + ", max:" + this.mMaxRecordOneday);
            return false;
        }
    }

    private void updateTimeDisable(Calendar calNow, long secondOfDay, NextTimer nxttimer) {
        boolean disable = false;
        if (this.timedisable_list != null) {
            int list_size = this.timedisable_list.size();
            for (int i = 0; i < list_size; i++) {
                if (((TimeDisable) this.timedisable_list.get(i)).checkDisable(calNow, secondOfDay, nxttimer)) {
                    disable = true;
                }
            }
            this.mTimeDisable = disable;
        }
    }

    public boolean checkTimerTriggers(Calendar calNow, long secondOfDay, long rtNow, NextTimer nxttimer) {
        boolean active = false;
        updateTimeDisable(calNow, secondOfDay, nxttimer);
        if (!isEnable()) {
            return false;
        }
        int list_size;
        int i;
        if (this.timertriger_list != null) {
            list_size = this.timertriger_list.size();
            for (i = 0; i < list_size; i++) {
                if (((ITimerTrigger) this.timertriger_list.get(i)).checkTrigger(calNow, secondOfDay, rtNow, nxttimer)) {
                    active = true;
                }
            }
        }
        if (this.eventtimertriger_list != null) {
            list_size = this.eventtimertriger_list.size();
            for (i = 0; i < list_size; i++) {
                if (((EventTimerTrigger) this.eventtimertriger_list.get(i)).checkTrigger(calNow, secondOfDay, rtNow, nxttimer)) {
                    active = true;
                }
            }
        }
        return active;
    }

    public boolean checkEventTriggers(String eventname) {
        if (this.eventtimertriger_list != null) {
            int list_size = this.eventtimertriger_list.size();
            for (int i = 0; i < list_size; i++) {
                ((EventTimerTrigger) this.eventtimertriger_list.get(i)).startTimer(eventname);
            }
        }
        if (this.eventtriger_list == null || !isEnable()) {
            return false;
        }
        return this.eventtriger_list.contains(eventname);
    }

    public boolean checkActionTriggers(String actionname) {
        if (this.actiontriger_list == null || !isEnable()) {
            return false;
        }
        return this.actiontriger_list.contains(actionname);
    }

    public static void dump(String prefix, PrintWriter pw) {
        String prefix2 = OPCollectUtils.DUMP_PRINT_PREFIX + prefix + "\\-";
        StringBuilder sb = new StringBuilder(prefix).append("<--Action Object(").append(objectMap.size()).append(")-->");
        for (Entry<String, Integer> entry : objectMap.entrySet()) {
            sb.append("\n").append(prefix2).append((String) entry.getKey()).append(": ").append(((Integer) entry.getValue()).intValue());
        }
        pw.println(sb.toString());
    }

    public void dump(int indentNum, PrintWriter pw) {
        if (pw != null) {
            String prefix = String.format("%" + indentNum + "s\\-", new Object[]{" "});
            String prefix2 = OPCollectUtils.DUMP_PRINT_PREFIX + prefix;
            StringBuilder sb = new StringBuilder("<--").append(this.mName).append("-->");
            sb.append("\n").append(prefix).append("mEnable: ").append(this.mEnable);
            sb.append("\n").append(prefix).append("mTimeDisable: ").append(this.mTimeDisable);
            sb.append("\n").append(prefix).append("mIntervalMin: ").append(this.mIntervalMin);
            sb.append("\n").append(prefix).append("mMaxRecordOneday: ").append(this.mMaxRecordOneday);
            sb.append("\n").append(prefix).append("mLastExecuteTime: ").append(this.mLastExecuteTime);
            sb.append("\n").append(prefix).append("mDailyRecordNum: ").append(this.mDailyRecordNum);
            if (this.timedisable_list == null) {
                sb.append("\n").append(prefix).append("timedisable_list is null");
            } else {
                sb.append("\n").append(prefix).append("timedisable_list(").append(this.timedisable_list.size()).append("):");
                for (TimeDisable td : this.timedisable_list) {
                    sb.append("\n").append(td.toString(prefix2));
                }
            }
            if (this.timertriger_list == null) {
                sb.append("\n").append(prefix).append("timertriger_list is null");
            } else {
                sb.append("\n").append(prefix).append("timertriger_list(").append(this.timertriger_list.size()).append("):");
                for (ITimerTrigger tt : this.timertriger_list) {
                    sb.append("\n").append(tt.toString(prefix2));
                }
            }
            if (this.eventtimertriger_list == null) {
                sb.append("\n").append(prefix).append("eventtimertriger_list is null");
            } else {
                sb.append("\n").append(prefix).append("eventtimertriger_list(").append(this.eventtimertriger_list.size()).append("):");
                for (EventTimerTrigger ett : this.eventtimertriger_list) {
                    sb.append("\n").append(ett.toString(prefix2));
                }
            }
            if (this.eventtriger_list == null) {
                sb.append("\n").append(prefix).append("eventtriger_list is null");
            } else {
                sb.append("\n").append(prefix).append("eventtriger_list(").append(this.eventtriger_list.size()).append("):");
                for (String et : this.eventtriger_list) {
                    sb.append("\n").append(prefix2).append(et);
                }
            }
            if (this.actiontriger_list == null) {
                sb.append("\n").append(prefix).append("actiontriger_list is null");
            } else {
                sb.append("\n").append(prefix).append("actiontriger_list(").append(this.actiontriger_list.size()).append("):");
                for (String at : this.actiontriger_list) {
                    sb.append("\n").append(prefix2).append(at);
                }
            }
            pw.println(sb.toString());
        }
    }
}
