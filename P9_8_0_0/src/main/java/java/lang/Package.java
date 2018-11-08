package java.lang;

import dalvik.system.VMRuntime;
import dalvik.system.VMStack;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import sun.net.www.ParseUtil;
import sun.reflect.CallerSensitive;

public class Package implements AnnotatedElement {
    private static Map<String, Manifest> mans = new HashMap(10);
    private static Map<String, Package> pkgs = new HashMap(31);
    private static Map<String, URL> urls = new HashMap(10);
    private final String implTitle;
    private final String implVendor;
    private final String implVersion;
    private final transient ClassLoader loader;
    private transient Class<?> packageInfo;
    private final String pkgName;
    private final URL sealBase;
    private final String specTitle;
    private final String specVendor;
    private final String specVersion;

    private static native String getSystemPackage0(String str);

    private static native String[] getSystemPackages0();

    public String getName() {
        return this.pkgName;
    }

    public String getSpecificationTitle() {
        return this.specTitle;
    }

    public String getSpecificationVersion() {
        return this.specVersion;
    }

    public String getSpecificationVendor() {
        return this.specVendor;
    }

    public String getImplementationTitle() {
        return this.implTitle;
    }

    public String getImplementationVersion() {
        return this.implVersion;
    }

    public String getImplementationVendor() {
        return this.implVendor;
    }

    public boolean isSealed() {
        return this.sealBase != null;
    }

    public boolean isSealed(URL url) {
        return url.equals(this.sealBase);
    }

    public boolean isCompatibleWith(String desired) throws NumberFormatException {
        if (this.specVersion == null || this.specVersion.length() < 1) {
            throw new NumberFormatException("Empty version string");
        }
        int i;
        String[] sa = this.specVersion.split("\\.", -1);
        int[] si = new int[sa.length];
        for (i = 0; i < sa.length; i++) {
            si[i] = Integer.parseInt(sa[i]);
            if (si[i] < 0) {
                throw NumberFormatException.forInputString("" + si[i]);
            }
        }
        String[] da = desired.split("\\.", -1);
        int[] di = new int[da.length];
        for (i = 0; i < da.length; i++) {
            di[i] = Integer.parseInt(da[i]);
            if (di[i] < 0) {
                throw NumberFormatException.forInputString("" + di[i]);
            }
        }
        int len = Math.max(di.length, si.length);
        i = 0;
        while (i < len) {
            int d = i < di.length ? di[i] : 0;
            int s = i < si.length ? si[i] : 0;
            if (s < d) {
                return false;
            }
            if (s > d) {
                return true;
            }
            i++;
        }
        return true;
    }

    @CallerSensitive
    public static Package getPackage(String name) {
        ClassLoader l = VMStack.getCallingClassLoader();
        if (l != null) {
            return l.getPackage(name);
        }
        return getSystemPackage(name);
    }

    @CallerSensitive
    public static Package[] getPackages() {
        ClassLoader l = VMStack.getCallingClassLoader();
        if (l != null) {
            return l.getPackages();
        }
        return getSystemPackages();
    }

    static Package getPackage(Class<?> c) {
        String name = c.getName();
        int i = name.lastIndexOf(46);
        if (i == -1) {
            return null;
        }
        name = name.substring(0, i);
        ClassLoader cl = c.getClassLoader();
        if (cl != null) {
            return cl.getPackage(name);
        }
        return getSystemPackage(name);
    }

    public int hashCode() {
        return this.pkgName.hashCode();
    }

    public String toString() {
        int targetSdkVersion = VMRuntime.getRuntime().getTargetSdkVersion();
        if (targetSdkVersion > 0 && targetSdkVersion <= 24) {
            return "package " + this.pkgName;
        }
        String spec = this.specTitle;
        String ver = this.specVersion;
        if (spec == null || spec.length() <= 0) {
            spec = "";
        } else {
            spec = ", " + spec;
        }
        if (ver == null || ver.length() <= 0) {
            ver = "";
        } else {
            ver = ", version " + ver;
        }
        return "package " + this.pkgName + spec + ver;
    }

