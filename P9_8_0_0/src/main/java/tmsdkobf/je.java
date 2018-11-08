package tmsdkobf;

import android.database.Cursor;
import android.database.CursorWrapper;

public class je extends CursorWrapper {
    public je(Cursor cursor) {
        super(cursor);
    }

    public void close() {
        try {
            super.close();
        } catch (Object -l_1_R) {
            -l_1_R.printStackTrace();
        }
    }
}
