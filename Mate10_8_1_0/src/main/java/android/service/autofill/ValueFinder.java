package android.service.autofill;

import android.view.autofill.AutofillId;

public interface ValueFinder {
    String findByAutofillId(AutofillId autofillId);
}
