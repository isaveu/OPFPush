Implementation of [Amazon Device Messaging][1] for OPFPush.

## How To Use

Add following permissions to your AndroidManifest.xml file:

```xml
<permission android:name="${applicationId}.permission.RECEIVE_ADM_MESSAGE" />
<uses-permission
    android:name="${applicationId}.permission.RECEIVE_ADM_MESSAGE"
    android:protectionLevel="signature" />
```

also add following receiver:

```xml
<receiver
    android:name="org.onepf.opfpush.adm.ADMReceiver"
    android:permission="com.amazon.device.messaging.permission.SEND">

    <intent-filter>
        <action android:name="com.amazon.device.messaging.intent.REGISTRATION" />
        <action android:name="com.amazon.device.messaging.intent.RECEIVE" />

        <category android:name="${applicationId}" />
    </intent-filter>
</receiver>
```

If you use JAR dependency, you also must add to your application AndroidManifest.xml file following:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:amazon="http://schemas.amazon.com/apk/res/android">

    <uses-permission android:name="com.amazon.device.messaging.permission.RECEIVE" />
    <uses-permission android:name="android.permission.GET_ACCOUNTS" />

    <application>

        <amazon:enable-feature
            android:name="com.amazon.device.messaging"
            android:required="false" />

        <service
            android:name=".ADMService"
            android:exported="false" />

        <receiver android:name=".LoginAccountsChangedReceiver">

            <intent-filter>
                <action android:name="android.accounts.LOGIN_ACCOUNTS_CHANGED" />
            </intent-filter>

        </receiver>

    </application>
</manifest>
```

To use `ADMProvider` just add it to `Configuration` when building new instance, like this:

```java
Configuration.Builder builder = new Configuration.Builder();
builder.addProviders(new ADMProvider(this));
```

[1]: https://developer.amazon.com/appsandservices/apis/engage/device-messaging
