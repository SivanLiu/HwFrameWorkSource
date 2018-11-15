package com.huawei.nearbysdk.closeRange;

import android.os.HandlerThread;
import android.os.Looper;
import android.os.RemoteException;
import com.huawei.nearbysdk.BleScanLevel;
import com.huawei.nearbysdk.HwLog;
import com.huawei.nearbysdk.INearbyAdapter;
import java.util.HashMap;

public class CloseRangeAdapter implements CloseRangeInterface {
    private static final String TAG = "CloseRangeAdapter";
    private CloseRangeBusinessCounter businessCounter = new CloseRangeBusinessCounter();
    private HashMap<CloseRangeDeviceFilter, CloseRangeDeviceListenerTransport> deviceListenerMap = new HashMap();
    private HashMap<CloseRangeEventFilter, CloseRangeEventListenerTransport> eventListenerMap = new HashMap();
    private HandlerThread handlerThread = null;
    private INearbyAdapter nearbyService = null;

    public CloseRangeAdapter(HandlerThread handlerThread) {
        this.handlerThread = handlerThread;
    }

    public void setNearbyService(INearbyAdapter nearbyService) {
        this.nearbyService = nearbyService;
    }

    public boolean subscribeEvent(CloseRangeEventFilter eventFilter, CloseRangeEventListener eventListener) {
        HwLog.d(TAG, "subscribeEvent");
        if (!eventFilterCheck(eventFilter) || eventListener == null) {
            HwLog.e(TAG, "null input");
            return false;
        } else if (this.nearbyService == null) {
            HwLog.e(TAG, "nearbyService is null. subscribe return false");
            return false;
        } else if (this.eventListenerMap.containsKey(eventFilter)) {
            HwLog.d(TAG, "device listener already registered && return");
            return false;
        } else {
            boolean result = false;
            CloseRangeEventListenerTransport transport = new CloseRangeEventListenerTransport(eventListener, getLooper());
            try {
                result = this.nearbyService.subscribeEvent(eventFilter, transport);
                if (result) {
                    this.eventListenerMap.put(eventFilter, transport);
                    this.businessCounter.increase(eventFilter.getBusinessType());
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("remote error ");
                stringBuilder.append(e.getMessage());
                HwLog.e(str, stringBuilder.toString());
            }
            return result;
        }
    }

    private boolean eventFilterCheck(CloseRangeEventFilter eventFilter) {
        if (eventFilter == null || eventFilter.getBusinessType() == null) {
            return false;
        }
        return true;
    }

    private Looper getLooper() {
        Looper looper = Looper.myLooper();
        if (looper == null) {
            if (this.handlerThread != null) {
                looper = this.handlerThread.getLooper();
            } else {
                HwLog.e(TAG, "can not get looper");
                return null;
            }
        }
        return looper;
    }

    public boolean unSubscribeEvent(CloseRangeEventFilter eventFilter) {
        HwLog.d(TAG, "unSubscribeEvent");
        boolean result = false;
        if (eventFilter == null) {
            HwLog.e(TAG, "null input");
            return false;
        } else if (this.nearbyService == null) {
            HwLog.e(TAG, "nearbyService is null");
            return false;
        } else if (this.eventListenerMap.containsKey(eventFilter)) {
            try {
                result = this.nearbyService.unSubscribeEvent(eventFilter);
                if (result) {
                    this.eventListenerMap.remove(eventFilter);
                    this.businessCounter.decrease(eventFilter.getBusinessType());
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("remote error");
                stringBuilder.append(e.getLocalizedMessage());
                HwLog.e(str, stringBuilder.toString());
            }
            return result;
        } else {
            HwLog.e(TAG, "not subscribe yet");
            return false;
        }
    }

    public boolean subscribeDevice(CloseRangeDeviceFilter deviceFilter, CloseRangeDeviceListener deviceListener) {
        HwLog.d(TAG, "subscribeDevice");
        if (!deviceFilterCheck(deviceFilter) || deviceListener == null) {
            HwLog.e(TAG, "null input");
            return false;
        } else if (this.nearbyService == null) {
            HwLog.e(TAG, "nearbyService is null. subscribe return false");
            return false;
        } else if (this.deviceListenerMap.containsKey(deviceFilter)) {
            HwLog.d(TAG, "device listener already registered && return");
            return false;
        } else {
            boolean result = false;
            CloseRangeDeviceListenerTransport transport = new CloseRangeDeviceListenerTransport(deviceListener, getLooper());
            try {
                result = this.nearbyService.subscribeDevice(deviceFilter, transport);
                if (result) {
                    this.deviceListenerMap.put(deviceFilter, transport);
                    this.businessCounter.increase(deviceFilter.getBusinessType());
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("remote error ");
                stringBuilder.append(e.getMessage());
                HwLog.e(str, stringBuilder.toString());
            }
            return result;
        }
    }

    private boolean deviceFilterCheck(CloseRangeDeviceFilter deviceFilter) {
        boolean z = false;
        if (deviceFilter == null || deviceFilter.getBusinessType() == null) {
            return false;
        }
        if (deviceFilter.getDeviceMAC() != null) {
            z = true;
        }
        return z;
    }

    public boolean unSubscribeDevice(CloseRangeDeviceFilter deviceFilter) {
        HwLog.d(TAG, "unSubscribeDevice");
        if (!deviceFilterCheck(deviceFilter)) {
            HwLog.e(TAG, "null input");
            return false;
        } else if (this.nearbyService == null) {
            HwLog.e(TAG, "nearbyService is null");
            return false;
        } else if (this.deviceListenerMap.containsKey(deviceFilter)) {
            boolean result = false;
            try {
                result = this.nearbyService.unSubscribeDevice(deviceFilter);
                if (result) {
                    this.deviceListenerMap.remove(deviceFilter);
                    this.businessCounter.decrease(deviceFilter.getBusinessType());
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("remote error");
                stringBuilder.append(e.getMessage());
                HwLog.e(str, stringBuilder.toString());
            }
            return result;
        } else {
            HwLog.e(TAG, "not subscribe yet");
            return false;
        }
    }

    public boolean setFrequency(CloseRangeBusinessType type, BleScanLevel frequency) {
        HwLog.d(TAG, "setFrequency");
        boolean result = false;
        if (type == null || frequency == null) {
            HwLog.e(TAG, "null input");
            return false;
        } else if (this.nearbyService == null) {
            HwLog.e(TAG, "nearbyService is null");
            return false;
        } else if (this.businessCounter.containsType(type)) {
            try {
                result = this.nearbyService.setFrequency(type, frequency);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("remote error");
                stringBuilder.append(e.getLocalizedMessage());
                HwLog.e(str, stringBuilder.toString());
            }
            return result;
        } else {
            HwLog.e(TAG, "no such business type");
            return false;
        }
    }
}
