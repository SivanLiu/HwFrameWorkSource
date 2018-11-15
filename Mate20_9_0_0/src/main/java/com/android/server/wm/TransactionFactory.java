package com.android.server.wm;

import android.view.SurfaceControl.Transaction;

interface TransactionFactory {
    Transaction make();
}
