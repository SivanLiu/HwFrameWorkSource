package java.util;

import dalvik.system.VMStack;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarEntry;
import sun.reflect.CallerSensitive;
import sun.util.locale.BaseLocale;
import sun.util.locale.LocaleObjectCache;

public abstract class ResourceBundle {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int INITIAL_CACHE_SIZE = 32;
    private static final ResourceBundle NONEXISTENT_BUNDLE = new ResourceBundle() {
        public Enumeration<String> getKeys() {
            return null;
        }

        protected Object handleGetObject(String key) {
            return null;
        }

        public String toString() {
            return "NONEXISTENT_BUNDLE";
        }
    };
    private static final ConcurrentMap<CacheKey, BundleReference> cacheList = new ConcurrentHashMap(32);
    private static final ReferenceQueue<Object> referenceQueue = new ReferenceQueue();
    private volatile CacheKey cacheKey;
    private volatile boolean expired;
    private volatile Set<String> keySet;
    private Locale locale = null;
    private String name;
    protected ResourceBundle parent = null;

    private interface CacheKeyReference {
        CacheKey getCacheKey();
    }

    public static class Control {
        private static final CandidateListCache CANDIDATES_CACHE = new CandidateListCache();
        public static final List<String> FORMAT_CLASS = Collections.unmodifiableList(Arrays.asList("java.class"));
        public static final List<String> FORMAT_DEFAULT = Collections.unmodifiableList(Arrays.asList("java.class", "java.properties"));
        public static final List<String> FORMAT_PROPERTIES = Collections.unmodifiableList(Arrays.asList("java.properties"));
        private static final Control INSTANCE = new Control();
        public static final long TTL_DONT_CACHE = -1;
        public static final long TTL_NO_EXPIRATION_CONTROL = -2;

        private static class CandidateListCache extends LocaleObjectCache<BaseLocale, List<Locale>> {
            private CandidateListCache() {
            }

            /* synthetic */ CandidateListCache(AnonymousClass1 x0) {
                this();
            }

            /* JADX WARNING: Removed duplicated region for block: B:47:0x00b0  */
            /* JADX WARNING: Removed duplicated region for block: B:46:0x00ad  */
            /* JADX WARNING: Removed duplicated region for block: B:47:0x00b0  */
            /* JADX WARNING: Removed duplicated region for block: B:46:0x00ad  */
            /* JADX WARNING: Removed duplicated region for block: B:47:0x00b0  */
            /* JADX WARNING: Removed duplicated region for block: B:46:0x00ad  */
            /* JADX WARNING: Removed duplicated region for block: B:47:0x00b0  */
            /* JADX WARNING: Removed duplicated region for block: B:46:0x00ad  */
            /* JADX WARNING: Missing block: B:40:0x009b, code skipped:
            if (r2.equals("HK") != false) goto L_0x00a9;
     */
            /* JADX WARNING: Missing block: B:55:0x00ce, code skipped:
            if (r1.equals("Hant") != false) goto L_0x00dc;
     */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            protected List<Locale> createObject(BaseLocale base) {
                String language = base.getLanguage();
                String script = base.getScript();
                String region = base.getRegion();
                String variant = base.getVariant();
                boolean isNorwegianBokmal = ResourceBundle.$assertionsDisabled;
                boolean isNorwegianNynorsk = ResourceBundle.$assertionsDisabled;
                if (language.equals("no")) {
                    if (region.equals("NO") && variant.equals("NY")) {
                        variant = "";
                        isNorwegianNynorsk = true;
                    } else {
                        isNorwegianBokmal = true;
                    }
                }
                List<Locale> tmpList;
                if (language.equals("nb") || isNorwegianBokmal) {
                    tmpList = getDefaultList("nb", script, region, variant);
                    List<Locale> bokmalList = new LinkedList();
                    for (Locale l : tmpList) {
                        bokmalList.add(l);
                        if (l.getLanguage().length() == 0) {
                            break;
                        }
                        bokmalList.add(Locale.getInstance("no", l.getScript(), l.getCountry(), l.getVariant(), null));
                    }
                    return bokmalList;
                }
                int i = 1;
                if (language.equals("nn") || isNorwegianNynorsk) {
                    tmpList = getDefaultList("nn", script, region, variant);
                    int idx = tmpList.size() - 1;
                    i = idx + 1;
                    tmpList.add(idx, Locale.getInstance("no", "NO", "NY"));
                    idx = i + 1;
                    tmpList.add(i, Locale.getInstance("no", "NO", ""));
                    i = idx + 1;
                    tmpList.add(idx, Locale.getInstance("no", "", ""));
                    return tmpList;
                }
                if (language.equals("zh")) {
                    if (script.length() != 0 || region.length() <= 0) {
                        if (script.length() > 0 && region.length() == 0) {
                            switch (script.hashCode()) {
                                case 2241694:
                                    if (script.equals("Hans")) {
                                        i = 0;
                                        break;
                                    }
                                case 2241695:
                                    break;
                                default:
                                    i = -1;
                                    break;
                            }
                            switch (i) {
                                case 0:
                                    region = "CN";
                                    break;
                                case 1:
                                    region = "TW";
                                    break;
                            }
                        }
                    }
                    int hashCode = region.hashCode();
                    if (hashCode == 2155) {
                        if (region.equals("CN")) {
                            i = 3;
                            switch (i) {
                                case 0:
                                case 1:
                                case 2:
                                    break;
                                case 3:
                                case 4:
                                    break;
                            }
                        }
                    } else if (hashCode != 2307) {
                        if (hashCode == 2466) {
                            if (region.equals("MO")) {
                                i = 2;
                                switch (i) {
                                    case 0:
                                    case 1:
                                    case 2:
                                        break;
                                    case 3:
                                    case 4:
                                        break;
                                }
                            }
                        } else if (hashCode == 2644) {
                            if (region.equals("SG")) {
                                i = 4;
                                switch (i) {
                                    case 0:
                                    case 1:
                                    case 2:
                                        break;
                                    case 3:
                                    case 4:
                                        break;
                                }
                            }
                        } else if (hashCode == 2691 && region.equals("TW")) {
                            i = 0;
                            switch (i) {
                                case 0:
                                case 1:
                                case 2:
                                    script = "Hant";
                                    break;
                                case 3:
                                case 4:
                                    script = "Hans";
                                    break;
                            }
                        }
                    }
                    i = -1;
                    switch (i) {
                        case 0:
                        case 1:
                        case 2:
                            break;
                        case 3:
                        case 4:
                            break;
                    }
                }
                return getDefaultList(language, script, region, variant);
            }

