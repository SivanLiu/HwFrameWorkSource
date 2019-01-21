package android.service.autofill;

import com.android.internal.util.Preconditions;

public final class Validators {
    private Validators() {
        throw new UnsupportedOperationException("contains static methods only");
    }

    public static Validator and(Validator... validators) {
        return new RequiredValidators(getInternalValidators(validators));
    }

    public static Validator or(Validator... validators) {
        return new OptionalValidators(getInternalValidators(validators));
    }

    public static Validator not(Validator validator) {
        boolean z = validator instanceof InternalValidator;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("validator not provided by Android System: ");
        stringBuilder.append(validator);
        Preconditions.checkArgument(z, stringBuilder.toString());
        return new NegationValidator((InternalValidator) validator);
    }

    private static InternalValidator[] getInternalValidators(Validator[] validators) {
        Preconditions.checkArrayElementsNotNull(validators, "validators");
        InternalValidator[] internals = new InternalValidator[validators.length];
        for (int i = 0; i < validators.length; i++) {
            boolean z = validators[i] instanceof InternalValidator;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("element ");
            stringBuilder.append(i);
            stringBuilder.append(" not provided by Android System: ");
            stringBuilder.append(validators[i]);
            Preconditions.checkArgument(z, stringBuilder.toString());
            internals[i] = (InternalValidator) validators[i];
        }
        return internals;
    }
}
