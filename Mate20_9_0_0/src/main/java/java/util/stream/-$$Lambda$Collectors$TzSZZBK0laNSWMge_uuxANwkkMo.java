package java.util.stream;

import java.util.Map;
import java.util.function.BinaryOperator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collectors$TzSZZBK0laNSWMge_uuxANwkkMo implements BinaryOperator {
    private final /* synthetic */ BinaryOperator f$0;

    public /* synthetic */ -$$Lambda$Collectors$TzSZZBK0laNSWMge_uuxANwkkMo(BinaryOperator binaryOperator) {
        this.f$0 = binaryOperator;
    }

    public final Object apply(Object obj, Object obj2) {
        return Collectors.lambda$mapMerger$7(this.f$0, (Map) obj, (Map) obj2);
    }
}
