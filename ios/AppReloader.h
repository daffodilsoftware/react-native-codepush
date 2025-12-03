#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>

@interface AppReloader : NSObject <RCTBridgeModule>

+ (nullable NSString *)getBundlePathIfExistsSync;

@end