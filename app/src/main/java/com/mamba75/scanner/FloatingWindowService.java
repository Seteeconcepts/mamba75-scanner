package com.mamba75.scanner;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

public class FloatingWindowService extends Service {

    private static final String CHANNEL_ID   = "mamba75_scanner_channel";
    private static final int    NOTIF_ID     = 42;

    private WindowManager               windowManager;
    private View                        floatingView;
    private WebView                     webView;
    private WindowManager.LayoutParams  params;

    private boolean isMinimized = false;

    // Touch tracking for drag
    private float touchStartRawX, touchStartRawY;
    private int   paramStartX,    paramStartY;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification());
        buildFloatingWindow();
    }

    // ── BUILD WINDOW ──────────────────────────────────────────────
    private void buildFloatingWindow() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Inflate layout
        LayoutInflater inflater = LayoutInflater.from(this);
        floatingView = inflater.inflate(R.layout.floating_window, null);

        // Screen dimensions
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int sw = dm.widthPixels;
        int sh = dm.heightPixels;

        // Window type — TYPE_APPLICATION_OVERLAY requires API 26
        int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        // Flags: allow touch inside window, still allow outside touches to pass through
        int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                  | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                  | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        params = new WindowManager.LayoutParams(
                sw,
                (int) (sh * 0.72f),
                overlayType,
                flags,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = (int) (sh * 0.10f);   // start 10% from top

        windowManager.addView(floatingView, params);

        // Setup sub-components
        setupWebView();
        setupDragHandle();
        setupButtons();
    }

    // ── WEBVIEW ───────────────────────────────────────────────────
    private void setupWebView() {
        webView = floatingView.findViewById(R.id.floating_webview);

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccessFromFileURLs(true);
        ws.setAllowUniversalAccessFromFileURLs(true);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        ws.setCacheMode(WebSettings.LOAD_DEFAULT);
        ws.setUserAgentString(ws.getUserAgentString() + " Mamba75Scanner/1.0");

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.setBackgroundColor(Color.parseColor("#050c14"));

        webView.loadUrl("file:///android_asset/index.html");
    }

    // ── DRAG HANDLE ───────────────────────────────────────────────
    private void setupDragHandle() {
        View dragHandle = floatingView.findViewById(R.id.drag_handle);

        dragHandle.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStartRawX = event.getRawX();
                    touchStartRawY = event.getRawY();
                    paramStartX    = params.x;
                    paramStartY    = params.y;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    params.x = (int) (paramStartX + (event.getRawX() - touchStartRawX));
                    params.y = (int) (paramStartY + (event.getRawY() - touchStartRawY));
                    windowManager.updateViewLayout(floatingView, params);
                    return true;

                case MotionEvent.ACTION_UP:
                    return true;
            }
            return false;
        });
    }

    // ── HEADER BUTTONS ────────────────────────────────────────────
    private void setupButtons() {
        ImageButton btnClose    = floatingView.findViewById(R.id.btn_float_close);
        ImageButton btnMinimize = floatingView.findViewById(R.id.btn_float_minimize);
        ImageButton btnOpenApp  = floatingView.findViewById(R.id.btn_float_openapp);

        btnClose.setOnClickListener(v -> stopSelf());

        btnMinimize.setOnClickListener(v -> toggleMinimize());

        btnOpenApp.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }

    // ── MINIMIZE / EXPAND ─────────────────────────────────────────
    private void toggleMinimize() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        FrameLayout webContainer = floatingView.findViewById(R.id.webview_container);
        ImageButton btnMin = floatingView.findViewById(R.id.btn_float_minimize);
        TextView titleView = floatingView.findViewById(R.id.float_title);

        isMinimized = !isMinimized;

        if (isMinimized) {
            webContainer.setVisibility(View.GONE);
            params.width  = (int) (dm.widthPixels * 0.55f);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            btnMin.setImageResource(android.R.drawable.arrow_up_float);
        } else {
            webContainer.setVisibility(View.VISIBLE);
            params.width  = dm.widthPixels;
            params.height = (int) (dm.heightPixels * 0.72f);
            btnMin.setImageResource(android.R.drawable.arrow_down_float);
        }
        windowManager.updateViewLayout(floatingView, params);
    }

    // ── CLEANUP ───────────────────────────────────────────────────
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }
    }

    // ── NOTIFICATION ──────────────────────────────────────────────
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Mamba75 Scanner",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps the Deriv digit scanner active in the background");
            channel.setSound(null, null);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent tapIntent = new Intent(this, MainActivity.class);
        tapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        int piFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pi = PendingIntent.getActivity(this, 0, tapIntent, piFlags);

        // Stop action in notification
        Intent stopIntent = new Intent(this, FloatingWindowService.class);
        stopIntent.setAction("STOP");
        PendingIntent stopPi = PendingIntent.getService(this, 1, stopIntent, piFlags);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Mamba75 Scanner — LIVE")
                    .setContentText("Scanning 10 Deriv volatility indices via WebSocket")
                    .setSmallIcon(android.R.drawable.ic_menu_compass)
                    .setContentIntent(pi)
                    .addAction(android.R.drawable.ic_delete, "Stop", stopPi)
                    .setOngoing(true)
                    .setColor(Color.parseColor("#38bdf8"))
                    .build();
        } else {
            //noinspection deprecation
            return new Notification.Builder(this)
                    .setContentTitle("Mamba75 Scanner — LIVE")
                    .setContentText("Scanning 10 Deriv volatility indices via WebSocket")
                    .setSmallIcon(android.R.drawable.ic_menu_compass)
                    .setContentIntent(pi)
                    .setOngoing(true)
                    .build();
        }
    }

    // Handle the STOP action from the notification
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopSelf();
        }
        return START_STICKY;
    }
}
