package com.android.server.policy;

import android.os.Handler;
import android.os.SystemProperties;
import android.util.Log;
import com.android.server.policy.IHWExtMotionRotationProcessor.WindowOrientationListenerProxy;
import com.huawei.hwextdevice.HWExtDeviceEvent;
import com.huawei.hwextdevice.HWExtDeviceEventListener;
import com.huawei.hwextdevice.HWExtDeviceManager;
import com.huawei.hwextdevice.devices.HWExtMotion;

public class HWExtMotionRotationProcessorEx implements IHWExtMotionRotationProcessor {
    private static final int MOTION_VALUES_LEN = 2;
    private HWExtDeviceEventListener mHWEDListener = new HWExtDeviceEventListener() {
        public void onDeviceDataChanged(HWExtDeviceEvent hwextDeviceEvent) {
            float[] deviceValues = hwextDeviceEvent.getDeviceValues();
            if (deviceValues == null) {
                Log.e("HWEMRP", "onDeviceDataChanged  deviceValues is null ");
            } else if (deviceValues.length >= 2) {
                int proposedRotation = (int) deviceValues[1];
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onDeviceDataChanged  proposedRotation:");
                stringBuilder.append(proposedRotation);
                Log.d("HWEMRP", stringBuilder.toString());
                int oldProposedRotation = HWExtMotionRotationProcessorEx.this.mProposedRotation;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("onDeviceDataChanged  oldProposedRotation:");
                stringBuilder2.append(oldProposedRotation);
                Log.d("HWEMRP", stringBuilder2.toString());
                HWExtMotionRotationProcessorEx.this.mProposedRotation = proposedRotation;
                if (proposedRotation != oldProposedRotation && proposedRotation >= 0) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("notifyProposedRotation: ");
                    stringBuilder2.append(HWExtMotionRotationProcessorEx.this.mProposedRotation);
                    Log.d("HWEMRP", stringBuilder2.toString());
                    HWExtMotionRotationProcessorEx.this.mWOLPxy.setCurrentOrientation(HWExtMotionRotationProcessorEx.this.mProposedRotation);
                    HWExtMotionRotationProcessorEx.this.mWOLPxy.notifyProposedRotation(HWExtMotionRotationProcessorEx.this.mProposedRotation);
                }
            }
        }
    };
    private HWExtDeviceManager mHWEDManager = null;
    private HWExtMotion mHWExtMotion = null;
    private int mProposedRotation = (SystemProperties.getInt("ro.panel.hw_orientation", 0) / 90);
    private WindowOrientationListenerProxy mWOLPxy = null;

    public HWExtMotionRotationProcessorEx(WindowOrientationListenerProxy wolPxy) {
        this.mWOLPxy = wolPxy;
        initProcessor();
    }

    public void enableMotionRotation(Handler handler) {
        if (this.mHWEDManager != null) {
            this.mHWEDManager.registerDeviceListener(this.mHWEDListener, this.mHWExtMotion, handler);
            Log.d("HWEMRP", "registerDeviceListener  mHWEDListener");
            return;
        }
        Log.e("HWEMRP", "enableMotionRotation  mHWEDManager is null ");
    }

    public void disableMotionRotation() {
        if (this.mHWEDManager != null) {
            this.mHWEDManager.unregisterDeviceListener(this.mHWEDListener, this.mHWExtMotion);
        } else {
            Log.e("HWEMRP", "disableMotionRotation  mHWEDManager is null ");
        }
    }

    public int getProposedRotation() {
        return this.mProposedRotation;
    }

    private void destroy() {
        if (this.mHWEDManager != null) {
            if (this.mHWExtMotion == null) {
                this.mHWExtMotion = null;
            }
            try {
                this.mHWEDManager.dispose();
                this.mHWEDManager = null;
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("destroy  error : ");
                stringBuilder.append(e);
                Log.e("HWEMRP", stringBuilder.toString());
            }
        }
    }

    private void initProcessor() {
        this.mHWEDManager = HWExtDeviceManager.getInstance(null);
        this.mHWExtMotion = new HWExtMotion(700);
    }
}
