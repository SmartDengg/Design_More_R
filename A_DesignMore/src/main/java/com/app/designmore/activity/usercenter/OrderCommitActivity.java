package com.app.designmore.activity.usercenter;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.app.designmore.Constants;
import com.app.designmore.R;
import com.app.designmore.activity.BaseActivity;
import com.app.designmore.adapter.CommitOrderAdapter;
import com.app.designmore.exception.WebServiceException;
import com.app.designmore.helper.DBHelper;
import com.app.designmore.manager.WrappingLinearLayoutManager;
import com.app.designmore.retrofit.AddressRetrofit;
import com.app.designmore.retrofit.entity.AddressEntity;
import com.app.designmore.retrofit.entity.DeliveryEntity;
import com.app.designmore.retrofit.entity.TrolleyEntity;
import com.app.designmore.revealLib.animation.SupportAnimator;
import com.app.designmore.revealLib.animation.ViewAnimationUtils;
import com.app.designmore.revealLib.widget.RevealFrameLayout;
import com.app.designmore.rxAndroid.schedulers.AndroidSchedulers;
import com.app.designmore.utils.DensityUtil;
import com.app.designmore.manager.DividerDecoration;
import com.app.designmore.utils.Utils;
import com.app.designmore.view.ProgressLayout;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.jakewharton.rxbinding.widget.TextViewTextChangeEvent;
import com.trello.rxlifecycle.ActivityEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import retrofit.RetrofitError;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

/**
 * Created by Joker on 2015/9/26.
 */
public class OrderCommitActivity extends BaseActivity {

  private static final String TAG = OrderCommitActivity.class.getSimpleName();
  private static final String ITEMS = "ITEMS";

  @Nullable @Bind(R.id.order_commit_layout_root_view) LinearLayout rootView;
  @Nullable @Bind(R.id.white_toolbar_root_view) Toolbar toolbar;
  @Nullable @Bind(R.id.white_toolbar_title_tv) TextView toolbarTitleTv;

  @Nullable @Bind(R.id.order_commit_layout_rfl) RevealFrameLayout revealFrameLayout;
  @Nullable @Bind(R.id.order_commit_layout_pl) ProgressLayout progressLayout;

  @Nullable @Bind(R.id.order_commit_layout_address_tv) TextView addressTv;
  @Nullable @Bind(R.id.order_commit_layout_rl) RecyclerView recyclerView;
  @Nullable @Bind(R.id.order_commit_layout_delivery_tv) TextView deliveryTv;
  @Nullable @Bind(R.id.order_commit_layout_message_et) EditText messageEt;

  @Nullable @Bind(R.id.order_commit_layout_total_count_tv) TextView totalCountTv;
  @Nullable @Bind(R.id.order_commit_layout_total_price_tv) TextView totalPriceTv;
  @Nullable @Bind(R.id.order_commit_layout_commit_btn) Button commitBtn;

  private float totalPrice;

  private Observable<TextViewTextChangeEvent> addressChangeObservable;
  private Observable<TextViewTextChangeEvent> deliveryChangeObservable;

  private List<TrolleyEntity> trolleyEntities = new ArrayList<>();

  private AddressEntity defaultAddress = null;
  private DeliveryEntity defaultDelivery = null;

  private View.OnClickListener retryClickListener = new View.OnClickListener() {
    @Override public void onClick(View v) {
      OrderCommitActivity.this.loadData();
    }
  };

  public static void navigateToOrderCommit(AppCompatActivity startingActivity,
      ArrayList<TrolleyEntity> trolleyEntities) {
    Intent intent = new Intent(startingActivity, OrderCommitActivity.class);
    intent.putParcelableArrayListExtra(ITEMS, trolleyEntities);
    startingActivity.startActivity(intent);
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.order_commit_layout);

