#import "AppReloader.h"
#import <React/RCTLog.h>
#import <React/RCTReloadCommand.h>

@implementation AppReloader

RCT_EXPORT_MODULE(AppReloader);

RCT_EXPORT_METHOD(restartApp)
{
  RCTLogInfo(@"[AppReloader] restartApp called");
  RCTTriggerReloadCommandListeners(@"user_initiated_restart");
}

// 🔥 Native sync method used by Swift on startup
+ (nullable NSString *)getBundlePathIfExistsSync
{
  NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
  NSString *docs = paths.firstObject;
  if (!docs) return nil;

  NSString *version = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleShortVersionString"];
  NSString *relative = [NSString stringWithFormat:@"CodePush/%@/unzipped/ota/main.jsbundle", version];
  NSString *otaPath = [docs stringByAppendingPathComponent:relative];
  
  BOOL exists = [[NSFileManager defaultManager] fileExistsAtPath:otaPath];

  return exists ? otaPath : nil;
}

@end
