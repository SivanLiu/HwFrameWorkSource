package com.android.server.security.securityprofile;

import org.json.JSONObject;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PolicyEngine$x44nI3x7YQL8Bb-A3B4e32udp1g implements RuleToFallthrough {
    private final /* synthetic */ PolicyEngine f$0;

    public /* synthetic */ -$$Lambda$PolicyEngine$x44nI3x7YQL8Bb-A3B4e32udp1g(PolicyEngine policyEngine) {
        this.f$0 = policyEngine;
    }

    public final BBDNode getFallthrough(JSONObject jSONObject) {
        return PolicyEngine.lambda$createLookup$2(this.f$0, jSONObject);
    }
}
