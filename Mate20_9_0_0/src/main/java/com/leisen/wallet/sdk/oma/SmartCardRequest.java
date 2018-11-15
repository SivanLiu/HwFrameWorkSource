package com.leisen.wallet.sdk.oma;

import android.se.omapi.Channel;
import android.se.omapi.Reader;
import android.se.omapi.SEService;
import android.se.omapi.Session;
import com.leisen.wallet.sdk.util.DataConvertUtil;
import com.leisen.wallet.sdk.util.LogUtil;
import java.io.IOException;

class SmartCardRequest implements Runnable {
    private static final String BOUNDARY = "==>";
    private static final String TAG = "SmartCardRequest";
    private SmartCardCallback mCallback;
    private Channel mChannel;
    private int mFlag = -1;
    private SEService mSEService;
    private Session mSession;
    private SmartCardBean mSmartCardBean;

    protected SmartCardRequest(SEService seService) {
        this.mSEService = seService;
    }

    public void setSmartCartBean(SmartCardBean bean) {
        this.mSmartCardBean = bean;
    }

    public void setSmartCardCallback(SmartCardCallback callback) {
        this.mCallback = callback;
    }

    public void setFlag(int flag) {
        this.mFlag = flag;
    }

    public synchronized void run() {
        if (openCurrentAvailableChannel()) {
            try {
                executeApduCmd();
            } catch (IOException e) {
                closeChannelAndSession();
                int i = this.mFlag;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("execute apdu error：");
                stringBuilder.append(e.getMessage());
                operFailure(i, stringBuilder.toString());
            }
        } else {
            return;
        }
        return;
    }

    private void executeApduCmd() throws IOException {
        String command = this.mSmartCardBean.getCommand();
        if (command == null || "".equals(command)) {
            String rapdu = DataConvertUtil.bytesToHexString(this.mChannel.getSelectResponse());
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("==>选择AID的结果为：");
            stringBuilder.append(rapdu);
            LogUtil.i(str, stringBuilder.toString());
            operSuccess(this.mFlag, rapdu);
        }
        byte[] byteCommand = DataConvertUtil.hexStringToBytes(command);
        if (this.mChannel != null) {
            String rapdu2 = DataConvertUtil.bytesToHexString(this.mChannel.transmit(byteCommand));
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("==>执行APDU:");
            stringBuilder2.append(command);
            stringBuilder2.append("，返回的RAPDU为：");
            stringBuilder2.append(rapdu2);
            LogUtil.i(str2, stringBuilder2.toString());
            operSuccess(this.mFlag, rapdu2);
        }
    }

    private boolean openCurrentAvailableChannel() {
        int i;
        StringBuilder stringBuilder;
        if (this.mChannel == null) {
            Reader reader = getCurrentAvailableReader();
            if (reader == null) {
                operFailure(this.mFlag, "choose reader not exist");
                return false;
            } else if (reader.isSecureElementPresent()) {
                StringBuilder stringBuilder2;
                try {
                    this.mSession = reader.openSession();
                    byte[] byteAid = DataConvertUtil.hexStringToBytes(this.mSmartCardBean.getAid());
                    String str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("==>打开通道的Aid：");
                    stringBuilder2.append(this.mSmartCardBean.getAid());
                    LogUtil.i(str, stringBuilder2.toString());
                    try {
                        if (this.mSession != null) {
                            this.mChannel = this.mSession.openBasicChannel(byteAid);
                        }
                    } catch (IOException e) {
                        closeChannelAndSession();
                        i = this.mFlag;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("IOException open channel error：");
                        stringBuilder.append(e.getMessage());
                        operFailure(i, stringBuilder.toString());
                        return false;
                    } catch (SecurityException e2) {
                        closeChannelAndSession();
                        i = this.mFlag;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("SecurityException open channel error：");
                        stringBuilder.append(e2.getMessage());
                        operFailure(i, stringBuilder.toString());
                        return false;
                    } catch (Exception e3) {
                        e3.printStackTrace();
                        closeChannelAndSession();
                        i = this.mFlag;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Exception open channel error：");
                        stringBuilder.append(e3.getMessage());
                        operFailure(i, stringBuilder.toString());
                        return false;
                    }
                } catch (IOException e4) {
                    closeChannelAndSession();
                    int i2 = this.mFlag;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("open session error：");
                    stringBuilder2.append(e4.getMessage());
                    operFailure(i2, stringBuilder2.toString());
                    return false;
                }
            } else {
                operFailure(this.mFlag, "choose reader can not use");
                return false;
            }
        }
        return true;
    }

    private Reader getCurrentAvailableReader() {
        Reader[] readers = this.mSEService.getReaders();
        if (readers == null || readers.length < 1) {
            operFailure(this.mFlag, "your devices not support any reader");
            return null;
        }
        int i = 0;
        for (Reader reader : readers) {
            if (reader.getName().startsWith("eSE2")) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("==> first use reader name:");
                stringBuilder.append(reader.getName());
                LogUtil.e(str, stringBuilder.toString());
                return reader;
            }
        }
        int length = readers.length;
        while (i < length) {
            Reader reader2 = readers[i];
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("==>reader name:");
            stringBuilder2.append(reader2.getName());
            LogUtil.e(str2, stringBuilder2.toString());
            if (reader2.getName().startsWith(this.mSmartCardBean.getReaderName())) {
                return reader2;
            }
            i++;
        }
        return null;
    }

    public void closeChannelAndSession() {
        try {
            if (this.mChannel != null) {
                this.mChannel.close();
                this.mChannel = null;
                LogUtil.i(TAG, "==>Channel正常关闭");
            }
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("==>Channel关闭异常");
            stringBuilder.append(e.getMessage());
            LogUtil.e(str, stringBuilder.toString());
        }
        try {
            if (this.mSession != null && !this.mSession.isClosed()) {
                this.mSession.close();
                this.mSession = null;
                LogUtil.i(TAG, "==>Session正常关闭");
            }
        } catch (Exception e2) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("==>Session关闭异常");
            stringBuilder2.append(e2.getMessage());
            LogUtil.e(str2, stringBuilder2.toString());
        }
    }

    private void operSuccess(int flag, String response) {
        if (this.mCallback != null) {
            this.mCallback.onOperSuccess(flag, response);
        }
    }

    private void operFailure(int flag, String detailMessage) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(BOUNDARY);
        stringBuilder.append(detailMessage);
        LogUtil.e(str, stringBuilder.toString());
        if (this.mCallback != null) {
            this.mCallback.onOperFailure(flag, new Error(detailMessage));
        }
    }
}
