package android.renderscript;

import android.content.res.AssetManager.AssetInputStream;
import android.content.res.Resources;
import android.os.Environment;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Font extends BaseObj {
    private static Map<String, FontFamily> sFontFamilyMap;
    private static final String[] sMonoNames = new String[]{"monospace", "courier", "courier new", "monaco"};
    private static final String[] sSansNames = new String[]{"sans-serif", "arial", "helvetica", "tahoma", "verdana"};
    private static final String[] sSerifNames = new String[]{"serif", "times", "times new roman", "palatino", "georgia", "baskerville", "goudy", "fantasy", "cursive", "ITC Stone Serif"};

    private static class FontFamily {
        String mBoldFileName;
        String mBoldItalicFileName;
        String mItalicFileName;
        String[] mNames;
        String mNormalFileName;

        private FontFamily() {
        }
    }

    public enum Style {
        NORMAL,
        BOLD,
        ITALIC,
        BOLD_ITALIC
    }

    static {
        initFontFamilyMap();
    }

    private static void addFamilyToMap(FontFamily family) {
        for (Object put : family.mNames) {
            sFontFamilyMap.put(put, family);
        }
    }

    private static void initFontFamilyMap() {
        sFontFamilyMap = new HashMap();
        FontFamily sansFamily = new FontFamily();
        sansFamily.mNames = sSansNames;
        sansFamily.mNormalFileName = "Roboto-Regular.ttf";
        sansFamily.mBoldFileName = "Roboto-Bold.ttf";
        sansFamily.mItalicFileName = "Roboto-Italic.ttf";
        sansFamily.mBoldItalicFileName = "Roboto-BoldItalic.ttf";
        addFamilyToMap(sansFamily);
        FontFamily serifFamily = new FontFamily();
        serifFamily.mNames = sSerifNames;
        serifFamily.mNormalFileName = "NotoSerif-Regular.ttf";
        serifFamily.mBoldFileName = "NotoSerif-Bold.ttf";
        serifFamily.mItalicFileName = "NotoSerif-Italic.ttf";
        serifFamily.mBoldItalicFileName = "NotoSerif-BoldItalic.ttf";
        addFamilyToMap(serifFamily);
        FontFamily monoFamily = new FontFamily();
        monoFamily.mNames = sMonoNames;
        monoFamily.mNormalFileName = "DroidSansMono.ttf";
        monoFamily.mBoldFileName = "DroidSansMono.ttf";
        monoFamily.mItalicFileName = "DroidSansMono.ttf";
        monoFamily.mBoldItalicFileName = "DroidSansMono.ttf";
        addFamilyToMap(monoFamily);
    }

    static String getFontFileName(String familyName, Style style) {
        FontFamily family = (FontFamily) sFontFamilyMap.get(familyName);
        if (family != null) {
            switch (style) {
                case NORMAL:
                    return family.mNormalFileName;
                case BOLD:
                    return family.mBoldFileName;
                case ITALIC:
                    return family.mItalicFileName;
                case BOLD_ITALIC:
                    return family.mBoldItalicFileName;
            }
        }
        return "DroidSans.ttf";
    }

    Font(long id, RenderScript rs) {
        super(id, rs);
        this.guard.open("destroy");
    }

    public static Font createFromFile(RenderScript rs, Resources res, String path, float pointSize) {
        rs.validate();
        long fontId = rs.nFontCreateFromFile(path, pointSize, res.getDisplayMetrics().densityDpi);
        if (fontId != 0) {
            return new Font(fontId, rs);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unable to create font from file ");
        stringBuilder.append(path);
        throw new RSRuntimeException(stringBuilder.toString());
    }

    public static Font createFromFile(RenderScript rs, Resources res, File path, float pointSize) {
        return createFromFile(rs, res, path.getAbsolutePath(), pointSize);
    }

    public static Font createFromAsset(RenderScript rs, Resources res, String path, float pointSize) {
        rs.validate();
        long fontId = rs.nFontCreateFromAsset(res.getAssets(), path, pointSize, res.getDisplayMetrics().densityDpi);
        if (fontId != 0) {
            return new Font(fontId, rs);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unable to create font from asset ");
        stringBuilder.append(path);
        throw new RSRuntimeException(stringBuilder.toString());
    }

    public static Font createFromResource(RenderScript rs, Resources res, int id, float pointSize) {
        int i = id;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("R.");
        stringBuilder.append(Integer.toString(id));
        String name = stringBuilder.toString();
        rs.validate();
        InputStream is = null;
        RenderScript renderScript;
        try {
            InputStream is2 = res.openRawResource(id);
            int dpi = res.getDisplayMetrics().densityDpi;
            if (is2 instanceof AssetInputStream) {
                long fontId = rs.nFontCreateFromAssetStream(name, pointSize, dpi, ((AssetInputStream) is2).getNativeAsset());
                if (fontId != 0) {
                    return new Font(fontId, rs);
                }
                renderScript = rs;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unable to create font from resource ");
                stringBuilder2.append(i);
                throw new RSRuntimeException(stringBuilder2.toString());
            }
            renderScript = rs;
            throw new RSRuntimeException("Unsupported asset stream created");
        } catch (Exception e) {
            renderScript = rs;
            Exception exception = e;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Unable to open resource ");
            stringBuilder3.append(i);
            throw new RSRuntimeException(stringBuilder3.toString());
        }
    }

    public static Font create(RenderScript rs, Resources res, String familyName, Style fontStyle, float pointSize) {
        String fileName = getFontFileName(familyName, fontStyle);
        String fontPath = Environment.getRootDirectory().getAbsolutePath();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(fontPath);
        stringBuilder.append("/fonts/");
        stringBuilder.append(fileName);
        return createFromFile(rs, res, stringBuilder.toString(), pointSize);
    }
}
