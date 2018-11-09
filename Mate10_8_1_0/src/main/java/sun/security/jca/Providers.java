package sun.security.jca;

import java.security.Provider;

public class Providers {
    private static final String BACKUP_PROVIDER_CLASSNAME = "sun.security.provider.VerificationProvider";
    private static final String[] jarVerificationProviders = new String[]{"com.android.org.conscrypt.OpenSSLProvider", "com.android.org.bouncycastle.jce.provider.BouncyCastleProvider", "com.android.org.conscrypt.JSSEProvider", BACKUP_PROVIDER_CLASSNAME};
    private static volatile ProviderList providerList;
    private static final ThreadLocal<ProviderList> threadLists = new InheritableThreadLocal();
    private static volatile int threadListsUsed;

    static {
        providerList = ProviderList.EMPTY;
        providerList = ProviderList.fromSecurityProperties();
        int numConfiguredProviders = providerList.size();
        providerList = providerList.removeInvalid();
        if (numConfiguredProviders != providerList.size()) {
            throw new AssertionError((Object) "Unable to configure default providers");
        }
    }

    private Providers() {
    }

    public static Provider getSunProvider() {
        try {
            return (Provider) Class.forName(jarVerificationProviders[0]).newInstance();
        } catch (Exception e) {
            try {
                return (Provider) Class.forName(BACKUP_PROVIDER_CLASSNAME).newInstance();
            } catch (Exception e2) {
                throw new RuntimeException("Sun provider not found", e);
            }
        }
    }

    public static Object startJarVerification() {
        return beginThreadProviderList(getProviderList().getJarList(jarVerificationProviders));
    }

    public static void stopJarVerification(Object obj) {
        endThreadProviderList((ProviderList) obj);
    }

    public static ProviderList getProviderList() {
        ProviderList list = getThreadProviderList();
        if (list == null) {
            return getSystemProviderList();
        }
        return list;
    }

    public static void setProviderList(ProviderList newList) {
        if (getThreadProviderList() == null) {
            setSystemProviderList(newList);
        } else {
            changeThreadProviderList(newList);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static ProviderList getFullProviderList() {
        synchronized (Providers.class) {
            ProviderList list = getThreadProviderList();
            if (list != null) {
                ProviderList newList = list.removeInvalid();
                if (newList != list) {
                    changeThreadProviderList(newList);
                    list = newList;
                }
            }
        }
    }

    private static ProviderList getSystemProviderList() {
        return providerList;
    }

    private static void setSystemProviderList(ProviderList list) {
        providerList = list;
    }

    public static ProviderList getThreadProviderList() {
        if (threadListsUsed == 0) {
            return null;
        }
        return (ProviderList) threadLists.get();
    }

    private static void changeThreadProviderList(ProviderList list) {
        threadLists.set(list);
    }

    public static synchronized ProviderList beginThreadProviderList(ProviderList list) {
        ProviderList oldList;
        synchronized (Providers.class) {
            if (ProviderList.debug != null) {
                ProviderList.debug.println("ThreadLocal providers: " + list);
            }
            oldList = (ProviderList) threadLists.get();
            threadListsUsed++;
            threadLists.set(list);
        }
        return oldList;
    }

    public static synchronized void endThreadProviderList(ProviderList list) {
        synchronized (Providers.class) {
            if (list == null) {
                if (ProviderList.debug != null) {
                    ProviderList.debug.println("Disabling ThreadLocal providers");
                }
                threadLists.remove();
            } else {
                if (ProviderList.debug != null) {
                    ProviderList.debug.println("Restoring previous ThreadLocal providers: " + list);
                }
                threadLists.set(list);
            }
            threadListsUsed--;
        }
    }
}
