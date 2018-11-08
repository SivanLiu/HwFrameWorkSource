package java.util.stream;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Predicate;

final /* synthetic */ class -$Lambda$ioGbka_-VkWTFjRjTt8T4zzsxgk implements Predicate {
    public static final /* synthetic */ -$Lambda$ioGbka_-VkWTFjRjTt8T4zzsxgk $INST$0 = new -$Lambda$ioGbka_-VkWTFjRjTt8T4zzsxgk((byte) 0);
    public static final /* synthetic */ -$Lambda$ioGbka_-VkWTFjRjTt8T4zzsxgk $INST$1 = new -$Lambda$ioGbka_-VkWTFjRjTt8T4zzsxgk((byte) 1);
    public static final /* synthetic */ -$Lambda$ioGbka_-VkWTFjRjTt8T4zzsxgk $INST$2 = new -$Lambda$ioGbka_-VkWTFjRjTt8T4zzsxgk((byte) 2);
    public static final /* synthetic */ -$Lambda$ioGbka_-VkWTFjRjTt8T4zzsxgk $INST$3 = new -$Lambda$ioGbka_-VkWTFjRjTt8T4zzsxgk((byte) 3);
    private final /* synthetic */ byte $id;

    private final /* synthetic */ boolean $m$0(Object arg0) {
        return ((OptionalDouble) arg0).isPresent();
    }

    private final /* synthetic */ boolean $m$1(Object arg0) {
        return ((OptionalInt) arg0).isPresent();
    }

    private final /* synthetic */ boolean $m$2(Object arg0) {
        return ((OptionalLong) arg0).isPresent();
    }

    private final /* synthetic */ boolean $m$3(Object arg0) {
        return ((Optional) arg0).isPresent();
    }

    private /* synthetic */ -$Lambda$ioGbka_-VkWTFjRjTt8T4zzsxgk(byte b) {
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
            default:
                throw new AssertionError();
        }
    }
}
