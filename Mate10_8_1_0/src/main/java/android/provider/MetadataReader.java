package android.provider;

import android.media.ExifInterface;
import android.os.Bundle;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MetadataReader {
    private static final String[] ALL_KNOWN_EXIF_KEYS = ((String[]) TYPE_MAPPING.keySet().toArray(new String[TYPE_MAPPING.size()]));
    private static final String[] DEFAULT_EXIF_TAGS = new String[]{"ImageWidth", "ImageLength", "DateTime", "GPSLatitude", "GPSLongitude", "Make", "Model", "FNumber", "ShutterSpeedValue"};
    private static final String JPEG_MIME_TYPE = "image/jpeg";
    private static final String JPG_MIME_TYPE = "image/jpg";
    private static final int TYPE_DOUBLE = 1;
    private static final int TYPE_INT = 0;
    private static final Map<String, Integer> TYPE_MAPPING = new HashMap();
    private static final int TYPE_STRING = 2;

    private MetadataReader() {
    }

    static {
        TYPE_MAPPING.put("Artist", Integer.valueOf(2));
        TYPE_MAPPING.put("BitsPerSample", Integer.valueOf(0));
        TYPE_MAPPING.put("Compression", Integer.valueOf(0));
        TYPE_MAPPING.put("Copyright", Integer.valueOf(2));
        TYPE_MAPPING.put("DateTime", Integer.valueOf(2));
        TYPE_MAPPING.put("ImageDescription", Integer.valueOf(2));
        TYPE_MAPPING.put("ImageLength", Integer.valueOf(0));
        TYPE_MAPPING.put("ImageWidth", Integer.valueOf(0));
        TYPE_MAPPING.put("JPEGInterchangeFormat", Integer.valueOf(0));
        TYPE_MAPPING.put("JPEGInterchangeFormatLength", Integer.valueOf(0));
        TYPE_MAPPING.put("Make", Integer.valueOf(2));
        TYPE_MAPPING.put("Model", Integer.valueOf(2));
        TYPE_MAPPING.put("Orientation", Integer.valueOf(0));
        TYPE_MAPPING.put("PhotometricInterpretation", Integer.valueOf(0));
        TYPE_MAPPING.put("PlanarConfiguration", Integer.valueOf(0));
        TYPE_MAPPING.put("PrimaryChromaticities", Integer.valueOf(1));
        TYPE_MAPPING.put("ReferenceBlackWhite", Integer.valueOf(1));
        TYPE_MAPPING.put("ResolutionUnit", Integer.valueOf(0));
        TYPE_MAPPING.put("RowsPerStrip", Integer.valueOf(0));
        TYPE_MAPPING.put("SamplesPerPixel", Integer.valueOf(0));
        TYPE_MAPPING.put("Software", Integer.valueOf(2));
        TYPE_MAPPING.put("StripByteCounts", Integer.valueOf(0));
        TYPE_MAPPING.put("StripOffsets", Integer.valueOf(0));
        TYPE_MAPPING.put("TransferFunction", Integer.valueOf(0));
        TYPE_MAPPING.put("WhitePoint", Integer.valueOf(1));
        TYPE_MAPPING.put("XResolution", Integer.valueOf(1));
        TYPE_MAPPING.put("YCbCrCoefficients", Integer.valueOf(1));
        TYPE_MAPPING.put("YCbCrPositioning", Integer.valueOf(0));
        TYPE_MAPPING.put("YCbCrSubSampling", Integer.valueOf(0));
        TYPE_MAPPING.put("YResolution", Integer.valueOf(1));
        TYPE_MAPPING.put("ApertureValue", Integer.valueOf(1));
        TYPE_MAPPING.put("BrightnessValue", Integer.valueOf(1));
        TYPE_MAPPING.put("CFAPattern", Integer.valueOf(2));
        TYPE_MAPPING.put("ColorSpace", Integer.valueOf(0));
        TYPE_MAPPING.put("ComponentsConfiguration", Integer.valueOf(2));
        TYPE_MAPPING.put("CompressedBitsPerPixel", Integer.valueOf(1));
        TYPE_MAPPING.put("Contrast", Integer.valueOf(0));
        TYPE_MAPPING.put("CustomRendered", Integer.valueOf(0));
        TYPE_MAPPING.put("DateTimeDigitized", Integer.valueOf(2));
        TYPE_MAPPING.put("DateTimeOriginal", Integer.valueOf(2));
        TYPE_MAPPING.put("DeviceSettingDescription", Integer.valueOf(2));
        TYPE_MAPPING.put("DigitalZoomRatio", Integer.valueOf(1));
        TYPE_MAPPING.put("ExifVersion", Integer.valueOf(2));
        TYPE_MAPPING.put("ExposureBiasValue", Integer.valueOf(1));
        TYPE_MAPPING.put("ExposureIndex", Integer.valueOf(1));
        TYPE_MAPPING.put("ExposureMode", Integer.valueOf(0));
        TYPE_MAPPING.put("ExposureProgram", Integer.valueOf(0));
        TYPE_MAPPING.put("ExposureTime", Integer.valueOf(1));
        TYPE_MAPPING.put("FNumber", Integer.valueOf(1));
        TYPE_MAPPING.put("FileSource", Integer.valueOf(2));
        TYPE_MAPPING.put("Flash", Integer.valueOf(0));
        TYPE_MAPPING.put("FlashEnergy", Integer.valueOf(1));
        TYPE_MAPPING.put("FlashpixVersion", Integer.valueOf(2));
        TYPE_MAPPING.put("FocalLength", Integer.valueOf(1));
        TYPE_MAPPING.put("FocalLengthIn35mmFilm", Integer.valueOf(0));
        TYPE_MAPPING.put("FocalPlaneResolutionUnit", Integer.valueOf(0));
        TYPE_MAPPING.put("FocalPlaneXResolution", Integer.valueOf(1));
        TYPE_MAPPING.put("FocalPlaneYResolution", Integer.valueOf(1));
        TYPE_MAPPING.put("GainControl", Integer.valueOf(0));
        TYPE_MAPPING.put("ISOSpeedRatings", Integer.valueOf(0));
        TYPE_MAPPING.put("ImageUniqueID", Integer.valueOf(2));
        TYPE_MAPPING.put("LightSource", Integer.valueOf(0));
        TYPE_MAPPING.put("MakerNote", Integer.valueOf(2));
        TYPE_MAPPING.put("MaxApertureValue", Integer.valueOf(1));
        TYPE_MAPPING.put("MeteringMode", Integer.valueOf(0));
        TYPE_MAPPING.put("NewSubfileType", Integer.valueOf(0));
        TYPE_MAPPING.put("OECF", Integer.valueOf(2));
        TYPE_MAPPING.put("PixelXDimension", Integer.valueOf(0));
        TYPE_MAPPING.put("PixelYDimension", Integer.valueOf(0));
        TYPE_MAPPING.put("RelatedSoundFile", Integer.valueOf(2));
        TYPE_MAPPING.put("Saturation", Integer.valueOf(0));
        TYPE_MAPPING.put("SceneCaptureType", Integer.valueOf(0));
        TYPE_MAPPING.put("SceneType", Integer.valueOf(2));
        TYPE_MAPPING.put("SensingMethod", Integer.valueOf(0));
        TYPE_MAPPING.put("Sharpness", Integer.valueOf(0));
        TYPE_MAPPING.put("ShutterSpeedValue", Integer.valueOf(1));
        TYPE_MAPPING.put("SpatialFrequencyResponse", Integer.valueOf(2));
        TYPE_MAPPING.put("SpectralSensitivity", Integer.valueOf(2));
        TYPE_MAPPING.put("SubfileType", Integer.valueOf(0));
        TYPE_MAPPING.put("SubSecTime", Integer.valueOf(2));
        TYPE_MAPPING.put("SubSecTimeDigitized", Integer.valueOf(2));
        TYPE_MAPPING.put("SubSecTimeOriginal", Integer.valueOf(2));
        TYPE_MAPPING.put("SubjectArea", Integer.valueOf(0));
        TYPE_MAPPING.put("SubjectDistance", Integer.valueOf(1));
        TYPE_MAPPING.put("SubjectDistanceRange", Integer.valueOf(0));
        TYPE_MAPPING.put("SubjectLocation", Integer.valueOf(0));
        TYPE_MAPPING.put("UserComment", Integer.valueOf(2));
        TYPE_MAPPING.put("WhiteBalance", Integer.valueOf(0));
        TYPE_MAPPING.put("GPSAltitude", Integer.valueOf(1));
        TYPE_MAPPING.put("GPSAltitudeRef", Integer.valueOf(0));
        TYPE_MAPPING.put("GPSAreaInformation", Integer.valueOf(2));
        TYPE_MAPPING.put("GPSDOP", Integer.valueOf(1));
        TYPE_MAPPING.put("GPSDateStamp", Integer.valueOf(2));
        TYPE_MAPPING.put("GPSDestBearing", Integer.valueOf(1));
        TYPE_MAPPING.put("GPSDestBearingRef", Integer.valueOf(2));
        TYPE_MAPPING.put("GPSDestDistance", Integer.valueOf(1));
        TYPE_MAPPING.put("GPSDestDistanceRef", Integer.valueOf(2));
        TYPE_MAPPING.put("GPSDestLatitude", Integer.valueOf(1));
        TYPE_MAPPING.put("GPSDestLatitudeRef", Integer.valueOf(2));
        TYPE_MAPPING.put("GPSDestLongitude", Integer.valueOf(1));
        TYPE_MAPPING.put("GPSDestLongitudeRef", Integer.valueOf(2));
        TYPE_MAPPING.put("GPSDifferential", Integer.valueOf(0));
        TYPE_MAPPING.put("GPSImgDirection", Integer.valueOf(1));
        TYPE_MAPPING.put("GPSImgDirectionRef", Integer.valueOf(2));
        TYPE_MAPPING.put("GPSLatitude", Integer.valueOf(1));
        TYPE_MAPPING.put("GPSLatitudeRef", Integer.valueOf(2));
        TYPE_MAPPING.put("GPSLongitude", Integer.valueOf(1));
        TYPE_MAPPING.put("GPSLongitudeRef", Integer.valueOf(2));
        TYPE_MAPPING.put("GPSMapDatum", Integer.valueOf(2));
        TYPE_MAPPING.put("GPSMeasureMode", Integer.valueOf(2));
        TYPE_MAPPING.put("GPSProcessingMethod", Integer.valueOf(2));
        TYPE_MAPPING.put("GPSSatellites", Integer.valueOf(2));
        TYPE_MAPPING.put("GPSSpeed", Integer.valueOf(1));
        TYPE_MAPPING.put("GPSSpeedRef", Integer.valueOf(2));
        TYPE_MAPPING.put("GPSStatus", Integer.valueOf(2));
        TYPE_MAPPING.put("GPSTimeStamp", Integer.valueOf(2));
        TYPE_MAPPING.put("GPSTrack", Integer.valueOf(1));
        TYPE_MAPPING.put("GPSTrackRef", Integer.valueOf(2));
        TYPE_MAPPING.put("GPSVersionID", Integer.valueOf(2));
        TYPE_MAPPING.put("InteroperabilityIndex", Integer.valueOf(2));
        TYPE_MAPPING.put("ThumbnailImageLength", Integer.valueOf(0));
        TYPE_MAPPING.put("ThumbnailImageWidth", Integer.valueOf(0));
        TYPE_MAPPING.put("DNGVersion", Integer.valueOf(0));
        TYPE_MAPPING.put("DefaultCropSize", Integer.valueOf(0));
        TYPE_MAPPING.put("PreviewImageStart", Integer.valueOf(0));
        TYPE_MAPPING.put("PreviewImageLength", Integer.valueOf(0));
        TYPE_MAPPING.put("AspectFrame", Integer.valueOf(0));
        TYPE_MAPPING.put("SensorBottomBorder", Integer.valueOf(0));
        TYPE_MAPPING.put("SensorLeftBorder", Integer.valueOf(0));
        TYPE_MAPPING.put("SensorRightBorder", Integer.valueOf(0));
        TYPE_MAPPING.put("SensorTopBorder", Integer.valueOf(0));
        TYPE_MAPPING.put("ISO", Integer.valueOf(0));
    }

    public static void getMetadata(Bundle metadata, InputStream stream, String mimeType, String[] tags) throws IOException {
        List<String> metadataTypes = new ArrayList();
        if (mimeType.equals(JPG_MIME_TYPE) || mimeType.equals(JPEG_MIME_TYPE)) {
            Bundle exifData = getExifData(new ExifInterface(stream), tags);
            if (exifData.size() > 0) {
                metadata.putBundle(DocumentsContract.METADATA_EXIF, exifData);
                metadataTypes.add(DocumentsContract.METADATA_EXIF);
            }
        }
        metadata.putStringArray(DocumentsContract.METADATA_TYPES, (String[]) metadataTypes.toArray(new String[metadataTypes.size()]));
    }

    private static Bundle getExifData(ExifInterface exifInterface, String[] tags) throws IOException {
        if (tags == null) {
            tags = DEFAULT_EXIF_TAGS;
        }
        Bundle exif = new Bundle();
        for (int i = 0; i < tags.length; i++) {
            if (((Integer) TYPE_MAPPING.get(tags[i])).equals(Integer.valueOf(0))) {
                int data = exifInterface.getAttributeInt(tags[i], Integer.MIN_VALUE);
                if (data != Integer.MIN_VALUE) {
                    exif.putInt(tags[i], data);
                }
            } else if (((Integer) TYPE_MAPPING.get(tags[i])).equals(Integer.valueOf(1))) {
                double data2 = exifInterface.getAttributeDouble(tags[i], Double.MIN_VALUE);
                if (data2 != Double.MIN_VALUE) {
                    exif.putDouble(tags[i], data2);
                }
            } else if (((Integer) TYPE_MAPPING.get(tags[i])).equals(Integer.valueOf(2))) {
                String data3 = exifInterface.getAttribute(tags[i]);
                if (data3 != null) {
                    exif.putString(tags[i], data3);
                }
            }
        }
        return exif;
    }
}
