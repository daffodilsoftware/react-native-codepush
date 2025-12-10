#import "CodePush.h"
#import <SSZipArchive/SSZipArchive.h>
#import <React/RCTLog.h>
#import <React/RCTReloadCommand.h>

@implementation CodePush

RCT_EXPORT_MODULE(CodePush);

RCT_EXPORT_METHOD(restartApp)
{
  RCTLogInfo(@"[CodePush] restartApp called");
  RCTTriggerReloadCommandListeners(@"user_initiated_restart");
}

RCT_EXPORT_METHOD(getVersion:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  @try {
    NSString *version = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleShortVersionString"];
    resolve(version);
  }
  @catch (NSException *error) {
    reject(@"ERR_VERSION", @"Failed to get version", nil);
  }
}

RCT_EXPORT_METHOD(getBuildNumber:(RCTPromiseResolveBlock)resolve
                        rejecter:(RCTPromiseRejectBlock)reject)
{
  @try {
    NSString *buildNumber = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleVersion"];
    resolve(buildNumber);
  }
  @catch (NSException *error) {
    reject(@"ERR_BUILD", @"Failed to get build number", nil);
  }
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

RCT_EXPORT_METHOD(unzip:(NSString *)zipPath
                  dest:(NSString *)destPath
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  BOOL result = [SSZipArchive unzipFileAtPath:zipPath toDestination:destPath];
  if (result) {
      RCTLogInfo(@"[CodePush] unzip success");
    resolve(@(YES));
  } else {
      RCTLogInfo(@"[CodePush] unzip error");
    reject(@"ERR_UNZIP", @"Failed to unzip file", nil);
  }
}

+ (NSString *)getDocumentsPath {
  return [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) firstObject];
}


RCT_REMAP_METHOD(getDocumentsPath,
                 getDocumentsPathWithResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
  @try {
    NSString *path = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) firstObject];
    resolve(path);
  }
  @catch (NSException *exception) {
    reject(@"E_PATH_ERROR", @"Failed to get documents path", nil);
  }
}

RCT_EXPORT_METHOD(exists:(NSString *)path
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  BOOL exists = [[NSFileManager defaultManager] fileExistsAtPath:path];
  resolve(@(exists));
}

RCT_EXPORT_METHOD(mkdir:(NSString *)path
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  NSError *error;
  BOOL ok = [[NSFileManager defaultManager] createDirectoryAtPath:path
                                      withIntermediateDirectories:YES
                                                       attributes:nil
                                                            error:&error];
  if (ok) resolve(@YES);
  else reject(@"ERR_MKDIR", @"Failed to create dir", error);
}

RCT_EXPORT_METHOD(unlink:(NSString *)path
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  NSError *error;
  BOOL ok = [[NSFileManager defaultManager] removeItemAtPath:path error:&error];
  if (ok) resolve(@YES);
  else reject(@"ERR_UNLINK", @"Failed to delete", error);
}

RCT_EXPORT_METHOD(downloadFile:(NSString *)url
                  toFile:(NSString *)destPath
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  NSURL *URL = [NSURL URLWithString:url];
  NSURLRequest *request = [NSURLRequest requestWithURL:URL];

  NSURLSessionDownloadTask *task =
    [[NSURLSession sharedSession] downloadTaskWithRequest:request
    completionHandler:^(NSURL *location, NSURLResponse *response, NSError *error) {

      if (error) {
        reject(@"ERR_DOWNLOAD", @"Download failed", error);
        return;
      }

      NSError *moveError;
      [[NSFileManager defaultManager] moveItemAtURL:location
                                              toURL:[NSURL fileURLWithPath:destPath]
                                              error:&moveError];

      if (moveError) reject(@"ERR_UPDATE", @"Failed to download update", moveError);
      else resolve(@{
                @"statusCode": @200,
                @"path": destPath
              });
    }];

  [task resume];
}

RCT_EXPORT_METHOD(setItem:(NSString *)key value:(NSString *)value)
{
  [[NSUserDefaults standardUserDefaults] setObject:value forKey:key];
  [[NSUserDefaults standardUserDefaults] synchronize];
}

RCT_EXPORT_METHOD(getItem:(NSString *)key
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  @try {
    NSString *value = [[NSUserDefaults standardUserDefaults] stringForKey:key];
    resolve(value ?: @"");
  }
  @catch (NSException *exception) {
    reject(@"ERR_READ", @"Failed to read otaVersion", nil);
  }
}

@end
