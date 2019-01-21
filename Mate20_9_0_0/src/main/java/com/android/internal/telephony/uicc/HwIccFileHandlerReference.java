package com.android.internal.telephony.uicc;

import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.HwSubscriptionManager;
import com.android.internal.telephony.uicc.AbstractIccFileHandler.IccFileHandlerReference;
import java.util.ArrayList;

public class HwIccFileHandlerReference extends Handler implements IccFileHandlerReference {
    protected static final int COMMAND_GET_RESPONSE = 192;
    protected static final int COMMAND_READ_BINARY = 176;
    protected static final int COMMAND_READ_RECORD = 178;
    protected static final int COMMAND_SEEK = 162;
    protected static final int COMMAND_UPDATE_BINARY = 214;
    protected static final int COMMAND_UPDATE_RECORD = 220;
    private static final String CUSTOM_FILE_PATH = "file_path";
    private static final String CUSTOM_FOR_APP = "efid_app";
    protected static final int EF_TYPE_CYCLIC = 3;
    protected static final int EF_TYPE_LINEAR_FIXED = 1;
    protected static final int EF_TYPE_TRANSPARENT = 0;
    protected static final int EVENT_GET_CUSTOM_BINARY_SIZE_DONE = 1200;
    static final int EVENT_GET_SMSC_ADDR_DONE = 1100;
    protected static final int EVENT_READ_CUSTOM_BINARY_DONE = 1201;
    static final int EVENT_SET_SMSC_ADDR_DONE = 1101;
    protected static final int GET_RESPONSE_EF_IMG_SIZE_BYTES = 10;
    protected static final int GET_RESPONSE_EF_SIZE_BYTES = 15;
    private static final String LOG_TAG = "AbstractIccFileHandler";
    protected static final int READ_RECORD_MODE_ABSOLUTE = 4;
    protected static final int RESPONSE_DATA_ACCESS_CONDITION_1 = 8;
    protected static final int RESPONSE_DATA_ACCESS_CONDITION_2 = 9;
    protected static final int RESPONSE_DATA_ACCESS_CONDITION_3 = 10;
    protected static final int RESPONSE_DATA_FILE_ID_1 = 4;
    protected static final int RESPONSE_DATA_FILE_ID_2 = 5;
    protected static final int RESPONSE_DATA_FILE_SIZE_1 = 2;
    protected static final int RESPONSE_DATA_FILE_SIZE_2 = 3;
    protected static final int RESPONSE_DATA_FILE_STATUS = 11;
    protected static final int RESPONSE_DATA_FILE_TYPE = 6;
    protected static final int RESPONSE_DATA_LENGTH = 12;
    protected static final int RESPONSE_DATA_RECORD_LENGTH = 14;
    protected static final int RESPONSE_DATA_RFU_1 = 0;
    protected static final int RESPONSE_DATA_RFU_2 = 1;
    protected static final int RESPONSE_DATA_RFU_3 = 7;
    protected static final int RESPONSE_DATA_STRUCTURE = 13;
    protected static final int TYPE_DF = 2;
    protected static final int TYPE_EF = 4;
    protected static final int TYPE_MF = 1;
    protected static final int TYPE_RFU = 0;
    private Handler efPartReaderHandler = new EfPartReaderHandler();
    private IccFileHandler mFh;
    private Handler vnReaderHandler = new VnReaderHandler();

    private class EfPartReaderHandler extends Handler {
        AsyncResult ar;
        byte[] data;
        IccException iccException;
        LoadLinearFixedContext lc;
        String path;
        Message response;
        IccIoResult result;
        int size;

        private EfPartReaderHandler() {
            this.response = null;
            this.path = null;
        }

