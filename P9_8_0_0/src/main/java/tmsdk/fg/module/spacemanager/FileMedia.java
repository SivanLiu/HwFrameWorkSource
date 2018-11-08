package tmsdk.fg.module.spacemanager;

import android.text.TextUtils;
import tmsdk.common.OfflineVideo;
import tmsdk.common.tcc.QFile;

public class FileMedia extends FileInfo {
    public String album;
    public String artist;
    public OfflineVideo mOfflineVideo;
    public String[] mPlayers;
    public String pkg;
    public String title;

    public boolean delFile() {
        int -l_1_I = super.delFile();
        if (this.mOfflineVideo != null) {
            if (TextUtils.isEmpty(this.mOfflineVideo.mPath)) {
                return false;
            }
            -l_1_I = (-l_1_I == 0 && !new QFile(this.mOfflineVideo.mPath).deleteSelf()) ? 0 : 1;
        }
        return -l_1_I;
    }
}
