package android.net.netlink;

import java.nio.ByteBuffer;

public class NetlinkErrorMessage extends NetlinkMessage {
    private StructNlMsgErr mNlMsgErr = null;

    public static NetlinkErrorMessage parse(StructNlMsgHdr header, ByteBuffer byteBuffer) {
        NetlinkErrorMessage errorMsg = new NetlinkErrorMessage(header);
        errorMsg.mNlMsgErr = StructNlMsgErr.parse(byteBuffer);
        if (errorMsg.mNlMsgErr == null) {
            return null;
        }
        return errorMsg;
    }

    NetlinkErrorMessage(StructNlMsgHdr header) {
        super(header);
    }

    public StructNlMsgErr getNlMsgError() {
        return this.mNlMsgErr;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("NetlinkErrorMessage{ nlmsghdr{");
        stringBuilder.append(this.mHeader == null ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : this.mHeader.toString());
        stringBuilder.append("}, nlmsgerr{");
        stringBuilder.append(this.mNlMsgErr == null ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : this.mNlMsgErr.toString());
        stringBuilder.append("} }");
        return stringBuilder.toString();
    }
}
