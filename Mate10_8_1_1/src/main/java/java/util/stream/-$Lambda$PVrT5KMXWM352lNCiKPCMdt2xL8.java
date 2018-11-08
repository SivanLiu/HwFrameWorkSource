package java.util.stream;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashSet;
import java.util.LongSummaryStatistics;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

final /* synthetic */ class -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 implements Supplier {
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$0 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 0);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$1 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 1);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$10 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 10);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$11 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 11);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$12 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 12);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$13 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 13);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$14 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 14);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$15 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 15);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$16 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 16);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$17 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8(Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$18 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 18);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$19 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 19);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$2 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 2);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$20 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 20);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$21 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8(Character.START_PUNCTUATION);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$22 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 22);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$23 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 23);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$24 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 24);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$25 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8(Character.MATH_SYMBOL);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$26 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8(Character.CURRENCY_SYMBOL);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$27 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 27);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$28 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 28);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$29 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8(Character.INITIAL_QUOTE_PUNCTUATION);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$3 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 3);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$30 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 30);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$4 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 4);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$5 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 5);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$6 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 6);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$7 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 7);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$8 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 8);
    public static final /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8 $INST$9 = new -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8((byte) 9);
    private final /* synthetic */ byte $id;

    private final /* synthetic */ Object $m$0() {
        return new double[4];
    }

    private final /* synthetic */ Object $m$1() {
        return new long[2];
    }

    private final /* synthetic */ Object $m$10() {
        return new double[3];
    }

    private final /* synthetic */ Object $m$11() {
        return new int[1];
    }

    private final /* synthetic */ Object $m$12() {
        return new long[1];
    }

    private final /* synthetic */ Object $m$13() {
        return new ConcurrentHashMap();
    }

    private final /* synthetic */ Object $m$14() {
        return new ConcurrentHashMap();
    }

    private final /* synthetic */ Object $m$15() {
        return new ArrayList();
    }

    private final /* synthetic */ Object $m$16() {
        return new HashMap();
    }

    private final /* synthetic */ Object $m$17() {
        return new HashMap();
    }

    private final /* synthetic */ Object $m$18() {
        return new HashSet();
    }

    private final /* synthetic */ Object $m$19() {
        return new LinkedHashSet();
    }

    private final /* synthetic */ Object $m$2() {
        return new long[2];
    }

    private final /* synthetic */ Object $m$20() {
        return new double[4];
    }

    private final /* synthetic */ Object $m$21() {
        return new double[3];
    }

    private final /* synthetic */ Object $m$22() {
        return new DoubleSummaryStatistics();
    }

    private final /* synthetic */ Object $m$23() {
        return new OfDouble();
    }

    private final /* synthetic */ Object $m$24() {
        return new OfInt();
    }

    private final /* synthetic */ Object $m$25() {
        return new OfLong();
    }

    private final /* synthetic */ Object $m$26() {
        return new OfRef();
    }

    private final /* synthetic */ Object $m$27() {
        return new long[2];
    }

    private final /* synthetic */ Object $m$28() {
        return new IntSummaryStatistics();
    }

    private final /* synthetic */ Object $m$29() {
        return new long[2];
    }

    private final /* synthetic */ Object $m$3() {
        return new HashMap();
    }

    private final /* synthetic */ Object $m$30() {
        return new LongSummaryStatistics();
    }

    private final /* synthetic */ Object $m$4() {
        return new ConcurrentHashMap();
    }

    private final /* synthetic */ Object $m$5() {
        return new ConcurrentHashMap();
    }

    private final /* synthetic */ Object $m$6() {
        return new StringBuilder();
    }

    private final /* synthetic */ Object $m$7() {
        return new DoubleSummaryStatistics();
    }

    private final /* synthetic */ Object $m$8() {
        return new IntSummaryStatistics();
    }

    private final /* synthetic */ Object $m$9() {
        return new LongSummaryStatistics();
    }

    private /* synthetic */ -$Lambda$PVrT5KMXWM352lNCiKPCMdt2xL8(byte b) {
        this.$id = b;
    }

    public final Object get() {
        switch (this.$id) {
            case (byte) 0:
                return $m$0();
            case (byte) 1:
                return $m$1();
            case (byte) 2:
                return $m$2();
            case (byte) 3:
                return $m$3();
            case (byte) 4:
                return $m$4();
            case (byte) 5:
                return $m$5();
            case (byte) 6:
                return $m$6();
            case (byte) 7:
                return $m$7();
            case (byte) 8:
                return $m$8();
            case (byte) 9:
                return $m$9();
            case (byte) 10:
                return $m$10();
            case (byte) 11:
                return $m$11();
            case (byte) 12:
                return $m$12();
            case (byte) 13:
                return $m$13();
            case (byte) 14:
                return $m$14();
            case (byte) 15:
                return $m$15();
            case (byte) 16:
                return $m$16();
            case (byte) 17:
                return $m$17();
            case (byte) 18:
                return $m$18();
            case (byte) 19:
                return $m$19();
            case (byte) 20:
                return $m$20();
            case (byte) 21:
                return $m$21();
            case (byte) 22:
                return $m$22();
            case SecureRandom.DEFAULT_SDK_TARGET_FOR_CRYPTO_PROVIDER_WORKAROUND /*23*/:
                return $m$23();
            case (byte) 24:
                return $m$24();
            case (byte) 25:
                return $m$25();
            case ZipConstants.LOCNAM /*26*/:
                return $m$26();
            case (byte) 27:
                return $m$27();
            case (byte) 28:
                return $m$28();
            case (byte) 29:
                return $m$29();
            case (byte) 30:
                return $m$30();
            default:
                throw new AssertionError();
        }
    }
}
