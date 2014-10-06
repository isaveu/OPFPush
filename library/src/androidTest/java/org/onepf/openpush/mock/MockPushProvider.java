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

package org.onepf.openpush.mock;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.onepf.openpush.BasePushProvider;
import org.onepf.openpush.OpenPushHelper;
import org.onepf.openpush.Result;
import org.onepf.openpush.util.PackageUtils;
import org.robolectric.Robolectric;

import java.util.UUID;

/**
 * @author Kirill Rozov
 * @since 11.09.14.
 */
public class MockPushProvider extends BasePushProvider {

    public static final String DEFAULT_HOST_APP_PACKAGE = "org.onepf.store";
    public static final String DEFAULT_NAME = MockPushProvider.class.getName();
    private String mRegistrationId;
    private boolean mAvailable;

    public MockPushProvider(@NonNull Context context) {
        this(context, DEFAULT_NAME);
    }

    public MockPushProvider(@NonNull Context context, String name) {
        this(context, name, true);
    }

    public MockPushProvider(@NonNull Context context, @NonNull String name, boolean available) {
        this(context, name, available, DEFAULT_HOST_APP_PACKAGE);
    }

    public MockPushProvider(@NonNull Context context, boolean available) {
        this(context, DEFAULT_NAME, available, DEFAULT_HOST_APP_PACKAGE);
    }

    public MockPushProvider(@NonNull Context context,
                            @NonNull String name,
                            @NonNull String hotAppPackage) {
        this(context, name, true, hotAppPackage);
    }

    public MockPushProvider(@NonNull Context context,
                     @NonNull String name,
                     boolean available,
                     @NonNull String hotAppPackage) {
        super(context, name, hotAppPackage);
        mAvailable = available;
    }

    @Override
    public void register() {
        mRegistrationId = UUID.randomUUID().toString();
        OpenPushHelper.getInstance(getContext()).getProviderCallback()
                .onResult(Result.success(getName(), mRegistrationId, Result.Type.REGISTRATION));
    }

    @Override
    public void unregister() {
        mRegistrationId = null;
        OpenPushHelper.getInstance(getContext()).getProviderCallback()
                .onResult(Result.success(getName(), mRegistrationId, Result.Type.UNREGISTRATION));
    }

    public void setAvailable(boolean available) {
        mAvailable = available;
    }

    @Override
    public void onRegistrationInvalid() {
        mRegistrationId = null;
    }

    @Override
    public void onUnavailable() {
        mRegistrationId = null;
    }

    @Override
    public boolean isAvailable() {
        return mAvailable && PackageUtils.isInstalled(Robolectric.application, getHostAppPackage());
    }

    @Override
    public boolean isRegistered() {
        return mRegistrationId != null;
    }

    @Nullable
    @Override
    public String getRegistrationId() {
        return mRegistrationId;
    }

    @Override
    public boolean checkManifest() {
        return true;
    }
}
