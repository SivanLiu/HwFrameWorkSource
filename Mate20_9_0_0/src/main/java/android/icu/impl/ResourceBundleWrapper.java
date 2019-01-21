package android.icu.impl;

import android.icu.impl.locale.BaseLocale;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public final class ResourceBundleWrapper extends UResourceBundle {
    private static CacheBase<String, ResourceBundleWrapper, Loader> BUNDLE_CACHE = new SoftCache<String, ResourceBundleWrapper, Loader>() {
        protected ResourceBundleWrapper createInstance(String unusedKey, Loader loader) {
            return loader.load();
        }
    };
    private static final boolean DEBUG = ICUDebug.enabled("resourceBundleWrapper");
    private String baseName;
    private ResourceBundle bundle;
    private List<String> keys;
    private String localeID;

    private static abstract class Loader {
        abstract ResourceBundleWrapper load();

        private Loader() {
        }

        /* synthetic */ Loader(AnonymousClass1 x0) {
            this();
        }
    }

    /* synthetic */ ResourceBundleWrapper(ResourceBundle x0, AnonymousClass1 x1) {
        this(x0);
    }

    private ResourceBundleWrapper(ResourceBundle bundle) {
        this.bundle = null;
        this.localeID = null;
        this.baseName = null;
        this.keys = null;
        this.bundle = bundle;
    }

    protected Object handleGetObject(String aKey) {
        ResourceBundleWrapper current = this;
        Object obj = null;
        while (current != null) {
            try {
                obj = current.bundle.getObject(aKey);
                break;
            } catch (MissingResourceException e) {
                current = (ResourceBundleWrapper) current.getParent();
            }
        }
        if (obj != null) {
            return obj;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Can't find resource for bundle ");
        stringBuilder.append(this.baseName);
        stringBuilder.append(", key ");
        stringBuilder.append(aKey);
        throw new MissingResourceException(stringBuilder.toString(), getClass().getName(), aKey);
    }

    public Enumeration<String> getKeys() {
        return Collections.enumeration(this.keys);
    }

    private void initKeysVector() {
        this.keys = new ArrayList();
        for (ResourceBundleWrapper current = this; current != null; current = (ResourceBundleWrapper) current.getParent()) {
            Enumeration<String> e = current.bundle.getKeys();
            while (e.hasMoreElements()) {
                String elem = (String) e.nextElement();
                if (!this.keys.contains(elem)) {
                    this.keys.add(elem);
                }
            }
        }
    }

    protected String getLocaleID() {
        return this.localeID;
    }

    protected String getBaseName() {
        return this.bundle.getClass().getName().replace('.', '/');
    }

    public ULocale getULocale() {
        return new ULocale(this.localeID);
    }

    public UResourceBundle getParent() {
        return (UResourceBundle) this.parent;
    }

    public static ResourceBundleWrapper getBundleInstance(String baseName, String localeID, ClassLoader root, boolean disableFallback) {
        ResourceBundleWrapper b;
        if (root == null) {
            root = ClassLoaderUtil.getClassLoader();
        }
        if (disableFallback) {
            b = instantiateBundle(baseName, localeID, null, root, disableFallback);
        } else {
            b = instantiateBundle(baseName, localeID, ULocale.getDefault().getBaseName(), root, disableFallback);
        }
        if (b != null) {
            return b;
        }
        String separator = BaseLocale.SEP;
        if (baseName.indexOf(47) >= 0) {
            separator = "/";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Could not find the bundle ");
        stringBuilder.append(baseName);
        stringBuilder.append(separator);
        stringBuilder.append(localeID);
        throw new MissingResourceException(stringBuilder.toString(), "", "");
    }

    private static boolean localeIDStartsWithLangSubtag(String localeID, String lang) {
        return localeID.startsWith(lang) && (localeID.length() == lang.length() || localeID.charAt(lang.length()) == '_');
    }

    private static ResourceBundleWrapper instantiateBundle(String baseName, String localeID, String defaultID, ClassLoader root, boolean disableFallback) {
        String name;
        String str;
        if (localeID.isEmpty()) {
            name = baseName;
        } else {
            name = new StringBuilder();
            name.append(baseName);
            name.append('_');
            name.append(localeID);
            name = name.toString();
        }
        if (disableFallback) {
            str = name;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(name);
            stringBuilder.append('#');
            stringBuilder.append(defaultID);
            str = stringBuilder.toString();
        }
        final String str2 = localeID;
        final String str3 = baseName;
        final String str4 = defaultID;
        final ClassLoader classLoader = root;
        final boolean z = disableFallback;
        final String str5 = name;
        return (ResourceBundleWrapper) BUNDLE_CACHE.getInstance(str, new Loader() {
            public ResourceBundleWrapper load() {
                ResourceBundleWrapper parent = null;
                int i = str2.lastIndexOf(95);
                boolean loadFromProperties = false;
                boolean parentIsRoot = false;
                if (i != -1) {
                    parent = ResourceBundleWrapper.instantiateBundle(str3, str2.substring(0, i), str4, classLoader, z);
                } else if (!str2.isEmpty()) {
                    parent = ResourceBundleWrapper.instantiateBundle(str3, "", str4, classLoader, z);
                    parentIsRoot = true;
                }
                ResourceBundleWrapper b = null;
                try {
                    b = new ResourceBundleWrapper((ResourceBundle) classLoader.loadClass(str5).asSubclass(ResourceBundle.class).newInstance(), null);
                    if (parent != null) {
                        b.setParent(parent);
                    }
                    b.baseName = str3;
                    b.localeID = str2;
                } catch (ClassNotFoundException e) {
                    loadFromProperties = true;
                } catch (NoClassDefFoundError e2) {
                    loadFromProperties = true;
                } catch (Exception e3) {
                    if (ResourceBundleWrapper.DEBUG) {
                        System.out.println("failure");
                    }
                    if (ResourceBundleWrapper.DEBUG) {
                        System.out.println(e3);
                    }
                }
                if (loadFromProperties) {
                    InputStream stream;
                    try {
                        String resName = new StringBuilder();
                        resName.append(str5.replace('.', '/'));
                        resName.append(".properties");
                        resName = resName.toString();
                        stream = (InputStream) AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
                            public InputStream run() {
                                return classLoader.getResourceAsStream(resName);
                            }
                        });
                        if (stream != null) {
                            stream = new BufferedInputStream(stream);
                            b = new ResourceBundleWrapper(new PropertyResourceBundle(stream), null);
                            if (parent != null) {
                                b.setParent(parent);
                            }
                            b.baseName = str3;
                            b.localeID = str2;
                            try {
                                stream.close();
                            } catch (Exception e4) {
                            }
                        }
                    } catch (Exception e5) {
                        stream.close();
                    } catch (Exception e6) {
                        if (ResourceBundleWrapper.DEBUG) {
                            System.out.println("failure");
                        }
                        if (ResourceBundleWrapper.DEBUG) {
                            System.out.println(e6);
                        }
                    } catch (Throwable th) {
                        try {
                            stream.close();
                        } catch (Exception e7) {
                        }
                    }
                    if (b == null) {
                        if (!(z || str2.isEmpty() || str2.indexOf(95) >= 0 || ResourceBundleWrapper.localeIDStartsWithLangSubtag(str4, str2))) {
                            b = ResourceBundleWrapper.instantiateBundle(str3, str4, str4, classLoader, z);
                        }
                    }
                    if (b == null && !(parentIsRoot && z)) {
                        b = parent;
                    }
                }
                if (b != null) {
                    b.initKeysVector();
                } else if (ResourceBundleWrapper.DEBUG) {
                    PrintStream printStream = System.out;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Returning null for ");
                    stringBuilder.append(str3);
                    stringBuilder.append(BaseLocale.SEP);
                    stringBuilder.append(str2);
                    printStream.println(stringBuilder.toString());
                }
                return b;
            }
        });
    }
}
