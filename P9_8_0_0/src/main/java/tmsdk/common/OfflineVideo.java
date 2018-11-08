package tmsdk.common;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class OfflineVideo implements Comparable<OfflineVideo> {
    public static final int PROGRESS_UNKNOWN = -1;
    public static final int STATUS_DL_UNCOMPLETED = 1;
    public static final int STATUS_READ_COMPLETED = 3;
    public static final int STATUS_READ_UNCOMPLETED = 2;
    public static final int STATUS_UNKNOWN = 0;
    public String mAdapter;
    public String mAppName;
    public int mDownProgress = -1;
    public String mPackage;
    public String mPath;
    public int mPlayProgress = -1;
    public String[] mPlayers;
    public boolean mSelected = false;
    public long mSize = -1;
    public boolean mThumbnailIsImage = false;
    public String mThumnbailPath;
    public String mTitle;

    public static void dumpToFile(List<OfflineVideo> list) {
        dumpToFile(list, getOfflineDatabase());
    }

    public static void dumpToFile(List<OfflineVideo> list, String str) {
        Object -l_6_R;
        Object -l_8_R;
        Object -l_2_R = new ByteArrayOutputStream();
        Object -l_3_R = new DataOutputStream(-l_2_R);
        int -l_4_I = 1;
        if (list != null) {
            for (OfflineVideo -l_6_R2 : list) {
                try {
                    -l_6_R2.writeTo(-l_3_R);
                } catch (Object -l_7_R) {
                    -l_4_I = 0;
                    -l_7_R.printStackTrace();
                }
                if (-l_4_I != 0) {
                }
            }
        }
        try {
            -l_3_R.flush();
        } catch (Object -l_5_R) {
            -l_5_R.printStackTrace();
        }
        if (-l_4_I != 0) {
            FileOutputStream -l_5_R2 = null;
            try {
                FileOutputStream -l_5_R3 = new FileOutputStream(str);
                try {
                    -l_5_R3.write(-l_2_R.toByteArray());
                    if (-l_5_R3 != null) {
                        try {
                            -l_5_R3.close();
                        } catch (Object -l_6_R3) {
                            -l_6_R3.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    -l_6_R3 = e;
                    -l_5_R2 = -l_5_R3;
                    try {
                        -l_6_R3.printStackTrace();
                        if (-l_5_R2 != null) {
                            try {
                                -l_5_R2.close();
                            } catch (Object -l_6_R32) {
                                -l_6_R32.printStackTrace();
                            }
                        }
                    } catch (Throwable th) {
                        -l_8_R = th;
                        if (-l_5_R2 != null) {
                            try {
                                -l_5_R2.close();
                            } catch (Object -l_9_R) {
                                -l_9_R.printStackTrace();
                            }
                        }
                        throw -l_8_R;
                    }
                } catch (Throwable th2) {
                    -l_8_R = th2;
                    -l_5_R2 = -l_5_R3;
                    if (-l_5_R2 != null) {
                        -l_5_R2.close();
                    }
                    throw -l_8_R;
                }
            } catch (Exception e2) {
                -l_6_R32 = e2;
                -l_6_R32.printStackTrace();
                if (-l_5_R2 != null) {
                    -l_5_R2.close();
                }
            }
        }
    }

    public static String getOfflineDatabase() {
        return TMSDKContext.getApplicaionContext().getApplicationInfo().dataDir + "/databases/offlinevideo.db";
    }

    public static OfflineVideo readFrom(DataInputStream dataInputStream) throws IOException {
        Object -l_1_R = new OfflineVideo();
        -l_1_R.mPath = dataInputStream.readUTF();
        -l_1_R.mTitle = dataInputStream.readUTF();
        -l_1_R.mPackage = dataInputStream.readUTF();
        -l_1_R.mAppName = dataInputStream.readUTF();
        -l_1_R.mPlayers = dataInputStream.readUTF().split("&");
        -l_1_R.mThumnbailPath = dataInputStream.readUTF();
        -l_1_R.mSize = dataInputStream.readLong();
        -l_1_R.mDownProgress = dataInputStream.readInt();
        -l_1_R.mPlayProgress = dataInputStream.readInt();
        -l_1_R.mAdapter = dataInputStream.readUTF();
        return -l_1_R;
    }

    public static List<OfflineVideo> readOfflineVideos() {
        return readOfflineVideos(getOfflineDatabase());
    }

    public static List<OfflineVideo> readOfflineVideos(String str) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(str);
        } catch (Object -l_2_R) {
            Object -l_2_R2;
            -l_2_R2.printStackTrace();
        }
        if (inputStream == null) {
            return null;
        }
        -l_2_R2 = new ArrayList();
        Object -l_3_R = new DataInputStream(inputStream);
        while (-l_3_R.available() > 0) {
            Object -l_4_R;
            try {
                -l_4_R = readFrom(-l_3_R);
                if (-l_4_R != null) {
                    -l_2_R2.add(-l_4_R);
                }
            } catch (Exception e) {
            }
        }
        try {
            -l_3_R.close();
        } catch (Exception e2) {
        }
        try {
            inputStream.close();
        } catch (Object -l_4_R2) {
            -l_4_R2.printStackTrace();
        }
        if (-l_2_R2.size() == 0) {
            -l_2_R2 = null;
        }
        return -l_2_R2;
    }

    public int compareTo(OfflineVideo offlineVideo) {
        long -l_2_J = this.mSize - offlineVideo.mSize;
        if ((-l_2_J <= 0 ? 1 : 0) == 0) {
            return -1;
        }
        return -l_2_J == 0 ? 0 : 1;
    }

    public int getStatus() {
        if (this.mDownProgress > 0 && this.mDownProgress < 95) {
            return 1;
        }
        if (this.mPlayProgress == -1) {
            return 0;
        }
        return this.mPlayProgress <= 85 ? 2 : 3;
    }

    public String getStatusDesc() {
        if (this.mDownProgress != -1 && this.mDownProgress < 95) {
            return "未下载完";
        }
        if (this.mPlayProgress == -1) {
            return null;
        }
        return this.mPlayProgress <= 85 ? "未看完" : "已播完";
    }

    public void writeTo(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeUTF(this.mPath != null ? this.mPath : "");
        dataOutputStream.writeUTF(this.mTitle != null ? this.mTitle : "");
        dataOutputStream.writeUTF(this.mPackage != null ? this.mPackage : "");
        dataOutputStream.writeUTF(this.mAppName != null ? this.mAppName : "");
        Object -l_2_R = new StringBuffer();
        if (this.mPlayers != null) {
            for (Object -l_6_R : this.mPlayers) {
                -l_2_R.append(-l_6_R);
                -l_2_R.append("&");
            }
        }
        dataOutputStream.writeUTF(-l_2_R.toString());
        dataOutputStream.writeUTF(this.mThumnbailPath != null ? this.mThumnbailPath : "");
        dataOutputStream.writeLong(this.mSize);
        dataOutputStream.writeInt(this.mDownProgress);
        dataOutputStream.writeInt(this.mPlayProgress);
        dataOutputStream.writeUTF(this.mAdapter != null ? this.mAdapter : "");
    }
}