            private static List<Locale> getDefaultList(String language, String script, String region, String variant) {
                List<String> variants = null;
                if (variant.length() > 0) {
                    variants = new LinkedList();
                    int idx = variant.length();
                    while (idx != -1) {
                        variants.add(variant.substring(0, idx));
                        idx = variant.lastIndexOf(95, idx - 1);
                    }
                }
                List<Locale> list = new LinkedList();
                if (variants != null) {
                    for (String v : variants) {
                        list.add(Locale.getInstance(language, script, region, v, null));
                    }
                }
                if (region.length() > 0) {
                    list.add(Locale.getInstance(language, script, region, "", null));
                }
                if (script.length() > 0) {
                    list.add(Locale.getInstance(language, script, "", "", null));
                    if (variants != null) {
                        for (String v2 : variants) {
                            list.add(Locale.getInstance(language, "", region, v2, null));
                        }
                    }
                    if (region.length() > 0) {
                        list.add(Locale.getInstance(language, "", region, "", null));
                    }
                }
                if (language.length() > 0) {
                    list.add(Locale.getInstance(language, "", "", "", null));
                }
                list.add(Locale.ROOT);
                return list;
            }
        }

        protected Control() {
        }

        public static final Control getControl(List<String> formats) {
            if (formats.equals(FORMAT_PROPERTIES)) {
                return SingleFormatControl.PROPERTIES_ONLY;
            }
            if (formats.equals(FORMAT_CLASS)) {
                return SingleFormatControl.CLASS_ONLY;
            }
            if (formats.equals(FORMAT_DEFAULT)) {
                return INSTANCE;
            }
            throw new IllegalArgumentException();
        }

        public static final Control getNoFallbackControl(List<String> formats) {
            if (formats.equals(FORMAT_DEFAULT)) {
                return NoFallbackControl.NO_FALLBACK;
            }
            if (formats.equals(FORMAT_PROPERTIES)) {
                return NoFallbackControl.PROPERTIES_ONLY_NO_FALLBACK;
            }
            if (formats.equals(FORMAT_CLASS)) {
                return NoFallbackControl.CLASS_ONLY_NO_FALLBACK;
            }
            throw new IllegalArgumentException();
        }

        public List<String> getFormats(String baseName) {
            if (baseName != null) {
                return FORMAT_DEFAULT;
            }
            throw new NullPointerException();
        }

        public List<Locale> getCandidateLocales(String baseName, Locale locale) {
            if (baseName != null) {
                return new ArrayList((Collection) CANDIDATES_CACHE.get(locale.getBaseLocale()));
            }
            throw new NullPointerException();
        }

