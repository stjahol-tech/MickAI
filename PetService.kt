package com.mickai.pet

import android.app.*
import android.content.*
import android.graphics.*
import android.os.*
import android.provider.Settings
import android.view.*
import kotlin.math.abs

class PetService : Service() {
 private val channel = "mickai_pet"
 private var manager: WindowManager? = null
 private var pet: PetView? = null
 private var params: WindowManager.LayoutParams? = null
 private val handler = Handler(Looper.getMainLooper())
 private lateinit var animations: List<AnimationSpec>
 private var state = 0
 private var index = 0
 private val tick = object : Runnable { override fun run() { index = (index + 1) % animations[state].frames; pet?.invalidate(); handler.postDelayed(this, 125) } }
 override fun onBind(intent: Intent?): IBinder? = null
 override fun onCreate() { super.onCreate()
  val notificationManager = getSystemService(NotificationManager::class.java)
  notificationManager.createNotificationChannel(NotificationChannel(channel, "MickAI companion", NotificationManager.IMPORTANCE_LOW))
  val stop = Intent(this, PetService::class.java).apply { action = "STOP" }
  val stopPending = PendingIntent.getService(this, 1, stop, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val notification = Notification.Builder(this, channel).setContentTitle("MickAI is floating").setContentText("Tap MickAI to wave; drag to move").setSmallIcon(android.R.drawable.ic_menu_compass).addAction(Notification.Action.Builder(null,"Stop",stopPending).build()).build()
  startForeground(1, notification)
  if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
  animations = AnimationCatalog.fromJson(assets.open("animation-catalog.json").bufferedReader().use { it.readText() }).let { catalog ->
   listOf("idle","running-right","running-left","waving","jumping","failed","waiting","running","review").map { catalog.getValue(it) }
  }
  val bitmap = assets.open("spritesheet-extended-clean.png").use { BitmapFactory.decodeStream(it) }
  manager = getSystemService(WINDOW_SERVICE) as WindowManager
  params = WindowManager.LayoutParams(230, 250, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = 50; y = 250 }
  pet = PetView(this, bitmap)
  manager?.addView(pet, params)
  handler.post(tick)
 }
 override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { if (intent?.action == "STOP") { stopSelf(); return START_NOT_STICKY }; return START_STICKY }
 override fun onDestroy() { handler.removeCallbacks(tick); pet?.let { try { manager?.removeView(it) } catch (_: IllegalArgumentException) {} }; pet?.release(); pet = null; super.onDestroy() }
 private inner class PetView(context: Context, private val atlas: Bitmap) : View(context) {
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
  private var downX = 0f; private var downY = 0f; private var startX = 0; private var startY = 0; private var dragged = false
  override fun onDraw(canvas: Canvas) { super.onDraw(canvas)
   val src = Rect(index*192,animations[state].row*208,index*192+192,animations[state].row*208+208)
   canvas.drawBitmap(atlas,src,Rect(0,0,width,height),paint)
  }
  override fun onTouchEvent(e: android.view.MotionEvent): Boolean { val p = params ?: return false
   when(e.actionMasked) {
    MotionEvent.ACTION_DOWN -> { downX = e.rawX; downY = e.rawY; startX = p.x; startY = p.y; dragged = false; return true }
    MotionEvent.ACTION_MOVE -> { val dx=e.rawX-downX; val dy=e.rawY-downY; if(abs(dx)>8 || abs(dy)>8) dragged=true; if(dragged) { p.x=startX+dx.toInt();p.y=startY+dy.toInt();manager?.updateViewLayout(this,p);state=if(dx<0)2 else 1 }; return true }
    MotionEvent.ACTION_UP -> { state=if(dragged)0 else 3; index=0; if(!dragged) handler.postDelayed({ if(state==3) {state=0;index=0} },900); performClick(); return true }
   }; return true
  }
  override fun performClick(): Boolean { super.performClick(); return true }
  fun release() { atlas.recycle() }
 }
}
