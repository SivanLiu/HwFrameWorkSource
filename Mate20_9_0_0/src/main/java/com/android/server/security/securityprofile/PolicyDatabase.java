package com.android.server.security.securityprofile;

import android.content.Context;
import android.util.Slog;
import huawei.android.security.securityprofile.ApkDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PolicyDatabase {
    public static final String DEFAULT_POLICY = "{\n\"overlay_labels\": {},\"domains\": {},\"rules\": [        {        \"decision\": \"deny\",        \"subject\": \"ANY\",        \"object\": \"Black\",        \"subsystem\": \"Intent\",        \"operation\": \"Send\"        },        {        \"decision\": \"allowif\",        \"subject\": \"ANY\",        \"object\": \"Red\",        \"subsystem\": \"Intent\",        \"operation\": \"Send\",        \"conditions\":  { \"state\": \"NoScreenRecording\" }        },        {        \"decision\": \"allowafter\",        \"subject\": \"ANY\",        \"object\": \"Red\",        \"subsystem\": \"Intent\",        \"operation\": \"Send\",        \"conditions\":  { \"action\": { \"name\": \"StopScreenRecording\" },              \"timeout\": 1000,              \"state\": \"NoScreenRecording\"            }         },        {        \"decision\": \"deny\",        \"subject\": \"ANY\",        \"object\": \"Red\",        \"subsystem\": \"Intent\",        \"operation\": \"Send\"        },        {        \"decision\": \"deny\",        \"subject\": \"ANY\",        \"object\": \"Red\",        \"subsystem\": \"MediaProjection\",        \"operation\": \"Record\"        },        {        \"decision\": \"allow\",        \"subject\": \"ANY\",        \"object\": \"ANY\",        \"subsystem\": \"MediaProjection\",        \"operation\": \"Record\"        },        {        \"decision\": \"allow\",        \"subject\": \"ANY\",        \"object\": \"ANY\",        \"subsystem\": \"Intent\",        \"operation\": \"Send\"        }]}";
    private static final String TAG = "SecurityProfileService";
    private boolean isNewVersionFirstBoot = false;
    private JSONObject mActivePolicy;
    private PolicyStorage mActiveStorage;
    private Context mContext;
    private PolicyStorage mDatabaseStorage;
    private JSONObject mPolicyDatabase;

    interface DigestHelper {
        boolean matches(ApkDigest apkDigest);
    }

    PolicyDatabase(Context context) {
        this.mContext = context;
        this.mDatabaseStorage = new PolicyStorage("/data/system/securityprofiledb.json", DEFAULT_POLICY);
        this.mPolicyDatabase = this.mDatabaseStorage.readDatabase();
        this.mActiveStorage = new PolicyStorage("/data/system/securityprofile.json", null);
        this.mActivePolicy = this.mActiveStorage.readDatabase();
        if (this.mActivePolicy == null) {
            this.isNewVersionFirstBoot = true;
            rebuildPolicy();
            importOldBlackListDatabase();
        }
    }

    protected boolean isNewVersionFirstBoot() {
        return this.isNewVersionFirstBoot;
    }

    private void writePolicyDatabase() {
        this.mDatabaseStorage.writeDatabase(this.mPolicyDatabase);
    }

    private void writeActivePolicy() {
        this.mActiveStorage.writeDatabase(this.mActivePolicy);
    }

    private boolean samePolicyTarget(JSONObject p1, JSONObject p2) {
        if (p1 == null && p2 == null) {
            return true;
        }
        if (p1 == null || p2 == null) {
            return false;
        }
        if (p1.optJSONObject("apk_digest").optString("digest").equals(p2.optJSONObject("apk_digest").optString("digest"))) {
            return true;
        }
        return false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:35:0x00d2 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x00cc A:{Catch:{ JSONException -> 0x00e5 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addPolicy(JSONObject policy) {
        JSONObject jSONObject = policy;
        if (jSONObject.optInt("policy_language_version", 1) == 1) {
            HashSet updatedDomains = new HashSet();
            JSONObject domains = jSONObject.optJSONObject("domains");
            try {
                Iterator<String> iter = domains.keys();
                while (iter.hasNext()) {
                    String domain = (String) iter.next();
                    JSONArray domainPolicies = domains.optJSONArray(domain);
                    JSONArray existingDomainPolicies = this.mPolicyDatabase.optJSONObject("domains").optJSONArray(domain);
                    if (existingDomainPolicies == null) {
                        existingDomainPolicies = new JSONArray();
                        this.mPolicyDatabase.optJSONObject("domains").put(domain, existingDomainPolicies);
                    }
                    int i = 0;
                    int i2 = 0;
                    while (i2 < domainPolicies.length()) {
                        JSONObject domainPolicy = domainPolicies.optJSONObject(i2);
                        boolean matchedOldPolicy = false;
                        int j = i;
                        while (j < existingDomainPolicies.length()) {
                            JSONObject existingDomainPolicy = existingDomainPolicies.optJSONObject(j);
                            if (samePolicyTarget(domainPolicy, existingDomainPolicy)) {
                                matchedOldPolicy = true;
                                if (domainPolicy.optInt("policy_version", i) > existingDomainPolicy.optInt("policy_version", i)) {
                                    existingDomainPolicies.put(j, domainPolicy);
                                    updatedDomains.add(domain);
                                } else {
                                    String str = TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("this policy_version is old than exit policy do not need update! domain:");
                                    stringBuilder.append(domain);
                                    Slog.i(str, stringBuilder.toString());
                                    String str2 = TAG;
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("domainPolicy:");
                                    stringBuilder2.append(domainPolicy);
                                    Slog.i(str2, stringBuilder2.toString());
                                    str2 = TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("existingDomainPolicy:");
                                    stringBuilder2.append(existingDomainPolicy);
                                    Slog.i(str2, stringBuilder2.toString());
                                }
                                if (matchedOldPolicy) {
                                    existingDomainPolicies.put(domainPolicy);
                                    updatedDomains.add(domain);
                                }
                                i2++;
                                i = 0;
                            } else {
                                j++;
                                i = 0;
                            }
                        }
                        if (matchedOldPolicy) {
                        }
                        i2++;
                        i = 0;
                    }
                }
                writePolicyDatabase();
                updateDomainPolicies(new ArrayList(updatedDomains));
            } catch (JSONException e) {
                Slog.e(TAG, e.getMessage());
            }
        }
    }

    public JSONObject getPolicy() {
        return this.mActivePolicy;
    }

    private JSONObject deepCopyJSONObject(JSONObject o) {
        try {
            return new JSONObject(o.toString());
        } catch (JSONException e) {
            Slog.e(TAG, e.getMessage());
            return new JSONObject();
        }
    }

    private JSONObject emptyDomainPolicy() {
        try {
            JSONObject result = new JSONObject();
            result.put("labels", new JSONArray());
            return result;
        } catch (JSONException e) {
            Slog.e(TAG, e.getMessage());
            return new JSONObject();
        }
    }

    int indexOfJSONArray(JSONArray array, String value) {
        for (int i = 0; i < array.length(); i++) {
            if (value.equals(array.optString(i))) {
                return i;
            }
        }
        return -1;
    }

    public void removeLabel(String label) {
        try {
            List<String> packageList = new ArrayList();
            JSONObject overlayLabels = this.mPolicyDatabase.getJSONObject("overlay_labels");
            String str;
            StringBuilder stringBuilder;
            if (overlayLabels == null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("overlayLabels is null, removeLabel must return,label:");
                stringBuilder.append(label);
                Slog.e(str, stringBuilder.toString());
                return;
            }
            Iterator<String> iter = overlayLabels.keys();
            while (iter.hasNext()) {
                packageList.add((String) iter.next());
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("removeLabel all packageList:");
            stringBuilder.append(packageList);
            Slog.i(str, stringBuilder.toString());
            removeLabel(packageList, label);
        } catch (Exception e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("removeLabel all err:");
            stringBuilder2.append(e.getMessage());
            Slog.e(str2, stringBuilder2.toString());
        }
    }

    public void addLabel(List<String> packageList, String label) {
        try {
            for (String packageName : packageList) {
                JSONArray packageOverlayLabels = this.mPolicyDatabase.getJSONObject("overlay_labels").optJSONArray(packageName);
                if (packageOverlayLabels == null) {
                    packageOverlayLabels = new JSONArray();
                    this.mPolicyDatabase.getJSONObject("overlay_labels").put(packageName, packageOverlayLabels);
                }
                if (indexOfJSONArray(packageOverlayLabels, label) == -1) {
                    packageOverlayLabels.put(label);
                }
            }
            writePolicyDatabase();
            updateDomainPolicies(packageList);
        } catch (JSONException e) {
            Slog.e(TAG, e.getMessage());
        }
    }

    public void removeLabel(List<String> packageList, String label) {
        try {
            JSONObject overlayLabels = this.mPolicyDatabase.getJSONObject("overlay_labels");
            for (String packageName : packageList) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("removeLabel:");
                stringBuilder.append(packageName);
                Slog.d(str, stringBuilder.toString());
                JSONArray packageOverlayLabels = overlayLabels.optJSONArray(packageName);
                if (packageOverlayLabels != null) {
                    int idx = indexOfJSONArray(packageOverlayLabels, label);
                    if (idx != -1) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("removeLabel:");
                        stringBuilder2.append(packageName);
                        stringBuilder2.append(",label:");
                        stringBuilder2.append(label);
                        Slog.d(str2, stringBuilder2.toString());
                        packageOverlayLabels.remove(idx);
                        if (packageOverlayLabels.length() == 0) {
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("removeLabel:");
                            stringBuilder2.append(packageName);
                            stringBuilder2.append(",has no overlay_labels left,remove package name from overlay_labels");
                            Slog.d(str2, stringBuilder2.toString());
                            overlayLabels.remove(packageName);
                        }
                    }
                }
            }
            writePolicyDatabase();
            updateDomainPolicies(packageList);
        } catch (JSONException e) {
            Slog.e(TAG, e.getMessage());
        }
    }

    public void updatePackageInformation(String packageName) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updatePackageInformation,packageName:");
        stringBuilder.append(packageName);
        Slog.d(str, stringBuilder.toString());
        updateDomainPolicies(Arrays.asList(new String[]{packageName}));
        updatePackageSigningInfo(packageName);
        writeActivePolicy();
    }

    /* JADX WARNING: Removed duplicated region for block: B:33:0x0114 A:{LOOP_START, PHI: r2 , LOOP:1: B:33:0x0114->B:35:0x011a} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private JSONObject resolveActiveDomainPolicy(String packageName, DigestHelper digestHelper) {
        String str;
        StringBuilder stringBuilder;
        JSONObject selectedPolicy;
        JSONArray domainOverlayLabels;
        String str2;
        JSONObject domains = this.mPolicyDatabase.optJSONObject("domains");
        if (domains == null) {
            try {
                this.mPolicyDatabase.put("domains", new JSONObject());
            } catch (JSONException e) {
                Slog.e(TAG, e.getMessage());
                return emptyDomainPolicy();
            }
        }
        int i = 0;
        if (domains != null) {
            try {
                if (domains.has(packageName)) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("resolveActiveDomainPolicy domain has packageName ");
                    stringBuilder.append(packageName);
                    Slog.d(str, stringBuilder.toString());
                    JSONObject latestMatch = null;
                    JSONArray domainPolicies = domains.optJSONArray(packageName);
                    String str3 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("resolveActiveDomainPolicy potential policies ");
                    stringBuilder2.append(String.valueOf(domainPolicies.length()));
                    Slog.d(str3, stringBuilder2.toString());
                    int latestVersion = 0;
                    for (int i2 = 0; i2 < domainPolicies.length(); i2++) {
                        JSONObject domainPolicy = domainPolicies.optJSONObject(i2);
                        String str4 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("domainPolicy candidate");
                        stringBuilder3.append(domainPolicy.toString());
                        Slog.d(str4, stringBuilder3.toString());
                        JSONObject digest = domainPolicy.optJSONObject("apk_digest");
                        if (digest == null || digestHelper.matches(jsonToApkDigest(digest))) {
                            Slog.d(TAG, "resolveActiveDomainPolicy match");
                            int version = domainPolicy.optInt("policy_version", 0);
                            if (version > latestVersion) {
                                latestMatch = domainPolicy;
                                latestVersion = version;
                            }
                        }
                    }
                    selectedPolicy = latestMatch != null ? deepCopyJSONObject(latestMatch) : emptyDomainPolicy();
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("selected1 policy:");
                    stringBuilder.append(selectedPolicy.toString());
                    Slog.d(str, stringBuilder.toString());
                    domainOverlayLabels = this.mPolicyDatabase.optJSONObject("overlay_labels").optJSONArray(packageName);
                    if (domainOverlayLabels != null) {
                        while (i < domainOverlayLabels.length()) {
                            selectedPolicy.optJSONArray("labels").put(domainOverlayLabels.optString(i));
                            i++;
                        }
                    }
                    str2 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("selected2 policy:");
                    stringBuilder.append(selectedPolicy.toString());
                    Slog.d(str2, stringBuilder.toString());
                    return selectedPolicy;
                }
            } catch (Exception e2) {
                String str5 = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("rules err:");
                stringBuilder4.append(e2.getMessage());
                Slog.e(str5, stringBuilder4.toString());
                selectedPolicy = emptyDomainPolicy();
            }
        }
        selectedPolicy = emptyDomainPolicy();
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("selected1 policy:");
        stringBuilder.append(selectedPolicy.toString());
        Slog.d(str, stringBuilder.toString());
        domainOverlayLabels = this.mPolicyDatabase.optJSONObject("overlay_labels").optJSONArray(packageName);
        if (domainOverlayLabels != null) {
        }
        str2 = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("selected2 policy:");
        stringBuilder.append(selectedPolicy.toString());
        Slog.d(str2, stringBuilder.toString());
        return selectedPolicy;
    }

    private boolean isEmptyDomainPolicy(JSONObject policy) {
        if (policy != null) {
            return policy.optJSONArray("labels").length() == 0 && policy.length() == 1;
        } else {
            return true;
        }
    }

    private void resolveActiveDomainPolicies(List<String> packages, JSONObject resultDomains) throws JSONException {
        for (String packageName : packages) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("resolveActiveDomainPolicies packageName ");
            stringBuilder.append(packageName);
            Slog.d(str, stringBuilder.toString());
            String str2;
            try {
                str = SecurityProfileUtils.getInstalledApkPath(packageName, this.mContext);
                if (str == null) {
                    Slog.e(TAG, "resolveActiveDomainPolicies packagePath is null");
                    resultDomains.put(packageName, null);
                } else {
                    JSONObject selectedPolicy = resolveActiveDomainPolicy(packageName, new -$$Lambda$PolicyDatabase$LwcoExQ65OK6nDlpDPiI3sSHA2E(str));
                    if (isEmptyDomainPolicy(selectedPolicy)) {
                        resultDomains.put(packageName, null);
                    } else {
                        str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("resolveActiveDomainPolicies packageName:");
                        stringBuilder2.append(packageName);
                        stringBuilder2.append(", selectedPolicy:");
                        stringBuilder2.append(selectedPolicy);
                        Slog.d(str2, stringBuilder2.toString());
                        resultDomains.put(packageName, selectedPolicy);
                    }
                }
            } catch (Exception e) {
                str2 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("resolveActiveDomainPolicies err:");
                stringBuilder3.append(e.getMessage());
                Slog.e(str2, stringBuilder3.toString());
            }
        }
    }

    private ApkDigest jsonToApkDigest(JSONObject json) {
        return new ApkDigest(json.optString("signature_scheme", "v2"), json.optString("digest_algorithm", "SHA-256"), json.optString("digest"));
    }

    private JSONObject getActiveDomainPolicy(String packageName) {
        JSONObject activeDomainPolicy = this.mActivePolicy.optJSONObject("domains").optJSONObject(packageName);
        if (activeDomainPolicy == null) {
            return emptyDomainPolicy();
        }
        return activeDomainPolicy;
    }

    public List<String> getLabels(String packageName, ApkDigest digest) {
        JSONObject activeDomainPolicy;
        if (digest != null) {
            activeDomainPolicy = resolveActiveDomainPolicy(packageName, new -$$Lambda$PolicyDatabase$BBm2hUsWXixteFSLn4sckREq5z4(digest));
        } else {
            activeDomainPolicy = getActiveDomainPolicy(packageName);
        }
        JSONArray labels = activeDomainPolicy.optJSONArray("labels");
        List<String> result = new ArrayList();
        for (int i = 0; i < labels.length(); i++) {
            result.add(labels.optString(i));
        }
        return result;
    }

    public void updateDomainPolicies(List<String> packageList) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateDomainPolicies,packageList:");
        stringBuilder.append(packageList);
        Slog.d(str, stringBuilder.toString());
        try {
            resolveActiveDomainPolicies(packageList, this.mActivePolicy.optJSONObject("domains"));
            writeActivePolicy();
        } catch (JSONException e) {
            Slog.e(TAG, e.getMessage());
        }
    }

    private void importOldBlackListDatabase() {
        addLabel(B200DatabaseImporter.getBlackListAndDeleteDatabase(this.mContext), "Black");
    }

    public boolean isPackageSigned(String packageName) {
        try {
            return this.mActivePolicy.optJSONObject("signedPackages").optBoolean(packageName, false);
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isPackageSigned err:");
            stringBuilder.append(e.getMessage());
            Slog.e(str, stringBuilder.toString());
            return false;
        }
    }

    protected void setPackageSigned(String packageName, boolean isPackageSigned) {
        if (isPackageSigned) {
            try {
                this.mActivePolicy.optJSONObject("signedPackages").put(packageName, isPackageSigned);
                return;
            } catch (JSONException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setPackageSigned json not found err:");
                stringBuilder.append(e.getMessage());
                Slog.e(str, stringBuilder.toString());
                return;
            }
        }
        this.mActivePolicy.optJSONObject("signedPackages").remove(packageName);
    }

    private void updatePackageSigningInfo(String packageName) {
        String str;
        StringBuilder stringBuilder;
        try {
            String packagePath = SecurityProfileUtils.getInstalledApkPath(packageName, this.mContext);
            if (packagePath == null) {
                this.mActivePolicy.optJSONObject("signedPackages").remove(packageName);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("update PackageSigned Info Action:remove, packageName:");
                stringBuilder.append(packageName);
                Slog.i(str, stringBuilder.toString());
                return;
            }
            boolean hasValidPolicy = PolicyVerifier.packageHasValidPolicy(packageName, packagePath);
            if (hasValidPolicy) {
                this.mActivePolicy.optJSONObject("signedPackages").put(packageName, hasValidPolicy);
            } else {
                this.mActivePolicy.optJSONObject("signedPackages").remove(packageName);
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("update PackageSigned Info Action:put, packageName:");
            stringBuilder.append(packageName);
            stringBuilder.append(",hasValidPolicy:");
            stringBuilder.append(hasValidPolicy);
            Slog.i(str, stringBuilder.toString());
        } catch (JSONException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("update PackageSigned Info json not found err:");
            stringBuilder.append(e.getMessage());
            Slog.e(str, stringBuilder.toString());
        } catch (Exception e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("update PackageSigned Info err:");
            stringBuilder.append(e2.getMessage());
            Slog.e(str, stringBuilder.toString());
        }
    }

    private void rebuildPolicy() {
        try {
            Slog.d(TAG, "rebuildPolicy");
            List<String> installedPackages = SecurityProfileUtils.getInstalledPackages(this.mContext);
            JSONObject result = deepCopyJSONObject(this.mPolicyDatabase);
            JSONObject resultDomains = new JSONObject();
            result.put("domains", resultDomains);
            resolveActiveDomainPolicies(installedPackages, resultDomains);
            result.put("signedPackages", new JSONObject());
            this.mActivePolicy = result;
            for (String packageName : installedPackages) {
                updatePackageSigningInfo(packageName);
            }
            writeActivePolicy();
        } catch (JSONException e) {
            Slog.e(TAG, e.getMessage());
        }
    }
}
