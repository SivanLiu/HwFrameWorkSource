package org.junit.experimental.results;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ResultMatchers {
    public static Matcher<PrintableResult> isSuccessful() {
        return failureCountIs(0);
    }

    public static Matcher<PrintableResult> failureCountIs(final int count) {
        return new TypeSafeMatcher<PrintableResult>() {
            public void describeTo(Description description) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("has ");
                stringBuilder.append(count);
                stringBuilder.append(" failures");
                description.appendText(stringBuilder.toString());
            }

            public boolean matchesSafely(PrintableResult item) {
                return item.failureCount() == count;
            }
        };
    }

    public static Matcher<Object> hasSingleFailureContaining(final String string) {
        return new BaseMatcher<Object>() {
            public boolean matches(Object item) {
                return item.toString().contains(string) && ResultMatchers.failureCountIs(1).matches(item);
            }

            public void describeTo(Description description) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("has single failure containing ");
                stringBuilder.append(string);
                description.appendText(stringBuilder.toString());
            }
        };
    }

    public static Matcher<PrintableResult> hasFailureContaining(final String string) {
        return new BaseMatcher<PrintableResult>() {
            public boolean matches(Object item) {
                return item.toString().contains(string);
            }

            public void describeTo(Description description) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("has failure containing ");
                stringBuilder.append(string);
                description.appendText(stringBuilder.toString());
            }
        };
    }
}
