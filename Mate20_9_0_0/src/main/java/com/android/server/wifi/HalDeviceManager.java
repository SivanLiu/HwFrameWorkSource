package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifi;
import android.hardware.wifi.V1_0.IWifiApIface;
import android.hardware.wifi.V1_0.IWifiChip;
import android.hardware.wifi.V1_0.IWifiChip.ChipIfaceCombination;
import android.hardware.wifi.V1_0.IWifiChip.ChipIfaceCombinationLimit;
import android.hardware.wifi.V1_0.IWifiChip.ChipMode;
import android.hardware.wifi.V1_0.IWifiChipEventCallback.Stub;
import android.hardware.wifi.V1_0.IWifiEventCallback;
import android.hardware.wifi.V1_0.IWifiIface;
import android.hardware.wifi.V1_0.IWifiNanIface;
import android.hardware.wifi.V1_0.IWifiP2pIface;
import android.hardware.wifi.V1_0.IWifiRttController;
import android.hardware.wifi.V1_0.IWifiStaIface;
import android.hardware.wifi.V1_0.WifiStatus;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.os.Handler;
import android.os.HidlSupport.Mutable;
import android.os.IHwBinder.DeathRecipient;
import android.os.RemoteException;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.MutableBoolean;
import android.util.MutableInt;
import android.util.Pair;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class HalDeviceManager {
    @VisibleForTesting
    public static final String HAL_INSTANCE_NAME = "default";
    private static final int[] IFACE_TYPES_BY_PRIORITY = new int[]{1, 0, 2, 3};
    private static final int MAX_SLEEP_RETRY_TIMES = 40;
    private static final int SLEEP_TIME_RETRY = 50;
    private static final int START_HAL_RETRY_INTERVAL_MS = 20;
    @VisibleForTesting
    public static final int START_HAL_RETRY_TIMES = 3;
    private static final String TAG = "HalDevMgr";
    private static final boolean VDBG = false;
    private final Clock mClock;
    private boolean mDbg = false;
    private final SparseArray<Stub> mDebugCallbacks = new SparseArray();
    private final DeathRecipient mIWifiDeathRecipient = new -$$Lambda$HalDeviceManager$noScTs3Ynk8rNxP5lvUv8ww_gg4(this);
    private final SparseArray<Map<InterfaceAvailableForRequestListenerProxy, Boolean>> mInterfaceAvailableForRequestListeners = new SparseArray();
    private final Map<Pair<String, Integer>, InterfaceCacheEntry> mInterfaceInfoCache = new HashMap();
    private final Object mLock = new Object();
    private final Set<ManagerStatusListenerProxy> mManagerStatusListeners = new HashSet();
    private IServiceManager mServiceManager;
    private final DeathRecipient mServiceManagerDeathRecipient = new -$$Lambda$HalDeviceManager$jNAzj5YlVhwJm5NjZ6HiKskQStI(this);
    private final IServiceNotification mServiceNotificationCallback = new IServiceNotification.Stub() {
        public void onRegistration(String fqName, String name, boolean preexisting) {
            String str = HalDeviceManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("IWifi registration notification: fqName=");
            stringBuilder.append(fqName);
            stringBuilder.append(", name=");
            stringBuilder.append(name);
            stringBuilder.append(", preexisting=");
            stringBuilder.append(preexisting);
            Log.d(str, stringBuilder.toString());
            synchronized (HalDeviceManager.this.mLock) {
                HalDeviceManager.this.initIWifiIfNecessary();
            }
        }
    };
    private IWifi mWifi;
    private final WifiEventCallback mWifiEventCallback = new WifiEventCallback(this, null);

    private class IfaceCreationData {
        public WifiChipInfo chipInfo;
        public int chipModeId;
        public List<WifiIfaceInfo> interfacesToBeRemovedFirst;

        private IfaceCreationData() {
        }

        /* synthetic */ IfaceCreationData(HalDeviceManager x0, AnonymousClass1 x1) {
            this();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{chipInfo=");
            sb.append(this.chipInfo);
            sb.append(", chipModeId=");
            sb.append(this.chipModeId);
            sb.append(", interfacesToBeRemovedFirst=");
            sb.append(this.interfacesToBeRemovedFirst);
            sb.append(")");
            return sb.toString();
        }
    }

    public interface InterfaceAvailableForRequestListener {
        void onAvailabilityChanged(boolean z);
    }

    private class InterfaceCacheEntry {
        public IWifiChip chip;
        public int chipId;
        public long creationTime;
        public Set<InterfaceDestroyedListenerProxy> destroyedListeners;
        public boolean isLowPriority;
        public String name;
        public int type;

        private InterfaceCacheEntry() {
            this.destroyedListeners = new HashSet();
        }

        /* synthetic */ InterfaceCacheEntry(HalDeviceManager x0, AnonymousClass1 x1) {
            this();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{name=");
            sb.append(this.name);
            sb.append(", type=");
            sb.append(this.type);
            sb.append(", destroyedListeners.size()=");
            sb.append(this.destroyedListeners.size());
            sb.append(", creationTime=");
            sb.append(this.creationTime);
            sb.append(", isLowPriority=");
            sb.append(this.isLowPriority);
            sb.append("}");
            return sb.toString();
        }
    }

    public interface InterfaceDestroyedListener {
        void onDestroyed(String str);
    }

    private abstract class ListenerProxy<LISTENER> {
        private Handler mHandler;
        protected LISTENER mListener;

        public boolean equals(Object obj) {
            return this.mListener == ((ListenerProxy) obj).mListener;
        }

        public int hashCode() {
            return this.mListener.hashCode();
        }

        void trigger() {
            if (this.mHandler != null) {
                this.mHandler.post(new -$$Lambda$HalDeviceManager$ListenerProxy$EUZ7m5GXHY27oKauEW_8pihGjbw(this));
            } else {
                action();
            }
        }

        void triggerWithArg(boolean arg) {
            if (this.mHandler != null) {
                this.mHandler.post(new -$$Lambda$HalDeviceManager$ListenerProxy$YGLSZf58sxTORRCaSB1wOY_oquo(this, arg));
            } else {
                actionWithArg(arg);
            }
        }

        protected void action() {
        }

        protected void actionWithArg(boolean arg) {
        }

        ListenerProxy(LISTENER listener, Handler handler, String tag) {
            this.mListener = listener;
            this.mHandler = handler;
        }
    }

    public interface ManagerStatusListener {
        void onStatusChanged();
    }

    private class WifiChipInfo {
        public ArrayList<ChipMode> availableModes;
        public IWifiChip chip;
        public int chipId;
        public int currentModeId;
        public boolean currentModeIdValid;
        public WifiIfaceInfo[][] ifaces;

        private WifiChipInfo() {
            this.ifaces = new WifiIfaceInfo[HalDeviceManager.IFACE_TYPES_BY_PRIORITY.length][];
        }

        /* synthetic */ WifiChipInfo(HalDeviceManager x0, AnonymousClass1 x1) {
            this();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{chipId=");
            sb.append(this.chipId);
            sb.append(", availableModes=");
            sb.append(this.availableModes);
            sb.append(", currentModeIdValid=");
            sb.append(this.currentModeIdValid);
            sb.append(", currentModeId=");
            sb.append(this.currentModeId);
            for (int type : HalDeviceManager.IFACE_TYPES_BY_PRIORITY) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(", ifaces[");
                stringBuilder.append(type);
                stringBuilder.append("].length=");
                sb.append(stringBuilder.toString());
                sb.append(this.ifaces[type].length);
            }
            sb.append(")");
            return sb.toString();
        }
    }

    private class WifiIfaceInfo {
        public IWifiIface iface;
        public String name;

        private WifiIfaceInfo() {
        }

        /* synthetic */ WifiIfaceInfo(HalDeviceManager x0, AnonymousClass1 x1) {
            this();
        }
    }

    private class InterfaceAvailableForRequestListenerProxy extends ListenerProxy<InterfaceAvailableForRequestListener> {
        InterfaceAvailableForRequestListenerProxy(InterfaceAvailableForRequestListener destroyedListener, Handler handler) {
            super(destroyedListener, handler, "InterfaceAvailableForRequestListenerProxy");
        }

        protected void actionWithArg(boolean isAvailable) {
            ((InterfaceAvailableForRequestListener) this.mListener).onAvailabilityChanged(isAvailable);
        }
    }

    private class InterfaceDestroyedListenerProxy extends ListenerProxy<InterfaceDestroyedListener> {
        private final String mIfaceName;

        InterfaceDestroyedListenerProxy(String ifaceName, InterfaceDestroyedListener destroyedListener, Handler handler) {
            super(destroyedListener, handler, "InterfaceDestroyedListenerProxy");
            this.mIfaceName = ifaceName;
        }

        protected void action() {
            ((InterfaceDestroyedListener) this.mListener).onDestroyed(this.mIfaceName);
        }
    }

    private class ManagerStatusListenerProxy extends ListenerProxy<ManagerStatusListener> {
        ManagerStatusListenerProxy(ManagerStatusListener statusListener, Handler handler) {
            super(statusListener, handler, "ManagerStatusListenerProxy");
        }

        protected void action() {
            ((ManagerStatusListener) this.mListener).onStatusChanged();
        }
    }

    private class WifiEventCallback extends IWifiEventCallback.Stub {
        private WifiEventCallback() {
        }

        /* synthetic */ WifiEventCallback(HalDeviceManager x0, AnonymousClass1 x1) {
            this();
        }

        public void onStart() throws RemoteException {
        }

        public void onStop() throws RemoteException {
        }

        public void onFailure(WifiStatus status) throws RemoteException {
            String str = HalDeviceManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("IWifiEventCallback.onFailure: ");
            stringBuilder.append(HalDeviceManager.statusString(status));
            Log.e(str, stringBuilder.toString());
            HalDeviceManager.this.teardownInternal();
        }
    }

    public HalDeviceManager(Clock clock) {
        this.mClock = clock;
        this.mInterfaceAvailableForRequestListeners.put(0, new HashMap());
        this.mInterfaceAvailableForRequestListeners.put(1, new HashMap());
        this.mInterfaceAvailableForRequestListeners.put(2, new HashMap());
        this.mInterfaceAvailableForRequestListeners.put(3, new HashMap());
    }

    void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            this.mDbg = true;
        } else {
            this.mDbg = false;
        }
    }

    public void initialize() {
        initializeInternal();
    }

    public void registerStatusListener(ManagerStatusListener listener, Handler handler) {
        synchronized (this.mLock) {
            if (!this.mManagerStatusListeners.add(new ManagerStatusListenerProxy(listener, handler))) {
                Log.w(TAG, "registerStatusListener: duplicate registration ignored");
            }
        }
    }

    public boolean isSupported() {
        return isSupportedInternal();
    }

    public boolean isReady() {
        return this.mWifi != null;
    }

    public boolean isStarted() {
        return isWifiStarted();
    }

    public boolean start() {
        return startWifi();
    }

    public void stop() {
        stopWifi();
    }

    public Set<Integer> getSupportedIfaceTypes() {
        return getSupportedIfaceTypesInternal(null);
    }

    public Set<Integer> getSupportedIfaceTypes(IWifiChip chip) {
        return getSupportedIfaceTypesInternal(chip);
    }

    public IWifiStaIface createStaIface(boolean lowPrioritySta, InterfaceDestroyedListener destroyedListener, Handler handler) {
        return (IWifiStaIface) createIface(0, lowPrioritySta, destroyedListener, handler);
    }

    public IWifiApIface createApIface(InterfaceDestroyedListener destroyedListener, Handler handler) {
        return (IWifiApIface) createIface(1, false, destroyedListener, handler);
    }

    public IWifiP2pIface createP2pIface(InterfaceDestroyedListener destroyedListener, Handler handler) {
        return (IWifiP2pIface) createIface(2, false, destroyedListener, handler);
    }

    public IWifiNanIface createNanIface(InterfaceDestroyedListener destroyedListener, Handler handler) {
        return (IWifiNanIface) createIface(3, false, destroyedListener, handler);
    }

    public boolean removeIface(IWifiIface iface) {
        boolean success = removeIfaceInternal(iface);
        dispatchAvailableForRequestListeners();
        return success;
    }

    public IWifiChip getChip(IWifiIface iface) {
        String name = getName(iface);
        int type = getType(iface);
        synchronized (this.mLock) {
            InterfaceCacheEntry cacheEntry = (InterfaceCacheEntry) this.mInterfaceInfoCache.get(Pair.create(name, Integer.valueOf(type)));
            if (cacheEntry == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getChip: no entry for iface(name)=");
                stringBuilder.append(name);
                Log.e(str, stringBuilder.toString());
                return null;
            }
            IWifiChip iWifiChip = cacheEntry.chip;
            return iWifiChip;
        }
    }

    public boolean registerDestroyedListener(IWifiIface iface, InterfaceDestroyedListener destroyedListener, Handler handler) {
        String name = getName(iface);
        int type = getType(iface);
        synchronized (this.mLock) {
            InterfaceCacheEntry cacheEntry = (InterfaceCacheEntry) this.mInterfaceInfoCache.get(Pair.create(name, Integer.valueOf(type)));
            if (cacheEntry == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("registerDestroyedListener: no entry for iface(name)=");
                stringBuilder.append(name);
                Log.e(str, stringBuilder.toString());
                return false;
            }
            boolean add = cacheEntry.destroyedListeners.add(new InterfaceDestroyedListenerProxy(name, destroyedListener, handler));
            return add;
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0025, code:
            r0 = getAllChipInfo();
     */
    /* JADX WARNING: Missing block: B:10:0x0029, code:
            if (r0 != null) goto L_0x0033;
     */
    /* JADX WARNING: Missing block: B:11:0x002b, code:
            android.util.Log.e(TAG, "registerInterfaceAvailableForRequestListener: no chip info found - but possibly registered pre-started - ignoring");
     */
    /* JADX WARNING: Missing block: B:12:0x0032, code:
            return;
     */
    /* JADX WARNING: Missing block: B:13:0x0033, code:
            dispatchAvailableForRequestListenersForType(r5, r0);
     */
    /* JADX WARNING: Missing block: B:14:0x0036, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void registerInterfaceAvailableForRequestListener(int ifaceType, InterfaceAvailableForRequestListener listener, Handler handler) {
        synchronized (this.mLock) {
            InterfaceAvailableForRequestListenerProxy proxy = new InterfaceAvailableForRequestListenerProxy(listener, handler);
            if (((Map) this.mInterfaceAvailableForRequestListeners.get(ifaceType)).containsKey(proxy)) {
                return;
            }
            ((Map) this.mInterfaceAvailableForRequestListeners.get(ifaceType)).put(proxy, null);
        }
    }

    public void unregisterInterfaceAvailableForRequestListener(int ifaceType, InterfaceAvailableForRequestListener listener) {
        synchronized (this.mLock) {
            ((Map) this.mInterfaceAvailableForRequestListeners.get(ifaceType)).remove(new InterfaceAvailableForRequestListenerProxy(listener, null));
        }
    }

    public static String getName(IWifiIface iface) {
        if (iface == null) {
            return "<null>";
        }
        Mutable<String> nameResp = new Mutable();
        try {
            iface.getName(new -$$Lambda$HalDeviceManager$bTmsDoAj9faJCBOTeT1Q3Ww5yNM(nameResp));
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception on getName: ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        }
        return (String) nameResp.value;
    }

    static /* synthetic */ void lambda$getName$0(Mutable nameResp, WifiStatus status, String name) {
        if (status.code == 0) {
            nameResp.value = name;
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Error on getName: ");
        stringBuilder.append(statusString(status));
        Log.e(str, stringBuilder.toString());
    }

    public IWifiRttController createRttController() {
        synchronized (this.mLock) {
            if (this.mWifi == null) {
                Log.e(TAG, "createRttController: null IWifi");
                return null;
            }
            WifiChipInfo[] chipInfos = getAllChipInfo();
            if (chipInfos == null) {
                Log.e(TAG, "createRttController: no chip info found");
                stopWifi();
                return null;
            }
            for (WifiChipInfo chipInfo : chipInfos) {
                Mutable<IWifiRttController> rttResp = new Mutable();
                try {
                    chipInfo.chip.createRttController(null, new -$$Lambda$HalDeviceManager$joTzPjiPCypwHxT_jbl9OKHFMJo(rttResp));
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("IWifiChip.createRttController exception: ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                }
                if (rttResp.value != null) {
                    return (IWifiRttController) rttResp.value;
                }
            }
            Log.e(TAG, "createRttController: not available from any of the chips");
            return null;
        }
    }

    static /* synthetic */ void lambda$createRttController$1(Mutable rttResp, WifiStatus status, IWifiRttController rtt) {
        if (status.code == 0) {
            rttResp.value = rtt;
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("IWifiChip.createRttController failed: ");
        stringBuilder.append(statusString(status));
        Log.e(str, stringBuilder.toString());
    }

    protected IWifi getWifiServiceMockable() {
        try {
            return IWifi.getService();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception getting IWifi service: ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            return null;
        }
    }

    protected IServiceManager getServiceManagerMockable() {
        try {
            return IServiceManager.getService();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception getting IServiceManager: ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            return null;
        }
    }

    private void initializeInternal() {
        initIServiceManagerIfNecessary();
        if (isSupportedInternal()) {
            initIWifiIfNecessary();
        }
    }

    private void teardownInternal() {
        managerStatusListenerDispatch();
        dispatchAllDestroyedListeners();
        ((Map) this.mInterfaceAvailableForRequestListeners.get(0)).clear();
        ((Map) this.mInterfaceAvailableForRequestListeners.get(1)).clear();
        ((Map) this.mInterfaceAvailableForRequestListeners.get(2)).clear();
        ((Map) this.mInterfaceAvailableForRequestListeners.get(3)).clear();
    }

    public static /* synthetic */ void lambda$new$2(HalDeviceManager halDeviceManager, long cookie) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("IServiceManager died: cookie=");
        stringBuilder.append(cookie);
        Log.wtf(str, stringBuilder.toString());
        synchronized (halDeviceManager.mLock) {
            halDeviceManager.mServiceManager = null;
        }
    }

    private void initIServiceManagerIfNecessary() {
        if (this.mDbg) {
            Log.d(TAG, "initIServiceManagerIfNecessary");
        }
        synchronized (this.mLock) {
            if (this.mServiceManager != null) {
                return;
            }
            this.mServiceManager = getServiceManagerMockable();
            if (this.mServiceManager == null) {
                Log.wtf(TAG, "Failed to get IServiceManager instance");
            } else {
                try {
                    if (!this.mServiceManager.linkToDeath(this.mServiceManagerDeathRecipient, 0)) {
                        Log.wtf(TAG, "Error on linkToDeath on IServiceManager");
                        this.mServiceManager = null;
                        return;
                    } else if (!this.mServiceManager.registerForNotifications(IWifi.kInterfaceName, "", this.mServiceNotificationCallback)) {
                        Log.wtf(TAG, "Failed to register a listener for IWifi service");
                        this.mServiceManager = null;
                    }
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception while operating on IServiceManager: ");
                    stringBuilder.append(e);
                    Log.wtf(str, stringBuilder.toString());
                    this.mServiceManager = null;
                }
            }
        }
    }

    private boolean isSupportedInternal() {
        synchronized (this.mLock) {
            boolean z = false;
            if (this.mServiceManager == null) {
                Log.e(TAG, "isSupported: called but mServiceManager is null!?");
                return false;
            }
            try {
                if (this.mServiceManager.getTransport(IWifi.kInterfaceName, HAL_INSTANCE_NAME) != (byte) 0) {
                    z = true;
                }
                return z;
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception while operating on IServiceManager: ");
                stringBuilder.append(e);
                Log.wtf(str, stringBuilder.toString());
                return false;
            }
        }
    }

    public static /* synthetic */ void lambda$new$3(HalDeviceManager halDeviceManager, long cookie) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("IWifi HAL service died! Have a listener for it ... cookie=");
        stringBuilder.append(cookie);
        Log.e(str, stringBuilder.toString());
        synchronized (halDeviceManager.mLock) {
            halDeviceManager.mWifi = null;
            halDeviceManager.teardownInternal();
        }
    }

    private void initIWifiIfNecessary() {
        if (this.mDbg) {
            Log.d(TAG, "initIWifiIfNecessary");
        }
        synchronized (this.mLock) {
            if (this.mWifi != null) {
                return;
            }
            String str;
            StringBuilder stringBuilder;
            try {
                this.mWifi = getWifiServiceMockable();
                if (this.mWifi == null) {
                    Log.e(TAG, "IWifi not (yet) available - but have a listener for it ...");
                    return;
                } else if (this.mWifi.linkToDeath(this.mIWifiDeathRecipient, 0)) {
                    WifiStatus status = this.mWifi.registerEventCallback(this.mWifiEventCallback);
                    if (status.code != 0) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("IWifi.registerEventCallback failed: ");
                        stringBuilder.append(statusString(status));
                        Log.e(str, stringBuilder.toString());
                        this.mWifi = null;
                        return;
                    }
                    stopWifi();
                } else {
                    Log.e(TAG, "Error on linkToDeath on IWifi - will retry later");
                    return;
                }
            } catch (RemoteException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception while operating on IWifi: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
            }
        }
    }

    private void initIWifiChipDebugListeners() {
    }

    private static /* synthetic */ void lambda$initIWifiChipDebugListeners$4(MutableBoolean statusOk, Mutable chipIdsResp, WifiStatus status, ArrayList chipIds) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            chipIdsResp.value = chipIds;
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getChipIds failed: ");
        stringBuilder.append(statusString(status));
        Log.e(str, stringBuilder.toString());
    }

    private static /* synthetic */ void lambda$initIWifiChipDebugListeners$5(MutableBoolean statusOk, Mutable chipResp, WifiStatus status, IWifiChip chip) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            chipResp.value = chip;
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getChip failed: ");
        stringBuilder.append(statusString(status));
        Log.e(str, stringBuilder.toString());
    }

    private WifiChipInfo[] getAllChipInfo() {
        synchronized (this.mLock) {
            WifiChipInfo[] wifiChipInfoArr = null;
            if (this.mWifi == null) {
                Log.e(TAG, "getAllChipInfo: called but mWifi is null!?");
                return null;
            }
            try {
                boolean z = false;
                MutableBoolean statusOk = new MutableBoolean(false);
                Mutable<ArrayList<Integer>> chipIdsResp = new Mutable();
                this.mWifi.getChipIds(new -$$Lambda$HalDeviceManager$oV0zj57wyQrMevn_BdPhBTwDZhY(statusOk, chipIdsResp));
                if (!statusOk.value) {
                    return null;
                } else if (((ArrayList) chipIdsResp.value).size() == 0) {
                    Log.e(TAG, "Should have at least 1 chip!");
                    return null;
                } else {
                    WifiChipInfo[] chipsInfo;
                    int chipInfoIndex;
                    WifiChipInfo[] chipsInfo2 = new WifiChipInfo[((ArrayList) chipIdsResp.value).size()];
                    Mutable<IWifiChip> chipResp = new Mutable();
                    Iterator ifaceNamesResp = ((ArrayList) chipIdsResp.value).iterator();
                    int chipInfoIndex2 = 0;
                    while (ifaceNamesResp.hasNext()) {
                        Integer chipId = (Integer) ifaceNamesResp.next();
                        this.mWifi.getChip(chipId.intValue(), new -$$Lambda$HalDeviceManager$ZUYyxSyT0hYOkWCRHSzePknlIo0(statusOk, chipResp));
                        if (statusOk.value) {
                            Mutable<ArrayList<ChipMode>> availableModesResp = new Mutable();
                            ((IWifiChip) chipResp.value).getAvailableModes(new -$$Lambda$HalDeviceManager$aTCTYHFoCRvUuzhQPn5Voq6cUFw(statusOk, availableModesResp));
                            if (statusOk.value) {
                                MutableBoolean currentModeValidResp = new MutableBoolean(z);
                                MutableInt currentModeResp = new MutableInt(z);
                                ((IWifiChip) chipResp.value).getMode(new -$$Lambda$HalDeviceManager$-QOM6V5ZTnXWwvLBR-5woE-K_9c(statusOk, currentModeValidResp, currentModeResp));
                                if (statusOk.value) {
                                    Mutable<ArrayList<String>> ifaceNamesResp2 = new Mutable();
                                    MutableInt ifaceIndex = new MutableInt(z);
                                    ((IWifiChip) chipResp.value).getStaIfaceNames(new -$$Lambda$HalDeviceManager$W3qf_0tQXw4SlDmLzDZsc-YHrJQ(statusOk, ifaceNamesResp2));
                                    if (statusOk.value) {
                                        Mutable<ArrayList<Integer>> chipIdsResp2;
                                        Iterator it;
                                        Mutable<ArrayList<String>> ifaceNamesResp3;
                                        MutableInt currentModeResp2;
                                        MutableBoolean currentModeValidResp2;
                                        Mutable<ArrayList<ChipMode>> availableModesResp2;
                                        WifiIfaceInfo[] wifiIfaceInfoArr;
                                        WifiIfaceInfo[] staIfaces;
                                        Integer chipId2;
                                        WifiIfaceInfo[] staIfaces2 = new WifiIfaceInfo[((ArrayList) ifaceNamesResp2.value).size()];
                                        Iterator it2 = ((ArrayList) ifaceNamesResp2.value).iterator();
                                        while (it2.hasNext()) {
                                            String ifaceName = (String) it2.next();
                                            Iterator it3 = it2;
                                            chipIdsResp2 = chipIdsResp;
                                            MutableInt ifaceIndex2 = ifaceIndex;
                                            it = ifaceNamesResp;
                                            ifaceNamesResp3 = ifaceNamesResp2;
                                            currentModeResp2 = currentModeResp;
                                            currentModeValidResp2 = currentModeValidResp;
                                            availableModesResp2 = availableModesResp;
                                            wifiIfaceInfoArr = staIfaces2;
                                            staIfaces = staIfaces2;
                                            chipId2 = chipId;
                                            ((IWifiChip) chipResp.value).getStaIface(ifaceName, new -$$Lambda$HalDeviceManager$HLPmFjXA6r19Ma_sML3KIFjYXI8(this, statusOk, ifaceName, wifiIfaceInfoArr, ifaceIndex2));
                                            if (statusOk.value) {
                                                chipId = chipId2;
                                                ifaceNamesResp2 = ifaceNamesResp3;
                                                it2 = it3;
                                                ifaceIndex = ifaceIndex2;
                                                chipIdsResp = chipIdsResp2;
                                                ifaceNamesResp = it;
                                                currentModeResp = currentModeResp2;
                                                currentModeValidResp = currentModeValidResp2;
                                                availableModesResp = availableModesResp2;
                                                staIfaces2 = staIfaces;
                                            } else {
                                                return null;
                                            }
                                        }
                                        currentModeResp2 = currentModeResp;
                                        currentModeValidResp2 = currentModeValidResp;
                                        availableModesResp2 = availableModesResp;
                                        staIfaces = staIfaces2;
                                        chipIdsResp2 = chipIdsResp;
                                        it = ifaceNamesResp;
                                        ifaceNamesResp3 = ifaceNamesResp2;
                                        chipId2 = chipId;
                                        MutableInt ifaceIndex3 = ifaceIndex;
                                        ifaceIndex3.value = 0;
                                        ((IWifiChip) chipResp.value).getApIfaceNames(new -$$Lambda$HalDeviceManager$7IqRxcNtEnrXS9uVkc3w4xT9lgk(statusOk, ifaceNamesResp3));
                                        if (statusOk.value) {
                                            Integer chipId3;
                                            String ifaceName2;
                                            chipIdsResp = new WifiIfaceInfo[((ArrayList) ifaceNamesResp3.value).size()];
                                            Iterator it4 = ((ArrayList) ifaceNamesResp3.value).iterator();
                                            while (it4.hasNext()) {
                                                String ifaceName3 = (String) it4.next();
                                                chipId3 = chipId2;
                                                -$$Lambda$HalDeviceManager$LisNucJKN8TgUZ4F_hMe1s79mng -__lambda_haldevicemanager_lisnucjkn8tguz4f_hme1s79mng = r1;
                                                chipsInfo = chipsInfo2;
                                                IWifiChip iWifiChip = (IWifiChip) chipResp.value;
                                                chipInfoIndex = chipInfoIndex2;
                                                ifaceName2 = ifaceName3;
                                                Iterator it5 = it4;
                                                -$$Lambda$HalDeviceManager$LisNucJKN8TgUZ4F_hMe1s79mng -__lambda_haldevicemanager_lisnucjkn8tguz4f_hme1s79mng2 = new -$$Lambda$HalDeviceManager$LisNucJKN8TgUZ4F_hMe1s79mng(this, statusOk, ifaceName3, chipIdsResp, ifaceIndex3);
                                                iWifiChip.getApIface(ifaceName2, -__lambda_haldevicemanager_lisnucjkn8tguz4f_hme1s79mng);
                                                if (statusOk.value) {
                                                    it4 = it5;
                                                    chipId2 = chipId3;
                                                    chipsInfo2 = chipsInfo;
                                                    chipInfoIndex2 = chipInfoIndex;
                                                } else {
                                                    return null;
                                                }
                                            }
                                            chipId3 = chipId2;
                                            chipsInfo = chipsInfo2;
                                            chipInfoIndex = chipInfoIndex2;
                                            ifaceIndex3.value = 0;
                                            ((IWifiChip) chipResp.value).getP2pIfaceNames(new -$$Lambda$HalDeviceManager$INj3cXuz7UCfJAOVdMEteizngtw(statusOk, ifaceNamesResp3));
                                            if (statusOk.value) {
                                                WifiIfaceInfo[] p2pIfaces;
                                                staIfaces2 = new WifiIfaceInfo[((ArrayList) ifaceNamesResp3.value).size()];
                                                Iterator it6 = ((ArrayList) ifaceNamesResp3.value).iterator();
                                                while (it6.hasNext()) {
                                                    ifaceName2 = (String) it6.next();
                                                    Iterator it7 = it6;
                                                    -$$Lambda$HalDeviceManager$ynHs4R12k_5_9Qxr5asWSHdsuE4 -__lambda_haldevicemanager_ynhs4r12k_5_9qxr5aswshdsue4 = r1;
                                                    wifiIfaceInfoArr = staIfaces2;
                                                    p2pIfaces = staIfaces2;
                                                    IWifiChip iWifiChip2 = (IWifiChip) chipResp.value;
                                                    -$$Lambda$HalDeviceManager$ynHs4R12k_5_9Qxr5asWSHdsuE4 -__lambda_haldevicemanager_ynhs4r12k_5_9qxr5aswshdsue42 = new -$$Lambda$HalDeviceManager$ynHs4R12k_5_9Qxr5asWSHdsuE4(this, statusOk, ifaceName2, wifiIfaceInfoArr, ifaceIndex3);
                                                    iWifiChip2.getP2pIface(ifaceName2, -__lambda_haldevicemanager_ynhs4r12k_5_9qxr5aswshdsue4);
                                                    if (statusOk.value) {
                                                        it6 = it7;
                                                        staIfaces2 = p2pIfaces;
                                                    } else {
                                                        return null;
                                                    }
                                                }
                                                p2pIfaces = staIfaces2;
                                                ifaceIndex3.value = 0;
                                                ((IWifiChip) chipResp.value).getNanIfaceNames(new -$$Lambda$HalDeviceManager$d3wDJSLIYr6Z1fiH2ZtAJWELMyY(statusOk, ifaceNamesResp3));
                                                if (statusOk.value) {
                                                    staIfaces2 = new WifiIfaceInfo[((ArrayList) ifaceNamesResp3.value).size()];
                                                    it6 = ((ArrayList) ifaceNamesResp3.value).iterator();
                                                    while (it6.hasNext()) {
                                                        ifaceName2 = (String) it6.next();
                                                        Iterator it8 = it6;
                                                        -$$Lambda$HalDeviceManager$OTxRCq8TAZZlX8UFhmqaHcpXJYQ -__lambda_haldevicemanager_otxrcq8tazzlx8ufhmqahcpxjyq = r1;
                                                        Mutable<ArrayList<String>> ifaceNamesResp4 = ifaceNamesResp3;
                                                        IWifiChip iWifiChip3 = (IWifiChip) chipResp.value;
                                                        -$$Lambda$HalDeviceManager$OTxRCq8TAZZlX8UFhmqaHcpXJYQ -__lambda_haldevicemanager_otxrcq8tazzlx8ufhmqahcpxjyq2 = new -$$Lambda$HalDeviceManager$OTxRCq8TAZZlX8UFhmqaHcpXJYQ(this, statusOk, ifaceName2, staIfaces2, ifaceIndex3);
                                                        iWifiChip3.getNanIface(ifaceName2, -__lambda_haldevicemanager_otxrcq8tazzlx8ufhmqahcpxjyq);
                                                        if (statusOk.value) {
                                                            it6 = it8;
                                                            ifaceNamesResp3 = ifaceNamesResp4;
                                                        } else {
                                                            return null;
                                                        }
                                                    }
                                                    WifiChipInfo chipInfo = new WifiChipInfo(this, null);
                                                    chipInfoIndex2 = chipInfoIndex + 1;
                                                    chipsInfo[chipInfoIndex] = chipInfo;
                                                    chipInfo.chip = (IWifiChip) chipResp.value;
                                                    chipInfo.chipId = chipId3.intValue();
                                                    chipInfo.availableModes = (ArrayList) availableModesResp2.value;
                                                    chipInfo.currentModeIdValid = currentModeValidResp2.value;
                                                    chipInfo.currentModeId = currentModeResp2.value;
                                                    chipInfo.ifaces[0] = staIfaces;
                                                    chipInfo.ifaces[1] = chipIdsResp;
                                                    chipInfo.ifaces[2] = p2pIfaces;
                                                    chipInfo.ifaces[3] = staIfaces2;
                                                    z = false;
                                                    chipIdsResp = chipIdsResp2;
                                                    ifaceNamesResp = it;
                                                    chipsInfo2 = chipsInfo;
                                                    wifiChipInfoArr = null;
                                                } else {
                                                    return null;
                                                }
                                            }
                                            return null;
                                        }
                                        return null;
                                    }
                                    return null;
                                }
                                return wifiChipInfoArr;
                            }
                            return wifiChipInfoArr;
                        }
                        return wifiChipInfoArr;
                    }
                    chipsInfo = chipsInfo2;
                    chipInfoIndex = chipInfoIndex2;
                    return chipsInfo;
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getAllChipInfoAndValidateCache exception: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                return null;
            }
        }
    }

    static /* synthetic */ void lambda$getAllChipInfo$6(MutableBoolean statusOk, Mutable chipIdsResp, WifiStatus status, ArrayList chipIds) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            chipIdsResp.value = chipIds;
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getChipIds failed: ");
        stringBuilder.append(statusString(status));
        Log.e(str, stringBuilder.toString());
    }

    static /* synthetic */ void lambda$getAllChipInfo$7(MutableBoolean statusOk, Mutable chipResp, WifiStatus status, IWifiChip chip) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            chipResp.value = chip;
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getChip failed: ");
        stringBuilder.append(statusString(status));
        Log.e(str, stringBuilder.toString());
    }

    static /* synthetic */ void lambda$getAllChipInfo$8(MutableBoolean statusOk, Mutable availableModesResp, WifiStatus status, ArrayList modes) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            availableModesResp.value = modes;
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getAvailableModes failed: ");
        stringBuilder.append(statusString(status));
        Log.e(str, stringBuilder.toString());
    }

    static /* synthetic */ void lambda$getAllChipInfo$9(MutableBoolean statusOk, MutableBoolean currentModeValidResp, MutableInt currentModeResp, WifiStatus status, int modeId) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            currentModeValidResp.value = true;
            currentModeResp.value = modeId;
        } else if (status.code == 5) {
            statusOk.value = true;
        } else {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getMode failed: ");
            stringBuilder.append(statusString(status));
            Log.e(str, stringBuilder.toString());
        }
    }

    static /* synthetic */ void lambda$getAllChipInfo$10(MutableBoolean statusOk, Mutable ifaceNamesResp, WifiStatus status, ArrayList ifnames) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            ifaceNamesResp.value = ifnames;
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getStaIfaceNames failed: ");
        stringBuilder.append(statusString(status));
        Log.e(str, stringBuilder.toString());
    }

    public static /* synthetic */ void lambda$getAllChipInfo$11(HalDeviceManager halDeviceManager, MutableBoolean statusOk, String ifaceName, WifiIfaceInfo[] staIfaces, MutableInt ifaceIndex, WifiStatus status, IWifiStaIface iface) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            WifiIfaceInfo ifaceInfo = new WifiIfaceInfo(halDeviceManager, null);
            ifaceInfo.name = ifaceName;
            ifaceInfo.iface = iface;
            int i = ifaceIndex.value;
            ifaceIndex.value = i + 1;
            staIfaces[i] = ifaceInfo;
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getStaIface failed: ");
        stringBuilder.append(statusString(status));
        Log.e(str, stringBuilder.toString());
    }

    static /* synthetic */ void lambda$getAllChipInfo$12(MutableBoolean statusOk, Mutable ifaceNamesResp, WifiStatus status, ArrayList ifnames) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            ifaceNamesResp.value = ifnames;
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getApIfaceNames failed: ");
        stringBuilder.append(statusString(status));
        Log.e(str, stringBuilder.toString());
    }

    public static /* synthetic */ void lambda$getAllChipInfo$13(HalDeviceManager halDeviceManager, MutableBoolean statusOk, String ifaceName, WifiIfaceInfo[] apIfaces, MutableInt ifaceIndex, WifiStatus status, IWifiApIface iface) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            WifiIfaceInfo ifaceInfo = new WifiIfaceInfo(halDeviceManager, null);
            ifaceInfo.name = ifaceName;
            ifaceInfo.iface = iface;
            int i = ifaceIndex.value;
            ifaceIndex.value = i + 1;
            apIfaces[i] = ifaceInfo;
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getApIface failed: ");
        stringBuilder.append(statusString(status));
        Log.e(str, stringBuilder.toString());
    }

    static /* synthetic */ void lambda$getAllChipInfo$14(MutableBoolean statusOk, Mutable ifaceNamesResp, WifiStatus status, ArrayList ifnames) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            ifaceNamesResp.value = ifnames;
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getP2pIfaceNames failed: ");
        stringBuilder.append(statusString(status));
        Log.e(str, stringBuilder.toString());
    }

    public static /* synthetic */ void lambda$getAllChipInfo$15(HalDeviceManager halDeviceManager, MutableBoolean statusOk, String ifaceName, WifiIfaceInfo[] p2pIfaces, MutableInt ifaceIndex, WifiStatus status, IWifiP2pIface iface) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            WifiIfaceInfo ifaceInfo = new WifiIfaceInfo(halDeviceManager, null);
            ifaceInfo.name = ifaceName;
            ifaceInfo.iface = iface;
            int i = ifaceIndex.value;
            ifaceIndex.value = i + 1;
            p2pIfaces[i] = ifaceInfo;
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getP2pIface failed: ");
        stringBuilder.append(statusString(status));
        Log.e(str, stringBuilder.toString());
    }

    static /* synthetic */ void lambda$getAllChipInfo$16(MutableBoolean statusOk, Mutable ifaceNamesResp, WifiStatus status, ArrayList ifnames) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            ifaceNamesResp.value = ifnames;
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getNanIfaceNames failed: ");
        stringBuilder.append(statusString(status));
        Log.e(str, stringBuilder.toString());
    }

    public static /* synthetic */ void lambda$getAllChipInfo$17(HalDeviceManager halDeviceManager, MutableBoolean statusOk, String ifaceName, WifiIfaceInfo[] nanIfaces, MutableInt ifaceIndex, WifiStatus status, IWifiNanIface iface) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            WifiIfaceInfo ifaceInfo = new WifiIfaceInfo(halDeviceManager, null);
            ifaceInfo.name = ifaceName;
            ifaceInfo.iface = iface;
            int i = ifaceIndex.value;
            ifaceIndex.value = i + 1;
            nanIfaces[i] = ifaceInfo;
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getNanIface failed: ");
        stringBuilder.append(statusString(status));
        Log.e(str, stringBuilder.toString());
    }

    private boolean validateInterfaceCache(WifiChipInfo[] chipInfos) {
        synchronized (this.mLock) {
            for (InterfaceCacheEntry entry : this.mInterfaceInfoCache.values()) {
                WifiChipInfo matchingChipInfo = null;
                for (WifiChipInfo ci : chipInfos) {
                    if (ci.chipId == entry.chipId) {
                        matchingChipInfo = ci;
                        break;
                    }
                }
                String str;
                if (matchingChipInfo == null) {
                    str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("validateInterfaceCache: no chip found for ");
                    stringBuilder.append(entry);
                    Log.e(str, stringBuilder.toString());
                    return false;
                }
                WifiIfaceInfo[] ifaceInfoList = matchingChipInfo.ifaces[entry.type];
                if (ifaceInfoList == null) {
                    str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("validateInterfaceCache: invalid type on entry ");
                    stringBuilder2.append(entry);
                    Log.e(str, stringBuilder2.toString());
                    return false;
                }
                boolean matchFound = false;
                for (WifiIfaceInfo ifaceInfo : ifaceInfoList) {
                    if (ifaceInfo.name.equals(entry.name)) {
                        matchFound = true;
                        break;
                    }
                }
                if (!matchFound) {
                    str = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("validateInterfaceCache: no interface found for ");
                    stringBuilder3.append(entry);
                    Log.e(str, stringBuilder3.toString());
                    return false;
                }
            }
            return true;
        }
    }

    private boolean isWifiStarted() {
        synchronized (this.mLock) {
            try {
                if (this.mWifi == null) {
                    Log.w(TAG, "isWifiStarted called but mWifi is null!?");
                    return false;
                }
                boolean isStarted = this.mWifi.isStarted();
                return isStarted;
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("isWifiStarted exception: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                return false;
            }
        }
    }

    private void ensureIWifiLocked() {
        int retries = 0;
        while (this.mWifi == null && retries < 40) {
            try {
                Log.e(TAG, "ensureIWifiLocked: sleep 50 ms");
                Thread.sleep(50);
                retries++;
            } catch (InterruptedException e) {
                Log.e(TAG, "ensureIWifiLocked: got an InterruptedException");
                return;
            }
        }
    }

    private boolean startWifi() {
        ensureIWifiLocked();
        synchronized (this.mLock) {
            String str;
            StringBuilder stringBuilder;
            try {
                if (this.mWifi == null) {
                    Log.w(TAG, "startWifi called but mWifi is null!?");
                    return false;
                }
                int triedCount = 0;
                while (triedCount <= 3) {
                    WifiStatus status = this.mWifi.start();
                    String str2;
                    StringBuilder stringBuilder2;
                    if (status.code == 0) {
                        initIWifiChipDebugListeners();
                        managerStatusListenerDispatch();
                        if (triedCount != 0) {
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("start IWifi succeeded after trying ");
                            stringBuilder2.append(triedCount);
                            stringBuilder2.append(" times");
                            Log.d(str2, stringBuilder2.toString());
                        }
                        return true;
                    } else if (status.code == 5) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Cannot start IWifi: ");
                        stringBuilder2.append(statusString(status));
                        stringBuilder2.append(", Retrying...");
                        Log.e(str2, stringBuilder2.toString());
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                        }
                        triedCount++;
                    } else {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Cannot start IWifi: ");
                        stringBuilder2.append(statusString(status));
                        Log.e(str2, stringBuilder2.toString());
                        return false;
                    }
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot start IWifi after trying ");
                stringBuilder.append(triedCount);
                stringBuilder.append(" times");
                Log.e(str, stringBuilder.toString());
                return false;
            } catch (RemoteException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("startWifi exception: ");
                stringBuilder.append(e2);
                Log.e(str, stringBuilder.toString());
                return false;
            }
        }
    }

    private void stopWifi() {
        synchronized (this.mLock) {
            String str;
            StringBuilder stringBuilder;
            try {
                if (this.mWifi == null) {
                    Log.w(TAG, "stopWifi called but mWifi is null!?");
                } else {
                    WifiStatus status = this.mWifi.stop();
                    if (status.code != 0) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Cannot stop IWifi: ");
                        stringBuilder.append(statusString(status));
                        Log.e(str, stringBuilder.toString());
                    }
                    teardownInternal();
                }
            } catch (RemoteException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("stopWifi exception: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
            }
        }
    }

    private void managerStatusListenerDispatch() {
        synchronized (this.mLock) {
            for (ManagerStatusListenerProxy cb : this.mManagerStatusListeners) {
                cb.trigger();
            }
        }
    }

    Set<Integer> getSupportedIfaceTypesInternal(IWifiChip chip) {
        IWifiChip iWifiChip = chip;
        HashSet results = new HashSet();
        WifiChipInfo[] chipInfos = getAllChipInfo();
        if (chipInfos == null) {
            Log.e(TAG, "getSupportedIfaceTypesInternal: no chip info found");
            return results;
        }
        int i = 0;
        MutableInt chipIdIfProvided = new MutableInt(0);
        if (iWifiChip != null) {
            MutableBoolean statusOk = new MutableBoolean(false);
            try {
                iWifiChip.getId(new -$$Lambda$HalDeviceManager$RvX7FGUhmxm-qNliFXxQKKDHrRc(chipIdIfProvided, statusOk));
                if (!statusOk.value) {
                    return results;
                }
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getSupportedIfaceTypesInternal IWifiChip.getId() exception: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                return results;
            }
        }
        int length = chipInfos.length;
        while (i < length) {
            WifiChipInfo wci = chipInfos[i];
            if (iWifiChip == null || wci.chipId == chipIdIfProvided.value) {
                Iterator it = wci.availableModes.iterator();
                while (it.hasNext()) {
                    Iterator it2 = ((ChipMode) it.next()).availableCombinations.iterator();
                    while (it2.hasNext()) {
                        Iterator it3 = ((ChipIfaceCombination) it2.next()).limits.iterator();
                        while (it3.hasNext()) {
                            Iterator it4 = ((ChipIfaceCombinationLimit) it3.next()).types.iterator();
                            while (it4.hasNext()) {
                                results.add(Integer.valueOf(((Integer) it4.next()).intValue()));
                            }
                        }
                    }
                }
            }
            i++;
        }
        return results;
    }

    static /* synthetic */ void lambda$getSupportedIfaceTypesInternal$18(MutableInt chipIdIfProvided, MutableBoolean statusOk, WifiStatus status, int id) {
        if (status.code == 0) {
            chipIdIfProvided.value = id;
            statusOk.value = true;
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getSupportedIfaceTypesInternal: IWifiChip.getId() error: ");
        stringBuilder.append(statusString(status));
        Log.e(str, stringBuilder.toString());
        statusOk.value = false;
    }

    /* JADX WARNING: Missing block: B:23:0x005f, code:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private IWifiIface createIface(int ifaceType, boolean lowPriority, InterfaceDestroyedListener destroyedListener, Handler handler) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("createIface: ifaceType=");
            stringBuilder.append(ifaceType);
            stringBuilder.append(", lowPriority=");
            stringBuilder.append(lowPriority);
            Log.d(str, stringBuilder.toString());
        }
        synchronized (this.mLock) {
            WifiChipInfo[] chipInfos = getAllChipInfo();
            if (chipInfos == null) {
                Log.e(TAG, "createIface: no chip info found");
                stopWifi();
                return null;
            } else if (validateInterfaceCache(chipInfos)) {
                IWifiIface iface = createIfaceIfPossible(chipInfos, ifaceType, lowPriority, destroyedListener, handler);
                if (iface == null || dispatchAvailableForRequestListeners()) {
                } else {
                    return null;
                }
            } else {
                Log.e(TAG, "createIface: local cache is invalid!");
                stopWifi();
                return null;
            }
        }
    }

    private IWifiIface createIfaceIfPossible(WifiChipInfo[] chipInfos, int ifaceType, boolean lowPriority, InterfaceDestroyedListener destroyedListener, Handler handler) {
        Throwable cacheEntry;
        Handler handler2;
        boolean z;
        int i = ifaceType;
        InterfaceDestroyedListener interfaceDestroyedListener = destroyedListener;
        synchronized (this.mLock) {
            try {
                IfaceCreationData bestIfaceCreationProposal = null;
                for (WifiChipInfo chipInfo : chipInfos) {
                    Iterator it = chipInfo.availableModes.iterator();
                    while (it.hasNext()) {
                        ChipMode chipMode = (ChipMode) it.next();
                        Iterator it2 = chipMode.availableCombinations.iterator();
                        while (it2.hasNext()) {
                            Iterator it3;
                            ChipMode chipMode2;
                            ChipIfaceCombination chipIfaceCombo = (ChipIfaceCombination) it2.next();
                            int[][] expandedIfaceCombos = expandIfaceCombos(chipIfaceCombo);
                            int length = expandedIfaceCombos.length;
                            IfaceCreationData bestIfaceCreationProposal2 = bestIfaceCreationProposal;
                            int bestIfaceCreationProposal3 = 0;
                            while (bestIfaceCreationProposal3 < length) {
                                int i2 = length;
                                int i3 = bestIfaceCreationProposal3;
                                int[][] expandedIfaceCombos2 = expandedIfaceCombos;
                                ChipIfaceCombination chipIfaceCombo2 = chipIfaceCombo;
                                it3 = it2;
                                chipMode2 = chipMode;
                                bestIfaceCreationProposal = canIfaceComboSupportRequest(chipInfo, chipMode, expandedIfaceCombos[bestIfaceCreationProposal3], i, lowPriority);
                                if (compareIfaceCreationData(bestIfaceCreationProposal, bestIfaceCreationProposal2)) {
                                    bestIfaceCreationProposal2 = bestIfaceCreationProposal;
                                }
                                bestIfaceCreationProposal3 = i3 + 1;
                                length = i2;
                                expandedIfaceCombos = expandedIfaceCombos2;
                                chipIfaceCombo = chipIfaceCombo2;
                                it2 = it3;
                                chipMode = chipMode2;
                            }
                            it3 = it2;
                            chipMode2 = chipMode;
                            bestIfaceCreationProposal = bestIfaceCreationProposal2;
                        }
                    }
                }
                if (bestIfaceCreationProposal != null) {
                    IWifiIface iface = executeChipReconfiguration(bestIfaceCreationProposal, i);
                    if (iface != null) {
                        InterfaceCacheEntry cacheEntry2 = new InterfaceCacheEntry(this, null);
                        cacheEntry2.chip = bestIfaceCreationProposal.chipInfo.chip;
                        cacheEntry2.chipId = bestIfaceCreationProposal.chipInfo.chipId;
                        cacheEntry2.name = getName(iface);
                        cacheEntry2.type = i;
                        if (interfaceDestroyedListener != null) {
                            try {
                                cacheEntry2.destroyedListeners.add(new InterfaceDestroyedListenerProxy(cacheEntry2.name, interfaceDestroyedListener, handler));
                            } catch (Throwable th) {
                                cacheEntry = th;
                                z = lowPriority;
                                throw cacheEntry;
                            }
                        }
                        handler2 = handler;
                        cacheEntry2.creationTime = this.mClock.getUptimeSinceBootMillis();
                        cacheEntry2.isLowPriority = lowPriority;
                        if (this.mDbg) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("createIfaceIfPossible: added cacheEntry=");
                            stringBuilder.append(cacheEntry2);
                            Log.d(str, stringBuilder.toString());
                        }
                        this.mInterfaceInfoCache.put(Pair.create(cacheEntry2.name, Integer.valueOf(cacheEntry2.type)), cacheEntry2);
                        return iface;
                    }
                }
                z = lowPriority;
                handler2 = handler;
                return null;
            } catch (Throwable th2) {
                cacheEntry = th2;
                throw cacheEntry;
            }
        }
    }

    private boolean isItPossibleToCreateIface(WifiChipInfo[] chipInfos, int ifaceType) {
        for (WifiChipInfo chipInfo : chipInfos) {
            Iterator it = chipInfo.availableModes.iterator();
            while (it.hasNext()) {
                ChipMode chipMode = (ChipMode) it.next();
                Iterator it2 = chipMode.availableCombinations.iterator();
                while (it2.hasNext()) {
                    int[][] expandedIfaceCombos = expandIfaceCombos((ChipIfaceCombination) it2.next());
                    int length = expandedIfaceCombos.length;
                    int i = 0;
                    while (i < length) {
                        int i2 = i;
                        int i3 = length;
                        int[][] expandedIfaceCombos2 = expandedIfaceCombos;
                        if (canIfaceComboSupportRequest(chipInfo, chipMode, expandedIfaceCombos[i], ifaceType, null) != null) {
                            return true;
                        }
                        i = i2 + 1;
                        length = i3;
                        expandedIfaceCombos = expandedIfaceCombos2;
                    }
                }
            }
        }
        return false;
    }

    private int[][] expandIfaceCombos(ChipIfaceCombination chipIfaceCombo) {
        int numOfCombos = 1;
        Iterator it = chipIfaceCombo.limits.iterator();
        while (true) {
            int i = 0;
            if (!it.hasNext()) {
                break;
            }
            ChipIfaceCombinationLimit limit = (ChipIfaceCombinationLimit) it.next();
            while (i < limit.maxIfaces) {
                numOfCombos *= limit.types.size();
                i++;
            }
        }
        int[][] expandedIfaceCombos = (int[][]) Array.newInstance(int.class, new int[]{numOfCombos, IFACE_TYPES_BY_PRIORITY.length});
        int span = numOfCombos;
        Iterator it2 = chipIfaceCombo.limits.iterator();
        while (it2.hasNext()) {
            ChipIfaceCombinationLimit limit2 = (ChipIfaceCombinationLimit) it2.next();
            int span2 = span;
            for (span = 0; span < limit2.maxIfaces; span++) {
                span2 /= limit2.types.size();
                for (int k = 0; k < numOfCombos; k++) {
                    int[] iArr = expandedIfaceCombos[k];
                    int intValue = ((Integer) limit2.types.get((k / span2) % limit2.types.size())).intValue();
                    iArr[intValue] = iArr[intValue] + 1;
                }
            }
            span = span2;
        }
        return expandedIfaceCombos;
    }

    private IfaceCreationData canIfaceComboSupportRequest(WifiChipInfo chipInfo, ChipMode chipMode, int[] chipIfaceCombo, int ifaceType, boolean lowPriority) {
        if (chipIfaceCombo[ifaceType] == 0) {
            return null;
        }
        int i = 0;
        boolean isChipModeChangeProposed = chipInfo.currentModeIdValid && chipInfo.currentModeId != chipMode.id;
        int type;
        IfaceCreationData ifaceCreationData;
        if (isChipModeChangeProposed) {
            int[] iArr = IFACE_TYPES_BY_PRIORITY;
            int length = iArr.length;
            while (i < length) {
                type = iArr[i];
                if (chipInfo.ifaces[type].length != 0 && (lowPriority || !allowedToDeleteIfaceTypeForRequestedType(type, ifaceType, chipInfo.ifaces, chipInfo.ifaces[type].length))) {
                    return null;
                }
                i++;
            }
            ifaceCreationData = new IfaceCreationData(this, null);
            ifaceCreationData.chipInfo = chipInfo;
            ifaceCreationData.chipModeId = chipMode.id;
            return ifaceCreationData;
        }
        List<WifiIfaceInfo> interfacesToBeRemovedFirst = new ArrayList();
        int[] iArr2 = IFACE_TYPES_BY_PRIORITY;
        type = iArr2.length;
        while (i < type) {
            int type2 = iArr2[i];
            int tooManyInterfaces = chipInfo.ifaces[type2].length - chipIfaceCombo[type2];
            if (type2 == ifaceType) {
                tooManyInterfaces++;
            }
            if (tooManyInterfaces > 0) {
                if (lowPriority || !allowedToDeleteIfaceTypeForRequestedType(type2, ifaceType, chipInfo.ifaces, tooManyInterfaces)) {
                    return null;
                }
                interfacesToBeRemovedFirst = selectInterfacesToDelete(tooManyInterfaces, chipInfo.ifaces[type2]);
            }
            i++;
        }
        ifaceCreationData = new IfaceCreationData(this, null);
        ifaceCreationData.chipInfo = chipInfo;
        ifaceCreationData.chipModeId = chipMode.id;
        ifaceCreationData.interfacesToBeRemovedFirst = interfacesToBeRemovedFirst;
        return ifaceCreationData;
    }

    private boolean compareIfaceCreationData(IfaceCreationData val1, IfaceCreationData val2) {
        if (val1 == null) {
            return false;
        }
        if (val2 == null) {
            return true;
        }
        for (int type : IFACE_TYPES_BY_PRIORITY) {
            int numIfacesToDelete1;
            int numIfacesToDelete2;
            if (!val1.chipInfo.currentModeIdValid || val1.chipInfo.currentModeId == val1.chipModeId) {
                numIfacesToDelete1 = val1.interfacesToBeRemovedFirst.size();
            } else {
                numIfacesToDelete1 = val1.chipInfo.ifaces[type].length;
            }
            if (!val2.chipInfo.currentModeIdValid || val2.chipInfo.currentModeId == val2.chipModeId) {
                numIfacesToDelete2 = val2.interfacesToBeRemovedFirst.size();
            } else {
                numIfacesToDelete2 = val2.chipInfo.ifaces[type].length;
            }
            if (numIfacesToDelete1 < numIfacesToDelete2) {
                return true;
            }
        }
        return false;
    }

    private boolean allowedToDeleteIfaceTypeForRequestedType(int existingIfaceType, int requestedIfaceType, WifiIfaceInfo[][] currentIfaces, int numNecessaryInterfaces) {
        int numAvailableLowPriorityInterfaces = 0;
        for (InterfaceCacheEntry entry : this.mInterfaceInfoCache.values()) {
            if (entry.type == existingIfaceType && entry.isLowPriority) {
                numAvailableLowPriorityInterfaces++;
            }
        }
        boolean z = true;
        if (numAvailableLowPriorityInterfaces >= numNecessaryInterfaces) {
            return true;
        }
        if (existingIfaceType == requestedIfaceType || currentIfaces[requestedIfaceType].length != 0) {
            return false;
        }
        if (currentIfaces[existingIfaceType].length > 1) {
            return true;
        }
        if (requestedIfaceType == 3) {
            return false;
        }
        if (requestedIfaceType != 2) {
            return true;
        }
        if (existingIfaceType != 3) {
            z = false;
        }
        return z;
    }

    private List<WifiIfaceInfo> selectInterfacesToDelete(int excessInterfaces, WifiIfaceInfo[] interfaces) {
        int size;
        boolean lookupError = false;
        LongSparseArray<WifiIfaceInfo> orderedListLowPriority = new LongSparseArray();
        LongSparseArray<WifiIfaceInfo> orderedList = new LongSparseArray();
        int i = 0;
        for (WifiIfaceInfo info : interfaces) {
            InterfaceCacheEntry cacheEntry = (InterfaceCacheEntry) this.mInterfaceInfoCache.get(Pair.create(info.name, Integer.valueOf(getType(info.iface))));
            if (cacheEntry == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("selectInterfacesToDelete: can't find cache entry with name=");
                stringBuilder.append(info.name);
                Log.e(str, stringBuilder.toString());
                lookupError = true;
                break;
            }
            if (cacheEntry.isLowPriority) {
                orderedListLowPriority.append(cacheEntry.creationTime, info);
            } else {
                orderedList.append(cacheEntry.creationTime, info);
            }
        }
        if (lookupError) {
            Log.e(TAG, "selectInterfacesToDelete: falling back to arbitrary selection");
            return Arrays.asList((WifiIfaceInfo[]) Arrays.copyOf(interfaces, excessInterfaces));
        }
        List<WifiIfaceInfo> result = new ArrayList(excessInterfaces);
        while (i < excessInterfaces) {
            size = (orderedListLowPriority.size() - i) - 1;
            if (size >= 0) {
                result.add((WifiIfaceInfo) orderedListLowPriority.valueAt(size));
            } else {
                result.add((WifiIfaceInfo) orderedList.valueAt(((orderedList.size() - i) + orderedListLowPriority.size()) - 1));
            }
            i++;
        }
        return result;
    }

    private IWifiIface executeChipReconfiguration(IfaceCreationData ifaceCreationData, int ifaceType) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("executeChipReconfiguration: ifaceCreationData=");
            stringBuilder.append(ifaceCreationData);
            stringBuilder.append(", ifaceType=");
            stringBuilder.append(ifaceType);
            Log.d(str, stringBuilder.toString());
        }
        synchronized (this.mLock) {
            try {
                String str2;
                StringBuilder stringBuilder2;
                boolean isModeConfigNeeded = (ifaceCreationData.chipInfo.currentModeIdValid && ifaceCreationData.chipInfo.currentModeId == ifaceCreationData.chipModeId) ? false : true;
                if (this.mDbg) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("isModeConfigNeeded=");
                    stringBuilder2.append(isModeConfigNeeded);
                    Log.d(str2, stringBuilder2.toString());
                }
                if (isModeConfigNeeded) {
                    for (WifiIfaceInfo[] ifaceInfos : ifaceCreationData.chipInfo.ifaces) {
                        for (WifiIfaceInfo ifaceInfo : r4[r6]) {
                            removeIfaceInternal(ifaceInfo.iface);
                        }
                    }
                    WifiStatus status = ifaceCreationData.chipInfo.chip.configureChip(ifaceCreationData.chipModeId);
                    if (status.code != 0) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("executeChipReconfiguration: configureChip error: ");
                        stringBuilder2.append(statusString(status));
                        Log.e(str2, stringBuilder2.toString());
                        return null;
                    }
                }
                for (WifiIfaceInfo ifaceInfo2 : ifaceCreationData.interfacesToBeRemovedFirst) {
                    removeIfaceInternal(ifaceInfo2.iface);
                }
                Mutable<WifiStatus> statusResp = new Mutable();
                Mutable<IWifiIface> ifaceResp = new Mutable();
                switch (ifaceType) {
                    case 0:
                        ifaceCreationData.chipInfo.chip.createStaIface(new -$$Lambda$HalDeviceManager$csull9RuGux3O9fMU2TmHd3K8YE(statusResp, ifaceResp));
                        break;
                    case 1:
                        ifaceCreationData.chipInfo.chip.createApIface(new -$$Lambda$HalDeviceManager$Sk1PB19thsUnVIURe7jAUQxhiGk(statusResp, ifaceResp));
                        break;
                    case 2:
                        ifaceCreationData.chipInfo.chip.createP2pIface(new -$$Lambda$HalDeviceManager$LydIQHqKB4e2ETtZbZ2Ps6wJmZg(statusResp, ifaceResp));
                        break;
                    case 3:
                        ifaceCreationData.chipInfo.chip.createNanIface(new -$$Lambda$HalDeviceManager$rMUl3IrUZdoNc-Vrb1rqn8XExY0(statusResp, ifaceResp));
                        break;
                }
                if (((WifiStatus) statusResp.value).code != 0) {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("executeChipReconfiguration: failed to create interface ifaceType=");
                    stringBuilder3.append(ifaceType);
                    stringBuilder3.append(": ");
                    stringBuilder3.append(statusString((WifiStatus) statusResp.value));
                    Log.e(str3, stringBuilder3.toString());
                    return null;
                }
                return (IWifiIface) ifaceResp.value;
            } catch (RemoteException e) {
                String str4 = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("executeChipReconfiguration exception: ");
                stringBuilder4.append(e);
                Log.e(str4, stringBuilder4.toString());
                return null;
            }
        }
    }

    static /* synthetic */ void lambda$executeChipReconfiguration$19(Mutable statusResp, Mutable ifaceResp, WifiStatus status, IWifiStaIface iface) {
        statusResp.value = status;
        ifaceResp.value = iface;
    }

    static /* synthetic */ void lambda$executeChipReconfiguration$20(Mutable statusResp, Mutable ifaceResp, WifiStatus status, IWifiApIface iface) {
        statusResp.value = status;
        ifaceResp.value = iface;
    }

    static /* synthetic */ void lambda$executeChipReconfiguration$21(Mutable statusResp, Mutable ifaceResp, WifiStatus status, IWifiP2pIface iface) {
        statusResp.value = status;
        ifaceResp.value = iface;
    }

    static /* synthetic */ void lambda$executeChipReconfiguration$22(Mutable statusResp, Mutable ifaceResp, WifiStatus status, IWifiNanIface iface) {
        statusResp.value = status;
        ifaceResp.value = iface;
    }

    private boolean removeIfaceInternal(IWifiIface iface) {
        String str;
        String name = getName(iface);
        int type = getType(iface);
        if (this.mDbg) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeIfaceInternal: iface(name)=");
            stringBuilder.append(name);
            stringBuilder.append(", type=");
            stringBuilder.append(type);
            Log.d(str, stringBuilder.toString());
        }
        if (type == -1) {
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("removeIfaceInternal: can't get type -- iface(name)=");
            stringBuilder2.append(name);
            Log.e(str, stringBuilder2.toString());
            return false;
        }
        synchronized (this.mLock) {
            if (this.mWifi == null) {
                String str2 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("removeIfaceInternal: null IWifi -- iface(name)=");
                stringBuilder3.append(name);
                Log.e(str2, stringBuilder3.toString());
                return false;
            }
            IWifiChip chip = getChip(iface);
            if (chip == null) {
                String str3 = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("removeIfaceInternal: null IWifiChip -- iface(name)=");
                stringBuilder4.append(name);
                Log.e(str3, stringBuilder4.toString());
                return false;
            } else if (name == null) {
                Log.e(TAG, "removeIfaceInternal: can't get name");
                return false;
            } else {
                String str4;
                StringBuilder stringBuilder5;
                WifiStatus status = null;
                switch (type) {
                    case 0:
                        status = chip.removeStaIface(name);
                        break;
                    case 1:
                        status = chip.removeApIface(name);
                        break;
                    case 2:
                        status = chip.removeP2pIface(name);
                        break;
                    case 3:
                        status = chip.removeNanIface(name);
                        break;
                    default:
                        try {
                            str4 = TAG;
                            stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("removeIfaceInternal: invalid type=");
                            stringBuilder5.append(type);
                            Log.wtf(str4, stringBuilder5.toString());
                            return false;
                        } catch (RemoteException e) {
                            String str5 = TAG;
                            StringBuilder stringBuilder6 = new StringBuilder();
                            stringBuilder6.append("IWifiChip.removeXxxIface exception: ");
                            stringBuilder6.append(e);
                            Log.e(str5, stringBuilder6.toString());
                            break;
                        }
                }
                dispatchDestroyedListeners(name, type);
                if (status == null || status.code != 0) {
                    str4 = TAG;
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("IWifiChip.removeXxxIface failed: ");
                    stringBuilder5.append(statusString(status));
                    Log.e(str4, stringBuilder5.toString());
                    return false;
                }
                return true;
            }
        }
    }

    private boolean dispatchAvailableForRequestListeners() {
        synchronized (this.mLock) {
            WifiChipInfo[] chipInfos = getAllChipInfo();
            int i = 0;
            if (chipInfos == null) {
                Log.e(TAG, "dispatchAvailableForRequestListeners: no chip info found");
                stopWifi();
                return false;
            }
            int[] iArr = IFACE_TYPES_BY_PRIORITY;
            int length = iArr.length;
            while (i < length) {
                dispatchAvailableForRequestListenersForType(iArr[i], chipInfos);
                i++;
            }
            return true;
        }
    }

    private void dispatchAvailableForRequestListenersForType(int ifaceType, WifiChipInfo[] chipInfos) {
        synchronized (this.mLock) {
            Map<InterfaceAvailableForRequestListenerProxy, Boolean> listeners = (Map) this.mInterfaceAvailableForRequestListeners.get(ifaceType);
            if (listeners.size() == 0) {
                return;
            }
            boolean isAvailable = isItPossibleToCreateIface(chipInfos, ifaceType);
            for (Entry<InterfaceAvailableForRequestListenerProxy, Boolean> listenerEntry : listeners.entrySet()) {
                if (listenerEntry.getValue() == null || ((Boolean) listenerEntry.getValue()).booleanValue() != isAvailable) {
                    ((InterfaceAvailableForRequestListenerProxy) listenerEntry.getKey()).triggerWithArg(isAvailable);
                }
                listenerEntry.setValue(Boolean.valueOf(isAvailable));
            }
        }
    }

    private void dispatchDestroyedListeners(String name, int type) {
        synchronized (this.mLock) {
            InterfaceCacheEntry entry = (InterfaceCacheEntry) this.mInterfaceInfoCache.get(Pair.create(name, Integer.valueOf(type)));
            if (entry == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("dispatchDestroyedListeners: no cache entry for iface(name)=");
                stringBuilder.append(name);
                Log.e(str, stringBuilder.toString());
                return;
            }
            for (InterfaceDestroyedListenerProxy listener : entry.destroyedListeners) {
                listener.trigger();
            }
            entry.destroyedListeners.clear();
            this.mInterfaceInfoCache.remove(Pair.create(name, Integer.valueOf(type)));
        }
    }

    private void dispatchAllDestroyedListeners() {
        synchronized (this.mLock) {
            Iterator<Entry<Pair<String, Integer>, InterfaceCacheEntry>> it = this.mInterfaceInfoCache.entrySet().iterator();
            while (it.hasNext()) {
                InterfaceCacheEntry entry = (InterfaceCacheEntry) ((Entry) it.next()).getValue();
                for (InterfaceDestroyedListenerProxy listener : entry.destroyedListeners) {
                    listener.trigger();
                }
                entry.destroyedListeners.clear();
                it.remove();
            }
        }
    }

    private static String statusString(WifiStatus status) {
        if (status == null) {
            return "status=null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(status.code);
        sb.append(" (");
        sb.append(status.description);
        sb.append(")");
        return sb.toString();
    }

    private static int getType(IWifiIface iface) {
        MutableInt typeResp = new MutableInt(-1);
        try {
            iface.getType(new -$$Lambda$HalDeviceManager$ErxCpEghr4yhQpGHX1NQPumvouc(typeResp));
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception on getType: ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        }
        return typeResp.value;
    }

    static /* synthetic */ void lambda$getType$23(MutableInt typeResp, WifiStatus status, int type) {
        if (status.code == 0) {
            typeResp.value = type;
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Error on getType: ");
        stringBuilder.append(statusString(status));
        Log.e(str, stringBuilder.toString());
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("HalDeviceManager:");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  mServiceManager: ");
        stringBuilder.append(this.mServiceManager);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mWifi: ");
        stringBuilder.append(this.mWifi);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mManagerStatusListeners: ");
        stringBuilder.append(this.mManagerStatusListeners);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mInterfaceAvailableForRequestListeners: ");
        stringBuilder.append(this.mInterfaceAvailableForRequestListeners);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mInterfaceInfoCache: ");
        stringBuilder.append(this.mInterfaceInfoCache);
        pw.println(stringBuilder.toString());
    }
}
