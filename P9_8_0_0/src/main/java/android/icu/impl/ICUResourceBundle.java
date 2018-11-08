package android.icu.impl;

import android.icu.impl.URLHandler.URLVisitor;
import android.icu.impl.UResource.Key;
import android.icu.impl.UResource.Sink;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import android.icu.util.UResourceBundleIterator;
import android.icu.util.UResourceTypeMismatchException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

public class ICUResourceBundle extends UResourceBundle {
    static final /* synthetic */ boolean -assertionsDisabled = (ICUResourceBundle.class.desiredAssertionStatus() ^ 1);
    public static final int ALIAS = 3;
    public static final int ARRAY16 = 9;
    private static CacheBase<String, ICUResourceBundle, Loader> BUNDLE_CACHE = new SoftCache<String, ICUResourceBundle, Loader>() {
        protected ICUResourceBundle createInstance(String unusedKey, Loader loader) {
            return loader.load();
        }
    };
    private static final boolean DEBUG = ICUDebug.enabled("localedata");
    private static final String DEFAULT_TAG = "default";
    private static final String FULL_LOCALE_NAMES_LIST = "fullLocaleNames.lst";
    private static CacheBase<String, AvailEntry, ClassLoader> GET_AVAILABLE_CACHE = new SoftCache<String, AvailEntry, ClassLoader>() {
        protected AvailEntry createInstance(String key, ClassLoader loader) {
            return new AvailEntry(key, loader);
        }
    };
    private static final char HYPHEN = '-';
    private static final String ICUDATA = "ICUDATA";
    public static final ClassLoader ICU_DATA_CLASS_LOADER = ClassLoaderUtil.getClassLoader(ICUData.class);
    private static final String ICU_RESOURCE_INDEX = "res_index";
    protected static final String INSTALLED_LOCALES = "InstalledLocales";
    private static final String LOCALE = "LOCALE";
    public static final String NO_INHERITANCE_MARKER = "∅∅∅";
    public static final int RES_BOGUS = -1;
    private static final char RES_PATH_SEP_CHAR = '/';
    private static final String RES_PATH_SEP_STR = "/";
    public static final int STRING_V2 = 6;
    public static final int TABLE16 = 5;
    public static final int TABLE32 = 4;
    private ICUResourceBundle container;
    protected String key;
    WholeBundle wholeBundle;

    private static abstract class Loader {
        abstract ICUResourceBundle load();

        private Loader() {
        }
    }

    private static final class AvailEntry {
        private volatile Set<String> fullNameSet;
        private ClassLoader loader;
        private volatile Locale[] locales;
        private volatile Set<String> nameSet;
        private String prefix;
        private volatile ULocale[] ulocales;

        AvailEntry(String prefix, ClassLoader loader) {
            this.prefix = prefix;
            this.loader = loader;
        }

        ULocale[] getULocaleList() {
            if (this.ulocales == null) {
                synchronized (this) {
                    if (this.ulocales == null) {
                        this.ulocales = ICUResourceBundle.createULocaleList(this.prefix, this.loader);
                    }
                }
            }
            return this.ulocales;
        }

        Locale[] getLocaleList() {
            if (this.locales == null) {
                getULocaleList();
                synchronized (this) {
                    if (this.locales == null) {
                        this.locales = ICUResourceBundle.getLocaleList(this.ulocales);
                    }
                }
            }
            return this.locales;
        }

        Set<String> getLocaleNameSet() {
            if (this.nameSet == null) {
                synchronized (this) {
                    if (this.nameSet == null) {
                        this.nameSet = ICUResourceBundle.createLocaleNameSet(this.prefix, this.loader);
                    }
                }
            }
            return this.nameSet;
        }

        Set<String> getFullLocaleNameSet() {
            if (this.fullNameSet == null) {
                synchronized (this) {
                    if (this.fullNameSet == null) {
                        this.fullNameSet = ICUResourceBundle.createFullLocaleNameSet(this.prefix, this.loader);
                    }
                }
            }
            return this.fullNameSet;
        }
    }

    public enum OpenType {
        LOCALE_DEFAULT_ROOT,
        LOCALE_ROOT,
        LOCALE_ONLY,
        DIRECT
    }

    protected static final class WholeBundle {
        String baseName;
        ClassLoader loader;
        String localeID;
        ICUResourceBundleReader reader;
        Set<String> topLevelKeys;
        ULocale ulocale;

        WholeBundle(String baseName, String localeID, ClassLoader loader, ICUResourceBundleReader reader) {
            this.baseName = baseName;
            this.localeID = localeID;
            this.ulocale = new ULocale(localeID);
            this.loader = loader;
            this.reader = reader;
        }
    }