        public Locale getFallbackLocale(String baseName, Locale locale) {
            if (baseName != null) {
                Locale defaultLocale = Locale.getDefault();
                return locale.equals(defaultLocale) ? null : defaultLocale;
            } else {
                throw new NullPointerException();
            }
        }

        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException {
            String bundleName = toBundleName(baseName, locale);
            ResourceBundle bundle = null;
            if (format.equals("java.class")) {
                try {
                    Class<? extends ResourceBundle> bundleClass = loader.loadClass(bundleName);
                    if (ResourceBundle.class.isAssignableFrom(bundleClass)) {
                        bundle = (ResourceBundle) bundleClass.newInstance();
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(bundleClass.getName());
                        stringBuilder.append(" cannot be cast to ResourceBundle");
                        throw new ClassCastException(stringBuilder.toString());
                    }
                } catch (ClassNotFoundException e) {
                }
            } else if (format.equals("java.properties")) {
                final String resourceName = toResourceName0(bundleName, "properties");
                if (resourceName == null) {
                    return null;
                }
                final ClassLoader classLoader = loader;
                final boolean reloadFlag = reload;
                try {
                    InputStream stream = (InputStream) AccessController.doPrivileged(new PrivilegedExceptionAction<InputStream>() {
                        public InputStream run() throws IOException {
                            if (!reloadFlag) {
                                return classLoader.getResourceAsStream(resourceName);
                            }
                            URL url = classLoader.getResource(resourceName);
                            if (url == null) {
                                return null;
                            }
                            URLConnection connection = url.openConnection();
                            if (connection == null) {
                                return null;
                            }
                            connection.setUseCaches(ResourceBundle.$assertionsDisabled);
                            return connection.getInputStream();
                        }
                    });
                    if (stream != null) {
                        try {
                            bundle = new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                        } finally {
                            stream.close();
                        }
                    }
                } catch (PrivilegedActionException e2) {
                    throw ((IOException) e2.getException());
                }
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("unknown format: ");
                stringBuilder2.append(format);
                throw new IllegalArgumentException(stringBuilder2.toString());
            }
            return bundle;
        }

        public long getTimeToLive(String baseName, Locale locale) {
            if (baseName != null && locale != null) {
                return -2;
            }
            throw new NullPointerException();
        }

        public boolean needsReload(String baseName, Locale locale, String format, ClassLoader loader, ResourceBundle bundle, long loadTime) {
            NullPointerException npe;
            ClassLoader classLoader;
            String format2 = format;
            if (bundle != null) {
                if (format2.equals("java.class") || format2.equals("java.properties")) {
                    format2 = format2.substring(5);
                }
                String format3 = format2;
                boolean z = ResourceBundle.$assertionsDisabled;
                boolean result = ResourceBundle.$assertionsDisabled;
                try {
                    try {
                        String resourceName = toResourceName0(toBundleName(baseName, locale), format3);
                        if (resourceName == null) {
                            return result;
                        }
                        try {
                            URL url = loader.getResource(resourceName);
                            if (url != null) {
                                long lastModified = 0;
                                URLConnection connection = url.openConnection();
                                if (connection != null) {
                                    connection.setUseCaches(ResourceBundle.$assertionsDisabled);
                                    if (connection instanceof JarURLConnection) {
                                        JarEntry ent = ((JarURLConnection) connection).getJarEntry();
                                        if (ent != null) {
                                            lastModified = ent.getTime();
                                            if (lastModified == -1) {
                                                lastModified = 0;
                                            }
                                        }
                                    } else {
                                        lastModified = connection.getLastModified();
                                    }
                                }
                                if (lastModified >= loadTime) {
                                    z = true;
                                }
                                result = z;
                            }
                        } catch (NullPointerException e) {
                            npe = e;
                            throw npe;
                        } catch (Exception e2) {
                        }
                        return result;
                    } catch (NullPointerException e3) {
                        npe = e3;
                        classLoader = loader;
                        throw npe;
                    } catch (Exception e4) {
                        classLoader = loader;
                        return result;
                    }
                } catch (NullPointerException e5) {
                    npe = e5;
                    classLoader = loader;
                    throw npe;
                } catch (Exception e6) {
                    classLoader = loader;
                    return result;
                }
            }
            classLoader = loader;
            throw new NullPointerException();
        }

