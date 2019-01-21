package com.android.server.am;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.view.Window;

final class StrictModeViolationDialog extends BaseErrorDialog {
    static final int ACTION_OK = 0;
    static final int ACTION_OK_AND_REPORT = 1;
    static final long DISMISS_TIMEOUT = 60000;
    private static final String TAG = "StrictModeViolationDialog";
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            synchronized (StrictModeViolationDialog.this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    if (StrictModeViolationDialog.this.mProc != null && StrictModeViolationDialog.this.mProc.crashDialog == StrictModeViolationDialog.this) {
                        StrictModeViolationDialog.this.mProc.crashDialog = null;
                    }
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
            StrictModeViolationDialog.this.mResult.set(msg.what);
            StrictModeViolationDialog.this.dismiss();
        }
    };
    private final ProcessRecord mProc;
    private final AppErrorResult mResult;
    private final ActivityManagerService mService;

    /* JADX WARNING: Removed duplicated region for block: B:8:0x0071  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public StrictModeViolationDialog(Context context, ActivityManagerService service, AppErrorResult result, ProcessRecord app) {
        Window window;
        StringBuilder stringBuilder;
        super(context);
        Resources res = context.getResources();
        this.mService = service;
        this.mProc = app;
        this.mResult = result;
        if (app.pkgList.size() == 1) {
            CharSequence applicationLabel = context.getPackageManager().getApplicationLabel(app.info);
            CharSequence name = applicationLabel;
            if (applicationLabel != null) {
                setMessage(res.getString(17041139, new Object[]{name.toString(), app.info.processName}));
                setCancelable(false);
                setButton(-1, res.getText(17039952), this.mHandler.obtainMessage(0));
                if (app.errorReportReceiver != null) {
                    setButton(-2, res.getText(17040998), this.mHandler.obtainMessage(1));
                }
                getWindow().addPrivateFlags(256);
                window = getWindow();
                stringBuilder = new StringBuilder();
                stringBuilder.append("Strict Mode Violation: ");
                stringBuilder.append(app.info.processName);
                window.setTitle(stringBuilder.toString());
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(0), 60000);
            }
        }
        setMessage(res.getString(17041140, new Object[]{app.processName.toString()}));
        setCancelable(false);
        setButton(-1, res.getText(17039952), this.mHandler.obtainMessage(0));
        if (app.errorReportReceiver != null) {
        }
        getWindow().addPrivateFlags(256);
        window = getWindow();
        stringBuilder = new StringBuilder();
        stringBuilder.append("Strict Mode Violation: ");
        stringBuilder.append(app.info.processName);
        window.setTitle(stringBuilder.toString());
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(0), 60000);
    }
}
