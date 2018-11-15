package com.huawei.nearbysdk;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.RemoteException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public class BluetoothSocketTransport {
    private static final String TAG = "BluetoothSocketTransport";
    private static final boolean isSecure = false;
    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    private int getPort(BluetoothServerSocket serverSocket) {
        try {
            Field field_mSocket = serverSocket.getClass().getDeclaredField("mSocket");
            field_mSocket.setAccessible(true);
            BluetoothSocket socket = (BluetoothSocket) field_mSocket.get(serverSocket);
            Method getPortMethod = socket.getClass().getDeclaredMethod("getPort", new Class[0]);
            getPortMethod.setAccessible(true);
            return ((Integer) getPortMethod.invoke(socket, new Object[0])).intValue();
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(serverSocket);
            stringBuilder.append(" getPort fail ");
            HwLog.e(str, stringBuilder.toString(), e);
            return -1;
        }
    }

    NearbySocket createNearbySocketServer(ChannelCreateRequestImpl requestImpl) {
        String str;
        NearbyDevice nearbyDevice = requestImpl.getRemoteNearbyDevice();
        int businessId = requestImpl.getBusinessId();
        int businessType = requestImpl.getBusinessType().toNumber();
        int channelId = requestImpl.getChannelId();
        String tag = requestImpl.getTag();
        int port = requestImpl.getPort();
        String serviceUuid = requestImpl.getServiceUuid();
        try {
            Class cls = this.mAdapter.getClass();
            BluetoothServerSocket portServerSocket = this.mAdapter.listenUsingInsecureRfcommWithServiceRecord("listenUsingInsecureRfcommWithServiceRecord", UUID.fromString(serviceUuid));
            int port2 = getPort(portServerSocket);
            requestImpl.accept(port2);
            try {
                BluetoothSocket portSocket = portServerSocket.accept();
                HwLog.d(TAG, "Socket port get");
                BluetoothSocket portSocket2 = port2;
                BluetoothNearbySocket bluetoothNearbySocket = new BluetoothNearbySocket(portSocket, nearbyDevice, businessId, businessType, channelId, tag, port2, serviceUuid);
                bluetoothNearbySocket.setSecurityType(requestImpl.getSecurityType());
                return bluetoothNearbySocket;
            } catch (IOException e) {
                str = serviceUuid;
                IOException iOException = e;
                HwLog.e(TAG, String.format("[Connected]Socket Type: accept(%d) failed", new Object[]{Integer.valueOf(port)}), e);
                return null;
            }
        } catch (Exception e2) {
            str = serviceUuid;
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(null);
            stringBuilder.append(" listenRfcomm fail ");
            HwLog.e(str2, stringBuilder.toString(), e2);
            return null;
        }
    }

    void createNearbySocketClient(InternalNearbySocket innerSocket, ICreateSocketCallback cb) {
        String mServiceUuid;
        HwLog.d(TAG, "createNearbySocketClient innerSocket");
        try {
            NearbyDevice mNearbyDevice = innerSocket.getRemoteNearbyDevice();
            mServiceUuid = innerSocket.getServiceUuid();
            int mPort = innerSocket.getPort();
            BluetoothDevice mBluetoothDevice = this.mAdapter.getRemoteDevice(mNearbyDevice.getBluetoothMac());
            try {
                final BluetoothSocket mPortSocket = (BluetoothSocket) mBluetoothDevice.getClass().getMethod("createInsecureRfcommSocket", new Class[]{Integer.TYPE}).invoke(mBluetoothDevice, new Object[]{Integer.valueOf(mPort)});
                final ICreateSocketCallback iCreateSocketCallback = cb;
                final int i = mPort;
                final InternalNearbySocket internalNearbySocket = innerSocket;
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            mPortSocket.connect();
                            iCreateSocketCallback.onStatusChange(0, new BluetoothNearbySocket(mPortSocket, internalNearbySocket), i);
                            HwLog.d(BluetoothSocketTransport.TAG, "createNearbySocketClient: success mPortSocket");
                        } catch (IOException e) {
                            String str = BluetoothSocketTransport.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("socket connect fail: ");
                            stringBuilder.append(e);
                            HwLog.e(str, stringBuilder.toString());
                            try {
                                mPortSocket.close();
                            } catch (IOException e2) {
                                HwLog.e(BluetoothSocketTransport.TAG, "unable to close() socket during connection failure", e2);
                            }
                            iCreateSocketCallback.onStatusChange(1, null, i);
                        }
                    }
                }).start();
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("createNearbySocketClient fail: ");
                stringBuilder.append(e);
                HwLog.e(str, stringBuilder.toString());
                cb.onStatusChange(1, null, mPort);
            }
        } catch (RemoteException e2) {
            mServiceUuid = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("createNearbySocketClient RemoteException ");
            stringBuilder2.append(e2);
            HwLog.e(mServiceUuid, stringBuilder2.toString());
            cb.onStatusChange(1, null, -1);
        }
    }
}
