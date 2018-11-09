package android.net.lowpan;

import android.content.Context;
import android.net.lowpan.ILowpanManager.Stub;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class LowpanManager {
    private static final String TAG = LowpanManager.class.getSimpleName();
    private final Map<ILowpanInterface, WeakReference<LowpanInterface>> mBinderCache = new WeakHashMap();
    private final Context mContext;
    private final Map<String, LowpanInterface> mInterfaceCache = new HashMap();
    private final Map<Integer, ILowpanManagerListener> mListenerMap = new HashMap();
    private final Looper mLooper;
    private final ILowpanManager mService;

    public static abstract class Callback {
        public void onInterfaceAdded(LowpanInterface lowpanInterface) {
        }

        public void onInterfaceRemoved(LowpanInterface lowpanInterface) {
        }
    }

    public static LowpanManager from(Context context) {
        return (LowpanManager) context.getSystemService("lowpan");
    }

    public static LowpanManager getManager() {
        IBinder binder = ServiceManager.getService("lowpan");
        if (binder != null) {
            return new LowpanManager(Stub.asInterface(binder));
        }
        return null;
    }

    LowpanManager(ILowpanManager service) {
        this.mService = service;
        this.mContext = null;
        this.mLooper = null;
    }

    public LowpanManager(Context context, ILowpanManager service, Looper looper) {
        this.mContext = context;
        this.mService = service;
        this.mLooper = looper;
    }

    public LowpanInterface getInterface(final ILowpanInterface ifaceService) {
        Throwable th;
        try {
            synchronized (this.mBinderCache) {
                try {
                    LowpanInterface iface;
                    LowpanInterface iface2;
                    if (this.mBinderCache.containsKey(ifaceService)) {
                        iface = (LowpanInterface) ((WeakReference) this.mBinderCache.get(ifaceService)).get();
                    } else {
                        iface = null;
                    }
                    if (iface == null) {
                        try {
                            final String ifaceName = ifaceService.getName();
                            iface2 = new LowpanInterface(this.mContext, ifaceService, this.mLooper);
                            synchronized (this.mInterfaceCache) {
                                this.mInterfaceCache.put(iface2.getName(), iface2);
                            }
                            this.mBinderCache.put(ifaceService, new WeakReference(iface2));
                            ifaceService.asBinder().linkToDeath(new DeathRecipient() {
                                public void binderDied() {
                                    synchronized (LowpanManager.this.mInterfaceCache) {
                                        LowpanInterface iface = (LowpanInterface) LowpanManager.this.mInterfaceCache.get(ifaceName);
                                        if (iface != null && iface.getService() == ifaceService) {
                                            LowpanManager.this.mInterfaceCache.remove(ifaceName);
                                        }
                                    }
                                }
                            }, 0);
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                    }
                    iface2 = iface;
                    return iface2;
                } catch (Throwable th3) {
                    th = th3;
                    throw th;
                }
            }
        } catch (RemoteException x) {
            throw x.rethrowAsRuntimeException();
        }
    }

    public LowpanInterface getInterface(String name) {
        LowpanInterface iface = null;
        try {
            synchronized (this.mInterfaceCache) {
                if (this.mInterfaceCache.containsKey(name)) {
                    iface = (LowpanInterface) this.mInterfaceCache.get(name);
                } else {
                    ILowpanInterface ifaceService = this.mService.getInterface(name);
                    if (ifaceService != null) {
                        iface = getInterface(ifaceService);
                    }
                }
            }
            return iface;
        } catch (RemoteException x) {
            throw x.rethrowFromSystemServer();
        }
    }

    public LowpanInterface getInterface() {
        String[] ifaceList = getInterfaceList();
        if (ifaceList.length > 0) {
            return getInterface(ifaceList[0]);
        }
        return null;
    }

    public String[] getInterfaceList() {
        try {
            return this.mService.getInterfaceList();
        } catch (RemoteException x) {
            throw x.rethrowFromSystemServer();
        }
    }

    public void registerCallback(final Callback cb, final Handler handler) throws LowpanException {
        ILowpanManagerListener.Stub listenerBinder = new ILowpanManagerListener.Stub() {
            private Handler mHandler;

            public void onInterfaceAdded(ILowpanInterface ifaceService) {
                this.mHandler.post(new -$Lambda$fU5N8X3bFktKBQFPK6v4czv7e_Y((byte) 0, this, ifaceService, cb));
            }

            /* synthetic */ void lambda$-android_net_lowpan_LowpanManager$2_8833(ILowpanInterface ifaceService, Callback cb) {
                LowpanInterface iface = LowpanManager.this.getInterface(ifaceService);
                if (iface != null) {
                    cb.onInterfaceAdded(iface);
                }
            }

            public void onInterfaceRemoved(ILowpanInterface ifaceService) {
                this.mHandler.post(new -$Lambda$fU5N8X3bFktKBQFPK6v4czv7e_Y((byte) 1, this, ifaceService, cb));
            }

            /* synthetic */ void lambda$-android_net_lowpan_LowpanManager$2_9391(ILowpanInterface ifaceService, Callback cb) {
                LowpanInterface iface = LowpanManager.this.getInterface(ifaceService);
                if (iface != null) {
                    cb.onInterfaceRemoved(iface);
                }
            }
        };
        try {
            this.mService.addListener(listenerBinder);
            synchronized (this.mListenerMap) {
                this.mListenerMap.put(Integer.valueOf(System.identityHashCode(cb)), listenerBinder);
            }
        } catch (RemoteException x) {
            throw x.rethrowFromSystemServer();
        }
    }

    public void registerCallback(Callback cb) throws LowpanException {
        registerCallback(cb, null);
    }

    public void unregisterCallback(Callback cb) {
        Integer hashCode = Integer.valueOf(System.identityHashCode(cb));
        synchronized (this.mListenerMap) {
            ILowpanManagerListener listenerBinder = (ILowpanManagerListener) this.mListenerMap.get(hashCode);
            this.mListenerMap.remove(hashCode);
        }
        if (listenerBinder != null) {
            try {
                this.mService.removeListener(listenerBinder);
                return;
            } catch (RemoteException x) {
                throw x.rethrowFromSystemServer();
            }
        }
        throw new RuntimeException("Attempt to unregister an unknown callback");
    }
}
