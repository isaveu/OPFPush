/*
 * Copyright 2012-2014 One Platform Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onepf.opfpush.adm;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.amazon.device.messaging.ADMConstants;

import org.onepf.opfpush.*;
import org.onepf.opfpush.Error;

/**
 * This class allows your app to receive messages sent via ADM.
 * <p/>
 * All methods on this class are called on a background thread with a wake lock held.
 * It is safe to do long-running operations in these methods.
 *
 * @author Kirill Rozov
 * @since 06.09.14.
 */
public class ADMService extends IntentService {

    private static String mLastRegistrationId;

    public ADMService() {
        super("ADMService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final String action = intent.getAction();
        if (ADMConstants.LowLevel.ACTION_RECEIVE_ADM_MESSAGE.equals(action)) {
            onMessage(intent);
        } else if (ADMConstants.LowLevel.ACTION_APP_REGISTRATION_EVENT.equals(action)) {
            if (intent.hasExtra(ADMConstants.LowLevel.EXTRA_ERROR)) {
                @ADMError String errorId = intent.getStringExtra(ADMConstants.LowLevel.EXTRA_ERROR);
                onRegistrationError(errorId);
            } else if (intent.hasExtra(ADMConstants.LowLevel.EXTRA_UNREGISTERED)) {
                if (mLastRegistrationId != null) {
                    onUnregistered(mLastRegistrationId);
                } else {
                    throw new OPFPushException("Not registered.");
                }
            } else if (intent.hasExtra(ADMConstants.LowLevel.EXTRA_REGISTRATION_ID)) {
                mLastRegistrationId = intent.getStringExtra(ADMConstants.LowLevel.EXTRA_REGISTRATION_ID);
                onRegistered(mLastRegistrationId);
            } else {
                onRegistrationError(ADMConstants.ERROR_SERVICE_NOT_AVAILABLE);
            }
        }
    }

    /**
     * Called each time ADM delivers a message to an instance of your app.
     *
     * @param intent An intent containing the message and associated data.
     *               You extract the message content from the set of extras attached
     *               to the {@code com.amazon.device.messaging.intent.RECEIVE intent}.
     *               For an example of defining the behavior of the onMessage() callback,
     *               see SampleADMMessageHandler.java in the ADMMessenger sample app.
     */
    protected void onMessage(@NonNull Intent intent) {
        OPFPushHelper.getInstance(this)
                .getProviderCallback().onMessage(ADMProvider.NAME, intent.getExtras());
    }

    /**
     * Called when a registration request fails.
     * You should consider a registration error fatal.
     * In response, your app may degrade gracefully,
     * or you may wish to notify the user that this part of your app's functionality is not available.
     *
     * @param errorId One of the following values:
     *                {@link ADMConstants#ERROR_AUTHENTICATION_FAILED},
     *                {@link ADMConstants#ERROR_INVALID_SENDER},
     *                {@link ADMConstants#ERROR_SERVICE_NOT_AVAILABLE}.
     */
    protected void onRegistrationError(@NonNull @ADMError String errorId) {
        Error error = convertError(errorId);
        OPFPushHelper.getInstance(this).getProviderCallback()
                .onResult(
                        Result.error(ADMProvider.NAME,
                                error,
                                Result.Type.REGISTRATION)
                );
    }

    @NonNull
    private Error convertError(@NonNull @ADMError String errorId) {
        Error error;
        if (ADMConstants.ERROR_SERVICE_NOT_AVAILABLE.equals(errorId)) {
            error = Error.SERVICE_NOT_AVAILABLE;
        } else if (ADMConstants.ERROR_INVALID_SENDER.equals(errorId)) {
            error = Error.INVALID_SENDER;
        } else if (ADMConstants.ERROR_AUTHENTICATION_FAILED.equals(errorId)) {
            error = Error.AUTHENTICATION_FAILED;
        } else {
            throw new OPFPushException(String.format("Unknown error '%s'.", errorId));
        }
        return error;
    }

    /**
     * Called when a registration request succeeds.
     * ADM may call this message in response to your app calling startRegister()
     * or if ADM has updated the registration ID for this app instance.
     *
     * @param registrationId The new registration ID for the instance of your app.
     *                       Pass this value to your components that are using ADM to send messages.
     *                       The {@link com.amazon.device.messaging.ADM#getRegistrationId()}
     *                       method also obtains the registration ID for an instance of your app.
     */
    protected void onRegistered(@NonNull String registrationId) {
        //TODO Send registration id.
        OPFPushHelper.getInstance(this).getProviderCallback()
                .onResult(Result.success(ADMProvider.NAME, registrationId, Result.Type.REGISTRATION));
    }

    /**
     * Called on successful unregistration. This method may be called in response to your app
     * calling startUnregister() or if ADM has unregistered the app for some reason (typically
     * because the device has lost its association with a user's Amazon account).
     * If this message is called, your app should notify your components that are using ADM
     * to send messages, so that they know this instance of your app is no longer a valid recipient.
     *
     * @param registrationId The registration ID for the instance of your app that is now unregistered.
     *                       This ID is no longer a valid destination for messages.
     *                       Calling {@link com.amazon.device.messaging.ADM#getRegistrationId()}
     *                       will show the registration ID for an unregistered app as {@code null}.
     */
    protected void onUnregistered(@NonNull String registrationId) {
        OPFPushHelper.getInstance(this).getProviderCallback()
                .onResult(Result.success(ADMProvider.NAME, registrationId, Result.Type.UNREGISTRATION));
    }
}