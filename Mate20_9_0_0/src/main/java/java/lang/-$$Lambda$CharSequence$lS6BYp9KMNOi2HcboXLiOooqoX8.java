package java.lang;

import java.util.NoSuchElementException;
import java.util.PrimitiveIterator.OfInt;
import java.util.Spliterators;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CharSequence$lS6BYp9KMNOi2HcboXLiOooqoX8 implements Supplier {
    private final /* synthetic */ CharSequence f$0;

    public /* synthetic */ -$$Lambda$CharSequence$lS6BYp9KMNOi2HcboXLiOooqoX8(CharSequence charSequence) {
        this.f$0 = charSequence;
    }

    public final Object get() {
        return Spliterators.spliterator(new OfInt() {
            int cur = 0;

            public boolean hasNext() {
                return this.cur < CharSequence.this.length();
            }

            public int nextInt() {
                if (hasNext()) {
                    CharSequence charSequence = CharSequence.this;
                    int i = this.cur;
                    this.cur = i + 1;
                    return charSequence.charAt(i);
                }
                throw new NoSuchElementException();
            }

            public void forEachRemaining(IntConsumer block) {
                while (this.cur < CharSequence.this.length()) {
                    block.accept(CharSequence.this.charAt(this.cur));
                    this.cur++;
                }
            }
        }, (long) this.f$0.length(), 16);
    }
}
