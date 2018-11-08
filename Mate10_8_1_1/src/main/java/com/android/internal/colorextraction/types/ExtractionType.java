package com.android.internal.colorextraction.types;

import android.app.WallpaperColors;
import com.android.internal.colorextraction.ColorExtractor.GradientColors;

public interface ExtractionType {
    void extractInto(WallpaperColors wallpaperColors, GradientColors gradientColors, GradientColors gradientColors2, GradientColors gradientColors3);
}
