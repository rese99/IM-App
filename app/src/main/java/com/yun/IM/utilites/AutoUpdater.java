package com.yun.IM.utilites;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.yun.IM.R;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.Call;

public class AutoUpdater {
    // 保存APK的文件名
    private static final String saveFileName = "my.apk";
    private static final int DOWN_UPDATE = 1;
    private static final int DOWN_OVER = 2;
    private static final int SHOWDOWN = 3;
    private static File apkFile;
    // 下载安装包的网络路径
    private String apkUrl = "";

    // 下载线程
    private Thread downLoadThread;
    private int progress;// 当前进度
    // 应用程序Context
    private Context mContext;
    // 是否是最新的应用,默认为false
    private boolean intercept = false;
    // 进度条与通知UI刷新的handler和msg常量
    private ProgressBar mProgress;
    private TextView txtStatus;
    private PreferenceManager preferenceManager;

    public AutoUpdater(Context context) {
        mContext = context;
        apkFile = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), saveFileName);
        preferenceManager = new PreferenceManager(context);
    }

    public void ShowUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setCancelable(false);
        builder.setTitle("软件版本更新");
        builder.setMessage("有最新的软件包，请下载并安装!");
        builder.setPositiveButton("立即下载", (dialog, which) -> ShowDownloadDialog());
        builder.setNegativeButton("以后再说", (dialog, which) -> dialog.dismiss());
        builder.setNeutralButton("忽略此版本", (dialog, which) -> {
            preferenceManager.putInt("version", preferenceManager.getInt("version") + 1);
            dialog.dismiss();
        });
        builder.create().show();
    }

    private void ShowDownloadDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
        dialog.setCancelable(false);
        dialog.setTitle("软件版本更新");
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View v = inflater.inflate(R.layout.progress, null);
        mProgress = v.findViewById(R.id.progress);
        txtStatus = v.findViewById(R.id.txtStatus);
        dialog.setView(v);
        dialog.setNegativeButton("取消", (dialog1, which) -> intercept = true);
        dialog.show();
        DownloadApk();
    }

    /**
     * 检查是否更新的内容
     */
    public void CheckUpdate(boolean flag) {
        new Thread(() -> {
            int localVersion = 0;
            try {
                localVersion = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionCode;
                if (flag){
                    localVersion = localVersion + preferenceManager.getInt("version");
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            OkHttpUtils.get().url(Constants.KEY_URL + "Check").addParams("version", String.valueOf(localVersion)).build().execute(new StringCallback() {
                @Override
                public void onError(Call call, Exception e) {

                }

                @Override
                public void onResponse(Call call, String s) {
                    if (s.contains("http")) {
                        apkUrl = s;
                        mHandler.sendEmptyMessage(SHOWDOWN);
                    }
                }
            });
        }).start();
    }

    /**
     * 从服务器下载APK安装包
     */
    public void DownloadApk() {
        downLoadThread = new Thread(DownApkWork);
        downLoadThread.start();
    }

    /**
     * 安装APK内容
     */
    public void installAPK() {
        try {
            if (!apkFile.exists()) {
                return;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                String packageName = mContext.getApplicationContext().getPackageName();
                String authority = new StringBuilder(packageName).append(".fileprovider").toString();
                Uri apkUri = FileProvider.getUriForFile(mContext, authority, apkFile);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            } else {
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
            }
            mContext.startActivity(intent);
            android.os.Process.killProcess(android.os.Process.myPid());
        } catch (Exception e) {
            Log.e("TAG", "installAPK: ", e);
        }
    }

    private Runnable DownApkWork = new Runnable() {
        @Override
        public void run() {
            URL url;
            try {
                url = new URL(apkUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();
                int length = conn.getContentLength();
                InputStream ins = conn.getInputStream();
                FileOutputStream fos = new FileOutputStream(apkFile);
                int count = 0;
                byte[] buf = new byte[1024 * 1024];
                while (!intercept) {
                    int numread = ins.read(buf);
                    count += numread;
                    progress = (int) (((float) count / length) * 100);
                    mHandler.sendEmptyMessage(DOWN_UPDATE);
                    if (numread <= 0) {
                        mHandler.sendEmptyMessage(DOWN_OVER);
                        break;
                    }
                    fos.write(buf, 0, numread);
                }
                fos.close();
                ins.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case SHOWDOWN:
                    ShowUpdateDialog();
                    break;
                case DOWN_UPDATE:
                    txtStatus.setText(progress + "%");
                    mProgress.setProgress(progress);
                    break;
                case DOWN_OVER:
                    Toast.makeText(mContext, "下载完毕", Toast.LENGTH_SHORT).show();
                    installAPK();
                    break;
                default:
                    break;
            }
        }

    };
}
