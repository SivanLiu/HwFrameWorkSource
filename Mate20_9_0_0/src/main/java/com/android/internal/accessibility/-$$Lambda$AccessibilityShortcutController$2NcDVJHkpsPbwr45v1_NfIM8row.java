package com.android.internal.accessibility;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.provider.Settings.Secure;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AccessibilityShortcutController$2NcDVJHkpsPbwr45v1_NfIM8row implements OnClickListener {
    private final /* synthetic */ AccessibilityShortcutController f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$AccessibilityShortcutController$2NcDVJHkpsPbwr45v1_NfIM8row(AccessibilityShortcutController accessibilityShortcutController, int i) {
        this.f$0 = accessibilityShortcutController;
        this.f$1 = i;
    }

    public final void onClick(DialogInterface dialogInterface, int i) {
        Secure.putStringForUser(this.f$0.mContext.getContentResolver(), "accessibility_shortcut_target_service", "", this.f$1);
    }
}
