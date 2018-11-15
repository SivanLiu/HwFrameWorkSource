package android.net.netlink;

import android.system.OsConstants;
import java.nio.ByteBuffer;

public class StructNdMsg {
    public static byte NTF_MASTER = (byte) 4;
    public static byte NTF_PROXY = (byte) 8;
    public static byte NTF_ROUTER = Byte.MIN_VALUE;
    public static byte NTF_SELF = (byte) 2;
    public static byte NTF_USE = (byte) 1;
    public static final short NUD_DELAY = (short) 8;
    public static final short NUD_FAILED = (short) 32;
    public static final short NUD_INCOMPLETE = (short) 1;
    public static final short NUD_NOARP = (short) 64;
    public static final short NUD_NONE = (short) 0;
    public static final short NUD_PERMANENT = (short) 128;
    public static final short NUD_PROBE = (short) 16;
    public static final short NUD_REACHABLE = (short) 2;
    public static final short NUD_STALE = (short) 4;
    public static final int STRUCT_SIZE = 12;
    public byte ndm_family = ((byte) OsConstants.AF_UNSPEC);
    public byte ndm_flags;
    public int ndm_ifindex;
    public short ndm_state;
    public byte ndm_type;

    public static String stringForNudState(short nudState) {
        if (nudState == (short) 4) {
            return "NUD_STALE";
        }
        if (nudState == (short) 8) {
            return "NUD_DELAY";
        }
        if (nudState == (short) 16) {
            return "NUD_PROBE";
        }
        if (nudState == (short) 32) {
            return "NUD_FAILED";
        }
        if (nudState == (short) 64) {
            return "NUD_NOARP";
        }
        if (nudState == NUD_PERMANENT) {
            return "NUD_PERMANENT";
        }
        switch (nudState) {
            case (short) 0:
                return "NUD_NONE";
            case (short) 1:
                return "NUD_INCOMPLETE";
            case (short) 2:
                return "NUD_REACHABLE";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unknown NUD state: ");
                stringBuilder.append(String.valueOf(nudState));
                return stringBuilder.toString();
        }
    }

    public static boolean isNudStateConnected(short nudState) {
        return (nudState & HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_NEUTRAL) != 0;
    }

    public static boolean isNudStateValid(short nudState) {
        return isNudStateConnected(nudState) || (nudState & 28) != 0;
    }

    public static String stringForNudFlags(byte flags) {
        StringBuilder sb = new StringBuilder();
        if ((NTF_USE & flags) != 0) {
            sb.append("NTF_USE");
        }
        if ((NTF_SELF & flags) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("NTF_SELF");
        }
        if ((NTF_MASTER & flags) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("NTF_MASTER");
        }
        if ((NTF_PROXY & flags) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("NTF_PROXY");
        }
        if ((NTF_ROUTER & flags) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("NTF_ROUTER");
        }
        return sb.toString();
    }

    private static boolean hasAvailableSpace(ByteBuffer byteBuffer) {
        return byteBuffer != null && byteBuffer.remaining() >= 12;
    }

    public static StructNdMsg parse(ByteBuffer byteBuffer) {
        if (!hasAvailableSpace(byteBuffer)) {
            return null;
        }
        StructNdMsg struct = new StructNdMsg();
        struct.ndm_family = byteBuffer.get();
        byte pad1 = byteBuffer.get();
        short pad2 = byteBuffer.getShort();
        struct.ndm_ifindex = byteBuffer.getInt();
        struct.ndm_state = byteBuffer.getShort();
        struct.ndm_flags = byteBuffer.get();
        struct.ndm_type = byteBuffer.get();
        return struct;
    }

    public void pack(ByteBuffer byteBuffer) {
        byteBuffer.put(this.ndm_family);
        byteBuffer.put((byte) 0);
        byteBuffer.putShort((short) 0);
        byteBuffer.putInt(this.ndm_ifindex);
        byteBuffer.putShort(this.ndm_state);
        byteBuffer.put(this.ndm_flags);
        byteBuffer.put(this.ndm_type);
    }

    public boolean nudConnected() {
        return isNudStateConnected(this.ndm_state);
    }

    public boolean nudValid() {
        return isNudStateValid(this.ndm_state);
    }

    public String toString() {
        String stateStr = new StringBuilder();
        stateStr.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        stateStr.append(this.ndm_state);
        stateStr.append(" (");
        stateStr.append(stringForNudState(this.ndm_state));
        stateStr.append(")");
        stateStr = stateStr.toString();
        String flagsStr = new StringBuilder();
        flagsStr.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        flagsStr.append(this.ndm_flags);
        flagsStr.append(" (");
        flagsStr.append(stringForNudFlags(this.ndm_flags));
        flagsStr.append(")");
        flagsStr = flagsStr.toString();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("StructNdMsg{ family{");
        stringBuilder.append(NetlinkConstants.stringForAddressFamily(this.ndm_family));
        stringBuilder.append("}, ifindex{");
        stringBuilder.append(this.ndm_ifindex);
        stringBuilder.append("}, state{");
        stringBuilder.append(stateStr);
        stringBuilder.append("}, flags{");
        stringBuilder.append(flagsStr);
        stringBuilder.append("}, type{");
        stringBuilder.append(this.ndm_type);
        stringBuilder.append("} }");
        return stringBuilder.toString();
    }
}
