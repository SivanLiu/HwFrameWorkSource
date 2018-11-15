package com.huawei.nearbysdk;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import com.huawei.nearbysdk.ICreateSocketListener.Stub;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class CreateSocketListenerTransport extends Stub {
    public static final int CANCEL_SUCCESS = 4;
    public static final int CREATE_NULL_SOCKET_ERROR = 2;
    public static final int MESSAGE_HANDLER_ERROR = 3;
    static final String TAG = "CreateSocketListenerTransport";
    public static final int TIME_LEFT_ERROR = 1;
    private static final int TYPE_CREATE_FAIL = 2;
    private static final int TYPE_HWSHARE_I_REMOTE = 3;
    private static final int TYPE_INNERSOCKET_CREATE_SUCCESS = 1;
    private static final int TYPE_THREAD_RESPONSE_CLIENT = 4;
    private static final int TYPE_THREAD_RESPONSE_SERVER = 5;
    private boolean hadCancel = false;
    private final BluetoothSocketTransport mBluetoothSocketTransport;
    private CreateSocketListener mListener;
    private final Handler mListenerHandler;
    private NearbyAdapter mNearbyAdapter;
    private ServerSocket mServerSocket;
    private long mStartTime = 0;
    private int mTimeOut = 0;

    class CreateP2PClientRunable implements Runnable {
        InternalNearbySocket innerSocket;
        int protocol;
        int timeLeft;

        public CreateP2PClientRunable(InternalNearbySocket socket, int pro, int timeout) {
            this.innerSocket = socket;
            this.protocol = pro;
            this.timeLeft = timeout;
        }

        public void run() {
            Message msg = Message.obtain();
            boolean hasException = false;
            Socket socket = new Socket();
            InternalNearbySocket iSocket = this.innerSocket;
            try {
                int connectTimeLeft = this.timeLeft;
                if (this.timeLeft == 0) {
                    connectTimeLeft = 1;
                }
                socket.bind(new InetSocketAddress(iSocket.getLocalIpAddress(), 0));
                socket.connect(new InetSocketAddress(iSocket.getIpAddress(), iSocket.getPort()), connectTimeLeft);
            } catch (IOException e) {
                hasException = true;
                HwLog.e(CreateSocketListenerTransport.TAG, "createP2PNearbySocketClient : IOException");
            } catch (RemoteException e2) {
                hasException = true;
                HwLog.e(CreateSocketListenerTransport.TAG, "createP2PNearbySocketClient : RemoteException");
            }
            try {
                socket.setKeepAlive(true);
            } catch (SocketException e3) {
                hasException = true;
                HwLog.e(CreateSocketListenerTransport.TAG, "createP2PNearbySocketClient : SocketException");
            }
            if (hasException) {
                try {
                    this.innerSocket.close();
                } catch (RemoteException e4) {
                    HwLog.e(CreateSocketListenerTransport.TAG, "createP2PNearbySocketClient : RemoteException");
                }
                msg.what = 2;
                msg.arg1 = NearbyConfig.ERROR_CREATE_THREAD_EXCEPTION;
                if (!CreateSocketListenerTransport.this.mListenerHandler.sendMessage(msg)) {
                    HwLog.e(CreateSocketListenerTransport.TAG, "createP2PNearbySocketClient : handler quitting,remove the listener. ");
                }
            }
            Message new_msg = Message.obtain();
            new_msg.what = 4;
            new_msg.arg1 = this.protocol;
            HwLog.d(CreateSocketListenerTransport.TAG, "Thread send msg.Create socketClient success");
            switch (this.protocol) {
                case 1:
                    TCPNearbySocket result = new TCPNearbySocket(this.innerSocket);
                    result.setSocket(socket);
                    new_msg.obj = result;
                    break;
            }
            if (!CreateSocketListenerTransport.this.mListenerHandler.sendMessage(new_msg)) {
                HwLog.e(CreateSocketListenerTransport.TAG, "createP2PNearbySocketClient : handler quitting,remove the listener. ");
            }
            HwLog.d(CreateSocketListenerTransport.TAG, "createP2PNearbySocketClient: success.");
        }
    }

    class CreateP2PServerRunable implements Runnable {
        InternalNearbySocket innerSocket;
        int protocol;
        int soTimeout;

        public CreateP2PServerRunable(InternalNearbySocket socket, int pro, int timeout) {
            this.innerSocket = socket;
            this.protocol = pro;
            this.soTimeout = timeout;
        }

        public void run() {
            Message msg = Message.obtain();
            boolean hasException = false;
            try {
                CreateSocketListenerTransport.this.mServerSocket = new ServerSocket(this.innerSocket.getPort());
                CreateSocketListenerTransport.this.mServerSocket.setSoTimeout(this.soTimeout);
                String str = CreateSocketListenerTransport.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setSoTimeout  soTimeout = ");
                stringBuilder.append(this.soTimeout);
                HwLog.d(str, stringBuilder.toString());
                Socket socket = CreateSocketListenerTransport.this.mServerSocket.accept();
                if (socket != null) {
                    msg.what = 5;
                    HwLog.d(CreateSocketListenerTransport.TAG, "Thread send msg.Create socketServer success");
                    switch (this.protocol) {
                        case 1:
                            TCPServerNearbySocket result = new TCPServerNearbySocket(this.innerSocket);
                            result.setServerSocket(CreateSocketListenerTransport.this.mServerSocket);
                            result.setSocket(socket);
                            msg.obj = result;
                            break;
                    }
                    if (!CreateSocketListenerTransport.this.mListenerHandler.sendMessage(msg)) {
                        HwLog.e(CreateSocketListenerTransport.TAG, "createP2PNearbySocketServer : handler quitting,remove the listener. ");
                    }
                }
            } catch (SocketTimeoutException e) {
                hasException = true;
                HwLog.e(CreateSocketListenerTransport.TAG, "createP2PNearbySocketServer connect timeout!");
            } catch (IOException e2) {
                hasException = true;
                HwLog.e(CreateSocketListenerTransport.TAG, "createP2PNearbySocketServer ServerSocket closed.");
            } catch (RemoteException e3) {
                hasException = true;
                HwLog.e(CreateSocketListenerTransport.TAG, "createP2PNearbySocketServer ServerSocket RemoteException.");
            }
            HwLog.d(CreateSocketListenerTransport.TAG, "createP2PNearbySocketServer: finish.");
            if (hasException) {
                CreateSocketListenerTransport.this.mServerSocket = null;
                Message new_msg = Message.obtain();
                try {
                    this.innerSocket.close();
                } catch (RemoteException e4) {
                    String str2 = CreateSocketListenerTransport.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("createP2PNearbySocketServer close fail: ");
                    stringBuilder2.append(e4);
                    HwLog.e(str2, stringBuilder2.toString());
                }
                new_msg.what = 2;
                new_msg.arg1 = NearbyConfig.ERROR_CREATE_THREAD_EXCEPTION;
                if (!CreateSocketListenerTransport.this.mListenerHandler.sendMessage(new_msg)) {
                    HwLog.e(CreateSocketListenerTransport.TAG, "createP2PNearbySocketServer : handler quitting,remove the listener. ");
                }
            }
        }
    }

    class BRCreateSocketCB implements ICreateSocketCallback {
        BRCreateSocketCB() {
        }

        public void onStatusChange(int status, NearbySocket nearbySocket, int arg) {
            if (status == 0) {
                CreateSocketListenerTransport.this.sendMessage(4, nearbySocket, arg, status);
            } else {
                CreateSocketListenerTransport.this.onCreateFail(NearbyConfig.ERROR_CLIENT_CONNECT_FAILED);
            }
        }
    }

    static class TCPServerNearbySocket extends TCPNearbySocket {
        private ServerSocket serverSocket = null;

        public TCPServerNearbySocket(InternalNearbySocket innerSocket) {
            super(innerSocket);
            HwLog.d(CreateSocketListenerTransport.TAG, "TCPServerNearbySocket construct");
        }

        public ServerSocket getServerSocket() {
            return this.serverSocket;
        }

        public void setServerSocket(ServerSocket socket) {
            String str = CreateSocketListenerTransport.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("TCPServerNearbySocket setServerSocket socket:");
            stringBuilder.append(socket);
            HwLog.d(str, stringBuilder.toString());
            this.serverSocket = socket;
        }

        public boolean close() {
            HwLog.d(CreateSocketListenerTransport.TAG, "TCPServerNearbySocket close");
            boolean result = super.close();
            if (this.serverSocket != null) {
                try {
                    this.serverSocket.close();
                } catch (IOException e) {
                    String str = CreateSocketListenerTransport.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("serverSocket close fail: ");
                    stringBuilder.append(e);
                    HwLog.e(str, stringBuilder.toString());
                }
            }
            return result;
        }
    }

    public void setTimeOut(int timeOut) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("timeOut = ");
        stringBuilder.append(timeOut);
        HwLog.d(str, stringBuilder.toString());
        this.mTimeOut = timeOut;
    }

    public void setStartTime(long startTime) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startTime = ");
        stringBuilder.append(startTime);
        HwLog.d(str, stringBuilder.toString());
        this.mStartTime = startTime;
    }

    CreateSocketListenerTransport(NearbyAdapter neabyAdapter, CreateSocketListener listener, Looper looper) {
        this.mNearbyAdapter = neabyAdapter;
        this.mBluetoothSocketTransport = new BluetoothSocketTransport();
        this.mListener = listener;
        this.mListenerHandler = new Handler(looper) {
            public void handleMessage(Message msg) {
                CreateSocketListenerTransport.this._handleMessage(msg);
            }
        };
    }

    private void _handleMessage(Message msg) {
        String str;
        StringBuilder stringBuilder;
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("_handleMessage: ");
        stringBuilder2.append(msg.toString());
        HwLog.d(str2, stringBuilder2.toString());
        InternalNearbySocket innerSocket;
        NearbySocket nearbySocket;
        switch (msg.what) {
            case 1:
                HwLog.d(TAG, "TYPE_INNERSOCKET_CREATE_SUCCESS createNearbySocketByChannelId()");
                innerSocket = msg.obj;
                if (!this.hadCancel) {
                    createNearbySocketByChannelId(innerSocket, msg.arg1);
                    break;
                }
                try {
                    innerSocket.close();
                } catch (RemoteException e) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("innerSocket close fail: ");
                    stringBuilder.append(e);
                    HwLog.e(str, stringBuilder.toString());
                }
                onCreateFailCallBack(4);
                return;
            case 2:
                HwLog.d(TAG, "TYPE_CREATE_FAIL Listener.onCreateFail");
                onCreateFailCallBack(msg.arg1);
                break;
            case 3:
                HwLog.d(TAG, "TYPE_HWSHARE_I_REMOTE createP2PNearbySocketServer");
                innerSocket = msg.obj;
                if (innerSocket == null) {
                    onCreateFailCallBack(2);
                } else if (this.hadCancel) {
                    try {
                        innerSocket.close();
                    } catch (RemoteException e2) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("socket close fail: ");
                        stringBuilder.append(e2);
                        HwLog.e(str, stringBuilder.toString());
                    }
                    onCreateFailCallBack(4);
                    return;
                }
                createP2PNearbySocketServer(innerSocket, 1, msg.arg1);
                break;
            case 4:
                HwLog.d(TAG, "TYPE_THREAD_RESPONSE_CLIENT Listener.onCreateSuccess");
                nearbySocket = msg.obj;
                if (nearbySocket != null) {
                    if (!this.hadCancel) {
                        onCreateSuccessCallBack(nearbySocket);
                        break;
                    } else {
                        onCreateFailCallBack(4);
                        break;
                    }
                }
                onCreateFailCallBack(2);
                break;
            case 5:
                HwLog.d(TAG, "TYPE_THREAD_RESPONSE_SERVER ready to listener.onOldVerConnect");
                nearbySocket = msg.obj;
                if (nearbySocket == null) {
                    onCreateFailCallBack(2);
                } else if (this.hadCancel) {
                    nearbySocket.close();
                    onCreateFailCallBack(4);
                    return;
                }
                if (this.mListener instanceof SocketBackwardCompatible) {
                    this.mListener.onOldVerConnect(nearbySocket, null);
                    HwLog.d(TAG, "onOldVerConnect: return nearbySocket success.");
                    break;
                }
                break;
            default:
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unknow message id:");
                stringBuilder2.append(msg.what);
                stringBuilder2.append(", can not be here!");
                HwLog.e(str2, stringBuilder2.toString());
                break;
        }
    }

    private void onCreateFailCallBack(int errorCode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("errorCode = ");
        stringBuilder.append(errorCode);
        HwLog.e(str, stringBuilder.toString());
        clear();
        this.mListener.onCreateFail(errorCode);
        if (this.mNearbyAdapter != null) {
            this.mNearbyAdapter.removeCreateSocketListener(this.mListener);
        }
    }

    private void onCreateSuccessCallBack(NearbySocket nearbySocket) {
        HwLog.e(TAG, "onCreateSuccessCallBack");
        this.mListener.onCreateSuccess(nearbySocket);
        if (this.mNearbyAdapter != null) {
            this.mNearbyAdapter.removeCreateSocketListener(this.mListener);
        }
    }

    private void createNearbySocketByChannelId(InternalNearbySocket innerSocket, int connectTimeLeft) {
        String str;
        StringBuilder stringBuilder;
        HwLog.d(TAG, "createNearbySocketByChannelId");
        try {
            int channelId = innerSocket.getChannelId();
            byte protocol = innerSocket.getProtocol();
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("check channelId = ");
            stringBuilder2.append(channelId);
            HwLog.d(str2, stringBuilder2.toString());
            switch (channelId) {
                case 2:
                    this.mBluetoothSocketTransport.createNearbySocketClient(innerSocket, new BRCreateSocketCB());
                    break;
                case 3:
                    createP2PNearbySocketClient(innerSocket, protocol, connectTimeLeft);
                    break;
                default:
                    try {
                        innerSocket.close();
                    } catch (RemoteException e) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("createNearbySocketByChannelId fail: ");
                        stringBuilder.append(e);
                        HwLog.e(str, stringBuilder.toString());
                    }
                    onCreateFailCallBack(NearbyConfig.ERROR_UNSUPPORT_CHANNLE);
                    break;
            }
        } catch (RemoteException e2) {
            onCreateFailCallBack(NearbyConfig.ERROR_REMOTE_DATA_EXCEPTION);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("createNearbySocketByChannelId fail: ");
            stringBuilder.append(e2);
            HwLog.e(str, stringBuilder.toString());
        }
    }

    private void createP2PNearbySocketClient(InternalNearbySocket innerSocket, int protocol, int timeLeft) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("createP2PNearbySocketClient protocol = ");
        stringBuilder.append(protocol);
        HwLog.d(str, stringBuilder.toString());
        new Thread(new CreateP2PClientRunable(innerSocket, protocol, timeLeft)).start();
    }

    private void createP2PNearbySocketServer(InternalNearbySocket innerSocket, int protocol, int soTimeout) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("createP2PNearbySocketServer protocol = ");
        stringBuilder.append(protocol);
        HwLog.d(str, stringBuilder.toString());
        new Thread(new CreateP2PServerRunable(innerSocket, protocol, soTimeout)).start();
    }

    void sendMessage(int msgWhat, Object obj, int para, int status) {
        Message msg = this.mListenerHandler.obtainMessage(msgWhat, para, status, obj);
        if (!this.mListenerHandler.sendMessage(msg)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendMessage fail with msg=");
            stringBuilder.append(msg.toString());
            HwLog.e(str, stringBuilder.toString());
        }
    }

    public void onCreateSuccess(InternalNearbySocket socket) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onCreateSuccess socket = ");
        stringBuilder.append(socket);
        HwLog.d(str, stringBuilder.toString());
        int timeLeft = getTimeLeft();
        if (timeLeft == 1) {
            try {
                socket.close();
            } catch (RemoteException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("socket close fail: ");
                stringBuilder2.append(e);
                HwLog.e(str2, stringBuilder2.toString());
            }
            onCreateFailCallBack(NearbyConfig.ERROR_TIME_OUT);
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("onCreateSuccess TIME_OUT mStartTime = ");
            stringBuilder3.append(this.mStartTime);
            stringBuilder3.append(" TimeOut = ");
            stringBuilder3.append(this.mTimeOut);
            stringBuilder3.append(" timeLeft = ");
            stringBuilder3.append(timeLeft);
            HwLog.d(str3, stringBuilder3.toString());
            return;
        }
        Message msg = Message.obtain();
        msg.what = 1;
        msg.obj = socket;
        msg.arg1 = timeLeft;
        if (!this.mListenerHandler.sendMessage(msg)) {
            messageError(socket);
        }
    }

    private int getTimeLeft() {
        long timeNow = System.currentTimeMillis();
        if (this.mStartTime > timeNow) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getTimeLeft get wrong time: mStartTime = ");
            stringBuilder.append(this.mStartTime);
            stringBuilder.append(" timeNow = ");
            stringBuilder.append(timeNow);
            HwLog.d(str, stringBuilder.toString());
        }
        long timeLeft = (((long) this.mTimeOut) + this.mStartTime) - timeNow;
        if (timeLeft < 0) {
            return 1;
        }
        return (int) timeLeft;
    }

    public void onCreateFail(int failCode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onCreateFail failCode = ");
        stringBuilder.append(failCode);
        HwLog.d(str, stringBuilder.toString());
        Message msg = Message.obtain();
        msg.what = 2;
        msg.arg1 = failCode;
        if (!this.mListenerHandler.sendMessage(msg)) {
            messageError(null);
        }
    }

    public void onHwShareIRemote(InternalNearbySocket socket) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onHwShareIRemote socket = ");
        stringBuilder.append(socket);
        HwLog.d(str, stringBuilder.toString());
        int timeLeft = getTimeLeft();
        if (timeLeft == 1) {
            try {
                socket.close();
            } catch (RemoteException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("onHwShareIRemote socket close exception: ");
                stringBuilder2.append(e);
                HwLog.e(str2, stringBuilder2.toString());
            }
            onCreateFailCallBack(NearbyConfig.ERROR_TIME_OUT);
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("onCreateSuccess TIME_OUT mStartTime = ");
            stringBuilder3.append(this.mStartTime);
            stringBuilder3.append(" TimeOut = ");
            stringBuilder3.append(this.mTimeOut);
            stringBuilder3.append(" timeLeft = ");
            stringBuilder3.append(timeLeft);
            HwLog.d(str3, stringBuilder3.toString());
            return;
        }
        Message msg = Message.obtain();
        msg.what = 3;
        msg.obj = socket;
        msg.arg1 = timeLeft;
        if (!this.mListenerHandler.sendMessage(msg)) {
            messageError(socket);
        }
    }

    public void cancel() {
        this.hadCancel = true;
        HwLog.d(TAG, "CreateSocketListenerTransport get cancle CMD.");
        closeServerSocket();
    }

    private void messageError(InternalNearbySocket socket) {
        HwLog.e(TAG, "messageError: handler quitting,remove the listener. ");
        if (socket != null) {
            try {
                socket.close();
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("socket close fail: ");
                stringBuilder.append(e);
                HwLog.e(str, stringBuilder.toString());
            }
        }
        onCreateFailCallBack(3);
    }

    private void clear() {
        HwLog.d(TAG, "clear");
        closeServerSocket();
        resetOpenTimeOut();
    }

    private void closeServerSocket() {
        if (this.mServerSocket != null) {
            try {
                this.mServerSocket.close();
            } catch (IOException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("socket close fail: ");
                stringBuilder.append(e);
                HwLog.e(str, stringBuilder.toString());
            } catch (Throwable th) {
                this.mServerSocket = null;
            }
            this.mServerSocket = null;
        }
    }

    private void resetOpenTimeOut() {
        this.mStartTime = 0;
    }
}
