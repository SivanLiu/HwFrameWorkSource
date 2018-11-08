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

    private static InternalValidator[] getInternalValidators(Validator[] validators) {
        Preconditions.checkArrayElementsNotNull(validators, "validators");
        InternalValidator[] internals = new InternalValidator[validators.length];
        for (int i = 0; i < validators.length; i++) {
            Preconditions.checkArgument(validators[i] instanceof InternalValidator, "element " + i + " not provided by Android System: " + validators[i]);
            internals[i] = (InternalValidator) validators[i];
        }
        return internals;
    }
}
