require 'json'
package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = "react-native-codepush"
  s.version      = package["version"]
  s.summary      = "Custom OTA update module"
  s.author       = "Surender Kumar"
  s.license      = { :type => "MIT" }
  s.homepage     = "https://github.com/surender/react-native-codepush"
  s.source       = { :path => "." }
  s.requires_arc = true

  s.ios.deployment_target = "12.0"

  s.source_files = "ios/**/*.{h,m,mm,swift}"

  s.frameworks = "Foundation", "UIKit"

  s.dependency "React-Core"
  s.dependency "React-CoreModules"
  s.dependency 'SSZipArchive'
end
