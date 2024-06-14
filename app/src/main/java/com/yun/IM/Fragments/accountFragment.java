package com.yun.IM.Fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.yun.IM.Application.MyApplication;
import com.yun.IM.activities.SignInActivity;
import com.yun.IM.databinding.FragmentAccountBinding;
import com.yun.IM.netWork.WebSocketService;
import com.yun.IM.utilites.AutoUpdater;
import com.yun.IM.utilites.Constants;
import com.yun.IM.utilites.Encrypt;
import com.yun.IM.utilites.PreferenceManager;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import okhttp3.Call;

public class accountFragment extends Fragment {
    private FragmentAccountBinding binding;
    private PreferenceManager preferenceManager;
    private Context context;
    private WebSocketService webSocketService;
    private boolean flag;
    private float x = 0;
    private float Settings_X = 0;
    private ViewGroup viewGroup;
    private boolean show;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.context = getActivity();
        preferenceManager = new PreferenceManager(context);
        binding = FragmentAccountBinding.inflate(getLayoutInflater());
        MyApplication myApplication = (MyApplication) getActivity().getApplication();
        webSocketService = myApplication.getWebSocketService();
        init();
        setListeners();
        viewGroup = container;
        return binding.getRoot();
    }


    private void init() {
        binding.UserName.setText(preferenceManager.getString(Constants.KEY_NAME));
        binding.UserId.setText(preferenceManager.getString(Constants.KEY_USER_ID));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }


    private void setListeners() {
        binding.SignOut.setOnClickListener(v -> {
            Intent intent = new Intent(context, SignInActivity.class);
            preferenceManager.clear();
            webSocketService.close();
            startActivity(intent);
            getActivity().finish();
        });
        binding.back.setOnClickListener(v -> {
            binding.bottomPanel.hidden();
        });
        binding.ChangePassword.setOnClickListener(v -> {
            flag = false;
            binding.newPassword.setVisibility(View.VISIBLE);
            binding.confirmPassword.setVisibility(View.VISIBLE);
            binding.bottomPanel.show();
        });
        binding.sure.setOnClickListener(v -> {
            if (!flag) {
                if (!binding.newPassword.getText().toString().matches(Constants.regex)) {
                    showToast("不满足密码规范");
                } else if (!binding.newPassword.getText().toString().equals(binding.confirmPassword.getText().toString())) {
                    showToast("密码不一致");
                } else {
                    ChangePassword(Encrypt.returnMd5Message(binding.oldPassword.getText().toString()), Encrypt.returnMd5Message(binding.newPassword.getText().toString()));
                }
            } else {
                CancelAccount(Encrypt.returnMd5Message(binding.oldPassword.getText().toString().trim()));
            }

        });

        binding.CancelAccount.setOnClickListener(v -> {
            flag = true;
            binding.bottomPanel.show();
            binding.newPassword.setVisibility(View.GONE);
            binding.confirmPassword.setVisibility(View.GONE);
        });
        binding.Settings.setOnClickListener(v -> {
            binding.settingsPanel.animate().setInterpolator(new DecelerateInterpolator()).setDuration(1000).x((float) (viewGroup.getWidth() * 0.3)).alpha(1f).start();
        });
        binding.CheckUp.setOnClickListener(v -> {
            binding.settingsPanel.setVisibility(View.GONE);
            AutoUpdater autoUpdater = new AutoUpdater(context);
            autoUpdater.CheckUpdate(false);
        });
        binding.getRoot().setOnTouchListener((v, event) -> {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    x = event.getX();
                    Settings_X = binding.settingsPanel.getX();
                    show = true;
                    binding.getRoot().getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (binding.settingsPanel.getX() == viewGroup.getWidth() && event.getX() > x) {
                        binding.getRoot().getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                    }
                    if (Settings_X - (x - event.getX()) < viewGroup.getWidth() * 0.3) {
                        break;
                    }
                    binding.settingsPanel.setX(Settings_X - (x - event.getX()));
                    binding.settingsPanel.setAlpha((viewGroup.getWidth() - event.getX()) / binding.settingsPanel.getWidth());
                    show = false;
                    break;
                case MotionEvent.ACTION_UP:
                    if (binding.settingsPanel.getX() < viewGroup.getWidth() / 2) {
                        binding.settingsPanel.setX((float) (viewGroup.getWidth() * 0.3));
                    } else {
                        binding.settingsPanel.animate().setInterpolator(new DecelerateInterpolator()).setDuration(500).x(viewGroup.getWidth()).alpha(0f).start();
                    }
                    if (event.getX() < viewGroup.getWidth() * 0.3 && show) {
                        binding.settingsPanel.animate().setInterpolator(new DecelerateInterpolator()).setDuration(500).x(viewGroup.getWidth()).alpha(0f).start();
                    }
                    break;
            }
            return true;
        });
    }

    private void CancelAccount(String password) {
        OkHttpUtils.get()
                .url(Constants.KEY_URL + "CancelAccount")
                .addParams("password", password)
                .addParams("userId", preferenceManager.getString(Constants.KEY_USER_ID))
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e) {
                        showToast(e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, String s) {
                        if (s.equals("true")) {
                            Intent intent = new Intent(context, SignInActivity.class);
                            preferenceManager.clear();
                            webSocketService.close();
                            startActivity(intent);
                            getActivity().finish();
                        } else {
                            showToast("出错");
                        }
                    }
                });
    }

    private void ChangePassword(String password, String newPassword) {
        OkHttpUtils.get()
                .url(Constants.KEY_URL + "ChangePassword")
                .addParams("password", password)
                .addParams("userId", preferenceManager.getString(Constants.KEY_USER_ID))
                .addParams("newPassword", newPassword)
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e) {
                        showToast(e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, String s) {
                        if (s.equals("true")) {
                            showToast("修改成功");
                        } else {
                            showToast("出错");
                        }
                    }
                });
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}