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

    /* synthetic */ Package(String x0, Manifest x1, URL x2, ClassLoader x3, AnonymousClass1 x4) {
        this(x0, x1, x2, x3);
    }

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
        StringBuilder stringBuilder;
        String[] sa = this.specVersion.split("\\.", -1);
        int[] si = new int[sa.length];
        int i = 0;
        while (i < sa.length) {
            si[i] = Integer.parseInt(sa[i]);
            if (si[i] >= 0) {
                i++;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("");
                stringBuilder.append(si[i]);
                throw NumberFormatException.forInputString(stringBuilder.toString());
            }
        }
        String[] da = desired.split("\\.", -1);
        int[] di = new int[da.length];
        int i2 = 0;
        while (i2 < da.length) {
            di[i2] = Integer.parseInt(da[i2]);
            if (di[i2] >= 0) {
                i2++;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("");
                stringBuilder.append(di[i2]);
                throw NumberFormatException.forInputString(stringBuilder.toString());
            }
        }
        i2 = Math.max(di.length, si.length);
        int i3 = 0;
        while (i3 < i2) {
            int d = i3 < di.length ? di[i3] : 0;
            int s = i3 < si.length ? si[i3] : 0;
            if (s < d) {
                return false;
            }
            if (s > d) {
                return true;
            }
            i3++;
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
        if (targetSdkVersion <= 0 || targetSdkVersion > 24) {
            StringBuilder stringBuilder;
            String spec = this.specTitle;
            String ver = this.specVersion;
            if (spec == null || spec.length() <= 0) {
                spec = "";
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(", ");
                stringBuilder.append(spec);
                spec = stringBuilder.toString();
            }
            if (ver == null || ver.length() <= 0) {
                ver = "";
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(", version ");
                stringBuilder.append(ver);
                ver = stringBuilder.toString();
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("package ");
            stringBuilder.append(this.pkgName);
            stringBuilder.append(spec);
            stringBuilder.append(ver);
            return stringBuilder.toString();
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("package ");
        stringBuilder2.append(this.pkgName);
        return stringBuilder2.toString();
    }

    private Class<?> getPackageInfo() {
        if (this.packageInfo == null) {
            try {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this.pkgName);
                stringBuilder.append(".package-info");
                this.packageInfo = Class.forName(stringBuilder.toString(), false, this.loader);
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
        String specTitle = null;
        String specVersion = null;
        String specVendor = null;
        String implTitle = null;
        String implVersion = null;
        String implVendor = null;
        URL sealBase = null;
        Attributes attr = man.getAttributes(name.replace('.', '/').concat("/"));
        if (attr != null) {
            specTitle = attr.getValue(Name.SPECIFICATION_TITLE);
            specVersion = attr.getValue(Name.SPECIFICATION_VERSION);
            specVendor = attr.getValue(Name.SPECIFICATION_VENDOR);
            implTitle = attr.getValue(Name.IMPLEMENTATION_TITLE);
            implVersion = attr.getValue(Name.IMPLEMENTATION_VERSION);
            implVendor = attr.getValue(Name.IMPLEMENTATION_VENDOR);
            sealed = attr.getValue(Name.SEALED);
        }
        attr = man.getMainAttributes();
        if (attr != null) {
            if (specTitle == null) {
                specTitle = attr.getValue(Name.SPECIFICATION_TITLE);
            }
            if (specVersion == null) {
                specVersion = attr.getValue(Name.SPECIFICATION_VERSION);
            }
            if (specVendor == null) {
                specVendor = attr.getValue(Name.SPECIFICATION_VENDOR);
            }
            if (implTitle == null) {
                implTitle = attr.getValue(Name.IMPLEMENTATION_TITLE);
            }
            if (implVersion == null) {
                implVersion = attr.getValue(Name.IMPLEMENTATION_VERSION);
            }
            if (implVendor == null) {
                implVendor = attr.getValue(Name.IMPLEMENTATION_VENDOR);
            }
            if (sealed == null) {
                sealed = attr.getValue(Name.SEALED);
            }
        }
        if ("true".equalsIgnoreCase(sealed)) {
            sealBase = url;
        }
        this.pkgName = name;
        this.specTitle = specTitle;
        this.specVersion = specVersion;
        this.specVendor = specVendor;
        this.implTitle = implTitle;
        this.implVersion = implVersion;
        this.implVendor = implVendor;
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
                    Package packageR = new Package(name, man, url, null, null);
                } else {
                    Package packageR2 = new Package(name, null, null, null, null, null, null, null, null);
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
        try {
            FileInputStream fis = new FileInputStream(fn);
            Throwable th4;
            try {
                JarInputStream jis = new JarInputStream(fis, false);
                try {
                    Manifest manifest = jis.getManifest();
                    $closeResource(null, jis);
                    $closeResource(null, fis);
                    return manifest;
                } catch (Throwable th22) {
                    th3 = th22;
                    th22 = th;
                    th = th3;
                }
                $closeResource(th22, jis);
                throw th;
                $closeResource(th, fis);
                throw th4;
            } catch (Throwable th5) {
                th3 = th5;
                th5 = th4;
                th4 = th3;
            }
        } catch (IOException e) {
            return null;
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }
}