    public static final ULocale getFunctionalEquivalent(String baseName, ClassLoader loader, String resName, String keyword, ULocale locID, boolean[] isAvailable, boolean omitDefault) {
        ICUResourceBundle r;
        ULocale[] availableULocales;
        ICUResourceBundle irb;
        ICUResourceBundle urb;
        String kwVal = locID.getKeywordValue(keyword);
        String baseLoc = locID.getBaseName();
        String defStr = null;
        ULocale parent = new ULocale(baseLoc);
        ULocale defLoc = null;
        boolean lookForDefault = false;
        ULocale fullBase = null;
        int defDepth = 0;
        int resDepth = 0;
        if (!(kwVal == null || kwVal.length() == 0)) {
            if (kwVal.equals(DEFAULT_TAG)) {
            }
            r = (ICUResourceBundle) UResourceBundle.getBundleInstance(baseName, parent);
            if (isAvailable != null) {
                isAvailable[0] = false;
                availableULocales = getAvailEntry(baseName, loader).getULocaleList();
                for (ULocale equals : availableULocales) {
                    if (parent.equals(equals)) {
                        isAvailable[0] = true;
                        break;
                    }
                }
            }
            do {
                try {
                    defStr = ((ICUResourceBundle) r.get(resName)).getString(DEFAULT_TAG);
                    if (lookForDefault) {
                        kwVal = defStr;
                        lookForDefault = false;
                    }
                    defLoc = r.getULocale();
                } catch (MissingResourceException e) {
                }
                if (defLoc == null) {
                    r = r.getParent();
                    defDepth++;
                }
                if (r == null) {
                    break;
                }
            } while (defLoc == null);
            r = (ICUResourceBundle) UResourceBundle.getBundleInstance(baseName, new ULocale(baseLoc));
            do {
                try {
                    irb = (ICUResourceBundle) r.get(resName);
                    irb.get(kwVal);
                    fullBase = irb.getULocale();
                    if (fullBase != null && resDepth > defDepth) {
                        defStr = irb.getString(DEFAULT_TAG);
                        defLoc = r.getULocale();
                        defDepth = resDepth;
                    }
                } catch (MissingResourceException e2) {
                }
                if (fullBase == null) {
                    r = r.getParent();
                    resDepth++;
                }
                if (r == null) {
                    break;
                }
            } while (fullBase == null);
            if (fullBase == null && defStr != null && (defStr.equals(kwVal) ^ 1) != 0) {
                kwVal = defStr;
                r = (ICUResourceBundle) UResourceBundle.getBundleInstance(baseName, new ULocale(baseLoc));
                resDepth = 0;
                do {
                    try {
                        irb = (ICUResourceBundle) r.get(resName);
                        urb = (ICUResourceBundle) irb.get(kwVal);
                        fullBase = r.getULocale();
                        if (!fullBase.getBaseName().equals(urb.getULocale().getBaseName())) {
                            fullBase = null;
                        }
                        if (fullBase != null && resDepth > defDepth) {
                            defStr = irb.getString(DEFAULT_TAG);
                            defLoc = r.getULocale();
                            defDepth = resDepth;
                        }
                    } catch (MissingResourceException e3) {
                    }
                    if (fullBase == null) {
                        r = r.getParent();
                        resDepth++;
                    }
                    if (r == null) {
                        break;
                    }
                } while (fullBase == null);
            }
            if (fullBase != null) {
                throw new MissingResourceException("Could not find locale containing requested or default keyword.", baseName, keyword + "=" + kwVal);
            } else if (omitDefault || !r8.equals(kwVal) || resDepth > defDepth) {
                return new ULocale(fullBase.getBaseName() + "@" + keyword + "=" + kwVal);
            } else {
                return fullBase;
            }
        }
        kwVal = "";
        lookForDefault = true;
        r = (ICUResourceBundle) UResourceBundle.getBundleInstance(baseName, parent);
        if (isAvailable != null) {
            isAvailable[0] = false;
            availableULocales = getAvailEntry(baseName, loader).getULocaleList();
            while (i < availableULocales.length) {
                if (parent.equals(equals)) {
                    isAvailable[0] = true;
                    break;
                }
            }
        }
        do {
            defStr = ((ICUResourceBundle) r.get(resName)).getString(DEFAULT_TAG);
            if (lookForDefault) {
                kwVal = defStr;
                lookForDefault = false;
            }
            defLoc = r.getULocale();
            if (defLoc == null) {
                r = r.getParent();
                defDepth++;
            }
            if (r == null) {
                break;
            }
            break;
        } while (defLoc == null);
        r = (ICUResourceBundle) UResourceBundle.getBundleInstance(baseName, new ULocale(baseLoc));
        do {
            irb = (ICUResourceBundle) r.get(resName);
            irb.get(kwVal);
            fullBase = irb.getULocale();
            defStr = irb.getString(DEFAULT_TAG);
            defLoc = r.getULocale();
            defDepth = resDepth;
            if (fullBase == null) {
                r = r.getParent();
                resDepth++;
            }
            if (r == null) {
                break;
            }
            break;
        } while (fullBase == null);
        kwVal = defStr;
        r = (ICUResourceBundle) UResourceBundle.getBundleInstance(baseName, new ULocale(baseLoc));
        resDepth = 0;
        do {
            irb = (ICUResourceBundle) r.get(resName);
            urb = (ICUResourceBundle) irb.get(kwVal);
            fullBase = r.getULocale();
            if (fullBase.getBaseName().equals(urb.getULocale().getBaseName())) {
                fullBase = null;
            }
            defStr = irb.getString(DEFAULT_TAG);
            defLoc = r.getULocale();
            defDepth = resDepth;
            if (fullBase == null) {
                r = r.getParent();
                resDepth++;
            }
            if (r == null) {
                break;
            }
            break;
        } while (fullBase == null);
        if (fullBase != null) {
            if (omitDefault) {
            }
            return new ULocale(fullBase.getBaseName() + "@" + keyword + "=" + kwVal);
        }
        throw new MissingResourceException("Could not find locale containing requested or default keyword.", baseName, keyword + "=" + kwVal);
    }