        public void handleMessage(Message msg) {
            StringBuilder stringBuilder;
            try {
                switch (msg.what) {
                    case 6:
                        this.ar = (AsyncResult) msg.obj;
                        this.lc = (LoadLinearFixedContext) this.ar.userObj;
                        this.result = (IccIoResult) this.ar.result;
                        this.response = this.lc.mOnLoaded;
                        if (this.ar.exception != null) {
                            HwIccFileHandlerReference.this.sendResult(this.response, null, this.ar.exception);
                            return;
                        }
                        this.iccException = this.result.getException();
                        if (this.iccException != null) {
                            HwIccFileHandlerReference.this.sendResult(this.response, null, this.iccException);
                            return;
                        }
                        this.data = this.result.payload;
                        if ((byte) 4 != this.data[6]) {
                            throw new IccFileTypeMismatch();
                        } else if ((byte) 1 == this.data[13]) {
                            this.lc.mRecordSize = this.data[14] & HwSubscriptionManager.SUB_INIT_STATE;
                            this.size = ((this.data[2] & HwSubscriptionManager.SUB_INIT_STATE) << 8) + (this.data[3] & HwSubscriptionManager.SUB_INIT_STATE);
                            this.lc.mCountRecords = this.size / this.lc.mRecordSize;
                            if (this.lc.mLoadAll) {
                                this.lc.results = new ArrayList(this.lc.mCountRecords);
                            } else if (this.lc.mLoadPart) {
                                this.lc.initLCResults(this.lc.mCountRecords);
                            }
                            HwIccFileHandlerReference.this.mFh.mCi.iccIOForApp(HwIccFileHandlerReference.COMMAND_READ_RECORD, this.lc.mEfid, HwIccFileHandlerReference.this.mFh.getEFPath(this.lc.mEfid), this.lc.mRecordNum, 4, this.lc.mRecordSize, null, null, HwIccFileHandlerReference.this.mFh.mAid, obtainMessage(7, this.lc));
                            return;
                        } else {
                            throw new IccFileTypeMismatch();
                        }
                    case 7:
                        this.ar = (AsyncResult) msg.obj;
                        this.lc = (LoadLinearFixedContext) this.ar.userObj;
                        this.result = (IccIoResult) this.ar.result;
                        this.response = this.lc.mOnLoaded;
                        if (this.ar.exception != null) {
                            HwIccFileHandlerReference.this.sendResult(this.response, null, this.ar.exception);
                            return;
                        }
                        this.iccException = this.result.getException();
                        LoadLinearFixedContext loadLinearFixedContext;
                        if (this.iccException != null) {
                            HwIccFileHandlerReference.this.sendResult(this.response, null, this.iccException);
                            return;
                        } else if (this.lc.mLoadAll) {
                            this.lc.results.add(this.result.payload);
                            loadLinearFixedContext = this.lc;
                            loadLinearFixedContext.mRecordNum++;
                            if (this.lc.mRecordNum > this.lc.mCountRecords) {
                                HwIccFileHandlerReference.this.sendResult(this.response, this.lc.results, null);
                                return;
                            } else {
                                HwIccFileHandlerReference.this.mFh.mCi.iccIOForApp(HwIccFileHandlerReference.COMMAND_READ_RECORD, this.lc.mEfid, HwIccFileHandlerReference.this.mFh.getEFPath(this.lc.mEfid), this.lc.mRecordNum, 4, this.lc.mRecordSize, null, null, HwIccFileHandlerReference.this.mFh.mAid, obtainMessage(7, this.lc));
                                return;
                            }
                        } else if (this.lc.mLoadPart) {
                            this.lc.results.set(this.lc.mRecordNum - 1, this.result.payload);
                            loadLinearFixedContext = this.lc;
                            loadLinearFixedContext.mCount++;
                            if (this.lc.mCount < this.lc.mCountLoadrecords) {
                                this.lc.mRecordNum = ((Integer) this.lc.mRecordNums.get(this.lc.mCount)).intValue();
                                if (this.lc.mRecordNum <= this.lc.mCountRecords) {
                                    if (this.path == null) {
                                        this.path = HwIccFileHandlerReference.this.mFh.getEFPath(this.lc.mEfid);
                                    }
                                    HwIccFileHandlerReference.this.mFh.mCi.iccIOForApp(HwIccFileHandlerReference.COMMAND_READ_RECORD, this.lc.mEfid, this.path, this.lc.mRecordNum, 4, this.lc.mRecordSize, null, null, HwIccFileHandlerReference.this.mFh.mAid, obtainMessage(7, this.lc));
                                    return;
                                }
                                HwIccFileHandlerReference.this.sendResult(this.response, this.lc.results, null);
                                return;
                            }
                            HwIccFileHandlerReference.this.sendResult(this.response, this.lc.results, null);
                            return;
                        } else {
                            HwIccFileHandlerReference.this.sendResult(this.response, this.result.payload, null);
                            return;
                        }
                    default:
                        String str = HwIccFileHandlerReference.LOG_TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("EfPartReaderHandler invalid message ");
                        stringBuilder.append(msg.what);
                        Rlog.d(str, stringBuilder.toString());
                        return;
                }
            } catch (Exception ex) {
                if (this.response != null) {
                    HwIccFileHandlerReference.this.sendResult(this.response, null, ex);
                    return;
                }
                String str2 = HwIccFileHandlerReference.LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("uncaught exception");
                stringBuilder.append(ex);
                Rlog.e(str2, stringBuilder.toString());
            }
        }
    }

