package com.android.internal.telephony;

import android.hardware.radio.V1_2.MaxSearchTimeRange;
import android.hardware.radio.V1_2.ScanIntervalRange;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.telephony.CellInfo;
import android.telephony.NetworkScanRequest;
import android.telephony.RadioAccessSpecifier;
import android.util.Log;
import com.android.internal.telephony.CommandException.Error;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class NetworkScanRequestTracker {
    private static final int CMD_INTERRUPT_NETWORK_SCAN = 6;
    private static final int CMD_START_NETWORK_SCAN = 1;
    private static final int CMD_STOP_NETWORK_SCAN = 4;
    private static final int EVENT_INTERRUPT_NETWORK_SCAN_DONE = 7;
    private static final int EVENT_RECEIVE_NETWORK_SCAN_RESULT = 3;
    private static final int EVENT_START_NETWORK_SCAN_DONE = 2;
    private static final int EVENT_STOP_NETWORK_SCAN_DONE = 5;
    private static final String TAG = "ScanRequestTracker";
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    NetworkScanRequestTracker.this.mScheduler.doStartScan((NetworkScanRequestInfo) msg.obj);
                    return;
                case 2:
                    NetworkScanRequestTracker.this.mScheduler.startScanDone((AsyncResult) msg.obj);
                    return;
                case 3:
                    NetworkScanRequestTracker.this.mScheduler.receiveResult((AsyncResult) msg.obj);
                    return;
                case 4:
                    NetworkScanRequestTracker.this.mScheduler.doStopScan(msg.arg1);
                    return;
                case 5:
                    NetworkScanRequestTracker.this.mScheduler.stopScanDone((AsyncResult) msg.obj);
                    return;
                case 6:
                    NetworkScanRequestTracker.this.mScheduler.doInterruptScan(msg.arg1);
                    return;
                case 7:
                    NetworkScanRequestTracker.this.mScheduler.interruptScanDone((AsyncResult) msg.obj);
                    return;
                default:
                    return;
            }
        }
    };
    private final AtomicInteger mNextNetworkScanRequestId = new AtomicInteger(1);
    private final NetworkScanRequestScheduler mScheduler = new NetworkScanRequestScheduler(this, null);

    class NetworkScanRequestInfo implements DeathRecipient {
        private final IBinder mBinder;
        private boolean mIsBinderDead = false;
        private final Messenger mMessenger;
        private final Phone mPhone;
        private final int mPid = Binder.getCallingPid();
        private final NetworkScanRequest mRequest;
        private final int mScanId;
        private final int mUid = Binder.getCallingUid();

        NetworkScanRequestInfo(NetworkScanRequest r, Messenger m, IBinder b, int id, Phone phone) {
            this.mRequest = r;
            this.mMessenger = m;
            this.mBinder = b;
            this.mScanId = id;
            this.mPhone = phone;
            try {
                this.mBinder.linkToDeath(this, 0);
            } catch (RemoteException e) {
                binderDied();
            }
        }

        synchronized void setIsBinderDead(boolean val) {
            this.mIsBinderDead = val;
        }

        synchronized boolean getIsBinderDead() {
            return this.mIsBinderDead;
        }

        NetworkScanRequest getRequest() {
            return this.mRequest;
        }

        void unlinkDeathRecipient() {
            if (this.mBinder != null) {
                this.mBinder.unlinkToDeath(this, 0);
            }
        }

        public void binderDied() {
            String str = NetworkScanRequestTracker.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("PhoneInterfaceManager NetworkScanRequestInfo binderDied(");
            stringBuilder.append(this.mRequest);
            stringBuilder.append(", ");
            stringBuilder.append(this.mBinder);
            stringBuilder.append(")");
            Log.e(str, stringBuilder.toString());
            setIsBinderDead(true);
            NetworkScanRequestTracker.this.interruptNetworkScan(this.mScanId);
        }
    }

    private class NetworkScanRequestScheduler {
        private NetworkScanRequestInfo mLiveRequestInfo;
        private NetworkScanRequestInfo mPendingRequestInfo;

        private NetworkScanRequestScheduler() {
        }

        /* synthetic */ NetworkScanRequestScheduler(NetworkScanRequestTracker x0, AnonymousClass1 x1) {
            this();
        }

        private int rilErrorToScanError(int rilError) {
            switch (rilError) {
                case 0:
                    return 0;
                case 1:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: RADIO_NOT_AVAILABLE");
                    return 1;
                case 6:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: REQUEST_NOT_SUPPORTED");
                    return 4;
                case 37:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: NO_MEMORY");
                    return 1;
                case 38:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: INTERNAL_ERR");
                    return 1;
                case 40:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: MODEM_ERR");
                    return 1;
                case 44:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: INVALID_ARGUMENTS");
                    return 2;
                case 54:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: OPERATION_NOT_ALLOWED");
                    return 1;
                case 64:
                    Log.e(NetworkScanRequestTracker.TAG, "rilErrorToScanError: DEVICE_IN_USE");
                    return 3;
                default:
                    String str = NetworkScanRequestTracker.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("rilErrorToScanError: Unexpected RadioError ");
                    stringBuilder.append(rilError);
                    Log.e(str, stringBuilder.toString());
                    return 10000;
            }
        }

        private int commandExceptionErrorToScanError(Error error) {
            switch (error) {
                case RADIO_NOT_AVAILABLE:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: RADIO_NOT_AVAILABLE");
                    return 1;
                case REQUEST_NOT_SUPPORTED:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: REQUEST_NOT_SUPPORTED");
                    return 4;
                case NO_MEMORY:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: NO_MEMORY");
                    return 1;
                case INTERNAL_ERR:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: INTERNAL_ERR");
                    return 1;
                case MODEM_ERR:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: MODEM_ERR");
                    return 1;
                case OPERATION_NOT_ALLOWED:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: OPERATION_NOT_ALLOWED");
                    return 1;
                case INVALID_ARGUMENTS:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: INVALID_ARGUMENTS");
                    return 2;
                case DEVICE_IN_USE:
                    Log.e(NetworkScanRequestTracker.TAG, "commandExceptionErrorToScanError: DEVICE_IN_USE");
                    return 3;
                default:
                    String str = NetworkScanRequestTracker.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("commandExceptionErrorToScanError: Unexpected CommandExceptionError ");
                    stringBuilder.append(error);
                    Log.e(str, stringBuilder.toString());
                    return 10000;
            }
        }

        private void doStartScan(NetworkScanRequestInfo nsri) {
            if (nsri == null) {
                Log.e(NetworkScanRequestTracker.TAG, "CMD_START_NETWORK_SCAN: nsri is null");
            } else if (!NetworkScanRequestTracker.this.isValidScan(nsri)) {
                NetworkScanRequestTracker.this.notifyMessenger(nsri, 2, 2, null);
            } else if (nsri.getIsBinderDead()) {
                Log.e(NetworkScanRequestTracker.TAG, "CMD_START_NETWORK_SCAN: Binder has died");
            } else {
                if (!(startNewScan(nsri) || interruptLiveScan(nsri) || cacheScan(nsri))) {
                    NetworkScanRequestTracker.this.notifyMessenger(nsri, 2, 3, null);
                }
            }
        }

        /* JADX WARNING: Missing block: B:23:0x005c, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private synchronized void startScanDone(AsyncResult ar) {
            NetworkScanRequestInfo nsri = ar.userObj;
            if (nsri == null) {
                Log.e(NetworkScanRequestTracker.TAG, "EVENT_START_NETWORK_SCAN_DONE: nsri is null");
                return;
            }
            if (this.mLiveRequestInfo != null) {
                if (nsri.mScanId == this.mLiveRequestInfo.mScanId) {
                    if (ar.exception != null || ar.result == null) {
                        NetworkScanRequestTracker.this.logEmptyResultOrException(ar);
                        if (ar.exception != null) {
                            deleteScanAndMayNotify(nsri, commandExceptionErrorToScanError(((CommandException) ar.exception).getCommandError()), true);
                        } else {
                            Log.wtf(NetworkScanRequestTracker.TAG, "EVENT_START_NETWORK_SCAN_DONE: ar.exception can not be null!");
                        }
                    } else {
                        nsri.mPhone.mCi.registerForNetworkScanResult(NetworkScanRequestTracker.this.mHandler, 3, nsri);
                    }
                }
            }
            Log.e(NetworkScanRequestTracker.TAG, "EVENT_START_NETWORK_SCAN_DONE: nsri does not match mLiveRequestInfo");
        }

        private void receiveResult(AsyncResult ar) {
            NetworkScanRequestInfo nsri = ar.userObj;
            if (nsri == null) {
                Log.e(NetworkScanRequestTracker.TAG, "EVENT_RECEIVE_NETWORK_SCAN_RESULT: nsri is null");
                return;
            }
            if (ar.exception != null || ar.result == null) {
                NetworkScanRequestTracker.this.logEmptyResultOrException(ar);
                deleteScanAndMayNotify(nsri, 10000, true);
                nsri.mPhone.mCi.unregisterForNetworkScanResult(NetworkScanRequestTracker.this.mHandler);
            } else {
                NetworkScanResult nsr = ar.result;
                if (nsr.scanError == 0) {
                    NetworkScanRequestTracker.this.notifyMessenger(nsri, 1, rilErrorToScanError(nsr.scanError), nsr.networkInfos);
                    if (nsr.scanStatus == 2) {
                        deleteScanAndMayNotify(nsri, 0, true);
                        nsri.mPhone.mCi.unregisterForNetworkScanResult(NetworkScanRequestTracker.this.mHandler);
                    }
                } else {
                    if (nsr.networkInfos != null) {
                        NetworkScanRequestTracker.this.notifyMessenger(nsri, 1, 0, nsr.networkInfos);
                    }
                    deleteScanAndMayNotify(nsri, rilErrorToScanError(nsr.scanError), true);
                    nsri.mPhone.mCi.unregisterForNetworkScanResult(NetworkScanRequestTracker.this.mHandler);
                }
            }
        }

        private synchronized void doStopScan(int scanId) {
            if (this.mLiveRequestInfo != null && scanId == this.mLiveRequestInfo.mScanId) {
                this.mLiveRequestInfo.mPhone.stopNetworkScan(NetworkScanRequestTracker.this.mHandler.obtainMessage(5, this.mLiveRequestInfo));
            } else if (this.mPendingRequestInfo == null || scanId != this.mPendingRequestInfo.mScanId) {
                String str = NetworkScanRequestTracker.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("stopScan: scan ");
                stringBuilder.append(scanId);
                stringBuilder.append(" does not exist!");
                Log.e(str, stringBuilder.toString());
            } else {
                NetworkScanRequestTracker.this.notifyMessenger(this.mPendingRequestInfo, 3, 0, null);
                this.mPendingRequestInfo = null;
            }
        }

        private void stopScanDone(AsyncResult ar) {
            NetworkScanRequestInfo nsri = ar.userObj;
            if (nsri == null) {
                Log.e(NetworkScanRequestTracker.TAG, "EVENT_STOP_NETWORK_SCAN_DONE: nsri is null");
                return;
            }
            if (ar.exception != null || ar.result == null) {
                NetworkScanRequestTracker.this.logEmptyResultOrException(ar);
                if (ar.exception != null) {
                    deleteScanAndMayNotify(nsri, commandExceptionErrorToScanError(((CommandException) ar.exception).getCommandError()), true);
                } else {
                    Log.wtf(NetworkScanRequestTracker.TAG, "EVENT_STOP_NETWORK_SCAN_DONE: ar.exception can not be null!");
                }
            } else {
                deleteScanAndMayNotify(nsri, 0, true);
            }
            nsri.mPhone.mCi.unregisterForNetworkScanResult(NetworkScanRequestTracker.this.mHandler);
        }

        private synchronized void doInterruptScan(int scanId) {
            if (this.mLiveRequestInfo == null || scanId != this.mLiveRequestInfo.mScanId) {
                String str = NetworkScanRequestTracker.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("doInterruptScan: scan ");
                stringBuilder.append(scanId);
                stringBuilder.append(" does not exist!");
                Log.e(str, stringBuilder.toString());
            } else {
                this.mLiveRequestInfo.mPhone.stopNetworkScan(NetworkScanRequestTracker.this.mHandler.obtainMessage(7, this.mLiveRequestInfo));
            }
        }

        private void interruptScanDone(AsyncResult ar) {
            NetworkScanRequestInfo nsri = ar.userObj;
            if (nsri == null) {
                Log.e(NetworkScanRequestTracker.TAG, "EVENT_INTERRUPT_NETWORK_SCAN_DONE: nsri is null");
                return;
            }
            nsri.mPhone.mCi.unregisterForNetworkScanResult(NetworkScanRequestTracker.this.mHandler);
            deleteScanAndMayNotify(nsri, 0, false);
        }

        private synchronized boolean interruptLiveScan(NetworkScanRequestInfo nsri) {
            if (this.mLiveRequestInfo == null || this.mPendingRequestInfo != null || nsri.mUid != 1001 || this.mLiveRequestInfo.mUid == 1001) {
                return false;
            }
            doInterruptScan(this.mLiveRequestInfo.mScanId);
            this.mPendingRequestInfo = nsri;
            NetworkScanRequestTracker.this.notifyMessenger(this.mLiveRequestInfo, 2, 10002, null);
            return true;
        }

        private boolean cacheScan(NetworkScanRequestInfo nsri) {
            return false;
        }

        private synchronized boolean startNewScan(NetworkScanRequestInfo nsri) {
            if (this.mLiveRequestInfo != null) {
                return false;
            }
            this.mLiveRequestInfo = nsri;
            nsri.mPhone.startNetworkScan(nsri.getRequest(), NetworkScanRequestTracker.this.mHandler.obtainMessage(2, nsri));
            return true;
        }

        private synchronized void deleteScanAndMayNotify(NetworkScanRequestInfo nsri, int error, boolean notify) {
            if (this.mLiveRequestInfo != null && nsri.mScanId == this.mLiveRequestInfo.mScanId) {
                if (notify) {
                    if (error == 0) {
                        NetworkScanRequestTracker.this.notifyMessenger(nsri, 3, error, null);
                    } else {
                        NetworkScanRequestTracker.this.notifyMessenger(nsri, 2, error, null);
                    }
                }
                this.mLiveRequestInfo = null;
                if (this.mPendingRequestInfo != null) {
                    startNewScan(this.mPendingRequestInfo);
                    this.mPendingRequestInfo = null;
                }
            }
        }
    }

    private void logEmptyResultOrException(AsyncResult ar) {
        if (ar.result == null) {
            Log.e(TAG, "NetworkScanResult: Empty result");
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("NetworkScanResult: Exception: ");
        stringBuilder.append(ar.exception);
        Log.e(str, stringBuilder.toString());
    }

    /* JADX WARNING: Missing block: B:51:0x00f1, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:52:0x00f2, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:53:0x00f3, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:54:0x00f4, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isValidScan(NetworkScanRequestInfo nsri) {
        if (nsri.mRequest == null || nsri.mRequest.getSpecifiers() == null || nsri.mRequest.getSpecifiers().length > 8) {
            return false;
        }
        for (RadioAccessSpecifier ras : nsri.mRequest.getSpecifiers()) {
            if (ras.getRadioAccessNetwork() != 1 && ras.getRadioAccessNetwork() != 2 && ras.getRadioAccessNetwork() != 3) {
                return false;
            }
            if (ras.getBands() != null && ras.getBands().length > 8) {
                return false;
            }
            if (ras.getChannels() != null && ras.getChannels().length > 32) {
                return false;
            }
        }
        if (nsri.mRequest.getSearchPeriodicity() < 5 || nsri.mRequest.getSearchPeriodicity() > ScanIntervalRange.MAX || nsri.mRequest.getMaxSearchTime() < 60 || nsri.mRequest.getMaxSearchTime() > MaxSearchTimeRange.MAX || nsri.mRequest.getIncrementalResultsPeriodicity() < 1 || nsri.mRequest.getIncrementalResultsPeriodicity() > 10 || nsri.mRequest.getSearchPeriodicity() > nsri.mRequest.getMaxSearchTime() || nsri.mRequest.getIncrementalResultsPeriodicity() > nsri.mRequest.getMaxSearchTime()) {
            return false;
        }
        if (nsri.mRequest.getPlmns() == null || nsri.mRequest.getPlmns().size() <= 20) {
            return true;
        }
        return false;
    }

    private void notifyMessenger(NetworkScanRequestInfo nsri, int what, int err, List<CellInfo> result) {
        Messenger messenger = nsri.mMessenger;
        Message message = Message.obtain();
        message.what = what;
        message.arg1 = err;
        message.arg2 = nsri.mScanId;
        if (result != null) {
            CellInfo[] ci = (CellInfo[]) result.toArray(new CellInfo[result.size()]);
            Bundle b = new Bundle();
            b.putParcelableArray("scanResult", ci);
            message.setData(b);
        } else {
            message.obj = null;
        }
        try {
            messenger.send(message);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception in notifyMessenger: ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        }
    }

    private void interruptNetworkScan(int scanId) {
        this.mHandler.obtainMessage(6, scanId, 0).sendToTarget();
    }

    public int startNetworkScan(NetworkScanRequest request, Messenger messenger, IBinder binder, Phone phone) {
        int scanId = this.mNextNetworkScanRequestId.getAndIncrement();
        this.mHandler.obtainMessage(1, new NetworkScanRequestInfo(request, messenger, binder, scanId, phone)).sendToTarget();
        return scanId;
    }

    public void stopNetworkScan(int scanId) {
        synchronized (this.mScheduler) {
            if ((this.mScheduler.mLiveRequestInfo != null && scanId == this.mScheduler.mLiveRequestInfo.mScanId && Binder.getCallingUid() == this.mScheduler.mLiveRequestInfo.mUid) || (this.mScheduler.mPendingRequestInfo != null && scanId == this.mScheduler.mPendingRequestInfo.mScanId && Binder.getCallingUid() == this.mScheduler.mPendingRequestInfo.mUid)) {
                this.mHandler.obtainMessage(4, scanId, 0).sendToTarget();
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Scan with id: ");
                stringBuilder.append(scanId);
                stringBuilder.append(" does not exist!");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
    }
}
