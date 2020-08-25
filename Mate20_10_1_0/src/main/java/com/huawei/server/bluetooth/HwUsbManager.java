package com.huawei.server.bluetooth;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.android.server.HwLog;
import com.android.server.mtm.iaware.brjob.AwareJobSchedulerConstants;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

public final class HwUsbManager {
    private static final String DEFAULT_ADDR = "00:00:00:00:00:00";
    private static final String DEFAULT_MODEL_ID = "000000";
    private static final String DEFAULT_NEARBY_VERSION = "01";
    private static final String DEFAULT_SUB_MODEL_ID = "00";
    private static final int ERROR = -1;
    private static final byte[] GET_PENCIL_INFO_COMMAND = {-88, 87};
    private static final int INTERFACE_CLASS_DATA = 10;
    private static final int MESSAGE_USB_STATE_CHANGED = 0;
    private static final int PENCIL_PRODUCT_ID = 4241;
    private static final int PENCIL_VENDOR_ID = 4817;
    private static final int READ_DATA_TIMEOUT = 4500;
    private static final String TAG = "BT-HwUsbManager";
    private static final int WRITE_DATA_TIMEOUT = 3000;
    private String mAddr = DEFAULT_ADDR;
    private HwBluetoothPencilManager mBluetoothPencilManager;
    private Context mContext;
    private UsbDeviceConnection mDeviceConnection;
    private UsbEndpoint mEndpointIn;
    private UsbEndpoint mEndpointOut;
    private Handler mHandler;
    private String mModelId = DEFAULT_MODEL_ID;
    private String mNearbyVersion = DEFAULT_NEARBY_VERSION;
    private String mSubModelId = DEFAULT_SUB_MODEL_ID;
    private UsbInterface mUsbInterface;
    private UsbManager mUsbManager;

    HwUsbManager(Context context, HwBluetoothPencilManager bluetoothPencilManager) {
        HwLog.i(TAG, "construction");
        this.mContext = context;
        HandlerThread mUsbManagerThread = new HandlerThread(TAG);
        mUsbManagerThread.start();
        this.mHandler = new UsbHandler(mUsbManagerThread.getLooper());
        this.mBluetoothPencilManager = bluetoothPencilManager;
    }

    /* access modifiers changed from: package-private */
    public void getUsbStateFromBroadcast(Intent intent) {
        Handler handler = this.mHandler;
        if (handler == null) {
            HwLog.e(TAG, "getUsbStateFromBroadcast, handler is null");
        } else {
            handler.obtainMessage(0, intent).sendToTarget();
        }
    }

