package com.android.server.am;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.view.Window;
import android.widget.CheckBox;

public class UnsupportedDisplaySizeDialog {
    private final AlertDialog mDialog;
    private final String mPackageName;

    public UnsupportedDisplaySizeDialog(AppWarnings manager, Context context, ApplicationInfo appInfo) {
        this.mPackageName = appInfo.packageName;
        this.mDialog = new Builder(context).setPositiveButton(17039370, null).setMessage(context.getString(17041272, new Object[]{appInfo.loadSafeLabel(context.getPackageManager())})).setView(17367326).create();
        this.mDialog.create();
        Window window = this.mDialog.getWindow();
        window.setType(2002);
        window.getAttributes().setTitle("UnsupportedDisplaySizeDialog");
        CheckBox alwaysShow = (CheckBox) this.mDialog.findViewById(16908738);
        alwaysShow.setChecked(true);
        alwaysShow.setOnCheckedChangeListener(new -$$Lambda$UnsupportedDisplaySizeDialog$3f6hcHrxiaslh6X9tny1rOFVGnI(this, manager));
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public void show() {
        this.mDialog.show();
    }

    public void dismiss() {
        this.mDialog.dismiss();
    }
}
