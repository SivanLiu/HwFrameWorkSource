package android.app.slice;

import android.app.DownloadManager;
import android.app.slice.ISliceManager.Stub;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceManager.ServiceNotFoundException;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SliceManager {
    public static final String ACTION_REQUEST_SLICE_PERMISSION = "com.android.intent.action.REQUEST_SLICE_PERMISSION";
    public static final String CATEGORY_SLICE = "android.app.slice.category.SLICE";
    public static final String SLICE_METADATA_KEY = "android.metadata.SLICE_URI";
    private static final String TAG = "SliceManager";
    private final Context mContext;
    private final ISliceManager mService;
    private final IBinder mToken = new Binder();

    public SliceManager(Context context, Handler handler) throws ServiceNotFoundException {
        this.mContext = context;
        this.mService = Stub.asInterface(ServiceManager.getServiceOrThrow("slice"));
    }

    public void pinSlice(Uri uri, Set<SliceSpec> specs) {
        try {
            this.mService.pinSlice(this.mContext.getPackageName(), uri, (SliceSpec[]) specs.toArray(new SliceSpec[specs.size()]), this.mToken);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void pinSlice(Uri uri, List<SliceSpec> specs) {
        pinSlice(uri, new ArraySet(specs));
    }

    public void unpinSlice(Uri uri) {
        try {
            this.mService.unpinSlice(this.mContext.getPackageName(), uri, this.mToken);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean hasSliceAccess() {
        try {
            return this.mService.hasSliceAccess(this.mContext.getPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Set<SliceSpec> getPinnedSpecs(Uri uri) {
        try {
            return new ArraySet(Arrays.asList(this.mService.getPinnedSpecs(uri, this.mContext.getPackageName())));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<Uri> getPinnedSlices() {
        try {
            return Arrays.asList(this.mService.getPinnedSlices(this.mContext.getPackageName()));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Collection<Uri> getSliceDescendants(Uri uri) {
        ContentProviderClient provider;
        try {
            provider = this.mContext.getContentResolver().acquireUnstableContentProviderClient(uri);
            Bundle extras = new Bundle();
            extras.putParcelable(SliceProvider.EXTRA_BIND_URI, uri);
            ArrayList parcelableArrayList = provider.call(SliceProvider.METHOD_GET_DESCENDANTS, null, extras).getParcelableArrayList(SliceProvider.EXTRA_SLICE_DESCENDANTS);
            if (provider != null) {
                $closeResource(null, provider);
            }
            return parcelableArrayList;
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get slice descendants", e);
            return Collections.emptyList();
        } catch (Throwable th) {
            if (provider != null) {
                $closeResource(r2, provider);
            }
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

    public Slice bindSlice(Uri uri, Set<SliceSpec> supportedSpecs) {
        Throwable th;
        Throwable th2;
        Preconditions.checkNotNull(uri, DownloadManager.COLUMN_URI);
        try {
            ContentProviderClient provider = this.mContext.getContentResolver().acquireUnstableContentProviderClient(uri);
            if (provider == null) {
                try {
                    Log.w(TAG, String.format("Unknown URI: %s", new Object[]{uri}));
                    if (provider != null) {
                        $closeResource(null, provider);
                    }
                    return null;
                } catch (Throwable th22) {
                    Throwable th3 = th22;
                    th22 = th;
                    th = th3;
                }
            } else {
                Bundle extras = new Bundle();
                extras.putParcelable(SliceProvider.EXTRA_BIND_URI, uri);
                extras.putParcelableArrayList(SliceProvider.EXTRA_SUPPORTED_SPECS, new ArrayList(supportedSpecs));
                Bundle res = provider.call(SliceProvider.METHOD_SLICE, null, extras);
                Bundle.setDefusable(res, true);
                if (res == null) {
                    if (provider != null) {
                        $closeResource(null, provider);
                    }
                    return null;
                }
                Slice slice = (Slice) res.getParcelable("slice");
                if (provider != null) {
                    $closeResource(null, provider);
                }
                return slice;
            }
            if (provider != null) {
                $closeResource(th22, provider);
            }
            throw th;
        } catch (RemoteException e) {
            return null;
        }
    }

    @Deprecated
    public Slice bindSlice(Uri uri, List<SliceSpec> supportedSpecs) {
        return bindSlice(uri, new ArraySet(supportedSpecs));
    }

    public Uri mapIntentToUri(Intent intent) {
        ContentProviderClient provider;
        Throwable th;
        Throwable th2;
        ContentResolver resolver = this.mContext.getContentResolver();
        Uri staticUri = resolveStatic(intent, resolver);
        if (staticUri != null) {
            return staticUri;
        }
        String authority = getAuthority(intent);
        if (authority == null) {
            return null;
        }
        try {
            provider = resolver.acquireUnstableContentProviderClient(new Builder().scheme("content").authority(authority).build());
            if (provider == null) {
                try {
                    Log.w(TAG, String.format("Unknown URI: %s", new Object[]{uri}));
                    if (provider != null) {
                        $closeResource(null, provider);
                    }
                    return null;
                } catch (Throwable th22) {
                    Throwable th3 = th22;
                    th22 = th;
                    th = th3;
                }
            } else {
                Bundle extras = new Bundle();
                extras.putParcelable(SliceProvider.EXTRA_INTENT, intent);
                Bundle res = provider.call(SliceProvider.METHOD_MAP_ONLY_INTENT, null, extras);
                if (res == null) {
                    if (provider != null) {
                        $closeResource(null, provider);
                    }
                    return null;
                }
                Uri uri = (Uri) res.getParcelable("slice");
                if (provider != null) {
                    $closeResource(null, provider);
                }
                return uri;
            }
        } catch (RemoteException e) {
            return null;
        }
        if (provider != null) {
            $closeResource(th22, provider);
        }
        throw th;
    }

    private String getAuthority(Intent intent) {
        Intent queryIntent = new Intent(intent);
        if (!queryIntent.hasCategory(CATEGORY_SLICE)) {
            queryIntent.addCategory(CATEGORY_SLICE);
        }
        List<ResolveInfo> providers = this.mContext.getPackageManager().queryIntentContentProviders(queryIntent, 0);
        if (providers == null || providers.isEmpty()) {
            return null;
        }
        return ((ResolveInfo) providers.get(0)).providerInfo.authority;
    }

    private Uri resolveStatic(Intent intent, ContentResolver resolver) {
        Preconditions.checkNotNull(intent, "intent");
        boolean z = (intent.getComponent() == null && intent.getPackage() == null && intent.getData() == null) ? false : true;
        Preconditions.checkArgument(z, "Slice intent must be explicit %s", new Object[]{intent});
        Uri intentData = intent.getData();
        if (intentData != null && SliceProvider.SLICE_TYPE.equals(resolver.getType(intentData))) {
            return intentData;
        }
        ResolveInfo resolve = this.mContext.getPackageManager().resolveActivity(intent, 128);
        if (resolve == null || resolve.activityInfo == null || resolve.activityInfo.metaData == null || !resolve.activityInfo.metaData.containsKey(SLICE_METADATA_KEY)) {
            return null;
        }
        return Uri.parse(resolve.activityInfo.metaData.getString(SLICE_METADATA_KEY));
    }

    public Slice bindSlice(Intent intent, Set<SliceSpec> supportedSpecs) {
        ContentProviderClient provider;
        Throwable th;
        Throwable th2;
        Preconditions.checkNotNull(intent, "intent");
        boolean z = (intent.getComponent() == null && intent.getPackage() == null && intent.getData() == null) ? false : true;
        Preconditions.checkArgument(z, "Slice intent must be explicit %s", new Object[]{intent});
        ContentResolver resolver = this.mContext.getContentResolver();
        Uri staticUri = resolveStatic(intent, resolver);
        if (staticUri != null) {
            return bindSlice(staticUri, (Set) supportedSpecs);
        }
        String authority = getAuthority(intent);
        if (authority == null) {
            return null;
        }
        try {
            provider = resolver.acquireUnstableContentProviderClient(new Builder().scheme("content").authority(authority).build());
            if (provider == null) {
                try {
                    Log.w(TAG, String.format("Unknown URI: %s", new Object[]{uri}));
                    if (provider != null) {
                        $closeResource(null, provider);
                    }
                    return null;
                } catch (Throwable th22) {
                    Throwable th3 = th22;
                    th22 = th;
                    th = th3;
                }
            } else {
                Bundle extras = new Bundle();
                extras.putParcelable(SliceProvider.EXTRA_INTENT, intent);
                Bundle res = provider.call(SliceProvider.METHOD_MAP_INTENT, null, extras);
                if (res == null) {
                    if (provider != null) {
                        $closeResource(null, provider);
                    }
                    return null;
                }
                Slice slice = (Slice) res.getParcelable("slice");
                if (provider != null) {
                    $closeResource(null, provider);
                }
                return slice;
            }
        } catch (RemoteException e) {
            return null;
        }
        if (provider != null) {
            $closeResource(th22, provider);
        }
        throw th;
    }

    @Deprecated
    public Slice bindSlice(Intent intent, List<SliceSpec> supportedSpecs) {
        return bindSlice(intent, new ArraySet(supportedSpecs));
    }

    public int checkSlicePermission(Uri uri, int pid, int uid) {
        try {
            return this.mService.checkSlicePermission(uri, null, pid, uid, null);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void grantSlicePermission(String toPackage, Uri uri) {
        try {
            this.mService.grantSlicePermission(this.mContext.getPackageName(), toPackage, uri);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void revokeSlicePermission(String toPackage, Uri uri) {
        try {
            this.mService.revokeSlicePermission(this.mContext.getPackageName(), toPackage, uri);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void enforceSlicePermission(Uri uri, String pkg, int pid, int uid, String[] autoGrantPermissions) {
        try {
            if (!UserHandle.isSameApp(uid, Process.myUid())) {
                if (pkg == null) {
                    throw new SecurityException("No pkg specified");
                } else if (this.mService.checkSlicePermission(uri, pkg, pid, uid, autoGrantPermissions) == -1) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("User ");
                    stringBuilder.append(uid);
                    stringBuilder.append(" does not have slice permission for ");
                    stringBuilder.append(uri);
                    stringBuilder.append(".");
                    throw new SecurityException(stringBuilder.toString());
                }
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void grantPermissionFromUser(Uri uri, String pkg, boolean allSlices) {
        try {
            this.mService.grantPermissionFromUser(uri, pkg, this.mContext.getPackageName(), allSlices);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
