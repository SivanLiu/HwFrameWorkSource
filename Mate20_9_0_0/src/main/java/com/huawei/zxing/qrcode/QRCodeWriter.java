package com.huawei.zxing.qrcode;

import com.huawei.zxing.BarcodeFormat;
import com.huawei.zxing.EncodeHintType;
import com.huawei.zxing.Writer;
import com.huawei.zxing.WriterException;
import com.huawei.zxing.common.BitMatrix;
import com.huawei.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.huawei.zxing.qrcode.encoder.ByteMatrix;
import com.huawei.zxing.qrcode.encoder.Encoder;
import com.huawei.zxing.qrcode.encoder.QRCode;
import java.util.Map;

public final class QRCodeWriter implements Writer {
    private static final int QUIET_ZONE_SIZE = 4;

    public BitMatrix encode(String contents, BarcodeFormat format, int width, int height) throws WriterException {
        return encode(contents, format, width, height, null);
    }

    public BitMatrix encode(String contents, BarcodeFormat format, int width, int height, Map<EncodeHintType, ?> hints) throws WriterException {
        StringBuilder stringBuilder;
        if (contents.isEmpty()) {
            throw new IllegalArgumentException("Found empty contents");
        } else if (format != BarcodeFormat.QR_CODE) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Can only encode QR_CODE, but got ");
            stringBuilder.append(format);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (width < 0 || height < 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Requested dimensions are too small: ");
            stringBuilder.append(width);
            stringBuilder.append('x');
            stringBuilder.append(height);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else {
            ErrorCorrectionLevel errorCorrectionLevel = ErrorCorrectionLevel.L;
            int quietZone = 4;
            if (hints != null) {
                ErrorCorrectionLevel requestedECLevel = (ErrorCorrectionLevel) hints.get(EncodeHintType.ERROR_CORRECTION);
                if (requestedECLevel != null) {
                    errorCorrectionLevel = requestedECLevel;
                }
                Integer quietZoneInt = (Integer) hints.get(EncodeHintType.MARGIN);
                if (quietZoneInt != null) {
                    quietZone = quietZoneInt.intValue();
                }
            }
            return renderResult(Encoder.encode(contents, errorCorrectionLevel, hints), width, height, quietZone);
        }
    }

    private static BitMatrix renderResult(QRCode code, int width, int height, int quietZone) {
        ByteMatrix input = code.getMatrix();
        ByteMatrix input2;
        if (input != null) {
            int inputWidth;
            int inputWidth2 = input.getWidth();
            int inputHeight = input.getHeight();
            int qrWidth = (quietZone << 1) + inputWidth2;
            int qrHeight = (quietZone << 1) + inputHeight;
            int outputWidth = Math.max(width, qrWidth);
            int outputHeight = Math.max(height, qrHeight);
            int multiple = Math.min(outputWidth / qrWidth, outputHeight / qrHeight);
            int leftPadding = (outputWidth - (inputWidth2 * multiple)) / 2;
            int topPadding = (outputHeight - (inputHeight * multiple)) / 2;
            BitMatrix output = new BitMatrix(outputWidth, outputHeight);
            int inputY = 0;
            int outputY = topPadding;
            while (inputY < inputHeight) {
                int inputX = 0;
                int outputX = leftPadding;
                while (true) {
                    int outputX2 = outputX;
                    if (inputX >= inputWidth2) {
                        break;
                    }
                    inputWidth = inputWidth2;
                    input2 = input;
                    if (input.get(inputX, inputY) == (byte) 1) {
                        input = outputX2;
                        output.setRegion(input, outputY, multiple, multiple);
                    } else {
                        input = outputX2;
                    }
                    inputX++;
                    outputX = input + multiple;
                    inputWidth2 = inputWidth;
                    input = input2;
                }
                inputWidth = inputWidth2;
                inputY++;
                outputY += multiple;
            }
            inputWidth = inputWidth2;
            return output;
        }
        int i = width;
        int i2 = height;
        input2 = input;
        throw new IllegalStateException();
    }
}
