# Atenea Cloud Android Client
[![Build Android APK](https://github.com/aleasoluciones/Atenea-Cloud-Android/actions/workflows/build-android-apk.yml/badge.svg)](https://github.com/aleasoluciones/Atenea-Cloud-Android/actions/workflows/build-android-apk.yml)

Atenea Cloud Android client - [SeaDroid](https://github.com/haiwen/seadroid) mod

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

## GitHub Actions Secrets

You need to add some GitHub Actions Secrets to launch the Action and build the release version:
```
ALEA_KEYSTORE=XXXX
ALEA_KEYSTORE_ALIAS=XXXX
ALEA_KEYSTORE_ALIAS_PASSWORD=XXXX
ALEA_KEYSTORE_PASSWORD=XXXX
```

## Run GitHub Action Workflow locally

You can run locally the 'build and release' GitHub action using the [act](https://github.com/nektos/act) project:

```
# A local directory is needed to upload the created artifacts (APK)
$ mkdir -p $HOME/.act
$ echo "--artifact-server-path $HOME/.act" >> $HOME/.actrc

The GITHUB_TOKEN env var is needed when running the workflow (You can obtain the token using `gh auth status -t`:
$ act -s GITHUB_TOKEN="XXXX_TOKEN_HERE_XXXX"

```

