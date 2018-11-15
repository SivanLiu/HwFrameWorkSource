package com.android.server.connectivity;

import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.net.ConnectivityManager;
import android.net.INetworkManagementEventObserver;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.LocalSocketAddress.Namespace;
import android.net.Network;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkMisc;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.UidRange;
import android.net.Uri;
import android.net.util.NetworkConstants;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemService;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Secure;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnInfo;
import com.android.internal.net.VpnProfile;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.ArrayUtils;
import com.android.server.net.BaseNetworkObserver;
import com.android.server.pm.Settings;
import com.android.server.usb.UsbAudioDevice;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import libcore.io.IoUtils;

public class Vpn {
    private static final boolean LOGD = true;
    private static final int MAX_ROUTES_TO_EVALUATE = 150;
    private static final long MOST_IPV4_ADDRESSES_COUNT = 3650722201L;
    private static final BigInteger MOST_IPV6_ADDRESSES_COUNT = BigInteger.ONE.shiftLeft(128).multiply(BigInteger.valueOf(85)).divide(BigInteger.valueOf(100));
    private static final String NETWORKTYPE = "VPN";
    private static final String TAG = "Vpn";
    private static final long VPN_LAUNCH_IDLE_WHITELIST_DURATION_MS = 60000;
    private boolean mAlwaysOn;
    @GuardedBy("this")
    private Set<UidRange> mBlockedUsers;
    @VisibleForTesting
    protected VpnConfig mConfig;
    private Connection mConnection;
    private Context mContext;
    private volatile boolean mEnableTeardown;
    private String mInterface;
    private boolean mIsPackageIntentReceiverRegistered;
    private LegacyVpnRunner mLegacyVpnRunner;
    private boolean mLockdown;
    private final Looper mLooper;
    private final INetworkManagementService mNetd;
    @VisibleForTesting
    protected NetworkAgent mNetworkAgent;
    @VisibleForTesting
    protected final NetworkCapabilities mNetworkCapabilities;
    private NetworkInfo mNetworkInfo;
    private INetworkManagementEventObserver mObserver;
    private int mOwnerUID;
    private String mPackage;
    private final BroadcastReceiver mPackageIntentReceiver;
    private PendingIntent mStatusIntent;
    private final SystemServices mSystemServices;
    private final int mUserHandle;

    private class Connection implements ServiceConnection {
        private IBinder mService;

        private Connection() {
        }

        /* synthetic */ Connection(Vpn x0, AnonymousClass1 x1) {
            this();
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            this.mService = service;
        }

        public void onServiceDisconnected(ComponentName name) {
            this.mService = null;
        }
    }

