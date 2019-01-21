package com.huawei.nearbysdk;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build.VERSION;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import com.huawei.nearbysdk.IAuthAdapter.Stub;
import com.huawei.nearbysdk.NearbyConfig.BusinessTypeEnum;
import com.huawei.nearbysdk.closeRange.CloseRangeAdapter;
import com.huawei.nearbysdk.closeRange.CloseRangeBusinessType;
import com.huawei.nearbysdk.closeRange.CloseRangeDeviceFilter;
import com.huawei.nearbysdk.closeRange.CloseRangeDeviceListener;
import com.huawei.nearbysdk.closeRange.CloseRangeEventFilter;
import com.huawei.nearbysdk.closeRange.CloseRangeEventListener;
import com.huawei.nearbysdk.closeRange.CloseRangeInterface;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;

public final class NearbyAdapter implements CloseRangeInterface {
    private static final int INVALIDE_CHANNEL = -1;
    private static final int INVALIDE_TYPE_CHANNEL = -1;
    static final String TAG = "NearbyServiceJar";
    private static CloseRangeAdapter closeRangeAdapter = null;
    private static final Object closeRangeLock = new Object();
    private static Boolean mAuthBound = Boolean.valueOf(false);
    private static NearbyAuthServiceConnection mAuthConnection;
    private static final Object mAuthLock = new Object();
    private static IAuthAdapter mAuthService;
    private static NearbyServiceConnection mConnection;
    private static final HashMap<String, byte[]> mDeviceSum_SessionKeys = new HashMap();
    private static NearbyAdapter mNearbyAdapter;
    private static Boolean mNearbyBound = Boolean.valueOf(false);
    private static Context mNearbyContext;
    private static final Object mNearbyLock = new Object();
    private static INearbyAdapter mNearbyService;
    private static HandlerThread mThread;
    private final HashMap<Long, NearbyDevice> mAuthId_Device;
    private final HashMap<AuthListener, AuthListenerTransport> mAuthListeners;
    private final HashMap<ConnectionListener, ConnectionListenerTransport> mConnectionListeners;
    private final HashMap<CreateSocketListener, CreateSocketListenerTransport> mCreateSocketListeners;
    private final HashMap<String, byte[]> mDevice_RSAKeys;
    private final HashMap<AuthListener, Integer> mMapBussinessIdListener;
    private final HashMap<PublishListener, PublishListenerTransport> mPublishListeners;
    private final HashMap<SocketListener, SocketListenerTransport> mSocketListeners;
    private final HashMap<SubscribeListener, SubscribeListenerTransport> mSubscribeListeners;

    public static class EncryptCfbFailException extends Exception {
        EncryptCfbFailException(String msg) {
            super(msg);
        }
    }

    public interface NAdapterGetCallback {
        void onAdapterGet(NearbyAdapter nearbyAdapter);
    }

    private static class NearbyAuthServiceConnection implements ServiceConnection {
        private NAdapterGetCallback callback;
        private Context context;

        public NearbyAuthServiceConnection(Context context, NAdapterGetCallback callback) {
            this.context = context;
            this.callback = callback;
            HwLog.d(NearbyAdapter.TAG, "NearbyAuthServiceConnection construct");
        }

