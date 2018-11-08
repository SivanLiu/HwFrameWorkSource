package com.android.server.pm;

import android.content.pm.PackageParser.Package;
import android.content.pm.ShortcutInfo;
import java.util.function.Predicate;

final /* synthetic */ class -$Lambda$s_oh3oeib-Exts1l3lS2Euiarsw implements Predicate {
    public static final /* synthetic */ -$Lambda$s_oh3oeib-Exts1l3lS2Euiarsw $INST$0 = new -$Lambda$s_oh3oeib-Exts1l3lS2Euiarsw((byte) 0);
    public static final /* synthetic */ -$Lambda$s_oh3oeib-Exts1l3lS2Euiarsw $INST$1 = new -$Lambda$s_oh3oeib-Exts1l3lS2Euiarsw((byte) 1);
    public static final /* synthetic */ -$Lambda$s_oh3oeib-Exts1l3lS2Euiarsw $INST$2 = new -$Lambda$s_oh3oeib-Exts1l3lS2Euiarsw((byte) 2);
    public static final /* synthetic */ -$Lambda$s_oh3oeib-Exts1l3lS2Euiarsw $INST$3 = new -$Lambda$s_oh3oeib-Exts1l3lS2Euiarsw((byte) 3);
    public static final /* synthetic */ -$Lambda$s_oh3oeib-Exts1l3lS2Euiarsw $INST$4 = new -$Lambda$s_oh3oeib-Exts1l3lS2Euiarsw((byte) 4);
    public static final /* synthetic */ -$Lambda$s_oh3oeib-Exts1l3lS2Euiarsw $INST$5 = new -$Lambda$s_oh3oeib-Exts1l3lS2Euiarsw((byte) 5);
    public static final /* synthetic */ -$Lambda$s_oh3oeib-Exts1l3lS2Euiarsw $INST$6 = new -$Lambda$s_oh3oeib-Exts1l3lS2Euiarsw((byte) 6);
    public static final /* synthetic */ -$Lambda$s_oh3oeib-Exts1l3lS2Euiarsw $INST$7 = new -$Lambda$s_oh3oeib-Exts1l3lS2Euiarsw((byte) 7);
    private final /* synthetic */ byte $id;

    private final /* synthetic */ boolean $m$0(Object arg0) {
        return ((Package) arg0).coreApp;
    }

    private final /* synthetic */ boolean $m$1(Object arg0) {
        return true;
    }

    private final /* synthetic */ boolean $m$2(Object arg0) {
        return true;
    }

    private final /* synthetic */ boolean $m$3(Object arg0) {
        return (((ShortcutInfo) arg0).isDynamic() ^ 1);
    }

    private final /* synthetic */ boolean $m$4(Object arg0) {
        return (((ShortcutInfo) arg0).isManifestShortcut() ^ 1);
    }

    private final /* synthetic */ boolean $m$5(Object arg0) {
        return ((ShortcutInfo) arg0).isDynamic();
    }

    private final /* synthetic */ boolean $m$6(Object arg0) {
        return ((ShortcutInfo) arg0).isManifestShortcut();
    }

    private final /* synthetic */ boolean $m$7(Object arg0) {
        return ((ShortcutInfo) arg0).isPinned();
    }

    private /* synthetic */ -$Lambda$s_oh3oeib-Exts1l3lS2Euiarsw(byte b) {
        this.$id = b;
    }

    public final boolean test(Object obj) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(obj);
            case (byte) 1:
                return $m$1(obj);
            case (byte) 2:
                return $m$2(obj);
            case (byte) 3:
                return $m$3(obj);
            case (byte) 4:
                return $m$4(obj);
            case (byte) 5:
                return $m$5(obj);
            case (byte) 6:
                return $m$6(obj);
            case (byte) 7:
                return $m$7(obj);
            default:
                throw new AssertionError();
        }
    }
}
