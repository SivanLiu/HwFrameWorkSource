package huawei.android.security.securityprofile;

import android.app.ActivityThread;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import huawei.android.security.ISecurityProfileService;
import huawei.android.security.securityprofile.PolicyExtractor.PolicyNotFoundException;
import java.util.Arrays;
import java.util.List;

public class HwSignedInfo {
    public static final String BUNDLE_KEY_ADD_DOMAIN_POLICY = "addDomainPolicy";
    public static final String BUNDLE_KEY_APKPATH = "apkPath";
    public static final String BUNDLE_KEY_CONTENT_TYPE = "ContentType";
    public static final String BUNDLE_KEY_DIGEST_ALGORITHM = "digestAlgorithm";
    public static final String BUNDLE_KEY_DIGEST_BASE64DIGEST = "base64Digest";
    public static final String BUNDLE_KEY_DIGEST_SCHEME = "apkSignatureScheme";
    public static final String BUNDLE_KEY_LABEL = "pureAndroidLabel";
    public static final String BUNDLE_KEY_PACKAGENAME = "packageName";
    public static final String CONTENT_TYPE_APK_DIGEST_ONLY = "ApkDigestOnly";
    public static final List<String> HUAWEI_INSTALLERS = Arrays.asList(new String[]{"com.huawei.appmarket", "com.huawei.gamebox"});
    public static final String POLICY_LEBALS_GREEN = "GREEN";
    public static final String POLICY_LEBALS_NORMAL = "NORMAL";
    public static final int POLICY_OK = 0;
    public static final int POLICY_VERIFICATION_FAILED = 1;
    public static final String PREFIX_RESULT = "RESULT_";
    public static final String TAG = "HwSignedInfo";

    public static final String getPureAndroidLabelFromLabelList(List<String> labelsList) {
        if (labelsList == null || labelsList.size() <= 0) {
            return null;
        }
        if (labelsList.contains(POLICY_LEBALS_GREEN)) {
            return POLICY_LEBALS_GREEN;
        }
        if (labelsList.contains(POLICY_LEBALS_NORMAL)) {
            return POLICY_LEBALS_NORMAL;
        }
        return null;
    }

    public static final Bundle resloveHwSignedBundleInfo(String packageName, List<String> labelsList, ApkDigest apkDigest) {
        Bundle bundle = new Bundle();
        bundle.putString("packageName", packageName);
        bundle.putString(BUNDLE_KEY_LABEL, getPureAndroidLabelFromLabelList(labelsList));
        if (apkDigest != null) {
            bundle.putString(BUNDLE_KEY_DIGEST_SCHEME, apkDigest.apkSignatureScheme);
            bundle.putString(BUNDLE_KEY_DIGEST_ALGORITHM, apkDigest.digestAlgorithm);
            bundle.putString(BUNDLE_KEY_DIGEST_BASE64DIGEST, apkDigest.base64Digest);
        }
        return bundle;
    }

