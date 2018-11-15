package com.android.server.security.securityprofile;

import org.json.JSONObject;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PolicyEngine$Ev97TXuQjn_50nSgWsrnm2ZJAmk implements RuleToFallthrough {
    private final /* synthetic */ PolicyEngine f$0;

    public /* synthetic */ -$$Lambda$PolicyEngine$Ev97TXuQjn_50nSgWsrnm2ZJAmk(PolicyEngine policyEngine) {
        this.f$0 = policyEngine;
    }

    public final BBDNode getFallthrough(JSONObject jSONObject) {
        return new BooleanNode(false);
    }
}