    public static final String[] getKeywordValues(String baseName, String keyword) {
        Set<String> keywords = new HashSet();
        ULocale[] locales = getAvailEntry(baseName, ICU_DATA_CLASS_LOADER).getULocaleList();
        for (ULocale bundleInstance : locales) {
            try {
                Enumeration<String> e = ((ICUResourceBundle) UResourceBundle.getBundleInstance(baseName, bundleInstance).getObject(keyword)).getKeys();
                while (e.hasMoreElements()) {
                    String s = (String) e.nextElement();
                    if (!(DEFAULT_TAG.equals(s) || (s.startsWith("private-") ^ 1) == 0)) {
                        keywords.add(s);
                    }
                }
            } catch (Throwable th) {
            }
        }
        return (String[]) keywords.toArray(new String[0]);
    }

    public ICUResourceBundle getWithFallback(String path) throws MissingResourceException {
        ICUResourceBundle actualBundle = this;
        ICUResourceBundle result = findResourceWithFallback(path, this, null);
        if (result == null) {
            throw new MissingResourceException("Can't find resource for bundle " + getClass().getName() + ", key " + getType(), path, getKey());
        } else if (result.getType() != 0 || !result.getString().equals(NO_INHERITANCE_MARKER)) {
            return result;
        } else {
            throw new MissingResourceException("Encountered NO_INHERITANCE_MARKER", path, getKey());
        }
    }

    public ICUResourceBundle at(int index) {
        return (ICUResourceBundle) handleGet(index, null, (UResourceBundle) this);
    }

    public ICUResourceBundle at(String key) {
        if (this instanceof ResourceTable) {
            return (ICUResourceBundle) handleGet(key, null, (UResourceBundle) this);
        }
        return null;
    }

    public ICUResourceBundle findTopLevel(int index) {
        return (ICUResourceBundle) super.findTopLevel(index);
    }

    public ICUResourceBundle findTopLevel(String aKey) {
        return (ICUResourceBundle) super.findTopLevel(aKey);
    }

    public ICUResourceBundle findWithFallback(String path) {
        return findResourceWithFallback(path, this, null);
    }

    public String findStringWithFallback(String path) {
        return findStringWithFallback(path, this, null);
    }

    public String getStringWithFallback(String path) throws MissingResourceException {
        ICUResourceBundle actualBundle = this;
        String result = findStringWithFallback(path, this, null);
        if (result == null) {
            throw new MissingResourceException("Can't find resource for bundle " + getClass().getName() + ", key " + getType(), path, getKey());
        } else if (!result.equals(NO_INHERITANCE_MARKER)) {
            return result;
        } else {
            throw new MissingResourceException("Encountered NO_INHERITANCE_MARKER", path, getKey());
        }
    }

    public void getAllItemsWithFallback(String path, Sink sink) throws MissingResourceException {
        ICUResourceBundle rb;
        int numPathKeys = countPathKeys(path);
        if (numPathKeys == 0) {
            rb = this;
        } else {
            int depth = getResDepth();
            String[] pathKeys = new String[(depth + numPathKeys)];
            getResPathKeys(path, numPathKeys, pathKeys, depth);
            rb = findResourceWithFallback(pathKeys, depth, this, null);
            if (rb == null) {
                throw new MissingResourceException("Can't find resource for bundle " + getClass().getName() + ", key " + getType(), path, getKey());
            }
        }
        rb.getAllItemsWithFallback(new Key(), new ReaderValue(), sink);
    }

    private void getAllItemsWithFallback(Key key, ReaderValue readerValue, Sink sink) {
        boolean z;
        ICUResourceBundleImpl impl = (ICUResourceBundleImpl) this;
        readerValue.reader = impl.wholeBundle.reader;
        readerValue.res = impl.getResource();
        key.setString(this.key != null ? this.key : "");
        if (this.parent == null) {
            z = true;
        } else {
            z = false;
        }
        sink.put(key, readerValue, z);
        if (this.parent != null) {
            ICUResourceBundle rb;
            ICUResourceBundle parentBundle = this.parent;
            int depth = getResDepth();
            if (depth == 0) {
                rb = parentBundle;
            } else {
                String[] pathKeys = new String[depth];
                getResPathKeys(pathKeys, depth);
                rb = findResourceWithFallback(pathKeys, 0, parentBundle, null);
            }
            if (rb != null) {
                rb.getAllItemsWithFallback(key, readerValue, sink);
            }
        }
    }

    public static Set<String> getAvailableLocaleNameSet(String bundlePrefix, ClassLoader loader) {
        return getAvailEntry(bundlePrefix, loader).getLocaleNameSet();
    }

    public static Set<String> getFullLocaleNameSet() {
        return getFullLocaleNameSet(ICUData.ICU_BASE_NAME, ICU_DATA_CLASS_LOADER);
    }

