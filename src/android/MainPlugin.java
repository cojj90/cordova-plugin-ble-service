package com.cojj.cordova.ble.service;

import org.apache.cordova.*;

import android.util.Log;

public class MainPlugin extends CordovaPlugin {
    private static final String TAG = "ICT BLE";

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Log.e(TAG, "BLE: Main Cordova Init");
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