        /* JADX WARNING: Missing block: B:16:0x0070, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            synchronized (this) {
                String str = NearbyAdapter.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("client onAuthServiceConnected  || service ");
                stringBuilder.append(service);
                HwLog.d(str, stringBuilder.toString());
                NearbyAdapter.mAuthService = Stub.asInterface(service);
                if (NearbyAdapter.mNearbyAdapter == null) {
                    NearbyAdapter.mNearbyAdapter = new NearbyAdapter();
                }
                NearbyAdapter.mNearbyAdapter;
                NearbyAdapter.setAuthService(NearbyAdapter.mAuthService);
                NearbyAdapter.mNearbyAdapter;
                NearbyAdapter.mAuthConnection = this;
                NearbyAdapter.mAuthBound = Boolean.valueOf(true);
                if (NearbyAdapter.mNearbyBound.booleanValue() && this.callback != null) {
                    if (NearbyAdapter.mNearbyAdapter.hasNullService()) {
                        NearbyAdapter.callBackNull(this.context, this.callback);
                        return;
                    }
                    this.callback.onAdapterGet(NearbyAdapter.mNearbyAdapter);
                }
            }
        }

        public void onServiceDisconnected(ComponentName arg0) {
            synchronized (this) {
                HwLog.d(NearbyAdapter.TAG, "onAuthServiceDisconnected");
                NearbyAdapter.mAuthService = null;
                NearbyAdapter.mAuthBound = Boolean.valueOf(false);
            }
        }
    }

    private static class NearbyServiceConnection implements ServiceConnection {
        private NAdapterGetCallback callback;
        private Context context;

        public NearbyServiceConnection(Context context, NAdapterGetCallback callback) {
            this.context = context.getApplicationContext();
            this.callback = callback;
            HwLog.d(NearbyAdapter.TAG, "NearbyServiceConnection construct");
        }

        /* JADX WARNING: Missing block: B:27:0x00a2, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            synchronized (this) {
                String str = NearbyAdapter.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("client onServiceConnected  || service ");
                stringBuilder.append(service);
                HwLog.d(str, stringBuilder.toString());
                NearbyAdapter.mNearbyService = INearbyAdapter.Stub.asInterface(service);
                boolean hasInit = false;
                try {
                    hasInit = NearbyAdapter.mNearbyService.hasInit();
                } catch (RemoteException e) {
                    String str2 = NearbyAdapter.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("error in onServiceConnected");
                    stringBuilder2.append(e.getLocalizedMessage());
                    HwLog.e(str2, stringBuilder2.toString());
                }
                if (!hasInit) {
                    HwLog.d(NearbyAdapter.TAG, "mNearbyService has not init. set mNearbyService = null");
                    NearbyAdapter.mNearbyService = null;
                }
                if (NearbyAdapter.mNearbyAdapter == null) {
                    NearbyAdapter.mNearbyAdapter = new NearbyAdapter();
                }
                NearbyAdapter.mNearbyAdapter;
                NearbyAdapter.setNearbySevice(NearbyAdapter.mNearbyService);
                NearbyAdapter.mNearbyAdapter;
                NearbyAdapter.mConnection = this;
                NearbyAdapter.mNearbyBound = Boolean.valueOf(true);
                if (NearbyAdapter.mAuthBound.booleanValue() && this.callback != null) {
                    if (NearbyAdapter.mNearbyAdapter.hasNullService()) {
                        NearbyAdapter.callBackNull(this.context, this.callback);
                        return;
                    }
                    this.callback.onAdapterGet(NearbyAdapter.mNearbyAdapter);
                }
            }
        }

        public void onServiceDisconnected(ComponentName arg0) {
            synchronized (this) {
                HwLog.d(NearbyAdapter.TAG, "onServiceDisconnected");
                NearbyAdapter.mNearbyService = null;
                NearbyAdapter.mNearbyBound = Boolean.valueOf(false);
            }
        }
    }

    public boolean subscribeEvent(CloseRangeEventFilter eventFilter, CloseRangeEventListener eventListener) {
        synchronized (closeRangeLock) {
            if (closeRangeAdapter == null) {
                HwLog.e(TAG, "empty adapter");
                return false;
            }
            boolean subscribeEvent = closeRangeAdapter.subscribeEvent(eventFilter, eventListener);
            return subscribeEvent;
        }
    }

    public boolean unSubscribeEvent(CloseRangeEventFilter eventFilter) {
        synchronized (closeRangeLock) {
            if (closeRangeAdapter == null) {
                HwLog.e(TAG, "empty adapter");
                return false;
            }
            boolean unSubscribeEvent = closeRangeAdapter.unSubscribeEvent(eventFilter);
            return unSubscribeEvent;
        }
    }

    public boolean subscribeDevice(CloseRangeDeviceFilter deviceFilter, CloseRangeDeviceListener deviceListener) {
        synchronized (closeRangeLock) {
            if (closeRangeAdapter == null) {
                HwLog.e(TAG, "empty adapter");
                return false;
            }
            boolean subscribeDevice = closeRangeAdapter.subscribeDevice(deviceFilter, deviceListener);
            return subscribeDevice;
        }
    }

    public boolean unSubscribeDevice(CloseRangeDeviceFilter deviceFilter) {
        synchronized (closeRangeLock) {
            if (closeRangeAdapter == null) {
                HwLog.e(TAG, "empty adapter");
                return false;
            }
            boolean unSubscribeDevice = closeRangeAdapter.unSubscribeDevice(deviceFilter);
            return unSubscribeDevice;
        }
    }

    public boolean setFrequency(CloseRangeBusinessType type, BleScanLevel frequency) {
        synchronized (closeRangeLock) {
            if (closeRangeAdapter == null) {
                HwLog.e(TAG, "empty adapter");
                return false;
            }
            boolean frequency2 = closeRangeAdapter.setFrequency(type, frequency);
            return frequency2;
        }
    }

    private NearbyAdapter() {
        this.mPublishListeners = new HashMap();
        this.mSubscribeListeners = new HashMap();
        this.mSocketListeners = new HashMap();
        this.mConnectionListeners = new HashMap();
        this.mCreateSocketListeners = new HashMap();
        this.mAuthListeners = new HashMap();
        this.mMapBussinessIdListener = new HashMap();
        this.mDevice_RSAKeys = new HashMap();
        this.mAuthId_Device = new HashMap();
        if (mThread == null) {
            mThread = new HandlerThread("NearbyAdapter Looper");
            mThread.start();
        }
        synchronized (closeRangeLock) {
            closeRangeAdapter = new CloseRangeAdapter(mThread);
        }
        HwLog.d(TAG, "NearbyAdapter init");
    }

    private static void setNearbySevice(INearbyAdapter remoteNearbySevice) {
        mNearbyService = remoteNearbySevice;
        synchronized (closeRangeLock) {
            closeRangeAdapter.setNearbyService(mNearbyService);
        }
    }

    private static void setAuthService(IAuthAdapter remoteAuthSevice) {
        mAuthService = remoteAuthSevice;
    }

    private boolean hasNullService() {
        return mNearbyService == null || mAuthService == null;
    }

    /* JADX WARNING: Missing block: B:23:0x003e, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static synchronized void getNearbyAdapter(Context context, NAdapterGetCallback callback) {
        synchronized (NearbyAdapter.class) {
            HwLog.d(TAG, "getNearbyAdapter");
            if (context == null) {
                HwLog.e(TAG, "context is null && return.");
            } else if (callback == null) {
                HwLog.e(TAG, "callback is null && return.");
            } else if (mNearbyAdapter != null && mNearbyBound.booleanValue() && mAuthBound.booleanValue()) {
                callback.onAdapterGet(mNearbyAdapter);
            } else {
                bindAidlService(context, callback);
            }
        }
    }

    public static synchronized void createInstance(Context context, NearbyAdapterCallback callback) {
        synchronized (NearbyAdapter.class) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("createInstance start ");
            stringBuilder.append(mNearbyContext);
            HwLog.d(str, stringBuilder.toString());
            if (mNearbyContext != null) {
            } else if (context == null || callback == null) {
                HwLog.e(TAG, "createInstance context or callback null");
                throw new IllegalArgumentException("createInstance context or callback null");
            } else {
                mNearbyContext = context;
                getNearbyAdapter(context, callback);
            }
        }
    }

    public static synchronized void releaseInstance() {
        synchronized (NearbyAdapter.class) {
            if (mNearbyContext == null) {
                HwLog.e(TAG, "Instance of NearbyAdapter already released or have not got yet");
                return;
            }
            unbindAidlService(mNearbyContext);
            mNearbyContext = null;
        }
    }

    public Looper getLooper() {
        synchronized (NearbyAdapter.class) {
            if (mThread == null) {
                return null;
            }
            Looper looper = mThread.getLooper();
            return looper;
        }
    }

    public INearbyAdapter getNearbyService() {
        INearbyAdapter iNearbyAdapter;
        synchronized (this) {
            iNearbyAdapter = mNearbyService;
        }
        return iNearbyAdapter;
    }

    private static void callBackNull(Context context, NAdapterGetCallback callback) {
        callback.onAdapterGet(null);
        unbindAidlService(context);
    }

    public static void bindAidlService(Context context, NAdapterGetCallback callback) {
        String str;
        StringBuilder stringBuilder;
        String str2;
        StringBuilder stringBuilder2;
        String str3 = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("bindAidlService mNearbyBound = ");
        stringBuilder3.append(mNearbyBound);
        stringBuilder3.append(";mAuthBound : ");
        stringBuilder3.append(mAuthBound);
        HwLog.d(str3, stringBuilder3.toString());
        str3 = NearbyConfig.getCurPackageName(mNearbyContext);
        String str4 = TAG;
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("nearbyJar final runningActivity name: ");
        stringBuilder4.append(str3);
        HwLog.i(str4, stringBuilder4.toString());
        synchronized (mNearbyLock) {
            if (!mNearbyBound.booleanValue()) {
                Intent intent = new Intent();
                intent.setAction("com.huawei.nearby.NEARBY_SERVICE");
                intent.setPackage(str3);
                try {
                    if (NearbyConfig.isRunAsAar(mNearbyContext)) {
                        HwLog.e(TAG, "run as aar try bind service!");
                        context.bindService(intent, new NearbyServiceConnection(context, callback), 1);
                    } else {
                        try {
                            HwLog.e(TAG, "try bind service!");
                            context.bindService(intent, new NearbyServiceConnection(context, callback), 1);
                        } catch (Throwable e) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("bindServiceAsUser ERROR:");
                            stringBuilder.append(e.getLocalizedMessage());
                            HwLog.e(str, stringBuilder.toString());
                        }
                    }
                } catch (Exception e2) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("bindAidlService bindService NearbyServiceConnection ERROR:");
                    stringBuilder.append(e2.getLocalizedMessage());
                    HwLog.e(str, stringBuilder.toString());
                }
            }
        }
        synchronized (mAuthLock) {
            if (!mAuthBound.booleanValue()) {
                Intent intentAuth = new Intent();
                intentAuth.setAction("com.huawei.nearby.NEARBY_AUTH_SERVICE");
                intentAuth.setPackage(str3);
                try {
                    if (NearbyConfig.isRunAsAar(mNearbyContext)) {
                        context.bindService(intentAuth, new NearbyAuthServiceConnection(context, callback), 1);
                    } else {
                        try {
                            HwLog.e(TAG, "try bind Auth service!");
                            context.bindService(intentAuth, new NearbyAuthServiceConnection(context, callback), 1);
                        } catch (Throwable e3) {
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("bindAuthServiceAsUser ERROR:");
                            stringBuilder2.append(e3.getLocalizedMessage());
                            HwLog.e(str2, stringBuilder2.toString());
                        }
                    }
                } catch (Exception e4) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("bindAidlService bindService NearbyAuthServiceConnection ERROR:");
                    stringBuilder2.append(e4.getLocalizedMessage());
                    HwLog.e(str2, stringBuilder2.toString());
                }
            }
        }
    }

    public static void unbindAidlService(Context context) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unbindAidlService mNearbyBound = ");
        stringBuilder.append(mNearbyBound);
        stringBuilder.append(";mAuthBound : ");
        stringBuilder.append(mAuthBound);
        HwLog.d(str, stringBuilder.toString());
        try {
            synchronized (mNearbyLock) {
                if (mNearbyBound.booleanValue()) {
                    mNearbyService = null;
                    mNearbyAdapter = null;
                    context.unbindService(mConnection);
                    mNearbyBound = Boolean.valueOf(false);
                    if (mThread != null) {
                        if (VERSION.SDK_INT >= 18) {
                            mThread.quitSafely();
                        } else {
                            mThread.quit();
                        }
                        mThread = null;
                    }
                }
            }
        } catch (Exception e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("error in unbindAidlService mNearbyService");
            stringBuilder2.append(e.getLocalizedMessage());
            HwLog.e(str2, stringBuilder2.toString());
            mNearbyBound = Boolean.valueOf(false);
        }
        try {
            synchronized (mAuthLock) {
                if (mAuthBound.booleanValue()) {
                    mAuthService = null;
                    mNearbyAdapter = null;
                    context.unbindService(mAuthConnection);
                    mAuthBound = Boolean.valueOf(false);
                    if (mThread != null) {
                        if (VERSION.SDK_INT >= 18) {
                            mThread.quitSafely();
                        } else {
                            mThread.quit();
                        }
                        mThread = null;
                    }
                }
            }
        } catch (Exception e2) {
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("error in unbindAidlService mAuthService");
            stringBuilder3.append(e2.getLocalizedMessage());
            HwLog.e(str3, stringBuilder3.toString());
            mAuthBound = Boolean.valueOf(false);
        }
    }

    protected void finalize() throws Throwable {
        HwLog.d(TAG, "Adapter finalize");
        super.finalize();
    }

    public boolean publish(int businessId, PublishListener listener) {
        return publish(BusinessTypeEnum.AllType, businessId, listener);
    }

    public boolean publish(BusinessTypeEnum businessType, int businessId, PublishListener listener) {
        return publish(businessType, businessId, -1, listener);
    }

    public boolean publish(BusinessTypeEnum businessType, int businessId, int typeChannel, PublishListener listener) {
        HwLog.d(TAG, "publish");
        return publish(businessType, businessId, typeChannel, listener, Looper.myLooper());
    }

    public boolean publish(BusinessTypeEnum businessType, int businessId, int typeChannel, PublishListener listener, Looper looper) {
        HwLog.d(TAG, "publish with new looper");
        boolean result = false;
        if (businessType == null || listener == null) {
            HwLog.e(TAG, "publish get null param");
            return false;
        } else if (this.mPublishListeners.get(listener) != null) {
            HwLog.d(TAG, "PublishListener already registered && return");
            return false;
        } else if (mNearbyService == null) {
            HwLog.e(TAG, "mNearbyService is null. Publish return false");
            return false;
        } else {
            if (looper == null) {
                HwLog.e(TAG, "PublishListener looper can not be null");
                looper = mThread.getLooper();
            }
            try {
                PublishListenerTransport transport = new PublishListenerTransport(listener, looper);
                HwLog.d(TAG, "mNearbyService.publish start");
                result = mNearbyService.publish(businessType.toNumber(), businessId, typeChannel, transport);
                if (result) {
                    HwLog.d(TAG, "put PublishListener into map");
                    this.mPublishListeners.put(listener, transport);
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("error in publish");
                stringBuilder.append(e.getLocalizedMessage());
                HwLog.e(str, stringBuilder.toString());
            }
            return result;
        }
    }

    public boolean unPublish(PublishListener listener) {
        HwLog.d(TAG, "unPublish");
        boolean result = false;
        if (listener == null) {
            HwLog.e(TAG, "unPublish get null param");
            return false;
        } else if (mNearbyService == null) {
            HwLog.e(TAG, "mNearbyService is null. unPublish return false");
            return false;
        } else {
            try {
                PublishListenerTransport transport = (PublishListenerTransport) this.mPublishListeners.remove(listener);
                if (transport != null) {
                    HwLog.d(TAG, "mNearbyService.unPublish start");
                    result = mNearbyService.unPublish(transport);
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("error in unPublish");
                stringBuilder.append(e.getLocalizedMessage());
                HwLog.e(str, stringBuilder.toString());
            }
            return result;
        }
    }

    public boolean subscribe(boolean allowWakeupById, int businessId, SubscribeListener listener) {
        HwLog.d(TAG, "subscribe");
        return subscribe(allowWakeupById, businessId, listener, Looper.myLooper());
    }

    public boolean subscribe(boolean allowWakeupById, int businessId, SubscribeListener listener, Looper looper) {
        HwLog.d(TAG, "subscribe with new looper");
        boolean result = false;
        if (listener == null) {
            HwLog.e(TAG, "subscribe get null param");
            return false;
        } else if (this.mSubscribeListeners.get(listener) != null) {
            HwLog.d(TAG, "SubscribeListener already registered && return");
            return false;
        } else if (mNearbyService == null) {
            HwLog.e(TAG, "mNearbyService is null. subscribe return false");
            return false;
        } else {
            if (looper == null) {
                HwLog.e(TAG, "SubscribeListener looper can not be null");
                looper = mThread.getLooper();
            }
            try {
                SubscribeListenerTransport transport = new SubscribeListenerTransport(listener, looper);
                HwLog.d(TAG, "mNearbyService.subscribe start");
                result = mNearbyService.subscribe(allowWakeupById, businessId, transport);
                if (result) {
                    HwLog.d(TAG, "put SubscribeListener into map");
                    this.mSubscribeListeners.put(listener, transport);
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("error in subscribe");
                stringBuilder.append(e.getLocalizedMessage());
                HwLog.e(str, stringBuilder.toString());
            }
            return result;
        }
    }

    public boolean unSubscribe(SubscribeListener listener) {
        HwLog.d(TAG, "unSubscribe");
        boolean result = false;
        if (listener == null) {
            HwLog.e(TAG, "unSubscribe get null param");
            return false;
        } else if (mNearbyService == null) {
            HwLog.e(TAG, "mNearbyService is null. unSubscribe return false");
            return false;
        } else {
            try {
                SubscribeListenerTransport transport = (SubscribeListenerTransport) this.mSubscribeListeners.remove(listener);
                if (transport != null) {
                    HwLog.d(TAG, "mNearbyService.unSubscribe start");
                    result = mNearbyService.unSubscribe(transport);
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("error in unSubscribe");
                stringBuilder.append(e.getLocalizedMessage());
                HwLog.e(str, stringBuilder.toString());
            }
            return result;
        }
    }

    public boolean openNearbySocket(BusinessTypeEnum businessType, int channel, int businessId, String businessTag, NearbyDevice device, int timeout, CreateSocketListener listener) {
        HwLog.d(TAG, "openNearbySocket with channel");
        return openNearbySocket(businessType, channel, businessId, businessTag, device, timeout, listener, Looper.myLooper());
    }

    public boolean openNearbySocket(BusinessTypeEnum businessType, int businessId, String businessTag, NearbyDevice device, int timeout, CreateSocketListener listener) {
        HwLog.d(TAG, "openNearbySocket");
        return openNearbySocket(businessType, -1, businessId, businessTag, device, timeout, listener);
    }

    public boolean openNearbySocket(BusinessTypeEnum businessType, int channel, int businessId, String businessTag, NearbyDevice device, int timeout, CreateSocketListener listener, Looper looper) {
        CreateSocketListener createSocketListener = listener;
        HwLog.d(TAG, "openNearbySocket with new looper");
        boolean result = false;
        if (businessType == null || createSocketListener == null || device == null) {
            HwLog.e(TAG, "openNearbySocket get null param");
            return false;
        } else if (mNearbyService == null) {
            HwLog.e(TAG, "mNearbyService is null. openNearbySocket return false");
            return false;
        } else {
            Looper looper2;
            if (looper == null) {
                HwLog.e(TAG, "CreateSocketListener looper can not be null");
                looper2 = mThread.getLooper();
            } else {
                looper2 = looper;
            }
            if (this.mCreateSocketListeners.get(createSocketListener) != null) {
                HwLog.d(TAG, "CreateSocketListener already registered && return");
                return false;
            }
            CreateSocketListenerTransport createSocketListenerTransport = new CreateSocketListenerTransport(this, createSocketListener, looper2);
            int i = timeout;
            createSocketListenerTransport.setTimeOut(i);
            createSocketListenerTransport.setStartTime(System.currentTimeMillis());
            this.mCreateSocketListeners.put(createSocketListener, createSocketListenerTransport);
            try {
                HwLog.d(TAG, "mNearbyService.openNearbySocket start");
                result = mNearbyService.openNearbySocket(businessType.toNumber(), channel, businessId, businessTag, device, i, createSocketListenerTransport);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("error in openNearbySocket");
                stringBuilder.append(e.getLocalizedMessage());
                HwLog.e(str, stringBuilder.toString());
            }
            return result;
        }
    }

    public boolean stopOpen(CreateSocketListener listener) {
        if (this.mCreateSocketListeners.get(listener) == null) {
            HwLog.d(TAG, "CreateSocketListener have not in list. stopOpen do nothing. Return false.");
            return false;
        }
        ((CreateSocketListenerTransport) this.mCreateSocketListeners.get(listener)).cancel();
        return true;
    }

    public boolean registerSocketListener(BusinessTypeEnum businessType, int businessId, SocketListener listener) {
        HwLog.d(TAG, "registerSocketListener");
        return registerSocketListener(businessType, businessId, listener, Looper.myLooper());
    }

    public boolean registerSocketListener(BusinessTypeEnum businessType, int businessId, SocketListener listener, Looper looper) {
        HwLog.d(TAG, "registerSocketListener with new looper");
        boolean result = false;
        if (businessType == null || listener == null) {
            HwLog.e(TAG, "registerSocketListener get null param");
            return false;
        } else if (this.mSocketListeners.get(listener) != null) {
            HwLog.d(TAG, "SocketListener already registered && return");
            return true;
        } else if (mNearbyService == null) {
            HwLog.e(TAG, "mNearbyService is null. registerSocketListener return false");
            return false;
        } else {
            if (looper == null) {
                HwLog.e(TAG, "SocketListener looper can not be null");
                looper = mThread.getLooper();
            }
            try {
                SocketListenerTransport transport = new SocketListenerTransport(listener, looper);
                HwLog.d(TAG, "mNearbyService.registerSocketListener start");
                result = mNearbyService.registerInternalSocketListener(businessType.toNumber(), businessId, transport);
                if (result) {
                    this.mSocketListeners.put(listener, transport);
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("error in registerSocketListener");
                stringBuilder.append(e.getLocalizedMessage());
                HwLog.e(str, stringBuilder.toString());
            }
            return result;
        }
    }

    public boolean unRegisterSocketListener(SocketListener listener) {
        HwLog.d(TAG, "unRegisterSocketListener");
        boolean result = false;
        if (listener == null) {
            HwLog.e(TAG, "unRegisterSocketListener get null param");
            return false;
        } else if (mNearbyService == null) {
            HwLog.e(TAG, "mNearbyService is null. unRegisterSocketListener return false");
            return false;
        } else {
            try {
                SocketListenerTransport transport = (SocketListenerTransport) this.mSocketListeners.remove(listener);
                if (transport != null) {
                    HwLog.d(TAG, "mNearbyService.unRegisterSocketListener start");
                    result = mNearbyService.unRegisterInternalSocketListener(transport);
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("error in unRegisterSocketListener");
                stringBuilder.append(e.getLocalizedMessage());
                HwLog.e(str, stringBuilder.toString());
            }
            return result;
        }
    }

    public boolean registerConnectionListener(Context context, BusinessTypeEnum businessType, int businessId, ConnectionListener listener) {
        HwLog.d(TAG, "registerConnectionListener");
        return registerConnectionListener(context, businessType, businessId, null, listener, Looper.myLooper());
    }

    public boolean registerConnectionListener(Context context, BusinessTypeEnum businessType, int businessId, NearbyConfiguration configuration, ConnectionListener listener, Looper looper) {
        HwLog.d(TAG, "registerConnectionListener with new looper");
        boolean result = false;
        if (businessType == null || listener == null) {
            HwLog.e(TAG, "registerConnectionListener get null param");
            return false;
        } else if (this.mConnectionListeners.get(listener) != null) {
            HwLog.d(TAG, "ConnectionListener already registered && return");
            return true;
        } else if (mNearbyService == null) {
            HwLog.e(TAG, "mNearbyService is null. registerConnectionListener return false");
            return false;
        } else {
            if (looper == null) {
                HwLog.e(TAG, "ConnectionListener looper can not be null");
                looper = mThread.getLooper();
            }
            try {
                ConnectionListenerTransport transport = new ConnectionListenerTransport(context, businessId, listener, looper);
                HwLog.d(TAG, "mNearbyService.registerConnectionListener start ");
                result = mNearbyService.registerConnectionListener(businessType.toNumber(), businessId, configuration, transport);
                if (result) {
                    this.mConnectionListeners.put(listener, transport);
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("error in registerConnectionListener");
                stringBuilder.append(e.getLocalizedMessage());
                HwLog.e(str, stringBuilder.toString());
            }
            return result;
        }
    }

    public boolean unRegisterConnectionListener(ConnectionListener listener) {
        HwLog.d(TAG, "unRegisterConnectionListener");
        boolean result = false;
        if (listener == null) {
            HwLog.e(TAG, "unRegisterConnectionListener get null param");
            return false;
        } else if (mNearbyService == null) {
            HwLog.e(TAG, "mNearbyService is null. unRegisterConnectionListener return false");
            return false;
        } else {
            try {
                HwLog.d(TAG, "mNearbyService.unRegisterConnectionListener start ");
                if (this.mConnectionListeners.get(listener) == null) {
                    return false;
                }
                result = mNearbyService.unRegisterConnectionListener((IInternalConnectionListener) this.mConnectionListeners.get(listener));
                if (result) {
                    this.mConnectionListeners.remove(listener);
                }
                return result;
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("error in unRegisterConnectionListener");
                stringBuilder.append(e.getLocalizedMessage());
                HwLog.e(str, stringBuilder.toString());
            }
        }
    }

    public boolean open(BusinessTypeEnum businessType, int businessId, NearbyDevice device, int timeoutMs) {
        return open(businessType, 0, businessId, device, timeoutMs);
    }

    public boolean open(BusinessTypeEnum businessType, int channelId, int businessId, NearbyDevice device, int timeoutMs) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("open ");
        stringBuilder.append(timeoutMs);
        HwLog.d(str, stringBuilder.toString());
        boolean result = false;
        if (businessType == null || device == null) {
            HwLog.e(TAG, "open get null param");
            return false;
        } else if (mNearbyService == null) {
            HwLog.e(TAG, "mNearbyService is null. open return false");
            return false;
        } else {
            try {
                HwLog.d(TAG, "mNearbyService.open start");
                result = mNearbyService.open(businessType.toNumber(), channelId, businessId, device, timeoutMs);
            } catch (RemoteException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("error in open");
                stringBuilder2.append(e.getLocalizedMessage());
                HwLog.e(str2, stringBuilder2.toString());
            }
            return result;
        }
    }

    public int write(BusinessTypeEnum businessType, int businessId, NearbyDevice device, byte[] message) {
        HwLog.d(TAG, "write");
        int result = -1;
        if (businessType == null || device == null) {
            HwLog.e(TAG, "write get null param");
            return -1;
        } else if (mNearbyService == null) {
            HwLog.e(TAG, "mNearbyService is null. write return -1");
            return -1;
        } else {
            try {
                HwLog.d(TAG, "mNearbyService.write start");
                result = mNearbyService.write(businessType.toNumber(), businessId, device, message);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("error in write");
                stringBuilder.append(e.getLocalizedMessage());
                HwLog.e(str, stringBuilder.toString());
            }
            return result;
        }
    }

    public void close(BusinessTypeEnum businessType, int businessId, NearbyDevice device) {
        HwLog.d(TAG, "close");
        if (businessType == null || device == null) {
            HwLog.e(TAG, "close get null param");
        } else if (mNearbyService == null) {
            HwLog.e(TAG, "mNearbyService is null. close return");
        } else {
            try {
                HwLog.d(TAG, "mNearbyService.close start");
                mNearbyService.close(businessType.toNumber(), businessId, device);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("error in close");
                stringBuilder.append(e.getLocalizedMessage());
                HwLog.e(str, stringBuilder.toString());
            }
        }
    }

    public boolean findVendorDevice(int manu, int devType, DevFindListener listener, Looper looper) {
        HwLog.d(TAG, "findVendorDevice");
        boolean result = false;
        if (listener == null) {
            HwLog.e(TAG, "listen is null");
            return false;
        } else if (mNearbyService == null) {
            HwLog.e(TAG, "mNearbyService is null. close return");
            return false;
        } else {
            if (looper == null) {
                HwLog.e(TAG, "PublishListener looper can not be null");
                looper = mThread.getLooper();
            }
            DevFindListenerTransport tansport = new DevFindListenerTransport(listener, looper);
            try {
                HwLog.d(TAG, "mNearbyService.findVendorDevice start");
                result = mNearbyService.findVendorDevice(manu, devType, tansport);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("error in findVendorDevice");
                stringBuilder.append(e.getLocalizedMessage());
                HwLog.e(str, stringBuilder.toString());
            }
            return result;
        }
    }

    public boolean stopFindVendorDevice(int manu, int devType) {
        boolean result = false;
        if (mNearbyService == null) {
            HwLog.e(TAG, "mNearbyService is null. close return");
            return false;
        }
        try {
            HwLog.d(TAG, "mNearbyService.stopFindVendorDevice start");
            result = mNearbyService.stopFindVendorDevice(manu, devType);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error in findVendorDevice");
            stringBuilder.append(e.getLocalizedMessage());
            HwLog.e(str, stringBuilder.toString());
        }
        return result;
    }

    public boolean connectVendorDevice(NearbyDevice dev, int timeout, DevConnectListener listener, Looper looper) {
        HwLog.d(TAG, "connectVendorDevice");
        boolean result = false;
        if (listener == null) {
            HwLog.e(TAG, "listen is null");
            return false;
        } else if (mNearbyService == null) {
            HwLog.e(TAG, "mNearbyService is null. close return");
            return false;
        } else {
            if (looper == null) {
                HwLog.e(TAG, "DevConnectListener looper can not be null");
                looper = mThread.getLooper();
            }
            DevConnectListenTransport tansport = new DevConnectListenTransport(listener, looper);
            try {
                HwLog.d(TAG, "mNearbyService.connectVendorDevice start");
                result = mNearbyService.connectVendorDevice(dev, timeout, tansport);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("error in connectVendorDevice");
                stringBuilder.append(e.getLocalizedMessage());
                HwLog.e(str, stringBuilder.toString());
            }
            return result;
        }
    }

    public boolean disconnectVendorDevice(NearbyDevice dev) {
        HwLog.d(TAG, "disConnectVendorDevice");
        boolean result = false;
        if (mNearbyService == null) {
            HwLog.e(TAG, "mNearbyService is null. close return");
            return false;
        }
        try {
            HwLog.d(TAG, "mNearbyService.disConnectVendorDevice start");
            result = mNearbyService.disconnectVendorDevice(dev);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error in disConnectVendorDevice");
            stringBuilder.append(e.getLocalizedMessage());
            HwLog.e(str, stringBuilder.toString());
        }
        return result;
    }

    public void setNickname(String nickname) {
        HwLog.d(TAG, "setNickname");
        if (mAuthService == null) {
            HwLog.e(TAG, "mAuthService is null. unRegisterAuthentification return false");
            return;
        }
        try {
            mAuthService.setNickname(nickname);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error in setNickname");
            stringBuilder.append(e.getLocalizedMessage());
            HwLog.e(str, stringBuilder.toString());
        }
    }

    public boolean registerAuthentification(int businessId, AuthListener listener) {
        HwLog.d(TAG, "registerAuthentification");
        return registerAuthentification(businessId, listener, Looper.myLooper());
    }

    public boolean registerAuthentification(int businessId, AuthListener listener, Looper looper) {
        HwLog.d(TAG, "registerAuthentification with new looper");
        boolean result = false;
        if (listener == null) {
            HwLog.e(TAG, "registerAuthentification get null param");
            return false;
        } else if (mAuthService == null) {
            HwLog.e(TAG, "mAuthService is null. registerAuthentification return false");
            return false;
        } else {
            if (looper == null) {
                HwLog.e(TAG, "AuthListener looper can not be null");
                looper = mThread.getLooper();
            }
            try {
                AuthListenerTransport transport = new AuthListenerTransport(this, listener, looper);
                HwLog.d(TAG, "mAuthService.registerAuthentification start ");
                result = mAuthService.registerAuthentification(businessId, transport);
                if (result) {
                    HwLog.d(TAG, "put AuthListener into map");
                    this.mMapBussinessIdListener.put(listener, Integer.valueOf(businessId));
                    this.mAuthListeners.put(listener, transport);
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("error in registerAuthentification");
                stringBuilder.append(e.getLocalizedMessage());
                HwLog.e(str, stringBuilder.toString());
            }
            return result;
        }
    }

    public boolean unRegisterAuthentification(AuthListener listener) {
        HwLog.d(TAG, "unRegisterAuthentification");
        boolean result = false;
        if (listener == null) {
            HwLog.e(TAG, "unRegisterAuthentification get null param");
            return false;
        } else if (mAuthService == null) {
            HwLog.e(TAG, "mAuthService is null. unRegisterAuthentification return false");
            return false;
        } else {
            try {
                HwLog.d(TAG, "mAuthService.unRegisterAuthentification start ");
                result = mAuthService.unRegisterAuthentification((IAuthListener) this.mAuthListeners.get(listener));
                this.mMapBussinessIdListener.remove(listener);
                this.mAuthListeners.remove(listener);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("error in unRegisterAuthentification");
                stringBuilder.append(e.getLocalizedMessage());
                HwLog.e(str, stringBuilder.toString());
            }
            return result;
        }
    }

    public long startAuthentification(NearbyDevice device, int mode) {
        HwLog.d(TAG, "getAuthentification with new looper");
        long result = -1;
        if (mAuthService == null) {
            HwLog.e(TAG, "mAuthService is null. getAuthentification return false");
            return -1;
        } else if (device == null) {
            HwLog.e(TAG, "device is null .getAuthentification return false");
            return -1;
        } else {
            byte[] bytes = new byte[16];
            new SecureRandom().nextBytes(bytes);
            byte[] encryptedAesKey = RSAUtils.encryptUsingPubKey(bytes);
            HwLog.logByteArray(TAG, bytes);
            try {
                HwLog.d(TAG, "mAuthService.getAuthentification start");
                result = mAuthService.startAuthentification(device, mode, encryptedAesKey);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("error in startAuthentification");
                stringBuilder.append(e.getLocalizedMessage());
                HwLog.e(str, stringBuilder.toString());
            }
            if (result != -1) {
                if (device.getSummary() == null) {
                    HwLog.d(TAG, "authId != -1.but device Summary is null.");
                } else {
                    this.mDevice_RSAKeys.put(device.getSummary(), bytes);
                    this.mAuthId_Device.put(Long.valueOf(result), device);
                }
            }
            return result;
        }
    }

    public boolean hasLoginHwId() {
        HwLog.d(TAG, "hasLoginHwId");
        boolean result = false;
        if (mAuthService == null) {
            HwLog.e(TAG, "mAuthService is null. hasLoginHwId return false");
            return false;
        }
        try {
            result = mAuthService.hasLoginHwId();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error in hasLoginHwId");
            stringBuilder.append(e.getLocalizedMessage());
            HwLog.e(str, stringBuilder.toString());
        }
        return result;
    }

    public boolean setUserId(String userId) {
        HwLog.d(TAG, "setUserId");
        boolean result = false;
        if (mAuthService == null) {
            HwLog.e(TAG, "mAuthService is null. setUserId return false");
            return false;
        }
        try {
            result = mAuthService.setUserIdFromAdapter(userId);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error in setUserId");
            stringBuilder.append(e.getLocalizedMessage());
            HwLog.e(str, stringBuilder.toString());
        }
        return result;
    }

    public void setSessionKey(long authId, byte[] sessionKey, byte[] sessionIV, byte[] rsa_bytes, NearbyDevice device) {
        HwLog.d(TAG, "setSessionKey");
        if (device != null) {
            String summara = device.getSummary();
            byte[] bytes = rsa_bytes;
            if (bytes == null) {
                if (summara != null) {
                    bytes = (byte[]) this.mDevice_RSAKeys.get(summara);
                } else {
                    HwLog.d(TAG, "summara = null");
                    return;
                }
            }
            if (sessionKey == null) {
                HwLog.d(TAG, "SessionKey = null");
                return;
            } else if (sessionIV == null) {
                HwLog.d(TAG, "SessionIV = null");
                return;
            } else if (bytes == null) {
                HwLog.d(TAG, "bytes = null");
                return;
            } else {
                String str;
                StringBuilder stringBuilder;
                try {
                    byte[] trueKey = AESUtils.decrypt(sessionKey, bytes, sessionIV);
                    mDeviceSum_SessionKeys.put(summara, trueKey);
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("put trueKey.");
                    stringBuilder.append(trueKey.length);
                    HwLog.d(str, stringBuilder.toString());
                } catch (Exception e) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("error in setSessionKey");
                    stringBuilder.append(e.getLocalizedMessage());
                    HwLog.e(str, stringBuilder.toString());
                }
                return;
            }
        }
        HwLog.d(TAG, "device = null");
    }

    public NearbyDevice getDevice(long authId) {
        if (this.mAuthId_Device != null) {
            return (NearbyDevice) this.mAuthId_Device.get(Long.valueOf(authId));
        }
        return null;
    }

    void removeCreateSocketListener(CreateSocketListener listener) {
        if (this.mCreateSocketListeners != null) {
            this.mCreateSocketListeners.remove(listener);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("remove CreateSocketListener. CreateSocketListenerList left length = ");
            stringBuilder.append(this.mCreateSocketListeners.size());
            HwLog.d(str, stringBuilder.toString());
        }
    }

    public static byte[] encrypt(byte[] data, NearbyDevice device) throws EncryptCfbFailException {
        HwLog.d(TAG, "encrypt");
        byte[] sessionIV = new byte[16];
        new SecureRandom().nextBytes(sessionIV);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sessionIV.length>>>");
        stringBuilder.append(sessionIV.length);
        HwLog.d(str, stringBuilder.toString());
        HwLog.logByteArray(TAG, sessionIV);
        if (device != null) {
            byte[] sessionKey = (byte[]) mDeviceSum_SessionKeys.get(device.getSummary());
            if (sessionKey != null) {
                try {
                    HwLog.logByteArray(TAG, AESUtils.encrypt(data, sessionKey, sessionIV, true));
                    sessionKey = SDKDataHelper.packageTLV(57345, NearbySDKUtils.jointByteArrays(SDKDataHelper.packageTLV(57601, NearbySDKUtils.Int2Byte(0)), SDKDataHelper.packageTLV(57602, sessionIV), SDKDataHelper.packageTLV(57603, content)));
                    HwLog.logByteArray(TAG, sessionKey);
                    return sessionKey;
                } catch (Exception e) {
                    throw new EncryptCfbFailException("encrypt faild");
                }
            }
            throw new EncryptCfbFailException("encrypt faild sessionKey = null");
        }
        throw new EncryptCfbFailException("encrypt faild device = null");
    }

    public static byte[] decrypt(byte[] data, NearbyDevice device) throws EncryptCfbFailException {
        HwLog.d(TAG, "decrypt");
        ArrayList<SDKTlvData> params = new ArrayList();
        if (SDKDataHelper.parseDataToParam(data, params) == 57345) {
            int version = -1;
            byte[] sessionIV = null;
            byte[] content = null;
            int size = params.size();
            for (int i = 0; i < size; i++) {
                SDKTlvData param = (SDKTlvData) params.get(i);
                String str;
                StringBuilder stringBuilder;
                switch (param.getType()) {
                    case 57601:
                        version = NearbySDKUtils.Byte2Int(param.getData());
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("version>>>");
                        stringBuilder.append(version);
                        HwLog.d(str, stringBuilder.toString());
                        break;
                    case 57602:
                        sessionIV = param.getData();
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("sessionIV.length>>>");
                        stringBuilder.append(sessionIV.length);
                        HwLog.d(str, stringBuilder.toString());
                        HwLog.logByteArray(TAG, sessionIV);
                        break;
                    case 57603:
                        content = param.getData();
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("content.length>>>");
                        stringBuilder.append(content.length);
                        HwLog.d(str, stringBuilder.toString());
                        HwLog.logByteArray(TAG, content);
                        break;
                    default:
                        break;
                }
            }
            if (sessionIV == null || content == null) {
                throw new EncryptCfbFailException("parseDataToParam faild Iv is null or content is null");
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("version = ");
            stringBuilder2.append(version);
            HwLog.d(str2, stringBuilder2.toString());
            if (version != 0) {
                throw new EncryptCfbFailException("In this Hotspot_version data can not be decrypt");
            } else if (device != null) {
                byte[] sessionKey = (byte[]) mDeviceSum_SessionKeys.get(device.getSummary());
                if (sessionKey != null) {
                    try {
                        return AESUtils.decrypt(content, sessionKey, sessionIV, true);
                    } catch (Exception e) {
                        throw new EncryptCfbFailException("decrypt faild");
                    }
                }
                throw new EncryptCfbFailException("decrypt faild sessionKey = null");
            } else {
                throw new EncryptCfbFailException("decrypt faild device = null");
            }
        }
        throw new EncryptCfbFailException("parseDataToParam did not find hotspot");
    }

    public static void removeSessionkey(String summary) {
        if (summary != null) {
            mDeviceSum_SessionKeys.remove(summary);
        }
    }
}
