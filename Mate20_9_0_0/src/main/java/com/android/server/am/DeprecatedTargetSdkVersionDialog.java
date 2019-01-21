package com.android.server.am;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import android.view.Window;
import com.android.server.utils.AppInstallerUtil;

public class DeprecatedTargetSdkVersionDialog {
    private static final String TAG = "ActivityManager";
    private final AlertDialog mDialog;
    private final String mPackageName;

    public DeprecatedTargetSdkVersionDialog(AppWarnings manager, Context context, ApplicationInfo appInfo) {
        this.mPackageName = appInfo.packageName;
        Builder builder = new Builder(context).setPositiveButton(17039370, new -$$Lambda$DeprecatedTargetSdkVersionDialog$06pwP7nH4-Aq27SqBfqb-dAMhzc(this, manager)).setMessage(context.getString(17039935)).setTitle(appInfo.loadSafeLabel(context.getPackageManager()));
        Intent installerIntent = AppInstallerUtil.createIntent(context, appInfo.packageName);
        if (installerIntent != null) {
            builder.setNeutralButton(17039934, new -$$Lambda$DeprecatedTargetSdkVersionDialog$rNs5B1U3M8_obMr8Ws5cSODCFn8(context, installerIntent));
        }
        this.mDialog = builder.create();
        this.mDialog.create();
        Window window = this.mDialog.getWindow();
        window.setType(2002);
        window.getAttributes().setTitle("DeprecatedTargetSdkVersionDialog");
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public void show() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Showing SDK deprecation warning for package ");
        stringBuilder.append(this.mPackageName);
        Log.w("ActivityManager", stringBuilder.toString());
        this.mDialog.show();
    }

    public void dismiss() {
        this.mDialog.dismiss();
    }
}
