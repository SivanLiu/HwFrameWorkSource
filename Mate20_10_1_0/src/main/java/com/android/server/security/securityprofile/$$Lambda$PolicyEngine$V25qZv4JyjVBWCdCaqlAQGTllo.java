package com.android.server.security.securityprofile;

import com.android.server.security.securityprofile.PolicyEngine;
import org.json.JSONObject;

/* renamed from: com.android.server.security.securityprofile.-$$Lambda$PolicyEngine$V25qZv4JyjVBWC-dCaqlAQGTllo  reason: invalid class name */
/* compiled from: lambda */
public final /* synthetic */ class $$Lambda$PolicyEngine$V25qZv4JyjVBWCdCaqlAQGTllo implements PolicyEngine.RuleToKey {
    public static final /* synthetic */ $$Lambda$PolicyEngine$V25qZv4JyjVBWCdCaqlAQGTllo INSTANCE = new $$Lambda$PolicyEngine$V25qZv4JyjVBWCdCaqlAQGTllo();

    private /* synthetic */ $$Lambda$PolicyEngine$V25qZv4JyjVBWCdCaqlAQGTllo() {
    }

    @Override // com.android.server.security.securityprofile.PolicyEngine.RuleToKey
    public final String getKey(JSONObject jSONObject) {
        return PolicyEngine.lambda$addPackageRulesToLookup$1(jSONObject);
    }
}
