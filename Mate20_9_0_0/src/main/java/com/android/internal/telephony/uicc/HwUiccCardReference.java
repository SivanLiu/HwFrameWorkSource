package com.android.internal.telephony.uicc;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConstants;
import com.android.internal.telephony.uicc.AbstractUiccCard.UiccCardReference;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.vsim.HwVSimConstants;
import com.huawei.utils.reflect.EasyInvokeFactory;

public class HwUiccCardReference implements UiccCardReference {
    protected static final boolean DBG = false;
    private static final int EVENT_GET_ATR_DONE = 105;
    private static final String LOG_TAG = "HwUiccCardReference";
    private static final int ONE_SECOND_TIME_MILLISECONDS = 1000;
    private static final int REBOOT_TOTAL_TIME_MILLISECONDS = 30000;
    private static UiccCardUtils uiccCardUtils = ((UiccCardUtils) EasyInvokeFactory.getInvokeUtils(UiccCardUtils.class));
    private boolean bShowedTipDlg = false;
    Button mCoutDownRootButton = null;
    private Handler mHandler = new MyHandler();
    AlertDialog mSimAddDialog = null;
    public UiccCard mUiccCard;
    Resources r = Resources.getSystem();

    static class ListenerSimAddDialog implements OnClickListener {
        Context mContext;

        ListenerSimAddDialog(Context mContext) {
            this.mContext = mContext;
        }

        public void onClick(View v) {
            Intent reboot = new Intent("android.intent.action.REBOOT");
            reboot.putExtra("android.intent.extra.KEY_CONFIRM", false);
            reboot.setFlags(268435456);
            this.mContext.startActivity(reboot);
        }
    }

    private static class MyHandler extends Handler {
        private MyHandler() {
        }

        /* synthetic */ MyHandler(AnonymousClass1 x0) {
            this();
        }

        public void handleMessage(Message msg) {
            if (msg.what != HwUiccCardReference.EVENT_GET_ATR_DONE) {
                String str = HwUiccCardReference.LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown Event ");
                stringBuilder.append(msg.what);
                Rlog.e(str, stringBuilder.toString());
                return;
            }
            AsyncResult ar = msg.obj;
            if (ar.exception != null) {
                String str2 = HwUiccCardReference.LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error in SIM access with exception");
                stringBuilder2.append(ar.exception);
                Rlog.e(str2, stringBuilder2.toString());
            }
            AsyncResult.forMessage((Message) ar.userObj, ar.result, ar.exception);
            ((Message) ar.userObj).sendToTarget();
        }
    }

    public HwUiccCardReference(UiccCard uiccCard) {
        this.mUiccCard = uiccCard;
    }

    public boolean hasAppActived() {
        int uiccApplicationLenght = 0;
        if (uiccCardUtils.getUiccApplications(this.mUiccCard) != null) {
            uiccApplicationLenght = uiccCardUtils.getUiccApplications(this.mUiccCard).length;
        }
        int i = 0;
        while (i < uiccApplicationLenght) {
            if (uiccCardUtils.getUiccApplications(this.mUiccCard)[i] != null && uiccCardUtils.getUiccApplications(this.mUiccCard)[i].getState() == AppState.APPSTATE_READY) {
                return true;
            }
            i++;
        }
        return false;
    }

    public int getNumApplications() {
        int count = 0;
        for (UiccCardApplication a : uiccCardUtils.getUiccApplications(this.mUiccCard)) {
            if (a != null) {
                count++;
            }
        }
        return count;
    }

    public void iccGetATR(Message onComplete) {
        uiccCardUtils.getCi(this.mUiccCard).iccGetATR(this.mHandler.obtainMessage(EVENT_GET_ATR_DONE, onComplete));
    }

