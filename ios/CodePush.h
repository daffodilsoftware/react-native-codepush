#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>

@interface CodePush : NSObject <RCTBridgeModule>

+ (nullable NSString *)getBundlePathIfExistsSync;

@end