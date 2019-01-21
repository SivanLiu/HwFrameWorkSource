package com.huawei.hwextdevice;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.HwLog;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.huawei.hwextdevice.devices.HWExtMotion;
import com.huawei.hwextdevice.devices.IHWExtDevice;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HWExtDeviceManager {
    public static final boolean DEBUG_FLAG = false;
    private static final int DEVICE_KEY_RATE = 100;
    private static final int ENABLE_DEVICE_SUCCESS = 1;
    private static final int ENABLE_DEVICE_SUCCESS_REPEAT = 2;
    public static final String TAG = "HWExtDeviceManager";
    private static HWExtDeviceManager mInstance = null;
    private static HWExtDeviceEvent mLastOritation = null;
    private static Object mOritationLock = new Object();
    private static Object sDeviceModuleLock = new Object();
    private static final SparseArray<IHWExtDevice> sHandleToDeviceList = new SparseArray();
    private static final int userType = SystemProperties.getInt("ro.logsystem.usertype", 1);
    private Context mContext = null;
    private HashMap<HWExtDeviceEventListener, DeviceEventQueue> mDeviceListenerMap = new HashMap();
    private ArrayList<IHWExtDevice> mFullDevicesList = new ArrayList();
    private ArrayList<Integer> mHWExtDeviceTypeList = new ArrayList();
    private boolean sFullDeviceGetOver = false;

    static final class DeviceEventQueue {
        private int DEVICE_MSG_TYPE_RESULT;
        private SparseBooleanArray mActiveDevices;
        private SparseArray<HWExtDeviceEvent> mDeviceEvents;
        private HWExtDeviceHandler mDeviceHandler;
        private float[] mDeviceValues;
        private HWExtDeviceEventListener mListener;
        private long nDevcieEventQueue;

        private class HWExtDeviceHandler extends Handler {
            public HWExtDeviceHandler(Looper looper) {
                super(looper);
            }

            public void handleMessage(Message msg) {
                if (DeviceEventQueue.this.mListener != null) {
                    DeviceEventQueue.this.mListener.onDeviceDataChanged((HWExtDeviceEvent) msg.obj);
                }
            }
        }

        public native long nativeCreateEventQueue(DeviceEventQueue deviceEventQueue, float[] fArr);

        public native void nativeDestroyEventQueue(long j);

        public native int nativeDisableDevice(long j, IHWExtDevice iHWExtDevice);

        public native int nativeEnableDevice(long j, IHWExtDevice iHWExtDevice);

        public DeviceEventQueue(HWExtDeviceEventListener listener) {
            this.DEVICE_MSG_TYPE_RESULT = 1001;
            this.mListener = null;
            this.mActiveDevices = new SparseBooleanArray();
            this.mDeviceEvents = new SparseArray();
            this.mDeviceValues = new float[500];
            this.mDeviceHandler = null;
            this.mListener = listener;
            Looper myLooper = Looper.myLooper();
            Looper looper = myLooper;
            if (myLooper != null) {
                this.mDeviceHandler = new HWExtDeviceHandler(looper);
            } else {
                myLooper = Looper.getMainLooper();
                looper = myLooper;
                if (myLooper != null) {
                    this.mDeviceHandler = new HWExtDeviceHandler(looper);
                } else {
                    this.mDeviceHandler = null;
                }
            }
            String str = HWExtDeviceManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("motion DeviceEventQueue mDeviceHandler: ");
            stringBuilder.append(this.mDeviceHandler);
            Log.d(str, stringBuilder.toString());
            this.nDevcieEventQueue = nativeCreateEventQueue(this, this.mDeviceValues);
        }

        public DeviceEventQueue(HWExtDeviceEventListener listener, Handler handler) {
            Looper looper;
            Looper looper2;
            String str;
            StringBuilder stringBuilder;
            this.DEVICE_MSG_TYPE_RESULT = 1001;
            this.mListener = null;
            this.mActiveDevices = new SparseBooleanArray();
            this.mDeviceEvents = new SparseArray();
            this.mDeviceValues = new float[500];
            this.mDeviceHandler = null;
            this.mListener = listener;
            if (handler != null) {
                looper = handler.getLooper();
                looper2 = looper;
                if (looper != null) {
                    this.mDeviceHandler = new HWExtDeviceHandler(looper2);
                    str = HWExtDeviceManager.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("motion DeviceEventQueue mDeviceHandler: ");
                    stringBuilder.append(this.mDeviceHandler);
                    Log.d(str, stringBuilder.toString());
                    this.nDevcieEventQueue = nativeCreateEventQueue(this, this.mDeviceValues);
                }
            }
            looper = Looper.myLooper();
            looper2 = looper;
            if (looper != null) {
                this.mDeviceHandler = new HWExtDeviceHandler(looper2);
            } else {
                looper = Looper.getMainLooper();
                looper2 = looper;
                if (looper != null) {
                    this.mDeviceHandler = new HWExtDeviceHandler(looper2);
                } else {
                    this.mDeviceHandler = null;
                }
            }
            str = HWExtDeviceManager.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("motion DeviceEventQueue mDeviceHandler: ");
            stringBuilder.append(this.mDeviceHandler);
            Log.d(str, stringBuilder.toString());
            this.nDevcieEventQueue = nativeCreateEventQueue(this, this.mDeviceValues);
        }

        public boolean addDevice(IHWExtDevice device) {
            int deviceUniqueId = HWExtDeviceManager.getDeviceUniqueId(device);
            if (this.mActiveDevices.get(deviceUniqueId)) {
                Log.w(HWExtDeviceManager.TAG, "addDevice device has been activated with this listener");
                return false;
            }
            this.mActiveDevices.put(deviceUniqueId, true);
            addDeviceEvent(device);
            return enableDevice(device);
        }

        private void addDeviceEvent(IHWExtDevice device) {
            if (this.mDeviceEvents != null) {
                this.mDeviceEvents.put(HWExtDeviceManager.getDeviceUniqueId(device), new HWExtDeviceEvent(device.getMaxLenValueArray()));
            }
        }

        private void removeDeviceEvent(IHWExtDevice device) {
            if (this.mDeviceEvents != null) {
                this.mDeviceEvents.remove(HWExtDeviceManager.getDeviceUniqueId(device));
            }
        }

        public boolean removeDevice(IHWExtDevice device) {
            int deviceUniqueId = HWExtDeviceManager.getDeviceUniqueId(device);
            if (!this.mActiveDevices.get(deviceUniqueId)) {
                return false;
            }
            boolean res = disableDevice(device);
            this.mActiveDevices.put(deviceUniqueId, false);
            removeDeviceEvent(device);
            return res;
        }

        public boolean hasDevices() {
            if (this.mActiveDevices.indexOfValue(true) >= 0) {
                return true;
            }
            return false;
        }

        public boolean removeAllDevices() {
            int deviceSize = this.mActiveDevices.size();
            int deviceUniqueId = -1;
            for (int i = 0; i < deviceSize; i++) {
                if (this.mActiveDevices.valueAt(i)) {
                    deviceUniqueId = this.mActiveDevices.keyAt(i);
                    IHWExtDevice device = (IHWExtDevice) HWExtDeviceManager.sHandleToDeviceList.get(deviceUniqueId);
                    disableDevice(device);
                    this.mActiveDevices.put(deviceUniqueId, false);
                    removeDeviceEvent(device);
                }
            }
            return true;
        }

        public void dispose() {
            nativeDestroyEventQueue(this.nDevcieEventQueue);
            this.mActiveDevices.clear();
        }

        protected void dispatchDeviceEvent(HWExtDeviceEvent deviceEvent) {
            if (deviceEvent == null) {
                Log.e(HWExtDeviceManager.TAG, "Error: deviceEvent is nullt");
                return;
            }
            if (deviceEvent.getDeviceType() == 1) {
                int deviceUniqueId = HWExtDeviceManager.getDeviceUniqueId(deviceEvent.getDeviceType(), deviceEvent.getSubDeviceType());
                HWExtDeviceEvent tmpDeviceEvent = (HWExtDeviceEvent) this.mDeviceEvents.get(deviceUniqueId);
                tmpDeviceEvent.setDevice((IHWExtDevice) HWExtDeviceManager.sHandleToDeviceList.get(deviceUniqueId));
                tmpDeviceEvent.setDeviceType(deviceEvent.getDeviceType());
                tmpDeviceEvent.setSubDeviceType(deviceEvent.getSubDeviceType());
                tmpDeviceEvent.setDeviceValues(deviceEvent.getDeviceValues(), deviceEvent.getDeviceValuesLen());
                if (this.mDeviceHandler != null) {
                    this.mDeviceHandler.sendMessage(this.mDeviceHandler.obtainMessage(this.DEVICE_MSG_TYPE_RESULT, tmpDeviceEvent.clone()));
                    if (tmpDeviceEvent.getSubDeviceType() == 700) {
                        synchronized (HWExtDeviceManager.mOritationLock) {
                            HWExtDeviceManager.mLastOritation = tmpDeviceEvent;
                        }
                    }
                } else {
                    Log.e(HWExtDeviceManager.TAG, "motion dispatchDeviceEvent mDeviceHandler is null ...... ");
                }
            }
        }

        protected void dispatchLastOritationEvent() {
            if (this.mDeviceHandler != null) {
                Log.d(HWExtDeviceManager.TAG, "dispatchLastOritationEvent ...... ");
                synchronized (HWExtDeviceManager.mOritationLock) {
                    if (HWExtDeviceManager.mLastOritation != null) {
                        this.mDeviceHandler.sendMessage(this.mDeviceHandler.obtainMessage(this.DEVICE_MSG_TYPE_RESULT, HWExtDeviceManager.mLastOritation.clone()));
                    } else {
                        Log.e(HWExtDeviceManager.TAG, "mdispatchLastOritationEvent mLastOritation is null ...... ");
                    }
                }
                return;
            }
            Log.e(HWExtDeviceManager.TAG, "dispatchLastOritationEvent mDeviceHandler is null ...... ");
        }

        private boolean enableDevice(IHWExtDevice device) {
            int enableDeviceResult = nativeEnableDevice(this.nDevcieEventQueue, device);
            if (enableDeviceResult == 1) {
                return true;
            }
            if (enableDeviceResult != 2 || device.getHWExtDeviceSubType() != 700) {
                return false;
            }
            dispatchLastOritationEvent();
            return true;
        }

        private boolean disableDevice(IHWExtDevice device) {
            if (nativeDisableDevice(this.nDevcieEventQueue, device) == 1) {
                return true;
            }
            return false;
        }
    }

    private native void nativeDispose();

    private native int nativeGetDeviceList();

    private native void nativeInit(boolean z);

    private HWExtDeviceManager(Context context) {
        this.mContext = context;
        synchronized (sDeviceModuleLock) {
            nativeInit(false);
            getFullHWExtDeviceList();
        }
    }

    public static synchronized HWExtDeviceManager getInstance(Context context) {
        HWExtDeviceManager hWExtDeviceManager;
        synchronized (HWExtDeviceManager.class) {
            if (mInstance == null) {
                mInstance = new HWExtDeviceManager(context);
            }
            hWExtDeviceManager = mInstance;
        }
        return hWExtDeviceManager;
    }

    public void dispose() throws Exception {
        synchronized (this.mDeviceListenerMap) {
            int listenerSize = this.mDeviceListenerMap.size();
            if (listenerSize <= 0) {
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mDeviceListenerMap is not empty, please ungrister all devices first listenerSize: ");
                stringBuilder.append(listenerSize);
                throw new Exception(stringBuilder.toString());
            }
        }
        nativeDispose();
        destroyInstance();
        synchronized (this.mFullDevicesList) {
            this.mFullDevicesList.clear();
        }
        synchronized (this.mHWExtDeviceTypeList) {
            this.mHWExtDeviceTypeList.clear();
        }
        this.sFullDeviceGetOver = false;
    }

    private static synchronized void destroyInstance() {
        synchronized (HWExtDeviceManager.class) {
            mInstance = null;
        }
    }

    public boolean registerDeviceListener(HWExtDeviceEventListener listener, IHWExtDevice device) {
        if (device != null) {
            boolean result;
            synchronized (this.mDeviceListenerMap) {
                DeviceEventQueue queue = (DeviceEventQueue) this.mDeviceListenerMap.get(listener);
                if (queue == null) {
                    queue = new DeviceEventQueue(listener);
                    this.mDeviceListenerMap.put(listener, queue);
                }
                result = queue.addDevice(device);
                if (result) {
                    dubaiReportMotionState(device, 1);
                }
            }
            return result;
        }
        throw new IllegalArgumentException("device cannot be null");
    }

    public boolean registerDeviceListener(HWExtDeviceEventListener listener, IHWExtDevice device, Handler handler) {
        if (device != null) {
            boolean result;
            synchronized (this.mDeviceListenerMap) {
                DeviceEventQueue queue = (DeviceEventQueue) this.mDeviceListenerMap.get(listener);
                if (queue == null) {
                    queue = new DeviceEventQueue(listener, handler);
                    this.mDeviceListenerMap.put(listener, queue);
                }
                result = queue.addDevice(device);
                if (result) {
                    dubaiReportMotionState(device, 1);
                }
            }
            return result;
        }
        throw new IllegalArgumentException("device cannot be null");
    }

    public boolean unregisterDeviceListener(HWExtDeviceEventListener listener, IHWExtDevice device) {
        synchronized (this.mDeviceListenerMap) {
            DeviceEventQueue queue = (DeviceEventQueue) this.mDeviceListenerMap.get(listener);
            if (queue != null) {
                if (device == null) {
                    queue.removeAllDevices();
                } else {
                    queue.removeDevice(device);
                }
                if (!queue.hasDevices()) {
                    queue.dispose();
                    this.mDeviceListenerMap.remove(listener);
                }
                dubaiReportMotionState(device, 0);
                return true;
            }
            return false;
        }
    }

    private void dubaiReportMotionState(IHWExtDevice device, int state) {
        if (device != null && userType == 3) {
            int type = device.getHWExtDeviceSubType();
            String name = "N/A";
            if (this.mContext != null) {
                name = this.mContext.getPackageName();
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("name=");
            stringBuilder.append(name);
            stringBuilder.append(" type=");
            stringBuilder.append(type);
            stringBuilder.append(" state=");
            stringBuilder.append(state);
            HwLog.dubaie("DUBAI_TAG_MOTION_RECOGNITION", stringBuilder.toString());
        }
    }

    public ArrayList<Integer> getHWExtDeviceTypes() {
        ArrayList arrayList;
        synchronized (this.mHWExtDeviceTypeList) {
            arrayList = this.mHWExtDeviceTypeList;
        }
        return arrayList;
    }

    public List<IHWExtDevice> getFullHWExtDeviceList() {
        ArrayList arrayList;
        if (!this.sFullDeviceGetOver && nativeGetDeviceList() > 0) {
            this.sFullDeviceGetOver = true;
        }
        synchronized (this.mFullDevicesList) {
            arrayList = this.mFullDevicesList;
        }
        return arrayList;
    }

    public List<IHWExtDevice> getHWExtSubDeviceList(int hwextDeviceType) {
        ArrayList<IHWExtDevice> devcieList = new ArrayList();
        if (this.sFullDeviceGetOver) {
            synchronized (this.mFullDevicesList) {
                int deviceListSize = this.mFullDevicesList.size();
                for (int i = 0; i < deviceListSize; i++) {
                    IHWExtDevice device = (IHWExtDevice) this.mFullDevicesList.get(i);
                    if (hwextDeviceType == device.getHWExtDeviceType()) {
                        devcieList.add(device);
                    }
                }
            }
        } else {
            getFullHWExtDeviceList();
        }
        return devcieList;
    }

    private void setDeviceList(IHWExtDevice[] deviceArr) {
        if (deviceArr == null) {
            Log.d(TAG, "motion setDeviceList deviceArr is null ");
            return;
        }
        for (IHWExtDevice device : deviceArr) {
            if (device != null) {
                synchronized (this.mFullDevicesList) {
                    this.mFullDevicesList.add(device);
                    sHandleToDeviceList.put(getDeviceUniqueId(device), device);
                }
                int hwextDeviceType = device.getHWExtDeviceType();
                Integer deviceType = Integer.valueOf(hwextDeviceType);
                synchronized (this.mHWExtDeviceTypeList) {
                    if (!this.mHWExtDeviceTypeList.contains(deviceType)) {
                        this.mHWExtDeviceTypeList.add(deviceType);
                    }
                }
                int i = hwextDeviceType;
            }
        }
    }

    private IHWExtDevice getDeviceByType(int hwextDeviceType) {
        if (hwextDeviceType != 1) {
            return null;
        }
        return new HWExtMotion(1);
    }

    private static int getDeviceUniqueId(IHWExtDevice device) {
        return (device.getHWExtDeviceSubType() * 100) + device.getHWExtDeviceType();
    }

    private static int getDeviceUniqueId(int deviceType, int deviceSubType) {
        return (deviceSubType * 100) + deviceType;
    }
}
