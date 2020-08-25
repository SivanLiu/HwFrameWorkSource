package com.huawei.opcollect.strategy;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.os.SystemClock;
import com.huawei.nb.kv.KCompositeString;
import com.huawei.nb.model.collectencrypt.CollectSwitch;
import com.huawei.nb.model.collectencrypt.DicEventPolicy;
import com.huawei.nb.model.collectencrypt.DicFieldCollectPolicy;
import com.huawei.nb.model.collectencrypt.DicTableCollectPolicy;
import com.huawei.nb.model.meta.DataLifeCycle;
import com.huawei.nb.notification.ChangeNotification;
import com.huawei.nb.notification.ModelObserver;
import com.huawei.nb.notification.ObserverType;
import com.huawei.nb.query.Query;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.opcollect.collector.servicecollection.ARStatusAction;
import com.huawei.opcollect.collector.servicecollection.LocationRecordAction;
import com.huawei.opcollect.location.ILocationListener;
import com.huawei.opcollect.location.SystemLocation;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.odmf.OdmfHelper;
import com.huawei.opcollect.utils.OPCollectConstant;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectUtils;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OdmfActionManager {
    private static final String ACTION_HALFHOUR_TIMETICK = "com.huawei.opcollect.action.halfhour_timetick";
    private static final int APP_FG_TABLE_SAVE_DAY_IN_DATABASE = 1;
    private static final String BIGDATA_PERMISSION = "com.huawei.permission.BIG_DATA";
    private static final long BOOTCOMPLETED_DELAY = 120000;
    private static final String DATABASE_NAME = "dsCollectEncrypt";
    private static final int EVENT_TYPE = 2;
    private static final Object LOCK = new Object();
    private static final String ODMF_API_VERSION_2_7_0 = "2.7.0";
    private static final String SP_DATALIFECYCLE = "DataLifeCycle";
    private static final int STATE_INITED = 1;
    private static final int STATE_POLICY_INITED = 2;
    private static final int STATE_UNINITED = 0;
    private static final String TABLE_PACKAGE_NAME = "com.huawei.nb.model.collectencrypt.";
    private static final int TABLE_SAVE_DAY_IN_DATABASE = 7;
    private static final int TABLE_SAVE_MODE_IN_DATABASE = 5;
    private static final int TABLE_TYPE = 1;
    private static final String TAG = "OdmfActionManager";
    private static final long TIMETICK_DELAY = 60000;
    private static final String TIME_STAMP_FIELD_NAME = "mTimeStamp";
    private static OdmfActionManager instance = null;
    private static boolean mBooting = true;
    private static NextTimer mNxtTimer = new NextTimer();
    private static ModelObserver mPolicyModelObserver = new PolicyModelObserver();
    private static StateMachine mState = new StateMachine();
    private static BroadcastReceiver mTimeReceiver = new TimeBroadcastReceiver();
    private Calendar mCalToday = null;
    private Context mContext = null;
    private boolean mEventObserverRegistered = false;
    private boolean mFieldObserverRegistered = false;
    private Map<String, Action> mPullActionList = null;
    private Map<String, Action> mPushActionList = null;
    private boolean mSwitchObserverRegistered = false;
    private boolean mTableObserverRegistered = false;

    private OdmfActionManager() {
    }

    public static OdmfActionManager getInstance() {
        OdmfActionManager odmfActionManager;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new OdmfActionManager();
            }
            odmfActionManager = instance;
        }
        return odmfActionManager;
    }

    static final class NextTimer {
        long beginAtMillis;
        long triggerAtMillis;

        NextTimer() {
            reset();
        }

        /* access modifiers changed from: package-private */
        public void setBegin(long begin) {
            this.beginAtMillis = begin;
        }

        /* access modifiers changed from: package-private */
        public void reset() {
            this.beginAtMillis = SystemClock.elapsedRealtime();
            this.triggerAtMillis = 86400000;
        }

        public void update(long nextTime) {
            if (nextTime > 0 && nextTime < this.triggerAtMillis) {
                this.triggerAtMillis = nextTime;
            }
        }

        public String toString() {
            return "The next trigger timer: " + ((this.beginAtMillis + this.triggerAtMillis) - SystemClock.elapsedRealtime()) + "ms later";
        }
    }

    private static final class StateMachine {
        /* access modifiers changed from: private */
        public int state = 0;

        StateMachine() {
        }

        /* access modifiers changed from: package-private */
        public boolean isInited() {
            return this.state >= 1;
        }

        /* access modifiers changed from: package-private */
        public boolean isPolicyInited() {
            return this.state >= 2;
        }

        public String toString() {
            switch (this.state) {
                case 0:
                    return "STATE_UNINITED";
                case 1:
                    return "STATE_INITED";
                case 2:
                    return "STATE_POLICY_INITED";
                default:
                    return "UNKNOWN STATE";
            }
        }
    }

    private void onNewDay() {
        Calendar calNow = Calendar.getInstance();
        synchronized (LOCK) {
            if (this.mPullActionList != null) {
                for (Action action : this.mPullActionList.values()) {
                    action.onNewDay(calNow);
                }
            }
            if (this.mPushActionList != null) {
                for (Action action2 : this.mPushActionList.values()) {
                    action2.onNewDay(calNow);
                }
            }
        }
    }

    private void onTimeTick(NextTimer nxtTimer) {
        Calendar calNow = Calendar.getInstance();
        long secondOfDay = OPCollectUtils.getTimeInMsFromMidnight(calNow) / 1000;
        long rtNow = SystemClock.elapsedRealtime() / 1000;
        synchronized (LOCK) {
            if (this.mPullActionList != null) {
                for (Action action : this.mPullActionList.values()) {
                    if (action.checkTimerTriggers(calNow, secondOfDay, rtNow, nxtTimer)) {
                        OPCollectLog.i(TAG, "checkTimerTriggers.perform:" + action.getName());
                        action.perform();
                    }
                }
            }
        }
        nxtTimer.setBegin(SystemClock.elapsedRealtime());
        postTimeTick(nxtTimer.triggerAtMillis);
        OPCollectLog.i(TAG, nxtTimer.toString());
    }

    public void initialize(Context context) {
        if (!mState.isInited()) {
            OPCollectLog.r(TAG, "initialize");
            synchronized (LOCK) {
                this.mContext = context;
                this.mCalToday = Calendar.getInstance();
                int unused = mState.state = 1;
            }
            getPolicy();
            if (mState.isPolicyInited()) {
                registerObserver();
            }
            if (OPCollectUtils.checkODMFApiVersion(this.mContext, ODMF_API_VERSION_2_7_0)) {
                dataLifeCycleControlNewVersion();
            } else {
                dataLifeCycleControl();
            }
            initAlarm();
            grantKvAuthority();
        }
    }

    private void initAlarm() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.TIME_SET");
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        filter.addAction(ACTION_HALFHOUR_TIMETICK);
        this.mContext.registerReceiver(mTimeReceiver, filter, BIGDATA_PERMISSION, OdmfCollectScheduler.getInstance().getRecvHandler());
        scheduleAlarms(this.mContext, 1800000);
    }

    private void dataLifeCycleControlNewVersion() {
        int ret;
        OPCollectLog.r(TAG, "dataLifeCycleControl initialize");
        OdmfHelper odmfHelper = OdmfCollectScheduler.getInstance().getOdmfHelper();
        if (odmfHelper == null) {
            OPCollectLog.e(TAG, "dataLifeCycleControl odmfHelper is null.");
            return;
        }
        ActionTableName[] values = ActionTableName.values();
        for (ActionTableName tableName : values) {
            if (ActionTableName.RAW_FG_APP_EVENT.getValue().equals(tableName.getValue())) {
                ret = odmfHelper.addDataLifeCycleConfig(DATABASE_NAME, TABLE_PACKAGE_NAME + tableName.getValue(), TIME_STAMP_FIELD_NAME, 5, 1);
            } else {
                ret = odmfHelper.addDataLifeCycleConfig(DATABASE_NAME, TABLE_PACKAGE_NAME + tableName.getValue(), TIME_STAMP_FIELD_NAME, 5, 7);
            }
            OPCollectLog.i(TAG, "Write DataLifeCycle of " + tableName + " result: " + ret);
        }
    }

    private void dataLifeCycleControl() {
        OPCollectLog.r(TAG, "dataLifeCycleControl initialize");
        OdmfHelper odmfHelper = OdmfCollectScheduler.getInstance().getOdmfHelper();
        if (odmfHelper == null) {
            OPCollectLog.e(TAG, "dataLifeCycleControl odmfHelper is null.");
            return;
        }
        List<AManagedObject> dataLifeCycleList = odmfHelper.queryManageObject(Query.select(DataLifeCycle.class).equalTo("mDBName", DATABASE_NAME));
        List<String> tableNameList = new ArrayList<>();
        if (dataLifeCycleList != null && dataLifeCycleList.size() > 0) {
            for (AManagedObject object : dataLifeCycleList) {
                tableNameList.add(object.getMTableName());
            }
        }
        ActionTableName[] values = ActionTableName.values();
        for (ActionTableName tableName : values) {
            if (tableNameList.contains(tableName.getValue())) {
                OPCollectLog.i(TAG, "dataLifeCycleControl, the DataLifeCycle is exist");
            } else {
                DataLifeCycle dataLifeCycle = new DataLifeCycle();
                dataLifeCycle.setMDBName(DATABASE_NAME);
                dataLifeCycle.setMTableName(TABLE_PACKAGE_NAME + tableName.getValue());
                dataLifeCycle.setMFieldName(TIME_STAMP_FIELD_NAME);
                if ("RawFgAPPEvent".equals(tableName.getValue())) {
                    dataLifeCycle.setMCount(1);
                } else {
                    dataLifeCycle.setMCount(7);
                }
                dataLifeCycle.setMMode(5);
                dataLifeCycle.setMDBRekeyTime(0L);
                dataLifeCycle.setMThreshold(0);
                dataLifeCycle.setMUnit(0);
                if (odmfHelper.insertManageObjectWithoutCache(dataLifeCycle) == null) {
                    OPCollectLog.i(TAG, "the DataLifeCycle of " + tableName + " set failed");
                }
            }
        }
    }

    public void uninitialize() {
        clearEventPolicy();
        clearTableCollectPolicy();
        unregisterObserver();
        SystemLocation.getInstance(this.mContext).destroy();
        this.mContext.unregisterReceiver(mTimeReceiver);
        OdmfCollectScheduler.getInstance().getCtrlHandler().removeMessages(OdmfCollectScheduler.MSG_CTRL_TIME_TICK);
        int unused = mState.state = 0;
    }

    private void odmfConnect() {
        if (mState.isInited()) {
            if (!mState.isPolicyInited()) {
                getPolicy();
            }
            if (mState.isPolicyInited()) {
                registerObserver();
            }
            grantKvAuthority();
        }
    }

    private void grantKvAuthority() {
        OPCollectLog.i(TAG, "grantKvAuthority ret " + OdmfCollectScheduler.getInstance().getOdmfHelper().grant(new KCompositeString(OPCollectConstant.PACKAGE_NAME), "com.huawei.recsys", 1));
    }

    private void odmfDisconnect() {
        if (mState.isPolicyInited()) {
            int unused = mState.state = 1;
        }
        this.mTableObserverRegistered = false;
        this.mEventObserverRegistered = false;
        this.mFieldObserverRegistered = false;
        this.mSwitchObserverRegistered = false;
    }

    private boolean getPolicy() {
        int unused = mState.state = 2;
        if (!initPolicySwitch()) {
            return false;
        }
        postTimeTick(TIMETICK_DELAY);
        return true;
    }

    private void updateTableCollectList(List<AManagedObject> list, boolean isSwitchChange) {
        if (list != null && list.size() > 0) {
            Iterator<AManagedObject> it = list.iterator();
            while (it.hasNext()) {
                DicTableCollectPolicy tcp = (DicTableCollectPolicy) it.next();
                Action action = ActionFactory.getSysInfoAction(this.mContext, tcp.getMTblName());
                if (action != null) {
                    String actionName = action.getName();
                    synchronized (LOCK) {
                        if (!isSwitchChange) {
                            if (this.mPullActionList == null || !this.mPullActionList.containsKey(actionName)) {
                                OPCollectLog.r(TAG, "ignore " + actionName + " policy change.");
                                action.destroy();
                            }
                        }
                        action.enable();
                        action.setCollectPolicy(tcp.getMTriggerPolicy());
                        Integer intVal = tcp.getMMaxRecordOneday();
                        if (intVal != null) {
                            action.setMaxRecordOneday(intVal.intValue());
                        }
                        Integer intVal2 = tcp.getMColdDownTime();
                        if (intVal2 != null) {
                            action.setIntervalMin(intVal2.intValue());
                        }
                        synchronized (LOCK) {
                            if (this.mPullActionList == null) {
                                this.mPullActionList = new HashMap();
                            }
                            this.mPullActionList.put(actionName, action);
                        }
                    }
                }
            }
        }
    }

    private void updateTableCollectPolicy(List<AManagedObject> list, boolean isSwitchChange) {
        OPCollectLog.r(TAG, "updateTableCollectPolicy");
        updateTableCollectList(list, isSwitchChange);
    }

    private void initTableCollectPolicy(List<AManagedObject> list, boolean isSwitchChange) {
        OPCollectLog.r(TAG, "initTableCollectPolicy");
        updateTableCollectList(list, isSwitchChange);
    }

    private void updateEventList(List<AManagedObject> list, boolean isSwitchChange) {
        if (list != null && list.size() > 0) {
            Iterator<AManagedObject> it = list.iterator();
            while (it.hasNext()) {
                DicEventPolicy ep = (DicEventPolicy) it.next();
                Action action = ActionFactory.getSysEventAction(this.mContext, ep.getMEventName());
                if (action != null) {
                    String actionName = action.getName();
                    synchronized (LOCK) {
                        if (!isSwitchChange) {
                            if (this.mPushActionList == null || !this.mPushActionList.containsKey(actionName)) {
                                OPCollectLog.r(TAG, "ignore " + actionName + " policy change.");
                                action.destroy();
                            }
                        }
                        action.enable();
                        Integer intVal = ep.getMMaxRecordOneday();
                        if (intVal != null) {
                            action.setMaxRecordOneday(intVal.intValue());
                        }
                        Integer intVal2 = ep.getMColdDownTime();
                        if (intVal2 != null) {
                            action.setIntervalMin(intVal2.intValue());
                        }
                        synchronized (LOCK) {
                            if (this.mPushActionList == null) {
                                this.mPushActionList = new HashMap();
                            }
                            this.mPushActionList.put(action.getName(), action);
                        }
                    }
                }
            }
        }
    }

    private void updateEventPolicy(List<AManagedObject> list, boolean isSwitchChange) {
        OPCollectLog.r(TAG, "updateEventPolicy");
        updateEventList(list, isSwitchChange);
    }

    private void initEventPolicy(List<AManagedObject> list, boolean isSwitchChange) {
        OPCollectLog.r(TAG, "initEventPolicy");
        updateEventList(list, isSwitchChange);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:28:0x0085, code lost:
        if (isPolicyNeedLocation() != false) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0087, code lost:
        com.huawei.opcollect.location.SystemLocation.getInstance(r7.mContext).disable();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:?, code lost:
        return;
     */
    private void clearTableCollectPolicy() {
        synchronized (LOCK) {
            if (this.mPullActionList != null && this.mPullActionList.size() != 0) {
                Iterator<Map.Entry<String, Action>> it = this.mPullActionList.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Action> item = it.next();
                    if (!(item == null || item.getValue() == null)) {
                        item.getValue().destroy();
                        try {
                            it.remove();
                        } catch (IllegalStateException e) {
                            OPCollectLog.e(TAG, "remove IllegalState: " + e.getMessage());
                        } catch (UnsupportedOperationException e2) {
                            OPCollectLog.e(TAG, "remove Unsupported: " + e2.getMessage());
                        }
                    }
                }
                this.mPullActionList.clear();
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:28:0x0085, code lost:
        if (isPolicyNeedLocation() != false) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0087, code lost:
        com.huawei.opcollect.location.SystemLocation.getInstance(r7.mContext).disable();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:?, code lost:
        return;
     */
    private void clearEventPolicy() {
        synchronized (LOCK) {
            if (this.mPushActionList != null && this.mPushActionList.size() != 0) {
                Iterator<Map.Entry<String, Action>> it = this.mPushActionList.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Action> item = it.next();
                    if (!(item == null || item.getValue() == null)) {
                        item.getValue().destroy();
                        try {
                            it.remove();
                        } catch (IllegalStateException e) {
                            OPCollectLog.e(TAG, "remove IllegalState: " + e.getMessage());
                        } catch (UnsupportedOperationException e2) {
                            OPCollectLog.e(TAG, "remove Unsupported: " + e2.getMessage());
                        }
                    }
                }
                this.mPushActionList.clear();
            }
        }
    }

    private boolean isPolicyNeedLocation() {
        synchronized (LOCK) {
            if (this.mPullActionList != null && this.mPullActionList.keySet().contains(OPCollectConstant.LOCATION_ACTION_NAME)) {
                return true;
            }
            if (this.mPushActionList == null || !this.mPushActionList.keySet().contains(OPCollectConstant.CAMERA_TAKE_ACTION_NAME)) {
                return false;
            }
            return true;
        }
    }

    public void removeLocationListener(String key, ILocationListener listener) {
        synchronized (LOCK) {
            if (this.mPullActionList != null && this.mPullActionList.keySet().contains(OPCollectConstant.LOCATION_ACTION_NAME)) {
                LocationRecordAction.getInstance(this.mContext).removeLocationListener(key, listener);
            }
        }
    }

    public boolean checkIfActionEnabled(String actionName) {
        boolean z;
        synchronized (LOCK) {
            z = (this.mPullActionList != null && this.mPullActionList.keySet().contains(actionName)) || (this.mPushActionList != null && this.mPushActionList.keySet().contains(actionName));
        }
        return z;
    }

    public void checkIfEnableLocation() {
        synchronized (LOCK) {
            if (this.mPullActionList != null && this.mPullActionList.keySet().contains(OPCollectConstant.LOCATION_ACTION_NAME)) {
                SystemLocation.getInstance(this.mContext).enable();
            }
        }
    }

    public void checkIfDisableLocation() {
        synchronized (LOCK) {
            if (this.mPullActionList != null && this.mPullActionList.keySet().contains(OPCollectConstant.LOCATION_ACTION_NAME)) {
                SystemLocation.getInstance(this.mContext).disable();
            }
        }
    }

    public void checkIfEnableARService() {
        synchronized (LOCK) {
            if (this.mPullActionList != null && this.mPullActionList.keySet().contains(OPCollectConstant.AR_ACTION_NAME)) {
                ARStatusAction.getInstance(this.mContext).enableAREvent(1);
            }
        }
    }

    public void checkIfDisableARService() {
        synchronized (LOCK) {
            if (this.mPullActionList != null && this.mPullActionList.keySet().contains(OPCollectConstant.AR_ACTION_NAME)) {
                ARStatusAction.getInstance(this.mContext).enableAREvent(2);
            }
        }
    }

    private boolean registerObserver() {
        if (!this.mSwitchObserverRegistered) {
            this.mSwitchObserverRegistered = OdmfCollectScheduler.getInstance().getOdmfHelper().subscribeManagedObject(CollectSwitch.class, ObserverType.OBSERVER_MODEL, mPolicyModelObserver);
            OPCollectLog.r(TAG, "mSwitchObserverRegistered: " + this.mSwitchObserverRegistered);
        }
        if (!this.mTableObserverRegistered) {
            this.mTableObserverRegistered = OdmfCollectScheduler.getInstance().getOdmfHelper().subscribeManagedObject(DicTableCollectPolicy.class, ObserverType.OBSERVER_MODEL, mPolicyModelObserver);
            OPCollectLog.r(TAG, "mTableObserverRegistered: " + this.mTableObserverRegistered);
        }
        if (!this.mFieldObserverRegistered) {
            this.mFieldObserverRegistered = OdmfCollectScheduler.getInstance().getOdmfHelper().subscribeManagedObject(DicFieldCollectPolicy.class, ObserverType.OBSERVER_MODEL, mPolicyModelObserver);
            OPCollectLog.r(TAG, "mFieldObserverRegistered: " + this.mFieldObserverRegistered);
        }
        if (!this.mEventObserverRegistered) {
            this.mEventObserverRegistered = OdmfCollectScheduler.getInstance().getOdmfHelper().subscribeManagedObject(DicEventPolicy.class, ObserverType.OBSERVER_MODEL, mPolicyModelObserver);
            OPCollectLog.r(TAG, "mEventObserverRegistered: " + this.mEventObserverRegistered);
        }
        return this.mSwitchObserverRegistered && this.mTableObserverRegistered && this.mFieldObserverRegistered && this.mEventObserverRegistered;
    }

    private boolean unregisterObserver() {
        if (this.mTableObserverRegistered) {
            OdmfCollectScheduler.getInstance().getOdmfHelper().unSubscribeManagedObject(DicTableCollectPolicy.class, ObserverType.OBSERVER_MODEL, mPolicyModelObserver);
            this.mTableObserverRegistered = false;
        }
        if (this.mFieldObserverRegistered) {
            OdmfCollectScheduler.getInstance().getOdmfHelper().unSubscribeManagedObject(DicFieldCollectPolicy.class, ObserverType.OBSERVER_MODEL, mPolicyModelObserver);
            this.mFieldObserverRegistered = false;
        }
        if (this.mEventObserverRegistered) {
            OdmfCollectScheduler.getInstance().getOdmfHelper().unSubscribeManagedObject(DicEventPolicy.class, ObserverType.OBSERVER_MODEL, mPolicyModelObserver);
            this.mEventObserverRegistered = false;
        }
        if (this.mSwitchObserverRegistered) {
            OdmfCollectScheduler.getInstance().getOdmfHelper().unSubscribeManagedObject(CollectSwitch.class, ObserverType.OBSERVER_MODEL, mPolicyModelObserver);
            this.mSwitchObserverRegistered = false;
        }
        OPCollectLog.r(TAG, "unregisterObserver");
        return true;
    }

    static final class PolicyModelObserver implements ModelObserver {
        PolicyModelObserver() {
        }

        public void onModelChanged(ChangeNotification changeNotification) {
            if (changeNotification != null && changeNotification.getType() != null) {
                String policyTblName = changeNotification.getType().getName();
                OPCollectLog.r(OdmfActionManager.TAG, "onModelChanged: " + policyTblName);
                if (CollectSwitch.class.getName().equalsIgnoreCase(policyTblName)) {
                    OdmfCollectScheduler.getInstance().getCtrlHandler().removeMessages(OdmfCollectScheduler.MSG_ODMF_SWITCH_CHANGED);
                    OdmfCollectScheduler.getInstance().getCtrlHandler().sendEmptyMessage(OdmfCollectScheduler.MSG_ODMF_SWITCH_CHANGED);
                    return;
                }
                OdmfCollectScheduler.getInstance().getCtrlHandler().obtainMessage(OdmfCollectScheduler.MSG_ODMF_POLICY_CHANGED, changeNotification).sendToTarget();
            }
        }
    }

    private void onPolicyChanged(ChangeNotification changeNotification) {
        if (changeNotification != null && changeNotification.getType() != null) {
            String policyTblName = changeNotification.getType().getName();
            OPCollectLog.r(TAG, "onPolicyChanged: " + policyTblName);
            List<AManagedObject> updateList = changeNotification.getUpdatedItems();
            if (!(updateList == null || updateList.size() == 0)) {
                Iterator<AManagedObject> it = updateList.iterator();
                while (it.hasNext()) {
                    OPCollectLog.r(TAG, "onPolicyChanged updateList size: " + updateList.size() + " object:" + it.next());
                }
            }
            if (DicTableCollectPolicy.class.getName().equalsIgnoreCase(policyTblName)) {
                updateTableCollectPolicy(updateList, false);
            } else if (DicEventPolicy.class.getName().equalsIgnoreCase(policyTblName)) {
                updateEventPolicy(updateList, false);
            }
            postTimeTick(0);
        }
    }

    private void onSwitchChanged() {
        initPolicySwitch();
        postTimeTick(0);
    }

    private boolean initPolicySwitch() {
        OPCollectLog.r(TAG, "+++entry initPolicySwitch");
        OdmfHelper odmfHelper = OdmfCollectScheduler.getInstance().getOdmfHelper();
        if (odmfHelper == null) {
            OPCollectLog.e(TAG, "initPolicySwitch odmf connect failed.");
            return false;
        }
        List<AManagedObject> collectSwitchList = odmfHelper.queryManageObject(Query.select(CollectSwitch.class));
        if (collectSwitchList == null || collectSwitchList.size() == 0) {
            OPCollectLog.i(TAG, "CollectSwitch empty");
            clearEventPolicy();
            clearTableCollectPolicy();
            return true;
        }
        HashSet<String> deDuplicateSet = new HashSet<>();
        for (AManagedObject sub : collectSwitchList) {
            String dataName = sub.getMDataName();
            if (dataName != null && dataName.length() > 0) {
                deDuplicateSet.add(dataName);
            }
        }
        if (deDuplicateSet.size() == 0) {
            OPCollectLog.i(TAG, "CollectSwitch deDuplicateSet empty.");
            clearEventPolicy();
            clearTableCollectPolicy();
            return true;
        }
        Map<String, String> actionSwitchMap = new HashMap<>();
        Iterator<String> it = deDuplicateSet.iterator();
        while (it.hasNext()) {
            String key = it.next();
            String actionName = OPCollectConstant.getActionNameFromSwitchName(key);
            if (actionName != null) {
                actionSwitchMap.put(actionName, key);
            }
        }
        List<String> action2Exist = new ArrayList<>();
        calculateRemovePullAction(actionSwitchMap, new ArrayList<>(), action2Exist);
        calculateRemovePushAction(actionSwitchMap, new ArrayList<>(), action2Exist);
        for (String str : action2Exist) {
            actionSwitchMap.remove(str);
        }
        initPolicySwitchInner(actionSwitchMap, odmfHelper);
        return true;
    }

    private void initPolicySwitchInner(Map<String, String> actionSwitchMap, OdmfHelper odmfHelper) {
        List<String> switch2Add = new ArrayList<>(actionSwitchMap.values());
        if (switch2Add.size() > 0) {
            List<AManagedObject> paramTableCollectList = new ArrayList<>();
            List<AManagedObject> subTableParamList = odmfHelper.queryManageObject(Query.select(DicTableCollectPolicy.class));
            for (String amoName : switch2Add) {
                AManagedObject targetObject = findParamObject(subTableParamList, amoName, 1);
                if (targetObject != null) {
                    paramTableCollectList.add(targetObject);
                }
            }
            if (paramTableCollectList.size() > 0) {
                initTableCollectPolicy(paramTableCollectList, true);
            }
            List<AManagedObject> paramEventCollectList = new ArrayList<>();
            List<AManagedObject> subEventParamList = odmfHelper.queryManageObject(Query.select(DicEventPolicy.class));
            for (String amoName2 : switch2Add) {
                AManagedObject targetObject2 = findParamObject(subEventParamList, amoName2, 2);
                if (targetObject2 != null) {
                    paramEventCollectList.add(targetObject2);
                }
            }
            if (paramEventCollectList.size() > 0) {
                initEventPolicy(paramEventCollectList, true);
            }
        }
    }

    private void calculateRemovePullAction(Map<String, String> actionSwitchMap, List<String> pullAction2Remove, List<String> action2Exist) {
        synchronized (LOCK) {
            if (this.mPullActionList != null && this.mPullActionList.size() > 0) {
                for (String str : this.mPullActionList.keySet()) {
                    if (!actionSwitchMap.containsKey(str)) {
                        pullAction2Remove.add(str);
                    } else {
                        action2Exist.add(str);
                    }
                }
                for (String str2 : pullAction2Remove) {
                    Action action = this.mPullActionList.remove(str2);
                    if (action != null) {
                        action.destroy();
                    }
                }
            }
        }
    }

    private void calculateRemovePushAction(Map<String, String> actionSwitchMap, List<String> pushAction2Remove, List<String> action2Exist) {
        synchronized (LOCK) {
            if (this.mPushActionList != null && this.mPushActionList.size() > 0) {
                for (String str : this.mPushActionList.keySet()) {
                    if (!actionSwitchMap.containsKey(str)) {
                        pushAction2Remove.add(str);
                    } else {
                        action2Exist.add(str);
                    }
                }
                for (String str2 : pushAction2Remove) {
                    Action action = this.mPushActionList.remove(str2);
                    if (action != null) {
                        action.destroy();
                    }
                }
            }
        }
    }

    private AManagedObject findParamObject(List<AManagedObject> srcList, String targetName, int type) {
        if (srcList == null) {
            return null;
        }
        if (srcList.size() == 0 || targetName == null) {
            return null;
        }
        if (type == 1) {
            for (AManagedObject amo : srcList) {
                if (((DicTableCollectPolicy) amo).getMTblName().equalsIgnoreCase(targetName)) {
                    return amo;
                }
            }
        } else if (type == 2) {
            for (AManagedObject amo2 : srcList) {
                if (((DicEventPolicy) amo2).getMEventName().equalsIgnoreCase(targetName)) {
                    return amo2;
                }
            }
        }
        return null;
    }

    private boolean isNewDay(NextTimer nxtTimer) {
        synchronized (LOCK) {
            Calendar calNow = Calendar.getInstance();
            if (this.mCalToday == null) {
                this.mCalToday = calNow;
                nxtTimer.update(OPCollectUtils.getTimeSpanToNextDay(calNow));
                return false;
            } else if (calNow.get(1) == this.mCalToday.get(1) && calNow.get(2) == this.mCalToday.get(2) && calNow.get(5) == this.mCalToday.get(5)) {
                nxtTimer.update(OPCollectUtils.getTimeSpanToNextDay(calNow));
                return false;
            } else {
                OPCollectLog.r(TAG, "isNewDay");
                this.mCalToday = calNow;
                nxtTimer.update(OPCollectUtils.getTimeSpanToNextDay(calNow));
                return true;
            }
        }
    }

    public static void handleMessage(Message msg) {
        OPCollectLog.r(TAG, "handleMessage msg: " + msg.what);
        if (!mState.isInited()) {
            OPCollectLog.w(TAG, "is not yet initialized, the msg will be dropped.");
            return;
        }
        switch (msg.what) {
            case OdmfCollectScheduler.MSG_CTRL_TIME_TICK:
                OdmfCollectScheduler.getInstance().getCtrlHandler().removeMessages(OdmfCollectScheduler.MSG_CTRL_TIME_TICK);
                mNxtTimer.reset();
                if (getInstance().isNewDay(mNxtTimer)) {
                    getInstance().onNewDay();
                }
                getInstance().onTimeTick(mNxtTimer);
                return;
            case OdmfCollectScheduler.MSG_ODMF_POLICY_CHANGED:
                getInstance().onPolicyChanged((ChangeNotification) msg.obj);
                return;
            case OdmfCollectScheduler.MSG_ODMF_CONNECTED:
                getInstance().odmfConnect();
                return;
            case OdmfCollectScheduler.MSG_ODMF_DISCONNECTED:
                getInstance().odmfDisconnect();
                return;
            case OdmfCollectScheduler.MSG_ODMF_SWITCH_CHANGED:
                getInstance().onSwitchChanged();
                return;
            default:
                OPCollectLog.e(TAG, "handleMessage error msg.");
                return;
        }
    }

    /* access modifiers changed from: private */
    public static void postTimeTick(long delayMillis) {
        long delay = delayMillis;
        if (mBooting && delay < TIMETICK_DELAY) {
            if (SystemClock.elapsedRealtime() < BOOTCOMPLETED_DELAY) {
                delay = TIMETICK_DELAY;
            } else {
                mBooting = false;
            }
        }
        OdmfCollectScheduler.getInstance().getCtrlHandler().sendEmptyMessageDelayed(OdmfCollectScheduler.MSG_CTRL_TIME_TICK, delay);
    }

    /* access modifiers changed from: private */
    public static void scheduleAlarms(Context ctxt, long triggerDelayed) {
        AlarmManager mgr = (AlarmManager) ctxt.getSystemService("alarm");
        Intent i = new Intent(ACTION_HALFHOUR_TIMETICK);
        i.setPackage(ctxt.getPackageName());
        PendingIntent pi = PendingIntent.getBroadcast(ctxt, 0, i, 0);
        mgr.cancel(pi);
        mgr.setExactAndAllowWhileIdle(3, SystemClock.elapsedRealtime() + triggerDelayed, pi);
        OPCollectLog.i(TAG, "scheduleAlarms:" + triggerDelayed);
    }

    static final class TimeBroadcastReceiver extends BroadcastReceiver {
        TimeBroadcastReceiver() {
        }

        public void onReceive(Context ctxt, Intent i) {
            String action = i.getAction();
            if (OdmfActionManager.ACTION_HALFHOUR_TIMETICK.equals(action)) {
                OdmfActionManager.postTimeTick(0);
                OdmfActionManager.scheduleAlarms(ctxt, 7200000);
                OPCollectLog.i(OdmfActionManager.TAG, "repeating timer " + action);
            } else if ("android.intent.action.TIME_SET".equals(action)) {
                OdmfActionManager.postTimeTick(0);
                OPCollectLog.i(OdmfActionManager.TAG, "ACTION_TIME_CHANGED");
            } else if ("android.intent.action.TIMEZONE_CHANGED".equals(action)) {
                OdmfActionManager.postTimeTick(0);
                OPCollectLog.i(OdmfActionManager.TAG, "ACTION_TIMEZONE_CHANGED");
            }
        }
    }

    public static void dump(PrintWriter pw) {
        synchronized (LOCK) {
            pw.println("OdmfActionManager MachineState: " + mState.toString());
            pw.println("OdmfActionManager mSwitchObserverRegistered: " + getInstance().mSwitchObserverRegistered);
            pw.println(mNxtTimer.toString());
            Action.dump("", pw);
            if (instance == null) {
                pw.println("OdmfActionManager instance is null");
                return;
            }
            if (instance.mPullActionList != null) {
                pw.println("----------pull action list(" + instance.mPullActionList.size() + ")----------");
                for (Action action : instance.mPullActionList.values()) {
                    if (action != null) {
                        action.dump(4, pw);
                    }
                }
            } else {
                pw.println("----------pull action list is null----------");
            }
            if (instance.mPushActionList != null) {
                pw.println("----------push action list(" + instance.mPushActionList.size() + ")----------");
                for (Action action2 : instance.mPushActionList.values()) {
                    if (action2 != null) {
                        action2.dump(4, pw);
                    }
                }
            } else {
                pw.println("----------push action list is null----------");
            }
        }
    }
}
