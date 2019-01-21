package java.lang;

import java.util.NoSuchElementException;
import java.util.PrimitiveIterator.OfInt;
import java.util.Spliterators;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CharSequence$lnrrVTEPDeRteHnQDz8kEht4CY8 implements Supplier {
    private final /* synthetic */ CharSequence f$0;

    public /* synthetic */ -$$Lambda$CharSequence$lnrrVTEPDeRteHnQDz8kEht4CY8(CharSequence charSequence) {
        this.f$0 = charSequence;
    }

    public final Object get() {
        return Spliterators.spliteratorUnknownSize(new OfInt() {
            int cur = 0;

            public void forEachRemaining(IntConsumer block) {
                Throwable i;
                int length = CharSequence.this.length();
                int i2 = this.cur;
                while (i2 < length) {
                    int i3;
                    try {
                        i3 = i2 + 1;
                        try {
                            char c1 = CharSequence.this.charAt(i2);
                            if (Character.isHighSurrogate(c1)) {
                                if (i3 < length) {
                                    char c2 = CharSequence.this.charAt(i3);
                                    if (Character.isLowSurrogate(c2)) {
                                        i3++;
                                        block.accept(Character.toCodePoint(c1, c2));
                                    } else {
                                        block.accept(c1);
                                    }
                                    i2 = i3;
                                }
                            }
                            block.accept(c1);
                            i2 = i3;
                        } catch (Throwable th) {
                            i = th;
                            this.cur = i3;
                            throw i;
                        }
                    } catch (Throwable th2) {
                        i3 = i2;
                        i = th2;
                        this.cur = i3;
                        throw i;
                    }
                }
                this.cur = i2;
            }

            public boolean hasNext() {
                return this.cur < CharSequence.this.length();
            }

            public int nextInt() {
                int length = CharSequence.this.length();
                if (this.cur < length) {
                    char c1 = CharSequence.this;
                    int i = this.cur;
                    this.cur = i + 1;
                    c1 = c1.charAt(i);
                    if (Character.isHighSurrogate(c1) && this.cur < length) {
                        char c2 = CharSequence.this.charAt(this.cur);
                        if (Character.isLowSurrogate(c2)) {
                            this.cur++;
                            return Character.toCodePoint(c1, c2);
                        }
                    }
                    return c1;
                }
                throw new NoSuchElementException();
            }
        }, 16);
    }
}
