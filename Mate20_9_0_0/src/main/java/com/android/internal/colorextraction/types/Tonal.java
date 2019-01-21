package com.android.internal.colorextraction.types;

import android.app.WallpaperColors;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.util.MathUtils;
import android.util.Range;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.colorextraction.ColorExtractor.GradientColors;
import com.android.internal.graphics.ColorUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class Tonal implements ExtractionType {
    private static final boolean DEBUG = true;
    private static final float FIT_WEIGHT_H = 1.0f;
    private static final float FIT_WEIGHT_L = 10.0f;
    private static final float FIT_WEIGHT_S = 1.0f;
    public static final int MAIN_COLOR_DARK = -16777216;
    public static final int MAIN_COLOR_LIGHT = -2039584;
    private static final String TAG = "Tonal";
    public static final int THRESHOLD_COLOR_DARK = -14606047;
    public static final int THRESHOLD_COLOR_LIGHT = -2039584;
    private final ArrayList<ColorRange> mBlacklistedColors;
    private final TonalPalette mGreyPalette;
    private float[] mTmpHSL = new float[3];
    private final ArrayList<TonalPalette> mTonalPalettes;

    @VisibleForTesting
    public static class ColorRange {
        private Range<Float> mHue;
        private Range<Float> mLightness;
        private Range<Float> mSaturation;

        public ColorRange(Range<Float> hue, Range<Float> saturation, Range<Float> lightness) {
            this.mHue = hue;
            this.mSaturation = saturation;
            this.mLightness = lightness;
        }

        public boolean containsColor(float h, float s, float l) {
            if (this.mHue.contains(Float.valueOf(h)) && this.mSaturation.contains(Float.valueOf(s)) && this.mLightness.contains(Float.valueOf(l))) {
                return true;
            }
            return false;
        }

        public float[] getCenter() {
            return new float[]{((Float) this.mHue.getLower()).floatValue() + ((((Float) this.mHue.getUpper()).floatValue() - ((Float) this.mHue.getLower()).floatValue()) / 2.0f), ((Float) this.mSaturation.getLower()).floatValue() + ((((Float) this.mSaturation.getUpper()).floatValue() - ((Float) this.mSaturation.getLower()).floatValue()) / 2.0f), ((Float) this.mLightness.getLower()).floatValue() + ((((Float) this.mLightness.getUpper()).floatValue() - ((Float) this.mLightness.getLower()).floatValue()) / 2.0f)};
        }

        public String toString() {
            return String.format("H: %s, S: %s, L %s", new Object[]{this.mHue, this.mSaturation, this.mLightness});
        }
    }

    @VisibleForTesting
    public static class ConfigParser {
        private final ArrayList<ColorRange> mBlacklistedColors = new ArrayList();
        private final ArrayList<TonalPalette> mTonalPalettes = new ArrayList();

        public ConfigParser(Context context) {
            try {
                XmlPullParser parser = context.getResources().getXml(18284549);
                for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                    if (eventType != 0) {
                        if (eventType != 3) {
                            if (eventType == 2) {
                                String tagName = parser.getName();
                                if (tagName.equals("palettes")) {
                                    parsePalettes(parser);
                                } else if (tagName.equals("blacklist")) {
                                    parseBlacklist(parser);
                                }
                            } else {
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Invalid XML event ");
                                stringBuilder.append(eventType);
                                stringBuilder.append(" - ");
                                stringBuilder.append(parser.getName());
                                throw new XmlPullParserException(stringBuilder.toString(), parser, null);
                            }
                        }
                    }
                }
            } catch (IOException | XmlPullParserException e) {
                throw new RuntimeException(e);
            }
        }

        public ArrayList<TonalPalette> getTonalPalettes() {
            return this.mTonalPalettes;
        }

        public ArrayList<ColorRange> getBlacklistedColors() {
            return this.mBlacklistedColors;
        }

        private void parseBlacklist(XmlPullParser parser) throws XmlPullParserException, IOException {
            parser.require(2, null, "blacklist");
            while (parser.next() != 3) {
                if (parser.getEventType() == 2) {
                    String name = parser.getName();
                    if (name.equals("range")) {
                        this.mBlacklistedColors.add(readRange(parser));
                        parser.next();
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Invalid tag: ");
                        stringBuilder.append(name);
                        throw new XmlPullParserException(stringBuilder.toString(), parser, null);
                    }
                }
            }
        }

        private ColorRange readRange(XmlPullParser parser) throws XmlPullParserException, IOException {
            parser.require(2, null, "range");
            float[] h = readFloatArray(parser.getAttributeValue(null, "h"));
            float[] s = readFloatArray(parser.getAttributeValue(null, "s"));
            float[] l = readFloatArray(parser.getAttributeValue(null, "l"));
            if (h != null && s != null && l != null) {
                return new ColorRange(new Range(Float.valueOf(h[0]), Float.valueOf(h[1])), new Range(Float.valueOf(s[0]), Float.valueOf(s[1])), new Range(Float.valueOf(l[0]), Float.valueOf(l[1])));
            }
            throw new XmlPullParserException("Incomplete range tag.", parser, null);
        }

        private void parsePalettes(XmlPullParser parser) throws XmlPullParserException, IOException {
            parser.require(2, null, "palettes");
            while (parser.next() != 3) {
                if (parser.getEventType() == 2) {
                    String name = parser.getName();
                    if (name.equals("palette")) {
                        this.mTonalPalettes.add(readPalette(parser));
                        parser.next();
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Invalid tag: ");
                        stringBuilder.append(name);
                        throw new XmlPullParserException(stringBuilder.toString());
                    }
                }
            }
        }

        private TonalPalette readPalette(XmlPullParser parser) throws XmlPullParserException, IOException {
            parser.require(2, null, "palette");
            float[] h = readFloatArray(parser.getAttributeValue(null, "h"));
            float[] s = readFloatArray(parser.getAttributeValue(null, "s"));
            float[] l = readFloatArray(parser.getAttributeValue(null, "l"));
            if (h != null && s != null && l != null) {
                return new TonalPalette(h, s, l);
            }
            throw new XmlPullParserException("Incomplete range tag.", parser, null);
        }

        private float[] readFloatArray(String attributeValue) throws IOException, XmlPullParserException {
            String[] tokens = attributeValue.replaceAll(" ", "").replaceAll("\n", "").split(",");
            float[] numbers = new float[tokens.length];
            for (int i = 0; i < tokens.length; i++) {
                numbers[i] = Float.parseFloat(tokens[i]);
            }
            return numbers;
        }
    }

    @VisibleForTesting
    public static class TonalPalette {
        public final float[] h;
        public final float[] l;
        public final float maxHue;
        public final float minHue;
        public final float[] s;

        TonalPalette(float[] h, float[] s, float[] l) {
            if (h.length == s.length && s.length == l.length) {
                this.h = h;
                this.s = s;
                this.l = l;
                float minHue = Float.POSITIVE_INFINITY;
                float maxHue = Float.NEGATIVE_INFINITY;
                for (float v : h) {
                    minHue = Math.min(v, minHue);
                    maxHue = Math.max(v, maxHue);
                }
                this.minHue = minHue;
                this.maxHue = maxHue;
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("All arrays should have the same size. h: ");
            stringBuilder.append(Arrays.toString(h));
            stringBuilder.append(" s: ");
            stringBuilder.append(Arrays.toString(s));
            stringBuilder.append(" l: ");
            stringBuilder.append(Arrays.toString(l));
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public Tonal(Context context) {
        ConfigParser parser = new ConfigParser(context);
        this.mTonalPalettes = parser.getTonalPalettes();
        this.mBlacklistedColors = parser.getBlacklistedColors();
        this.mGreyPalette = (TonalPalette) this.mTonalPalettes.get(0);
        this.mTonalPalettes.remove(0);
    }

    public void extractInto(WallpaperColors inWallpaperColors, GradientColors outColorsNormal, GradientColors outColorsDark, GradientColors outColorsExtraDark) {
        if (!runTonalExtraction(inWallpaperColors, outColorsNormal, outColorsDark, outColorsExtraDark)) {
            applyFallback(inWallpaperColors, outColorsNormal, outColorsDark, outColorsExtraDark);
        }
    }

    private boolean runTonalExtraction(WallpaperColors inWallpaperColors, GradientColors outColorsNormal, GradientColors outColorsDark, GradientColors outColorsExtraDark) {
        GradientColors gradientColors = outColorsNormal;
        GradientColors gradientColors2 = outColorsDark;
        GradientColors gradientColors3 = outColorsExtraDark;
        if (inWallpaperColors == null) {
            return false;
        }
        List<Color> mainColors = inWallpaperColors.getMainColors();
        int mainColorsSize = mainColors.size();
        int hints = inWallpaperColors.getColorHints();
        boolean supportsDarkText = (hints & 1) != 0;
        boolean generatedFromBitmap = (hints & 4) != 0;
        if (mainColorsSize == 0) {
            return false;
        }
        int colorValue;
        Color bestColor = null;
        float[] hsl = new float[3];
        int i = 0;
        while (i < mainColorsSize) {
            Color color = (Color) mainColors.get(i);
            colorValue = color.toArgb();
            List<Color> mainColors2 = mainColors;
            ColorUtils.RGBToHSL(Color.red(colorValue), Color.green(colorValue), Color.blue(colorValue), hsl);
            if (!generatedFromBitmap || !isBlacklisted(hsl)) {
                bestColor = color;
                break;
            }
            i++;
            mainColors = mainColors2;
        }
        if (bestColor == null) {
            return false;
        }
        int colorValue2 = bestColor.toArgb();
        ColorUtils.RGBToHSL(Color.red(colorValue2), Color.green(colorValue2), Color.blue(colorValue2), hsl);
        hsl[0] = hsl[0] / 360.0f;
        TonalPalette palette = findTonalPalette(hsl[0], hsl[1]);
        if (palette == null) {
            Log.w(TAG, "Could not find a tonal palette!");
            return false;
        }
        colorValue = bestFit(palette, hsl[0], hsl[1], hsl[2]);
        if (colorValue == -1) {
            Log.w(TAG, "Could not find best fit!");
            return false;
        }
        int i2;
        float[] h = fit(palette.h, hsl[0], colorValue, Float.NEGATIVE_INFINITY, 2139095040);
        float[] s = fit(palette.s, hsl[1], colorValue, 0.0f, 1.0f);
        float[] l = fit(palette.l, hsl[2], colorValue, 0.0f, 1.0f);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Tonal Palette - index: ");
        stringBuilder.append(colorValue);
        stringBuilder.append(". Main color: ");
        stringBuilder.append(Integer.toHexString(getColorInt(colorValue, h, s, l)));
        stringBuilder.append("\nColors: ");
        StringBuilder builder = new StringBuilder(stringBuilder.toString());
        for (i2 = 0; i2 < h.length; i2++) {
            builder.append(Integer.toHexString(getColorInt(i2, h, s, l)));
            if (i2 < h.length - 1) {
                builder.append(", ");
            }
        }
        Log.d(TAG, builder.toString());
        mainColorsSize = colorValue;
        i2 = getColorInt(mainColorsSize, h, s, l);
        ColorUtils.colorToHSL(i2, this.mTmpHSL);
        float mainLuminosity = this.mTmpHSL[2];
        ColorUtils.colorToHSL(-2039584, this.mTmpHSL);
        float lightLuminosity = this.mTmpHSL[2];
        if (mainLuminosity > lightLuminosity) {
            return false;
        }
        ColorUtils.colorToHSL(THRESHOLD_COLOR_DARK, this.mTmpHSL);
        lightLuminosity = this.mTmpHSL[2];
        if (mainLuminosity < lightLuminosity) {
            return false;
        }
        gradientColors.setMainColor(i2);
        gradientColors.setSecondaryColor(i2);
        if (supportsDarkText) {
            hints = h.length - 1;
        } else if (colorValue < 2) {
            hints = 0;
        } else {
            hints = Math.min(colorValue, 3);
        }
        i2 = getColorInt(hints, h, s, l);
        gradientColors2.setMainColor(i2);
        gradientColors2.setSecondaryColor(i2);
        if (supportsDarkText) {
            mainColorsSize = h.length - 1;
        } else {
            if (colorValue < 2) {
                mainColorsSize = 0;
            } else {
                mainColorsSize = 2;
            }
        }
        hints = getColorInt(mainColorsSize, h, s, l);
        gradientColors3.setMainColor(hints);
        gradientColors3.setSecondaryColor(hints);
        gradientColors.setSupportsDarkText(supportsDarkText);
        gradientColors2.setSupportsDarkText(supportsDarkText);
        gradientColors3.setSupportsDarkText(supportsDarkText);
        String str = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Gradients: \n\tNormal ");
        stringBuilder2.append(gradientColors);
        stringBuilder2.append("\n\tDark ");
        stringBuilder2.append(gradientColors2);
        stringBuilder2.append("\n\tExtra dark: ");
        stringBuilder2.append(gradientColors3);
        Log.d(str, stringBuilder2.toString());
        return true;
    }

    private void applyFallback(WallpaperColors inWallpaperColors, GradientColors outColorsNormal, GradientColors outColorsDark, GradientColors outColorsExtraDark) {
        applyFallback(inWallpaperColors, outColorsNormal);
        applyFallback(inWallpaperColors, outColorsDark);
        applyFallback(inWallpaperColors, outColorsExtraDark);
    }

    public static void applyFallback(WallpaperColors inWallpaperColors, GradientColors outGradientColors) {
        boolean light = true;
        if (inWallpaperColors == null || (inWallpaperColors.getColorHints() & 1) == 0) {
            light = false;
        }
        int color = light ? -2039584 : MAIN_COLOR_DARK;
        outGradientColors.setMainColor(color);
        outGradientColors.setSecondaryColor(color);
        outGradientColors.setSupportsDarkText(light);
    }

    private int getColorInt(int fitIndex, float[] h, float[] s, float[] l) {
        this.mTmpHSL[0] = fract(h[fitIndex]) * 360.0f;
        this.mTmpHSL[1] = s[fitIndex];
        this.mTmpHSL[2] = l[fitIndex];
        return ColorUtils.HSLToColor(this.mTmpHSL);
    }

    private boolean isBlacklisted(float[] hsl) {
        for (int i = this.mBlacklistedColors.size() - 1; i >= 0; i--) {
            if (((ColorRange) this.mBlacklistedColors.get(i)).containsColor(hsl[0], hsl[1], hsl[2])) {
                return true;
            }
        }
        return false;
    }

    private static float[] fit(float[] data, float v, int index, float min, float max) {
        float[] fitData = new float[data.length];
        float delta = v - data[index];
        for (int i = 0; i < data.length; i++) {
            fitData[i] = MathUtils.constrain(data[i] + delta, min, max);
        }
        return fitData;
    }

    private static int bestFit(TonalPalette palette, float h, float s, float l) {
        int minErrorIndex = -1;
        float minError = Float.POSITIVE_INFINITY;
        for (int i = 0; i < palette.h.length; i++) {
            float error = ((Math.abs(h - palette.h[i]) * 1.0f) + (1.0f * Math.abs(s - palette.s[i]))) + (FIT_WEIGHT_L * Math.abs(l - palette.l[i]));
            if (error < minError) {
                minError = error;
                minErrorIndex = i;
            }
        }
        return minErrorIndex;
    }

    @VisibleForTesting
    public List<ColorRange> getBlacklistedColors() {
        return this.mBlacklistedColors;
    }

    private TonalPalette findTonalPalette(float h, float s) {
        if (s < 0.05f) {
            return this.mGreyPalette;
        }
        TonalPalette best = null;
        float error = Float.POSITIVE_INFINITY;
        int tonalPalettesCount = this.mTonalPalettes.size();
        int i = 0;
        while (i < tonalPalettesCount) {
            TonalPalette candidate = (TonalPalette) this.mTonalPalettes.get(i);
            if (h < candidate.minHue || h > candidate.maxHue) {
                if (candidate.maxHue <= 1.0f || h < 0.0f || h > fract(candidate.maxHue)) {
                    if (candidate.minHue < 0.0f && h >= fract(candidate.minHue) && h <= 1.0f) {
                        best = candidate;
                        break;
                    }
                    float error2;
                    if (h <= candidate.minHue && candidate.minHue - h < error) {
                        best = candidate;
                        error2 = candidate.minHue - h;
                    } else if (h >= candidate.maxHue && h - candidate.maxHue < error) {
                        best = candidate;
                        error = h - candidate.maxHue;
                        i++;
                    } else if (candidate.maxHue <= 1.0f || h < fract(candidate.maxHue) || h - fract(candidate.maxHue) >= error) {
                        if (candidate.minHue < 0.0f && h <= fract(candidate.minHue) && fract(candidate.minHue) - h < error) {
                            best = candidate;
                            error2 = fract(candidate.minHue) - h;
                        }
                        i++;
                    } else {
                        best = candidate;
                        error = h - fract(candidate.maxHue);
                        i++;
                    }
                    error = error2;
                    i++;
                } else {
                    best = candidate;
                    break;
                }
            }
            best = candidate;
            break;
        }
        return best;
    }

    private static float fract(float v) {
        return v - ((float) Math.floor((double) v));
    }
}