        public String toBundleName(String baseName, Locale locale) {
            if (locale == Locale.ROOT) {
                return baseName;
            }
            String language = locale.getLanguage();
            String script = locale.getScript();
            String country = locale.getCountry();
            String variant = locale.getVariant();
            if (language == "" && country == "" && variant == "") {
                return baseName;
            }
            StringBuilder sb = new StringBuilder(baseName);
            sb.append('_');
            if (script != "") {
                if (variant != "") {
                    sb.append(language);
                    sb.append('_');
                    sb.append(script);
                    sb.append('_');
                    sb.append(country);
                    sb.append('_');
                    sb.append(variant);
                } else if (country != "") {
                    sb.append(language);
                    sb.append('_');
                    sb.append(script);
                    sb.append('_');
                    sb.append(country);
                } else {
                    sb.append(language);
                    sb.append('_');
                    sb.append(script);
                }
            } else if (variant != "") {
                sb.append(language);
                sb.append('_');
                sb.append(country);
                sb.append('_');
                sb.append(variant);
            } else if (country != "") {
                sb.append(language);
                sb.append('_');
                sb.append(country);
            } else {
                sb.append(language);
            }
            return sb.toString();
        }

        public final String toResourceName(String bundleName, String suffix) {
            StringBuilder sb = new StringBuilder((bundleName.length() + 1) + suffix.length());
            sb.append(bundleName.replace('.', '/'));
            sb.append('.');
            sb.append(suffix);
            return sb.toString();
        }

        private String toResourceName0(String bundleName, String suffix) {
            if (bundleName.contains("://")) {
                return null;
            }
            return toResourceName(bundleName, suffix);
        }
    }

    private static class CacheKey implements Cloneable {
        private Throwable cause;
        private volatile long expirationTime;
        private String format;
        private int hashCodeCache;
        private volatile long loadTime;
        private LoaderReference loaderRef;
        private Locale locale;
        private String name;

        CacheKey(String baseName, Locale locale, ClassLoader loader) {
            this.name = baseName;
            this.locale = locale;
            if (loader == null) {
                this.loaderRef = null;
            } else {
                this.loaderRef = new LoaderReference(loader, ResourceBundle.referenceQueue, this);
            }
            calculateHashCode();
        }

        String getName() {
            return this.name;
        }

        CacheKey setName(String baseName) {
            if (!this.name.equals(baseName)) {
                this.name = baseName;
                calculateHashCode();
            }
            return this;
        }

        Locale getLocale() {
            return this.locale;
        }

        CacheKey setLocale(Locale locale) {
            if (!this.locale.equals(locale)) {
                this.locale = locale;
                calculateHashCode();
            }
            return this;
        }

        ClassLoader getLoader() {
            return this.loaderRef != null ? (ClassLoader) this.loaderRef.get() : null;
        }

        public boolean equals(Object other) {
            boolean z = true;
            if (this == other) {
                return true;
            }
            try {
                CacheKey otherEntry = (CacheKey) other;
                if (this.hashCodeCache != otherEntry.hashCodeCache || !this.name.equals(otherEntry.name) || !this.locale.equals(otherEntry.locale)) {
                    return ResourceBundle.$assertionsDisabled;
                }
                if (this.loaderRef == null) {
                    if (otherEntry.loaderRef != null) {
                        z = ResourceBundle.$assertionsDisabled;
                    }
                    return z;
                }
                ClassLoader loader = (ClassLoader) this.loaderRef.get();
                if (otherEntry.loaderRef == null || loader == null || loader != otherEntry.loaderRef.get()) {
                    z = ResourceBundle.$assertionsDisabled;
                }
                return z;
            } catch (ClassCastException | NullPointerException e) {
                return ResourceBundle.$assertionsDisabled;
            }
        }

        public int hashCode() {
            return this.hashCodeCache;
        }

        private void calculateHashCode() {
            this.hashCodeCache = this.name.hashCode() << 3;
            this.hashCodeCache ^= this.locale.hashCode();
            ClassLoader loader = getLoader();
            if (loader != null) {
                this.hashCodeCache ^= loader.hashCode();
            }
        }

        public Object clone() {
            try {
                CacheKey clone = (CacheKey) super.clone();
                if (this.loaderRef != null) {
                    clone.loaderRef = new LoaderReference((ClassLoader) this.loaderRef.get(), ResourceBundle.referenceQueue, clone);
                }
                clone.cause = null;
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new InternalError(e);
            }
        }

        String getFormat() {
            return this.format;
        }

        void setFormat(String format) {
            this.format = format;
        }

        private void setCause(Throwable cause) {
            if (this.cause == null) {
                this.cause = cause;
            } else if (this.cause instanceof ClassNotFoundException) {
                this.cause = cause;
            }
        }

        private Throwable getCause() {
            return this.cause;
        }

