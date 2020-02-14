package com.godot.rtmppushstream.live.channel.utils;

import android.os.Build;
import android.util.Size;

import androidx.annotation.RequiresApi;

import java.util.Comparator;

/**
 * @Desc 说明
 * @Author Godot
 * @Date 2020-02-04
 * @Version 1.0
 * @Mail 809373383@qq.com
 */
class CompareSizesByArea implements Comparator<Size> {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int compare(Size o1, Size o2) {
        return Long.signum((long)(o1.getWidth()*o1.getHeight())-(long)(o2.getWidth()*o2.getHeight()));
    }
}
