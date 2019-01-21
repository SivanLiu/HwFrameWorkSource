package com.huawei.zxing.qrcode;

import com.huawei.hwsqlite.SQLiteDatabase;
import com.huawei.zxing.BarcodeFormat;
import com.huawei.zxing.BinaryBitmap;
import com.huawei.zxing.ChecksumException;
import com.huawei.zxing.DecodeHintType;
import com.huawei.zxing.FormatException;
import com.huawei.zxing.NotFoundException;
import com.huawei.zxing.Reader;
import com.huawei.zxing.Result;
import com.huawei.zxing.ResultMetadataType;
import com.huawei.zxing.ResultPoint;
import com.huawei.zxing.common.BitMatrix;
import com.huawei.zxing.common.DecoderResult;
import com.huawei.zxing.qrcode.decoder.Decoder;
import com.huawei.zxing.qrcode.decoder.QRCodeDecoderMetaData;
import com.huawei.zxing.qrcode.detector.Detector;
import java.util.List;
import java.util.Map;

public class QRCodeReader implements Reader {
    private static final ResultPoint[] NO_POINTS = new ResultPoint[0];
    private final Decoder decoder = new Decoder();

    protected final Decoder getDecoder() {
        return this.decoder;
    }

    public Result decode(BinaryBitmap image) throws NotFoundException, ChecksumException, FormatException {
        return decode(image, null);
    }

    public final Result decode(BinaryBitmap image, Map<DecodeHintType, ?> hints) throws NotFoundException, ChecksumException, FormatException {
        ResultPoint[] points;
        DecoderResult decoderResult;
        if (hints == null || !hints.containsKey(DecodeHintType.PURE_BARCODE)) {
            points = new Detector(image.getBlackMatrix()).detect(hints);
            decoderResult = this.decoder.decode(points.getBits(), (Map) hints);
            points = points.getPoints();
        } else {
            decoderResult = this.decoder.decode(extractPureBits(image.getBlackMatrix()), (Map) hints);
            points = NO_POINTS;
        }
        if (decoderResult.getOther() instanceof QRCodeDecoderMetaData) {
            ((QRCodeDecoderMetaData) decoderResult.getOther()).applyMirroredCorrection(points);
        }
        Result result = new Result(decoderResult.getText(), decoderResult.getRawBytes(), points, BarcodeFormat.QR_CODE);
        List<byte[]> byteSegments = decoderResult.getByteSegments();
        if (byteSegments != null) {
            result.putMetadata(ResultMetadataType.BYTE_SEGMENTS, byteSegments);
        }
        String ecLevel = decoderResult.getECLevel();
        if (ecLevel != null) {
            result.putMetadata(ResultMetadataType.ERROR_CORRECTION_LEVEL, ecLevel);
        }
        return result;
    }

    public void reset() {
    }

    private static BitMatrix extractPureBits(BitMatrix image) throws NotFoundException {
        BitMatrix bitMatrix = image;
        int[] leftTopBlack = image.getTopLeftOnBit();
        int[] rightBottomBlack = image.getBottomRightOnBit();
        int[] iArr;
        if (leftTopBlack == null || rightBottomBlack == null) {
            iArr = rightBottomBlack;
            throw NotFoundException.getNotFoundInstance();
        }
        float moduleSize = moduleSize(leftTopBlack, bitMatrix);
        int top = leftTopBlack[1];
        int bottom = rightBottomBlack[1];
        int left = leftTopBlack[0];
        int right = rightBottomBlack[0];
        if (left >= right || top >= bottom) {
            iArr = rightBottomBlack;
            throw NotFoundException.getNotFoundInstance();
        }
        if (bottom - top != right - left) {
            right = left + (bottom - top);
        }
        int matrixWidth = Math.round(((float) ((right - left) + 1)) / moduleSize);
        int matrixHeight = Math.round(((float) ((bottom - top) + 1)) / moduleSize);
        if (matrixWidth <= 0 || matrixHeight <= 0) {
            iArr = rightBottomBlack;
            throw NotFoundException.getNotFoundInstance();
        } else if (matrixHeight == matrixWidth) {
            int nudge = (int) (moduleSize / SQLiteDatabase.ENABLE_DATABASE_ENCRYPTION);
            top += nudge;
            left += nudge;
            int nudgedTooFarRight = (((int) (((float) (matrixWidth - 1)) * moduleSize)) + left) - (right - 1);
            if (nudgedTooFarRight > 0) {
                if (nudgedTooFarRight <= nudge) {
                    left -= nudgedTooFarRight;
                } else {
                    throw NotFoundException.getNotFoundInstance();
                }
            }
            int nudgedTooFarDown = (((int) (((float) (matrixHeight - 1)) * moduleSize)) + top) - (bottom - 1);
            if (nudgedTooFarDown > 0) {
                if (nudgedTooFarDown <= nudge) {
                    top -= nudgedTooFarDown;
                } else {
                    throw NotFoundException.getNotFoundInstance();
                }
            }
            BitMatrix bits = new BitMatrix(matrixWidth, matrixHeight);
            int y = 0;
            while (y < matrixHeight) {
                int[] leftTopBlack2;
                int iOffset = ((int) (((float) y) * moduleSize)) + top;
                int x = 0;
                while (true) {
                    leftTopBlack2 = leftTopBlack;
                    leftTopBlack = x;
                    if (leftTopBlack >= matrixWidth) {
                        break;
                    }
                    iArr = rightBottomBlack;
                    if (bitMatrix.get(((int) (((float) leftTopBlack) * moduleSize)) + left, iOffset)) {
                        bits.set(leftTopBlack, y);
                    }
                    x = leftTopBlack + 1;
                    leftTopBlack = leftTopBlack2;
                    rightBottomBlack = iArr;
                }
                y++;
                leftTopBlack = leftTopBlack2;
            }
            iArr = rightBottomBlack;
            return bits;
        } else {
            iArr = rightBottomBlack;
            throw NotFoundException.getNotFoundInstance();
        }
    }

    private static float moduleSize(int[] leftTopBlack, BitMatrix image) throws NotFoundException {
        int height = image.getHeight();
        int width = image.getWidth();
        int x = leftTopBlack[0];
        boolean inBlack = true;
        int y = leftTopBlack[1];
        int x2 = x;
        x = 0;
        while (x2 < width && y < height) {
            if (inBlack != image.get(x2, y)) {
                x++;
                if (x == 5) {
                    break;
                }
                inBlack = !inBlack;
            }
            x2++;
            y++;
        }
        if (x2 != width && y != height) {
            return ((float) (x2 - leftTopBlack[0])) / 7.0f;
        }
        throw NotFoundException.getNotFoundInstance();
    }
}
