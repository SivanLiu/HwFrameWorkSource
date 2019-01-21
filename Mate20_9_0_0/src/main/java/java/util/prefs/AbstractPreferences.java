package java.util.prefs;

import java.io.IOException;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

public abstract class AbstractPreferences extends Preferences {
    private static final AbstractPreferences[] EMPTY_ABSTRACT_PREFS_ARRAY = new AbstractPreferences[0];
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static Thread eventDispatchThread = null;
    private static final List<EventObject> eventQueue = new LinkedList();
    private final String absolutePath;
    private Map<String, AbstractPreferences> kidCache = new HashMap();
    protected final Object lock = new Object();
    private final String name;
    protected boolean newNode = false;
    private final ArrayList<NodeChangeListener> nodeListeners = new ArrayList();
    final AbstractPreferences parent;
    private final ArrayList<PreferenceChangeListener> prefListeners = new ArrayList();
    private boolean removed = false;
    private final AbstractPreferences root;

    private static class EventDispatchThread extends Thread {
        private EventDispatchThread() {
        }

        /* synthetic */ EventDispatchThread(AnonymousClass1 x0) {
            this();
        }

        public void run() {
            while (true) {
                int i;
                EventObject event;
                synchronized (AbstractPreferences.eventQueue) {
                    while (AbstractPreferences.eventQueue.isEmpty()) {
                        try {
                            AbstractPreferences.eventQueue.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    i = 0;
                    event = (EventObject) AbstractPreferences.eventQueue.remove(0);
                }
                AbstractPreferences src = (AbstractPreferences) event.getSource();
                if (event instanceof PreferenceChangeEvent) {
                    PreferenceChangeEvent pce = (PreferenceChangeEvent) event;
                    PreferenceChangeListener[] listeners = src.prefListeners();
                    while (i < listeners.length) {
                        listeners[i].preferenceChange(pce);
                        i++;
                    }
                } else {
                    NodeChangeEvent nce = (NodeChangeEvent) event;
                    NodeChangeListener[] listeners2 = src.nodeListeners();
                    if (nce instanceof NodeAddedEvent) {
                        while (i < listeners2.length) {
                            listeners2[i].childAdded(nce);
                            i++;
                        }
                    } else {
                        while (i < listeners2.length) {
                            listeners2[i].childRemoved(nce);
                            i++;
                        }
                    }
                }
            }
        }
    }

    private class NodeAddedEvent extends NodeChangeEvent {
        private static final long serialVersionUID = -6743557530157328528L;

        NodeAddedEvent(Preferences parent, Preferences child) {
            super(parent, child);
        }
    }

    private class NodeRemovedEvent extends NodeChangeEvent {
        private static final long serialVersionUID = 8735497392918824837L;

        NodeRemovedEvent(Preferences parent, Preferences child) {
            super(parent, child);
        }
    }

    protected abstract AbstractPreferences childSpi(String str);

    protected abstract String[] childrenNamesSpi() throws BackingStoreException;

    protected abstract void flushSpi() throws BackingStoreException;

    protected abstract String getSpi(String str);

    protected abstract String[] keysSpi() throws BackingStoreException;

    protected abstract void putSpi(String str, String str2);

    protected abstract void removeNodeSpi() throws BackingStoreException;

    protected abstract void removeSpi(String str);

    protected abstract void syncSpi() throws BackingStoreException;

    protected AbstractPreferences(AbstractPreferences parent, String name) {
        StringBuilder stringBuilder;
        if (parent == null) {
            if (name.equals("")) {
                this.absolutePath = "/";
                this.root = this;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Root name '");
                stringBuilder.append(name);
                stringBuilder.append("' must be \"\"");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        } else if (name.indexOf(47) != -1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Name '");
            stringBuilder.append(name);
            stringBuilder.append("' contains '/'");
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (name.equals("")) {
            throw new IllegalArgumentException("Illegal name: empty string");
        } else {
            String stringBuilder2;
            this.root = parent.root;
            StringBuilder stringBuilder3;
            if (parent == this.root) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("/");
                stringBuilder3.append(name);
                stringBuilder2 = stringBuilder3.toString();
            } else {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(parent.absolutePath());
                stringBuilder3.append("/");
                stringBuilder3.append(name);
                stringBuilder2 = stringBuilder3.toString();
            }
            this.absolutePath = stringBuilder2;
        }
        this.name = name;
        this.parent = parent;
    }

    public void put(String key, String value) {
        StringBuilder stringBuilder;
        if (key == null || value == null) {
            throw new NullPointerException();
        } else if (key.length() > 80) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Key too long: ");
            stringBuilder.append(key);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (value.length() <= 8192) {
            synchronized (this.lock) {
                if (this.removed) {
                    throw new IllegalStateException("Node has been removed.");
                }
                putSpi(key, value);
                enqueuePreferenceChangeEvent(key, value);
            }
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Value too long: ");
            stringBuilder.append(value);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public String get(String key, String def) {
        if (key != null) {
            String str;
            synchronized (this.lock) {
                if (this.removed) {
                    throw new IllegalStateException("Node has been removed.");
                }
                String result = null;
                try {
                    result = getSpi(key);
                } catch (Exception e) {
                }
                str = result == null ? def : result;
            }
            return str;
        }
        throw new NullPointerException("Null key");
    }

    public void remove(String key) {
        Objects.requireNonNull((Object) key, "Specified key cannot be null");
        synchronized (this.lock) {
            if (this.removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            removeSpi(key);
            enqueuePreferenceChangeEvent(key, null);
        }
    }

    public void clear() throws BackingStoreException {
        synchronized (this.lock) {
            String[] keys = keys();
            for (String remove : keys) {
                remove(remove);
            }
        }
    }

    public void putInt(String key, int value) {
        put(key, Integer.toString(value));
    }

    public int getInt(String key, int def) {
        int result = def;
        try {
            String value = get(key, null);
            if (value != null) {
                return Integer.parseInt(value);
            }
            return result;
        } catch (NumberFormatException e) {
            return result;
        }
    }

    public void putLong(String key, long value) {
        put(key, Long.toString(value));
    }

    public long getLong(String key, long def) {
        long result = def;
        try {
            String value = get(key, null);
            if (value != null) {
                return Long.parseLong(value);
            }
            return result;
        } catch (NumberFormatException e) {
            return result;
        }
    }

    public void putBoolean(String key, boolean value) {
        put(key, String.valueOf(value));
    }

    public boolean getBoolean(String key, boolean def) {
        boolean result = def;
        String value = get(key, null);
        if (value == null) {
            return result;
        }
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        return result;
    }

    public void putFloat(String key, float value) {
        put(key, Float.toString(value));
    }

    public float getFloat(String key, float def) {
        float result = def;
        try {
            String value = get(key, null);
            if (value != null) {
                return Float.parseFloat(value);
            }
            return result;
        } catch (NumberFormatException e) {
            return result;
        }
    }

    public void putDouble(String key, double value) {
        put(key, Double.toString(value));
    }

    public double getDouble(String key, double def) {
        double result = def;
        try {
            String value = get(key, null);
            if (value != null) {
                return Double.parseDouble(value);
            }
            return result;
        } catch (NumberFormatException e) {
            return result;
        }
    }

    public void putByteArray(String key, byte[] value) {
        put(key, Base64.byteArrayToBase64(value));
    }

    public byte[] getByteArray(String key, byte[] def) {
        byte[] result = def;
        String value = get(key, null);
        if (value == null) {
            return result;
        }
        try {
            return Base64.base64ToByteArray(value);
        } catch (RuntimeException e) {
            return result;
        }
    }

    public String[] keys() throws BackingStoreException {
        String[] keysSpi;
        synchronized (this.lock) {
            if (this.removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            keysSpi = keysSpi();
        }
        return keysSpi;
    }

    public String[] childrenNames() throws BackingStoreException {
        Set<String> s;
        synchronized (this.lock) {
            if (this.removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            s = new TreeSet(this.kidCache.keySet());
            for (String kid : childrenNamesSpi()) {
                s.add(kid);
            }
        }
        return (String[]) s.toArray(EMPTY_STRING_ARRAY);
    }

    protected final AbstractPreferences[] cachedChildren() {
        return (AbstractPreferences[]) this.kidCache.values().toArray(EMPTY_ABSTRACT_PREFS_ARRAY);
    }

    public Preferences parent() {
        AbstractPreferences abstractPreferences;
        synchronized (this.lock) {
            if (this.removed) {
                throw new IllegalStateException("Node has been removed.");
            }
            abstractPreferences = this.parent;
        }
        return abstractPreferences;
    }

    public Preferences node(String path) {
        synchronized (this.lock) {
            if (this.removed) {
                throw new IllegalStateException("Node has been removed.");
            } else if (path.equals("")) {
                return this;
            } else if (path.equals("/")) {
                AbstractPreferences abstractPreferences = this.root;
                return abstractPreferences;
            } else if (path.charAt(0) != '/') {
                Preferences node = node(new StringTokenizer(path, "/", true));
                return node;
            } else {
                return this.root.node(new StringTokenizer(path.substring(1), "/", true));
            }
        }
    }

    private Preferences node(StringTokenizer path) {
        String token = path.nextToken();
        if (token.equals("/")) {
            throw new IllegalArgumentException("Consecutive slashes in path");
        }
        synchronized (this.lock) {
            AbstractPreferences child = (AbstractPreferences) this.kidCache.get(token);
            if (child == null) {
                if (token.length() <= 80) {
                    child = childSpi(token);
                    if (child.newNode) {
                        enqueueNodeAddedEvent(child);
                    }
                    this.kidCache.put(token, child);
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Node name ");
                    stringBuilder.append(token);
                    stringBuilder.append(" too long");
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            if (path.hasMoreTokens()) {
                path.nextToken();
                if (path.hasMoreTokens()) {
                    Preferences node = child.node(path);
                    return node;
                }
                throw new IllegalArgumentException("Path ends with slash");
            }
            return child;
        }
    }

    public boolean nodeExists(String path) throws BackingStoreException {
        synchronized (this.lock) {
            if (path.equals("")) {
                int i = this.removed ^ 1;
                return i;
            } else if (this.removed) {
                throw new IllegalStateException("Node has been removed.");
            } else if (path.equals("/")) {
                return true;
            } else if (path.charAt(0) != '/') {
                boolean nodeExists = nodeExists(new StringTokenizer(path, "/", true));
                return nodeExists;
            } else {
                return this.root.nodeExists(new StringTokenizer(path.substring(1), "/", true));
            }
        }
    }

    private boolean nodeExists(StringTokenizer path) throws BackingStoreException {
        String token = path.nextToken();
        if (token.equals("/")) {
            throw new IllegalArgumentException("Consecutive slashes in path");
        }
        synchronized (this.lock) {
            AbstractPreferences child = (AbstractPreferences) this.kidCache.get(token);
            if (child == null) {
                child = getChild(token);
            }
            if (child == null) {
                return false;
            } else if (path.hasMoreTokens()) {
                path.nextToken();
                if (path.hasMoreTokens()) {
                    boolean nodeExists = child.nodeExists(path);
                    return nodeExists;
                }
                throw new IllegalArgumentException("Path ends with slash");
            } else {
                return true;
            }
        }
    }

    public void removeNode() throws BackingStoreException {
        if (this != this.root) {
            synchronized (this.parent.lock) {
                removeNode2();
                this.parent.kidCache.remove(this.name);
            }
            return;
        }
        throw new UnsupportedOperationException("Can't remove the root!");
    }

    private void removeNode2() throws BackingStoreException {
        synchronized (this.lock) {
            if (this.removed) {
                throw new IllegalStateException("Node already removed.");
            }
            String[] kidNames = childrenNamesSpi();
            for (int i = 0; i < kidNames.length; i++) {
                if (!this.kidCache.containsKey(kidNames[i])) {
                    this.kidCache.put(kidNames[i], childSpi(kidNames[i]));
                }
            }
            Iterator<AbstractPreferences> i2 = this.kidCache.values().iterator();
            while (i2.hasNext()) {
                try {
                    ((AbstractPreferences) i2.next()).removeNode2();
                    i2.remove();
                } catch (BackingStoreException e) {
                }
            }
            removeNodeSpi();
            this.removed = true;
            this.parent.enqueueNodeRemovedEvent(this);
        }
    }

    public String name() {
        return this.name;
    }

    public String absolutePath() {
        return this.absolutePath;
    }

    public boolean isUserNode() {
        return ((Boolean) AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            public Boolean run() {
                return Boolean.valueOf(AbstractPreferences.this.root == Preferences.userRoot());
            }
        })).booleanValue();
    }

    public void addPreferenceChangeListener(PreferenceChangeListener pcl) {
        if (pcl != null) {
            synchronized (this.lock) {
                if (this.removed) {
                    throw new IllegalStateException("Node has been removed.");
                }
                this.prefListeners.add(pcl);
            }
            startEventDispatchThreadIfNecessary();
            return;
        }
        throw new NullPointerException("Change listener is null.");
    }

    public void removePreferenceChangeListener(PreferenceChangeListener pcl) {
        synchronized (this.lock) {
            if (this.removed) {
                throw new IllegalStateException("Node has been removed.");
            } else if (this.prefListeners.contains(pcl)) {
                this.prefListeners.remove((Object) pcl);
            } else {
                throw new IllegalArgumentException("Listener not registered.");
            }
        }
    }

    public void addNodeChangeListener(NodeChangeListener ncl) {
        if (ncl != null) {
            synchronized (this.lock) {
                if (this.removed) {
                    throw new IllegalStateException("Node has been removed.");
                }
                this.nodeListeners.add(ncl);
            }
            startEventDispatchThreadIfNecessary();
            return;
        }
        throw new NullPointerException("Change listener is null.");
    }

    public void removeNodeChangeListener(NodeChangeListener ncl) {
        synchronized (this.lock) {
            if (this.removed) {
                throw new IllegalStateException("Node has been removed.");
            } else if (this.nodeListeners.contains(ncl)) {
                this.nodeListeners.remove((Object) ncl);
            } else {
                throw new IllegalArgumentException("Listener not registered.");
            }
        }
    }

    protected AbstractPreferences getChild(String nodeName) throws BackingStoreException {
        synchronized (this.lock) {
            String[] kidNames = childrenNames();
            for (int i = 0; i < kidNames.length; i++) {
                if (kidNames[i].equals(nodeName)) {
                    AbstractPreferences childSpi = childSpi(kidNames[i]);
                    return childSpi;
                }
            }
            return null;
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(isUserNode() ? "User" : "System");
        stringBuilder.append(" Preference Node: ");
        stringBuilder.append(absolutePath());
        return stringBuilder.toString();
    }

    public void sync() throws BackingStoreException {
        sync2();
    }

    private void sync2() throws BackingStoreException {
        AbstractPreferences[] cachedKids;
        synchronized (this.lock) {
            if (this.removed) {
                throw new IllegalStateException("Node has been removed");
            }
            syncSpi();
            cachedKids = cachedChildren();
        }
        for (AbstractPreferences sync2 : cachedKids) {
            sync2.sync2();
        }
    }

    public void flush() throws BackingStoreException {
        flush2();
    }

    /* JADX WARNING: Missing block: B:9:0x0011, code skipped:
            r0 = 0;
     */
    /* JADX WARNING: Missing block: B:11:0x0013, code skipped:
            if (r0 >= r1.length) goto L_0x001d;
     */
    /* JADX WARNING: Missing block: B:12:0x0015, code skipped:
            r1[r0].flush2();
            r0 = r0 + 1;
     */
    /* JADX WARNING: Missing block: B:13:0x001d, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void flush2() throws BackingStoreException {
        synchronized (this.lock) {
            flushSpi();
            if (this.removed) {
                return;
            }
            AbstractPreferences[] cachedKids = cachedChildren();
        }
    }

    protected boolean isRemoved() {
        boolean z;
        synchronized (this.lock) {
            z = this.removed;
        }
        return z;
    }

    private static synchronized void startEventDispatchThreadIfNecessary() {
        synchronized (AbstractPreferences.class) {
            if (eventDispatchThread == null) {
                eventDispatchThread = new EventDispatchThread();
                eventDispatchThread.setDaemon(true);
                eventDispatchThread.start();
            }
        }
    }

    PreferenceChangeListener[] prefListeners() {
        PreferenceChangeListener[] preferenceChangeListenerArr;
        synchronized (this.lock) {
            preferenceChangeListenerArr = (PreferenceChangeListener[]) this.prefListeners.toArray(new PreferenceChangeListener[this.prefListeners.size()]);
        }
        return preferenceChangeListenerArr;
    }

    NodeChangeListener[] nodeListeners() {
        NodeChangeListener[] nodeChangeListenerArr;
        synchronized (this.lock) {
            nodeChangeListenerArr = (NodeChangeListener[]) this.nodeListeners.toArray(new NodeChangeListener[this.nodeListeners.size()]);
        }
        return nodeChangeListenerArr;
    }

    private void enqueuePreferenceChangeEvent(String key, String newValue) {
        if (!this.prefListeners.isEmpty()) {
            synchronized (eventQueue) {
                eventQueue.add(new PreferenceChangeEvent(this, key, newValue));
                eventQueue.notify();
            }
        }
    }

    private void enqueueNodeAddedEvent(Preferences child) {
        if (!this.nodeListeners.isEmpty()) {
            synchronized (eventQueue) {
                eventQueue.add(new NodeAddedEvent(this, child));
                eventQueue.notify();
            }
        }
    }

    private void enqueueNodeRemovedEvent(Preferences child) {
        if (!this.nodeListeners.isEmpty()) {
            synchronized (eventQueue) {
                eventQueue.add(new NodeRemovedEvent(this, child));
                eventQueue.notify();
            }
        }
    }

    public void exportNode(OutputStream os) throws IOException, BackingStoreException {
        XmlSupport.export(os, this, false);
    }

    public void exportSubtree(OutputStream os) throws IOException, BackingStoreException {
        XmlSupport.export(os, this, true);
    }
}
