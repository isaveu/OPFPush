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

package org.onepf.openpush.sample.event;

import org.jetbrains.annotations.NotNull;

/**
 * Created by  Kirill Rozov on 10.09.14.
 */
public final class RegisteredEvent extends ProviderEvent{
    private String mRegistrationId;

    public RegisteredEvent(@NotNull String providerName, @NotNull String registrationId) {
        super(providerName);
        mRegistrationId = registrationId;
    }

    @NotNull
    public String getRegistrationId() {
        return mRegistrationId;
    }
}