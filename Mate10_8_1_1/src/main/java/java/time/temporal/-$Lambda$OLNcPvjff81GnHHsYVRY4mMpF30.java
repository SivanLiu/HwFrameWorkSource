package java.time.temporal;

import java.time.LocalDate;
import java.util.function.UnaryOperator;

final /* synthetic */ class -$Lambda$OLNcPvjff81GnHHsYVRY4mMpF30 implements TemporalAdjuster {
    public static final /* synthetic */ -$Lambda$OLNcPvjff81GnHHsYVRY4mMpF30 $INST$0 = new -$Lambda$OLNcPvjff81GnHHsYVRY4mMpF30((byte) 0);
    public static final /* synthetic */ -$Lambda$OLNcPvjff81GnHHsYVRY4mMpF30 $INST$1 = new -$Lambda$OLNcPvjff81GnHHsYVRY4mMpF30((byte) 1);
    public static final /* synthetic */ -$Lambda$OLNcPvjff81GnHHsYVRY4mMpF30 $INST$2 = new -$Lambda$OLNcPvjff81GnHHsYVRY4mMpF30((byte) 2);
    public static final /* synthetic */ -$Lambda$OLNcPvjff81GnHHsYVRY4mMpF30 $INST$3 = new -$Lambda$OLNcPvjff81GnHHsYVRY4mMpF30((byte) 3);
    public static final /* synthetic */ -$Lambda$OLNcPvjff81GnHHsYVRY4mMpF30 $INST$4 = new -$Lambda$OLNcPvjff81GnHHsYVRY4mMpF30((byte) 4);
    public static final /* synthetic */ -$Lambda$OLNcPvjff81GnHHsYVRY4mMpF30 $INST$5 = new -$Lambda$OLNcPvjff81GnHHsYVRY4mMpF30((byte) 5);
    private final /* synthetic */ byte $id;

    /* renamed from: java.time.temporal.-$Lambda$OLNcPvjff81GnHHsYVRY4mMpF30$1 */
    final /* synthetic */ class AnonymousClass1 implements TemporalAdjuster {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ Temporal $m$0(Temporal arg0) {
            return arg0.with((LocalDate) ((UnaryOperator) this.-$f0).apply(LocalDate.from(arg0)));
        }

        public /* synthetic */ AnonymousClass1(Object obj) {
            this.-$f0 = obj;
        }

        public final Temporal adjustInto(Temporal temporal) {
            return $m$0(temporal);
        }
    }

    /* renamed from: java.time.temporal.-$Lambda$OLNcPvjff81GnHHsYVRY4mMpF30$2 */
    final /* synthetic */ class AnonymousClass2 implements TemporalAdjuster {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ int -$f0;

        private final /* synthetic */ Temporal $m$0(Temporal arg0) {
            return TemporalAdjusters.lambda$-java_time_temporal_TemporalAdjusters_17076(this.-$f0, arg0);
        }

        private final /* synthetic */ Temporal $m$1(Temporal arg0) {
            return TemporalAdjusters.lambda$-java_time_temporal_TemporalAdjusters_18421(this.-$f0, arg0);
        }

        private final /* synthetic */ Temporal $m$2(Temporal arg0) {
            return TemporalAdjusters.lambda$-java_time_temporal_TemporalAdjusters_19758(this.-$f0, arg0);
        }

        private final /* synthetic */ Temporal $m$3(Temporal arg0) {
            return TemporalAdjusters.lambda$-java_time_temporal_TemporalAdjusters_21123(this.-$f0, arg0);
        }

        public /* synthetic */ AnonymousClass2(byte b, int i) {
            this.$id = b;
            this.-$f0 = i;
        }

        public final Temporal adjustInto(Temporal temporal) {
            switch (this.$id) {
                case (byte) 0:
                    return $m$0(temporal);
                case (byte) 1:
                    return $m$1(temporal);
                case (byte) 2:
                    return $m$2(temporal);
                case (byte) 3:
                    return $m$3(temporal);
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: java.time.temporal.-$Lambda$OLNcPvjff81GnHHsYVRY4mMpF30$3 */
    final /* synthetic */ class AnonymousClass3 implements TemporalAdjuster {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ int -$f1;

        private final /* synthetic */ Temporal $m$0(Temporal arg0) {
            return arg0.with(ChronoField.DAY_OF_MONTH, 1);
        }

        private final /* synthetic */ Temporal $m$1(Temporal arg0) {
            return TemporalAdjusters.lambda$-java_time_temporal_TemporalAdjusters_15513(this.-$f0, this.-$f1, arg0);
        }

        public /* synthetic */ AnonymousClass3(byte b, int i, int i2) {
            this.$id = b;
            this.-$f0 = i;
            this.-$f1 = i2;
        }

        public final Temporal adjustInto(Temporal temporal) {
            switch (this.$id) {
                case (byte) 0:
                    return $m$0(temporal);
                case (byte) 1:
                    return $m$1(temporal);
                default:
                    throw new AssertionError();
            }
        }
    }

    private final /* synthetic */ Temporal $m$0(Temporal arg0) {
        return arg0.with(ChronoField.DAY_OF_MONTH, 1);
    }

    private final /* synthetic */ Temporal $m$1(Temporal arg0) {
        return arg0.with(ChronoField.DAY_OF_MONTH, 1).plus(1, ChronoUnit.MONTHS);
    }

    private final /* synthetic */ Temporal $m$2(Temporal arg0) {
        return arg0.with(ChronoField.DAY_OF_YEAR, 1).plus(1, ChronoUnit.YEARS);
    }

    private final /* synthetic */ Temporal $m$3(Temporal arg0) {
        return arg0.with(ChronoField.DAY_OF_YEAR, 1);
    }

    private final /* synthetic */ Temporal $m$4(Temporal arg0) {
        return arg0.with(ChronoField.DAY_OF_MONTH, arg0.range(ChronoField.DAY_OF_MONTH).getMaximum());
    }

    private final /* synthetic */ Temporal $m$5(Temporal arg0) {
        return arg0.with(ChronoField.DAY_OF_YEAR, arg0.range(ChronoField.DAY_OF_YEAR).getMaximum());
    }

    private /* synthetic */ -$Lambda$OLNcPvjff81GnHHsYVRY4mMpF30(byte b) {
        this.$id = b;
    }

    public final Temporal adjustInto(Temporal temporal) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(temporal);
            case (byte) 1:
                return $m$1(temporal);
            case (byte) 2:
                return $m$2(temporal);
            case (byte) 3:
                return $m$3(temporal);
            case (byte) 4:
                return $m$4(temporal);
            case (byte) 5:
                return $m$5(temporal);
            default:
                throw new AssertionError();
        }
    }
}
