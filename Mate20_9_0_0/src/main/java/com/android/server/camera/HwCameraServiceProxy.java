package com.android.server.camera;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.cover.CoverManager;
import android.cover.HallState;
import android.cover.IHallCallback.Stub;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;

public class HwCameraServiceProxy implements IHwCameraServiceProxy {
    private static final String TAG = "HwCameraServiceProxy";
    private BroadcastReceiver coverStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                Slog.w(HwCameraServiceProxy.TAG, "onReceive, the intent is null!");
                return;
            }
            Slog.w(HwCameraServiceProxy.TAG, "onReceive");
            HwCameraServiceProxy.this.dismissSlidedownTip();
        }
    };
    private Window dialogWindow;
    private Handler handler;
    private View imageView;
    private IntentFilter intentFilter = null;
    private Stub mCallback = new Stub() {
        public void onStateChange(HallState hallState) {
            if (hallState.state == 2) {
                Slog.w(HwCameraServiceProxy.TAG, "hallState SLIDE_HALL_OPEN");
                HwCameraServiceProxy.this.dismissSlidedownTip();
            }
        }
    };
    private final Context mContext;
    private CoverManager mCoverManager;
    private boolean ret = false;
    private Dialog tipDialog;

    public HwCameraServiceProxy(Context context) {
        this.mContext = context;
        this.mCoverManager = new CoverManager();
        this.handler = new Handler(Looper.getMainLooper());
        this.handler.post(new Runnable() {
            public void run() {
                if (HwCameraServiceProxy.this.mContext == null || HwCameraServiceProxy.this.mCoverManager == null) {
                    Slog.w(HwCameraServiceProxy.TAG, "Context == null or coverManager == null");
                    return;
                }
                HwCameraServiceProxy.this.tipDialog = new Dialog(HwCameraServiceProxy.this.mContext.getApplicationContext());
                HwCameraServiceProxy.this.tipDialog.requestWindowFeature(1);
                HwCameraServiceProxy.this.tipDialog.setContentView(34013296);
                HwCameraServiceProxy.this.tipDialog.setCanceledOnTouchOutside(true);
                HwCameraServiceProxy.this.dialogWindow = HwCameraServiceProxy.this.tipDialog.getWindow();
                HwCameraServiceProxy.this.dialogWindow.setType(HwArbitrationDEFS.MSG_MPLINK_UNBIND_SUCCESS);
                HwCameraServiceProxy.this.dialogWindow.setGravity(48);
                HwCameraServiceProxy.this.dialogWindow.setBackgroundDrawable(new ColorDrawable(0));
                HwCameraServiceProxy.this.dialogWindow.addFlags(524288);
                HwCameraServiceProxy.this.intentFilter = new IntentFilter();
                HwCameraServiceProxy.this.intentFilter.addAction("android.intent.action.SCREEN_OFF");
                HwCameraServiceProxy.this.intentFilter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
                HwCameraServiceProxy.this.tipDialog.setOnKeyListener(new OnKeyListener() {
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == 4) {
                            Slog.w(HwCameraServiceProxy.TAG, "onKeyBack");
                            HwCameraServiceProxy.this.dismissSlidedownTip();
                        }
                        return false;
                    }
                });
            }
        });
    }

    public void setType(final int type) {
        this.handler.post(new Runnable() {
            public void run() {
                String str = HwCameraServiceProxy.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setType ");
                stringBuilder.append(type);
                Slog.w(str, stringBuilder.toString());
                if (HwCameraServiceProxy.this.tipDialog == null) {
                    Slog.w(HwCameraServiceProxy.TAG, "tipDialog = null");
                    return;
                }
                if (HwCameraServiceProxy.this.imageView == null) {
                    HwCameraServiceProxy.this.imageView = HwCameraServiceProxy.this.tipDialog.findViewById(34603060);
                    if (HwCameraServiceProxy.this.imageView == null) {
                        return;
                    }
                }
                if (type == 0) {
                    HwCameraServiceProxy.this.imageView.setVisibility(0);
                } else {
                    HwCameraServiceProxy.this.imageView.setVisibility(8);
                }
            }
        });
    }

    public void regesiterService() {
        Slog.w(TAG, "regesiterService begin");
        this.ret = this.mCoverManager.registerHallCallback("cameraserver", 1, this.mCallback);
        Slog.w(TAG, "regesiterService end");
    }

    public void showSlidedownTip() {
        if (!this.ret) {
            Slog.w(TAG, "registerHallCallback fail!");
        }
        this.handler.post(new Runnable() {
            public void run() {
                if (HwCameraServiceProxy.this.tipDialog != null && !HwCameraServiceProxy.this.tipDialog.isShowing() && HwCameraServiceProxy.this.mContext != null) {
                    String str = HwCameraServiceProxy.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("show tipDialog = ");
                    stringBuilder.append(HwCameraServiceProxy.this.tipDialog.toString());
                    Slog.w(str, stringBuilder.toString());
                    HwCameraServiceProxy.this.mContext.registerReceiver(HwCameraServiceProxy.this.coverStateReceiver, HwCameraServiceProxy.this.intentFilter);
                    HwCameraServiceProxy.this.tipDialog.show();
                }
            }
        });
    }

    public void dismissSlidedownTip() {
        this.handler.post(new Runnable() {
            public void run() {
                if (HwCameraServiceProxy.this.tipDialog == null || !HwCameraServiceProxy.this.tipDialog.isShowing()) {
                    Slog.w(HwCameraServiceProxy.TAG, "tipDialog = null");
                    return;
                }
                String str = HwCameraServiceProxy.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("dismiss tipDialog = ");
                stringBuilder.append(HwCameraServiceProxy.this.tipDialog.toString());
                Slog.w(str, stringBuilder.toString());
                HwCameraServiceProxy.this.tipDialog.dismiss();
            }
        });
    }

    public void unRegesiterService() {
        Slog.w(TAG, "unRegesiterService begin");
        dismissSlidedownTip();
        this.handler.post(new Runnable() {
            public void run() {
                try {
                    if (HwCameraServiceProxy.this.coverStateReceiver != null && HwCameraServiceProxy.this.mContext != null) {
                        Slog.w(HwCameraServiceProxy.TAG, "coverStateReceiver.unregisterReceiver begin");
                        HwCameraServiceProxy.this.mContext.unregisterReceiver(HwCameraServiceProxy.this.coverStateReceiver);
                        HwCameraServiceProxy.this.coverStateReceiver = null;
                        Slog.w(HwCameraServiceProxy.TAG, "coverStateReceiver.unregisterReceiver end");
                    }
                } catch (IllegalArgumentException e) {
                    Slog.d(HwCameraServiceProxy.TAG, e.getMessage());
                    Slog.w(HwCameraServiceProxy.TAG, "coverStateReceiver.unregisterReceiver end with error");
                }
            }
        });
        this.mCoverManager.unRegisterHallCallbackEx(1, this.mCallback);
        Slog.w(TAG, "unRegesiterService end");
    }
}
