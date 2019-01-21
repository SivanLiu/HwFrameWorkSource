package org.bouncycastle.util.io.pem;

import java.io.BufferedWriter;
import java.io.IOException;
import org.bouncycastle.util.encoders.Base64;

public class PemWriter extends BufferedWriter {
    private static final int LINE_LENGTH = 64;
    private char[] buf;
    private final int nlLength;

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x0018 in {2, 4, 5} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:242)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:52)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:42)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    public PemWriter(java.io.Writer r1) {
        /*
        r0 = this;
        r0.<init>(r1);
        r1 = 64;
        r1 = new char[r1];
        r0.buf = r1;
        r1 = org.bouncycastle.util.Strings.lineSeparator();
        if (r1 == 0) goto L_0x0016;
        r1 = r1.length();
        r0.nlLength = r1;
        return;
        r1 = 2;
        goto L_0x0013;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.util.io.pem.PemWriter.<init>(java.io.Writer):void");
    }

    private void writeEncoded(byte[] bArr) throws IOException {
        bArr = Base64.encode(bArr);
        int i = 0;
        while (i < bArr.length) {
            int i2 = 0;
            while (i2 != this.buf.length) {
                int i3 = i + i2;
                if (i3 >= bArr.length) {
                    break;
                }
                this.buf[i2] = (char) bArr[i3];
                i2++;
            }
            write(this.buf, 0, i2);
            newLine();
            i += this.buf.length;
        }
    }

    private void writePostEncapsulationBoundary(String str) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("-----END ");
        stringBuilder.append(str);
        stringBuilder.append("-----");
        write(stringBuilder.toString());
        newLine();
    }

    private void writePreEncapsulationBoundary(String str) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("-----BEGIN ");
        stringBuilder.append(str);
        stringBuilder.append("-----");
        write(stringBuilder.toString());
        newLine();
    }

    public int getOutputSize(PemObject pemObject) {
        int length = ((((pemObject.getType().length() + 10) + this.nlLength) * 2) + 6) + 4;
        if (!pemObject.getHeaders().isEmpty()) {
            for (PemHeader pemHeader : pemObject.getHeaders()) {
                length += ((pemHeader.getName().length() + ": ".length()) + pemHeader.getValue().length()) + this.nlLength;
            }
            length += this.nlLength;
        }
        int length2 = ((pemObject.getContent().length + 2) / 3) * 4;
        return length + (length2 + ((((length2 + 64) - 1) / 64) * this.nlLength));
    }

    public void writeObject(PemObjectGenerator pemObjectGenerator) throws IOException {
        PemObject generate = pemObjectGenerator.generate();
        writePreEncapsulationBoundary(generate.getType());
        if (!generate.getHeaders().isEmpty()) {
            for (PemHeader pemHeader : generate.getHeaders()) {
                write(pemHeader.getName());
                write(": ");
                write(pemHeader.getValue());
                newLine();
            }
            newLine();
        }
        writeEncoded(generate.getContent());
        writePostEncapsulationBoundary(generate.getType());
    }
}
