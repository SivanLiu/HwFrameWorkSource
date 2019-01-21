package com.android.server.media;

import android.app.ActivityManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioRoutesInfo;
import android.media.IAudioRoutesObserver;
import android.media.IAudioService;
import android.media.IMediaRouterClient;
import android.media.IMediaRouterService.Stub;
import android.media.MediaRouterClientState;
import android.media.MediaRouterClientState.RouteInfo;
import android.media.RemoteDisplayState;
import android.media.RemoteDisplayState.RemoteDisplayInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.IntArray;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import com.android.internal.util.DumpUtils;
import com.android.server.Watchdog;
import com.android.server.Watchdog.Monitor;
import com.android.server.media.RemoteDisplayProviderWatcher.Callback;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public final class MediaRouterService extends Stub implements Monitor {
    static final long CONNECTED_TIMEOUT = 60000;
    static final long CONNECTING_TIMEOUT = 5000;
    private static final boolean DEBUG = true;
    private static final String TAG = "MediaRouterService";
    static final long WAIT_MS = 500;
    BluetoothDevice mActiveBluetoothDevice;
    private final IntArray mActivePlayerMinPriorityQueue = new IntArray();
    private final IntArray mActivePlayerUidMinPriorityQueue = new IntArray();
    private final ArrayMap<IBinder, ClientRecord> mAllClientRecords = new ArrayMap();
    private final AudioPlayerStateMonitor mAudioPlayerStateMonitor;
    int mAudioRouteMainType = 0;
    private final IAudioService mAudioService;
    private ArrayList<BluetoothDevice> mConnectedBTDevicesList = new ArrayList();
    private final Context mContext;
    private int mCurrentUserId = -1;
    boolean mGlobalBluetoothA2dpOn = false;
    private final Handler mHandler = new Handler();
    private final Object mLock = new Object();
    private final BroadcastReceiver mReceiver = new MediaRouterServiceBroadcastReceiver();
    private final SparseArray<UserRecord> mUserRecords = new SparseArray();

    final class ClientRecord implements DeathRecipient {
        public boolean mActiveScan;
        public final IMediaRouterClient mClient;
        public final String mPackageName;
        public final int mPid;
        public int mRouteTypes;
        public String mSelectedRouteId;
        public final boolean mTrusted;
        public final int mUid;
        public final UserRecord mUserRecord;

        public ClientRecord(UserRecord userRecord, IMediaRouterClient client, int uid, int pid, String packageName, boolean trusted) {
            this.mUserRecord = userRecord;
            this.mClient = client;
            this.mUid = uid;
            this.mPid = pid;
            this.mPackageName = packageName;
            this.mTrusted = trusted;
        }

        public void dispose() {
            this.mClient.asBinder().unlinkToDeath(this, 0);
        }

        public void binderDied() {
            MediaRouterService.this.clientDied(this);
        }

        MediaRouterClientState getState() {
            return this.mTrusted ? this.mUserRecord.mRouterState : null;
        }

        public void dump(PrintWriter pw, String prefix) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append(this);
            pw.println(stringBuilder.toString());
            String indent = new StringBuilder();
            indent.append(prefix);
            indent.append("  ");
            indent = indent.toString();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append("mTrusted=");
            stringBuilder2.append(this.mTrusted);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append("mRouteTypes=0x");
            stringBuilder2.append(Integer.toHexString(this.mRouteTypes));
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append("mActiveScan=");
            stringBuilder2.append(this.mActiveScan);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append("mSelectedRouteId=");
            stringBuilder2.append(this.mSelectedRouteId);
            pw.println(stringBuilder2.toString());
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Client ");
            stringBuilder.append(this.mPackageName);
            stringBuilder.append(" (pid ");
            stringBuilder.append(this.mPid);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    final class MediaRouterServiceBroadcastReceiver extends BroadcastReceiver {
        MediaRouterServiceBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED")) {
                BluetoothDevice btDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                synchronized (MediaRouterService.this.mLock) {
                    MediaRouterService.this.mActiveBluetoothDevice = btDevice;
                    MediaRouterService.this.mGlobalBluetoothA2dpOn = btDevice != null;
                }
            }
        }
    }

    final class UserRecord {
        public final ArrayList<ClientRecord> mClientRecords = new ArrayList();
        public final UserHandler mHandler;
        public MediaRouterClientState mRouterState;
        public final int mUserId;

        public UserRecord(int userId) {
            this.mUserId = userId;
            this.mHandler = new UserHandler(MediaRouterService.this, this);
        }

        public void dump(final PrintWriter pw, String prefix) {
            StringBuilder stringBuilder;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(prefix);
            stringBuilder2.append(this);
            pw.println(stringBuilder2.toString());
            String indent = new StringBuilder();
            indent.append(prefix);
            indent.append("  ");
            indent = indent.toString();
            int clientCount = this.mClientRecords.size();
            if (clientCount != 0) {
                for (int i = 0; i < clientCount; i++) {
                    ((ClientRecord) this.mClientRecords.get(i)).dump(pw, indent);
                }
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(indent);
                stringBuilder.append("<no clients>");
                pw.println(stringBuilder.toString());
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append("State");
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(indent);
            stringBuilder.append("mRouterState=");
            stringBuilder.append(this.mRouterState);
            pw.println(stringBuilder.toString());
            if (!this.mHandler.runWithScissors(new Runnable() {
                public void run() {
                    UserRecord.this.mHandler.dump(pw, indent);
                }
            }, 1000)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(indent);
                stringBuilder.append("<could not dump handler state>");
                pw.println(stringBuilder.toString());
            }
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("User ");
            stringBuilder.append(this.mUserId);
            return stringBuilder.toString();
        }
    }

    static final class UserHandler extends Handler implements Callback, RemoteDisplayProviderProxy.Callback {
        private static final int MSG_CONNECTION_TIMED_OUT = 9;
        public static final int MSG_REQUEST_SET_VOLUME = 6;
        public static final int MSG_REQUEST_UPDATE_VOLUME = 7;
        public static final int MSG_SELECT_ROUTE = 4;
        public static final int MSG_START = 1;
        public static final int MSG_STOP = 2;
        public static final int MSG_UNSELECT_ROUTE = 5;
        private static final int MSG_UPDATE_CLIENT_STATE = 8;
        public static final int MSG_UPDATE_DISCOVERY_REQUEST = 3;
        private static final int PHASE_CONNECTED = 2;
        private static final int PHASE_CONNECTING = 1;
        private static final int PHASE_NOT_AVAILABLE = -1;
        private static final int PHASE_NOT_CONNECTED = 0;
        private static final int TIMEOUT_REASON_CONNECTION_LOST = 2;
        private static final int TIMEOUT_REASON_NOT_AVAILABLE = 1;
        private static final int TIMEOUT_REASON_WAITING_FOR_CONNECTED = 4;
        private static final int TIMEOUT_REASON_WAITING_FOR_CONNECTING = 3;
        private boolean mClientStateUpdateScheduled;
        private int mConnectionPhase = -1;
        private int mConnectionTimeoutReason;
        private long mConnectionTimeoutStartTime;
        private int mDiscoveryMode = 0;
        private final ArrayList<ProviderRecord> mProviderRecords = new ArrayList();
        private boolean mRunning;
        private RouteRecord mSelectedRouteRecord;
        private final MediaRouterService mService;
        private final ArrayList<IMediaRouterClient> mTempClients = new ArrayList();
        private final UserRecord mUserRecord;
        private final RemoteDisplayProviderWatcher mWatcher;

        static final class ProviderRecord {
            private RemoteDisplayState mDescriptor;
            private final RemoteDisplayProviderProxy mProvider;
            private final ArrayList<RouteRecord> mRoutes = new ArrayList();
            private final String mUniquePrefix;

            public ProviderRecord(RemoteDisplayProviderProxy provider) {
                this.mProvider = provider;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(provider.getFlattenedComponentName());
                stringBuilder.append(":");
                this.mUniquePrefix = stringBuilder.toString();
            }

            public RemoteDisplayProviderProxy getProvider() {
                return this.mProvider;
            }

            public String getUniquePrefix() {
                return this.mUniquePrefix;
            }

            public boolean updateDescriptor(RemoteDisplayState descriptor) {
                boolean changed = false;
                if (this.mDescriptor != descriptor) {
                    this.mDescriptor = descriptor;
                    int targetIndex = 0;
                    if (descriptor != null) {
                        if (descriptor.isValid()) {
                            List<RemoteDisplayInfo> routeDescriptors = descriptor.displays;
                            int routeCount = routeDescriptors.size();
                            for (int i = 0; i < routeCount; i++) {
                                RemoteDisplayInfo routeDescriptor = (RemoteDisplayInfo) routeDescriptors.get(i);
                                String descriptorId = routeDescriptor.id;
                                int sourceIndex = findRouteByDescriptorId(descriptorId);
                                if (sourceIndex < 0) {
                                    RouteRecord route = new RouteRecord(this, descriptorId, assignRouteUniqueId(descriptorId));
                                    int targetIndex2 = targetIndex + 1;
                                    this.mRoutes.add(targetIndex, route);
                                    route.updateDescriptor(routeDescriptor);
                                    changed = true;
                                    targetIndex = targetIndex2;
                                } else if (sourceIndex < targetIndex) {
                                    String str = MediaRouterService.TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("Ignoring route descriptor with duplicate id: ");
                                    stringBuilder.append(routeDescriptor);
                                    Slog.w(str, stringBuilder.toString());
                                } else {
                                    RouteRecord route2 = (RouteRecord) this.mRoutes.get(sourceIndex);
                                    int targetIndex3 = targetIndex + 1;
                                    Collections.swap(this.mRoutes, sourceIndex, targetIndex);
                                    changed |= route2.updateDescriptor(routeDescriptor);
                                    targetIndex = targetIndex3;
                                }
                            }
                        } else {
                            String str2 = MediaRouterService.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Ignoring invalid descriptor from media route provider: ");
                            stringBuilder2.append(this.mProvider.getFlattenedComponentName());
                            Slog.w(str2, stringBuilder2.toString());
                        }
                    }
                    for (int i2 = this.mRoutes.size() - 1; i2 >= targetIndex; i2--) {
                        ((RouteRecord) this.mRoutes.remove(i2)).updateDescriptor(null);
                        changed = true;
                    }
                }
                return changed;
            }

            public void appendClientState(MediaRouterClientState state) {
                int routeCount = this.mRoutes.size();
                for (int i = 0; i < routeCount; i++) {
                    state.routes.add(((RouteRecord) this.mRoutes.get(i)).getInfo());
                }
            }

            public RouteRecord findRouteByUniqueId(String uniqueId) {
                int routeCount = this.mRoutes.size();
                for (int i = 0; i < routeCount; i++) {
                    RouteRecord route = (RouteRecord) this.mRoutes.get(i);
                    if (route.getUniqueId().equals(uniqueId)) {
                        return route;
                    }
                }
                return null;
            }

            private int findRouteByDescriptorId(String descriptorId) {
                int routeCount = this.mRoutes.size();
                for (int i = 0; i < routeCount; i++) {
                    if (((RouteRecord) this.mRoutes.get(i)).getDescriptorId().equals(descriptorId)) {
                        return i;
                    }
                }
                return -1;
            }

            public void dump(PrintWriter pw, String prefix) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append(this);
                pw.println(stringBuilder.toString());
                String indent = new StringBuilder();
                indent.append(prefix);
                indent.append("  ");
                indent = indent.toString();
                this.mProvider.dump(pw, indent);
                int routeCount = this.mRoutes.size();
                if (routeCount != 0) {
                    for (int i = 0; i < routeCount; i++) {
                        ((RouteRecord) this.mRoutes.get(i)).dump(pw, indent);
                    }
                    return;
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(indent);
                stringBuilder2.append("<no routes>");
                pw.println(stringBuilder2.toString());
            }

            public String toString() {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Provider ");
                stringBuilder.append(this.mProvider.getFlattenedComponentName());
                return stringBuilder.toString();
            }

            private String assignRouteUniqueId(String descriptorId) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this.mUniquePrefix);
                stringBuilder.append(descriptorId);
                return stringBuilder.toString();
            }
        }

        static final class RouteRecord {
            private RemoteDisplayInfo mDescriptor;
            private final String mDescriptorId;
            private RouteInfo mImmutableInfo;
            private final RouteInfo mMutableInfo;
            private final ProviderRecord mProviderRecord;

            public RouteRecord(ProviderRecord providerRecord, String descriptorId, String uniqueId) {
                this.mProviderRecord = providerRecord;
                this.mDescriptorId = descriptorId;
                this.mMutableInfo = new RouteInfo(uniqueId);
            }

            public RemoteDisplayProviderProxy getProvider() {
                return this.mProviderRecord.getProvider();
            }

            public ProviderRecord getProviderRecord() {
                return this.mProviderRecord;
            }

            public String getDescriptorId() {
                return this.mDescriptorId;
            }

            public String getUniqueId() {
                return this.mMutableInfo.id;
            }

            public RouteInfo getInfo() {
                if (this.mImmutableInfo == null) {
                    this.mImmutableInfo = new RouteInfo(this.mMutableInfo);
                }
                return this.mImmutableInfo;
            }

            public boolean isValid() {
                return this.mDescriptor != null;
            }

            public boolean isEnabled() {
                return this.mMutableInfo.enabled;
            }

            public int getStatus() {
                return this.mMutableInfo.statusCode;
            }

            public boolean updateDescriptor(RemoteDisplayInfo descriptor) {
                boolean changed = false;
                if (this.mDescriptor != descriptor) {
                    this.mDescriptor = descriptor;
                    if (descriptor != null) {
                        String name = computeName(descriptor);
                        if (!Objects.equals(this.mMutableInfo.name, name)) {
                            this.mMutableInfo.name = name;
                            changed = true;
                        }
                        String description = computeDescription(descriptor);
                        if (!Objects.equals(this.mMutableInfo.description, description)) {
                            this.mMutableInfo.description = description;
                            changed = true;
                        }
                        int supportedTypes = computeSupportedTypes(descriptor);
                        if (this.mMutableInfo.supportedTypes != supportedTypes) {
                            this.mMutableInfo.supportedTypes = supportedTypes;
                            changed = true;
                        }
                        boolean enabled = computeEnabled(descriptor);
                        if (this.mMutableInfo.enabled != enabled) {
                            this.mMutableInfo.enabled = enabled;
                            changed = true;
                        }
                        int statusCode = computeStatusCode(descriptor);
                        if (this.mMutableInfo.statusCode != statusCode) {
                            this.mMutableInfo.statusCode = statusCode;
                            changed = true;
                        }
                        int playbackType = computePlaybackType(descriptor);
                        if (this.mMutableInfo.playbackType != playbackType) {
                            this.mMutableInfo.playbackType = playbackType;
                            changed = true;
                        }
                        int playbackStream = computePlaybackStream(descriptor);
                        if (this.mMutableInfo.playbackStream != playbackStream) {
                            this.mMutableInfo.playbackStream = playbackStream;
                            changed = true;
                        }
                        int volume = computeVolume(descriptor);
                        if (this.mMutableInfo.volume != volume) {
                            this.mMutableInfo.volume = volume;
                            changed = true;
                        }
                        int volumeMax = computeVolumeMax(descriptor);
                        if (this.mMutableInfo.volumeMax != volumeMax) {
                            this.mMutableInfo.volumeMax = volumeMax;
                            changed = true;
                        }
                        int volumeHandling = computeVolumeHandling(descriptor);
                        if (this.mMutableInfo.volumeHandling != volumeHandling) {
                            this.mMutableInfo.volumeHandling = volumeHandling;
                            changed = true;
                        }
                        int presentationDisplayId = computePresentationDisplayId(descriptor);
                        if (this.mMutableInfo.presentationDisplayId != presentationDisplayId) {
                            this.mMutableInfo.presentationDisplayId = presentationDisplayId;
                            changed = true;
                        }
                    }
                }
                if (changed) {
                    this.mImmutableInfo = null;
                }
                return changed;
            }

            public void dump(PrintWriter pw, String prefix) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append(this);
                pw.println(stringBuilder.toString());
                String indent = new StringBuilder();
                indent.append(prefix);
                indent.append("  ");
                indent = indent.toString();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(indent);
                stringBuilder2.append("mMutableInfo=");
                stringBuilder2.append(this.mMutableInfo);
                pw.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(indent);
                stringBuilder2.append("mDescriptorId=");
                stringBuilder2.append(this.mDescriptorId);
                pw.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(indent);
                stringBuilder2.append("mDescriptor=");
                stringBuilder2.append(this.mDescriptor);
                pw.println(stringBuilder2.toString());
            }

            public String toString() {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Route ");
                stringBuilder.append(this.mMutableInfo.name);
                stringBuilder.append(" (");
                stringBuilder.append(this.mMutableInfo.id);
                stringBuilder.append(")");
                return stringBuilder.toString();
            }

            private static String computeName(RemoteDisplayInfo descriptor) {
                return descriptor.name;
            }

            private static String computeDescription(RemoteDisplayInfo descriptor) {
                String description = descriptor.description;
                return TextUtils.isEmpty(description) ? null : description;
            }

            private static int computeSupportedTypes(RemoteDisplayInfo descriptor) {
                return 7;
            }

            private static boolean computeEnabled(RemoteDisplayInfo descriptor) {
                switch (descriptor.status) {
                    case 2:
                    case 3:
                    case 4:
                        return true;
                    default:
                        return false;
                }
            }

            private static int computeStatusCode(RemoteDisplayInfo descriptor) {
                switch (descriptor.status) {
                    case 0:
                        return 4;
                    case 1:
                        return 5;
                    case 2:
                        return 3;
                    case 3:
                        return 2;
                    case 4:
                        return 6;
                    default:
                        return 0;
                }
            }

            private static int computePlaybackType(RemoteDisplayInfo descriptor) {
                return 1;
            }

            private static int computePlaybackStream(RemoteDisplayInfo descriptor) {
                return 3;
            }

            private static int computeVolume(RemoteDisplayInfo descriptor) {
                int volume = descriptor.volume;
                int volumeMax = descriptor.volumeMax;
                if (volume < 0) {
                    return 0;
                }
                if (volume > volumeMax) {
                    return volumeMax;
                }
                return volume;
            }

            private static int computeVolumeMax(RemoteDisplayInfo descriptor) {
                int volumeMax = descriptor.volumeMax;
                return volumeMax > 0 ? volumeMax : 0;
            }

            private static int computeVolumeHandling(RemoteDisplayInfo descriptor) {
                if (descriptor.volumeHandling != 1) {
                    return 0;
                }
                return 1;
            }

            private static int computePresentationDisplayId(RemoteDisplayInfo descriptor) {
                int displayId = descriptor.presentationDisplayId;
                return displayId < 0 ? -1 : displayId;
            }
        }

        public UserHandler(MediaRouterService service, UserRecord userRecord) {
            super(Looper.getMainLooper(), null, true);
            this.mService = service;
            this.mUserRecord = userRecord;
            this.mWatcher = new RemoteDisplayProviderWatcher(service.mContext, this, this, this.mUserRecord.mUserId);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    start();
                    return;
                case 2:
                    stop();
                    return;
                case 3:
                    updateDiscoveryRequest();
                    return;
                case 4:
                    selectRoute((String) msg.obj);
                    return;
                case 5:
                    unselectRoute((String) msg.obj);
                    return;
                case 6:
                    requestSetVolume((String) msg.obj, msg.arg1);
                    return;
                case 7:
                    requestUpdateVolume((String) msg.obj, msg.arg1);
                    return;
                case 8:
                    updateClientState();
                    return;
                case 9:
                    connectionTimedOut();
                    return;
                default:
                    return;
            }
        }

        public void dump(PrintWriter pw, String prefix) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("Handler");
            pw.println(stringBuilder.toString());
            String indent = new StringBuilder();
            indent.append(prefix);
            indent.append("  ");
            indent = indent.toString();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append("mRunning=");
            stringBuilder2.append(this.mRunning);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append("mDiscoveryMode=");
            stringBuilder2.append(this.mDiscoveryMode);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append("mSelectedRouteRecord=");
            stringBuilder2.append(this.mSelectedRouteRecord);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append("mConnectionPhase=");
            stringBuilder2.append(this.mConnectionPhase);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append("mConnectionTimeoutReason=");
            stringBuilder2.append(this.mConnectionTimeoutReason);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(indent);
            stringBuilder2.append("mConnectionTimeoutStartTime=");
            stringBuilder2.append(this.mConnectionTimeoutReason != 0 ? TimeUtils.formatUptime(this.mConnectionTimeoutStartTime) : "<n/a>");
            pw.println(stringBuilder2.toString());
            this.mWatcher.dump(pw, prefix);
            int providerCount = this.mProviderRecords.size();
            if (providerCount != 0) {
                for (int i = 0; i < providerCount; i++) {
                    ((ProviderRecord) this.mProviderRecords.get(i)).dump(pw, prefix);
                }
                return;
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(indent);
            stringBuilder3.append("<no providers>");
            pw.println(stringBuilder3.toString());
        }

        private void start() {
            if (!this.mRunning) {
                this.mRunning = true;
                this.mWatcher.start();
            }
        }

        private void stop() {
            if (this.mRunning) {
                this.mRunning = false;
                unselectSelectedRoute();
                this.mWatcher.stop();
            }
        }

        /* JADX WARNING: Missing block: B:10:0x002d, code skipped:
            if ((r1 & 4) == 0) goto L_0x0035;
     */
        /* JADX WARNING: Missing block: B:11:0x002f, code skipped:
            if (r5 == false) goto L_0x0033;
     */
        /* JADX WARNING: Missing block: B:12:0x0031, code skipped:
            r0 = 2;
     */
        /* JADX WARNING: Missing block: B:13:0x0033, code skipped:
            r0 = 1;
     */
        /* JADX WARNING: Missing block: B:14:0x0035, code skipped:
            r0 = 0;
     */
        /* JADX WARNING: Missing block: B:16:0x0038, code skipped:
            if (r8.mDiscoveryMode == r0) goto L_?;
     */
        /* JADX WARNING: Missing block: B:17:0x003a, code skipped:
            r8.mDiscoveryMode = r0;
            r2 = r8.mProviderRecords.size();
     */
        /* JADX WARNING: Missing block: B:18:0x0043, code skipped:
            r3 = r4;
     */
        /* JADX WARNING: Missing block: B:19:0x0044, code skipped:
            if (r3 >= r2) goto L_0x005a;
     */
        /* JADX WARNING: Missing block: B:20:0x0046, code skipped:
            ((com.android.server.media.MediaRouterService.UserHandler.ProviderRecord) r8.mProviderRecords.get(r3)).getProvider().setDiscoveryMode(r8.mDiscoveryMode);
            r4 = r3 + 1;
     */
        /* JADX WARNING: Missing block: B:29:?, code skipped:
            return;
     */
        /* JADX WARNING: Missing block: B:30:?, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void updateDiscoveryRequest() {
            synchronized (this.mService.mLock) {
                boolean activeScan;
                int i;
                try {
                    int i2 = 0;
                    activeScan = false;
                    boolean activeScan2 = false;
                    i = 0;
                    while (i < this.mUserRecord.mClientRecords.size()) {
                        try {
                            ClientRecord clientRecord = (ClientRecord) this.mUserRecord.mClientRecords.get(i);
                            activeScan2 |= clientRecord.mRouteTypes;
                            activeScan |= clientRecord.mActiveScan;
                            i++;
                        } catch (Throwable th) {
                            i = th;
                            throw i;
                        }
                    }
                } catch (Throwable th2) {
                    activeScan = false;
                    int routeTypes = 0;
                    i = th2;
                    throw i;
                }
            }
        }

        private void selectRoute(String routeId) {
            if (routeId == null) {
                return;
            }
            if (this.mSelectedRouteRecord == null || !routeId.equals(this.mSelectedRouteRecord.getUniqueId())) {
                RouteRecord routeRecord = findRouteRecord(routeId);
                if (routeRecord != null) {
                    unselectSelectedRoute();
                    String str = MediaRouterService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Selected route:");
                    stringBuilder.append(routeRecord);
                    Slog.i(str, stringBuilder.toString());
                    this.mSelectedRouteRecord = routeRecord;
                    checkSelectedRouteState();
                    routeRecord.getProvider().setSelectedDisplay(routeRecord.getDescriptorId());
                    scheduleUpdateClientState();
                }
            }
        }

        private void unselectRoute(String routeId) {
            if (routeId != null && this.mSelectedRouteRecord != null && routeId.equals(this.mSelectedRouteRecord.getUniqueId())) {
                unselectSelectedRoute();
            }
        }

        private void unselectSelectedRoute() {
            if (this.mSelectedRouteRecord != null) {
                String str = MediaRouterService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unselected route:");
                stringBuilder.append(this.mSelectedRouteRecord);
                Slog.i(str, stringBuilder.toString());
                this.mSelectedRouteRecord.getProvider().setSelectedDisplay(null);
                this.mSelectedRouteRecord = null;
                checkSelectedRouteState();
                scheduleUpdateClientState();
            }
        }

        private void requestSetVolume(String routeId, int volume) {
            if (this.mSelectedRouteRecord != null && routeId.equals(this.mSelectedRouteRecord.getUniqueId())) {
                this.mSelectedRouteRecord.getProvider().setDisplayVolume(volume);
            }
        }

        private void requestUpdateVolume(String routeId, int direction) {
            if (this.mSelectedRouteRecord != null && routeId.equals(this.mSelectedRouteRecord.getUniqueId())) {
                this.mSelectedRouteRecord.getProvider().adjustDisplayVolume(direction);
            }
        }

        public void addProvider(RemoteDisplayProviderProxy provider) {
            provider.setCallback(this);
            provider.setDiscoveryMode(this.mDiscoveryMode);
            provider.setSelectedDisplay(null);
            ProviderRecord providerRecord = new ProviderRecord(provider);
            this.mProviderRecords.add(providerRecord);
            providerRecord.updateDescriptor(provider.getDisplayState());
            scheduleUpdateClientState();
        }

        public void removeProvider(RemoteDisplayProviderProxy provider) {
            int index = findProviderRecord(provider);
            if (index >= 0) {
                ((ProviderRecord) this.mProviderRecords.remove(index)).updateDescriptor(null);
                provider.setCallback(null);
                provider.setDiscoveryMode(0);
                checkSelectedRouteState();
                scheduleUpdateClientState();
            }
        }

        public void onDisplayStateChanged(RemoteDisplayProviderProxy provider, RemoteDisplayState state) {
            updateProvider(provider, state);
        }

        private void updateProvider(RemoteDisplayProviderProxy provider, RemoteDisplayState state) {
            int index = findProviderRecord(provider);
            if (index >= 0 && ((ProviderRecord) this.mProviderRecords.get(index)).updateDescriptor(state)) {
                checkSelectedRouteState();
                scheduleUpdateClientState();
            }
        }

        private void checkSelectedRouteState() {
            if (this.mSelectedRouteRecord == null) {
                this.mConnectionPhase = -1;
                updateConnectionTimeout(0);
            } else if (this.mSelectedRouteRecord.isValid() && this.mSelectedRouteRecord.isEnabled()) {
                int oldPhase = this.mConnectionPhase;
                this.mConnectionPhase = getConnectionPhase(this.mSelectedRouteRecord.getStatus());
                if (oldPhase < 1 || this.mConnectionPhase >= 1) {
                    switch (this.mConnectionPhase) {
                        case 0:
                            updateConnectionTimeout(3);
                            break;
                        case 1:
                            if (oldPhase != 1) {
                                String str = MediaRouterService.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Connecting to route: ");
                                stringBuilder.append(this.mSelectedRouteRecord);
                                Slog.i(str, stringBuilder.toString());
                            }
                            updateConnectionTimeout(4);
                            break;
                        case 2:
                            if (oldPhase != 2) {
                                String str2 = MediaRouterService.TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Connected to route: ");
                                stringBuilder2.append(this.mSelectedRouteRecord);
                                Slog.i(str2, stringBuilder2.toString());
                            }
                            updateConnectionTimeout(0);
                            break;
                        default:
                            updateConnectionTimeout(1);
                            break;
                    }
                    return;
                }
                updateConnectionTimeout(2);
            } else {
                updateConnectionTimeout(1);
            }
        }

        private void updateConnectionTimeout(int reason) {
            if (reason != this.mConnectionTimeoutReason) {
                if (this.mConnectionTimeoutReason != 0) {
                    removeMessages(9);
                }
                this.mConnectionTimeoutReason = reason;
                this.mConnectionTimeoutStartTime = SystemClock.uptimeMillis();
                switch (reason) {
                    case 1:
                    case 2:
                        sendEmptyMessage(9);
                        return;
                    case 3:
                        sendEmptyMessageDelayed(9, MediaRouterService.CONNECTING_TIMEOUT);
                        return;
                    case 4:
                        sendEmptyMessageDelayed(9, 60000);
                        return;
                    default:
                        return;
                }
            }
        }

        private void connectionTimedOut() {
            if (this.mConnectionTimeoutReason == 0 || this.mSelectedRouteRecord == null) {
                Log.wtf(MediaRouterService.TAG, "Handled connection timeout for no reason.");
                return;
            }
            String str;
            StringBuilder stringBuilder;
            switch (this.mConnectionTimeoutReason) {
                case 1:
                    str = MediaRouterService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Selected route no longer available: ");
                    stringBuilder.append(this.mSelectedRouteRecord);
                    Slog.i(str, stringBuilder.toString());
                    break;
                case 2:
                    str = MediaRouterService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Selected route connection lost: ");
                    stringBuilder.append(this.mSelectedRouteRecord);
                    Slog.i(str, stringBuilder.toString());
                    break;
                case 3:
                    str = MediaRouterService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Selected route timed out while waiting for connection attempt to begin after ");
                    stringBuilder.append(SystemClock.uptimeMillis() - this.mConnectionTimeoutStartTime);
                    stringBuilder.append(" ms: ");
                    stringBuilder.append(this.mSelectedRouteRecord);
                    Slog.i(str, stringBuilder.toString());
                    break;
                case 4:
                    str = MediaRouterService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Selected route timed out while connecting after ");
                    stringBuilder.append(SystemClock.uptimeMillis() - this.mConnectionTimeoutStartTime);
                    stringBuilder.append(" ms: ");
                    stringBuilder.append(this.mSelectedRouteRecord);
                    Slog.i(str, stringBuilder.toString());
                    break;
            }
            this.mConnectionTimeoutReason = 0;
            unselectSelectedRoute();
        }

        private void scheduleUpdateClientState() {
            if (!this.mClientStateUpdateScheduled) {
                this.mClientStateUpdateScheduled = true;
                sendEmptyMessage(8);
            }
        }

        private void updateClientState() {
            int i;
            int i2 = 0;
            this.mClientStateUpdateScheduled = false;
            MediaRouterClientState routerState = new MediaRouterClientState();
            int providerCount = this.mProviderRecords.size();
            for (i = 0; i < providerCount; i++) {
                ((ProviderRecord) this.mProviderRecords.get(i)).appendClientState(routerState);
            }
            try {
                synchronized (this.mService.mLock) {
                    this.mUserRecord.mRouterState = routerState;
                    int count = this.mUserRecord.mClientRecords.size();
                    for (int i3 = 0; i3 < count; i3++) {
                        this.mTempClients.add(((ClientRecord) this.mUserRecord.mClientRecords.get(i3)).mClient);
                    }
                }
                i = this.mTempClients.size();
                while (i2 < i) {
                    ((IMediaRouterClient) this.mTempClients.get(i2)).onStateChanged();
                    i2++;
                }
                this.mTempClients.clear();
            } catch (RemoteException e) {
                Slog.w(MediaRouterService.TAG, "Failed to call onStateChanged. Client probably died.");
            } catch (Throwable th) {
                this.mTempClients.clear();
            }
        }

        private int findProviderRecord(RemoteDisplayProviderProxy provider) {
            int count = this.mProviderRecords.size();
            for (int i = 0; i < count; i++) {
                if (((ProviderRecord) this.mProviderRecords.get(i)).getProvider() == provider) {
                    return i;
                }
            }
            return -1;
        }

        private RouteRecord findRouteRecord(String uniqueId) {
            int count = this.mProviderRecords.size();
            for (int i = 0; i < count; i++) {
                RouteRecord record = ((ProviderRecord) this.mProviderRecords.get(i)).findRouteByUniqueId(uniqueId);
                if (record != null) {
                    return record;
                }
            }
            return null;
        }

        private static int getConnectionPhase(int status) {
            if (status != 6) {
                switch (status) {
                    case 0:
                        break;
                    case 1:
                    case 3:
                        return 0;
                    case 2:
                        return 1;
                    default:
                        return -1;
                }
            }
            return 2;
        }
    }

    public MediaRouterService(Context context) {
        this.mContext = context;
        Watchdog.getInstance().addMonitor(this);
        this.mAudioService = IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
        this.mAudioPlayerStateMonitor = AudioPlayerStateMonitor.getInstance();
        this.mAudioPlayerStateMonitor.registerListener(new OnAudioPlayerActiveStateChangedListener() {
            final Runnable mRestoreBluetoothA2dpRunnable = new Runnable() {
                public void run() {
                    MediaRouterService.this.restoreBluetoothA2dp();
                }
            };

            public void onAudioPlayerActiveStateChanged(AudioPlaybackConfiguration config, boolean isRemoved) {
                boolean active = !isRemoved && config.isActive();
                int pii = config.getPlayerInterfaceId();
                int uid = config.getClientUid();
                int idx = MediaRouterService.this.mActivePlayerMinPriorityQueue.indexOf(pii);
                if (idx >= 0) {
                    MediaRouterService.this.mActivePlayerMinPriorityQueue.remove(idx);
                    MediaRouterService.this.mActivePlayerUidMinPriorityQueue.remove(idx);
                }
                int restoreUid = -1;
                if (active) {
                    MediaRouterService.this.mActivePlayerMinPriorityQueue.add(config.getPlayerInterfaceId());
                    MediaRouterService.this.mActivePlayerUidMinPriorityQueue.add(uid);
                    restoreUid = uid;
                } else if (MediaRouterService.this.mActivePlayerUidMinPriorityQueue.size() > 0) {
                    restoreUid = MediaRouterService.this.mActivePlayerUidMinPriorityQueue.get(MediaRouterService.this.mActivePlayerUidMinPriorityQueue.size() - 1);
                }
                MediaRouterService.this.mHandler.removeCallbacks(this.mRestoreBluetoothA2dpRunnable);
                String str;
                StringBuilder stringBuilder;
                if (restoreUid >= 0) {
                    MediaRouterService.this.restoreRoute(restoreUid);
                    str = MediaRouterService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("onAudioPlayerActiveStateChanged: uid=");
                    stringBuilder.append(uid);
                    stringBuilder.append(", active=");
                    stringBuilder.append(active);
                    stringBuilder.append(", restoreUid=");
                    stringBuilder.append(restoreUid);
                    Slog.d(str, stringBuilder.toString());
                    return;
                }
                MediaRouterService.this.mHandler.postDelayed(this.mRestoreBluetoothA2dpRunnable, 500);
                str = MediaRouterService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onAudioPlayerActiveStateChanged: uid=");
                stringBuilder.append(uid);
                stringBuilder.append(", active=");
                stringBuilder.append(active);
                stringBuilder.append(", delaying");
                Slog.d(str, stringBuilder.toString());
            }
        }, this.mHandler);
        this.mAudioPlayerStateMonitor.registerSelfIntoAudioServiceIfNeeded(this.mAudioService);
        try {
            AudioRoutesInfo audioRoutes = this.mAudioService.startWatchingRoutes(new IAudioRoutesObserver.Stub() {
                public void dispatchAudioRoutesChanged(AudioRoutesInfo newRoutes) {
                    synchronized (MediaRouterService.this.mLock) {
                        if (newRoutes.mainType != MediaRouterService.this.mAudioRouteMainType) {
                            boolean z = false;
                            String str;
                            StringBuilder stringBuilder;
                            if ((newRoutes.mainType & 19) == 0) {
                                MediaRouterService mediaRouterService = MediaRouterService.this;
                                if (newRoutes.bluetoothName == null) {
                                    if (MediaRouterService.this.mActiveBluetoothDevice == null) {
                                        mediaRouterService.mGlobalBluetoothA2dpOn = z;
                                        str = MediaRouterService.TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("headset was plugged out:");
                                        stringBuilder.append(MediaRouterService.this.mGlobalBluetoothA2dpOn);
                                        Slog.w(str, stringBuilder.toString());
                                    }
                                }
                                z = true;
                                mediaRouterService.mGlobalBluetoothA2dpOn = z;
                                str = MediaRouterService.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("headset was plugged out:");
                                stringBuilder.append(MediaRouterService.this.mGlobalBluetoothA2dpOn);
                                Slog.w(str, stringBuilder.toString());
                            } else {
                                MediaRouterService.this.mGlobalBluetoothA2dpOn = false;
                                str = MediaRouterService.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("headset was plugged in:");
                                stringBuilder.append(MediaRouterService.this.mGlobalBluetoothA2dpOn);
                                Slog.w(str, stringBuilder.toString());
                            }
                            MediaRouterService.this.mAudioRouteMainType = newRoutes.mainType;
                            MediaRouterService.this.restoreBluetoothA2dp();
                        }
                    }
                }
            });
        } catch (RemoteException e) {
            Slog.w(TAG, "RemoteException in the audio service.");
        }
        Context context2 = context;
        context2.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, new IntentFilter("android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED"), null, null);
    }

    public void systemRunning() {
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.intent.action.USER_SWITCHED")) {
                    MediaRouterService.this.switchUser();
                }
            }
        }, new IntentFilter("android.intent.action.USER_SWITCHED"));
        switchUser();
    }

    public void monitor() {
        synchronized (this.mLock) {
        }
    }

    public void registerClientAsUser(IMediaRouterClient client, String packageName, int userId) {
        String str;
        if (client != null) {
            int uid = Binder.getCallingUid();
            str = packageName;
            if (validatePackageName(uid, str)) {
                int pid = Binder.getCallingPid();
                int resolvedUserId = ActivityManager.handleIncomingUser(pid, uid, userId, false, true, "registerClientAsUser", str);
                boolean trusted = this.mContext.checkCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY") == 0;
                long token = Binder.clearCallingIdentity();
                try {
                    Object obj = this.mLock;
                    synchronized (obj) {
                        Object obj2 = obj;
                        registerClientLocked(client, uid, pid, str, resolvedUserId, trusted);
                        Binder.restoreCallingIdentity(token);
                        return;
                    }
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(token);
                }
            } else {
                throw new SecurityException("packageName must match the calling uid");
            }
        }
        str = packageName;
        throw new IllegalArgumentException("client must not be null");
    }

    public void unregisterClient(IMediaRouterClient client) {
        if (client != null) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    unregisterClientLocked(client, false);
                }
                Binder.restoreCallingIdentity(token);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        } else {
            throw new IllegalArgumentException("client must not be null");
        }
    }

    public MediaRouterClientState getState(IMediaRouterClient client) {
        if (client != null) {
            long token = Binder.clearCallingIdentity();
            try {
                MediaRouterClientState stateLocked;
                synchronized (this.mLock) {
                    stateLocked = getStateLocked(client);
                }
                Binder.restoreCallingIdentity(token);
                return stateLocked;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        } else {
            throw new IllegalArgumentException("client must not be null");
        }
    }

    public boolean isPlaybackActive(IMediaRouterClient client) {
        if (client != null) {
            long token = Binder.clearCallingIdentity();
            try {
                ClientRecord clientRecord;
                synchronized (this.mLock) {
                    clientRecord = (ClientRecord) this.mAllClientRecords.get(client.asBinder());
                }
                if (clientRecord != null) {
                    boolean isPlaybackActive = this.mAudioPlayerStateMonitor.isPlaybackActive(clientRecord.mUid);
                    Binder.restoreCallingIdentity(token);
                    return isPlaybackActive;
                }
                Binder.restoreCallingIdentity(token);
                return false;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        } else {
            throw new IllegalArgumentException("client must not be null");
        }
    }

    public void setDiscoveryRequest(IMediaRouterClient client, int routeTypes, boolean activeScan) {
        if (client != null) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    setDiscoveryRequestLocked(client, routeTypes, activeScan);
                }
                Binder.restoreCallingIdentity(token);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        } else {
            throw new IllegalArgumentException("client must not be null");
        }
    }

    public void setSelectedRoute(IMediaRouterClient client, String routeId, boolean explicit) {
        if (client != null) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    setSelectedRouteLocked(client, routeId, explicit);
                }
                Binder.restoreCallingIdentity(token);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        } else {
            throw new IllegalArgumentException("client must not be null");
        }
    }

    public void requestSetVolume(IMediaRouterClient client, String routeId, int volume) {
        if (client == null) {
            throw new IllegalArgumentException("client must not be null");
        } else if (routeId != null) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    requestSetVolumeLocked(client, routeId, volume);
                }
                Binder.restoreCallingIdentity(token);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        } else {
            throw new IllegalArgumentException("routeId must not be null");
        }
    }

    public void requestUpdateVolume(IMediaRouterClient client, String routeId, int direction) {
        if (client == null) {
            throw new IllegalArgumentException("client must not be null");
        } else if (routeId != null) {
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (this.mLock) {
                    requestUpdateVolumeLocked(client, routeId, direction);
                }
                Binder.restoreCallingIdentity(token);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
            }
        } else {
            throw new IllegalArgumentException("routeId must not be null");
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            pw.println("MEDIA ROUTER SERVICE (dumpsys media_router)");
            pw.println();
            pw.println("Global state");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("  mCurrentUserId=");
            stringBuilder.append(this.mCurrentUserId);
            pw.println(stringBuilder.toString());
            synchronized (this.mLock) {
                int count = this.mUserRecords.size();
                for (int i = 0; i < count; i++) {
                    UserRecord userRecord = (UserRecord) this.mUserRecords.valueAt(i);
                    pw.println();
                    userRecord.dump(pw, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                }
            }
        }
    }

    void restoreBluetoothA2dp() {
        try {
            boolean a2dpOn;
            BluetoothDevice btDevice;
            synchronized (this.mLock) {
                a2dpOn = this.mGlobalBluetoothA2dpOn;
                btDevice = this.mActiveBluetoothDevice;
            }
            if (btDevice != null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("restoreBluetoothA2dp(");
                stringBuilder.append(a2dpOn);
                stringBuilder.append(")");
                Slog.v(str, stringBuilder.toString());
                this.mAudioService.setBluetoothA2dpOn(a2dpOn);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "RemoteException while calling setBluetoothA2dpOn.");
        }
    }

    void restoreRoute(int uid) {
        ClientRecord clientRecord = null;
        synchronized (this.mLock) {
            UserRecord userRecord = (UserRecord) this.mUserRecords.get(UserHandle.getUserId(uid));
            if (userRecord != null && userRecord.mClientRecords != null) {
                Iterator it = userRecord.mClientRecords.iterator();
                while (it.hasNext()) {
                    ClientRecord cr = (ClientRecord) it.next();
                    if (validatePackageName(uid, cr.mPackageName)) {
                        clientRecord = cr;
                        break;
                    }
                }
            }
        }
        if (clientRecord != null) {
            try {
                clientRecord.mClient.onRestoreRoute();
                return;
            } catch (RemoteException e) {
                Slog.w(TAG, "Failed to call onRestoreRoute. Client probably died.");
                return;
            }
        }
        this.mHandler.postDelayed(new Runnable() {
            public void run() {
                MediaRouterService.this.restoreBluetoothA2dp();
            }
        }, 500);
    }

    void switchUser() {
        synchronized (this.mLock) {
            int userId = ActivityManager.getCurrentUser();
            if (this.mCurrentUserId != userId) {
                int oldUserId = this.mCurrentUserId;
                this.mCurrentUserId = userId;
                UserRecord oldUser = (UserRecord) this.mUserRecords.get(oldUserId);
                if (oldUser != null) {
                    oldUser.mHandler.sendEmptyMessage(2);
                    disposeUserIfNeededLocked(oldUser);
                }
                UserRecord newUser = (UserRecord) this.mUserRecords.get(userId);
                if (newUser != null) {
                    newUser.mHandler.sendEmptyMessage(1);
                }
            }
        }
    }

    void clientDied(ClientRecord clientRecord) {
        synchronized (this.mLock) {
            unregisterClientLocked(clientRecord.mClient, true);
        }
    }

    private void registerClientLocked(IMediaRouterClient client, int uid, int pid, String packageName, int userId, boolean trusted) {
        ClientRecord clientRecord;
        int i = userId;
        IBinder binder = client.asBinder();
        if (((ClientRecord) this.mAllClientRecords.get(binder)) == null) {
            boolean newUser = false;
            UserRecord userRecord = (UserRecord) this.mUserRecords.get(i);
            if (userRecord == null) {
                userRecord = new UserRecord(i);
                newUser = true;
            }
            boolean newUser2 = newUser;
            UserRecord userRecord2 = userRecord;
            ClientRecord clientRecord2 = new ClientRecord(userRecord2, client, uid, pid, packageName, trusted);
            try {
                binder.linkToDeath(clientRecord2, 0);
                if (newUser2) {
                    this.mUserRecords.put(i, userRecord2);
                    initializeUserLocked(userRecord2);
                }
                userRecord2.mClientRecords.add(clientRecord2);
                this.mAllClientRecords.put(binder, clientRecord2);
                initializeClientLocked(clientRecord2);
                clientRecord = clientRecord2;
            } catch (RemoteException clientRecord3) {
                ClientRecord clientRecord4 = clientRecord3;
                throw new RuntimeException("Media router client died prematurely.", clientRecord3);
            }
        }
    }

    private void unregisterClientLocked(IMediaRouterClient client, boolean died) {
        ClientRecord clientRecord = (ClientRecord) this.mAllClientRecords.remove(client.asBinder());
        if (clientRecord != null) {
            UserRecord userRecord = clientRecord.mUserRecord;
            userRecord.mClientRecords.remove(clientRecord);
            disposeClientLocked(clientRecord, died);
            disposeUserIfNeededLocked(userRecord);
        }
    }

    private MediaRouterClientState getStateLocked(IMediaRouterClient client) {
        ClientRecord clientRecord = (ClientRecord) this.mAllClientRecords.get(client.asBinder());
        if (clientRecord != null) {
            return clientRecord.getState();
        }
        return null;
    }

    private void setDiscoveryRequestLocked(IMediaRouterClient client, int routeTypes, boolean activeScan) {
        ClientRecord clientRecord = (ClientRecord) this.mAllClientRecords.get(client.asBinder());
        if (clientRecord != null) {
            if (!clientRecord.mTrusted) {
                routeTypes &= -5;
            }
            if (clientRecord.mRouteTypes != routeTypes || clientRecord.mActiveScan != activeScan) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(clientRecord);
                stringBuilder.append(": Set discovery request, routeTypes=0x");
                stringBuilder.append(Integer.toHexString(routeTypes));
                stringBuilder.append(", activeScan=");
                stringBuilder.append(activeScan);
                Slog.d(str, stringBuilder.toString());
                clientRecord.mRouteTypes = routeTypes;
                clientRecord.mActiveScan = activeScan;
                clientRecord.mUserRecord.mHandler.sendEmptyMessage(3);
            }
        }
    }

    private void setSelectedRouteLocked(IMediaRouterClient client, String routeId, boolean explicit) {
        ClientRecord clientRecord = (ClientRecord) this.mAllClientRecords.get(client.asBinder());
        if (clientRecord != null) {
            String oldRouteId = clientRecord.mSelectedRouteId;
            if (!Objects.equals(routeId, oldRouteId)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(clientRecord);
                stringBuilder.append(": Set selected route, routeId=");
                stringBuilder.append(routeId);
                stringBuilder.append(", oldRouteId=");
                stringBuilder.append(oldRouteId);
                stringBuilder.append(", explicit=");
                stringBuilder.append(explicit);
                Slog.d(str, stringBuilder.toString());
                clientRecord.mSelectedRouteId = routeId;
                if (explicit && clientRecord.mTrusted) {
                    if (oldRouteId != null) {
                        clientRecord.mUserRecord.mHandler.obtainMessage(5, oldRouteId).sendToTarget();
                    }
                    if (routeId != null) {
                        clientRecord.mUserRecord.mHandler.obtainMessage(4, routeId).sendToTarget();
                    }
                }
            }
        }
    }

    private void requestSetVolumeLocked(IMediaRouterClient client, String routeId, int volume) {
        ClientRecord clientRecord = (ClientRecord) this.mAllClientRecords.get(client.asBinder());
        if (clientRecord != null) {
            clientRecord.mUserRecord.mHandler.obtainMessage(6, volume, 0, routeId).sendToTarget();
        }
    }

    private void requestUpdateVolumeLocked(IMediaRouterClient client, String routeId, int direction) {
        ClientRecord clientRecord = (ClientRecord) this.mAllClientRecords.get(client.asBinder());
        if (clientRecord != null) {
            clientRecord.mUserRecord.mHandler.obtainMessage(7, direction, 0, routeId).sendToTarget();
        }
    }

    private void initializeUserLocked(UserRecord userRecord) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(userRecord);
        stringBuilder.append(": Initialized");
        Slog.d(str, stringBuilder.toString());
        if (userRecord.mUserId == this.mCurrentUserId) {
            userRecord.mHandler.sendEmptyMessage(1);
        }
    }

    private void disposeUserIfNeededLocked(UserRecord userRecord) {
        if (userRecord.mUserId != this.mCurrentUserId && userRecord.mClientRecords.isEmpty()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(userRecord);
            stringBuilder.append(": Disposed");
            Slog.d(str, stringBuilder.toString());
            this.mUserRecords.remove(userRecord.mUserId);
        }
    }

    private void initializeClientLocked(ClientRecord clientRecord) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(clientRecord);
        stringBuilder.append(": Registered");
        Slog.d(str, stringBuilder.toString());
    }

    private void disposeClientLocked(ClientRecord clientRecord, boolean died) {
        String str;
        StringBuilder stringBuilder;
        if (died) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(clientRecord);
            stringBuilder.append(": Died!");
            Slog.d(str, stringBuilder.toString());
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(clientRecord);
            stringBuilder.append(": Unregistered");
            Slog.d(str, stringBuilder.toString());
        }
        if (clientRecord.mRouteTypes != 0 || clientRecord.mActiveScan) {
            clientRecord.mUserRecord.mHandler.sendEmptyMessage(3);
        }
        clientRecord.dispose();
    }

    private boolean validatePackageName(int uid, String packageName) {
        if (packageName != null) {
            String[] packageNames = this.mContext.getPackageManager().getPackagesForUid(uid);
            if (packageNames != null) {
                for (String n : packageNames) {
                    if (n.equals(packageName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
