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

package org.onepf.opfpush.nokia;

import android.content.Context;

import com.nokia.push.PushBroadcastReceiver;

/**
 * BroadcastReceiver that receives Nokia Push Notification messages and delivers them to
 * {@link NokiaNotificationService}.
 *
 * @author Kirill Rozov
 * @since 05.09.2014
 */
public final class NokiaNotificationsReceiver extends PushBroadcastReceiver {

    private static final String SERVICE_CLASS_NAME
            = NokiaNotificationService.class.getCanonicalName();

    /**
     * Gets the class name of the intent service that will handle Push Notification messages.
     */
    @Override
    protected String getPushIntentServiceClassName(Context context) {
        return SERVICE_CLASS_NAME;
    }
}
