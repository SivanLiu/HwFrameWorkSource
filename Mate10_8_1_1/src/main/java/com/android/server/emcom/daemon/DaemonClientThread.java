package com.android.server.emcom.daemon;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.LocalSocketAddress.Namespace;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.util.Log;
import android.util.SparseArray;
import com.android.server.emcom.FeatureManager;
import com.android.server.emcom.daemon.DaemonCommand.DaemonReportCallback;
import java.io.IOException;
import java.io.InputStream;

public class DaemonClientThread extends Thread {
    public static final int DAEMON_NOT_AVAILABLE = -1;
    static final int EVENT_SEND = 1;
    static final int EVENT_WAKE_LOCK_TIMEOUT = 2;
    static final int MAX_COMMAND_BYTES = 8192;
    public static final int RESPONSE_REPLY = 0;
    public static final int RESPONSE_REPORT = 1;
    public static final String SOCKET_ADDRESS = "emcomd";
    static final int SOCKET_OPEN_RETRY_MILLIS = 4000;
    public static final String TAG = "DaemonClientThread";
    protected DaemonClientReceiver mReceiver = new DaemonClientReceiver();
    protected Thread mReceiverThread = new Thread(this.mReceiver, DaemonClientReceiver.LOG_TAG);
    private DaemonReportCallback mReportCb = null;
    SparseArray<DaemonRequest> mRequestList = new SparseArray();
    private DaemonClientSender mSender = null;
    private LocalSocket mSocket = null;

    public class DaemonClientReceiver implements Runnable {
        public static final String LOG_TAG = "DaemonClientReceiver";
        private byte[] mBuffer = new byte[8192];

        public void run() {
            Throwable tr;
            int retryCount = 0;
            while (true) {
                LocalSocket localSocket = null;
                try {
                    LocalSocket s = new LocalSocket();
                    try {
                        s.connect(new LocalSocketAddress(DaemonClientThread.SOCKET_ADDRESS, Namespace.RESERVED));
                        retryCount = 0;
                        DaemonClientThread.this.mSocket = s;
                        Log.i(LOG_TAG, "Connected to 'emcomd' socket");
                        int length = 0;
                        try {
                            InputStream is = DaemonClientThread.this.mSocket.getInputStream();
                            while (true) {
                                length = DaemonClientThread.readMessage(is, this.mBuffer);
                                if (length >= 0) {
                                    Parcel p = Parcel.obtain();
                                    p.unmarshall(this.mBuffer, 0, length);
                                    p.setDataPosition(0);
                                    Log.d(LOG_TAG, "read packet " + length + " bytes");
                                    DaemonClientThread.this.processResponse(p);
                                    p.recycle();
                                }
                                break;
                            }
                        } catch (IOException ex) {
                            Log.i(LOG_TAG, "'emcomd' socket closed", ex);
                        } catch (Throwable th) {
                            tr = th;
                            localSocket = s;
                        }
                        Log.i(LOG_TAG, "Disconnected from 'emcomd' socket");
                        try {
                            DaemonClientThread.this.mSocket.close();
                        } catch (IOException ex2) {
                            Log.e(LOG_TAG, "close socket error.", ex2);
                        }
                        DaemonClientThread.this.mSocket = null;
                        DaemonRequest.resetSerial();
                        DaemonClientThread.this.clearRequestList(-1);
                    } catch (IOException e) {
                        localSocket = s;
                        if (localSocket != null) {
                            try {
                                localSocket.close();
                            } catch (IOException e2) {
                            }
                        }
                        if (retryCount != 8) {
                            Log.e(LOG_TAG, "Couldn't find 'emcomd' socket after " + retryCount + " times, continuing to retry silently");
                        } else if (retryCount >= 0 && retryCount < 8) {
                            try {
                                Log.i(LOG_TAG, "Couldn't find 'emcomd' socket; retrying after timeout");
                            } catch (Throwable th2) {
                                tr = th2;
                            }
                        }
                        try {
                            Thread.sleep(4000);
                        } catch (InterruptedException e3) {
                        }
                        retryCount++;
                    }
                } catch (IOException e4) {
                    if (localSocket != null) {
                        localSocket.close();
                    }
                    if (retryCount != 8) {
                        Log.i(LOG_TAG, "Couldn't find 'emcomd' socket; retrying after timeout");
                    } else {
                        Log.e(LOG_TAG, "Couldn't find 'emcomd' socket after " + retryCount + " times, continuing to retry silently");
                    }
                    Thread.sleep(4000);
                    retryCount++;
                }
            }
            Log.e(LOG_TAG, "Uncaught exception", tr);
        }
    }

