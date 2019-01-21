package com.android.internal.telephony.cat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkRequest.Builder;
import android.net.StringNetworkSpecifier;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.AbstractPhoneInternalInterface;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.cat.AppInterface.CommandType;
import com.android.internal.telephony.cat.BearerDescription.BearerType;
import com.android.internal.telephony.cat.CatCmdMessage.ChannelSettings;
import com.android.internal.telephony.cat.CatCmdMessage.DataSettings;
import com.android.internal.telephony.cat.InterfaceTransportLevel.TransportProtocol;
import huawei.cust.HwCustUtils;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class BipProxy extends Handler {
    static final int CHANNEL_CLOSED = 2;
    static final int CHANNEL_IDENTIFIER_NOT_VALID = 3;
    static final int CHANNEL_STATUS_AVAILABLE = 32768;
    static final int CH_STATUS_LINK_DROP = 5;
    static final int CMD_QUAL_AUTO_RECONN = 2;
    static final int CMD_QUAL_BACKGROUND = 4;
    static final int CMD_QUAL_IMMEDIATE_LINK_ESTABLISH = 1;
    private static final int DEFAULT_DC_TIMEOUT = 180000;
    private static final int DEFAULT_WAKE_LOCK_TIMEOUT = 150000;
    private static final int DNS_SERVER_ADDRESS_REQUESTED = 8;
    static final int EVENT_DC_TIMEOUT = 100;
    static final int EVENT_WAKE_LOCK_TIMEOUT = 99;
    private static final int INVALID_SUBID = -1;
    private static final boolean IS_VZ;
    static final int MAX_CHANNEL_NUM = 7;
    static final int MAX_LEN_OF_CHANNEL_DATA = 237;
    static final int MAX_TCP_SERVER_CLIENT_CHANNEL_NUM = 15;
    static final int MSG_ID_DATA_STATE_CHANGED = 12;
    static final int MSG_ID_GET_DATA_STATE = 13;
    static final int MSG_ID_SETUP_DATA_CALL = 10;
    static final int MSG_ID_TEARDOWN_DATA_CALL = 11;
    static final int NO_CHANNEL_AVAILABLE = 1;
    static final int NO_SPECIFIC_CAUSE_CAN_BE_GIVEN = 0;
    static final int REQUESTED_BUFFER_SIZE_NOT_AVAILABLE = 4;
    static final int REQUESTED_SIM_ME_INTERFACE_TRANSPORT_LEVEL_NOT_AVAILABLE = 6;
    static final int SECURITY_ERROR = 5;
    private static final int SIM_NUM = TelephonyManager.getDefault().getPhoneCount();
    static final int TCP_CHANNEL_BUFFER_SIZE = 16384;
    static final int UDP_CHANNEL_BUFFER_SIZE = 1500;
    private BipChannel[] mBipChannels = new BipChannel[7];
    private ChannelApnInfo[] mChannelApnInfo = new ChannelApnInfo[7];
    private Context mContext;
    private DefaultBearerStateReceiver mDefaultBearerStateReceiver;
    private HwCustBipProxy mHwCustBipProxy = null;
    boolean mImmediateLinkEstablish = true;
    protected boolean[] mIsWifiConnected = new boolean[7];
    private CatService mStkService = null;
    WakeLock mWakeLock;
    final int mWakeLockTimeout;
    CatCmdMessage openChCmdMsg;

    interface BipChannel {
        void close(CatCmdMessage catCmdMessage);

        int getStatus();

        void onSessionEnd();

        boolean open(CatCmdMessage catCmdMessage);

        boolean preProcessOpen(CatCmdMessage catCmdMessage);

        void receive(CatCmdMessage catCmdMessage);

        void send(CatCmdMessage catCmdMessage);

        void setStatus(int i);
    }

    class BipNetworkCallback extends NetworkCallback {
        CatCmdMessage mCmdMsg;
        Network mCurrentNetwork = null;
        private int mNetworkType;

        BipNetworkCallback(int networkType, CatCmdMessage cmdMsg) {
            this.mNetworkType = networkType;
            this.mCmdMsg = cmdMsg;
        }

        public void onAvailable(Network network) {
            this.mCurrentNetwork = network;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onAvailable got Network: ");
            stringBuilder.append(network);
            CatLog.d((Object) this, stringBuilder.toString());
            CatLog.d((Object) this, "MSG_ID_SETUP_DATA_CALL");
            Message msg = BipProxy.this.obtainMessage(10, BipProxy.this.mChannelApnInfo[this.mNetworkType - 38].bakCmdMsg);
            AsyncResult.forMessage(msg, null, null);
            msg.sendToTarget();
        }

        public void onLost(Network network) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onLost Network: ");
            stringBuilder.append(network);
            stringBuilder.append(", mCurrentNetwork: ");
            stringBuilder.append(this.mCurrentNetwork);
            CatLog.d((Object) this, stringBuilder.toString());
            if (network.equals(this.mCurrentNetwork)) {
                this.mCurrentNetwork = null;
                CatCmdMessage cmdMsg = BipProxy.this.mChannelApnInfo[this.mNetworkType - 38].bakCmdMsg;
                BipProxy.this.teardownDataConnection(cmdMsg);
                BipProxy.this.checkSetStatusOrNot(cmdMsg);
            }
        }

        public void onUnavailable() {
            CatLog.d((Object) this, "onUnavailable");
            sendTerminalResponse(ResultCode.BEYOND_TERMINAL_CAPABILITY);
        }

        private void sendTerminalResponse(ResultCode rc) {
            if (this.mCmdMsg != null) {
                BipProxy.this.mStkService.sendTerminalResponse(this.mCmdMsg.mCmdDet, rc, false, 0, null);
            }
        }
    }

    static class ChannelApnInfo {
        CatCmdMessage bakCmdMsg;
        String feature;
        BipNetworkCallback networkCallback;
        int networkType;
        String type;

        public ChannelApnInfo(int channel, CatCmdMessage cmdMsg) {
            switch (channel) {
                case 1:
                    this.networkType = 38;
                    this.feature = AbstractPhoneInternalInterface.FEATURE_ENABLE_BIP0;
                    this.type = "bip0";
                    break;
                case 2:
                    this.networkType = 39;
                    this.feature = AbstractPhoneInternalInterface.FEATURE_ENABLE_BIP1;
                    this.type = "bip1";
                    break;
                case 3:
                    this.networkType = 40;
                    this.feature = AbstractPhoneInternalInterface.FEATURE_ENABLE_BIP2;
                    this.type = "bip2";
                    break;
                case 4:
                    this.networkType = 41;
                    this.feature = AbstractPhoneInternalInterface.FEATURE_ENABLE_BIP3;
                    this.type = "bip3";
                    break;
                case 5:
                    this.networkType = 42;
                    this.feature = AbstractPhoneInternalInterface.FEATURE_ENABLE_BIP4;
                    this.type = "bip4";
                    break;
                case 6:
                    this.networkType = 43;
                    this.feature = AbstractPhoneInternalInterface.FEATURE_ENABLE_BIP5;
                    this.type = "bip5";
                    break;
                case 7:
                    this.networkType = 44;
                    this.feature = AbstractPhoneInternalInterface.FEATURE_ENABLE_BIP6;
                    this.type = "bip6";
                    break;
            }
            this.bakCmdMsg = cmdMsg;
        }
    }

    private static class ConnectionSetupFailedException extends IOException {
        public ConnectionSetupFailedException(String message) {
            super(message);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ConnectionSetupFailedException: ");
            stringBuilder.append(message);
            CatLog.d((Object) this, stringBuilder.toString());
        }
    }

    class DefaultBearerStateReceiver extends BroadcastReceiver {
        Context mContext;
        IntentFilter mFilter = new IntentFilter();
        boolean mIsRegistered;

        public DefaultBearerStateReceiver(Context context) {
            this.mContext = context;
            this.mFilter.addAction("android.net.wifi.STATE_CHANGE");
            this.mIsRegistered = false;
        }

        public void startListening() {
            if (this.mIsRegistered) {
                CatLog.d((Object) this, "already registered");
                return;
            }
            this.mContext.registerReceiver(this, this.mFilter);
            this.mIsRegistered = true;
        }

        public void stopListening() {
            if (this.mIsRegistered) {
                this.mContext.unregisterReceiver(this);
                this.mIsRegistered = false;
                return;
            }
            CatLog.d((Object) this, "not registered or already de-registered");
        }

        public void handleWifiDisconnectedMsg(boolean isWifiConnected) {
            if (!isWifiConnected) {
                for (int i = 0; i < 7; i++) {
                    StringBuilder stringBuilder;
                    if (BipProxy.this.mBipChannels[i] != null) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("handleWifiDisconnectedMsg: mBipChannels[");
                        stringBuilder.append(i);
                        stringBuilder.append("] Status is ");
                        stringBuilder.append(BipProxy.this.mBipChannels[i].getStatus() & 255);
                        CatLog.d((Object) this, stringBuilder.toString());
                        if (5 == (BipProxy.this.mBipChannels[i].getStatus() & 255)) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("handleWifiDisconnectedMsg: mBipChannels[");
                            stringBuilder.append(i);
                            stringBuilder.append("] already link droped");
                            CatLog.d((Object) this, stringBuilder.toString());
                        } else if (BipProxy.this.mIsWifiConnected[i]) {
                            BipProxy.this.mBipChannels[i].setStatus(5);
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("handleWifiDisconnectedMsg: mBipChannels[");
                            stringBuilder.append(i);
                            stringBuilder.append("] CH_STATUS_LINK_DROP");
                            CatLog.d((Object) this, stringBuilder.toString());
                            BipProxy.this.cleanChannelApnInfo(i + 1);
                        } else {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("handleWifiDisconnectedMsg: mIsWifiConnected[");
                            stringBuilder.append(i);
                            stringBuilder.append("] is false");
                            CatLog.d((Object) this, stringBuilder.toString());
                        }
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("handleWifiDisconnectedMsg: mBipChannels[");
                        stringBuilder.append(i);
                        stringBuilder.append("] is null");
                        CatLog.d((Object) this, stringBuilder.toString());
                    }
                }
            }
        }

        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                CatLog.d((Object) this, "Received broadcast: intent is null");
            } else if (intent.getAction() == null) {
                CatLog.d((Object) this, "Received broadcast: Action is null");
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onReceive, action: ");
                stringBuilder.append(intent.getAction());
                CatLog.d((Object) this, stringBuilder.toString());
                if ("android.net.wifi.STATE_CHANGE".equals(intent.getAction())) {
                    NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    boolean isWifiConnected = networkInfo != null && networkInfo.isConnected();
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("WifiManager.NETWORK_STATE_CHANGED_ACTION: IsWifiConnected = ");
                    stringBuilder2.append(isWifiConnected);
                    CatLog.d((Object) this, stringBuilder2.toString());
                    handleWifiDisconnectedMsg(isWifiConnected);
                }
            }
        }
    }

    class TcpClientChannel implements BipChannel {
        CatCmdMessage catCmdMsg = null;
        ChannelSettings mChannelSettings = null;
        int mChannelStatus = 0;
        byte[] mRxBuf = new byte[16384];
        int mRxLen = 0;
        int mRxPos = 0;
        TcpClientSendThread mSendThread = null;
        Socket mSocket;
        TcpClientThread mThread = null;
        byte[] mTxBuf = new byte[16384];
        int mTxLen = 0;
        int mTxPos = 0;
        ResultCode result = ResultCode.OK;
        private Object token = new Object();

        class TcpClientSendThread extends Thread {
            private CatCmdMessage cmdMsg;

            public TcpClientSendThread(CatCmdMessage cmdMsg) {
                this.cmdMsg = cmdMsg;
            }

            public void run() {
                int i;
                DataSettings dataSettings = this.cmdMsg.getDataSettings();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("SEND_DATA on channel no: ");
                stringBuilder.append(dataSettings.channel);
                stringBuilder.append(" Transfer data into tx buffer");
                CatLog.d((Object) this, stringBuilder.toString());
                for (i = 0; i < dataSettings.data.length && TcpClientChannel.this.mTxPos < TcpClientChannel.this.mTxBuf.length; i++) {
                    byte[] bArr = TcpClientChannel.this.mTxBuf;
                    TcpClientChannel tcpClientChannel = TcpClientChannel.this;
                    int i2 = tcpClientChannel.mTxPos;
                    tcpClientChannel.mTxPos = i2 + 1;
                    bArr[i2] = dataSettings.data[i];
                }
                TcpClientChannel tcpClientChannel2 = TcpClientChannel.this;
                tcpClientChannel2.mTxLen += dataSettings.data.length;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Tx buffer now contains ");
                stringBuilder2.append(TcpClientChannel.this.mTxLen);
                stringBuilder2.append(" bytes.");
                CatLog.d((Object) this, stringBuilder2.toString());
                if (this.cmdMsg.getCommandQualifier() == 1) {
                    TcpClientChannel.this.mTxPos = 0;
                    i = TcpClientChannel.this.mTxLen;
                    TcpClientChannel.this.mTxLen = 0;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Sent data to socket ");
                    stringBuilder3.append(i);
                    stringBuilder3.append(" bytes.");
                    CatLog.d((Object) this, stringBuilder3.toString());
                    if (TcpClientChannel.this.mSocket == null) {
                        CatLog.d((Object) this, "Socket not available.");
                        BipProxy.this.mStkService.sendTerminalResponse(this.cmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 0, new SendDataResponseData(0));
                        return;
                    }
                    try {
                        TcpClientChannel.this.mSocket.getOutputStream().write(TcpClientChannel.this.mTxBuf, 0, i);
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Data on channel no: ");
                        stringBuilder3.append(dataSettings.channel);
                        stringBuilder3.append(" sent to socket.");
                        CatLog.d((Object) this, stringBuilder3.toString());
                    } catch (IOException e) {
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("IOException ");
                        stringBuilder4.append(e.getMessage());
                        CatLog.d((Object) this, stringBuilder4.toString());
                        BipProxy.this.mStkService.sendTerminalResponse(this.cmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 0, new SendDataResponseData(0));
                        return;
                    }
                }
                int avail = 238;
                if (TcpClientChannel.this.mChannelSettings != null) {
                    avail = TcpClientChannel.this.mChannelSettings.bufSize - TcpClientChannel.this.mTxLen;
                    if (avail > 255) {
                        avail = 255;
                    }
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("TR with ");
                stringBuilder2.append(avail);
                stringBuilder2.append(" bytes available in Tx Buffer on channel ");
                stringBuilder2.append(dataSettings.channel);
                CatLog.d((Object) this, stringBuilder2.toString());
                BipProxy.this.mStkService.sendTerminalResponse(this.cmdMsg.mCmdDet, ResultCode.OK, false, 0, new SendDataResponseData(avail));
            }
        }

        class TcpClientThread extends Thread {
            TcpClientThread() {
            }

            public void run() {
                StringBuilder stringBuilder;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Client thread start on channel no: ");
                stringBuilder2.append(TcpClientChannel.this.mChannelSettings.channel);
                CatLog.d((Object) this, stringBuilder2.toString());
                try {
                    InetAddress addr;
                    if (TransportProtocol.TCP_CLIENT_REMOTE == TcpClientChannel.this.mChannelSettings.protocol) {
                        addr = InetAddress.getByAddress(TcpClientChannel.this.mChannelSettings.destinationAddress);
                    } else {
                        addr = InetAddress.getLocalHost();
                    }
                    TcpClientChannel.this.mSocket = new Socket();
                    CatLog.d((Object) this, "TcpClientThread bindSocket");
                    ChannelApnInfo curInfo = BipProxy.this.mChannelApnInfo[TcpClientChannel.this.mChannelSettings.channel - 1];
                    if (!(curInfo == null || curInfo.networkCallback == null || curInfo.networkCallback.mCurrentNetwork == null)) {
                        curInfo.networkCallback.mCurrentNetwork.bindSocket(TcpClientChannel.this.mSocket);
                        TcpClientChannel.this.mSocket.connect(new InetSocketAddress(addr, TcpClientChannel.this.mChannelSettings.port));
                        CatLog.d((Object) this, "TcpClientThread mSocket.connect");
                    }
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Connected TCP client socket for channel ");
                    stringBuilder3.append(TcpClientChannel.this.mChannelSettings.channel);
                    CatLog.d((Object) this, stringBuilder3.toString());
                    TcpClientChannel.this.mChannelStatus = 32768 + (TcpClientChannel.this.mChannelSettings.channel << 8);
                    if (BipProxy.this.mImmediateLinkEstablish) {
                        BipProxy.this.mStkService.sendTerminalResponse(TcpClientChannel.this.catCmdMsg.mCmdDet, TcpClientChannel.this.result, false, 0, new OpenChannelResponseData(TcpClientChannel.this.mChannelSettings.bufSize, Integer.valueOf(TcpClientChannel.this.mChannelStatus), TcpClientChannel.this.mChannelSettings.bearerDescription));
                    } else {
                        BipProxy.this.mImmediateLinkEstablish = true;
                    }
                    while (TcpClientChannel.this.mSocket != null) {
                        try {
                            TcpClientChannel.this.mRxLen = TcpClientChannel.this.mSocket.getInputStream().read(TcpClientChannel.this.mRxBuf);
                        } catch (IOException e) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Read on No: ");
                            stringBuilder.append(TcpClientChannel.this.mChannelSettings.channel);
                            stringBuilder.append(", IOException ");
                            stringBuilder.append(e.getMessage());
                            CatLog.d((Object) this, stringBuilder.toString());
                            TcpClientChannel.this.mSocket = null;
                            TcpClientChannel.this.mRxBuf = new byte[TcpClientChannel.this.mChannelSettings.bufSize];
                            TcpClientChannel.this.mTxBuf = new byte[TcpClientChannel.this.mChannelSettings.bufSize];
                            TcpClientChannel.this.mRxPos = 0;
                            TcpClientChannel.this.mRxLen = 0;
                            TcpClientChannel.this.mTxPos = 0;
                            TcpClientChannel.this.mTxLen = 0;
                            if (BipProxy.this.mChannelApnInfo[TcpClientChannel.this.mChannelSettings.channel - 1] != null) {
                                CatLog.d((Object) this, "TcpClientThread Exception happened");
                                BipProxy.this.teardownDataConnection(TcpClientChannel.this.catCmdMsg);
                                BipProxy.this.checkSetStatusOrNot(TcpClientChannel.this.catCmdMsg);
                            }
                        }
                        synchronized (TcpClientChannel.this.token) {
                            if (TcpClientChannel.this.mRxLen > 0) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("BipLog, ");
                                stringBuilder.append(TcpClientChannel.this.mRxLen);
                                stringBuilder.append(" data read.");
                                CatLog.d((Object) this, stringBuilder.toString());
                                TcpClientChannel.this.mRxPos = 0;
                                int available = 255;
                                if (TcpClientChannel.this.mRxLen < 255) {
                                    available = TcpClientChannel.this.mRxLen;
                                }
                                BipProxy.this.sendDataAvailableEvent(TcpClientChannel.this.mChannelStatus, (byte) (available & 255));
                                try {
                                    TcpClientChannel.this.token.wait();
                                } catch (InterruptedException e2) {
                                    e2.printStackTrace();
                                }
                            }
                        }
                    }
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Client thread end on channel no: ");
                    stringBuilder2.append(TcpClientChannel.this.mChannelSettings.channel);
                    CatLog.d((Object) this, stringBuilder2.toString());
                } catch (IOException e3) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("OPEN_CHANNEL - Client connection failed: ");
                    stringBuilder.append(e3.getMessage());
                    CatLog.d((Object) this, stringBuilder.toString());
                    if (BipProxy.this.mImmediateLinkEstablish) {
                        BipProxy.this.mStkService.sendTerminalResponse(TcpClientChannel.this.catCmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 0, new OpenChannelResponseData(TcpClientChannel.this.mChannelSettings.bufSize, Integer.valueOf(TcpClientChannel.this.mChannelStatus), TcpClientChannel.this.mChannelSettings.bearerDescription));
                    } else {
                        BipProxy.this.mImmediateLinkEstablish = true;
                    }
                    if (BipProxy.this.mChannelApnInfo[TcpClientChannel.this.mChannelSettings.channel - 1] != null) {
                        BipProxy.this.teardownDataConnection(TcpClientChannel.this.catCmdMsg);
                    }
                    BipProxy.this.mStkService.sendBroadcastToOtaUI(BipProxy.this.mStkService.OTA_TYPE, false);
                }
            }
        }

        TcpClientChannel() {
        }

        public boolean preProcessOpen(CatCmdMessage cmdMsg) {
            this.mChannelSettings = cmdMsg.getChannelSettings();
            this.mChannelStatus = this.mChannelSettings.channel << 8;
            this.catCmdMsg = cmdMsg;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("preProcessOpen bufSize = ");
            stringBuilder.append(this.mChannelSettings.bufSize);
            stringBuilder.append(" mImmediateLinkEstablish = ");
            stringBuilder.append(BipProxy.this.mImmediateLinkEstablish);
            CatLog.d((Object) this, stringBuilder.toString());
            if (this.mChannelSettings.bufSize > 16384) {
                this.result = ResultCode.PRFRMD_WITH_MODIFICATION;
                this.mChannelSettings.bufSize = 16384;
            } else {
                this.mRxBuf = new byte[this.mChannelSettings.bufSize];
                this.mTxBuf = new byte[this.mChannelSettings.bufSize];
            }
            if (!BipProxy.this.mImmediateLinkEstablish) {
                BipProxy.this.mStkService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.OK, false, 0, new OpenChannelResponseData(this.mChannelSettings.bufSize, Integer.valueOf(this.mChannelStatus), this.mChannelSettings.bearerDescription));
            }
            return true;
        }

        public boolean open(CatCmdMessage cmdMsg) {
            this.mThread = new TcpClientThread();
            this.mThread.start();
            return true;
        }

        public void close(CatCmdMessage cmdMsg) {
            if (this.mChannelSettings != null) {
                CatLog.d((Object) this, "Update channel status to closed before close socket");
                this.mChannelStatus = this.mChannelSettings.channel << 8;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mSocket = ");
            stringBuilder.append(this.mSocket);
            CatLog.d((Object) this, stringBuilder.toString());
            if (!(this.mSocket == null || this.mSocket.isClosed())) {
                try {
                    this.mSocket.close();
                } catch (IOException e) {
                }
            }
            this.mSocket = null;
            this.mRxPos = 0;
            this.mRxLen = 0;
            this.mTxPos = 0;
            this.mTxLen = 0;
            if (this.mChannelSettings == null) {
                CatLog.d((Object) this, "TcpClientChannel close BIP_ERROR");
                BipProxy.this.mStkService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 3, null);
                return;
            }
            this.mChannelStatus = this.mChannelSettings.channel << 8;
            BipProxy.this.mStkService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.OK, false, 0, null);
            BipProxy.this.mStkService.sendBroadcastToOtaUI(BipProxy.this.mStkService.OTA_TYPE, true);
            if (BipProxy.this.mChannelApnInfo[this.mChannelSettings.channel - 1] != null) {
                BipProxy.this.teardownDataConnection(cmdMsg);
            }
        }

        public void send(CatCmdMessage cmdMsg) {
            if (!BipProxy.this.mImmediateLinkEstablish && cmdMsg.getCommandQualifier() == 1 && BipProxy.this.setupDataConnection(BipProxy.this.openChCmdMsg)) {
                CatLog.d((Object) this, "Continue processing open channel");
                if (BipProxy.this.mBipChannels[cmdMsg.getDataSettings().channel - 1].open(BipProxy.this.openChCmdMsg)) {
                    this.mSendThread = new TcpClientSendThread(cmdMsg);
                    this.mSendThread.start();
                } else {
                    BipProxy.this.cleanupBipChannel(cmdMsg.getDataSettings().channel);
                }
                return;
            }
            this.mSendThread = new TcpClientSendThread(cmdMsg);
            this.mSendThread.start();
        }

        public void receive(CatCmdMessage cmdMsg) {
            ResultCode result = ResultCode.OK;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RECEIVE_DATA on channel no: ");
            stringBuilder.append(cmdMsg.getDataSettings().channel);
            CatLog.d((Object) this, stringBuilder.toString());
            int requested = cmdMsg.getDataSettings().length;
            if (requested > BipProxy.MAX_LEN_OF_CHANNEL_DATA) {
                requested = BipProxy.MAX_LEN_OF_CHANNEL_DATA;
            }
            if (requested > this.mRxLen) {
                requested = this.mRxLen;
                result = ResultCode.PRFRMD_WITH_MISSING_INFO;
            }
            this.mRxLen -= requested;
            int available = 255;
            if (this.mRxLen < 255) {
                available = this.mRxLen;
            }
            int available2 = available;
            byte[] data = null;
            if (requested > 0) {
                data = new byte[requested];
                System.arraycopy(this.mRxBuf, this.mRxPos, data, 0, requested);
                this.mRxPos += requested;
            }
            BipProxy.this.mStkService.sendTerminalResponse(cmdMsg.mCmdDet, result, false, 0, new ReceiveDataResponseData(data, available2));
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Receive Data, available data is: ");
            stringBuilder2.append(available2);
            CatLog.d((Object) this, stringBuilder2.toString());
            if (this.mRxLen == 0) {
                synchronized (this.token) {
                    this.token.notifyAll();
                }
            }
        }

        public int getStatus() {
            if (this.mChannelSettings == null) {
                this.mChannelStatus = 0;
            } else if (this.mChannelSettings.channel == 0) {
                this.mChannelStatus = this.mChannelSettings.channel << 8;
            }
            return this.mChannelStatus;
        }

        public void setStatus(int status) {
            if (5 == status && (this.mChannelStatus & 32768) == 32768) {
                this.mChannelStatus = (this.mChannelStatus & 32512) | status;
                BipProxy.this.sendChannelStatusEvent(this.mChannelStatus);
            }
        }

        public void onSessionEnd() {
            if (this.mThread == null || !this.mThread.isAlive()) {
                this.mThread = new TcpClientThread();
                this.mThread.start();
            }
        }
    }

    class UdpClientChannel implements BipChannel {
        CatCmdMessage catCmdMsg = null;
        ChannelSettings mChannelSettings = null;
        int mChannelStatus = 0;
        DatagramSocket mDatagramSocket;
        byte[] mRxBuf = new byte[BipProxy.UDP_CHANNEL_BUFFER_SIZE];
        int mRxLen = 0;
        int mRxPos = 0;
        UdpClientSendThread mSendThread = null;
        UdpClientThread mThread = null;
        byte[] mTxBuf = new byte[BipProxy.UDP_CHANNEL_BUFFER_SIZE];
        int mTxLen = 0;
        int mTxPos = 0;
        ResultCode result = ResultCode.OK;
        private Object token = new Object();

        class UdpClientSendThread extends Thread {
            private CatCmdMessage cmdMsg;

            public UdpClientSendThread(CatCmdMessage cmdMsg) {
                this.cmdMsg = cmdMsg;
            }

            public void run() {
                int i;
                StringBuilder stringBuilder;
                DataSettings dataSettings = this.cmdMsg.getDataSettings();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("SEND_DATA on channel no: ");
                stringBuilder2.append(dataSettings.channel);
                CatLog.d((Object) this, stringBuilder2.toString());
                CatLog.d((Object) this, "Transfer data into tx buffer");
                for (i = 0; i < dataSettings.data.length && UdpClientChannel.this.mTxPos < UdpClientChannel.this.mTxBuf.length; i++) {
                    byte[] bArr = UdpClientChannel.this.mTxBuf;
                    UdpClientChannel udpClientChannel = UdpClientChannel.this;
                    int i2 = udpClientChannel.mTxPos;
                    udpClientChannel.mTxPos = i2 + 1;
                    bArr[i2] = dataSettings.data[i];
                }
                UdpClientChannel udpClientChannel2 = UdpClientChannel.this;
                udpClientChannel2.mTxLen += dataSettings.data.length;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Tx buffer now contains ");
                stringBuilder3.append(UdpClientChannel.this.mTxLen);
                stringBuilder3.append(" bytes.");
                CatLog.d((Object) this, stringBuilder3.toString());
                if (this.cmdMsg.getCommandQualifier() == 1) {
                    UdpClientChannel.this.mTxPos = 0;
                    i = UdpClientChannel.this.mTxLen;
                    UdpClientChannel.this.mTxLen = 0;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Sent data to socket ");
                    stringBuilder4.append(i);
                    stringBuilder4.append(" bytes.");
                    CatLog.d((Object) this, stringBuilder4.toString());
                    if (UdpClientChannel.this.mDatagramSocket == null) {
                        CatLog.d((Object) this, "Socket not available.");
                        BipProxy.this.mStkService.sendTerminalResponse(this.cmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 0, new SendDataResponseData(0));
                        return;
                    }
                    InetAddress addr = null;
                    try {
                        InetAddress addr2;
                        if (TransportProtocol.UDP_CLIENT_REMOTE == UdpClientChannel.this.mChannelSettings.protocol) {
                            addr2 = InetAddress.getByAddress(UdpClientChannel.this.mChannelSettings.destinationAddress);
                        } else {
                            addr2 = InetAddress.getLocalHost();
                        }
                        addr = addr2;
                    } catch (IOException e) {
                        StringBuilder stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("OPEN_CHANNEL - UDP Client connection failed: ");
                        stringBuilder5.append(e.getMessage());
                        CatLog.d((Object) this, stringBuilder5.toString());
                        BipProxy.this.mStkService.sendTerminalResponse(this.cmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 0, new OpenChannelResponseData(UdpClientChannel.this.mChannelSettings.bufSize, Integer.valueOf(UdpClientChannel.this.mChannelStatus), UdpClientChannel.this.mChannelSettings.bearerDescription));
                        if (BipProxy.this.mChannelApnInfo[UdpClientChannel.this.mChannelSettings.channel - 1] != null) {
                            BipProxy.this.teardownDataConnection(this.cmdMsg);
                        }
                    }
                    try {
                        UdpClientChannel.this.mDatagramSocket.send(new DatagramPacket(UdpClientChannel.this.mTxBuf, i, addr, UdpClientChannel.this.mChannelSettings.port));
                        StringBuilder stringBuilder6 = new StringBuilder();
                        stringBuilder6.append("Data on channel no: ");
                        stringBuilder6.append(dataSettings.channel);
                        stringBuilder6.append(" sent to socket.");
                        CatLog.d((Object) this, stringBuilder6.toString());
                    } catch (IOException e2) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("IOException ");
                        stringBuilder.append(e2.getMessage());
                        CatLog.d((Object) this, stringBuilder.toString());
                        BipProxy.this.mStkService.sendTerminalResponse(this.cmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 0, new SendDataResponseData(0));
                        return;
                    } catch (IllegalArgumentException e3) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("IllegalArgumentException ");
                        stringBuilder.append(e3.getMessage());
                        CatLog.d((Object) this, stringBuilder.toString());
                        BipProxy.this.mStkService.sendTerminalResponse(this.cmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 0, new SendDataResponseData(0));
                        return;
                    }
                }
                int avail = 238;
                if (UdpClientChannel.this.mChannelSettings != null) {
                    avail = UdpClientChannel.this.mChannelSettings.bufSize - UdpClientChannel.this.mTxLen;
                    if (avail > 255) {
                        avail = 255;
                    }
                }
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("TR with ");
                stringBuilder3.append(avail);
                stringBuilder3.append(" bytes available in Tx Buffer on channel ");
                stringBuilder3.append(dataSettings.channel);
                CatLog.d((Object) this, stringBuilder3.toString());
                BipProxy.this.mStkService.sendTerminalResponse(this.cmdMsg.mCmdDet, ResultCode.OK, false, 0, new SendDataResponseData(avail));
            }
        }

        class UdpClientThread extends Thread {
            UdpClientThread() {
            }

            public void run() {
                StringBuilder stringBuilder;
                StringBuilder stringBuilder2;
                try {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Creating ");
                    stringBuilder3.append(TransportProtocol.UDP_CLIENT_REMOTE == UdpClientChannel.this.mChannelSettings.protocol ? "remote" : "local");
                    stringBuilder3.append(" client socket for channel ");
                    stringBuilder3.append(UdpClientChannel.this.mChannelSettings.channel);
                    CatLog.d((Object) this, stringBuilder3.toString());
                    UdpClientChannel.this.mDatagramSocket = new DatagramSocket();
                    CatLog.d((Object) this, "UdpClientThread bindSocket");
                    ChannelApnInfo curInfo = BipProxy.this.mChannelApnInfo[UdpClientChannel.this.mChannelSettings.channel - 1];
                    if (!(curInfo == null || curInfo.networkCallback == null || curInfo.networkCallback.mCurrentNetwork == null)) {
                        curInfo.networkCallback.mCurrentNetwork.bindSocket(UdpClientChannel.this.mDatagramSocket);
                    }
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Connected UDP client socket for channel ");
                    stringBuilder2.append(UdpClientChannel.this.mChannelSettings.channel);
                    CatLog.d((Object) this, stringBuilder2.toString());
                    UdpClientChannel.this.mChannelStatus = 32768 + (UdpClientChannel.this.mChannelSettings.channel << 8);
                    BipProxy.this.mStkService.sendTerminalResponse(UdpClientChannel.this.catCmdMsg.mCmdDet, UdpClientChannel.this.result, false, 0, new OpenChannelResponseData(UdpClientChannel.this.mChannelSettings.bufSize, Integer.valueOf(UdpClientChannel.this.mChannelStatus), UdpClientChannel.this.mChannelSettings.bearerDescription));
                    while (UdpClientChannel.this.mDatagramSocket != null) {
                        DatagramPacket packet = null;
                        boolean success = false;
                        try {
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("UDP Client listening on port: ");
                            stringBuilder4.append(UdpClientChannel.this.mDatagramSocket.getLocalPort());
                            CatLog.d((Object) this, stringBuilder4.toString());
                            packet = new DatagramPacket(UdpClientChannel.this.mRxBuf, UdpClientChannel.this.mRxBuf.length);
                            UdpClientChannel.this.mDatagramSocket.receive(packet);
                            success = true;
                        } catch (IOException e) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Read on No: ");
                            stringBuilder.append(UdpClientChannel.this.mChannelSettings.channel);
                            stringBuilder.append(", IOException ");
                            stringBuilder.append(e.getMessage());
                            CatLog.d((Object) this, stringBuilder.toString());
                        } catch (IllegalArgumentException e2) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("IllegalArgumentException: ");
                            stringBuilder.append(e2.getMessage());
                            CatLog.d((Object) this, stringBuilder.toString());
                        }
                        if (success) {
                            UdpClientChannel.this.mRxLen = packet.getLength();
                        } else {
                            UdpClientChannel.this.mDatagramSocket = null;
                            UdpClientChannel.this.mRxBuf = new byte[UdpClientChannel.this.mChannelSettings.bufSize];
                            UdpClientChannel.this.mTxBuf = new byte[UdpClientChannel.this.mChannelSettings.bufSize];
                            UdpClientChannel.this.mRxPos = 0;
                            UdpClientChannel.this.mRxLen = 0;
                            UdpClientChannel.this.mTxPos = 0;
                            UdpClientChannel.this.mTxLen = 0;
                            if (BipProxy.this.mChannelApnInfo[UdpClientChannel.this.mChannelSettings.channel - 1] != null) {
                                CatLog.d((Object) this, "UdpClientThread Exception happened");
                                BipProxy.this.teardownDataConnection(UdpClientChannel.this.catCmdMsg);
                                BipProxy.this.checkSetStatusOrNot(UdpClientChannel.this.catCmdMsg);
                            }
                        }
                        synchronized (UdpClientChannel.this.token) {
                            if (UdpClientChannel.this.mRxLen <= 0) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("No data read. ");
                                stringBuilder.append(UdpClientChannel.this.mRxLen);
                                CatLog.d((Object) this, stringBuilder.toString());
                            } else {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("BipLog, ");
                                stringBuilder.append(UdpClientChannel.this.mRxLen);
                                stringBuilder.append(" data read.");
                                CatLog.d((Object) this, stringBuilder.toString());
                                UdpClientChannel.this.mRxPos = 0;
                                int available = 255;
                                if (UdpClientChannel.this.mRxLen < 255) {
                                    available = UdpClientChannel.this.mRxLen;
                                }
                                BipProxy.this.sendDataAvailableEvent(UdpClientChannel.this.mChannelStatus, (byte) (available & 255));
                                try {
                                    UdpClientChannel.this.token.wait();
                                } catch (InterruptedException e3) {
                                    e3.printStackTrace();
                                }
                            }
                        }
                    }
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("UDP Client thread end on channel no: ");
                    stringBuilder5.append(UdpClientChannel.this.mChannelSettings.channel);
                    CatLog.d((Object) this, stringBuilder5.toString());
                } catch (IOException e4) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("OPEN_CHANNEL - UDP Client connection failed: ");
                    stringBuilder2.append(e4.getMessage());
                    CatLog.d((Object) this, stringBuilder2.toString());
                    if (BipProxy.this.mImmediateLinkEstablish) {
                        BipProxy.this.mStkService.sendTerminalResponse(UdpClientChannel.this.catCmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 0, new OpenChannelResponseData(UdpClientChannel.this.mChannelSettings.bufSize, Integer.valueOf(UdpClientChannel.this.mChannelStatus), UdpClientChannel.this.mChannelSettings.bearerDescription));
                    } else {
                        BipProxy.this.mImmediateLinkEstablish = true;
                    }
                    if (BipProxy.this.mChannelApnInfo[UdpClientChannel.this.mChannelSettings.channel - 1] != null) {
                        BipProxy.this.teardownDataConnection(UdpClientChannel.this.catCmdMsg);
                    }
                    BipProxy.this.mStkService.sendBroadcastToOtaUI(BipProxy.this.mStkService.OTA_TYPE, false);
                }
            }
        }

        UdpClientChannel() {
        }

        public boolean preProcessOpen(CatCmdMessage cmdMsg) {
            this.mChannelSettings = cmdMsg.getChannelSettings();
            this.mChannelStatus = this.mChannelSettings.channel << 8;
            this.catCmdMsg = cmdMsg;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("preProcessOpen bufSize = ");
            stringBuilder.append(this.mChannelSettings.bufSize);
            stringBuilder.append(", mImmediateLinkEstablish = ");
            stringBuilder.append(BipProxy.this.mImmediateLinkEstablish);
            CatLog.d((Object) this, stringBuilder.toString());
            if (this.mChannelSettings.bufSize > BipProxy.UDP_CHANNEL_BUFFER_SIZE) {
                this.result = ResultCode.PRFRMD_WITH_MODIFICATION;
                this.mChannelSettings.bufSize = BipProxy.UDP_CHANNEL_BUFFER_SIZE;
            } else if (this.mChannelSettings.bufSize > 0) {
                this.mRxBuf = new byte[this.mChannelSettings.bufSize];
                this.mTxBuf = new byte[this.mChannelSettings.bufSize];
            } else {
                this.mChannelSettings.bufSize = BipProxy.UDP_CHANNEL_BUFFER_SIZE;
            }
            if (!BipProxy.this.mImmediateLinkEstablish) {
                BipProxy.this.mStkService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.OK, false, 0, new OpenChannelResponseData(this.mChannelSettings.bufSize, Integer.valueOf(this.mChannelStatus), this.mChannelSettings.bearerDescription));
            }
            return true;
        }

        public boolean open(CatCmdMessage cmdMsg) {
            this.mThread = new UdpClientThread();
            this.mThread.start();
            return true;
        }

        public void close(CatCmdMessage cmdMsg) {
            if (this.mChannelSettings != null) {
                CatLog.d((Object) this, "Update channel status to closed before close socket");
                this.mChannelStatus = this.mChannelSettings.channel << 8;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mDatagramSocket = ");
            stringBuilder.append(this.mDatagramSocket);
            stringBuilder.append(" mChannelSettings = ");
            stringBuilder.append(this.mChannelSettings);
            CatLog.d((Object) this, stringBuilder.toString());
            if (!(this.mDatagramSocket == null || this.mDatagramSocket.isClosed())) {
                this.mDatagramSocket.close();
            }
            this.mDatagramSocket = null;
            this.mRxPos = 0;
            this.mRxLen = 0;
            this.mTxPos = 0;
            this.mTxLen = 0;
            if (this.mChannelSettings == null) {
                CatLog.d((Object) this, "UdpClientChannel close BIP_ERROR");
                BipProxy.this.mStkService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 3, null);
                return;
            }
            this.mChannelStatus = this.mChannelSettings.channel << 8;
            BipProxy.this.mStkService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.OK, false, 0, null);
            BipProxy.this.mStkService.sendBroadcastToOtaUI(BipProxy.this.mStkService.OTA_TYPE, true);
            if (BipProxy.this.mChannelApnInfo[this.mChannelSettings.channel - 1] != null) {
                CatLog.d((Object) this, "UdpClientChannel close");
                BipProxy.this.teardownDataConnection(cmdMsg);
            }
        }

        public void send(CatCmdMessage cmdMsg) {
            if (!BipProxy.this.mImmediateLinkEstablish && cmdMsg.getCommandQualifier() == 1 && BipProxy.this.setupDataConnection(BipProxy.this.openChCmdMsg)) {
                CatLog.d((Object) this, "Continue processing open channel");
                if (BipProxy.this.mBipChannels[cmdMsg.getDataSettings().channel - 1].open(BipProxy.this.openChCmdMsg)) {
                    this.mSendThread = new UdpClientSendThread(cmdMsg);
                    this.mSendThread.start();
                } else {
                    BipProxy.this.cleanupBipChannel(cmdMsg.getDataSettings().channel);
                }
                return;
            }
            this.mSendThread = new UdpClientSendThread(cmdMsg);
            this.mSendThread.start();
        }

        public void receive(CatCmdMessage cmdMsg) {
            ResultCode result = ResultCode.OK;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RECEIVE_DATA on channel no: ");
            stringBuilder.append(cmdMsg.getDataSettings().channel);
            CatLog.d((Object) this, stringBuilder.toString());
            int requested = cmdMsg.getDataSettings().length;
            if (requested > BipProxy.MAX_LEN_OF_CHANNEL_DATA) {
                requested = BipProxy.MAX_LEN_OF_CHANNEL_DATA;
            }
            if (requested > this.mRxLen) {
                requested = this.mRxLen;
                result = ResultCode.PRFRMD_WITH_MISSING_INFO;
            }
            this.mRxLen -= requested;
            int available = 255;
            if (this.mRxLen < 255) {
                available = this.mRxLen;
            }
            int available2 = available;
            byte[] data = null;
            if (requested > 0) {
                data = new byte[requested];
                System.arraycopy(this.mRxBuf, this.mRxPos, data, 0, requested);
                this.mRxPos += requested;
            }
            BipProxy.this.mStkService.sendTerminalResponse(cmdMsg.mCmdDet, result, false, 0, new ReceiveDataResponseData(data, available2));
            if (this.mRxLen == 0) {
                synchronized (this.token) {
                    this.token.notifyAll();
                }
            }
        }

        public int getStatus() {
            if (this.mChannelSettings == null) {
                this.mChannelStatus = 0;
            } else if (this.mChannelSettings.channel == 0) {
                this.mChannelStatus = this.mChannelSettings.channel << 8;
            }
            return this.mChannelStatus;
        }

        public void setStatus(int status) {
            if (5 == status && (this.mChannelStatus & 32768) == 32768) {
                this.mChannelStatus = (this.mChannelStatus & 32512) | status;
                BipProxy.this.sendChannelStatusEvent(this.mChannelStatus);
            }
        }

        public void onSessionEnd() {
            if (this.mThread == null || !this.mThread.isAlive()) {
                this.mThread = new UdpClientThread();
                this.mThread.start();
            }
        }
    }

    static {
        boolean z = "389".equals(SystemProperties.get("ro.config.hw_opta")) && "840".equals(SystemProperties.get("ro.config.hw_optb"));
        IS_VZ = z;
    }

    public BipProxy(CatService stkService, CommandsInterface cmdIf, Context context) {
        this.mStkService = stkService;
        this.mContext = context;
        this.mDefaultBearerStateReceiver = new DefaultBearerStateReceiver(context);
        this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, "BipProxy");
        this.mWakeLock.setReferenceCounted(false);
        this.mWakeLockTimeout = DEFAULT_WAKE_LOCK_TIMEOUT;
        this.mHwCustBipProxy = (HwCustBipProxy) HwCustUtils.createObj(HwCustBipProxy.class, new Object[]{this.mContext});
    }

    public boolean canHandleNewChannel() {
        for (int i = 0; i < this.mBipChannels.length; i++) {
            if (this.mChannelApnInfo[i] == null) {
                this.mBipChannels[i] = null;
            }
            if (this.mBipChannels[i] == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("channel index ");
                stringBuilder.append(i);
                stringBuilder.append(" found. new channel can be handled");
                CatLog.d((Object) this, stringBuilder.toString());
                return true;
            }
        }
        CatLog.d((Object) this, "new channel can't be handled");
        return false;
    }

    private boolean isBipOverWlanAllowed() {
        return SystemProperties.getBoolean("ro.config.bip_over_wlan", false);
    }

    private boolean isDefaultBearerDescriptionType(CatCmdMessage cmdMsg) {
        boolean z = false;
        if (cmdMsg == null || CommandType.OPEN_CHANNEL != cmdMsg.getCmdType()) {
            return false;
        }
        if (BearerType.DEFAULT_BEARER == cmdMsg.getChannelSettings().bearerDescription.type) {
            z = true;
        }
        return z;
    }

    /* JADX WARNING: Missing block: B:13:0x0020, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isIPV6Address(CatCmdMessage cmdMsg) {
        if (cmdMsg == null || CommandType.OPEN_CHANNEL != cmdMsg.getCmdType()) {
            return false;
        }
        ChannelSettings newChannel = cmdMsg.getChannelSettings();
        if (newChannel == null || newChannel.destinationAddress == null || 4 == newChannel.destinationAddress.length) {
            return false;
        }
        return true;
    }

    /* JADX WARNING: Missing block: B:9:0x0018, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isDnsServerAddressRequested(CatCmdMessage cmdMsg) {
        if (cmdMsg != null && CommandType.OPEN_CHANNEL == cmdMsg.getCmdType() && 8 == (cmdMsg.getCommandQualifier() & 8)) {
            return true;
        }
        return false;
    }

    private void updateWifiAvailableFlag(CatCmdMessage cmdMsg) {
        if (cmdMsg == null) {
            CatLog.d((Object) this, "updateWifiAvailableFlag, input param invalid!");
        } else if (!cmdMsg.getWifiConnectedFlag()) {
            CatLog.d((Object) this, "updateWifiAvailableFlag, getWifiConnectedFlag is false, just return!");
        } else if (CommandType.OPEN_CHANNEL != cmdMsg.getCmdType()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateWifiAvailableFlag to false, cmdType is ");
            stringBuilder.append(cmdMsg.getCmdType());
            CatLog.d((Object) this, stringBuilder.toString());
            cmdMsg.setWifiConnectedFlag(false);
        } else if (isDnsServerAddressRequested(cmdMsg)) {
            CatLog.d((Object) this, "updateWifiAvailableFlag to false, DNS server address(es) requested!");
            cmdMsg.setWifiConnectedFlag(false);
        } else if (!isBipOverWlanAllowed()) {
            CatLog.d((Object) this, "updateWifiAvailableFlag to false, isBipOverWlanAllowed is false");
            cmdMsg.setWifiConnectedFlag(false);
        } else if (!isDefaultBearerDescriptionType(cmdMsg)) {
            CatLog.d((Object) this, "updateWifiAvailableFlag to false, not default_bearer!");
            cmdMsg.setWifiConnectedFlag(false);
        } else if (isIPV6Address(cmdMsg)) {
            CatLog.d((Object) this, "updateWifiAvailableFlag to false, IPV6!");
            cmdMsg.setWifiConnectedFlag(false);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:60:0x017c  */
    /* JADX WARNING: Removed duplicated region for block: B:58:0x016f  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void handleBipCommand(CatCmdMessage cmdMsg) {
        CatCmdMessage catCmdMessage = cmdMsg;
        int i = 0;
        if (catCmdMessage == null) {
            CatLog.d(this, "handleBipCommand null cmdMsg");
            while (i < this.mBipChannels.length) {
                if (this.mBipChannels[i] != null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("handleBipCommand handle channel ");
                    stringBuilder.append(i);
                    stringBuilder.append(" session end");
                    CatLog.d(this, stringBuilder.toString());
                    this.mBipChannels[i].onSessionEnd();
                }
                i++;
            }
            return;
        }
        CommandType curCmdType = cmdMsg.getCmdType();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("handleBipCommand curCmdType: ");
        stringBuilder2.append(curCmdType);
        stringBuilder2.append(", channelSettings: ");
        stringBuilder2.append(cmdMsg.getChannelSettings());
        stringBuilder2.append(", cmd_qual: ");
        stringBuilder2.append(cmdMsg.getCommandQualifier());
        stringBuilder2.append(", dataSettings: ");
        stringBuilder2.append(cmdMsg.getDataSettings());
        CatLog.d(this, stringBuilder2.toString());
        updateWifiAvailableFlag(cmdMsg);
        int i2;
        StringBuilder stringBuilder3;
        switch (curCmdType) {
            case OPEN_CHANNEL:
                ChannelSettings channelSettings = cmdMsg.getChannelSettings();
                if (channelSettings != null) {
                    acquireWakeLock();
                    if (allChannelsClosed()) {
                        this.mDefaultBearerStateReceiver.startListening();
                    }
                    for (i2 = 0; i2 < this.mBipChannels.length; i2++) {
                        if (this.mBipChannels[i2] == null) {
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("mBipChannels ");
                            stringBuilder3.append(i2);
                            stringBuilder3.append(" is available");
                            CatLog.d(this, stringBuilder3.toString());
                            channelSettings.channel = i2 + 1;
                            if (channelSettings.channel != 0) {
                                this.mStkService.sendTerminalResponse(catCmdMessage.mCmdDet, ResultCode.BIP_ERROR, true, 1, null);
                                return;
                            }
                            switch (channelSettings.protocol) {
                                case TCP_SERVER:
                                    this.mStkService.sendTerminalResponse(catCmdMessage.mCmdDet, ResultCode.CMD_DATA_NOT_UNDERSTOOD, false, 0, null);
                                    return;
                                case TCP_CLIENT_REMOTE:
                                case TCP_CLIENT_LOCAL:
                                    this.mBipChannels[channelSettings.channel - 1] = new TcpClientChannel();
                                    break;
                                case UDP_CLIENT_REMOTE:
                                case UDP_CLIENT_LOCAL:
                                    this.mBipChannels[channelSettings.channel - 1] = new UdpClientChannel();
                                    break;
                                default:
                                    CatLog.d(this, "invalid protocol found");
                                    this.mStkService.sendTerminalResponse(catCmdMessage.mCmdDet, ResultCode.CMD_DATA_NOT_UNDERSTOOD, false, 0, null);
                                    return;
                            }
                            if ((cmdMsg.getCommandQualifier() & 1) == 0) {
                                this.mImmediateLinkEstablish = false;
                                this.openChCmdMsg = catCmdMessage;
                                this.mBipChannels[channelSettings.channel - 1].preProcessOpen(catCmdMessage);
                            } else {
                                this.mImmediateLinkEstablish = true;
                                if (setupDataConnection(cmdMsg)) {
                                    CatLog.d(this, "Continue processing open channel");
                                    this.mBipChannels[channelSettings.channel - 1].preProcessOpen(catCmdMessage);
                                    if (!this.mBipChannels[channelSettings.channel - 1].open(catCmdMessage)) {
                                        CatLog.d(this, "open channel failed");
                                        cleanupBipChannel(channelSettings.channel);
                                    }
                                } else {
                                    CatLog.d(this, "handleBipCommand :setupDataConnection returned");
                                }
                            }
                            return;
                        }
                    }
                    if (channelSettings.channel != 0) {
                    }
                }
                break;
            case SEND_DATA:
            case RECEIVE_DATA:
            case CLOSE_CHANNEL:
                if (cmdMsg.getDataSettings() != null) {
                    try {
                        BipChannel curChannel = this.mBipChannels[cmdMsg.getDataSettings().channel - 1];
                        if (curChannel != null) {
                            if (CommandType.SEND_DATA != curCmdType) {
                                if (CommandType.RECEIVE_DATA != curCmdType) {
                                    if (CommandType.CLOSE_CHANNEL != curCmdType) {
                                        break;
                                    }
                                    clearWakeLock();
                                    curChannel.close(catCmdMessage);
                                    cleanupBipChannel(cmdMsg.getDataSettings().channel);
                                    return;
                                }
                                curChannel.receive(catCmdMessage);
                                return;
                            }
                            curChannel.send(catCmdMessage);
                            return;
                        }
                        this.mStkService.sendTerminalResponse(catCmdMessage.mCmdDet, ResultCode.BIP_ERROR, true, 3, null);
                        CatLog.d(this, "handleBipCommand, There is not open channel");
                        return;
                    } catch (ArrayIndexOutOfBoundsException e) {
                        this.mStkService.sendTerminalResponse(catCmdMessage.mCmdDet, ResultCode.BIP_ERROR, true, 3, null);
                        CatLog.d(this, "handleBipCommand error");
                        return;
                    }
                }
                break;
            case GET_CHANNEL_STATUS:
                int[] status = new int[7];
                for (i2 = 0; i2 < 7; i2++) {
                    if (this.mBipChannels[i2] != null) {
                        status[i2] = this.mBipChannels[i2].getStatus();
                    } else {
                        status[i2] = 0;
                    }
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("get channel status = ");
                    stringBuilder3.append(status[i2]);
                    CatLog.d(this, stringBuilder3.toString());
                }
                this.mStkService.sendTerminalResponse(catCmdMessage.mCmdDet, ResultCode.OK, false, 0, new ChannelStatusResponseData(status));
                return;
        }
        this.mStkService.sendTerminalResponse(catCmdMessage.mCmdDet, ResultCode.CMD_DATA_NOT_UNDERSTOOD, false, 0, null);
    }

    private boolean allChannelsClosed() {
        for (BipChannel channel : this.mBipChannels) {
            if (channel != null) {
                CatLog.d((Object) this, "not all Channels Closed");
                return false;
            }
        }
        CatLog.d((Object) this, "all Channels Closed");
        return true;
    }

    private void cleanupBipChannel(int channel) {
        this.mBipChannels[channel - 1] = null;
        if (allChannelsClosed()) {
            this.mDefaultBearerStateReceiver.stopListening();
        }
    }

    private void sendChannelStatusEvent(int channelStatus) {
        byte[] additionalInfo = new byte[]{(byte) -72, (byte) 2, (byte) 0, (byte) 0};
        additionalInfo[2] = (byte) ((channelStatus >> 8) & 255);
        additionalInfo[3] = (byte) (channelStatus & 255);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendChannelStatusEvent channelStatus = ");
        stringBuilder.append(channelStatus);
        CatLog.d((Object) this, stringBuilder.toString());
        this.mStkService.onEventDownload(new CatEventMessage(EventCode.CHANNEL_STATUS.value(), additionalInfo, true));
    }

    private void sendDataAvailableEvent(int channelStatus, int dataAvailable) {
        byte[] additionalInfo = new byte[]{(byte) -72, (byte) 2, (byte) 0, (byte) 0, (byte) -73, (byte) 1, (byte) 0};
        additionalInfo[2] = (byte) ((channelStatus >> 8) & 255);
        additionalInfo[3] = (byte) (channelStatus & 255);
        additionalInfo[6] = (byte) (dataAvailable & 255);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendDataAvailableEvent channelStatus = ");
        stringBuilder.append(channelStatus);
        stringBuilder.append(" dataAvailable = ");
        stringBuilder.append(dataAvailable);
        CatLog.d((Object) this, stringBuilder.toString());
        this.mStkService.onEventDownload(new CatEventMessage(EventCode.DATA_AVAILABLE.value(), additionalInfo, true));
    }

    private boolean checkExistingCsCallInNetworkClass2G() {
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
        int networkType = tm.getNetworkType();
        int networkClass = TelephonyManager.getNetworkClass(networkType);
        if ((1 != networkClass && networkClass != 0) || tm.getCallState() == 0) {
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bearer not setup, busy on voice call, networkClass = ");
        stringBuilder.append(networkClass);
        stringBuilder.append(" networkType = ");
        stringBuilder.append(networkType);
        CatLog.d((Object) this, stringBuilder.toString());
        return true;
    }

    private String getLguPlusOtaApn() {
        return SystemProperties.get("ro.config.lgu_plus_ota_apn", "ota.lguplus.co.kr");
    }

    private boolean isLguPlusOtaEnable() {
        return SystemProperties.getBoolean("ro.config.hw_enable_ota_bip_lgu", false);
    }

    private String formatDefaultApn(CatCmdMessage cmdMsg) {
        ChannelSettings newChannel = cmdMsg.getChannelSettings();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("formatDefaultApn, mStkService.OTA_TYPE:");
        stringBuilder.append(this.mStkService.OTA_TYPE);
        CatLog.d((Object) this, stringBuilder.toString());
        if (!isLguPlusOtaEnable()) {
            return "default";
        }
        if (this.mStkService.OTA_TYPE != 0) {
            return "default";
        }
        if (newChannel.networkAccessName == null) {
            newChannel.networkAccessName = getLguPlusOtaApn();
        }
        if (newChannel.userLogin == null) {
            newChannel.userLogin = "";
        }
        if (newChannel.userPassword == null) {
            newChannel.userPassword = "";
        }
        String apnString = new StringBuilder();
        apnString.append("bipapn, ");
        apnString.append(newChannel.networkAccessName);
        apnString.append(", ,");
        apnString.append(String.valueOf(newChannel.port));
        apnString.append(", ");
        apnString.append(newChannel.userLogin);
        apnString.append(", ");
        apnString.append(newChannel.userPassword);
        apnString.append(", , , , , , ,3 , ");
        apnString.append(this.mChannelApnInfo[newChannel.channel - 1].type);
        apnString = apnString.toString();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("formatDefaultApn, apnString:");
        stringBuilder2.append(apnString);
        CatLog.d((Object) this, stringBuilder2.toString());
        return apnString;
    }

    private boolean setupDefaultDataConnection(CatCmdMessage cmdMsg) throws ConnectionSetupFailedException {
        ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        ChannelSettings newChannel = cmdMsg.getChannelSettings();
        if (checkExistingCsCallInNetworkClass2G()) {
            this.mStkService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 2, new OpenChannelResponseData(newChannel.bufSize, null, newChannel.bearerDescription));
            throw new ConnectionSetupFailedException("Busy on voice call");
        }
        this.mChannelApnInfo[newChannel.channel - 1] = new ChannelApnInfo(newChannel.channel, cmdMsg);
        this.mIsWifiConnected[newChannel.channel - 1] = cmdMsg.getWifiConnectedFlag();
        String apnString = formatDefaultApn(cmdMsg);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("IS_vz = ");
        stringBuilder.append(IS_VZ);
        CatLog.d((Object) this, stringBuilder.toString());
        if (true == IS_VZ) {
            SystemProperties.set("gsm.bip.apn", null);
        } else {
            SystemProperties.set("gsm.bip.apn", apnString);
        }
        CatLog.d((Object) this, "setupDefaultDataConnection");
        NetworkRequest request = new Builder().addTransportType(0).addCapability(getBipCapability(this.mChannelApnInfo[newChannel.channel - 1].feature)).build();
        this.mChannelApnInfo[newChannel.channel - 1].networkCallback = new BipNetworkCallback(this.mChannelApnInfo[newChannel.channel - 1].networkType, cmdMsg);
        int mSlotId = cmdMsg.getSlotId();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("setupDefaultDataConnection  mSlotId = ");
        stringBuilder2.append(mSlotId);
        CatLog.d((Object) this, stringBuilder2.toString());
        if (mSlotId > -1 && mSlotId < SIM_NUM) {
            String subid = String.valueOf(mSlotId);
            request.networkCapabilities.setNetworkSpecifier(new StringNetworkSpecifier(subid));
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("setupDefaultDataConnection  mSlotId = ");
            stringBuilder3.append(mSlotId);
            stringBuilder3.append(" subid = ");
            stringBuilder3.append(subid);
            CatLog.d((Object) this, stringBuilder3.toString());
        }
        cm.requestNetwork(request, this.mChannelApnInfo[newChannel.channel - 1].networkCallback);
        startDataConnectionTimer(cmdMsg);
        return false;
    }

    private boolean setupSpecificPdpConnection(CatCmdMessage cmdMsg) throws ConnectionSetupFailedException {
        ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        ChannelSettings newChannel = cmdMsg.getChannelSettings();
        if (newChannel.networkAccessName == null) {
            CatLog.d((Object) this, "no accessname for PS bearer req");
            return setupDefaultDataConnection(cmdMsg);
        } else if (checkExistingCsCallInNetworkClass2G()) {
            this.mStkService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 2, new OpenChannelResponseData(newChannel.bufSize, null, newChannel.bearerDescription));
            throw new ConnectionSetupFailedException("Busy on voice call");
        } else {
            String apnstring;
            CatLog.d((Object) this, "Detected new data connection parameters");
            boolean userbipSetting = this.mHwCustBipProxy != null && this.mHwCustBipProxy.kddiBipOtaEnable();
            if (newChannel.userLogin == null) {
                newChannel.userLogin = userbipSetting ? "au" : "";
            }
            if (newChannel.userPassword == null) {
                newChannel.userPassword = userbipSetting ? "au" : "";
            }
            this.mChannelApnInfo[newChannel.channel - 1] = new ChannelApnInfo(newChannel.channel, cmdMsg);
            this.mIsWifiConnected[newChannel.channel - 1] = cmdMsg.getWifiConnectedFlag();
            if (userbipSetting) {
                apnstring = this.mHwCustBipProxy.getApnString(newChannel, this.mChannelApnInfo[newChannel.channel - 1].type);
            } else {
                apnstring = getApnString(newChannel);
            }
            SystemProperties.set("gsm.bip.apn", apnstring);
            CatLog.d((Object) this, "setupSpecificPdpConnection");
            NetworkRequest request = new Builder().addTransportType(0).addCapability(getBipCapability(this.mChannelApnInfo[newChannel.channel - 1].feature)).build();
            this.mChannelApnInfo[newChannel.channel - 1].networkCallback = new BipNetworkCallback(this.mChannelApnInfo[newChannel.channel - 1].networkType, cmdMsg);
            cm.requestNetwork(request, this.mChannelApnInfo[newChannel.channel - 1].networkCallback);
            startDataConnectionTimer(cmdMsg);
            return false;
        }
    }

    public String getApnString(ChannelSettings newChannel) {
        StringBuilder apnstring = new StringBuilder("bipapn, ");
        apnstring.append(newChannel.networkAccessName);
        apnstring.append(", ,");
        apnstring.append(String.valueOf(newChannel.port));
        apnstring.append(", ");
        apnstring.append(newChannel.userLogin);
        apnstring.append(", ");
        apnstring.append(newChannel.userPassword);
        apnstring.append(", , , , , , ,3 , ");
        apnstring.append(this.mChannelApnInfo[newChannel.channel - 1].type);
        return apnstring.toString();
    }

    private int getBipCapability(String feature) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("feature: ");
        stringBuilder.append(feature);
        CatLog.d((Object) this, stringBuilder.toString());
        if (AbstractPhoneInternalInterface.FEATURE_ENABLE_BIP0.equals(feature)) {
            return 23;
        }
        if (AbstractPhoneInternalInterface.FEATURE_ENABLE_BIP1.equals(feature)) {
            return 24;
        }
        if (AbstractPhoneInternalInterface.FEATURE_ENABLE_BIP2.equals(feature)) {
            return 25;
        }
        if (AbstractPhoneInternalInterface.FEATURE_ENABLE_BIP3.equals(feature)) {
            return 26;
        }
        if (AbstractPhoneInternalInterface.FEATURE_ENABLE_BIP4.equals(feature)) {
            return 27;
        }
        if (AbstractPhoneInternalInterface.FEATURE_ENABLE_BIP5.equals(feature)) {
            return 28;
        }
        if (AbstractPhoneInternalInterface.FEATURE_ENABLE_BIP6.equals(feature)) {
            return 29;
        }
        return 23;
    }

    private int getChannelId(CatCmdMessage cmdMsg) {
        int channel = 0;
        if (cmdMsg.getCmdType() == CommandType.OPEN_CHANNEL) {
            channel = cmdMsg.getChannelSettings().channel;
        } else if (cmdMsg.getCmdType() == CommandType.CLOSE_CHANNEL || cmdMsg.getCmdType() == CommandType.RECEIVE_DATA || cmdMsg.getCmdType() == CommandType.SEND_DATA) {
            channel = cmdMsg.getDataSettings().channel;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getChannelId:");
        stringBuilder.append(channel);
        CatLog.d((Object) this, stringBuilder.toString());
        return channel;
    }

    private synchronized void checkSetStatusOrNot(CatCmdMessage cmdMsg) {
        StringBuilder stringBuilder;
        int channel = getChannelId(cmdMsg);
        int index = channel - 1;
        BipChannel tempChannel = this.mBipChannels[index];
        if (channel > 0) {
            if (channel <= 7) {
                if (tempChannel == null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("checkSetStatusOrNot, mBipChannel[");
                    stringBuilder.append(index);
                    stringBuilder.append("] is null, just return");
                    CatLog.d((Object) this, stringBuilder.toString());
                    return;
                } else if (5 == (tempChannel.getStatus() & 255)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("checkSetStatusOrNot, mBipChannel[");
                    stringBuilder.append(index);
                    stringBuilder.append("] already link droped, just return");
                    CatLog.d((Object) this, stringBuilder.toString());
                    return;
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("checkSetStatusOrNot, mBipChannel[");
                    stringBuilder.append(index);
                    stringBuilder.append("] CH_STATUS_LINK_DROP");
                    CatLog.d((Object) this, stringBuilder.toString());
                    tempChannel.setStatus(5);
                    return;
                }
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("checkSetStatusOrNot, channel_id");
        stringBuilder.append(index);
        stringBuilder.append("is invalid, just return");
        CatLog.d((Object) this, stringBuilder.toString());
    }

    private boolean setupDataConnection(CatCmdMessage cmdMsg) {
        boolean result = false;
        ChannelSettings newChannel = cmdMsg.getChannelSettings();
        if (newChannel.protocol == TransportProtocol.TCP_CLIENT_REMOTE || newChannel.protocol == TransportProtocol.UDP_CLIENT_REMOTE) {
            BearerDescription bd = newChannel.bearerDescription;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("bd.type = ");
            stringBuilder.append(bd.type);
            stringBuilder.append(", isWifiConnectedFlag = ");
            stringBuilder.append(cmdMsg.getWifiConnectedFlag());
            CatLog.d((Object) this, stringBuilder.toString());
            if (cmdMsg.getWifiConnectedFlag()) {
                this.mChannelApnInfo[newChannel.channel - 1] = new ChannelApnInfo(newChannel.channel, cmdMsg);
                this.mIsWifiConnected[newChannel.channel - 1] = cmdMsg.getWifiConnectedFlag();
                return true;
            }
            try {
                if (BearerType.DEFAULT_BEARER == bd.type) {
                    result = (this.mHwCustBipProxy == null || !this.mHwCustBipProxy.kddiBipOtaEnable()) ? setupDefaultDataConnection(cmdMsg) : setupSpecificPdpConnection(cmdMsg);
                } else {
                    if (!(BearerType.MOBILE_PS == bd.type || BearerType.MOBILE_PS_EXTENDED_QOS == bd.type)) {
                        if (BearerType.E_UTRAN != bd.type) {
                            CatLog.d((Object) this, "Unsupported bearer type");
                            this.mStkService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.BEYOND_TERMINAL_CAPABILITY, false, 0, null);
                        }
                    }
                    result = setupSpecificPdpConnection(cmdMsg);
                }
            } catch (ConnectionSetupFailedException csfe) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setupDataConnection failed: ");
                stringBuilder2.append(csfe.getMessage());
                CatLog.d((Object) this, stringBuilder2.toString());
                this.mBipChannels[newChannel.channel - 1] = null;
                cleanupBipChannel(newChannel.channel);
            }
            return result;
        }
        CatLog.d((Object) this, "No data connection needed for this channel");
        return true;
    }

    private synchronized boolean teardownDataConnection(CatCmdMessage cmdMsg) {
        StringBuilder stringBuilder;
        ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        int channel = getChannelId(cmdMsg);
        if (channel > 0) {
            if (channel <= 7) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("teardownDataConnection channel = ");
                stringBuilder.append(channel);
                CatLog.d((Object) this, stringBuilder.toString());
                if (this.mChannelApnInfo[channel - 1] == null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("teardownDataConnection mChannelApnInfo[");
                    stringBuilder.append(channel - 1);
                    stringBuilder.append("] is null");
                    CatLog.d((Object) this, stringBuilder.toString());
                    return false;
                }
                if (!cmdMsg.getWifiConnectedFlag()) {
                    CatLog.d((Object) this, "teardownDataConnection begin");
                    if (!(this.mChannelApnInfo[channel - 1] == null || this.mChannelApnInfo[channel - 1].networkCallback == null)) {
                        cm.unregisterNetworkCallback(this.mChannelApnInfo[channel - 1].networkCallback);
                        this.mChannelApnInfo[channel - 1].networkCallback = null;
                        CatLog.d((Object) this, "unregisterNetworkCallback");
                    }
                    CatLog.d((Object) this, "teardownDataConnection end");
                }
                cleanChannelApnInfo(channel);
                return true;
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("teardownDataConnection, channel_id");
        stringBuilder.append(channel);
        stringBuilder.append("is invalid, just return");
        CatLog.d((Object) this, stringBuilder.toString());
        return false;
    }

    private void onSetupConnectionCompleted(AsyncResult ar) {
        if (ar == null) {
            CatLog.d((Object) this, "onSetupConnectionCompleted ar null");
            return;
        }
        CatCmdMessage cmdMsg = ar.userObj;
        StringBuilder stringBuilder;
        if (ar.exception != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to setup data connection for channel: ");
            stringBuilder.append(cmdMsg.getChannelSettings().channel);
            CatLog.d((Object) this, stringBuilder.toString());
            this.mStkService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.NETWORK_CRNTLY_UNABLE_TO_PROCESS, false, 0, new OpenChannelResponseData(cmdMsg.getChannelSettings().bufSize, null, cmdMsg.getChannelSettings().bearerDescription));
            cleanupBipChannel(cmdMsg.getChannelSettings().channel);
            cleanChannelApnInfo(cmdMsg.getChannelSettings().channel);
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("setup data connection for channel: ");
            stringBuilder.append(cmdMsg.getChannelSettings().channel);
            CatLog.d((Object) this, stringBuilder.toString());
            if (this.mChannelApnInfo[cmdMsg.getChannelSettings().channel - 1] == null || this.mChannelApnInfo[cmdMsg.getChannelSettings().channel - 1].networkType == 0) {
                CatLog.d((Object) this, "Succeeded to setup data connection for channel - Default bearer");
            }
            CatLog.d((Object) this, "Continue processing open channel");
            this.mBipChannels[cmdMsg.getChannelSettings().channel - 1].preProcessOpen(cmdMsg);
            if (!this.mBipChannels[cmdMsg.getChannelSettings().channel - 1].open(cmdMsg)) {
                CatLog.d((Object) this, "fail to open channel");
                cleanupBipChannel(cmdMsg.getChannelSettings().channel);
            }
        }
    }

    private void onTeardownConnectionCompleted(AsyncResult ar) {
        if (ar == null) {
            CatLog.d((Object) this, "onTeardownConnectionCompleted ar null");
            return;
        }
        int channel;
        CatCmdMessage cmdMsg = ar.userObj;
        if (cmdMsg.getCmdType() == CommandType.OPEN_CHANNEL) {
            channel = cmdMsg.getChannelSettings().channel;
        } else if (cmdMsg.getCmdType() == CommandType.CLOSE_CHANNEL) {
            channel = cmdMsg.getDataSettings().channel;
        } else {
            return;
        }
        StringBuilder stringBuilder;
        if (ar.exception != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to teardown data connection for channel ");
            stringBuilder.append(channel);
            stringBuilder.append(": ");
            stringBuilder.append(ar.exception.getMessage());
            CatLog.d((Object) this, stringBuilder.toString());
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Succedded to teardown data connection for channel: ");
            stringBuilder.append(channel);
            CatLog.d((Object) this, stringBuilder.toString());
            for (int i = 0; i < 7; i++) {
                if (this.mBipChannels[i] != null) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("channel ");
                    stringBuilder2.append(i);
                    stringBuilder2.append(" link drop");
                    CatLog.d((Object) this, stringBuilder2.toString());
                    this.mBipChannels[i].setStatus(5);
                }
            }
        }
        cleanupBipChannel(channel);
    }

    private void startDataConnectionTimer(CatCmdMessage cmdMsg) {
        cancelDataConnectionTimer();
        CatLog.d((Object) this, "startDataConnectionTimer.");
        sendMessageDelayed(obtainMessage(100, cmdMsg), 180000);
    }

    private void cancelDataConnectionTimer() {
        CatLog.d((Object) this, "cancelDataConnectionTimer.");
        removeMessages(100);
    }

    private void acquireWakeLock() {
        synchronized (this.mWakeLock) {
            CatLog.d((Object) this, "acquireWakeLock.");
            this.mWakeLock.acquire();
            removeMessages(99);
            sendMessageDelayed(obtainMessage(99), (long) this.mWakeLockTimeout);
        }
    }

    private void clearWakeLock() {
        synchronized (this.mWakeLock) {
            if (this.mWakeLock.isHeld()) {
                CatLog.d((Object) this, "clearWakeLock.");
                this.mWakeLock.release();
                removeMessages(99);
            }
        }
    }

    public void handleMessage(Message msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleMessage: ");
        stringBuilder.append(msg.what);
        CatLog.d((Object) this, stringBuilder.toString());
        switch (msg.what) {
            case 10:
                cancelDataConnectionTimer();
                if (msg.obj != null) {
                    onSetupConnectionCompleted((AsyncResult) msg.obj);
                    return;
                }
                return;
            case 11:
                if (msg.obj != null) {
                    onTeardownConnectionCompleted((AsyncResult) msg.obj);
                    return;
                }
                return;
            case 99:
                clearWakeLock();
                return;
            case 100:
                CatCmdMessage cmdMsg = msg.obj;
                CatLog.d((Object) this, "EVENT_DC_TIMEOUT teardownDataConnection");
                teardownDataConnection(cmdMsg);
                this.mStkService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.BEYOND_TERMINAL_CAPABILITY, false, 0, null);
                return;
            default:
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unrecognized message: ");
                stringBuilder2.append(msg.what);
                throw new AssertionError(stringBuilder2.toString());
        }
    }

    public void cleanChannelApnInfo(int channel) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("cleanChannelApnInfo, channel: ");
        stringBuilder.append(channel);
        CatLog.d((Object) this, stringBuilder.toString());
        this.mChannelApnInfo[channel - 1] = null;
    }
}
