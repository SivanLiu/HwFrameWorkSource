package com.android.server.net;

import android.app.IActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.Xml;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.rms.iaware.dev.SceneInfo;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class HwNetworkPolicyManagerService extends NetworkPolicyManagerService {
    private static final String ATTR_OPPACKAGENAME = "packagename";
    private static final String ATTR_POLICY = "policy";
    private static final String ATTR_UID = "uid";
    private static final String ATTR_VERSION = "version";
    private static final int HSM_NETWORKMANAGER_SERVICE_TRANSACTION_CODE = 201;
    private static final String HW_CONNECTIVITY_ACTION = "huawei.net.conn.HW_CONNECTIVITY_CHANGE";
    private static final int HW_RULE_ALL_ACCESS = 0;
    private static final int HW_RULE_MOBILE_RESTRICT = 1;
    private static final int HW_RULE_WIFI_RESTRICT = 2;
    private static final boolean HW_SIM_ACTIVATION = SystemProperties.getBoolean("ro.config.hw_sim_activation", false);
    static final String TAG = "HwNetworkPolicy";
    private static final String TAG_APP_POLICY = "app-policy";
    private static final String TAG_NETWORK_POLICY = "network-policy";
    private static final String TAG_POLICY_LIST = "policy-list";
    private static final String TAG_UID_POLICY = "uid-policy";
    private static final String VERIZON_ICCID_PREFIX = "891480";
    private static final int VERSION_INIT = 1;
    private static final int VERSION_LATEST = 1;
    final Context mContext;
    final AtomicFile mHwPolicyFile;
    final SparseIntArray mHwUidPolicy = new SparseIntArray();
    final SparseArray<String> mHwUidPolicyWriters = new SparseArray();
    final SparseIntArray mHwUidRules = new SparseIntArray();
    private boolean mIsRoaming = false;
    final Object mRulesLock = new Object();
    private BroadcastReceiver netReceiver = new IntenterBoradCastReceiver();
    private int sIncreaseCmdCount = 0;

    private class IntenterBoradCastReceiver extends BroadcastReceiver {
        private IntenterBoradCastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (HwNetworkPolicyManagerService.this.isMatchedOperator()) {
                String action = intent.getAction();
                String str = HwNetworkPolicyManagerService.TAG_NETWORK_POLICY;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onReceive ");
                stringBuilder.append(action);
                Log.d(str, stringBuilder.toString());
                if (HwNetworkPolicyManagerService.HW_CONNECTIVITY_ACTION.equals(action)) {
                    HwNetworkPolicyManagerService.this.updateInterfaceWhiteList(context, intent.getStringExtra("intfName"));
                }
                if ("android.intent.action.AIRPLANE_MODE".equals(action)) {
                    HwNetworkPolicyManagerService.this.updateInterfaceWhiteList(context, null);
                }
            }
        }
    }

    public HwNetworkPolicyManagerService(Context context, IActivityManager activityManager, INetworkManagementService networkManagement) {
        super(context, activityManager, networkManagement);
        this.mContext = context;
        this.mHwPolicyFile = new AtomicFile(new File(getHwSystemDir(), "hwnetpolicy.xml"));
        initRegisterReceiver();
    }

    static File getHwSystemDir() {
        return new File(Environment.getDataDirectory(), "system");
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onTransact, code = ");
        stringBuilder.append(code);
        Slog.i(str, stringBuilder.toString());
        if (code == 200) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
            setHwUidPolicy(data);
            return true;
        }
        if (code == 201) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
            addHwUidPolicy(data);
        } else if (code == 202) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
            removeHwUidPolicy(data);
            return true;
        } else if (code == 203) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
            getHwUidPolicy(data, reply);
            return true;
        } else if (code == 204) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
            getHwUidsWithPolicy(data, reply);
            return true;
        } else if (code == 205) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
            forceUpdatePolicyLocked(data);
            return true;
        }
        return super.onTransact(code, data, reply, flags);
    }

    public void setHwUidPolicy(Parcel data) {
        setHwUidPolicy(data.readInt(), data.readInt(), data.readString());
    }

    public void setHwUidPolicy(int uid, int policy, String opPackageName) {
        synchronized (this.mRulesLock) {
            if (this.mHwUidPolicy.get(uid, 0) != policy) {
                setHwUidPolicyUncheckedLocked(uid, policy, opPackageName, true);
            }
        }
    }

    public void setHwUidPolicyUncheckedLocked(int uid, int policy, String opPackageName, boolean persist) {
        this.mHwUidPolicy.put(uid, policy);
        this.mHwUidPolicyWriters.put(uid, opPackageName);
        updateHwRuleForRestrictLocked(uid);
        if (persist) {
            writeHwPolicyLocked();
        }
    }

    public void addHwUidPolicy(Parcel data) {
        addHwUidPolicy(data.readInt(), data.readInt(), data.readString());
    }

    public void addHwUidPolicy(int uid, int policy, String opPackageName) {
        synchronized (this.mRulesLock) {
            int oldPolicy = this.mHwUidPolicy.get(uid, 0);
            policy |= oldPolicy;
            if (oldPolicy != policy) {
                setHwUidPolicyUncheckedLocked(uid, policy, opPackageName, true);
            }
        }
    }

    public void removeHwUidPolicy(Parcel data) {
        removeHwUidPolicy(data.readInt(), data.readInt(), data.readString());
    }

    public void removeHwUidPolicy(int uid, int policy, String opPackageName) {
        synchronized (this.mRulesLock) {
            int oldPolicy = this.mHwUidPolicy.get(uid, 0);
            policy = oldPolicy & (~policy);
            if (oldPolicy != policy) {
                setHwUidPolicyUncheckedLocked(uid, policy, opPackageName, true);
            }
        }
    }

    public void getHwUidPolicy(Parcel data, Parcel reply) {
        reply.writeInt(getHwUidPolicy(data.readInt()));
    }

    public int getHwUidPolicy(int uid) {
        int i;
        synchronized (this.mRulesLock) {
            i = this.mHwUidPolicy.get(uid, 0);
        }
        return i;
    }

    public void getHwUidsWithPolicy(Parcel data, Parcel reply) {
        int[] uids = getHwUidsWithPolicy(data.readInt());
        reply.writeInt(uids.length);
        reply.writeIntArray(uids);
    }

    public int[] getHwUidsWithPolicy(int policy) {
        int i = 0;
        int[] uids = new int[0];
        synchronized (this.mRulesLock) {
            while (i < this.mHwUidPolicy.size()) {
                int uid = this.mHwUidPolicy.keyAt(i);
                if ((this.mHwUidPolicy.valueAt(i) & policy) != 0) {
                    uids = ArrayUtils.appendInt(uids, uid);
                }
                i++;
            }
        }
        return uids;
    }

    void updateHwRuleForRestrictLocked(int uid) {
        int newPolicy;
        int uidPolicy = this.mHwUidPolicy.get(uid, 0);
        boolean ruleRestrict = true;
        boolean isMobileRestrict = (uidPolicy & 1) != 0;
        boolean isWifiRestrict = (uidPolicy & 2) != 0;
        boolean isRoamingRestrict = (uidPolicy & 4) != 0;
        if (this.mIsRoaming) {
            if (isMobileRestrict || isRoamingRestrict) {
                newPolicy = 0 | 1;
            } else {
                newPolicy = 0 & -2;
            }
        } else if (isMobileRestrict) {
            newPolicy = 0 | 1;
        } else {
            newPolicy = 0 & -2;
        }
        if (isWifiRestrict) {
            newPolicy |= 2;
        } else {
            newPolicy &= -3;
        }
        int oldPolicy = this.mHwUidRules.get(uid, 0);
        if (newPolicy != oldPolicy) {
            if (newPolicy == 0) {
                this.mHwUidRules.delete(uid);
            } else {
                this.mHwUidRules.put(uid, newPolicy);
            }
            IBinder networkManager = ServiceManager.getService("network_management");
            if (networkManager != null) {
                if ((newPolicy & 1) != (oldPolicy & 1)) {
                    setHwNetworkRestrict(uid, (newPolicy & 1) != 0, true, networkManager);
                }
                if ((newPolicy & 2) != (oldPolicy & 2)) {
                    if ((newPolicy & 2) == 0) {
                        ruleRestrict = false;
                    }
                    setHwNetworkRestrict(uid, ruleRestrict, false, networkManager);
                }
            }
        }
    }

    private void setHwNetworkRestrict(int uid, boolean isRestrict, boolean isMobileNetwork, IBinder networkManager) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        String cmd = "bandwidth";
        String[] args = new String[5];
        args[0] = "firewall";
        args[1] = isRestrict ? "block" : SceneInfo.ITEM_RULE_ALLOW;
        args[2] = isMobileNetwork ? "mobile" : "wifi";
        args[3] = String.valueOf(uid);
        args[4] = String.valueOf(this.sIncreaseCmdCount);
        try {
            data.writeString(cmd);
            data.writeArray(args);
            networkManager.transact(201, data, reply, 0);
            this.sIncreaseCmdCount++;
            if (data != null) {
                data.recycle();
            }
            if (reply == null) {
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (data != null) {
                data.recycle();
            }
            if (reply == null) {
                return;
            }
        } catch (Throwable th) {
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.recycle();
            }
            throw th;
        }
        reply.recycle();
    }

    public void writeHwPolicyLocked() {
        Slog.i(TAG, "writeHwPolicyLocked");
        FileOutputStream fos = null;
        try {
            fos = this.mHwPolicyFile.startWrite();
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(fos, StandardCharsets.UTF_8.name());
            out.startDocument(null, Boolean.valueOf(true));
            out.startTag(null, TAG_POLICY_LIST);
            XmlUtils.writeIntAttribute(out, ATTR_VERSION, 1);
            for (int i = 0; i < this.mHwUidPolicy.size(); i++) {
                int uid = this.mHwUidPolicy.keyAt(i);
                int policy = this.mHwUidPolicy.valueAt(i);
                String opPackageName = (String) this.mHwUidPolicyWriters.get(uid);
                if (policy != 0) {
                    out.startTag(null, TAG_UID_POLICY);
                    XmlUtils.writeIntAttribute(out, "uid", uid);
                    XmlUtils.writeIntAttribute(out, ATTR_POLICY, policy);
                    XmlUtils.writeStringAttribute(out, ATTR_OPPACKAGENAME, opPackageName);
                    out.endTag(null, TAG_UID_POLICY);
                }
            }
            out.endTag(null, TAG_POLICY_LIST);
            out.endDocument();
            this.mHwPolicyFile.finishWrite(fos);
        } catch (IOException e) {
            if (fos != null) {
                this.mHwPolicyFile.failWrite(fos);
            }
        }
    }

    public void factoryReset(String subscriber) {
        super.factoryReset(subscriber);
        for (int i = 0; i < this.mHwUidPolicy.size(); i++) {
            setUidPolicy(this.mHwUidPolicy.keyAt(i), 0);
        }
    }

    protected void readPolicyAL() {
        String str;
        StringBuilder stringBuilder;
        super.readPolicyAL();
        Slog.i(TAG, "readHwPolicyLocked");
        this.mHwUidPolicy.clear();
        FileInputStream fis = null;
        try {
            fis = this.mHwPolicyFile.openRead();
            XmlPullParser in = Xml.newPullParser();
            in.setInput(fis, StandardCharsets.UTF_8.name());
            int version = 1;
            while (true) {
                int next = in.next();
                int type = next;
                if (next == 1) {
                    break;
                }
                String tag = in.getName();
                if (type == 2 && TAG_UID_POLICY.equals(tag)) {
                    setHwUidPolicyUncheckedLocked(XmlUtils.readIntAttribute(in, "uid"), XmlUtils.readIntAttribute(in, ATTR_POLICY), XmlUtils.readStringAttribute(in, ATTR_OPPACKAGENAME), false);
                }
            }
        } catch (FileNotFoundException e) {
            Slog.e(TAG, "problem reading hw network policy, file not found");
        } catch (IOException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("problem reading hw network policy");
            stringBuilder.append(e2);
            Slog.e(str, stringBuilder.toString());
        } catch (XmlPullParserException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("problem reading hw network policy");
            stringBuilder.append(e3);
            Slog.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            IoUtils.closeQuietly(null);
        }
        IoUtils.closeQuietly(fis);
    }

    public void forceUpdatePolicyLocked(Parcel data) {
        synchronized (this.mRulesLock) {
            int i = 0;
            boolean z = true;
            if (data.readInt() != 1) {
                z = false;
            }
            this.mIsRoaming = z;
            while (true) {
                int i2 = i;
                if (i2 < this.mHwUidPolicy.size()) {
                    updateHwRuleForRestrictLocked(this.mHwUidPolicy.keyAt(i2));
                    i = i2 + 1;
                }
            }
        }
    }

    private void initRegisterReceiver() {
        String str = TAG_NETWORK_POLICY;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("initRegisterReceiver ");
        stringBuilder.append(this.mContext);
        Log.i(str, stringBuilder.toString());
        if (this.mContext != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(HW_CONNECTIVITY_ACTION);
            filter.addAction("android.intent.action.AIRPLANE_MODE");
            this.mContext.registerReceiver(this.netReceiver, filter);
        }
    }

    private void updateInterfaceWhiteList(Context context, String itfName) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService("connectivity");
        Set<String> need2UpdateIntfList = new HashSet();
        Set<String> allIntfList = new HashSet();
        Network[] networks = connManager.getAllNetworks();
        for (int i = 0; i < networks.length; i++) {
            NetworkCapabilities networkCapabilities = connManager.getNetworkCapabilities(networks[i]);
            LinkProperties linkProperties = connManager.getLinkProperties(networks[i]);
            NetworkInfo networkInfo = connManager.getNetworkInfo(networks[i]);
            if (networkInfo != null && networkInfo.isConnected()) {
                String intfName = linkProperties.getInterfaceName();
                allIntfList.add(intfName);
                if (networkCapabilities.hasCapability(11)) {
                    need2UpdateIntfList.add(intfName);
                }
            }
        }
        if (itfName != null) {
            allIntfList.add(itfName);
        }
        String str = TAG_NETWORK_POLICY;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("allIntfList ");
        stringBuilder.append(allIntfList);
        Log.d(str, stringBuilder.toString());
        str = TAG_NETWORK_POLICY;
        stringBuilder = new StringBuilder();
        stringBuilder.append("need2UpdateIntfList ");
        stringBuilder.append(need2UpdateIntfList);
        Log.d(str, stringBuilder.toString());
        for (String intfName2 : allIntfList) {
            setMeteredInterface(intfName2, true);
        }
        for (String intfName22 : need2UpdateIntfList) {
            setMeteredInterface(intfName22, false);
        }
    }

    /*  JADX ERROR: NullPointerException in pass: ProcessVariables
        java.lang.NullPointerException
        	at jadx.core.dex.visitors.regions.ProcessVariables.addToUsageMap(ProcessVariables.java:278)
        	at jadx.core.dex.visitors.regions.ProcessVariables.access$000(ProcessVariables.java:31)
        	at jadx.core.dex.visitors.regions.ProcessVariables$CollectUsageRegionVisitor.processInsn(ProcessVariables.java:163)
        	at jadx.core.dex.visitors.regions.ProcessVariables$CollectUsageRegionVisitor.processBlockTraced(ProcessVariables.java:129)
        	at jadx.core.dex.visitors.regions.TracedRegionVisitor.processBlock(TracedRegionVisitor.java:23)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:53)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at java.util.Collections$UnmodifiableCollection.forEach(Collections.java:1080)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at java.util.Collections$UnmodifiableCollection.forEach(Collections.java:1080)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at java.util.Collections$UnmodifiableCollection.forEach(Collections.java:1080)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverse(DepthRegionTraversal.java:18)
        	at jadx.core.dex.visitors.regions.ProcessVariables.visit(ProcessVariables.java:183)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    private void setMeteredInterface(java.lang.String r10, boolean r11) {
        /*
        r9 = this;
        if (r10 != 0) goto L_0x0003;
    L_0x0002:
        return;
    L_0x0003:
        r0 = "network_management";
        r0 = android.os.ServiceManager.getService(r0);
        if (r0 != 0) goto L_0x0014;
    L_0x000b:
        r1 = "network-policy";
        r2 = "setIfWhitelist networkManager is null";
        android.util.Log.e(r1, r2);
        return;
    L_0x0014:
        r1 = android.os.Parcel.obtain();
        r2 = android.os.Parcel.obtain();
        r3 = "bandwidth";
        r4 = 2;
        r4 = new java.lang.String[r4];
        if (r11 != 0) goto L_0x0026;
    L_0x0023:
        r5 = "enable_ifwhitelist";
        goto L_0x0028;
    L_0x0026:
        r5 = "disable_ifwhitelist";
    L_0x0028:
        r6 = 0;
        r4[r6] = r5;
        r5 = 1;
        r4[r5] = r10;
        r1.writeString(r3);	 Catch:{ Exception -> 0x005e }
        r1.writeArray(r4);	 Catch:{ Exception -> 0x005e }
        r7 = 201; // 0xc9 float:2.82E-43 double:9.93E-322;	 Catch:{ Exception -> 0x005e }
        r0.transact(r7, r1, r2, r6);	 Catch:{ Exception -> 0x005e }
        r6 = r9.sIncreaseCmdCount;	 Catch:{ Exception -> 0x005e }
        r6 = r6 + r5;	 Catch:{ Exception -> 0x005e }
        r9.sIncreaseCmdCount = r6;	 Catch:{ Exception -> 0x005e }
        r5 = "network-policy";	 Catch:{ Exception -> 0x005e }
        r6 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x005e }
        r6.<init>();	 Catch:{ Exception -> 0x005e }
        r7 = "";	 Catch:{ Exception -> 0x005e }
        r6.append(r7);	 Catch:{ Exception -> 0x005e }
        r6.append(r2);	 Catch:{ Exception -> 0x005e }
        r6 = r6.toString();	 Catch:{ Exception -> 0x005e }
        android.util.Log.e(r5, r6);	 Catch:{ Exception -> 0x005e }
        if (r1 == 0) goto L_0x0059;
    L_0x0056:
        r1.recycle();
    L_0x0059:
        if (r2 == 0) goto L_0x0080;
    L_0x005b:
        goto L_0x007d;
    L_0x005c:
        r5 = move-exception;
        goto L_0x0081;
    L_0x005e:
        r5 = move-exception;
        r6 = "HwNetworkPolicy";	 Catch:{ all -> 0x005c }
        r7 = new java.lang.StringBuilder;	 Catch:{ all -> 0x005c }
        r7.<init>();	 Catch:{ all -> 0x005c }
        r8 = "setIfWhitelist-->";	 Catch:{ all -> 0x005c }
        r7.append(r8);	 Catch:{ all -> 0x005c }
        r7.append(r5);	 Catch:{ all -> 0x005c }
        r7 = r7.toString();	 Catch:{ all -> 0x005c }
        android.util.Slog.e(r6, r7);	 Catch:{ all -> 0x005c }
        if (r1 == 0) goto L_0x007b;
    L_0x0078:
        r1.recycle();
    L_0x007b:
        if (r2 == 0) goto L_0x0080;
    L_0x007d:
        r2.recycle();
    L_0x0080:
        return;
    L_0x0081:
        if (r1 == 0) goto L_0x0086;
    L_0x0083:
        r1.recycle();
    L_0x0086:
        if (r2 == 0) goto L_0x008b;
    L_0x0088:
        r2.recycle();
    L_0x008b:
        throw r5;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.net.HwNetworkPolicyManagerService.setMeteredInterface(java.lang.String, boolean):void");
    }

    private boolean isMatchedOperator() {
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
        String iccid = new StringBuilder();
        iccid.append("");
        iccid.append(tm.getSimSerialNumber());
        iccid = iccid.toString();
        if (HW_SIM_ACTIVATION && iccid.startsWith(VERIZON_ICCID_PREFIX)) {
            return true;
        }
        return false;
    }
}
