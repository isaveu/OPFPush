package org.onepf.openpush.adm;

import android.support.annotation.StringDef;

import com.amazon.device.messaging.ADMConstants;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by krozov on 24.09.14.
 */
@Retention(RetentionPolicy.SOURCE)
@StringDef({
        ADMConstants.ERROR_AUTHENTICATION_FAILED,
        ADMConstants.ERROR_INVALID_SENDER,
        ADMConstants.ERROR_SERVICE_NOT_AVAILABLE
})
public @interface ADMError {
}