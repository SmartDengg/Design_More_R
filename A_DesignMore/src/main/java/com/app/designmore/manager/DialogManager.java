package com.app.designmore.manager;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.drm.ProcessedData;
import android.os.Message;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import com.app.designmore.R;
import com.app.designmore.utils.DensityUtil;
import com.app.designmore.view.dialogplus.DialogPlus;
import com.app.designmore.view.dialogplus.OnBackPressListener;
import com.app.designmore.view.dialogplus.OnCancelListener;
import com.app.designmore.view.dialogplus.OnClickListener;
import com.app.designmore.view.dialogplus.OnDismissListener;
import com.app.designmore.view.dialogplus.ViewHolder;

/**
 * Created by Joker on 2015/8/26.
 */
public class DialogManager {

  private DialogManager() {

  }

  private static class SingletonHolder {
    private static DialogManager instance = new DialogManager();
  }

  public static DialogManager getInstance() {
    return SingletonHolder.instance;
  }

  public DialogPlus showSelectorDialog(Context context, int gravity, int layoutId,
      final OnClickListener onClickListener) {

    return DialogManager.this.showSelectorDialog(context, gravity, layoutId, false, onClickListener,
        null, null, null);
  }

  public DialogPlus showSelectorDialog(Context context, int gravity, int layoutId,
      final OnClickListener onClickListener, final OnBackPressListener onBackPressListener) {

    return DialogManager.this.showSelectorDialog(context, gravity, layoutId, false, onClickListener,
        onBackPressListener, null, null);
  }

  public DialogPlus showSelectorDialog(Context context, int gravity, int layoutId,
      boolean cancelable, final OnClickListener onClickListener,
      final OnBackPressListener onBackPressListener, final OnCancelListener onCancelListener,
      final OnDismissListener onDismissListener) {

    DialogPlus dialog = DialogPlus.newDialog(context)
        .setContentHolder(new ViewHolder(layoutId))
        .setGravity(gravity)
        .setOnClickListener(new OnClickListener() {
          @Override public void onClick(DialogPlus dialog, View view) {
            if (onClickListener != null) onClickListener.onClick(dialog, view);
          }
        })
        .setOnBackPressListener(new OnBackPressListener() {
          @Override public void onBackPressed(DialogPlus dialogPlus) {
            if (onBackPressListener != null) onBackPressListener.onBackPressed(dialogPlus);
          }
        })
        .setOnCancelListener(new OnCancelListener() {
          @Override public void onCancel(DialogPlus dialog) {
            if (onCancelListener != null) onCancelListener.onCancel(dialog);
          }
        })
        .setOnDismissListener(new OnDismissListener() {
          @Override public void onDismiss(DialogPlus dialog) {
            if (onDismissListener != null) onDismissListener.onDismiss(dialog);
          }
        })
        .setCancelable(cancelable)
        .create();
    dialog.show();

    return dialog;
  }

  public ProgressDialog showProgressDialog(Context context,
      final DialogInterface.OnShowListener onShowListener,
      final DialogInterface.OnCancelListener onCancelListener) {

    ProgressDialog progressDialog = new ProgressDialog(context);
    progressDialog.setCancelable(true);
    progressDialog.setCanceledOnTouchOutside(false);

    progressDialog.setOnShowListener(new DialogInterface.OnShowListener() {
      @Override public void onShow(DialogInterface dialog) {
        if (onShowListener != null) onShowListener.onShow(dialog);
      }
    });

    /*progressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
      @Override public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 && callback != null) {
          callback.onBackPressListener(dialog);
          return true;
        }
        return false;
      }
    });*/

    progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
      @Override public void onCancel(DialogInterface dialog) {
        if (onCancelListener != null) {
          onCancelListener.onCancel(dialog);
        }
      }
    });

   /* progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
      @Override public void onDismiss(DialogInterface dialog) {
        if (onDismissListener != null) {
          onDismissListener.onDismiss(dialog);
        }
      }
    });*/

    progressDialog.show();
    progressDialog.setContentView(R.layout.dialog_progressing_layout);

    return progressDialog;
  }

  interface Callback {

    void onBackPressListener(DialogInterface dialog);
  }
}