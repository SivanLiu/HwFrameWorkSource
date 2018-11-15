package com.leisen.wallet.sdk.apdu;

import android.content.Context;
import com.leisen.wallet.sdk.business.ApduBean;
import com.leisen.wallet.sdk.oma.SmartCard;
import com.leisen.wallet.sdk.oma.SmartCardBean;
import com.leisen.wallet.sdk.oma.SmartCardCallback;
import com.leisen.wallet.sdk.tsm.TSMOperator;
import com.leisen.wallet.sdk.util.LogUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ApduSmartCardRequest implements Runnable, SmartCardCallback {
    private static final int RESULT_FAILURE = 1;
    private static final int RESULT_SUCCESS = 0;
    private static final String TAG = "ApduSmartCardRequest";
    private String mApduAid;
    private List<ApduBean> mCapduList;
    private Context mContext;
    private ApduBean mCurrentExecuteApduBean;
    private int mCurrentExecuteIndex = 0;
    private int mFlag;
    private ApduResponseHandler mHandler;
    private boolean mIsGetLocalData = false;

    public ApduSmartCardRequest(Context context, ApduResponseHandler handler) {
        this.mContext = context;
        this.mHandler = handler;
    }

    public synchronized void run() {
        sendApudToSmartCard();
    }

    private void sendApudToSmartCard() {
        if (this.mCapduList != null && this.mCurrentExecuteIndex != this.mCapduList.size()) {
            this.mCurrentExecuteApduBean = (ApduBean) this.mCapduList.get(this.mCurrentExecuteIndex);
            String apdu = this.mCurrentExecuteApduBean.getApdu();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("==>start get apdu index:");
            stringBuilder.append(this.mCurrentExecuteIndex);
            stringBuilder.append("==apdu:");
            stringBuilder.append(apdu);
            LogUtil.d(str, stringBuilder.toString());
            StringBuilder stringBuilder2;
            if (apdu.length() >= 4 && "00A4".equals(apdu.substring(0, 4))) {
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("==>deal with select apdu :");
                stringBuilder2.append(apdu);
                LogUtil.d(str, stringBuilder2.toString());
                this.mApduAid = apdu.substring(apdu.length() - (Integer.parseInt(apdu.substring(8, 10), 16) * 2), apdu.length());
                this.mCurrentExecuteIndex++;
                SmartCard.getInstance().closeChannel();
                String str2 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("==>has been get select aid:");
                stringBuilder3.append(this.mApduAid);
                LogUtil.d(str2, stringBuilder3.toString());
                sendApudToSmartCard();
            } else if (this.mApduAid != null) {
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("==>start execute apdu：");
                stringBuilder2.append(apdu);
                LogUtil.d(str, stringBuilder2.toString());
                SmartCardBean bean = new SmartCardBean(1, this.mApduAid);
                bean.setCommand(apdu);
                SmartCard.getInstance().setSmartCardCallBack(this).execute(this.mContext, this.mFlag, bean);
            }
        }
    }

    public void onOperSuccess(int flag, String response) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("==>handle apdu response:");
        stringBuilder.append(response);
        LogUtil.d(str, stringBuilder.toString());
        if (this.mIsGetLocalData) {
            sendSuccessMessage(response);
            return;
        }
        str = response;
        String r_apdu = "";
        if (response != null && response.length() > 4) {
            str = response.substring(response.length() - 4, response.length());
            r_apdu = response.substring(0, response.length() - 4).toUpperCase(Locale.getDefault());
        }
        if (str != null) {
            str = str.toUpperCase(Locale.getDefault());
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("==>get response res_sw:");
        stringBuilder2.append(str);
        LogUtil.d(str2, stringBuilder2.toString());
        if (Arrays.asList(this.mCurrentExecuteApduBean.getSw()).contains(str)) {
            if (this.mCurrentExecuteIndex < this.mCapduList.size() - 1) {
                this.mCurrentExecuteIndex++;
                sendApudToSmartCard();
            } else {
                sendMessage(0, this.mCurrentExecuteApduBean.getIndex(), r_apdu, str);
            }
            return;
        }
        sendMessage(1, this.mCurrentExecuteApduBean.getIndex(), r_apdu, str);
    }

    public void onOperFailure(int flag, Error e) {
        if (this.mIsGetLocalData) {
            sendFailureMessage(TSMOperator.RETURN_SMARTCARD_OPER_FAILURE, e);
            return;
        }
        sendErrorMessage(1, this.mCurrentExecuteApduBean.getIndex(), "", "", e);
    }

    public void setCapduList(List<ApduBean> capduList) {
        this.mCapduList = capduList;
    }

    public void setFlag(int flag) {
        this.mFlag = flag;
    }

    public void isGetLocalData(boolean enable) {
        this.mIsGetLocalData = enable;
    }

    public void setGetLocalDataApdu(String apdu, String aid) {
        if (this.mCapduList == null) {
            this.mCapduList = new ArrayList();
        } else {
            this.mCapduList.clear();
        }
        this.mCapduList.add(new ApduBean(apdu));
        this.mApduAid = aid;
    }

    private void sendMessage(int result, int index, String rapdu, String sw) {
        clearData();
        if (this.mHandler != null) {
            this.mHandler.sendSendNextMessage(result, index, rapdu, sw);
        }
    }

    private void sendErrorMessage(int result, int index, String rapdu, String sw, Error e) {
        clearData();
        if (this.mHandler != null) {
            this.mHandler.sendSendNextErrorMessage(result, index, rapdu, sw, e);
        }
    }

    private void sendSuccessMessage(String response) {
        clearData();
        if (this.mHandler != null) {
            this.mHandler.sendSuccessMessage(response);
        }
    }

    private void sendFailureMessage(int result, Error e) {
        clearData();
        if (this.mHandler != null) {
            this.mHandler.sendFailureMessage(result, e);
        }
    }

    private void clearData() {
        this.mCurrentExecuteIndex = 0;
        this.mCurrentExecuteApduBean = null;
        this.mIsGetLocalData = false;
    }
}
