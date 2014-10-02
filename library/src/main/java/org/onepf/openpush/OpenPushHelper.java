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

package org.onepf.openpush;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import junit.framework.Assert;

import org.onepf.openpush.util.PackageUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.onepf.openpush.OpenPushLog.LOGD;
import static org.onepf.openpush.OpenPushLog.LOGI;
import static org.onepf.openpush.OpenPushLog.LOGW;

/**
 * Main class for manage push providers.
 * For get instance of this class call {@link #getInstance(android.content.Context)}.
 * <p/>
 * Before do any operations with {@code OpenPushHelper} you must call {@link #init(Options)}.
 * <p/>
 * For start select provider for register call {@link #register()}.
 *
 * @author Kirill Rozov
 * @since 04.09.2014
 */
public class OpenPushHelper {

    static final int STATE_UNREGISTERED = 0;
    static final int STATE_REGISTERING = 1;
    static final int STATE_WORKING = 2;
    static final int STATE_UNREGISTERING = 3;

    @Nullable
    private static OpenPushHelper sInstance;

    @NonNull
    private final Context mAppContext;

    @Nullable
    private OpenPushListener mListener;

    @Nullable
    private BroadcastReceiver mPackageReceiver;

    @Nullable
    private PushProvider mCurrentProvider;

    private Options mOptions;

    private final Object mRegistrationLock = new Object();
    private final Object mInitLock = new Object();

    private ProviderCallback mProviderCallback;

    private final OpenPushSettings mSettings;

    private OpenPushHelper(@NonNull Context context) {
        mAppContext = context.getApplicationContext();
        mSettings = new OpenPushSettings(context);
    }

    /**
     * Get instance of {@code OpenPushHelper}.
     *
     * @param context The current context.
     * @return Instance of {@link OpenPushHelper}.
     */
    public static OpenPushHelper getInstance(@NonNull Context context) {
        if (sInstance == null) {
            synchronized (OpenPushHelper.class) {
                if (sInstance == null) {
                    sInstance = new OpenPushHelper(context);
                }
            }
        }
        return sInstance;
    }

    /**
     * Create new instance of {@link OpenPushHelper}.
     * <b>Use for test purposes only!!!</b>
     *
     * @param context The current context.
     * @return New instance of {@link OpenPushHelper}.
     */
    static OpenPushHelper newInstance(@NonNull Context context) {
        synchronized (OpenPushHelper.class) {
            sInstance = new OpenPushHelper(context);
        }
        return sInstance;
    }

    public ProviderCallback getProviderCallback() {
        checkInitDone();
        return mProviderCallback;
    }

    /**
     * Is init done and you may work with {@code OpenPushHelper}.
     *
     * @return True if init is done, else - false.
     */
    public boolean isInitDone() {
        synchronized (mInitLock) {
            return mOptions != null;
        }
    }

    /**
     * Is push registered.
     *
     * @return True if push registered, else - false.
     */
    public boolean isRegistered() {
        int state = mSettings.getState();
        return mCurrentProvider != null
                && (state == STATE_WORKING || state == STATE_UNREGISTERING);
    }

    public boolean isUnegistered() {
        return mCurrentProvider == null && mSettings.getState() == STATE_UNREGISTERED;
    }

    public boolean isRegistering() {
        return mSettings.getState() == STATE_REGISTERING;
    }

    public boolean isUnegistering() {
        return mSettings.getState() == STATE_UNREGISTERING;
    }

    private void checkInitDone() {
        if (!isInitDone()) {
            throw new OpenPushException("Before work with OpenPushHelper call init() first.");
        }
    }

    /**
     * Init {@code OpenPushHelper}. You must call this method before do any operation.
     *
     * @param options Instance of {@code Options}.
     */
    public void init(@NonNull Options options) {
        if (mOptions == null) {
            synchronized (mInitLock) {
                if (mOptions == null) {
                    mOptions = options;
                }
            }
        } else {
            throw new OpenPushException("Attempt to init twice.");
        }

        mProviderCallback = new ProviderCallback(options);

        initLastProvider();
        LOGI("Init done.");
    }

