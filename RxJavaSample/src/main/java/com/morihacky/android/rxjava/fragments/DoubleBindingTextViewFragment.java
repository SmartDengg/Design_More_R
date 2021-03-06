package com.morihacky.android.rxjava.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnTextChanged;
import com.morihacky.android.rxjava.R;
import rx.Subscription;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

import static android.text.TextUtils.isEmpty;

public class DoubleBindingTextViewFragment extends BaseFragment {

  private static final String TAG = DoubleBindingTextViewFragment.class.getSimpleName();
  @InjectView(R.id.double_binding_num1) EditText _number1;
  @InjectView(R.id.double_binding_num2) EditText _number2;
  @InjectView(R.id.double_binding_result) TextView _result;

  Subscription _subscription;
  PublishSubject<Float> _resultEmitterSubject;

  @Override public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View layout = inflater.inflate(R.layout.fragment_double_binding_textview, container, false);
    ButterKnife.inject(this, layout);

    _resultEmitterSubject = PublishSubject.create();
    _subscription = _resultEmitterSubject.asObservable().subscribe(new Action1<Float>() {
      @Override public void call(Float aFloat) {
        _result.setText(String.valueOf(aFloat));
      }
    });

    //onNumberChanged();
    _number2.requestFocus();

    return layout;
  }

  @OnTextChanged({ R.id.double_binding_num1, R.id.double_binding_num2 })
  public void onNumberChanged() {
    float num1 = 0;
    float num2 = 0;

    //Log.e(TAG, editText.toString());

    if (!isEmpty(_number1.getText().toString())) {
      num1 = Float.parseFloat(_number1.getText().toString());
    }

    if (!isEmpty(_number2.getText().toString())) {
      num2 = Float.parseFloat(_number2.getText().toString());
    }

    _resultEmitterSubject.onNext(num1 + num2);
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    if (_subscription != null) {
      _subscription.unsubscribe();
    }
  }
}
