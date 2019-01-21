package android.service.autofill;

import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;

public interface ValueFinder {
    AutofillValue findRawValueByAutofillId(AutofillId autofillId);

    String findByAutofillId(AutofillId id) {
        AutofillValue value = findRawValueByAutofillId(id);
        return (value == null || !value.isText()) ? null : value.getTextValue().toString();
    }
}
