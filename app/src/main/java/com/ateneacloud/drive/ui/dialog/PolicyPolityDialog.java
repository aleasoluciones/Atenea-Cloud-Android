package com.ateneacloud.drive.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ateneacloud.drive.R;
import com.ateneacloud.drive.util.Utils;

public class PolicyPolityDialog extends Dialog implements View.OnClickListener {
    private Button buttonAccept, buttonCancel;

    private TextView textViewPolicy;
    private CheckBox checkBox;
    private Context mContext;
   private PolicyPolityDialog.OnCloseListener onCloseListener;

    public PolicyPolityDialog(@NonNull Context context, PolicyPolityDialog.OnCloseListener listener) {
        super(context);
        this.mContext = context;
        this.onCloseListener = listener;
    }

    public interface OnCloseListener {
        void onClose(boolean accepted);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_policy_polity);

        checkBox = findViewById(R.id.checkbox_policy_polity);
        buttonAccept = findViewById(R.id.confirm_policy);
        buttonCancel = findViewById(R.id.cancel_policy);
        textViewPolicy = findViewById(R.id.text_policy);

        Window win = getWindow();
        if (win != null) {
            WindowManager.LayoutParams lp = win.getAttributes();
            lp.height = Utils.dip2px(mContext, 150);
            lp.width = Utils.dip2px(mContext, 350);
            win.setAttributes(lp);
        }

        buttonCancel.setOnClickListener(this);
        buttonAccept.setOnClickListener(this);
        buttonAccept.setEnabled(false); // Inicialmente deshabilitado

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            buttonAccept.setEnabled(isChecked); // Habilita el botón de "Aceptar" solo si el CheckBox está marcado
        });

        SpannableString spannableString = new SpannableString(mContext.getString(R.string.policy_privacy));
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                // Acción cuando se hace clic en el enlace
                Intent intent = new Intent(Intent.ACTION_VIEW,  Uri.parse(mContext.getString(R.string.url_policy)));
                mContext.startActivity(intent);
            }
        };
        spannableString.setSpan(clickableSpan, 0, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new UnderlineSpan(), 0, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#333333")), 0, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        textViewPolicy.setText(spannableString);
        textViewPolicy.setText(spannableString);
        textViewPolicy.setMovementMethod(LinkMovementMethod.getInstance());

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel_policy:
                if (onCloseListener != null) {
                    onCloseListener.onClose(false);
                }
                dismiss();
                break;
            case R.id.confirm_policy:
                if (onCloseListener != null) {
                    onCloseListener.onClose(true);
                }
                dismiss();
                break;
        }
    }


}
