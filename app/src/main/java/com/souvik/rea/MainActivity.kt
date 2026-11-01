package com.souvik.rea

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var logView: TextView
    private lateinit var scroll: ScrollView
    private lateinit var jadxBox: CheckBox
    private lateinit var saveBtn: Button
    @Volatile private var lastOut: File? = null

    private val pickApk = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) start("picked_apk") {
            File(cacheDir, "input.apk").also { f ->
                contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { i.copyTo(it) } }
            }
        }
    }

    private val saveZip = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        val dir = lastOut
        if (uri != null && dir != null) Thread {
            try {
                contentResolver.openOutputStream(uri)?.use { Engine.zipDir(dir, it) }
                log("ZIP saved.")
            } catch (t: Throwable) { log("ZIP ERROR: $t") }
        }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fun btn(t: String, a: () -> Unit) = Button(this).apply { text = t; setOnClickListener { a() } }
        jadxBox = CheckBox(this).apply { text = "Java source (JADX, slower)"; isChecked = true; setTextColor(Color.WHITE) }
        saveBtn = btn("Save result as ZIP") { saveZip.launch("rea-output.zip") }.apply { isEnabled = false }
        logView = TextView(this).apply { setTextColor(Color.WHITE); typeface = Typeface.MONOSPACE; textSize = 12f }
        scroll = ScrollView(this).apply { addView(logView) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(32, 96, 32, 32)
            addView(btn("Pick installed app") { pickInstalled() })
            addView(btn("Pick APK file") {
                pickApk.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream"))
            })
            addView(jadxBox)
            addView(saveBtn)
            addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        setContentView(root)
    }

    private fun pickInstalled() {
        val pm = packageManager
        val apps = pm.getInstalledApplications(0).sortedBy { pm.getApplicationLabel(it).toString().lowercase() }
        val names = apps.map { "${pm.getApplicationLabel(it)}\n${it.packageName}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Pick app").setItems(names) { _, i ->
            val a = apps[i]
            start(a.packageName) { File(a.sourceDir) }
        }.show()
    }

    private fun start(name: String, prepare: () -> File) {
        saveBtn.isEnabled = false
        logView.text = ""
        val useJadx = jadxBox.isChecked
        Thread(null, {
            try {
                val apk = prepare()
                val out = File(getExternalFilesDir(null) ?: filesDir, "out/$name").apply { deleteRecursively(); mkdirs() }
                Engine.run(packageManager, apk, out, useJadx, ::log)
                lastOut = out
                runOnUiThread { saveBtn.isEnabled = true }
                log("DONE. Tap 'Save result as ZIP'.")
            } catch (t: Throwable) { log("ERROR: $t") }
        }, "rea-worker", 512L * 1024 * 1024).start()
    }

    private fun log(s: String) = runOnUiThread {
        logView.append(s + "\n")
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }
}
