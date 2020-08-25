package com.android.server.hidata.wavemapping.dataprovider;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.cons.ParamManager;
import com.android.server.hidata.wavemapping.dao.LocationDao;
import com.android.server.hidata.wavemapping.entity.ParameterInfo;
import com.android.server.hidata.wavemapping.util.LogUtil;
import com.huawei.android.location.activityrecognition.HwActivityChangedEvent;
import com.huawei.android.location.activityrecognition.HwActivityChangedExtendEvent;
import com.huawei.android.location.activityrecognition.HwActivityRecognition;
import com.huawei.android.location.activityrecognition.HwActivityRecognitionExtendEvent;
import com.huawei.android.location.activityrecognition.HwActivityRecognitionHardwareSink;
import com.huawei.android.location.activityrecognition.HwActivityRecognitionServiceConnection;
import com.huawei.android.location.activityrecognition.HwEnvironmentChangedEvent;
import com.huawei.android.location.activityrecognition.OtherParameters;

public class FrequentLocation {
    private static final int DEFAULT_RIGHT_SHIFT_NUMBER = 32;
    private static final long DEFAULT_VALUE = 4294967295L;
    private static final String ENTER_TYPE = "enter";
    private static final String EXIT_TYPE = "exit";
    private static final String KEY_LOCATION = "LOCATION";
    public static final String TAG = ("WMapping." + FrequentLocation.class.getSimpleName());
    private static FrequentLocation mFrequentLocation = null;
    private HwActivityRecognitionServiceConnection connect = new HwActivityRecognitionServiceConnection() {
        /* class com.android.server.hidata.wavemapping.dataprovider.FrequentLocation.AnonymousClass2 */

        @Override // com.huawei.android.location.activityrecognition.HwActivityRecognitionServiceConnection
        public void onServiceConnected() {
            LogUtil.i(false, "onServiceConnected()", new Object[0]);
            boolean unused = FrequentLocation.this.isConnected = true;
            FrequentLocation frequentLocation = FrequentLocation.this;
            String[] unused2 = frequentLocation.mSupportedEnvironments = frequentLocation.getSupportedEnvironment();
            if (FrequentLocation.this.mSupportedEnvironments == null || FrequentLocation.this.mSupportedEnvironments.length == 0) {
                LogUtil.d(false, "Can't get supported environment.", new Object[0]);
            } else {
                String[] access$200 = FrequentLocation.this.mSupportedEnvironments;
                int length = access$200.length;
                for (int i = 0; i < length; i++) {
                    LogUtil.d(false, " mSupportedEnvironments:%{public}s", access$200[i]);
                }
            }
            long currentTime = System.currentTimeMillis();
            OtherParameters params = new OtherParameters((double) (FrequentLocation.DEFAULT_VALUE & currentTime), (double) (currentTime >> 32), 0.0d, 0.0d, "");
            LogUtil.d(false, "initEnvironmentFunction ENV_TYPE_HOME:%{public}s", String.valueOf(FrequentLocation.this.initEnvironmentFunction("android.activity_recognition.env_home", params)));
            LogUtil.d(false, "initEnvironmentFunction ENV_TYPE_OFFICE:%{public}s", String.valueOf(FrequentLocation.this.initEnvironmentFunction("android.activity_recognition.env_office", params)));
            LogUtil.d(false, "enableEnvironmentEvent ENV_TYPE_HOME EVENT_TYPE_ENTER:%{public}s", String.valueOf(FrequentLocation.this.enableEnvironmentEvent("android.activity_recognition.env_home", 1, 0, params)));
            LogUtil.d(false, "enableEnvironmentEvent ENV_TYPE_HOME EVENT_TYPE_EXIT:%{public}s", String.valueOf(FrequentLocation.this.enableEnvironmentEvent("android.activity_recognition.env_home", 2, 0, params)));
            LogUtil.d(false, "enableEnvironmentEvent ENV_TYPE_OFFICE EVENT_TYPE_ENTER:%{public}s", String.valueOf(FrequentLocation.this.enableEnvironmentEvent("android.activity_recognition.env_office", 1, 0, params)));
            LogUtil.d(false, "enableEnvironmentEvent ENV_TYPE_OFFICE EVENT_TYPE_EXIT:%{public}s", String.valueOf(FrequentLocation.this.enableEnvironmentEvent("android.activity_recognition.env_office", 2, 0, params)));
        }

        @Override // com.huawei.android.location.activityrecognition.HwActivityRecognitionServiceConnection
        public void onServiceDisconnected() {
            LogUtil.i(false, "onServiceDisconnected()", new Object[0]);
            boolean unused = FrequentLocation.this.isConnected = false;
        }
    };
    private HwActivityRecognition hwAr;
    private HwActivityRecognitionHardwareSink hwArSink = new HwActivityRecognitionHardwareSink() {
        /* class com.android.server.hidata.wavemapping.dataprovider.FrequentLocation.AnonymousClass1 */

        @Override // com.huawei.android.location.activityrecognition.HwActivityRecognitionHardwareSink
        public void onActivityChanged(HwActivityChangedEvent activityChangedEvent) {
            LogUtil.i(false, "onActivityChanged .....", new Object[0]);
        }

        @Override // com.huawei.android.location.activityrecognition.HwActivityRecognitionHardwareSink
        public void onActivityExtendChanged(HwActivityChangedExtendEvent activityChangedExtendEvent) {
            LogUtil.i(false, "onActivityExtendChanged .....", new Object[0]);
        }

        @Override // com.huawei.android.location.activityrecognition.HwActivityRecognitionHardwareSink
        public void onEnvironmentChanged(HwEnvironmentChangedEvent environmentChangedEvent) {
            LogUtil.i(false, "onEnvironmentChanged .....", new Object[0]);
            if (environmentChangedEvent != null) {
                for (HwActivityRecognitionExtendEvent event : environmentChangedEvent.getEnvironmentRecognitionEvents()) {
                    if (event != null) {
                        int eventType = event.getEventType();
                        String activityType = event.getActivity();
                        LogUtil.d(false, "HwActivityRecognition callback", new Object[0]);
                        if (eventType == 1 && "android.activity_recognition.env_home".equals(activityType)) {
                            FrequentLocation.this.sendNotification(Constant.NAME_FREQLOCATION_HOME, 30);
                        } else if (eventType == 1 && "android.activity_recognition.env_office".equals(activityType)) {
                            FrequentLocation.this.sendNotification(Constant.NAME_FREQLOCATION_OFFICE, 30);
                        } else if (eventType == 2 && "android.activity_recognition.env_office".equals(activityType)) {
                            FrequentLocation.this.sendNotification(Constant.NAME_FREQLOCATION_OFFICE, 50);
                        } else if (eventType != 2 || !"android.activity_recognition.env_home".equals(activityType)) {
                            LogUtil.d(false, "unknown event", new Object[0]);
                        } else {
                            FrequentLocation.this.sendNotification(Constant.NAME_FREQLOCATION_HOME, 50);
                        }
                        LogUtil.i(false, "Activity:%{public}s EventType:%{public}s Timestamp:%{public}s OtherParams:%{public}s", activityType, "" + FrequentLocation.this.getEventType(eventType), String.valueOf(event.getTimestampNs()), event.getOtherParams().toString());
                    }
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public boolean isConnected = false;
    private String mLocation = Constant.NAME_FREQLOCATION_OTHER;
    private LocationDao mLocationDao;
    private Handler mMachineHandler;
    private long mOobTime = 0;
    /* access modifiers changed from: private */
    public String[] mSupportedEnvironments;
    private ParameterInfo param;

    public FrequentLocation(Handler handler) {
        LogUtil.i(false, "FrequentLocation", new Object[0]);
        this.mMachineHandler = handler;
        this.mLocationDao = new LocationDao();
        this.param = ParamManager.getInstance().getParameterInfo();
        this.mOobTime = System.currentTimeMillis();
    }

    public static synchronized FrequentLocation getInstance(Handler handler) {
        FrequentLocation frequentLocation;
        synchronized (FrequentLocation.class) {
            if (mFrequentLocation == null) {
                mFrequentLocation = new FrequentLocation(handler);
            }
            frequentLocation = mFrequentLocation;
        }
        return frequentLocation;
    }

    public static synchronized FrequentLocation getInstance() {
        FrequentLocation frequentLocation;
        synchronized (FrequentLocation.class) {
            frequentLocation = mFrequentLocation;
        }
        return frequentLocation;
    }

    public boolean updateWaveMapping(int location, int action) {
        LogUtil.d(false, "updateWaveMapping location:%{public}d action:%{public}d", Integer.valueOf(location), Integer.valueOf(action));
        if (action == 0 && location == 0) {
            sendNotification(Constant.NAME_FREQLOCATION_HOME, 30);
        } else if (action == 0 && location == 1) {
            sendNotification(Constant.NAME_FREQLOCATION_OFFICE, 30);
        } else if (action == 1 && location == 0) {
            sendNotification(Constant.NAME_FREQLOCATION_HOME, 50);
        } else if (action == 1 && location == 1) {
            sendNotification(Constant.NAME_FREQLOCATION_OFFICE, 50);
        } else {
            LogUtil.d(false, "UNKNOWN EVENT", new Object[0]);
            return false;
        }
        return true;
    }

    public boolean queryFrequentLocationState() {
        LogUtil.i(false, "queryFrequentLocationState", new Object[0]);
        this.mLocationDao.insertOobTime(this.mOobTime);
        String freqlocation = this.mLocationDao.getFrequentLocation();
        if (freqlocation != null && (Constant.NAME_FREQLOCATION_HOME.equals(freqlocation) || Constant.NAME_FREQLOCATION_OFFICE.equals(freqlocation))) {
            this.mLocation = freqlocation;
            LogUtil.d(false, "From database - device is already in:%{private}s", this.mLocation);
            Message msg = Message.obtain(this.mMachineHandler, 30);
            Bundle bundle = new Bundle();
            bundle.putCharSequence(KEY_LOCATION, this.mLocation);
            msg.setData(bundle);
            this.mMachineHandler.sendMessage(msg);
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void sendNotification(String location, int actionMessageId) {
        String tempLocation = location;
        int tempActionMessageId = actionMessageId;
        String oldLocation = this.mLocation;
        if (tempActionMessageId == 50 && !tempLocation.equals(this.mLocation)) {
            LogUtil.d(false, "MSG_OUT_FREQ_LOCATION - location not match mLocation:%{private}s location:%{private}s", this.mLocation, tempLocation);
        } else if (tempActionMessageId != 30 || !tempLocation.equals(this.mLocation)) {
            if (tempActionMessageId == 50) {
                this.mLocation = Constant.NAME_FREQLOCATION_OTHER;
                tempLocation = Constant.NAME_FREQLOCATION_OTHER;
            } else {
                this.mLocation = tempLocation;
                if (tempLocation.equals(Constant.NAME_FREQLOCATION_OTHER)) {
                    tempActionMessageId = 50;
                }
            }
            long now = System.currentTimeMillis();
            long lastUpdateTime = this.mLocationDao.getLastUpdateTime();
            long benefitChrTime = this.mLocationDao.getBenefitChrTime();
            long maxTime = benefitChrTime > lastUpdateTime ? benefitChrTime : lastUpdateTime;
            if (maxTime > 0 && now > maxTime) {
                int durationMinutes = Math.round(((float) (now - maxTime)) / 60000.0f);
                this.mLocationDao.accChrLeaveByFreqLoc(oldLocation);
                this.mLocationDao.addChrDurationByFreqLoc(durationMinutes, oldLocation);
            }
            this.mLocationDao.insert(this.mLocation);
            this.mLocationDao.accChrEnterByFreqLoc(this.mLocation);
            if (tempActionMessageId == 30 && !oldLocation.equals(tempLocation)) {
                LogUtil.i(false, "MSG_IN_FREQ_LOCATION - location is different oldLocation:%{private}s location:%{private}s", oldLocation, tempLocation);
                Message msg = Message.obtain(this.mMachineHandler, 50);
                Bundle bundle = new Bundle();
                bundle.putCharSequence(KEY_LOCATION, oldLocation);
                msg.setData(bundle);
                this.mMachineHandler.sendMessage(msg);
            }
            LogUtil.d(false, "FrequentLocation switch from %{private}s to %{private}s", oldLocation, this.mLocation);
            Message msg2 = Message.obtain(this.mMachineHandler, tempActionMessageId);
            Bundle bundle2 = new Bundle();
            bundle2.putCharSequence(KEY_LOCATION, tempLocation);
            msg2.setData(bundle2);
            this.mMachineHandler.sendMessage(msg2);
        } else {
            LogUtil.d(false, "MSG_IN_FREQ_LOCATION - location is the same mLocation:%{private}s location:%{private}s", this.mLocation, tempLocation);
        }
    }

    public boolean isConnected() {
        return this.isConnected;
    }

    public void connectService(Context context) {
        LogUtil.i(false, "connectService", new Object[0]);
        this.hwAr = new HwActivityRecognition(context);
        this.hwAr.connectService(this.hwArSink, this.connect);
    }

    public void disconnectService() {
        if (this.hwAr != null && this.isConnected) {
            LogUtil.i(false, "disconnectService", new Object[0]);
            this.hwAr.disconnectService();
            this.isConnected = false;
            this.mSupportedEnvironments = null;
        }
    }

    public String[] getSupportedEnvironment() {
        HwActivityRecognition hwActivityRecognition = this.hwAr;
        if (hwActivityRecognition != null) {
            return hwActivityRecognition.getSupportedEnvironments();
        }
        return new String[0];
    }

    public HwEnvironmentChangedEvent getCurrentEnvironment() {
        HwActivityRecognition hwActivityRecognition = this.hwAr;
        if (hwActivityRecognition != null) {
            return hwActivityRecognition.getCurrentEnvironment();
        }
        return null;
    }

    public boolean initEnvironmentFunction(String environment, OtherParameters params) {
        HwActivityRecognition hwActivityRecognition = this.hwAr;
        if (hwActivityRecognition != null) {
            return hwActivityRecognition.initEnvironmentFunction(environment, params);
        }
        return false;
    }

    public boolean exitEnvironmentFunction(String environment, OtherParameters params) {
        HwActivityRecognition hwActivityRecognition = this.hwAr;
        if (hwActivityRecognition != null) {
            return hwActivityRecognition.exitEnvironmentFunction(environment, params);
        }
        return false;
    }

    public boolean enableEnvironmentEvent(String environment, int eventType, long reportLatencyNs, OtherParameters params) {
        HwActivityRecognition hwActivityRecognition = this.hwAr;
        if (hwActivityRecognition != null) {
            return hwActivityRecognition.enableEnvironmentEvent(environment, eventType, reportLatencyNs, params);
        }
        return false;
    }

    public boolean disableEnvironmentEvent(String environment, int eventType) {
        HwActivityRecognition hwActivityRecognition = this.hwAr;
        if (hwActivityRecognition != null) {
            return hwActivityRecognition.disableEnvironmentEvent(environment, eventType);
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public Object getEventType(int type) {
        if (type == 1) {
            return ENTER_TYPE;
        }
        if (type == 2) {
            return EXIT_TYPE;
        }
        return String.valueOf(type);
    }

    public boolean flush() {
        HwActivityRecognition hwActivityRecognition = this.hwAr;
        if (hwActivityRecognition != null) {
            return hwActivityRecognition.flush();
        }
        return false;
    }

    public void checkInFrequentLocation(Context context) {
        LogUtil.i(false, "checkInFrequentLocation", new Object[0]);
        try {
            HwEnvironmentChangedEvent currentEnvironment = getCurrentEnvironment();
            if (currentEnvironment != null) {
                for (HwActivityRecognitionExtendEvent event : currentEnvironment.getEnvironmentRecognitionEvents()) {
                    if (event != null) {
                        int eventType = event.getEventType();
                        String activityType = event.getActivity();
                        LogUtil.d(false, "Check HwActivityRecognition Activity:%{public}s EventType:%{public}s", activityType, "" + getEventType(eventType));
                        if (eventType == 1 && "android.activity_recognition.env_home".equals(activityType)) {
                            sendNotification(Constant.NAME_FREQLOCATION_HOME, 30);
                        } else if (eventType != 1 || !"android.activity_recognition.env_office".equals(activityType)) {
                            sendNotification(Constant.NAME_FREQLOCATION_OTHER, 30);
                        } else {
                            sendNotification(Constant.NAME_FREQLOCATION_OFFICE, 30);
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            LogUtil.e(false, "LocatingState RuntimeException,e %{public}s", e.getMessage());
        } catch (Exception e2) {
            LogUtil.e(false, "checkInFrequentLocation failed by Exception", new Object[0]);
        }
    }
}
