package huawei.cust;

import android.os.SystemProperties;
import android.util.Log;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public final class HwCustUtils {
    static final String CUST_CLS_NULL_REPLACE = "-";
    static final String CUST_CLS_SUFFIX_DEF = "Impl:-";
    static final String CUST_CLS_SUFFIX_SEP = ":";
    static boolean CUST_VERSION = new File(FILE_ONLY_IN_CUST).exists();
    static final boolean DEBUG_I = false;
    static final boolean EXCEPTION_WHEN_ERROR = true;
    static final String[] FACTORY_ARRAY = SystemProperties.get(PROP_CUST_CLS_SUFFIX, CUST_CLS_SUFFIX_DEF).split(CUST_CLS_SUFFIX_SEP);
    static String FILE_ONLY_IN_CUST = "/system/etc/permissions/hwcustframework.xml";
    static final String HWCUST_PREFIX = "HwCust";
    static final String PROP_CUST_CLS_SUFFIX = "cust.cls.suffixes";
    static final String TAG = "HwCust";
    private static HashMap<String, ClassInfo> mClassCache = new HashMap();
    private static HashMap<String, Constructor<?>> mConstructorCache = new HashMap();
    private static HashMap<Class<?>, Class<?>> mPrimitiveMap = new HashMap();
    private static String mRegion;

    static class ClassInfo {
        Class<?> mCls;
        Constructor<?>[] mCs;
        String mOrgClsName;

        ClassInfo(String orgName, Class<?> cls) {
            this.mOrgClsName = orgName;
            this.mCls = cls;
            this.mCs = cls.getDeclaredConstructors();
        }
    }

    static {
        mPrimitiveMap.put(Boolean.TYPE, Boolean.class);
        mPrimitiveMap.put(Byte.TYPE, Byte.class);
        mPrimitiveMap.put(Character.TYPE, Character.class);
        mPrimitiveMap.put(Short.TYPE, Short.class);
        mPrimitiveMap.put(Integer.TYPE, Integer.class);
        mPrimitiveMap.put(Long.TYPE, Long.class);
        mPrimitiveMap.put(Float.TYPE, Float.class);
        mPrimitiveMap.put(Double.TYPE, Double.class);
        int i = 0;
        while (i < FACTORY_ARRAY.length) {
            if (FACTORY_ARRAY[i] == null || FACTORY_ARRAY[i].equals("-")) {
                FACTORY_ARRAY[i] = "";
            }
            i++;
        }
        initRegionInfo();
    }

    public static Object createObj(String className, ClassLoader cl, Object... args) {
        ClassInfo clsInfo = getClassByName(className, cl, FACTORY_ARRAY);
        if (clsInfo == null) {
            return null;
        }
        Constructor<?> useConstructor = findConstructor(clsInfo, args);
        if (useConstructor == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("constructor not found for ");
            stringBuilder.append(clsInfo.mCls);
            handle_exception(stringBuilder.toString(), new NullPointerException());
            return null;
        }
        try {
            return useConstructor.newInstance(args);
        } catch (ExceptionInInitializerError | IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException ex) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("create cust obj fail. Class = ");
            stringBuilder2.append(clsInfo.mCls);
            stringBuilder2.append(", constructor = ");
            stringBuilder2.append(useConstructor);
            handle_exception(stringBuilder2.toString(), ex);
            return null;
        }
    }

    public static Object createObj(Class<?> classClass, Object... args) {
        return createObj(classClass.getName(), classClass.getClassLoader(), args);
    }

    public static String getVersionRegion() {
        return mRegion;
    }

    private static void initRegionInfo() {
        String optb = SystemProperties.get("ro.config.hw_optb", "");
        String vendor = SystemProperties.get("ro.hw.vendor", "");
        String country = SystemProperties.get("ro.hw.country", "");
        if (optb.equals("156")) {
            mRegion = "cn";
        } else if (optb.equals("376")) {
            mRegion = "il";
        } else if (((vendor.equals("orange") || vendor.equals("altice")) && country.equals("all")) || (vendor.equals("tef") && country.equals("normal"))) {
            mRegion = "eu";
        } else if (vendor.equals("orange") && country.equals("btob")) {
            mRegion = "fr";
        } else {
            mRegion = country;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:32:0x0085 A:{SYNTHETIC, Splitter:B:32:0x0085} */
    /* JADX WARNING: Missing block: B:35:0x009f, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static synchronized ClassInfo getClassByName(String className, ClassLoader cl, String[] allSuffix) {
        synchronized (HwCustUtils.class) {
            ClassInfo clsInfo = (ClassInfo) mClassCache.get(className);
            if (clsInfo != null) {
                return clsInfo;
            }
            StringBuilder stringBuilder;
            if (className != null) {
                if (!(className.length() == 0 || className.contains("$"))) {
                    if (className.contains(".HwCust")) {
                        int i = 0;
                        while (i < allSuffix.length) {
                            try {
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(className);
                                stringBuilder2.append(allSuffix[i]);
                                Class<?> dstClass = Class.forName(stringBuilder2.toString(), true, cl);
                                clsInfo = new ClassInfo(className, dstClass);
                                mClassCache.put(className, clsInfo);
                                if ((CUST_VERSION && i == allSuffix.length - 1) || !(CUST_VERSION || i == allSuffix.length - 1)) {
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("CUST VERSION = ");
                                    stringBuilder3.append(CUST_VERSION);
                                    stringBuilder3.append(", use class = ");
                                    stringBuilder3.append(dstClass);
                                    Log.w("HwCust", stringBuilder3.toString());
                                }
                                if (clsInfo == null) {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Class / custClass not found for: ");
                                    stringBuilder.append(className);
                                    handle_exception(stringBuilder.toString(), new ClassNotFoundException());
                                }
                            } catch (ClassNotFoundException e) {
                                i++;
                            }
                        }
                        if (clsInfo == null) {
                        }
                    }
                }
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("createCustImpl obj, className invalid: ");
            stringBuilder.append(className);
            handle_exception(stringBuilder.toString(), new Exception());
            return null;
        }
    }

    static synchronized Constructor<?> findConstructor(ClassInfo info, Object... args) {
        synchronized (HwCustUtils.class) {
            String tag = getArgsType(info.mOrgClsName, args);
            Constructor<?> useConstructor = (Constructor) mConstructorCache.get(tag);
            if (useConstructor != null) {
                return useConstructor;
            }
            Constructor<?> useConstructor2 = useConstructor;
            for (Constructor<?> c : info.mCs) {
                Class<?>[] ptcs = c.getParameterTypes();
                if (!Modifier.isPrivate(c.getModifiers())) {
                    if (ptcs.length == args.length) {
                        if (ptcs.length == 0) {
                            useConstructor2 = c;
                        } else {
                            Constructor<?> useConstructor3 = useConstructor2;
                            for (int i = 0; i < args.length; i++) {
                                if (args[i] == null) {
                                    if (ptcs[i].isPrimitive()) {
                                        break;
                                    }
                                }
                                Class<?> argCls = args[i].getClass();
                                Class<?> ptcCls = ptcs[i];
                                if (!(ptcCls.isPrimitive() ? (Class) mPrimitiveMap.get(ptcCls) : ptcCls).isAssignableFrom(argCls.isPrimitive() ? (Class) mPrimitiveMap.get(argCls) : argCls)) {
                                    break;
                                }
                                if (i == args.length - 1) {
                                    useConstructor3 = c;
                                }
                            }
                            useConstructor2 = useConstructor3;
                        }
                        if (useConstructor2 != null) {
                            break;
                        }
                    }
                }
            }
            mConstructorCache.put(tag, useConstructor2);
            return useConstructor2;
        }
    }

    static String getArgsType(String clsName, Object... args) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(clsName);
        stringBuilder.append(":-");
        StringBuilder sb = new StringBuilder(stringBuilder.toString());
        for (Object arg : args) {
            if (arg == null) {
                sb.append(":null");
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(CUST_CLS_SUFFIX_SEP);
                stringBuilder2.append(arg.getClass());
                sb.append(stringBuilder2.toString());
            }
        }
        return sb.toString();
    }

    static void log_info(String msg) {
    }

    static void handle_exception(String msg, Throwable th) {
        throw new RuntimeException(msg, th);
    }
}
