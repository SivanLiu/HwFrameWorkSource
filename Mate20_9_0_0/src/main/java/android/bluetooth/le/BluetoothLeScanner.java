package android.bluetooth.le;

import android.annotation.SystemApi;
import android.app.ActivityThread;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.IBluetoothGatt;
import android.bluetooth.IBluetoothManager;
import android.bluetooth.le.IScannerCallback.Stub;
import android.bluetooth.le.ScanSettings.Builder;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.WorkSource;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BluetoothLeScanner {
    private static final boolean DBG = true;
    public static final String EXTRA_CALLBACK_TYPE = "android.bluetooth.le.extra.CALLBACK_TYPE";
    public static final String EXTRA_ERROR_CODE = "android.bluetooth.le.extra.ERROR_CODE";
    public static final String EXTRA_LIST_SCAN_RESULT = "android.bluetooth.le.extra.LIST_SCAN_RESULT";
    private static final String TAG = "BluetoothLeScanner";
    private static final boolean VDBG = false;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private final IBluetoothManager mBluetoothManager;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Map<ScanCallback, BleScanCallbackWrapper> mLeScanClients = new HashMap();

    private class BleScanCallbackWrapper extends Stub {
        private static final int REGISTRATION_CALLBACK_TIMEOUT_MILLIS = 2000;
        private IBluetoothGatt mBluetoothGatt;
        private final List<ScanFilter> mFilters;
        private List<List<ResultStorageDescriptor>> mResultStorages;
        private final ScanCallback mScanCallback;
        private int mScannerId = 0;
        private ScanSettings mSettings;
        private final WorkSource mWorkSource;

        public BleScanCallbackWrapper(IBluetoothGatt bluetoothGatt, List<ScanFilter> filters, ScanSettings settings, WorkSource workSource, ScanCallback scanCallback, List<List<ResultStorageDescriptor>> resultStorages) {
            this.mBluetoothGatt = bluetoothGatt;
            this.mFilters = filters;
            this.mSettings = settings;
            this.mWorkSource = workSource;
            this.mScanCallback = scanCallback;
            this.mResultStorages = resultStorages;
        }

        /* JADX WARNING: Missing block: B:24:0x004e, code skipped:
            return;
     */
        /* JADX WARNING: Missing block: B:26:0x0050, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void startRegistration() {
            synchronized (this) {
                if (this.mScannerId == -1 || this.mScannerId == -2) {
                } else {
                    try {
                        this.mBluetoothGatt.registerScanner(this, this.mWorkSource);
                        wait(2000);
                    } catch (RemoteException | InterruptedException e) {
                        Log.e(BluetoothLeScanner.TAG, "application registeration exception", e);
                        BluetoothLeScanner.this.postCallbackError(this.mScanCallback, 3);
                    }
                    if (this.mScannerId > 0) {
                        BluetoothLeScanner.this.mLeScanClients.put(this.mScanCallback, this);
                    } else {
                        if (this.mScannerId == 0) {
                            this.mScannerId = -1;
                        }
                        if (this.mScannerId == -2) {
                            return;
                        }
                        BluetoothLeScanner.this.postCallbackError(this.mScanCallback, 2);
                    }
                }
            }
        }

        public void stopLeScan() {
            synchronized (this) {
                if (this.mScannerId <= 0) {
                    String str = BluetoothLeScanner.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Error state, mLeHandle: ");
                    stringBuilder.append(this.mScannerId);
                    Log.e(str, stringBuilder.toString());
                    return;
                }
                try {
                    this.mBluetoothGatt.stopScan(this.mScannerId);
                    this.mBluetoothGatt.unregisterScanner(this.mScannerId);
                } catch (RemoteException e) {
                    Log.e(BluetoothLeScanner.TAG, "Failed to stop scan and unregister", e);
                }
                this.mScannerId = -1;
            }
        }

        public void updateLeScanParams(int window, int interval) {
            String str = BluetoothLeScanner.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateLeScanParams win:");
            stringBuilder.append(window);
            stringBuilder.append(" ivl:");
            stringBuilder.append(interval);
            Log.e(str, stringBuilder.toString());
            synchronized (this) {
                if (this.mScannerId <= 0) {
                    str = BluetoothLeScanner.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error state, mLeHandle: ");
                    stringBuilder.append(this.mScannerId);
                    Log.e(str, stringBuilder.toString());
                    return;
                }
                try {
                    this.mBluetoothGatt.updateScanParams(this.mScannerId, false, window, interval);
                } catch (RemoteException e) {
                    Log.e(BluetoothLeScanner.TAG, "Failed to stop scan and unregister", e);
                }
            }
        }

        void flushPendingBatchResults() {
            synchronized (this) {
                if (this.mScannerId <= 0) {
                    String str = BluetoothLeScanner.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Error state, mLeHandle: ");
                    stringBuilder.append(this.mScannerId);
                    Log.e(str, stringBuilder.toString());
                    return;
                }
                try {
                    this.mBluetoothGatt.flushPendingBatchResults(this.mScannerId);
                } catch (RemoteException e) {
                    Log.e(BluetoothLeScanner.TAG, "Failed to get pending scan results", e);
                }
            }
        }

        public void onScannerRegistered(int status, int scannerId) {
            String str = BluetoothLeScanner.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onScannerRegistered() - status=");
            stringBuilder.append(status);
            stringBuilder.append(" scannerId=");
            stringBuilder.append(scannerId);
            stringBuilder.append(" mScannerId=");
            stringBuilder.append(this.mScannerId);
            Log.d(str, stringBuilder.toString());
            synchronized (this) {
                if (status == 0) {
                    try {
                        if (this.mScannerId == -1) {
                            this.mBluetoothGatt.unregisterClient(scannerId);
                        } else {
                            this.mScannerId = scannerId;
                            this.mBluetoothGatt.startScan(this.mScannerId, this.mSettings, this.mFilters, this.mResultStorages, ActivityThread.currentOpPackageName());
                        }
                    } catch (RemoteException e) {
                        String str2 = BluetoothLeScanner.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("fail to start le scan: ");
                        stringBuilder2.append(e);
                        Log.e(str2, stringBuilder2.toString());
                        this.mScannerId = -1;
                    }
                } else if (status == 6) {
                    this.mScannerId = -2;
                } else {
                    this.mScannerId = -1;
                }
                notifyAll();
            }
        }

        public void onScanResult(final ScanResult scanResult) {
            synchronized (this) {
                if (this.mScannerId <= 0) {
                    return;
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        BleScanCallbackWrapper.this.mScanCallback.onScanResult(1, scanResult);
                    }
                });
            }
        }

        public void onBatchScanResults(final List<ScanResult> results) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                public void run() {
                    BleScanCallbackWrapper.this.mScanCallback.onBatchScanResults(results);
                }
            });
        }

        public void onFoundOrLost(final boolean onFound, final ScanResult scanResult) {
            synchronized (this) {
                if (this.mScannerId <= 0) {
                    return;
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        if (onFound) {
                            BleScanCallbackWrapper.this.mScanCallback.onScanResult(2, scanResult);
                        } else {
                            BleScanCallbackWrapper.this.mScanCallback.onScanResult(4, scanResult);
                        }
                    }
                });
            }
        }

        public void onScanManagerErrorCallback(int errorCode) {
            synchronized (this) {
                if (this.mScannerId <= 0) {
                    return;
                }
                BluetoothLeScanner.this.postCallbackError(this.mScanCallback, errorCode);
            }
        }
    }

    public BluetoothLeScanner(IBluetoothManager bluetoothManager) {
        this.mBluetoothManager = bluetoothManager;
    }

    public void startScan(ScanCallback callback) {
        startScan(null, new Builder().build(), callback);
    }

    public void startScan(List<ScanFilter> filters, ScanSettings settings, ScanCallback callback) {
        startScan(filters, settings, null, callback, null, null);
    }

    public int startScan(List<ScanFilter> filters, ScanSettings settings, PendingIntent callbackIntent) {
        return startScan(filters, settings != null ? settings : new Builder().build(), null, null, callbackIntent, null);
    }

    @SystemApi
    public void startScanFromSource(WorkSource workSource, ScanCallback callback) {
        startScanFromSource(null, new Builder().build(), workSource, callback);
    }

    @SystemApi
    public void startScanFromSource(List<ScanFilter> filters, ScanSettings settings, WorkSource workSource, ScanCallback callback) {
        startScan(filters, settings, workSource, callback, null, null);
    }

    private int startScan(List<ScanFilter> filters, ScanSettings settings, WorkSource workSource, ScanCallback callback, PendingIntent callbackIntent, List<List<ResultStorageDescriptor>> resultStorages) {
        List<ScanFilter> list = filters;
        ScanSettings scanSettings = settings;
        ScanCallback scanCallback = callback;
        PendingIntent pendingIntent = callbackIntent;
        BluetoothLeUtils.checkAdapterStateOn(this.mBluetoothAdapter);
        if (scanCallback == null && pendingIntent == null) {
            throw new IllegalArgumentException("callback is null");
        } else if (scanSettings != null) {
            synchronized (this.mLeScanClients) {
                int postCallbackErrorOrReturn;
                IBluetoothGatt gatt;
                if (scanCallback != null) {
                    if (this.mLeScanClients.containsKey(scanCallback)) {
                        postCallbackErrorOrReturn = postCallbackErrorOrReturn(scanCallback, 1);
                        return postCallbackErrorOrReturn;
                    }
                }
                try {
                    gatt = this.mBluetoothManager.getBluetoothGatt();
                } catch (RemoteException e) {
                    gatt = null;
                }
                IBluetoothGatt gatt2 = gatt;
                if (gatt2 == null) {
                    postCallbackErrorOrReturn = postCallbackErrorOrReturn(scanCallback, 3);
                    return postCallbackErrorOrReturn;
                } else if (!isSettingsConfigAllowedForScan(scanSettings)) {
                    postCallbackErrorOrReturn = postCallbackErrorOrReturn(scanCallback, 4);
                    return postCallbackErrorOrReturn;
                } else if (!isHardwareResourcesAvailableForScan(scanSettings)) {
                    postCallbackErrorOrReturn = postCallbackErrorOrReturn(scanCallback, 5);
                    return postCallbackErrorOrReturn;
                } else if (isSettingsAndFilterComboAllowed(scanSettings, list)) {
                    if (scanCallback != null) {
                        new BleScanCallbackWrapper(gatt2, list, scanSettings, workSource, scanCallback, resultStorages).startRegistration();
                    } else {
                        try {
                            gatt2.startScanForIntent(pendingIntent, scanSettings, list, ActivityThread.currentOpPackageName());
                        } catch (RemoteException e2) {
                            return 3;
                        }
                    }
                    return 0;
                } else {
                    postCallbackErrorOrReturn = postCallbackErrorOrReturn(scanCallback, 4);
                    return postCallbackErrorOrReturn;
                }
            }
        } else {
            throw new IllegalArgumentException("settings is null");
        }
    }

    public void stopScan(ScanCallback callback) {
        BluetoothLeUtils.checkAdapterStateOn(this.mBluetoothAdapter);
        synchronized (this.mLeScanClients) {
            BleScanCallbackWrapper wrapper = (BleScanCallbackWrapper) this.mLeScanClients.remove(callback);
            if (wrapper == null) {
                Log.d(TAG, "could not find callback wrapper");
                return;
            }
            wrapper.stopLeScan();
        }
    }

    public void updateScanParams(ScanCallback callback, int window, int interval) {
        Log.i(TAG, "updateScanParams");
        BluetoothLeUtils.checkAdapterStateOn(this.mBluetoothAdapter);
        synchronized (this.mLeScanClients) {
            BleScanCallbackWrapper wrapper = (BleScanCallbackWrapper) this.mLeScanClients.get(callback);
            if (wrapper == null) {
                Log.d(TAG, "could not find callback wrapper");
                return;
            }
            wrapper.updateLeScanParams(window, interval);
        }
    }

    public void stopLeScanByPkg(String pkgName) {
        if (pkgName != null && !TextUtils.isEmpty(pkgName)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("stopLeScanByPkg() ");
            stringBuilder.append(pkgName);
            Log.d(str, stringBuilder.toString());
            try {
                this.mBluetoothManager.getBluetoothGatt().stopScanByPkg(pkgName);
            } catch (RemoteException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to get stopLeScanByPkg, ");
                stringBuilder2.append(e.getMessage());
                Log.e(str2, stringBuilder2.toString());
            }
        }
    }

    public void startLeScanByPkg(String pkgName) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startLeScanByPkg() ");
        stringBuilder.append(pkgName);
        Log.d(str, stringBuilder.toString());
        try {
            this.mBluetoothManager.getBluetoothGatt().startScanByPkg(pkgName);
        } catch (RemoteException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to get startLeScanByPkg, ");
            stringBuilder2.append(e.getMessage());
            Log.e(str2, stringBuilder2.toString());
        }
    }

    public void stopScan(PendingIntent callbackIntent) {
        BluetoothLeUtils.checkAdapterStateOn(this.mBluetoothAdapter);
        try {
            this.mBluetoothManager.getBluetoothGatt().stopScanForIntent(callbackIntent, ActivityThread.currentOpPackageName());
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to get stopScan, ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
    }

    public void flushPendingScanResults(ScanCallback callback) {
        BluetoothLeUtils.checkAdapterStateOn(this.mBluetoothAdapter);
        if (callback != null) {
            synchronized (this.mLeScanClients) {
                BleScanCallbackWrapper wrapper = (BleScanCallbackWrapper) this.mLeScanClients.get(callback);
                if (wrapper == null) {
                    return;
                }
                wrapper.flushPendingBatchResults();
                return;
            }
        }
        throw new IllegalArgumentException("callback cannot be null!");
    }

    @SystemApi
    public void startTruncatedScan(List<TruncatedFilter> truncatedFilters, ScanSettings settings, ScanCallback callback) {
        int filterSize = truncatedFilters.size();
        List<ScanFilter> scanFilters = new ArrayList(filterSize);
        List scanStorages = new ArrayList(filterSize);
        for (TruncatedFilter filter : truncatedFilters) {
            scanFilters.add(filter.getFilter());
            scanStorages.add(filter.getStorageDescriptors());
        }
        startScan(scanFilters, settings, null, callback, null, scanStorages);
    }

    public void cleanup() {
        this.mLeScanClients.clear();
    }

    private int postCallbackErrorOrReturn(ScanCallback callback, int errorCode) {
        if (callback == null) {
            return errorCode;
        }
        postCallbackError(callback, errorCode);
        return 0;
    }

    private void postCallbackError(final ScanCallback callback, final int errorCode) {
        this.mHandler.post(new Runnable() {
            public void run() {
                callback.onScanFailed(errorCode);
            }
        });
    }

    private boolean isSettingsConfigAllowedForScan(ScanSettings settings) {
        if (this.mBluetoothAdapter.isOffloadedFilteringSupported()) {
            return true;
        }
        if (settings.getCallbackType() == 1 && settings.getReportDelayMillis() == 0) {
            return true;
        }
        return false;
    }

    private boolean isSettingsAndFilterComboAllowed(ScanSettings settings, List<ScanFilter> filterList) {
        if ((settings.getCallbackType() & 6) != 0) {
            if (filterList == null) {
                return false;
            }
            for (ScanFilter filter : filterList) {
                if (filter.isAllFieldsEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isHardwareResourcesAvailableForScan(ScanSettings settings) {
        int callbackType = settings.getCallbackType();
        boolean z = true;
        if ((callbackType & 2) == 0 && (callbackType & 4) == 0) {
            return true;
        }
        if (!(this.mBluetoothAdapter.isOffloadedFilteringSupported() && this.mBluetoothAdapter.isHardwareTrackingFiltersAvailable())) {
            z = false;
        }
        return z;
    }
}
