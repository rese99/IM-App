package com.yun.IM.activities;

import static com.yun.IM.utilites.Constants.dbAdapter;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.yun.IM.R;
import com.yun.IM.databinding.ActivitySignUpBinding;
import com.yun.IM.models.User;
import com.yun.IM.utilites.Constants;
import com.yun.IM.utilites.Encrypt;
import com.yun.IM.utilites.PreferenceManager;
import com.yun.IM.view.CodeEditView;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import okhttp3.Call;

public class SignUpActivity extends AppCompatActivity {
    private ActivitySignUpBinding binding;
    private PreferenceManager preferenceManager;
    private String encodedImage;
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfile.setImageBitmap(bitmap);
                            binding.textAddImage.setVisibility(View.GONE);
                            encodedImage = encodeImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );
    private CodeEditView codeEditView;
    private CountDownTimer mCountDownTimer;
    private Gson gson;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        gson = new Gson();
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
    }

    public void setListeners() {
        binding.textSignIn.setOnClickListener(v -> onBackPressed());
        binding.buttonSignUp.setOnClickListener(v -> {
            if (isValidSigUpDetails()) {
                singUp();
            }
        });
        binding.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    private void singUp() {
        loading(true);
        user=new User();
        user.name=binding.inputName.getText().toString();
        user.email=binding.inputEmail.getText().toString();
        user.password= Encrypt.returnMd5Message(binding.inputPassword.getText().toString());
        user.image=encodedImage;
        user.AES=Encrypt.returnMd5Message(binding.inputEmail.getText().toString());
        OkHttpUtils
                .post()
                .url(Constants.KEY_URL + "register")
                .addParams("user",gson.toJson(user))
                .build()
                .readTimeOut(3000)
                .writeTimeOut(3000)
                .connTimeOut(3000)
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e) {
                        showToast(e.toString());
                        loading(false);
                    }

                    @Override
                    public void onResponse(Call call, String s) {
                        if (s.equals("该邮箱已被注册")) {
                            showToast(s);
                            loading(false);
                        } else {
                            setContentView(R.layout.code);
                            TextView textView = findViewById(R.id.text);
                            textView.setText("验证码已发送至邮箱" + "\n" + binding.inputEmail.getText().toString());
                            sendVerificationCode();
                            codeInfo();
                        }
                    }
                });

    }

    private void sendVerificationCode() {
        Button btnSendCode = findViewById(R.id.retry);
        btnSendCode.setEnabled(false);

        mCountDownTimer = new CountDownTimer(60_000, 1_000) {
            @Override
            public void onTick(long millisUntilFinished) {
                btnSendCode.setText(millisUntilFinished / 1000 + "秒后重发");
            }

            @Override
            public void onFinish() {
                btnSendCode.setEnabled(true);
                btnSendCode.setText("发送验证码");
            }
        };
        btnSendCode.setOnClickListener(v -> OkHttpUtils
                .post()
                .url(Constants.KEY_URL + "retryCode")
                .addParams("email", user.email)
                .addParams("head", Constants.SignUp)
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e) {
                        showToast("请求失败");
                    }

                    @Override
                    public void onResponse(Call call, String s) {
                        showToast("发送成功");
                        sendVerificationCode();
                    }
                }));
        mCountDownTimer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mCountDownTimer = null;
        }
    }

    private void codeInfo() {
        codeEditView = (CodeEditView) findViewById(R.id.codeEditView);
        codeEditView.setOnInputEndCallBack(new CodeEditView.inputEndListener() {
            @Override
            public void input(String text) {
                OkHttpUtils
                        .get()
                        .url(Constants.KEY_URL + "code")
                        .addParams("code", text)
                        .addParams("email", user.email)
                        .addParams("head", Constants.SignUp)
                        .build()
                        .execute(new StringCallback() {
                            @Override
                            public void onError(Call call, Exception e) {
                                showToast("出错");
                            }

                            @Override
                            public void onResponse(Call call, String s) {
                                switch (s) {
                                    case "null":
                                        showToast("请返回重试");
                                        break;
                                    case "验证码错误":
                                        codeEditView.error();
                                        break;
                                    case "验证码已过期":
                                        showToast("验证码已过期，请重试");
                                        codeEditView.error();
                                        break;
                                    default:
                                        loading(false);
                                        preferenceManager.putString(Constants.KEY_USER_ID, gson.fromJson(s, User.class).id);
                                        preferenceManager.putString(Constants.KEY_TOKEN, gson.fromJson(s, User.class).token);
                                        preferenceManager.putString(Constants.KEY_NAME, user.name);
                                        preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                                        preferenceManager.putString(Constants.KEY_AES,user.AES);
                                        dbAdapter.open(preferenceManager.getString(Constants.KEY_USER_ID));
                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
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


    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private Boolean isValidSigUpDetails() {
        if (encodedImage == null) {
            showToast("选择头像");
            return false;
        } else if (binding.inputName.getText().toString().trim().isEmpty()) {
            showToast("输入昵称");
            return false;
        } else if (binding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("输入邮箱");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
            showToast("邮箱格式错误");
            return false;
        } else if (binding.inputPassword.getText().toString().trim().isEmpty()) {
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
            binding.buttonSignUp.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSignUp.setVisibility(View.VISIBLE);
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}