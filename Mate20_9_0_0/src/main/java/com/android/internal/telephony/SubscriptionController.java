package com.android.internal.telephony;

import android.app.AppOpsManager;
import android.common.HwFrameworkFactory;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccAccessRule;
import android.telephony.euicc.EuiccManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.annotations.VisibleForTesting.Visibility;
import com.android.internal.telephony.ITelephonyRegistry.Stub;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.internal.telephony.dataconnection.KeepaliveStatus;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccController;
import dalvik.system.PathClassLoader;
import huawei.android.security.IHwBehaviorCollectManager;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SubscriptionController extends AbstractSubscriptionController {
    public static final int CLAT_SET_ERROR = 4;
    public static final int CLAT_SET_FALSE = 1;
    public static final int CLAT_SET_NULL = 3;
    public static final int CLAT_SET_TRUE = 2;
    static final boolean DBG = true;
    static final boolean DBG_CACHE = false;
    private static final int DEPRECATED_SETTING = -1;
    public static final boolean IS_FAST_SWITCH_SIMSLOT = SystemProperties.getBoolean("ro.config.fast_switch_simslot", false);
    static final String LOG_TAG = "SubscriptionController";
    static final int MAX_LOCAL_LOG_LINES = 500;
    private static final Comparator<SubscriptionInfo> SUBSCRIPTION_INFO_COMPARATOR = -$$Lambda$SubscriptionController$Nt_ojdeqo4C2mbuwymYLvwgOLGo.INSTANCE;
    static final boolean VDBG = false;
    private static int mDefaultFallbackSubId = 0;
    private static int mDefaultPhoneId = 0;
    private static SubscriptionController sInstance = null;
    protected static Phone[] sPhones;
    private static Map<Integer, Integer> sSlotIndexToSubId = new ConcurrentHashMap();
    private int[] colorArr;
    private AppOpsManager mAppOps;
    protected CallManager mCM;
    private final List<SubscriptionInfo> mCacheActiveSubInfoList = new ArrayList();
    protected Context mContext;
    private long mLastISubServiceRegTime;
    private ScLocalLog mLocalLog = new ScLocalLog(500);
    protected final Object mLock = new Object();
    protected TelephonyManager mTelephonyManager;
    private Object qcRilHook = null;

    static class ScLocalLog {
        private LinkedList<String> mLog = new LinkedList();

        public ScLocalLog(int maxLines) {
        }

        public synchronized void log(String msg) {
        }

        public synchronized void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            int i = 0;
            Iterator<String> itr = this.mLog.listIterator(0);
            while (itr.hasNext()) {
                StringBuilder stringBuilder = new StringBuilder();
                int i2 = i + 1;
                stringBuilder.append(Integer.toString(i));
                stringBuilder.append(": ");
                stringBuilder.append((String) itr.next());
                pw.println(stringBuilder.toString());
                if (i2 % 10 == 0) {
                    pw.flush();
                }
                i = i2;
            }
        }
    }

    static /* synthetic */ int lambda$static$0(SubscriptionInfo arg0, SubscriptionInfo arg1) {
        int flag = arg0.getSimSlotIndex() - arg1.getSimSlotIndex();
        if (flag == 0) {
            return arg0.getSubscriptionId() - arg1.getSubscriptionId();
        }
        return flag;
    }

    public static SubscriptionController init(Phone phone) {
        SubscriptionController subscriptionController;
        synchronized (SubscriptionController.class) {
            if (sInstance == null) {
                sInstance = new SubscriptionController(phone);
            } else {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("init() called multiple times!  sInstance = ");
                stringBuilder.append(sInstance);
                Log.wtf(str, stringBuilder.toString());
            }
            subscriptionController = sInstance;
        }
        return subscriptionController;
    }

    public static SubscriptionController init(Context c, CommandsInterface[] ci) {
        SubscriptionController subscriptionController;
        synchronized (SubscriptionController.class) {
            if (sInstance == null) {
                sInstance = new SubscriptionController(c);
            } else {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("init() called multiple times!  sInstance = ");
                stringBuilder.append(sInstance);
                Log.wtf(str, stringBuilder.toString());
            }
            subscriptionController = sInstance;
        }
        return subscriptionController;
    }

    public static SubscriptionController getInstance() {
        if (sInstance == null) {
            Log.wtf(LOG_TAG, "getInstance null");
        }
        return sInstance;
    }

    protected SubscriptionController(Context c) {
        init(c);
        migrateImsSettings();
    }

    protected void init(Context c) {
        this.mContext = c;
        this.mCM = CallManager.getInstance();
        this.mTelephonyManager = TelephonyManager.from(this.mContext);
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
        if (ServiceManager.getService("isub") == null) {
            ServiceManager.addService("isub", this);
            this.mLastISubServiceRegTime = System.currentTimeMillis();
        }
        if (HwModemCapability.isCapabilitySupport(9)) {
            getQcRilHook();
        }
        logdl("[SubscriptionController] init by Context");
    }

    private boolean isSubInfoReady() {
        return sSlotIndexToSubId.size() > 0;
    }

    private SubscriptionController(Phone phone) {
        this.mContext = phone.getContext();
        this.mCM = CallManager.getInstance();
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        if (ServiceManager.getService("isub") == null) {
            ServiceManager.addService("isub", this);
        }
        if (HwModemCapability.isCapabilitySupport(9)) {
            getQcRilHook();
        }
        migrateImsSettings();
        logdl("[SubscriptionController] init by Phone");
    }

    private void enforceModifyPhoneState(String message) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", message);
    }

    private void broadcastSimInfoContentChanged() {
        this.mContext.sendBroadcast(new Intent("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE"));
        this.mContext.sendBroadcast(new Intent("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED"));
        this.mContext.sendBroadcast(new Intent("com.huawei.intent.action.ACTION_SUBINFO_RECORD_UPDATED"));
    }

    public void notifySubscriptionInfoChanged() {
        ITelephonyRegistry tr = Stub.asInterface(ServiceManager.getService("telephony.registry"));
        try {
            logd("notifySubscriptionInfoChanged:");
            tr.notifySubscriptionInfoChanged();
        } catch (RemoteException e) {
        }
        broadcastSimInfoContentChanged();
    }

    private SubscriptionInfo getSubInfoRecord(Cursor cursor) {
        UiccAccessRule[] decodeRules;
        Cursor cursor2 = cursor;
        int id = cursor2.getInt(cursor2.getColumnIndexOrThrow(HbpcdLookup.ID));
        String iccId = cursor2.getString(cursor2.getColumnIndexOrThrow("icc_id"));
        int simSlotIndex = cursor2.getInt(cursor2.getColumnIndexOrThrow("sim_id"));
        String displayName = cursor2.getString(cursor2.getColumnIndexOrThrow("display_name"));
        String carrierName = cursor2.getString(cursor2.getColumnIndexOrThrow("carrier_name"));
        int nameSource = cursor2.getInt(cursor2.getColumnIndexOrThrow("name_source"));
        int iconTint = cursor2.getInt(cursor2.getColumnIndexOrThrow("color"));
        String number = cursor2.getString(cursor2.getColumnIndexOrThrow("number"));
        int dataRoaming = cursor2.getInt(cursor2.getColumnIndexOrThrow("data_roaming"));
        Bitmap iconBitmap = BitmapFactory.decodeResource(this.mContext.getResources(), 17302762);
        int mcc = cursor2.getInt(cursor2.getColumnIndexOrThrow("mcc"));
        int mnc = cursor2.getInt(cursor2.getColumnIndexOrThrow("mnc"));
        String cardId = cursor2.getString(cursor2.getColumnIndexOrThrow("card_id"));
        String countryIso = getSubscriptionCountryIso(id);
        boolean z = true;
        if (cursor2.getInt(cursor2.getColumnIndexOrThrow("is_embedded")) != 1) {
            z = false;
        }
        boolean isEmbedded = z;
        if (isEmbedded) {
            decodeRules = UiccAccessRule.decodeRules(cursor2.getBlob(cursor2.getColumnIndexOrThrow("access_rules")));
        } else {
            decodeRules = null;
        }
        UiccAccessRule[] accessRules = decodeRules;
        int status = cursor2.getInt(cursor2.getColumnIndexOrThrow("sub_state"));
        int nwMode = cursor2.getInt(cursor2.getColumnIndexOrThrow("network_mode"));
        String line1Number = this.mTelephonyManager.getLine1Number(simSlotIndex);
        if (!(TextUtils.isEmpty(line1Number) || line1Number.equals(number))) {
            number = line1Number;
        }
        return new SubscriptionInfo(simSlotIndex, iccId, simSlotIndex, displayName, carrierName, nameSource, iconTint, number, dataRoaming, iconBitmap, mcc, mnc, countryIso, isEmbedded, accessRules, cardId, status, nwMode);
    }

    private String getSubscriptionCountryIso(int subId) {
        int phoneId = getPhoneId(subId);
        if (phoneId < 0) {
            return "";
        }
        return this.mTelephonyManager.getSimCountryIsoForPhone(phoneId);
    }

    private List<SubscriptionInfo> getSubInfo(String selection, Object queryKey) {
        String[] selectionArgs = null;
        if (queryKey != null) {
            selectionArgs = new String[]{queryKey.toString()};
        }
        ArrayList<SubscriptionInfo> subList = null;
        Cursor cursor = this.mContext.getContentResolver().query(SubscriptionManager.CONTENT_URI, null, selection, selectionArgs, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                try {
                    SubscriptionInfo subInfo = getSubInfoRecord(cursor);
                    if (subInfo != null) {
                        if (subList == null) {
                            subList = new ArrayList();
                        }
                        subList.add(subInfo);
                    }
                } catch (Throwable th) {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        } else {
            logd("Query fail");
        }
        if (cursor != null) {
            cursor.close();
        }
        return subList;
    }

    private int getUnusedColor(String callingPackage) {
        List<SubscriptionInfo> availableSubInfos = getActiveSubscriptionInfoList(callingPackage);
        this.colorArr = this.mContext.getResources().getIntArray(17236076);
        int colorIdx = 0;
        if (availableSubInfos != null) {
            int i = 0;
            while (i < this.colorArr.length) {
                int j = 0;
                while (j < availableSubInfos.size() && this.colorArr[i] != ((SubscriptionInfo) availableSubInfos.get(j)).getIconTint()) {
                    j++;
                }
                if (j == availableSubInfos.size()) {
                    return this.colorArr[i];
                }
                i++;
            }
            colorIdx = availableSubInfos.size() % this.colorArr.length;
        }
        return this.colorArr[colorIdx];
    }

    public SubscriptionInfo getActiveSubscriptionInfo(int subId, String callingPackage) {
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, subId, callingPackage, "getActiveSubscriptionInfo")) {
            return null;
        }
        if (SubscriptionManager.isValidSubscriptionId(subId) && isSubInfoReady()) {
            long identity = Binder.clearCallingIdentity();
            try {
                List<SubscriptionInfo> subList = getActiveSubscriptionInfoList(this.mContext.getOpPackageName());
                if (subList != null) {
                    SubscriptionInfo si;
                    Iterator it = subList.iterator();
                    while (true) {
                        si = it.hasNext();
                        if (si != null) {
                            si = (SubscriptionInfo) it.next();
                            if (si.getSimSlotIndex() == subId) {
                                break;
                            }
                        }
                    }
                    return si;
                }
                logd("[getActiveSubInfoForSubscriber] subInfo=null");
                Binder.restoreCallingIdentity(identity);
                return null;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[getSubInfoUsingSubIdx]- invalid subId or not ready = ");
            stringBuilder.append(subId);
            logd(stringBuilder.toString());
            return null;
        }
    }

    public SubscriptionInfo getActiveSubscriptionInfoForIccId(String iccId, String callingPackage) {
        SubscriptionInfo si = getActiveSubscriptionInfoForIccIdInternal(iccId);
        if (TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, si != null ? si.getSubscriptionId() : -1, callingPackage, "getActiveSubscriptionInfoForIccId")) {
            return si;
        }
        return null;
    }

    /* JADX WARNING: Missing block: B:12:0x002e, code skipped:
            r0 = new java.lang.StringBuilder();
            r0.append("[getActiveSubInfoUsingIccId]+ iccId=");
            r0.append(android.telephony.SubscriptionInfo.givePrintableIccid(r8));
            r0.append(" subInfo=");
            r0.append(r5);
            logd(r0.toString());
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private SubscriptionInfo getActiveSubscriptionInfoForIccIdInternal(String iccId) {
        if (iccId == null) {
            return null;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            SubscriptionInfo si;
            List<SubscriptionInfo> subList = getActiveSubscriptionInfoList(this.mContext.getOpPackageName());
            if (subList != null) {
                Iterator it = subList.iterator();
                while (true) {
                    si = it.hasNext();
                    if (si != null) {
                        si = (SubscriptionInfo) it.next();
                        if (iccId.equals(si.getIccId())) {
                            break;
                        }
                    }
                }
                return si;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[getActiveSubInfoUsingIccId]+ iccId=");
            stringBuilder.append(SubscriptionInfo.givePrintableIccid(iccId));
            stringBuilder.append(" subList=");
            stringBuilder.append(subList);
            si = " subInfo=null";
            stringBuilder.append(si);
            logd(stringBuilder.toString());
            Binder.restoreCallingIdentity(identity);
            return null;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* JADX WARNING: Missing block: B:19:0x005c, code skipped:
            r2 = new java.lang.StringBuilder();
            r2.append("[getActiveSubscriptionInfoForSimSlotIndex]+ slotIndex=");
            r2.append(r10);
            r2.append(" subId=*");
            logd(r2.toString());
     */
    /* JADX WARNING: Missing block: B:23:?, code skipped:
            r6 = new java.lang.StringBuilder();
            r6.append("[getActiveSubscriptionInfoForSimSlotIndex]+ slotIndex=");
            r6.append(r10);
            r6.append(" subId=null");
            logd(r6.toString());
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public SubscriptionInfo getActiveSubscriptionInfoForSimSlotIndex(int slotIndex, String callingPackage) {
        SubscriptionInfo si;
        IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
        if (manager != null) {
            manager.sendBehavior(BehaviorId.TELEPHONY_GETACTIVESUBSCRIPTIONINFOFORSIMSLOTINDEX);
        }
        Phone phone = PhoneFactory.getPhone(slotIndex);
        if (phone == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[getActiveSubscriptionInfoForSimSlotIndex] no phone, slotIndex=");
            stringBuilder.append(slotIndex);
            loge(stringBuilder.toString());
            return null;
        } else if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, phone.getSubId(), callingPackage, "getActiveSubscriptionInfoForSimSlotIndex")) {
            return null;
        } else {
            long identity = Binder.clearCallingIdentity();
            try {
                List<SubscriptionInfo> subList = getActiveSubscriptionInfoList(this.mContext.getOpPackageName());
                if (subList != null) {
                    Iterator it = subList.iterator();
                    while (true) {
                        si = it.hasNext();
                        if (si == null) {
                            break;
                        }
                        si = (SubscriptionInfo) it.next();
                        if (si.getSimSlotIndex() == slotIndex) {
                            break;
                        }
                    }
                } else {
                    logd("[getActiveSubscriptionInfoForSimSlotIndex]+ subList=null");
                }
                Binder.restoreCallingIdentity(identity);
                return null;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
        return si;
    }

    public List<SubscriptionInfo> getAllSubInfoList(String callingPackage) {
        logd("[getAllSubInfoList]+");
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, -1, callingPackage, "getAllSubInfoList")) {
            return null;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            List<SubscriptionInfo> subList = getSubInfo(null, null);
            if (subList != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[getAllSubInfoList]- ");
                stringBuilder.append(subList.size());
                stringBuilder.append(" infos return");
                logd(stringBuilder.toString());
            } else {
                logd("[getAllSubInfoList]- no info return");
            }
            Binder.restoreCallingIdentity(identity);
            return subList;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public List<SubscriptionInfo> getActiveSubscriptionInfoList(String callingPackage) {
        IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
        if (manager != null) {
            manager.sendBehavior(BehaviorId.TELEPHONY_GETACTIVESUBSCRIPTIONINFOLIST);
        }
        if (isSubInfoReady()) {
            boolean canReadAllPhoneState;
            try {
                canReadAllPhoneState = TelephonyPermissions.checkReadPhoneState(this.mContext, -1, Binder.getCallingPid(), Binder.getCallingUid(), callingPackage, "getActiveSubscriptionInfoList");
            } catch (SecurityException e) {
                canReadAllPhoneState = false;
            }
            synchronized (this.mCacheActiveSubInfoList) {
                if (canReadAllPhoneState) {
                    try {
                        ArrayList arrayList = new ArrayList(this.mCacheActiveSubInfoList);
                        return arrayList;
                    } catch (Throwable th) {
                    }
                } else {
                    List list = (List) this.mCacheActiveSubInfoList.stream().filter(new -$$Lambda$SubscriptionController$tMI7DzRlXdGT29a2mf9-vcxGNO0(this, callingPackage)).collect(Collectors.toList());
                    return list;
                }
            }
        }
        logdl("[getActiveSubInfoList] Sub Controller not ready");
        return null;
    }

    public static /* synthetic */ boolean lambda$getActiveSubscriptionInfoList$1(SubscriptionController subscriptionController, String callingPackage, SubscriptionInfo subscriptionInfo) {
        try {
            return TelephonyPermissions.checkCallingOrSelfReadPhoneState(subscriptionController.mContext, subscriptionInfo.getSubscriptionId(), callingPackage, "getActiveSubscriptionInfoList");
        } catch (SecurityException e) {
            return false;
        }
    }

    @VisibleForTesting
    public void refreshCachedActiveSubscriptionInfoList() {
        if (isSubInfoReady()) {
            synchronized (this.mCacheActiveSubInfoList) {
                this.mCacheActiveSubInfoList.clear();
                List<SubscriptionInfo> activeSubscriptionInfoList = getSubInfo("sim_id>=0", null);
                if (activeSubscriptionInfoList != null) {
                    this.mCacheActiveSubInfoList.addAll(activeSubscriptionInfoList);
                }
            }
        }
    }

    public int getActiveSubInfoCount(String callingPackage) {
        List<SubscriptionInfo> records = getActiveSubscriptionInfoList(callingPackage);
        if (records == null) {
            return 0;
        }
        return records.size();
    }

    public int getAllSubInfoCount(String callingPackage) {
        logd("[getAllSubInfoCount]+");
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, -1, callingPackage, "getAllSubInfoCount")) {
            return 0;
        }
        long identity = Binder.clearCallingIdentity();
        Cursor cursor;
        try {
            cursor = this.mContext.getContentResolver().query(SubscriptionManager.CONTENT_URI, null, null, null, null);
            if (cursor != null) {
                int count = cursor.getCount();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[getAllSubInfoCount]- ");
                stringBuilder.append(count);
                stringBuilder.append(" SUB(s) in DB");
                logd(stringBuilder.toString());
                if (cursor != null) {
                    cursor.close();
                }
                Binder.restoreCallingIdentity(identity);
                return count;
            }
            if (cursor != null) {
                cursor.close();
            }
            logd("[getAllSubInfoCount]- no SUB in DB");
            Binder.restoreCallingIdentity(identity);
            return 0;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int getActiveSubInfoCountMax() {
        return this.mTelephonyManager.getSimCount();
    }

    public List<SubscriptionInfo> getAvailableSubscriptionInfoList(String callingPackage) {
        if (TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, -1, callingPackage, "getAvailableSubscriptionInfoList")) {
            long identity = Binder.clearCallingIdentity();
            try {
                List<SubscriptionInfo> list = null;
                if (((EuiccManager) this.mContext.getSystemService("euicc")).isEnabled()) {
                    List<SubscriptionInfo> subList = getSubInfo("sim_id>=0 OR is_embedded=1", list);
                    if (subList != null) {
                        subList.sort(SUBSCRIPTION_INFO_COMPARATOR);
                    } else {
                        logdl("[getAvailableSubInfoList]- no info return");
                    }
                    Binder.restoreCallingIdentity(identity);
                    return subList;
                }
                logdl("[getAvailableSubInfoList] Embedded subscriptions are disabled");
                return list;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        } else {
            throw new SecurityException("Need READ_PHONE_STATE to call  getAvailableSubscriptionInfoList");
        }
    }

    public List<SubscriptionInfo> getAccessibleSubscriptionInfoList(String callingPackage) {
        if (((EuiccManager) this.mContext.getSystemService("euicc")).isEnabled()) {
            this.mAppOps.checkPackage(Binder.getCallingUid(), callingPackage);
            long identity = Binder.clearCallingIdentity();
            try {
                List<SubscriptionInfo> subList = getSubInfo("is_embedded=1", null);
                if (subList != null) {
                    return (List) subList.stream().filter(new -$$Lambda$SubscriptionController$3VswDVLryax7J6vjeeeQyAns1Mg(this, callingPackage)).sorted(SUBSCRIPTION_INFO_COMPARATOR).collect(Collectors.toList());
                }
                logdl("[getAccessibleSubInfoList] No info returned");
                return null;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        } else {
            logdl("[getAccessibleSubInfoList] Embedded subscriptions are disabled");
            return null;
        }
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public List<SubscriptionInfo> getSubscriptionInfoListForEmbeddedSubscriptionUpdate(String[] embeddedIccids, boolean isEuiccRemovable) {
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("(");
        whereClause.append("is_embedded");
        whereClause.append("=1");
        if (isEuiccRemovable) {
            whereClause.append(" AND ");
            whereClause.append("is_removable");
            whereClause.append("=1");
        }
        whereClause.append(") OR ");
        whereClause.append("icc_id");
        whereClause.append(" IN (");
        for (int i = 0; i < embeddedIccids.length; i++) {
            if (i > 0) {
                whereClause.append(",");
            }
            whereClause.append("\"");
            whereClause.append(embeddedIccids[i]);
            whereClause.append("\"");
        }
        whereClause.append(")");
        List<SubscriptionInfo> list = getSubInfo(whereClause.toString(), null);
        if (list == null) {
            return Collections.emptyList();
        }
        return list;
    }

    public void requestEmbeddedSubscriptionInfoListRefresh() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_EMBEDDED_SUBSCRIPTIONS", "requestEmbeddedSubscriptionInfoListRefresh");
        long token = Binder.clearCallingIdentity();
        try {
            PhoneFactory.requestEmbeddedSubscriptionInfoListRefresh(null);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void requestEmbeddedSubscriptionInfoListRefresh(Runnable callback) {
        PhoneFactory.requestEmbeddedSubscriptionInfoListRefresh(callback);
    }

    /* JADX WARNING: Removed duplicated region for block: B:92:0x021b A:{Catch:{ all -> 0x0264, all -> 0x003c }} */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x0127 A:{Catch:{ all -> 0x0134 }} */
    /* JADX WARNING: Removed duplicated region for block: B:50:0x0101 A:{Catch:{ all -> 0x013a }} */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x0167 A:{SYNTHETIC, Splitter:B:68:0x0167} */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0184 A:{SYNTHETIC, Splitter:B:73:0x0184} */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x026d A:{Catch:{ all -> 0x0264, all -> 0x003c }} */
    /* JADX WARNING: Removed duplicated region for block: B:108:0x0294  */
    /* JADX WARNING: Removed duplicated region for block: B:105:0x027a A:{Catch:{ all -> 0x0264, all -> 0x003c }} */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x0167 A:{SYNTHETIC, Splitter:B:68:0x0167} */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x0184 A:{SYNTHETIC, Splitter:B:73:0x0184} */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x026d A:{Catch:{ all -> 0x0264, all -> 0x003c }} */
    /* JADX WARNING: Removed duplicated region for block: B:105:0x027a A:{Catch:{ all -> 0x0264, all -> 0x003c }} */
    /* JADX WARNING: Removed duplicated region for block: B:108:0x0294  */
    /* JADX WARNING: Removed duplicated region for block: B:128:0x033e A:{SYNTHETIC, Splitter:B:128:0x033e} */
    /* JADX WARNING: Removed duplicated region for block: B:128:0x033e A:{SYNTHETIC, Splitter:B:128:0x033e} */
    /* JADX WARNING: Removed duplicated region for block: B:128:0x033e A:{SYNTHETIC, Splitter:B:128:0x033e} */
    /* JADX WARNING: Removed duplicated region for block: B:128:0x033e A:{SYNTHETIC, Splitter:B:128:0x033e} */
    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:2:0x0032, B:73:0x0184] */
    /* JADX WARNING: Missing block: B:97:0x0265, code skipped:
            if (r7 != false) goto L_0x0267;
     */
    /* JADX WARNING: Missing block: B:99:?, code skipped:
            r7.close();
     */
    /* JADX WARNING: Missing block: B:131:0x0342, code skipped:
            android.os.Binder.restoreCallingIdentity(r4);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int addSubInfoRecord(String iccId, int slotIndex) {
        boolean setDisplayName;
        Throwable th;
        Cursor cursor;
        boolean setDisplayName2;
        String str = iccId;
        int i = slotIndex;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[addSubInfoRecord]+ iccId:");
        stringBuilder.append(SubscriptionInfo.givePrintableIccid(iccId));
        stringBuilder.append(" slotIndex:");
        stringBuilder.append(i);
        logdl(stringBuilder.toString());
        enforceModifyPhoneState("addSubInfoRecord");
        long identity = Binder.clearCallingIdentity();
        if (str == null) {
            logdl("[addSubInfoRecord]- null iccId");
            Binder.restoreCallingIdentity(identity);
            return -1;
        }
        Uri uri;
        int i2;
        StringBuilder stringBuilder2;
        ContentResolver resolver = this.mContext.getContentResolver();
        Cursor cursor2 = resolver.query(SubscriptionManager.CONTENT_URI, new String[]{HbpcdLookup.ID, "sim_id", "name_source", "icc_id", "card_id"}, "icc_id=? OR icc_id=?", new String[]{str, IccUtils.getDecimalSubstring(iccId)}, null);
        boolean setDisplayName3 = false;
        String[] strArr;
        if (cursor2 != null) {
            try {
                if (cursor2.moveToFirst()) {
                    int subId = cursor2.getInt(0);
                    int oldSimInfoId = cursor2.getInt(1);
                    int nameSource = cursor2.getInt(2);
                    String oldIccId = cursor2.getString(3);
                    String oldCardId = cursor2.getString(4);
                    ContentValues value = new ContentValues();
                    if (i != oldSimInfoId) {
                        value.put("sim_id", Integer.valueOf(slotIndex));
                        setDisplayName = setDisplayName3;
                        try {
                            value.put("network_mode", Integer.valueOf(-1));
                        } catch (Throwable th2) {
                            th = th2;
                            cursor = cursor2;
                            setDisplayName3 = setDisplayName;
                        }
                    } else {
                        setDisplayName = setDisplayName3;
                    }
                    if (nameSource != 2) {
                        setDisplayName3 = true;
                    } else {
                        setDisplayName3 = setDisplayName;
                    }
                    if (oldIccId != null) {
                        try {
                            if (oldIccId.length() < iccId.length() && oldIccId.equals(IccUtils.getDecimalSubstring(iccId))) {
                                value.put("icc_id", str);
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            cursor = cursor2;
                            if (cursor != null) {
                            }
                            throw th;
                        }
                    }
                    try {
                        UiccCard card = UiccController.getInstance().getUiccCardForPhone(i);
                        if (card != null) {
                            String cardId = card.getCardId();
                            if (!(cardId == null || cardId.equals(oldCardId))) {
                                StringBuilder stringBuilder3;
                                StringBuilder stringBuilder4;
                                StringBuilder stringBuilder5;
                                value.put("card_id", cardId);
                                if (value.size() <= 0) {
                                    uri = SubscriptionManager.CONTENT_URI;
                                    StringBuilder stringBuilder6 = new StringBuilder();
                                    setDisplayName2 = setDisplayName3;
                                    try {
                                        stringBuilder6.append("_id=");
                                        stringBuilder6.append(Long.toString((long) subId));
                                        String stringBuilder7 = stringBuilder6.toString();
                                        strArr = null;
                                        resolver.update(uri, value, stringBuilder7, null);
                                        refreshCachedActiveSubscriptionInfoList();
                                    } catch (Throwable th4) {
                                        th = th4;
                                        cursor = cursor2;
                                        setDisplayName3 = setDisplayName2;
                                        if (cursor != null) {
                                        }
                                        throw th;
                                    }
                                }
                                setDisplayName2 = setDisplayName3;
                                int i3 = oldSimInfoId;
                                int i4 = nameSource;
                                strArr = null;
                                logdl("[addSubInfoRecord] Record already exists");
                                if (cursor2 != null) {
                                    cursor2.close();
                                }
                                setDisplayName3 = resolver.query(SubscriptionManager.CONTENT_URI, null, "sim_id=?", new String[]{String.valueOf(slotIndex)}, null);
                                if (setDisplayName3) {
                                    if (setDisplayName3.moveToFirst()) {
                                        do {
                                            int i5;
                                            i2 = setDisplayName3.getInt(setDisplayName3.getColumnIndexOrThrow(HbpcdLookup.ID));
                                            i2 = i;
                                            Integer currentSubId = (Integer) sSlotIndexToSubId.get(Integer.valueOf(slotIndex));
                                            if (currentSubId != null && currentSubId.intValue() == i2) {
                                                if (SubscriptionManager.isValidSubscriptionId(currentSubId.intValue())) {
                                                    logdl("[addSubInfoRecord] currentSubId != null && currentSubId is valid, IGNORE");
                                                    stringBuilder3 = new StringBuilder();
                                                    stringBuilder3.append("[addSubInfoRecord] hashmap(");
                                                    stringBuilder3.append(i);
                                                    stringBuilder3.append(",");
                                                    stringBuilder3.append(i2);
                                                    stringBuilder3.append(")");
                                                    logdl(stringBuilder3.toString());
                                                }
                                            }
                                            sSlotIndexToSubId.put(Integer.valueOf(slotIndex), Integer.valueOf(i2));
                                            oldSimInfoId = getActiveSubInfoCountMax();
                                            nameSource = getDefaultSubId();
                                            stringBuilder4 = new StringBuilder();
                                            stringBuilder4.append("[addSubInfoRecord] sSlotIndexToSubId.size=");
                                            stringBuilder4.append(sSlotIndexToSubId.size());
                                            stringBuilder4.append(" slotIndex=");
                                            stringBuilder4.append(i);
                                            stringBuilder4.append(" subId=");
                                            stringBuilder4.append(i2);
                                            stringBuilder4.append(" defaultSubId=");
                                            stringBuilder4.append(nameSource);
                                            stringBuilder4.append(" simCount=");
                                            stringBuilder4.append(oldSimInfoId);
                                            logdl(stringBuilder4.toString());
                                            if (SubscriptionManager.isValidSubscriptionId(nameSource)) {
                                                i5 = 1;
                                                if (oldSimInfoId == 1) {
                                                }
                                                if (oldSimInfoId == i5) {
                                                    stringBuilder5 = new StringBuilder();
                                                    stringBuilder5.append("[addSubInfoRecord] one sim set defaults to subId=");
                                                    stringBuilder5.append(i2);
                                                    logdl(stringBuilder5.toString());
                                                    setDefaultDataSubId(i2);
                                                    setDataSubId(i2);
                                                    setDefaultSmsSubId(i2);
                                                    setDefaultVoiceSubId(i2);
                                                }
                                                stringBuilder3 = new StringBuilder();
                                                stringBuilder3.append("[addSubInfoRecord] hashmap(");
                                                stringBuilder3.append(i);
                                                stringBuilder3.append(",");
                                                stringBuilder3.append(i2);
                                                stringBuilder3.append(")");
                                                logdl(stringBuilder3.toString());
                                            } else {
                                                i5 = 1;
                                            }
                                            setDefaultFallbackSubId(i2);
                                            if (oldSimInfoId == i5) {
                                            }
                                            stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("[addSubInfoRecord] hashmap(");
                                            stringBuilder3.append(i);
                                            stringBuilder3.append(",");
                                            stringBuilder3.append(i2);
                                            stringBuilder3.append(")");
                                            logdl(stringBuilder3.toString());
                                        } while (setDisplayName3.moveToNext());
                                    }
                                }
                                if (setDisplayName3) {
                                    setDisplayName3.close();
                                }
                                i2 = getSubIdUsingPhoneId(i);
                                if (SubscriptionManager.isValidSubscriptionId(i2)) {
                                    if (setDisplayName2) {
                                        String nameToSet;
                                        ContentValues value2;
                                        Uri uri2;
                                        String simCarrierName = this.mTelephonyManager.getSimOperatorName(i2);
                                        if (!TextUtils.isEmpty(simCarrierName)) {
                                            nameToSet = simCarrierName;
                                        } else if (this.mTelephonyManager.isMultiSimEnabled()) {
                                            stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("CARD ");
                                            stringBuilder3.append(Integer.toString(i + 1));
                                            nameToSet = stringBuilder3.toString();
                                        } else {
                                            nameToSet = "CARD";
                                            value2 = new ContentValues();
                                            value2.put("display_name", nameToSet);
                                            uri2 = SubscriptionManager.CONTENT_URI;
                                            stringBuilder5 = new StringBuilder();
                                            stringBuilder5.append("sim_id=");
                                            stringBuilder5.append(Long.toString((long) i2));
                                            resolver.update(uri2, value2, stringBuilder5.toString(), strArr);
                                            refreshCachedActiveSubscriptionInfoList();
                                            stringBuilder4 = new StringBuilder();
                                            stringBuilder4.append("[addSubInfoRecord] sim name = ");
                                            stringBuilder4.append(nameToSet);
                                            logdl(stringBuilder4.toString());
                                        }
                                        value2 = new ContentValues();
                                        value2.put("display_name", nameToSet);
                                        uri2 = SubscriptionManager.CONTENT_URI;
                                        stringBuilder5 = new StringBuilder();
                                        stringBuilder5.append("sim_id=");
                                        stringBuilder5.append(Long.toString((long) i2));
                                        resolver.update(uri2, value2, stringBuilder5.toString(), strArr);
                                        refreshCachedActiveSubscriptionInfoList();
                                        stringBuilder4 = new StringBuilder();
                                        stringBuilder4.append("[addSubInfoRecord] sim name = ");
                                        stringBuilder4.append(nameToSet);
                                        logdl(stringBuilder4.toString());
                                    } else if (this.mCacheActiveSubInfoList.isEmpty()) {
                                        logdl("[addSubInfoRecord] need to refresh empty cache if setDisplayName is false");
                                        refreshCachedActiveSubscriptionInfoList();
                                    }
                                    sPhones[i].updateDataConnectionTracker();
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("[addSubInfoRecord]- info size=");
                                    stringBuilder2.append(sSlotIndexToSubId.size());
                                    logdl(stringBuilder2.toString());
                                    Binder.restoreCallingIdentity(identity);
                                    return 0;
                                }
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("[addSubInfoRecord]- getSubId failed invalid subId = ");
                                stringBuilder2.append(i2);
                                logdl(stringBuilder2.toString());
                                Binder.restoreCallingIdentity(identity);
                                return -1;
                            }
                        }
                        if (value.size() <= 0) {
                        }
                        logdl("[addSubInfoRecord] Record already exists");
                        if (cursor2 != null) {
                        }
                        setDisplayName3 = resolver.query(SubscriptionManager.CONTENT_URI, null, "sim_id=?", new String[]{String.valueOf(slotIndex)}, null);
                        if (setDisplayName3) {
                        }
                        if (setDisplayName3) {
                        }
                        i2 = getSubIdUsingPhoneId(i);
                        if (SubscriptionManager.isValidSubscriptionId(i2)) {
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        setDisplayName2 = setDisplayName3;
                        cursor = cursor2;
                        if (cursor != null) {
                        }
                        throw th;
                    }
                }
                setDisplayName = setDisplayName3;
                strArr = null;
            } catch (Throwable th6) {
                th = th6;
                setDisplayName = setDisplayName3;
                cursor = cursor2;
                if (cursor != null) {
                    cursor.close();
                }
                throw th;
            }
        }
        strArr = null;
        try {
            uri = insertEmptySubInfoRecord(iccId, slotIndex);
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[addSubInfoRecord] New record created: ");
            stringBuilder2.append(uri);
            logdl(stringBuilder2.toString());
            setDisplayName2 = true;
            if (cursor2 != null) {
            }
            setDisplayName3 = resolver.query(SubscriptionManager.CONTENT_URI, null, "sim_id=?", new String[]{String.valueOf(slotIndex)}, null);
            if (setDisplayName3) {
            }
            if (setDisplayName3) {
            }
            i2 = getSubIdUsingPhoneId(i);
            if (SubscriptionManager.isValidSubscriptionId(i2)) {
            }
        } catch (Throwable th7) {
            th = th7;
            cursor = cursor2;
            if (cursor != null) {
            }
            throw th;
        }
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public Uri insertEmptySubInfoRecord(String iccId, int slotIndex) {
        ContentResolver resolver = this.mContext.getContentResolver();
        ContentValues value = new ContentValues();
        value.put("icc_id", iccId);
        value.put("color", Integer.valueOf(getUnusedColor(this.mContext.getOpPackageName())));
        value.put("sim_id", Integer.valueOf(slotIndex));
        value.put("carrier_name", "");
        UiccCard card = UiccController.getInstance().getUiccCardForPhone(slotIndex);
        if (card != null) {
            String cardId = card.getCardId();
            if (cardId != null) {
                value.put("card_id", cardId);
            } else {
                value.put("card_id", iccId);
            }
        } else {
            value.put("card_id", iccId);
        }
        Uri uri = resolver.insert(SubscriptionManager.CONTENT_URI, value);
        refreshCachedActiveSubscriptionInfoList();
        return uri;
    }

    public boolean setPlmnSpn(int slotIndex, boolean showPlmn, String plmn, boolean showSpn, String spn) {
        synchronized (this.mLock) {
            int subId = getSubIdUsingPhoneId(slotIndex);
            if (this.mContext.getPackageManager().resolveContentProvider(SubscriptionManager.CONTENT_URI.getAuthority(), 0) != null) {
                if (SubscriptionManager.isValidSubscriptionId(subId)) {
                    String carrierText = "";
                    if (showPlmn) {
                        carrierText = plmn;
                        if (showSpn && !Objects.equals(spn, plmn)) {
                            String separator = this.mContext.getString(17040305).toString();
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(carrierText);
                            stringBuilder.append(separator);
                            stringBuilder.append(spn);
                            carrierText = stringBuilder.toString();
                        }
                    } else if (showSpn) {
                        carrierText = spn;
                    }
                    setCarrierText(carrierText, subId);
                    return true;
                }
            }
            logd("[setPlmnSpn] No valid subscription to store info");
            notifySubscriptionInfoChanged();
            return false;
        }
    }

    private int setCarrierText(String text, int subId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[setCarrierText]+ text:");
        stringBuilder.append(text);
        stringBuilder.append(" subId:");
        stringBuilder.append(subId);
        logd(stringBuilder.toString());
        enforceModifyPhoneState("setCarrierText");
        long identity = Binder.clearCallingIdentity();
        try {
            ContentValues value = new ContentValues(1);
            value.put("carrier_name", text);
            int result = this.mContext.getContentResolver();
            Uri uri = SubscriptionManager.CONTENT_URI;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("sim_id=");
            stringBuilder2.append(Long.toString((long) subId));
            result = result.update(uri, value, stringBuilder2.toString(), null);
            notifySubscriptionInfoChanged();
            return result;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int setIconTint(int tint, int subId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[setIconTint]+ tint:");
        stringBuilder.append(tint);
        stringBuilder.append(" subId:");
        stringBuilder.append(subId);
        logd(stringBuilder.toString());
        enforceModifyPhoneState("setIconTint");
        long identity = Binder.clearCallingIdentity();
        try {
            validateSubId(subId);
            ContentValues value = new ContentValues(1);
            value.put("color", Integer.valueOf(tint));
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[setIconTint]- tint:");
            stringBuilder2.append(tint);
            stringBuilder2.append(" set");
            logd(stringBuilder2.toString());
            int result = this.mContext.getContentResolver();
            Uri uri = SubscriptionManager.CONTENT_URI;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("sim_id=");
            stringBuilder3.append(Long.toString((long) subId));
            result = result.update(uri, value, stringBuilder3.toString(), null);
            refreshCachedActiveSubscriptionInfoList();
            notifySubscriptionInfoChanged();
            return result;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int setDisplayName(String displayName, int subId) {
        return setDisplayNameUsingSrc(displayName, subId, -1);
    }

    public int setDisplayNameUsingSrc(String displayName, int subId, long nameSource) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[setDisplayName]+  displayName:");
        stringBuilder.append(displayName);
        stringBuilder.append(" subId:");
        stringBuilder.append(subId);
        stringBuilder.append(" nameSource:");
        stringBuilder.append(nameSource);
        logd(stringBuilder.toString());
        enforceModifyPhoneState("setDisplayNameUsingSrc");
        long identity = Binder.clearCallingIdentity();
        try {
            String nameToSet;
            StringBuilder stringBuilder2;
            validateSubId(subId);
            if (displayName == null) {
                nameToSet = this.mContext.getString(17039374);
            } else {
                nameToSet = displayName;
            }
            ContentValues value = new ContentValues(1);
            value.put("display_name", nameToSet);
            if (nameSource >= 0) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Set nameSource=");
                stringBuilder2.append(nameSource);
                logd(stringBuilder2.toString());
                value.put("name_source", Long.valueOf(nameSource));
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[setDisplayName]- mDisplayName:");
            stringBuilder2.append(nameToSet);
            stringBuilder2.append(" set");
            logd(stringBuilder2.toString());
            int result = this.mContext.getContentResolver();
            Uri uri = SubscriptionManager.CONTENT_URI;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("sim_id=");
            stringBuilder3.append(Long.toString((long) subId));
            result = result.update(uri, value, stringBuilder3.toString(), null);
            refreshCachedActiveSubscriptionInfoList();
            notifySubscriptionInfoChanged();
            return result;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int setDisplayNumber(String number, int subId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[setDisplayNumber]: subId:");
        stringBuilder.append(subId);
        logd(stringBuilder.toString());
        enforceModifyPhoneState("setDisplayNumber");
        long identity = Binder.clearCallingIdentity();
        try {
            int phoneCount;
            validateSubId(subId);
            int phoneId = getPhoneId(subId);
            if (number != null && phoneId >= 0) {
                phoneCount = this.mTelephonyManager.getPhoneCount();
                if (phoneId < phoneCount) {
                    ContentValues value = new ContentValues(1);
                    value.put("number", number);
                    int result = this.mContext.getContentResolver();
                    Uri uri = SubscriptionManager.CONTENT_URI;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("sim_id=");
                    stringBuilder2.append(Long.toString((long) subId));
                    result = result.update(uri, value, stringBuilder2.toString(), null);
                    refreshCachedActiveSubscriptionInfoList();
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("[setDisplayNumber]- update result :");
                    stringBuilder3.append(result);
                    logd(stringBuilder3.toString());
                    notifySubscriptionInfoChanged();
                    Binder.restoreCallingIdentity(identity);
                    return result;
                }
            }
            phoneCount = "[setDispalyNumber]- fail";
            logd(phoneCount);
            phoneCount = -1;
            return phoneCount;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int setDataRoaming(int roaming, int subId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[setDataRoaming]+ roaming:");
        stringBuilder.append(roaming);
        stringBuilder.append(" subId:");
        stringBuilder.append(subId);
        logd(stringBuilder.toString());
        enforceModifyPhoneState("setDataRoaming");
        long identity = Binder.clearCallingIdentity();
        try {
            validateSubId(subId);
            if (roaming < 0) {
                logd("[setDataRoaming]- fail");
                return -1;
            }
            ContentValues value = new ContentValues(1);
            value.put("data_roaming", Integer.valueOf(roaming));
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[setDataRoaming]- roaming:");
            stringBuilder2.append(roaming);
            stringBuilder2.append(" set");
            logd(stringBuilder2.toString());
            int result = this.mContext.getContentResolver();
            Uri uri = SubscriptionManager.CONTENT_URI;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("sim_id=");
            stringBuilder3.append(Long.toString((long) subId));
            result = result.update(uri, value, stringBuilder3.toString(), null);
            refreshCachedActiveSubscriptionInfoList();
            notifySubscriptionInfoChanged();
            Binder.restoreCallingIdentity(identity);
            return result;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int setMccMnc(String mccMnc, int subId) {
        int mcc = 0;
        int mnc = 0;
        try {
            mcc = Integer.parseInt(mccMnc.substring(0, 3));
            mnc = Integer.parseInt(mccMnc.substring(3));
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[setMccMnc] - couldn't parse mcc/mnc: ");
            stringBuilder.append(mccMnc);
            loge(stringBuilder.toString());
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("[setMccMnc]+ mcc/mnc:");
        stringBuilder2.append(mcc);
        stringBuilder2.append("/");
        stringBuilder2.append(mnc);
        stringBuilder2.append(" subId:");
        stringBuilder2.append(subId);
        logd(stringBuilder2.toString());
        ContentValues value = new ContentValues(2);
        value.put("mcc", Integer.valueOf(mcc));
        value.put("mnc", Integer.valueOf(mnc));
        int result = this.mContext.getContentResolver();
        Uri uri = SubscriptionManager.CONTENT_URI;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("sim_id=");
        stringBuilder3.append(Long.toString((long) subId));
        result = result.update(uri, value, stringBuilder3.toString(), null);
        refreshCachedActiveSubscriptionInfoList();
        notifySubscriptionInfoChanged();
        return result;
    }

    public int getSlotIndex(int subId) {
        if (HwTelephonyFactory.getHwUiccManager().isUsingHwSubIdDesign()) {
            return getHwSlotId(subId);
        }
        if (subId == KeepaliveStatus.INVALID_HANDLE) {
            subId = getDefaultSubId();
        }
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            logd("[getSlotIndex]- subId invalid");
            return -1;
        } else if (sSlotIndexToSubId.size() == 0) {
            logd("[getSlotIndex]- size == 0, return SIM_NOT_INSERTED instead");
            return -1;
        } else {
            for (Entry<Integer, Integer> entry : sSlotIndexToSubId.entrySet()) {
                int sim = ((Integer) entry.getKey()).intValue();
                if (subId == ((Integer) entry.getValue()).intValue()) {
                    return sim;
                }
            }
            logd("[getSlotIndex]- return fail");
            return -1;
        }
    }

    @Deprecated
    public int[] getSubId(int slotIndex) {
        if (HwTelephonyFactory.getHwUiccManager().isUsingHwSubIdDesign()) {
            return getHwSubId(slotIndex);
        }
        if (slotIndex == KeepaliveStatus.INVALID_HANDLE) {
            slotIndex = getSlotIndex(getDefaultSubId());
        }
        if (!SubscriptionManager.isValidSlotIndex(slotIndex)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[getSubId]- invalid slotIndex=");
            stringBuilder.append(slotIndex);
            logd(stringBuilder.toString());
            return null;
        } else if (sSlotIndexToSubId.size() == 0) {
            return getDummySubIds(slotIndex);
        } else {
            int slot;
            ArrayList<Integer> subIds = new ArrayList();
            for (Entry<Integer, Integer> entry : sSlotIndexToSubId.entrySet()) {
                slot = ((Integer) entry.getKey()).intValue();
                int sub = ((Integer) entry.getValue()).intValue();
                if (slotIndex == slot) {
                    subIds.add(Integer.valueOf(sub));
                }
            }
            int numSubIds = subIds.size();
            if (numSubIds > 0) {
                int[] subIdArr = new int[numSubIds];
                for (slot = 0; slot < numSubIds; slot++) {
                    subIdArr[slot] = ((Integer) subIds.get(slot)).intValue();
                }
                return subIdArr;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[getSubId]- numSubIds == 0, return DummySubIds slotIndex=");
            stringBuilder2.append(slotIndex);
            logd(stringBuilder2.toString());
            return getDummySubIds(slotIndex);
        }
    }

    public int getPhoneId(int subId) {
        if (HwTelephonyFactory.getHwUiccManager().isUsingHwSubIdDesign()) {
            return getHwPhoneId(subId);
        }
        if (subId == KeepaliveStatus.INVALID_HANDLE) {
            subId = getDefaultSubId();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[getPhoneId] asked for default subId=");
            stringBuilder.append(subId);
            logdl(stringBuilder.toString());
        }
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return -1;
        }
        int phoneId;
        StringBuilder stringBuilder2;
        if (sSlotIndexToSubId.size() == 0) {
            phoneId = mDefaultPhoneId;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[getPhoneId]- no sims, returning default phoneId=");
            stringBuilder2.append(phoneId);
            logdl(stringBuilder2.toString());
            return phoneId;
        }
        for (Entry<Integer, Integer> entry : sSlotIndexToSubId.entrySet()) {
            int sim = ((Integer) entry.getKey()).intValue();
            if (subId == ((Integer) entry.getValue()).intValue()) {
                return sim;
            }
        }
        phoneId = mDefaultPhoneId;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("[getPhoneId]- subId=");
        stringBuilder2.append(subId);
        stringBuilder2.append(" not found return default phoneId=");
        stringBuilder2.append(phoneId);
        logdl(stringBuilder2.toString());
        return phoneId;
    }

    private int[] getDummySubIds(int slotIndex) {
        int numSubs = getActiveSubInfoCountMax();
        if (numSubs <= 0) {
            return null;
        }
        int[] dummyValues = new int[numSubs];
        for (int i = 0; i < numSubs; i++) {
            dummyValues[i] = -2 - slotIndex;
        }
        return dummyValues;
    }

    public int clearSubInfo() {
        enforceModifyPhoneState("clearSubInfo");
        long identity = Binder.clearCallingIdentity();
        try {
            int size = sSlotIndexToSubId.size();
            StringBuilder stringBuilder;
            int i;
            if (size == 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("[clearSubInfo]- no simInfo size=");
                stringBuilder.append(size);
                logdl(stringBuilder.toString());
                i = 0;
                return i;
            }
            sSlotIndexToSubId.clear();
            stringBuilder = new StringBuilder();
            stringBuilder.append("[clearSubInfo]- clear size=");
            stringBuilder.append(size);
            i = stringBuilder.toString();
            logdl(i);
            Binder.restoreCallingIdentity(identity);
            return size;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void logvl(String msg) {
        logv(msg);
        this.mLocalLog.log(msg);
    }

    private void logv(String msg) {
        Rlog.v(LOG_TAG, msg);
    }

    private void logdl(String msg) {
        logd(msg);
        this.mLocalLog.log(msg);
    }

    private static void slogd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    private void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    private void logel(String msg) {
        loge(msg);
        this.mLocalLog.log(msg);
    }

    private void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }

    public int getDefaultSubId() {
        int subId;
        if (this.mContext.getResources().getBoolean(17957068)) {
            subId = getDefaultVoiceSubId();
        } else {
            subId = getDefaultDataSubId();
        }
        if (isActiveSubId(subId)) {
            return subId;
        }
        return mDefaultFallbackSubId;
    }

    public void setDefaultSmsSubId(int subId) {
        enforceModifyPhoneState("setDefaultSmsSubId");
        if (subId != KeepaliveStatus.INVALID_HANDLE) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[setDefaultSmsSubId] subId=");
            stringBuilder.append(subId);
            logdl(stringBuilder.toString());
            Global.putInt(this.mContext.getContentResolver(), "multi_sim_sms", subId);
            broadcastDefaultSmsSubIdChanged(subId);
            return;
        }
        throw new RuntimeException("setDefaultSmsSubId called with DEFAULT_SUB_ID");
    }

    private void broadcastDefaultSmsSubIdChanged(int subId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[broadcastDefaultSmsSubIdChanged] subId=");
        stringBuilder.append(subId);
        logdl(stringBuilder.toString());
        Intent intent = new Intent("android.telephony.action.DEFAULT_SMS_SUBSCRIPTION_CHANGED");
        intent.addFlags(553648128);
        intent.putExtra("subscription", subId);
        intent.putExtra("android.telephony.extra.SUBSCRIPTION_INDEX", subId);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    public int getDefaultSmsSubId() {
        return Global.getInt(this.mContext.getContentResolver(), "multi_sim_sms", -1);
    }

    public void setDefaultVoiceSubId(int subId) {
        enforceModifyPhoneState("setDefaultVoiceSubId");
        if (subId != KeepaliveStatus.INVALID_HANDLE) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[setDefaultVoiceSubId] subId=");
            stringBuilder.append(subId);
            logdl(stringBuilder.toString());
            Global.putInt(this.mContext.getContentResolver(), "multi_sim_voice_call", subId);
            broadcastDefaultVoiceSubIdChanged(subId);
            return;
        }
        throw new RuntimeException("setDefaultVoiceSubId called with DEFAULT_SUB_ID");
    }

    @VisibleForTesting(visibility = Visibility.PRIVATE)
    public void broadcastDefaultVoiceSubIdChanged(int subId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[broadcastDefaultVoiceSubIdChanged] subId=");
        stringBuilder.append(subId);
        logdl(stringBuilder.toString());
        Intent intent = new Intent("android.intent.action.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED");
        intent.addFlags(553648128);
        intent.putExtra("subscription", subId);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    public int getDefaultVoiceSubId() {
        return Global.getInt(this.mContext.getContentResolver(), "multi_sim_voice_call", -1);
    }

    public int getDefaultDataSubId() {
        return Global.getInt(this.mContext.getContentResolver(), "multi_sim_data_call", 0);
    }

    public void setDefaultDataSubId(int subId) {
        enforceModifyPhoneState("setDefaultDataSubId");
        if (subId != KeepaliveStatus.INVALID_HANDLE) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[setDefaultDataSubId] subId=");
            stringBuilder.append(subId);
            logdl(stringBuilder.toString());
            if (TelephonyManager.getDefault().getSimState(subId) == 5 && getSubState(subId) == 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("[setDefaultDataSubId] subId(");
                stringBuilder.append(subId);
                stringBuilder.append(") is ready but inactive, not set, return.");
                logd(stringBuilder.toString());
                return;
            }
            ProxyController proxyController = ProxyController.getInstance();
            int len = sPhones.length;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[setDefaultDataSubId] num phones=");
            stringBuilder2.append(len);
            stringBuilder2.append(", subId=");
            stringBuilder2.append(subId);
            logdl(stringBuilder2.toString());
            if (HwTelephonyFactory.getHwDataConnectionManager().isSlaveActive()) {
                logdl("slave in call, not allow setDefaultDataSubId");
                return;
            }
            String flexMapSupportType = SystemProperties.get("persist.radio.flexmap_type", "nw_mode");
            boolean isQcomPlat = HwModemCapability.isCapabilitySupport(true);
            if (SubscriptionManager.isValidSubscriptionId(subId) && ((!isQcomPlat || flexMapSupportType.equals("dds")) && !IS_FAST_SWITCH_SIMSLOT)) {
                RadioAccessFamily[] rafs = new RadioAccessFamily[len];
                boolean atLeastOneMatch = false;
                for (int phoneId = 0; phoneId < len; phoneId++) {
                    int raf;
                    int id = sPhones[phoneId].getSubId();
                    if (id == subId) {
                        raf = proxyController.getMaxRafSupported();
                        atLeastOneMatch = true;
                    } else {
                        raf = proxyController.getMinRafSupported();
                    }
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("[setDefaultDataSubId] phoneId=");
                    stringBuilder3.append(phoneId);
                    stringBuilder3.append(" subId=");
                    stringBuilder3.append(id);
                    stringBuilder3.append(" RAF=");
                    stringBuilder3.append(raf);
                    logdl(stringBuilder3.toString());
                    rafs[phoneId] = new RadioAccessFamily(phoneId, raf);
                }
                if (atLeastOneMatch) {
                    proxyController.setRadioCapability(rafs);
                } else {
                    logdl("[setDefaultDataSubId] no valid subId's found - not updating.");
                }
            }
            updateAllDataConnectionTrackers();
            if (4 == updateClatForMobile(subId)) {
                logd("set clat is error.");
            }
            checkNeedSetMainSlotByPid(subId, Binder.getCallingPid());
            if (!HwTelephonyFactory.getHwUiccManager().get4GSlotInSwitchProgress()) {
                Global.putInt(this.mContext.getContentResolver(), "multi_sim_data_call", subId);
                broadcastDefaultDataSubIdChanged(subId);
            }
            return;
        }
        throw new RuntimeException("setDefaultDataSubId called with DEFAULT_SUB_ID");
    }

    public void informDdsToQcril(int ddsPhoneId, int reason) {
        if (this.qcRilHook != null) {
            try {
                Method qcRilSendDDSInfo = this.qcRilHook.getClass().getMethod("qcRilSendDDSInfo", new Class[]{Integer.TYPE, Integer.TYPE, Integer.TYPE});
                if (ddsPhoneId < 0 || ddsPhoneId >= sPhones.length) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("informDdsToQcril dds phoneId is invalid = ");
                    stringBuilder.append(ddsPhoneId);
                    logd(stringBuilder.toString());
                    return;
                }
                for (int i = 0; i < sPhones.length; i++) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("informDdsToQcril rild= ");
                    stringBuilder2.append(i);
                    stringBuilder2.append(", ddsPhoneId=");
                    stringBuilder2.append(ddsPhoneId);
                    stringBuilder2.append(", reason = ");
                    stringBuilder2.append(reason);
                    logd(stringBuilder2.toString());
                    qcRilSendDDSInfo.invoke(this.qcRilHook, new Object[]{Integer.valueOf(ddsPhoneId), Integer.valueOf(reason), Integer.valueOf(i)});
                }
                return;
            } catch (NoSuchMethodException e) {
                loge("qcRilSendDDSInfo NoSuchMethodException.");
                return;
            } catch (RuntimeException e2) {
                loge("qcRilSendDDSInfo RuntimeException.");
                return;
            } catch (IllegalAccessException e3) {
                loge("qcRilSendDDSInfo IllegalAccessException.");
                return;
            } catch (InvocationTargetException e4) {
                loge("qcRilSendDDSInfo InvocationTargetException.");
                return;
            }
        }
        logd("informDdsToQcril qcRilHook is null.");
    }

    public Object getQcRilHook() {
        logd("Get QcRilHook Class");
        if (this.qcRilHook == null) {
            try {
                Object[] params = new Object[]{this.mContext};
                this.qcRilHook = new PathClassLoader("system/framework/qcrilhook.jar", ClassLoader.getSystemClassLoader()).loadClass("com.qualcomm.qcrilhook.QcRilHook").getConstructor(new Class[]{Context.class}).newInstance(params);
            } catch (ClassNotFoundException e) {
                loge("getQcRilHook ClassNotFoundException.");
            } catch (RuntimeException e2) {
                loge("getQcRilHook RuntimeException.");
            } catch (NoSuchMethodException e3) {
                loge("getQcRilHook NoSuchMethodException.");
            } catch (InstantiationException e4) {
                loge("getQcRilHook InstantiationException.");
            } catch (IllegalAccessException e5) {
                loge("getQcRilHook IllegalAccessException.");
            } catch (InvocationTargetException e6) {
                loge("getQcRilHook InvocationTargetException.");
            }
        }
        return this.qcRilHook;
    }

    private void updateAllDataConnectionTrackers() {
        int len = sPhones.length;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[updateAllDataConnectionTrackers] sPhones.length=");
        stringBuilder.append(len);
        logdl(stringBuilder.toString());
        for (int phoneId = 0; phoneId < len; phoneId++) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[updateAllDataConnectionTrackers] phoneId=");
            stringBuilder2.append(phoneId);
            logdl(stringBuilder2.toString());
            sPhones[phoneId].updateDataConnectionTracker();
        }
    }

    private void broadcastDefaultDataSubIdChanged(int subId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[broadcastDefaultDataSubIdChanged] subId=");
        stringBuilder.append(subId);
        logdl(stringBuilder.toString());
        Intent intent = new Intent("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        intent.addFlags(553648128);
        intent.putExtra("subscription", subId);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void setDefaultFallbackSubId(int subId) {
        if (subId != KeepaliveStatus.INVALID_HANDLE) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[setDefaultFallbackSubId] subId=");
            stringBuilder.append(subId);
            logdl(stringBuilder.toString());
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                int phoneId = getPhoneId(subId);
                StringBuilder stringBuilder2;
                if (phoneId < 0 || (phoneId >= this.mTelephonyManager.getPhoneCount() && this.mTelephonyManager.getSimCount() != 1)) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("[setDefaultFallbackSubId] not set invalid phoneId=");
                    stringBuilder2.append(phoneId);
                    stringBuilder2.append(" subId=");
                    stringBuilder2.append(subId);
                    logdl(stringBuilder2.toString());
                    return;
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[setDefaultFallbackSubId] set mDefaultFallbackSubId=");
                stringBuilder2.append(subId);
                logdl(stringBuilder2.toString());
                mDefaultFallbackSubId = subId;
                MccTable.updateMccMncConfiguration(this.mContext, this.mTelephonyManager.getSimOperatorNumericForPhone(phoneId), false);
                Intent intent = new Intent("android.telephony.action.DEFAULT_SUBSCRIPTION_CHANGED");
                intent.addFlags(553648128);
                SubscriptionManager.putPhoneIdAndSubIdExtra(intent, phoneId, subId);
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("[setDefaultFallbackSubId] broadcast default subId changed phoneId=");
                stringBuilder3.append(phoneId);
                stringBuilder3.append(" subId=");
                stringBuilder3.append(subId);
                logdl(stringBuilder3.toString());
                this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
                return;
            }
            return;
        }
        throw new RuntimeException("setDefaultSubId called with DEFAULT_SUB_ID");
    }

    public void clearDefaultsForInactiveSubIds() {
        enforceModifyPhoneState("clearDefaultsForInactiveSubIds");
        long identity = Binder.clearCallingIdentity();
        try {
            List<SubscriptionInfo> records = getActiveSubscriptionInfoList(this.mContext.getOpPackageName());
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[clearDefaultsForInactiveSubIds] records: ");
            stringBuilder.append(records);
            logdl(stringBuilder.toString());
            if (shouldDefaultBeCleared(records, getDefaultDataSubId())) {
                logd("[clearDefaultsForInactiveSubIds] clearing default data sub id");
                setDefaultDataSubId(-1);
            }
            if (shouldDefaultBeCleared(records, getDefaultSmsSubId())) {
                logdl("[clearDefaultsForInactiveSubIds] clearing default sms sub id");
                setDefaultSmsSubId(-1);
            }
            if (shouldDefaultBeCleared(records, getDefaultVoiceSubId())) {
                logdl("[clearDefaultsForInactiveSubIds] clearing default voice sub id");
                setDefaultVoiceSubId(-1);
            }
            Binder.restoreCallingIdentity(identity);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private boolean shouldDefaultBeCleared(List<SubscriptionInfo> records, int subId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[shouldDefaultBeCleared: subId] ");
        stringBuilder.append(subId);
        logdl(stringBuilder.toString());
        StringBuilder stringBuilder2;
        if (records == null) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[shouldDefaultBeCleared] return true no records subId=");
            stringBuilder2.append(subId);
            logdl(stringBuilder2.toString());
            return true;
        } else if (SubscriptionManager.isValidSubscriptionId(subId)) {
            for (SubscriptionInfo record : records) {
                int id = record.getSimSlotIndex();
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("[shouldDefaultBeCleared] Record.id: ");
                stringBuilder3.append(id);
                logdl(stringBuilder3.toString());
                if (id == subId) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("[shouldDefaultBeCleared] return false subId is active, subId=");
                    stringBuilder.append(subId);
                    logdl(stringBuilder.toString());
                    return false;
                }
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[shouldDefaultBeCleared] return true not active subId=");
            stringBuilder2.append(subId);
            logdl(stringBuilder2.toString());
            return true;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("[shouldDefaultBeCleared] return false only one subId, subId=");
            stringBuilder.append(subId);
            logdl(stringBuilder.toString());
            return false;
        }
    }

    public int getSubIdUsingPhoneId(int phoneId) {
        int[] subIds = getSubId(phoneId);
        if (subIds == null || subIds.length == 0) {
            return -1;
        }
        return subIds[0];
    }

    @VisibleForTesting
    public List<SubscriptionInfo> getSubInfoUsingSlotIndexPrivileged(int slotIndex, boolean needCheck) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[getSubInfoUsingSlotIndexPrivileged]+ slotIndex:");
        stringBuilder.append(slotIndex);
        logd(stringBuilder.toString());
        if (slotIndex == KeepaliveStatus.INVALID_HANDLE) {
            slotIndex = getSlotIndex(getDefaultSubId());
        }
        ArrayList<SubscriptionInfo> subList = null;
        if (!SubscriptionManager.isValidSlotIndex(slotIndex)) {
            logd("[getSubInfoUsingSlotIndexPrivileged]- invalid slotIndex");
            return null;
        } else if (!needCheck || isSubInfoReady()) {
            Cursor cursor = this.mContext.getContentResolver().query(SubscriptionManager.CONTENT_URI, null, "sim_id=?", new String[]{String.valueOf(slotIndex)}, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        do {
                            SubscriptionInfo subInfo = getSubInfoRecord(cursor);
                            if (subInfo != null) {
                                if (subList == null) {
                                    subList = new ArrayList();
                                }
                                subList.add(subInfo);
                            }
                        } while (cursor.moveToNext());
                    }
                } catch (Throwable th) {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
            if (cursor != null) {
                cursor.close();
            }
            logd("[getSubInfoUsingSlotIndex]- null info return");
            return subList;
        } else {
            logd("[getSubInfoUsingSlotIndexPrivileged]- not ready");
            return null;
        }
    }

    private void validateSubId(int subId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("validateSubId subId: ");
        stringBuilder.append(subId);
        logd(stringBuilder.toString());
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            throw new RuntimeException("Invalid sub id passed as parameter");
        } else if (subId == KeepaliveStatus.INVALID_HANDLE) {
            throw new RuntimeException("Default sub id passed as parameter");
        }
    }

    public void updatePhonesAvailability(Phone[] phones) {
        sPhones = phones;
    }

    public int[] getActiveSubIdList() {
        Set<Entry<Integer, Integer>> simInfoSet = new HashSet(sSlotIndexToSubId.entrySet());
        int[] subIdArr = new int[simInfoSet.size()];
        int i = 0;
        for (Entry<Integer, Integer> entry : simInfoSet) {
            subIdArr[i] = ((Integer) entry.getValue()).intValue();
            i++;
        }
        return subIdArr;
    }

    public boolean isActiveSubId(int subId) {
        return SubscriptionManager.isValidSubscriptionId(subId) && sSlotIndexToSubId.containsValue(Integer.valueOf(subId));
    }

    public int getSimStateForSlotIndex(int slotIndex) {
        State simState;
        String err;
        if (slotIndex < 0) {
            err = "invalid slotIndex";
            simState = State.UNKNOWN;
        } else {
            Phone phone = PhoneFactory.getPhone(slotIndex);
            if (phone == null) {
                simState = State.UNKNOWN;
                err = "phone == null";
            } else {
                IccCard icc = phone.getIccCard();
                if (icc == null) {
                    simState = State.UNKNOWN;
                    err = "icc == null";
                } else {
                    simState = icc.getState();
                    err = "";
                }
            }
        }
        return simState.ordinal();
    }

    public void setSubscriptionProperty(int subId, String propKey, String propValue) {
        enforceModifyPhoneState("setSubscriptionProperty");
        long token = Binder.clearCallingIdentity();
        ContentResolver resolver = this.mContext.getContentResolver();
        setSubscriptionPropertyIntoSettingsGlobal(subId, propKey, propValue);
        setSubscriptionPropertyIntoContentResolver(subId, propKey, propValue, resolver);
        refreshCachedActiveSubscriptionInfoList();
        Binder.restoreCallingIdentity(token);
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void setSubscriptionPropertyIntoContentResolver(int subId, String propKey, String propValue, ContentResolver resolver) {
        Object obj;
        ContentValues value = new ContentValues();
        switch (propKey.hashCode()) {
            case -2000412720:
                if (propKey.equals("enable_alert_vibrate")) {
                    obj = 6;
                    break;
                }
            case -1950380197:
                if (propKey.equals("volte_vt_enabled")) {
                    obj = 12;
                    break;
                }
            case -1555340190:
                if (propKey.equals("enable_cmas_extreme_threat_alerts")) {
                    obj = null;
                    break;
                }
            case -1433878403:
                if (propKey.equals("enable_cmas_test_alerts")) {
                    obj = 10;
                    break;
                }
            case -1390801311:
                if (propKey.equals("enable_alert_speech")) {
                    obj = 7;
                    break;
                }
            case -1218173306:
                if (propKey.equals("wfc_ims_enabled")) {
                    obj = 14;
                    break;
                }
            case -461686719:
                if (propKey.equals("enable_emergency_alerts")) {
                    obj = 3;
                    break;
                }
            case -420099376:
                if (propKey.equals("vt_ims_enabled")) {
                    obj = 13;
                    break;
                }
            case -349439993:
                if (propKey.equals("alert_sound_duration")) {
                    obj = 4;
                    break;
                }
            case 180938212:
                if (propKey.equals("wfc_ims_roaming_mode")) {
                    obj = 16;
                    break;
                }
            case 203677434:
                if (propKey.equals("enable_cmas_amber_alerts")) {
                    obj = 2;
                    break;
                }
            case 240841894:
                if (propKey.equals("show_cmas_opt_out_dialog")) {
                    obj = 11;
                    break;
                }
            case 407275608:
                if (propKey.equals("enable_cmas_severe_threat_alerts")) {
                    obj = 1;
                    break;
                }
            case 462555599:
                if (propKey.equals("alert_reminder_interval")) {
                    obj = 5;
                    break;
                }
            case 1270593452:
                if (propKey.equals("enable_etws_test_alerts")) {
                    obj = 8;
                    break;
                }
            case 1288054979:
                if (propKey.equals("enable_channel_50_alerts")) {
                    obj = 9;
                    break;
                }
            case 1334635646:
                if (propKey.equals("wfc_ims_mode")) {
                    obj = 15;
                    break;
                }
            case 1604840288:
                if (propKey.equals("wfc_ims_roaming_enabled")) {
                    obj = 17;
                    break;
                }
            default:
                obj = -1;
                break;
        }
        switch (obj) {
            case null:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
                value.put(propKey, Integer.valueOf(Integer.parseInt(propValue)));
                break;
            default:
                slogd("Invalid column name");
                break;
        }
        Uri uri = SubscriptionManager.CONTENT_URI;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sim_id=");
        stringBuilder.append(Integer.toString(subId));
        resolver.update(uri, value, stringBuilder.toString(), null);
    }

    /* JADX WARNING: Missing block: B:69:0x0117, code skipped:
            r8 = -1;
     */
    /* JADX WARNING: Missing block: B:70:0x0118, code skipped:
            switch(r8) {
                case 0: goto L_0x011e;
                case 1: goto L_0x011e;
                case 2: goto L_0x011e;
                case 3: goto L_0x011e;
                case 4: goto L_0x011e;
                case 5: goto L_0x011e;
                case 6: goto L_0x011e;
                case 7: goto L_0x011e;
                case 8: goto L_0x011e;
                case 9: goto L_0x011e;
                case 10: goto L_0x011e;
                case 11: goto L_0x011e;
                case 12: goto L_0x011e;
                case 13: goto L_0x011e;
                case 14: goto L_0x011e;
                case 15: goto L_0x011e;
                case 16: goto L_0x011e;
                case 17: goto L_0x011e;
                default: goto L_0x011b;
            };
     */
    /* JADX WARNING: Missing block: B:72:0x011e, code skipped:
            r3 = new java.lang.StringBuilder();
            r3.append(r2.getInt(0));
            r3.append("");
            r0 = r3.toString();
     */
    /* JADX WARNING: Missing block: B:73:0x0135, code skipped:
            logd("Invalid column name");
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String getSubscriptionProperty(int subId, String propKey, String callingPackage) {
        if (!TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, subId, callingPackage, "getSubscriptionProperty")) {
            return null;
        }
        String resultValue = getSubscriptionPropertyFromSettingsGlobal(subId, propKey);
        if (resultValue != null) {
            return resultValue;
        }
        ContentResolver resolver = this.mContext.getContentResolver();
        Uri uri = SubscriptionManager.CONTENT_URI;
        int i = 1;
        String[] strArr = new String[]{propKey};
        String[] strArr2 = new String[1];
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(subId);
        stringBuilder.append("");
        strArr2[0] = stringBuilder.toString();
        Cursor cursor = resolver.query(uri, strArr, "sim_id=?", strArr2, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    switch (propKey.hashCode()) {
                        case -2000412720:
                            if (propKey.equals("enable_alert_vibrate")) {
                                i = 6;
                                break;
                            }
                        case -1950380197:
                            if (propKey.equals("volte_vt_enabled")) {
                                i = 12;
                                break;
                            }
                        case -1555340190:
                            if (propKey.equals("enable_cmas_extreme_threat_alerts")) {
                                i = 0;
                                break;
                            }
                        case -1433878403:
                            if (propKey.equals("enable_cmas_test_alerts")) {
                                i = 10;
                                break;
                            }
                        case -1390801311:
                            if (propKey.equals("enable_alert_speech")) {
                                i = 7;
                                break;
                            }
                        case -1218173306:
                            if (propKey.equals("wfc_ims_enabled")) {
                                i = 14;
                                break;
                            }
                        case -461686719:
                            if (propKey.equals("enable_emergency_alerts")) {
                                i = 3;
                                break;
                            }
                        case -420099376:
                            if (propKey.equals("vt_ims_enabled")) {
                                i = 13;
                                break;
                            }
                        case -349439993:
                            if (propKey.equals("alert_sound_duration")) {
                                i = 4;
                                break;
                            }
                        case 180938212:
                            if (propKey.equals("wfc_ims_roaming_mode")) {
                                i = 16;
                                break;
                            }
                        case 203677434:
                            if (propKey.equals("enable_cmas_amber_alerts")) {
                                i = 2;
                                break;
                            }
                        case 240841894:
                            if (propKey.equals("show_cmas_opt_out_dialog")) {
                                i = 11;
                                break;
                            }
                        case 407275608:
                            if (propKey.equals("enable_cmas_severe_threat_alerts")) {
                                break;
                            }
                        case 462555599:
                            if (propKey.equals("alert_reminder_interval")) {
                                i = 5;
                                break;
                            }
                        case 1270593452:
                            if (propKey.equals("enable_etws_test_alerts")) {
                                i = 8;
                                break;
                            }
                        case 1288054979:
                            if (propKey.equals("enable_channel_50_alerts")) {
                                i = 9;
                                break;
                            }
                        case 1334635646:
                            if (propKey.equals("wfc_ims_mode")) {
                                i = 15;
                                break;
                            }
                        case 1604840288:
                            if (propKey.equals("wfc_ims_roaming_enabled")) {
                                i = 17;
                                break;
                            }
                        default:
                    }
                } else {
                    logd("Valid row not present in db");
                }
            } catch (Throwable th) {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else {
            logd("Query failed");
        }
        if (cursor != null) {
            cursor.close();
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getSubscriptionProperty Query value = ");
        stringBuilder2.append(resultValue);
        logd(stringBuilder2.toString());
        return resultValue;
    }

    private static void printStackTrace(String msg) {
        RuntimeException re = new RuntimeException();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("StackTrace - ");
        stringBuilder.append(msg);
        slogd(stringBuilder.toString());
        boolean first = true;
        for (StackTraceElement ste : re.getStackTrace()) {
            if (first) {
                first = false;
            } else {
                slogd(ste.toString());
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", "Requires DUMP");
        long token = Binder.clearCallingIdentity();
        try {
            StringBuilder stringBuilder;
            pw.println("SubscriptionController:");
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" mLastISubServiceRegTime=");
            stringBuilder2.append(this.mLastISubServiceRegTime);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" defaultSubId=");
            stringBuilder2.append(getDefaultSubId());
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" defaultDataSubId=");
            stringBuilder2.append(getDefaultDataSubId());
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" defaultVoiceSubId=");
            stringBuilder2.append(getDefaultVoiceSubId());
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" defaultSmsSubId=");
            stringBuilder2.append(getDefaultSmsSubId());
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" defaultDataPhoneId=");
            stringBuilder2.append(SubscriptionManager.from(this.mContext).getDefaultDataPhoneId());
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" defaultVoicePhoneId=");
            stringBuilder2.append(SubscriptionManager.getDefaultVoicePhoneId());
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" defaultSmsPhoneId=");
            stringBuilder2.append(SubscriptionManager.from(this.mContext).getDefaultSmsPhoneId());
            pw.println(stringBuilder2.toString());
            pw.flush();
            for (Entry<Integer, Integer> entry : sSlotIndexToSubId.entrySet()) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(" sSlotIndexToSubId[");
                stringBuilder3.append(entry.getKey());
                stringBuilder3.append("]: subId=");
                stringBuilder3.append(entry.getValue());
                pw.println(stringBuilder3.toString());
            }
            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
            List<SubscriptionInfo> sirl = getActiveSubscriptionInfoList(this.mContext.getOpPackageName());
            if (sirl != null) {
                pw.println(" ActiveSubInfoList:");
                for (SubscriptionInfo entry2 : sirl) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("  ");
                    stringBuilder.append(entry2.toString());
                    pw.println(stringBuilder.toString());
                }
            } else {
                pw.println(" ActiveSubInfoList: is null");
            }
            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
            sirl = getAllSubInfoList(this.mContext.getOpPackageName());
            if (sirl != null) {
                pw.println(" AllSubInfoList:");
                for (SubscriptionInfo entry22 : sirl) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("  ");
                    stringBuilder.append(entry22.toString());
                    pw.println(stringBuilder.toString());
                }
            } else {
                pw.println(" AllSubInfoList: is null");
            }
            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
            this.mLocalLog.dump(fd, pw, args);
            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
            pw.flush();
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void setDefaultFallbackSubIdHw(int value) {
        setDefaultFallbackSubId(value);
    }

    public void updateAllDataConnectionTrackersHw() {
        updateAllDataConnectionTrackers();
    }

    @VisibleForTesting(visibility = Visibility.PRIVATE)
    public void migrateImsSettings() {
        migrateImsSettingHelper("volte_vt_enabled", "volte_vt_enabled");
        migrateImsSettingHelper("vt_ims_enabled", "vt_ims_enabled");
        migrateImsSettingHelper("wfc_ims_enabled", "wfc_ims_enabled");
        migrateImsSettingHelper("wfc_ims_mode", "wfc_ims_mode");
        migrateImsSettingHelper("wfc_ims_roaming_mode", "wfc_ims_roaming_mode");
        migrateImsSettingHelper("wfc_ims_roaming_enabled", "wfc_ims_roaming_enabled");
    }

    private void migrateImsSettingHelper(String settingGlobal, String subscriptionProperty) {
        ContentResolver resolver = this.mContext.getContentResolver();
        int defaultSubId = getDefaultVoiceSubId();
        try {
            int prevSetting = Global.getInt(resolver, settingGlobal);
            if (prevSetting != -1) {
                setSubscriptionPropertyIntoContentResolver(defaultSubId, subscriptionProperty, Integer.toString(prevSetting), resolver);
            }
        } catch (SettingNotFoundException e) {
        }
    }
}
