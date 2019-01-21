package android.view.accessibility;

import android.os.Build;
import android.util.ArraySet;
import android.util.Log;
import android.util.LongArray;
import android.util.LongSparseArray;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.List;

public class AccessibilityCache {
    public static final int CACHE_CRITICAL_EVENTS_MASK = 4307005;
    private static final boolean CHECK_INTEGRITY = Build.IS_ENG;
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "AccessibilityCache";
    private long mAccessibilityFocus = 2147483647L;
    private final AccessibilityNodeRefresher mAccessibilityNodeRefresher;
    private long mInputFocus = 2147483647L;
    private boolean mIsAllWindowsCached;
    private final Object mLock = new Object();
    private final SparseArray<LongSparseArray<AccessibilityNodeInfo>> mNodeCache = new SparseArray();
    private final SparseArray<AccessibilityWindowInfo> mTempWindowArray = new SparseArray();
    private final SparseArray<AccessibilityWindowInfo> mWindowCache = new SparseArray();

    public static class AccessibilityNodeRefresher {
        public boolean refreshNode(AccessibilityNodeInfo info, boolean bypassCache) {
            return info.refresh(null, bypassCache);
        }
    }

    public AccessibilityCache(AccessibilityNodeRefresher nodeRefresher) {
        this.mAccessibilityNodeRefresher = nodeRefresher;
    }

    public void setWindows(List<AccessibilityWindowInfo> windows) {
        synchronized (this.mLock) {
            clearWindowCache();
            if (windows == null) {
                return;
            }
            int windowCount = windows.size();
            for (int i = 0; i < windowCount; i++) {
                addWindow((AccessibilityWindowInfo) windows.get(i));
            }
            this.mIsAllWindowsCached = true;
        }
    }

    public void addWindow(AccessibilityWindowInfo window) {
        synchronized (this.mLock) {
            int windowId = window.getId();
            AccessibilityWindowInfo oldWindow = (AccessibilityWindowInfo) this.mWindowCache.get(windowId);
            if (oldWindow != null) {
                oldWindow.recycle();
            }
            this.mWindowCache.put(windowId, AccessibilityWindowInfo.obtain(window));
        }
    }

    public void onAccessibilityEvent(AccessibilityEvent event) {
        synchronized (this.mLock) {
            switch (event.getEventType()) {
                case 1:
                case 4:
                case 16:
                case 8192:
                    refreshCachedNodeLocked(event.getWindowId(), event.getSourceNodeId());
                    break;
                case 8:
                    if (this.mInputFocus != 2147483647L) {
                        refreshCachedNodeLocked(event.getWindowId(), this.mInputFocus);
                    }
                    this.mInputFocus = event.getSourceNodeId();
                    refreshCachedNodeLocked(event.getWindowId(), this.mInputFocus);
                    break;
                case 32:
                case 4194304:
                    clear();
                    break;
                case 2048:
                    synchronized (this.mLock) {
                        int windowId = event.getWindowId();
                        long sourceId = event.getSourceNodeId();
                        if ((event.getContentChangeTypes() & 1) != 0) {
                            clearSubTreeLocked(windowId, sourceId);
                        } else {
                            refreshCachedNodeLocked(windowId, sourceId);
                        }
                    }
                    break;
                case 4096:
                    clearSubTreeLocked(event.getWindowId(), event.getSourceNodeId());
                    break;
                case 32768:
                    if (this.mAccessibilityFocus != 2147483647L) {
                        refreshCachedNodeLocked(event.getWindowId(), this.mAccessibilityFocus);
                    }
                    this.mAccessibilityFocus = event.getSourceNodeId();
                    refreshCachedNodeLocked(event.getWindowId(), this.mAccessibilityFocus);
                    break;
                case 65536:
                    if (this.mAccessibilityFocus == event.getSourceNodeId()) {
                        refreshCachedNodeLocked(event.getWindowId(), this.mAccessibilityFocus);
                        this.mAccessibilityFocus = 2147483647L;
                        break;
                    }
                    break;
                default:
                    break;
            }
        }
        if (CHECK_INTEGRITY) {
            checkIntegrity();
        }
    }

    private void refreshCachedNodeLocked(int windowId, long sourceId) {
        LongSparseArray<AccessibilityNodeInfo> nodes = (LongSparseArray) this.mNodeCache.get(windowId);
        if (nodes != null) {
            AccessibilityNodeInfo cachedInfo = (AccessibilityNodeInfo) nodes.get(sourceId);
            if (cachedInfo != null && !this.mAccessibilityNodeRefresher.refreshNode(cachedInfo, true)) {
                clearSubTreeLocked(windowId, sourceId);
            }
        }
    }

