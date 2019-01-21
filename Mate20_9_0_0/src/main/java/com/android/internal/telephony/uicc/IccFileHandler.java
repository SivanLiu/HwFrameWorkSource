package com.android.internal.telephony.uicc;

import android.os.AsyncResult;
import android.os.Message;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.ProxyController;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import java.util.ArrayList;

public abstract class IccFileHandler extends AbstractIccFileHandler implements IccConstants {
    protected static final int COMMAND_GET_RESPONSE = 192;
    protected static final int COMMAND_READ_BINARY = 176;
    protected static final int COMMAND_READ_RECORD = 178;
    protected static final int COMMAND_SEEK = 162;
    protected static final int COMMAND_UPDATE_BINARY = 214;
    protected static final int COMMAND_UPDATE_RECORD = 220;
    protected static final int COMMAND_WRITE_MEID_OR_PESN = 222;
    protected static final int EF_ESN_ME = 28472;
    protected static final int EF_TYPE_CYCLIC = 3;
    protected static final int EF_TYPE_LINEAR_FIXED = 1;
    protected static final int EF_TYPE_TRANSPARENT = 0;
    protected static final int EVENT_GET_BINARY_SIZE_DONE = 4;
    protected static final int EVENT_GET_EF_LINEAR_RECORD_SIZE_DONE = 8;
    protected static final int EVENT_GET_RECORD_SIZE_DONE = 6;
    protected static final int EVENT_GET_RECORD_SIZE_IMG_DONE = 11;
    protected static final int EVENT_READ_BINARY_DONE = 5;
    protected static final int EVENT_READ_ICON_DONE = 10;
    protected static final int EVENT_READ_IMG_DONE = 9;
    protected static final int EVENT_READ_RECORD_DONE = 7;
    protected static final int GET_RESPONSE_EF_IMG_SIZE_BYTES = 10;
    protected static final int GET_RESPONSE_EF_SIZE_BYTES = 15;
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
    private static final boolean VDBG = false;
    protected final String mAid;
    protected final CommandsInterface mCi;
    protected final UiccCardApplication mParentApp;

    static class LoadLinearFixedContext {
        int mCountRecords;
        int mEfid;
        boolean mLoadAll;
        Message mOnLoaded;
        String mPath;
        int mRecordNum;
        int mRecordSize;
        ArrayList<byte[]> results;

        LoadLinearFixedContext(int efid, int recordNum, Message onLoaded) {
            this.mEfid = efid;
            this.mRecordNum = recordNum;
            this.mOnLoaded = onLoaded;
            this.mLoadAll = false;
            this.mPath = null;
        }

        LoadLinearFixedContext(int efid, int recordNum, String path, Message onLoaded) {
            this.mEfid = efid;
            this.mRecordNum = recordNum;
            this.mOnLoaded = onLoaded;
            this.mLoadAll = false;
            this.mPath = path;
        }

        LoadLinearFixedContext(int efid, String path, Message onLoaded) {
            this.mEfid = efid;
            this.mRecordNum = 1;
            this.mLoadAll = true;
            this.mOnLoaded = onLoaded;
            this.mPath = path;
        }

        LoadLinearFixedContext(int efid, Message onLoaded) {
            this.mEfid = efid;
            this.mRecordNum = 1;
            this.mLoadAll = true;
            this.mOnLoaded = onLoaded;
            this.mPath = null;
        }
    }

    protected abstract String getEFPath(int i);

    protected abstract void logd(String str);

    protected abstract void loge(String str);

    protected IccFileHandler(UiccCardApplication app, String aid, CommandsInterface ci) {
        this.mParentApp = app;
        this.mAid = aid;
        this.mCi = ci;
    }

    public void dispose() {
    }

    public void loadEFLinearFixed(int fileid, String path, int recordNum, Message onLoaded) {
        String efPath = path == null ? getEFPath(fileid) : path;
        int i = fileid;
        this.mCi.iccIOForApp(192, i, efPath, 0, 0, 15, null, null, this.mAid, obtainMessage(6, new LoadLinearFixedContext(i, recordNum, efPath, onLoaded)));
    }

