package com.android.server.display;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import com.android.server.display.DisplayEffectMonitor.MonitorModule;
import java.util.ArrayList;
import java.util.List;

class BackLightMonitorManager implements MonitorModule {
    private static final boolean HWFLOW;
    private static final String PARAM_ENABLE = "enable";
    private static final String TAG = "BackLightMonitorManager";
    private static final String TYPE_XML_CONFIG = "xmlConfig";
    private List<MonitorModule> mChildMonitorList;
    private BackLightCommonData mCommonData;
    private HandlerThread mHandlerThread;
    private MessageHandler mMessageHandler;

    private class MessageHandler extends Handler {
        public MessageHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == MsgType.PARAM.ordinal() || msg.what == MsgType.STATE_TIMER.ordinal()) {
                BackLightMonitorManager.this.processMonitorParam((ArrayMap) msg.obj);
            } else if (msg.what == MsgType.UPLOAD_TIMER.ordinal()) {
                BackLightMonitorManager.this.processUploadTimer();
            }
        }
    }

    public enum MsgType {
        PARAM,
        UPLOAD_TIMER,
        STATE_TIMER
    }

    static {
        boolean isLoggable = !Log.HWINFO ? Log.HWModuleLog ? Log.isLoggable(TAG, 4) : false : true;
        HWFLOW = isLoggable;
    }

    public BackLightMonitorManager(DisplayEffectMonitor monitor) {
        if (monitor != null) {
            this.mHandlerThread = new HandlerThread(TAG);
            this.mHandlerThread.start();
            this.mMessageHandler = new MessageHandler(this.mHandlerThread.getLooper());
            this.mCommonData = new BackLightCommonData();
            int usertype = SystemProperties.getInt("ro.logsystem.usertype", 0);
            boolean isCommercialVersion = usertype == 1 || usertype == 6;
            this.mCommonData.setCommercialVersion(isCommercialVersion);
            this.mChildMonitorList = new ArrayList();
            this.mChildMonitorList.add(new AmbientLightMonitor(monitor, this));
            this.mChildMonitorList.add(new BrightnessSeekBarMonitor(monitor, this));
            this.mChildMonitorList.add(new BrightnessSettingsMonitor(monitor, this));
            this.mChildMonitorList.add(new BrightnessStateMonitor(monitor, this));
            if (HWFLOW) {
                Slog.i(TAG, "new instance success, isCommercialVersion=" + isCommercialVersion);
            }
        }
    }

    public boolean isParamOwner(String paramType) {
        if (paramType == null) {
            return false;
        }
        if (paramType.equals(TYPE_XML_CONFIG)) {
            return true;
        }
        for (MonitorModule module : this.mChildMonitorList) {
            if (module.isParamOwner(paramType)) {
                return true;
            }
        }
        return false;
    }

    public void sendMonitorParam(ArrayMap<String, Object> params) {
        if (this.mMessageHandler != null) {
            this.mMessageHandler.sendMessage(this.mMessageHandler.obtainMessage(MsgType.PARAM.ordinal(), params));
        }
    }

    public void sendMonitorMsgDelayed(MsgType type, ArrayMap<String, Object> params, long delayMillis) {
        if (this.mMessageHandler != null && type != null && params != null && delayMillis > 0) {
            this.mMessageHandler.sendMessageDelayed(this.mMessageHandler.obtainMessage(type.ordinal(), params), delayMillis);
        }
    }

    public void removeMonitorMsg(MsgType type) {
        if (this.mMessageHandler != null && type != null) {
            this.mMessageHandler.removeMessages(type.ordinal());
        }
    }

    public void triggerUploadTimer() {
        if (this.mMessageHandler != null) {
            this.mMessageHandler.sendMessage(this.mMessageHandler.obtainMessage(MsgType.UPLOAD_TIMER.ordinal()));
        }
    }

    private void processMonitorParam(ArrayMap<String, Object> params) {
        String paramType = (String) params.get(MonitorModule.PARAM_TYPE);
        if (paramType.equals(TYPE_XML_CONFIG)) {
            xmlConfig(params);
            return;
        }
        for (MonitorModule module : this.mChildMonitorList) {
            if (module.isParamOwner(paramType)) {
                module.sendMonitorParam(params);
            }
        }
    }

    private void processUploadTimer() {
        for (MonitorModule module : this.mChildMonitorList) {
            module.triggerUploadTimer();
        }
    }

    public BackLightCommonData getBackLightCommonData() {
        return this.mCommonData;
    }

    private void xmlConfig(ArrayMap<String, Object> params) {
        if (params.get(PARAM_ENABLE) instanceof Boolean) {
            boolean isProductEnable = ((Boolean) params.get(PARAM_ENABLE)).booleanValue();
            if (HWFLOW) {
                Slog.i(TAG, "xmlConfig() isProductEnable=" + isProductEnable);
            }
            this.mCommonData.setProductEnable(isProductEnable);
            return;
        }
        Slog.e(TAG, "xmlConfig() can't get param: enable");
    }

    public boolean needHourTimer() {
        return this.mCommonData.isProductEnable() ? this.mCommonData.isCommercialVersion() ^ 1 : false;
    }

    public boolean needSceneRecognition() {
        return this.mCommonData.isProductEnable() ? this.mCommonData.isCommercialVersion() ^ 1 : false;
    }
}
