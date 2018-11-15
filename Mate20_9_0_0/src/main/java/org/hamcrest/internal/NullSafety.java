package org.hamcrest.internal;

import java.util.ArrayList;
import java.util.List;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsNull;

public class NullSafety {
    public static <E> List<Matcher<? super E>> nullSafe(Matcher<? super E>[] itemMatchers) {
        List<Matcher<? super E>> matchers = new ArrayList(itemMatchers.length);
        for (Matcher<? super E> itemMatcher : itemMatchers) {
            matchers.add(itemMatcher == null ? IsNull.nullValue() : itemMatcher);
        }
        return matchers;
    }
}
