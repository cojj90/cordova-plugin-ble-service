package com.cojj.cordova.ble.service;

import org.apache.cordova.*;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.os.Handler;

public class MainPlugin extends CordovaPlugin {
    private static final String TAG = "ICT BLE";
    private CallbackContext callbackContext;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Log.e(TAG, "BLE: Main Cordova Init");
    }

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        Log.e(TAG, "BLE: " + action);
        this.callbackContext = callbackContext;

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                MainPlugin.this.callbackContext.success();
            }
        }, 5000);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                MainPlugin.this.callbackContext.success();
            }
        }, 10000);

        /*
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
        */
        return true;
    }

    // START OF ANDROID LIFECYCLE
    public void onCreate() {
        Log.e(TAG, "BLE: Main onCreate");
    }

    public void onRestart() {
        Log.e(TAG, "BLE: Main onRestart");
    }

    public void onStart() {
        Log.e(TAG, "BLE: Main onStart");
    }

    public void onResume() {
        Log.e(TAG, "BLE: Main onResume");
    }

    public void onPause() {
        Log.e(TAG, "BLE: Main onPause");
    }

    public void onStop() {
        Log.e(TAG, "BLE: Main onStop");
    }

    public void onDestroy() {
        Log.e(TAG, "BLE: Main onDestroy");
    }

    // END OF ANDROID LIFECYCLE
}