package java.lang;

import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public interface CharSequence {
    char charAt(int i);

    int length();

    CharSequence subSequence(int i, int i2);

    String toString();

    IntStream chars() {
        return StreamSupport.intStream(new -$$Lambda$CharSequence$lS6BYp9KMNOi2HcboXLiOooqoX8(this), 16464, false);
    }

    IntStream codePoints() {
        return StreamSupport.intStream(new -$$Lambda$CharSequence$lnrrVTEPDeRteHnQDz8kEht4CY8(this), 16, false);
    }
}
