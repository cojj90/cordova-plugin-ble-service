"use strict";

module.exports = {
    scan: function (success, failure) {
        cordova.exec(success, failure, 'ICT_BLE', 'scan', null);
    }
};