        public String toString() {
            StringBuilder stringBuilder;
            String l = this.locale.toString();
            if (l.length() == 0) {
                if (this.locale.getVariant().length() != 0) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("__");
                    stringBuilder.append(this.locale.getVariant());
                    l = stringBuilder.toString();
                } else {
                    l = "\"\"";
                }
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("CacheKey[");
            stringBuilder.append(this.name);
            stringBuilder.append(", lc=");
            stringBuilder.append(l);
            stringBuilder.append(", ldr=");
            stringBuilder.append(getLoader());
            stringBuilder.append("(format=");
            stringBuilder.append(this.format);
            stringBuilder.append(")]");
            return stringBuilder.toString();
        }
    }

    private static class RBClassLoader extends ClassLoader {
        private static final RBClassLoader INSTANCE = ((RBClassLoader) AccessController.doPrivileged(new PrivilegedAction<RBClassLoader>() {
            public RBClassLoader run() {
                return new RBClassLoader();
            }
        }));
        private static final ClassLoader loader = ClassLoader.getSystemClassLoader();

        /* synthetic */ RBClassLoader(AnonymousClass1 x0) {
            this();
        }

        private RBClassLoader() {
        }

        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (loader != null) {
                return loader.loadClass(name);
            }
            return Class.forName(name);
        }

        public URL getResource(String name) {
            if (loader != null) {
                return loader.getResource(name);
            }
            return ClassLoader.getSystemResource(name);
        }