    public static Set<String> getFullLocaleNameSet(String bundlePrefix, ClassLoader loader) {
        return getAvailEntry(bundlePrefix, loader).getFullLocaleNameSet();
    }

    public static Set<String> getAvailableLocaleNameSet() {
        return getAvailableLocaleNameSet(ICUData.ICU_BASE_NAME, ICU_DATA_CLASS_LOADER);
    }

    public static final ULocale[] getAvailableULocales(String baseName, ClassLoader loader) {
        return getAvailEntry(baseName, loader).getULocaleList();
    }

    public static final ULocale[] getAvailableULocales() {
        return getAvailableULocales(ICUData.ICU_BASE_NAME, ICU_DATA_CLASS_LOADER);
    }

    public static final Locale[] getAvailableLocales(String baseName, ClassLoader loader) {
        return getAvailEntry(baseName, loader).getLocaleList();
    }

    public static final Locale[] getAvailableLocales() {
        return getAvailEntry(ICUData.ICU_BASE_NAME, ICU_DATA_CLASS_LOADER).getLocaleList();
    }

    public static final Locale[] getLocaleList(ULocale[] ulocales) {
        ArrayList<Locale> list = new ArrayList(ulocales.length);
        HashSet<Locale> uniqueSet = new HashSet();
        for (ULocale toLocale : ulocales) {
            Locale loc = toLocale.toLocale();
            if (!uniqueSet.contains(loc)) {
                list.add(loc);
                uniqueSet.add(loc);
            }
        }
        return (Locale[]) list.toArray(new Locale[list.size()]);
    }

    public Locale getLocale() {
        return getULocale().toLocale();
    }

    private static final ULocale[] createULocaleList(String baseName, ClassLoader root) {
        ICUResourceBundle bundle = (ICUResourceBundle) ((ICUResourceBundle) UResourceBundle.instantiateBundle(baseName, ICU_RESOURCE_INDEX, root, true)).get(INSTALLED_LOCALES);
        int i = 0;
        ULocale[] locales = new ULocale[bundle.getSize()];
        UResourceBundleIterator iter = bundle.getIterator();
        iter.reset();
        while (iter.hasNext()) {
            String locstr = iter.next().getKey();
            int i2;
            if (locstr.equals("root")) {
                i2 = i + 1;
                locales[i] = ULocale.ROOT;
                i = i2;
            } else {
                i2 = i + 1;
                locales[i] = new ULocale(locstr);
                i = i2;
            }
        }
        return locales;
    }

    private static final void addLocaleIDsFromIndexBundle(String baseName, ClassLoader root, Set<String> locales) {
        try {
            UResourceBundleIterator iter = ((ICUResourceBundle) ((ICUResourceBundle) UResourceBundle.instantiateBundle(baseName, ICU_RESOURCE_INDEX, root, true)).get(INSTALLED_LOCALES)).getIterator();
            iter.reset();
            while (iter.hasNext()) {
                locales.add(iter.next().getKey());
            }
        } catch (MissingResourceException e) {
            if (DEBUG) {
                System.out.println("couldn't find " + baseName + RES_PATH_SEP_CHAR + ICU_RESOURCE_INDEX + ".res");
                Thread.dumpStack();
            }
        }
    }

