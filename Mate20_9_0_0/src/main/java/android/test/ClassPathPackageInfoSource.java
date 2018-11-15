package android.test;

import dalvik.system.DexFile;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

@Deprecated
public class ClassPathPackageInfoSource {
    private static final ClassLoader CLASS_LOADER = ClassPathPackageInfoSource.class.getClassLoader();
    private static String[] apkPaths;
    private static ClassPathPackageInfoSource classPathSource;
    private final SimpleCache<String, ClassPathPackageInfo> cache = new SimpleCache<String, ClassPathPackageInfo>() {
        protected ClassPathPackageInfo load(String pkgName) {
            return ClassPathPackageInfoSource.this.createPackageInfo(pkgName);
        }
    };
    private final ClassLoader classLoader;
    private final String[] classPath;

    private class ClassPathPackageInfo {
        private final String packageName;
        private final Set<String> subpackageNames;
        private final Set<Class<?>> topLevelClasses;

        /* synthetic */ ClassPathPackageInfo(ClassPathPackageInfoSource x0, String x1, Set x2, Set x3, AnonymousClass1 x4) {
            this(x1, x2, x3);
        }

        private ClassPathPackageInfo(String packageName, Set<String> subpackageNames, Set<Class<?>> topLevelClasses) {
            this.packageName = packageName;
            this.subpackageNames = Collections.unmodifiableSet(subpackageNames);
            this.topLevelClasses = Collections.unmodifiableSet(topLevelClasses);
        }

        private Set<ClassPathPackageInfo> getSubpackages() {
            Set<ClassPathPackageInfo> info = new HashSet();
            for (String name : this.subpackageNames) {
                info.add((ClassPathPackageInfo) ClassPathPackageInfoSource.this.cache.get(name));
            }
            return info;
        }

        private Set<Class<?>> getTopLevelClassesRecursive() {
            Set<Class<?>> set = new HashSet();
            addTopLevelClassesTo(set);
            return set;
        }

        private void addTopLevelClassesTo(Set<Class<?>> set) {
            set.addAll(this.topLevelClasses);
            for (ClassPathPackageInfo info : getSubpackages()) {
                info.addTopLevelClassesTo(set);
            }
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof ClassPathPackageInfo)) {
                return false;
            }
            return this.packageName.equals(((ClassPathPackageInfo) obj).packageName);
        }

        public int hashCode() {
            return this.packageName.hashCode();
        }
    }

    private ClassPathPackageInfoSource(ClassLoader classLoader) {
        this.classLoader = classLoader;
        this.classPath = getClassPath();
    }

    static void setApkPaths(String[] apkPaths) {
        apkPaths = apkPaths;
    }

    public static ClassPathPackageInfoSource forClassPath(ClassLoader classLoader) {
        if (classPathSource == null) {
            classPathSource = new ClassPathPackageInfoSource(classLoader);
        }
        return classPathSource;
    }

    public Set<Class<?>> getTopLevelClassesRecursive(String packageName) {
        return ((ClassPathPackageInfo) this.cache.get(packageName)).getTopLevelClassesRecursive();
    }

    /* JADX WARNING: Removed duplicated region for block: B:15:0x0047 A:{ExcHandler: java.lang.ClassNotFoundException (r3_6 'e' java.lang.Throwable), Splitter: B:9:0x0036} */
    /* JADX WARNING: Missing block: B:15:0x0047, code:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:16:0x0048, code:
            r5 = new java.lang.StringBuilder();
            r5.append("Cannot load class. Make sure it is in your apk. Class name: '");
            r5.append(r2);
            r5.append("'. Message: ");
            r5.append(r3.getMessage());
            android.util.Log.w("ClassPathPackageInfoSource", r5.toString(), r3);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private ClassPathPackageInfo createPackageInfo(String packageName) {
        Set<String> subpackageNames = new TreeSet();
        TreeSet<String> classNames = new TreeSet();
        Set topLevelClasses = new HashSet();
        findClasses(packageName, classNames, subpackageNames);
        for (String className : classNames) {
            if (!className.endsWith(".R")) {
                if (!className.endsWith(".Manifest")) {
                    try {
                        topLevelClasses.add(Class.forName(className, false, this.classLoader != null ? this.classLoader : CLASS_LOADER));
                    } catch (Throwable e) {
                    }
                }
            }
        }
        return new ClassPathPackageInfo(this, packageName, subpackageNames, topLevelClasses, null);
    }

    private void findClasses(String packageName, Set<String> classNames, Set<String> subpackageNames) {
        for (String entryName : this.classPath) {
            if (new File(entryName).exists()) {
                try {
                    if (entryName.endsWith(".apk")) {
                        findClassesInApk(entryName, packageName, classNames, subpackageNames);
                    } else {
                        for (String apkPath : apkPaths) {
                            scanForApkFiles(new File(apkPath), packageName, classNames, subpackageNames);
                        }
                    }
                } catch (IOException e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Can't read classpath entry ");
                    stringBuilder.append(entryName);
                    stringBuilder.append(": ");
                    stringBuilder.append(e.getMessage());
                    throw new AssertionError(stringBuilder.toString());
                }
            }
        }
    }

    private void scanForApkFiles(File source, String packageName, Set<String> classNames, Set<String> subpackageNames) throws IOException {
        if (source.getPath().endsWith(".apk")) {
            findClassesInApk(source.getPath(), packageName, classNames, subpackageNames);
            return;
        }
        File[] files = source.listFiles();
        if (files != null) {
            for (File file : files) {
                scanForApkFiles(file, packageName, classNames, subpackageNames);
            }
        }
    }

    private void findClassesInApk(String apkPath, String packageName, Set<String> classNames, Set<String> subpackageNames) throws IOException {
        try {
            Enumeration<String> apkClassNames = new DexFile(apkPath).entries();
            while (apkClassNames.hasMoreElements()) {
                String className = (String) apkClassNames.nextElement();
                if (className.startsWith(packageName)) {
                    String subPackageName = packageName;
                    int lastPackageSeparator = className.lastIndexOf(46);
                    if (lastPackageSeparator > 0) {
                        subPackageName = className.substring(0, lastPackageSeparator);
                    }
                    if (subPackageName.length() > packageName.length()) {
                        subpackageNames.add(subPackageName);
                    } else if (isToplevelClass(className)) {
                        classNames.add(className);
                    }
                }
            }
        } catch (IOException e) {
        }
    }

    private static boolean isToplevelClass(String fileName) {
        return fileName.indexOf(36) < 0;
    }

    private static String[] getClassPath() {
        return System.getProperty("java.class.path").split(Pattern.quote(System.getProperty("path.separator", ":")));
    }
}
