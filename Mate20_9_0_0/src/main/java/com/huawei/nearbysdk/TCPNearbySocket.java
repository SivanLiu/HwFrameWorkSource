package com.huawei.nearbysdk;

import android.os.RemoteException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;

public class TCPNearbySocket implements NearbySocket {
    private static final String TAG = "TCPNS";
    InternalNearbySocket iNearbySocket = null;
    boolean mAvailability = false;
    int mBusinessId = -1;
    int mBusinessType = -1;
    int mChannel = -1;
    NearbyDevice mRemoteNearbyDevice = null;
    int mSecurityType = 0;
    Socket mSocket = null;
    String mTag = "";

    public TCPNearbySocket(InternalNearbySocket internalNearbySocket) {
        HwLog.d(TAG, "TCPNearbySocket construct");
        try {
            this.mBusinessId = internalNearbySocket.getBusinessId();
            this.mBusinessType = internalNearbySocket.getBusinessType();
            this.mChannel = internalNearbySocket.getChannelId();
            this.mTag = internalNearbySocket.getTag();
            this.iNearbySocket = internalNearbySocket;
            this.mRemoteNearbyDevice = internalNearbySocket.getRemoteNearbyDevice();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("TCPNearbySocket fail: ");
            stringBuilder.append(e.toString());
            HwLog.e(str, stringBuilder.toString());
        }
    }

    public TCPNearbySocket(NearbyDevice nearbyDevice, int businessId, int businessType, int channel, String tag, InternalNearbySocket internalNearbySocket) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("TCPNearbySocket construct businessId = ");
        stringBuilder.append(businessId);
        stringBuilder.append(";businessType = ");
        stringBuilder.append(businessType);
        stringBuilder.append(";channel = ");
        stringBuilder.append(channel);
        stringBuilder.append(";tag = ");
        stringBuilder.append(tag);
        HwLog.d(str, stringBuilder.toString());
        this.mRemoteNearbyDevice = nearbyDevice;
        this.mBusinessId = businessId;
        this.mBusinessType = businessType;
        this.mChannel = channel;
        this.mTag = tag;
        this.iNearbySocket = internalNearbySocket;
    }

    public void setSocket(Socket socket) {
        HwLog.d(TAG, "setSocket");
        this.mSocket = socket;
        if (socket != null) {
            this.mAvailability = true;
        }
    }

    public void setSecurityType(int securityType) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setSecurityType = ");
        stringBuilder.append(securityType);
        HwLog.d(str, stringBuilder.toString());
        this.mSecurityType = securityType;
    }

    public int getSecurityType() {
        return this.mSecurityType;
    }

    public boolean isValidity() {
        return this.mAvailability;
    }

    public boolean close() {
        String str;
        StringBuilder stringBuilder;
        HwLog.d(TAG, "close.");
        if (this.mSocket != null) {
            try {
                this.mSocket.close();
            } catch (IOException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("mSocket close fail: ");
                stringBuilder.append(e);
                HwLog.e(str, stringBuilder.toString());
            }
        }
        if (this.iNearbySocket != null) {
            HwLog.d(TAG, "internalNearbySocket.close()");
            try {
                this.iNearbySocket.close();
            } catch (RemoteException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("iNearbySocket close fail: ");
                stringBuilder.append(e2);
                HwLog.e(str, stringBuilder.toString());
            }
        } else {
            HwLog.d(TAG, "iNearbySocket is null!!!!");
        }
        return true;
    }

    public InputStream getInputStream() {
        HwLog.d(TAG, "getInputStream");
        try {
            if (this.mSocket != null && this.mAvailability) {
                return this.mSocket.getInputStream();
            }
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getInputStream fail: ");
            stringBuilder.append(e);
            HwLog.e(str, stringBuilder.toString());
        }
        return null;
    }

    public OutputStream getOutputStream() {
        HwLog.d(TAG, "getOutputStream");
        try {
            if (this.mSocket != null && this.mAvailability) {
                return this.mSocket.getOutputStream();
            }
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getOutputStream fail: ");
            stringBuilder.append(e);
            HwLog.e(str, stringBuilder.toString());
        }
        return null;
    }

    public void shutdownInput() {
        HwLog.d(TAG, "shutdownInput");
        try {
            if (this.mSocket != null) {
                this.mSocket.shutdownInput();
            }
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("shutdownInput fail: ");
            stringBuilder.append(e);
            HwLog.e(str, stringBuilder.toString());
        }
    }

    public void shutdownOutput() {
        HwLog.d(TAG, "shutdownOutput");
        try {
            if (this.mSocket != null) {
                this.mSocket.shutdownOutput();
            }
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("shutdownOutput fail: ");
            stringBuilder.append(e);
            HwLog.e(str, stringBuilder.toString());
        }
    }

    public int getBusinessId() {
        return this.mBusinessId;
    }

    public int getBusinessType() {
        return this.mBusinessType;
    }

    public int getChannel() {
        return this.mChannel;
    }

    public String getTag() {
        return this.mTag;
    }

    public NearbyDevice getRemoteNearbyDevice() {
        return this.mRemoteNearbyDevice;
    }

    public SocketAddress getLocalSocketIpAddress() {
        if (this.mSocket != null) {
            return this.mSocket.getLocalSocketAddress();
        }
        return null;
    }

    public SocketAddress getRemoteSocketIpAddress() {
        if (this.mSocket != null) {
            return this.mSocket.getRemoteSocketAddress();
        }
        return null;
    }

    public int getPort() {
        return this.mSocket.getPort();
    }

    public String getServiceUuid() {
        return "";
    }

    public boolean registerSocketStatus(SocketStatusListener statuslistener) {
        return true;
    }
}
