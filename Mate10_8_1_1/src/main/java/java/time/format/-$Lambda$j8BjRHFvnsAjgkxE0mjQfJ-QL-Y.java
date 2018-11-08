package java.time.format;

import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;

final /* synthetic */ class -$Lambda$j8BjRHFvnsAjgkxE0mjQfJ-QL-Y implements TemporalQuery {
    public static final /* synthetic */ -$Lambda$j8BjRHFvnsAjgkxE0mjQfJ-QL-Y $INST$0 = new -$Lambda$j8BjRHFvnsAjgkxE0mjQfJ-QL-Y((byte) 0);
    public static final /* synthetic */ -$Lambda$j8BjRHFvnsAjgkxE0mjQfJ-QL-Y $INST$1 = new -$Lambda$j8BjRHFvnsAjgkxE0mjQfJ-QL-Y((byte) 1);
    public static final /* synthetic */ -$Lambda$j8BjRHFvnsAjgkxE0mjQfJ-QL-Y $INST$2 = new -$Lambda$j8BjRHFvnsAjgkxE0mjQfJ-QL-Y((byte) 2);
    private final /* synthetic */ byte $id;

    private final /* synthetic */ Object $m$0(TemporalAccessor arg0) {
        return DateTimeFormatter.lambda$-java_time_format_DateTimeFormatter_61156(arg0);
    }

    private final /* synthetic */ Object $m$1(TemporalAccessor arg0) {
        return DateTimeFormatter.lambda$-java_time_format_DateTimeFormatter_63118(arg0);
    }

    private final /* synthetic */ Object $m$2(TemporalAccessor arg0) {
        return DateTimeFormatterBuilder.lambda$-java_time_format_DateTimeFormatterBuilder_6842(arg0);
    }

    private /* synthetic */ -$Lambda$j8BjRHFvnsAjgkxE0mjQfJ-QL-Y(byte b) {
        this.$id = b;
    }

    public final Object queryFrom(TemporalAccessor temporalAccessor) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(temporalAccessor);
            case (byte) 1:
                return $m$1(temporalAccessor);
            case (byte) 2:
                return $m$2(temporalAccessor);
            default:
                throw new AssertionError();
        }
    }
}
