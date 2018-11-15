package com.android.server.wifi;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.WindowManager.LayoutParams;

public class WifiEapUIManager {
    private static final int ERROR_CODE_MAX = 32766;
    private static final int ERROR_CODE_MIN = 32760;
    private static final String TAG = "WifiEapUIManager";
    private Context mContext;
    private Dialog mDialog;

    public WifiEapUIManager(Context context) {
        this.mContext = context;
    }

    public void showDialog(String title, String message) {
        if (this.mDialog != null && this.mDialog.isShowing()) {
            this.mDialog.dismiss();
        }
        this.mDialog = new Builder(this.mContext, 33947691).setTitle(title).setMessage(message).setCancelable(false).setPositiveButton(Resources.getSystem().getString(17039370), null).create();
        this.mDialog.getWindow().setType(2003);
        LayoutParams attrs = this.mDialog.getWindow().getAttributes();
        attrs.privateFlags = 16;
        this.mDialog.getWindow().setAttributes(attrs);
        this.mDialog.show();
    }

    public void showDialog(int errorcode) {
        showDialog(Resources.getSystem().getString(33686180), getMessage(errorcode));
    }

    private String getMessage(int errorcode) {
        String message = "";
        String[] errorMessages = Resources.getSystem().getStringArray(33816593);
        if (errorcode >= ERROR_CODE_MIN && errorcode <= ERROR_CODE_MAX) {
            return errorMessages[errorcode - 32760];
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Error code is not supported. (Error = ");
        stringBuilder.append(errorcode);
        stringBuilder.append(")");
        Log.e(str, stringBuilder.toString());
        return message;
    }
}
