package android.telephony.ims.stub;

import android.annotation.SystemApi;
import android.content.Context;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsConfigCallback;
import android.telephony.ims.aidl.IImsConfigCallback.Stub;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import java.lang.ref.WeakReference;
import java.util.HashMap;

@SystemApi
public class ImsConfigImplBase {
    public static final int CONFIG_RESULT_FAILED = 1;
    public static final int CONFIG_RESULT_SUCCESS = 0;
    public static final int CONFIG_RESULT_UNKNOWN = -1;
    private static final String TAG = "ImsConfigImplBase";
    private final RemoteCallbackList<IImsConfigCallback> mCallbacks = new RemoteCallbackList();
    ImsConfigStub mImsConfigStub = new ImsConfigStub(this);

    public static class Callback extends Stub {
        public final void onIntConfigChanged(int item, int value) throws RemoteException {
            onConfigChanged(item, value);
        }

        public final void onStringConfigChanged(int item, String value) throws RemoteException {
            onConfigChanged(item, value);
        }

        public void onConfigChanged(int item, int value) {
        }

        public void onConfigChanged(int item, String value) {
        }
    }

    @VisibleForTesting
    public static class ImsConfigStub extends IImsConfig.Stub {
        WeakReference<ImsConfigImplBase> mImsConfigImplBaseWeakReference;
        private HashMap<Integer, Integer> mProvisionedIntValue = new HashMap();
        private HashMap<Integer, String> mProvisionedStringValue = new HashMap();

        @VisibleForTesting
        public ImsConfigStub(ImsConfigImplBase imsConfigImplBase) {
            this.mImsConfigImplBaseWeakReference = new WeakReference(imsConfigImplBase);
        }

        public void addImsConfigCallback(IImsConfigCallback c) throws RemoteException {
            getImsConfigImpl().addImsConfigCallback(c);
        }

        public void removeImsConfigCallback(IImsConfigCallback c) throws RemoteException {
            getImsConfigImpl().removeImsConfigCallback(c);
        }

