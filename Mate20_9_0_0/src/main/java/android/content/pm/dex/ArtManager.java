package android.content.pm.dex;

import android.annotation.SystemApi;
import android.content.Context;
import android.content.pm.dex.ISnapshotRuntimeProfileCallback.Stub;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Slog;
import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

@SystemApi
public class ArtManager {
    public static final int PROFILE_APPS = 0;
    public static final int PROFILE_BOOT_IMAGE = 1;
    public static final int SNAPSHOT_FAILED_CODE_PATH_NOT_FOUND = 1;
    public static final int SNAPSHOT_FAILED_INTERNAL_ERROR = 2;
    public static final int SNAPSHOT_FAILED_PACKAGE_NOT_FOUND = 0;
    private static final String TAG = "ArtManager";
    private final IArtManager mArtManager;
    private final Context mContext;

    @Retention(RetentionPolicy.SOURCE)
    public @interface ProfileType {
    }

    public static abstract class SnapshotRuntimeProfileCallback {
        public abstract void onError(int i);

        public abstract void onSuccess(ParcelFileDescriptor parcelFileDescriptor);
    }

    private static class SnapshotRuntimeProfileCallbackDelegate extends Stub {
        private final SnapshotRuntimeProfileCallback mCallback;
        private final Executor mExecutor;

        private SnapshotRuntimeProfileCallbackDelegate(SnapshotRuntimeProfileCallback callback, Executor executor) {
            this.mCallback = callback;
            this.mExecutor = executor;
        }

        public void onSuccess(ParcelFileDescriptor profileReadFd) {
            this.mExecutor.execute(new -$$Lambda$ArtManager$SnapshotRuntimeProfileCallbackDelegate$OOdGv4iFxuVpH2kzFMr8KwX3X8s(this, profileReadFd));
        }

        public void onError(int errCode) {
            this.mExecutor.execute(new -$$Lambda$ArtManager$SnapshotRuntimeProfileCallbackDelegate$m2Wpsf6LxhWt_1tS6tQt3B8QcGo(this, errCode));
        }
    }

    public ArtManager(Context context, IArtManager manager) {
        this.mContext = context;
        this.mArtManager = manager;
    }

    public void snapshotRuntimeProfile(int profileType, String packageName, String codePath, Executor executor, SnapshotRuntimeProfileCallback callback) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Requesting profile snapshot for ");
        stringBuilder.append(packageName);
        stringBuilder.append(":");
        stringBuilder.append(codePath);
        Slog.d(str, stringBuilder.toString());
        try {
            this.mArtManager.snapshotRuntimeProfile(profileType, packageName, codePath, new SnapshotRuntimeProfileCallbackDelegate(callback, executor), this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    public boolean isRuntimeProfilingEnabled(int profileType) {
        try {
            return this.mArtManager.isRuntimeProfilingEnabled(profileType, this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    public static String getProfileName(String splitName) {
        if (splitName == null) {
            return "primary.prof";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(splitName);
        stringBuilder.append(".split.prof");
        return stringBuilder.toString();
    }

    public static String getCurrentProfilePath(String packageName, int userId, String splitName) {
        return new File(Environment.getDataProfilesDePackageDirectory(userId, packageName), getProfileName(splitName)).getAbsolutePath();
    }

    public static File getProfileSnapshotFileForName(String packageName, String profileName) {
        File profileDir = Environment.getDataRefProfilesDePackageDirectory(packageName);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(profileName);
        stringBuilder.append(".snapshot");
        return new File(profileDir, stringBuilder.toString());
    }
}
