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
    static final /* synthetic */ boolean $assertionsDisabled = false;
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

    private static abstract class Loader {
        abstract ICUResourceBundle load();

        private Loader() {
        }

        /* synthetic */ Loader(AnonymousClass1 x0) {
            this();
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
        String kwVal;
        String kwVal2;
        String defStr;
        ICUResourceBundle defStr2;
        ICUResourceBundle irb;
        String str = baseName;
        String str2 = resName;
        String str3 = keyword;
        String kwVal3 = locID.getKeywordValue(str3);
        String baseLoc = locID.getBaseName();
        String defStr3 = null;
        ULocale parent = new ULocale(baseLoc);
        ULocale defLoc = null;
        boolean lookForDefault = false;
        ULocale fullBase = null;
        int defDepth = 0;
        int resDepth = 0;
        if (kwVal3 == null || kwVal3.length() == 0 || kwVal3.equals(DEFAULT_TAG)) {
            kwVal3 = "";
            lookForDefault = true;
        }
        ICUResourceBundle r = (ICUResourceBundle) UResourceBundle.getBundleInstance(str, parent);
        if (isAvailable != null) {
            isAvailable[0] = false;
            ULocale[] availableULocales = getAvailEntry(baseName, loader).getULocaleList();
            int i = 0;
            while (true) {
                kwVal = kwVal3;
                int i2 = i;
                if (i2 < availableULocales.length) {
                    if (parent.equals(availableULocales[i2])) {
                        isAvailable[0] = true;
                        break;
                    }
                    i = i2 + 1;
                    kwVal3 = kwVal;
                    ULocale uLocale = locID;
                }
            }
        } else {
            kwVal = kwVal3;
        }
        while (true) {
            try {
                defStr3 = ((ICUResourceBundle) r.get(str2)).getString(DEFAULT_TAG);
                if (lookForDefault) {
                    lookForDefault = false;
                    kwVal = defStr3;
                }
                try {
                    defLoc = r.getULocale();
                } catch (MissingResourceException e) {
                }
            } catch (MissingResourceException e2) {
            }
            kwVal2 = kwVal;
            if (defLoc == null) {
                defDepth++;
                r = r.getParent();
            }
            if (r == null || defLoc != null) {
                defStr = defStr3;
                defStr2 = (ICUResourceBundle) UResourceBundle.getBundleInstance(str, new ULocale(baseLoc));
            } else {
                kwVal = kwVal2;
            }
        }
        defStr = defStr3;
        defStr2 = (ICUResourceBundle) UResourceBundle.getBundleInstance(str, new ULocale(baseLoc));
        do {
            try {
                irb = (ICUResourceBundle) defStr2.get(str2);
                irb.get(kwVal2);
                fullBase = irb.getULocale();
                if (fullBase != null && resDepth > defDepth) {
                    defStr = irb.getString(DEFAULT_TAG);
                    defLoc = defStr2.getULocale();
                    defDepth = resDepth;
                }
            } catch (MissingResourceException e3) {
            }
            if (fullBase == null) {
                resDepth++;
                defStr2 = defStr2.getParent();
            }
            if (defStr2 == null) {
                break;
            }
        } while (fullBase == null);
        if (!(fullBase != null || defStr == null || defStr.equals(kwVal2))) {
            ULocale uLocale2;
            kwVal2 = defStr;
            ULocale defLoc2 = defLoc;
            String defStr4 = defStr;
            int resDepth2 = 0;
            defStr2 = (ICUResourceBundle) UResourceBundle.getBundleInstance(str, new ULocale(baseLoc));
            while (true) {
                try {
                    irb = (ICUResourceBundle) defStr2.get(str2);
                    r = (ICUResourceBundle) irb.get(kwVal2);
                    fullBase = defStr2.getULocale();
                    if (!fullBase.getBaseName().equals(r.getULocale().getBaseName())) {
                        fullBase = null;
                    }
                    if (fullBase != null && resDepth2 > defDepth) {
                        defStr4 = irb.getString(DEFAULT_TAG);
                        defLoc2 = defStr2.getULocale();
                        defDepth = resDepth2;
                    }
                } catch (MissingResourceException e4) {
                }
                if (fullBase == null) {
                    resDepth2++;
                    defStr2 = defStr2.getParent();
                }
                if (defStr2 == null || fullBase != null) {
                    uLocale2 = defLoc2;
                    resDepth = resDepth2;
                    defStr = defStr4;
                    defLoc = uLocale2;
                } else {
                    str2 = resName;
                }
            }
            uLocale2 = defLoc2;
            resDepth = resDepth2;
            defStr = defStr4;
            defLoc = uLocale2;
        }
        StringBuilder stringBuilder;
        if (fullBase == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(str3);
            stringBuilder.append("=");
            stringBuilder.append(kwVal2);
            throw new MissingResourceException("Could not find locale containing requested or default keyword.", str, stringBuilder.toString());
        } else if (omitDefault && defStr.equals(kwVal2) && resDepth <= defDepth) {
            return fullBase;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(fullBase.getBaseName());
            stringBuilder.append("@");
            stringBuilder.append(str3);
            stringBuilder.append("=");
            stringBuilder.append(kwVal2);
            return new ULocale(stringBuilder.toString());
        }
    }

    public static final String[] getKeywordValues(String baseName, String keyword) {
        Set<String> keywords = new HashSet();
        ULocale[] locales = getAvailEntry(baseName, ICU_DATA_CLASS_LOADER).getULocaleList();
        for (ULocale b : locales) {
            try {
                Enumeration<String> e = ((ICUResourceBundle) UResourceBundle.getBundleInstance(baseName, b).getObject(keyword)).getKeys();
                while (e.hasMoreElements()) {
                    String s = (String) e.nextElement();
                    if (!(DEFAULT_TAG.equals(s) || s.startsWith("private-"))) {
                        keywords.add(s);
                    }
                }
            } catch (Throwable th) {
            }
        }
        return (String[]) keywords.toArray(new String[0]);
    }

    public ICUResourceBundle getWithFallback(String path) throws MissingResourceException {
        ICUResourceBundle result = findResourceWithFallback(path, this, null);
        if (result == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can't find resource for bundle ");
            stringBuilder.append(getClass().getName());
            stringBuilder.append(", key ");
            stringBuilder.append(getType());
            throw new MissingResourceException(stringBuilder.toString(), path, getKey());
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
        String result = findStringWithFallback(path, this, null);
        if (result == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can't find resource for bundle ");
            stringBuilder.append(getClass().getName());
            stringBuilder.append(", key ");
            stringBuilder.append(getType());
            throw new MissingResourceException(stringBuilder.toString(), path, getKey());
        } else if (!result.equals(NO_INHERITANCE_MARKER)) {
            return result;
        } else {
            throw new MissingResourceException("Encountered NO_INHERITANCE_MARKER", path, getKey());
        }
    }

    public void getAllItemsWithFallbackNoFail(String path, Sink sink) {
        try {
            getAllItemsWithFallback(path, sink);
        } catch (MissingResourceException e) {
        }
    }

    public void getAllItemsWithFallback(String path, Sink sink) throws MissingResourceException {
        int depth;
        int numPathKeys = countPathKeys(path);
        if (numPathKeys == 0) {
            depth = this;
        } else {
            depth = getResDepth();
            String[] pathKeys = new String[(depth + numPathKeys)];
            getResPathKeys(path, numPathKeys, pathKeys, depth);
            ICUResourceBundle rb = findResourceWithFallback(pathKeys, depth, this, null);
            if (rb != null) {
                depth = rb;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Can't find resource for bundle ");
                stringBuilder.append(getClass().getName());
                stringBuilder.append(", key ");
                stringBuilder.append(getType());
                throw new MissingResourceException(stringBuilder.toString(), path, getKey());
            }
        }
        depth.getAllItemsWithFallback(new Key(), new ReaderValue(), sink);
    }

    private void getAllItemsWithFallback(Key key, ReaderValue readerValue, Sink sink) {
        ICUResourceBundleImpl impl = (ICUResourceBundleImpl) this;
        readerValue.reader = impl.wholeBundle.reader;
        readerValue.res = impl.getResource();
        key.setString(this.key != null ? this.key : "");
        sink.put(key, readerValue, this.parent == null);
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
        for (Locale loc : ulocales) {
            Locale loc2 = loc2.toLocale();
            if (!uniqueSet.contains(loc2)) {
                list.add(loc2);
                uniqueSet.add(loc2);
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
            int i2;
            String locstr = iter.next().getKey();
            if (locstr.equals("root")) {
                i2 = i + 1;
                locales[i] = ULocale.ROOT;
            } else {
                i2 = i + 1;
                locales[i] = new ULocale(locstr);
            }
            i = i2;
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
                PrintStream printStream = System.out;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("couldn't find ");
                stringBuilder.append(baseName);
                stringBuilder.append(RES_PATH_SEP_CHAR);
                stringBuilder.append(ICU_RESOURCE_INDEX);
                stringBuilder.append(".res");
                printStream.println(stringBuilder.toString());
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
                    URLVisitor v = new URLVisitor() {
                        public void visit(String s) {
                            if (s.endsWith(".res")) {
                                names.add(s.substring(null, s.length() - 4));
                            }
                        }
                    };
                    while (urls.hasMoreElements()) {
                        URL url = (URL) urls.nextElement();
                        URLHandler handler = URLHandler.get(url);
                        if (handler != null) {
                            handler.guide(v, false);
                        } else if (ICUResourceBundle.DEBUG) {
                            PrintStream printStream = System.out;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("handler for ");
                            stringBuilder.append(url);
                            stringBuilder.append(" is null");
                            printStream.println(stringBuilder.toString());
                        }
                    }
                    return null;
                } catch (IOException e) {
                    if (ICUResourceBundle.DEBUG) {
                        PrintStream printStream2 = System.out;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("ouch: ");
                        stringBuilder2.append(e.getMessage());
                        printStream2.println(stringBuilder2.toString());
                    }
                }
            }
        });
    }

    private static void addLocaleIDsFromListFile(String bn, ClassLoader root, Set<String> locales) {
        BufferedReader br;
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(bn);
            stringBuilder.append(FULL_LOCALE_NAMES_LIST);
            InputStream s = root.getResourceAsStream(stringBuilder.toString());
            if (s != null) {
                br = new BufferedReader(new InputStreamReader(s, "ASCII"));
                while (true) {
                    String readLine = br.readLine();
                    String line = readLine;
                    if (readLine == null) {
                        br.close();
                        return;
                    } else if (!(line.length() == 0 || line.startsWith("#"))) {
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
        String bn;
        if (baseName.endsWith(RES_PATH_SEP_STR)) {
            bn = baseName;
        } else {
            bn = new StringBuilder();
            bn.append(baseName);
            bn.append(RES_PATH_SEP_STR);
            bn = bn.toString();
        }
        Set<String> set = new HashSet();
        if (!ICUConfig.get("android.icu.impl.ICUResourceBundle.skipRuntimeLocaleResourceScan", "false").equalsIgnoreCase("true")) {
            addBundleBaseNamesFromClassLoader(bn, loader, set);
            if (baseName.startsWith(ICUData.ICU_BASE_NAME)) {
                String folder;
                if (baseName.length() == ICUData.ICU_BASE_NAME.length()) {
                    folder = "";
                } else if (baseName.charAt(ICUData.ICU_BASE_NAME.length()) == RES_PATH_SEP_CHAR) {
                    folder = baseName.substring(ICUData.ICU_BASE_NAME.length() + 1);
                } else {
                    folder = null;
                }
                if (folder != null) {
                    ICUBinary.addBaseNamesInFileFolder(folder, ".res", set);
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
                PrintStream printStream = System.out;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unable to enumerate data files in ");
                stringBuilder.append(baseName);
                printStream.println(stringBuilder.toString());
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
        String[] keys = new String[(depth + numPathKeys)];
        getResPathKeys(path, numPathKeys, keys, depth);
        return findResourceWithFallback(keys, depth, base, requested);
    }

    private static final ICUResourceBundle findResourceWithFallback(String[] keys, int subKey, ICUResourceBundle base, UResourceBundle requested) {
        if (requested == null) {
            requested = base;
        }
        while (true) {
            String subKey2;
            int depth = subKey2 + 1;
            ICUResourceBundle sub = (ICUResourceBundle) base.handleGet(keys[subKey2], null, requested);
            if (sub == null) {
                depth--;
                ICUResourceBundle nextBase = base.getParent();
                if (nextBase == null) {
                    return null;
                }
                int baseDepth = base.getResDepth();
                if (depth != baseDepth) {
                    sub = new String[((keys.length - depth) + baseDepth)];
                    System.arraycopy(keys, depth, sub, baseDepth, keys.length - depth);
                    keys = sub;
                }
                base.getResPathKeys(keys, baseDepth);
                base = nextBase;
                subKey2 = null;
            } else if (depth == keys.length) {
                return sub;
            } else {
                base = sub;
                subKey2 = depth;
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:53:0x00ea  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x00e8 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x00e8 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x00ea  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static final String findStringWithFallback(String path, UResourceBundle actualBundle, UResourceBundle requested) {
        UResourceBundle uResourceBundle = actualBundle;
        if (path.length() == 0 || !(uResourceBundle instanceof ResourceContainer)) {
            return null;
        }
        UResourceBundle requested2;
        if (requested == null) {
            requested2 = uResourceBundle;
        } else {
            requested2 = requested;
        }
        ICUResourceBundle base = (ICUResourceBundle) uResourceBundle;
        ICUResourceBundleReader reader = base.wholeBundle.reader;
        int res = -1;
        int baseDepth = base.getResDepth();
        String subKey = baseDepth;
        int numPathKeys = countPathKeys(path);
        String[] keys = new String[(subKey + numPathKeys)];
        getResPathKeys(path, numPathKeys, keys, subKey);
        ICUResourceBundle base2 = base;
        ICUResourceBundleReader reader2 = reader;
        int baseDepth2 = baseDepth;
        String[] keys2 = keys;
        while (true) {
            int type;
            Container readerContainer;
            ICUResourceBundle nextBase;
            if (res == -1) {
                type = base2.getType();
                if (type == 2 || type == 8) {
                    readerContainer = ((ResourceContainer) base2).value;
                }
                nextBase = base2.getParent();
                if (nextBase == null) {
                    return null;
                }
                base2.getResPathKeys(keys2, baseDepth2);
                base2 = nextBase;
                reader2 = base2.wholeBundle.reader;
                baseDepth2 = 0;
                subKey = null;
                Object obj = null;
            } else {
                type = ICUResourceBundleReader.RES_GET_TYPE(res);
                if (ICUResourceBundleReader.URES_IS_TABLE(type)) {
                    readerContainer = reader2.getTable(res);
                } else if (ICUResourceBundleReader.URES_IS_ARRAY(type)) {
                    readerContainer = reader2.getArray(res);
                } else {
                    res = -1;
                    nextBase = base2.getParent();
                    if (nextBase == null) {
                    }
                }
            }
            Container readerContainer2 = readerContainer;
            int depth = subKey + 1;
            subKey = keys2[subKey];
            baseDepth = readerContainer2.getResource(reader2, subKey);
            if (baseDepth == -1) {
                subKey = depth - 1;
                res = baseDepth;
                nextBase = base2.getParent();
                if (nextBase == null) {
                }
            } else {
                int res2;
                int depth2;
                if (ICUResourceBundleReader.RES_GET_TYPE(baseDepth) == 3) {
                    base2.getResPathKeys(keys2, baseDepth2);
                    res2 = baseDepth;
                    depth2 = depth;
                    base = getAliasedResource(base2, keys2, depth, subKey, res2, null, requested2);
                } else {
                    res2 = baseDepth;
                    String str = subKey;
                    depth2 = depth;
                    Container container = readerContainer2;
                    base = null;
                }
                if (depth2 != keys2.length) {
                    type = res2;
                    if (base != null) {
                        base2 = base;
                        res = base2.wholeBundle.reader;
                        type = -1;
                        baseDepth = base2.getResDepth();
                        if (depth2 != baseDepth) {
                            String[] newKeys = new String[((keys2.length - depth2) + baseDepth)];
                            System.arraycopy(keys2, depth2, newKeys, baseDepth, keys2.length - depth2);
                            keys2 = newKeys;
                            depth2 = baseDepth;
                        }
                        subKey = depth2;
                        reader2 = res;
                        baseDepth2 = baseDepth;
                    } else {
                        subKey = depth2;
                    }
                    res = type;
                } else if (base != null) {
                    return base.getString();
                } else {
                    String s = reader2.getString(res2);
                    if (s != null) {
                        return s;
                    }
                    throw new UResourceTypeMismatchException("");
                }
            }
        }
    }

    private int getResDepth() {
        return this.container == null ? 0 : this.container.getResDepth() + 1;
    }

    private void getResPathKeys(String[] keys, int depth) {
        int depth2 = depth;
        ICUResourceBundle b = this;
        while (depth2 > 0) {
            depth2--;
            keys[depth2] = b.key;
            b = b.container;
        }
    }

    private static int countPathKeys(String path) {
        int i = 0;
        if (path.isEmpty()) {
            return 0;
        }
        int num = 1;
        while (i < path.length()) {
            if (path.charAt(i) == RES_PATH_SEP_CHAR) {
                num++;
            }
            i++;
        }
        return num;
    }

    private static void getResPathKeys(String path, int num, String[] keys, int start) {
        if (num != 0) {
            if (num == 1) {
                keys[start] = path;
                return;
            }
            int i = 0;
            while (true) {
                int j = path.indexOf(47, i);
                int start2 = start + 1;
                keys[start] = path.substring(i, j);
                if (num == 2) {
                    keys[start2] = path.substring(j + 1);
                    return;
                }
                i = j + 1;
                num--;
                start = start2;
            }
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
        return 42;
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Could not find the bundle ");
        stringBuilder.append(baseName);
        stringBuilder.append(RES_PATH_SEP_STR);
        stringBuilder.append(localeID);
        stringBuilder.append(".res");
        throw new MissingResourceException(stringBuilder.toString(), "", "");
    }

    private static boolean localeIDStartsWithLangSubtag(String localeID, String lang) {
        return localeID.startsWith(lang) && (localeID.length() == lang.length() || localeID.charAt(lang.length()) == '_');
    }

    private static ICUResourceBundle instantiateBundle(String baseName, String localeID, String defaultID, ClassLoader root, OpenType openType) {
        String stringBuilder;
        String str;
        String fullName = ICUResourceBundleReader.getFullName(baseName, localeID);
        char openTypeChar = (char) (48 + openType.ordinal());
        OpenType openType2 = openType;
        StringBuilder stringBuilder2;
        if (openType2 != OpenType.LOCALE_DEFAULT_ROOT) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(fullName);
            stringBuilder2.append('#');
            stringBuilder2.append(openTypeChar);
            stringBuilder = stringBuilder2.toString();
            str = defaultID;
        } else {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(fullName);
            stringBuilder2.append('#');
            stringBuilder2.append(openTypeChar);
            stringBuilder2.append('#');
            str = defaultID;
            stringBuilder2.append(str);
            stringBuilder = stringBuilder2.toString();
        }
        final String str2 = fullName;
        final String str3 = baseName;
        final String str4 = localeID;
        final ClassLoader classLoader = root;
        final OpenType openType3 = openType2;
        final String str5 = str;
        return (ICUResourceBundle) BUNDLE_CACHE.getInstance(stringBuilder, new Loader() {
            public ICUResourceBundle load() {
                if (ICUResourceBundle.DEBUG) {
                    PrintStream printStream = System.out;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Creating ");
                    stringBuilder.append(str2);
                    printStream.println(stringBuilder.toString());
                }
                String rootLocale = str3.indexOf(46) == -1 ? "root" : "";
                String localeName = str4.isEmpty() ? rootLocale : str4;
                ICUResourceBundle b = ICUResourceBundle.createBundle(str3, localeName, classLoader);
                if (ICUResourceBundle.DEBUG) {
                    PrintStream printStream2 = System.out;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("The bundle created is: ");
                    stringBuilder2.append(b);
                    stringBuilder2.append(" and openType=");
                    stringBuilder2.append(openType3);
                    stringBuilder2.append(" and bundle.getNoFallback=");
                    boolean z = b != null && b.getNoFallback();
                    stringBuilder2.append(z);
                    printStream2.println(stringBuilder2.toString());
                }
                if (openType3 == OpenType.DIRECT || (b != null && b.getNoFallback())) {
                    return b;
                }
                int i;
                if (b == null) {
                    ICUResourceBundle b2;
                    i = localeName.lastIndexOf(95);
                    if (i != -1) {
                        b2 = ICUResourceBundle.instantiateBundle(str3, localeName.substring(0, i), str5, classLoader, openType3);
                    } else if (openType3 == OpenType.LOCALE_DEFAULT_ROOT && !ICUResourceBundle.localeIDStartsWithLangSubtag(str5, localeName)) {
                        b2 = ICUResourceBundle.instantiateBundle(str3, str5, str5, classLoader, openType3);
                    } else if (!(openType3 == OpenType.LOCALE_ONLY || rootLocale.isEmpty())) {
                        b2 = ICUResourceBundle.createBundle(str3, rootLocale, classLoader);
                    }
                    b = b2;
                } else {
                    UResourceBundle parent = null;
                    localeName = b.getLocaleID();
                    i = localeName.lastIndexOf(95);
                    String parentLocaleName = ((ResourceTable) b).findString("%%Parent");
                    if (parentLocaleName != null) {
                        parent = ICUResourceBundle.instantiateBundle(str3, parentLocaleName, str5, classLoader, openType3);
                    } else if (i != -1) {
                        parent = ICUResourceBundle.instantiateBundle(str3, localeName.substring(0, i), str5, classLoader, openType3);
                    } else if (!localeName.equals(rootLocale)) {
                        parent = ICUResourceBundle.instantiateBundle(str3, rootLocale, str5, classLoader, openType3);
                    }
                    if (!b.equals(parent)) {
                        b.setParent(parent);
                    }
                }
                return b;
            }
        });
    }

    ICUResourceBundle get(String aKey, HashMap<String, String> aliasesVisited, UResourceBundle requested) {
        ICUResourceBundle obj = (ICUResourceBundle) handleGet(aKey, (HashMap) aliasesVisited, requested);
        if (obj == null) {
            obj = getParent();
            if (obj != null) {
                obj = obj.get(aKey, aliasesVisited, requested);
            }
            if (obj == null) {
                String fullName = ICUResourceBundleReader.getFullName(getBaseName(), getLocaleID());
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Can't find resource for bundle ");
                stringBuilder.append(fullName);
                stringBuilder.append(", key ");
                stringBuilder.append(aKey);
                throw new MissingResourceException(stringBuilder.toString(), getClass().getName(), aKey);
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
        return this.wholeBundle.localeID.isEmpty() || this.wholeBundle.localeID.equals("root");
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
        HashMap<String, String> aliasesVisited2;
        ICUResourceBundle iCUResourceBundle = base;
        String str = key;
        UResourceBundle uResourceBundle = requested;
        WholeBundle wholeBundle = iCUResourceBundle.wholeBundle;
        ClassLoader loaderToUse = wholeBundle.loader;
        String keyPath = null;
        String rpath = wholeBundle.reader.getAlias(_resource);
        if (aliasesVisited == null) {
            aliasesVisited2 = new HashMap();
        } else {
            aliasesVisited2 = aliasesVisited;
        }
        ClassLoader loaderToUse2;
        if (aliasesVisited2.get(rpath) == null) {
            int i;
            String bundleName;
            String locale;
            int idx;
            aliasesVisited2.put(rpath, "");
            if (rpath.indexOf(47) == 0) {
                i = rpath.indexOf(47, 1);
                int j = rpath.indexOf(47, i + 1);
                bundleName = rpath.substring(1, i);
                if (j < 0) {
                    locale = rpath.substring(i + 1);
                } else {
                    locale = rpath.substring(i + 1, j);
                    keyPath = rpath.substring(j + 1, rpath.length());
                }
                if (bundleName.equals(ICUDATA)) {
                    bundleName = ICUData.ICU_BASE_NAME;
                    loaderToUse = ICU_DATA_CLASS_LOADER;
                } else {
                    if (bundleName.indexOf(ICUDATA) > -1) {
                        idx = bundleName.indexOf(45);
                        if (idx > -1) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("android/icu/impl/data/icudt60b/");
                            stringBuilder.append(bundleName.substring(idx + 1, bundleName.length()));
                            bundleName = stringBuilder.toString();
                            loaderToUse = ICU_DATA_CLASS_LOADER;
                        }
                    }
                    loaderToUse = loaderToUse;
                }
            } else {
                loaderToUse2 = loaderToUse;
                int i2 = rpath.indexOf(47);
                if (i2 != -1) {
                    bundleName = rpath.substring(0, i2);
                    keyPath = rpath.substring(i2 + 1);
                } else {
                    bundleName = rpath;
                }
                locale = bundleName;
                bundleName = wholeBundle.baseName;
                loaderToUse = loaderToUse2;
            }
            String bundleName2 = bundleName;
            ICUResourceBundle sub = null;
            ICUResourceBundle bundle;
            String[] strArr;
            ClassLoader classLoader;
            if (bundleName2.equals(LOCALE)) {
                bundleName2 = wholeBundle.baseName;
                keyPath = rpath.substring(LOCALE.length() + 2, rpath.length());
                bundle = (ICUResourceBundle) uResourceBundle;
                while (bundle.container != null) {
                    bundle = bundle.container;
                }
                sub = findResourceWithFallback(keyPath, bundle, null);
                strArr = keys;
                idx = depth;
                classLoader = loaderToUse;
            } else {
                bundle = getBundleInstance(bundleName2, locale, loaderToUse, false);
                if (keyPath != null) {
                    i = countPathKeys(keyPath);
                    if (i > 0) {
                        strArr = new String[i];
                        getResPathKeys(keyPath, i, strArr, 0);
                    } else {
                        strArr = keys;
                    }
                } else if (keys != null) {
                    idx = depth;
                    i = depth;
                    strArr = keys;
                } else {
                    int depth2 = base.getResDepth();
                    idx = depth2 + 1;
                    String[] keys2 = new String[idx];
                    iCUResourceBundle.getResPathKeys(keys2, depth2);
                    keys2[depth2] = str;
                    String[] strArr2 = keys2;
                    i = idx;
                    strArr = strArr2;
                }
                if (i > 0) {
                    sub = bundle;
                    int i3 = 0;
                    while (true) {
                        int i4 = i3;
                        if (sub == null) {
                            break;
                        }
                        int i5 = i4;
                        if (i5 >= i) {
                            break;
                        }
                        classLoader = loaderToUse;
                        sub = sub.get(strArr[i5], aliasesVisited2, uResourceBundle);
                        i3 = i5 + 1;
                        loaderToUse = classLoader;
                        iCUResourceBundle = base;
                    }
                }
            }
            if (sub != null) {
                return sub;
            }
            throw new MissingResourceException(wholeBundle.localeID, wholeBundle.baseName, str);
        }
        loaderToUse2 = loaderToUse;
        throw new IllegalArgumentException("Circular references in the resource bundles");
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
