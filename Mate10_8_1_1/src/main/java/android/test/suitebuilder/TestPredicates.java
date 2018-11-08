package android.test.suitebuilder;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.Smoke;
import android.test.suitebuilder.annotation.Suppress;
import com.android.internal.util.Predicate;
import java.lang.annotation.Annotation;

public class TestPredicates {
    static final Predicate<TestMethod> REJECT_INSTRUMENTATION = not(new AssignableFrom(InstrumentationTestCase.class));
    static final Predicate<TestMethod> REJECT_SUPPRESSED = not(hasAnnotation(Suppress.class));
    static final Predicate<TestMethod> SELECT_SMOKE = hasAnnotation(Smoke.class);

    private static class HasAnnotation implements Predicate<TestMethod> {
        private final Class<? extends Annotation> annotationClass;

        private HasAnnotation(Class<? extends Annotation> annotationClass) {
            this.annotationClass = annotationClass;
        }

        public boolean apply(TestMethod testMethod) {
            if (testMethod.getAnnotation(this.annotationClass) == null && testMethod.getEnclosingClass().getAnnotation(this.annotationClass) == null) {
                return false;
            }
            return true;
        }
    }

    private static class NotPredicate<T> implements Predicate<T> {
        private final Predicate<? super T> predicate;

        private NotPredicate(Predicate<? super T> predicate) {
            this.predicate = predicate;
        }

        public boolean apply(T t) {
            return this.predicate.apply(t) ^ 1;
        }
    }

    public static Predicate<TestMethod> hasAnnotation(Class<? extends Annotation> annotationClass) {
        return new HasAnnotation(annotationClass);
    }

    public static <T> Predicate<T> not(Predicate<? super T> predicate) {
        return new NotPredicate(predicate);
    }
}
