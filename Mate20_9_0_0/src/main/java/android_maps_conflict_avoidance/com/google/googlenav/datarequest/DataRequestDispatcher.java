package android_maps_conflict_avoidance.com.google.googlenav.datarequest;

import android_maps_conflict_avoidance.com.google.common.Clock;
import android_maps_conflict_avoidance.com.google.common.Config;
import android_maps_conflict_avoidance.com.google.common.Log;
import android_maps_conflict_avoidance.com.google.common.Log.LogSaver;
import android_maps_conflict_avoidance.com.google.common.StaticUtil;
import android_maps_conflict_avoidance.com.google.common.io.GoogleHttpConnection;
import android_maps_conflict_avoidance.com.google.common.io.HttpConnectionFactory;
import android_maps_conflict_avoidance.com.google.common.io.PersistentStore;
import android_maps_conflict_avoidance.com.google.common.io.protocol.ProtoBuf;
import android_maps_conflict_avoidance.com.google.common.util.text.TextUtil;
import android_maps_conflict_avoidance.com.google.googlenav.GmmSettings;
import android_maps_conflict_avoidance.com.google.googlenav.proto.GmmMessageTypes;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Vector;

public class DataRequestDispatcher {
    public static final int MAX_WORKER_THREAD_COUNT = 4;
    private static volatile DataRequestDispatcher instance;
    private static int requestId = 0;
    protected volatile boolean active = false;
    protected int bytesReceived;
    protected int bytesSent;
    private final Clock clock;
    protected HttpConnectionFactory connectionFactory;
    protected long cookie;
    protected final boolean debug;
    protected final DispatcherServer defaultServer;
    protected final String distributionChannel;
    private long errorRetryTime = 0;
    private long firstConnectionErrorTime = Long.MIN_VALUE;
    protected String globalSpecialUrlArguments;
    private volatile long lastActiveTime = Long.MIN_VALUE;
    private Throwable lastException;
    private long lastExceptionTime;
    private volatile long lastSuccessTime = Long.MIN_VALUE;
    private final Vector listeners = new Vector();
    private long maxNetworkErrorRetryTimeout = 300000;
    protected volatile boolean mockLostDataConnection;
    private volatile boolean networkErrorMode;
    private volatile int networkSpeedBytesPerSecond = -1;
    protected final String platformID;
    protected final ProtoBuf properties;
    protected volatile String serverAddress;
    protected final String softwareVersion;
    private volatile int suspendCount;
    protected Vector thirdPartyServers = new Vector();
    private final Object threadDispatchLock = new Object();
    protected ConnectionWarmUpManager warmUpManager;
    private volatile int workerForegroundThreadCount = 0;
    private volatile int workerSubmissionThreadCount = 0;
    private volatile int workerThreadCount = 0;

    public class DispatcherServer implements Runnable {
        protected final byte headerFlag;
        protected volatile String serverAddress;
        protected Vector serverRequests = new Vector();
        protected final Vector supportedDataRequests;
        private Vector tempRequests;

        protected boolean canHandle(int protocolId) {
            return this.supportedDataRequests.isEmpty() || this.supportedDataRequests.contains(new Integer(protocolId));
        }

        public DispatcherServer(String address, Vector protocolList, byte headerFlag) {
            this.serverAddress = address;
            this.supportedDataRequests = protocolList;
            this.headerFlag = headerFlag;
        }

        public void addDataRequest(DataRequest dataRequest) {
            this.serverRequests.addElement(dataRequest);
            if (dataRequest.isImmediate() && !DataRequestDispatcher.this.isSuspended()) {
                activate();
            }
        }

