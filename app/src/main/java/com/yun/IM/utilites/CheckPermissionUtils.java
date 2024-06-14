package com.yun.IM.utilites;

import android.Manifest;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public final class CheckPermissionUtils {
    //需要申请的权限
    private static final String[] permissions = new String[]{
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.INSTALL_SHORTCUT,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            Manifest.permission.INSTALL_SHORTCUT,
            Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
            Manifest.permission.REQUEST_INSTALL_PACKAGES
    };

    private CheckPermissionUtils() {
    }

    public static String[] checkPermission(Context context) {
        List<String> data = new ArrayList<>();
        for (String permission : permissions) {
            if (!EasyPermissions.hasPermissions(context, permission)) {
                data.add(permission);
            }
        }
        return data.toArray(new String[data.size()]);
    }
}
