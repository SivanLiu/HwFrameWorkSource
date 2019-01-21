package com.huawei.android.hwaps;

public interface IHwapsFactory {
    IColorPlusController getColorPlusController();

    IEventAnalyzed getEventAnalyzed();

    IFpsController getFpsController();

    IFpsRequest getFpsRequest();

    ISmartLowpowerBrowser getSmartLowpowerBrowser();
}
