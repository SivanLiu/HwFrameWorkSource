package com.android.server;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.net.nsd.DnsSdTxtRecord;
import android.net.nsd.INsdManager.Stub;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Messenger;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.util.Base64;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class NsdService extends Stub {
    private static final boolean DBG = true;
    private static final int INVALID_ID = 0;
    private static final String MDNS_TAG = "mDnsConnector";
    private static final String TAG = "NsdService";
    private final HashMap<Messenger, ClientInfo> mClients = new HashMap();
    private final Context mContext;
    private final DaemonConnection mDaemon;
    private final NativeCallbackReceiver mDaemonCallback;
    private final SparseArray<ClientInfo> mIdToClientInfoMap = new SparseArray();
    private final NsdSettings mNsdSettings;
    private final NsdStateMachine mNsdStateMachine;
    private final AsyncChannel mReplyChannel = new AsyncChannel();
    private int mUniqueId = 1;

    private class ClientInfo {
        private static final int MAX_LIMIT = 10;
        private final AsyncChannel mChannel;
        private final SparseIntArray mClientIds;
        private final SparseIntArray mClientRequests;
        private final Messenger mMessenger;
        private NsdServiceInfo mResolvedService;

        private ClientInfo(AsyncChannel c, Messenger m) {
            this.mClientIds = new SparseIntArray();
            this.mClientRequests = new SparseIntArray();
            this.mChannel = c;
            this.mMessenger = m;
            String str = NsdService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("New client, channel: ");
            stringBuilder.append(c);
            stringBuilder.append(" messenger: ");
            stringBuilder.append(m);
            Slog.d(str, stringBuilder.toString());
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("mChannel ");
            sb.append(this.mChannel);
            sb.append("\n");
            sb.append("mMessenger ");
            sb.append(this.mMessenger);
            sb.append("\n");
            sb.append("mResolvedService ");
            sb.append(this.mResolvedService);
            sb.append("\n");
            for (int i = 0; i < this.mClientIds.size(); i++) {
                int clientID = this.mClientIds.keyAt(i);
                sb.append("clientId ");
                sb.append(clientID);
                sb.append(" mDnsId ");
                sb.append(this.mClientIds.valueAt(i));
                sb.append(" type ");
                sb.append(this.mClientRequests.get(clientID));
                sb.append("\n");
            }
            return sb.toString();
        }

        private void expungeAllRequests() {
            for (int i = 0; i < this.mClientIds.size(); i++) {
                int clientId = this.mClientIds.keyAt(i);
                int globalId = this.mClientIds.valueAt(i);
                NsdService.this.mIdToClientInfoMap.remove(globalId);
                String str = NsdService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Terminating client-ID ");
                stringBuilder.append(clientId);
                stringBuilder.append(" global-ID ");
                stringBuilder.append(globalId);
                stringBuilder.append(" type ");
                stringBuilder.append(this.mClientRequests.get(clientId));
                Slog.d(str, stringBuilder.toString());
                int i2 = this.mClientRequests.get(clientId);
                if (i2 == 393217) {
                    NsdService.this.stopServiceDiscovery(globalId);
                } else if (i2 == 393225) {
                    NsdService.this.unregisterService(globalId);
                } else if (i2 == 393234) {
                    NsdService.this.stopResolveService(globalId);
                }
            }
            this.mClientIds.clear();
            this.mClientRequests.clear();
        }

        private int getClientId(int globalId) {
            int idx = this.mClientIds.indexOfValue(globalId);
            if (idx < 0) {
                return idx;
            }
            return this.mClientIds.keyAt(idx);
        }
    }

    @VisibleForTesting
    public static class DaemonConnection {
        final NativeDaemonConnector mNativeConnector;

        DaemonConnection(NativeCallbackReceiver callback) {
            this.mNativeConnector = new NativeDaemonConnector(callback, "mdns", 10, NsdService.MDNS_TAG, 25, null);
            new Thread(this.mNativeConnector, NsdService.MDNS_TAG).start();
        }

        public boolean execute(Object... args) {
            String str = NsdService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mdnssd ");
            stringBuilder.append(Arrays.toString(args));
            Slog.d(str, stringBuilder.toString());
            try {
                this.mNativeConnector.execute("mdnssd", args);
                return true;
            } catch (NativeDaemonConnectorException e) {
                String str2 = NsdService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to execute mdnssd ");
                stringBuilder2.append(Arrays.toString(args));
                Slog.e(str2, stringBuilder2.toString(), e);
                return false;
            }
        }

        public void start() {
            execute("start-service");
        }

        public void stop() {
            execute("stop-service");
        }
    }

    interface DaemonConnectionSupplier {
        DaemonConnection get(NativeCallbackReceiver nativeCallbackReceiver);
    }

    private class NativeEvent {
        final int code;
        final String[] cooked;
        final String raw;

        NativeEvent(int code, String raw, String[] cooked) {
            this.code = code;
            this.raw = raw;
            this.cooked = cooked;
        }
    }

    static final class NativeResponseCode {
        private static final SparseArray<String> CODE_NAMES = new SparseArray();
        public static final int SERVICE_DISCOVERY_FAILED = 602;
        public static final int SERVICE_FOUND = 603;
        public static final int SERVICE_GET_ADDR_FAILED = 611;
        public static final int SERVICE_GET_ADDR_SUCCESS = 612;
        public static final int SERVICE_LOST = 604;
        public static final int SERVICE_REGISTERED = 606;
        public static final int SERVICE_REGISTRATION_FAILED = 605;
        public static final int SERVICE_RESOLUTION_FAILED = 607;
        public static final int SERVICE_RESOLVED = 608;
        public static final int SERVICE_UPDATED = 609;
        public static final int SERVICE_UPDATE_FAILED = 610;

        NativeResponseCode() {
        }

        static {
            CODE_NAMES.put(SERVICE_DISCOVERY_FAILED, "SERVICE_DISCOVERY_FAILED");
            CODE_NAMES.put(SERVICE_FOUND, "SERVICE_FOUND");
            CODE_NAMES.put(SERVICE_LOST, "SERVICE_LOST");
            CODE_NAMES.put(SERVICE_REGISTRATION_FAILED, "SERVICE_REGISTRATION_FAILED");
            CODE_NAMES.put(SERVICE_REGISTERED, "SERVICE_REGISTERED");
            CODE_NAMES.put(SERVICE_RESOLUTION_FAILED, "SERVICE_RESOLUTION_FAILED");
            CODE_NAMES.put(SERVICE_RESOLVED, "SERVICE_RESOLVED");
            CODE_NAMES.put(SERVICE_UPDATED, "SERVICE_UPDATED");
            CODE_NAMES.put(SERVICE_UPDATE_FAILED, "SERVICE_UPDATE_FAILED");
            CODE_NAMES.put(SERVICE_GET_ADDR_FAILED, "SERVICE_GET_ADDR_FAILED");
            CODE_NAMES.put(SERVICE_GET_ADDR_SUCCESS, "SERVICE_GET_ADDR_SUCCESS");
        }

        static String nameOf(int code) {
            String name = (String) CODE_NAMES.get(code);
            if (name == null) {
                return Integer.toString(code);
            }
            return name;
        }
    }

    @VisibleForTesting
    public interface NsdSettings {
        boolean isEnabled();

        void putEnabledStatus(boolean z);

        void registerContentObserver(Uri uri, ContentObserver contentObserver);

        static NsdSettings makeDefault(Context context) {
            final ContentResolver resolver = context.getContentResolver();
            return new NsdSettings() {
                public boolean isEnabled() {
                    return Global.getInt(resolver, "nsd_on", 1) == 1;
                }

                public void putEnabledStatus(boolean isEnabled) {
                    Global.putInt(resolver, "nsd_on", isEnabled);
                }

                public void registerContentObserver(Uri uri, ContentObserver observer) {
                    resolver.registerContentObserver(uri, false, observer);
                }
            };
        }
    }

    private class NsdStateMachine extends StateMachine {
        private final DefaultState mDefaultState = new DefaultState();
        private final DisabledState mDisabledState = new DisabledState();
        private final EnabledState mEnabledState = new EnabledState();

        class DefaultState extends State {
            DefaultState() {
            }

            public boolean processMessage(Message msg) {
                String str;
                StringBuilder stringBuilder;
                switch (msg.what) {
                    case 69632:
                        if (msg.arg1 != 0) {
                            str = NsdService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Client connection failure, error=");
                            stringBuilder.append(msg.arg1);
                            Slog.e(str, stringBuilder.toString());
                            break;
                        }
                        AsyncChannel c = msg.obj;
                        Slog.d(NsdService.TAG, "New client listening to asynchronous messages");
                        c.sendMessage(69634);
                        NsdService.this.mClients.put(msg.replyTo, new ClientInfo(c, msg.replyTo));
                        break;
                    case 69633:
                        new AsyncChannel().connect(NsdService.this.mContext, NsdStateMachine.this.getHandler(), msg.replyTo);
                        break;
                    case 69636:
                        int i = msg.arg1;
                        if (i == 2) {
                            Slog.e(NsdService.TAG, "Send failed, client connection lost");
                        } else if (i != 4) {
                            str = NsdService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Client connection lost with reason: ");
                            stringBuilder.append(msg.arg1);
                            Slog.d(str, stringBuilder.toString());
                        } else {
                            Slog.d(NsdService.TAG, "Client disconnected");
                        }
                        ClientInfo cInfo = (ClientInfo) NsdService.this.mClients.get(msg.replyTo);
                        if (cInfo != null) {
                            cInfo.expungeAllRequests();
                            NsdService.this.mClients.remove(msg.replyTo);
                        }
                        if (NsdService.this.mClients.size() == 0) {
                            NsdService.this.mDaemon.stop();
                            break;
                        }
                        break;
                    case 393217:
                        NsdService.this.replyToMessage(msg, 393219, 0);
                        break;
                    case 393222:
                        NsdService.this.replyToMessage(msg, 393223, 0);
                        break;
                    case 393225:
                        NsdService.this.replyToMessage(msg, 393226, 0);
                        break;
                    case 393228:
                        NsdService.this.replyToMessage(msg, 393229, 0);
                        break;
                    case 393234:
                        NsdService.this.replyToMessage(msg, 393235, 0);
                        break;
                    default:
                        str = NsdService.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unhandled ");
                        stringBuilder2.append(msg);
                        Slog.e(str, stringBuilder2.toString());
                        return false;
                }
                return true;
            }
        }

        class DisabledState extends State {
            DisabledState() {
            }

            public void enter() {
                NsdService.this.sendNsdStateChangeBroadcast(false);
            }

            public boolean processMessage(Message msg) {
                if (msg.what != 393240) {
                    return false;
                }
                NsdStateMachine.this.transitionTo(NsdStateMachine.this.mEnabledState);
                return true;
            }
        }

        class EnabledState extends State {
            EnabledState() {
            }

            public void enter() {
                NsdService.this.sendNsdStateChangeBroadcast(true);
                if (NsdService.this.mClients.size() > 0) {
                    NsdService.this.mDaemon.start();
                }
            }

            public void exit() {
                if (NsdService.this.mClients.size() > 0) {
                    NsdService.this.mDaemon.stop();
                }
            }

            private boolean requestLimitReached(ClientInfo clientInfo) {
                if (clientInfo.mClientIds.size() < 10) {
                    return false;
                }
                String str = NsdService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exceeded max outstanding requests ");
                stringBuilder.append(clientInfo);
                Slog.d(str, stringBuilder.toString());
                return true;
            }

            private void storeRequestMap(int clientId, int globalId, ClientInfo clientInfo, int what) {
                clientInfo.mClientIds.put(clientId, globalId);
                clientInfo.mClientRequests.put(clientId, what);
                NsdService.this.mIdToClientInfoMap.put(globalId, clientInfo);
            }

            private void removeRequestMap(int clientId, int globalId, ClientInfo clientInfo) {
                clientInfo.mClientIds.delete(clientId);
                clientInfo.mClientRequests.delete(clientId);
                NsdService.this.mIdToClientInfoMap.remove(globalId);
            }

            public boolean processMessage(Message msg) {
                NsdServiceInfo servInfo;
                int id;
                String str;
                ClientInfo clientInfo;
                int id2;
                switch (msg.what) {
                    case 69632:
                        if (msg.arg1 == 0 && NsdService.this.mClients.size() == 0) {
                            NsdService.this.mDaemon.start();
                        }
                        return false;
                    case 69636:
                        return false;
                    case 393217:
                        Slog.d(NsdService.TAG, "Discover services");
                        servInfo = msg.obj;
                        ClientInfo clientInfo2 = (ClientInfo) NsdService.this.mClients.get(msg.replyTo);
                        if (!requestLimitReached(clientInfo2)) {
                            id = NsdService.this.getUniqueId();
                            if (!NsdService.this.discoverServices(id, servInfo.getServiceType())) {
                                NsdService.this.stopServiceDiscovery(id);
                                NsdService.this.replyToMessage(msg, 393219, 0);
                                break;
                            }
                            str = NsdService.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Discover ");
                            stringBuilder.append(msg.arg2);
                            stringBuilder.append(" ");
                            stringBuilder.append(id);
                            stringBuilder.append(servInfo.getServiceType());
                            Slog.d(str, stringBuilder.toString());
                            storeRequestMap(msg.arg2, id, clientInfo2, msg.what);
                            NsdService.this.replyToMessage(msg, 393218, (Object) servInfo);
                            break;
                        }
                        NsdService.this.replyToMessage(msg, 393219, 4);
                        break;
                    case 393222:
                        Slog.d(NsdService.TAG, "Stop service discovery");
                        clientInfo = (ClientInfo) NsdService.this.mClients.get(msg.replyTo);
                        try {
                            id2 = clientInfo.mClientIds.get(msg.arg2);
                            removeRequestMap(msg.arg2, id2, clientInfo);
                            if (!NsdService.this.stopServiceDiscovery(id2)) {
                                NsdService.this.replyToMessage(msg, 393223, 0);
                                break;
                            }
                            NsdService.this.replyToMessage(msg, 393224);
                            break;
                        } catch (NullPointerException e) {
                            NsdService.this.replyToMessage(msg, 393223, 0);
                            break;
                        }
                    case 393225:
                        Slog.d(NsdService.TAG, "Register service");
                        clientInfo = (ClientInfo) NsdService.this.mClients.get(msg.replyTo);
                        if (!requestLimitReached(clientInfo)) {
                            id = NsdService.this.getUniqueId();
                            if (!NsdService.this.registerService(id, (NsdServiceInfo) msg.obj)) {
                                NsdService.this.unregisterService(id);
                                NsdService.this.replyToMessage(msg, 393226, 0);
                                break;
                            }
                            str = NsdService.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Register ");
                            stringBuilder2.append(msg.arg2);
                            stringBuilder2.append(" ");
                            stringBuilder2.append(id);
                            Slog.d(str, stringBuilder2.toString());
                            storeRequestMap(msg.arg2, id, clientInfo, msg.what);
                            break;
                        }
                        NsdService.this.replyToMessage(msg, 393226, 4);
                        break;
                    case 393228:
                        Slog.d(NsdService.TAG, "unregister service");
                        clientInfo = (ClientInfo) NsdService.this.mClients.get(msg.replyTo);
                        try {
                            id2 = clientInfo.mClientIds.get(msg.arg2);
                            removeRequestMap(msg.arg2, id2, clientInfo);
                            if (!NsdService.this.unregisterService(id2)) {
                                NsdService.this.replyToMessage(msg, 393229, 0);
                                break;
                            }
                            NsdService.this.replyToMessage(msg, 393230);
                            break;
                        } catch (NullPointerException e2) {
                            NsdService.this.replyToMessage(msg, 393229, 0);
                            break;
                        }
                    case 393234:
                        Slog.d(NsdService.TAG, "Resolve service");
                        servInfo = msg.obj;
                        ClientInfo clientInfo3 = (ClientInfo) NsdService.this.mClients.get(msg.replyTo);
                        if (clientInfo3.mResolvedService == null) {
                            id2 = NsdService.this.getUniqueId();
                            if (!NsdService.this.resolveService(id2, servInfo)) {
                                NsdService.this.replyToMessage(msg, 393235, 0);
                                break;
                            }
                            clientInfo3.mResolvedService = new NsdServiceInfo();
                            storeRequestMap(msg.arg2, id2, clientInfo3, msg.what);
                            break;
                        }
                        NsdService.this.replyToMessage(msg, 393235, 3);
                        break;
                    case 393241:
                        NsdStateMachine.this.transitionTo(NsdStateMachine.this.mDisabledState);
                        break;
                    case 393242:
                        NativeEvent event = msg.obj;
                        if (!handleNativeEvent(event.code, event.raw, event.cooked)) {
                            return false;
                        }
                        break;
                    default:
                        return false;
                }
                return true;
            }

            private boolean handleNativeEvent(int code, String raw, String[] cooked) {
                String str = raw;
                int id = Integer.parseInt(cooked[1]);
                ClientInfo clientInfo = (ClientInfo) NsdService.this.mIdToClientInfoMap.get(id);
                if (clientInfo == null) {
                    String name = NativeResponseCode.nameOf(code);
                    Slog.e(NsdService.TAG, String.format("id %d for %s has no client mapping", new Object[]{Integer.valueOf(id), name}));
                    return false;
                }
                int clientId = clientInfo.getClientId(id);
                String name2;
                if (clientId < 0) {
                    name2 = NativeResponseCode.nameOf(code);
                    Slog.d(NsdService.TAG, String.format("Notification %s for listener id %d that is no longer active", new Object[]{name2, Integer.valueOf(id)}));
                    return false;
                }
                name2 = NativeResponseCode.nameOf(code);
                Slog.d(NsdService.TAG, String.format("Native daemon message %s: %s", new Object[]{name2, str}));
                switch (code) {
                    case NativeResponseCode.SERVICE_DISCOVERY_FAILED /*602*/:
                        clientInfo.mChannel.sendMessage(393219, 0, clientId);
                        break;
                    case NativeResponseCode.SERVICE_FOUND /*603*/:
                        clientInfo.mChannel.sendMessage(393220, 0, clientId, new NsdServiceInfo(cooked[2], cooked[3]));
                        break;
                    case NativeResponseCode.SERVICE_LOST /*604*/:
                        clientInfo.mChannel.sendMessage(393221, 0, clientId, new NsdServiceInfo(cooked[2], cooked[3]));
                        break;
                    case NativeResponseCode.SERVICE_REGISTRATION_FAILED /*605*/:
                        clientInfo.mChannel.sendMessage(393226, 0, clientId);
                        break;
                    case NativeResponseCode.SERVICE_REGISTERED /*606*/:
                        clientInfo.mChannel.sendMessage(393227, id, clientId, new NsdServiceInfo(cooked[2], null));
                        break;
                    case NativeResponseCode.SERVICE_RESOLUTION_FAILED /*607*/:
                        NsdServiceInfo nsdServiceInfo = null;
                        NsdService.this.stopResolveService(id);
                        removeRequestMap(clientId, id, clientInfo);
                        clientInfo.mResolvedService = nsdServiceInfo;
                        clientInfo.mChannel.sendMessage(393235, 0, clientId);
                        break;
                    case NativeResponseCode.SERVICE_RESOLVED /*608*/:
                        int index = 0;
                        while (index < cooked[2].length() && cooked[2].charAt(index) != '.') {
                            if (cooked[2].charAt(index) == '\\') {
                                index++;
                            }
                            index++;
                        }
                        if (index < cooked[2].length()) {
                            String name3 = cooked[2].substring(0, index);
                            String type = cooked[2].substring(index).replace(".local.", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                            clientInfo.mResolvedService.setServiceName(NsdService.this.unescape(name3));
                            clientInfo.mResolvedService.setServiceType(type);
                            clientInfo.mResolvedService.setPort(Integer.parseInt(cooked[4]));
                            clientInfo.mResolvedService.setTxtRecords(cooked[6]);
                            NsdService.this.stopResolveService(id);
                            removeRequestMap(clientId, id, clientInfo);
                            int id2 = NsdService.this.getUniqueId();
                            if (!NsdService.this.getAddrInfo(id2, cooked[3])) {
                                clientInfo.mChannel.sendMessage(393235, 0, clientId);
                                clientInfo.mResolvedService = null;
                                break;
                            }
                            storeRequestMap(clientId, id2, clientInfo, 393234);
                            break;
                        }
                        String str2 = NsdService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Invalid service found ");
                        stringBuilder.append(str);
                        Slog.e(str2, stringBuilder.toString());
                        break;
                        break;
                    case NativeResponseCode.SERVICE_UPDATED /*609*/:
                    case NativeResponseCode.SERVICE_UPDATE_FAILED /*610*/:
                        break;
                    case NativeResponseCode.SERVICE_GET_ADDR_FAILED /*611*/:
                        NsdService.this.stopGetAddrInfo(id);
                        removeRequestMap(clientId, id, clientInfo);
                        clientInfo.mResolvedService = null;
                        clientInfo.mChannel.sendMessage(393235, 0, clientId);
                        break;
                    case NativeResponseCode.SERVICE_GET_ADDR_SUCCESS /*612*/:
                        try {
                            clientInfo.mResolvedService.setHost(InetAddress.getByName(cooked[4]));
                            clientInfo.mChannel.sendMessage(393236, 0, clientId, clientInfo.mResolvedService);
                        } catch (UnknownHostException e) {
                            clientInfo.mChannel.sendMessage(393235, 0, clientId);
                        }
                        NsdService.this.stopGetAddrInfo(id);
                        removeRequestMap(clientId, id, clientInfo);
                        clientInfo.mResolvedService = null;
                        break;
                    default:
                        return false;
                }
                return true;
            }
        }

        protected String getWhatToString(int what) {
            return NsdManager.nameOf(what);
        }

        private void registerForNsdSetting() {
            ContentObserver contentObserver = new ContentObserver(getHandler()) {
                public void onChange(boolean selfChange) {
                    NsdService.this.notifyEnabled(NsdService.this.isNsdEnabled());
                }
            };
            NsdService.this.mNsdSettings.registerContentObserver(Global.getUriFor("nsd_on"), contentObserver);
        }

        NsdStateMachine(String name, Handler handler) {
            super(name, handler);
            addState(this.mDefaultState);
            addState(this.mDisabledState, this.mDefaultState);
            addState(this.mEnabledState, this.mDefaultState);
            setInitialState(NsdService.this.isNsdEnabled() ? this.mEnabledState : this.mDisabledState);
            setLogRecSize(25);
            registerForNsdSetting();
        }
    }

    class NativeCallbackReceiver implements INativeDaemonConnectorCallbacks {
        private final CountDownLatch connected = new CountDownLatch(1);

        NativeCallbackReceiver() {
        }

        public void awaitConnection() throws InterruptedException {
            this.connected.await();
        }

        public void onDaemonConnected() {
            this.connected.countDown();
        }

        public boolean onCheckHoldWakeLock(int code) {
            return false;
        }

        public boolean onEvent(int code, String raw, String[] cooked) {
            NsdService.this.mNsdStateMachine.sendMessage(393242, new NativeEvent(code, raw, cooked));
            return true;
        }
    }

    private String unescape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\') {
                i++;
                String str;
                StringBuilder stringBuilder;
                if (i >= s.length()) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected end of escape sequence in: ");
                    stringBuilder.append(s);
                    Slog.e(str, stringBuilder.toString());
                    break;
                }
                c = s.charAt(i);
                if (!(c == '.' || c == '\\')) {
                    if (i + 2 >= s.length()) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unexpected end of escape sequence in: ");
                        stringBuilder.append(s);
                        Slog.e(str, stringBuilder.toString());
                        break;
                    }
                    c = (char) ((((c - 48) * 100) + ((s.charAt(i + 1) - 48) * 10)) + (s.charAt(i + 2) - 48));
                    i += 2;
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    @VisibleForTesting
    NsdService(Context ctx, NsdSettings settings, Handler handler, DaemonConnectionSupplier fn) {
        this.mContext = ctx;
        this.mNsdSettings = settings;
        this.mNsdStateMachine = new NsdStateMachine(TAG, handler);
        this.mNsdStateMachine.start();
        this.mDaemonCallback = new NativeCallbackReceiver();
        this.mDaemon = fn.get(this.mDaemonCallback);
    }

    public static NsdService create(Context context) throws InterruptedException {
        NsdSettings settings = NsdSettings.makeDefault(context);
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        NsdService service = new NsdService(context, settings, new Handler(thread.getLooper()), -$$Lambda$1xUIIN0BU8izGcnYWT-VzczLBFU.INSTANCE);
        service.mDaemonCallback.awaitConnection();
        return service;
    }

    public Messenger getMessenger() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERNET", TAG);
        return new Messenger(this.mNsdStateMachine.getHandler());
    }

    public void setEnabled(boolean isEnabled) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        this.mNsdSettings.putEnabledStatus(isEnabled);
        notifyEnabled(isEnabled);
    }

    private void notifyEnabled(boolean isEnabled) {
        this.mNsdStateMachine.sendMessage(isEnabled ? 393240 : 393241);
    }

    private void sendNsdStateChangeBroadcast(boolean isEnabled) {
        Intent intent = new Intent("android.net.nsd.STATE_CHANGED");
        intent.addFlags(67108864);
        intent.putExtra("nsd_state", isEnabled ? 2 : 1);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private boolean isNsdEnabled() {
        boolean ret = this.mNsdSettings.isEnabled();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Network service discovery is ");
        stringBuilder.append(ret ? "enabled" : "disabled");
        Slog.d(str, stringBuilder.toString());
        return ret;
    }

    private int getUniqueId() {
        int i = this.mUniqueId + 1;
        this.mUniqueId = i;
        if (i != 0) {
            return this.mUniqueId;
        }
        i = this.mUniqueId + 1;
        this.mUniqueId = i;
        return i;
    }

    private boolean registerService(int regId, NsdServiceInfo service) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("registerService: ");
        stringBuilder.append(regId);
        stringBuilder.append(" ");
        stringBuilder.append(service);
        Slog.d(str, stringBuilder.toString());
        str = service.getServiceName();
        String type = service.getServiceType();
        int port = service.getPort();
        String record = Base64.encodeToString(service.getTxtRecord(), 0).replace("\n", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        return this.mDaemon.execute("register", Integer.valueOf(regId), str, type, Integer.valueOf(port), record);
    }

    private boolean unregisterService(int regId) {
        return this.mDaemon.execute("stop-register", Integer.valueOf(regId));
    }

    private boolean updateService(int regId, DnsSdTxtRecord t) {
        if (t == null) {
            return false;
        }
        return this.mDaemon.execute("update", Integer.valueOf(regId), Integer.valueOf(t.size()), t.getRawData());
    }

    private boolean discoverServices(int discoveryId, String serviceType) {
        return this.mDaemon.execute("discover", Integer.valueOf(discoveryId), serviceType);
    }

    private boolean stopServiceDiscovery(int discoveryId) {
        return this.mDaemon.execute("stop-discover", Integer.valueOf(discoveryId));
    }

    private boolean resolveService(int resolveId, NsdServiceInfo service) {
        String name = service.getServiceName();
        String type = service.getServiceType();
        return this.mDaemon.execute("resolve", Integer.valueOf(resolveId), name, type, "local.");
    }

    private boolean stopResolveService(int resolveId) {
        return this.mDaemon.execute("stop-resolve", Integer.valueOf(resolveId));
    }

    private boolean getAddrInfo(int resolveId, String hostname) {
        return this.mDaemon.execute("getaddrinfo", Integer.valueOf(resolveId), hostname);
    }

    private boolean stopGetAddrInfo(int resolveId) {
        return this.mDaemon.execute("stop-getaddrinfo", Integer.valueOf(resolveId));
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            for (ClientInfo client : this.mClients.values()) {
                pw.println("Client Info");
                pw.println(client);
            }
            this.mNsdStateMachine.dump(fd, pw, args);
        }
    }

    private Message obtainMessage(Message srcMsg) {
        Message msg = Message.obtain();
        msg.arg2 = srcMsg.arg2;
        return msg;
    }

    private void replyToMessage(Message msg, int what) {
        if (msg.replyTo != null) {
            Message dstMsg = obtainMessage(msg);
            dstMsg.what = what;
            this.mReplyChannel.replyToMessage(msg, dstMsg);
        }
    }

    private void replyToMessage(Message msg, int what, int arg1) {
        if (msg.replyTo != null) {
            Message dstMsg = obtainMessage(msg);
            dstMsg.what = what;
            dstMsg.arg1 = arg1;
            this.mReplyChannel.replyToMessage(msg, dstMsg);
        }
    }

    private void replyToMessage(Message msg, int what, Object obj) {
        if (msg.replyTo != null) {
            Message dstMsg = obtainMessage(msg);
            dstMsg.what = what;
            dstMsg.obj = obj;
            this.mReplyChannel.replyToMessage(msg, dstMsg);
        }
    }
}
