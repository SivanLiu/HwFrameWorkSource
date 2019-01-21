package java.util;

import java.util.function.UnaryOperator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Collections$CheckedList$gXIP1Db1_l1aVeW3UfOh4dLyESo implements UnaryOperator {
    private final /* synthetic */ CheckedList f$0;
    private final /* synthetic */ UnaryOperator f$1;

    public /* synthetic */ -$$Lambda$Collections$CheckedList$gXIP1Db1_l1aVeW3UfOh4dLyESo(CheckedList checkedList, UnaryOperator unaryOperator) {
        this.f$0 = checkedList;
        this.f$1 = unaryOperator;
    }

    public final Object apply(Object obj) {
        return this.f$0.typeCheck(this.f$1.apply(obj));
    }
}
