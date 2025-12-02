declare module "@daffodilsw/react-native-codepush" {
  import * as React from "react";

  export interface CodePushUpdateAlertProps {
    otaConfig?: any;
  }

  const CodePushUpdateAlert: React.FC<CodePushUpdateAlertProps>;

  export { CodePushUpdateAlert };

  export default CodePushUpdateAlert;
}
