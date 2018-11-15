package com.android.server.location;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.WorkSource;
import android.util.Log;
import com.android.internal.location.ILocationProvider;
import com.android.internal.location.ILocationProvider.Stub;
import com.android.internal.location.ProviderProperties;
import com.android.internal.location.ProviderRequest;
import com.android.internal.os.TransferPipe;
import com.android.server.ServiceWatcher;
import com.android.server.ServiceWatcher.BinderRunner;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class LocationProviderProxy implements LocationProviderInterface {
    private static final boolean D = false;
    private static final String TAG = "LocationProviderProxy";
    private final Context mContext;
    private boolean mEnabled = false;
    private Object mLock = new Object();
    private final String mName;
    private Runnable mNewServiceWork = new Runnable() {
        public void run() {
            final boolean enabled;
            final ProviderRequest request;
            final WorkSource source;
            ProviderProperties[] properties = new ProviderProperties[1];
            synchronized (LocationProviderProxy.this.mLock) {
                enabled = LocationProviderProxy.this.mEnabled;
                request = LocationProviderProxy.this.mRequest;
                source = LocationProviderProxy.this.mWorksource;
            }
            final ProviderProperties[] providerPropertiesArr = properties;
            LocationProviderProxy.this.mServiceWatcher.runOnBinder(new BinderRunner() {
                public void run(IBinder binder) {
                    ILocationProvider service = Stub.asInterface(binder);
                    try {
                        providerPropertiesArr[0] = service.getProperties();
                        if (providerPropertiesArr[0] == null) {
                            String str = LocationProviderProxy.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(LocationProviderProxy.this.mServiceWatcher.getBestPackageName());
                            stringBuilder.append(" has invalid location provider properties");
                            Log.e(str, stringBuilder.toString());
                        }
                        if (enabled) {
                            service.enable();
                            if (request != null) {
                                service.setRequest(request, source);
                            }
                        }
                    } catch (RemoteException e) {
                        Log.w(LocationProviderProxy.TAG, e);
                    } catch (Exception e2) {
                        String str2 = LocationProviderProxy.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Exception from ");
                        stringBuilder2.append(LocationProviderProxy.this.mServiceWatcher.getBestPackageName());
                        Log.e(str2, stringBuilder2.toString(), e2);
                    }
                }
            });
            synchronized (LocationProviderProxy.this.mLock) {
                LocationProviderProxy.this.mProperties = properties[0];
            }
        }
    };
    private ProviderProperties mProperties;
    private ProviderRequest mRequest = null;
    private final ServiceWatcher mServiceWatcher;
    private WorkSource mWorksource = new WorkSource();

    public static LocationProviderProxy createAndBind(Context context, String name, String action, int overlaySwitchResId, int defaultServicePackageNameResId, int initialPackageNamesResId, Handler handler) {
        LocationProviderProxy proxy = new LocationProviderProxy(context, name, action, overlaySwitchResId, defaultServicePackageNameResId, initialPackageNamesResId, handler);
        if (proxy.bind()) {
            return proxy;
        }
        return null;
    }

    private LocationProviderProxy(Context context, String name, String action, int overlaySwitchResId, int defaultServicePackageNameResId, int initialPackageNamesResId, Handler handler) {
        String str = name;
        this.mContext = context;
        this.mName = str;
        Context context2 = this.mContext;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("LocationProviderProxy-");
        stringBuilder.append(str);
        this.mServiceWatcher = new ServiceWatcher(context2, stringBuilder.toString(), action, overlaySwitchResId, defaultServicePackageNameResId, initialPackageNamesResId, this.mNewServiceWork, handler);
    }

    protected LocationProviderProxy(Context context, String name) {
        this.mContext = context;
        this.mName = name;
        this.mServiceWatcher = null;
    }

    private boolean bind() {
        return this.mServiceWatcher.start();
    }

    public String getConnectedPackageName() {
        return this.mServiceWatcher.getBestPackageName();
    }

    public String getName() {
        return this.mName;
    }

    public ProviderProperties getProperties() {
        ProviderProperties providerProperties;
        synchronized (this.mLock) {
            providerProperties = this.mProperties;
        }
        return providerProperties;
    }

    public void enable() {
        synchronized (this.mLock) {
            this.mEnabled = true;
        }
        this.mServiceWatcher.runOnBinder(new BinderRunner() {
            public void run(IBinder binder) {
                try {
                    Stub.asInterface(binder).enable();
                } catch (RemoteException e) {
                    Log.w(LocationProviderProxy.TAG, e);
                } catch (Exception e2) {
                    String str = LocationProviderProxy.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception from ");
                    stringBuilder.append(LocationProviderProxy.this.mServiceWatcher.getBestPackageName());
                    Log.e(str, stringBuilder.toString(), e2);
                }
            }
        });
    }

    public void disable() {
        synchronized (this.mLock) {
            this.mEnabled = false;
        }
        this.mServiceWatcher.runOnBinder(new BinderRunner() {
            public void run(IBinder binder) {
                try {
                    Stub.asInterface(binder).disable();
                } catch (RemoteException e) {
                    Log.w(LocationProviderProxy.TAG, e);
                } catch (Exception e2) {
                    String str = LocationProviderProxy.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception from ");
                    stringBuilder.append(LocationProviderProxy.this.mServiceWatcher.getBestPackageName());
                    Log.e(str, stringBuilder.toString(), e2);
                }
            }
        });
    }

    public boolean isEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mEnabled;
        }
        return z;
    }

    public void setRequest(final ProviderRequest request, final WorkSource source) {
        synchronized (this.mLock) {
            this.mRequest = request;
            this.mWorksource = source;
        }
        this.mServiceWatcher.runOnBinder(new BinderRunner() {
            public void run(IBinder binder) {
                try {
                    Stub.asInterface(binder).setRequest(request, source);
                } catch (RemoteException e) {
                    Log.w(LocationProviderProxy.TAG, e);
                } catch (Exception e2) {
                    String str = LocationProviderProxy.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception from ");
                    stringBuilder.append(LocationProviderProxy.this.mServiceWatcher.getBestPackageName());
                    Log.e(str, stringBuilder.toString(), e2);
                }
            }
        });
    }

    public void dump(final FileDescriptor fd, final PrintWriter pw, final String[] args) {
        pw.append("REMOTE SERVICE");
        pw.append(" name=").append(this.mName);
        pw.append(" pkg=").append(this.mServiceWatcher.getBestPackageName());
        PrintWriter append = pw.append(" version=");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        stringBuilder.append(this.mServiceWatcher.getBestVersion());
        append.append(stringBuilder.toString());
        pw.append(10);
        if (!this.mServiceWatcher.runOnBinder(new BinderRunner() {
            /* JADX WARNING: Removed duplicated region for block: B:3:0x0010 A:{Splitter: B:1:0x0004, ExcHandler: java.io.IOException (r1_1 'e' java.lang.Exception)} */
            /* JADX WARNING: Missing block: B:3:0x0010, code:
            r1 = move-exception;
     */
            /* JADX WARNING: Missing block: B:4:0x0011, code:
            r2 = r5;
            r3 = new java.lang.StringBuilder();
            r3.append("Failed to dump location provider: ");
            r3.append(r1);
            r2.println(r3.toString());
     */
            /* JADX WARNING: Missing block: B:5:?, code:
            return;
     */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void run(IBinder binder) {
                try {
                    TransferPipe.dumpAsync(Stub.asInterface(binder).asBinder(), fd, args);
                } catch (Exception e) {
                }
            }
        })) {
            pw.println("service down (null)");
        }
    }

    public int getStatus(final Bundle extras) {
        final int[] result = new int[]{1};
        this.mServiceWatcher.runOnBinder(new BinderRunner() {
            public void run(IBinder binder) {
                try {
                    result[0] = Stub.asInterface(binder).getStatus(extras);
                } catch (RemoteException e) {
                    Log.w(LocationProviderProxy.TAG, e);
                } catch (Exception e2) {
                    String str = LocationProviderProxy.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception from ");
                    stringBuilder.append(LocationProviderProxy.this.mServiceWatcher.getBestPackageName());
                    Log.e(str, stringBuilder.toString(), e2);
                }
            }
        });
        return result[0];
    }

    public long getStatusUpdateTime() {
        final long[] result = new long[]{0};
        this.mServiceWatcher.runOnBinder(new BinderRunner() {
            public void run(IBinder binder) {
                try {
                    result[0] = Stub.asInterface(binder).getStatusUpdateTime();
                } catch (RemoteException e) {
                    Log.w(LocationProviderProxy.TAG, e);
                } catch (Exception e2) {
                    String str = LocationProviderProxy.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception from ");
                    stringBuilder.append(LocationProviderProxy.this.mServiceWatcher.getBestPackageName());
                    Log.e(str, stringBuilder.toString(), e2);
                }
            }
        });
        return result[0];
    }

    public boolean sendExtraCommand(final String command, final Bundle extras) {
        final boolean[] result = new boolean[]{false};
        this.mServiceWatcher.runOnBinder(new BinderRunner() {
            public void run(IBinder binder) {
                try {
                    result[0] = Stub.asInterface(binder).sendExtraCommand(command, extras);
                } catch (RemoteException e) {
                    Log.w(LocationProviderProxy.TAG, e);
                } catch (Exception e2) {
                    String str = LocationProviderProxy.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception from ");
                    stringBuilder.append(LocationProviderProxy.this.mServiceWatcher.getBestPackageName());
                    Log.e(str, stringBuilder.toString(), e2);
                }
            }
        });
        return result[0];
    }
}