    OrderCommitActivity.this.initView(savedInstanceState);
  }

  @Override public void initView(Bundle savedInstanceState) {

    OrderCommitActivity.this.setSupportActionBar(toolbar);
    toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_icon));

    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) toolbarTitleTv.getLayoutParams();
    params.rightMargin = DensityUtil.getActionBarSize(OrderCommitActivity.this);

    OrderCommitActivity.this.toolbarTitleTv.setVisibility(View.VISIBLE);
    OrderCommitActivity.this.toolbarTitleTv.setText("确认订单");

    this.trolleyEntities.clear();
    this.trolleyEntities.addAll(
        getIntent().getExtras().<TrolleyEntity>getParcelableArrayList(ITEMS));

    /*创建Adapter*/
    OrderCommitActivity.this.setupAdapter();

    if (savedInstanceState == null) {
      revealFrameLayout.getViewTreeObserver()
          .addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
              revealFrameLayout.getViewTreeObserver().removeOnPreDrawListener(this);
              OrderCommitActivity.this.startEnterAnim();
              return true;
            }
          });
    } else {
      OrderCommitActivity.this.loadData();
    }
  }

  private void setupAdapter() {

    LinearLayoutManager linearLayoutManager =
        new WrappingLinearLayoutManager(OrderCommitActivity.this);
    linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
    linearLayoutManager.setSmoothScrollbarEnabled(true);

    CommitOrderAdapter commitOrderAdapter = new CommitOrderAdapter(OrderCommitActivity.this);
    commitOrderAdapter.updateItems(trolleyEntities);

    recyclerView.setLayoutManager(linearLayoutManager);
    recyclerView.setHasFixedSize(true);
    recyclerView.setAdapter(commitOrderAdapter);
    recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
    recyclerView.addItemDecoration(
        new DividerDecoration(OrderCommitActivity.this, R.dimen.material_1dp));

    /*计算总价钱*/
    totalPrice = 0;
    for (TrolleyEntity trolleyEntity : trolleyEntities) {
      totalPrice += Float.parseFloat(trolleyEntity.getGoodPrice());
    }

    totalCountTv.setText(trolleyEntities.size() + "");
    totalPriceTv.setText("￥" + totalPrice);
  }

  private void loadData() {

    /* Action=GetUserByAddress&uid=1*/
    Map<String, String> params = new HashMap<>(2);
    params.put("Action", "GetUserByAddress");
    params.put("uid",
        DBHelper.getInstance(getApplicationContext()).getUserID(OrderCommitActivity.this));

    AddressRetrofit.getInstance()
        .getAddressList(params)
        .doOnSubscribe(new Action0() {
          @Override public void call() {
            /*加载数据，显示进度条*/
            progressLayout.showLoading();
          }
        })
        .map(new Func1<HashMap, AddressEntity>() {
          @Override public AddressEntity call(HashMap hashMap) {

            OrderCommitActivity.this.defaultAddress =
                (AddressEntity) hashMap.get(AddressMangerActivity.DEFAULT_ADDRESS);

            return OrderCommitActivity.this.defaultAddress;
          }
        })
        .compose(OrderCommitActivity.this.<AddressEntity>bindUntilEvent(ActivityEvent.DESTROY))
        .subscribe(new Subscriber<AddressEntity>() {
          @Override public void onCompleted() {

            /*加载完毕，显示内容界面*/
            progressLayout.showContent();
          }

          @Override public void onError(Throwable error) {
            /*加载失败，显示错误界面*/
            OrderCommitActivity.this.showErrorLayout(error);
          }

          @Override public void onNext(AddressEntity addresses) {

            if (addresses != null) {

              SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
              spannableStringBuilder.append(addresses.getUserName());
              spannableStringBuilder.append("   " + addresses.getMobile() + "\n \n");
              spannableStringBuilder.append(
                  addresses.getProvince() + addresses.getCity() + addresses.getAddress());
              addressTv.setText(spannableStringBuilder);
            } else {
              addressTv.setText(getResources().getString(R.string.order_default_address));
            }
          }
        });
  }

  private void showErrorLayout(Throwable error) {
    if (error instanceof TimeoutException) {
      OrderCommitActivity.this.showError(getResources().getString(R.string.timeout_title),
          getResources().getString(R.string.timeout_content));
    } else if (error instanceof RetrofitError) {
      Log.e(TAG, "Kind:  " + ((RetrofitError) error).getKind());
      OrderCommitActivity.this.showError(getResources().getString(R.string.six_word_title),
          getResources().getString(R.string.six_word_content));
    } else if (error instanceof WebServiceException) {
      OrderCommitActivity.this.showError(getResources().getString(R.string.service_exception_title),
          getResources().getString(R.string.service_exception_content));
    } else {
      Log.e(TAG, error.getMessage());
      error.printStackTrace();
      throw new RuntimeException("See inner exception");
    }
  }

  private void showError(String errorTitle, String errorContent) {
    progressLayout.showError(getResources().getDrawable(R.drawable.ic_grey_logo_icon), errorTitle,
        errorContent, getResources().getString(R.string.retry_button_text), retryClickListener);
  }

  @Nullable @OnClick(R.id.order_commit_layout_address_ll) void onAddressClick() {

    OrderAddressActivity.navigateToOrderAddress(OrderCommitActivity.this);
    overridePendingTransition(0, 0);
  }

  @Nullable @OnClick(R.id.order_commit_layout_delivery_rl) void onDeliveryClick(
      RelativeLayout relativeLayout) {

    OrderDeliveryActivity.startFromLocation(OrderCommitActivity.this, defaultDelivery,
        DensityUtil.getLocationY(relativeLayout));
    overridePendingTransition(0, 0);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    if (requestCode == OrderAddressActivity.ACTIVITY_CODE) {

      if (resultCode == RESULT_OK && data != null) {

        defaultAddress =
            (AddressEntity) data.getSerializableExtra(OrderAddressActivity.DEFAULT_ADDRESS);

        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        spannableStringBuilder.append(defaultAddress.getUserName());
        spannableStringBuilder.append("   " + defaultAddress.getMobile() + "\n \n");
        spannableStringBuilder.append(
            defaultAddress.getProvince() + defaultAddress.getCity() + defaultAddress.getAddress());
        addressTv.setText(spannableStringBuilder);
      } else {

        OrderCommitActivity.this.defaultAddress = null;
        addressTv.setText(getResources().getString(R.string.order_default_address));
      }
    } else if (requestCode == OrderDeliveryActivity.ACTIVITY_CODE
        && resultCode == RESULT_OK
        && data != null) {

      defaultDelivery =
          (DeliveryEntity) data.getSerializableExtra(OrderDeliveryActivity.DEFAULT_DELIVERY);

      deliveryTv.setText(defaultDelivery.getDeliveryName());

      totalPriceTv.setText(
          "￥" + (totalPrice + Float.parseFloat(defaultDelivery.getDeliveryBaseFee())));
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {

    /*创建联合observable*/
    OrderCommitActivity.this.combineLatestEvents();

    return true;
  }

  private void combineLatestEvents() {

    addressChangeObservable = RxTextView.textChangeEvents(addressTv).skip(1);
    deliveryChangeObservable = RxTextView.textChangeEvents(deliveryTv).skip(1);

    Observable.combineLatest(addressChangeObservable, deliveryChangeObservable,
        new Func2<TextViewTextChangeEvent, TextViewTextChangeEvent, Boolean>() {
          @Override public Boolean call(TextViewTextChangeEvent textViewTextChangeEvent,
              TextViewTextChangeEvent textViewTextChangeEvent2) {

            return defaultAddress != null && defaultDelivery != null;
          }
        })
        .debounce(Constants.MILLISECONDS_300, TimeUnit.MILLISECONDS)
        .compose(OrderCommitActivity.this.<Boolean>bindUntilEvent(ActivityEvent.DESTROY))
        .observeOn(AndroidSchedulers.mainThread())
        .startWith(false)
        .subscribe(new Action1<Boolean>() {
          @Override public void call(Boolean aBoolean) {

            commitBtn.setEnabled(aBoolean);
          }
        });
  }

  private void startEnterAnim() {

    final Rect bounds = new Rect();
    revealFrameLayout.getHitRect(bounds);

    OrderCommitActivity.this.revealFrameLayout.setLayerType(View.LAYER_TYPE_HARDWARE, null);

    SupportAnimator revealAnimator =
        ViewAnimationUtils.createCircularReveal(revealFrameLayout.getChildAt(0), 0, bounds.left, 0,
            Utils.pythagorean(bounds.width(), bounds.height()));
    revealAnimator.setDuration(Constants.MILLISECONDS_400);
    revealAnimator.setInterpolator(new AccelerateInterpolator());
    revealAnimator.addListener(new SupportAnimator.SimpleAnimatorListener() {
      @Override public void onAnimationEnd() {

        if (progressLayout != null) {
          OrderCommitActivity.this.revealFrameLayout.setLayerType(View.LAYER_TYPE_NONE, null);
          OrderCommitActivity.this.loadData();
        }
      }
    });
    revealAnimator.start();
  }

  @Override public void exit() {

    ViewCompat.animate(rootView)
        .translationY(DensityUtil.getScreenHeight(OrderCommitActivity.this))
        .setDuration(Constants.MILLISECONDS_400)
        .setInterpolator(new LinearInterpolator())
        .withLayer()
        .setListener(new ViewPropertyAnimatorListenerAdapter() {
          @Override public void onAnimationEnd(View view) {
            OrderCommitActivity.this.finish();
          }
        });
  }

  @Override protected void onDestroy() {
    super.onDestroy();
  }
}
