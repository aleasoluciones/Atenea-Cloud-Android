# Atenea Cloud Android Client

Atenea Cloud Android client - SeaDroid mod

## Contributors

See [Contributors Graph](https://github.com/haiwen/atenea-cloud/graphs/contributors)

## Build the APK

* Make sure you have installed the [Android SDK](http://developer.android.com/sdk/index.html) then:

* cd into atenea-cloud directory
* Create `key.properties` file or simply rename `key.properties.example` and change configurations to match yours.

* Create keystore file if you don't have one

 ```
 keytool -genkey -v -keystore app/debug.keystore -alias AndroidDebugKey -keyalg RSA -keysize 2048 -validity 1 -storepass android -keypass android -dname "cn=TEST, ou=TEST, o=TEST, c=TE"
 ```
* Build with `./gradlew assembleRelease`

You will get `app/build/outputs/apk/atenea-cloud-${versionName}.apk` after the build finishes.

## Develop in Android Studio

### Prerequisites

* Android Studio
* OpenJDK 8 / OracleJDK 8

### Import project

* Open Android Studio
* Import project
* Select atenea-cloud directory
* Choose import from gradle
* Click next until import is completed
