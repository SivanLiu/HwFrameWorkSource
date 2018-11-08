package android.content.pm;

import android.content.pm.PackageParser.Package;
import com.android.internal.util.ArrayUtils;
import java.util.ArrayList;

public class PackageBackwardCompatibility {
    private static final String ANDROID_TEST_MOCK = "android.test.mock";
    private static final String ANDROID_TEST_RUNNER = "android.test.runner";

    public static void modifySharedLibraries(Package pkg) {
        ArrayList<String> usesLibraries = pkg.usesLibraries;
        ArrayList<String> usesOptionalLibraries = pkg.usesOptionalLibraries;
        usesLibraries = orgApacheHttpLegacy(usesLibraries);
        usesOptionalLibraries = orgApacheHttpLegacy(usesOptionalLibraries);
        int androidTestMockPresent;
        if (ArrayUtils.contains(usesLibraries, ANDROID_TEST_MOCK)) {
            androidTestMockPresent = 1;
        } else {
            androidTestMockPresent = ArrayUtils.contains(usesOptionalLibraries, ANDROID_TEST_MOCK);
        }
        if (ArrayUtils.contains(usesLibraries, ANDROID_TEST_RUNNER) && (r0 ^ 1) != 0) {
            usesLibraries.add(ANDROID_TEST_MOCK);
        }
        if (ArrayUtils.contains(usesOptionalLibraries, ANDROID_TEST_RUNNER) && (r0 ^ 1) != 0) {
            usesOptionalLibraries.add(ANDROID_TEST_MOCK);
        }
        pkg.usesLibraries = usesLibraries;
        pkg.usesOptionalLibraries = usesOptionalLibraries;
    }

    private static ArrayList<String> orgApacheHttpLegacy(ArrayList<String> libraries) {
        return ArrayUtils.remove(libraries, "org.apache.http.legacy");
    }
}
