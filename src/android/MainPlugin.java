package com.cojj.cordova.ble.service;

import org.apache.cordova.*;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.os.IBinder;
import android.util.Log;
import android.os.Handler;
import android.Manifest;
import android.content.pm.PackageManager;

public class MainPlugin extends CordovaPlugin {
    private static final String TAG = "ICT BLE";
    private static final String ACCESS_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int REQUEST_ACCESS_COARSE_LOCATION = 2;
    private CallbackContext callbackContext;
    private Context context;
    private BLEService bleService;

    private String credential;
    private byte scanSensitivity;
    private Intent bleServiceIntent;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName paramAnonymousComponentName, IBinder paramAnonymousIBinder) {
            Log.e(TAG, "BLE: SERVICE CONNECTED");
            MainPlugin.this.bleService = ((BLEService.LocalBinder) paramAnonymousIBinder).getService();
            //MainPlugin.this.bleService.initialise(cordova);

        }

        public void onServiceDisconnected(ComponentName paramAnonymousComponentName) {
            Log.e(TAG, "BLE: SERVICE DISCONNECTED");
        }

    };

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Log.e(TAG, "BLE: Main Cordova Init");
    }

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {

        JSONObject options = args.getJSONObject(0);
        Log.e(TAG, "BLE: " + action);
        //Log.e(TAG, "BLE Arg1: " + options.getInt("id"));
        //Log.e(TAG, "BLE Arg2: " + options.getBoolean("test"));
        Log.e(TAG, "BLE Arg3: " + options.getString("credential"));

        this.credential = options.getString("credential");
        this.scanSensitivity = (byte) options.getInt("sensitivity");

        if(options.getBoolean("test") == true){
            Log.e(TAG, "BLE: STOP");
            this.stopService();
            return true;
        }
        Log.e(TAG, "BLE: GO");

        if (!PermissionHelper.hasPermission(this, ACCESS_COARSE_LOCATION)) {
            PermissionHelper.requestPermission(this, REQUEST_ACCESS_COARSE_LOCATION, ACCESS_COARSE_LOCATION);
            return false;
        }else{
            initService();
        }
        
        this.callbackContext = callbackContext;

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                PluginResult result = new PluginResult(PluginResult.Status.OK, "YOUR_MESSAGE");
                // PluginResult result = new PluginResult(PluginResult.Status.ERROR, "YOUR_ERROR_MESSAGE");
                result.setKeepCallback(true);
                MainPlugin.this.callbackContext.sendPluginResult(result);
            }
        }, 5000);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
                result.setKeepCallback(false);
                MainPlugin.this.callbackContext.sendPluginResult(result);
            }
        }, 10000);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                MainPlugin.this.callbackContext.success();
            }
        }, 15000);

        /*
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
        */
        return true;
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
            int[] grantResults) /* throws JSONException */ {
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) {
                Log.e(TAG, "BLE User *rejected* Coarse Location Access");
                return;
            }
        }

        switch (requestCode) {
        case REQUEST_ACCESS_COARSE_LOCATION:
            Log.e(TAG, "BLE User granted Coarse Location Access");        
            initService();
            break;
        }
    }

    private void initService(){
            this.bleServiceIntent = new Intent(cordova.getActivity(), BLEService.class);
            //cordova.getActivity().bindService(intent, this.mServiceConnection, Context.BIND_AUTO_CREATE);
            this.context = cordova.getActivity();
            this.bleServiceIntent.putExtra("credential", this.credential);
            this.bleServiceIntent.putExtra("scanSensitivity", this.scanSensitivity);
            context.startService(this.bleServiceIntent);
    }

    private void stopService(){
        this.context.stopService(this.bleServiceIntent);
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