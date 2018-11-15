package com.android.server.wifi.HwQoE;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.HwQoE.IHwQoECallback;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import com.android.server.wifi.WifiStateMachine;
import com.android.server.wifi.wifipro.WifiHandover;

public class HwQoEMonitorAdapter implements IHwQoEMonitorCallback {
    private static final String VOWIFI_NETWORK_ACTION = "android.net.wifi.action.VOWIFI_NETWORK_STATE_CHANGED";
    private int NETWORK_HAVE_INTERNET = 1;
    private int NETWORK_NO_INTERNET = 0;
    private boolean isHaveInternet = false;
    private boolean isInitCheck = false;
    private boolean isStartMonitor = false;
    private boolean isWaittingCheckResult = false;
    public IHwQoECallback mCallback;
    private HwQoENetWorkInfo mCheckInfo = null;
    private int mCheckResult = 0;
    private HwQoEMonitorConfig mConifg;
    private Context mContext;
    private HwQoEJNIAdapter mHwQoEJNIAdapter;
    private HwQoENetWorkInfo mHwQoENetWorkInfo = null;
    private HwQoENetWorkMonitor mHwQoENetWorkMonitor;
    private HwQoEUdpServiceImpl mHwQoEUdpServiceImpl;
    private Handler mLocalHandler;
    private int mNetworkDisableCount = 0;
    private int mVoWIFIStatus = 0;

    public HwQoEMonitorAdapter(Context context, WifiStateMachine wifiStateMachine, HwQoEMonitorConfig conifg, IHwQoECallback callback) {
        this.mContext = context;
        this.mConifg = conifg;
        this.mCallback = callback;
        initQoEAdapter();
        this.mHwQoENetWorkMonitor = new HwQoENetWorkMonitor(context, wifiStateMachine, this.mConifg.mPeriod, this);
        this.mHwQoEUdpServiceImpl = new HwQoEUdpServiceImpl(context, this);
        this.mHwQoEJNIAdapter = HwQoEJNIAdapter.getInstance();
    }

