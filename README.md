@daffodilsw/react-native-codepush

A lightweight OTA (Over-The-Air) update system for React Native, allowing you to update your JavaScript bundle instantly without publishing a new version to the Play Store or App Store.

This library supports:
- Android and iOS both platform

- S3/HTTPS bundle hosting
- Zero AppCenter dependency
- Customizable update modal

# 🛠 Getting Started

Note: This library assumes you already have a working React Native environment.

If not, follow the official guide:
https://reactnative.dev/docs/set-up-your-environment

📦 Installation

Install the module using npm or yarn:

## npm
npm install @daffodilsw/react-native-codepush

## yarn
yarn add @daffodilsw/react-native-codepush

## iOS Setup
- From your iOS folder, install CocoaPods:
cd ios
pod install

## 🚀 Usage
Import the component:
```shell
import CodePushUpdateAlert from '@daffodilsw/react-native-codepush';

otaConfig = {
   otaVersion: 1, 
    immediate: true/false, // set true if you want to reflect changes immediately.
    content: {
      "title": "Update Popup Title",  
      "description": "Update Popup Description"
    },
    BUNDLE_URL: CODEPUSH_URL,  // this should be public url of your bundle directory. https://your-bucket/ota/
    button: {
        download: "Download Update",
        downloading:'Downloading',
        installing: 'Installing',
        relaunching:'Relaunching',
    },
}

  if (otaConfig?.otaVersion && otaConfig?.otaVersion > 0) {
    return <CodePushUpdateAlert otaConfig={otaConfig} />;
  }
  ```


##  📘 How OTA Works

### If a newer OTA version exists:
  - Downloads bundle ZIP from CODEPUSH_URL
  - Extracts JS bundle + assets
  - Saves them in local storage
  - Restarts the app using native module
  - React Native loads the new bundle on next launch

## 📁 Generating OTA Bundles
- Follow these steps every time you want to release a new JS-only OTA update.

1️⃣ Generate Android Bundle:
```shell
npx react-native bundle \
  --platform android \
  --dev false \
  --entry-file index.js \
  --bundle-output ./codepush/android/ota/index.android.bundle \
  --assets-dest ./codepush/android/ota

  A folder will be created:
  - codepush/android/ota/
  - ├── index.android.bundle
  - └── drawable-xxxx/ 
  - └── raw/ 
  ```

  2️⃣ Generate iOS Bundle: 

```shell
npx react-native bundle \
  --platform ios \
  --dev false \
  --entry-file index.js \
  --bundle-output ./codepush/ios/ota/main.jsbundle \
  --assets-dest ./codepush/ios/ota

  A folder will be created:
  - codepush/ios/ota/
  - ├── main.jsbundle
  - └── assets/
```

  ## 3️⃣ Prepare ZIP/Bundle Files

Please zip only the OTA folder and ensure the bundle file follows the correct naming convention.
### Android:
- android-{versionName}-{otaVersion}.zip  -> android-1.0.1-1.zip

### iOS:
- ios-{versionName}-{otaVersion}.zip -> ios-1.0-1.zip

## Upload bundle to public bucket:
Upload the ZIP file to your public bucket at: // https://your-bucket/ota/filename.zip
Example:
- android:
    - https://your-bucket/ota/android-1.0.1-1.zip
- ios:
    - https://your-bucket/ota/ios-1.1-1.zip

## Serve new bundle on relaunch 
## Anroid:
### Update/add getJsbundleFile override method in MainApplication.kt
```shell
        // ✅ This is the key part
        override fun getJSBundleFile(): String? {
          val assetRoot = File(
              applicationContext.filesDir,
              "CodePush/unzipped/ota"
          )
            val updatedBundle = File(assetRoot, "index.android.bundle")
          return if (updatedBundle.exists()) {
            updatedBundle.absolutePath
          } else {
            super.getJSBundleFile()
          }
        }
```

## IOS:
### Update/add bundleURL override method in AppDelegate.swift

```shell
// replace bundleURL() with

  override func bundleURL() -> URL? {

    let docsDir = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true).first!
    let otaBundlePath = "\(docsDir)/CodePush/unzipped/ota/main.jsbundle"
    let otaExists = FileManager.default.fileExists(atPath: otaBundlePath)

    if otaExists {
      return URL(fileURLWithPath: otaBundlePath)
    }

    #if DEBUG
      return RCTBundleURLProvider.sharedSettings().jsBundleURL(forBundleRoot: "index")
    #else
      return Bundle.main.url(forResource: "main", withExtension: "jsbundle")
    #endif
  }

```