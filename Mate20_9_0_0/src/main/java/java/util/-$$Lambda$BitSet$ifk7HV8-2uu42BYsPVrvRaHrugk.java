package java.util;

import java.util.PrimitiveIterator.OfInt;
import java.util.function.Supplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BitSet$ifk7HV8-2uu42BYsPVrvRaHrugk implements Supplier {
    private final /* synthetic */ BitSet f$0;

    public /* synthetic */ -$$Lambda$BitSet$ifk7HV8-2uu42BYsPVrvRaHrugk(BitSet bitSet) {
        this.f$0 = bitSet;
    }

    public final Object get() {
        return Spliterators.spliterator(new OfInt() {
            int next = BitSet.this.nextSetBit(0);

            public boolean hasNext() {
                return this.next != -1 ? true : BitSet.$assertionsDisabled;
            }

            public int nextInt() {
                if (this.next != -1) {
                    int ret = this.next;
                    this.next = BitSet.this.nextSetBit(this.next + 1);
                    return ret;
                }
                throw new NoSuchElementException();
            }
        }, (long) this.f$0.cardinality(), 21);
    }
}
