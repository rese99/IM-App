package com.yun.IM.utilites;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.yun.IM.R;

public class DialogManager {

    /**
     * 以下为dialog的初始化控件，包括其中的布局文件
     */

    private Dialog mDialog;

    private ImageView mIcon;
    private ImageView mVoice;

    private TextView mLable;

    private Context mContext;
    private View view;
    private boolean flag;
    private Handler handler;

    public DialogManager(Context context) {
        mContext = context;
        handler = new Handler();
    }

    /**
     * 显示Dialog
     */
    public void showRecordingDialog() {
        mDialog = new Dialog(mContext, R.style.Theme_audioDialog);
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        view = inflater.inflate(R.layout.layout_voice_dialog_manager, null);
        mDialog.setContentView(view);
        mIcon = (ImageView) mDialog.findViewById(R.id.dialog_icon);
        mVoice = (ImageView) mDialog.findViewById(R.id.dialog_voice);
        mLable = (TextView) mDialog.findViewById(R.id.recorder_dialogtext);
        Window dialogWindow = mDialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        int width = ScreenUtil.getScreenWidth(mContext) / 2;
        lp.width = width; // 宽度
        lp.height = width; // 高度
        dialogWindow.setAttributes(lp);
        mDialog.setCancelable(false);
        mDialog.show();
    }

    /**
     * 设置正在录音时的dialog界面
     */
    public void recording() {
        flag = false;
        if (mDialog != null && mDialog.isShowing()) {
            mIcon.setVisibility(View.GONE);
            mVoice.setVisibility(View.VISIBLE);
            mLable.setVisibility(View.VISIBLE);
            mLable.setText(R.string.shouzhishanghua);
            view.setBackground(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    /**
     * 取消界面
     */
    public void wantToCancel() {
        flag = true;
        if (mDialog != null && mDialog.isShowing()) {
            mIcon.setVisibility(View.VISIBLE);
            mVoice.setVisibility(View.GONE);
            mLable.setVisibility(View.VISIBLE);
            mIcon.setImageResource(R.mipmap.cancel);
            mLable.setText(R.string.want_to_cancle);
            view.setBackgroundResource(R.drawable.ic_dialog_loading_bg_red);
        }
    }

    /**
     * 时长过短
     */
    public void tooShort() {
        if (mDialog != null && mDialog.isShowing()) {
            mIcon.setVisibility(View.VISIBLE);
            mVoice.setVisibility(View.GONE);
            mLable.setVisibility(View.VISIBLE);
            mIcon.setImageResource(R.mipmap.voice_to_short);
            mLable.setText(R.string.tooshort);
            view.setBackgroundResource(R.drawable.ic_dialog_loading_bg_red);
        }
        handler.postDelayed(() -> mDialog.dismiss(), 1500);
    }
    /**
     * 时长过长
     */
    public void tooLong() {
        if (mDialog != null && mDialog.isShowing()) {
            mIcon.setVisibility(View.VISIBLE);
            mVoice.setVisibility(View.GONE);
            mLable.setVisibility(View.VISIBLE);
            mIcon.setImageResource(R.mipmap.voice_to_short);
            mLable.setText(R.string.toolong);
            view.setBackgroundResource(R.drawable.ic_dialog_loading_bg_red);
        }
        handler.postDelayed(() -> mDialog.dismiss(), 1500);
    }

    /**
     * 隐藏Dialog
     */
    public void dimissDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            mDialog = null;
        }

    }

    public boolean IsShow() {
        return flag;
    }
}
