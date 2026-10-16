package com.focusday.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.LinearGradient
import android.graphics.Shader
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.os.Handler
import android.os.Looper
import kotlin.math.sin

class CatWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = CatEngine()

    inner class CatEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var visible = false
        private var frame = 0
        private var widthPx = 1080
        private var heightPx = 1920
        private var x = -120f
        private var direction = 1f

        private val drawFrame = object : Runnable {
            override fun run() {
                if (!visible) return
                val holder = surfaceHolder
                var canvas: Canvas? = null
                try {
                    canvas = holder.lockCanvas()
                    if (canvas != null) drawScene(canvas)
                } catch (_: Exception) {
                } finally {
                    if (canvas != null) holder.unlockCanvasAndPost(canvas)
                }
                frame++
                x += 3.2f * direction
                if (x > widthPx + 130) { direction = -1f; x = widthPx + 130f }
                if (x < -130) { direction = 1f; x = -130f }
                handler.removeCallbacks(this)
                handler.postDelayed(this, 40L)
            }
        }

        override fun onVisibilityChanged(visibleNow: Boolean) {
            visible = visibleNow
            if (visibleNow) drawFrame.run() else handler.removeCallbacks(drawFrame)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            widthPx = width
            heightPx = height
            super.onSurfaceChanged(holder, format, width, height)
            if (visible) drawFrame.run()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(drawFrame)
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            visible = false
            handler.removeCallbacks(drawFrame)
            super.onDestroy()
        }

        private fun drawScene(c: Canvas) {
            val w = c.width.toFloat()
            val h = c.height.toFloat()
            paint.shader = LinearGradient(0f, 0f, w, h,
                Color.rgb(255, 239, 226), Color.rgb(226, 235, 255), Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, w, h, paint)
            paint.shader = null

            // Floating tiny stars and hearts.
            for (i in 0..13) {
                val sx = ((i * 173 + frame * (i % 3 + 1)) % w.toInt().coerceAtLeast(1)).toFloat()
                val sy = (h * (0.18f + (i % 7) * 0.095f)) + sin((frame + i) * 0.06).toFloat() * 8f
                paint.color = if (i % 3 == 0) Color.rgb(255, 174, 190) else Color.rgb(255, 255, 255)
                paint.alpha = 210
                c.drawCircle(sx, sy, if (i % 2 == 0) 4f else 2.5f, paint)
            }
            paint.alpha = 255

            // Soft ground shadow.
            paint.color = Color.argb(35, 71, 70, 104)
            c.drawOval(x - 48, h * 0.78f + 47, x + 65, h * 0.78f + 63, paint)

            val baseY = h * 0.78f
            val bob = sin(frame * 0.25).toFloat() * 5f
            c.save()
            if (direction < 0) {
                c.scale(-1f, 1f, x, baseY)
            }
            drawCat(c, x, baseY + bob)
            c.restore()

            paint.color = Color.rgb(86, 81, 111)
            paint.textSize = (w * 0.043f).coerceIn(24f, 46f)
            paint.textAlign = Paint.Align.CENTER
            c.drawText("one little step at a time ♡", w / 2f, h * 0.88f, paint)
            paint.textSize = (w * 0.025f).coerceIn(16f, 26f)
            paint.color = Color.rgb(117, 112, 145)
            c.drawText("you've got this", w / 2f, h * 0.915f, paint)
        }

        private fun drawCat(c: Canvas, cx: Float, y: Float) {
            val s = (widthPx * 0.00062f).coerceIn(0.55f, 1.4f)
            fun oval(color: Int, l: Float, t: Float, r: Float, b: Float) {
                paint.color = color
                c.drawOval(cx + l*s, y + t*s, cx + r*s, y + b*s, paint)
            }
            val fur = Color.rgb(255, 250, 244)
            val outline = Color.rgb(114, 104, 126)
            // Tail swings a little.
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 10f*s
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = outline
            val tail = android.graphics.Path().apply {
                moveTo(cx + 39*s, y + 7*s)
                cubicTo(cx + 68*s, y - 5*s, cx + 66*s, y - 35*s, cx + 49*s, y - 31*s)
            }
            c.drawPath(tail, paint)
            paint.style = Paint.Style.FILL

            // Body and legs.
            oval(outline, -37f, -34f, 43f, 22f)
            oval(fur, -34f, -38f, 40f, 18f)
            val step = sin(frame * 0.35).toFloat() * 8f
            oval(outline, -27f + step, 7f, -11f + step, 37f)
            oval(fur, -25f + step, 5f, -13f + step, 34f)
            oval(outline, 15f - step, 7f, 31f - step, 37f)
            oval(fur, 17f - step, 5f, 29f - step, 34f)

            // Head, ears and inner ears.
            oval(outline, -36f, -83f, 36f, -18f)
            paint.color = outline
            val ears = android.graphics.Path().apply {
                moveTo(cx - 31*s, y - 59*s); lineTo(cx - 28*s, y - 91*s); lineTo(cx - 7*s, y - 72*s); close()
                moveTo(cx + 7*s, y - 72*s); lineTo(cx + 28*s, y - 91*s); lineTo(cx + 32*s, y - 58*s); close()
            }
            c.drawPath(ears, paint)
            paint.color = Color.rgb(255, 190, 205)
            val inner = android.graphics.Path().apply {
                moveTo(cx - 25*s, y - 67*s); lineTo(cx - 25*s, y - 82*s); lineTo(cx - 15*s, y - 73*s); close()
                moveTo(cx + 15*s, y - 73*s); lineTo(cx + 25*s, y - 82*s); lineTo(cx + 26*s, y - 66*s); close()
            }
            c.drawPath(inner, paint)
            oval(fur, -34f, -78f, 34f, -20f)

            // Eyes, blush, nose and whiskers.
            paint.color = Color.rgb(73, 67, 88)
            val blink = frame % 100 in 0..4
            if (blink) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f*s
                c.drawLine(cx - 17*s, y - 51*s, cx - 9*s, y - 51*s, paint)
                c.drawLine(cx + 9*s, y - 51*s, cx + 17*s, y - 51*s, paint)
                paint.style = Paint.Style.FILL
            } else {
                c.drawCircle(cx - 13*s, y - 52*s, 3.5f*s, paint)
                c.drawCircle(cx + 13*s, y - 52*s, 3.5f*s, paint)
            }
            oval(Color.rgb(255, 194, 205), -27f, -43f, -17f, -37f)
            oval(Color.rgb(255, 194, 205), 17f, -43f, 27f, -37f)
            paint.color = Color.rgb(226, 139, 164)
            val nose = android.graphics.Path().apply {
                moveTo(cx - 4*s, y - 43*s); lineTo(cx + 4*s, y - 43*s); lineTo(cx, y - 39*s); close()
            }
            c.drawPath(nose, paint)
            paint.color = outline
            paint.strokeWidth = 1.5f*s
            paint.style = Paint.Style.STROKE
            c.drawLine(cx - 22*s, y - 42*s, cx - 5*s, y - 41*s, paint)
            c.drawLine(cx + 5*s, y - 41*s, cx + 22*s, y - 42*s, paint)
            c.drawLine(cx - 22*s, y - 38*s, cx - 7*s, y - 39*s, paint)
            c.drawLine(cx + 7*s, y - 39*s, cx + 22*s, y - 38*s, paint)
            paint.style = Paint.Style.FILL
        }
    }
}
