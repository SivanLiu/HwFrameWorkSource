package java.util.stream;

import java.security.PrivilegedAction;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Tripwire$h-WLrZCXxuA6HLdDg4eUp2SowfQ implements PrivilegedAction {
    public static final /* synthetic */ -$$Lambda$Tripwire$h-WLrZCXxuA6HLdDg4eUp2SowfQ INSTANCE = new -$$Lambda$Tripwire$h-WLrZCXxuA6HLdDg4eUp2SowfQ();

    private /* synthetic */ -$$Lambda$Tripwire$h-WLrZCXxuA6HLdDg4eUp2SowfQ() {
    }

    public final Object run() {
        return Boolean.valueOf(Boolean.getBoolean(Tripwire.TRIPWIRE_PROPERTY));
    }
}
