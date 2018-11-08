package tmsdk.fg.module.spacemanager;

import java.util.ArrayList;
import java.util.List;

public class FileScanResult {
    public List<FileInfo> mBigFiles = new ArrayList();
    public List<FileMedia> mRadioFiles = new ArrayList();
    public List<FileMedia> mVideoFiles = new ArrayList();

    public long getBigFileSize() {
        long -l_1_J = 0;
        for (FileInfo -l_4_R : this.mBigFiles) {
            -l_1_J += -l_4_R.mSize;
        }
        return -l_1_J;
    }

    public long getRadioSize() {
        long -l_1_J = 0;
        for (FileMedia -l_4_R : this.mRadioFiles) {
            -l_1_J += -l_4_R.mSize;
        }
        return -l_1_J;
    }

    public long getVideoSize() {
        long -l_1_J = 0;
        for (FileMedia -l_4_R : this.mVideoFiles) {
            -l_1_J += -l_4_R.mSize;
        }
        return -l_1_J;
    }
}
