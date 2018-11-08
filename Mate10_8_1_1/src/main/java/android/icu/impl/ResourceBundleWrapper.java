package android.icu.impl;

import android.icu.impl.locale.BaseLocale;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.io.BufferedInputStream;
import java.io.InputStream;
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
        throw new MissingResourceException("Can't find resource for bundle " + this.baseName + ", key " + aKey, getClass().getName(), aKey);
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
        throw new MissingResourceException("Could not find the bundle " + baseName + separator + localeID, "", "");
    }

    private static boolean localeIDStartsWithLangSubtag(String localeID, String lang) {
        if (localeID.startsWith(lang)) {
            return localeID.length() == lang.length() || localeID.charAt(lang.length()) == '_';
        } else {
            return false;
        }
    }

    private static ResourceBundleWrapper instantiateBundle(String baseName, String localeID, String defaultID, ClassLoader root, boolean disableFallback) {
        final String name = localeID.isEmpty() ? baseName : baseName + '_' + localeID;
        final String str = localeID;
        final String str2 = baseName;
        final String str3 = defaultID;
        final ClassLoader classLoader = root;
        final boolean z = disableFallback;
        return (ResourceBundleWrapper) BUNDLE_CACHE.getInstance(disableFallback ? name : name + '#' + defaultID, new Loader() {
            public ResourceBundleWrapper load() {
                ResourceBundleWrapper b;
                String resName;
                final ClassLoader classLoader;
                final String str;
                InputStream stream;
                InputStream bufferedInputStream;
                Exception e;
                Throwable th;
                ResourceBundle parent = null;
                int i = str.lastIndexOf(95);
                boolean loadFromProperties = false;
                boolean parentIsRoot = false;
                if (i != -1) {
                    String locName = str.substring(0, i);
                    parent = ResourceBundleWrapper.instantiateBundle(str2, locName, str3, classLoader, z);
                } else if (!str.isEmpty()) {
                    parent = ResourceBundleWrapper.instantiateBundle(str2, "", str3, classLoader, z);
                    parentIsRoot = true;
                }
                ResourceBundleWrapper b2 = null;
                try {
                    b = new ResourceBundleWrapper((ResourceBundle) classLoader.loadClass(name).asSubclass(ResourceBundle.class).newInstance());
                    if (parent != null) {
                        try {
                            b.setParent(parent);
                        } catch (ClassNotFoundException e2) {
                            b2 = b;
                            loadFromProperties = true;
                            b = b2;
                            if (loadFromProperties) {
                                try {
                                    resName = name.replace('.', '/') + ".properties";
                                    classLoader = classLoader;
                                    str = resName;
                                    stream = (InputStream) AccessController.doPrivileged(new PrivilegedAction<InputStream>() {
                                        public InputStream run() {
                                            return classLoader.getResourceAsStream(str);
                                        }
                                    });
                                    if (stream != null) {
                                        bufferedInputStream = new BufferedInputStream(stream);
                                        try {
                                            b2 = new ResourceBundleWrapper(new PropertyResourceBundle(bufferedInputStream));
                                            if (parent != null) {
                                                try {
                                                    b2.setParent(parent);
                                                } catch (Exception e3) {
                                                    try {
                                                        bufferedInputStream.close();
                                                    } catch (Exception e4) {
                                                    }
                                                    stream = bufferedInputStream;
                                                    if (b2 == null) {
                                                        try {
                                                            b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                                        } catch (Exception e5) {
                                                            e = e5;
                                                        }
                                                    }
                                                    b2 = parent;
                                                    if (b2 != null) {
                                                        b2.initKeysVector();
                                                    } else if (ResourceBundleWrapper.DEBUG) {
                                                        System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                                                    }
                                                    return b2;
                                                } catch (Throwable th2) {
                                                    th = th2;
                                                    try {
                                                        bufferedInputStream.close();
                                                    } catch (Exception e6) {
                                                    }
                                                    throw th;
                                                }
                                            }
                                            b2.baseName = str2;
                                            b2.localeID = str;
                                            try {
                                                bufferedInputStream.close();
                                            } catch (Exception e7) {
                                            }
                                        } catch (Exception e8) {
                                            b2 = b;
                                            bufferedInputStream.close();
                                            stream = bufferedInputStream;
                                            if (b2 == null) {
                                                b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                            }
                                            b2 = parent;
                                            if (b2 != null) {
                                                b2.initKeysVector();
                                            } else if (ResourceBundleWrapper.DEBUG) {
                                                System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                                            }
                                            return b2;
                                        } catch (Throwable th3) {
                                            th = th3;
                                            b2 = b;
                                            bufferedInputStream.close();
                                            throw th;
                                        }
                                        stream = bufferedInputStream;
                                    } else {
                                        b2 = b;
                                    }
                                    if (b2 == null) {
                                        b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                    }
                                    b2 = parent;
                                } catch (Exception e9) {
                                    e = e9;
                                    b2 = b;
                                    if (ResourceBundleWrapper.DEBUG) {
                                        System.out.println("failure");
                                    }
                                    if (ResourceBundleWrapper.DEBUG) {
                                        System.out.println(e);
                                    }
                                    if (b2 != null) {
                                        b2.initKeysVector();
                                    } else if (ResourceBundleWrapper.DEBUG) {
                                        System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                                    }
                                    return b2;
                                }
                            }
                            b2 = b;
                            if (b2 != null) {
                                b2.initKeysVector();
                            } else if (ResourceBundleWrapper.DEBUG) {
                                System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                            }
                            return b2;
                        } catch (NoClassDefFoundError e10) {
                            b2 = b;
                            loadFromProperties = true;
                            b = b2;
                            if (loadFromProperties) {
                                b2 = b;
                            } else {
                                resName = name.replace('.', '/') + ".properties";
                                classLoader = classLoader;
                                str = resName;
                                stream = (InputStream) AccessController.doPrivileged(/* anonymous class already generated */);
                                if (stream != null) {
                                    b2 = b;
                                } else {
                                    bufferedInputStream = new BufferedInputStream(stream);
                                    b2 = new ResourceBundleWrapper(new PropertyResourceBundle(bufferedInputStream));
                                    if (parent != null) {
                                        b2.setParent(parent);
                                    }
                                    b2.baseName = str2;
                                    b2.localeID = str;
                                    bufferedInputStream.close();
                                    stream = bufferedInputStream;
                                }
                                if (b2 == null) {
                                    b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                }
                                b2 = parent;
                            }
                            if (b2 != null) {
                                b2.initKeysVector();
                            } else if (ResourceBundleWrapper.DEBUG) {
                                System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                            }
                            return b2;
                        } catch (Exception e11) {
                            e = e11;
                            b2 = b;
                            if (ResourceBundleWrapper.DEBUG) {
                                System.out.println("failure");
                            }
                            if (ResourceBundleWrapper.DEBUG) {
                                System.out.println(e);
                                b = b2;
                            } else {
                                b = b2;
                            }
                            if (loadFromProperties) {
                                resName = name.replace('.', '/') + ".properties";
                                classLoader = classLoader;
                                str = resName;
                                stream = (InputStream) AccessController.doPrivileged(/* anonymous class already generated */);
                                if (stream != null) {
                                    bufferedInputStream = new BufferedInputStream(stream);
                                    b2 = new ResourceBundleWrapper(new PropertyResourceBundle(bufferedInputStream));
                                    if (parent != null) {
                                        b2.setParent(parent);
                                    }
                                    b2.baseName = str2;
                                    b2.localeID = str;
                                    bufferedInputStream.close();
                                    stream = bufferedInputStream;
                                } else {
                                    b2 = b;
                                }
                                if (b2 == null) {
                                    b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                                }
                                b2 = parent;
                            } else {
                                b2 = b;
                            }
                            if (b2 != null) {
                                b2.initKeysVector();
                            } else if (ResourceBundleWrapper.DEBUG) {
                                System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                            }
                            return b2;
                        }
                    }
                    b.baseName = str2;
                    b.localeID = str;
                } catch (ClassNotFoundException e12) {
                    loadFromProperties = true;
                    b = b2;
                    if (loadFromProperties) {
                        b2 = b;
                    } else {
                        resName = name.replace('.', '/') + ".properties";
                        classLoader = classLoader;
                        str = resName;
                        stream = (InputStream) AccessController.doPrivileged(/* anonymous class already generated */);
                        if (stream != null) {
                            b2 = b;
                        } else {
                            bufferedInputStream = new BufferedInputStream(stream);
                            b2 = new ResourceBundleWrapper(new PropertyResourceBundle(bufferedInputStream));
                            if (parent != null) {
                                b2.setParent(parent);
                            }
                            b2.baseName = str2;
                            b2.localeID = str;
                            bufferedInputStream.close();
                            stream = bufferedInputStream;
                        }
                        if (b2 == null) {
                            b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                        }
                        b2 = parent;
                    }
                    if (b2 != null) {
                        b2.initKeysVector();
                    } else if (ResourceBundleWrapper.DEBUG) {
                        System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                    }
                    return b2;
                } catch (NoClassDefFoundError e13) {
                    loadFromProperties = true;
                    b = b2;
                    if (loadFromProperties) {
                        resName = name.replace('.', '/') + ".properties";
                        classLoader = classLoader;
                        str = resName;
                        stream = (InputStream) AccessController.doPrivileged(/* anonymous class already generated */);
                        if (stream != null) {
                            bufferedInputStream = new BufferedInputStream(stream);
                            b2 = new ResourceBundleWrapper(new PropertyResourceBundle(bufferedInputStream));
                            if (parent != null) {
                                b2.setParent(parent);
                            }
                            b2.baseName = str2;
                            b2.localeID = str;
                            bufferedInputStream.close();
                            stream = bufferedInputStream;
                        } else {
                            b2 = b;
                        }
                        if (b2 == null) {
                            b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                        }
                        b2 = parent;
                    } else {
                        b2 = b;
                    }
                    if (b2 != null) {
                        b2.initKeysVector();
                    } else if (ResourceBundleWrapper.DEBUG) {
                        System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                    }
                    return b2;
                } catch (Exception e14) {
                    e = e14;
                    if (ResourceBundleWrapper.DEBUG) {
                        System.out.println("failure");
                    }
                    if (ResourceBundleWrapper.DEBUG) {
                        System.out.println(e);
                        b = b2;
                    } else {
                        b = b2;
                    }
                    if (loadFromProperties) {
                        b2 = b;
                    } else {
                        resName = name.replace('.', '/') + ".properties";
                        classLoader = classLoader;
                        str = resName;
                        stream = (InputStream) AccessController.doPrivileged(/* anonymous class already generated */);
                        if (stream != null) {
                            b2 = b;
                        } else {
                            bufferedInputStream = new BufferedInputStream(stream);
                            b2 = new ResourceBundleWrapper(new PropertyResourceBundle(bufferedInputStream));
                            if (parent != null) {
                                b2.setParent(parent);
                            }
                            b2.baseName = str2;
                            b2.localeID = str;
                            bufferedInputStream.close();
                            stream = bufferedInputStream;
                        }
                        if (b2 == null) {
                            b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                        }
                        b2 = parent;
                    }
                    if (b2 != null) {
                        b2.initKeysVector();
                    } else if (ResourceBundleWrapper.DEBUG) {
                        System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                    }
                    return b2;
                }
                if (loadFromProperties) {
                    resName = name.replace('.', '/') + ".properties";
                    classLoader = classLoader;
                    str = resName;
                    stream = (InputStream) AccessController.doPrivileged(/* anonymous class already generated */);
                    if (stream != null) {
                        bufferedInputStream = new BufferedInputStream(stream);
                        b2 = new ResourceBundleWrapper(new PropertyResourceBundle(bufferedInputStream));
                        if (parent != null) {
                            b2.setParent(parent);
                        }
                        b2.baseName = str2;
                        b2.localeID = str;
                        bufferedInputStream.close();
                        stream = bufferedInputStream;
                    } else {
                        b2 = b;
                    }
                    if (b2 == null) {
                        if (!((z ^ 1) == 0 || (str.isEmpty() ^ 1) == 0 || str.indexOf(95) >= 0 || (ResourceBundleWrapper.localeIDStartsWithLangSubtag(str3, str) ^ 1) == 0)) {
                            b2 = ResourceBundleWrapper.instantiateBundle(str2, str3, str3, classLoader, z);
                        }
                    }
                    if (b2 == null && !(parentIsRoot && (z ^ 1) == 0)) {
                        b2 = parent;
                    }
                } else {
                    b2 = b;
                }
                if (b2 != null) {
                    b2.initKeysVector();
                } else if (ResourceBundleWrapper.DEBUG) {
                    System.out.println("Returning null for " + str2 + BaseLocale.SEP + str);
                }
                return b2;
            }
        });
    }
}
