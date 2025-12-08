import RNFS from 'react-native-fs';
import { Image, Modal, Platform, StyleSheet, Text, View, NativeModules, TouchableOpacity } from 'react-native';
const { AppReloader } = NativeModules;
import DeviceInfo from 'react-native-device-info';
import { unzip } from 'react-native-zip-archive';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useEffect, useState } from 'react';


const CodePushUpdateAlert = (props) => {
  const {otaConfig} = props || {};
  const otaVersion = otaConfig?.otaVersion
  const [isUpdateAvailable, setUpdateAvailable] = useState();
  const [filePath, setFilePath] = useState({ filename: '', downloadUrl: '' });
  const [buttonState, setButtonState] = useState(1);

  const fileExistsAtUrl = async url => {
    try {
      const res = await fetch(url, { method: 'HEAD' });
      return res.ok;
    } catch (err) {
      return false;
    }
  };

  const checkIsLocalBundleSame = async filename => {
    const isBundleExist = await AsyncStorage.getItem(`otaVersion${Platform.OS}`);
    if (isBundleExist === filename) {
      return true;
    }
    return false;
  };

  const checkUpdate = async () => {
    // --- Version Info ---
    const versionCode = await DeviceInfo.getVersion();
    const build = await DeviceInfo.getBuildNumber();
    const version = `${versionCode}-${build}`;
    console.log('📱 Current App Version', version);

    const filename =
      Platform.OS === 'android'
        ? `android-${versionCode}-${otaVersion}.zip`
        : `ios-${versionCode}-${otaVersion}.zip`;
    const downloadUrl = `${otaConfig.BUNDLE_URL}${filename}`;
    const fileExist = await fileExistsAtUrl(downloadUrl);
    console.log("fileExist>>>>>>>", fileExist)
    if (fileExist) {
      const isLocalBundleSame = await checkIsLocalBundleSame(filename);
       console.log("isLocalBundleSame>>>>>>>>>>", isLocalBundleSame)
      if (!isLocalBundleSame) {
        // show download popup
        setFilePath({ filename, downloadUrl });
        setUpdateAvailable(true);
      }
    }
  };

  useEffect(() => {
    checkUpdate();

    return () => {
      setFilePath({});
      setUpdateAvailable(null);
    };
  }, []);

  const downloadUpdate = async () => {
    try {
       const versionCode = await DeviceInfo.getVersion();
      const DEPLOY_DIR = `${RNFS.DocumentDirectoryPath}/CodePush/${versionCode}`;
      console.log("deploy_dir>>>>>>>", DEPLOY_DIR)
      const ZIP_PATH = `${DEPLOY_DIR}/update.zip`;
      const UNZIP_DIR = `${DEPLOY_DIR}/unzipped`;
      const { filename = '', downloadUrl = '' } = filePath || {};

      setButtonState(2); // downloading....
      const dirExists = await RNFS.exists(DEPLOY_DIR);

      if (!dirExists) {
        await RNFS.mkdir(DEPLOY_DIR);
      }

      console.log('⬇️ Downloading update');

      const downloadRes = await RNFS.downloadFile({
        fromUrl: downloadUrl,
        toFile: ZIP_PATH,
      }).promise;
      console.log('✅ ZIP file downloaded');

      if (downloadRes.statusCode !== 200) {
        console.warn('⚠️ Failed to download update', downloadRes);
        setUpdateAvailable(null);
        return null;
      }

      setButtonState(3); // installing...

      const zipExists = await RNFS.exists(ZIP_PATH);
      console.log(zipExists ? "✅ ZIP file downloaded" : '❌ Missing ZIP file');

      // --- Clean previous unzipped folder ---
      if (await RNFS.exists(UNZIP_DIR)) {
        await RNFS.unlink(UNZIP_DIR);
      }

      await RNFS.mkdir(UNZIP_DIR);

      const unzipRes = await unzip(ZIP_PATH, UNZIP_DIR);

      await AsyncStorage.setItem(`otaVersion${Platform.OS}`, filename);
      await RNFS.unlink(ZIP_PATH);
      if(otaConfig?.immediate){
        setButtonState(4);
        setTimeout(() => {
          setUpdateAvailable(null);
          setTimeout(()=>{
            AppReloader.restartApp();
          },300)
        }, 1000);
      }
      else{
        setUpdateAvailable(null);
      }
    } catch (error) {
      console.error("codepush:", error);
      setUpdateAvailable(null);
      return error;
    }
  };

  const onButtonClick = () => {
    if (buttonState === 1) {
      downloadUpdate();
    }
    if (buttonState === 4) {
      // restart app
    }
  };

  const getButtonText = () => {
    switch (buttonState) {
      case 1:
        return otaConfig?.button?.download || "Download Update";
      case 2:
        return  otaConfig?.button?.downloading || "Downloading Update";
      case 3:
        return  otaConfig?.button?.installing || "Installing Update";
      default:
        return otaConfig?.button?.relaunching || "Relaunching";
    }
  };

  if (!isUpdateAvailable) return null;

  const modalStyle = typeof otaConfig?.style?.modal === 'object' ? otaConfig?.style?.modal : {}
  const modalHeading = typeof otaConfig?.style?.title === 'object' ? otaConfig?.style?.title : {}
  const modalDescription = typeof otaConfig?.style?.description === 'object' ? otaConfig?.style?.description : {}
  const buttonStyle = typeof otaConfig?.style?.button === 'object' ? otaConfig?.style?.button : {}
  const buttonTextStyle = typeof otaConfig?.style?.buttonText === 'object' ? otaConfig?.style?.buttonText : {}
  return (
    <Modal
      visible={isUpdateAvailable}
      transparent
      style={styles.popupContainer}
      statusBarTranslucent
      navigationBarTranslucent
      animationType="slide"
    >
        <View style={styles.container}>
          <View style={{ backgroundColor: '#090B0A' }}>
            <View style={[styles.card, modalStyle]}>
             {otaConfig?.logo && <Image source={typeof otaConfig.logo === 'number' ? otaConfig.logo : {uri:otaConfig.logo}} style={styles.icon} />}
              <View>
                <Text style={[styles.heading, modalHeading]}>{otaConfig?.content?.title ||  "Update Availalbe!"}</Text>
                <Text style={[styles.description, modalDescription]}>{otaConfig?.content?.description || "We have added new features and updates to make your experience seamless."}</Text>
              </View>
              <TouchableOpacity 
                  onPress={onButtonClick}
                  style={[{
                    padding: 10,
                    backgroundColor:'#B4FA39',
                    borderRadius: 10
                  }, buttonStyle]}>
                    <Text style={[{fontSize: 15, fontWeight: '600',  color: 'black'}, buttonTextStyle]}>{getButtonText()}</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
    </Modal>
  );
};
export default CodePushUpdateAlert;
const styles = StyleSheet.create({
  popupContainer: {
    flex: 1,
  },
  container: {
    padding: 16,
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    elevation: 3,
  },
  outlineBox: {
    flex: 1,
    elevation: 3,
    ...StyleSheet.absoluteFillObject,
    justifyContent: 'flex-end',
    backgroundColor: 'rgb(11, 11, 11, 0.60)',
  },
  card: {
    // flex: 1,
    alignItems: 'center',
    gap: 16,
    borderRadius: 20,
    backgroundColor: '#191B1A',
    paddingHorizontal: 16,
    paddingVertical: 20,
  },
  icon: {
    height: 60,
    width: 60,
  },
  heading: {
    color: '#EBEBEC',
    fontSize: 18,
    fontStyle: 'normal',
    fontWeight: '500',
    textAlign: 'center',
    lineHeight: 20,
    letterSpacing: 0.36,
    marginBottom: 10
  },
  description: {
    color: '#929594',
    fontSize: 18,
    fontStyle: 'normal',
    fontWeight: '400',
    textAlign: 'center',
    lineHeight: 18,
    letterSpacing: 0.36,
  },
});