    private class UsbHandler extends Handler {
        UsbHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            HwLog.i(HwUsbManager.TAG, "handleMessage: " + msg.what);
            if (msg.what != 0) {
                HwLog.e(HwUsbManager.TAG, "UsbHandler: unknow message:" + msg.what);
                return;
            }
            Intent intent = (Intent) msg.obj;
            Object usbDevice = intent.getParcelableExtra("device");
            if (usbDevice instanceof UsbDevice) {
                HwUsbManager.this.onUsbStateChanged((UsbDevice) usbDevice, intent.getAction());
            }
        }
    }

    private void getDeviceConnection(UsbDevice usbDevice) {
        if (this.mUsbManager == null) {
            HwLog.e(TAG, "getDeviceConnection: manager is null");
            return;
        }
        UsbDeviceConnection usbDeviceConnection = this.mDeviceConnection;
        if (usbDeviceConnection != null) {
            usbDeviceConnection.close();
        }
        this.mDeviceConnection = this.mUsbManager.openDevice(usbDevice);
    }

    private void getUsbInterface(UsbDevice usbDevice) {
        for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
            HwLog.i(TAG, "InterfaceClass: " + usbDevice.getInterface(i).toString() + " " + usbDevice.getInterface(i).getEndpointCount());
            if (usbDevice.getInterface(i).getInterfaceClass() == 10) {
                this.mUsbInterface = usbDevice.getInterface(i);
            }
        }
    }

    private void getEndpoint() {
        int endpointCount = this.mUsbInterface.getEndpointCount();
        this.mEndpointOut = null;
        this.mEndpointIn = null;
        HwLog.i(TAG, "getEndpoint: endpointCount " + endpointCount);
        for (int i = 0; i < endpointCount; i++) {
            UsbEndpoint endpoint = this.mUsbInterface.getEndpoint(i);
            if (endpoint == null) {
                HwLog.w(TAG, "getEndpoint: endpoint is null i = " + i);
            } else {
                HwLog.i(TAG, "getEndpoint: endpoint: " + endpoint.toString() + ", type: " + endpoint.getType());
                if (endpoint.getType() == 2) {
                    if (endpoint.getDirection() == 0) {
                        this.mEndpointOut = endpoint;
                    } else if (endpoint.getDirection() == 128) {
                        this.mEndpointIn = endpoint;
                    } else {
                        HwLog.w(TAG, "getEndpoint: invalid type");
                    }
                }
            }
        }
    }

    private void releaseConnection() {
        UsbInterface usbInterface;
        UsbDeviceConnection usbDeviceConnection = this.mDeviceConnection;
        if (usbDeviceConnection != null && (usbInterface = this.mUsbInterface) != null) {
            usbDeviceConnection.releaseInterface(usbInterface);
            this.mDeviceConnection.close();
            this.mDeviceConnection = null;
        }
    }

    private boolean initEndpoint(UsbDevice usbDevice) {
        HwLog.i(TAG, "init Endpoint");
        getDeviceConnection(usbDevice);
        if (this.mDeviceConnection == null) {
            HwLog.e(TAG, "initEndpoint: mDeviceConnection is null");
            return false;
        }
        getUsbInterface(usbDevice);
        UsbInterface usbInterface = this.mUsbInterface;
        if (usbInterface == null) {
            HwLog.w(TAG, "initEndpoint: interface is null");
            this.mDeviceConnection.close();
            this.mDeviceConnection = null;
            return false;
        } else if (!this.mDeviceConnection.claimInterface(usbInterface, true)) {
            HwLog.w(TAG, "initEndpoint: claimInterface failed");
            releaseConnection();
            return false;
        } else if (!this.mDeviceConnection.setInterface(this.mUsbInterface)) {
            HwLog.w(TAG, "initEndpoint: setInterface failed");
            releaseConnection();
            return false;
        } else {
            getEndpoint();
            if (this.mEndpointIn != null && this.mEndpointOut != null) {
                return true;
            }
            HwLog.i(TAG, "init failed");
            releaseConnection();
            return false;
        }
    }

    private int sendCommand() {
        UsbDeviceConnection usbDeviceConnection = this.mDeviceConnection;
        if (usbDeviceConnection == null) {
            HwLog.e(TAG, "device connection is null");
            return -1;
        }
        UsbEndpoint usbEndpoint = this.mEndpointOut;
        if (usbEndpoint == null) {
            HwLog.e(TAG, "device endpointOut is null");
            return -1;
        }
        byte[] bArr = GET_PENCIL_INFO_COMMAND;
        int out = usbDeviceConnection.bulkTransfer(usbEndpoint, bArr, bArr.length, 3000);
        HwLog.i(TAG, "sendCommand: send data out " + out);
        return out;
    }

    private void getInfoFromUsb() {
        HwLog.i(TAG, "getInfoFromUsb");
        ByteBuffer byteBuffer = ByteBuffer.allocate(this.mEndpointIn.getMaxPacketSize());
        UsbRequest usbRequest = new UsbRequest();
        usbRequest.initialize(this.mDeviceConnection, this.mEndpointIn);
        usbRequest.queue(byteBuffer, this.mEndpointIn.getMaxPacketSize());
        try {
            if (this.mDeviceConnection.requestWait(4500) == usbRequest) {
                byte[] retData = byteBuffer.array();
                HwLog.i(TAG, "getInfoFromUsb: " + HwUtils.bytesToHexString(retData));
                parseUsbData(retData);
            }
        } catch (BufferOverflowException | TimeoutException e) {
            HwLog.e(TAG, "getInfoFromUsb: exception occured");
        } catch (Throwable th) {
            releaseConnection();
            usbRequest.close();
            throw th;
        }
        releaseConnection();
        usbRequest.close();
    }

    private void sendPencilAttachEvent(String event) {
        HwLog.i(TAG, "sendPencilAttachEvent: " + event);
        HwBluetoothPencilManager hwBluetoothPencilManager = this.mBluetoothPencilManager;
        if (hwBluetoothPencilManager == null) {
            HwLog.e(TAG, "sendPencilAttachEvent: BluetoothPencilManager is null");
        } else {
            hwBluetoothPencilManager.sendHwPencilBroadcast(event, this.mAddr, this.mNearbyVersion, this.mModelId, this.mSubModelId, "1");
        }
    }

    private void parseUsbData(byte[] data) {
        if (data == null || data.length < 15) {
            HwLog.e(TAG, "parseUsbData: arg error");
            return;
        }
        this.mAddr = HwUtils.bytesToHexAddrString(Arrays.copyOfRange(data, 2, 8));
        this.mModelId = HwUtils.bytesToHexString(Arrays.copyOfRange(data, 8, 11));
        this.mSubModelId = HwUtils.bytesToHexString(Arrays.copyOfRange(data, 11, 12));
        this.mNearbyVersion = HwUtils.bytesToHexString(Arrays.copyOfRange(data, 13, 14));
        sendPencilAttachEvent(AwareJobSchedulerConstants.SERVICES_STATUS_CONNECTED);
    }

    /* access modifiers changed from: private */
    public void onUsbStateChanged(UsbDevice usbDevice, String action) {
        HwLog.i(TAG, "onUsbStateChanged");
        if (usbDevice == null) {
            HwLog.e(TAG, "onUsbStateChanged: device is null");
            return;
        }
        if (this.mUsbManager == null) {
            this.mUsbManager = (UsbManager) this.mContext.getSystemService("usb");
        }
        if (usbDevice.getVendorId() != PENCIL_VENDOR_ID || usbDevice.getProductId() != PENCIL_PRODUCT_ID) {
            HwLog.i(TAG, "onUsbStateChanged: not pencil");
        } else if (!this.mUsbManager.hasPermission(usbDevice)) {
            HwLog.e(TAG, "onUsbStateChanged: not has permission");
        } else if (Objects.equals(action, "android.hardware.usb.action.USB_DEVICE_ATTACHED")) {
            sendPencilAttachEvent("PING_SUCC");
            if (!initEndpoint(usbDevice)) {
                HwLog.e(TAG, "onUsbStateChanged: init endpoint failed");
            } else if (sendCommand() != -1) {
                getInfoFromUsb();
            }
        } else if (Objects.equals(action, "android.hardware.usb.action.USB_DEVICE_DETACHED")) {
            sendPencilAttachEvent("DISCONNECTED");
            this.mDeviceConnection = null;
        } else {
            HwLog.w(TAG, "onUsbStateChanged: unknown action " + action);
        }
    }
}
