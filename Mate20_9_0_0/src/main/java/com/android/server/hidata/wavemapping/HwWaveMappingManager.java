package com.android.server.hidata.wavemapping;

import android.content.Context;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import com.android.server.hidata.arbitration.HwArbitrationHistoryQoeManager;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.cons.ContextManager;
import com.android.server.hidata.wavemapping.dataprovider.BootBroadcastReceiver;
import com.android.server.hidata.wavemapping.util.LogUtil;

public class HwWaveMappingManager {
    private static final String TAG;
    private static HwWaveMappingManager instance = null;
    private BootBroadcastReceiver bootBroadcastReceiver;
    private HandlerThread handlerThread;
    private HwWMStateMachine hwWMStateMachine;
    private Context mContext;
    private Handler mHandler;
    private HwArbitrationHistoryQoeManager mHistoryQoE;
    private IWaveMappingCallback mIWaveMappingCallback;
    private boolean smInitFinish = false;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(HwWaveMappingManager.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    private HwWaveMappingManager(Context mContext) {
        this.mContext = mContext;
        ContextManager.getInstance().setContext(mContext);
        this.hwWMStateMachine = HwWMStateMachine.getInstance(mContext);
        initControllerHandler();
        this.mHistoryQoE = HwArbitrationHistoryQoeManager.getInstance(this.hwWMStateMachine.getHandler());
        this.bootBroadcastReceiver = new BootBroadcastReceiver(this.mHandler);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.ACTION_SHUTDOWN");
        filter.addAction("android.intent.action.BOOT_COMPLETED");
        filter.addAction("android.intent.action.LOCKED_BOOT_COMPLETED");
        this.mContext.registerReceiver(this.bootBroadcastReceiver, filter);
        LogUtil.d("HwWaveMappingManager init completed");
    }

    public static HwWaveMappingManager getInstance(Context mContext) {
        if (instance == null) {
            instance = new HwWaveMappingManager(mContext);
        }
        return instance;
    }

    public static HwWaveMappingManager getInstance() {
        return instance;
    }

    private void initControllerHandler() {
        this.handlerThread = new HandlerThread("HwWaveMappingManager_thread");
        this.handlerThread.start();
        this.mHandler = new Handler(this.handlerThread.getLooper()) {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        LogUtil.d("Constant.MSG_REV_SYSBOOT  begin.");
                        if (Constant.checkPath(HwWaveMappingManager.this.mContext)) {
                            if (!HwWaveMappingManager.this.smInitFinish) {
                                HwWaveMappingManager.this.hwWMStateMachine.init();
                            }
                            HwWaveMappingManager.this.smInitFinish = true;
                            return;
                        }
                        LogUtil.e("init failure.");
                        return;
                    case 2:
                        LogUtil.d("Constant.MSG_REV_SYSSHUTDOWN  begin.");
                        HwWaveMappingManager.this.hwWMStateMachine.handleShutDown();
                        return;
                    default:
                        return;
                }
            }
        };
    }

    public void registerWaveMappingCallback(IWaveMappingCallback callback) {
        if (callback != null) {
            LogUtil.d("registerWaveMappingCallback");
            this.mIWaveMappingCallback = callback;
        }
    }

    public void queryWaveMappingInfo(int UID, int appId, int sense, int network) {
        if (this.mIWaveMappingCallback != null) {
            LogUtil.d("queryWaveMappingInfo");
            if (this.smInitFinish) {
                this.mHistoryQoE.queryHistoryQoE(UID, appId, sense, network, this.mIWaveMappingCallback);
                return;
            }
            this.mIWaveMappingCallback.onWaveMappingRespondCallback(UID, 0, network, true, false);
            return;
        }
        LogUtil.w("mIWaveMappingCallback is none");
    }

    public IWaveMappingCallback getWaveMappingCallback() {
        return this.mIWaveMappingCallback;
    }
}
