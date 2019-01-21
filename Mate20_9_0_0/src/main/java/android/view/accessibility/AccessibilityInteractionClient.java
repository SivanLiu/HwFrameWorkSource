package android.view.accessibility;

import android.accessibilityservice.IAccessibilityServiceConnection;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityCache.AccessibilityNodeRefresher;
import android.view.accessibility.IAccessibilityInteractionConnectionCallback.Stub;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public final class AccessibilityInteractionClient extends Stub {
    private static final boolean CHECK_INTEGRITY = true;
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "AccessibilityInteractionClient";
    public static final int NO_ID = -1;
    private static final long TIMEOUT_INTERACTION_MILLIS = 5000;
    private static AccessibilityCache sAccessibilityCache = new AccessibilityCache(new AccessibilityNodeRefresher());
    private static final LongSparseArray<AccessibilityInteractionClient> sClients = new LongSparseArray();
    private static final SparseArray<IAccessibilityServiceConnection> sConnectionCache = new SparseArray();
    private static final Object sStaticLock = new Object();
    private AccessibilityNodeInfo mFindAccessibilityNodeInfoResult;
    private List<AccessibilityNodeInfo> mFindAccessibilityNodeInfosResult;
    private final Object mInstanceLock = new Object();
    private volatile int mInteractionId = -1;
    private final AtomicInteger mInteractionIdCounter = new AtomicInteger();
    private boolean mPerformAccessibilityActionResult;
    private Message mSameThreadMessage;

    public static AccessibilityInteractionClient getInstance() {
        return getInstanceForThread(Thread.currentThread().getId());
    }

    public static AccessibilityInteractionClient getInstanceForThread(long threadId) {
        AccessibilityInteractionClient client;
        synchronized (sStaticLock) {
            client = (AccessibilityInteractionClient) sClients.get(threadId);
            if (client == null) {
                client = new AccessibilityInteractionClient();
                sClients.put(threadId, client);
            }
        }
        return client;
    }

    public static IAccessibilityServiceConnection getConnection(int connectionId) {
        IAccessibilityServiceConnection iAccessibilityServiceConnection;
        synchronized (sConnectionCache) {
            iAccessibilityServiceConnection = (IAccessibilityServiceConnection) sConnectionCache.get(connectionId);
        }
        return iAccessibilityServiceConnection;
    }

    public static void addConnection(int connectionId, IAccessibilityServiceConnection connection) {
        synchronized (sConnectionCache) {
            sConnectionCache.put(connectionId, connection);
        }
    }

    public static void removeConnection(int connectionId) {
        synchronized (sConnectionCache) {
            sConnectionCache.remove(connectionId);
        }
    }

    @VisibleForTesting
    public static void setCache(AccessibilityCache cache) {
        sAccessibilityCache = cache;
    }

    private AccessibilityInteractionClient() {
    }

    public void setSameThreadMessage(Message message) {
        synchronized (this.mInstanceLock) {
            this.mSameThreadMessage = message;
            this.mInstanceLock.notifyAll();
        }
    }

    public AccessibilityNodeInfo getRootInActiveWindow(int connectionId) {
        return findAccessibilityNodeInfoByAccessibilityId(connectionId, Integer.MAX_VALUE, AccessibilityNodeInfo.ROOT_NODE_ID, false, 4, null);
    }

    public AccessibilityWindowInfo getWindow(int connectionId, int accessibilityWindowId) {
        long identityToken;
        try {
            IAccessibilityServiceConnection connection = getConnection(connectionId);
            if (connection != null) {
                AccessibilityWindowInfo window = sAccessibilityCache.getWindow(accessibilityWindowId);
                if (window != null) {
                    return window;
                }
                identityToken = Binder.clearCallingIdentity();
                window = connection.getWindow(accessibilityWindowId);
                Binder.restoreCallingIdentity(identityToken);
                if (window != null) {
                    sAccessibilityCache.addWindow(window);
                    return window;
                }
            }
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error while calling remote getWindow", re);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identityToken);
        }
        return null;
    }

    public List<AccessibilityWindowInfo> getWindows(int connectionId) {
        long identityToken;
        try {
            IAccessibilityServiceConnection connection = getConnection(connectionId);
            if (connection != null) {
                List<AccessibilityWindowInfo> windows = sAccessibilityCache.getWindows();
                if (windows != null) {
                    return windows;
                }
                identityToken = Binder.clearCallingIdentity();
                windows = connection.getWindows();
                Binder.restoreCallingIdentity(identityToken);
                if (windows != null) {
                    sAccessibilityCache.setWindows(windows);
                    return windows;
                }
            }
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error while calling remote getWindows", re);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identityToken);
        }
        return Collections.emptyList();
    }

    public AccessibilityNodeInfo findAccessibilityNodeInfoByAccessibilityId(int connectionId, int accessibilityWindowId, long accessibilityNodeId, boolean bypassCache, int prefetchFlags, Bundle arguments) {
        RemoteException re;
        int i;
        Throwable th;
        boolean z = bypassCache;
        if ((prefetchFlags & 2) == 0 || (prefetchFlags & 1) != 0) {
            try {
                IAccessibilityServiceConnection connection = getConnection(connectionId);
                if (connection != null) {
                    int i2;
                    long j;
                    if (z) {
                        i2 = accessibilityWindowId;
                        j = accessibilityNodeId;
                    } else {
                        try {
                            i2 = accessibilityWindowId;
                            j = accessibilityNodeId;
                            AccessibilityNodeInfo cachedInfo = sAccessibilityCache.getNode(i2, j);
                            if (cachedInfo != null) {
                                return cachedInfo;
                            }
                        } catch (RemoteException e) {
                            re = e;
                            j = accessibilityNodeId;
                            i = connectionId;
                            Log.e(LOG_TAG, "Error while calling remote findAccessibilityNodeInfoByAccessibilityId", re);
                            return null;
                        }
                    }
                    int interactionId = this.mInteractionIdCounter.getAndIncrement();
                    long identityToken = Binder.clearCallingIdentity();
                    long identityToken2;
                    int interactionId2;
                    try {
                        IAccessibilityServiceConnection iAccessibilityServiceConnection = connection;
                        identityToken2 = identityToken;
                        interactionId2 = interactionId;
                        try {
                            String[] packageNames = iAccessibilityServiceConnection.findAccessibilityNodeInfoByAccessibilityId(i2, j, interactionId, this, prefetchFlags, Thread.currentThread().getId(), arguments);
                            Binder.restoreCallingIdentity(identityToken2);
                            if (packageNames != null) {
                                List<AccessibilityNodeInfo> infos = getFindAccessibilityNodeInfosResultAndClear(interactionId2);
                                try {
                                    finalizeAndCacheAccessibilityNodeInfos(infos, connectionId, z, packageNames);
                                    if (!(infos == null || infos.isEmpty())) {
                                        for (int i3 = 1; i3 < infos.size(); i3++) {
                                            ((AccessibilityNodeInfo) infos.get(i3)).recycle();
                                        }
                                        return (AccessibilityNodeInfo) infos.get(0);
                                    }
                                } catch (RemoteException e2) {
                                    re = e2;
                                    Log.e(LOG_TAG, "Error while calling remote findAccessibilityNodeInfoByAccessibilityId", re);
                                    return null;
                                }
                                return null;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            i = connectionId;
                            Binder.restoreCallingIdentity(identityToken2);
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        i = connectionId;
                        interactionId2 = interactionId;
                        IAccessibilityServiceConnection iAccessibilityServiceConnection2 = connection;
                        identityToken2 = identityToken;
                        Binder.restoreCallingIdentity(identityToken2);
                        throw th;
                    }
                }
                i = connectionId;
            } catch (RemoteException e3) {
                re = e3;
                i = connectionId;
                Log.e(LOG_TAG, "Error while calling remote findAccessibilityNodeInfoByAccessibilityId", re);
                return null;
            }
            return null;
        }
        throw new IllegalArgumentException("FLAG_PREFETCH_SIBLINGS requires FLAG_PREFETCH_PREDECESSORS");
    }

    private static String idToString(int accessibilityWindowId, long accessibilityNodeId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(accessibilityWindowId);
        stringBuilder.append("/");
        stringBuilder.append(AccessibilityNodeInfo.idToString(accessibilityNodeId));
        return stringBuilder.toString();
    }

    public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByViewId(int connectionId, int accessibilityWindowId, long accessibilityNodeId, String viewId) {
        int i;
        RemoteException re;
        try {
            IAccessibilityServiceConnection connection = getConnection(connectionId);
            if (connection != null) {
                int interactionId = this.mInteractionIdCounter.getAndIncrement();
                long identityToken = Binder.clearCallingIdentity();
                try {
                    String[] packageNames = connection.findAccessibilityNodeInfosByViewId(accessibilityWindowId, accessibilityNodeId, viewId, interactionId, this, Thread.currentThread().getId());
                    Binder.restoreCallingIdentity(identityToken);
                    if (packageNames != null) {
                        List<AccessibilityNodeInfo> infos = getFindAccessibilityNodeInfosResultAndClear(interactionId);
                        if (infos != null) {
                            finalizeAndCacheAccessibilityNodeInfos(infos, connectionId, false, packageNames);
                            return infos;
                        }
                    }
                } catch (RemoteException e) {
                    re = e;
                    Log.w(LOG_TAG, "Error while calling remote findAccessibilityNodeInfoByViewIdInActiveWindow", re);
                    return Collections.emptyList();
                } catch (Throwable th) {
                    i = connectionId;
                    Binder.restoreCallingIdentity(identityToken);
                }
            }
            i = connectionId;
        } catch (RemoteException e2) {
            re = e2;
            i = connectionId;
            Log.w(LOG_TAG, "Error while calling remote findAccessibilityNodeInfoByViewIdInActiveWindow", re);
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(int connectionId, int accessibilityWindowId, long accessibilityNodeId, String text) {
        int i;
        RemoteException re;
        try {
            IAccessibilityServiceConnection connection = getConnection(connectionId);
            if (connection != null) {
                int interactionId = this.mInteractionIdCounter.getAndIncrement();
                long identityToken = Binder.clearCallingIdentity();
                try {
                    String[] packageNames = connection.findAccessibilityNodeInfosByText(accessibilityWindowId, accessibilityNodeId, text, interactionId, this, Thread.currentThread().getId());
                    Binder.restoreCallingIdentity(identityToken);
                    if (packageNames != null) {
                        List<AccessibilityNodeInfo> infos = getFindAccessibilityNodeInfosResultAndClear(interactionId);
                        if (infos != null) {
                            finalizeAndCacheAccessibilityNodeInfos(infos, connectionId, false, packageNames);
                            return infos;
                        }
                    }
                } catch (RemoteException e) {
                    re = e;
                    Log.w(LOG_TAG, "Error while calling remote findAccessibilityNodeInfosByViewText", re);
                    return Collections.emptyList();
                } catch (Throwable th) {
                    i = connectionId;
                    Binder.restoreCallingIdentity(identityToken);
                }
            }
            i = connectionId;
        } catch (RemoteException e2) {
            re = e2;
            i = connectionId;
            Log.w(LOG_TAG, "Error while calling remote findAccessibilityNodeInfosByViewText", re);
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    public AccessibilityNodeInfo findFocus(int connectionId, int accessibilityWindowId, long accessibilityNodeId, int focusType) {
        int i;
        RemoteException re;
        try {
            IAccessibilityServiceConnection connection = getConnection(connectionId);
            if (connection != null) {
                int interactionId = this.mInteractionIdCounter.getAndIncrement();
                long identityToken = Binder.clearCallingIdentity();
                try {
                    String[] packageNames = connection.findFocus(accessibilityWindowId, accessibilityNodeId, focusType, interactionId, this, Thread.currentThread().getId());
                    Binder.restoreCallingIdentity(identityToken);
                    if (packageNames != null) {
                        AccessibilityNodeInfo info = getFindAccessibilityNodeInfoResultAndClear(interactionId);
                        finalizeAndCacheAccessibilityNodeInfo(info, connectionId, false, packageNames);
                        return info;
                    }
                } catch (RemoteException e) {
                    re = e;
                    Log.w(LOG_TAG, "Error while calling remote findFocus", re);
                    return null;
                } catch (Throwable th) {
                    i = connectionId;
                    Binder.restoreCallingIdentity(identityToken);
                }
            }
            i = connectionId;
        } catch (RemoteException e2) {
            re = e2;
            i = connectionId;
            Log.w(LOG_TAG, "Error while calling remote findFocus", re);
            return null;
        }
        return null;
    }

    public AccessibilityNodeInfo focusSearch(int connectionId, int accessibilityWindowId, long accessibilityNodeId, int direction) {
        int i;
        RemoteException re;
        try {
            IAccessibilityServiceConnection connection = getConnection(connectionId);
            if (connection != null) {
                int interactionId = this.mInteractionIdCounter.getAndIncrement();
                long identityToken = Binder.clearCallingIdentity();
                try {
                    String[] packageNames = connection.focusSearch(accessibilityWindowId, accessibilityNodeId, direction, interactionId, this, Thread.currentThread().getId());
                    Binder.restoreCallingIdentity(identityToken);
                    if (packageNames != null) {
                        AccessibilityNodeInfo info = getFindAccessibilityNodeInfoResultAndClear(interactionId);
                        finalizeAndCacheAccessibilityNodeInfo(info, connectionId, false, packageNames);
                        return info;
                    }
                } catch (RemoteException e) {
                    re = e;
                    Log.w(LOG_TAG, "Error while calling remote accessibilityFocusSearch", re);
                    return null;
                } catch (Throwable th) {
                    i = connectionId;
                    Binder.restoreCallingIdentity(identityToken);
                }
            }
            i = connectionId;
        } catch (RemoteException e2) {
            re = e2;
            i = connectionId;
            Log.w(LOG_TAG, "Error while calling remote accessibilityFocusSearch", re);
            return null;
        }
        return null;
    }

    public boolean performAccessibilityAction(int connectionId, int accessibilityWindowId, long accessibilityNodeId, int action, Bundle arguments) {
        long identityToken;
        try {
            IAccessibilityServiceConnection connection = getConnection(connectionId);
            if (connection != null) {
                int interactionId = this.mInteractionIdCounter.getAndIncrement();
                identityToken = Binder.clearCallingIdentity();
                boolean success = connection.performAccessibilityAction(accessibilityWindowId, accessibilityNodeId, action, arguments, interactionId, this, Thread.currentThread().getId());
                Binder.restoreCallingIdentity(identityToken);
                if (success) {
                    return getPerformAccessibilityActionResultAndClear(interactionId);
                }
            }
        } catch (RemoteException re) {
            Log.w(LOG_TAG, "Error while calling remote performAccessibilityAction", re);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identityToken);
        }
        return false;
    }

    public void clearCache() {
        sAccessibilityCache.clear();
    }

    public void onAccessibilityEvent(AccessibilityEvent event) {
        sAccessibilityCache.onAccessibilityEvent(event);
    }

    private AccessibilityNodeInfo getFindAccessibilityNodeInfoResultAndClear(int interactionId) {
        AccessibilityNodeInfo result;
        synchronized (this.mInstanceLock) {
            result = waitForResultTimedLocked(interactionId) ? this.mFindAccessibilityNodeInfoResult : null;
            clearResultLocked();
        }
        return result;
    }

    public void setFindAccessibilityNodeInfoResult(AccessibilityNodeInfo info, int interactionId) {
        synchronized (this.mInstanceLock) {
            if (interactionId > this.mInteractionId) {
                this.mFindAccessibilityNodeInfoResult = info;
                this.mInteractionId = interactionId;
            }
            this.mInstanceLock.notifyAll();
        }
    }

    private List<AccessibilityNodeInfo> getFindAccessibilityNodeInfosResultAndClear(int interactionId) {
        List<AccessibilityNodeInfo> result;
        synchronized (this.mInstanceLock) {
            if (waitForResultTimedLocked(interactionId)) {
                result = this.mFindAccessibilityNodeInfosResult;
            } else {
                result = Collections.emptyList();
            }
            clearResultLocked();
            if (Build.IS_DEBUGGABLE) {
                checkFindAccessibilityNodeInfoResultIntegrity(result);
            }
        }
        return result;
    }

    public void setFindAccessibilityNodeInfosResult(List<AccessibilityNodeInfo> infos, int interactionId) {
        synchronized (this.mInstanceLock) {
            if (interactionId > this.mInteractionId) {
                if (infos != null) {
                    if (Binder.getCallingPid() != Process.myPid()) {
                        this.mFindAccessibilityNodeInfosResult = infos;
                    } else {
                        this.mFindAccessibilityNodeInfosResult = new ArrayList(infos);
                    }
                } else {
                    this.mFindAccessibilityNodeInfosResult = Collections.emptyList();
                }
                this.mInteractionId = interactionId;
            }
            this.mInstanceLock.notifyAll();
        }
    }

    private boolean getPerformAccessibilityActionResultAndClear(int interactionId) {
        boolean result;
        synchronized (this.mInstanceLock) {
            result = waitForResultTimedLocked(interactionId) ? this.mPerformAccessibilityActionResult : false;
            clearResultLocked();
        }
        return result;
    }

    public void setPerformAccessibilityActionResult(boolean succeeded, int interactionId) {
        synchronized (this.mInstanceLock) {
            if (interactionId > this.mInteractionId) {
                this.mPerformAccessibilityActionResult = succeeded;
                this.mInteractionId = interactionId;
            }
            this.mInstanceLock.notifyAll();
        }
    }

    private void clearResultLocked() {
        this.mInteractionId = -1;
        this.mFindAccessibilityNodeInfoResult = null;
        this.mFindAccessibilityNodeInfosResult = null;
        this.mPerformAccessibilityActionResult = false;
    }

    private boolean waitForResultTimedLocked(int interactionId) {
        long startTimeMillis = SystemClock.uptimeMillis();
        while (true) {
            try {
                Message sameProcessMessage = getSameProcessMessageAndClear();
                if (sameProcessMessage != null) {
                    sameProcessMessage.getTarget().handleMessage(sameProcessMessage);
                }
                if (this.mInteractionId == interactionId) {
                    return true;
                }
                if (this.mInteractionId > interactionId) {
                    return false;
                }
                long waitTimeMillis = 5000 - (SystemClock.uptimeMillis() - startTimeMillis);
                if (waitTimeMillis <= 0) {
                    return false;
                }
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("old interaction Id is: ");
                stringBuilder.append(this.mInteractionId);
                stringBuilder.append(",current interaction Id is:");
                stringBuilder.append(interactionId);
                Log.w(str, stringBuilder.toString());
                this.mInstanceLock.wait(waitTimeMillis);
            } catch (InterruptedException e) {
            }
        }
    }

    private void finalizeAndCacheAccessibilityNodeInfo(AccessibilityNodeInfo info, int connectionId, boolean bypassCache, String[] packageNames) {
        if (info != null) {
            info.setConnectionId(connectionId);
            if (!ArrayUtils.isEmpty(packageNames)) {
                CharSequence packageName = info.getPackageName();
                if (packageName == null || !ArrayUtils.contains(packageNames, packageName.toString())) {
                    info.setPackageName(packageNames[0]);
                }
            }
            info.setSealed(true);
            if (!bypassCache) {
                sAccessibilityCache.add(info);
            }
        }
    }

    private void finalizeAndCacheAccessibilityNodeInfos(List<AccessibilityNodeInfo> infos, int connectionId, boolean bypassCache, String[] packageNames) {
        if (infos != null) {
            int infosCount = infos.size();
            for (int i = 0; i < infosCount; i++) {
                finalizeAndCacheAccessibilityNodeInfo((AccessibilityNodeInfo) infos.get(i), connectionId, bypassCache, packageNames);
            }
        }
    }

    private Message getSameProcessMessageAndClear() {
        Message result;
        synchronized (this.mInstanceLock) {
            result = this.mSameThreadMessage;
            this.mSameThreadMessage = null;
        }
        return result;
    }

    private void checkFindAccessibilityNodeInfoResultIntegrity(List<AccessibilityNodeInfo> infos) {
        if (infos.size() != 0) {
            AccessibilityNodeInfo candidate;
            AccessibilityNodeInfo root = (AccessibilityNodeInfo) infos.get(0);
            int infoCount = infos.size();
            for (int i = 1; i < infoCount; i++) {
                for (int j = i; j < infoCount; j++) {
                    candidate = (AccessibilityNodeInfo) infos.get(j);
                    if (root.getParentNodeId() == candidate.getSourceNodeId()) {
                        root = candidate;
                        break;
                    }
                }
            }
            if (root == null) {
                Log.e(LOG_TAG, "No root.");
            }
            HashSet<AccessibilityNodeInfo> seen = new HashSet();
            Queue<AccessibilityNodeInfo> fringe = new LinkedList();
            fringe.add(root);
            while (!fringe.isEmpty()) {
                candidate = (AccessibilityNodeInfo) fringe.poll();
                if (seen.add(candidate)) {
                    int childCount = candidate.getChildCount();
                    for (int i2 = 0; i2 < childCount; i2++) {
                        long childId = candidate.getChildId(i2);
                        for (int j2 = 0; j2 < infoCount; j2++) {
                            AccessibilityNodeInfo child = (AccessibilityNodeInfo) infos.get(j2);
                            if (child.getSourceNodeId() == childId) {
                                fringe.add(child);
                            }
                        }
                    }
                } else {
                    Log.e(LOG_TAG, "Duplicate node.");
                    return;
                }
            }
            int disconnectedCount = infos.size() - seen.size();
            if (disconnectedCount > 0) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(disconnectedCount);
                stringBuilder.append(" Disconnected nodes.");
                Log.e(str, stringBuilder.toString());
            }
        }
    }
}