        protected Vector dequeuePendingRequests() {
            Throwable th;
            synchronized (this) {
                try {
                    Vector pendingServerRequests = this.serverRequests;
                    this.serverRequests = new Vector();
                    return pendingServerRequests;
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        }

        protected synchronized void activate() {
            if (DataRequestDispatcher.this.canDispatchNow()) {
                synchronized (DataRequestDispatcher.this.threadDispatchLock) {
                    this.tempRequests = dequeuePendingRequests();
                    if (this.tempRequests != null) {
                        DataRequestDispatcher.this.workerThreadCount = DataRequestDispatcher.this.workerThreadCount + 1;
                        if (DataRequestDispatcher.containsForegroundRequest(this.tempRequests)) {
                            DataRequestDispatcher.this.workerForegroundThreadCount = DataRequestDispatcher.this.workerForegroundThreadCount + 1;
                        }
                        if (DataRequestDispatcher.containsSubmissionRequest(this.tempRequests)) {
                            DataRequestDispatcher.this.workerSubmissionThreadCount = DataRequestDispatcher.this.workerSubmissionThreadCount + 1;
                        }
                        new Thread(this, "DataRequestDispatcher").start();
                        while (this.tempRequests != null) {
                            try {
                                DataRequestDispatcher.this.threadDispatchLock.wait();
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                }
            }
        }

        protected void activateIfNeeded() {
            if (checkNeedToActivate()) {
                activate();
            }
        }

        private boolean checkNeedToActivate() {
            if (DataRequestDispatcher.this.isSuspended()) {
                return false;
            }
            synchronized (this.serverRequests) {
                for (int i = 0; i < this.serverRequests.size(); i++) {
                    if (((DataRequest) this.serverRequests.elementAt(i)).isImmediate()) {
                        return true;
                    }
                }
                return false;
            }
        }

        /* JADX WARNING: Missing block: B:8:0x0025, code:
            r1 = android_maps_conflict_avoidance.com.google.googlenav.datarequest.DataRequestDispatcher.containsForegroundRequest(r2);
            r0 = android_maps_conflict_avoidance.com.google.googlenav.datarequest.DataRequestDispatcher.containsSubmissionRequest(r2);
     */
        /* JADX WARNING: Missing block: B:9:0x002d, code:
            r3 = r0;
     */
        /* JADX WARNING: Missing block: B:12:0x0032, code:
            if (r9.this$0.active == false) goto L_0x009e;
     */
        /* JADX WARNING: Missing block: B:14:0x0038, code:
            if (r2.size() <= 0) goto L_0x009e;
     */
        /* JADX WARNING: Missing block: B:15:0x003a, code:
            monitor-enter(r9);
     */
        /* JADX WARNING: Missing block: B:19:0x0045, code:
            if (android_maps_conflict_avoidance.com.google.googlenav.datarequest.DataRequestDispatcher.access$800(r9.this$0) <= 0) goto L_0x0052;
     */
        /* JADX WARNING: Missing block: B:21:?, code:
            wait(android_maps_conflict_avoidance.com.google.googlenav.datarequest.DataRequestDispatcher.access$800(r9.this$0));
     */
        /* JADX WARNING: Missing block: B:28:0x0076, code:
            r4 = move-exception;
     */
        /* JADX WARNING: Missing block: B:30:?, code:
            android_maps_conflict_avoidance.com.google.common.StaticUtil.handleOutOfMemory();
            android_maps_conflict_avoidance.com.google.googlenav.datarequest.DataRequestDispatcher.access$1100(r9.this$0, 5, r4);
     */
        /* JADX WARNING: Missing block: B:31:0x0080, code:
            r4 = move-exception;
     */
        /* JADX WARNING: Missing block: B:32:0x0081, code:
            android_maps_conflict_avoidance.com.google.googlenav.datarequest.DataRequestDispatcher.access$1100(r9.this$0, 5, r4);
            android_maps_conflict_avoidance.com.google.common.Log.logThrowable("REQUEST", r4);
     */
        /* JADX WARNING: Missing block: B:33:0x008c, code:
            r0 = move-exception;
     */
        /* JADX WARNING: Missing block: B:34:0x008d, code:
            android_maps_conflict_avoidance.com.google.googlenav.datarequest.DataRequestDispatcher.access$1100(r9.this$0, 3, r0);
     */
        /* JADX WARNING: Missing block: B:35:0x0094, code:
            r0 = move-exception;
     */
        /* JADX WARNING: Missing block: B:36:0x0095, code:
            networkAccessDenied(r0);
     */
        /* JADX WARNING: Missing block: B:43:0x009e, code:
            r0 = android_maps_conflict_avoidance.com.google.googlenav.datarequest.DataRequestDispatcher.access$200(r9.this$0);
     */
        /* JADX WARNING: Missing block: B:44:0x00a4, code:
            monitor-enter(r0);
     */
        /* JADX WARNING: Missing block: B:46:?, code:
            android_maps_conflict_avoidance.com.google.googlenav.datarequest.DataRequestDispatcher.access$310(r9.this$0);
     */
        /* JADX WARNING: Missing block: B:47:0x00aa, code:
            if (r1 == false) goto L_0x00b1;
     */
        /* JADX WARNING: Missing block: B:48:0x00ac, code:
            android_maps_conflict_avoidance.com.google.googlenav.datarequest.DataRequestDispatcher.access$410(r9.this$0);
     */
        /* JADX WARNING: Missing block: B:49:0x00b1, code:
            if (r3 == false) goto L_0x00b8;
     */
        /* JADX WARNING: Missing block: B:50:0x00b3, code:
            android_maps_conflict_avoidance.com.google.googlenav.datarequest.DataRequestDispatcher.access$510(r9.this$0);
     */
        /* JADX WARNING: Missing block: B:51:0x00b8, code:
            monitor-exit(r0);
     */
        /* JADX WARNING: Missing block: B:52:0x00b9, code:
            activateIfNeeded();
     */
        /* JADX WARNING: Missing block: B:53:0x00bd, code:
            return;
     */
        /* JADX WARNING: Missing block: B:60:0x00c8, code:
            monitor-enter(android_maps_conflict_avoidance.com.google.googlenav.datarequest.DataRequestDispatcher.access$200(r9.this$0));
     */
        /* JADX WARNING: Missing block: B:62:?, code:
            android_maps_conflict_avoidance.com.google.googlenav.datarequest.DataRequestDispatcher.access$310(r9.this$0);
     */
        /* JADX WARNING: Missing block: B:63:0x00ce, code:
            if (r1 != false) goto L_0x00d0;
     */
        /* JADX WARNING: Missing block: B:64:0x00d0, code:
            android_maps_conflict_avoidance.com.google.googlenav.datarequest.DataRequestDispatcher.access$410(r9.this$0);
     */
        /* JADX WARNING: Missing block: B:65:0x00d5, code:
            if (r3 != false) goto L_0x00d7;
     */
        /* JADX WARNING: Missing block: B:66:0x00d7, code:
            android_maps_conflict_avoidance.com.google.googlenav.datarequest.DataRequestDispatcher.access$510(r9.this$0);
     */
        /* JADX WARNING: Missing block: B:68:0x00dd, code:
            activateIfNeeded();
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            Vector requests;
            Throwable th;
            synchronized (DataRequestDispatcher.this.threadDispatchLock) {
                try {
                    requests = this.tempRequests;
                    try {
                        this.tempRequests = null;
                        DataRequestDispatcher.this.lastActiveTime = DataRequestDispatcher.this.clock.relativeTimeMillis();
                        DataRequestDispatcher.this.threadDispatchLock.notifyAll();
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                } catch (Throwable th3) {
                    Throwable th4 = th3;
                    requests = null;
                    th = th4;
                    throw th;
                }
            }
            boolean containsSubmissionRequest;
            DataRequestDispatcher.this.serviceRequests(requests, this);
            DataRequestDispatcher.this.connectionFactory.registerNetworkSuccess(false);
            DataRequestDispatcher.this.clearNetworkError();
            DataRequestDispatcher.this.lastSuccessTime = DataRequestDispatcher.this.clock.relativeTimeMillis();
            containsSubmissionRequest = containsSubmissionRequest;
            containsSubmissionRequest = containsSubmissionRequest;
        }

        private void networkAccessDenied(Exception e) {
            Log.logQuietThrowable("REQUEST", e);
            DataRequestDispatcher.this.stop();
            DataRequestDispatcher.this.maybeNotifyNetworkError(0);
        }

        public void start() {
            activateIfNeeded();
        }
    }

    public static final class DataRequestEventUploader implements LogSaver {
        private final DataRequestDispatcher drd;

        private DataRequestEventUploader(DataRequestDispatcher drd) {
            this.drd = drd;
        }

        public Object uploadEventLog(boolean immediate, Object waitObject, byte[] logBytes) {
            if (logBytes != null && logBytes.length > 2) {
                SimpleDataRequest edr = new SimpleDataRequest(10, logBytes, immediate, false, waitObject);
                DataRequestDispatcher drd = DataRequestDispatcher.getInstance();
                if (drd == null) {
                    return null;
                }
                drd.addDataRequest(edr);
            }
            return null;
        }
    }

    private class CookieDataRequest extends BaseDataRequest {
        private CookieDataRequest() {
        }

        public int getRequestType() {
            return 15;
        }

        public boolean isImmediate() {
            return false;
        }

        public void writeRequestData(DataOutput dos) throws IOException {
        }

        public boolean readResponseData(DataInput dis) throws IOException {
            DataRequestDispatcher.this.cookie = dis.readLong();
            DataRequestDispatcher.saveCookie(DataRequestDispatcher.this.cookie);
            return true;
        }
    }

    public static synchronized DataRequestDispatcher createInstance(String serverAddress, String platformID, String softwareVersion, String distributionChannel, boolean debug) {
        DataRequestDispatcher dataRequestDispatcher;
        synchronized (DataRequestDispatcher.class) {
            if (instance == null) {
                instance = new DataRequestDispatcher(serverAddress, platformID, softwareVersion, distributionChannel, debug);
                Log.setLogSaver(new DataRequestEventUploader());
                dataRequestDispatcher = instance;
            } else {
                throw new RuntimeException("Attempting to create multiple DataRequestDispatchers");
            }
        }
        return dataRequestDispatcher;
    }

    public static DataRequestDispatcher getInstance() {
        return instance;
    }

    protected DataRequestDispatcher(String serverAddress, String platformID, String softwareVersion, String distributionChannel, boolean debug) {
        if (serverAddress == null || serverAddress.length() == 0) {
            throw new IllegalArgumentException();
        }
        this.serverAddress = serverAddress;
        this.softwareVersion = softwareVersion;
        this.platformID = platformID;
        this.distributionChannel = distributionChannel;
        this.debug = debug;
        this.connectionFactory = Config.getInstance().getConnectionFactory();
        this.clock = Config.getInstance().getClock();
        this.warmUpManager = new ConnectionWarmUpManager(this, this.clock);
        this.bytesSent = 0;
        this.bytesReceived = 0;
        this.defaultServer = new DispatcherServer(this.serverAddress, new Vector(), (byte) 0);
        this.cookie = loadOrRequestCookie();
        this.properties = new ProtoBuf(GmmMessageTypes.CLIENT_PROPERTIES_REQUEST_PROTO);
    }

    public synchronized boolean isSuspended() {
        return this.suspendCount > 0;
    }

    protected long loadOrRequestCookie() {
        DataInput dis = StaticUtil.readPreferenceAsDataInput("SessionID");
        if (dis != null) {
            try {
                return dis.readLong();
            } catch (IOException e) {
                Config.getInstance().getPersistentStore().setPreference("SessionID", null);
            }
        }
        addDataRequest(new CookieDataRequest());
        return 0;
    }

    public synchronized void addDataRequestListener(DataRequestListener listenerData) {
        if (!this.listeners.contains(listenerData)) {
            this.listeners.addElement(listenerData);
        }
    }

    public synchronized void removeDataRequestListener(DataRequestListener listenerData) {
        this.listeners.removeElement(listenerData);
    }

    protected synchronized DataRequestListener[] snapshotListeners() {
        DataRequestListener[] listenersArray;
        listenersArray = new DataRequestListener[this.listeners.size()];
        this.listeners.copyInto(listenersArray);
        return listenersArray;
    }

    protected void notifyComplete(DataRequest dataRequest) {
        DataRequestListener[] listenersArray = snapshotListeners();
        for (DataRequestListener onComplete : listenersArray) {
            onComplete.onComplete(dataRequest);
        }
    }

    protected void notifyNetworkError(int errorCode, boolean networkEverWorked, String debugMessage) {
        DataRequestListener[] listenersArray = snapshotListeners();
        for (DataRequestListener onNetworkError : listenersArray) {
            onNetworkError.onNetworkError(errorCode, networkEverWorked, debugMessage);
        }
    }

    protected final void maybeNotifyNetworkError(int errorCode) {
        boolean notifyListeners = false;
        synchronized (this) {
            if (!this.networkErrorMode) {
                Log.logToScreen("DRD: in Error Mode");
                this.networkErrorMode = true;
                this.firstConnectionErrorTime = Long.MIN_VALUE;
                notifyListeners = true;
            }
        }
        boolean networkEverWorked = this.connectionFactory.getNetworkWorked();
        if (notifyListeners) {
            notifyNetworkError(errorCode, networkEverWorked, null);
        }
    }

    public void addDataRequest(DataRequest dataRequest) {
        if (this.mockLostDataConnection) {
            notifyNetworkError(5, true, null);
        }
        for (int i = 0; i < this.thirdPartyServers.size(); i++) {
            DispatcherServer tps = (DispatcherServer) this.thirdPartyServers.elementAt(i);
            if (tps.canHandle(dataRequest.getRequestType())) {
                tps.addDataRequest(dataRequest);
                return;
            }
        }
        this.defaultServer.addDataRequest(dataRequest);
    }

    public synchronized boolean canDispatchNow() {
        boolean z;
        z = this.active && this.workerThreadCount < MAX_WORKER_THREAD_COUNT && (this.connectionFactory.getNetworkWorkedThisSession() || this.workerThreadCount == 0);
        return z;
    }

    public void stop() {
        this.active = false;
    }

    public void start() {
        this.active = true;
        for (int i = 0; i < this.thirdPartyServers.size(); i++) {
            ((DispatcherServer) this.thirdPartyServers.elementAt(i)).start();
        }
        this.defaultServer.start();
    }

    private synchronized void clearNetworkError() {
        this.firstConnectionErrorTime = Long.MIN_VALUE;
        this.networkErrorMode = false;
        this.errorRetryTime = 0;
    }

    private void handleError(int code, Throwable t) {
        boolean call = false;
        synchronized (this) {
            this.lastException = t;
            this.lastExceptionTime = System.currentTimeMillis();
            if (t != null && GmmSettings.isDebugBuild()) {
                t.printStackTrace();
            }
            this.connectionFactory.notifyFailure();
            if (this.networkErrorMode) {
                if (this.errorRetryTime < 2000) {
                    this.errorRetryTime = 2000;
                } else {
                    this.errorRetryTime = (this.errorRetryTime * 5) / 4;
                }
                if (this.errorRetryTime > this.maxNetworkErrorRetryTimeout) {
                    this.errorRetryTime = this.maxNetworkErrorRetryTimeout;
                }
            } else {
                this.errorRetryTime = 200;
                if (this.firstConnectionErrorTime == Long.MIN_VALUE) {
                    this.firstConnectionErrorTime = this.clock.relativeTimeMillis();
                } else if (this.firstConnectionErrorTime + 15000 < this.clock.relativeTimeMillis()) {
                    call = true;
                }
            }
        }
        if (call) {
            if (code == 3 && this.connectionFactory.usingMDS() && !this.connectionFactory.getNetworkWorked()) {
                code = 4;
            }
            maybeNotifyNetworkError(code);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:254:0x058b A:{SYNTHETIC, Splitter: B:254:0x058b} */
    /* JADX WARNING: Removed duplicated region for block: B:263:0x059f A:{SYNTHETIC, Splitter: B:263:0x059f} */
    /* JADX WARNING: Removed duplicated region for block: B:294:0x05c5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x05b1  */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x058b A:{SYNTHETIC, Splitter: B:254:0x058b} */
    /* JADX WARNING: Removed duplicated region for block: B:263:0x059f A:{SYNTHETIC, Splitter: B:263:0x059f} */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x05b1  */
    /* JADX WARNING: Removed duplicated region for block: B:294:0x05c5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x058b A:{SYNTHETIC, Splitter: B:254:0x058b} */
    /* JADX WARNING: Removed duplicated region for block: B:263:0x059f A:{SYNTHETIC, Splitter: B:263:0x059f} */
    /* JADX WARNING: Removed duplicated region for block: B:294:0x05c5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x05b1  */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x058b A:{SYNTHETIC, Splitter: B:254:0x058b} */
    /* JADX WARNING: Removed duplicated region for block: B:263:0x059f A:{SYNTHETIC, Splitter: B:263:0x059f} */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x05b1  */
    /* JADX WARNING: Removed duplicated region for block: B:294:0x05c5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x058b A:{SYNTHETIC, Splitter: B:254:0x058b} */
    /* JADX WARNING: Removed duplicated region for block: B:263:0x059f A:{SYNTHETIC, Splitter: B:263:0x059f} */
    /* JADX WARNING: Removed duplicated region for block: B:294:0x05c5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x05b1  */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x058b A:{SYNTHETIC, Splitter: B:254:0x058b} */
    /* JADX WARNING: Removed duplicated region for block: B:263:0x059f A:{SYNTHETIC, Splitter: B:263:0x059f} */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x05b1  */
    /* JADX WARNING: Removed duplicated region for block: B:294:0x05c5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x058b A:{SYNTHETIC, Splitter: B:254:0x058b} */
    /* JADX WARNING: Removed duplicated region for block: B:263:0x059f A:{SYNTHETIC, Splitter: B:263:0x059f} */
    /* JADX WARNING: Removed duplicated region for block: B:294:0x05c5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x05b1  */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x058b A:{SYNTHETIC, Splitter: B:254:0x058b} */
    /* JADX WARNING: Removed duplicated region for block: B:263:0x059f A:{SYNTHETIC, Splitter: B:263:0x059f} */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x05b1  */
    /* JADX WARNING: Removed duplicated region for block: B:294:0x05c5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x058b A:{SYNTHETIC, Splitter: B:254:0x058b} */
    /* JADX WARNING: Removed duplicated region for block: B:263:0x059f A:{SYNTHETIC, Splitter: B:263:0x059f} */
    /* JADX WARNING: Removed duplicated region for block: B:294:0x05c5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x05b1  */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x058b A:{SYNTHETIC, Splitter: B:254:0x058b} */
    /* JADX WARNING: Removed duplicated region for block: B:263:0x059f A:{SYNTHETIC, Splitter: B:263:0x059f} */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x05b1  */
    /* JADX WARNING: Removed duplicated region for block: B:294:0x05c5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x058b A:{SYNTHETIC, Splitter: B:254:0x058b} */
    /* JADX WARNING: Removed duplicated region for block: B:263:0x059f A:{SYNTHETIC, Splitter: B:263:0x059f} */
    /* JADX WARNING: Removed duplicated region for block: B:294:0x05c5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x05b1  */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x058b A:{SYNTHETIC, Splitter: B:254:0x058b} */
    /* JADX WARNING: Removed duplicated region for block: B:263:0x059f A:{SYNTHETIC, Splitter: B:263:0x059f} */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x05b1  */
    /* JADX WARNING: Removed duplicated region for block: B:294:0x05c5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x058b A:{SYNTHETIC, Splitter: B:254:0x058b} */
    /* JADX WARNING: Removed duplicated region for block: B:263:0x059f A:{SYNTHETIC, Splitter: B:263:0x059f} */
    /* JADX WARNING: Removed duplicated region for block: B:294:0x05c5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x05b1  */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x058b A:{SYNTHETIC, Splitter: B:254:0x058b} */
    /* JADX WARNING: Removed duplicated region for block: B:263:0x059f A:{SYNTHETIC, Splitter: B:263:0x059f} */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x05b1  */
    /* JADX WARNING: Removed duplicated region for block: B:294:0x05c5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x058b A:{SYNTHETIC, Splitter: B:254:0x058b} */
    /* JADX WARNING: Removed duplicated region for block: B:263:0x059f A:{SYNTHETIC, Splitter: B:263:0x059f} */
    /* JADX WARNING: Removed duplicated region for block: B:294:0x05c5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x05b1  */
    /* JADX WARNING: Removed duplicated region for block: B:173:0x034c  */
    /* JADX WARNING: Removed duplicated region for block: B:254:0x058b A:{SYNTHETIC, Splitter: B:254:0x058b} */
    /* JADX WARNING: Removed duplicated region for block: B:263:0x059f A:{SYNTHETIC, Splitter: B:263:0x059f} */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x05b1  */
    /* JADX WARNING: Removed duplicated region for block: B:294:0x05c5 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:210:0x042e A:{SYNTHETIC, Splitter: B:210:0x042e} */
    /* JADX WARNING: Removed duplicated region for block: B:291:0x0451 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:215:0x043e  */
    /* JADX WARNING: Removed duplicated region for block: B:213:0x0436 A:{LOOP_START, LOOP:6: B:213:0x0436->B:218:0x044d, PHI: r22 } */
    /* JADX WARNING: Missing block: B:184:0x0387, code:
            r29 = r4;
            r30 = r5;
     */
    /* JADX WARNING: Missing block: B:186:?, code:
            r0 = (int) (r1.clock.relativeTimeMillis() - r17);
            r3 = new java.lang.StringBuilder();
            r3.append("");
            r3.append(r13);
            android_maps_conflict_avoidance.com.google.common.Log.addEvent((short) 22, "fb", r3.toString());
            r3 = new java.lang.StringBuilder();
            r3.append("");
            r3.append(r0);
            android_maps_conflict_avoidance.com.google.common.Log.addEvent((short) 22, "lb", r3.toString());
     */
    /* JADX WARNING: Missing block: B:187:0x03c7, code:
            r21 = r27;
            r16 = (int) r13;
            r32 = r13;
            r34 = r15;
            r14 = r29;
            r13 = r30;
            r15 = r28;
            r9 = r6;
            r22 = 0;
     */
    /* JADX WARNING: Missing block: B:189:?, code:
            r1.warmUpManager.onFinishServiceRequests(r8, r17, r16, r0);
     */
    /* JADX WARNING: Missing block: B:190:0x03e5, code:
            if (r11 < 8192) goto L_0x03f4;
     */
    /* JADX WARNING: Missing block: B:192:0x03ed, code:
            if (((long) r0) > 60000) goto L_0x03f4;
     */
    /* JADX WARNING: Missing block: B:193:0x03ef, code:
            r1.networkSpeedBytesPerSecond = (r11 * 1000) / r0;
     */
    /* JADX WARNING: Missing block: B:194:0x03f4, code:
            r12.append(", ");
     */
    /* JADX WARNING: Missing block: B:195:0x03fb, code:
            if (r11 >= 1000) goto L_0x0403;
     */
    /* JADX WARNING: Missing block: B:196:0x03fd, code:
            r12.append("<1kb");
     */
    /* JADX WARNING: Missing block: B:197:0x0403, code:
            r12.append(r11 / 1000);
            r12.append("kb");
     */
    /* JADX WARNING: Missing block: B:198:0x040d, code:
            r37.removeAllElements();
     */
    /* JADX WARNING: Missing block: B:199:0x0410, code:
            android_maps_conflict_avoidance.com.google.common.Log.logToScreen(r12.toString());
     */
    /* JADX WARNING: Missing block: B:200:0x0418, code:
            if (r14 == null) goto L_0x0421;
     */
    /* JADX WARNING: Missing block: B:202:?, code:
            r14.close();
     */
    /* JADX WARNING: Missing block: B:220:0x0452, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:221:0x0453, code:
            r9 = r6;
            r22 = 0;
            r32 = r13;
            r21 = r27;
            r15 = r28;
            r13 = r30;
            r2 = r0;
            r4 = r29;
     */
    /* JADX WARNING: Missing block: B:228:0x04b4, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:229:0x04b5, code:
            r2 = r0;
            r6 = r9;
            r4 = r14;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void serviceRequests(Vector requests, DispatcherServer dispatcherServer) throws IOException, SecurityException {
        byte[] sendData;
        int i;
        String contentType;
        Throwable th;
        DataOutputStream os;
        int responseCode;
        long firstByteTime;
        Vector requests2;
        DataOutputStream os2;
        boolean osSkipClose;
        StringBuffer drdDebug;
        int i2;
        DataRequestDispatcher dataRequestDispatcher;
        DataOutputStream os3;
        DataInputStream is;
        DataOutputStream os4;
        PrintStream printStream;
        DataRequest dataRequest;
        DataInputStream dataInputStream;
        DataRequestDispatcher dataRequestDispatcher2 = this;
        Vector vector = requests;
        DispatcherServer dispatcherServer2 = dispatcherServer;
        DataInputStream is2 = null;
        dataRequestDispatcher2.warmUpManager.onStartServiceRequests(vector);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String addToUrl = urlArguments(requests);
        dataRequestDispatcher2.generateRequest(vector, baos, dispatcherServer2);
        StringBuffer drdDebug2 = new StringBuffer("DRD");
        drdDebug2.append("(");
        int i3 = requestId;
        requestId = i3 + 1;
        drdDebug2.append(i3);
        drdDebug2.append("): ");
        for (i3 = 0; i3 < requests.size(); i3++) {
            drdDebug2.append(((DataRequest) vector.elementAt(i3)).getRequestType());
            if (i3 != requests.size() - 1) {
                drdDebug2.append("|");
            }
        }
        byte[] sendData2 = baos.toByteArray();
        ByteArrayOutputStream baos2 = null;
        long startTime;
        GoogleHttpConnection hc;
        String addToUrl2;
        ByteArrayOutputStream baos3;
        String contentType2;
        try {
            StringBuilder stringBuilder;
            StringBuilder stringBuilder2;
            DataOutputStream os5;
            GoogleHttpConnection googleHttpConnection;
            long firstByteTime2;
            startTime = dataRequestDispatcher2.clock.relativeTimeMillis();
            try {
                HttpConnectionFactory httpConnectionFactory = dataRequestDispatcher2.connectionFactory;
                stringBuilder = new StringBuilder();
                stringBuilder.append(dispatcherServer2.serverAddress);
                stringBuilder.append(addToUrl);
                hc = httpConnectionFactory.createConnection(stringBuilder.toString(), true);
                try {
                    hc.setConnectionProperty("Content-Type", "application/binary");
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("");
                    stringBuilder2.append(sendData2.length);
                    hc.setConnectionProperty("Content-Length", stringBuilder2.toString());
                    os5 = hc.openDataOutputStream();
                } catch (Throwable th2) {
                    googleHttpConnection = hc;
                    addToUrl2 = addToUrl;
                    sendData = sendData2;
                    baos3 = null;
                    i = 0;
                    contentType = th2;
                    os = null;
                    contentType2 = null;
                    responseCode = 0;
                    firstByteTime = 0;
                    requests2 = vector;
                    os2 = os;
                    osSkipClose = false;
                    drdDebug = drdDebug2;
                    Log.logToScreen(drdDebug.toString());
                    if (is2 != null) {
                    }
                    try {
                        os2.close();
                    } catch (IOException e) {
                    }
                    if (hc != null) {
                    }
                    while (true) {
                        i2 = i;
                        dataRequestDispatcher = dataRequestDispatcher2;
                        if (i2 < requests2.size()) {
                        }
                        i = i2 + 1;
                        dataRequestDispatcher2 = dataRequestDispatcher;
                    }
                }
            } catch (Throwable th3) {
                th2 = th3;
                addToUrl2 = addToUrl;
                sendData = sendData2;
                baos3 = null;
                i = 0;
                hc = null;
                os = null;
                contentType2 = null;
                responseCode = 0;
                firstByteTime = 0;
                contentType = th2;
                requests2 = vector;
                os2 = os;
                osSkipClose = false;
                drdDebug = drdDebug2;
                Log.logToScreen(drdDebug.toString());
                if (is2 != null) {
                }
                os2.close();
                if (hc != null) {
                }
                while (true) {
                    i2 = i;
                    dataRequestDispatcher = dataRequestDispatcher2;
                    if (i2 < requests2.size()) {
                    }
                    i = i2 + 1;
                    dataRequestDispatcher2 = dataRequestDispatcher;
                }
            }
            try {
                os5.write(sendData2);
                dataRequestDispatcher2.bytesSent += sendData2.length;
                is2 = hc.openDataInputStream();
                try {
                    os3 = hc.getResponseCode();
                    try {
                        contentType = hc.getContentType();
                        try {
                            sendData = sendData2;
                            firstByteTime2 = dataRequestDispatcher2.clock.relativeTimeMillis() - startTime;
                        } catch (Throwable th22) {
                            responseCode = os3;
                            os = os5;
                            googleHttpConnection = hc;
                            addToUrl2 = addToUrl;
                            sendData = sendData2;
                            baos3 = null;
                            i = 0;
                            contentType2 = contentType;
                            sendData2 = is2;
                            contentType = th22;
                            firstByteTime = 0;
                            requests2 = vector;
                            os2 = os;
                            osSkipClose = false;
                            drdDebug = drdDebug2;
                            Log.logToScreen(drdDebug.toString());
                            if (is2 != null) {
                            }
                            os2.close();
                            if (hc != null) {
                            }
                            while (true) {
                                i2 = i;
                                dataRequestDispatcher = dataRequestDispatcher2;
                                if (i2 < requests2.size()) {
                                }
                                i = i2 + 1;
                                dataRequestDispatcher2 = dataRequestDispatcher;
                            }
                        }
                    } catch (Throwable th222) {
                        responseCode = os3;
                        os = os5;
                        googleHttpConnection = hc;
                        addToUrl2 = addToUrl;
                        sendData = sendData2;
                        baos3 = null;
                        i = 0;
                        sendData2 = is2;
                        contentType = th222;
                        contentType2 = null;
                        firstByteTime = 0;
                        requests2 = vector;
                        os2 = os;
                        osSkipClose = false;
                        drdDebug = drdDebug2;
                        Log.logToScreen(drdDebug.toString());
                        if (is2 != null) {
                        }
                        os2.close();
                        if (hc != null) {
                        }
                        while (true) {
                            i2 = i;
                            dataRequestDispatcher = dataRequestDispatcher2;
                            if (i2 < requests2.size()) {
                            }
                            i = i2 + 1;
                            dataRequestDispatcher2 = dataRequestDispatcher;
                        }
                    }
                } catch (Throwable th2222) {
                    os = os5;
                    googleHttpConnection = hc;
                    addToUrl2 = addToUrl;
                    sendData = sendData2;
                    baos3 = null;
                    i = 0;
                    sendData2 = is2;
                    contentType = th2222;
                    contentType2 = null;
                    responseCode = 0;
                    firstByteTime = 0;
                    requests2 = vector;
                    os2 = os;
                    osSkipClose = false;
                    drdDebug = drdDebug2;
                    Log.logToScreen(drdDebug.toString());
                    if (is2 != null) {
                    }
                    os2.close();
                    if (hc != null) {
                    }
                    while (true) {
                        i2 = i;
                        dataRequestDispatcher = dataRequestDispatcher2;
                        if (i2 < requests2.size()) {
                        }
                        i = i2 + 1;
                        dataRequestDispatcher2 = dataRequestDispatcher;
                    }
                }
            } catch (Throwable th22222) {
                os = os5;
                googleHttpConnection = hc;
                addToUrl2 = addToUrl;
                sendData = sendData2;
                baos3 = null;
                i = 0;
                contentType = th22222;
                contentType2 = null;
                responseCode = 0;
                firstByteTime = 0;
                requests2 = vector;
                os2 = os;
                osSkipClose = false;
                drdDebug = drdDebug2;
                Log.logToScreen(drdDebug.toString());
                if (is2 != null) {
                }
                os2.close();
                if (hc != null) {
                }
                while (true) {
                    i2 = i;
                    dataRequestDispatcher = dataRequestDispatcher2;
                    if (i2 < requests2.size()) {
                    }
                    i = i2 + 1;
                    dataRequestDispatcher2 = dataRequestDispatcher;
                }
            }
            try {
                DataRequest contentType3;
                drdDebug2.append(", ");
                if (firstByteTime2 < 1000) {
                    try {
                        drdDebug2.append("<1s");
                    } catch (Throwable th4) {
                        th22222 = th4;
                    }
                } else {
                    try {
                        drdDebug2.append(firstByteTime2 / 1000);
                        drdDebug2.append("s");
                    } catch (Throwable th5) {
                        th22222 = th5;
                        responseCode = os3;
                        googleHttpConnection = hc;
                        addToUrl2 = addToUrl;
                        firstByteTime = firstByteTime2;
                        baos3 = null;
                        vector = requests;
                        i = 0;
                        contentType2 = contentType;
                        sendData2 = is2;
                        os = os5;
                        contentType = th22222;
                        requests2 = vector;
                        os2 = os;
                        osSkipClose = false;
                        drdDebug = drdDebug2;
                        Log.logToScreen(drdDebug.toString());
                        if (is2 != null) {
                        }
                        os2.close();
                        if (hc != null) {
                        }
                        while (true) {
                            i2 = i;
                            dataRequestDispatcher = dataRequestDispatcher2;
                            if (i2 < requests2.size()) {
                            }
                            i = i2 + 1;
                            dataRequestDispatcher2 = dataRequestDispatcher;
                        }
                    }
                }
                int i4;
                if (os3 == 501) {
                    try {
                        dataRequestDispatcher2.maybeNotifyNetworkError(2);
                        Log.logToScreen(drdDebug2.toString());
                        if (is2 != null) {
                            try {
                                is2.close();
                            } catch (IOException e2) {
                            }
                        }
                        if (os5 != null && null == null) {
                            try {
                                os5.close();
                            } catch (IOException e3) {
                            }
                        }
                        if (hc != null) {
                            try {
                                hc.close();
                            } catch (IOException e4) {
                            }
                        }
                        i4 = 0;
                        while (true) {
                            i2 = i4;
                            vector = requests;
                            if (i2 < requests.size()) {
                                DataRequest dataRequest2 = (DataRequest) vector.elementAt(i2);
                                if (!dataRequest2.retryOnFailure()) {
                                    vector.removeElement(dataRequest2);
                                }
                                i4 = i2 + 1;
                            } else {
                                return;
                            }
                        }
                    } catch (Throwable th6) {
                        th22222 = th6;
                        vector = requests;
                        responseCode = os3;
                        addToUrl2 = addToUrl;
                        firstByteTime = firstByteTime2;
                        baos3 = null;
                        i = 0;
                        contentType2 = contentType;
                        os = os5;
                        contentType = th22222;
                        requests2 = vector;
                        os2 = os;
                        osSkipClose = false;
                        drdDebug = drdDebug2;
                        Log.logToScreen(drdDebug.toString());
                        if (is2 != null) {
                        }
                        os2.close();
                        if (hc != null) {
                        }
                        while (true) {
                            i2 = i;
                            dataRequestDispatcher = dataRequestDispatcher2;
                            if (i2 < requests2.size()) {
                            }
                            i = i2 + 1;
                            dataRequestDispatcher2 = dataRequestDispatcher;
                        }
                    }
                } else {
                    vector = requests;
                    StringBuilder stringBuilder3;
                    StringBuilder stringBuilder4;
                    if (os3 != 200) {
                        try {
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Bad Response Code ");
                            stringBuilder3.append(os3);
                            stringBuilder3.append(" ");
                            stringBuilder3.append(drdDebug2.toString());
                            Log.logToScreen(stringBuilder3.toString());
                            if (os3 == 500) {
                                StringBuffer debugMessage = new StringBuffer("Server 500 for request types: ");
                                int index = 0;
                                while (true) {
                                    addToUrl2 = addToUrl;
                                    try {
                                        if (index >= requests.size()) {
                                            break;
                                        }
                                        DataRequest addToUrl3 = (DataRequest) vector.elementAt(index);
                                        addToUrl3.onServerFailure();
                                        baos3 = baos2;
                                        debugMessage.append(addToUrl3.getRequestType());
                                        if (index != requests.size() - 1) {
                                            debugMessage.append(',');
                                        }
                                        index++;
                                        addToUrl = addToUrl2;
                                        baos2 = baos3;
                                    } catch (Throwable th222222) {
                                        contentType2 = contentType;
                                        responseCode = os3;
                                        firstByteTime = firstByteTime2;
                                        i = 0;
                                        contentType = th222222;
                                        os = os5;
                                        requests2 = vector;
                                        os2 = os;
                                        osSkipClose = false;
                                        drdDebug = drdDebug2;
                                        Log.logToScreen(drdDebug.toString());
                                        if (is2 != null) {
                                        }
                                        os2.close();
                                        if (hc != null) {
                                        }
                                        while (true) {
                                            i2 = i;
                                            dataRequestDispatcher = dataRequestDispatcher2;
                                            if (i2 < requests2.size()) {
                                            }
                                            i = i2 + 1;
                                            dataRequestDispatcher2 = dataRequestDispatcher;
                                        }
                                    }
                                }
                                if (dataRequestDispatcher2.debug) {
                                    dataRequestDispatcher2.notifyNetworkError(7, dataRequestDispatcher2.connectionFactory.getNetworkWorked(), debugMessage.toString());
                                }
                            } else {
                                baos3 = null;
                            }
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Bad HTTP response code: ");
                            stringBuilder4.append(os3);
                            throw new IOException(stringBuilder4.toString());
                        } catch (Throwable th2222222) {
                            addToUrl2 = addToUrl;
                            baos3 = null;
                            contentType2 = contentType;
                            responseCode = os3;
                            firstByteTime = firstByteTime2;
                            i = 0;
                            contentType = th2222222;
                            os = os5;
                            requests2 = vector;
                            os2 = os;
                            osSkipClose = false;
                            drdDebug = drdDebug2;
                            Log.logToScreen(drdDebug.toString());
                            if (is2 != null) {
                            }
                            os2.close();
                            if (hc != null) {
                            }
                            while (true) {
                                i2 = i;
                                dataRequestDispatcher = dataRequestDispatcher2;
                                if (i2 < requests2.size()) {
                                }
                                i = i2 + 1;
                                dataRequestDispatcher2 = dataRequestDispatcher;
                            }
                        }
                    } else {
                        addToUrl2 = addToUrl;
                        baos3 = null;
                        try {
                            if ("application/binary".equals(contentType)) {
                                String contentType4 = contentType;
                                int responseCode2 = os3;
                                try {
                                    addToUrl = (int) hc.getLength();
                                    dataRequestDispatcher2.bytesReceived += addToUrl;
                                    int serverProtocolVersion = is2.readUnsignedShort();
                                    if (serverProtocolVersion == 23) {
                                        i2 = 0;
                                        while (true) {
                                            DataOutputStream contentType5 = i2;
                                            if (contentType5 >= requests.size()) {
                                                break;
                                            }
                                            DataRequest os6;
                                            try {
                                                os6 = (DataRequest) vector.elementAt(contentType5);
                                                dataRequestDispatcher2.processDataRequest(is2, os6, dispatcherServer2);
                                                i2 = contentType5 + 1;
                                            } catch (IOException e5) {
                                                is = is2;
                                                os4 = os5;
                                                IOException is3 = e5;
                                                StringBuilder stringBuilder5 = new StringBuilder();
                                                stringBuilder5.append("IOException: ");
                                                stringBuilder5.append(os6.getRequestType());
                                                Log.logToScreen(stringBuilder5.toString());
                                                if (dataRequestDispatcher2.debug) {
                                                    printStream = System.err;
                                                    stringBuilder = new StringBuilder();
                                                    stringBuilder.append("IOException processing: ");
                                                    stringBuilder.append(os6.getRequestType());
                                                    printStream.println(stringBuilder.toString());
                                                    e5.printStackTrace();
                                                }
                                                if (e5 instanceof EOFException) {
                                                    os6.onServerFailure();
                                                    if (dataRequestDispatcher2.debug) {
                                                        int requestType = os6.getRequestType();
                                                        os5 = new StringBuilder();
                                                        os5.append("No server support for data request: ");
                                                        os5.append(requestType);
                                                        dataRequest = os6;
                                                        dataRequestDispatcher2.notifyNetworkError(7, dataRequestDispatcher2.connectionFactory.getNetworkWorked(), os5.toString());
                                                        for (os3 = null; os3 < contentType5; os3++) {
                                                            vector.removeElementAt(0);
                                                        }
                                                        throw e5;
                                                    }
                                                }
                                                dataRequest = os6;
                                                while (os3 < contentType5) {
                                                }
                                                throw e5;
                                            } catch (RuntimeException e6) {
                                                RuntimeException runtimeException = e6;
                                                stringBuilder4 = new StringBuilder();
                                                is = is2;
                                                stringBuilder4.append("RuntimeException: ");
                                                stringBuilder4.append(os6.getRequestType());
                                                Log.logToScreen(stringBuilder4.toString());
                                                if (dataRequestDispatcher2.debug) {
                                                    printStream = System.err;
                                                    stringBuilder4 = new StringBuilder();
                                                    os4 = os5;
                                                    stringBuilder4.append("RuntimeException processing: ");
                                                    stringBuilder4.append(os6.getRequestType());
                                                    printStream.println(stringBuilder4.toString());
                                                    e6.printStackTrace();
                                                }
                                                throw e6;
                                            } catch (Throwable th22222222) {
                                                contentType = th22222222;
                                                i = 0;
                                                firstByteTime = firstByteTime2;
                                                responseCode = responseCode2;
                                                contentType2 = contentType4;
                                                is2 = is;
                                                os = os4;
                                            }
                                        }
                                    } else {
                                        try {
                                            dataRequestDispatcher2.maybeNotifyNetworkError(1);
                                            Log.logToScreen(drdDebug2.toString());
                                            if (is2 != null) {
                                                try {
                                                    is2.close();
                                                } catch (IOException e7) {
                                                }
                                            }
                                            if (os5 != null && null == null) {
                                                try {
                                                    os5.close();
                                                } catch (IOException e8) {
                                                }
                                            }
                                            if (hc != null) {
                                                try {
                                                    hc.close();
                                                } catch (IOException e9) {
                                                }
                                            }
                                            i4 = 0;
                                            while (true) {
                                                i2 = i4;
                                                if (i2 < requests.size()) {
                                                    contentType3 = (DataRequest) vector.elementAt(i2);
                                                    if (contentType3.retryOnFailure() == null) {
                                                        vector.removeElement(contentType3);
                                                    }
                                                    i4 = i2 + 1;
                                                } else {
                                                    return;
                                                }
                                            }
                                        } catch (Throwable th222222222) {
                                            contentType = th222222222;
                                            firstByteTime = firstByteTime2;
                                            responseCode = responseCode2;
                                            contentType2 = contentType4;
                                            i = 0;
                                            os = os5;
                                            requests2 = vector;
                                            os2 = os;
                                            osSkipClose = false;
                                            drdDebug = drdDebug2;
                                            Log.logToScreen(drdDebug.toString());
                                            if (is2 != null) {
                                                try {
                                                    is2.close();
                                                } catch (IOException e10) {
                                                }
                                            }
                                            if (!(os2 == null || osSkipClose)) {
                                                os2.close();
                                            }
                                            if (hc != null) {
                                                try {
                                                    hc.close();
                                                } catch (IOException e11) {
                                                }
                                            }
                                            while (true) {
                                                i2 = i;
                                                dataRequestDispatcher = dataRequestDispatcher2;
                                                if (i2 < requests2.size()) {
                                                    DataRequest dataRequest3 = (DataRequest) requests2.elementAt(i2);
                                                    if (!dataRequest3.retryOnFailure()) {
                                                        requests2.removeElement(dataRequest3);
                                                    }
                                                    i = i2 + 1;
                                                    dataRequestDispatcher2 = dataRequestDispatcher;
                                                } else {
                                                    throw contentType;
                                                }
                                            }
                                        }
                                    }
                                } catch (Throwable th2222222222) {
                                    googleHttpConnection = hc;
                                    firstByteTime = firstByteTime2;
                                    responseCode = responseCode2;
                                    contentType2 = contentType4;
                                    i = 0;
                                    dataInputStream = is2;
                                    os = os5;
                                    contentType = th2222222222;
                                }
                            } else {
                                contentType2 = contentType;
                                responseCode = os3;
                                googleHttpConnection = hc;
                                firstByteTime = firstByteTime2;
                                i = 0;
                                dataInputStream = is2;
                                os = os5;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("Bad HTTP content type: ");
                                stringBuilder3.append(contentType2);
                                stringBuilder3.append(" ");
                                stringBuilder3.append(drdDebug2.toString());
                                Log.logToScreen(stringBuilder3.toString());
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Bad HTTP content type: ");
                                stringBuilder2.append(contentType2);
                                throw new IOException(stringBuilder2.toString());
                            }
                        } catch (Throwable th22222222222) {
                            contentType2 = contentType;
                            responseCode = os3;
                            googleHttpConnection = hc;
                            firstByteTime = firstByteTime2;
                            i = 0;
                            dataInputStream = is2;
                            os = os5;
                            contentType = th22222222222;
                            requests2 = vector;
                            os2 = os;
                            osSkipClose = false;
                            drdDebug = drdDebug2;
                            Log.logToScreen(drdDebug.toString());
                            if (is2 != null) {
                            }
                            os2.close();
                            if (hc != null) {
                            }
                            while (true) {
                                i2 = i;
                                dataRequestDispatcher = dataRequestDispatcher2;
                                if (i2 < requests2.size()) {
                                }
                                i = i2 + 1;
                                dataRequestDispatcher2 = dataRequestDispatcher;
                            }
                        }
                    }
                }
                if (os != null && null == null) {
                    try {
                        os.close();
                    } catch (IOException e12) {
                    }
                }
                if (googleHttpConnection != null) {
                    try {
                        googleHttpConnection.close();
                    } catch (IOException e13) {
                    }
                }
                while (true) {
                    i2 = i;
                    if (i2 >= requests.size()) {
                        contentType3 = (DataRequest) vector.elementAt(i2);
                        if (contentType3.retryOnFailure() == null) {
                            vector.removeElement(contentType3);
                        }
                        i = i2 + 1;
                    } else {
                        return;
                    }
                }
                if (googleHttpConnection != null) {
                }
                while (true) {
                    i2 = i;
                    if (i2 >= requests.size()) {
                    }
                    i = i2 + 1;
                }
            } catch (Throwable th7) {
                th22222222222 = th7;
                responseCode = os3;
                googleHttpConnection = hc;
                addToUrl2 = addToUrl;
                firstByteTime = firstByteTime2;
                baos3 = null;
                i = 0;
                contentType2 = contentType;
                sendData2 = is2;
                os = os5;
                contentType = th22222222222;
                requests2 = vector;
                os2 = os;
                osSkipClose = false;
                drdDebug = drdDebug2;
                Log.logToScreen(drdDebug.toString());
                if (is2 != null) {
                }
                os2.close();
                if (hc != null) {
                }
                while (true) {
                    i2 = i;
                    dataRequestDispatcher = dataRequestDispatcher2;
                    if (i2 < requests2.size()) {
                    }
                    i = i2 + 1;
                    dataRequestDispatcher2 = dataRequestDispatcher;
                }
            }
        } catch (Throwable th8) {
            th22222222222 = th8;
            addToUrl2 = addToUrl;
            sendData = sendData2;
            baos3 = null;
            i = 0;
            hc = null;
            os = null;
            contentType2 = null;
            responseCode = 0;
            startTime = 0;
            firstByteTime = 0;
            contentType = th22222222222;
            requests2 = vector;
            os2 = os;
            osSkipClose = false;
            drdDebug = drdDebug2;
            Log.logToScreen(drdDebug.toString());
            if (is2 != null) {
            }
            os2.close();
            if (hc != null) {
            }
            while (true) {
                i2 = i;
                dataRequestDispatcher = dataRequestDispatcher2;
                if (i2 < requests2.size()) {
                }
                i = i2 + 1;
                dataRequestDispatcher2 = dataRequestDispatcher;
            }
        }
    }

    protected void processDataRequest(DataInput is, DataRequest dataRequest, DispatcherServer dispatcherServer) throws IOException {
        int requestType = is.readUnsignedByte();
        if (requestType != dataRequest.getRequestType()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RT: ");
            stringBuilder.append(requestType);
            stringBuilder.append(" != ");
            stringBuilder.append(dataRequest.getRequestType());
            throw new IOException(stringBuilder.toString());
        } else if (!dataRequest.readResponseData(is)) {
            dispatcherServer.serverRequests.insertElementAt(dataRequest, 0);
        } else if (dataRequest != this && !dataRequest.isCancelled()) {
            notifyComplete(dataRequest);
        }
    }

    public void generateRequest(Vector requests, OutputStream outputStream, DispatcherServer dispatcherServer) throws IOException {
        DataOutputStream out = new DataOutputStream(outputStream);
        addClientPropertiesRequest(requests, dispatcherServer);
        if (dispatcherServer.headerFlag == (byte) 0) {
            out.writeShort(23);
            out.writeLong(this.cookie);
            out.writeUTF(Config.getLocale());
            out.writeUTF(this.platformID);
            out.writeUTF(this.softwareVersion);
            out.writeUTF(this.distributionChannel);
        } else if (dispatcherServer.headerFlag == (byte) 1) {
            out.writeShort(23);
            out.writeLong(this.cookie);
            out.writeUTF("");
            out.writeUTF("");
            out.writeUTF("");
            out.writeUTF("");
        }
        for (int i = 0; i < requests.size(); i++) {
            DataRequest dataRequest = (DataRequest) requests.elementAt(i);
            out.writeByte(dataRequest.getRequestType());
            dataRequest.writeRequestData(out);
        }
        out.flush();
    }

    private void addClientPropertiesRequest(Vector requests, DispatcherServer dispatcherServer) {
        if (dispatcherServer.canHandle(62)) {
            ClientPropertiesRequest clientProperties = new ClientPropertiesRequest(this.properties);
            if (requests.size() <= 0) {
                requests.insertElementAt(clientProperties, 0);
            } else if (((DataRequest) requests.elementAt(0)) instanceof ClientPropertiesRequest) {
                requests.setElementAt(clientProperties, 0);
            } else {
                requests.insertElementAt(clientProperties, 0);
            }
        }
    }

    public final void addSimpleRequest(int requestType, byte[] data, boolean immediate, boolean foreground) {
        addDataRequest(new SimpleDataRequest(requestType, data, immediate, foreground));
    }

    protected static boolean containsForegroundRequest(Vector requests) {
        for (int i = 0; i < requests.size(); i++) {
            if (((DataRequest) requests.elementAt(i)).isForeground()) {
                return true;
            }
        }
        return false;
    }

    protected static boolean containsSubmissionRequest(Vector requests) {
        for (int i = 0; i < requests.size(); i++) {
            if (((DataRequest) requests.elementAt(i)).isSubmission()) {
                return true;
            }
        }
        return false;
    }

    static void saveCookie(long cookie) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new DataOutputStream(baos).writeLong(cookie);
            PersistentStore store = Config.getInstance().getPersistentStore();
            store.setPreference("SessionID", baos.toByteArray());
            store.savePreferences();
        } catch (IOException e) {
            throw new RuntimeException(e.toString());
        }
    }

    protected String urlArguments(Vector requests) {
        StringBuffer addToUrl = new StringBuffer();
        String separator = "?";
        if (!TextUtil.isEmpty(this.globalSpecialUrlArguments)) {
            addToUrl.append(separator);
            addToUrl.append(this.globalSpecialUrlArguments);
            separator = "&";
        }
        for (int i = 0; i < requests.size(); i++) {
            DataRequest request = (DataRequest) requests.elementAt(i);
            if (request instanceof NeedsSpecialUrl) {
                String param = ((NeedsSpecialUrl) request).getParams();
                if (!TextUtil.isEmpty(param)) {
                    addToUrl.append(separator);
                    addToUrl.append(param);
                    separator = "&";
                }
            }
        }
        String addString = addToUrl.toString();
        TextUtil.isEmpty(addString);
        return addString;
    }

    public void setAndroidMapKey(String mapKey) {
        this.properties.setString(17, mapKey);
    }

    public void setAndroidSignature(String signature) {
        this.properties.setString(18, signature);
    }

    public void setAndroidLoggingId2(String androidLoggingId2) {
        this.properties.setString(19, androidLoggingId2);
    }

    public void setApplicationName(String applicationName) {
        this.properties.setString(5, applicationName);
    }

    public void resetConnectionFactory() {
        this.connectionFactory = Config.getInstance().getConnectionFactory();
    }
}
