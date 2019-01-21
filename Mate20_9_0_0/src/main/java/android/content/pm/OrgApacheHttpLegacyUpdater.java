package android.content.pm;

import android.content.pm.PackageParser.Package;
import com.android.internal.annotations.VisibleForTesting;

@VisibleForTesting
public class OrgApacheHttpLegacyUpdater extends PackageSharedLibraryUpdater {
    public void updatePackage(Package pkg) {
        if (apkTargetsApiLevelLessThanOrEqualToOMR1(pkg)) {
            prefixRequiredLibrary(pkg, "org.apache.http.legacy");
        }
    }
}