    static class LoadLinearFixedContext {
        int mCount;
        int mCountLoadrecords;
        int mCountRecords;
        int mEfid;
        boolean mLoadAll;
        boolean mLoadPart;
        Message mOnLoaded;
        String mPath;
        int mRecordNum;
        ArrayList<Integer> mRecordNums;
        int mRecordSize;
        ArrayList<byte[]> results;

        LoadLinearFixedContext(int efid, int recordNum, Message onLoaded) {
            this.mEfid = efid;
            this.mRecordNum = recordNum;
            this.mOnLoaded = onLoaded;
            this.mLoadAll = false;
            this.mLoadPart = false;
            this.mPath = null;
        }

        LoadLinearFixedContext(int efid, int recordNum, String path, Message onLoaded) {
            this.mEfid = efid;
            this.mRecordNum = recordNum;
            this.mOnLoaded = onLoaded;
            this.mLoadAll = false;
            this.mLoadPart = false;
            this.mPath = path;
        }

        LoadLinearFixedContext(int efid, String path, Message onLoaded) {
            this.mEfid = efid;
            this.mRecordNum = 1;
            this.mLoadAll = true;
            this.mLoadPart = false;
            this.mOnLoaded = onLoaded;
            this.mPath = path;
        }

        LoadLinearFixedContext(int efid, Message onLoaded) {
            this.mEfid = efid;
            this.mRecordNum = 1;
            this.mLoadAll = true;
            this.mLoadPart = false;
            this.mOnLoaded = onLoaded;
            this.mPath = null;
        }

        LoadLinearFixedContext(int efid, ArrayList<Integer> recordNums, String path, Message onLoaded) {
            this.mEfid = efid;
            this.mLoadAll = false;
            this.mLoadPart = true;
            this.mRecordNums = new ArrayList();
            if (recordNums != null) {
                this.mRecordNum = ((Integer) recordNums.get(0)).intValue();
                this.mRecordNums.addAll(recordNums);
                this.mCountLoadrecords = recordNums.size();
            }
            this.mCount = 0;
            this.mOnLoaded = onLoaded;
            this.mPath = path;
        }

        private void initLCResults(int size) {
            this.results = new ArrayList(size);
            byte[] data = new byte[this.mRecordSize];
            int i = 0;
            for (int i2 = 0; i2 < this.mRecordSize; i2++) {
                data[i2] = (byte) -1;
            }
            while (i < size) {
                this.results.add(data);
                i++;
            }
        }
    }

    private class VnReaderHandler extends Handler {
        AsyncResult ar;
        byte[] data;
        int fileid;
        IccException iccException;
        Message response;
        IccIoResult result;
        int size;

        private VnReaderHandler() {
            this.response = null;
        }

        public void handleMessage(Message msg) {
            Message message = msg;
            try {
                switch (message.what) {
                    case 1200:
                        this.ar = (AsyncResult) message.obj;
                        this.response = (Message) this.ar.userObj;
                        this.result = (IccIoResult) this.ar.result;
                        if (this.ar.exception != null) {
                            HwIccFileHandlerReference.this.sendResult(this.response, null, this.ar.exception);
                            return;
                        }
                        this.iccException = this.result.getException();
                        if (this.iccException != null) {
                            HwIccFileHandlerReference.this.sendResult(this.response, null, this.iccException);
                            return;
                        }
                        this.data = this.result.payload;
                        this.fileid = message.arg1;
                        String filePath = msg.getData().getString(HwIccFileHandlerReference.CUSTOM_FILE_PATH);
                        boolean isForApp = msg.getData().getBoolean(HwIccFileHandlerReference.CUSTOM_FOR_APP);
                        if ((byte) 4 != this.data[6]) {
                            throw new IccFileTypeMismatch();
                        } else if (this.data[13] == (byte) 0) {
                            this.size = ((this.data[2] & HwSubscriptionManager.SUB_INIT_STATE) << 8) + (this.data[3] & HwSubscriptionManager.SUB_INIT_STATE);
                            if (isForApp) {
                                Rlog.d(HwIccFileHandlerReference.LOG_TAG, "loadEFTransparent for App");
                                HwIccFileHandlerReference.this.mFh.mCi.iccIOForApp(HwIccFileHandlerReference.COMMAND_READ_BINARY, this.fileid, filePath, 0, 0, this.size, null, null, HwIccFileHandlerReference.this.mFh.mAid, obtainMessage(HwIccFileHandlerReference.EVENT_READ_CUSTOM_BINARY_DONE, this.fileid, 0, this.response));
                                return;
                            }
                            CommandsInterface commandsInterface = HwIccFileHandlerReference.this.mFh.mCi;
                            int i = this.fileid;
                            commandsInterface.iccIO(HwIccFileHandlerReference.COMMAND_READ_BINARY, i, filePath, 0, 0, this.size, null, null, obtainMessage(HwIccFileHandlerReference.EVENT_READ_CUSTOM_BINARY_DONE, this.fileid, 0, this.response));
                            return;
                        } else {
                            throw new IccFileTypeMismatch();
                        }
                    case HwIccFileHandlerReference.EVENT_READ_CUSTOM_BINARY_DONE /*1201*/:
                        this.ar = (AsyncResult) message.obj;
                        this.response = (Message) this.ar.userObj;
                        this.result = (IccIoResult) this.ar.result;
                        if (this.ar.exception != null) {
                            HwIccFileHandlerReference.this.sendResult(this.response, null, this.ar.exception);
                            return;
                        }
                        this.iccException = this.result.getException();
                        if (this.iccException != null) {
                            HwIccFileHandlerReference.this.sendResult(this.response, null, this.iccException);
                            return;
                        } else {
                            HwIccFileHandlerReference.this.sendResult(this.response, this.result.payload, null);
                            return;
                        }
                    default:
                        String str = HwIccFileHandlerReference.LOG_TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("VnReaderHandler invalid message ");
                        stringBuilder.append(message.what);
                        Rlog.d(str, stringBuilder.toString());
                        return;
                }
            } catch (Exception exc) {
                if (this.response != null) {
                    HwIccFileHandlerReference.this.sendResult(this.response, null, exc);
                    return;
                }
                String str2 = HwIccFileHandlerReference.LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("uncaught exception");
                stringBuilder2.append(exc);
                Rlog.e(str2, stringBuilder2.toString());
            }
        }
    }

