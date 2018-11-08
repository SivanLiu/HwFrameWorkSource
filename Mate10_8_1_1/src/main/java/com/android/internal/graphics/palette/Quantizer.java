package com.android.internal.graphics.palette;

import com.android.internal.graphics.palette.Palette.Filter;
import com.android.internal.graphics.palette.Palette.Swatch;
import java.util.List;

public interface Quantizer {
    List<Swatch> getQuantizedColors();

    void quantize(int[] iArr, int i, Filter[] filterArr);
}