    public void loadEFLinearFixed(int fileid, int recordNum, Message onLoaded) {
        loadEFLinearFixed(fileid, getEFPath(fileid), recordNum, onLoaded);
    }

    public void loadEFImgLinearFixed(int recordNum, Message onLoaded) {
        int i = recordNum;
        int i2 = i;
        this.mCi.iccIOForApp(192, IccConstants.EF_IMG, getEFPath(IccConstants.EF_IMG), i2, 4, 10, null, null, this.mAid, obtainMessage(11, new LoadLinearFixedContext((int) IccConstants.EF_IMG, i, onLoaded)));
    }

    public void getEFLinearRecordSize(int fileid, String path, Message onLoaded) {
        String efPath = path == null ? getEFPath(fileid) : path;
        int i = fileid;
        this.mCi.iccIOForApp(192, i, efPath, 0, 0, 15, null, null, this.mAid, obtainMessage(8, new LoadLinearFixedContext(i, efPath, onLoaded)));
    }

    public void getEFLinearRecordSize(int fileid, Message onLoaded) {
        getEFLinearRecordSize(fileid, getEFPath(fileid), onLoaded);
    }

    public void loadEFLinearFixedAll(int fileid, String path, Message onLoaded) {
        String efPath = path == null ? getEFPath(fileid) : path;
        int i = fileid;
        this.mCi.iccIOForApp(192, i, efPath, 0, 0, 15, null, null, this.mAid, obtainMessage(6, new LoadLinearFixedContext(i, efPath, onLoaded)));
    }

    public void loadEFLinearFixedAll(int fileid, Message onLoaded) {
        loadEFLinearFixedAll(fileid, getEFPath(fileid), onLoaded);
    }

    public void loadEFTransparent(int fileid, Message onLoaded) {
        int i = fileid;
        this.mCi.iccIOForApp(192, i, getEFPath(fileid), 0, 0, 15, null, null, this.mAid, obtainMessage(4, fileid, 0, onLoaded));
    }

    public void loadEFTransparent(int fileid, int size, Message onLoaded) {
        int i = fileid;
        int i2 = i;
        int i3 = size;
        this.mCi.iccIOForApp(176, i2, getEFPath(fileid), 0, 0, i3, null, null, this.mAid, obtainMessage(5, i, 0, onLoaded));
    }

    public void loadEFImgTransparent(int fileid, int highOffset, int lowOffset, int length, Message onLoaded) {
        int i = fileid;
        Message response = obtainMessage(10, i, 0, onLoaded);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("IccFileHandler: loadEFImgTransparent fileid = ");
        stringBuilder.append(i);
        stringBuilder.append(" filePath = ");
        stringBuilder.append(getEFPath(IccConstants.EF_IMG));
        stringBuilder.append(" highOffset = ");
        int i2 = highOffset;
        stringBuilder.append(i2);
        stringBuilder.append(" lowOffset = ");
        int i3 = lowOffset;
        stringBuilder.append(i3);
        stringBuilder.append(" length = ");
        int i4 = length;
        stringBuilder.append(i4);
        logd(stringBuilder.toString());
        this.mCi.iccIOForApp(176, i, getEFPath(IccConstants.EF_IMG), i2, i3, i4, null, null, this.mAid, response);
    }

    public void updateEFLinearFixed(int fileid, String path, int recordNum, byte[] data, String pin2, Message onComplete) {
        this.mCi.iccIOForApp(COMMAND_UPDATE_RECORD, fileid, path == null ? getEFPath(fileid) : path, recordNum, 4, data.length, IccUtils.bytesToHexString(data), pin2, this.mAid, onComplete);
    }

    public void updateEFLinearFixed(int fileid, int recordNum, byte[] data, String pin2, Message onComplete) {
        this.mCi.iccIOForApp(COMMAND_UPDATE_RECORD, fileid, getEFPath(fileid), recordNum, 4, data.length, IccUtils.bytesToHexString(data), pin2, this.mAid, onComplete);
    }

