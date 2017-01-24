package com.cojj.cordova.ble.service;

public class BLEData {

   private static BLEData singleton = new BLEData();

   public String test = "ASDASD";

   private BLEData() { }

   public static BLEData getInstance() {
      return singleton;
   }

}