package com.android.internal.telephony.ims;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsMmTelFeature;
import android.telephony.ims.aidl.IImsRcsFeature;
import android.telephony.ims.aidl.IImsRegistration;
import android.telephony.ims.stub.ImsFeatureConfiguration;
import android.telephony.ims.stub.ImsFeatureConfiguration.FeatureSlotPair;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.ims.internal.IImsServiceFeatureCallback;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.SomeArgs;
import com.android.internal.telephony.ims.ImsServiceController.ImsServiceControllerCallbacks;
import com.android.internal.telephony.ims.ImsServiceFeatureQueryManager.Listener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImsResolver implements ImsServiceControllerCallbacks {
    private static final int DELAY_DYNAMIC_QUERY_MS = 5000;
    private static final int HANDLER_ADD_PACKAGE = 0;
    private static final int HANDLER_CONFIG_CHANGED = 2;
    private static final int HANDLER_DYNAMIC_FEATURE_CHANGE = 4;
    private static final int HANDLER_OVERRIDE_IMS_SERVICE_CONFIG = 5;
    private static final int HANDLER_REMOVE_PACKAGE = 1;
    private static final int HANDLER_START_DYNAMIC_FEATURE_QUERY = 3;
    public static final String METADATA_EMERGENCY_MMTEL_FEATURE = "android.telephony.ims.EMERGENCY_MMTEL_FEATURE";
    public static final String METADATA_MMTEL_FEATURE = "android.telephony.ims.MMTEL_FEATURE";
    private static final String METADATA_OVERRIDE_PERM_CHECK = "override_bind_check";
    public static final String METADATA_RCS_FEATURE = "android.telephony.ims.RCS_FEATURE";
    private static final String TAG = "ImsResolver";
    private static final boolean volte = SystemProperties.getBoolean("ro.config.hw_volte_on", false);
    private Map<ComponentName, ImsServiceController> mActiveControllers = new HashMap();
    private BroadcastReceiver mAppChangedReceiver = new BroadcastReceiver() {
        /* JADX WARNING: Removed duplicated region for block: B:22:0x0053 A:{RETURN} */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0062  */
        /* JADX WARNING: Removed duplicated region for block: B:23:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x0053 A:{RETURN} */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0062  */
        /* JADX WARNING: Removed duplicated region for block: B:23:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x0053 A:{RETURN} */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0062  */
        /* JADX WARNING: Removed duplicated region for block: B:23:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x0053 A:{RETURN} */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0062  */
        /* JADX WARNING: Removed duplicated region for block: B:23:0x0054  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String packageName = intent.getData().getSchemeSpecificPart();
            int hashCode = action.hashCode();
            if (hashCode != -810471698) {
                if (hashCode != 172491798) {
                    if (hashCode != 525384130) {
                        if (hashCode == 1544582882 && action.equals("android.intent.action.PACKAGE_ADDED")) {
                            hashCode = 0;
                            switch (hashCode) {
                                case 0:
                                case 1:
                                case 2:
                                    ImsResolver.this.mHandler.obtainMessage(0, packageName).sendToTarget();
                                    break;
                                case 3:
                                    ImsResolver.this.mHandler.obtainMessage(1, packageName).sendToTarget();
                                    break;
                                default:
                                    return;
                            }
                        }
                    } else if (action.equals("android.intent.action.PACKAGE_REMOVED")) {
                        hashCode = 3;
                        switch (hashCode) {
                            case 0:
                            case 1:
                            case 2:
                                break;
                            case 3:
                                break;
                            default:
                                break;
                        }
                    }
                } else if (action.equals("android.intent.action.PACKAGE_CHANGED")) {
                    hashCode = 2;
                    switch (hashCode) {
                        case 0:
                        case 1:
                        case 2:
                            break;
                        case 3:
                            break;
                        default:
                            break;
                    }
                }
            } else if (action.equals("android.intent.action.PACKAGE_REPLACED")) {
                hashCode = 1;
                switch (hashCode) {
                    case 0:
                    case 1:
                    case 2:
                        break;
                    case 3:
                        break;
                    default:
                        break;
                }
            }
            hashCode = -1;
            switch (hashCode) {
                case 0:
                case 1:
                case 2:
                    break;
                case 3:
                    break;
                default:
                    break;
            }
        }
    };
    private List<SparseArray<ImsServiceController>> mBoundImsServicesByFeature;
    private final Object mBoundServicesLock = new Object();
    private final CarrierConfigManager mCarrierConfigManager;
    private String[] mCarrierServices;
    private BroadcastReceiver mConfigChangedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            int slotId = intent.getIntExtra("android.telephony.extra.SLOT_INDEX", -1);
            if (slotId == -1) {
                Log.i(ImsResolver.TAG, "Received SIM change for invalid slot id.");
                return;
            }
            String str = ImsResolver.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Received Carrier Config Changed for SlotId: ");
            stringBuilder.append(slotId);
            Log.i(str, stringBuilder.toString());
            ImsResolver.this.mHandler.obtainMessage(2, Integer.valueOf(slotId)).sendToTarget();
        }
    };
    private final Context mContext;
    private String mDeviceService;
    private Listener mDynamicQueryListener = new Listener() {
        public void onComplete(ComponentName name, Set<FeatureSlotPair> features) {
            String str = ImsResolver.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onComplete called for name: ");
            stringBuilder.append(name);
            stringBuilder.append("features:");
            stringBuilder.append(ImsResolver.this.printFeatures(features));
            Log.d(str, stringBuilder.toString());
            ImsResolver.this.handleFeaturesChanged(name, features);
        }

        public void onError(ComponentName name) {
            String str = ImsResolver.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onError: ");
            stringBuilder.append(name);
            stringBuilder.append("returned with an error result");
            Log.w(str, stringBuilder.toString());
            ImsResolver.this.scheduleQueryForFeatures(name, 5000);
        }
    };
    private ImsDynamicQueryManagerFactory mDynamicQueryManagerFactory = -$$Lambda$WamP7BPq0j01TgYE3GvUqU3b-rs.INSTANCE;
    private ImsServiceFeatureQueryManager mFeatureQueryManager;
    private Handler mHandler = new Handler(Looper.getMainLooper(), new -$$Lambda$ImsResolver$pNx4XUM9FmR6cV_MCAGiEt8F4pg(this));
    private ImsServiceControllerFactory mImsServiceControllerFactory = new ImsServiceControllerFactory() {
        public String getServiceInterface() {
            return "android.telephony.ims.ImsService";
        }

        public ImsServiceController create(Context context, ComponentName componentName, ImsServiceControllerCallbacks callbacks) {
            return new ImsServiceController(context, componentName, callbacks);
        }
    };
    private ImsServiceControllerFactory mImsServiceControllerFactoryCompat = new ImsServiceControllerFactory() {
        public String getServiceInterface() {
            return "android.telephony.ims.compat.ImsService";
        }

        public ImsServiceController create(Context context, ComponentName componentName, ImsServiceControllerCallbacks callbacks) {
            return new ImsServiceControllerCompat(context, componentName, callbacks);
        }
    };
    private ImsServiceControllerFactory mImsServiceControllerFactoryStaticBindingCompat = new ImsServiceControllerFactory() {
        public String getServiceInterface() {
            return null;
        }

        public ImsServiceController create(Context context, ComponentName componentName, ImsServiceControllerCallbacks callbacks) {
            return new ImsServiceControllerStaticCompat(context, componentName, callbacks);
        }
    };
    private Map<ComponentName, ImsServiceInfo> mInstalledServicesCache = new HashMap();
    private final boolean mIsDynamicBinding;
    private final int mNumSlots;
    private final ComponentName mStaticComponent;
    private SubscriptionManagerProxy mSubscriptionManagerProxy = new SubscriptionManagerProxy() {
        public int getSubId(int slotId) {
            int[] subIds = SubscriptionManager.getSubId(slotId);
            if (subIds != null) {
                return subIds[0];
            }
            return -1;
        }

        public int getSlotIndex(int subId) {
            return SubscriptionManager.getSlotIndex(subId);
        }
    };

    @VisibleForTesting
    public interface ImsDynamicQueryManagerFactory {
        ImsServiceFeatureQueryManager create(Context context, Listener listener);
    }

    @VisibleForTesting
    public interface ImsServiceControllerFactory {
        ImsServiceController create(Context context, ComponentName componentName, ImsServiceControllerCallbacks imsServiceControllerCallbacks);

        String getServiceInterface();
    }

    @VisibleForTesting
    public static class ImsServiceInfo {
        public ImsServiceControllerFactory controllerFactory;
        public boolean featureFromMetadata = true;
        private final int mNumSlots;
        private final HashSet<FeatureSlotPair> mSupportedFeatures;
        public ComponentName name;

        public ImsServiceInfo(int numSlots) {
            this.mNumSlots = numSlots;
            this.mSupportedFeatures = new HashSet();
        }

        void addFeatureForAllSlots(int feature) {
            for (int i = 0; i < this.mNumSlots; i++) {
                this.mSupportedFeatures.add(new FeatureSlotPair(i, feature));
            }
        }

        void replaceFeatures(Set<FeatureSlotPair> newFeatures) {
            this.mSupportedFeatures.clear();
            this.mSupportedFeatures.addAll(newFeatures);
        }

        @VisibleForTesting
        public HashSet<FeatureSlotPair> getSupportedFeatures() {
            return this.mSupportedFeatures;
        }

        public boolean equals(Object o) {
            boolean z = true;
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ImsServiceInfo that = (ImsServiceInfo) o;
            if (!this.name == null ? this.name.equals(that.name) : that.name == null) {
                return false;
            }
            if (!this.mSupportedFeatures.equals(that.mSupportedFeatures)) {
                return false;
            }
            if (this.controllerFactory != null) {
                z = this.controllerFactory.equals(that.controllerFactory);
            } else if (that.controllerFactory != null) {
                z = false;
            }
            return z;
        }

        public int hashCode() {
            int i = 0;
            int result = 31 * (this.name != null ? this.name.hashCode() : 0);
            if (this.controllerFactory != null) {
                i = this.controllerFactory.hashCode();
            }
            return result + i;
        }

        public String toString() {
            StringBuilder res = new StringBuilder();
            res.append("[ImsServiceInfo] name=");
            res.append(this.name);
            res.append(", supportedFeatures=[ ");
            Iterator it = this.mSupportedFeatures.iterator();
            while (it.hasNext()) {
                FeatureSlotPair feature = (FeatureSlotPair) it.next();
                res.append("(");
                res.append(feature.slotId);
                res.append(",");
                res.append(feature.featureType);
                res.append(") ");
            }
            return res.toString();
        }
    }

    @VisibleForTesting
    public interface SubscriptionManagerProxy {
        int getSlotIndex(int i);

        int getSubId(int i);
    }

    public static /* synthetic */ boolean lambda$new$0(ImsResolver imsResolver, Message msg) {
        boolean isCarrierImsService = false;
        switch (msg.what) {
            case 0:
                imsResolver.maybeAddedImsService((String) msg.obj);
                break;
            case 1:
                imsResolver.maybeRemovedImsService(msg.obj);
                break;
            case 2:
                imsResolver.carrierConfigChanged(((Integer) msg.obj).intValue());
                break;
            case 3:
                imsResolver.startDynamicQuery(msg.obj);
                break;
            case 4:
                SomeArgs args = msg.obj;
                ComponentName name = args.arg1;
                Set<FeatureSlotPair> features = args.arg2;
                args.recycle();
                imsResolver.dynamicQueryComplete(name, features);
                break;
            case 5:
                int slotId = msg.arg1;
                if (msg.arg2 == 1) {
                    isCarrierImsService = true;
                }
                String packageName = msg.obj;
                String str;
                StringBuilder stringBuilder;
                if (!isCarrierImsService) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("overriding device ImsService -  packageName=");
                    stringBuilder.append(packageName);
                    Log.i(str, stringBuilder.toString());
                    if (packageName == null || packageName.isEmpty()) {
                        imsResolver.unbindImsService(imsResolver.getImsServiceInfoFromCache(imsResolver.mDeviceService));
                    }
                    imsResolver.mDeviceService = packageName;
                    ImsServiceInfo deviceInfo = imsResolver.getImsServiceInfoFromCache(imsResolver.mDeviceService);
                    if (deviceInfo != null) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("overriding device ImsService - deviceInfo.featureFromMetadata=");
                        stringBuilder2.append(deviceInfo.featureFromMetadata);
                        Log.i(str2, stringBuilder2.toString());
                        if (!deviceInfo.featureFromMetadata) {
                            imsResolver.scheduleQueryForFeatures(deviceInfo);
                            break;
                        }
                        imsResolver.bindImsService(deviceInfo);
                        break;
                    }
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("overriding carrier ImsService - slot=");
                stringBuilder.append(slotId);
                stringBuilder.append(" packageName=");
                stringBuilder.append(packageName);
                Log.i(str, stringBuilder.toString());
                imsResolver.maybeRebindService(slotId, packageName);
                break;
                break;
            default:
                return false;
        }
        return true;
    }

    public ImsResolver(Context context, String defaultImsPackageName, int numSlots, boolean isDynamicBinding) {
        this.mContext = context;
        this.mDeviceService = defaultImsPackageName;
        this.mNumSlots = numSlots;
        this.mIsDynamicBinding = isDynamicBinding;
        this.mStaticComponent = new ComponentName(this.mContext, ImsResolver.class);
        if (!this.mIsDynamicBinding) {
            Log.i(TAG, "ImsResolver initialized with static binding.");
            this.mDeviceService = this.mStaticComponent.getPackageName();
        }
        this.mCarrierConfigManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        this.mCarrierServices = new String[numSlots];
        this.mBoundImsServicesByFeature = (List) Stream.generate(-$$Lambda$ImsResolver$WVd6ghNMbVDukmkxia3ZwNeZzEY.INSTANCE).limit((long) this.mNumSlots).collect(Collectors.toList());
        if (this.mIsDynamicBinding) {
            IntentFilter appChangedFilter = new IntentFilter();
            appChangedFilter.addAction("android.intent.action.PACKAGE_CHANGED");
            appChangedFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            appChangedFilter.addAction("android.intent.action.PACKAGE_ADDED");
            appChangedFilter.addDataScheme("package");
            context.registerReceiverAsUser(this.mAppChangedReceiver, UserHandle.ALL, appChangedFilter, null, null);
            context.registerReceiver(this.mConfigChangedReceiver, new IntentFilter("android.telephony.action.CARRIER_CONFIG_CHANGED"));
        }
    }

    @VisibleForTesting
    public void setSubscriptionManagerProxy(SubscriptionManagerProxy proxy) {
        this.mSubscriptionManagerProxy = proxy;
    }

    @VisibleForTesting
    public void setImsServiceControllerFactory(ImsServiceControllerFactory factory) {
        this.mImsServiceControllerFactory = factory;
    }

    @VisibleForTesting
    public Handler getHandler() {
        return this.mHandler;
    }

    @VisibleForTesting
    public void setImsDynamicQueryManagerFactory(ImsDynamicQueryManagerFactory m) {
        this.mDynamicQueryManagerFactory = m;
    }

    public void initPopulateCacheAndStartBind() {
        Log.i(TAG, "Initializing cache and binding.");
        this.mFeatureQueryManager = this.mDynamicQueryManagerFactory.create(this.mContext, this.mDynamicQueryListener);
        this.mHandler.obtainMessage(2, Integer.valueOf(-1)).sendToTarget();
        this.mHandler.obtainMessage(0, null).sendToTarget();
    }

    public void enableIms(int slotId) {
        SparseArray<ImsServiceController> controllers = getImsServiceControllers(slotId);
        if (controllers != null) {
            for (int i = 0; i < controllers.size(); i++) {
                ((ImsServiceController) controllers.get(controllers.keyAt(i))).enableIms(slotId);
            }
        }
    }

    public void disableIms(int slotId) {
        SparseArray<ImsServiceController> controllers = getImsServiceControllers(slotId);
        if (controllers != null) {
            for (int i = 0; i < controllers.size(); i++) {
                ((ImsServiceController) controllers.get(controllers.keyAt(i))).disableIms(slotId);
            }
        }
    }

    public IImsMmTelFeature getMmTelFeatureAndListen(int slotId, IImsServiceFeatureCallback callback) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getMmTelFeatureAndListen - slot: ");
        stringBuilder.append(slotId);
        stringBuilder.append(" callback: ");
        stringBuilder.append(callback);
        Rlog.i(str, stringBuilder.toString());
        ImsServiceController controller = getImsServiceControllerAndListen(slotId, 1, callback);
        return controller != null ? controller.getMmTelFeature(slotId) : null;
    }

    public IImsRcsFeature getRcsFeatureAndListen(int slotId, IImsServiceFeatureCallback callback) {
        ImsServiceController controller = getImsServiceControllerAndListen(slotId, 2, callback);
        return controller != null ? controller.getRcsFeature(slotId) : null;
    }

    public IImsRegistration getImsRegistration(int slotId, int feature) throws RemoteException {
        ImsServiceController controller = getImsServiceController(slotId, feature);
        if (controller != null) {
            return controller.getRegistration(slotId);
        }
        return null;
    }

    public IImsConfig getImsConfig(int slotId, int feature) throws RemoteException {
        ImsServiceController controller = getImsServiceController(slotId, feature);
        if (controller != null) {
            return controller.getConfig(slotId);
        }
        return null;
    }

    @VisibleForTesting
    public ImsServiceController getImsServiceController(int slotId, int feature) {
        if (slotId < 0 || slotId >= this.mNumSlots) {
            return null;
        }
        synchronized (this.mBoundServicesLock) {
            SparseArray<ImsServiceController> services = (SparseArray) this.mBoundImsServicesByFeature.get(slotId);
            if (services == null) {
                return null;
            }
            ImsServiceController controller = (ImsServiceController) services.get(feature);
            return controller;
        }
    }

    private SparseArray<ImsServiceController> getImsServiceControllers(int slotId) {
        if (slotId < 0 || slotId >= this.mNumSlots) {
            return null;
        }
        synchronized (this.mBoundServicesLock) {
            SparseArray<ImsServiceController> services = (SparseArray) this.mBoundImsServicesByFeature.get(slotId);
            if (services == null) {
                return null;
            }
            return services;
        }
    }

    @VisibleForTesting
    public ImsServiceController getImsServiceControllerAndListen(int slotId, int feature, IImsServiceFeatureCallback callback) {
        ImsServiceController controller = getImsServiceController(slotId, feature);
        if (controller == null) {
            return null;
        }
        controller.addImsServiceFeatureCallback(callback);
        return controller;
    }

    public boolean overrideImsServiceConfiguration(int slotId, boolean isCarrierService, String packageName) {
        if (slotId < 0 || slotId >= this.mNumSlots) {
            Log.w(TAG, "overrideImsServiceConfiguration: invalid slotId!");
            return false;
        } else if (packageName == null) {
            Log.w(TAG, "overrideImsServiceConfiguration: null packageName!");
            return false;
        } else {
            Message.obtain(this.mHandler, 5, slotId, isCarrierService, packageName).sendToTarget();
            return true;
        }
    }

    public String getImsServiceConfiguration(int slotId, boolean isCarrierService) {
        if (slotId < 0 || slotId >= this.mNumSlots) {
            Log.w(TAG, "getImsServiceConfiguration: invalid slotId!");
            return "";
        }
        return isCarrierService ? this.mCarrierServices[slotId] : this.mDeviceService;
    }

    private void putImsController(int slotId, int feature, ImsServiceController controller) {
        if (slotId < 0 || slotId >= this.mNumSlots || feature <= -1 || feature >= 3) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("putImsController received invalid parameters - slot: ");
            stringBuilder.append(slotId);
            stringBuilder.append(", feature: ");
            stringBuilder.append(feature);
            Log.w(str, stringBuilder.toString());
            return;
        }
        synchronized (this.mBoundServicesLock) {
            SparseArray<ImsServiceController> services = (SparseArray) this.mBoundImsServicesByFeature.get(slotId);
            if (services == null) {
                services = new SparseArray();
                this.mBoundImsServicesByFeature.add(slotId, services);
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("ImsServiceController added on slot: ");
            stringBuilder2.append(slotId);
            stringBuilder2.append(" with feature: ");
            stringBuilder2.append(feature);
            stringBuilder2.append(" using package: ");
            stringBuilder2.append(controller.getComponentName());
            Rlog.i(str2, stringBuilder2.toString());
            services.put(feature, controller);
        }
    }

    /* JADX WARNING: Missing block: B:19:0x0053, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private ImsServiceController removeImsController(int slotId, int feature) {
        if (slotId < 0 || slotId >= this.mNumSlots || feature <= -1 || feature >= 3) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeImsController received invalid parameters - slot: ");
            stringBuilder.append(slotId);
            stringBuilder.append(", feature: ");
            stringBuilder.append(feature);
            Log.w(str, stringBuilder.toString());
            return null;
        }
        synchronized (this.mBoundServicesLock) {
            SparseArray<ImsServiceController> services = (SparseArray) this.mBoundImsServicesByFeature.get(slotId);
            if (services == null) {
                return null;
            }
            ImsServiceController c = (ImsServiceController) services.get(feature, null);
            if (c != null) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("ImsServiceController removed on slot: ");
                stringBuilder2.append(slotId);
                stringBuilder2.append(" with feature: ");
                stringBuilder2.append(feature);
                stringBuilder2.append(" using package: ");
                stringBuilder2.append(c.getComponentName());
                Rlog.i(str2, stringBuilder2.toString());
                services.remove(feature);
            }
        }
    }

    private void maybeAddedImsService(String packageName) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("maybeAddedImsService, packageName: ");
        stringBuilder.append(packageName);
        Rlog.d(str, stringBuilder.toString());
        List<ImsServiceInfo> infos = getImsServiceInfo(packageName);
        List<ImsServiceInfo> newlyAddedInfos = new ArrayList();
        for (ImsServiceInfo info : infos) {
            ImsServiceInfo match = getInfoByComponentName(this.mInstalledServicesCache, info.name);
            String str2;
            StringBuilder stringBuilder2;
            if (match == null) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Adding newly added ImsService to cache: ");
                stringBuilder2.append(info.name);
                Log.i(str2, stringBuilder2.toString());
                this.mInstalledServicesCache.put(info.name, info);
                if (info.featureFromMetadata) {
                    newlyAddedInfos.add(info);
                } else {
                    scheduleQueryForFeatures(info);
                }
            } else if (info.featureFromMetadata) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Updating features in cached ImsService: ");
                stringBuilder2.append(info.name);
                Log.i(str2, stringBuilder2.toString());
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Updating features - Old features: ");
                stringBuilder2.append(match);
                stringBuilder2.append(" new features: ");
                stringBuilder2.append(info);
                Log.d(str2, stringBuilder2.toString());
                match.replaceFeatures(info.getSupportedFeatures());
                updateImsServiceFeatures(info);
            } else {
                scheduleQueryForFeatures(info);
            }
        }
        for (ImsServiceInfo info2 : newlyAddedInfos) {
            if (isActiveCarrierService(info2)) {
                bindImsService(info2);
                updateImsServiceFeatures(getImsServiceInfoFromCache(this.mDeviceService));
            } else if (isDeviceService(info2)) {
                bindImsService(info2);
            }
        }
    }

    private boolean maybeRemovedImsService(String packageName) {
        ImsServiceInfo match = getInfoByPackageName(this.mInstalledServicesCache, packageName);
        if (match == null) {
            return false;
        }
        this.mInstalledServicesCache.remove(match.name);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Removing ImsService: ");
        stringBuilder.append(match.name);
        Log.i(str, stringBuilder.toString());
        unbindImsService(match);
        updateImsServiceFeatures(getImsServiceInfoFromCache(this.mDeviceService));
        return true;
    }

    private boolean isActiveCarrierService(ImsServiceInfo info) {
        for (int i = 0; i < this.mNumSlots; i++) {
            if (TextUtils.equals(this.mCarrierServices[i], info.name.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isDeviceService(ImsServiceInfo info) {
        return TextUtils.equals(this.mDeviceService, info.name.getPackageName());
    }

    private int getSlotForActiveCarrierService(ImsServiceInfo info) {
        for (int i = 0; i < this.mNumSlots; i++) {
            if (TextUtils.equals(this.mCarrierServices[i], info.name.getPackageName())) {
                return i;
            }
        }
        return -1;
    }

    private ImsServiceController getControllerByServiceInfo(Map<ComponentName, ImsServiceController> searchMap, ImsServiceInfo matchValue) {
        return (ImsServiceController) searchMap.values().stream().filter(new -$$Lambda$ImsResolver$aWLlEvfonhYSfDR8cVsM6A5pmqI(matchValue)).findFirst().orElse(null);
    }

    private ImsServiceInfo getInfoByPackageName(Map<ComponentName, ImsServiceInfo> searchMap, String matchValue) {
        return (ImsServiceInfo) searchMap.values().stream().filter(new -$$Lambda$ImsResolver$rPjfocpARQ2sab24iic4o3kTTgw(matchValue)).findFirst().orElse(null);
    }

    private ImsServiceInfo getInfoByComponentName(Map<ComponentName, ImsServiceInfo> searchMap, ComponentName matchValue) {
        return (ImsServiceInfo) searchMap.get(matchValue);
    }

    private void updateImsServiceFeatures(ImsServiceInfo newInfo) {
        if (newInfo != null) {
            ImsServiceController controller = getControllerByServiceInfo(this.mActiveControllers, newInfo);
            HashSet<FeatureSlotPair> features = calculateFeaturesToCreate(newInfo);
            String str;
            StringBuilder stringBuilder;
            if (shouldFeaturesCauseBind(features)) {
                if (controller != null) {
                    try {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Updating features for ImsService: ");
                        stringBuilder.append(controller.getComponentName());
                        Log.i(str, stringBuilder.toString());
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Updating Features - New Features: ");
                        stringBuilder.append(features);
                        Log.d(str, stringBuilder.toString());
                        controller.changeImsServiceFeatures(features);
                    } catch (RemoteException e) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("updateImsServiceFeatures: Remote Exception: ");
                        stringBuilder2.append(e.getMessage());
                        Rlog.e(str2, stringBuilder2.toString());
                    }
                } else {
                    Log.i(TAG, "updateImsServiceFeatures: unbound with active features, rebinding");
                    bindImsServiceWithFeatures(newInfo, features);
                }
                if (isActiveCarrierService(newInfo) && !TextUtils.equals(newInfo.name.getPackageName(), this.mDeviceService)) {
                    Log.i(TAG, "Updating device default");
                    updateImsServiceFeatures(getImsServiceInfoFromCache(this.mDeviceService));
                }
            } else if (controller != null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unbinding: features = 0 for ImsService: ");
                stringBuilder.append(controller.getComponentName());
                Log.i(str, stringBuilder.toString());
                unbindImsService(newInfo);
            }
        }
    }

    private void bindImsService(ImsServiceInfo info) {
        if (!volte) {
            Rlog.i(TAG, "Not support volte, skip bind ImsService");
        } else if (info != null) {
            bindImsServiceWithFeatures(info, calculateFeaturesToCreate(info));
        }
    }

    private void bindImsServiceWithFeatures(ImsServiceInfo info, HashSet<FeatureSlotPair> features) {
        if (shouldFeaturesCauseBind(features)) {
            ImsServiceController controller = getControllerByServiceInfo(this.mActiveControllers, info);
            String str;
            StringBuilder stringBuilder;
            if (controller != null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("ImsService connection exists, updating features ");
                stringBuilder.append(features);
                Log.i(str, stringBuilder.toString());
                try {
                    controller.changeImsServiceFeatures(features);
                } catch (RemoteException e) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("bindImsService: error=");
                    stringBuilder2.append(e.getMessage());
                    Log.w(str2, stringBuilder2.toString());
                }
            } else {
                controller = info.controllerFactory.create(this.mContext, info.name, this);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Binding ImsService: ");
                stringBuilder.append(controller.getComponentName());
                stringBuilder.append(" with features: ");
                stringBuilder.append(features);
                Log.i(str, stringBuilder.toString());
                controller.bind(features);
            }
            this.mActiveControllers.put(info.name, controller);
        }
    }

    private void unbindImsService(ImsServiceInfo info) {
        if (info != null) {
            ImsServiceController controller = getControllerByServiceInfo(this.mActiveControllers, info);
            if (controller != null) {
                try {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unbinding ImsService: ");
                    stringBuilder.append(controller.getComponentName());
                    Log.i(str, stringBuilder.toString());
                    controller.unbind();
                } catch (RemoteException e) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("unbindImsService: Remote Exception: ");
                    stringBuilder2.append(e.getMessage());
                    Rlog.e(str2, stringBuilder2.toString());
                }
                this.mActiveControllers.remove(info.name);
            }
        }
    }

    private HashSet<FeatureSlotPair> calculateFeaturesToCreate(ImsServiceInfo info) {
        HashSet<FeatureSlotPair> imsFeaturesBySlot = new HashSet();
        int slotId = getSlotForActiveCarrierService(info);
        if (slotId != -1) {
            imsFeaturesBySlot.addAll((Collection) info.getSupportedFeatures().stream().filter(new -$$Lambda$ImsResolver$-jFhgP_NotuFSwzjQBXWuvls4x4(slotId)).collect(Collectors.toList()));
        } else if (isDeviceService(info)) {
            for (int i = 0; i < this.mNumSlots; i++) {
                int currSlotId = i;
                ImsServiceInfo carrierImsInfo = getImsServiceInfoFromCache(this.mCarrierServices[i]);
                if (carrierImsInfo == null) {
                    imsFeaturesBySlot.addAll((Collection) info.getSupportedFeatures().stream().filter(new -$$Lambda$ImsResolver$VfY5To_kbbTJevLzywTg-_S1JhA(currSlotId)).collect(Collectors.toList()));
                } else {
                    HashSet<FeatureSlotPair> deviceFeatures = new HashSet(info.getSupportedFeatures());
                    deviceFeatures.removeAll(carrierImsInfo.getSupportedFeatures());
                    imsFeaturesBySlot.addAll((Collection) deviceFeatures.stream().filter(new -$$Lambda$ImsResolver$kF808g2NWzNL8H1SwzDc1FxiQdQ(currSlotId)).collect(Collectors.toList()));
                }
            }
        }
        return imsFeaturesBySlot;
    }

    static /* synthetic */ boolean lambda$calculateFeaturesToCreate$3(int slotId, FeatureSlotPair feature) {
        return slotId == feature.slotId;
    }

    static /* synthetic */ boolean lambda$calculateFeaturesToCreate$4(int currSlotId, FeatureSlotPair feature) {
        return currSlotId == feature.slotId;
    }

    static /* synthetic */ boolean lambda$calculateFeaturesToCreate$5(int currSlotId, FeatureSlotPair feature) {
        return currSlotId == feature.slotId;
    }

    public void imsServiceFeatureCreated(int slotId, int feature, ImsServiceController controller) {
        putImsController(slotId, feature, controller);
    }

    public void imsServiceFeatureRemoved(int slotId, int feature, ImsServiceController controller) {
        removeImsController(slotId, feature);
    }

    public void imsServiceFeaturesChanged(ImsFeatureConfiguration config, ImsServiceController controller) {
        if (controller != null && config != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("imsServiceFeaturesChanged: config=");
            stringBuilder.append(config.getServiceFeatures());
            stringBuilder.append(", ComponentName=");
            stringBuilder.append(controller.getComponentName());
            Log.i(str, stringBuilder.toString());
            handleFeaturesChanged(controller.getComponentName(), config.getServiceFeatures());
        }
    }

    private boolean shouldFeaturesCauseBind(HashSet<FeatureSlotPair> features) {
        return features.stream().filter(-$$Lambda$ImsResolver$SIkPixr-qGLIK-usUJIKu6S5BBs.INSTANCE).count() > 0;
    }

    static /* synthetic */ boolean lambda$shouldFeaturesCauseBind$6(FeatureSlotPair f) {
        return f.featureType != 0;
    }

    private void maybeRebindService(int slotId, String newPackageName) {
        if (slotId <= -1) {
            for (int i = 0; i < this.mNumSlots; i++) {
                updateBoundCarrierServices(i, newPackageName);
            }
            return;
        }
        updateBoundCarrierServices(slotId, newPackageName);
    }

    private void carrierConfigChanged(int slotId) {
        PersistableBundle config = this.mCarrierConfigManager.getConfigForSubId(this.mSubscriptionManagerProxy.getSubId(slotId));
        if (config != null) {
            maybeRebindService(slotId, config.getString("config_ims_package_override_string", null));
        } else {
            Log.w(TAG, "carrierConfigChanged: CarrierConfig is null!");
        }
    }

    private void updateBoundCarrierServices(int slotId, String newPackageName) {
        if (slotId > -1 && slotId < this.mNumSlots) {
            String oldPackageName = this.mCarrierServices[slotId];
            this.mCarrierServices[slotId] = newPackageName;
            if (!TextUtils.equals(newPackageName, oldPackageName)) {
                Rlog.i(TAG, "Carrier Config updated, binding new ImsService");
                unbindImsService(getImsServiceInfoFromCache(oldPackageName));
                ImsServiceInfo newInfo = getImsServiceInfoFromCache(newPackageName);
                if (newInfo == null || newInfo.featureFromMetadata) {
                    bindImsService(newInfo);
                    updateImsServiceFeatures(getImsServiceInfoFromCache(this.mDeviceService));
                    return;
                }
                scheduleQueryForFeatures(newInfo);
            }
        }
    }

    private void scheduleQueryForFeatures(ImsServiceInfo service, int delayMs) {
        if (isDeviceService(service) || getSlotForActiveCarrierService(service) != -1) {
            Message msg = Message.obtain(this.mHandler, 3, service);
            String str;
            StringBuilder stringBuilder;
            if (this.mHandler.hasMessages(3, service)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("scheduleQueryForFeatures: dynamic query for ");
                stringBuilder.append(service.name);
                stringBuilder.append(" already scheduled");
                Log.d(str, stringBuilder.toString());
                return;
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("scheduleQueryForFeatures: starting dynamic query for ");
            stringBuilder.append(service.name);
            stringBuilder.append(" in ");
            stringBuilder.append(delayMs);
            stringBuilder.append("ms.");
            Log.d(str, stringBuilder.toString());
            this.mHandler.sendMessageDelayed(msg, (long) delayMs);
            return;
        }
        Log.i(TAG, "scheduleQueryForFeatures: skipping query for ImsService that is not set as carrier/device ImsService.");
    }

    private void scheduleQueryForFeatures(ComponentName name, int delayMs) {
        ImsServiceInfo service = getImsServiceInfoFromCache(name.getPackageName());
        if (service == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("scheduleQueryForFeatures: Couldn't find cached info for name: ");
            stringBuilder.append(name);
            Log.w(str, stringBuilder.toString());
            return;
        }
        scheduleQueryForFeatures(service, delayMs);
    }

    private void scheduleQueryForFeatures(ImsServiceInfo service) {
        scheduleQueryForFeatures(service, 0);
    }

    private void handleFeaturesChanged(ComponentName name, Set<FeatureSlotPair> features) {
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = name;
        args.arg2 = features;
        this.mHandler.obtainMessage(4, args).sendToTarget();
    }

    private void startDynamicQuery(ImsServiceInfo service) {
        if (this.mFeatureQueryManager.startQuery(service.name, service.controllerFactory.getServiceInterface())) {
            Log.d(TAG, "startDynamicQuery: Service queried, waiting for response.");
            return;
        }
        Log.w(TAG, "startDynamicQuery: service could not connect. Retrying after delay.");
        scheduleQueryForFeatures(service, 5000);
    }

    private void dynamicQueryComplete(ComponentName name, Set<FeatureSlotPair> features) {
        ImsServiceInfo service = getImsServiceInfoFromCache(name.getPackageName());
        if (service == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleFeaturesChanged: Couldn't find cached info for name: ");
            stringBuilder.append(name);
            Log.w(str, stringBuilder.toString());
            return;
        }
        service.replaceFeatures(features);
        if (isActiveCarrierService(service)) {
            bindImsService(service);
            updateImsServiceFeatures(getImsServiceInfoFromCache(this.mDeviceService));
        } else if (isDeviceService(service)) {
            bindImsService(service);
        }
    }

    public boolean isResolvingBinding() {
        return this.mHandler.hasMessages(3) || this.mHandler.hasMessages(4) || this.mFeatureQueryManager.isQueryInProgress();
    }

    private String printFeatures(Set<FeatureSlotPair> features) {
        StringBuilder featureString = new StringBuilder();
        featureString.append("features: [");
        if (features != null) {
            for (FeatureSlotPair feature : features) {
                featureString.append("{");
                featureString.append(feature.slotId);
                featureString.append(",");
                featureString.append(feature.featureType);
                featureString.append("} ");
            }
            featureString.append("]");
        }
        return featureString.toString();
    }

    @VisibleForTesting
    public ImsServiceInfo getImsServiceInfoFromCache(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return null;
        }
        ImsServiceInfo infoFilter = getInfoByPackageName(this.mInstalledServicesCache, packageName);
        if (infoFilter != null) {
            return infoFilter;
        }
        return null;
    }

    private List<ImsServiceInfo> getImsServiceInfo(String packageName) {
        List<ImsServiceInfo> infos = new ArrayList();
        if (this.mIsDynamicBinding) {
            infos.addAll(searchForImsServices(packageName, this.mImsServiceControllerFactory));
            infos.addAll(searchForImsServices(packageName, this.mImsServiceControllerFactoryCompat));
        } else {
            infos.addAll(getStaticImsService());
        }
        return infos;
    }

    private List<ImsServiceInfo> getStaticImsService() {
        List<ImsServiceInfo> infos = new ArrayList();
        ImsServiceInfo info = new ImsServiceInfo(this.mNumSlots);
        info.name = this.mStaticComponent;
        info.controllerFactory = this.mImsServiceControllerFactoryStaticBindingCompat;
        info.addFeatureForAllSlots(0);
        info.addFeatureForAllSlots(1);
        infos.add(info);
        return infos;
    }

    private List<ImsServiceInfo> searchForImsServices(String packageName, ImsServiceControllerFactory controllerFactory) {
        List<ImsServiceInfo> infos = new ArrayList();
        Intent serviceIntent = new Intent(controllerFactory.getServiceInterface());
        serviceIntent.setPackage(packageName);
        for (ResolveInfo entry : this.mContext.getPackageManager().queryIntentServicesAsUser(serviceIntent, 128, this.mContext.getUserId())) {
            ServiceInfo serviceInfo = entry.serviceInfo;
            if (serviceInfo != null) {
                ImsServiceInfo info = new ImsServiceInfo(this.mNumSlots);
                info.name = new ComponentName(serviceInfo.packageName, serviceInfo.name);
                info.controllerFactory = controllerFactory;
                if (isDeviceService(info) || this.mImsServiceControllerFactoryCompat == controllerFactory) {
                    if (serviceInfo.metaData != null) {
                        if (serviceInfo.metaData.getBoolean(METADATA_EMERGENCY_MMTEL_FEATURE, false)) {
                            info.addFeatureForAllSlots(0);
                        }
                        if (serviceInfo.metaData.getBoolean(METADATA_MMTEL_FEATURE, false)) {
                            info.addFeatureForAllSlots(1);
                        }
                        if (serviceInfo.metaData.getBoolean(METADATA_RCS_FEATURE, false)) {
                            info.addFeatureForAllSlots(2);
                        }
                    }
                    if (this.mImsServiceControllerFactoryCompat != controllerFactory && info.getSupportedFeatures().isEmpty()) {
                        info.featureFromMetadata = false;
                    }
                } else {
                    info.featureFromMetadata = false;
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("service name: ");
                stringBuilder.append(info.name);
                stringBuilder.append(", manifest query: ");
                stringBuilder.append(info.featureFromMetadata);
                Log.i(str, stringBuilder.toString());
                StringBuilder stringBuilder2;
                if (TextUtils.equals(serviceInfo.permission, "android.permission.BIND_IMS_SERVICE") || (serviceInfo.metaData != null && serviceInfo.metaData.getBoolean(METADATA_OVERRIDE_PERM_CHECK, false))) {
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ImsService (");
                    stringBuilder2.append(serviceIntent);
                    stringBuilder2.append(") added to cache: ");
                    stringBuilder2.append(info.name);
                    Log.d(str, stringBuilder2.toString());
                    infos.add(info);
                } else {
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ImsService is not protected with BIND_IMS_SERVICE permission: ");
                    stringBuilder2.append(info.name);
                    Log.w(str, stringBuilder2.toString());
                }
            }
        }
        return infos;
    }
}
