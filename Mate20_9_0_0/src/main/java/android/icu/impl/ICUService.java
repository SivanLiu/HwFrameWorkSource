package android.icu.impl;

import android.icu.impl.ICURWLock.Stats;
import android.icu.util.ULocale;
import android.icu.util.ULocale.Category;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class ICUService extends ICUNotifier {
    private static final boolean DEBUG = ICUDebug.enabled("service");
    private Map<String, CacheEntry> cache;
    private int defaultSize;
    private LocaleRef dnref;
    private final List<Factory> factories;
    private final ICURWLock factoryLock;
    private Map<String, Factory> idcache;
    protected final String name;

    private static final class CacheEntry {
        final String actualDescriptor;
        final Object service;

        CacheEntry(String actualDescriptor, Object service) {
            this.actualDescriptor = actualDescriptor;
            this.service = service;
        }
    }

    public interface Factory {
        Object create(Key key, ICUService iCUService);

        String getDisplayName(String str, ULocale uLocale);

        void updateVisibleIDs(Map<String, Factory> map);
    }

    public static class Key {
        private final String id;

        public Key(String id) {
            this.id = id;
        }

        public final String id() {
            return this.id;
        }

        public String canonicalID() {
            return this.id;
        }

        public String currentID() {
            return canonicalID();
        }

        public String currentDescriptor() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("/");
            stringBuilder.append(currentID());
            return stringBuilder.toString();
        }

        public boolean fallback() {
            return false;
        }

        public boolean isFallbackOf(String idToCheck) {
            return canonicalID().equals(idToCheck);
        }
    }

    private static class LocaleRef {
        private Comparator<Object> com;
        private SortedMap<String, String> dnCache;
        private final ULocale locale;

        LocaleRef(SortedMap<String, String> dnCache, ULocale locale, Comparator<Object> com) {
            this.locale = locale;
            this.com = com;
            this.dnCache = dnCache;
        }

        SortedMap<String, String> get(ULocale loc, Comparator<Object> comp) {
            SortedMap<String, String> m = this.dnCache;
            if (m == null || !this.locale.equals(loc) || (this.com != comp && (this.com == null || !this.com.equals(comp)))) {
                return null;
            }
            return m;
        }
    }

    public interface ServiceListener extends EventListener {
        void serviceChanged(ICUService iCUService);
    }

    public static class SimpleFactory implements Factory {
        protected String id;
        protected Object instance;
        protected boolean visible;

        public SimpleFactory(Object instance, String id) {
            this(instance, id, true);
        }

        public SimpleFactory(Object instance, String id, boolean visible) {
            if (instance == null || id == null) {
                throw new IllegalArgumentException("Instance or id is null");
            }
            this.instance = instance;
            this.id = id;
            this.visible = visible;
        }

        public Object create(Key key, ICUService service) {
            if (this.id.equals(key.currentID())) {
                return this.instance;
            }
            return null;
        }

        public void updateVisibleIDs(Map<String, Factory> result) {
            if (this.visible) {
                result.put(this.id, this);
            } else {
                result.remove(this.id);
            }
        }

        public String getDisplayName(String identifier, ULocale locale) {
            return (this.visible && this.id.equals(identifier)) ? identifier : null;
        }

        public String toString() {
            StringBuilder buf = new StringBuilder(super.toString());
            buf.append(", id: ");
            buf.append(this.id);
            buf.append(", visible: ");
            buf.append(this.visible);
            return buf.toString();
        }
    }

    public ICUService() {
        this.factoryLock = new ICURWLock();
        this.factories = new ArrayList();
        this.defaultSize = 0;
        this.name = "";
    }

    public ICUService(String name) {
        this.factoryLock = new ICURWLock();
        this.factories = new ArrayList();
        this.defaultSize = 0;
        this.name = name;
    }

    public Object get(String descriptor) {
        return getKey(createKey(descriptor), null);
    }

    public Object get(String descriptor, String[] actualReturn) {
        if (descriptor != null) {
            return getKey(createKey(descriptor), actualReturn);
        }
        throw new NullPointerException("descriptor must not be null");
    }

    public Object getKey(Key key) {
        return getKey(key, null);
    }

    public Object getKey(Key key, String[] actualReturn) {
        return getKey(key, actualReturn, null);
    }

    /* JADX WARNING: Missing block: B:53:0x0167, code skipped:
            r4 = new android.icu.impl.ICUService.CacheEntry(r5, r3);
     */
    /* JADX WARNING: Missing block: B:56:0x016e, code skipped:
            if (DEBUG == false) goto L_0x019a;
     */
    /* JADX WARNING: Missing block: B:57:0x0170, code skipped:
            r13 = java.lang.System.out;
            r16 = r3;
            r3 = new java.lang.StringBuilder();
     */
    /* JADX WARNING: Missing block: B:58:0x0179, code skipped:
            r17 = r4;
     */
    /* JADX WARNING: Missing block: B:60:?, code skipped:
            r3.append(r1.name);
            r3.append(" factory supported: ");
            r3.append(r5);
            r3.append(", caching");
            r13.println(r3.toString());
     */
    /* JADX WARNING: Missing block: B:61:0x0195, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:62:0x0196, code skipped:
            r4 = r17;
     */
    /* JADX WARNING: Missing block: B:63:0x019a, code skipped:
            r17 = r4;
     */
    /* JADX WARNING: Missing block: B:64:0x019c, code skipped:
            r4 = r17;
     */
    /* JADX WARNING: Missing block: B:65:0x019f, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:66:0x01a0, code skipped:
            r17 = r4;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Object getKey(Key key, String[] actualReturn, Factory factory) {
        CacheEntry result;
        Throwable th;
        Key key2 = key;
        Factory factory2 = factory;
        if (this.factories.size() == 0) {
            return handleDefault(key, actualReturn);
        }
        PrintStream printStream;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        if (DEBUG) {
            printStream = System.out;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Service: ");
            stringBuilder.append(this.name);
            stringBuilder.append(" key: ");
            stringBuilder.append(key.canonicalID());
            printStream.println(stringBuilder.toString());
        }
        if (key2 != null) {
            try {
                StringBuilder stringBuilder3;
                PrintStream printStream2;
                StringBuilder stringBuilder4;
                CacheEntry result2;
                Object service;
                PrintStream printStream3;
                this.factoryLock.acquireRead();
                Map<String, CacheEntry> cache = this.cache;
                if (cache == null) {
                    if (DEBUG) {
                        PrintStream printStream4 = System.out;
                        StringBuilder stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("Service ");
                        stringBuilder5.append(this.name);
                        stringBuilder5.append(" cache was empty");
                        printStream4.println(stringBuilder5.toString());
                    }
                    cache = new ConcurrentHashMap();
                }
                ArrayList<String> cacheDescriptorList = null;
                boolean putInCache = false;
                int NDebug = 0;
                int startIndex = 0;
                int limit = this.factories.size();
                boolean cacheResult = true;
                if (factory2 != null) {
                    for (int i = 0; i < limit; i++) {
                        if (factory2 == this.factories.get(i)) {
                            startIndex = i + 1;
                            break;
                        }
                    }
                    if (startIndex != 0) {
                        cacheResult = false;
                    } else {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Factory ");
                        stringBuilder3.append(factory2);
                        stringBuilder3.append("not registered with service: ");
                        stringBuilder3.append(this);
                        throw new IllegalStateException(stringBuilder3.toString());
                    }
                }
                loop1:
                while (true) {
                    int NDebug2;
                    String currentDescriptor = key.currentDescriptor();
                    if (DEBUG) {
                        printStream2 = System.out;
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append(this.name);
                        stringBuilder4.append("[");
                        NDebug2 = NDebug + 1;
                        stringBuilder4.append(NDebug);
                        stringBuilder4.append("] looking for: ");
                        stringBuilder4.append(currentDescriptor);
                        printStream2.println(stringBuilder4.toString());
                        NDebug = NDebug2;
                    }
                    result2 = (CacheEntry) cache.get(currentDescriptor);
                    PrintStream printStream5;
                    if (result2 == null) {
                        try {
                            if (DEBUG) {
                                printStream5 = System.out;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("did not find: ");
                                stringBuilder3.append(currentDescriptor);
                                stringBuilder3.append(" in cache");
                                printStream5.println(stringBuilder3.toString());
                            }
                            putInCache = cacheResult;
                            NDebug2 = startIndex;
                            while (NDebug2 < limit) {
                                int index = NDebug2 + 1;
                                Factory f = (Factory) this.factories.get(NDebug2);
                                if (DEBUG) {
                                    printStream2 = System.out;
                                    stringBuilder2 = new StringBuilder();
                                    result = result2;
                                    try {
                                        stringBuilder2.append("trying factory[");
                                        stringBuilder2.append(index - 1);
                                        stringBuilder2.append("] ");
                                        stringBuilder2.append(f.toString());
                                        printStream2.println(stringBuilder2.toString());
                                    } catch (Throwable th2) {
                                        th = th2;
                                        result2 = result;
                                        this.factoryLock.releaseRead();
                                        throw th;
                                    }
                                }
                                result = result2;
                                service = f.create(key2, this);
                                if (service != null) {
                                    break loop1;
                                }
                                Object obj = service;
                                if (DEBUG) {
                                    printStream3 = System.out;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("factory did not support: ");
                                    stringBuilder.append(currentDescriptor);
                                    printStream3.println(stringBuilder.toString());
                                }
                                NDebug2 = index;
                                result2 = result;
                                factory2 = factory;
                            }
                            result = result2;
                            if (cacheDescriptorList == null) {
                                cacheDescriptorList = new ArrayList(5);
                            }
                            cacheDescriptorList.add(currentDescriptor);
                            if (!key.fallback()) {
                                result2 = result;
                                break;
                            }
                            factory2 = factory;
                        } catch (Throwable th3) {
                            th = th3;
                            result = result2;
                            this.factoryLock.releaseRead();
                            throw th;
                        }
                    } else if (DEBUG) {
                        printStream5 = System.out;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(this.name);
                        stringBuilder3.append(" found with descriptor: ");
                        stringBuilder3.append(currentDescriptor);
                        printStream5.println(stringBuilder3.toString());
                    }
                }
                if (result2 != null) {
                    StringBuilder stringBuilder6;
                    if (putInCache) {
                        if (DEBUG) {
                            printStream3 = System.out;
                            stringBuilder6 = new StringBuilder();
                            stringBuilder6.append("caching '");
                            stringBuilder6.append(result2.actualDescriptor);
                            stringBuilder6.append("'");
                            printStream3.println(stringBuilder6.toString());
                        }
                        cache.put(result2.actualDescriptor, result2);
                        if (cacheDescriptorList != null) {
                            Iterator it = cacheDescriptorList.iterator();
                            while (it.hasNext()) {
                                Iterator it2;
                                String desc = (String) it.next();
                                if (DEBUG) {
                                    printStream2 = System.out;
                                    stringBuilder4 = new StringBuilder();
                                    it2 = it;
                                    stringBuilder4.append(this.name);
                                    stringBuilder4.append(" adding descriptor: '");
                                    stringBuilder4.append(desc);
                                    stringBuilder4.append("' for actual: '");
                                    stringBuilder4.append(result2.actualDescriptor);
                                    stringBuilder4.append("'");
                                    printStream2.println(stringBuilder4.toString());
                                } else {
                                    it2 = it;
                                }
                                cache.put(desc, result2);
                                it = it2;
                            }
                        }
                        this.cache = cache;
                    }
                    if (actualReturn != null) {
                        if (result2.actualDescriptor.indexOf("/") == 0) {
                            actualReturn[0] = result2.actualDescriptor.substring(1);
                        } else {
                            actualReturn[0] = result2.actualDescriptor;
                        }
                    }
                    if (DEBUG) {
                        printStream3 = System.out;
                        stringBuilder6 = new StringBuilder();
                        stringBuilder6.append("found in service: ");
                        stringBuilder6.append(this.name);
                        printStream3.println(stringBuilder6.toString());
                    }
                    service = result2.service;
                    this.factoryLock.releaseRead();
                    return service;
                }
                this.factoryLock.releaseRead();
            } catch (Throwable th4) {
                th = th4;
                this.factoryLock.releaseRead();
                throw th;
            }
        }
        if (DEBUG) {
            printStream = System.out;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("not found in service: ");
            stringBuilder2.append(this.name);
            printStream.println(stringBuilder2.toString());
        }
        return handleDefault(key, actualReturn);
    }

    protected Object handleDefault(Key key, String[] actualIDReturn) {
        return null;
    }

    public Set<String> getVisibleIDs() {
        return getVisibleIDs(null);
    }

    public Set<String> getVisibleIDs(String matchID) {
        Set<String> result = getVisibleIDMap().keySet();
        Key fallbackKey = createKey(matchID);
        if (fallbackKey == null) {
            return result;
        }
        Set<String> temp = new HashSet(result.size());
        for (String id : result) {
            if (fallbackKey.isFallbackOf(id)) {
                temp.add(id);
            }
        }
        return temp;
    }

    private Map<String, Factory> getVisibleIDMap() {
        synchronized (this) {
            if (this.idcache == null) {
                try {
                    this.factoryLock.acquireRead();
                    Map<String, Factory> mutableMap = new HashMap();
                    ListIterator<Factory> lIter = this.factories.listIterator(this.factories.size());
                    while (lIter.hasPrevious()) {
                        ((Factory) lIter.previous()).updateVisibleIDs(mutableMap);
                    }
                    this.idcache = Collections.unmodifiableMap(mutableMap);
                } finally {
                    this.factoryLock.releaseRead();
                }
            }
        }
        return this.idcache;
    }

    public String getDisplayName(String id) {
        return getDisplayName(id, ULocale.getDefault(Category.DISPLAY));
    }

    public String getDisplayName(String id, ULocale locale) {
        Map<String, Factory> m = getVisibleIDMap();
        Factory f = (Factory) m.get(id);
        if (f != null) {
            return f.getDisplayName(id, locale);
        }
        Key key = createKey(id);
        while (key.fallback()) {
            f = (Factory) m.get(key.currentID());
            if (f != null) {
                return f.getDisplayName(id, locale);
            }
        }
        return null;
    }

    public SortedMap<String, String> getDisplayNames() {
        return getDisplayNames(ULocale.getDefault(Category.DISPLAY), null, null);
    }

    public SortedMap<String, String> getDisplayNames(ULocale locale) {
        return getDisplayNames(locale, null, null);
    }

    public SortedMap<String, String> getDisplayNames(ULocale locale, Comparator<Object> com) {
        return getDisplayNames(locale, com, null);
    }

    public SortedMap<String, String> getDisplayNames(ULocale locale, String matchID) {
        return getDisplayNames(locale, null, matchID);
    }

    public SortedMap<String, String> getDisplayNames(ULocale locale, Comparator<Object> com, String matchID) {
        SortedMap<String, String> dncache = null;
        LocaleRef ref = this.dnref;
        if (ref != null) {
            dncache = ref.get(locale, com);
        }
        while (dncache == null) {
            synchronized (this) {
                if (ref != this.dnref) {
                    if (this.dnref != null) {
                        ref = this.dnref;
                        dncache = ref.get(locale, com);
                    }
                }
                TreeMap dncache2 = new TreeMap(com);
                for (Entry<String, Factory> e : getVisibleIDMap().entrySet()) {
                    String id = (String) e.getKey();
                    dncache2.put(((Factory) e.getValue()).getDisplayName(id, locale), id);
                }
                dncache = Collections.unmodifiableSortedMap(dncache2);
                this.dnref = new LocaleRef(dncache, locale, com);
            }
        }
        Key matchKey = createKey(matchID);
        if (matchKey == null) {
            return dncache;
        }
        SortedMap<String, String> result = new TreeMap(dncache);
        Iterator<Entry<String, String>> iter = result.entrySet().iterator();
        while (iter.hasNext()) {
            if (!matchKey.isFallbackOf((String) ((Entry) iter.next()).getValue())) {
                iter.remove();
            }
        }
        return result;
    }

    public final List<Factory> factories() {
        try {
            this.factoryLock.acquireRead();
            List<Factory> arrayList = new ArrayList(this.factories);
            return arrayList;
        } finally {
            this.factoryLock.releaseRead();
        }
    }

    public Factory registerObject(Object obj, String id) {
        return registerObject(obj, id, true);
    }

    public Factory registerObject(Object obj, String id, boolean visible) {
        return registerFactory(new SimpleFactory(obj, createKey(id).canonicalID(), visible));
    }

    public final Factory registerFactory(Factory factory) {
        if (factory != null) {
            try {
                this.factoryLock.acquireWrite();
                this.factories.add(0, factory);
                clearCaches();
                notifyChanged();
                return factory;
            } finally {
                this.factoryLock.releaseWrite();
            }
        } else {
            throw new NullPointerException();
        }
    }

    public final boolean unregisterFactory(Factory factory) {
        if (factory != null) {
            boolean result = false;
            try {
                this.factoryLock.acquireWrite();
                if (this.factories.remove(factory)) {
                    result = true;
                    clearCaches();
                }
                this.factoryLock.releaseWrite();
                if (result) {
                    notifyChanged();
                }
                return result;
            } catch (Throwable th) {
                this.factoryLock.releaseWrite();
            }
        } else {
            throw new NullPointerException();
        }
    }

    public final void reset() {
        try {
            this.factoryLock.acquireWrite();
            reInitializeFactories();
            clearCaches();
            notifyChanged();
        } finally {
            this.factoryLock.releaseWrite();
        }
    }

    protected void reInitializeFactories() {
        this.factories.clear();
    }

    public boolean isDefault() {
        return this.factories.size() == this.defaultSize;
    }

    protected void markDefault() {
        this.defaultSize = this.factories.size();
    }

    public Key createKey(String id) {
        return id == null ? null : new Key(id);
    }

    protected void clearCaches() {
        this.cache = null;
        this.idcache = null;
        this.dnref = null;
    }

    protected void clearServiceCache() {
        this.cache = null;
    }

    protected boolean acceptsListener(EventListener l) {
        return l instanceof ServiceListener;
    }

    protected void notifyListener(EventListener l) {
        ((ServiceListener) l).serviceChanged(this);
    }

    public String stats() {
        Stats stats = this.factoryLock.resetStats();
        if (stats != null) {
            return stats.toString();
        }
        return "no stats";
    }

    public String getName() {
        return this.name;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(super.toString());
        stringBuilder.append("{");
        stringBuilder.append(this.name);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
