package cn.dlc.wifilistdemo;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by John on 2018/3/28.
 */

public class PasswordDialog extends Dialog {

    @BindView(R.id.et_password)
    EditText mEtPassword;
    @BindView(R.id.btn_cancel)
    Button mBtnCancel;
    @BindView(R.id.btn_connect)
    Button mBtnConnect;

    private OnClickConnectListener mListener;

    public interface OnClickConnectListener {
        void toConnect(PasswordDialog dialog, String password);
    }

    public PasswordDialog(@NonNull Context context) {
        super(context, R.style.CommonDialogStyle);

        setContentView(R.layout.dialog_password);
        ButterKnife.bind(this);
    }

    @OnClick({ R.id.btn_cancel, R.id.btn_connect })
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_cancel:
                dismiss();
                break;
            case R.id.btn_connect:

                String password = mEtPassword.getText().toString();
                if (mListener != null) {
                    mListener.toConnect(this, password);
                }
                break;
        }
    }

    public void show(OnClickConnectListener listener) {
        mListener = listener;
        super.show();
    }

    @Override
    public void show() {
        super.show();
        throw new RuntimeException("不要调用这个方法");
    }
}