    private void initLastProvider() {
        final PushProvider lastProvider = getLastProvider();
        if (lastProvider == null) {
            return;
        }

        LOGI("Try restore last provider '%s'.", lastProvider);

        if (lastProvider.isAvailable()) {
            if (lastProvider.isRegistered()) {
                LOGI("Last provider running.");
                mCurrentProvider = lastProvider;
                mSettings.saveState(STATE_WORKING);
            } else {
                LOGI("Last provider need register.");
                mSettings.saveState(STATE_REGISTERING);
                if (!register(lastProvider)) {
                    mSettings.saveState(STATE_UNREGISTERED);
                    mSettings.saveLastProvider(null);
                }
            }
        } else {
            lastProvider.onUnavailable();
            LOGI("Last provider isn't available.");
            mSettings.clear();
            if (mOptions.isRecoverProvider()) {
                LOGI("Try register any available provider.");
                register();
            }
        }
    }

    public boolean hasAvailableProvider() {
        for (PushProvider provider : mOptions.getProviders()) {
            if (provider.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    public void setListener(@Nullable OpenPushListener l) {
        mListener = l == null ? null : new MainThreadListenerWrapper(l);
    }

    void restartRegister() {
        checkInitDone();

        synchronized (mRegistrationLock) {
            switch (mSettings.getState()) {
                case STATE_REGISTERING:
                case STATE_UNREGISTERED:
                    mSettings.saveState(STATE_REGISTERING);
                    if (mOptions.isSelectSystemPreferred()
                            && registerSystemPreferredProvider()) {
                        return;
                    }
                    registerNextProvider(null);
                    break;

                case STATE_UNREGISTERING:
                    throw new OpenPushException("Can't register while unregistration is running.");

                case STATE_WORKING:
                    throw new OpenPushException("Attempt to register twice!");
            }
        }
    }

    /**
     * Start registration process async.
     *
     * @throws OpenPushException When try call this method while init not done,
     *                           unregistration process in progress or already registered.
     */
    public void register() {
        checkInitDone();

        synchronized (mRegistrationLock) {
            switch (mSettings.getState()) {
                case STATE_REGISTERING:
                    break;

                case STATE_UNREGISTERED:
                    mSettings.saveState(STATE_REGISTERING);
                    if (mOptions.isSelectSystemPreferred()
                            && registerSystemPreferredProvider()) {
                        return;
                    }
                    registerNextProvider(null);
                    break;

                case STATE_UNREGISTERING:
                    throw new OpenPushException("Can't register while unregistration is running.");

                case STATE_WORKING:
                    throw new OpenPushException("Attempt to register twice!");
            }
        }
    }

    private boolean registerSystemPreferredProvider() {
        for (PushProvider provider : mOptions.getProviders()) {
            if (PackageUtils.isSystemApp(mAppContext, provider.getHostAppPackage())
                    && register(provider)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Register first available provider. Iterate all provider from the next provider after
     * {@code lastProvider} param.
     *
     * @param lastProvider Last provider what check to register or null if has no.
     * @return True if find provider that can try to register, otherwise false.
     */
    private boolean registerNextProvider(@Nullable PushProvider lastProvider) {
        int nextProviderIndex = 0;
        final List<PushProvider> providers = mOptions.getProviders();
        if (lastProvider != null) {
            int lastProviderIndex = providers.indexOf(lastProvider);
            if (lastProviderIndex != -1) {
                nextProviderIndex = lastProviderIndex + 1;
            }
        }

        for (int providersCount = providers.size();
             nextProviderIndex < providersCount; ++nextProviderIndex) {

            if (register(providers.get(nextProviderIndex))) {
                return true;
            }
        }

        mSettings.saveState(STATE_UNREGISTERED);
        LOGW("No more available providers.");
        if (mListener != null) {
            mListener.onNoAvailableProvider();
        }
        return false;
    }

    /**
     * Same that {@link #register(PushProvider, boolean)}
     * with {@code registerNext} set to false.
     */
    boolean retryRegister(@NonNull String providerName) {
        PushProvider provider = getProviderWithException(providerName);
        return register(provider, false);
    }

    /**
     * Same that {@link #register(PushProvider, boolean)}
     * with {@code registerNext} set to false.
     */
    private boolean register(@NonNull PushProvider provider) {
        return register(provider, false);
    }

    /**
     * Start register provider.
     *
     * @param provider        Provider for registration.
     * @param tryRegisterNext Try to register next available push provider after the {@code provider},
     *                        if the {@code provider} isn't available.
     * @return If provider available and can start registration return true, otherwise - false.
     */
    private boolean register(@NonNull PushProvider provider, boolean tryRegisterNext) {
        if (provider.isAvailable()) {
            LOGD("Try register %s.", provider);
            provider.register();
            return true;
        }
        LOGI("Provider '%s' not available.", provider);
        return tryRegisterNext && registerNextProvider(provider);

    }

    /**
     * Start unregistration process async.
     *
     * @throws OpenPushException When try call this method while init not done,
     *                           registration process in progress or registration not done.
     */
    public void unregister() {
        checkInitDone();

        synchronized (mRegistrationLock) {
            if (!isRegistered()) {
                throw new OpenPushException("No one provider is registered!");
            }

            Assert.assertNotNull(mCurrentProvider);

            switch (mSettings.getState()) {
                case STATE_WORKING:
                    mSettings.saveState(STATE_UNREGISTERING);
                    unregisterPackageChangeReceiver();
                    mCurrentProvider.unregister();
                    break;

                case STATE_UNREGISTERING:
                    break;

                case STATE_REGISTERING:
                    throw new OpenPushException("Can't unregister when registration in progress.!");

                case STATE_UNREGISTERED:
                    throw new OpenPushException("Before to unregister you must register provider.!");
            }
        }
    }

    private void unregisterPackageChangeReceiver() {
        if (mPackageReceiver != null) {
            mAppContext.unregisterReceiver(mPackageReceiver);
            mPackageReceiver = null;
        }
    }

    @Nullable
    public PushProvider getCurrentProvider() {
        return mCurrentProvider;
    }

    /**
     * Search provider by name in {@code options} and return in.
     * If {@code} doesn't contain provider with described name return null.
     *
     * @param providerName Name of provider for search.
     * @return Provider with described name or null if nothing have found.
     */
    @Nullable
    private PushProvider getProvider(@NonNull String providerName) {
        for (PushProvider provider : mOptions.getProviders()) {
            if (providerName.equals(provider.getName())) {
                return provider;
            }
        }
        return null;
    }

    /**
     * Same that {@link #getProvider(String)} but if provider not found throw exception.
     *
     * @param providerName Name of provider for search.
     * @return Provider with name {@code providerName}.
     * @throws OpenPushException When {@code PushProvider} with name {@code providerName} not found.
     */
    @NonNull
    private PushProvider getProviderWithException(@NonNull String providerName) {
        PushProvider provider = getProvider(providerName);
        if (provider == null) {
            throw new OpenPushException("Provider with name '%s' not found.", providerName);
        }
        return provider;
    }

    @Nullable
    private PushProvider getLastProvider() {
        String storedProviderName = mSettings.getLastProviderName();
        if (storedProviderName != null) {
            if (!TextUtils.isEmpty(storedProviderName)) {
                PushProvider provider = getProvider(storedProviderName);
                if (provider != null) {
                    return provider;
                }
            }
            mSettings.saveLastProvider(null);
        }
        return null;
    }

    @Override
    public String toString() {
        return "OpenPushHelper{" +
                "options=" + mOptions +
                ", currentProvider=" + mCurrentProvider +
                ", initDone=" + isInitDone() +
                ", registered=" + isRegistered() +
                '}';
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            STATE_UNREGISTERED,
            STATE_UNREGISTERING,
            STATE_REGISTERING,
            STATE_WORKING
    })
    @interface State {
    }

    public class ProviderCallback {

        @Nullable
        private final AtomicInteger mTryCount;

        private ProviderCallback(@NonNull Options options) {
            mTryCount = options.getBackoff() != null ? new AtomicInteger(0) : null;
        }

        /**
         * Provider must call this method when new message received.
         *
         * @param providerName Name of provider from what message received.
         * @param extras       Message extras.
         * @throws OpenPushException When call this method when registration not done
         *                           or {@code providerName} isn't current registered provider.
         */
        public void onMessage(@NonNull String providerName, @Nullable Bundle extras) {
            checkProviderWorking(providerName);

            LOGD("onMessage(providerName = %s).", providerName);
            if (mListener != null) {
                mListener.onMessage(providerName, extras);
            }
        }

        private void checkProviderWorking(String providerName) {
            if (!isRegistered() || mCurrentProvider == null) {
                throw new OpenPushException("Can't receive message when not registered.");
            }
            if (!providerName.equalsIgnoreCase(mCurrentProvider.getName())) {
                throw new OpenPushException("Can't receive message from not registered provider. " +
                        "Current provider '%s', message source ='%s'",
                        mCurrentProvider.getName(), providerName);
            }
        }

        /**
         * Provider must call this method when new message deleted.
         *
         * @param providerName  Name of provider from what message deleted.
         * @param messagesCount Deleted messages count. If messages count is unknown pass -1.
         * @throws OpenPushException When call this method when registration not done
         *                           or {@code providerName} isn't current registered provider.
         */
        public void onDeletedMessages(@NonNull String providerName, int messagesCount) {
            checkProviderWorking(providerName);

            LOGD("onDeletedMessages(providerName = %s, messagesCount = %d).", providerName, messagesCount);
            if (mListener != null) {
                mListener.onDeletedMessages(providerName, messagesCount);
            }
        }

        /**
         * Call this method when device state changed and need retry registration.
         * May be call only when the helper in registered state.
         *
         * @throws OpenPushException When call this method when registration not done
         *                           or {@code providerName} isn't current registered provider.
         */
        public void onNeedRetryRegister() {
            LOGD("onNeedRetryRegister(providerName = %s).", mCurrentProvider);
            Assert.assertNotNull(mCurrentProvider);

            mSettings.clear();
            mCurrentProvider.onAppStateChanged();
            mSettings.saveState(STATE_REGISTERING);
            if (!register(mCurrentProvider)) {
                mSettings.saveState(STATE_UNREGISTERED);
            }
        }

        /**
         * Call this method when current provider become unavailable.
         *
         * @param provider Provider that become unavailable.
         * @throws OpenPushException When call this method when registration not done
         *                           or {@code providerName} isn't current registered provider.
         */
        public void onUnavailable(@NonNull PushProvider provider) {
            checkProviderWorking(provider.getName());

            LOGD("onUnavailable(provider = %s).", provider);
            if (mCurrentProvider != null && mCurrentProvider.equals(provider)) {
                mSettings.clear();
                mCurrentProvider.onUnavailable();
                mCurrentProvider = null;
                if (mListener != null) {
                    mListener.onProviderBecameUnavailable(provider.getName());
                }

                if (mOptions.isRecoverProvider()) {
                    OpenPushHelper.this.register(); //Restart registration
                }
            }
        }

        /**
         * Call this method on new registration or unregistration result.
         *
         * @param result Registration or unregistration result.
         * @throws OpenPushException When result type can't be handle
         *                           in current state of {@code OpenPushHelper}.
         */
        public void onResult(@NonNull Result result) {
            synchronized (mRegistrationLock) {
                if (result.getType() == Result.Type.REGISTRATION) {
                    if (mSettings.getState() == STATE_REGISTERING) {
                        onRegistrationResult(result);
                    } else {
                        throw new OpenPushException("Registration result can be" +
                                " handled only when registration is running.");
                    }
                } else if (result.getType() == Result.Type.UNREGISTRATION) {
                    if (mSettings.getState() == STATE_UNREGISTERING) {
                        onUnregistrationResult(result);
                    } else {
                        throw new OpenPushException("Unregistration result can be" +
                                " handled only when unregistration is running.");
                    }
                } else {
                    switch (mSettings.getState()) {
                        case STATE_REGISTERING:
                            onRegistrationResult(result);
                            return;

                        case STATE_UNREGISTERING:
                            onUnregistrationResult(result);
                            break;

                        default:
                            throw new OpenPushException("Result of unknown type can be" +
                                    " handled only when registration or unregistration is running.");
                    }
                }
            }
        }

        private void onUnregistrationResult(@NonNull Result result) {
            if (result.isSuccess()) {
                LOGI("Successfully unregister provider '%s'.", result.getProviderName());
                mSettings.clear();
                mCurrentProvider = null;
                if (mListener != null) {
                    Assert.assertNotNull(result.getRegistrationId());
                    mListener.onUnregistered(result.getProviderName(), result.getRegistrationId());
                }
            } else if (mListener != null) {
                Assert.assertNotNull(result.getErrorCode());

                LOGI("Error unregister provider '%s'.", result.getProviderName());
                PushProvider provider = getProviderWithException(result.getProviderName());
                mListener.onUnregistrationError(provider.getName(), result.getErrorCode());
            }
        }

        private void onRegistrationResult(@NonNull Result result) {
            if (result.isSuccess()) {
                onRegistrationSuccess(result);
            } else {
                onRegistrationError(result);
            }
        }

        private void onRegistrationError(@NonNull Result result) {
            Assert.assertNotNull(result.getErrorCode());

            LOGI("Error register provider '%s'.", result.getProviderName());
            PushProvider provider = getProviderWithException(result.getProviderName());
            if (mListener != null) {
                mListener.onRegistrationError(provider.getName(), result.getErrorCode());
            }

            if (mTryCount != null
                    && mOptions.getBackoff() != null
                    && mTryCount.getAndIncrement() < mOptions.getBackoff().getTryCount()) {
                postRegistration(provider.getName());
            } else {
                if (mTryCount != null) {
                    mTryCount.set(0);
                }
                registerNextProvider(provider);
            }
        }

        private void onRegistrationSuccess(Result result) {
            if (mTryCount != null) {
                mTryCount.set(0);
            }
            LOGI("Successfully register provider '%s'.", result.getProviderName());
            LOGI("Register id '%s'.", result.getRegistrationId());
            mSettings.saveState(STATE_WORKING);
            mSettings.saveLastAndroidId(android.provider.Settings.Secure.ANDROID_ID);

            mCurrentProvider = getProviderWithException(result.getProviderName());
            mSettings.saveLastProvider(mCurrentProvider);
            Assert.assertNotNull(result.getRegistrationId());
            if (mListener != null) {
                mListener.onRegistered(result.getProviderName(), result.getRegistrationId());
            }

            mPackageReceiver =
                    PackageUtils.registerPackageChangeReceiver(mAppContext, mCurrentProvider);
        }

        private void postRegistration(@NonNull String providerName) {
            Assert.assertNotNull(mTryCount);
            Assert.assertNotNull(mOptions.getBackoff());

            final long when = System.currentTimeMillis()
                    + mOptions.getBackoff().getDelay(mTryCount.get());

            LOGI("Post retry register provider '%s' at %s", providerName,
                    SimpleDateFormat.getDateTimeInstance().format(new Date(when)));

            Intent intent = new Intent(mAppContext, RetryBroadcastReceiver.class);
            intent.putExtra(RetryBroadcastReceiver.EXTRA_PROVIDER_NAME, providerName);

            AlarmManager am = (AlarmManager) mAppContext.getSystemService(Context.ALARM_SERVICE);
            am.set(AlarmManager.RTC, when, PendingIntent.getBroadcast(mAppContext, 0, intent, 0));
        }
    }

    /**
     * Wrapper for execute all method on main thread.
     *
     * @author Kirill Rozov
     * @since 24.09.14.
     */
    private static class MainThreadListenerWrapper implements OpenPushListener {
        private static final Handler HANDLER = new Handler(Looper.getMainLooper());
        private final OpenPushListener mListener;

        MainThreadListenerWrapper(OpenPushListener listener) {
            mListener = listener;
        }

        @Override
        public void onMessage(@NonNull final String providerName, @Nullable final Bundle extras) {
            HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onMessage(providerName, extras);
                }
            });
        }

        @Override
        public void onDeletedMessages(@NonNull final String providerName, final int messagesCount) {
            HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onDeletedMessages(providerName, messagesCount);
                }
            });

        }

        @Override
        public void onRegistered(@NonNull final String providerName,
                                 @NonNull final String registrationId) {
            HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onRegistered(providerName, registrationId);
                }
            });

        }

        @Override
        public void onRegistrationError(@NonNull final String providerName, @NonNull final Error error) {
            HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onRegistrationError(providerName, error);
                }
            });

        }

        @Override
        public void onUnregistrationError(@NonNull final String providerName,
                                          @NonNull final Error error) {
            HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onUnregistrationError(providerName, error);
                }
            });
        }

        @Override
        public void onNoAvailableProvider() {
            HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onNoAvailableProvider();
                }
            });
        }

        @Override
        public void onUnregistered(@NonNull final String providerName,
                                   @NonNull final String registrationId) {
            HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onUnregistered(providerName, registrationId);
                }
            });
        }

        @Override
        public void onProviderBecameUnavailable(@NonNull final String providerName) {
            HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onProviderBecameUnavailable(providerName);
                }
            });
        }
    }
}
