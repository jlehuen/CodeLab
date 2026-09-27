#include <ApplicationServices/ApplicationServices.h>
#include <CoreFoundation/CoreFoundation.h>
#include <jni.h>
#include "net_java_games_input_OSXMouse.h"

static CGPoint last_pos = {0.0, 0.0};
static int has_last_pos = 0;

JNIEXPORT void JNICALL Java_net_java_games_input_OSXMouse_nPollPointer(JNIEnv *env, jclass unused, jfloatArray data_array) {
    if (data_array == NULL) {
        return;
    }
    jsize len = (*env)->GetArrayLength(env, data_array);
    if (len < 5) {
        return;
    }

    float out[5];
    int32_t dx = 0, dy = 0;
    CGGetLastMouseDelta(&dx, &dy);

    CGEventRef event = CGEventCreate(NULL);
    if (event != NULL) {
        CGPoint current_pos = CGEventGetLocation(event);
        CFRelease(event);

        if (!has_last_pos) {
            last_pos = current_pos;
            has_last_pos = 1;
            out[0] = (float)dx;
            out[1] = (float)dy;
        } else {
            float diff_x = (float)(current_pos.x - last_pos.x);
            float diff_y = (float)(current_pos.y - last_pos.y);
            last_pos = current_pos;

            out[0] = (dx != 0) ? (float)dx : diff_x;
            out[1] = (dy != 0) ? (float)dy : diff_y;
        }
    } else {
        out[0] = (float)dx;
        out[1] = (float)dy;
    }

    bool left = CGEventSourceButtonState(kCGEventSourceStateCombinedSessionState, kCGMouseButtonLeft);
    bool right = CGEventSourceButtonState(kCGEventSourceStateCombinedSessionState, kCGMouseButtonRight);
    bool middle = CGEventSourceButtonState(kCGEventSourceStateCombinedSessionState, kCGMouseButtonCenter);

    out[2] = left ? 1.0f : 0.0f;
    out[3] = right ? 1.0f : 0.0f;
    out[4] = middle ? 1.0f : 0.0f;

    (*env)->SetFloatArrayRegion(env, data_array, 0, 5, out);
}
