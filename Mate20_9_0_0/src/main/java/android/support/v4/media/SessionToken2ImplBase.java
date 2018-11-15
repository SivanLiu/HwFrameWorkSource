package android.support.v4.media;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.app.BundleCompat;
import android.support.v4.media.IMediaSession2.Stub;
import android.text.TextUtils;
import java.util.List;

final class SessionToken2ImplBase implements SupportLibraryImpl {
    private final ComponentName mComponentName;
    private final IMediaSession2 mISession2;
    private final String mPackageName;
    private final String mServiceName;
    private final String mSessionId;
    private final int mType;
    private final int mUid;

    SessionToken2ImplBase(@NonNull Context context, @NonNull ComponentName serviceComponent) {
        this(context, serviceComponent, -1);
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    SessionToken2ImplBase(@NonNull Context context, @NonNull ComponentName serviceComponent, int uid) {
        StringBuilder stringBuilder;
        if (serviceComponent != null) {
            this.mComponentName = serviceComponent;
            this.mPackageName = serviceComponent.getPackageName();
            this.mServiceName = serviceComponent.getClassName();
            PackageManager manager = context.getPackageManager();
            if (uid == -1) {
                try {
                    uid = manager.getApplicationInfo(this.mPackageName, 0).uid;
                } catch (NameNotFoundException e) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Cannot find package ");
                    stringBuilder.append(this.mPackageName);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            this.mUid = uid;
            String sessionId = getSessionIdFromService(manager, MediaLibraryService2.SERVICE_INTERFACE, serviceComponent);
            if (sessionId != null) {
                this.mSessionId = sessionId;
                this.mType = 2;
            } else {
                this.mSessionId = getSessionIdFromService(manager, MediaSessionService2.SERVICE_INTERFACE, serviceComponent);
                this.mType = 1;
            }
            if (this.mSessionId != null) {
                this.mISession2 = null;
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("service ");
            stringBuilder.append(this.mServiceName);
            stringBuilder.append(" doesn't implement");
            stringBuilder.append(" session service nor library service. Use service's full name.");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        throw new IllegalArgumentException("serviceComponent shouldn't be null");
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    SessionToken2ImplBase(int uid, int type, String packageName, String serviceName, String sessionId, IMediaSession2 iSession2) {
        this.mUid = uid;
        this.mType = type;
        this.mPackageName = packageName;
        this.mServiceName = serviceName;
        this.mComponentName = this.mType == 0 ? null : new ComponentName(packageName, serviceName);
        this.mSessionId = sessionId;
        this.mISession2 = iSession2;
    }

    public int hashCode() {
        return this.mType + (31 * (this.mUid + ((this.mPackageName.hashCode() + ((this.mSessionId.hashCode() + ((this.mServiceName != null ? this.mServiceName.hashCode() : 0) * 31)) * 31)) * 31)));
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (!(obj instanceof SessionToken2ImplBase)) {
            return false;
        }
        SessionToken2ImplBase other = (SessionToken2ImplBase) obj;
        if (this.mUid == other.mUid && TextUtils.equals(this.mPackageName, other.mPackageName) && TextUtils.equals(this.mServiceName, other.mServiceName) && TextUtils.equals(this.mSessionId, other.mSessionId) && this.mType == other.mType && sessionBinderEquals(this.mISession2, other.mISession2)) {
            z = true;
        }
        return z;
    }

    private boolean sessionBinderEquals(IMediaSession2 a, IMediaSession2 b) {
        if (a != null && b != null) {
            return a.asBinder().equals(b.asBinder());
        }
        return a == b;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SessionToken {pkg=");
        stringBuilder.append(this.mPackageName);
        stringBuilder.append(" id=");
        stringBuilder.append(this.mSessionId);
        stringBuilder.append(" type=");
        stringBuilder.append(this.mType);
        stringBuilder.append(" service=");
        stringBuilder.append(this.mServiceName);
        stringBuilder.append(" IMediaSession2=");
        stringBuilder.append(this.mISession2);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public int getUid() {
        return this.mUid;
    }

    @NonNull
    public String getPackageName() {
        return this.mPackageName;
    }

    @Nullable
    public String getServiceName() {
        return this.mServiceName;
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public ComponentName getComponentName() {
        return this.mComponentName;
    }

    public String getSessionId() {
        return this.mSessionId;
    }

    public int getType() {
        return this.mType;
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt("android.media.token.uid", this.mUid);
        bundle.putString("android.media.token.package_name", this.mPackageName);
        bundle.putString("android.media.token.service_name", this.mServiceName);
        bundle.putString("android.media.token.session_id", this.mSessionId);
        bundle.putInt("android.media.token.type", this.mType);
        if (this.mISession2 != null) {
            BundleCompat.putBinder(bundle, "android.media.token.session_binder", this.mISession2.asBinder());
        }
        return bundle;
    }

    public Object getBinder() {
        return this.mISession2 == null ? null : this.mISession2.asBinder();
    }

    public static SessionToken2ImplBase fromBundle(@NonNull Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        int uid = bundle.getInt("android.media.token.uid");
        int type = bundle.getInt("android.media.token.type", -1);
        String packageName = bundle.getString("android.media.token.package_name");
        String serviceName = bundle.getString("android.media.token.service_name");
        String sessionId = bundle.getString("android.media.token.session_id");
        IMediaSession2 iSession2 = Stub.asInterface(BundleCompat.getBinder(bundle, "android.media.token.session_binder"));
        switch (type) {
            case 0:
                if (iSession2 == null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected token for session, binder=");
                    stringBuilder.append(iSession2);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
                break;
            case 1:
            case 2:
                if (TextUtils.isEmpty(serviceName)) {
                    throw new IllegalArgumentException("Session service needs service name");
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid type");
        }
        if (!TextUtils.isEmpty(packageName) && sessionId != null) {
            return new SessionToken2ImplBase(uid, type, packageName, serviceName, sessionId, iSession2);
        }
        throw new IllegalArgumentException("Package name nor ID cannot be null.");
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public static String getSessionId(ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            return null;
        }
        if (resolveInfo.serviceInfo.metaData == null) {
            return "";
        }
        return resolveInfo.serviceInfo.metaData.getString(MediaSessionService2.SERVICE_META_DATA, "");
    }

    private static String getSessionIdFromService(PackageManager manager, String serviceInterface, ComponentName serviceComponent) {
        Intent serviceIntent = new Intent(serviceInterface);
        serviceIntent.setPackage(serviceComponent.getPackageName());
        List<ResolveInfo> list = manager.queryIntentServices(serviceIntent, 128);
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                ResolveInfo resolveInfo = (ResolveInfo) list.get(i);
                if (resolveInfo != null && resolveInfo.serviceInfo != null && TextUtils.equals(resolveInfo.serviceInfo.name, serviceComponent.getClassName())) {
                    return getSessionId(resolveInfo);
                }
            }
        }
        return null;
    }
}
