package com.oursky.authgeartest.wxapi;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.oursky.authgear.Authgear;
import com.oursky.authgear.OnConfigureListener;
import com.oursky.authgear.OnWeChatAuthCallbackListener;
import com.oursky.authgeartest.MainApplication;
import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

public class WXEntryActivity extends Activity implements IWXAPIEventHandler{
    private static final String TAG = WXEntryActivity.class.getSimpleName();

    private IWXAPI api;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        api = WXAPIFactory.createWXAPI(this, MainApplication.WECHAT_APP_ID, false);
        try {
            Intent intent = getIntent();
            api.handleIntent(intent, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
        api.handleIntent(intent, this);
    }

    @Override
    public void onReq(BaseReq req) {
        Log.d(TAG, "onReq: " + req.getType());
        finish();
    }

    @Override
    public void onResp(BaseResp resp) {
        String result;

        switch (resp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                result = "errcode_success";
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                result = "errcode_cancel";
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                result = "errcode_deny";
                break;
            case BaseResp.ErrCode.ERR_UNSUPPORT:
                result = "errcode_unsupported";
                break;
            default:
                result = "errcode_unknown";
                break;
        }

        Log.d(TAG, "onResp: " + result + ", type=" + resp.getType());
        if (resp.getType() == ConstantsAPI.COMMAND_SENDAUTH) {
            SendAuth.Resp authResp = (SendAuth.Resp)resp;
            Log.d(TAG, "code: " + authResp.code + ", state= " + authResp.state);
            sendWeChatAuthCallback(authResp.code, authResp.state);
        }
        finish();
    }

    private void sendWeChatAuthCallback(String code, String state) {
        Application app = getApplication();
        SharedPreferences preferences = app.getSharedPreferences("authgear.demo", Context.MODE_PRIVATE);
        if (preferences != null) {
            String clientID = preferences.getString("clientID", "");
            String endpoint = preferences.getString("endpoint", "");
            Boolean isThirdParty = preferences.getBoolean("isThirdParty", true);
            Authgear authgear = new Authgear(getApplication(), clientID, endpoint, null, isThirdParty);
            authgear.configure(false, new OnConfigureListener() {
                @Override
                public void onConfigured() {
                    authgear.weChatAuthCallback(code, state, new OnWeChatAuthCallbackListener() {
                        @Override
                        public void onWeChatAuthCallback() {
                            Log.d(TAG, "onWeChatAuthCallback");
                        }

                        @Override
                        public void onWeChatAuthCallbackFailed(Throwable throwable) {
                            Log.e(TAG, "onWeChatAuthCallbackFailed", throwable);
                        }
                    });
                }

                @Override
                public void onConfigurationFailed(@NonNull Throwable throwable) {
                    Log.e(TAG, throwable.toString());
                }
            });

        }
    }
}
