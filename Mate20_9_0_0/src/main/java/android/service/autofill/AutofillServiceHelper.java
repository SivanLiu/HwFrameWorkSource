package android.service.autofill;

import android.view.autofill.AutofillId;
import com.android.internal.util.Preconditions;

final class AutofillServiceHelper {
    static AutofillId[] assertValid(AutofillId[] ids) {
        int i = 0;
        boolean z = ids != null && ids.length > 0;
        Preconditions.checkArgument(z, "must have at least one id");
        while (i < ids.length) {
            if (ids[i] != null) {
                i++;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ids[");
                stringBuilder.append(i);
                stringBuilder.append("] must not be null");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        return ids;
    }

    private AutofillServiceHelper() {
        throw new UnsupportedOperationException("contains static members only");
    }
}