    public AlertDialog getSimAddDialog(Context mContext, String title, String message, String buttonTxt, OnClickListener listener) {
        View button = new Button(new ContextThemeWrapper(mContext, mContext.getResources().getIdentifier("androidhwext:style/Theme.Emui", null, null)));
        button.setText(buttonTxt);
        button.setOnClickListener(listener);
        AlertDialog dialog = new Builder(mContext, 33947691).setTitle(title).setMessage(message).create();
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setView(button, Dp2Px(mContext, 15.0f), Dp2Px(mContext, 12.0f), Dp2Px(mContext, 15.0f), Dp2Px(mContext, 12.0f));
        return dialog;
    }

    private int Dp2Px(Context context, float dp) {
        return (int) ((dp * context.getResources().getDisplayMetrics().density) + 0.5f);
    }

    public void displayUimTipDialog(Context context, int resId) {
        if (!this.bShowedTipDlg) {
            if (context == null) {
                Rlog.e(LOG_TAG, "context ==null");
                return;
            }
            try {
                this.bShowedTipDlg = true;
                AlertDialog dialog = new Builder(context, 33947691).setTitle(33685797).setMessage(resId).setPositiveButton(17039370, null).setCancelable(false).create();
                dialog.getWindow().setType(HwFullNetworkConstants.EVENT_GET_PREF_NETWORK_MODE_DONE);
                dialog.show();
            } catch (NullPointerException e) {
                this.bShowedTipDlg = false;
                Rlog.e(LOG_TAG, "displayUimTipDialog NullPointerException");
            }
        }
    }

    public boolean isAllAndCardRemoved(boolean isAdded) {
        boolean z = false;
        if (SystemProperties.getBoolean("ro.hwpp.hot_swap_restart_remov", false) && !isAdded) {
            z = true;
        }
        return z;
    }

    private AlertDialog getSimAddDialogPlk(Context mContext, String title, String message, Button btn, OnClickListener listener) {
        this.mSimAddDialog = new Builder(mContext, 33947691).setTitle(title).setMessage(message).create();
        this.mSimAddDialog.setCancelable(false);
        this.mSimAddDialog.setCanceledOnTouchOutside(false);
        this.mSimAddDialog.setView(btn, Dp2Px(mContext, 15.0f), Dp2Px(mContext, 12.0f), Dp2Px(mContext, 15.0f), Dp2Px(mContext, 12.0f));
        return this.mSimAddDialog;
    }

    public void displayRestartDialog(Context mContext) {
        Context context = mContext;
        OnClickListener listener = new ListenerSimAddDialog(context);
        String title = " ";
        String message = " ";
        String buttonTxt = " ";
        String title2 = this.r.getString(33685824);
        String message2 = this.r.getString(33685825);
        String buttonTxt2 = this.r.getString(33685826);
        this.mCoutDownRootButton = new Button(new ContextThemeWrapper(context, mContext.getResources().getIdentifier("androidhwext:style/Theme.Emui", null, null)));
        this.mCoutDownRootButton.setText(buttonTxt2);
        this.mCoutDownRootButton.setOnClickListener(listener);
        this.mSimAddDialog = getSimAddDialogPlk(context, title2, message2, this.mCoutDownRootButton, listener);
        final Context context2 = context;
        new CountDownTimer(HwVSimConstants.WAIT_FOR_NV_CFG_MATCH_TIMEOUT, 1000) {
            public void onTick(long millisUntilFinished) {
                if (HwUiccCardReference.this.mSimAddDialog != null && HwUiccCardReference.this.mCoutDownRootButton != null) {
                    HwUiccCardReference.this.mCoutDownRootButton.setText(String.format(HwUiccCardReference.this.r.getString(33685826), new Object[]{Integer.valueOf((int) (millisUntilFinished / 1000))}));
                }
            }

            public void onFinish() {
                HwUiccCardReference.this.mSimAddDialog.dismiss();
                Intent reboot = new Intent("android.intent.action.REBOOT");
                reboot.setFlags(268435456);
                context2.startActivity(reboot);
            }
        }.start();
        this.mSimAddDialog.getWindow().setType(HwFullNetworkConstants.EVENT_GET_PREF_NETWORK_MODE_DONE);
        this.mSimAddDialog.show();
    }
}
