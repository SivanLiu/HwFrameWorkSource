package com.android.server.security.securityprofile;

import com.android.server.security.securityprofile.PolicyEngine;
import org.json.JSONObject;

/* renamed from: com.android.server.security.securityprofile.-$$Lambda$PolicyEngine$yZiY9YV_xtVf1FogXPsMB6qTaTM  reason: invalid class name */
/* compiled from: lambda */
public final /* synthetic */ class $$Lambda$PolicyEngine$yZiY9YV_xtVf1FogXPsMB6qTaTM implements PolicyEngine.RuleToKey {
    public static final /* synthetic */ $$Lambda$PolicyEngine$yZiY9YV_xtVf1FogXPsMB6qTaTM INSTANCE = new $$Lambda$PolicyEngine$yZiY9YV_xtVf1FogXPsMB6qTaTM();

    private /* synthetic */ $$Lambda$PolicyEngine$yZiY9YV_xtVf1FogXPsMB6qTaTM() {
    }

    @Override // com.android.server.security.securityprofile.PolicyEngine.RuleToKey
    public final String getKey(JSONObject jSONObject) {
        return PolicyEngine.lambda$addBaseRulesToLookup$3(jSONObject);
    }
}
