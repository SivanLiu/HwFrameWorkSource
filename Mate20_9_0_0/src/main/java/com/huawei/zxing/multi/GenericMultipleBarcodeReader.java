package com.huawei.zxing.multi;

import com.huawei.zxing.BinaryBitmap;
import com.huawei.zxing.DecodeHintType;
import com.huawei.zxing.NotFoundException;
import com.huawei.zxing.Reader;
import com.huawei.zxing.ReaderException;
import com.huawei.zxing.Result;
import com.huawei.zxing.ResultPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GenericMultipleBarcodeReader implements MultipleBarcodeReader {
    private static final int MAX_DEPTH = 4;
    private static final int MIN_DIMENSION_TO_RECUR = 100;
    private final Reader delegate;

    public GenericMultipleBarcodeReader(Reader delegate) {
        this.delegate = delegate;
    }

    public Result[] decodeMultiple(BinaryBitmap image) throws NotFoundException {
        return decodeMultiple(image, null);
    }

    public Result[] decodeMultiple(BinaryBitmap image, Map<DecodeHintType, ?> hints) throws NotFoundException {
        List<Result> results = new ArrayList();
        doDecodeMultiple(image, hints, results, 0, 0, 0);
        if (!results.isEmpty()) {
            return (Result[]) results.toArray(new Result[results.size()]);
        }
        throw NotFoundException.getNotFoundInstance();
    }

    private void doDecodeMultiple(BinaryBitmap image, Map<DecodeHintType, ?> hints, List<Result> results, int xOffset, int yOffset, int currentDepth) {
        List<Result> list;
        BinaryBitmap binaryBitmap = image;
        int i = xOffset;
        int i2 = yOffset;
        int i3 = currentDepth;
        if (i3 <= 4) {
            Map<DecodeHintType, ?> map;
            try {
                map = hints;
                try {
                    List list2;
                    Result result = this.delegate.decode(binaryBitmap, map);
                    boolean alreadyFound = false;
                    for (Result existingResult : results) {
                        if (existingResult.getText().equals(result.getText())) {
                            alreadyFound = true;
                            break;
                        }
                    }
                    boolean alreadyFound2 = alreadyFound;
                    if (alreadyFound2) {
                        list2 = results;
                    } else {
                        list2 = results;
                        list2.add(translateResultPoints(result, i, i2));
                    }
                    ResultPoint[] resultPoints = result.getResultPoints();
                    ResultPoint[] resultPointArr;
                    boolean z;
                    if (resultPoints == null) {
                        resultPointArr = resultPoints;
                        z = alreadyFound2;
                    } else if (resultPoints.length == 0) {
                        Result result2 = result;
                        resultPointArr = resultPoints;
                        z = alreadyFound2;
                    } else {
                        float maxY;
                        float maxX;
                        int height;
                        float maxX2;
                        int height2;
                        int width = image.getWidth();
                        int height3 = image.getHeight();
                        float minX = (float) width;
                        boolean minY = (float) height3;
                        result = resultPoints.length;
                        float maxY2 = 0.0f;
                        float minX2 = minX;
                        int minX3 = 0;
                        boolean minY2 = minY;
                        float maxX3 = 0.0f;
                        while (minX3 < result) {
                            Result result3 = result;
                            result = resultPoints[minX3];
                            float x = result.getX();
                            boolean y = result.getY();
                            if (x < minX2) {
                                minX2 = x;
                            }
                            if (y < minY2) {
                                minY2 = y;
                            }
                            if (x > maxX3) {
                                maxX3 = x;
                            }
                            if (y > maxY2) {
                                maxY2 = y;
                            }
                            minX3++;
                            result = result3;
                        }
                        if (minX2 > 100.0f) {
                            BinaryBitmap crop = binaryBitmap.crop(0, 0, (int) minX2, height3);
                            maxY = maxY2;
                            maxX = maxX3;
                            alreadyFound2 = minY2;
                            height = height3;
                            height3 = i;
                            i = width;
                            doDecodeMultiple(crop, map, list2, height3, i2, i3 + 1);
                        } else {
                            maxX = maxX3;
                            float f = minX2;
                            height = height3;
                            i = width;
                            resultPointArr = resultPoints;
                            maxY = maxY2;
                            alreadyFound2 = minY2;
                        }
                        if (alreadyFound2 > true) {
                            result = i;
                            i = xOffset;
                            doDecodeMultiple(binaryBitmap.crop(0, 0, i, (int) alreadyFound2), map, list2, i, i2, i3 + 1);
                        } else {
                            result = i;
                            i = xOffset;
                        }
                        if (maxX < ((float) (result - 100))) {
                            maxX2 = maxX;
                            height3 = height;
                            height2 = height3;
                            doDecodeMultiple(binaryBitmap.crop((int) maxX2, 0, result - ((int) maxX2), height3), map, list2, i + ((int) maxX2), i2, i3 + 1);
                        } else {
                            height2 = height;
                        }
                        if (maxY < ((float) (height2 - 100))) {
                            maxX2 = maxY;
                            doDecodeMultiple(binaryBitmap.crop(0, (int) maxX2, result, height2 - ((int) maxX2)), map, list2, i, i2 + ((int) maxX2), i3 + 1);
                        }
                    }
                } catch (ReaderException e) {
                    list = results;
                }
            } catch (ReaderException e2) {
                map = hints;
                list = results;
            }
        }
    }

    private static Result translateResultPoints(Result result, int xOffset, int yOffset) {
        ResultPoint[] oldResultPoints = result.getResultPoints();
        if (oldResultPoints == null) {
            return result;
        }
        ResultPoint[] newResultPoints = new ResultPoint[oldResultPoints.length];
        for (int i = 0; i < oldResultPoints.length; i++) {
            ResultPoint oldPoint = oldResultPoints[i];
            newResultPoints[i] = new ResultPoint(oldPoint.getX() + ((float) xOffset), oldPoint.getY() + ((float) yOffset));
        }
        Result newResult = new Result(result.getText(), result.getRawBytes(), newResultPoints, result.getBarcodeFormat());
        newResult.putAllMetadata(result.getResultMetadata());
        return newResult;
    }
}
