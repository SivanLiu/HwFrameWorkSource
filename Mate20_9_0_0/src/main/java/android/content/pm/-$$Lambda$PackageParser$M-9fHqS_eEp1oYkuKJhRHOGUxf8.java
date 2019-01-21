package android.content.pm;

import android.content.pm.PackageParser.Service;
import java.util.Comparator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PackageParser$M-9fHqS_eEp1oYkuKJhRHOGUxf8 implements Comparator {
    public static final /* synthetic */ -$$Lambda$PackageParser$M-9fHqS_eEp1oYkuKJhRHOGUxf8 INSTANCE = new -$$Lambda$PackageParser$M-9fHqS_eEp1oYkuKJhRHOGUxf8();

    private /* synthetic */ -$$Lambda$PackageParser$M-9fHqS_eEp1oYkuKJhRHOGUxf8() {
    }

    public final int compare(Object obj, Object obj2) {
        return Integer.compare(((Service) obj2).order, ((Service) obj).order);
    }
}
