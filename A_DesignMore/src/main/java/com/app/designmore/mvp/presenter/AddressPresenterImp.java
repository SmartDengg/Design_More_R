package com.app.designmore.mvp.presenter;

import android.content.Context;
import com.app.designmore.event.ProvinceEvent;
import com.app.designmore.manager.EventBusInstance;
import com.app.designmore.mvp.viewinterface.AddressView;
import com.app.designmore.retrofit.entity.Province;
import com.app.designmore.rxAndroid.SchedulersCompat;
import com.app.designmore.view.CustomWheelPicker;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Func0;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Created by Joker on 2015/9/9.
 */
public class AddressPresenterImp implements AddressPresenter {

  private Context context;
  private AddressView addressView;
  private List<Province> provinces = new ArrayList<>();
  private Scheduler.Worker worker;
  private Subscription subscribe;

  public AddressPresenterImp() {

  }

  @Override public void attach(Context context, AddressView addressView) {
    this.context = context;
    this.addressView = addressView;
  }

  @Override public void showPicker() {

    if (provinces.size() > 0) {
      addressView.onInflateFinish(provinces);
    } else {

      subscribe = Observable.defer(new Func0<Observable<List<Province>>>() {
        @Override public Observable<List<Province>> call() {

          InputStream inputStream = null;
          try {
            inputStream = context.getResources().getAssets().open("address.txt");
            byte[] arrayOfByte = new byte[inputStream.available()];
            inputStream.read(arrayOfByte);
            JSONArray jsonList = new JSONArray(new String(arrayOfByte, "UTF-8"));
            Gson gson =
                new GsonBuilder().excludeFieldsWithoutExposeAnnotation().serializeNulls().create();
            for (int i = 0; i < jsonList.length(); i++) {
              provinces.add(gson.fromJson(jsonList.getString(i), Province.class));
            }
          } catch (Exception e) {
            return Observable.error(e);
          } finally {
            if (inputStream != null) {
              try {
                inputStream.close();
              } catch (IOException e) {
                return Observable.error(e);
              }
            }
          }
          return Observable.just(provinces);
        }
      })
          .compose(SchedulersCompat.<List<Province>>applyExecutorSchedulers())
          .doOnSubscribe(new Action0() {
            @Override public void call() {
              /*显示进度条*/
              addressView.showProgress();
            }
          })
          .subscribe(new Subscriber<List<Province>>() {
            @Override public void onCompleted() {
              /*隐藏进度条*/
              addressView.hideProgress();
            }

            @Override public void onError(Throwable e) {
              e.printStackTrace();
              addressView.showError();
            }

            @Override public void onNext(List<Province> provinces) {
              addressView.onInflateFinish(provinces);
            }
          });


      /*if (worker == null) {
        worker = Schedulers.io().createWorker();
      }*/

       /*worker.schedule(new Action0() {
        @Override public void call() {

          InputStream in = null;

          try {
            in = context.getResources().getAssets().open("address.txt");
            byte[] arrayOfByte = new byte[in.available()];
            in.read(arrayOfByte);
            JSONArray jsonList = new JSONArray(new String(arrayOfByte, "UTF-8"));
            Gson gson =
                new GsonBuilder().excludeFieldsWithoutExposeAnnotation().serializeNulls().create();
            for (int i = 0; i < jsonList.length(); i++) {
              provinces.add(gson.fromJson(jsonList.getString(i), Province.class));
            }
          } catch (Exception ignored) {
          } finally {
            if (in != null) {
              try {
                in.close();
              } catch (IOException ignored) {
              }
            }
            if (provinces.size() > 0) {
              addressView.hideProgress();
              addressView.onInflateFinish(provinces);
            } else {
              addressView.showError();
            }
          }
        }
      });*/
    }
  }

  @Override public void detach() {
    //if (!worker.isUnsubscribed()) worker.unsubscribe();
    if (subscribe != null && !subscribe.isUnsubscribed()) subscribe.unsubscribe();
  }
}