    private static boolean isInstalledByHuaweiAppMarket(ISecurityProfileService service, String packageName) {
        String str;
        StringBuilder stringBuilder;
        try {
            String installer = service.getInstallerPackageName(packageName);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(packageName);
            stringBuilder.append(",installer:");
            stringBuilder.append(installer);
            Log.d(str, stringBuilder.toString());
            if (HUAWEI_INSTALLERS.contains(installer)) {
                return true;
            }
        } catch (RemoteException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(packageName);
            stringBuilder.append(",check isInstalledByHuaweiAppMarket get RemoteException:");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
        return false;
    }

    private static boolean isInstalledAppCanGetHwSignedInfo(ISecurityProfileService sSecurityProfileService, String packageName) {
        try {
            if (sSecurityProfileService.isPackageSigned(packageName)) {
                return true;
            }
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(packageName);
            stringBuilder.append(",check isInstalledByHuaweiAppMarket get RemoteException:");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
        if (DigestMatcher.CALCULATE_APKDIGEST && isInstalledByHuaweiAppMarket(sSecurityProfileService, packageName)) {
            return true;
        }
        return false;
    }

    private static Bundle getInstalledHwSignedInfo(ISecurityProfileService sSecurityProfileService, String packageName) {
        String str;
        StringBuilder stringBuilder;
        try {
            List<String> labelsList = sSecurityProfileService.getLabels(packageName, null);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(packageName);
            stringBuilder.append(" labelsList = ");
            stringBuilder.append(labelsList);
            Log.d(str, stringBuilder.toString());
            return resloveHwSignedBundleInfo(packageName, labelsList, null);
        } catch (Exception e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getInstalledApkHwSignedInfo [");
            stringBuilder.append(packageName);
            stringBuilder.append("] occurs Exception:");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return resloveHwSignedBundleInfo(packageName, null, null);
        }
    }

    private static Bundle getInstalledHwSignedApkDigest(ISecurityProfileService sSecurityProfileService, String packageName) {
        String apkPath;
        try {
            apkPath = ActivityThread.getPackageManager().getApplicationInfo(packageName, 0, 0).sourceDir;
            ApkDigest apkDigest = null;
            try {
                apkDigest = PolicyExtractor.getDigest(packageName, PolicyExtractor.getPolicy(apkPath));
            } catch (PolicyNotFoundException e) {
                apkDigest = DigestMatcher.getApkDigest(apkPath);
            }
            return resloveHwSignedBundleInfo(packageName, null, apkDigest);
        } catch (Exception e2) {
            apkPath = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getInstalledHwSignedApkDigest [");
            stringBuilder.append(packageName);
            stringBuilder.append("] occurs Exception:");
            stringBuilder.append(e2.getMessage());
            Log.e(apkPath, stringBuilder.toString());
            return resloveHwSignedBundleInfo(packageName, null, null);
        }
    }

    private static Bundle getUnInstalledHwSignedApkDigest(ISecurityProfileService sSecurityProfileService, String packageName, String apkPath) {
        byte[] policyBlock = null;
        String str;
        StringBuilder stringBuilder;
        try {
            policyBlock = PolicyExtractor.getPolicy(apkPath);
            ApkDigest apkDigest = PolicyExtractor.getDigest(packageName, policyBlock);
            if (apkDigest == null) {
                Log.e(TAG, "get apkDigest is null!");
                return resloveHwSignedBundleInfo(packageName, null, null);
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("digest.base64Digest: ");
            stringBuilder2.append(apkDigest.base64Digest);
            Log.d(str2, stringBuilder2.toString());
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("digest.digestAlgorithm: ");
            stringBuilder2.append(apkDigest.digestAlgorithm);
            Log.d(str2, stringBuilder2.toString());
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("digest.apkSignatureScheme: ");
            stringBuilder2.append(apkDigest.apkSignatureScheme);
            Log.d(str2, stringBuilder2.toString());
            if (DigestMatcher.packageMatchesDigest(apkPath, apkDigest)) {
                int result = sSecurityProfileService.addDomainPolicy(policyBlock);
                if (result != 0) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(packageName);
                    stringBuilder2.append(" addDomainPolicy err, result = ");
                    stringBuilder2.append(result);
                    Log.e(str2, stringBuilder2.toString());
                    return resloveHwSignedBundleInfo(packageName, null, null);
                }
                List<String> labelsList = sSecurityProfileService.getLabels(packageName, apkDigest);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(packageName);
                stringBuilder.append(" labelsList = ");
                stringBuilder.append(labelsList);
                Log.d(str, stringBuilder.toString());
                return resloveHwSignedBundleInfo(packageName, labelsList, apkDigest);
            }
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(packageName);
            stringBuilder2.append(" Package digest did not match policy digest:");
            stringBuilder2.append(apkDigest.base64Digest);
            stringBuilder2.append(", apkPath:");
            stringBuilder2.append(apkPath);
            Log.e(str2, stringBuilder2.toString());
            return resloveHwSignedBundleInfo(packageName, null, null);
        } catch (Exception e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getUnInstalledHwSignedApkDigest [");
            stringBuilder.append(packageName);
            stringBuilder.append("] occurs Exception:");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return resloveHwSignedBundleInfo(packageName, null, null);
        }
    }

    public static Bundle getHwSignedInfo(ISecurityProfileService sSecurityProfileService, String packageName, Bundle extraParams) {
        if (packageName == null) {
            Log.e(TAG, "getHwSignedInfo err, packageName is null");
            return null;
        } else if (sSecurityProfileService == null) {
            return null;
        } else {
            String apkPath = null;
            String contentType = null;
            if (extraParams != null) {
                apkPath = extraParams.getString(BUNDLE_KEY_APKPATH, null);
                contentType = extraParams.getString(BUNDLE_KEY_CONTENT_TYPE, null);
            }
            if (apkPath != null) {
                return getUnInstalledHwSignedApkDigest(sSecurityProfileService, packageName, apkPath);
            }
            if (!isInstalledAppCanGetHwSignedInfo(sSecurityProfileService, packageName)) {
                return resloveHwSignedBundleInfo(packageName, null, null);
            }
            if (CONTENT_TYPE_APK_DIGEST_ONLY.equals(contentType)) {
                return getInstalledHwSignedApkDigest(sSecurityProfileService, packageName);
            }
            return getInstalledHwSignedInfo(sSecurityProfileService, packageName);
        }
    }

    public static Bundle setHwSignedInfoToSEAPP(ISecurityProfileService sSecurityProfileService, Bundle params) {
        if (params == null) {
            Log.e(TAG, "setHwSignedInfoToSEAPP params is null");
            return null;
        } else if (sSecurityProfileService == null) {
            Log.e(TAG, "setHwSignedInfoToSEAPP sSecurityProfileService is null");
            return null;
        } else {
            try {
                Bundle bundle = new Bundle();
                byte[] domainPolicy = params.getByteArray(BUNDLE_KEY_ADD_DOMAIN_POLICY);
                if (domainPolicy != null) {
                    bundle.putInt("RESULT_addDomainPolicy", sSecurityProfileService.addDomainPolicy(domainPolicy));
                }
                return bundle;
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setHwSignedInfoToSEAPP occurs Exception");
                stringBuilder.append(e.getMessage());
                Log.e(str, stringBuilder.toString());
                return null;
            }
        }
    }
}
