package com.focusday.app

import android.Manifest
import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.time.LocalTime

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("focusday", MODE_PRIVATE) }
    private val ink = Color.rgb(67, 54, 91)
    private val lavender = Color.rgb(239, 231, 255)
    private val pink = Color.rgb(255, 235, 239)
    private val cream = Color.rgb(255, 250, 244)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askNotificationPermission()
        TaskReceiver.refreshNow(this)
        render()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setBackgroundColor(cream)
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val hero = ImageView(this).apply {
            setImageResource(R.drawable.welcome_cat)
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "A cute kitten in a pastel study nook"
        }
        content.addView(hero, LinearLayout.LayoutParams(-1, dp(190)).apply { bottomMargin = dp(12) })
        content.addView(label("FocusDay ♡", 29f, ink, true))
        content.addView(label("Tiny steps, lovely progress.", 15f, Color.rgb(139, 103, 151)).apply {
            setPadding(0, dp(3), 0, dp(14))
        })

        val now = LocalTime.now()
        val minute = now.hour * 60 + now.minute
        val current = TaskData.tasks.firstOrNull { minute >= it.startMinute() && minute < it.endMinute() }
        val next = TaskData.tasks.firstOrNull { minute < it.startMinute() }
        val elapsed = current?.let { ((minute - it.startMinute()).toFloat() / (it.endMinute() - it.startMinute()) * 100).toInt().coerceIn(0,100) } ?: 0

        val focusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(lavender)
        }
        focusCard.addView(label(if (current != null) "🐾 YOUR CURRENT MISSION" else "🐾 UP NEXT", 12f, Color.rgb(119, 83, 151), true))
        focusCard.addView(label(current?.title ?: next?.title ?: "Today's plan is complete", 21f, ink, true).apply { setPadding(0, dp(5), 0, dp(5)) })
        focusCard.addView(label(current?.let { "${displayTime(it.start)}–${displayTime(it.end)} · ${it.detail}" } ?: next?.let { "Starts at ${displayTime(it.start)} · ${it.detail}" } ?: "You showed up for yourself today. Well done!", 13f, ink))
        focusCard.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; progress = elapsed
        }, LinearLayout.LayoutParams(-1, dp(10)).apply { topMargin = dp(12) })
        content.addView(focusCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) })
        content.addView(label("Today's little plan", 20f, ink, true).apply { setPadding(0, 0, 0, dp(10)) })

        TaskData.tasks.forEachIndexed { index, task ->
            val done = prefs.getBoolean("done_$index", false)
            val isCurrent = current == task
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(10))
                setBackgroundColor(if (done) Color.rgb(231, 246, 235) else if (isCurrent) pink else Color.WHITE)
                elevation = dp(1).toFloat()
            }
            card.addView(label("${displayTime(task.start)}–${displayTime(task.end)}   ·   ${if (done) "✓ DONE" else if (isCurrent) "NOW ♡" else "ON THE PLAN"}", 11f, Color.rgb(139, 103, 151), true))
            card.addView(label(task.title, 17f, ink, true).apply { setPadding(0, dp(4), 0, dp(4)) })
            card.addView(label(task.detail, 13f, Color.rgb(90, 83, 101)))
            if (isCurrent) card.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100; progress = elapsed
            }, LinearLayout.LayoutParams(-1, dp(8)).apply { topMargin = dp(8) })
            val action = Button(this).apply {
                text = if (done) "Undo complete" else "Mark complete ✓"
                setOnClickListener {
                    prefs.edit().putBoolean("done_$index", !done).apply()
                    render()
                    TaskReceiver.refreshNow(this@MainActivity)
                }
            }
            card.addView(action)
            content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(Button(this).apply {
            text = "Set walking-cat wallpaper 🐈"
            setOnClickListener {
                try {
                    startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(this@MainActivity, CatWallpaperService::class.java))
                    })
                } catch (_: Exception) {
                    Toast.makeText(this@MainActivity, "Open Wallpaper settings and choose FocusDay Cat Wallpaper.", Toast.LENGTH_LONG).show()
                }
            }
        })
        root.addView(Button(this).apply {
            text = "Refresh task reminder"
            setOnClickListener {
                TaskReceiver.refreshNow(this@MainActivity)
                Toast.makeText(this@MainActivity, "Task reminder refreshed ♡", Toast.LENGTH_SHORT).show()
            }
        })
        setContentView(root)
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }
    }
}
