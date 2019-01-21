package android.util.apk;

import java.nio.ByteBuffer;

class SignatureInfo {
    public final long apkSigningBlockOffset;
    public final long centralDirOffset;
    public final ByteBuffer eocd;
    public final long eocdOffset;
    public final ByteBuffer signatureBlock;

    SignatureInfo(ByteBuffer signatureBlock, long apkSigningBlockOffset, long centralDirOffset, long eocdOffset, ByteBuffer eocd) {
        this.signatureBlock = signatureBlock;
        this.apkSigningBlockOffset = apkSigningBlockOffset;
        this.centralDirOffset = centralDirOffset;
        this.eocdOffset = eocdOffset;
        this.eocd = eocd;
    }
}
