package com.gykj.zhumulangma.common.net.http;


import android.util.Log;

import com.blankj.utilcode.util.SPUtils;
import com.google.gson.Gson;
import com.gykj.zhumulangma.common.AppConstants;
import com.gykj.zhumulangma.common.bean.UserBean;
import com.gykj.zhumulangma.common.net.NetManager;
import com.gykj.zhumulangma.common.net.dto.LoginDTO;
import com.gykj.zhumulangma.common.net.dto.ResponseDTO;
import com.gykj.zhumulangma.common.util.log.TLog;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Author: Thomas.
 * <br/>Date: 2019/8/1 8:01
 * <br/>Email: 1071931588@qq.com
 * <br/>Description:拦截指定内部异常,如Token超时等
 */
public class RetryException implements Function<Observable<Throwable>, Observable<?>> {

    @Override
    public Observable<?> apply(Observable<Throwable> observable) throws Exception {

        return observable.compose(upstream -> upstream.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()))
                .flatMap((Function<Throwable, Observable<?>>) throwable -> {
                    TLog.d(Log.getStackTraceString(throwable));
                    return Observable.error(throwable);
                /*
                    //拦截内部异常
                    if (throwable instanceof RespException) {
                        RespException ex = (RespException) throwable;
                        switch (ex.code) {
                            case ExceptionHandler.APP_ERROR.TOKEN_OUTTIME:
                                return reLogin();
                            default:
                                return Observable.error(throwable);
                        }
                    } else {
                        //外部异常直接放过
                        return Observable.error(throwable);
                    }
                    */
                });
    }

    /**
     * 重新登陆
     *
     * @return
     */
    private Observable reLogin() {
        final UserBean userBean = new Gson().fromJson(SPUtils.getInstance()
                .getString(AppConstants.SP.USER), UserBean.class);
        if (null != userBean) {
            LoginDTO loginDTO = new LoginDTO();
            loginDTO.setCode(userBean.getCode());
            loginDTO.setDescer_name(userBean.getDescer_name());
            loginDTO.setDescer_phone(userBean.getDescer_phone());
            loginDTO.setGraer_name(userBean.getGraer_name());
            loginDTO.setGraer_phone(userBean.getGraer_phone());
            return NetManager.getInstance().getCommonService().login(loginDTO)
                    .flatMap((Function<ResponseDTO<UserBean>, Observable<?>>) userBeanResponseDTO -> {
                        if (!userBeanResponseDTO.code.equals(ExceptionHandler.APP_ERROR.SUCCESS)) {
                            return Observable.error(new RespException(ExceptionHandler.APP_ERROR.ACCOUNT_ERROR,
                                    "账户异常,请先登陆"));
                        } else {
                            SPUtils.getInstance().put(AppConstants.SP.TOKEN, userBeanResponseDTO.result.getToken());
                            SPUtils.getInstance().put(AppConstants.SP.USER, new Gson().toJson(userBeanResponseDTO.result));
                            return Observable.just(0);
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());
        } else {
            return Observable.error(new RespException(ExceptionHandler.APP_ERROR.ACCOUNT_ERROR,
                    "账户异常,请先登陆"));
        }
    }
}
