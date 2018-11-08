package com.android.internal.colorextraction.types;

import android.app.WallpaperColors;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.util.MathUtils;
import android.util.Range;
import com.android.internal.R;
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
    public static final int MAIN_COLOR_DARK = -14606047;
    public static final int MAIN_COLOR_LIGHT = -2039584;
    public static final int SECONDARY_COLOR_DARK = -16777216;
    public static final int SECONDARY_COLOR_LIGHT = -6381922;
    private static final String TAG = "Tonal";
    private final ArrayList<ColorRange> mBlacklistedColors;
    private final TonalPalette mGreyPalette;
    private float[] mTmpHSL = new float[3];
    private final ArrayList<TonalPalette> mTonalPalettes;

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
            float[] fArr = new float[3];
            fArr[0] = ((((Float) this.mHue.getUpper()).floatValue() - ((Float) this.mHue.getLower()).floatValue()) / 2.0f) + ((Float) this.mHue.getLower()).floatValue();
            fArr[1] = ((((Float) this.mSaturation.getUpper()).floatValue() - ((Float) this.mSaturation.getLower()).floatValue()) / 2.0f) + ((Float) this.mSaturation.getLower()).floatValue();
            fArr[2] = ((((Float) this.mLightness.getUpper()).floatValue() - ((Float) this.mLightness.getLower()).floatValue()) / 2.0f) + ((Float) this.mLightness.getLower()).floatValue();
            return fArr;
        }

        public String toString() {
            return String.format("H: %s, S: %s, L %s", new Object[]{this.mHue, this.mSaturation, this.mLightness});
        }
    }

    public static class ConfigParser {
        private final ArrayList<ColorRange> mBlacklistedColors = new ArrayList();
        private final ArrayList<TonalPalette> mTonalPalettes = new ArrayList();

        public ConfigParser(Context context) {
            try {
                XmlPullParser parser = context.getResources().getXml(R.xml.color_extraction);
                int eventType = parser.getEventType();
                while (eventType != 1) {
                    if (!(eventType == 0 || eventType == 3)) {
                        if (eventType == 2) {
                            String tagName = parser.getName();
                            if (tagName.equals("palettes")) {
                                parsePalettes(parser);
                            } else if (tagName.equals("blacklist")) {
                                parseBlacklist(parser);
                            }
                        } else {
                            throw new XmlPullParserException("Invalid XML event " + eventType + " - " + parser.getName(), parser, null);
                        }
                    }
                    eventType = parser.next();
                }
            } catch (Exception e) {
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
                        throw new XmlPullParserException("Invalid tag: " + name, parser, null);
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
                        throw new XmlPullParserException("Invalid tag: " + name);
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

    static class TonalPalette {
        final float[] h;
        final float[] l;
        final float maxHue;
        final float minHue;
        final float[] s;

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
            throw new IllegalArgumentException("All arrays should have the same size. h: " + Arrays.toString(h) + " s: " + Arrays.toString(s) + " l: " + Arrays.toString(l));
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
        int i;
        Color bestColor = null;
        float[] hsl = new float[3];
        for (i = 0; i < mainColorsSize; i++) {
            Color color = (Color) mainColors.get(i);
            int colorValue = color.toArgb();
            ColorUtils.RGBToHSL(Color.red(colorValue), Color.green(colorValue), Color.blue(colorValue), hsl);
            if (!generatedFromBitmap || (isBlacklisted(hsl) ^ 1) != 0) {
                bestColor = color;
                break;
            }
        }
        if (bestColor == null) {
            return false;
        }
        colorValue = bestColor.toArgb();
        ColorUtils.RGBToHSL(Color.red(colorValue), Color.green(colorValue), Color.blue(colorValue), hsl);
        hsl[0] = hsl[0] / 360.0f;
        TonalPalette palette = findTonalPalette(hsl[0], hsl[1]);
        if (palette == null) {
            Log.w(TAG, "Could not find a tonal palette!");
            return false;
        }
        int fitIndex = bestFit(palette, hsl[0], hsl[1], hsl[2]);
        if (fitIndex == -1) {
            Log.w(TAG, "Could not find best fit!");
            return false;
        }
        float[] h = fit(palette.h, hsl[0], fitIndex, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        float[] s = fit(palette.s, hsl[1], fitIndex, 0.0f, 1.0f);
        float[] l = fit(palette.l, hsl[2], fitIndex, 0.0f, 1.0f);
        StringBuilder builder = new StringBuilder("Tonal Palette - index: " + fitIndex + ". Main color: " + Integer.toHexString(getColorInt(fitIndex, h, s, l)) + "\nColors: ");
        for (i = 0; i < h.length; i++) {
            builder.append(Integer.toHexString(getColorInt(i, h, s, l)));
            if (i < h.length - 1) {
                builder.append(", ");
            }
        }
        Log.d(TAG, builder.toString());
        int primaryIndex = fitIndex;
        int mainColor = getColorInt(fitIndex, h, s, l);
        ColorUtils.colorToHSL(mainColor, this.mTmpHSL);
        float mainLuminosity = this.mTmpHSL[2];
        ColorUtils.colorToHSL(MAIN_COLOR_LIGHT, this.mTmpHSL);
        if (mainLuminosity > this.mTmpHSL[2]) {
            return false;
        }
        ColorUtils.colorToHSL(MAIN_COLOR_DARK, this.mTmpHSL);
        if (mainLuminosity < this.mTmpHSL[2]) {
            return false;
        }
        outColorsNormal.setMainColor(mainColor);
        outColorsNormal.setSecondaryColor(getColorInt(fitIndex + (fitIndex >= 2 ? -2 : 2), h, s, l));
        if (supportsDarkText) {
            primaryIndex = h.length - 1;
        } else if (fitIndex < 2) {
            primaryIndex = 0;
        } else {
            primaryIndex = Math.min(fitIndex, 3);
        }
        int secondaryIndex = primaryIndex + (primaryIndex >= 2 ? -2 : 2);
        outColorsDark.setMainColor(getColorInt(primaryIndex, h, s, l));
        outColorsDark.setSecondaryColor(getColorInt(secondaryIndex, h, s, l));
        if (supportsDarkText) {
            primaryIndex = h.length - 1;
        } else if (fitIndex < 2) {
            primaryIndex = 0;
        } else {
            primaryIndex = 2;
        }
        secondaryIndex = primaryIndex + (primaryIndex >= 2 ? -2 : 2);
        outColorsExtraDark.setMainColor(getColorInt(primaryIndex, h, s, l));
        outColorsExtraDark.setSecondaryColor(getColorInt(secondaryIndex, h, s, l));
        outColorsNormal.setSupportsDarkText(supportsDarkText);
        outColorsDark.setSupportsDarkText(supportsDarkText);
        outColorsExtraDark.setSupportsDarkText(supportsDarkText);
        Log.d(TAG, "Gradients: \n\tNormal " + outColorsNormal + "\n\tDark " + outColorsDark + "\n\tExtra dark: " + outColorsExtraDark);
        return true;
    }

    private void applyFallback(WallpaperColors inWallpaperColors, GradientColors outColorsNormal, GradientColors outColorsDark, GradientColors outColorsExtraDark) {
        applyFallback(inWallpaperColors, outColorsNormal);
        applyFallback(inWallpaperColors, outColorsDark);
        applyFallback(inWallpaperColors, outColorsExtraDark);
    }

    public static void applyFallback(WallpaperColors inWallpaperColors, GradientColors outGradientColors) {
        boolean light = inWallpaperColors != null ? (inWallpaperColors.getColorHints() & 1) != 0 : false;
        int innerColor = light ? MAIN_COLOR_LIGHT : MAIN_COLOR_DARK;
        int outerColor = light ? SECONDARY_COLOR_LIGHT : -16777216;
        outGradientColors.setMainColor(innerColor);
        outGradientColors.setSecondaryColor(outerColor);
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
            float error = ((Math.abs(h - palette.h[i]) * 1.0f) + (Math.abs(s - palette.s[i]) * 1.0f)) + (Math.abs(l - palette.l[i]) * FIT_WEIGHT_L);
            if (error < minError) {
                minError = error;
                minErrorIndex = i;
            }
        }
        return minErrorIndex;
    }

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
                    if (h <= candidate.minHue && candidate.minHue - h < error) {
                        best = candidate;
                        error = candidate.minHue - h;
                    } else if (h >= candidate.maxHue && h - candidate.maxHue < error) {
                        best = candidate;
                        error = h - candidate.maxHue;
                    } else if (candidate.maxHue > 1.0f && h >= fract(candidate.maxHue) && h - fract(candidate.maxHue) < error) {
                        best = candidate;
                        error = h - fract(candidate.maxHue);
                    } else if (candidate.minHue < 0.0f && h <= fract(candidate.minHue) && fract(candidate.minHue) - h < error) {
                        best = candidate;
                        error = fract(candidate.minHue) - h;
                    }
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