    /* JADX WARNING: Missing block: B:12:0x001e, code skipped:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public AccessibilityNodeInfo getNode(int windowId, long accessibilityNodeId) {
        synchronized (this.mLock) {
            LongSparseArray<AccessibilityNodeInfo> nodes = (LongSparseArray) this.mNodeCache.get(windowId);
            if (nodes == null) {
                return null;
            }
            AccessibilityNodeInfo info = (AccessibilityNodeInfo) nodes.get(accessibilityNodeId);
            if (info != null) {
                info = AccessibilityNodeInfo.obtain(info);
            }
        }
    }

    public List<AccessibilityWindowInfo> getWindows() {
        synchronized (this.mLock) {
            if (this.mIsAllWindowsCached) {
                int windowCount = this.mWindowCache.size();
                if (windowCount > 0) {
                    int i;
                    SparseArray<AccessibilityWindowInfo> sortedWindows = this.mTempWindowArray;
                    sortedWindows.clear();
                    for (i = 0; i < windowCount; i++) {
                        AccessibilityWindowInfo window = (AccessibilityWindowInfo) this.mWindowCache.valueAt(i);
                        sortedWindows.put(window.getLayer(), window);
                    }
                    i = sortedWindows.size();
                    List<AccessibilityWindowInfo> windows = new ArrayList(i);
                    for (int i2 = i - 1; i2 >= 0; i2--) {
                        windows.add(AccessibilityWindowInfo.obtain((AccessibilityWindowInfo) sortedWindows.valueAt(i2)));
                        sortedWindows.removeAt(i2);
                    }
                    return windows;
                }
                return null;
            }
            return null;
        }
    }

    public AccessibilityWindowInfo getWindow(int windowId) {
        synchronized (this.mLock) {
            AccessibilityWindowInfo window = (AccessibilityWindowInfo) this.mWindowCache.get(windowId);
            if (window != null) {
                AccessibilityWindowInfo obtain = AccessibilityWindowInfo.obtain(window);
                return obtain;
            }
            return null;
        }
    }

    /* JADX WARNING: Missing block: B:32:0x007b, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void add(AccessibilityNodeInfo info) {
        synchronized (this.mLock) {
            int windowId = info.getWindowId();
            LongSparseArray<AccessibilityNodeInfo> nodes = (LongSparseArray) this.mNodeCache.get(windowId);
            if (nodes == null) {
                nodes = new LongSparseArray();
                this.mNodeCache.put(windowId, nodes);
            }
            long sourceId = info.getSourceNodeId();
            AccessibilityNodeInfo oldInfo = (AccessibilityNodeInfo) nodes.get(sourceId);
            if (oldInfo != null) {
                LongArray newChildrenIds = info.getChildNodeIds();
                int oldChildCount = oldInfo.getChildCount();
                for (int i = 0; i < oldChildCount; i++) {
                    if (nodes.get(sourceId) == null) {
                        clearNodesForWindowLocked(windowId);
                        return;
                    }
                    long oldChildId = oldInfo.getChildId(i);
                    if (newChildrenIds == null || newChildrenIds.indexOf(oldChildId) < 0) {
                        clearSubTreeLocked(windowId, oldChildId);
                    }
                }
                long oldParentId = oldInfo.getParentNodeId();
                if (info.getParentNodeId() != oldParentId) {
                    clearSubTreeLocked(windowId, oldParentId);
                } else {
                    oldInfo.recycle();
                }
            }
            AccessibilityNodeInfo clone = AccessibilityNodeInfo.obtain(info);
            nodes.put(sourceId, clone);
            if (clone.isAccessibilityFocused()) {
                this.mAccessibilityFocus = sourceId;
            }
            if (clone.isFocused()) {
                this.mInputFocus = sourceId;
            }
        }
    }

    public void clear() {
        synchronized (this.mLock) {
            clearWindowCache();
            int nodesForWindowCount = this.mNodeCache.size();
            for (int i = 0; i < nodesForWindowCount; i++) {
                clearNodesForWindowLocked(this.mNodeCache.keyAt(i));
            }
            this.mAccessibilityFocus = 2147483647L;
            this.mInputFocus = 2147483647L;
        }
    }

    private void clearWindowCache() {
        for (int i = this.mWindowCache.size() - 1; i >= 0; i--) {
            ((AccessibilityWindowInfo) this.mWindowCache.valueAt(i)).recycle();
            this.mWindowCache.removeAt(i);
        }
        this.mIsAllWindowsCached = false;
    }

    private void clearNodesForWindowLocked(int windowId) {
        LongSparseArray<AccessibilityNodeInfo> nodes = (LongSparseArray) this.mNodeCache.get(windowId);
        if (nodes != null) {
            for (int i = nodes.size() - 1; i >= 0; i--) {
                AccessibilityNodeInfo info = (AccessibilityNodeInfo) nodes.valueAt(i);
                nodes.removeAt(i);
                info.recycle();
            }
            this.mNodeCache.remove(windowId);
        }
    }

    private void clearSubTreeLocked(int windowId, long rootNodeId) {
        LongSparseArray<AccessibilityNodeInfo> nodes = (LongSparseArray) this.mNodeCache.get(windowId);
        if (nodes != null) {
            clearSubTreeRecursiveLocked(nodes, rootNodeId);
        }
    }

    private void clearSubTreeRecursiveLocked(LongSparseArray<AccessibilityNodeInfo> nodes, long rootNodeId) {
        AccessibilityNodeInfo current = (AccessibilityNodeInfo) nodes.get(rootNodeId);
        if (current != null) {
            nodes.remove(rootNodeId);
            int childCount = current.getChildCount();
            for (int i = 0; i < childCount; i++) {
                clearSubTreeRecursiveLocked(nodes, current.getChildId(i));
            }
            current.recycle();
        }
    }

    public void checkIntegrity() {
        AccessibilityCache accessibilityCache = this;
        synchronized (accessibilityCache.mLock) {
            if (accessibilityCache.mWindowCache.size() > 0 || accessibilityCache.mNodeCache.size() != 0) {
                int i;
                String str;
                StringBuilder stringBuilder;
                int windowCount = accessibilityCache.mWindowCache.size();
                AccessibilityWindowInfo activeWindow = null;
                AccessibilityWindowInfo focusedWindow = null;
                for (i = 0; i < windowCount; i++) {
                    AccessibilityWindowInfo window = (AccessibilityWindowInfo) accessibilityCache.mWindowCache.valueAt(i);
                    if (window.isActive()) {
                        if (activeWindow != null) {
                            str = LOG_TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Duplicate active window:");
                            stringBuilder.append(window);
                            Log.e(str, stringBuilder.toString());
                        } else {
                            activeWindow = window;
                        }
                    }
                    if (window.isFocused()) {
                        if (focusedWindow != null) {
                            str = LOG_TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Duplicate focused window:");
                            stringBuilder.append(window);
                            Log.e(str, stringBuilder.toString());
                        } else {
                            focusedWindow = window;
                        }
                    }
                }
                int nodesForWindowCount = accessibilityCache.mNodeCache.size();
                AccessibilityNodeInfo inputFocus = null;
                AccessibilityNodeInfo accessFocus = null;
                i = 0;
                while (i < nodesForWindowCount) {
                    AccessibilityWindowInfo focusedWindow2;
                    int windowCount2;
                    AccessibilityWindowInfo activeWindow2;
                    int nodesForWindowCount2;
                    LongSparseArray<AccessibilityNodeInfo> nodes = (LongSparseArray) accessibilityCache.mNodeCache.valueAt(i);
                    if (nodes.size() <= 0) {
                        focusedWindow2 = focusedWindow;
                        windowCount2 = windowCount;
                        activeWindow2 = activeWindow;
                        nodesForWindowCount2 = nodesForWindowCount;
                    } else {
                        ArraySet<AccessibilityNodeInfo> seen = new ArraySet();
                        int windowId = accessibilityCache.mNodeCache.keyAt(i);
                        int nodeCount = nodes.size();
                        AccessibilityNodeInfo inputFocus2 = inputFocus;
                        inputFocus = accessFocus;
                        int j = 0;
                        while (j < nodeCount) {
                            AccessibilityNodeInfo node = (AccessibilityNodeInfo) nodes.valueAt(j);
                            if (seen.add(node)) {
                                String str2;
                                AccessibilityNodeInfo accessFocus2;
                                focusedWindow2 = focusedWindow;
                                if (node.isAccessibilityFocused()) {
                                    if (inputFocus != null) {
                                        str2 = LOG_TAG;
                                        focusedWindow = new StringBuilder();
                                        focusedWindow.append("Duplicate accessibility focus:");
                                        focusedWindow.append(node);
                                        focusedWindow.append(" in window:");
                                        focusedWindow.append(windowId);
                                        Log.e(str2, focusedWindow.toString());
                                    } else {
                                        inputFocus = node;
                                    }
                                }
                                if (node.isFocused()) {
                                    if (inputFocus2 != null) {
                                        str2 = LOG_TAG;
                                        focusedWindow = new StringBuilder();
                                        focusedWindow.append("Duplicate input focus: ");
                                        focusedWindow.append(node);
                                        focusedWindow.append(" in window:");
                                        focusedWindow.append(windowId);
                                        Log.e(str2, focusedWindow.toString());
                                    } else {
                                        inputFocus2 = node;
                                    }
                                }
                                windowCount2 = windowCount;
                                AccessibilityNodeInfo nodeParent = (AccessibilityNodeInfo) nodes.get(node.getParentNodeId());
                                if (nodeParent != null) {
                                    int childCount;
                                    boolean childOfItsParent;
                                    focusedWindow = null;
                                    windowCount = nodeParent.getChildCount();
                                    int k = 0;
                                    while (k < windowCount) {
                                        boolean childOfItsParent2 = focusedWindow;
                                        childCount = windowCount;
                                        if (((AccessibilityNodeInfo) nodes.get(nodeParent.getChildId(k))) == node) {
                                            childOfItsParent = true;
                                            break;
                                        }
                                        k++;
                                        focusedWindow = childOfItsParent2;
                                        windowCount = childCount;
                                    }
                                    childCount = windowCount;
                                    childOfItsParent = focusedWindow;
                                    if (!childOfItsParent) {
                                        focusedWindow = LOG_TAG;
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("Invalid parent-child relation between parent: ");
                                        stringBuilder2.append(nodeParent);
                                        stringBuilder2.append(" and child: ");
                                        stringBuilder2.append(node);
                                        Log.e(focusedWindow, stringBuilder2.toString());
                                    }
                                }
                                focusedWindow = node.getChildCount();
                                windowCount = 0;
                                while (windowCount < focusedWindow) {
                                    int childCount2;
                                    activeWindow2 = activeWindow;
                                    AccessibilityNodeInfo child = (AccessibilityNodeInfo) nodes.get(node.getChildId(windowCount));
                                    if (child != null) {
                                        nodesForWindowCount2 = nodesForWindowCount;
                                        accessFocus2 = inputFocus;
                                        if (((AccessibilityNodeInfo) nodes.get(child.getParentNodeId())) != node) {
                                            str = LOG_TAG;
                                            stringBuilder = new StringBuilder();
                                            childCount2 = focusedWindow;
                                            stringBuilder.append("Invalid child-parent relation between child: ");
                                            stringBuilder.append(node);
                                            stringBuilder.append(" and parent: ");
                                            stringBuilder.append(nodeParent);
                                            Log.e(str, stringBuilder.toString());
                                        } else {
                                            childCount2 = focusedWindow;
                                        }
                                    } else {
                                        childCount2 = focusedWindow;
                                        nodesForWindowCount2 = nodesForWindowCount;
                                        accessFocus2 = inputFocus;
                                    }
                                    windowCount++;
                                    activeWindow = activeWindow2;
                                    nodesForWindowCount = nodesForWindowCount2;
                                    inputFocus = accessFocus2;
                                    focusedWindow = childCount2;
                                }
                                activeWindow2 = activeWindow;
                                nodesForWindowCount2 = nodesForWindowCount;
                                accessFocus2 = inputFocus;
                            } else {
                                String str3 = LOG_TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                focusedWindow2 = focusedWindow;
                                stringBuilder3.append("Duplicate node: ");
                                stringBuilder3.append(node);
                                stringBuilder3.append(" in window:");
                                stringBuilder3.append(windowId);
                                Log.e(str3, stringBuilder3.toString());
                                windowCount2 = windowCount;
                                activeWindow2 = activeWindow;
                                nodesForWindowCount2 = nodesForWindowCount;
                            }
                            j++;
                            focusedWindow = focusedWindow2;
                            windowCount = windowCount2;
                            activeWindow = activeWindow2;
                            nodesForWindowCount = nodesForWindowCount2;
                        }
                        focusedWindow2 = focusedWindow;
                        windowCount2 = windowCount;
                        activeWindow2 = activeWindow;
                        nodesForWindowCount2 = nodesForWindowCount;
                        accessFocus = inputFocus;
                        inputFocus = inputFocus2;
                    }
                    i++;
                    focusedWindow = focusedWindow2;
                    windowCount = windowCount2;
                    activeWindow = activeWindow2;
                    nodesForWindowCount = nodesForWindowCount2;
                    accessibilityCache = this;
                }
                return;
            }
        }
    }
}
