package android.app;

import android.app.UiAutomation.OnAccessibilityEventListener;
import android.view.accessibility.AccessibilityEvent;
import java.util.function.BiConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$GnVtsLTLDH5bZdtLeTd6cfwpgcs implements BiConsumer {
    public static final /* synthetic */ -$$Lambda$GnVtsLTLDH5bZdtLeTd6cfwpgcs INSTANCE = new -$$Lambda$GnVtsLTLDH5bZdtLeTd6cfwpgcs();

    private /* synthetic */ -$$Lambda$GnVtsLTLDH5bZdtLeTd6cfwpgcs() {
    }

    public final void accept(Object obj, Object obj2) {
        ((OnAccessibilityEventListener) obj).onAccessibilityEvent((AccessibilityEvent) obj2);
    }
}
