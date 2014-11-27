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

package org.onepf.opfpush.gcm;

import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * @author Kirill Rozov
 * @since 06.09.14.
 */
//CHECKSTYLE:OFF
interface GCMConstants {
    String ACTION_REGISTRATION = "com.google.android.c2dm.intent.REGISTRATION";
    String ACTION_REGISTRATION_CALLBACK = BuildConfig.APPLICATION_ID + ".intent.REGISTRATION";
    String ACTION_UNREGISTRATION_CALLBACK = BuildConfig.APPLICATION_ID + ".intent.UNREGISTRATION";

    String EXTRA_ERROR_ID = "error_id";
    String EXTRA_REGISTRATION_ID = "registration_id";

    String ERROR_AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
    String ERROR_SERVICE_NOT_AVAILABLE = GoogleCloudMessaging.ERROR_SERVICE_NOT_AVAILABLE;
}
//CHECKSTYLE:ON