    public void updateEFTransparent(int fileid, byte[] data, Message onComplete) {
        this.mCi.iccIOForApp(214, fileid, getEFPath(fileid), 0, 0, data.length, IccUtils.bytesToHexString(data), null, this.mAid, onComplete);
    }

    private void sendResult(Message response, Object result, Throwable ex) {
        if (response != null) {
            AsyncResult.forMessage(response, result, ex);
            response.sendToTarget();
        }
    }

    private boolean processException(Message response, AsyncResult ar) {
        IccIoResult result = ar.result;
        if (ar.exception != null) {
            sendResult(response, null, ar.exception);
            return true;
        }
        IccException iccException = result.getException();
        if (iccException == null) {
            return false;
        }
        sendResult(response, null, iccException);
        return true;
    }

    public void handleMessage(Message msg) {
        Message message = msg;
        String path = null;
        try {
            AsyncResult ar;
            Message response;
            IccIoResult result;
            int size;
            LoadLinearFixedContext lc;
            int i;
            int i2;
            switch (message.what) {
                case 4:
                    ar = (AsyncResult) message.obj;
                    response = (Message) ar.userObj;
                    result = (IccIoResult) ar.result;
                    if (!processException(response, (AsyncResult) message.obj)) {
                        byte[] data = result.payload;
                        int fileid = message.arg1;
                        if ((byte) 4 != data[6]) {
                            throw new IccFileTypeMismatch();
                        } else if (data[13] == (byte) 0) {
                            size = ((data[2] & 255) << 8) + (data[3] & 255);
                            CommandsInterface commandsInterface = this.mCi;
                            CommandsInterface commandsInterface2 = commandsInterface;
                            int i3 = fileid;
                            commandsInterface2.iccIOForApp(176, i3, getEFPath(fileid), 0, 0, size, null, null, this.mAid, obtainMessage(5, fileid, 0, response));
                            return;
                        } else {
                            throw new IccFileTypeMismatch();
                        }
                    }
                    return;
                case 5:
                case 10:
                    ar = (AsyncResult) message.obj;
                    response = (Message) ar.userObj;
                    result = ar.result;
                    if (!processException(response, (AsyncResult) message.obj)) {
                        sendResult(response, result.payload, null);
                        return;
                    }
                    return;
                case 6:
                case 11:
                    ar = (AsyncResult) message.obj;
                    lc = (LoadLinearFixedContext) ar.userObj;
                    IccIoResult result2 = ar.result;
                    if (processException(lc.mOnLoaded, (AsyncResult) message.obj)) {
                        loge("exception caught from EVENT_GET_RECORD_SIZE");
                        return;
                    }
                    byte[] data2 = result2.payload;
                    path = lc.mPath;
                    if ((byte) 4 != data2[6]) {
                        throw new IccFileTypeMismatch();
                    } else if ((byte) 1 == data2[13]) {
                        lc.mRecordSize = data2[14] & 255;
                        lc.mCountRecords = (((data2[2] & 255) << 8) + (data2[3] & 255)) / lc.mRecordSize;
                        if (lc.mLoadAll) {
                            lc.results = new ArrayList(lc.mCountRecords);
                        }
                        if (path == null) {
                            path = getEFPath(lc.mEfid);
                        }
                        CommandsInterface commandsInterface3 = this.mCi;
                        size = lc.mEfid;
                        i = lc.mRecordNum;
                        i2 = lc.mRecordSize;
                        commandsInterface3.iccIOForApp(178, size, path, i, 4, i2, null, null, this.mAid, obtainMessage(7, lc));
                        return;
                    } else {
                        throw new IccFileTypeMismatch();
                    }
                case 7:
                case 9:
                    ar = (AsyncResult) message.obj;
                    lc = ar.userObj;
                    IccIoResult result3 = ar.result;
                    response = lc.mOnLoaded;
                    path = lc.mPath;
                    if (!processException(response, (AsyncResult) message.obj)) {
                        if (lc.mLoadAll) {
                            lc.results.add(result3.payload);
                            lc.mRecordNum++;
                            if (lc.mRecordNum > lc.mCountRecords) {
                                sendResult(response, lc.results, null);
                                return;
                            }
                            if (path == null) {
                                path = getEFPath(lc.mEfid);
                            }
                            CommandsInterface commandsInterface4 = this.mCi;
                            size = lc.mEfid;
                            i = lc.mRecordNum;
                            i2 = lc.mRecordSize;
                            commandsInterface4.iccIOForApp(178, size, path, i, 4, i2, null, null, this.mAid, obtainMessage(7, lc));
                            return;
                        }
                        sendResult(response, result3.payload, null);
                        return;
                    }
                    return;
                case 8:
                    ar = message.obj;
                    IccIoResult result4 = ar.result;
                    response = ar.userObj.mOnLoaded;
                    if (!processException(response, (AsyncResult) message.obj)) {
                        byte[] data3 = result4.payload;
                        if ((byte) 4 == data3[6] && (byte) 1 == data3[13]) {
                            sendResult(response, new int[]{data3[14] & 255, ((data3[2] & 255) << 8) + (data3[3] & 255), recordSize[1] / recordSize[0]}, null);
                            return;
                        }
                        throw new IccFileTypeMismatch();
                    }
                    return;
                default:
                    super.handleMessage(msg);
                    return;
            }
        } catch (Exception exc) {
            if (null != null) {
                sendResult(null, null, exc);
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("uncaught exception");
            stringBuilder.append(exc);
            loge(stringBuilder.toString());
        }
    }

    protected String getCommonIccEFPath(int efid) {
        if (efid == IccConstants.EF_PL || efid == IccConstants.EF_ICCID) {
            return IccConstants.MF_SIM;
        }
        if (efid == IccConstants.EF_IMG) {
            return "3F007F105F50";
        }
        if (efid == IccConstants.EF_PBR) {
            return "3F007F105F3A";
        }
        if (efid == IccConstants.EF_OCSGL) {
            return "3F007FFF5F50";
        }
        if (!(efid == IccConstants.EF_MSISDN || efid == IccConstants.EF_PSI)) {
            switch (efid) {
                case 28474:
                case IccConstants.EF_FDN /*28475*/:
                    break;
                default:
                    switch (efid) {
                        case IccConstants.EF_SDN /*28489*/:
                        case IccConstants.EF_EXT1 /*28490*/:
                        case IccConstants.EF_EXT2 /*28491*/:
                        case IccConstants.EF_EXT3 /*28492*/:
                            break;
                        default:
                            return null;
                    }
            }
        }
        return "3F007F10";
    }

    private String getCdmaPath() {
        String filePath = "3F007F25";
        UiccCard card = this.mParentApp.getUiccCard();
        if (card == null || !card.isApplicationOnIcc(AppType.APPTYPE_CSIM)) {
            return filePath;
        }
        return "3F007FFF";
    }

    public void isUimSupportMeidValue(Message result) {
        this.mCi.iccIOForApp(176, IccConstants.EF_CST, "3F007F25", 0, 0, 3, null, null, this.mAid, result);
    }

    public void getMeidOrPesnValue(Message result) {
        this.mCi.iccIOForApp(176, 28472, getCdmaPath(), 0, 0, 8, null, null, this.mAid, result);
    }

    public void setMeidOrPesnValue(String meid, String pesn, Message result) {
        String str = meid;
        String writedValue = "";
        StringBuilder stringBuilder;
        if (str != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(ProxyController.MODEM_0);
            stringBuilder.append(meid.length() / 2);
            stringBuilder.append(str);
            writedValue = stringBuilder.toString();
            String str2 = pesn;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(ProxyController.MODEM_0);
            stringBuilder.append(pesn.length() / 2);
            stringBuilder.append(pesn);
            stringBuilder.append(ServiceStateTracker.UNACTIVATED_MIN2_VALUE);
            writedValue = stringBuilder.toString();
        }
        this.mCi.iccIOForApp(COMMAND_WRITE_MEID_OR_PESN, 28472, getCdmaPath(), 0, 0, 8, writedValue, null, this.mAid, result);
    }
}
