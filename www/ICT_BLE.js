"use strict";

module.exports = {
    scan: function (success, failure, param) {
        cordova.exec(success, failure, 'ICT_BLE', 'scan', param);
    }
};
