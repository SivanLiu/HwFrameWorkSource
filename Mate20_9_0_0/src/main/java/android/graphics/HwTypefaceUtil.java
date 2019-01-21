package android.graphics;

import android.os.SystemProperties;
import android.text.FontConfig.Family;
import android.text.FontConfig.Font;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;

public class HwTypefaceUtil {
    private static final String HWFONT_CONFIG_LOCATION = "/data/skin/fonts/";
    public static final String HWFONT_NAME = "HwFont.ttf";
    private static final String HWFONT_XMLCONFIG_TAG = "hw";
    public static final String HW_CUSTOM_FONT_NAME = SystemProperties.get("ro.config.custom_font", null);
    public static final String HW_CUSTOM_FONT_PATH = "data/hw_init/product/fonts/";
    public static final String HW_DEFAULT_FONT_NAME = "DroidSansChinese.ttf";
    private static final String HW_FONTS_CONFIG_NAME = "HwEnglishfonts.xml";
    private static final String TAG = "HwTypeface";

    private static File getHwFontConfig() {
        return new File(HWFONT_CONFIG_LOCATION);
    }

    private static Family getMultyWeightHwFamily() {
        File systemFontConfigLocation = getHwFontConfig();
        if (systemFontConfigLocation.exists()) {
            return parseHwFontConfig(new File(systemFontConfigLocation, HW_FONTS_CONFIG_NAME));
        }
        return null;
    }

    private static Family parseHwFontConfig(File configFile) {
        String str;
        StringBuilder stringBuilder;
        Family hwFamily = null;
        try {
            Family[] familyArray = FontListParser.parse(new FileInputStream(configFile)).getFamilies();
            int i = 0;
            if (familyArray != null && familyArray.length > 0) {
                hwFamily = familyArray[0];
            }
            if (hwFamily == null) {
                return null;
            }
            Font[] fonts = hwFamily.getFonts();
            int length = fonts.length;
            while (i < length) {
                Font font = fonts[i];
                if (font.getFontName() != null) {
                    font.setFontName(font.getFontName().replaceAll(HWFONT_XMLCONFIG_TAG, getHwFontConfig().getAbsolutePath()));
                }
                i++;
            }
            return hwFamily;
        } catch (RuntimeException e) {
            hwFamily = null;
            Log.w(TAG, "Didn't create  family (most likely, non-Minikin build)", e);
            return hwFamily;
        } catch (FileNotFoundException e2) {
            hwFamily = null;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error opening ");
            stringBuilder.append(configFile);
            Log.e(str, stringBuilder.toString(), e2);
            return hwFamily;
        } catch (IOException e3) {
            hwFamily = null;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error reading ");
            stringBuilder.append(configFile);
            Log.e(str, stringBuilder.toString(), e3);
            return hwFamily;
        } catch (XmlPullParserException e4) {
            hwFamily = null;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("XML parse exception for ");
            stringBuilder.append(configFile);
            Log.e(str, stringBuilder.toString(), e4);
            return hwFamily;
        }
    }

    public static void updateFont(FontFamily HwFontFamily) {
        if (HwFontFamily != null) {
            boolean currentHwFontsExist = false;
            Family hwMultyWeightFamily = null;
            File hwfontfile = new File(getHwFontConfig(), HW_DEFAULT_FONT_NAME);
            if (hwfontfile.exists()) {
                currentHwFontsExist = true;
            }
            if (!currentHwFontsExist) {
                hwMultyWeightFamily = getMultyWeightHwFamily();
            }
            HwFontFamily.resetFont();
            if (currentHwFontsExist) {
                HwFontFamily.hwAddFont(hwfontfile.getAbsolutePath(), 0, -1, -1);
                HwFontFamily.setHwFontFamilyType(2);
            } else if (hwMultyWeightFamily != null) {
                for (Font font : hwMultyWeightFamily.getFonts()) {
                    if (font.getFontName() != null && new File(font.getFontName()).exists()) {
                        String fontName = font.getFontName();
                        int weight = font.getWeight();
                        if (font.isItalic()) {
                        }
                        HwFontFamily.hwAddFont(fontName, 0, weight, 0);
                        HwFontFamily.setHwFontFamilyType(2);
                    }
                }
            } else {
                HwFontFamily.setHwFontFamilyType(-1);
            }
            HwFontFamily.resetCoverage();
        }
    }
}