    private class LegacyVpnRunner extends Thread {
        private static final String TAG = "LegacyVpnRunner";
        private final String[][] mArguments;
        private long mBringupStartTime = -1;
        private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (Vpn.this.mEnableTeardown && intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE") && intent.getIntExtra("networkType", -1) == LegacyVpnRunner.this.mOuterConnection.get()) {
                    NetworkInfo info = (NetworkInfo) intent.getExtra("networkInfo");
                    if (!(info == null || info.isConnectedOrConnecting())) {
                        try {
                            Vpn.this.mObserver.interfaceStatusChanged(LegacyVpnRunner.this.mOuterInterface, false);
                        } catch (RemoteException e) {
                        }
                    }
                }
            }
        };
        private final String[] mDaemons;
        private final AtomicInteger mOuterConnection = new AtomicInteger(-1);
        private final String mOuterInterface;
        private final LocalSocket[] mSockets;

        public LegacyVpnRunner(VpnConfig config, String[] racoon, String[] mtpd) {
            super(TAG);
            Vpn.this.mConfig = config;
            this.mDaemons = new String[]{"racoon", "mtpd"};
            r0 = new String[2][];
            int i = 0;
            r0[0] = racoon;
            r0[1] = mtpd;
            this.mArguments = r0;
            this.mSockets = new LocalSocket[this.mDaemons.length];
            this.mOuterInterface = Vpn.this.mConfig.interfaze;
            if (!TextUtils.isEmpty(this.mOuterInterface)) {
                ConnectivityManager cm = ConnectivityManager.from(Vpn.this.mContext);
                Network[] allNetworks = cm.getAllNetworks();
                int length = allNetworks.length;
                while (i < length) {
                    Network network = allNetworks[i];
                    LinkProperties lp = cm.getLinkProperties(network);
                    if (lp != null && lp.getAllInterfaceNames().contains(this.mOuterInterface)) {
                        NetworkInfo networkInfo = cm.getNetworkInfo(network);
                        if (networkInfo != null) {
                            this.mOuterConnection.set(networkInfo.getType());
                        }
                    }
                    i++;
                }
            }
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            Vpn.this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
        }

        public void check(String interfaze) {
            if (interfaze.equals(this.mOuterInterface)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Legacy VPN is going down with ");
                stringBuilder.append(interfaze);
                Log.i(str, stringBuilder.toString());
                exit();
            }
        }

        public void exit() {
            interrupt();
            Vpn.this.agentDisconnect();
            try {
                Vpn.this.mContext.unregisterReceiver(this.mBroadcastReceiver);
            } catch (IllegalArgumentException e) {
            }
        }

        public void run() {
            Log.v(TAG, "Waiting");
            synchronized (TAG) {
                Log.v(TAG, "Executing");
                int i = 0;
                String[] strArr;
                int length;
                try {
                    bringup();
                    waitForDaemonsToStop();
                    interrupted();
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                    }
                    strArr = this.mDaemons;
                    length = strArr.length;
                    while (i < length) {
                        SystemService.stop(strArr[i]);
                        i++;
                    }
                } catch (InterruptedException e2) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e3) {
                    }
                    strArr = this.mDaemons;
                    length = strArr.length;
                    while (i < length) {
                        SystemService.stop(strArr[i]);
                        i++;
                    }
                    Vpn.this.agentDisconnect();
                } finally {
                    for (LocalSocket socket : this.mSockets) {
                        IoUtils.closeQuietly(socket);
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e4) {
                    }
                    strArr = this.mDaemons;
                    length = strArr.length;
                    while (i < length) {
                        SystemService.stop(strArr[i]);
                        i++;
                    }
                }
                Vpn.this.agentDisconnect();
            }
        }

        private void checkInterruptAndDelay(boolean sleepLonger) throws InterruptedException {
            if (SystemClock.elapsedRealtime() - this.mBringupStartTime <= 60000) {
                Thread.sleep(sleepLonger ? 200 : 1);
            } else {
                Vpn.this.updateState(DetailedState.FAILED, "checkpoint");
                throw new IllegalStateException("VPN bringup took too long");
            }
        }

        /* JADX WARNING: Missing block: B:58:0x0113, code:
            if (r4.exists() != false) goto L_0x014b;
     */
        /* JADX WARNING: Missing block: B:59:0x0115, code:
            r0 = 0;
     */
        /* JADX WARNING: Missing block: B:61:0x0119, code:
            if (r0 >= r1.mDaemons.length) goto L_0x0146;
     */
        /* JADX WARNING: Missing block: B:62:0x011b, code:
            r5 = r1.mDaemons[r0];
     */
        /* JADX WARNING: Missing block: B:63:0x0123, code:
            if (r1.mArguments[r0] == null) goto L_0x0143;
     */
        /* JADX WARNING: Missing block: B:65:0x0129, code:
            if (android.os.SystemService.isRunning(r5) == false) goto L_0x012c;
     */
        /* JADX WARNING: Missing block: B:66:0x012c, code:
            r6 = new java.lang.StringBuilder();
            r6.append(r5);
            r6.append(" is dead");
     */
        /* JADX WARNING: Missing block: B:67:0x0142, code:
            throw new java.lang.IllegalStateException(r6.toString());
     */
        /* JADX WARNING: Missing block: B:68:0x0143, code:
            r0 = r0 + 1;
     */
        /* JADX WARNING: Missing block: B:69:0x0146, code:
            checkInterruptAndDelay(true);
     */
        /* JADX WARNING: Missing block: B:70:0x014b, code:
            r5 = android.os.FileUtils.readTextFile(r4, 0, null).split("\n", -1);
     */
        /* JADX WARNING: Missing block: B:71:0x015a, code:
            if (r5.length != 7) goto L_0x02c8;
     */
        /* JADX WARNING: Missing block: B:72:0x015c, code:
            r1.this$0.mConfig.interfaze = r5[0].trim();
            r1.this$0.mConfig.addLegacyAddresses(r5[1]);
     */
        /* JADX WARNING: Missing block: B:73:0x0178, code:
            if (r1.this$0.mConfig.routes == null) goto L_0x0186;
     */
        /* JADX WARNING: Missing block: B:75:0x0184, code:
            if (r1.this$0.mConfig.routes.isEmpty() == false) goto L_0x0190;
     */
        /* JADX WARNING: Missing block: B:76:0x0186, code:
            r1.this$0.mConfig.addLegacyRoutes(r5[2]);
     */
        /* JADX WARNING: Missing block: B:78:0x0196, code:
            if (r1.this$0.mConfig.dnsServers == null) goto L_0x01a4;
     */
        /* JADX WARNING: Missing block: B:80:0x01a2, code:
            if (r1.this$0.mConfig.dnsServers.size() != 0) goto L_0x01c1;
     */
        /* JADX WARNING: Missing block: B:81:0x01a4, code:
            r0 = r5[3].trim();
     */
        /* JADX WARNING: Missing block: B:82:0x01af, code:
            if (r0.isEmpty() != false) goto L_0x01c1;
     */
        /* JADX WARNING: Missing block: B:83:0x01b1, code:
            r1.this$0.mConfig.dnsServers = java.util.Arrays.asList(r0.split(" "));
     */
        /* JADX WARNING: Missing block: B:85:0x01c7, code:
            if (r1.this$0.mConfig.searchDomains == null) goto L_0x01d5;
     */
        /* JADX WARNING: Missing block: B:87:0x01d3, code:
            if (r1.this$0.mConfig.searchDomains.size() != 0) goto L_0x01f2;
     */
        /* JADX WARNING: Missing block: B:88:0x01d5, code:
            r0 = r5[4].trim();
     */
        /* JADX WARNING: Missing block: B:89:0x01e0, code:
            if (r0.isEmpty() != false) goto L_0x01f2;
     */
        /* JADX WARNING: Missing block: B:90:0x01e2, code:
            r1.this$0.mConfig.searchDomains = java.util.Arrays.asList(r0.split(" "));
     */
        /* JADX WARNING: Missing block: B:91:0x01f2, code:
            r6 = r5[5];
     */
        /* JADX WARNING: Missing block: B:92:0x01fa, code:
            if (r6.isEmpty() != false) goto L_0x026c;
     */
        /* JADX WARNING: Missing block: B:94:?, code:
            r0 = java.net.InetAddress.parseNumericAddress(r6);
     */
        /* JADX WARNING: Missing block: B:95:0x0204, code:
            if ((r0 instanceof java.net.Inet4Address) == false) goto L_0x021c;
     */
        /* JADX WARNING: Missing block: B:96:0x0206, code:
            r1.this$0.mConfig.routes.add(new android.net.RouteInfo(new android.net.IpPrefix(r0, 32), 9));
     */
        /* JADX WARNING: Missing block: B:98:0x021e, code:
            if ((r0 instanceof java.net.Inet6Address) == false) goto L_0x0236;
     */
        /* JADX WARNING: Missing block: B:99:0x0220, code:
            r1.this$0.mConfig.routes.add(new android.net.RouteInfo(new android.net.IpPrefix(r0, 128), 9));
     */
        /* JADX WARNING: Missing block: B:100:0x0236, code:
            r7 = TAG;
            r9 = new java.lang.StringBuilder();
            r9.append("Unknown IP address family for VPN endpoint: ");
            r9.append(r6);
            android.util.Log.e(r7, r9.toString());
     */
        /* JADX WARNING: Missing block: B:101:0x024d, code:
            r0 = move-exception;
     */
        /* JADX WARNING: Missing block: B:103:?, code:
            r7 = TAG;
            r9 = new java.lang.StringBuilder();
            r9.append("Exception constructing throw route to ");
            r9.append(r6);
            r9.append(": ");
            r9.append(r0);
            android.util.Log.e(r7, r9.toString());
     */
        /* JADX WARNING: Missing block: B:118:0x02cf, code:
            throw new java.lang.IllegalStateException("Cannot parse the state");
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void bringup() {
            boolean initFinished = false;
            try {
                boolean z;
                this.mBringupStartTime = SystemClock.elapsedRealtime();
                String[] strArr = this.mDaemons;
                int length = strArr.length;
                int i = 0;
                while (true) {
                    z = true;
                    if (i >= length) {
                        break;
                    }
                    String daemon = strArr[i];
                    while (!SystemService.isStopped(daemon)) {
                        checkInterruptAndDelay(true);
                    }
                    i++;
                }
                File state = new File("/data/misc/vpn/state");
                state.delete();
                if (state.exists()) {
                    throw new IllegalStateException("Cannot delete the state");
                }
                int restart;
                String[] arguments;
                new File("/data/misc/vpn/abort").delete();
                boolean restart2 = false;
                for (String[] arguments2 : this.mArguments) {
                    boolean z2 = restart2 || arguments2 != null;
                    restart2 = z2;
                }
                if (restart2) {
                    Vpn.this.updateState(DetailedState.CONNECTING, "execute");
                    restart = 0;
                    while (true) {
                        i = restart;
                        if (i >= this.mDaemons.length) {
                            break;
                        }
                        arguments2 = this.mArguments[i];
                        if (arguments2 != null) {
                            String daemon2 = this.mDaemons[i];
                            SystemService.start(daemon2);
                            while (!SystemService.isRunning(daemon2)) {
                                checkInterruptAndDelay(z);
                            }
                            this.mSockets[i] = new LocalSocket();
                            LocalSocketAddress address = new LocalSocketAddress(daemon2, Namespace.RESERVED);
                            while (true) {
                                LocalSocketAddress address2 = address;
                                try {
                                    this.mSockets[i].connect(address2);
                                    break;
                                } catch (Exception e) {
                                    checkInterruptAndDelay(true);
                                    address = address2;
                                }
                            }
                            this.mSockets[i].setSoTimeout(500);
                            OutputStream out = this.mSockets[i].getOutputStream();
                            restart = arguments2.length;
                            int i2 = 0;
                            while (i2 < restart) {
                                byte[] bytes = arguments2[i2].getBytes(StandardCharsets.UTF_8);
                                if (bytes.length < NetworkConstants.ARP_HWTYPE_RESERVED_HI) {
                                    out.write(bytes.length >> 8);
                                    out.write(bytes.length);
                                    out.write(bytes);
                                    checkInterruptAndDelay(false);
                                    i2++;
                                } else {
                                    throw new IllegalArgumentException("Argument is too large");
                                }
                            }
                            out.write(255);
                            out.write(255);
                            out.flush();
                            InputStream in = this.mSockets[i].getInputStream();
                            while (true) {
                                InputStream in2 = in;
                                try {
                                    if (in2.read() == -1) {
                                        break;
                                    }
                                } catch (Exception e2) {
                                }
                                checkInterruptAndDelay(true);
                                in = in2;
                            }
                        }
                        restart = i + 1;
                        z = true;
                    }
                }
                Vpn.this.agentDisconnect();
                return;
                synchronized (Vpn.this) {
                    Vpn.this.mConfig.startTime = SystemClock.elapsedRealtime();
                    checkInterruptAndDelay(false);
                    if (Vpn.this.jniCheck(Vpn.this.mConfig.interfaze) != 0) {
                        Vpn.this.mInterface = Vpn.this.mConfig.interfaze;
                        Vpn.this.prepareStatusIntent();
                        Vpn.this.agentConnect();
                        Log.i(TAG, "Connected!");
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(Vpn.this.mConfig.interfaze);
                        stringBuilder.append(" is gone");
                        throw new IllegalStateException(stringBuilder.toString());
                    }
                }
            } catch (Exception e3) {
                Log.i(TAG, "Aborting", e3);
                Vpn.this.updateState(DetailedState.FAILED, e3.getMessage());
                exit();
            }
        }

        private void waitForDaemonsToStop() throws InterruptedException {
            if (!Vpn.this.mNetworkInfo.isConnected()) {
                return;
            }
            while (true) {
                Thread.sleep(2000);
                int i = 0;
                while (i < this.mDaemons.length) {
                    if (this.mArguments[i] == null || !SystemService.isStopped(this.mDaemons[i])) {
                        i++;
                    } else {
                        return;
                    }
                }
            }
        }
    }

    @VisibleForTesting
    public static class SystemServices {
        private final Context mContext;

        public SystemServices(Context context) {
            this.mContext = context;
        }

        public PendingIntent pendingIntentGetActivityAsUser(Intent intent, int flags, UserHandle user) {
            return PendingIntent.getActivityAsUser(this.mContext, 0, intent, flags, null, user);
        }

        public void settingsSecurePutStringForUser(String key, String value, int userId) {
            Secure.putStringForUser(this.mContext.getContentResolver(), key, value, userId);
        }

        public void settingsSecurePutIntForUser(String key, int value, int userId) {
            Secure.putIntForUser(this.mContext.getContentResolver(), key, value, userId);
        }

        public String settingsSecureGetStringForUser(String key, int userId) {
            return Secure.getStringForUser(this.mContext.getContentResolver(), key, userId);
        }

        public int settingsSecureGetIntForUser(String key, int def, int userId) {
            return Secure.getIntForUser(this.mContext.getContentResolver(), key, def, userId);
        }
    }

    private native boolean jniAddAddress(String str, String str2, int i);

    private native int jniCheck(String str);

    private native int jniCreate(int i);

    private native boolean jniDelAddress(String str, String str2, int i);

    private native String jniGetName(int i);

    private native void jniReset(String str);

    private native int jniSetAddresses(String str, String str2);

    public Vpn(Looper looper, Context context, INetworkManagementService netService, int userHandle) {
        this(looper, context, netService, userHandle, new SystemServices(context));
    }

    @VisibleForTesting
    protected Vpn(Looper looper, Context context, INetworkManagementService netService, int userHandle, SystemServices systemServices) {
        this.mEnableTeardown = true;
        this.mAlwaysOn = false;
        this.mLockdown = false;
        this.mBlockedUsers = new ArraySet();
        this.mPackageIntentReceiver = new BroadcastReceiver() {
            /* JADX WARNING: Missing block: B:29:0x0090, code:
            return;
     */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void onReceive(Context context, Intent intent) {
                Uri data = intent.getData();
                String packageName = data == null ? null : data.getSchemeSpecificPart();
                if (packageName != null) {
                    synchronized (Vpn.this) {
                        if (packageName.equals(Vpn.this.getAlwaysOnPackage())) {
                            String action = intent.getAction();
                            String str = Vpn.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Received broadcast ");
                            stringBuilder.append(action);
                            stringBuilder.append(" for always-on VPN package ");
                            stringBuilder.append(packageName);
                            stringBuilder.append(" in user ");
                            stringBuilder.append(Vpn.this.mUserHandle);
                            Log.i(str, stringBuilder.toString());
                            int i = -1;
                            int hashCode = action.hashCode();
                            if (hashCode != -810471698) {
                                if (hashCode == 525384130 && action.equals("android.intent.action.PACKAGE_REMOVED")) {
                                    i = 1;
                                }
                            } else if (action.equals("android.intent.action.PACKAGE_REPLACED")) {
                                i = false;
                            }
                            switch (i) {
                                case 0:
                                    Vpn.this.startAlwaysOnVpn();
                                    break;
                                case 1:
                                    if (intent.getBooleanExtra("android.intent.extra.REPLACING", false) ^ true) {
                                        Vpn.this.setAlwaysOnPackage(null, false);
                                        break;
                                    }
                                    break;
                            }
                        }
                    }
                }
            }
        };
        this.mIsPackageIntentReceiverRegistered = false;
        this.mObserver = new BaseNetworkObserver() {
            public void interfaceStatusChanged(String interfaze, boolean up) {
                synchronized (Vpn.this) {
                    if (!up) {
                        if (Vpn.this.mLegacyVpnRunner != null) {
                            Vpn.this.mLegacyVpnRunner.check(interfaze);
                        }
                    }
                }
            }

            public void interfaceRemoved(String interfaze) {
                synchronized (Vpn.this) {
                    if (interfaze.equals(Vpn.this.mInterface) && Vpn.this.jniCheck(interfaze) == 0) {
                        Vpn.this.mStatusIntent = null;
                        Vpn.this.mNetworkCapabilities.setUids(null);
                        Vpn.this.mConfig = null;
                        Vpn.this.mInterface = null;
                        if (Vpn.this.mConnection != null) {
                            Vpn.this.mContext.unbindService(Vpn.this.mConnection);
                            Vpn.this.mConnection = null;
                            Vpn.this.agentDisconnect();
                        } else if (Vpn.this.mLegacyVpnRunner != null) {
                            Vpn.this.mLegacyVpnRunner.exit();
                            Vpn.this.mLegacyVpnRunner = null;
                        }
                    }
                }
            }
        };
        this.mContext = context;
        this.mNetd = netService;
        this.mUserHandle = userHandle;
        this.mLooper = looper;
        this.mSystemServices = systemServices;
        this.mPackage = "[Legacy VPN]";
        this.mOwnerUID = getAppUid(this.mPackage, this.mUserHandle);
        try {
            netService.registerObserver(this.mObserver);
        } catch (RemoteException e) {
            Log.wtf(TAG, "Problem registering observer", e);
        }
        this.mNetworkInfo = new NetworkInfo(17, 0, NETWORKTYPE, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        this.mNetworkCapabilities = new NetworkCapabilities();
        this.mNetworkCapabilities.addTransportType(4);
        this.mNetworkCapabilities.removeCapability(15);
        updateCapabilities();
        loadAlwaysOnPackage();
    }

    public void setEnableTeardown(boolean enableTeardown) {
        this.mEnableTeardown = enableTeardown;
    }

    @VisibleForTesting
    protected void updateState(DetailedState detailedState, String reason) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setting state=");
        stringBuilder.append(detailedState);
        stringBuilder.append(", reason=");
        stringBuilder.append(reason);
        Log.d(str, stringBuilder.toString());
        this.mNetworkInfo.setDetailedState(detailedState, reason, null);
        if (this.mNetworkAgent != null) {
            this.mNetworkAgent.sendNetworkInfo(this.mNetworkInfo);
        }
        updateAlwaysOnNotification(detailedState);
    }

    public void updateCapabilities() {
        updateCapabilities((ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class), this.mConfig != null ? this.mConfig.underlyingNetworks : null, this.mNetworkCapabilities);
        if (this.mNetworkAgent != null) {
            this.mNetworkAgent.sendNetworkCapabilities(this.mNetworkCapabilities);
        }
    }

    @VisibleForTesting
    public static void updateCapabilities(ConnectivityManager cm, Network[] underlyingNetworks, NetworkCapabilities caps) {
        int upKbps;
        Network[] networkArr = underlyingNetworks;
        NetworkCapabilities networkCapabilities = caps;
        boolean z = true;
        int[] transportTypes = new int[]{4};
        int downKbps = 0;
        boolean metered = false;
        boolean roaming = false;
        boolean congested = false;
        boolean hadUnderlyingNetworks = false;
        ConnectivityManager connectivityManager;
        if (networkArr != null) {
            int length = networkArr.length;
            upKbps = 0;
            int upKbps2 = 0;
            int[] transportTypes2 = transportTypes;
            int transportTypes3 = 0;
            while (transportTypes3 < length) {
                Network underlying = networkArr[transportTypes3];
                NetworkCapabilities underlyingCaps = cm.getNetworkCapabilities(underlying);
                if (underlyingCaps != null) {
                    hadUnderlyingNetworks = true;
                    int[] transportTypes4 = underlyingCaps.getTransportTypes();
                    int length2 = transportTypes4.length;
                    int[] transportTypes5 = transportTypes2;
                    downKbps = 0;
                    while (downKbps < length2) {
                        Network underlying2 = underlying;
                        transportTypes5 = ArrayUtils.appendInt(transportTypes5, transportTypes4[downKbps]);
                        downKbps++;
                        underlying = underlying2;
                    }
                    upKbps2 = NetworkCapabilities.minBandwidth(upKbps2, underlyingCaps.getLinkDownstreamBandwidthKbps());
                    upKbps = NetworkCapabilities.minBandwidth(upKbps, underlyingCaps.getLinkUpstreamBandwidthKbps());
                    z = true;
                    metered |= underlyingCaps.hasCapability(11) ^ 1;
                    roaming |= underlyingCaps.hasCapability(18) ^ 1;
                    congested |= underlyingCaps.hasCapability(20) ^ 1;
                    transportTypes2 = transportTypes5;
                }
                transportTypes3++;
                networkArr = underlyingNetworks;
            }
            connectivityManager = cm;
            transportTypes = transportTypes2;
            downKbps = upKbps2;
        } else {
            connectivityManager = cm;
            upKbps = 0;
        }
        if (!hadUnderlyingNetworks) {
            metered = true;
            roaming = false;
            congested = false;
        }
        networkCapabilities.setTransportTypes(transportTypes);
        networkCapabilities.setLinkDownstreamBandwidthKbps(downKbps);
        networkCapabilities.setLinkUpstreamBandwidthKbps(upKbps);
        networkCapabilities.setCapability(11, !metered ? z : false);
        networkCapabilities.setCapability(18, !roaming ? z : false);
        if (congested) {
            z = false;
        }
        networkCapabilities.setCapability(20, z);
    }

    public synchronized void setLockdown(boolean lockdown) {
        enforceControlPermissionOrInternalCaller();
        setVpnForcedLocked(lockdown);
        this.mLockdown = lockdown;
        if (this.mAlwaysOn) {
            saveAlwaysOnPackage();
        }
    }

    public boolean isAlwaysOnPackageSupported(String packageName) {
        enforceSettingsPermission();
        if (packageName == null) {
            return false;
        }
        PackageManager pm = this.mContext.getPackageManager();
        ApplicationInfo appInfo = null;
        try {
            appInfo = pm.getApplicationInfoAsUser(packageName, 0, this.mUserHandle);
        } catch (NameNotFoundException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can't find \"");
            stringBuilder.append(packageName);
            stringBuilder.append("\" when checking always-on support");
            Log.w(str, stringBuilder.toString());
        }
        if (appInfo == null || appInfo.targetSdkVersion < 24) {
            return false;
        }
        Intent intent = new Intent("android.net.VpnService");
        intent.setPackage(packageName);
        List<ResolveInfo> services = pm.queryIntentServicesAsUser(intent, 128, this.mUserHandle);
        if (services == null || services.size() == 0) {
            return false;
        }
        for (ResolveInfo rInfo : services) {
            Bundle metaData = rInfo.serviceInfo.metaData;
            if (metaData != null && !metaData.getBoolean("android.net.VpnService.SUPPORTS_ALWAYS_ON", true)) {
                return false;
            }
        }
        return true;
    }

    public synchronized boolean setAlwaysOnPackage(String packageName, boolean lockdown) {
        enforceControlPermissionOrInternalCaller();
        if (!setAlwaysOnPackageInternal(packageName, lockdown)) {
            return false;
        }
        saveAlwaysOnPackage();
        return true;
    }

    @GuardedBy("this")
    private boolean setAlwaysOnPackageInternal(String packageName, boolean lockdown) {
        boolean z = false;
        if ("[Legacy VPN]".equals(packageName)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Not setting legacy VPN \"");
            stringBuilder.append(packageName);
            stringBuilder.append("\" as always-on.");
            Log.w(str, stringBuilder.toString());
            return false;
        }
        if (packageName == null) {
            packageName = "[Legacy VPN]";
            this.mAlwaysOn = false;
        } else if (!setPackageAuthorization(packageName, true)) {
            return false;
        } else {
            this.mAlwaysOn = true;
        }
        if (this.mAlwaysOn && lockdown) {
            z = true;
        }
        this.mLockdown = z;
        if (isCurrentPreparedPackage(packageName)) {
            updateAlwaysOnNotification(this.mNetworkInfo.getDetailedState());
        } else {
            prepareInternal(packageName);
        }
        maybeRegisterPackageChangeReceiverLocked(packageName);
        setVpnForcedLocked(this.mLockdown);
        return true;
    }

    private static boolean isNullOrLegacyVpn(String packageName) {
        return packageName == null || "[Legacy VPN]".equals(packageName);
    }

    private void unregisterPackageChangeReceiverLocked() {
        if (this.mIsPackageIntentReceiverRegistered) {
            this.mContext.unregisterReceiver(this.mPackageIntentReceiver);
            this.mIsPackageIntentReceiverRegistered = false;
        }
    }

    private void maybeRegisterPackageChangeReceiverLocked(String packageName) {
        unregisterPackageChangeReceiverLocked();
        if (!isNullOrLegacyVpn(packageName)) {
            this.mIsPackageIntentReceiverRegistered = true;
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.PACKAGE_REPLACED");
            intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            intentFilter.addDataScheme("package");
            intentFilter.addDataSchemeSpecificPart(packageName, 0);
            this.mContext.registerReceiverAsUser(this.mPackageIntentReceiver, UserHandle.of(this.mUserHandle), intentFilter, null, null);
        }
    }

    public synchronized String getAlwaysOnPackage() {
        enforceControlPermissionOrInternalCaller();
        return this.mAlwaysOn ? this.mPackage : null;
    }

    @GuardedBy("this")
    private void saveAlwaysOnPackage() {
        long token = Binder.clearCallingIdentity();
        try {
            this.mSystemServices.settingsSecurePutStringForUser("always_on_vpn_app", getAlwaysOnPackage(), this.mUserHandle);
            SystemServices systemServices = this.mSystemServices;
            String str = "always_on_vpn_lockdown";
            int i = (this.mAlwaysOn && this.mLockdown) ? 1 : 0;
            systemServices.settingsSecurePutIntForUser(str, i, this.mUserHandle);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @GuardedBy("this")
    private void loadAlwaysOnPackage() {
        long token = Binder.clearCallingIdentity();
        try {
            String alwaysOnPackage = this.mSystemServices.settingsSecureGetStringForUser("always_on_vpn_app", this.mUserHandle);
            boolean z = false;
            if (this.mSystemServices.settingsSecureGetIntForUser("always_on_vpn_lockdown", 0, this.mUserHandle) != 0) {
                z = true;
            }
            setAlwaysOnPackageInternal(alwaysOnPackage, z);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0024, code:
            r11 = android.os.Binder.clearCallingIdentity();
     */
    /* JADX WARNING: Missing block: B:18:?, code:
            ((com.android.server.DeviceIdleController.LocalService) com.android.server.LocalServices.getService(com.android.server.DeviceIdleController.LocalService.class)).addPowerSaveTempWhitelistApp(android.os.Process.myUid(), r0, 60000, r13.mUserHandle, false, "vpn");
            r2 = new android.content.Intent("android.net.VpnService");
            r2.setPackage(r0);
     */
    /* JADX WARNING: Missing block: B:21:0x0058, code:
            if (r13.mContext.startServiceAsUser(r2, android.os.UserHandle.of(r13.mUserHandle)) == null) goto L_0x005b;
     */
    /* JADX WARNING: Missing block: B:22:0x005b, code:
            r9 = false;
     */
    /* JADX WARNING: Missing block: B:23:0x005c, code:
            android.os.Binder.restoreCallingIdentity(r11);
     */
    /* JADX WARNING: Missing block: B:24:0x005f, code:
            return r9;
     */
    /* JADX WARNING: Missing block: B:25:0x0060, code:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:27:?, code:
            r4 = TAG;
            r5 = new java.lang.StringBuilder();
            r5.append("VpnService ");
            r5.append(r2);
            r5.append(" failed to start");
            android.util.Log.e(r4, r5.toString(), r3);
     */
    /* JADX WARNING: Missing block: B:28:0x007c, code:
            android.os.Binder.restoreCallingIdentity(r11);
     */
    /* JADX WARNING: Missing block: B:29:0x0080, code:
            return false;
     */
    /* JADX WARNING: Missing block: B:30:0x0081, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:31:0x0082, code:
            android.os.Binder.restoreCallingIdentity(r11);
     */
    /* JADX WARNING: Missing block: B:32:0x0085, code:
            throw r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean startAlwaysOnVpn() {
        synchronized (this) {
            try {
                String alwaysOnPackage = getAlwaysOnPackage();
                boolean z = true;
                if (alwaysOnPackage == null) {
                    return true;
                } else if (!isAlwaysOnPackageSupported(alwaysOnPackage)) {
                    setAlwaysOnPackage(null, false);
                    return false;
                } else if (getNetworkInfo().isConnected()) {
                    return true;
                }
            } catch (Throwable th) {
                while (true) {
                    throw th;
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:21:0x002b, code:
            return false;
     */
    /* JADX WARNING: Missing block: B:50:0x006b, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean prepare(String oldPackage, String newPackage) {
        if (oldPackage != null) {
            if (this.mAlwaysOn && !isCurrentPreparedPackage(oldPackage)) {
                return false;
            }
            if (isCurrentPreparedPackage(oldPackage)) {
                if (!(oldPackage.equals("[Legacy VPN]") || isVpnUserPreConsented(oldPackage))) {
                    prepareInternal("[Legacy VPN]");
                    return false;
                }
            } else if (!oldPackage.equals("[Legacy VPN]") && isVpnUserPreConsented(oldPackage)) {
                prepareInternal(oldPackage);
                return true;
            }
        }
        if (newPackage != null) {
            if (newPackage.equals("[Legacy VPN]") || !isCurrentPreparedPackage(newPackage)) {
                enforceControlPermission();
                if (this.mAlwaysOn && !isCurrentPreparedPackage(newPackage)) {
                    return false;
                }
                prepareInternal(newPackage);
                return true;
            }
        }
    }

    public synchronized boolean turnOffAllVpn(String packageName) {
        prepareInternal(packageName);
        return true;
    }

    private boolean isCurrentPreparedPackage(String packageName) {
        return getAppUid(packageName, this.mUserHandle) == this.mOwnerUID;
    }

    private void prepareInternal(String newPackage) {
        String str;
        StringBuilder stringBuilder;
        long token = Binder.clearCallingIdentity();
        try {
            if (this.mInterface != null) {
                this.mStatusIntent = null;
                agentDisconnect();
                jniReset(this.mInterface);
                this.mInterface = null;
                this.mNetworkCapabilities.setUids(null);
            }
            if (this.mConnection != null) {
                try {
                    this.mConnection.mService.transact(UsbAudioDevice.kAudioDeviceClassMask, Parcel.obtain(), null, 1);
                } catch (Exception e) {
                }
                this.mContext.unbindService(this.mConnection);
                this.mConnection = null;
            } else if (this.mLegacyVpnRunner != null) {
                this.mLegacyVpnRunner.exit();
                this.mLegacyVpnRunner = null;
            }
            this.mNetd.denyProtect(this.mOwnerUID);
        } catch (Exception e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to disallow UID ");
            stringBuilder.append(this.mOwnerUID);
            stringBuilder.append(" to call protect() ");
            stringBuilder.append(e2);
            Log.wtf(str, stringBuilder.toString());
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Switched from ");
        stringBuilder2.append(this.mPackage);
        stringBuilder2.append(" to ");
        stringBuilder2.append(newPackage);
        Log.i(str2, stringBuilder2.toString());
        this.mPackage = newPackage;
        this.mOwnerUID = getAppUid(newPackage, this.mUserHandle);
        try {
            this.mNetd.allowProtect(this.mOwnerUID);
        } catch (Exception e22) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to allow UID ");
            stringBuilder.append(this.mOwnerUID);
            stringBuilder.append(" to call protect() ");
            stringBuilder.append(e22);
            Log.wtf(str, stringBuilder.toString());
        }
        this.mConfig = null;
        updateState(DetailedState.IDLE, "prepare");
        setVpnForcedLocked(this.mLockdown);
        Binder.restoreCallingIdentity(token);
    }

    public boolean setPackageAuthorization(String packageName, boolean authorized) {
        enforceControlPermissionOrInternalCaller();
        int uid = getAppUid(packageName, this.mUserHandle);
        if (uid == -1 || "[Legacy VPN]".equals(packageName)) {
            return false;
        }
        long token = Binder.clearCallingIdentity();
        try {
            ((AppOpsManager) this.mContext.getSystemService("appops")).setMode(47, uid, packageName, authorized ^ 1);
            return true;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to set app ops for package ");
            stringBuilder.append(packageName);
            stringBuilder.append(", uid ");
            stringBuilder.append(uid);
            Log.wtf(str, stringBuilder.toString(), e);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean isVpnUserPreConsented(String packageName) {
        return ((AppOpsManager) this.mContext.getSystemService("appops")).noteOpNoThrow(47, Binder.getCallingUid(), packageName) == 0;
    }

    private int getAppUid(String app, int userHandle) {
        if ("[Legacy VPN]".equals(app)) {
            return Process.myUid();
        }
        int result;
        try {
            result = this.mContext.getPackageManager().getPackageUidAsUser(app, userHandle);
        } catch (NameNotFoundException e) {
            result = -1;
        }
        return result;
    }

    public NetworkInfo getNetworkInfo() {
        return this.mNetworkInfo;
    }

    public int getNetId() {
        return this.mNetworkAgent != null ? this.mNetworkAgent.netId : 0;
    }

    private LinkProperties makeLinkProperties() {
        InetAddress address;
        boolean allowIPv4 = this.mConfig.allowIPv4;
        boolean allowIPv6 = this.mConfig.allowIPv6;
        LinkProperties lp = new LinkProperties();
        lp.setInterfaceName(this.mInterface);
        if (this.mConfig.addresses != null) {
            for (LinkAddress address2 : this.mConfig.addresses) {
                lp.addLinkAddress(address2);
                allowIPv4 |= address2.getAddress() instanceof Inet4Address;
                allowIPv6 |= address2.getAddress() instanceof Inet6Address;
            }
        }
        if (this.mConfig.routes != null) {
            for (RouteInfo route : this.mConfig.routes) {
                lp.addRoute(route);
                address = route.getDestination().getAddress();
                allowIPv4 |= address instanceof Inet4Address;
                allowIPv6 |= address instanceof Inet6Address;
            }
        }
        if (this.mConfig.dnsServers != null) {
            for (String dnsServer : this.mConfig.dnsServers) {
                address = InetAddress.parseNumericAddress(dnsServer);
                lp.addDnsServer(address);
                allowIPv4 |= address instanceof Inet4Address;
                allowIPv6 |= address instanceof Inet6Address;
            }
        }
        if (!allowIPv4) {
            lp.addRoute(new RouteInfo(new IpPrefix(Inet4Address.ANY, 0), 7));
        }
        if (!allowIPv6) {
            lp.addRoute(new RouteInfo(new IpPrefix(Inet6Address.ANY, 0), 7));
        }
        StringBuilder buffer = new StringBuilder();
        if (this.mConfig.searchDomains != null) {
            for (String domain : this.mConfig.searchDomains) {
                buffer.append(domain);
                buffer.append(' ');
            }
        }
        lp.setDomains(buffer.toString().trim());
        return lp;
    }

    @VisibleForTesting
    static boolean providesRoutesToMostDestinations(LinkProperties lp) {
        List<RouteInfo> routes = lp.getAllRoutes();
        boolean z = true;
        if (routes.size() > 150) {
            return true;
        }
        Comparator<IpPrefix> prefixLengthComparator = IpPrefix.lengthComparator();
        TreeSet<IpPrefix> ipv4Prefixes = new TreeSet(prefixLengthComparator);
        TreeSet<IpPrefix> ipv6Prefixes = new TreeSet(prefixLengthComparator);
        for (RouteInfo route : routes) {
            IpPrefix destination = route.getDestination();
            if (destination.isIPv4()) {
                ipv4Prefixes.add(destination);
            } else {
                ipv6Prefixes.add(destination);
            }
        }
        if (NetworkUtils.routedIPv4AddressCount(ipv4Prefixes) > MOST_IPV4_ADDRESSES_COUNT) {
            return true;
        }
        if (NetworkUtils.routedIPv6AddressCount(ipv6Prefixes).compareTo(MOST_IPV6_ADDRESSES_COUNT) < 0) {
            z = false;
        }
        return z;
    }

    private boolean updateLinkPropertiesInPlaceIfPossible(NetworkAgent agent, VpnConfig oldConfig) {
        if (oldConfig.allowBypass != this.mConfig.allowBypass) {
            Log.i(TAG, "Handover not possible due to changes to allowBypass");
            return false;
        } else if (Objects.equals(oldConfig.allowedApplications, this.mConfig.allowedApplications) && Objects.equals(oldConfig.disallowedApplications, this.mConfig.disallowedApplications)) {
            LinkProperties lp = makeLinkProperties();
            if (this.mNetworkCapabilities.hasCapability(12) != providesRoutesToMostDestinations(lp)) {
                Log.i(TAG, "Handover not possible due to changes to INTERNET capability");
                return false;
            }
            agent.sendLinkProperties(lp);
            return true;
        } else {
            Log.i(TAG, "Handover not possible due to changes to whitelisted/blacklisted apps");
            return false;
        }
    }

    private void agentConnect() {
        Throwable th;
        long token;
        LinkProperties lp = makeLinkProperties();
        if (providesRoutesToMostDestinations(lp)) {
            this.mNetworkCapabilities.addCapability(12);
        } else {
            this.mNetworkCapabilities.removeCapability(12);
        }
        this.mNetworkInfo.setDetailedState(DetailedState.CONNECTING, null, null);
        NetworkMisc networkMisc = new NetworkMisc();
        boolean z = this.mConfig.allowBypass && !this.mLockdown;
        networkMisc.allowBypass = z;
        this.mNetworkCapabilities.setEstablishingVpnAppUid(Binder.getCallingUid());
        this.mNetworkCapabilities.setUids(createUserAndRestrictedProfilesRanges(this.mUserHandle, this.mConfig.allowedApplications, this.mConfig.disallowedApplications));
        long token2 = Binder.clearCallingIdentity();
        try {
            NetworkAgent anonymousClass2 = anonymousClass2;
            long token3 = token2;
            try {
                this.mNetworkAgent = new NetworkAgent(this, this.mLooper, this.mContext, NETWORKTYPE, this.mNetworkInfo, this.mNetworkCapabilities, lp, 101, networkMisc) {
                    final /* synthetic */ Vpn this$0;

                    public void unwanted() {
                    }
                };
                Binder.restoreCallingIdentity(token3);
                this.mNetworkInfo.setIsAvailable(true);
                updateState(DetailedState.CONNECTED, "agentConnect");
            } catch (Throwable th2) {
                th = th2;
                token = token3;
                Binder.restoreCallingIdentity(token);
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            token = token2;
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    private boolean canHaveRestrictedProfile(int userId) {
        long token = Binder.clearCallingIdentity();
        try {
            boolean canHaveRestrictedProfile = UserManager.get(this.mContext).canHaveRestrictedProfile(userId);
            return canHaveRestrictedProfile;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void agentDisconnect(NetworkAgent networkAgent) {
        if (networkAgent != null) {
            NetworkInfo networkInfo = new NetworkInfo(this.mNetworkInfo);
            networkInfo.setIsAvailable(false);
            networkInfo.setDetailedState(DetailedState.DISCONNECTED, null, null);
            networkAgent.sendNetworkInfo(networkInfo);
        }
    }

    private void agentDisconnect() {
        if (this.mNetworkInfo.isConnected()) {
            this.mNetworkInfo.setIsAvailable(false);
            updateState(DetailedState.DISCONNECTED, "agentDisconnect");
            this.mNetworkAgent = null;
        }
    }

    public synchronized ParcelFileDescriptor establish(VpnConfig config) {
        UserInfo user;
        UserManager userManager;
        Throwable th;
        VpnConfig vpnConfig = config;
        synchronized (this) {
            UserManager mgr = UserManager.get(this.mContext);
            if (Binder.getCallingUid() != this.mOwnerUID) {
                return null;
            } else if (isVpnUserPreConsented(this.mPackage)) {
                Intent intent = new Intent("android.net.VpnService");
                intent.setClassName(this.mPackage, vpnConfig.user);
                long token = Binder.clearCallingIdentity();
                Intent intent2;
                StringBuilder stringBuilder;
                try {
                    if (mgr.getUserInfo(this.mUserHandle).isRestricted()) {
                        intent2 = intent;
                        throw new SecurityException("Restricted users cannot establish VPNs");
                    }
                    ResolveInfo info = AppGlobals.getPackageManager().resolveService(intent, null, 0, this.mUserHandle);
                    if (info == null) {
                        intent2 = intent;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Cannot find ");
                        stringBuilder.append(vpnConfig.user);
                        throw new SecurityException(stringBuilder.toString());
                    } else if ("android.permission.BIND_VPN_SERVICE".equals(info.serviceInfo.permission)) {
                        Binder.restoreCallingIdentity(token);
                        VpnConfig info2 = this.mConfig;
                        String oldInterface = this.mInterface;
                        Connection oldConnection = this.mConnection;
                        NetworkAgent oldNetworkAgent = this.mNetworkAgent;
                        Set<UidRange> oldUsers = this.mNetworkCapabilities.getUids();
                        ParcelFileDescriptor tun = ParcelFileDescriptor.adoptFd(jniCreate(vpnConfig.mtu));
                        try {
                            String interfaze = jniGetName(tun.getFd());
                            StringBuilder builder = new StringBuilder();
                            Iterator it = vpnConfig.addresses.iterator();
                            while (it.hasNext()) {
                                try {
                                    LinkAddress address = (LinkAddress) it.next();
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    Iterator it2 = it;
                                    stringBuilder2.append(" ");
                                    stringBuilder2.append(address);
                                    builder.append(stringBuilder2.toString());
                                    it = it2;
                                } catch (RuntimeException e) {
                                    user = e;
                                    userManager = mgr;
                                    intent2 = intent;
                                }
                            }
                            if (jniSetAddresses(interfaze, builder.toString()) >= 1) {
                                Connection connection = new Connection(this, null);
                                try {
                                    StringBuilder stringBuilder3;
                                    if (this.mContext.bindServiceAsUser(intent, connection, 67108865, new UserHandle(this.mUserHandle))) {
                                        this.mConnection = connection;
                                        this.mInterface = interfaze;
                                        vpnConfig.user = this.mPackage;
                                        vpnConfig.interfaze = this.mInterface;
                                        try {
                                            vpnConfig.startTime = SystemClock.elapsedRealtime();
                                            this.mConfig = vpnConfig;
                                            if (info2 == null || !updateLinkPropertiesInPlaceIfPossible(this.mNetworkAgent, info2)) {
                                                this.mNetworkAgent = null;
                                                updateState(DetailedState.CONNECTING, "establish");
                                                agentConnect();
                                                agentDisconnect(oldNetworkAgent);
                                            }
                                            if (oldConnection != null) {
                                                this.mContext.unbindService(oldConnection);
                                            }
                                            if (!(oldInterface == null || oldInterface.equals(interfaze))) {
                                                jniReset(oldInterface);
                                            }
                                            IoUtils.setBlocking(tun.getFileDescriptor(), vpnConfig.blocking);
                                            String str = TAG;
                                            stringBuilder3 = new StringBuilder();
                                            stringBuilder3.append("Established by ");
                                            stringBuilder3.append(vpnConfig.user);
                                            stringBuilder3.append(" on ");
                                            stringBuilder3.append(this.mInterface);
                                            Log.i(str, stringBuilder3.toString());
                                            return tun;
                                        } catch (IOException e2) {
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("Cannot set tunnel's fd as blocking=");
                                            stringBuilder.append(vpnConfig.blocking);
                                            throw new IllegalStateException(stringBuilder.toString(), e2);
                                        } catch (RuntimeException e3) {
                                            user = e3;
                                            IoUtils.closeQuietly(tun);
                                            agentDisconnect();
                                            this.mConfig = info2;
                                            this.mConnection = oldConnection;
                                            this.mNetworkCapabilities.setUids(oldUsers);
                                            this.mNetworkAgent = oldNetworkAgent;
                                            this.mInterface = oldInterface;
                                            throw user;
                                        }
                                    }
                                    stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("Cannot bind ");
                                    stringBuilder3.append(vpnConfig.user);
                                    throw new IllegalStateException(stringBuilder3.toString());
                                } catch (RuntimeException e4) {
                                    user = e4;
                                    intent2 = intent;
                                    IoUtils.closeQuietly(tun);
                                    agentDisconnect();
                                    this.mConfig = info2;
                                    this.mConnection = oldConnection;
                                    this.mNetworkCapabilities.setUids(oldUsers);
                                    this.mNetworkAgent = oldNetworkAgent;
                                    this.mInterface = oldInterface;
                                    throw user;
                                }
                            }
                            intent2 = intent;
                            StringBuilder stringBuilder4 = builder;
                            throw new IllegalArgumentException("At least one address must be specified");
                        } catch (RuntimeException e5) {
                            user = e5;
                            userManager = mgr;
                            intent2 = intent;
                            IoUtils.closeQuietly(tun);
                            agentDisconnect();
                            this.mConfig = info2;
                            this.mConnection = oldConnection;
                            this.mNetworkCapabilities.setUids(oldUsers);
                            this.mNetworkAgent = oldNetworkAgent;
                            this.mInterface = oldInterface;
                            throw user;
                        }
                    } else {
                        intent2 = intent;
                        try {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(vpnConfig.user);
                            stringBuilder.append(" does not require ");
                            stringBuilder.append("android.permission.BIND_VPN_SERVICE");
                            throw new SecurityException(stringBuilder.toString());
                        } catch (RemoteException e6) {
                            try {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Cannot find ");
                                stringBuilder.append(vpnConfig.user);
                                throw new SecurityException(stringBuilder.toString());
                            } catch (Throwable th2) {
                                th = th2;
                                Binder.restoreCallingIdentity(token);
                                throw th;
                            }
                        }
                    }
                } catch (RemoteException e7) {
                    userManager = mgr;
                    intent2 = intent;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Cannot find ");
                    stringBuilder.append(vpnConfig.user);
                    throw new SecurityException(stringBuilder.toString());
                } catch (Throwable th3) {
                    th = th3;
                    userManager = mgr;
                    intent2 = intent;
                    Binder.restoreCallingIdentity(token);
                    throw th;
                }
            } else {
                return null;
            }
        }
    }

    private boolean isRunningLocked() {
        return (this.mNetworkAgent == null || this.mInterface == null) ? false : true;
    }

    @VisibleForTesting
    protected boolean isCallerEstablishedOwnerLocked() {
        return isRunningLocked() && Binder.getCallingUid() == this.mOwnerUID;
    }

    private SortedSet<Integer> getAppsUids(List<String> packageNames, int userHandle) {
        SortedSet<Integer> uids = new TreeSet();
        for (String app : packageNames) {
            int uid = getAppUid(app, userHandle);
            if (uid != -1) {
                uids.add(Integer.valueOf(uid));
            }
        }
        return uids;
    }

    @VisibleForTesting
    Set<UidRange> createUserAndRestrictedProfilesRanges(int userHandle, List<String> allowedApplications, List<String> disallowedApplications) {
        Set<UidRange> ranges = new ArraySet();
        addUserToRanges(ranges, userHandle, allowedApplications, disallowedApplications);
        if (canHaveRestrictedProfile(userHandle)) {
            long token = Binder.clearCallingIdentity();
            try {
                List<UserInfo> users = UserManager.get(this.mContext).getUsers(true);
                for (UserInfo user : users) {
                    if (user.isRestricted() && user.restrictedProfileParentId == userHandle) {
                        addUserToRanges(ranges, user.id, allowedApplications, disallowedApplications);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
        return ranges;
    }

    @VisibleForTesting
    void addUserToRanges(Set<UidRange> ranges, int userHandle, List<String> allowedApplications, List<String> disallowedApplications) {
        int stop;
        int uid;
        if (allowedApplications != null) {
            int start = -1;
            stop = -1;
            for (Integer uid2 : getAppsUids(allowedApplications, userHandle)) {
                uid = uid2.intValue();
                if (start == -1) {
                    start = uid;
                } else if (uid != stop + 1) {
                    ranges.add(new UidRange(start, stop));
                    start = uid;
                }
                stop = uid;
            }
            if (start != -1) {
                ranges.add(new UidRange(start, stop));
            }
        } else if (disallowedApplications != null) {
            UidRange userRange = UidRange.createForUser(userHandle);
            stop = userRange.start;
            for (Integer uid22 : getAppsUids(disallowedApplications, userHandle)) {
                uid = uid22.intValue();
                if (uid == stop) {
                    stop++;
                } else {
                    ranges.add(new UidRange(stop, uid - 1));
                    stop = uid + 1;
                }
            }
            if (stop <= userRange.stop) {
                ranges.add(new UidRange(stop, userRange.stop));
            }
        } else {
            ranges.add(UidRange.createForUser(userHandle));
        }
    }

    private static List<UidRange> uidRangesForUser(int userHandle, Set<UidRange> existingRanges) {
        UidRange userRange = UidRange.createForUser(userHandle);
        List<UidRange> ranges = new ArrayList();
        for (UidRange range : existingRanges) {
            if (userRange.containsRange(range)) {
                ranges.add(range);
            }
        }
        return ranges;
    }

    public void onUserAdded(int userHandle) {
        UserInfo user = UserManager.get(this.mContext).getUserInfo(userHandle);
        if (user.isRestricted() && user.restrictedProfileParentId == this.mUserHandle) {
            synchronized (this) {
                Set<UidRange> existingRanges = this.mNetworkCapabilities.getUids();
                if (existingRanges != null) {
                    try {
                        addUserToRanges(existingRanges, userHandle, this.mConfig.allowedApplications, this.mConfig.disallowedApplications);
                        this.mNetworkCapabilities.setUids(existingRanges);
                        updateCapabilities();
                    } catch (Exception e) {
                        Log.wtf(TAG, "Failed to add restricted user to owner", e);
                    }
                }
                setVpnForcedLocked(this.mLockdown);
            }
        }
    }

    public void onUserRemoved(int userHandle) {
        UserInfo user = UserManager.get(this.mContext).getUserInfo(userHandle);
        if (user.isRestricted() && user.restrictedProfileParentId == this.mUserHandle) {
            synchronized (this) {
                Set<UidRange> existingRanges = this.mNetworkCapabilities.getUids();
                if (existingRanges != null) {
                    try {
                        existingRanges.removeAll(uidRangesForUser(userHandle, existingRanges));
                        this.mNetworkCapabilities.setUids(existingRanges);
                        updateCapabilities();
                    } catch (Exception e) {
                        Log.wtf(TAG, "Failed to remove restricted user to owner", e);
                    }
                }
                setVpnForcedLocked(this.mLockdown);
            }
        }
    }

    public synchronized void onUserStopped() {
        setLockdown(false);
        this.mAlwaysOn = false;
        unregisterPackageChangeReceiverLocked();
        agentDisconnect();
    }

    @GuardedBy("this")
    private void setVpnForcedLocked(boolean enforce) {
        List<String> exemptedPackages = isNullOrLegacyVpn(this.mPackage) ? null : Collections.singletonList(this.mPackage);
        Set<UidRange> removedRanges = new ArraySet(this.mBlockedUsers);
        Set<UidRange> addedRanges = Collections.emptySet();
        if (enforce) {
            addedRanges = createUserAndRestrictedProfilesRanges(this.mUserHandle, null, exemptedPackages);
            for (UidRange range : addedRanges) {
                if (range.start == 0) {
                    addedRanges.remove(range);
                    if (range.stop != 0) {
                        addedRanges.add(new UidRange(1, range.stop));
                    }
                }
            }
            removedRanges.removeAll(addedRanges);
            addedRanges.removeAll(this.mBlockedUsers);
        }
        setAllowOnlyVpnForUids(false, removedRanges);
        setAllowOnlyVpnForUids(true, addedRanges);
    }

    /* JADX WARNING: Removed duplicated region for block: B:10:0x0028 A:{Splitter: B:4:0x0014, ExcHandler: android.os.RemoteException (r1_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:10:0x0028, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:11:0x0029, code:
            r2 = TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("Updating blocked=");
            r3.append(r6);
            r3.append(" for UIDs ");
            r3.append(java.util.Arrays.toString(r7.toArray()));
            r3.append(" failed");
            android.util.Log.e(r2, r3.toString(), r1);
     */
    /* JADX WARNING: Missing block: B:12:0x0055, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @GuardedBy("this")
    private boolean setAllowOnlyVpnForUids(boolean enforce, Collection<UidRange> ranges) {
        if (ranges.size() == 0) {
            return true;
        }
        try {
            this.mNetd.setAllowOnlyVpnForUids(enforce, (UidRange[]) ranges.toArray(new UidRange[ranges.size()]));
            if (enforce) {
                this.mBlockedUsers.addAll(ranges);
            } else {
                this.mBlockedUsers.removeAll(ranges);
            }
            return true;
        } catch (Exception e) {
        }
    }

    public VpnConfig getVpnConfig() {
        enforceControlPermission();
        return this.mConfig;
    }

    @Deprecated
    public synchronized void interfaceStatusChanged(String iface, boolean up) {
        try {
            this.mObserver.interfaceStatusChanged(iface, up);
        } catch (RemoteException e) {
        }
    }

    private void enforceControlPermission() {
        this.mContext.enforceCallingPermission("android.permission.CONTROL_VPN", "Unauthorized Caller");
    }

    private void enforceControlPermissionOrInternalCaller() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONTROL_VPN", "Unauthorized Caller");
    }

    private void enforceSettingsPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_SETTINGS", "Unauthorized Caller");
    }

    private void prepareStatusIntent() {
        long token = Binder.clearCallingIdentity();
        try {
            this.mStatusIntent = VpnConfig.getIntentForStatusPanel(this.mContext);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public synchronized boolean addAddress(String address, int prefixLength) {
        if (!isCallerEstablishedOwnerLocked()) {
            return false;
        }
        boolean success = jniAddAddress(this.mInterface, address, prefixLength);
        this.mNetworkAgent.sendLinkProperties(makeLinkProperties());
        return success;
    }

    public synchronized boolean removeAddress(String address, int prefixLength) {
        if (!isCallerEstablishedOwnerLocked()) {
            return false;
        }
        boolean success = jniDelAddress(this.mInterface, address, prefixLength);
        this.mNetworkAgent.sendLinkProperties(makeLinkProperties());
        return success;
    }

    public synchronized boolean setUnderlyingNetworks(Network[] networks) {
        int i = 0;
        if (!isCallerEstablishedOwnerLocked()) {
            return false;
        }
        if (networks == null) {
            this.mConfig.underlyingNetworks = null;
        } else {
            this.mConfig.underlyingNetworks = new Network[networks.length];
            while (i < networks.length) {
                if (networks[i] == null) {
                    this.mConfig.underlyingNetworks[i] = null;
                } else {
                    this.mConfig.underlyingNetworks[i] = new Network(networks[i].netId);
                }
                i++;
            }
        }
        updateCapabilities();
        return true;
    }

    public synchronized Network[] getUnderlyingNetworks() {
        if (!isRunningLocked()) {
            return null;
        }
        return this.mConfig.underlyingNetworks;
    }

    public synchronized VpnInfo getVpnInfo() {
        if (!isRunningLocked()) {
            return null;
        }
        VpnInfo info = new VpnInfo();
        info.ownerUid = this.mOwnerUID;
        info.vpnIface = this.mInterface;
        return info;
    }

    public synchronized boolean appliesToUid(int uid) {
        if (!isRunningLocked()) {
            return false;
        }
        return this.mNetworkCapabilities.appliesToUid(uid);
    }

    public synchronized boolean isBlockingUid(int uid) {
        if (!this.mLockdown) {
            return false;
        }
        if (this.mNetworkInfo.isConnected()) {
            return appliesToUid(uid) ^ true;
        }
        for (UidRange uidRange : this.mBlockedUsers) {
            if (uidRange.contains(uid)) {
                return true;
            }
        }
        return false;
    }

    private void updateAlwaysOnNotification(DetailedState networkState) {
        boolean visible = this.mAlwaysOn && networkState != DetailedState.CONNECTED;
        UserHandle user = UserHandle.of(this.mUserHandle);
        long token = Binder.clearCallingIdentity();
        try {
            NotificationManager notificationManager = NotificationManager.from(this.mContext);
            if (visible) {
                Intent intent = new Intent();
                intent.setComponent(ComponentName.unflattenFromString(this.mContext.getString(17039777)));
                intent.putExtra("lockdown", this.mLockdown);
                intent.addFlags(268435456);
                notificationManager.notifyAsUser(TAG, 17, new Builder(this.mContext, SystemNotificationChannels.VPN).setSmallIcon(17303738).setContentTitle(this.mContext.getString(17041339)).setContentText(this.mContext.getString(17041336)).setContentIntent(this.mSystemServices.pendingIntentGetActivityAsUser(intent, 201326592, user)).setCategory("sys").setVisibility(1).setOngoing(true).setColor(this.mContext.getColor(17170784)).build(), user);
                Binder.restoreCallingIdentity(token);
                return;
            }
            notificationManager.cancelAsUser(TAG, 17, user);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private static RouteInfo findIPv4DefaultRoute(LinkProperties prop) {
        for (RouteInfo route : prop.getAllRoutes()) {
            if (route.isDefaultRoute() && (route.getGateway() instanceof Inet4Address)) {
                return route;
            }
        }
        throw new IllegalStateException("Unable to find IPv4 default gateway");
    }

    public void startLegacyVpn(VpnProfile profile, KeyStore keyStore, LinkProperties egress) {
        enforceControlPermission();
        long token = Binder.clearCallingIdentity();
        try {
            startLegacyVpnPrivileged(profile, keyStore, egress);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void startLegacyVpnPrivileged(VpnProfile profile, KeyStore keyStore, LinkProperties egress) {
        VpnProfile vpnProfile = profile;
        KeyStore keyStore2 = keyStore;
        UserManager mgr = UserManager.get(this.mContext);
        if (mgr.getUserInfo(this.mUserHandle).isRestricted() || mgr.hasUserRestriction("no_config_vpn", new UserHandle(this.mUserHandle))) {
            throw new SecurityException("Restricted users cannot establish VPNs");
        }
        StringBuilder stringBuilder;
        byte[] value;
        RouteInfo ipv4DefaultRoute = findIPv4DefaultRoute(egress);
        String gateway = ipv4DefaultRoute.getGateway().getHostAddress();
        String iface = ipv4DefaultRoute.getInterface();
        String privateKey = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        String userCert = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        String caCert = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        String serverCert = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        String str = null;
        if (!vpnProfile.ipsecUserCert.isEmpty()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("USRPKEY_");
            stringBuilder.append(vpnProfile.ipsecUserCert);
            privateKey = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append("USRCERT_");
            stringBuilder.append(vpnProfile.ipsecUserCert);
            value = keyStore2.get(stringBuilder.toString());
            userCert = value == null ? null : new String(value, StandardCharsets.UTF_8);
        }
        if (!vpnProfile.ipsecCaCert.isEmpty()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("CACERT_");
            stringBuilder.append(vpnProfile.ipsecCaCert);
            value = keyStore2.get(stringBuilder.toString());
            caCert = value == null ? null : new String(value, StandardCharsets.UTF_8);
        }
        if (!vpnProfile.ipsecServerCert.isEmpty()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("USRCERT_");
            stringBuilder.append(vpnProfile.ipsecServerCert);
            value = keyStore2.get(stringBuilder.toString());
            if (value != null) {
                str = new String(value, StandardCharsets.UTF_8);
            }
            serverCert = str;
        }
        if (privateKey == null || userCert == null || caCert == null || serverCert == null) {
            throw new IllegalStateException("Cannot load credentials");
        }
        String[] racoon = null;
        switch (vpnProfile.type) {
            case 2:
                racoon = new String[]{iface, vpnProfile.server, "udppsk", vpnProfile.ipsecIdentifier, vpnProfile.ipsecSecret, "1701"};
                break;
            case 3:
                racoon = new String[]{iface, vpnProfile.server, "udprsa", privateKey, userCert, caCert, serverCert, "1701"};
                break;
            case 4:
                racoon = new String[]{iface, vpnProfile.server, "xauthpsk", vpnProfile.ipsecIdentifier, vpnProfile.ipsecSecret, vpnProfile.username, vpnProfile.password, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, gateway};
                break;
            case 5:
                racoon = new String[]{iface, vpnProfile.server, "xauthrsa", privateKey, userCert, caCert, serverCert, vpnProfile.username, vpnProfile.password, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, gateway};
                break;
            case 6:
                racoon = new String[]{iface, vpnProfile.server, "hybridrsa", caCert, serverCert, vpnProfile.username, vpnProfile.password, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, gateway};
                break;
        }
        String[] mtpd = null;
        switch (vpnProfile.type) {
            case 0:
                String[] strArr = new String[20];
                strArr[0] = iface;
                strArr[1] = "pptp";
                strArr[2] = vpnProfile.server;
                strArr[3] = "1723";
                strArr[4] = Settings.ATTR_NAME;
                strArr[5] = vpnProfile.username;
                strArr[6] = "password";
                strArr[7] = vpnProfile.password;
                strArr[8] = "linkname";
                strArr[9] = "vpn";
                strArr[10] = "refuse-eap";
                strArr[11] = "nodefaultroute";
                strArr[12] = "usepeerdns";
                strArr[13] = "idle";
                strArr[14] = "1800";
                strArr[15] = "mtu";
                strArr[16] = "1400";
                strArr[17] = "mru";
                strArr[18] = "1400";
                strArr[19] = vpnProfile.mppe ? "+mppe" : "nomppe";
                mtpd = strArr;
                break;
            case 1:
            case 2:
            case 3:
                mtpd = new String[]{iface, "l2tp", vpnProfile.server, "1701", vpnProfile.l2tpSecret, Settings.ATTR_NAME, vpnProfile.username, "password", vpnProfile.password, "linkname", "vpn", "refuse-eap", "nodefaultroute", "usepeerdns", "idle", "1800", "mtu", "1400", "mru", "1400"};
                break;
        }
        VpnConfig config = new VpnConfig();
        config.legacy = true;
        config.user = vpnProfile.key;
        config.interfaze = iface;
        config.session = vpnProfile.name;
        config.addLegacyRoutes(vpnProfile.routes);
        if (!vpnProfile.dnsServers.isEmpty()) {
            config.dnsServers = Arrays.asList(vpnProfile.dnsServers.split(" +"));
        }
        if (!vpnProfile.searchDomains.isEmpty()) {
            config.searchDomains = Arrays.asList(vpnProfile.searchDomains.split(" +"));
        }
        startLegacyVpn(config, racoon, mtpd);
    }

    private synchronized void startLegacyVpn(VpnConfig config, String[] racoon, String[] mtpd) {
        stopLegacyVpnPrivileged();
        prepareInternal("[Legacy VPN]");
        updateState(DetailedState.CONNECTING, "startLegacyVpn");
        this.mLegacyVpnRunner = new LegacyVpnRunner(config, racoon, mtpd);
        this.mLegacyVpnRunner.start();
    }

    public synchronized void stopLegacyVpnPrivileged() {
        if (this.mLegacyVpnRunner != null) {
            this.mLegacyVpnRunner.exit();
            this.mLegacyVpnRunner = null;
            synchronized ("LegacyVpnRunner") {
            }
        }
    }

    public synchronized LegacyVpnInfo getLegacyVpnInfo() {
        enforceControlPermission();
        return getLegacyVpnInfoPrivileged();
    }

    /* JADX WARNING: Missing block: B:12:0x0028, code:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized LegacyVpnInfo getLegacyVpnInfoPrivileged() {
        if (this.mLegacyVpnRunner == null) {
            return null;
        }
        LegacyVpnInfo info = new LegacyVpnInfo();
        info.key = this.mConfig.user;
        info.state = LegacyVpnInfo.stateFromNetworkInfo(this.mNetworkInfo);
        if (this.mNetworkInfo.isConnected()) {
            info.intent = this.mStatusIntent;
        }
    }

    public VpnConfig getLegacyVpnConfig() {
        if (this.mLegacyVpnRunner != null) {
            return this.mConfig;
        }
        return null;
    }
}
