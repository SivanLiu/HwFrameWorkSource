package com.android.server.am;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.Global;
import android.text.BidiFormatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.TextView;

final class AppErrorDialog extends BaseErrorDialog implements OnClickListener {
    static int ALREADY_SHOWING = -3;
    static final int APP_INFO = 8;
    static int BACKGROUND_USER = -2;
    static final int CANCEL = 7;
    static int CANT_SHOW = -1;
    static final long DISMISS_TIMEOUT = 300000;
    static final int FORCE_QUIT = 1;
    static final int FORCE_QUIT_AND_REPORT = 2;
    static final int MUTE = 5;
    static final int RESTART = 3;
    static final int TIMEOUT = 6;
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            AppErrorDialog.this.setResult(msg.what);
            AppErrorDialog.this.dismiss();
        }
    };
    private final boolean mIsRestartable;
    private final ProcessRecord mProc;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                AppErrorDialog.this.cancel();
            }
        }
    };
    private final AppErrorResult mResult;
    private final ActivityManagerService mService;

    static class Data {
        boolean isRestartableForService;
        ProcessRecord proc;
        boolean repeating;
        AppErrorResult result;
        TaskRecord task;

        Data() {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:24:0x00ea  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public AppErrorDialog(Context context, ActivityManagerService service, Data data) {
        CharSequence name;
        int i;
        LayoutParams attrs;
        StringBuilder stringBuilder;
        super(context);
        Resources res = context.getResources();
        this.mService = service;
        this.mProc = data.proc;
        this.mResult = data.result;
        boolean z = (data.task != null || data.isRestartableForService) && Global.getInt(context.getContentResolver(), "show_restart_in_crash_dialog", 0) != 0;
        this.mIsRestartable = z;
        BidiFormatter bidi = BidiFormatter.getInstance();
        if (this.mProc.pkgList.size() == 1) {
            CharSequence applicationLabel = context.getPackageManager().getApplicationLabel(this.mProc.info);
            name = applicationLabel;
            if (applicationLabel != null) {
                if (data.repeating) {
                    i = 17039560;
                } else {
                    i = 17039559;
                }
                setTitle(res.getString(i, new Object[]{bidi.unicodeWrap(name.toString()), bidi.unicodeWrap(this.mProc.info.processName)}));
                setCancelable(true);
                setCancelMessage(this.mHandler.obtainMessage(7));
                attrs = getWindow().getAttributes();
                stringBuilder = new StringBuilder();
                stringBuilder.append("Application Error: ");
                stringBuilder.append(this.mProc.info.processName);
                attrs.setTitle(stringBuilder.toString());
                attrs.privateFlags |= 272;
                getWindow().setAttributes(attrs);
                if (this.mProc.persistent) {
                    getWindow().setType(2010);
                }
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(6), 300000);
            }
        }
        name = this.mProc.processName;
        if (data.repeating) {
            i = 17039565;
        } else {
            i = 17039564;
        }
        setTitle(res.getString(i, new Object[]{bidi.unicodeWrap(name.toString())}));
        setCancelable(true);
        setCancelMessage(this.mHandler.obtainMessage(7));
        attrs = getWindow().getAttributes();
        stringBuilder = new StringBuilder();
        stringBuilder.append("Application Error: ");
        stringBuilder.append(this.mProc.info.processName);
        attrs.setTitle(stringBuilder.toString());
        attrs.privateFlags |= 272;
        getWindow().setAttributes(attrs);
        if (this.mProc.persistent) {
        }
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(6), 300000);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout frame = (FrameLayout) findViewById(16908331);
        Context context = getContext();
        boolean showMute = true;
        LayoutInflater.from(context).inflate(17367093, frame, true);
        boolean hasReceiver = this.mProc.errorReportReceiver != null;
        TextView restart = (TextView) findViewById(16908712);
        restart.setOnClickListener(this);
        int i = 8;
        restart.setVisibility(this.mIsRestartable ? 0 : 8);
        TextView report = (TextView) findViewById(16908711);
        report.setOnClickListener(this);
        report.setVisibility(hasReceiver ? 0 : 8);
        ((TextView) findViewById(16908709)).setOnClickListener(this);
        ((TextView) findViewById(16908708)).setOnClickListener(this);
        if (Build.IS_USER || Global.getInt(context.getContentResolver(), "development_settings_enabled", 0) == 0 || Global.getInt(context.getContentResolver(), "show_mute_in_crash_dialog", 0) == 0) {
            showMute = false;
        }
        TextView mute = (TextView) findViewById(16908710);
        mute.setOnClickListener(this);
        if (showMute) {
            i = 0;
        }
        mute.setVisibility(i);
        findViewById(16908834).setVisibility(0);
    }

    public void onStart() {
        super.onStart();
        getContext().registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
    }

    protected void onStop() {
        super.onStop();
        getContext().unregisterReceiver(this.mReceiver);
    }

    public void dismiss() {
        if (!this.mResult.mHasResult) {
            setResult(1);
        }
        super.dismiss();
    }

    private void setResult(int result) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (this.mProc != null && this.mProc.crashDialog == this) {
                    this.mProc.crashDialog = null;
                }
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        this.mResult.set(result);
        this.mHandler.removeMessages(6);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case 16908708:
                this.mHandler.obtainMessage(8).sendToTarget();
                return;
            case 16908709:
                this.mHandler.obtainMessage(1).sendToTarget();
                return;
            case 16908710:
                this.mHandler.obtainMessage(5).sendToTarget();
                return;
            case 16908711:
                this.mHandler.obtainMessage(2).sendToTarget();
                return;
            case 16908712:
                this.mHandler.obtainMessage(3).sendToTarget();
                return;
            default:
                return;
        }
    }
}
