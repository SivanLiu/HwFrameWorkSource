package android.telephony;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Process;
import android.provider.Settings.Global;
import android.util.Log;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class HwCustTelephonyManagerImpl extends HwCustTelephonyManager {
    private static final String APP_POWER_GENIE = "com.huawei.powergenie";
    public static final String TAG = "HwCustTelephonyManagerImpl";
    private static final String VZW_MCCMNC = "311810;311480";
    private static final HashSet<String> mWhiteList = new HashSet<String>() {
        {
            add(HwCustTelephonyManagerImpl.APP_POWER_GENIE);
        }
    };
    private AlertDialog mConfirmDialog;
    private Context mContext;
    private boolean mIsOkClicked;

    public HwCustTelephonyManagerImpl(Context context) {
        super(context);
        this.mContext = context;
    }

    public boolean isVZW() {
        return HwCustUtil.isVZW;
    }

    public String getVZWLine1Number(int subId, String number, String mccmnc) {
        if (!isVZWValidNumber(number)) {
            number = handleIMPUToNumber(HwTelephonyManagerInner.getDefault().getImsImpu(subId), mccmnc);
        }
        if (isVZWValidNumber(number)) {
            return handleVZWNumber(number, mccmnc);
        }
        return null;
    }

    public boolean isVZWValidNumber(String number) {
        if (number == null || number.length() == 0) {
            return false;
        }
        int i;
        if (number.startsWith("+")) {
            number = number.substring(1);
        }
        int length = number.length();
        for (i = 0; i < length; i++) {
            if (!Character.isDigit(number.charAt(i))) {
                return false;
            }
        }
        for (i = 0; i < length; i++) {
            if ('0' != number.charAt(i)) {
                return true;
            }
        }
        return false;
    }

    private String handleIMPUToNumber(String IMPU, String mccmnc) {
        Log.d(TAG, "handleIMPUToNumber");
        if (IMPU == null) {
            return null;
        }
        String number = IMPU.split("@")[0];
        int index = number.indexOf(":");
        if (index > -1) {
            number = number.substring(index + 1);
        }
        if (number == null || number.length() == 0 || !number.startsWith(mccmnc)) {
            return number;
        }
        return null;
    }

    public String handleVZWNumber(String number, String mccmnc) {
        if (number == null || number.length() == 0) {
            return null;
        }
        int length = number.length();
        if (length <= 10 || !isVZWCard(mccmnc)) {
            return number;
        }
        return number.substring(length - 10, length);
    }

    public boolean isVZWCard(String mccmnc) {
        if (mccmnc == null || mccmnc.length() == 0 || !Arrays.asList(VZW_MCCMNC.trim().split(";")).contains(mccmnc)) {
            return false;
        }
        return true;
    }

    public void setDataEnabledVZW(Context context, final int subId, final boolean enable) {
        final TelephonyManager teleManager = (TelephonyManager) context.getSystemService("phone");
        String callingApp = getCallingAppName(context);
        String str;
        StringBuilder stringBuilder;
        if (enable || mWhiteList.contains(callingApp)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setDataEnabledVZW: calling app is:  ");
            stringBuilder.append(callingApp);
            stringBuilder.append(", setDataEnabled :  ");
            stringBuilder.append(enable);
            stringBuilder.append(", without prompt.");
            Log.d(str, stringBuilder.toString());
            teleManager.setDataEnabled(subId, enable);
            return;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("setDataEnabledVZW: calling app is:  ");
        stringBuilder.append(callingApp);
        stringBuilder.append(", setDataEnabled :  ");
        stringBuilder.append(enable);
        stringBuilder.append(", with prompt.");
        Log.d(str, stringBuilder.toString());
        Global.putInt(context.getContentResolver(), "mobile_data", 0);
        this.mIsOkClicked = false;
        Builder builder = new Builder(context, 33947691);
        builder.setTitle(33685919);
        builder.setMessage(33685917);
        if (HwCustUtil.isVoLteOn && !HwCustUtil.isVoWiFi) {
            builder.setMessage(33685918);
        }
        builder.setPositiveButton(17039370, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                teleManager.setDataEnabled(subId, enable);
                dialog.dismiss();
                HwCustTelephonyManagerImpl.this.mConfirmDialog = null;
                HwCustTelephonyManagerImpl.this.mIsOkClicked = true;
                Log.d(HwCustTelephonyManagerImpl.TAG, "setDataEnabledVZW: Confirm to turn off data --> OK");
            }
        });
        builder.setNegativeButton(17039360, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                HwCustTelephonyManagerImpl.this.mIsOkClicked = false;
                HwCustTelephonyManagerImpl.this.onDialogDismiss();
                Log.d(HwCustTelephonyManagerImpl.TAG, "setDataEnabledVZW: Confirm to turn off data  --> Cancel");
            }
        });
        builder.setOnDismissListener(new OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                HwCustTelephonyManagerImpl.this.onDialogDismiss();
            }
        });
        if (this.mConfirmDialog == null) {
            this.mConfirmDialog = builder.create();
            this.mConfirmDialog.getWindow().setType(2009);
            if (!this.mConfirmDialog.isShowing()) {
                this.mConfirmDialog.show();
            }
        }
    }

    private void onDialogDismiss() {
        Global.putInt(this.mContext.getContentResolver(), "mobile_data", this.mIsOkClicked ^ 1);
        this.mConfirmDialog = null;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setDataEnabledVZW: Turn off data:");
        stringBuilder.append(this.mIsOkClicked);
        Log.d(str, stringBuilder.toString());
    }

    private String getCallingAppName(Context context) {
        String appName = "";
        if (context == null) {
            return appName;
        }
        int callingPid = Process.myPid();
        ActivityManager am = (ActivityManager) context.getSystemService("activity");
        if (am == null) {
            return appName;
        }
        List<RunningAppProcessInfo> appProcessList = am.getRunningAppProcesses();
        if (appProcessList == null) {
            return appName;
        }
        for (RunningAppProcessInfo appProcess : appProcessList) {
            if (appProcess.pid == callingPid) {
                appName = appProcess.processName;
            }
        }
        return appName;
    }
}
