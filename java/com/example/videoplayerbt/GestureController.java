package com.example.videoplayerbt;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class GestureController {
    private static final String TAG = "GestureController";

    public static void performAction(String action, Context context) {
        if (!(context instanceof AccessibilityService)) {
            return;
        }

        AccessibilityService service = (AccessibilityService) context;
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        int screenWidth = size.x;
        int screenHeight = size.y;

        switch (action) {
            case "Double Tap":
                Log.d(TAG, "Performing double tap gesture");
                performDoubleTap(service, screenWidth / 2, screenHeight / 2);
                break;
            case "Long Press":
                // 长按时向下滑动
                Log.d(TAG, "Performing swipe down gesture (Long Press)");
                performSwipeDown(service, screenWidth / 2, screenHeight);
                break;
            case "Short Press":
                // 短按时向上滑动
                Log.d(TAG, "Performing swipe up gesture (Short Press)");
                performSwipeUp(service, screenWidth / 2, screenHeight);
                break;
        }
    }

    private static void performSwipeDown(AccessibilityService service, int x, int y) {
        Path path = new Path();
        float startY = y * 0.3f;
        float endY = y * 0.7f;

        path.moveTo(x, startY);
        path.lineTo(x, endY);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 200));

        service.dispatchGesture(builder.build(), new AccessibilityService.GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d(TAG, "Swipe down gesture completed");
            }
        }, null);
    }

    private static void performSwipeUp(AccessibilityService service, int x, int y) {
        Path path = new Path();
        float startY = y * 0.7f;
        float endY = y * 0.3f;

        path.moveTo(x, startY);
        path.lineTo(x, endY);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 200));

        service.dispatchGesture(builder.build(), new AccessibilityService.GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d(TAG, "Swipe up gesture completed");
            }
        }, null);
    }

    private static void performDoubleTap(AccessibilityService service, int x, int y) {
        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 50));

        service.dispatchGesture(builder.build(), new AccessibilityService.GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d(TAG, "First tap completed, performing second tap");
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Path secondTapPath = new Path();
                    secondTapPath.moveTo(x, y);

                    GestureDescription.Builder secondBuilder = new GestureDescription.Builder();
                    secondBuilder.addStroke(new GestureDescription.StrokeDescription(secondTapPath, 0, 50));

                    service.dispatchGesture(secondBuilder.build(), new AccessibilityService.GestureResultCallback() {
                        @Override
                        public void onCompleted(GestureDescription gestureDescription) {
                            super.onCompleted(gestureDescription);
                            Log.d(TAG, "Double tap completed");
                        }
                    }, null);
                }, 100);
            }
        }, null);
    }
}