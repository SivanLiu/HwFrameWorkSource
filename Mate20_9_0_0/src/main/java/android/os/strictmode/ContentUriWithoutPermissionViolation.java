package android.os.strictmode;

import android.net.Uri;

public final class ContentUriWithoutPermissionViolation extends Violation {
    public ContentUriWithoutPermissionViolation(Uri uri, String location) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(uri);
        stringBuilder.append(" exposed beyond app through ");
        stringBuilder.append(location);
        stringBuilder.append(" without permission grant flags; did you forget FLAG_GRANT_READ_URI_PERMISSION?");
        super(stringBuilder.toString());
    }
}
