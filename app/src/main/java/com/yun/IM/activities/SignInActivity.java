package com.yun.IM.activities;

import static com.yun.IM.utilites.Constants.KEY_PERMISSIONS;
import static com.yun.IM.utilites.Constants.conversations;
import static com.yun.IM.utilites.Constants.dbAdapter;
import static com.yun.IM.utilites.Constants.friends;
import static com.yun.IM.utilites.Constants.userId;
import static com.yun.IM.utilites.Constants.users;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Patterns;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yun.IM.R;
import com.yun.IM.databinding.ActivitySignInBinding;
import com.yun.IM.models.ChatMessage;
import com.yun.IM.models.Friends;
import com.yun.IM.models.User;
import com.yun.IM.utilites.CheckPermissionUtils;
import com.yun.IM.utilites.Constants;
import com.yun.IM.utilites.Encrypt;
import com.yun.IM.utilites.PreferenceManager;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class SignInActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    private final Gson gson = new Gson();
    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;

    public void retryLogin() {
        if (Constants.KEY_RETRY_LOGIN) {
            Constants.KEY_RETRY_LOGIN = false;
            new AlertDialog.Builder(this)
                    .setTitle("账号异常提醒")
                    .setMessage("账号在别处登录，请重新输入密码登录")
                    .setPositiveButton("确定", null)
                    .setCancelable(false)
                    .show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());
        Constants.clean();
        retryLogin();
        GetPermissions(CheckPermissionUtils.checkPermission(this));
        ignoreBatteryOptimization(this);
        checkPermissions();
        if (preferenceManager.getString(Constants.KEY_USER_ID) != null) {
            getUsers();
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
    }

    private void setListeners() {
        binding.forgetPassword.setOnClickListener(v -> {
            if (check()) {
                startActivity(new Intent(getApplicationContext(), forgetPasswordActivity.class));
            }
        });
        binding.textCreateNewAccount.setOnClickListener(v -> {
            if (check()) {
                startActivity(new Intent(getApplicationContext(), SignUpActivity.class));
            }
        });
        binding.buttonSignIn.setOnClickListener(v -> {
            if (check() && isValidSignDetails()) {
                signIn();
            }
        });
        binding.serviceInfo.setOnClickListener(v -> showToast("不要做违法的事"));
    }

    private boolean check() {
        if (!binding.checkbox.isChecked()) {
            Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
            binding.bottomPanel.startAnimation(shake);
        }
        return binding.checkbox.isChecked();
    }

    private void signIn() {
        loading(true);
        OkHttpUtils.get().url(Constants.KEY_URL + "Login").addParams("email", Encrypt.returnMd5Message(binding.inputEmail.getText().toString().trim())).addParams("password", Encrypt.returnMd5Message(binding.inputPassword.getText().toString().trim())).build().execute(new StringCallback() {
            @Override
            public void onError(Call call, Exception e) {
                loading(false);
                showToast("出错");
            }

            @Override
            public void onResponse(Call call, String s) {
                User user = new Gson().fromJson(s, User.class);
                if (user != null) {
                    preferenceManager.putString(Constants.KEY_USER_ID, user.id);
                    preferenceManager.putString(Constants.KEY_TOKEN, user.token);
                    preferenceManager.putString(Constants.KEY_NAME, user.name);
                    preferenceManager.putString(Constants.KEY_IMAGE, user.image);
                    preferenceManager.putString(Constants.KEY_AES, user.AES);
                    dbAdapter.open(user.id);
                    getUsers();
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    loading(false);
                    showToast("用户名或密码错误");
                }
            }
        });

    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.buttonSignIn.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSignIn.setVisibility(View.VISIBLE);
        }
    }

    private Boolean isValidSignDetails() {
        if (binding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
            showToast("Enter valid email");
            return false;
        } else if (binding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter password");
            return false;
        } else {
            return true;
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void getUsers() {
        friends = dbAdapter.queryAllData(preferenceManager.getString(Constants.KEY_USER_ID), Constants.KEY_FRIENDS);
        if (null != friends) {
            for (int i = 0; i < friends.size(); i++) {
                if (null != friends.get(i).lastMessage) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.conversionImage = friends.get(i).image;
                    chatMessage.conversionName = friends.get(i).name;
                    chatMessage.message = friends.get(i).lastMessage;
                    chatMessage.conversionId = friends.get(i).id;
                    chatMessage.dateObject = friends.get(i).dateObject;
                    chatMessage.message_type = "null";
                    chatMessage.AES = friends.get(i).aes;
                    conversations.add(chatMessage);
                }
                if (friends.get(i).studs.equals("0")) {
                    User user = new User();
                    user.name = friends.get(i).name;
                    user.email = friends.get(i).email;
                    user.image = friends.get(i).image;
                    user.id = friends.get(i).id;
                    user.AES = friends.get(i).aes;
                    users.add(user);
                    userId.add(user.id);
                }
            }
        } else {
            OkHttpUtils.get().url(Constants.KEY_URL + "GetUsers").addParams("userId", preferenceManager.getString(Constants.KEY_USER_ID)).build().execute(new StringCallback() {
                @Override
                public void onError(Call call, Exception e) {
                    showToast("获取失败");
                }

                @Override
                public void onResponse(Call call, String s) {
                    if (null != s) {
                        friends = gson.fromJson(s, new TypeToken<List<Friends>>() {
                        }.getType());
                        for (int i = 0; i < friends.size(); i++) {
                            if (friends.get(i).studs.equals("0")) {
                                User user = new User();
                                user.name = friends.get(i).name;
                                user.email = friends.get(i).email;
                                user.image = friends.get(i).image;
                                user.id = friends.get(i).id;
                                users.add(user);
                                userId.add(user.id);
                            }
                            dbAdapter.insert(friends.get(i), Constants.KEY_FRIENDS);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
//        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
//            new AppSettingsDialog.Builder(this).setTitle("获取权限").setRationale("需要获取后台自启动权限以满足应用运行").build().show();
//        }
    }

    @AfterPermissionGranted(KEY_PERMISSIONS)
    public void GetPermissions(String[] permissions) {
        EasyPermissions.requestPermissions(this, "获取应用所需权限", KEY_PERMISSIONS, permissions);
    }

    private void ignoreBatteryOptimization(Activity activity) {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean hasIgnored = powerManager.isIgnoringBatteryOptimizations(activity.getPackageName());
        if (!hasIgnored) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            startActivity(intent);
        }
    }

    private void checkPermissions() {
        try {
            //6.0才用动态权限
            if (Build.VERSION.SDK_INT >= 23) {
                String[] permissions = {
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
                        Manifest.permission.REQUEST_INSTALL_PACKAGES};
                List<String> permissionList = new ArrayList<>();
                for (int i = 0; i < permissions.length; i++) {
                    if (ActivityCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                        permissionList.add(permissions[i]);
                    }
                }
                if (permissionList.size() > 0) {
                    ActivityCompat.requestPermissions(this, permissions, 100);
                }
            }
        } catch (Exception ex) {
            showToast("自动更新异常：" + ex.getMessage());
        }
    }
}