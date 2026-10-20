#import <Cocoa/Cocoa.h>
#import <unistd.h>
#import <libgen.h>
#import <mach-o/dyld.h>

@interface LauncherDelegate : NSObject <NSApplicationDelegate>
@property (strong) NSMutableArray<NSString *> *files;
@property (assign) int origArgc;
@property (assign) const char **origArgv;
@end

@implementation LauncherDelegate

- (instancetype)initWithArgc:(int)argc argv:(const char **)argv {
    self = [super init];
    if (self) {
        _files = [NSMutableArray array];
        _origArgc = argc;
        _origArgv = argv;
    }
    return self;
}

- (void)application:(NSApplication *)sender openFiles:(NSArray<NSString *> *)filenames {
    if (filenames) [self.files addObjectsFromArray:filenames];
}

- (BOOL)application:(NSApplication *)sender openFile:(NSString *)filename {
    if (filename) [self.files addObject:filename];
    return YES;
}

- (void)application:(NSApplication *)application openURLs:(NSArray<NSURL *> *)urls {
    for (NSURL *url in urls) {
        if ([url isFileURL]) {
            [self.files addObject:[url path]];
        }
    }
}

- (void)applicationDidFinishLaunching:(NSNotification *)notification {
    char execPath[PATH_MAX];
    uint32_t size = sizeof(execPath);
    if (_NSGetExecutablePath(execPath, &size) != 0) {
        exit(1);
    }
    char *dir = dirname(execPath);
    char scriptPath[PATH_MAX];
    snprintf(scriptPath, sizeof(scriptPath), "%s/run.sh", dir);

    int numOrig = (self.origArgc > 1) ? (self.origArgc - 1) : 0;
    int numFiles = (int)[self.files count];
    int maxTotal = 1 + numOrig + numFiles + 1;

    char **newArgv = (char **)malloc(sizeof(char *) * maxTotal);
    newArgv[0] = scriptPath;
    int idx = 1;
    for (int i = 1; i < self.origArgc; i++) {
        if (strncmp(self.origArgv[i], "-psn_", 5) != 0) {
            newArgv[idx++] = strdup(self.origArgv[i]);
        }
    }
    for (NSString *file in self.files) {
        newArgv[idx++] = strdup([file UTF8String]);
    }
    newArgv[idx] = NULL;

    execv(scriptPath, newArgv);
    perror("execv failed");
    exit(1);
}

@end

int main(int argc, const char * argv[]) {
    @autoreleasepool {
        NSApplication *app = [NSApplication sharedApplication];
        LauncherDelegate *delegate = [[LauncherDelegate alloc] initWithArgc:argc argv:argv];
        [app setDelegate:delegate];
        [app run];
    }
    return 0;
}
