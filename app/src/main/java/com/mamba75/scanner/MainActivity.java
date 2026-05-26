package com.mamba75.scanner;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int OVERLAY_PERMISSION_REQUEST = 1001;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1002;

    private Button btnLaunch, btnStop;
    private TextView tvStatus, tvInstructions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnLaunch       = findViewById(R.id.btn_launch);
        btnStop         = findViewById(R.id.btn_stop);
        tvStatus        = findViewById(R.id.tv_status);
        tvInstructions  = findViewById(R.id.tv_instructions);

        btnLaunch.setOnClickListener(v -> onLaunchClicked());
        btnStop.setOnClickListener(v -> onStopClicked());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void onLaunchClicked() {
        // Step 1: check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            tvStatus.setText("⚠ Grant \"Display over other apps\" permission first");
            tvStatus.setTextColor(0xFFfbbf24);
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST);
            return;
        }

        // Step 2: request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                new String[]{"android.permission.POST_NOTIFICATIONS"},
                NOTIFICATION_PERMISSION_REQUEST
            );
        }

        startFloatingScanner();
    }

    private void startFloatingScanner() {
        Intent serviceIntent = new Intent(this, FloatingWindowService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        updateUI();
        // Move app to background so user can see the floating scanner
        moveTaskToBack(true);
    }

    private void onStopClicked() {
        stopService(new Intent(this, FloatingWindowService.class));
        updateUI();
    }

    private void updateUI() {
        boolean running = isServiceRunning(FloatingWindowService.class);

        if (running) {
            tvStatus.setText("🟢  SCANNER RUNNING");
            tvStatus.setTextColor(0xFF22c55e);
            btnLaunch.setText("▶  SHOW FLOATING SCANNER");
            btnStop.setVisibility(View.VISIBLE);
            tvInstructions.setText(
                "The scanner is active and floating on top of all apps.\n\n" +
                "• Minimize this app — the floating scanner stays visible\n" +
                "• Drag the scanner by its top bar to reposition\n" +
                "• Tap  —  to collapse it to a compact bar\n" +
                "• Tap  ✕  in the floating window to stop scanning\n\n" +
                "The WebSocket connection to Deriv runs continuously in the background."
            );
        } else {
            tvStatus.setText("⚫  SCANNER STOPPED");
            tvStatus.setTextColor(0xFF475569);
            btnLaunch.setText("▶  LAUNCH FLOATING SCANNER");
            btnStop.setVisibility(View.GONE);
            tvInstructions.setText(
                "Tap LAUNCH to start the scanner.\n\n" +
                "The scanner will appear as a floating overlay. " +
                "You can then leave this app — the scanner stays on top of whatever you're doing, " +
                "continuously monitoring all 10 Deriv volatility indices via WebSocket.\n\n" +
                "A persistent notification keeps the scanner alive in the background."
            );
        }
    }

    @SuppressWarnings({"deprecation", "unchecked"})
    private boolean isServiceRunning(Class serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startFloatingScanner();
            } else {
                tvStatus.setText("✗  Permission denied — cannot show overlay");
                tvStatus.setTextColor(0xFFef4444);
            }
        }
    }
}