        public InputStream getResourceAsStream(String name) {
            if (loader != null) {
                return loader.getResourceAsStream(name);
            }
            return ClassLoader.getSystemResourceAsStream(name);
        }
    }

    private static class SingleFormatControl extends Control {
        private static final Control CLASS_ONLY = new SingleFormatControl(FORMAT_CLASS);
        private static final Control PROPERTIES_ONLY = new SingleFormatControl(FORMAT_PROPERTIES);
        private final List<String> formats;

        protected SingleFormatControl(List<String> formats) {
            this.formats = formats;
        }

        public List<String> getFormats(String baseName) {
            if (baseName != null) {
                return this.formats;
            }
            throw new NullPointerException();
        }
    }

    private static class BundleReference extends SoftReference<ResourceBundle> implements CacheKeyReference {
        private CacheKey cacheKey;

        BundleReference(ResourceBundle referent, ReferenceQueue<Object> q, CacheKey key) {
            super(referent, q);
            this.cacheKey = key;
        }

        public CacheKey getCacheKey() {
            return this.cacheKey;
        }
    }

    private static class LoaderReference extends WeakReference<ClassLoader> implements CacheKeyReference {
        private CacheKey cacheKey;

        LoaderReference(ClassLoader referent, ReferenceQueue<Object> q, CacheKey key) {
            super(referent, q);
            this.cacheKey = key;
        }

        public CacheKey getCacheKey() {
            return this.cacheKey;
        }
    }

    private static final class NoFallbackControl extends SingleFormatControl {
        private static final Control CLASS_ONLY_NO_FALLBACK = new NoFallbackControl(FORMAT_CLASS);
        private static final Control NO_FALLBACK = new NoFallbackControl(FORMAT_DEFAULT);
        private static final Control PROPERTIES_ONLY_NO_FALLBACK = new NoFallbackControl(FORMAT_PROPERTIES);

        protected NoFallbackControl(List<String> formats) {
            super(formats);
        }

        public Locale getFallbackLocale(String baseName, Locale locale) {
            if (baseName != null && locale != null) {
                return null;
            }
            throw new NullPointerException();
        }
    }

    public abstract Enumeration<String> getKeys();

    protected abstract Object handleGetObject(String str);

    public String getBaseBundleName() {
        return this.name;
    }

    public final String getString(String key) {
        return (String) getObject(key);
    }

    public final String[] getStringArray(String key) {
        return (String[]) getObject(key);
    }

    public final Object getObject(String key) {
        Object obj = handleGetObject(key);
        if (obj == null) {
            if (this.parent != null) {
                obj = this.parent.getObject(key);
            }
            if (obj == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Can't find resource for bundle ");
                stringBuilder.append(getClass().getName());
                stringBuilder.append(", key ");
                stringBuilder.append(key);
                throw new MissingResourceException(stringBuilder.toString(), getClass().getName(), key);
            }
        }
        return obj;
    }

    public Locale getLocale() {
        return this.locale;
    }

    private static ClassLoader getLoader(ClassLoader cl) {
        if (cl == null) {
            return RBClassLoader.INSTANCE;
        }
        return cl;
    }

    protected void setParent(ResourceBundle parent) {
        this.parent = parent;
    }

    @CallerSensitive
    public static final ResourceBundle getBundle(String baseName) {
        return getBundleImpl(baseName, Locale.getDefault(), getLoader(VMStack.getCallingClassLoader()), getDefaultControl(baseName));
    }

    @CallerSensitive
    public static final ResourceBundle getBundle(String baseName, Control control) {
        return getBundleImpl(baseName, Locale.getDefault(), getLoader(VMStack.getCallingClassLoader()), control);
    }

    @CallerSensitive
    public static final ResourceBundle getBundle(String baseName, Locale locale) {
        return getBundleImpl(baseName, locale, getLoader(VMStack.getCallingClassLoader()), getDefaultControl(baseName));
    }

    @CallerSensitive
    public static final ResourceBundle getBundle(String baseName, Locale targetLocale, Control control) {
        return getBundleImpl(baseName, targetLocale, getLoader(VMStack.getCallingClassLoader()), control);
    }

    public static ResourceBundle getBundle(String baseName, Locale locale, ClassLoader loader) {
        if (loader != null) {
            return getBundleImpl(baseName, locale, loader, getDefaultControl(baseName));
        }
        throw new NullPointerException();
    }

    public static ResourceBundle getBundle(String baseName, Locale targetLocale, ClassLoader loader, Control control) {
        if (loader != null && control != null) {
            return getBundleImpl(baseName, targetLocale, loader, control);
        }
        throw new NullPointerException();
    }

    private static Control getDefaultControl(String baseName) {
        return Control.INSTANCE;
    }

    private static ResourceBundle getBundleImpl(String baseName, Locale locale, ClassLoader loader, Control control) {
        String str = baseName;
        Locale locale2 = locale;
        Control control2 = control;
        if (locale2 == null || control2 == null) {
            ClassLoader classLoader = loader;
            throw new NullPointerException();
        }
        CacheKey cacheKey = new CacheKey(str, locale2, loader);
        ResourceBundle bundle = null;
        BundleReference bundleRef = (BundleReference) cacheList.get(cacheKey);
        if (bundleRef != null) {
            bundle = (ResourceBundle) bundleRef.get();
            bundleRef = null;
        }
        if (isValidBundle(bundle) && hasValidParentChain(bundle)) {
            return bundle;
        }
        boolean z = (control2 == Control.INSTANCE || (control2 instanceof SingleFormatControl)) ? true : $assertionsDisabled;
        boolean isKnownControl = z;
        List<String> formats = control2.getFormats(str);
        if (isKnownControl || checkList(formats)) {
            ResourceBundle bundle2 = bundle;
            ResourceBundle baseBundle = null;
            Locale targetLocale = locale2;
            while (true) {
                Locale targetLocale2 = targetLocale;
                if (targetLocale2 == null) {
                    break;
                }
                List<Locale> candidateLocales = control2.getCandidateLocales(str, targetLocale2);
                if (isKnownControl || checkList(candidateLocales)) {
                    List<Locale> candidateLocales2 = candidateLocales;
                    Locale targetLocale3 = targetLocale2;
                    bundle = findBundle(cacheKey, candidateLocales, formats, 0, control2, baseBundle);
                    if (isValidBundle(bundle)) {
                        z = Locale.ROOT.equals(bundle.locale);
                        if (!z || bundle.locale.equals(locale2)) {
                            break;
                        }
                        if (candidateLocales2.size() == 1) {
                            if (bundle.locale.equals(candidateLocales2.get(0))) {
                                break;
                            }
                        }
                        if (z && baseBundle == null) {
                            baseBundle = bundle;
                        }
                    }
                    bundle2 = bundle;
                    targetLocale = control2.getFallbackLocale(str, targetLocale3);
                } else {
                    throw new IllegalArgumentException("Invalid Control: getCandidateLocales");
                }
            }
            bundle2 = bundle;
            if (bundle2 == null) {
                if (baseBundle == null) {
                    throwMissingResourceException(str, locale2, cacheKey.getCause());
                }
                bundle2 = baseBundle;
            }
            return bundle2;
        }
        throw new IllegalArgumentException("Invalid Control: getFormats");
    }

    private static boolean checkList(List<?> a) {
        boolean valid = (a == null || a.isEmpty()) ? $assertionsDisabled : true;
        if (!valid) {
            return valid;
        }
        int size = a.size();
        boolean valid2 = valid;
        int i = 0;
        while (valid2 && i < size) {
            valid2 = a.get(i) != null;
            i++;
        }
        return valid2;
    }

    private static ResourceBundle findBundle(CacheKey cacheKey, List<Locale> candidateLocales, List<String> formats, int index, Control control, ResourceBundle baseBundle) {
        ResourceBundle resourceBundle;
        Locale targetLocale = (Locale) candidateLocales.get(index);
        ResourceBundle parent = null;
        if (index != candidateLocales.size() - 1) {
            resourceBundle = index + 1;
            parent = findBundle(cacheKey, candidateLocales, formats, resourceBundle, control, baseBundle);
        } else if (baseBundle != null && Locale.ROOT.equals(targetLocale)) {
            return baseBundle;
        }
        while (true) {
            Reference poll = referenceQueue.poll();
            Reference ref = poll;
            if (poll == null) {
                break;
            }
            cacheList.remove(((CacheKeyReference) ref).getCacheKey());
        }
        boolean expiredBundle = $assertionsDisabled;
        cacheKey.setLocale(targetLocale);
        ResourceBundle bundle = findBundleInCache(cacheKey, control);
        if (isValidBundle(bundle)) {
            expiredBundle = bundle.expired;
            if (!expiredBundle) {
                if (bundle.parent == parent) {
                    return bundle;
                }
                BundleReference bundleRef = (BundleReference) cacheList.get(cacheKey);
                if (bundleRef != null) {
                    resourceBundle = bundleRef.get();
                    if (resourceBundle == bundle) {
                        resourceBundle = cacheList;
                        resourceBundle.remove(cacheKey, bundleRef);
                    }
                }
            }
        }
        if (bundle != NONEXISTENT_BUNDLE) {
            CacheKey constKey = (CacheKey) cacheKey.clone();
            try {
                resourceBundle = loadBundle(cacheKey, formats, control, expiredBundle);
                bundle = resourceBundle;
                if (bundle != null) {
                    if (bundle.parent == null) {
                        bundle.setParent(parent);
                    }
                    bundle.locale = targetLocale;
                    resourceBundle = putBundleInCache(cacheKey, bundle, control);
                    bundle = resourceBundle;
                    return bundle;
                }
                resourceBundle = NONEXISTENT_BUNDLE;
                putBundleInCache(cacheKey, resourceBundle, control);
                if (constKey.getCause() instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            } finally {
                ResourceBundle resourceBundle2 = resourceBundle;
                if (constKey.getCause() instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return parent;
    }

    private static ResourceBundle loadBundle(CacheKey cacheKey, List<String> formats, Control control, boolean reload) {
        Locale targetLocale = cacheKey.getLocale();
        int size = formats.size();
        ResourceBundle bundle = null;
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= size) {
                break;
            }
            String format = (String) formats.get(i2);
            try {
                bundle = control.newBundle(cacheKey.getName(), targetLocale, format, cacheKey.getLoader(), reload);
            } catch (LinkageError error) {
                cacheKey.setCause(error);
            } catch (Exception cause) {
                cacheKey.setCause(cause);
            }
            if (bundle != null) {
                cacheKey.setFormat(format);
                bundle.name = cacheKey.getName();
                bundle.locale = targetLocale;
                bundle.expired = $assertionsDisabled;
                break;
            }
            i = i2 + 1;
        }
        return bundle;
    }

    private static boolean isValidBundle(ResourceBundle bundle) {
        return (bundle == null || bundle == NONEXISTENT_BUNDLE) ? $assertionsDisabled : true;
    }

    private static boolean hasValidParentChain(ResourceBundle bundle) {
        long now = System.currentTimeMillis();
        while (bundle != null) {
            if (bundle.expired) {
                return $assertionsDisabled;
            }
            CacheKey key = bundle.cacheKey;
            if (key != null) {
                long expirationTime = key.expirationTime;
                if (expirationTime >= 0 && expirationTime <= now) {
                    return $assertionsDisabled;
                }
            }
            bundle = bundle.parent;
        }
        return true;
    }

    private static void throwMissingResourceException(String baseName, Locale locale, Throwable cause) {
        if (cause instanceof MissingResourceException) {
            cause = null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Can't find bundle for base name ");
        stringBuilder.append(baseName);
        stringBuilder.append(", locale ");
        stringBuilder.append((Object) locale);
        String stringBuilder2 = stringBuilder.toString();
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append(baseName);
        stringBuilder3.append(BaseLocale.SEP);
        stringBuilder3.append((Object) locale);
        throw new MissingResourceException(stringBuilder2, stringBuilder3.toString(), "", cause);
    }

    private static ResourceBundle findBundleInCache(CacheKey cacheKey, Control control) {
        Throwable th;
        Control control2;
        CacheKey cacheKey2 = cacheKey;
        BundleReference bundleRef = (BundleReference) cacheList.get(cacheKey2);
        if (bundleRef == null) {
            return null;
        }
        ResourceBundle bundle = (ResourceBundle) bundleRef.get();
        if (bundle == null) {
            return null;
        }
        ResourceBundle p = bundle.parent;
        if (p == null || !p.expired) {
            CacheKey key = bundleRef.getCacheKey();
            long expirationTime = key.expirationTime;
            if (!bundle.expired && expirationTime >= 0 && expirationTime <= System.currentTimeMillis()) {
                if (bundle != NONEXISTENT_BUNDLE) {
                    synchronized (bundle) {
                        try {
                            long expirationTime2 = key.expirationTime;
                            try {
                                if (!bundle.expired && expirationTime2 >= 0 && expirationTime2 <= System.currentTimeMillis()) {
                                    bundle.expired = control.needsReload(key.getName(), key.getLocale(), key.getFormat(), key.getLoader(), bundle, key.loadTime);
                                    if (bundle.expired) {
                                        bundle.cacheKey = null;
                                        cacheList.remove(cacheKey2, bundleRef);
                                    } else {
                                        try {
                                            setExpirationTime(key, control);
                                        } catch (Throwable th2) {
                                            th = th2;
                                            while (true) {
                                                try {
                                                    break;
                                                } catch (Throwable th3) {
                                                    th = th3;
                                                }
                                            }
                                            throw th;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                cacheKey2.setCause(e);
                            } catch (Throwable th4) {
                                th = th4;
                                control2 = control;
                                while (true) {
                                    break;
                                }
                                throw th;
                            }
                            control2 = control;
                        } catch (Throwable th5) {
                            th = th5;
                            control2 = control;
                            while (true) {
                                break;
                            }
                            throw th;
                        }
                    }
                }
                control2 = control;
                cacheList.remove(cacheKey2, bundleRef);
                bundle = null;
                return bundle;
            }
        }
        bundle.expired = true;
        bundle.cacheKey = null;
        cacheList.remove(cacheKey2, bundleRef);
        bundle = null;
        control2 = control;
        return bundle;
    }

    private static ResourceBundle putBundleInCache(CacheKey cacheKey, ResourceBundle bundle, Control control) {
        setExpirationTime(cacheKey, control);
        if (cacheKey.expirationTime == -1) {
            return bundle;
        }
        CacheKey key = (CacheKey) cacheKey.clone();
        BundleReference bundleRef = new BundleReference(bundle, referenceQueue, key);
        bundle.cacheKey = key;
        BundleReference result = (BundleReference) cacheList.putIfAbsent(key, bundleRef);
        if (result == null) {
            return bundle;
        }
        ResourceBundle rb = (ResourceBundle) result.get();
        if (rb == null || rb.expired) {
            cacheList.put(key, bundleRef);
            return bundle;
        }
        bundle.cacheKey = null;
        bundle = rb;
        bundleRef.clear();
        return bundle;
    }

    private static void setExpirationTime(CacheKey cacheKey, Control control) {
        long ttl = control.getTimeToLive(cacheKey.getName(), cacheKey.getLocale());
        if (ttl >= 0) {
            long now = System.currentTimeMillis();
            cacheKey.loadTime = now;
            cacheKey.expirationTime = now + ttl;
        } else if (ttl >= -2) {
            cacheKey.expirationTime = ttl;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid Control: TTL=");
            stringBuilder.append(ttl);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    @CallerSensitive
    public static final void clearCache() {
        clearCache(getLoader(VMStack.getCallingClassLoader()));
    }

    public static final void clearCache(ClassLoader loader) {
        if (loader != null) {
            Set<CacheKey> set = cacheList.keySet();
            for (CacheKey key : set) {
                if (key.getLoader() == loader) {
                    set.remove(key);
                }
            }
            return;
        }
        throw new NullPointerException();
    }

    public boolean containsKey(String key) {
        if (key != null) {
            for (ResourceBundle rb = this; rb != null; rb = rb.parent) {
                if (rb.handleKeySet().contains(key)) {
                    return true;
                }
            }
            return $assertionsDisabled;
        }
        throw new NullPointerException();
    }

    public Set<String> keySet() {
        Set<String> keys = new HashSet();
        for (ResourceBundle rb = this; rb != null; rb = rb.parent) {
            keys.addAll(rb.handleKeySet());
        }
        return keys;
    }

    protected Set<String> handleKeySet() {
        if (this.keySet == null) {
            synchronized (this) {
                if (this.keySet == null) {
                    Set<String> keys = new HashSet();
                    Enumeration<String> enumKeys = getKeys();
                    while (enumKeys.hasMoreElements()) {
                        String key = (String) enumKeys.nextElement();
                        if (handleGetObject(key) != null) {
                            keys.add(key);
                        }
                    }
                    this.keySet = keys;
                }
            }
        }
        return this.keySet;
    }
}
