package com.android.server.net;

import android.app.IActivityManager;
import android.content.Context;
import android.net.INetworkStatsService;
import android.os.Environment;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.IPowerManager;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.Xml;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.wifipro.WifiProCommonUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    private static final int HW_RULE_ALL_ACCESS = 0;
    private static final int HW_RULE_MOBILE_RESTRICT = 1;
    private static final int HW_RULE_WIFI_RESTRICT = 2;
    static final String TAG = "HwNetworkPolicy";
    private static final String TAG_APP_POLICY = "app-policy";
    private static final String TAG_NETWORK_POLICY = "network-policy";
    private static final String TAG_POLICY_LIST = "policy-list";
    private static final String TAG_UID_POLICY = "uid-policy";
    private static final int VERSION_INIT = 1;
    private static final int VERSION_LATEST = 1;
    final Context mContext;
    final AtomicFile mHwPolicyFile;
    final SparseIntArray mHwUidPolicy = new SparseIntArray();
    final SparseArray<String> mHwUidPolicyWriters = new SparseArray();
    final SparseIntArray mHwUidRules = new SparseIntArray();
    private boolean mIsRoaming = false;
    final Object mRulesLock = new Object();
    private int sIncreaseCmdCount = 0;

    private void setHwNetworkRestrict(int r8, boolean r9, boolean r10, android.os.IBinder r11) {
        /* JADX: method processing error */
/*
Error: java.util.NoSuchElementException
	at java.util.HashMap$HashIterator.nextNode(HashMap.java:1439)
	at java.util.HashMap$KeyIterator.next(HashMap.java:1461)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.applyRemove(BlockFinallyExtract.java:537)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.extractFinally(BlockFinallyExtract.java:176)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.processExceptionHandler(BlockFinallyExtract.java:81)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.visit(BlockFinallyExtract.java:52)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r7 = this;
        r6 = 0;
        r2 = android.os.Parcel.obtain();
        r4 = android.os.Parcel.obtain();
        r1 = "bandwidth";
        r5 = 5;
        r0 = new java.lang.String[r5];
        r5 = "firewall";
        r0[r6] = r5;
        if (r9 == 0) goto L_0x0051;
    L_0x0016:
        r5 = "block";
    L_0x0019:
        r6 = 1;
        r0[r6] = r5;
        if (r10 == 0) goto L_0x0055;
    L_0x001e:
        r5 = "mobile";
    L_0x0021:
        r6 = 2;
        r0[r6] = r5;
        r5 = java.lang.String.valueOf(r8);
        r6 = 3;
        r0[r6] = r5;
        r5 = r7.sIncreaseCmdCount;
        r5 = java.lang.String.valueOf(r5);
        r6 = 4;
        r0[r6] = r5;
        r2.writeString(r1);	 Catch:{ Exception -> 0x0059, all -> 0x0068 }
        r2.writeArray(r0);	 Catch:{ Exception -> 0x0059, all -> 0x0068 }
        r5 = 201; // 0xc9 float:2.82E-43 double:9.93E-322;	 Catch:{ Exception -> 0x0059, all -> 0x0068 }
        r6 = 0;	 Catch:{ Exception -> 0x0059, all -> 0x0068 }
        r11.transact(r5, r2, r4, r6);	 Catch:{ Exception -> 0x0059, all -> 0x0068 }
        r5 = r7.sIncreaseCmdCount;	 Catch:{ Exception -> 0x0059, all -> 0x0068 }
        r5 = r5 + 1;	 Catch:{ Exception -> 0x0059, all -> 0x0068 }
        r7.sIncreaseCmdCount = r5;	 Catch:{ Exception -> 0x0059, all -> 0x0068 }
        if (r2 == 0) goto L_0x004b;
    L_0x0048:
        r2.recycle();
    L_0x004b:
        if (r4 == 0) goto L_0x0050;
    L_0x004d:
        r4.recycle();
    L_0x0050:
        return;
    L_0x0051:
        r5 = "allow";
        goto L_0x0019;
    L_0x0055:
        r5 = "wifi";
        goto L_0x0021;
    L_0x0059:
        r3 = move-exception;
        r3.printStackTrace();	 Catch:{ Exception -> 0x0059, all -> 0x0068 }
        if (r2 == 0) goto L_0x0062;
    L_0x005f:
        r2.recycle();
    L_0x0062:
        if (r4 == 0) goto L_0x0050;
    L_0x0064:
        r4.recycle();
        goto L_0x0050;
    L_0x0068:
        r5 = move-exception;
        if (r2 == 0) goto L_0x006e;
    L_0x006b:
        r2.recycle();
    L_0x006e:
        if (r4 == 0) goto L_0x0073;
    L_0x0070:
        r4.recycle();
    L_0x0073:
        throw r5;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.net.HwNetworkPolicyManagerService.setHwNetworkRestrict(int, boolean, boolean, android.os.IBinder):void");
    }

    public void removeHwUidPolicy(int r1, int r2, java.lang.String r3) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: com.android.server.net.HwNetworkPolicyManagerService.removeHwUidPolicy(int, int, java.lang.String):void
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 5 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.net.HwNetworkPolicyManagerService.removeHwUidPolicy(int, int, java.lang.String):void");
    }

    public HwNetworkPolicyManagerService(Context context, IActivityManager activityManager, IPowerManager powerManager, INetworkStatsService networkStats, INetworkManagementService networkManagement) {
        super(context, activityManager, networkStats, networkManagement);
        this.mContext = context;
        this.mHwPolicyFile = new AtomicFile(new File(getHwSystemDir(), "hwnetpolicy.xml"));
    }

    static File getHwSystemDir() {
        return new File(Environment.getDataDirectory(), "system");
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        Slog.i(TAG, "onTransact, code = " + code);
        if (code == 200) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_NETWORK_POLICY", TAG);
            setHwUidPolicy(data);
            return true;
        }
        if (code == HSM_NETWORKMANAGER_SERVICE_TRANSACTION_CODE) {
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
        } else if (code == WifiProCommonUtils.HTTP_REACHALBE_GOOLE) {
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
        int[] uids = new int[0];
        synchronized (this.mRulesLock) {
            for (int i = 0; i < this.mHwUidPolicy.size(); i++) {
                int uid = this.mHwUidPolicy.keyAt(i);
                if ((this.mHwUidPolicy.valueAt(i) & policy) != 0) {
                    uids = ArrayUtils.appendInt(uids, uid);
                }
            }
        }
        return uids;
    }

    void updateHwRuleForRestrictLocked(int uid) {
        int newPolicy;
        int uidPolicy = this.mHwUidPolicy.get(uid, 0);
        boolean isMobileRestrict = (uidPolicy & 1) != 0;
        boolean isWifiRestrict = (uidPolicy & 2) != 0;
        boolean isRoamingRestrict = (uidPolicy & 4) != 0;
        if (this.mIsRoaming) {
            if (isMobileRestrict || (isRoamingRestrict ^ 1) == 0) {
                newPolicy = 1;
            } else {
                newPolicy = 0;
            }
        } else if (isMobileRestrict) {
            newPolicy = 1;
        } else {
            newPolicy = 0;
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
                    setHwNetworkRestrict(uid, (newPolicy & 2) != 0, false, networkManager);
                }
            }
        }
    }

    public void writeHwPolicyLocked() {
        Slog.i(TAG, "writeHwPolicyLocked");
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = this.mHwPolicyFile.startWrite();
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(fileOutputStream, StandardCharsets.UTF_8.name());
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
            this.mHwPolicyFile.finishWrite(fileOutputStream);
        } catch (IOException e) {
            if (fileOutputStream != null) {
                this.mHwPolicyFile.failWrite(fileOutputStream);
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
        super.readPolicyAL();
        Slog.i(TAG, "readHwPolicyLocked");
        this.mHwUidPolicy.clear();
        AutoCloseable autoCloseable = null;
        try {
            autoCloseable = this.mHwPolicyFile.openRead();
            XmlPullParser in = Xml.newPullParser();
            in.setInput(autoCloseable, StandardCharsets.UTF_8.name());
            while (true) {
                int type = in.next();
                if (type == 1) {
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
            Slog.e(TAG, "problem reading hw network policy" + e2);
        } catch (XmlPullParserException e3) {
            Slog.e(TAG, "problem reading hw network policy" + e3);
        } finally {
            IoUtils.closeQuietly(autoCloseable);
        }
    }

    public void forceUpdatePolicyLocked(Parcel data) {
        boolean z = true;
        synchronized (this.mRulesLock) {
            if (data.readInt() != 1) {
                z = false;
            }
            this.mIsRoaming = z;
            for (int i = 0; i < this.mHwUidPolicy.size(); i++) {
                updateHwRuleForRestrictLocked(this.mHwUidPolicy.keyAt(i));
            }
        }
    }
}
