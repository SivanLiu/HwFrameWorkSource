package tmsdk.common.module.numbermarker;

import tmsdkobf.cr;
import tmsdkobf.ef;

public class NumberMarkEntity {
    public static final int CLIENT_LOGIC_MIN = 0;
    public static int TAG_TYPE_CORRECT_YELLOW = 10056;
    public static int TAG_TYPE_MAX = 30056;
    public static int TAG_TYPE_NONE = 0;
    public static int TAG_TYPE_OTHER = 50;
    public static int TAG_TYPE_SELF_TAG = 10055;
    public static int TEL_TYPE_MISS_CALL = 3;
    public static int TEL_TYPE_RING_ONE_SOUND = 1;
    public static int TEL_TYPE_USER_CANCEL = 2;
    public static int TEL_TYPE_USER_HANG_UP = 4;
    public static final int USER_ACTION_IMPEACH = 11;
    public int calltime = 0;
    public int clientlogic = 0;
    public int localTagType = 0;
    public String originName;
    public String phonenum = "";
    public int scene = 0;
    public int tagtype = 0;
    public int talktime = 0;
    public int teltype = ef.jZ.value();
    public String userDefineName;
    public int useraction = 11;

    public cr toTelReport() {
        Object -l_1_R = new cr();
        -l_1_R.fe = this.phonenum;
        -l_1_R.fP = this.useraction;
        -l_1_R.fQ = this.teltype;
        -l_1_R.fR = this.talktime;
        -l_1_R.fS = this.calltime;
        -l_1_R.fT = this.clientlogic;
        -l_1_R.tagType = this.tagtype;
        -l_1_R.userDefineName = this.userDefineName;
        -l_1_R.localTagType = this.localTagType;
        -l_1_R.originName = this.originName;
        -l_1_R.scene = this.scene;
        return -l_1_R;
    }
}