    public class DaemonClientSender extends Handler implements Runnable {
        public static final String LOG_TAG = "DaemonClientSender";
        byte[] dataLength = new byte[4];

        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "receive " + msg.what);
            DaemonRequest rr = msg.obj;
            switch (msg.what) {
                case 1:
                    try {
                        LocalSocket s = DaemonClientThread.this.mSocket;
                        if (s != null) {
                            synchronized (DaemonClientThread.this.mRequestList) {
                                DaemonClientThread.this.mRequestList.append(rr.mSerial, rr);
                            }
                            byte[] data = rr.mParcel.marshall();
                            rr.mParcel.recycle();
                            rr.mParcel = null;
                            if (data.length <= 8192) {
                                byte[] bArr = this.dataLength;
                                this.dataLength[1] = (byte) 0;
                                bArr[0] = (byte) 0;
                                this.dataLength[2] = (byte) ((data.length >> 8) & 255);
                                this.dataLength[3] = (byte) (data.length & 255);
                                Log.d(LOG_TAG, "writing packet " + data.length + " bytes");
                                s.getOutputStream().write(this.dataLength);
                                s.getOutputStream().write(data);
                                break;
                            }
                            throw new RuntimeException("Parcel larger than max bytes allowed !" + data.length);
                        }
                        rr.onError(-1);
                        rr.release();
                        return;
                    } catch (IOException ex) {
                        Log.d(LOG_TAG, "IOException  " + ex);
                        if (DaemonClientThread.this.findAndRemoveRequestFromList(rr.mSerial) != null) {
                            rr.onError(-1);
                            rr.release();
                            return;
                        }
                    } catch (RuntimeException exc) {
                        Log.d(LOG_TAG, "Uncaught exception " + exc);
                        if (DaemonClientThread.this.findAndRemoveRequestFromList(rr.mSerial) != null) {
                            rr.onError(-1);
                            rr.release();
                            return;
                        }
                    }
                    break;
            }
        }

        public void run() {
        }
    }

    public DaemonClientThread() {
        this.mReceiverThread.start();
        this.mSender = new DaemonClientSender();
    }

    private void processResponse(Parcel p) {
        int type = p.readInt();
        Log.d(TAG, "read processResponse  type = " + type);
        switch (type) {
            case 0:
                DaemonRequest rr = processReply(p, type);
                if (rr != null) {
                    rr.release();
                    return;
                }
                break;
            case 1:
                processReport(p);
                break;
        }
    }

    private void processReport(Parcel p) {
        int commnd = p.readInt();
        Log.d(TAG, "  processReport commnd   = " + commnd);
        switch (commnd) {
            case 1:
                int length = p.readInt();
                int featureValue = p.readInt();
                Log.d(TAG, "the featureValue is " + featureValue + ", length is " + length);
                FeatureManager.getInstance();
                FeatureManager.setFeatureValue(featureValue);
                return;
            case 2:
                this.mReportCb.onReportDevFail();
                return;
            case 513:
                this.mReportCb.onUpdateBrowserInfo(p);
                return;
            case CommandsInterface.EMCOM_DS_HTTP_INFO /*514*/:
                this.mReportCb.onUpdateHttpInfo(p);
                return;
            case 515:
                this.mReportCb.onUpdateTcpStatusInfo(p);
                return;
            case CommandsInterface.EMCOM_DS_SAMPLE_WIN_STATS /*516*/:
                this.mReportCb.onUpdateSampleWinStat(p.readInt() != 0);
                return;
            case CommandsInterface.EMCOM_DS_PAGE_ID /*517*/:
                this.mReportCb.onUpdatePageId(p.readInt());
                return;
            case CommandsInterface.EMCOM_DS_APP_LIST /*518*/:
                this.mReportCb.onUpdateAppList(p);
                return;
            default:
                return;
        }
    }

    public void registerDaemonCallback(DaemonReportCallback cb) {
        this.mReportCb = cb;
    }

    public void unRegisterDaemonCallback(DaemonReportCallback cb) {
        this.mReportCb = null;
    }

    private DaemonRequest processReply(Parcel p, int type) {
        int Serial = p.readInt();
        int errorNo = p.readInt();
        Log.d(TAG, "  Serial   = " + Serial + "   errorNo  = " + errorNo);
        DaemonRequest rr = findAndRemoveRequestFromList(Serial);
        if (rr == null) {
            Log.d(TAG, "  Unexpected solicited response! sn:   = " + Serial + "   errorNo  = " + errorNo);
            return null;
        }
        Log.d(TAG, "  processReply request is " + rr.mRequest);
        if (errorNo == 0) {
            switch (rr.mRequest) {
                case 1:
                case 2:
                case 3:
                case 4:
                case CommandsInterface.EMCOM_SD_XENGINE_START_ACC /*257*/:
                case CommandsInterface.EMCOM_SD_XENGINE_STOP_ACC /*258*/:
                    Log.d(TAG, "  processReply result is " + p.readInt());
                    break;
                case 5:
                    int num = p.readInt();
                    int result = p.readInt();
                    FeatureManager.getInstance();
                    FeatureManager.setFeatureValue(result);
                    Log.d(TAG, "  processReply result is " + result + ", num is " + num);
                    break;
            }
            if (rr.mResult != null) {
                AsyncResult.forMessage(rr.mResult, null, null);
                rr.mResult.sendToTarget();
            }
        }
        if (errorNo != 0) {
            rr.onError(errorNo);
            rr.release();
        }
        return rr;
    }

    public void send(DaemonRequest rr) {
        if (this.mSocket == null) {
            rr.onError(-1);
            rr.release();
            return;
        }
        Message msg = this.mSender.obtainMessage(1, rr);
        Log.d(TAG, "send  " + msg.what);
        msg.sendToTarget();
    }

    private DaemonRequest findAndRemoveRequestFromList(int serial) {
        DaemonRequest rr;
        synchronized (this.mRequestList) {
            rr = (DaemonRequest) this.mRequestList.get(serial);
            if (rr != null) {
                this.mRequestList.remove(serial);
            }
        }
        return rr;
    }

    private void clearRequestList(int error) {
        synchronized (this.mRequestList) {
            int count = this.mRequestList.size();
            Log.d(TAG, "clearRequestList  mRequestList=" + count);
            for (int i = 0; i < count; i++) {
                DaemonRequest rr = (DaemonRequest) this.mRequestList.valueAt(i);
                rr.onError(error);
                rr.release();
            }
            this.mRequestList.clear();
        }
    }

    public static int readMessage(InputStream is, byte[] buffer) throws IOException {
        int offset = 0;
        int remaining = 4;
        do {
            int countRead = is.read(buffer, offset, remaining);
            if (countRead < 0) {
                Log.e(TAG, "Hit EOS reading message length");
                return -1;
            }
            offset += countRead;
            remaining -= countRead;
        } while (remaining > 0);
        int messageLength = ((((buffer[0] & 255) << 24) | ((buffer[1] & 255) << 16)) | ((buffer[2] & 255) << 8)) | (buffer[3] & 255);
        offset = 0;
        remaining = messageLength;
        do {
            countRead = is.read(buffer, offset, remaining);
            if (countRead < 0) {
                Log.e(TAG, "Hit EOS reading message.  messageLength=" + messageLength + " remaining=" + remaining);
                return -1;
            }
            offset += countRead;
            remaining -= countRead;
        } while (remaining > 0);
        return messageLength;
    }
}
