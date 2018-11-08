package tmsdkobf;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore.Files;
import java.io.File;
import java.io.IOException;

@TargetApi(11)
public class rf {
    private final ContentResolver Pf;
    private final Uri Pg = Files.getContentUri("external");
    private final File file;

    public rf(ContentResolver contentResolver, File file) {
        this.file = file;
        this.Pf = contentResolver;
    }

    public boolean delete() throws IOException {
        boolean z = false;
        if (!this.file.exists() || this.file.isDirectory()) {
            return true;
        }
        if (this.Pf == null) {
            return false;
        }
        try {
            Object -l_2_R = new String[]{this.file.getAbsolutePath()};
            this.Pf.delete(this.Pg, "_data=?", -l_2_R);
            if (this.file.exists()) {
                Object -l_3_R = new ContentValues();
                -l_3_R.put("_data", this.file.getAbsolutePath());
                Object -l_4_R = this.Pf.insert(this.Pg, -l_3_R);
                Object -l_5_R = new ContentValues();
                -l_5_R.put("media_type", Integer.valueOf(4));
                this.Pf.update(-l_4_R, -l_5_R, null, null);
                this.Pf.delete(-l_4_R, null, null);
            }
        } catch (Throwable th) {
        }
        if (!this.file.exists()) {
            z = true;
        }
        return z;
    }
}
