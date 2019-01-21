package android.os;

import java.util.Comparator;
import java.util.Map.Entry;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BinderProxy$ProxyMap$huB_NMtOmTDIIYkL7mXm-Otlfnw implements Comparator {
    public static final /* synthetic */ -$$Lambda$BinderProxy$ProxyMap$huB_NMtOmTDIIYkL7mXm-Otlfnw INSTANCE = new -$$Lambda$BinderProxy$ProxyMap$huB_NMtOmTDIIYkL7mXm-Otlfnw();

    private /* synthetic */ -$$Lambda$BinderProxy$ProxyMap$huB_NMtOmTDIIYkL7mXm-Otlfnw() {
    }

    public final int compare(Object obj, Object obj2) {
        return ((Integer) ((Entry) obj2).getValue()).compareTo((Integer) ((Entry) obj).getValue());
    }
}
