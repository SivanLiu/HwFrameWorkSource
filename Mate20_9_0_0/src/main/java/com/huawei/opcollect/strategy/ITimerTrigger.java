package com.huawei.opcollect.strategy;

import java.util.Calendar;

interface ITimerTrigger {
    boolean checkTrigger(Calendar calendar, long j, long j2, NextTimer nextTimer);

    String toString(String str);
}
