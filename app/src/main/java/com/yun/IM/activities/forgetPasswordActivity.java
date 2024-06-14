package com.yun.IM.activities;

import static com.yun.IM.utilites.Constants.ShowError;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.yun.IM.databinding.ActivityForgetPasswordBinding;
import com.yun.IM.utilites.Constants;
import com.yun.IM.utilites.Encrypt;
import com.yun.IM.view.CodeEditView;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import okhttp3.Call;

public class forgetPasswordActivity extends AppCompatActivity {
    private ActivityForgetPasswordBinding binding;
    private CountDownTimer mCountDownTimer;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.retry.setEnabled(false);
        setListeners();
    }

    public void setListeners() {
        binding.forgetPassword.setOnClickListener(v -> {
            loading(true);
            if (isValidSigUpDetails()) {
                OkHttpUtils
                        .get()
                        .addParams("email", email)
                        .addParams("password", Encrypt.returnMd5Message(binding.inputPassword.getText().toString().trim()))
                        .url(Constants.KEY_URL + "ChangePasswordToId")
                        .build()
                        .execute(new StringCallback() {
                            @Override
                            public void onError(Call call, Exception e) {
                                loading(false);
                                showToast(ShowError);
                            }

                            @Override
                            public void onResponse(Call call, String s) {
                                loading(false);
                                if (s.equals("true")) {
                                    showToast("密码修改成功，请登录");
                                    Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                }
                            }
                        });
            }
        });
        binding.retry.setOnClickListener(v -> OkHttpUtils.get()
                .addParams("email", binding.inputEmail.getText().toString().trim())
                .url(Constants.KEY_URL + "forgetPassword")
                .build()
                .readTimeOut(30000)
                .writeTimeOut(30000)
                .connTimeOut(30000)
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e) {
                        Log.d("error", e.toString());
                        showToast(ShowError);
                    }

                    @Override
                    public void onResponse(Call call, String s) {
                        if (s.equals("true")) {
                            sendVerificationCode();
                            codeInfo();
                            showToast("发送成功");
                            binding.codeEditView.setVisibility(View.VISIBLE);
                        } else {
                            showToast("未找到该账户信息");
                        }
                    }
                }));
        binding.inputEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                binding.retry.setEnabled(s.toString().matches(Constants.email));
            }
        });
    }

    private void sendVerificationCode() {

        binding.retry.setEnabled(false);

        mCountDownTimer = new CountDownTimer(60_000, 1_000) {
            @Override
            public void onTick(long millisUntilFinished) {
                binding.retry.setText(millisUntilFinished / 1000 + "秒后重发");
            }

            @Override
            public void onFinish() {
                binding.retry.setEnabled(true);
                binding.retry.setText("发送验证码");
            }
        };
        binding.retry.setOnClickListener(v -> OkHttpUtils
                .post()
                .url(Constants.KEY_URL + "retryCode")
                .addParams("email", binding.inputEmail.getText().toString().trim())
                .addParams("head", Constants.forget)
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e) {
                        showToast(ShowError);
                    }

                    @Override
                    public void onResponse(Call call, String s) {
                        showToast("发送成功");
                        sendVerificationCode();
                    }
                }));
        mCountDownTimer.start();
    }

    private void codeInfo() {
        binding.codeEditView.setOnInputEndCallBack(new CodeEditView.inputEndListener() {
            @Override
            public void input(String text) {
                OkHttpUtils
                        .get()
                        .url(Constants.KEY_URL + "code")
                        .addParams("code", text)
                        .addParams("email", binding.inputEmail.getText().toString().trim())
                        .addParams("head", Constants.forget)
                        .build()
                        .execute(new StringCallback() {
                            @Override
                            public void onError(Call call, Exception e) {
                                showToast(ShowError);
                            }

                            @Override
                            public void onResponse(Call call, String s) {
                                switch (s) {
                                    case "null":
                                        showToast("请返回重试");
                                        break;
                                    case "验证码错误":
                                        binding.codeEditView.error();
                                        break;
                                    case "验证码已过期":
                                        showToast("验证码已过期，请重试");
                                        binding.codeEditView.error();
                                        break;
                                    default:
                                        email = s;
                                        binding.topPanel.setVisibility(View.GONE);
                                        binding.bottomPanel.setVisibility(View.VISIBLE);
                                        break;
                                }
                            }
                        });
            }

            @Override
            public void afterTextChanged(String text) {

            }
        });

    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private Boolean isValidSigUpDetails() {
        if (binding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("输入密码");
            return false;
        } else if (!binding.inputPassword.getText().toString().trim().matches(Constants.regex)) {
            showToast("密码格式错误");
            return false;
        } else if (binding.inputConfirmPassword.getText().toString().trim().isEmpty()) {
            showToast("输入确认密码");
            return false;
        } else if (!binding.inputPassword.getText().toString().equals(binding.inputConfirmPassword.getText().toString())) {
            showToast("密码和确认密码不同");
            return false;
        } else {
            return true;
        }
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.forgetPassword.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.forgetPassword.setVisibility(View.VISIBLE);
        }
    }
}