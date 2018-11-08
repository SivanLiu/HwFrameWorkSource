package tmsdk.fg.module.spacemanager;

import android.text.TextUtils;
import java.io.File;
import tmsdk.common.tcc.QFile;

public class FileInfo {
    public static final int TYPE_BIGFILE = 3;
    public static final int TYPE_RADIO = 1;
    public static final int TYPE_VIDEO = 2;
    private long Pj = -1;
    private long Pk = -1;
    private long Pl = -1;
    public String mPath;
    public long mSize;
    public String mSrcName;
    public int type = 3;

    public boolean delFile() {
        return !TextUtils.isEmpty(this.mPath) ? new QFile(this.mPath).deleteSelf() : false;
    }

    public String getFileName() {
        return (TextUtils.isEmpty(this.mPath) || this.mPath.endsWith(File.separator)) ? null : this.mPath.substring(this.mPath.lastIndexOf(File.separator) + 1, this.mPath.length());
    }

    public long getmAccessTime() {
        if (-1 == this.Pl) {
            if (TextUtils.isEmpty(this.mPath)) {
                return -1;
            }
            Object -l_1_R = new QFile(this.mPath);
            -l_1_R.fillExtraInfo();
            this.Pj = -l_1_R.createTime;
            this.Pk = -l_1_R.modifyTime;
            this.Pl = -l_1_R.accessTime;
        }
        return this.Pl;
    }

    public long getmCreateTime() {
        if (TextUtils.isEmpty(this.mPath)) {
            return -1;
        }
        if (-1 == this.Pj) {
            Object -l_1_R = new QFile(this.mPath);
            -l_1_R.fillExtraInfo();
            this.Pj = -l_1_R.createTime;
            this.Pk = -l_1_R.modifyTime;
            this.Pl = -l_1_R.accessTime;
        }
        return this.Pj;
    }

    public long getmModifyTime() {
        if (-1 == this.Pk) {
            if (TextUtils.isEmpty(this.mPath)) {
                return -1;
            }
            Object -l_1_R = new QFile(this.mPath);
            -l_1_R.fillExtraInfo();
            this.Pj = -l_1_R.createTime;
            this.Pk = -l_1_R.modifyTime;
            this.Pl = -l_1_R.accessTime;
        }
        return this.Pk;
    }

    public String toString() {
        return this.mPath + " :: " + this.mSize + " :: " + this.mSrcName;
    }
}
