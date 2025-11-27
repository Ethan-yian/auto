package com.example.videoplayerbt;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import android.util.Log;

public class VolumeService extends AccessibilityService {
    private static final String TAG = "VolumeService";
    private static final long DOUBLE_PRESS_INTERVAL = 300; // 双击间隔时间
    private static final long LONG_PRESS_THRESHOLD = 1000; // 长按阈值

    private long lastPressTime = 0;
    private int clickCount = 0;
    private Handler handler;
    private boolean isLongPressDetected = false;
    private boolean isShortPressHandled = false; // 新增：标记短按是否已处理
    private AudioManager audioManager;
    private Runnable longPressRunnable;
    private Runnable doubleClickResetRunnable;
    private Runnable shortPressRunnable; // 新增：短按检测

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // 长按检测
        longPressRunnable = () -> {
            isLongPressDetected = true;
            clickCount = 0;
            isShortPressHandled = false; // 长按时取消短按标记
            handler.removeCallbacks(doubleClickResetRunnable);
            handler.removeCallbacks(shortPressRunnable);
            performAction("Long Press");
        };

        // 双击重置
        doubleClickResetRunnable = () -> {
            clickCount = 0;
            isLongPressDetected = false;
        };

        // 短按检测（在按键抬起后执行）
        shortPressRunnable = () -> {
            if (!isLongPressDetected && !isShortPressHandled) {
                isShortPressHandled = true;
                performAction("Short Press");
            }
        };
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Service connected");
        Toast.makeText(this, "音量控制服务已启动", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
            // 阻止系统默认的音量调节
            audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

            switch (event.getAction()) {
                case KeyEvent.ACTION_DOWN:
                    handleVolumeKeyDown();
                    return true;
                case KeyEvent.ACTION_UP:
                    handleVolumeKeyUp();
                    return true;
            }
        }
        return super.onKeyEvent(event);
    }

    private void handleVolumeKeyDown() {
        long currentTime = SystemClock.elapsedRealtime();

        // 移除所有待执行的回调
        handler.removeCallbacks(longPressRunnable);
        handler.removeCallbacks(doubleClickResetRunnable);
        handler.removeCallbacks(shortPressRunnable);

        isLongPressDetected = false;
        isShortPressHandled = false;

        // 检查是否可能是双击
        if (currentTime - lastPressTime <= DOUBLE_PRESS_INTERVAL) {
            clickCount++;
            if (clickCount == 2) {
                // 确认是双击
                Log.d(TAG, "Double click detected");
                performAction("Double Tap");
                clickCount = 0;
                isShortPressHandled = true; // 双击时标记短按已处理
                return;
            }
        } else {
            clickCount = 1;
        }

        lastPressTime = currentTime;

        // 设置长按检测
        handler.postDelayed(longPressRunnable, LONG_PRESS_THRESHOLD);

        // 设置双击检测超时
        handler.postDelayed(doubleClickResetRunnable, DOUBLE_PRESS_INTERVAL);
    }

    private void handleVolumeKeyUp() {
        // 移除长按检测
        handler.removeCallbacks(longPressRunnable);

        if (!isLongPressDetected) {
            // 如果不是长按，则在双击间隔后检测短按
            handler.postDelayed(shortPressRunnable, DOUBLE_PRESS_INTERVAL);
        } else {
            // 如果是长按，重置状态
            clickCount = 0;
            isLongPressDetected = false;
            isShortPressHandled = true;
        }

        // 移除双击重置，因为我们要用新的定时器处理短按
        handler.removeCallbacks(doubleClickResetRunnable);
    }

    private void performAction(String action) {
        handler.post(() -> {
            Log.d(TAG, "Performing action: " + action);
            Toast.makeText(this, "执行动作: " + action, Toast.LENGTH_SHORT).show();
            GestureController.performAction(action, this);

            // 执行动作后重置状态
            if (!action.equals("Double Tap")) {
                clickCount = 0;
            }
            isLongPressDetected = false;
            isShortPressHandled = true;
        });
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 不需要处理
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Service interrupted");
        Toast.makeText(this, "音量控制服务已中断", Toast.LENGTH_SHORT).show();
    }
}