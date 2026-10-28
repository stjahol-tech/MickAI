package com.mickai.pet

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs

class PetService : Service() {
    private val channelId = "mickai_pet"
    private var manager: WindowManager? = null
    private var pet: PetView? = null
    private var params: WindowManager.LayoutParams? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var animations: List<AnimationSpec>
    private var state = 0
    private var index = 0

    private val tick = object : Runnable {
        override fun run() {
            if (!::animations.isInitialized) return
            index = (index + 1) % animations[state].frames
            pet?.invalidate()
            handler.postDelayed(this, 125)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(channelId, "MickAI companion", NotificationManager.IMPORTANCE_LOW)
        )

        val stop = Intent(this, PetService::class.java).apply { action = "STOP" }
        val stopPending = PendingIntent.getService(
            this,
            1,
            stop,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("MickAI is floating")
            .setContentText("Tap MickAI to wave; drag to move")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
            .build()

        startForeground(1, notification)

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        val catalogText = try {
            assets.open("animation-catalog.json").bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            "{\"animations\":[{\"name\":\"idle\",\"row\":0,\"frames\":6},{\"name\":\"waving\",\"row\":3,\"frames\":4},{\"name\":\"waiting\",\"row\":6,\"frames\":6}] }"
        }

        animations = AnimationCatalog.fromJson(catalogText).let { catalog ->
            listOf("idle", "running-right", "running-left", "waving", "jumping", "failed", "waiting", "running", "review")
                .map { name -> catalog[name] ?: AnimationSpec(0, 1) }
        }

        val bitmap = try {
            assets.open("spritesheet-extended-clean.png").use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            Bitmap.createBitmap(230, 250, Bitmap.Config.ARGB_8888)
        }

        manager = getSystemService(WindowManager::class.java)
        params = WindowManager.LayoutParams(
            230,
            250,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
            x = 100
            y = 300
        }

        pet = PetView(this, bitmap)
        manager?.addView(pet, params)
        handler.post(tick)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        pet?.let {
            try {
                manager?.removeView(it)
            } catch (_: IllegalArgumentException) {
            }
        }
        pet?.release()
        pet = null
        super.onDestroy()
    }

    private inner class PetView(context: Context, private val atlas: Bitmap) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0
        private var dragged = false

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val row = animations[state].row
            val frames = animations[state].frames
            val source = Rect((index % frames) * 192, row * 208, (index % frames) * 192 + 192, row * 208 + 208)
            canvas.drawBitmap(atlas, source, Rect(0, 0, width, height), paint)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val p = params ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = p.x
                    startY = p.y
                    dragged = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (abs(dx) > 8 || abs(dy) > 8) {
                        dragged = true
                    }
                    if (dragged) {
                        p.x = startX + dx.toInt()
                        p.y = startY + dy.toInt()
                        manager?.updateViewLayout(this@PetView, p)
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    state = if (dragged) 0 else 3
                    index = 0
                    if (!dragged) {
                        handler.postDelayed({
                            if (state == 3) {
                                state = 0
                                index = 0
                            }
                        }, 900)
                    }
                    performClick()
                    return true
                }
            }
            return true
        }

        override fun performClick(): Boolean {
            super.performClick()
            return true
        }

        fun release() {
            if (!atlas.isRecycled) atlas.recycle()
        }
    }
}