    private void initQoEAdapter() {
        HandlerThread handlerThread = new HandlerThread("HwQoEAdapter monior Thread");
        handlerThread.start();
        this.mLocalHandler = new Handler(handlerThread.getLooper()) {
            public void handleMessage(Message msg) {
                int i = msg.what;
                StringBuilder stringBuilder;
                if (i == HwQoEUtils.QOE_MSG_WIFI_CHECK_TIMEOUT) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("QOE_MSG_WIFI_CHECK_TIMEOUT isWaittingCheckResult = ");
                    stringBuilder.append(HwQoEMonitorAdapter.this.isWaittingCheckResult);
                    stringBuilder.append(" isInitCheck = ");
                    stringBuilder.append(HwQoEMonitorAdapter.this.isInitCheck);
                    HwQoEUtils.logD(stringBuilder.toString());
                    if (HwQoEMonitorAdapter.this.isWaittingCheckResult) {
                        HwQoEMonitorAdapter.this.isWaittingCheckResult = false;
                        HwQoENetWorkInfo result = HwQoEMonitorAdapter.this.mHwQoEJNIAdapter.queryPeriodData();
                        result.mDnsFailCount = HwQoEMonitorAdapter.this.mHwQoENetWorkMonitor.getDnsFaileCount();
                        if (HwQoEMonitorAdapter.this.isInitCheck) {
                            HwQoEMonitorAdapter.this.isInitCheck = false;
                            HwQoEMonitorAdapter.this.initCheckResult(HwQoEMonitorAdapter.this.mCheckInfo, result);
                        } else {
                            HwQoEMonitorAdapter.this.processCheckResult(HwQoEMonitorAdapter.this.mCheckInfo, result);
                        }
                        HwQoEMonitorAdapter.this.mCheckInfo = null;
                    }
                } else if (i != HwQoEUtils.QOE_MSG_MONITOR_START) {
                    switch (i) {
                        case 101:
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("QOE_MSG_UPDATE_TCP_INFO isWaittingCheckResult = ");
                            stringBuilder.append(HwQoEMonitorAdapter.this.isWaittingCheckResult);
                            HwQoEUtils.logD(stringBuilder.toString());
                            if (!HwQoEMonitorAdapter.this.isWaittingCheckResult) {
                                HwQoEMonitorAdapter.this.detectNetworkQuality(HwQoEMonitorAdapter.this.mHwQoENetWorkInfo, false);
                                break;
                            }
                            break;
                        case 102:
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("QOE_MSG_MONITOR_UPDATE_UDP_INFO isWaittingCheckResult = ");
                            stringBuilder.append(HwQoEMonitorAdapter.this.isWaittingCheckResult);
                            HwQoEUtils.logD(stringBuilder.toString());
                            if (!HwQoEMonitorAdapter.this.isWaittingCheckResult && HwQoEMonitorAdapter.this.isHaveInternet) {
                                HwQoEMonitorAdapter.this.detectNetworkQuality(null, true);
                                break;
                            }
                        case 103:
                            HwQoEUtils.logD("QOE_MSG_HAVE_INTERNET");
                            if (HwQoEMonitorAdapter.this.isWaittingCheckResult) {
                                removeMessages(4000);
                                HwQoEMonitorAdapter.this.mCheckResult = HwQoEMonitorAdapter.this.NETWORK_HAVE_INTERNET;
                                break;
                            }
                            break;
                        case 104:
                            HwQoEUtils.logD("QOE_MSG_NO_INTERNET");
                            if (HwQoEMonitorAdapter.this.isWaittingCheckResult) {
                                removeMessages(4000);
                                HwQoEMonitorAdapter.this.mCheckResult = HwQoEMonitorAdapter.this.NETWORK_NO_INTERNET;
                                break;
                            }
                            break;
                    }
                } else {
                    HwQoEUtils.logD("QOE_MSG_MONITOR_START");
                    HwQoEMonitorAdapter.this.startMonitorMessage();
                }
                super.handleMessage(msg);
            }
        };
    }

    private void processCheckResult(HwQoENetWorkInfo firstInfo, HwQoENetWorkInfo secInfo) {
        StringBuilder stringBuilder;
        HwQoENetWorkInfo info = new HwQoENetWorkInfo();
        info.mTcpTxPacket = secInfo.mTcpTxPacket - firstInfo.mTcpTxPacket;
        info.mTcpRxPacket = secInfo.mTcpRxPacket - firstInfo.mTcpRxPacket;
        info.mDnsFailCount = secInfo.mDnsFailCount - firstInfo.mDnsFailCount;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("processCheckResult mTcpTxPacket = ");
        stringBuilder2.append(info.mTcpTxPacket);
        stringBuilder2.append(" info.mTcpRxPacket = ");
        stringBuilder2.append(info.mTcpRxPacket);
        stringBuilder2.append(" info.mDnsFailCount = ");
        stringBuilder2.append(info.mDnsFailCount);
        HwQoEUtils.logD(stringBuilder2.toString());
        if (this.isHaveInternet) {
            if (!detectTcpNetworkAvailable(info) && this.mCheckResult == this.NETWORK_NO_INTERNET) {
                try {
                    HwQoEUtils.logD("processCheckResult callback have no internet");
                    this.mCallback.onNetworkStateChange(this.NETWORK_NO_INTERNET);
                } catch (RemoteException e) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("processCheckResult error ");
                    stringBuilder.append(e.toString());
                    HwQoEUtils.logE(stringBuilder.toString());
                }
                sendNetworkBroadcast(this.NETWORK_NO_INTERNET);
                this.isHaveInternet = false;
            }
        } else if (detectTcpNetworkAvailable(info) && this.mCheckResult == this.NETWORK_HAVE_INTERNET) {
            try {
                HwQoEUtils.logD("processCheckResult callback have internet");
                this.mCallback.onNetworkStateChange(this.NETWORK_HAVE_INTERNET);
            } catch (RemoteException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("processCheckResult error ");
                stringBuilder.append(e2.toString());
                HwQoEUtils.logE(stringBuilder.toString());
            }
            sendNetworkBroadcast(this.NETWORK_HAVE_INTERNET);
            this.isHaveInternet = true;
        }
    }

    private void haveInternetProcess(HwQoENetWorkInfo info, boolean noUdpAccess) {
        boolean isInternet;
        int disableCount;
        if (info == null) {
            isInternet = noUdpAccess ^ 1;
        } else if (detectTcpNetworkAvailable(info)) {
            isInternet = true;
        } else {
            isInternet = false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("haveInternetProcess isInternet = ");
        stringBuilder.append(isInternet);
        stringBuilder.append(" noUdpAccess = ");
        stringBuilder.append(noUdpAccess);
        stringBuilder.append(" mNetworkDisableCount = ");
        stringBuilder.append(this.mNetworkDisableCount);
        HwQoEUtils.logD(stringBuilder.toString());
        if (this.mVoWIFIStatus == 1) {
            disableCount = 1;
        } else {
            disableCount = 2;
        }
        if (isInternet) {
            this.mNetworkDisableCount = 0;
            return;
        }
        this.mNetworkDisableCount++;
        if (this.mNetworkDisableCount >= disableCount) {
            startNetworkChecking();
        }
    }

    private void haveNoInternetProcess(HwQoENetWorkInfo info, boolean isUDPAvailable) {
        boolean isInternet;
        if (info == null) {
            isInternet = isUDPAvailable ^ 1;
        } else if (info.mTcpRxPacket > 0) {
            isInternet = true;
        } else {
            isInternet = false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("haveNoInternetProcess isUDPAvailable = ");
        stringBuilder.append(isUDPAvailable);
        stringBuilder.append(" isInternet = ");
        stringBuilder.append(isInternet);
        HwQoEUtils.logD(stringBuilder.toString());
        if (isInternet) {
            startNetworkChecking();
        }
    }

    private void detectNetworkQuality(HwQoENetWorkInfo info, boolean noUdpAccess) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("detectNetworkQuality noUdpAccess = ");
        stringBuilder.append(noUdpAccess);
        HwQoEUtils.logD(stringBuilder.toString());
        if (this.isHaveInternet) {
            haveInternetProcess(info, noUdpAccess);
        } else {
            haveNoInternetProcess(info, noUdpAccess);
        }
        HwQoEUtils.logE("***************************period end ***************************");
    }

    private boolean detectTcpNetworkAvailable(HwQoENetWorkInfo info) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("detectTcpNetworkAvailable info.mPeriodTcpRxPacket = ");
        stringBuilder.append(info.mTcpRxPacket);
        stringBuilder.append(" info.mPeriodTcpTxPacket = ");
        stringBuilder.append(info.mTcpTxPacket);
        stringBuilder.append(" info.mPeriodDnsFailCount = ");
        stringBuilder.append(info.mDnsFailCount);
        HwQoEUtils.logE(stringBuilder.toString());
        if (0 != info.mTcpRxPacket || (info.mTcpTxPacket <= 4 && info.mDnsFailCount <= 0)) {
            return true;
        }
        return false;
    }

    public void onNetworkInfoUpdate(HwQoENetWorkInfo info) {
        HwQoEUtils.logE("onNetworkInfoUpdate");
        if (info != null) {
            this.mHwQoENetWorkInfo = info;
            this.mLocalHandler.sendEmptyMessage(101);
        }
    }

    public void onUDPInternetAccessStatusChange(boolean noUdpAccess) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onUDPInternetAccessStatusChange, noUdpAccess:");
        stringBuilder.append(noUdpAccess);
        HwQoEUtils.logD(stringBuilder.toString());
        if (!noUdpAccess) {
            return;
        }
        if (this.mConifg.mPeriod > 2) {
            this.mLocalHandler.sendEmptyMessage(102);
            return;
        }
        try {
            HwQoEUtils.logD("onUDPInternetAccessStatusChange callback have no internet");
            this.mCallback.onNetworkStateChange(this.NETWORK_NO_INTERNET);
        } catch (RemoteException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onUDPInternetAccessStatusChange error ");
            stringBuilder2.append(e.toString());
            HwQoEUtils.logE(stringBuilder2.toString());
        }
        sendNetworkBroadcast(this.NETWORK_NO_INTERNET);
        this.isHaveInternet = false;
    }

    public void startMonitor() {
        this.mLocalHandler.sendEmptyMessage(HwQoEUtils.QOE_MSG_MONITOR_START);
    }

    private void startMonitorMessage() {
        this.mHwQoENetWorkMonitor.startMonitor();
        if (this.mVoWIFIStatus == 1) {
            this.mHwQoEUdpServiceImpl.setUDPInternetAccessMonitorEnabled(true);
        }
        if (!this.isStartMonitor) {
            this.isStartMonitor = true;
            this.isInitCheck = true;
            startNetworkChecking();
        }
    }

    public void stopMonitor() {
        this.isStartMonitor = false;
        this.isHaveInternet = false;
        this.mHwQoENetWorkMonitor.stopMonitor();
        this.mHwQoEUdpServiceImpl.setUDPInternetAccessMonitorEnabled(false);
    }

    public void updateVOWIFIState(int state) {
        this.mVoWIFIStatus = state;
        if (this.mVoWIFIStatus == 1) {
            this.mHwQoEUdpServiceImpl.setUDPInternetAccessMonitorEnabled(true);
        } else {
            this.mHwQoEUdpServiceImpl.setUDPInternetAccessMonitorEnabled(false);
        }
    }

    public void updateCallback(IHwQoECallback callback) {
        if (callback != null) {
            this.mCallback = callback;
        }
    }

    private void sendNetworkBroadcast(int state) {
        Intent intent = new Intent(VOWIFI_NETWORK_ACTION);
        intent.putExtra("state", state);
        this.mContext.sendBroadcast(intent, "android.permission.ACCESS_WIFI_STATE");
    }

    private void startNetworkChecking() {
        this.isWaittingCheckResult = true;
        this.mCheckInfo = this.mHwQoEJNIAdapter.queryPeriodData();
        this.mCheckInfo.mDnsFailCount = this.mHwQoENetWorkMonitor.getDnsFaileCount();
        this.mCheckResult = 0;
        new HwQoENetworkChecker(this.mContext, this.mLocalHandler).start();
        this.mLocalHandler.sendEmptyMessageDelayed(HwQoEUtils.QOE_MSG_WIFI_CHECK_TIMEOUT, WifiHandover.HANDOVER_WAIT_SCAN_TIME_OUT);
    }

    private void initCheckResult(HwQoENetWorkInfo firstInfo, HwQoENetWorkInfo secInfo) {
        HwQoENetWorkInfo info = new HwQoENetWorkInfo();
        info.mTcpTxPacket = secInfo.mTcpTxPacket - firstInfo.mTcpTxPacket;
        info.mTcpRxPacket = secInfo.mTcpRxPacket - firstInfo.mTcpRxPacket;
        info.mDnsFailCount = secInfo.mDnsFailCount - firstInfo.mDnsFailCount;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("initCheckResult mTcpTxPacket = ");
        stringBuilder.append(info.mTcpTxPacket);
        stringBuilder.append(" info.mTcpRxPacket = ");
        stringBuilder.append(info.mTcpRxPacket);
        stringBuilder.append(" info.mDnsFailCount = ");
        stringBuilder.append(info.mDnsFailCount);
        HwQoEUtils.logD(stringBuilder.toString());
        if (detectTcpNetworkAvailable(info) && this.mCheckResult == this.NETWORK_HAVE_INTERNET) {
            try {
                HwQoEUtils.logD("initCheckResult callback have internet");
                this.mCallback.onNetworkStateChange(this.NETWORK_HAVE_INTERNET);
            } catch (RemoteException e) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("initCheckResult error ");
                stringBuilder2.append(e.toString());
                HwQoEUtils.logE(stringBuilder2.toString());
            }
            sendNetworkBroadcast(this.NETWORK_HAVE_INTERNET);
            this.isHaveInternet = true;
            return;
        }
        this.isHaveInternet = false;
    }

    public void release() {
        if (this.mLocalHandler != null) {
            Looper looper = this.mLocalHandler.getLooper();
            if (!(looper == null || looper == Looper.getMainLooper())) {
                looper.quitSafely();
                HwQoEUtils.logD("HwQoEMonitorAdapter$HandlerThread::Release");
            }
        }
        this.mHwQoENetWorkMonitor.release();
        this.mHwQoEUdpServiceImpl.release();
    }
}
