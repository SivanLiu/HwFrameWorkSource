package com.huawei.nearbysdk;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import com.huawei.nearbysdk.IInternalSocketListener.Stub;
import com.huawei.nearbysdk.NearbyConfig.BusinessTypeEnum;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class SocketListenerTransport extends Stub {
    private static final String MSG_KEY_IP = "IP";
    private static final String MSG_KEY_PASSPHARSE = "PASSPHARSE";
    private static final int SOCKET_TIMEOUT = 30000;
    static final String TAG = "SocketListenerTransport";
    private static final int TYPE_CONNECT_REQUEST = 2;
    private static final int TYPE_HWSHARE_I_CONNECT_REQUEST = 3;
    private static final int TYPE_STATUS_CHANGED = 1;
    private static final int TYPE_THREAD_RESPONSE_CLIENT = 4;
    private final BluetoothSocketTransport mBluetoothSocketTransport = new BluetoothSocketTransport();
    private SocketListener mListener;
    private final Handler mListenerHandler;
    private String mPasspharse = null;

    class ChannelCreateRequestImpl implements ChannelCreateRequest {
        IInternalChannelCreateRequest mInnerRequest;

        public ChannelCreateRequestImpl(IInternalChannelCreateRequest innerRequest) {
            this.mInnerRequest = innerRequest;
        }

        public int getBusinessId() {
            try {
                return this.mInnerRequest.getBusinessId();
            } catch (RemoteException e) {
                String str = SocketListenerTransport.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getBusinessId() fail: ");
                stringBuilder.append(e);
                HwLog.e(str, stringBuilder.toString());
                return -1;
            }
        }

        public int getSecurityType() {
            try {
                return this.mInnerRequest.getSecurityType();
            } catch (RemoteException e) {
                String str = SocketListenerTransport.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getSecurityType() fail: ");
                stringBuilder.append(e);
                HwLog.e(str, stringBuilder.toString());
                return 0;
            }
        }

        public String getTag() {
            String result = "";
            try {
                return this.mInnerRequest.getTag();
            } catch (RemoteException e) {
                String str = SocketListenerTransport.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getTag() fail: ");
                stringBuilder.append(e);
                HwLog.e(str, stringBuilder.toString());
                return result;
            }
        }

        public BusinessTypeEnum getBusinessType() {
            try {
                return NearbySDKUtils.getEnumFromInt(this.mInnerRequest.getBusinessType());
            } catch (RemoteException e) {
                String str = SocketListenerTransport.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getBusinessType fail: ");
                stringBuilder.append(e);
                HwLog.e(str, stringBuilder.toString());
                return null;
            }
        }

        public int getChannelId() {
            try {
                return this.mInnerRequest.getChannelId();
            } catch (RemoteException e) {
                String str = SocketListenerTransport.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getChannelId fail: ");
                stringBuilder.append(e);
                HwLog.e(str, stringBuilder.toString());
                return -1;
            }
        }

        public String getServiceUuid() {
            String result = "";
            try {
                return this.mInnerRequest.getServiceUuid();
            } catch (RemoteException e) {
                String str = SocketListenerTransport.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getServiceUuid fail: ");
                stringBuilder.append(e);
                HwLog.e(str, stringBuilder.toString());
                return result;
            }
        }

        public int getPort() {
            try {
                return this.mInnerRequest.getPort();
            } catch (RemoteException e) {
                String str = SocketListenerTransport.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getPort fail: ");
                stringBuilder.append(e);
                HwLog.e(str, stringBuilder.toString());
                return -1;
            }
        }

        public NearbyDevice getRemoteNearbyDevice() {
            String str;
            StringBuilder stringBuilder;
            NearbyDevice result = null;
            try {
                result = this.mInnerRequest.getRemoteNearbyDevice();
            } catch (RemoteException e) {
                str = SocketListenerTransport.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("getRemoteNearbyDevice fail: ");
                stringBuilder.append(e);
                HwLog.e(str, stringBuilder.toString());
            }
            String summary = "null";
            if (result != null) {
                summary = result.getSummary();
            }
            str = SocketListenerTransport.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getRemoteNearbyDevice result=");
            stringBuilder.append(result);
            stringBuilder.append(" summary[");
            stringBuilder.append(summary.length());
            stringBuilder.append("]=");
            stringBuilder.append(summary.hashCode());
            HwLog.i(str, stringBuilder.toString());
            return result;
        }

        public void accept(int port) {
            try {
                this.mInnerRequest.accept(port);
            } catch (RemoteException e) {
                HwLog.e(SocketListenerTransport.TAG, "accept fail: RemoteException");
            }
        }

        public NearbySocket accept() throws IOException {
            return acceptTimer(0);
        }

        public NearbySocket acceptTimer(int timeOut) throws IOException {
            int channelId = -1;
            int protocol = -1;
            String str = SocketListenerTransport.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("accept.. timeOut = ");
            stringBuilder.append(timeOut);
            HwLog.e(str, stringBuilder.toString());
            try {
                channelId = this.mInnerRequest.getChannelId();
                protocol = this.mInnerRequest.getProtocol();
            } catch (RemoteException e) {
                String str2 = SocketListenerTransport.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("acceptTimer fail: ");
                stringBuilder2.append(e);
                HwLog.e(str2, stringBuilder2.toString());
            }
            return createNearbySocketByChannelId(channelId, protocol, timeOut);
        }

        public void reject() {
            HwLog.d(SocketListenerTransport.TAG, "reject.. ");
            try {
                this.mInnerRequest.reject();
            } catch (RemoteException e) {
                HwLog.e(SocketListenerTransport.TAG, "reject fail: RemoteException");
            }
        }

        public boolean equals(NearbyDevice nearbyDevice) {
            return true;
        }

        private NearbySocket createNearbySocketByChannelId(int channelId, int protocol, int timeOut) throws IOException {
            String str = SocketListenerTransport.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("createNearbySocketByChannelId channelId = ");
            stringBuilder.append(channelId);
            stringBuilder.append(";protocol = ");
            stringBuilder.append(protocol);
            HwLog.d(str, stringBuilder.toString());
            StringBuilder stringBuilder2;
            NearbySocket result;
            switch (channelId) {
                case 1:
                    String str2;
                    StringBuilder stringBuilder3;
                    try {
                        String str3 = SocketListenerTransport.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("SecurityType: ");
                        stringBuilder2.append(this.mInnerRequest.getSecurityType());
                        HwLog.i(str3, stringBuilder2.toString());
                        NearbyDevice nearbyDevice = this.mInnerRequest.getRemoteNearbyDevice();
                        if (nearbyDevice != null) {
                            str2 = SocketListenerTransport.TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("BluetoothMac: ");
                            stringBuilder3.append(String.valueOf(nearbyDevice.getBluetoothMac()));
                            HwLog.s(str2, stringBuilder3.toString());
                        }
                    } catch (RemoteException e) {
                        str2 = SocketListenerTransport.TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("RemoteException ");
                        stringBuilder3.append(e);
                        HwLog.e(str2, stringBuilder3.toString());
                    }
                    result = createTCPNearbySocket(timeOut);
                    if (result != null) {
                        return result;
                    }
                    throw new SocketException("Socket is error ");
                case 2:
                    result = SocketListenerTransport.this.mBluetoothSocketTransport.createNearbySocketServer(this);
                    if (result != null) {
                        return result;
                    }
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Socket is error ");
                    stringBuilder2.append(getPort());
                    throw new SocketException(stringBuilder2.toString());
                case 3:
                    return createP2PNearbySocketFromAccept(protocol, timeOut);
                default:
                    return null;
            }
        }

        private NearbySocket createP2PNearbySocketFromAccept(int protocol, int timeOut) {
            String str = SocketListenerTransport.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("createP2PNearbySocketFromAccept protocol = ");
            stringBuilder.append(protocol);
            HwLog.d(str, stringBuilder.toString());
            switch (protocol) {
                case 1:
                    return createTCPNearbySocket(timeOut);
                case 2:
                    return createUDPNearbySocket(timeOut);
                default:
                    return null;
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:107:0x01b1 A:{SYNTHETIC, Splitter: B:107:0x01b1} */
        /* JADX WARNING: Removed duplicated region for block: B:96:0x0186 A:{SYNTHETIC, Splitter: B:96:0x0186} */
        /* JADX WARNING: Removed duplicated region for block: B:85:0x014c A:{SYNTHETIC, Splitter: B:85:0x014c} */
        /* JADX WARNING: Removed duplicated region for block: B:114:0x01d3 A:{SYNTHETIC, Splitter: B:114:0x01d3} */
        /* JADX WARNING: Removed duplicated region for block: B:107:0x01b1 A:{SYNTHETIC, Splitter: B:107:0x01b1} */
        /* JADX WARNING: Removed duplicated region for block: B:96:0x0186 A:{SYNTHETIC, Splitter: B:96:0x0186} */
        /* JADX WARNING: Removed duplicated region for block: B:85:0x014c A:{SYNTHETIC, Splitter: B:85:0x014c} */
        /* JADX WARNING: Removed duplicated region for block: B:114:0x01d3 A:{SYNTHETIC, Splitter: B:114:0x01d3} */
        /* JADX WARNING: Removed duplicated region for block: B:107:0x01b1 A:{SYNTHETIC, Splitter: B:107:0x01b1} */
        /* JADX WARNING: Removed duplicated region for block: B:96:0x0186 A:{SYNTHETIC, Splitter: B:96:0x0186} */
        /* JADX WARNING: Removed duplicated region for block: B:85:0x014c A:{SYNTHETIC, Splitter: B:85:0x014c} */
        /* JADX WARNING: Removed duplicated region for block: B:114:0x01d3 A:{SYNTHETIC, Splitter: B:114:0x01d3} */
        /* JADX WARNING: Removed duplicated region for block: B:107:0x01b1 A:{SYNTHETIC, Splitter: B:107:0x01b1} */
        /* JADX WARNING: Removed duplicated region for block: B:96:0x0186 A:{SYNTHETIC, Splitter: B:96:0x0186} */
        /* JADX WARNING: Removed duplicated region for block: B:85:0x014c A:{SYNTHETIC, Splitter: B:85:0x014c} */
        /* JADX WARNING: Removed duplicated region for block: B:114:0x01d3 A:{SYNTHETIC, Splitter: B:114:0x01d3} */
        /* JADX WARNING: Removed duplicated region for block: B:96:0x0186 A:{SYNTHETIC, Splitter: B:96:0x0186} */
        /* JADX WARNING: Removed duplicated region for block: B:85:0x014c A:{SYNTHETIC, Splitter: B:85:0x014c} */
        /* JADX WARNING: Removed duplicated region for block: B:114:0x01d3 A:{SYNTHETIC, Splitter: B:114:0x01d3} */
        /* JADX WARNING: Removed duplicated region for block: B:107:0x01b1 A:{SYNTHETIC, Splitter: B:107:0x01b1} */
        /* JADX WARNING: Removed duplicated region for block: B:96:0x0186 A:{SYNTHETIC, Splitter: B:96:0x0186} */
        /* JADX WARNING: Removed duplicated region for block: B:85:0x014c A:{SYNTHETIC, Splitter: B:85:0x014c} */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private NearbySocket createTCPNearbySocket(int timeOut) {
            NearbyDevice nearbyDevice;
            SocketTimeoutException e;
            RemoteException e2;
            RemoteException e3;
            String str;
            StringBuilder stringBuilder;
            IOException e4;
            IOException e5;
            Throwable th;
            int businessId;
            int businessType;
            int i;
            int i2;
            Object obj;
            Throwable th2;
            int i3;
            int i4;
            IOException iOException;
            String str2;
            StringBuilder stringBuilder2;
            int i5 = -1;
            ServerSocket serverSocket = null;
            int localPort = 0;
            Socket socket = null;
            String tag = "";
            HwLog.d(SocketListenerTransport.TAG, "createP2PNearbySocketFromAccept..");
            try {
                String tag2;
                serverSocket = new ServerSocket(0);
                try {
                    serverSocket.setSoTimeout(timeOut);
                    i5 = serverSocket.getLocalPort();
                    this.mInnerRequest.accept(i5);
                    nearbyDevice = this.mInnerRequest.getRemoteNearbyDevice();
                } catch (SocketTimeoutException e6) {
                    e = e6;
                    HwLog.e(SocketListenerTransport.TAG, "createTCPNearbySocket socket(1.0) connect timeout!");
                    if (serverSocket != null) {
                    }
                    return null;
                } catch (RemoteException e7) {
                    e2 = e7;
                    e3 = e2;
                    str = SocketListenerTransport.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("createTCPNearbySocket RemoteException ");
                    stringBuilder.append(e3);
                    HwLog.e(str, stringBuilder.toString());
                    if (serverSocket != null) {
                    }
                    return null;
                } catch (IOException e8) {
                    e4 = e8;
                    e5 = e4;
                    try {
                        str = SocketListenerTransport.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("createTCPNearbySocket IOException ");
                        stringBuilder.append(e5);
                        HwLog.e(str, stringBuilder.toString());
                        if (serverSocket != null) {
                        }
                        return null;
                    } catch (Throwable th3) {
                        th = th3;
                    }
                }
                try {
                    businessId = this.mInnerRequest.getBusinessId();
                    try {
                        businessType = this.mInnerRequest.getBusinessType();
                        try {
                            tag2 = this.mInnerRequest.getTag();
                        } catch (SocketTimeoutException e9) {
                            e = e9;
                            socket = nearbyDevice;
                            i = businessId;
                            i2 = businessType;
                            HwLog.e(SocketListenerTransport.TAG, "createTCPNearbySocket socket(1.0) connect timeout!");
                            if (serverSocket != null) {
                            }
                            return null;
                        } catch (RemoteException e10) {
                            e2 = e10;
                            socket = nearbyDevice;
                            i = businessId;
                            i2 = businessType;
                            e3 = e2;
                            str = SocketListenerTransport.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("createTCPNearbySocket RemoteException ");
                            stringBuilder.append(e3);
                            HwLog.e(str, stringBuilder.toString());
                            if (serverSocket != null) {
                            }
                            return null;
                        } catch (IOException e11) {
                            e4 = e11;
                            socket = nearbyDevice;
                            i = businessId;
                            i2 = businessType;
                            e5 = e4;
                            str = SocketListenerTransport.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("createTCPNearbySocket IOException ");
                            stringBuilder.append(e5);
                            HwLog.e(str, stringBuilder.toString());
                            if (serverSocket != null) {
                            }
                            return null;
                        } catch (Throwable th4) {
                            th = th4;
                            obj = 0;
                            i = businessId;
                            i2 = businessType;
                            localPort = i5;
                            th2 = th;
                            if (serverSocket != null) {
                            }
                            throw th2;
                        }
                    } catch (SocketTimeoutException e12) {
                        e = e12;
                        socket = nearbyDevice;
                        i = businessId;
                        HwLog.e(SocketListenerTransport.TAG, "createTCPNearbySocket socket(1.0) connect timeout!");
                        if (serverSocket != null) {
                        }
                        return null;
                    } catch (RemoteException e13) {
                        e2 = e13;
                        socket = nearbyDevice;
                        i = businessId;
                        e3 = e2;
                        str = SocketListenerTransport.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("createTCPNearbySocket RemoteException ");
                        stringBuilder.append(e3);
                        HwLog.e(str, stringBuilder.toString());
                        if (serverSocket != null) {
                        }
                        return null;
                    } catch (IOException e14) {
                        e4 = e14;
                        socket = nearbyDevice;
                        i = businessId;
                        e5 = e4;
                        str = SocketListenerTransport.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("createTCPNearbySocket IOException ");
                        stringBuilder.append(e5);
                        HwLog.e(str, stringBuilder.toString());
                        if (serverSocket != null) {
                        }
                        return null;
                    } catch (Throwable th5) {
                        th = th5;
                        obj = 0;
                        i = businessId;
                        localPort = i5;
                        th2 = th;
                        if (serverSocket != null) {
                        }
                        throw th2;
                    }
                } catch (SocketTimeoutException e15) {
                    e = e15;
                    socket = nearbyDevice;
                    HwLog.e(SocketListenerTransport.TAG, "createTCPNearbySocket socket(1.0) connect timeout!");
                    if (serverSocket != null) {
                    }
                    return null;
                } catch (RemoteException e16) {
                    e2 = e16;
                    socket = nearbyDevice;
                    e3 = e2;
                    str = SocketListenerTransport.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("createTCPNearbySocket RemoteException ");
                    stringBuilder.append(e3);
                    HwLog.e(str, stringBuilder.toString());
                    if (serverSocket != null) {
                    }
                    return null;
                } catch (IOException e17) {
                    e4 = e17;
                    socket = nearbyDevice;
                    e5 = e4;
                    str = SocketListenerTransport.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("createTCPNearbySocket IOException ");
                    stringBuilder.append(e5);
                    HwLog.e(str, stringBuilder.toString());
                    if (serverSocket != null) {
                    }
                    return null;
                } catch (Throwable th6) {
                    th = th6;
                    socket = localPort;
                    localPort = i5;
                    th2 = th;
                    if (serverSocket != null) {
                    }
                    throw th2;
                }
                try {
                    int channelId = this.mInnerRequest.getChannelId();
                    try {
                        HwLog.e(SocketListenerTransport.TAG, "accept before..");
                        localPort = serverSocket.accept();
                        HwLog.e(SocketListenerTransport.TAG, "accept after..");
                        try {
                            serverSocket.close();
                        } catch (IOException e42) {
                            IOException iOException2 = e42;
                            String str3 = SocketListenerTransport.TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("serverSocket close fail: ");
                            stringBuilder3.append(e42);
                            HwLog.e(str3, stringBuilder3.toString());
                        }
                        TCPNearbySocket result = null;
                        try {
                            result = new TCPNearbySocket(nearbyDevice, businessId, businessType, channelId, tag2, this.mInnerRequest.getInnerNearbySocket());
                            result.setSecurityType(this.mInnerRequest.getSecurityType());
                        } catch (RemoteException e22) {
                            String str4 = SocketListenerTransport.TAG;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("RemoteException: ");
                            stringBuilder4.append(e22);
                            HwLog.e(str4, stringBuilder4.toString());
                        }
                        if (result != null) {
                            result.setSocket(localPort);
                        }
                        return result;
                    } catch (SocketTimeoutException e18) {
                        e = e18;
                        socket = nearbyDevice;
                        i = businessId;
                        i2 = businessType;
                        i3 = channelId;
                    } catch (RemoteException e19) {
                        e22 = e19;
                        socket = nearbyDevice;
                        i = businessId;
                        i2 = businessType;
                        i3 = channelId;
                        e3 = e22;
                        str = SocketListenerTransport.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("createTCPNearbySocket RemoteException ");
                        stringBuilder.append(e3);
                        HwLog.e(str, stringBuilder.toString());
                        if (serverSocket != null) {
                        }
                        return null;
                    } catch (IOException e20) {
                        e42 = e20;
                        socket = nearbyDevice;
                        i = businessId;
                        i2 = businessType;
                        i3 = channelId;
                        e5 = e42;
                        str = SocketListenerTransport.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("createTCPNearbySocket IOException ");
                        stringBuilder.append(e5);
                        HwLog.e(str, stringBuilder.toString());
                        if (serverSocket != null) {
                        }
                        return null;
                    } catch (Throwable th7) {
                        th = th7;
                        obj = localPort;
                        i = businessId;
                        i2 = businessType;
                        i3 = channelId;
                        localPort = i5;
                        th2 = th;
                        if (serverSocket != null) {
                        }
                        throw th2;
                    }
                } catch (SocketTimeoutException e21) {
                    e = e21;
                    socket = nearbyDevice;
                    i = businessId;
                    i2 = businessType;
                    HwLog.e(SocketListenerTransport.TAG, "createTCPNearbySocket socket(1.0) connect timeout!");
                    if (serverSocket != null) {
                    }
                    return null;
                } catch (RemoteException e23) {
                    e22 = e23;
                    socket = nearbyDevice;
                    i = businessId;
                    i2 = businessType;
                    e3 = e22;
                    str = SocketListenerTransport.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("createTCPNearbySocket RemoteException ");
                    stringBuilder.append(e3);
                    HwLog.e(str, stringBuilder.toString());
                    if (serverSocket != null) {
                    }
                    return null;
                } catch (IOException e24) {
                    e42 = e24;
                    socket = nearbyDevice;
                    i = businessId;
                    i2 = businessType;
                    e5 = e42;
                    str = SocketListenerTransport.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("createTCPNearbySocket IOException ");
                    stringBuilder.append(e5);
                    HwLog.e(str, stringBuilder.toString());
                    if (serverSocket != null) {
                    }
                    return null;
                } catch (Throwable th8) {
                    th = th8;
                    socket = null;
                    i = businessId;
                    i2 = businessType;
                    localPort = i5;
                    th2 = th;
                    if (serverSocket != null) {
                    }
                    throw th2;
                }
            } catch (SocketTimeoutException e25) {
                e = e25;
                i4 = timeOut;
                HwLog.e(SocketListenerTransport.TAG, "createTCPNearbySocket socket(1.0) connect timeout!");
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e422) {
                        iOException = e422;
                        str2 = SocketListenerTransport.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("serverSocket close fail: ");
                        stringBuilder2.append(e422);
                        HwLog.e(str2, stringBuilder2.toString());
                    }
                }
                return null;
            } catch (RemoteException e26) {
                e22 = e26;
                i4 = timeOut;
                e3 = e22;
                str = SocketListenerTransport.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("createTCPNearbySocket RemoteException ");
                stringBuilder.append(e3);
                HwLog.e(str, stringBuilder.toString());
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e4222) {
                        iOException = e4222;
                        str2 = SocketListenerTransport.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("serverSocket close fail: ");
                        stringBuilder2.append(e4222);
                        HwLog.e(str2, stringBuilder2.toString());
                    }
                }
                return null;
            } catch (IOException e27) {
                e4222 = e27;
                i4 = timeOut;
                e5 = e4222;
                str = SocketListenerTransport.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("createTCPNearbySocket IOException ");
                stringBuilder.append(e5);
                HwLog.e(str, stringBuilder.toString());
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e42222) {
                        iOException = e42222;
                        str2 = SocketListenerTransport.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("serverSocket close fail: ");
                        stringBuilder2.append(e42222);
                        HwLog.e(str2, stringBuilder2.toString());
                    }
                }
                return null;
            } catch (Throwable th9) {
                th = th9;
                i4 = timeOut;
                Socket nearbyDevice2 = socket;
                socket = localPort;
                localPort = i5;
                th2 = th;
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e422222) {
                        IOException iOException3 = e422222;
                        StringBuilder stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("serverSocket close fail: ");
                        stringBuilder5.append(e422222);
                        HwLog.e(SocketListenerTransport.TAG, stringBuilder5.toString());
                    }
                }
                throw th2;
            }
        }

        private NearbySocket createUDPNearbySocket(int timeOut) {
            return null;
        }
    }

    public SocketListenerTransport(SocketListener listener, Looper looper) {
        this.mListener = listener;
        this.mListenerHandler = new Handler(looper) {
            public void handleMessage(Message msg) {
                SocketListenerTransport.this._handleMessage(msg);
            }
        };
    }

    private void _handleMessage(Message msg) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("_handleMessage: ");
        stringBuilder.append(msg.toString());
        HwLog.d(str, stringBuilder.toString());
        switch (msg.what) {
            case 1:
                this.mListener.onStatusChange(msg.arg1);
                return;
            case 2:
                HwLog.d(TAG, "TYPE_CONNECT_REQUEST Listener.onConnectRequest");
                this.mListener.onConnectRequest(new ChannelCreateRequestImpl(msg.obj));
                return;
            case 3:
                HwLog.d(TAG, "TYPE_HWSHARE_I_CONNECT_REQUEST createNearbySocketClient");
                InternalNearbySocket innerSocket = msg.obj;
                this.mPasspharse = msg.getData().getString(MSG_KEY_PASSPHARSE);
                createNearbySocketClient(innerSocket);
                return;
            case 4:
                HwLog.d(TAG, "TYPE_THREAD_RESPONSE_CLIENT");
                NearbySocket nearbySocket = msg.obj;
                if (this.mListener instanceof SocketBackwardCompatible) {
                    SocketBackwardCompatible listener = this.mListener;
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Listener.onOldVerConnect mPasspharse = ");
                    stringBuilder2.append(this.mPasspharse);
                    HwLog.d(str2, stringBuilder2.toString());
                    listener.onOldVerConnect(nearbySocket, this.mPasspharse);
                    return;
                }
                return;
            default:
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unknow message id:");
                stringBuilder.append(msg.what);
                stringBuilder.append(", can not be here!");
                HwLog.e(str, stringBuilder.toString());
                return;
        }
    }

    public void onStatusChange(int state) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onStatusChange state = ");
        stringBuilder.append(state);
        HwLog.d(str, stringBuilder.toString());
        Message msg = Message.obtain();
        msg.what = 1;
        msg.arg1 = state;
        if (!this.mListenerHandler.sendMessage(msg)) {
            HwLog.e(TAG, "onStatusChange: handler quitting,remove the listener. ");
        }
    }

    public void onConnectRequest(IInternalChannelCreateRequest request) {
        HwLog.d(TAG, "onConnectRequest");
        Message msg = Message.obtain();
        msg.what = 2;
        msg.obj = request;
        HwLog.e(TAG, "onConnectRequest: come in.");
        if (!this.mListenerHandler.sendMessage(msg)) {
            HwLog.e(TAG, "onConnectRequest: handler quitting,remove the listener. ");
        }
    }

    public void onHwShareIConnectRequest(InternalNearbySocket socket, String passpharse) {
        HwLog.d(TAG, "onHwShareIConnectRequest");
        Message msg = Message.obtain();
        msg.what = 3;
        msg.obj = socket;
        Bundle bundle = new Bundle();
        bundle.putString(MSG_KEY_PASSPHARSE, passpharse);
        msg.setData(bundle);
        HwLog.e(TAG, "onHwShareConnectRequest: come in.");
        if (!this.mListenerHandler.sendMessage(msg)) {
            HwLog.e(TAG, "onConnectRequest: handler quitting,remove the listener. ");
        }
    }

    private void createNearbySocketClient(final InternalNearbySocket innerSocket) {
        HwLog.d(TAG, "createNearbySocketClient");
        new Thread(new Runnable() {
            public void run() {
                String str;
                StringBuilder stringBuilder;
                Socket socket = new Socket();
                InternalNearbySocket iSocket = innerSocket;
                int protocol = -1;
                try {
                    socket.bind(new InetSocketAddress(iSocket.getLocalIpAddress(), 0));
                    socket.connect(new InetSocketAddress(iSocket.getIpAddress(), iSocket.getPort()), SocketListenerTransport.SOCKET_TIMEOUT);
                    protocol = innerSocket.getProtocol();
                } catch (IOException e) {
                    str = SocketListenerTransport.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("socket bind or connect error: ");
                    stringBuilder.append(e);
                    HwLog.e(str, stringBuilder.toString());
                } catch (RemoteException e2) {
                    str = SocketListenerTransport.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("socket bind or connect error: ");
                    stringBuilder.append(e2);
                    HwLog.e(str, stringBuilder.toString());
                }
                try {
                    socket.setKeepAlive(true);
                } catch (SocketException e3) {
                    str = SocketListenerTransport.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("socket setKeepAlive error: ");
                    stringBuilder.append(e3);
                    HwLog.e(str, stringBuilder.toString());
                }
                Message msg = Message.obtain();
                msg.what = 4;
                str = SocketListenerTransport.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Create SocketClient success protocol = ");
                stringBuilder.append(protocol);
                HwLog.d(str, stringBuilder.toString());
                switch (protocol) {
                    case 1:
                        TCPNearbySocket result = new TCPNearbySocket(innerSocket);
                        result.setSocket(socket);
                        msg.obj = result;
                        break;
                }
                if (!SocketListenerTransport.this.mListenerHandler.sendMessage(msg)) {
                    HwLog.e(SocketListenerTransport.TAG, "createP2PNearbySocketClient : handler quitting,remove the listener. ");
                }
                HwLog.d(SocketListenerTransport.TAG, "createP2PNearbySocketClient: success.");
            }
        }).start();
    }
}
