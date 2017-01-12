"use strict";

module.exports = {
    scan: function (services, seconds, success, failure) {
        var successWrapper = function (peripheral) {
            convertToNativeJS(peripheral);
            success(peripheral);
        };
        cordova.exec(successWrapper, failure, 'BLE', 'scan', [services, seconds]);
    }
};