        /* JADX WARNING: Missing block: B:12:0x002f, code skipped:
            return r0;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public synchronized int getConfigInt(int item) throws RemoteException {
            if (this.mProvisionedIntValue.containsKey(Integer.valueOf(item))) {
                return ((Integer) this.mProvisionedIntValue.get(Integer.valueOf(item))).intValue();
            }
            int retVal = getImsConfigImpl().getConfigInt(item);
            if (retVal != -1) {
                updateCachedValue(item, retVal, false);
            }
        }

        /* JADX WARNING: Missing block: B:12:0x002a, code skipped:
            return r0;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public synchronized String getConfigString(int item) throws RemoteException {
            if (this.mProvisionedIntValue.containsKey(Integer.valueOf(item))) {
                return (String) this.mProvisionedStringValue.get(Integer.valueOf(item));
            }
            String retVal = getImsConfigImpl().getConfigString(item);
            if (retVal != null) {
                updateCachedValue(item, retVal, false);
            }
        }

        public synchronized int setConfigInt(int item, int value) throws RemoteException {
            int retVal;
            this.mProvisionedIntValue.remove(Integer.valueOf(item));
            retVal = getImsConfigImpl().setConfig(item, value);
            if (retVal == 0) {
                updateCachedValue(item, value, true);
            } else {
                String str = ImsConfigImplBase.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Set provision value of ");
                stringBuilder.append(item);
                stringBuilder.append(" to ");
                stringBuilder.append(value);
                stringBuilder.append(" failed with error code ");
                stringBuilder.append(retVal);
                Log.d(str, stringBuilder.toString());
            }
            return retVal;
        }

        public synchronized int setConfigString(int item, String value) throws RemoteException {
            int retVal;
            this.mProvisionedStringValue.remove(Integer.valueOf(item));
            retVal = getImsConfigImpl().setConfig(item, value);
            if (retVal == 0) {
                updateCachedValue(item, value, true);
            }
            return retVal;
        }

        private ImsConfigImplBase getImsConfigImpl() throws RemoteException {
            ImsConfigImplBase ref = (ImsConfigImplBase) this.mImsConfigImplBaseWeakReference.get();
            if (ref != null) {
                return ref;
            }
            throw new RemoteException("Fail to get ImsConfigImpl");
        }

        private void notifyImsConfigChanged(int item, int value) throws RemoteException {
            getImsConfigImpl().notifyConfigChanged(item, value);
        }

        private void notifyImsConfigChanged(int item, String value) throws RemoteException {
            getImsConfigImpl().notifyConfigChanged(item, value);
        }

        protected synchronized void updateCachedValue(int item, int value, boolean notifyChange) throws RemoteException {
            this.mProvisionedIntValue.put(Integer.valueOf(item), Integer.valueOf(value));
            if (notifyChange) {
                notifyImsConfigChanged(item, value);
            }
        }

        protected synchronized void updateCachedValue(int item, String value, boolean notifyChange) throws RemoteException {
            this.mProvisionedStringValue.put(Integer.valueOf(item), value);
            if (notifyChange) {
                notifyImsConfigChanged(item, value);
            }
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (getImsConfigImpl().isHwCustCode(code)) {
                return getImsConfigImpl().onTransact(code, data, reply, flags);
            }
            return super.onTransact(code, data, reply, flags);
        }

        public int setImsConfig(String configKey, PersistableBundle configValue) throws RemoteException {
            return getImsConfigImpl().setImsConfig(configKey, configValue);
        }

        public PersistableBundle getImsConfig(String configKey) throws RemoteException {
            return getImsConfigImpl().getImsConfig(configKey);
        }
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        return false;
    }

    public boolean isHwCustCode(int code) {
        return false;
    }

    public ImsConfigImplBase(Context context) {
    }

    private void addImsConfigCallback(IImsConfigCallback c) {
        this.mCallbacks.register(c);
    }

    private void removeImsConfigCallback(IImsConfigCallback c) {
        this.mCallbacks.unregister(c);
    }

    private final void notifyConfigChanged(int item, int value) {
        if (this.mCallbacks != null) {
            this.mCallbacks.broadcast(new -$$Lambda$ImsConfigImplBase$yL4863k-FoQyqg_FX2mWsLMqbyA(item, value));
        }
    }

    static /* synthetic */ void lambda$notifyConfigChanged$0(int item, int value, IImsConfigCallback c) {
        try {
            c.onIntConfigChanged(item, value);
        } catch (RemoteException e) {
            Log.w(TAG, "notifyConfigChanged(int): dead binder in notify, skipping.");
        }
    }

    private void notifyConfigChanged(int item, String value) {
        if (this.mCallbacks != null) {
            this.mCallbacks.broadcast(new -$$Lambda$ImsConfigImplBase$GAuYvQ8qBc7KgCJhNp4Pt4j5t-0(item, value));
        }
    }

    static /* synthetic */ void lambda$notifyConfigChanged$1(int item, String value, IImsConfigCallback c) {
        try {
            c.onStringConfigChanged(item, value);
        } catch (RemoteException e) {
            Log.w(TAG, "notifyConfigChanged(string): dead binder in notify, skipping.");
        }
    }

    public IImsConfig getIImsConfig() {
        return this.mImsConfigStub;
    }

    public final void notifyProvisionedValueChanged(int item, int value) {
        try {
            this.mImsConfigStub.updateCachedValue(item, value, true);
        } catch (RemoteException e) {
            Log.w(TAG, "notifyProvisionedValueChanged(int): Framework connection is dead.");
        }
    }

    public final void notifyProvisionedValueChanged(int item, String value) {
        try {
            this.mImsConfigStub.updateCachedValue(item, value, true);
        } catch (RemoteException e) {
            Log.w(TAG, "notifyProvisionedValueChanged(string): Framework connection is dead.");
        }
    }

    public int setConfig(int item, int value) {
        return 1;
    }

    public int setConfig(int item, String value) {
        return 1;
    }

    public int getConfigInt(int item) {
        return -1;
    }

    public String getConfigString(int item) {
        return null;
    }

    public int setImsConfig(String configKey, PersistableBundle configValue) throws RemoteException {
        return 1;
    }

    public PersistableBundle getImsConfig(String configKey) throws RemoteException {
        return null;
    }
}
