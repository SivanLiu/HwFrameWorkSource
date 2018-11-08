package android.system;

import libcore.util.Objects;

public final class StructStat {
    public final StructTimespec st_atim;
    public final long st_atime;
    public final long st_blksize;
    public final long st_blocks;
    public final StructTimespec st_ctim;
    public final long st_ctime;
    public final long st_dev;
    public final int st_gid;
    public final long st_ino;
    public final int st_mode;
    public final StructTimespec st_mtim;
    public final long st_mtime;
    public final long st_nlink;
    public final long st_rdev;
    public final long st_size;
    public final int st_uid;

    public StructStat(long st_dev, long st_ino, int st_mode, long st_nlink, int st_uid, int st_gid, long st_rdev, long st_size, long st_atime, long st_mtime, long st_ctime, long st_blksize, long st_blocks) {
        this(st_dev, st_ino, st_mode, st_nlink, st_uid, st_gid, st_rdev, st_size, new StructTimespec(st_atime, 0), new StructTimespec(st_mtime, 0), new StructTimespec(st_ctime, 0), st_blksize, st_blocks);
    }

    public StructStat(long st_dev, long st_ino, int st_mode, long st_nlink, int st_uid, int st_gid, long st_rdev, long st_size, StructTimespec st_atim, StructTimespec st_mtim, StructTimespec st_ctim, long st_blksize, long st_blocks) {
        this.st_dev = st_dev;
        this.st_ino = st_ino;
        this.st_mode = st_mode;
        this.st_nlink = st_nlink;
        this.st_uid = st_uid;
        this.st_gid = st_gid;
        this.st_rdev = st_rdev;
        this.st_size = st_size;
        this.st_atime = st_atim.tv_sec;
        this.st_mtime = st_mtim.tv_sec;
        this.st_ctime = st_ctim.tv_sec;
        this.st_atim = st_atim;
        this.st_mtim = st_mtim;
        this.st_ctim = st_ctim;
        this.st_blksize = st_blksize;
        this.st_blocks = st_blocks;
    }

    public String toString() {
        return Objects.toString(this);
    }
}
