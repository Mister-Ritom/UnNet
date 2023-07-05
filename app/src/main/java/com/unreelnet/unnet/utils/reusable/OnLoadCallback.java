package com.unreelnet.unnet.utils.reusable;

import android.graphics.Bitmap;

@FunctionalInterface
public interface OnLoadCallback {
    void onLoad(Bitmap resource);
}
