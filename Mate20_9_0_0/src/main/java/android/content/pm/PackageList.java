package android.content.pm;

import android.content.pm.PackageManagerInternal.PackageListObserver;
import com.android.server.LocalServices;
import java.util.List;

public class PackageList implements PackageListObserver, AutoCloseable {
    private final List<String> mPackageNames;
    private final PackageListObserver mWrappedObserver;

    public PackageList(List<String> packageNames, PackageListObserver observer) {
        this.mPackageNames = packageNames;
        this.mWrappedObserver = observer;
    }

    public void onPackageAdded(String packageName) {
        if (this.mWrappedObserver != null) {
            this.mWrappedObserver.onPackageAdded(packageName);
        }
    }

    public void onPackageRemoved(String packageName) {
        if (this.mWrappedObserver != null) {
            this.mWrappedObserver.onPackageRemoved(packageName);
        }
    }

    public void close() throws Exception {
        ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).removePackageListObserver(this);
    }

    public List<String> getPackageNames() {
        return this.mPackageNames;
    }
}
