package android.content.pm;

import android.Manifest.permission;
import android.accounts.GrantCredentialsPermissionActivity;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.hwtheme.HwThemeManager;
import android.os.Environment;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.AtomicFile;
import android.util.AttributeSet;
import android.util.IntArray;
import android.util.Log;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastXmlSerializer;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public abstract class RegisteredServicesCache<V> {
    private static final boolean DEBUG = false;
    protected static final String REGISTERED_SERVICES_DIR = "registered_services";
    private static final String TAG = "PackageManager";
    private final String mAttributesName;
    public final Context mContext;
    private final BroadcastReceiver mExternalReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            RegisteredServicesCache.this.handlePackageEvent(intent, 0);
        }
    };
    private Handler mHandler;
    private final String mInterfaceName;
    private RegisteredServicesCacheListener<V> mListener;
    private final String mMetaDataName;
    private final BroadcastReceiver mPackageReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            int uid = intent.getIntExtra(Intent.EXTRA_UID, -1);
            if (uid != -1) {
                RegisteredServicesCache.this.handlePackageEvent(intent, UserHandle.getUserId(uid));
            }
        }
    };
    private final XmlSerializerAndParser<V> mSerializerAndParser;
    protected final Object mServicesLock = new Object();
    private final BroadcastReceiver mUserRemovedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            RegisteredServicesCache.this.onUserRemoved(intent.getIntExtra(Intent.EXTRA_USER_HANDLE, -1));
        }
    };
    @GuardedBy("mServicesLock")
    private final SparseArray<UserServices<V>> mUserServices = new SparseArray(2);

    public static class ServiceInfo<V> {
        public final ComponentInfo componentInfo;
        public final ComponentName componentName;
        public final V type;
        public final int uid;

        public ServiceInfo(V type, ComponentInfo componentInfo, ComponentName componentName) {
            this.type = type;
            this.componentInfo = componentInfo;
            this.componentName = componentName;
            this.uid = componentInfo != null ? componentInfo.applicationInfo.uid : -1;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ServiceInfo: ");
            stringBuilder.append(this.type);
            stringBuilder.append(", ");
            stringBuilder.append(this.componentName);
            stringBuilder.append(", uid ");
            stringBuilder.append(this.uid);
            return stringBuilder.toString();
        }
    }

    private static class UserServices<V> {
        @GuardedBy("mServicesLock")
        boolean mBindInstantServiceAllowed;
        @GuardedBy("mServicesLock")
        boolean mPersistentServicesFileDidNotExist;
        @GuardedBy("mServicesLock")
        final Map<V, Integer> persistentServices;
        @GuardedBy("mServicesLock")
        Map<V, ServiceInfo<V>> services;

        private UserServices() {
            this.persistentServices = Maps.newHashMap();
            this.services = null;
            this.mPersistentServicesFileDidNotExist = true;
            this.mBindInstantServiceAllowed = false;
        }

        /* synthetic */ UserServices(AnonymousClass1 x0) {
            this();
        }
    }

    public abstract V parseServiceAttributes(Resources resources, String str, AttributeSet attributeSet);

    @GuardedBy("mServicesLock")
    private UserServices<V> findOrCreateUserLocked(int userId) {
        return findOrCreateUserLocked(userId, true);
    }

    @GuardedBy("mServicesLock")
    private UserServices<V> findOrCreateUserLocked(int userId, boolean loadFromFileIfNew) {
        UserServices<V> services = (UserServices) this.mUserServices.get(userId);
        if (services == null) {
            InputStream is = null;
            services = new UserServices();
            this.mUserServices.put(userId, services);
            if (loadFromFileIfNew && this.mSerializerAndParser != null) {
                UserInfo user = getUser(userId);
                if (user != null) {
                    AtomicFile file = createFileForUser(user.id);
                    if (file.getBaseFile().exists()) {
                        try {
                            is = file.openRead();
                            readPersistentServicesLocked(is);
                        } catch (Exception e) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Error reading persistent services for user ");
                            stringBuilder.append(user.id);
                            Log.w(str, stringBuilder.toString(), e);
                        } catch (Throwable th) {
                            IoUtils.closeQuietly(is);
                        }
                        IoUtils.closeQuietly(is);
                    }
                }
            }
        }
        return services;
    }

    public RegisteredServicesCache(Context context, String interfaceName, String metaDataName, String attributeName, XmlSerializerAndParser<V> serializerAndParser) {
        this.mContext = context;
        this.mInterfaceName = interfaceName;
        this.mMetaDataName = metaDataName;
        this.mAttributesName = attributeName;
        this.mSerializerAndParser = serializerAndParser;
        migrateIfNecessaryLocked();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mPackageReceiver, UserHandle.ALL, intentFilter, null, null);
        IntentFilter sdFilter = new IntentFilter();
        sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        sdFilter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        this.mContext.registerReceiver(this.mExternalReceiver, sdFilter);
        IntentFilter userFilter = new IntentFilter();
        sdFilter.addAction(Intent.ACTION_USER_REMOVED);
        this.mContext.registerReceiver(this.mUserRemovedReceiver, userFilter);
    }

    private final void handlePackageEvent(Intent intent, int userId) {
        String action = intent.getAction();
        boolean isRemoval = Intent.ACTION_PACKAGE_REMOVED.equals(action) || Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE.equals(action);
        boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
        if (!isRemoval || !replacing) {
            int[] uids = null;
            if (Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(action) || Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE.equals(action)) {
                uids = intent.getIntArrayExtra(Intent.EXTRA_CHANGED_UID_LIST);
            } else {
                if (intent.getIntExtra(Intent.EXTRA_UID, -1) > 0) {
                    uids = new int[]{intent.getIntExtra(Intent.EXTRA_UID, -1)};
                }
            }
            generateServicesMap(uids, userId);
        }
    }

    public void invalidateCache(int userId) {
        synchronized (this.mServicesLock) {
            findOrCreateUserLocked(userId).services = null;
            onServicesChangedLocked(userId);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter fout, String[] args, int userId) {
        synchronized (this.mServicesLock) {
            UserServices<V> user = findOrCreateUserLocked(userId);
            if (user.services != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("RegisteredServicesCache: ");
                stringBuilder.append(user.services.size());
                stringBuilder.append(" services");
                fout.println(stringBuilder.toString());
                for (ServiceInfo<?> info : user.services.values()) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("  ");
                    stringBuilder2.append(info);
                    fout.println(stringBuilder2.toString());
                }
            } else {
                fout.println("RegisteredServicesCache: services not loaded");
            }
        }
    }

    public RegisteredServicesCacheListener<V> getListener() {
        RegisteredServicesCacheListener registeredServicesCacheListener;
        synchronized (this) {
            registeredServicesCacheListener = this.mListener;
        }
        return registeredServicesCacheListener;
    }

    public void setListener(RegisteredServicesCacheListener<V> listener, Handler handler) {
        if (handler == null) {
            handler = new Handler(this.mContext.getMainLooper());
        }
        synchronized (this) {
            this.mHandler = handler;
            this.mListener = listener;
        }
    }

    private void notifyListener(V type, int userId, boolean removed) {
        RegisteredServicesCacheListener<V> listener;
        Handler handler;
        synchronized (this) {
            listener = this.mListener;
            handler = this.mHandler;
        }
        if (listener != null) {
            final RegisteredServicesCacheListener<V> listener2 = listener;
            final V v = type;
            final int i = userId;
            final boolean z = removed;
            handler.post(new Runnable() {
                public void run() {
                    listener2.onServiceChanged(v, i, z);
                }
            });
        }
    }

    public ServiceInfo<V> getServiceInfo(V type, int userId) {
        ServiceInfo serviceInfo;
        synchronized (this.mServicesLock) {
            UserServices<V> user = findOrCreateUserLocked(userId);
            if (user.services == null) {
                generateServicesMap(null, userId);
            }
            serviceInfo = (ServiceInfo) user.services.get(type);
        }
        return serviceInfo;
    }

    public Collection<ServiceInfo<V>> getAllServices(int userId) {
        Collection unmodifiableCollection;
        synchronized (this.mServicesLock) {
            UserServices<V> user = findOrCreateUserLocked(userId);
            if (user.services == null) {
                generateServicesMap(null, userId);
            }
            unmodifiableCollection = Collections.unmodifiableCollection(new ArrayList(user.services.values()));
        }
        return unmodifiableCollection;
    }

    /* JADX WARNING: Missing block: B:9:0x001a, code skipped:
            r0 = null;
            r2 = r1.iterator();
     */
    /* JADX WARNING: Missing block: B:11:0x0023, code skipped:
            if (r2.hasNext() == false) goto L_0x0064;
     */
    /* JADX WARNING: Missing block: B:12:0x0025, code skipped:
            r3 = (android.content.pm.RegisteredServicesCache.ServiceInfo) r2.next();
            r4 = (long) r3.componentInfo.applicationInfo.versionCode;
            r7 = null;
     */
    /* JADX WARNING: Missing block: B:15:0x0042, code skipped:
            r7 = r11.mContext.getPackageManager().getApplicationInfoAsUser(r3.componentInfo.packageName, 0, r12);
     */
    /* JADX WARNING: Missing block: B:17:0x0045, code skipped:
            android.util.Log.e(TAG, "updateServices()");
     */
    /* JADX WARNING: Missing block: B:25:0x0064, code skipped:
            if (r0 == null) goto L_0x0073;
     */
    /* JADX WARNING: Missing block: B:27:0x006a, code skipped:
            if (r0.size() <= 0) goto L_0x0073;
     */
    /* JADX WARNING: Missing block: B:28:0x006c, code skipped:
            generateServicesMap(r0.toArray(), r12);
     */
    /* JADX WARNING: Missing block: B:29:0x0073, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateServices(int userId) {
        synchronized (this.mServicesLock) {
            UserServices<V> user = findOrCreateUserLocked(userId);
            if (user.services == null) {
                return;
            }
            ArrayList allServices = new ArrayList(user.services.values());
        }
        if (newAppInfo == null || ((long) newAppInfo.versionCode) != versionCode) {
            IntArray updatedUids;
            if (updatedUids == null) {
                updatedUids = new IntArray();
            }
            updatedUids.add(service.uid);
        }
    }

    public boolean getBindInstantServiceAllowed(int userId) {
        boolean z;
        this.mContext.enforceCallingOrSelfPermission(permission.MANAGE_BIND_INSTANT_SERVICE, "getBindInstantServiceAllowed");
        synchronized (this.mServicesLock) {
            z = findOrCreateUserLocked(userId).mBindInstantServiceAllowed;
        }
        return z;
    }

    public void setBindInstantServiceAllowed(int userId, boolean allowed) {
        this.mContext.enforceCallingOrSelfPermission(permission.MANAGE_BIND_INSTANT_SERVICE, "setBindInstantServiceAllowed");
        synchronized (this.mServicesLock) {
            findOrCreateUserLocked(userId).mBindInstantServiceAllowed = allowed;
        }
    }

    @VisibleForTesting
    protected boolean inSystemImage(int callerUid) {
        String[] packages = this.mContext.getPackageManager().getPackagesForUid(callerUid);
        if (packages != null) {
            int length = packages.length;
            int i = 0;
            while (i < length) {
                try {
                    if ((this.mContext.getPackageManager().getPackageInfo(packages[i], 0).applicationInfo.flags & 1) != 0) {
                        return true;
                    }
                    i++;
                } catch (NameNotFoundException e) {
                    return false;
                }
            }
        }
        return false;
    }

    @VisibleForTesting
    protected List<ResolveInfo> queryIntentServices(int userId) {
        PackageManager pm = this.mContext.getPackageManager();
        int flags = 786560;
        synchronized (this.mServicesLock) {
            if (findOrCreateUserLocked(userId).mBindInstantServiceAllowed) {
                flags = 786560 | 8388608;
            }
        }
        return pm.queryIntentServicesAsUser(new Intent(this.mInterfaceName), flags, userId);
    }

    /* JADX WARNING: Missing block: B:59:0x015c, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void generateServicesMap(int[] changedUids, int userId) {
        Throwable th;
        int i = userId;
        ArrayList<ServiceInfo<V>> serviceInfos = new ArrayList();
        for (ResolveInfo resolveInfo : queryIntentServices(i)) {
            String str;
            StringBuilder stringBuilder;
            try {
                ServiceInfo<V> info = parseServiceInfo(resolveInfo);
                if (info == null) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to load service info ");
                    stringBuilder.append(resolveInfo.toString());
                    Log.w(str, stringBuilder.toString());
                } else {
                    serviceInfos.add(info);
                }
            } catch (IOException | XmlPullParserException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to load service info ");
                stringBuilder.append(resolveInfo.toString());
                Log.w(str, stringBuilder.toString(), e);
            }
        }
        synchronized (this.mServicesLock) {
            int[] iArr;
            try {
                V v1;
                UserServices<V> user = findOrCreateUserLocked(i);
                boolean firstScan = user.services == null;
                if (firstScan) {
                    user.services = Maps.newHashMap();
                }
                StringBuilder changes = new StringBuilder();
                boolean changed = false;
                Iterator it = serviceInfos.iterator();
                while (it.hasNext()) {
                    ServiceInfo<V> info2 = (ServiceInfo) it.next();
                    Integer previousUid = (Integer) user.persistentServices.get(info2.type);
                    if (previousUid == null) {
                        changed = true;
                        user.services.put(info2.type, info2);
                        user.persistentServices.put(info2.type, Integer.valueOf(info2.uid));
                        if (!user.mPersistentServicesFileDidNotExist || !firstScan) {
                            notifyListener(info2.type, i, false);
                        }
                    } else if (previousUid.intValue() == info2.uid) {
                        user.services.put(info2.type, info2);
                    } else if (inSystemImage(info2.uid) || !containsTypeAndUid(serviceInfos, info2.type, previousUid.intValue())) {
                        user.services.put(info2.type, info2);
                        user.persistentServices.put(info2.type, Integer.valueOf(info2.uid));
                        notifyListener(info2.type, i, false);
                        changed = true;
                    }
                }
                ArrayList<V> toBeRemoved = Lists.newArrayList();
                for (V v12 : user.persistentServices.keySet()) {
                    if (containsType(serviceInfos, v12)) {
                        iArr = changedUids;
                    } else if (containsUid(changedUids, ((Integer) user.persistentServices.get(v12)).intValue())) {
                        toBeRemoved.add(v12);
                    }
                }
                iArr = changedUids;
                Iterator it2 = toBeRemoved.iterator();
                while (it2.hasNext()) {
                    v12 = it2.next();
                    changed = true;
                    user.persistentServices.remove(v12);
                    user.services.remove(v12);
                    notifyListener(v12, i, true);
                }
                if (changed) {
                    onServicesChangedLocked(i);
                    writePersistentServicesLocked(user, i);
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    protected void onServicesChangedLocked(int userId) {
    }

    private boolean containsUid(int[] changedUids, int uid) {
        return changedUids == null || ArrayUtils.contains(changedUids, uid);
    }

    private boolean containsType(ArrayList<ServiceInfo<V>> serviceInfos, V type) {
        int N = serviceInfos.size();
        for (int i = 0; i < N; i++) {
            if (((ServiceInfo) serviceInfos.get(i)).type.equals(type)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsTypeAndUid(ArrayList<ServiceInfo<V>> serviceInfos, V type, int uid) {
        int N = serviceInfos.size();
        for (int i = 0; i < N; i++) {
            ServiceInfo<V> serviceInfo = (ServiceInfo) serviceInfos.get(i);
            if (serviceInfo.type.equals(type) && serviceInfo.uid == uid) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    protected ServiceInfo<V> parseServiceInfo(ResolveInfo service) throws XmlPullParserException, IOException {
        ServiceInfo si = service.serviceInfo;
        ComponentName componentName = new ComponentName(si.packageName, si.name);
        PackageManager pm = this.mContext.getPackageManager();
        XmlResourceParser parser = null;
        try {
            parser = si.loadXmlMetaData(pm, this.mMetaDataName);
            if (parser != null) {
                AttributeSet attrs = Xml.asAttributeSet(parser);
                while (true) {
                    int next = parser.next();
                    int type = next;
                    if (next == 1 || type == 2) {
                    }
                }
                if (this.mAttributesName.equals(parser.getName())) {
                    V v = parseServiceAttributes(pm.getResourcesForApplication(si.applicationInfo), si.packageName, attrs);
                    if (v == null) {
                        if (parser != null) {
                            parser.close();
                        }
                        return null;
                    }
                    ServiceInfo serviceInfo = new ServiceInfo(v, service.serviceInfo, componentName);
                    if (parser != null) {
                        parser.close();
                    }
                    return serviceInfo;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Meta-data does not start with ");
                stringBuilder.append(this.mAttributesName);
                stringBuilder.append(" tag");
                throw new XmlPullParserException(stringBuilder.toString());
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("No ");
            stringBuilder2.append(this.mMetaDataName);
            stringBuilder2.append(" meta-data");
            throw new XmlPullParserException(stringBuilder2.toString());
        } catch (NameNotFoundException e) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Unable to load resources for pacakge ");
            stringBuilder3.append(si.packageName);
            throw new XmlPullParserException(stringBuilder3.toString());
        } catch (Throwable th) {
            if (parser != null) {
                parser.close();
            }
        }
    }

    private void readPersistentServicesLocked(InputStream is) throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(is, StandardCharsets.UTF_8.name());
        int eventType = parser.getEventType();
        while (eventType != 2 && eventType != 1) {
            eventType = parser.next();
        }
        if ("services".equals(parser.getName())) {
            eventType = parser.next();
            do {
                if (eventType == 2 && parser.getDepth() == 2) {
                    if (Notification.CATEGORY_SERVICE.equals(parser.getName())) {
                        V service = this.mSerializerAndParser.createFromXml(parser);
                        if (service != null) {
                            int uid = Integer.parseInt(parser.getAttributeValue(null, GrantCredentialsPermissionActivity.EXTRAS_REQUESTING_UID));
                            findOrCreateUserLocked(UserHandle.getUserId(uid), null).persistentServices.put(service, Integer.valueOf(uid));
                        } else {
                            return;
                        }
                    }
                }
                eventType = parser.next();
            } while (eventType != 1);
        }
    }

    private void migrateIfNecessaryLocked() {
        if (this.mSerializerAndParser != null) {
            File syncDir = new File(new File(getDataDirectory(), HwThemeManager.HWT_USER_SYSTEM), REGISTERED_SERVICES_DIR);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mInterfaceName);
            stringBuilder.append(".xml");
            AtomicFile oldFile = new AtomicFile(new File(syncDir, stringBuilder.toString()));
            if (oldFile.getBaseFile().exists()) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(this.mInterfaceName);
                stringBuilder2.append(".xml.migrated");
                File marker = new File(syncDir, stringBuilder2.toString());
                if (!marker.exists()) {
                    InputStream is = null;
                    try {
                        is = oldFile.openRead();
                        this.mUserServices.clear();
                        readPersistentServicesLocked(is);
                    } catch (Exception e) {
                        Log.w(TAG, "Error reading persistent services, starting from scratch", e);
                    } catch (Throwable th) {
                        IoUtils.closeQuietly(is);
                    }
                    IoUtils.closeQuietly(is);
                    try {
                        for (UserInfo user : getUsers()) {
                            UserServices<V> userServices = (UserServices) this.mUserServices.get(user.id);
                            if (userServices != null) {
                                writePersistentServicesLocked(userServices, user.id);
                            }
                        }
                        marker.createNewFile();
                    } catch (Exception e2) {
                        Log.w(TAG, "Migration failed", e2);
                    }
                    this.mUserServices.clear();
                }
            }
        }
    }

    private void writePersistentServicesLocked(UserServices<V> user, int userId) {
        if (this.mSerializerAndParser != null) {
            AtomicFile atomicFile = createFileForUser(userId);
            FileOutputStream fos = null;
            try {
                fos = atomicFile.startWrite();
                XmlSerializer out = new FastXmlSerializer();
                out.setOutput(fos, StandardCharsets.UTF_8.name());
                out.startDocument(null, Boolean.valueOf(true));
                out.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                out.startTag(null, "services");
                for (Entry<V, Integer> service : user.persistentServices.entrySet()) {
                    out.startTag(null, Notification.CATEGORY_SERVICE);
                    out.attribute(null, GrantCredentialsPermissionActivity.EXTRAS_REQUESTING_UID, Integer.toString(((Integer) service.getValue()).intValue()));
                    this.mSerializerAndParser.writeAsXml(service.getKey(), out);
                    out.endTag(null, Notification.CATEGORY_SERVICE);
                }
                out.endTag(null, "services");
                out.endDocument();
                atomicFile.finishWrite(fos);
            } catch (IOException e1) {
                Log.w(TAG, "Error writing accounts", e1);
                if (fos != null) {
                    atomicFile.failWrite(fos);
                }
            }
        }
    }

    @VisibleForTesting
    protected void onUserRemoved(int userId) {
        synchronized (this.mServicesLock) {
            this.mUserServices.remove(userId);
        }
    }

    @VisibleForTesting
    protected List<UserInfo> getUsers() {
        return UserManager.get(this.mContext).getUsers(true);
    }

    @VisibleForTesting
    protected UserInfo getUser(int userId) {
        return UserManager.get(this.mContext).getUserInfo(userId);
    }

    private AtomicFile createFileForUser(int userId) {
        File userDir = getUserSystemDirectory(userId);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("registered_services/");
        stringBuilder.append(this.mInterfaceName);
        stringBuilder.append(".xml");
        return new AtomicFile(new File(userDir, stringBuilder.toString()));
    }

    @VisibleForTesting
    protected File getUserSystemDirectory(int userId) {
        return Environment.getUserSystemDirectory(userId);
    }

    @VisibleForTesting
    protected File getDataDirectory() {
        return Environment.getDataDirectory();
    }

    @VisibleForTesting
    protected Map<V, Integer> getPersistentServices(int userId) {
        return findOrCreateUserLocked(userId).persistentServices;
    }
}