    public HwIccFileHandlerReference(IccFileHandler fileHandler) {
        this.mFh = fileHandler;
    }

    private void sendResult(Message response, Object result, Throwable ex) {
        if (response != null) {
            AsyncResult.forMessage(response, result, ex);
            response.sendToTarget();
        }
    }

    public void loadEFTransparent(String filePath, int fileid, Message onLoaded) {
        int i = fileid;
        Message response = this.vnReaderHandler.obtainMessage(1200, i, 0, onLoaded);
        Bundle data = new Bundle();
        String str = filePath;
        data.putString(CUSTOM_FILE_PATH, str);
        response.setData(data);
        this.mFh.mCi.iccIO(COMMAND_GET_RESPONSE, i, str, 0, 0, 15, null, null, response);
    }

    public void loadEFTransparent(String filePath, int fileid, Message onLoaded, boolean isForApp) {
        boolean z = isForApp;
        int i = fileid;
        Message response = this.vnReaderHandler.obtainMessage(1200, i, 0, onLoaded);
        Bundle data = new Bundle();
        String str = filePath;
        data.putString(CUSTOM_FILE_PATH, str);
        data.putBoolean(CUSTOM_FOR_APP, z);
        response.setData(data);
        String str2 = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("loadEFTransparent isForApp: ");
        stringBuilder.append(z);
        Rlog.d(str2, stringBuilder.toString());
        this.mFh.mCi.iccIOForApp(COMMAND_GET_RESPONSE, i, str, 0, 0, 15, null, null, this.mFh.mAid, response);
    }

    public void getSmscAddress(Message result) {
        this.mFh.mCi.getSmscAddress(obtainMessage(1100, result));
    }

    public void setSmscAddress(String address, Message result) {
        this.mFh.mCi.setSmscAddress(address, obtainMessage(EVENT_SET_SMSC_ADDR_DONE, result));
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1100:
            case EVENT_SET_SMSC_ADDR_DONE /*1101*/:
                AsyncResult ar = msg.obj;
                AsyncResult.forMessage((Message) ar.userObj).exception = ar.exception;
                ((AsyncResult) ((Message) ar.userObj).obj).result = ar.result;
                ((Message) ar.userObj).sendToTarget();
                return;
            default:
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleMessage invalid message ");
                stringBuilder.append(msg.what);
                Rlog.d(str, stringBuilder.toString());
                return;
        }
    }

    public void loadEFLinearFixedPartHW(int fileid, ArrayList<Integer> recordNums, Message onLoaded) {
        int i = fileid;
        int i2 = i;
        this.mFh.mCi.iccIOForApp(COMMAND_GET_RESPONSE, i2, this.mFh.getEFPath(i), 0, 0, 15, null, null, this.mFh.mAid, this.efPartReaderHandler.obtainMessage(6, new LoadLinearFixedContext(i, (ArrayList) recordNums, this.mFh.getEFPath(i), onLoaded)));
    }
}