    private Class<?> getPackageInfo() {
        if (this.packageInfo == null) {
            try {
                this.packageInfo = Class.forName(this.pkgName + ".package-info", false, this.loader);
            } catch (ClassNotFoundException e) {
                this.packageInfo = AnonymousClass1PackageInfoProxy.class;
            }
        }
        return this.packageInfo;
    }

    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return getPackageInfo().getAnnotation(annotationClass);
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return super.isAnnotationPresent(annotationClass);
    }

    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
        return getPackageInfo().getAnnotationsByType(annotationClass);
    }

    public Annotation[] getAnnotations() {
        return getPackageInfo().getAnnotations();
    }

    public <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationClass) {
        return getPackageInfo().getDeclaredAnnotation(annotationClass);
    }

    public <A extends Annotation> A[] getDeclaredAnnotationsByType(Class<A> annotationClass) {
        return getPackageInfo().getDeclaredAnnotationsByType(annotationClass);
    }

    public Annotation[] getDeclaredAnnotations() {
        return getPackageInfo().getDeclaredAnnotations();
    }

    Package(String name, String spectitle, String specversion, String specvendor, String impltitle, String implversion, String implvendor, URL sealbase, ClassLoader loader) {
        this.pkgName = name;
        this.implTitle = impltitle;
        this.implVersion = implversion;
        this.implVendor = implvendor;
        this.specTitle = spectitle;
        this.specVersion = specversion;
        this.specVendor = specvendor;
        this.sealBase = sealbase;
        this.loader = loader;
    }

    private Package(String name, Manifest man, URL url, ClassLoader loader) {
        String sealed = null;
        String str = null;
        String str2 = null;
        String str3 = null;
        String str4 = null;
        String str5 = null;
        String str6 = null;
        URL sealBase = null;
        Attributes attr = man.getAttributes(name.replace('.', '/').concat("/"));
        if (attr != null) {
            str = attr.getValue(Name.SPECIFICATION_TITLE);
            str2 = attr.getValue(Name.SPECIFICATION_VERSION);
            str3 = attr.getValue(Name.SPECIFICATION_VENDOR);
            str4 = attr.getValue(Name.IMPLEMENTATION_TITLE);
            str5 = attr.getValue(Name.IMPLEMENTATION_VERSION);
            str6 = attr.getValue(Name.IMPLEMENTATION_VENDOR);
            sealed = attr.getValue(Name.SEALED);
        }
        attr = man.getMainAttributes();
        if (attr != null) {
            if (str == null) {
                str = attr.getValue(Name.SPECIFICATION_TITLE);
            }
            if (str2 == null) {
                str2 = attr.getValue(Name.SPECIFICATION_VERSION);
            }
            if (str3 == null) {
                str3 = attr.getValue(Name.SPECIFICATION_VENDOR);
            }
            if (str4 == null) {
                str4 = attr.getValue(Name.IMPLEMENTATION_TITLE);
            }
            if (str5 == null) {
                str5 = attr.getValue(Name.IMPLEMENTATION_VERSION);
            }
            if (str6 == null) {
                str6 = attr.getValue(Name.IMPLEMENTATION_VENDOR);
            }
            if (sealed == null) {
                sealed = attr.getValue(Name.SEALED);
            }
        }
        if ("true".equalsIgnoreCase(sealed)) {
            sealBase = url;
        }
        this.pkgName = name;
        this.specTitle = str;
        this.specVersion = str2;
        this.specVendor = str3;
        this.implTitle = str4;
        this.implVersion = str5;
        this.implVendor = str6;
        this.sealBase = sealBase;
        this.loader = loader;
    }

    static Package getSystemPackage(String name) {
        Package pkg;
        synchronized (pkgs) {
            pkg = (Package) pkgs.get(name);
            if (pkg == null) {
                name = name.replace('.', '/').concat("/");
                String fn = getSystemPackage0(name);
                if (fn != null) {
                    pkg = defineSystemPackage(name, fn);
                }
            }
        }
        return pkg;
    }

    static Package[] getSystemPackages() {
        Package[] packageArr;
        String[] names = getSystemPackages0();
        synchronized (pkgs) {
            for (int i = 0; i < names.length; i++) {
                defineSystemPackage(names[i], getSystemPackage0(names[i]));
            }
            packageArr = (Package[]) pkgs.values().toArray(new Package[pkgs.size()]);
        }
        return packageArr;
    }

    private static Package defineSystemPackage(final String iname, final String fn) {
        return (Package) AccessController.doPrivileged(new PrivilegedAction<Package>() {
            public Package run() {
                Package pkg;
                String name = iname;
                URL url = (URL) Package.urls.get(fn);
                if (url == null) {
                    File file = new File(fn);
                    try {
                        url = ParseUtil.fileToEncodedURL(file);
                    } catch (MalformedURLException e) {
                    }
                    if (url != null) {
                        Package.urls.put(fn, url);
                        if (file.isFile()) {
                            Package.mans.put(fn, Package.loadManifest(fn));
                        }
                    }
                }
                name = name.substring(0, name.length() - 1).replace('/', '.');
                Manifest man = (Manifest) Package.mans.get(fn);
                if (man != null) {
                    pkg = new Package(name, man, url, null);
                } else {
                    Package packageR = new Package(name, null, null, null, null, null, null, null, null);
                }
                Package.pkgs.put(name, pkg);
                return pkg;
            }
        });
    }

    private static Manifest loadManifest(String fn) {
        Throwable th;
        Throwable th2;
        Throwable th3;
        FileInputStream fileInputStream = null;
        JarInputStream jarInputStream = null;
        try {
            FileInputStream fis = new FileInputStream(fn);
            try {
                JarInputStream jis = new JarInputStream(fis, false);
                try {
                    Manifest manifest = jis.getManifest();
                    if (jis != null) {
                        try {
                            jis.close();
                        } catch (Throwable th4) {
                            th = th4;
                        }
                    }
                    th = null;
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (Throwable th5) {
                            th2 = th5;
                            if (th != null) {
                                if (th != th2) {
                                    th.addSuppressed(th2);
                                    th2 = th;
                                }
                            }
                        }
                    }
                    th2 = th;
                    if (th2 == null) {
                        return manifest;
                    }
                    try {
                        throw th2;
                    } catch (IOException e) {
                        fileInputStream = fis;
                        return null;
                    }
                } catch (Throwable th6) {
                    th2 = th6;
                    jarInputStream = jis;
                    fileInputStream = fis;
                    th = null;
                    if (jarInputStream != null) {
                        try {
                            jarInputStream.close();
                        } catch (Throwable th7) {
                            th3 = th7;
                            if (th != null) {
                                if (th != th3) {
                                    th.addSuppressed(th3);
                                    th3 = th;
                                }
                            }
                        }
                    }
                    th3 = th;
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (Throwable th8) {
                            th = th8;
                            if (th3 != null) {
                                if (th3 != th) {
                                    th3.addSuppressed(th);
                                    th = th3;
                                }
                            }
                        }
                    }
                    th = th3;
                    if (th != null) {
                        try {
                            throw th;
                        } catch (IOException e2) {
                            return null;
                        }
                    }
                    throw th2;
                }
            } catch (Throwable th9) {
                th2 = th9;
                fileInputStream = fis;
                th = null;
                if (jarInputStream != null) {
                    jarInputStream.close();
                }
                th3 = th;
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                th = th3;
                if (th != null) {
                    throw th2;
                }
                throw th;
            }
        } catch (Throwable th10) {
            th2 = th10;
            th = null;
            if (jarInputStream != null) {
                jarInputStream.close();
            }
            th3 = th;
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            th = th3;
            if (th != null) {
                throw th;
            }
            throw th2;
        }
    }
}
