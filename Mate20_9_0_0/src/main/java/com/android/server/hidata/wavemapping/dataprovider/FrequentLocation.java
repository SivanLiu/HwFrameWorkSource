package com.android.server.hidata.wavemapping.dataprovider;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.cons.ParamManager;
import com.android.server.hidata.wavemapping.dao.LocationDAO;
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
    public static final String TAG;
    private static FrequentLocation mFrequentLocation = null;
    private HwActivityRecognitionServiceConnection connect = new HwActivityRecognitionServiceConnection() {
        public void onServiceConnected() {
            LogUtil.i("onServiceConnected()");
            FrequentLocation.this.isConnected = true;
            FrequentLocation.this.mSupportedEnvironments = FrequentLocation.this.getSupportedEnvironment();
            if (FrequentLocation.this.mSupportedEnvironments == null || FrequentLocation.this.mSupportedEnvironments.length == 0) {
                LogUtil.d("Can't get supported environment.");
            } else {
                for (String append : FrequentLocation.this.mSupportedEnvironments) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(" mSupportedEnvironments:");
                    stringBuilder.append(append);
                    LogUtil.d(stringBuilder.toString());
                }
            }
            long currentTime = System.currentTimeMillis();
            double param2 = (double) (currentTime >> 32);
            OtherParameters params = new OtherParameters((double) (4294967295L & currentTime), param2, 0.0d, 0.0d, "");
            boolean result = FrequentLocation.this.initEnvironmentFunction(HwActivityRecognition.ENV_TYPE_HOME, params);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("initEnvironmentFunction ENV_TYPE_HOME:");
            stringBuilder2.append(result);
            LogUtil.d(stringBuilder2.toString());
            result = FrequentLocation.this.initEnvironmentFunction(HwActivityRecognition.ENV_TYPE_OFFICE, params);
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("initEnvironmentFunction ENV_TYPE_OFFICE:");
            stringBuilder2.append(result);
            LogUtil.d(stringBuilder2.toString());
            OtherParameters otherParameters = params;
            result = FrequentLocation.this.enableEnvironmentEvent(HwActivityRecognition.ENV_TYPE_HOME, 1, 0, otherParameters);
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("enableEnvironmentEvent ENV_TYPE_HOME EVENT_TYPE_ENTER:");
            stringBuilder2.append(result);
            LogUtil.d(stringBuilder2.toString());
            result = FrequentLocation.this.enableEnvironmentEvent(HwActivityRecognition.ENV_TYPE_HOME, 2, 0, otherParameters);
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("enableEnvironmentEvent ENV_TYPE_HOME EVENT_TYPE_EXIT:");
            stringBuilder2.append(result);
            LogUtil.d(stringBuilder2.toString());
            result = FrequentLocation.this.enableEnvironmentEvent(HwActivityRecognition.ENV_TYPE_OFFICE, 1, 0, otherParameters);
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("enableEnvironmentEvent ENV_TYPE_OFFICE EVENT_TYPE_ENTER:");
            stringBuilder2.append(result);
            LogUtil.d(stringBuilder2.toString());
            result = FrequentLocation.this.enableEnvironmentEvent(HwActivityRecognition.ENV_TYPE_OFFICE, 2, 0, otherParameters);
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("enableEnvironmentEvent ENV_TYPE_OFFICE EVENT_TYPE_EXIT:");
            stringBuilder2.append(result);
            LogUtil.d(stringBuilder2.toString());
        }

        public void onServiceDisconnected() {
            LogUtil.i("onServiceDisconnected()");
            FrequentLocation.this.isConnected = false;
        }
    };
    private HwActivityRecognition hwAR;
    private HwActivityRecognitionHardwareSink hwArSink = new HwActivityRecognitionHardwareSink() {
        public void onActivityChanged(HwActivityChangedEvent activityChangedEvent) {
            LogUtil.i("onActivityChanged .....");
        }

        public void onActivityExtendChanged(HwActivityChangedExtendEvent activityChangedExtendEvent) {
            LogUtil.i("onActivityExtendChanged .....");
        }

        public void onEnvironmentChanged(HwEnvironmentChangedEvent environmentChangedEvent) {
            LogUtil.i("onEnvironmentChanged .....");
            if (environmentChangedEvent != null) {
                for (HwActivityRecognitionExtendEvent event : environmentChangedEvent.getEnvironmentRecognitionEvents()) {
                    if (event != null) {
                        int eventType = event.getEventType();
                        String activityType = event.getActivity();
                        LogUtil.d("HwActivityRecognition callback");
                        if (eventType == 1 && activityType.equals(HwActivityRecognition.ENV_TYPE_HOME)) {
                            FrequentLocation.this.sendNotification(Constant.NAME_FREQLOCATION_HOME, 30);
                        } else if (eventType == 1 && activityType.equals(HwActivityRecognition.ENV_TYPE_OFFICE)) {
                            FrequentLocation.this.sendNotification(Constant.NAME_FREQLOCATION_OFFICE, 30);
                        } else if (eventType == 2 && activityType.equals(HwActivityRecognition.ENV_TYPE_OFFICE)) {
                            FrequentLocation.this.sendNotification(Constant.NAME_FREQLOCATION_OFFICE, 50);
                        } else if (eventType == 2 && activityType.equals(HwActivityRecognition.ENV_TYPE_HOME)) {
                            FrequentLocation.this.sendNotification(Constant.NAME_FREQLOCATION_HOME, 50);
                        } else {
                            LogUtil.d("unknown event");
                        }
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Activity:");
                        stringBuilder.append(activityType);
                        stringBuilder.append(" EventType:");
                        stringBuilder.append(FrequentLocation.this.getEventType(eventType));
                        stringBuilder.append(" Timestamp:");
                        stringBuilder.append(event.getTimestampNs());
                        stringBuilder.append(" OtherParams:");
                        stringBuilder.append(event.getOtherParams());
                        LogUtil.i(stringBuilder.toString());
                    }
                }
            }
        }
    };
    private boolean isConnected = false;
    private String mLocation = Constant.NAME_FREQLOCATION_OTHER;
    private LocationDAO mLocationDAO;
    private Handler mMachineHandler;
    private long mOOBTime = 0;
    private String[] mSupportedEnvironments;
    private ParameterInfo param;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(FrequentLocation.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    public boolean isConnected() {
        return this.isConnected;
    }

    public FrequentLocation(Handler handler) {
        LogUtil.i("FrequentLocation");
        this.mMachineHandler = handler;
        this.mLocationDAO = new LocationDAO();
        this.param = ParamManager.getInstance().getParameterInfo();
        this.mOOBTime = System.currentTimeMillis();
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateWaveMapping location:");
        stringBuilder.append(location);
        stringBuilder.append(" action:");
        stringBuilder.append(action);
        LogUtil.d(stringBuilder.toString());
        if (action == 0 && location == 0) {
            sendNotification(Constant.NAME_FREQLOCATION_HOME, 30);
        } else if (action == 0 && location == 1) {
            sendNotification(Constant.NAME_FREQLOCATION_OFFICE, 30);
        } else if (action == 1 && location == 0) {
            sendNotification(Constant.NAME_FREQLOCATION_HOME, 50);
        } else if (action == 1 && location == 1) {
            sendNotification(Constant.NAME_FREQLOCATION_OFFICE, 50);
        } else {
            LogUtil.d("UNKNOWN EVENT");
            return false;
        }
        return true;
    }

    public boolean queryFrequentLocationState() {
        LogUtil.i("queryFrequentLocationState");
        this.mLocationDAO.insertOOBTime(this.mOOBTime);
        String freqlocation = this.mLocationDAO.getFrequentLocation();
        if (freqlocation != null && (freqlocation.equals(Constant.NAME_FREQLOCATION_HOME) || freqlocation.equals(Constant.NAME_FREQLOCATION_OFFICE))) {
            this.mLocation = freqlocation;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("From database - device is already in:");
            stringBuilder.append(this.mLocation);
            LogUtil.d(stringBuilder.toString());
            Message msg = Message.obtain(this.mMachineHandler, 30);
            Bundle bundle = new Bundle();
            bundle.putCharSequence("LOCATION", this.mLocation);
            msg.setData(bundle);
            this.mMachineHandler.sendMessage(msg);
        }
        return true;
    }

    private void sendNotification(String location, int actionMessageID) {
        String oldLocation = this.mLocation;
        StringBuilder stringBuilder;
        if (actionMessageID == 50 && !this.mLocation.equals(location)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("MSG_OUT_FREQ_LOCATION - location not match mLocation:");
            stringBuilder.append(this.mLocation);
            stringBuilder.append(" location:");
            stringBuilder.append(location);
            LogUtil.d(stringBuilder.toString());
        } else if (actionMessageID == 30 && this.mLocation.equals(location)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("MSG_IN_FREQ_LOCATION - location is the same mLocation:");
            stringBuilder.append(this.mLocation);
            stringBuilder.append(" location:");
            stringBuilder.append(location);
            LogUtil.d(stringBuilder.toString());
        } else {
            Message msg;
            Bundle bundle;
            if (actionMessageID == 50) {
                this.mLocation = Constant.NAME_FREQLOCATION_OTHER;
                location = Constant.NAME_FREQLOCATION_OTHER;
            } else {
                this.mLocation = location;
                if (location.equals(Constant.NAME_FREQLOCATION_OTHER)) {
                    actionMessageID = 50;
                }
            }
            long now = System.currentTimeMillis();
            long lastupdatetime = this.mLocationDAO.getlastUpdateTime();
            long benefitCHRTime = this.mLocationDAO.getBenefitCHRTime();
            long maxtime = benefitCHRTime > lastupdatetime ? benefitCHRTime : lastupdatetime;
            if (maxtime > 0 && now > maxtime) {
                int duration_minutes = Math.round(((float) (now - maxtime)) / 1198153728);
                this.mLocationDAO.accCHRLeavebyFreqLoc(oldLocation);
                this.mLocationDAO.addCHRDurationbyFreqLoc(duration_minutes, oldLocation);
            }
            this.mLocationDAO.insert(this.mLocation);
            this.mLocationDAO.accCHREnterybyFreqLoc(this.mLocation);
            if (actionMessageID == 30 && !oldLocation.equals(location)) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("MSG_IN_FREQ_LOCATION - location is different oldLocation:");
                stringBuilder2.append(oldLocation);
                stringBuilder2.append(" location:");
                stringBuilder2.append(location);
                LogUtil.i(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("send addtional MSG_OUT_FREQ_LOCATION in advanced for old location:");
                stringBuilder2.append(oldLocation);
                LogUtil.d(stringBuilder2.toString());
                msg = Message.obtain(this.mMachineHandler, 50);
                bundle = new Bundle();
                bundle.putCharSequence("LOCATION", oldLocation);
                msg.setData(bundle);
                this.mMachineHandler.sendMessage(msg);
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("FrequentLocation switch from ");
            stringBuilder.append(oldLocation);
            stringBuilder.append(" to ");
            stringBuilder.append(this.mLocation);
            LogUtil.d(stringBuilder.toString());
            msg = Message.obtain(this.mMachineHandler, actionMessageID);
            bundle = new Bundle();
            bundle.putCharSequence("LOCATION", location);
            msg.setData(bundle);
            this.mMachineHandler.sendMessage(msg);
        }
    }

    public void connectService(Context context) {
        LogUtil.i("connectService");
        this.hwAR = new HwActivityRecognition(context);
        this.hwAR.connectService(this.hwArSink, this.connect);
    }

    public void disconnectService() {
        if (this.hwAR != null && this.isConnected) {
            LogUtil.i("disconnectService");
            this.hwAR.disconnectService();
            this.isConnected = false;
            this.mSupportedEnvironments = null;
        }
    }

    public String[] getSupportedEnvironment() {
        if (this.hwAR != null) {
            return this.hwAR.getSupportedEnvironments();
        }
        return new String[0];
    }

    public HwEnvironmentChangedEvent getCurrentEnvironment() {
        if (this.hwAR != null) {
            return this.hwAR.getCurrentEnvironment();
        }
        return null;
    }

    public boolean initEnvironmentFunction(String environment, OtherParameters params) {
        if (this.hwAR != null) {
            return this.hwAR.initEnvironmentFunction(environment, params);
        }
        return false;
    }

    public boolean exitEnvironmentFunction(String environment, OtherParameters params) {
        if (this.hwAR != null) {
            return this.hwAR.exitEnvironmentFunction(environment, params);
        }
        return false;
    }

    public boolean enableEnvironmentEvent(String environment, int eventType, long reportLatencyNs, OtherParameters params) {
        if (this.hwAR != null) {
            return this.hwAR.enableEnvironmentEvent(environment, eventType, reportLatencyNs, params);
        }
        return false;
    }

    public boolean disableEnvironmentEvent(String environment, int eventType) {
        if (this.hwAR != null) {
            return this.hwAR.disableEnvironmentEvent(environment, eventType);
        }
        return false;
    }

    protected Object getEventType(int type) {
        if (type == 1) {
            return "enter";
        }
        if (type == 2) {
            return "exit";
        }
        return String.valueOf(type);
    }

    public boolean flush() {
        if (this.hwAR != null) {
            return this.hwAR.flush();
        }
        return false;
    }

    public void checkInFrequentLocation(Context context) {
        StringBuilder stringBuilder;
        LogUtil.i("checkInFrequentLocation");
        try {
            HwEnvironmentChangedEvent currentEnvironment = getCurrentEnvironment();
            if (currentEnvironment != null) {
                for (HwActivityRecognitionExtendEvent event : currentEnvironment.getEnvironmentRecognitionEvents()) {
                    if (event != null) {
                        int eventType = event.getEventType();
                        String activityType = event.getActivity();
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Check HwActivityRecognition Activity:");
                        stringBuilder2.append(activityType);
                        stringBuilder2.append(" EventType:");
                        stringBuilder2.append(getEventType(eventType));
                        LogUtil.d(stringBuilder2.toString());
                        if (eventType == 1 && activityType.equals(HwActivityRecognition.ENV_TYPE_HOME)) {
                            sendNotification(Constant.NAME_FREQLOCATION_HOME, 30);
                        } else if (eventType == 1 && activityType.equals(HwActivityRecognition.ENV_TYPE_OFFICE)) {
                            sendNotification(Constant.NAME_FREQLOCATION_OFFICE, 30);
                        } else {
                            sendNotification(Constant.NAME_FREQLOCATION_OTHER, 30);
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("LocatingState RuntimeException,e");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("LocatingState,e");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
    }
}