    private static final void addBundleBaseNamesFromClassLoader(final String bn, final ClassLoader root, final Set<String> names) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    Enumeration<URL> urls = root.getResources(bn);
                    if (urls == null) {
                        return null;
                    }
                    final Set set = names;
                    URLVisitor v = new URLVisitor() {
                        public void visit(String s) {
                            if (s.endsWith(".res")) {
                                set.add(s.substring(0, s.length() - 4));
                            }
                        }
                    };
                    while (urls.hasMoreElements()) {
                        URL url = (URL) urls.nextElement();
                        URLHandler handler = URLHandler.get(url);
                        if (handler != null) {
                            handler.guide(v, false);
                        } else if (ICUResourceBundle.DEBUG) {
                            System.out.println("handler for " + url + " is null");
                        }
                    }
                    return null;
                } catch (IOException e) {
                    if (ICUResourceBundle.DEBUG) {
                        System.out.println("ouch: " + e.getMessage());
                    }
                }
            }
        });
    }

    private static void addLocaleIDsFromListFile(String bn, ClassLoader root, Set<String> locales) {
        BufferedReader br;
        try {
            InputStream s = root.getResourceAsStream(bn + FULL_LOCALE_NAMES_LIST);
            if (s != null) {
                br = new BufferedReader(new InputStreamReader(s, "ASCII"));
                while (true) {
                    String line = br.readLine();
                    if (line == null) {
                        br.close();
                        return;
                    } else if (!(line.length() == 0 || (line.startsWith("#") ^ 1) == 0)) {
                        locales.add(line);
                    }
                }
            }
        } catch (IOException e) {
        } catch (Throwable th) {
            br.close();
        }
    }

    private static Set<String> createFullLocaleNameSet(String baseName, ClassLoader loader) {
        String bn = baseName.endsWith(RES_PATH_SEP_STR) ? baseName : baseName + RES_PATH_SEP_STR;
        Set<String> set = new HashSet();
        if (!ICUConfig.get("android.icu.impl.ICUResourceBundle.skipRuntimeLocaleResourceScan", "false").equalsIgnoreCase("true")) {
            addBundleBaseNamesFromClassLoader(bn, loader, set);
            if (baseName.startsWith(ICUData.ICU_BASE_NAME)) {
                String str;
                if (baseName.length() == ICUData.ICU_BASE_NAME.length()) {
                    str = "";
                } else if (baseName.charAt(ICUData.ICU_BASE_NAME.length()) == RES_PATH_SEP_CHAR) {
                    str = baseName.substring(ICUData.ICU_BASE_NAME.length() + 1);
                } else {
                    str = null;
                }
                if (str != null) {
                    ICUBinary.addBaseNamesInFileFolder(str, ".res", set);
                }
            }
            set.remove(ICU_RESOURCE_INDEX);
            Iterator<String> iter = set.iterator();
            while (iter.hasNext()) {
                String name = (String) iter.next();
                if ((name.length() == 1 || name.length() > 3) && name.indexOf(95) < 0) {
                    iter.remove();
                }
            }
        }
        if (set.isEmpty()) {
            if (DEBUG) {
                System.out.println("unable to enumerate data files in " + baseName);
            }
            addLocaleIDsFromListFile(bn, loader, set);
        }
        if (set.isEmpty()) {
            addLocaleIDsFromIndexBundle(baseName, loader, set);
        }
        set.remove("root");
        set.add(ULocale.ROOT.toString());
        return Collections.unmodifiableSet(set);
    }

    private static Set<String> createLocaleNameSet(String baseName, ClassLoader loader) {
        HashSet<String> set = new HashSet();
        addLocaleIDsFromIndexBundle(baseName, loader, set);
        return Collections.unmodifiableSet(set);
    }

    private static AvailEntry getAvailEntry(String key, ClassLoader loader) {
        return (AvailEntry) GET_AVAILABLE_CACHE.getInstance(key, loader);
    }

    private static final ICUResourceBundle findResourceWithFallback(String path, UResourceBundle actualBundle, UResourceBundle requested) {
        if (path.length() == 0) {
            return null;
        }
        ICUResourceBundle base = (ICUResourceBundle) actualBundle;
        int depth = base.getResDepth();
        int numPathKeys = countPathKeys(path);
        if (-assertionsDisabled || numPathKeys > 0) {
            String[] keys = new String[(depth + numPathKeys)];
            getResPathKeys(path, numPathKeys, keys, depth);
            return findResourceWithFallback(keys, depth, base, requested);
        }
        throw new AssertionError();
    }

    private static final ICUResourceBundle findResourceWithFallback(String[] keys, int depth, ICUResourceBundle base, UResourceBundle requested) {
        if (requested == null) {
            requested = base;
        }
        while (true) {
            int depth2 = depth + 1;
            ICUResourceBundle sub = (ICUResourceBundle) base.handleGet(keys[depth], null, requested);
            if (sub == null) {
                depth = depth2 - 1;
                ICUResourceBundle nextBase = base.getParent();
                if (nextBase == null) {
                    return null;
                }
                int baseDepth = base.getResDepth();
                if (depth != baseDepth) {
                    String[] newKeys = new String[((keys.length - depth) + baseDepth)];
                    System.arraycopy(keys, depth, newKeys, baseDepth, keys.length - depth);
                    keys = newKeys;
                }
                base.getResPathKeys(keys, baseDepth);
                base = nextBase;
                depth = 0;
            } else if (depth2 == keys.length) {
                return sub;
            } else {
                base = sub;
                depth = depth2;
            }
        }
    }

    private static final String findStringWithFallback(String path, UResourceBundle actualBundle, UResourceBundle requested) {
        if (path.length() == 0) {
            return null;
        }
        if (!(actualBundle instanceof ResourceContainer)) {
            return null;
        }
        if (requested == null) {
            requested = actualBundle;
        }
        ICUResourceBundle base = (ICUResourceBundle) actualBundle;
        ICUResourceBundleReader reader = base.wholeBundle.reader;
        int res = -1;
        int baseDepth = base.getResDepth();
        int depth = baseDepth;
        int numPathKeys = countPathKeys(path);
        if (-assertionsDisabled || numPathKeys > 0) {
            ICUResourceBundle aliasedResource;
            String[] keys = new String[(baseDepth + numPathKeys)];
            getResPathKeys(path, numPathKeys, keys, baseDepth);
            int depth2 = depth;
            while (true) {
                Container readerContainer;
                ICUResourceBundle nextBase;
                int type;
                if (res == -1) {
                    type = base.getType();
                    if (type == 2 || type == 8) {
                        readerContainer = ((ResourceContainer) base).value;
                    } else {
                        nextBase = base.getParent();
                        if (nextBase != null) {
                            return null;
                        }
                        base.getResPathKeys(keys, baseDepth);
                        base = nextBase;
                        reader = nextBase.wholeBundle.reader;
                        baseDepth = 0;
                        depth = 0;
                        depth2 = depth;
                    }
                } else {
                    type = ICUResourceBundleReader.RES_GET_TYPE(res);
                    if (ICUResourceBundleReader.URES_IS_TABLE(type)) {
                        readerContainer = reader.getTable(res);
                    } else if (ICUResourceBundleReader.URES_IS_ARRAY(type)) {
                        readerContainer = reader.getArray(res);
                    } else {
                        res = -1;
                        depth = depth2;
                        nextBase = base.getParent();
                        if (nextBase != null) {
                            return null;
                        }
                        base.getResPathKeys(keys, baseDepth);
                        base = nextBase;
                        reader = nextBase.wholeBundle.reader;
                        baseDepth = 0;
                        depth = 0;
                        depth2 = depth;
                    }
                }
                depth = depth2 + 1;
                String subKey = keys[depth2];
                res = readerContainer.getResource(reader, subKey);
                if (res == -1) {
                    depth--;
                    nextBase = base.getParent();
                    if (nextBase != null) {
                        return null;
                    }
                    base.getResPathKeys(keys, baseDepth);
                    base = nextBase;
                    reader = nextBase.wholeBundle.reader;
                    baseDepth = 0;
                    depth = 0;
                    depth2 = depth;
                } else {
                    if (ICUResourceBundleReader.RES_GET_TYPE(res) == 3) {
                        base.getResPathKeys(keys, baseDepth);
                        aliasedResource = getAliasedResource(base, keys, depth, subKey, res, null, requested);
                    } else {
                        aliasedResource = null;
                    }
                    if (depth == keys.length) {
                        break;
                    }
                    if (aliasedResource != null) {
                        base = aliasedResource;
                        reader = aliasedResource.wholeBundle.reader;
                        res = -1;
                        baseDepth = base.getResDepth();
                        if (depth != baseDepth) {
                            String[] newKeys = new String[((keys.length - depth) + baseDepth)];
                            System.arraycopy(keys, depth, newKeys, baseDepth, keys.length - depth);
                            keys = newKeys;
                            depth = baseDepth;
                        }
                    }
                    depth2 = depth;
                }
            }
            if (aliasedResource != null) {
                return aliasedResource.getString();
            }
            String s = reader.getString(res);
            if (s != null) {
                return s;
            }
            throw new UResourceTypeMismatchException("");
        }
        throw new AssertionError();
    }

    private int getResDepth() {
        return this.container == null ? 0 : this.container.getResDepth() + 1;
    }

    private void getResPathKeys(String[] keys, int depth) {
        ICUResourceBundle b = this;
        while (depth > 0) {
            depth--;
            keys[depth] = b.key;
            b = b.container;
            if (!-assertionsDisabled) {
                if ((depth == 0 ? 1 : null) != (b.container == null ? 1 : null)) {
                    throw new AssertionError();
                }
            }
        }
    }

    private static int countPathKeys(String path) {
        if (path.isEmpty()) {
            return 0;
        }
        int num = 1;
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == RES_PATH_SEP_CHAR) {
                num++;
            }
        }
        return num;
    }

    private static void getResPathKeys(String path, int num, String[] keys, int start) {
        if (num != 0) {
            if (num == 1) {
                keys[start] = path;
                return;
            }
            int j;
            int start2;
            int i = 0;
            while (true) {
                j = path.indexOf(47, i);
                if (-assertionsDisabled || j >= i) {
                    start2 = start + 1;
                    keys[start] = path.substring(i, j);
                    if (num == 2) {
                        break;
                    }
                    i = j + 1;
                    num--;
                    start = start2;
                } else {
                    throw new AssertionError();
                }
            }
            if (-assertionsDisabled || path.indexOf(47, j + 1) < 0) {
                keys[start2] = path.substring(j + 1);
                return;
            }
            throw new AssertionError();
        }
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof ICUResourceBundle) {
            ICUResourceBundle o = (ICUResourceBundle) other;
            if (getBaseName().equals(o.getBaseName()) && getLocaleID().equals(o.getLocaleID())) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        if (-assertionsDisabled) {
            return 42;
        }
        throw new AssertionError("hashCode not designed");
    }

    public static ICUResourceBundle getBundleInstance(String baseName, String localeID, ClassLoader root, boolean disableFallback) {
        return getBundleInstance(baseName, localeID, root, disableFallback ? OpenType.DIRECT : OpenType.LOCALE_DEFAULT_ROOT);
    }

    public static ICUResourceBundle getBundleInstance(String baseName, ULocale locale, OpenType openType) {
        if (locale == null) {
            locale = ULocale.getDefault();
        }
        return getBundleInstance(baseName, locale.getBaseName(), ICU_DATA_CLASS_LOADER, openType);
    }

    public static ICUResourceBundle getBundleInstance(String baseName, String localeID, ClassLoader root, OpenType openType) {
        ICUResourceBundle b;
        if (baseName == null) {
            baseName = ICUData.ICU_BASE_NAME;
        }
        localeID = ULocale.getBaseName(localeID);
        if (openType == OpenType.LOCALE_DEFAULT_ROOT) {
            b = instantiateBundle(baseName, localeID, ULocale.getDefault().getBaseName(), root, openType);
        } else {
            b = instantiateBundle(baseName, localeID, null, root, openType);
        }
        if (b != null) {
            return b;
        }
        throw new MissingResourceException("Could not find the bundle " + baseName + RES_PATH_SEP_STR + localeID + ".res", "", "");
    }

    private static boolean localeIDStartsWithLangSubtag(String localeID, String lang) {
        if (localeID.startsWith(lang)) {
            return localeID.length() == lang.length() || localeID.charAt(lang.length()) == '_';
        } else {
            return false;
        }
    }

    private static ICUResourceBundle instantiateBundle(String baseName, String localeID, String defaultID, ClassLoader root, OpenType openType) {
        if (!-assertionsDisabled && localeID.indexOf(64) >= 0) {
            throw new AssertionError();
        } else if (-assertionsDisabled || defaultID == null || defaultID.indexOf(64) < 0) {
            String cacheKey;
            final String fullName = ICUResourceBundleReader.getFullName(baseName, localeID);
            char openTypeChar = (char) (openType.ordinal() + 48);
            if (openType != OpenType.LOCALE_DEFAULT_ROOT) {
                cacheKey = fullName + '#' + openTypeChar;
            } else {
                cacheKey = fullName + '#' + openTypeChar + '#' + defaultID;
            }
            final String str = baseName;
            final String str2 = localeID;
            final ClassLoader classLoader = root;
            final OpenType openType2 = openType;
            final String str3 = defaultID;
            return (ICUResourceBundle) BUNDLE_CACHE.getInstance(cacheKey, new Loader() {
                public ICUResourceBundle load() {
                    if (ICUResourceBundle.DEBUG) {
                        System.out.println("Creating " + fullName);
                    }
                    String rootLocale = str.indexOf(46) == -1 ? "root" : "";
                    String localeName = str2.isEmpty() ? rootLocale : str2;
                    ICUResourceBundle b = ICUResourceBundle.createBundle(str, localeName, classLoader);
                    if (ICUResourceBundle.DEBUG) {
                        boolean -wrap2;
                        PrintStream printStream = System.out;
                        StringBuilder append = new StringBuilder().append("The bundle created is: ").append(b).append(" and openType=").append(openType2).append(" and bundle.getNoFallback=");
                        if (b != null) {
                            -wrap2 = b.getNoFallback();
                        } else {
                            -wrap2 = false;
                        }
                        printStream.println(append.append(-wrap2).toString());
                    }
                    if (openType2 == OpenType.DIRECT || (b != null && b.getNoFallback())) {
                        return b;
                    }
                    int i;
                    if (b == null) {
                        i = localeName.lastIndexOf(95);
                        if (i != -1) {
                            b = ICUResourceBundle.instantiateBundle(str, localeName.substring(0, i), str3, classLoader, openType2);
                        } else if (openType2 == OpenType.LOCALE_DEFAULT_ROOT && (ICUResourceBundle.localeIDStartsWithLangSubtag(str3, localeName) ^ 1) != 0) {
                            b = ICUResourceBundle.instantiateBundle(str, str3, str3, classLoader, openType2);
                        } else if (!(openType2 == OpenType.LOCALE_ONLY || (rootLocale.isEmpty() ^ 1) == 0)) {
                            b = ICUResourceBundle.createBundle(str, rootLocale, classLoader);
                        }
                    } else {
                        ResourceBundle parent = null;
                        localeName = b.getLocaleID();
                        i = localeName.lastIndexOf(95);
                        String parentLocaleName = ((ResourceTable) b).findString("%%Parent");
                        if (parentLocaleName != null) {
                            parent = ICUResourceBundle.instantiateBundle(str, parentLocaleName, str3, classLoader, openType2);
                        } else if (i != -1) {
                            parent = ICUResourceBundle.instantiateBundle(str, localeName.substring(0, i), str3, classLoader, openType2);
                        } else if (!localeName.equals(rootLocale)) {
                            parent = ICUResourceBundle.instantiateBundle(str, rootLocale, str3, classLoader, openType2);
                        }
                        if (!b.equals(parent)) {
                            b.setParent(parent);
                        }
                    }
                    return b;
                }
            });
        } else {
            throw new AssertionError();
        }
    }

    ICUResourceBundle get(String aKey, HashMap<String, String> aliasesVisited, UResourceBundle requested) {
        ICUResourceBundle obj = (ICUResourceBundle) handleGet(aKey, (HashMap) aliasesVisited, requested);
        if (obj == null) {
            obj = getParent();
            if (obj != null) {
                obj = obj.get(aKey, aliasesVisited, requested);
            }
            if (obj == null) {
                throw new MissingResourceException("Can't find resource for bundle " + ICUResourceBundleReader.getFullName(getBaseName(), getLocaleID()) + ", key " + aKey, getClass().getName(), aKey);
            }
        }
        return obj;
    }

    public static ICUResourceBundle createBundle(String baseName, String localeID, ClassLoader root) {
        ICUResourceBundleReader reader = ICUResourceBundleReader.getReader(baseName, localeID, root);
        if (reader == null) {
            return null;
        }
        return getBundle(reader, baseName, localeID, root);
    }

    protected String getLocaleID() {
        return this.wholeBundle.localeID;
    }

    protected String getBaseName() {
        return this.wholeBundle.baseName;
    }

    public ULocale getULocale() {
        return this.wholeBundle.ulocale;
    }

    public boolean isRoot() {
        return !this.wholeBundle.localeID.isEmpty() ? this.wholeBundle.localeID.equals("root") : true;
    }

    public ICUResourceBundle getParent() {
        return (ICUResourceBundle) this.parent;
    }

    protected void setParent(ResourceBundle parent) {
        this.parent = parent;
    }

    public String getKey() {
        return this.key;
    }

    private boolean getNoFallback() {
        return this.wholeBundle.reader.getNoFallback();
    }

    private static ICUResourceBundle getBundle(ICUResourceBundleReader reader, String baseName, String localeID, ClassLoader loader) {
        int rootRes = reader.getRootResource();
        if (ICUResourceBundleReader.URES_IS_TABLE(ICUResourceBundleReader.RES_GET_TYPE(rootRes))) {
            ResourceTable rootTable = new ResourceTable(new WholeBundle(baseName, localeID, loader, reader), rootRes);
            String aliasString = rootTable.findString("%%ALIAS");
            if (aliasString != null) {
                return (ICUResourceBundle) UResourceBundle.getBundleInstance(baseName, aliasString);
            }
            return rootTable;
        }
        throw new IllegalStateException("Invalid format error");
    }

    protected ICUResourceBundle(WholeBundle wholeBundle) {
        this.wholeBundle = wholeBundle;
    }

    protected ICUResourceBundle(ICUResourceBundle container, String key) {
        this.key = key;
        this.wholeBundle = container.wholeBundle;
        this.container = container;
        this.parent = container.parent;
    }

    protected static ICUResourceBundle getAliasedResource(ICUResourceBundle base, String[] keys, int depth, String key, int _resource, HashMap<String, String> aliasesVisited, UResourceBundle requested) {
        WholeBundle wholeBundle = base.wholeBundle;
        ClassLoader loaderToUse = wholeBundle.loader;
        String str = null;
        String rpath = wholeBundle.reader.getAlias(_resource);
        if (aliasesVisited == null) {
            aliasesVisited = new HashMap();
        }
        if (aliasesVisited.get(rpath) != null) {
            throw new IllegalArgumentException("Circular references in the resource bundles");
        }
        int i;
        String bundleName;
        String locale;
        aliasesVisited.put(rpath, "");
        if (rpath.indexOf(47) == 0) {
            i = rpath.indexOf(47, 1);
            int j = rpath.indexOf(47, i + 1);
            bundleName = rpath.substring(1, i);
            if (j < 0) {
                locale = rpath.substring(i + 1);
            } else {
                locale = rpath.substring(i + 1, j);
                str = rpath.substring(j + 1, rpath.length());
            }
            if (bundleName.equals(ICUDATA)) {
                bundleName = ICUData.ICU_BASE_NAME;
                loaderToUse = ICU_DATA_CLASS_LOADER;
            } else if (bundleName.indexOf(ICUDATA) > -1) {
                int idx = bundleName.indexOf(45);
                if (idx > -1) {
                    bundleName = "android/icu/impl/data/icudt58b/" + bundleName.substring(idx + 1, bundleName.length());
                    loaderToUse = ICU_DATA_CLASS_LOADER;
                }
            }
        } else {
            i = rpath.indexOf(47);
            if (i != -1) {
                locale = rpath.substring(0, i);
                str = rpath.substring(i + 1);
            } else {
                locale = rpath;
            }
            bundleName = wholeBundle.baseName;
        }
        ICUResourceBundle sub = null;
        ICUResourceBundle bundle;
        if (bundleName.equals(LOCALE)) {
            bundleName = wholeBundle.baseName;
            str = rpath.substring(LOCALE.length() + 2, rpath.length());
            bundle = (ICUResourceBundle) requested;
            while (bundle.container != null) {
                bundle = bundle.container;
            }
            sub = findResourceWithFallback(str, bundle, null);
        } else {
            int numKeys;
            bundle = getBundleInstance(bundleName, locale, loaderToUse, false);
            if (str != null) {
                numKeys = countPathKeys(str);
                if (numKeys > 0) {
                    keys = new String[numKeys];
                    getResPathKeys(str, numKeys, keys, 0);
                }
            } else if (keys != null) {
                numKeys = depth;
            } else {
                depth = base.getResDepth();
                numKeys = depth + 1;
                keys = new String[numKeys];
                base.getResPathKeys(keys, depth);
                keys[depth] = key;
            }
            if (numKeys > 0) {
                sub = bundle;
                i = 0;
                while (sub != null && i < numKeys) {
                    sub = sub.get(keys[i], aliasesVisited, requested);
                    i++;
                }
            }
        }
        if (sub != null) {
            return sub;
        }
        throw new MissingResourceException(wholeBundle.localeID, wholeBundle.baseName, key);
    }

    @Deprecated
    public final Set<String> getTopLevelKeySet() {
        return this.wholeBundle.topLevelKeys;
    }

    @Deprecated
    public final void setTopLevelKeySet(Set<String> keySet) {
        this.wholeBundle.topLevelKeys = keySet;
    }

    protected Enumeration<String> handleGetKeys() {
        return Collections.enumeration(handleKeySet());
    }

    protected boolean isTopLevelResource() {
        return this.container == null;
    }
}
