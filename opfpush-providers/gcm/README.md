[Google Cloud Messaging][1] implementation for OPFPush.

## How To Use

Add following permissions to your AndroidManifest.xml file:

```xml
<uses-permission android:name="${applicationId}.permission.C2D_MESSAGE" />
<permission
    android:name="${applicationId}.permission.C2D_MESSAGE"
    android:protectionLevel="signature" />
```

also add following receiver:

```xml
<receiver
    android:name="org.onepf.opfpush.gcm.GCMReceiver"
    android:exported="true"
    android:permission="com.google.android.c2dm.permission.SEND">
    <intent-filter>
        <action android:name="com.google.android.c2dm.intent.RECEIVE" />
        <category android:name="${applicationId}" />
    </intent-filter>
    <intent-filter>
        <action android:name="org.onepf.opfpush.gcm.intent.UNREGISTRATION" />
        <action android:name="org.onepf.opfpush.gcm.intent.REGISTRATION" />
    </intent-filter>
</receiver>
```

If you use JAR dependency, you also must add to your application AndroidManifest.xml file following:

```xml
<uses-permission android:name="com.google.android.c2dm.permission.RECEIVE" />
<uses-permission android:name="android.permission.GET_ACCOUNTS" />

<application>
    <service
        android:name=".GCMService"
        android:exported="false" />

    <service
        android:name=".SendMessageService"
        android:exported="false" />

</application>
```

To use `GCMProvider` just add it to `Configuration` when building new instance, like this:

```java
Configuration.Builder builder = new Configuration.Builder();
builder.addProviders(new GCMProvider(this, GCM_SENDER_ID));
```

[1]: https://developer.android.com/google/gcm/index.html
