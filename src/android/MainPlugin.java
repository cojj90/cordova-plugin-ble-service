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

import android.os.SystemClock;
import android.app.AlarmManager;
import android.app.PendingIntent;

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
    private PendingIntent pintent;
    private AlarmManager alarm;

    public static byte bobby = -55;
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
        this.context = cordova.getActivity();
        this.bleServiceIntent = new Intent(this.context, BLEService.class);
        this.alarm = (AlarmManager)this.context.getSystemService(Context.ALARM_SERVICE);
        Log.e(TAG, "BLE: Main Cordova Init");
    }

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {

        JSONObject options = args.getJSONObject(0);
        Log.e(TAG, "BLE: " + action);
        //Log.e(TAG, "BLE Arg1: " + options.getInt("id"));
        //Log.e(TAG, "BLE Arg2: " + options.getBoolean("test"));
        

        this.credential = options.getString("credential");
        this.scanSensitivity = (byte) options.getInt("sensitivity");
        Log.e(TAG, "BLE Arg3: " + this.credential + "/" + this.scanSensitivity);

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
        
        /*
        
        this.bleServiceIntent = new Intent(cordova.getActivity(), BLEService.class);
        //context.bindService(this.bleServiceIntent, this.mServiceConnection, Context.BIND_AUTO_CREATE);
        this.bleServiceIntent.putExtra("credential", this.credential);
        this.bleServiceIntent.putExtra("scanSensitivity", this.scanSensitivity);
        context.startService(this.bleServiceIntent);
        */
        
        // this.alarm.cancel(this.pintent);
        // this.bleServiceIntent.putExtra("credential", this.credential);
        // this.bleServiceIntent.putExtra("scanSensitivity", this.scanSensitivity);
        // Log.e(TAG, "BLE: SENSITIVITY: "+this.bleServiceIntent.getByteExtra("scanSensitivity", (byte) -59));
        // //this.context.startService(this.bleServiceIntent);
        // //this.context.setExact(int, long, PendingIntent).
        // this.pintent = PendingIntent.getService(this.context, 444, this.bleServiceIntent, 0);
        // //PendingIntent.FLAG_CANCEL_CURRENT
        // this.alarm.cancel(this.pintent);
        
        // //this.alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), AlarmManager.INTERVAL_FIFTEEN_MINUTES, this.pintent);
        // alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),30000, this.pintent);

        Intent intent  = new Intent(this.context, BLEService.class);
        intent.putExtra("credential", this.credential);
        Log.e(TAG, "BLE check1: " + this.credential + "/" + this.scanSensitivity);
        intent.putExtra("scanSensitivity", this.scanSensitivity);
        Log.e(TAG, "BLE check2: " + this.credential + "/" + this.scanSensitivity + "/" + intent.getByteExtra("scanSensitivity", (byte) -59));
        PendingIntent pendingIntent = PendingIntent.getService(this.context, 444, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager)this.context.getSystemService(Context.ALARM_SERVICE);
        this.context.startService(intent);
        //int a = 3600000;
        int a = 12*60000;
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+a, a, pendingIntent);
        //alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+60000,60000, pendingIntent);
        /*
        this.bleServiceIntent = new Intent(context, BLEIntentService.class);
        this.bleServiceIntent.putExtra("credential", this.credential);
        this.bleServiceIntent.putExtra("scanSensitivity", this.scanSensitivity);
        context.startService(this.bleServiceIntent);
        */
        
    }

    private void stopService(){
        // this.context.stopService(this.bleServiceIntent);
        // this.alarm.cancel(this.pintent);

        Intent intent  = new Intent(this.context, BLEService.class);
        intent.putExtra("credential", this.credential);
        intent.putExtra("scanSensitivity", this.scanSensitivity);
        PendingIntent pendingIntent = PendingIntent.getService(this.context, 444, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager)this.context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        this.context.stopService(intent);
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

    @Override
    public void onDestroy() {
        Log.e(TAG, "BLE: Main onDestroy");
        this.stopService();
        super.onDestroy();
    }

    // END OF ANDROID LIFECYCLE
}