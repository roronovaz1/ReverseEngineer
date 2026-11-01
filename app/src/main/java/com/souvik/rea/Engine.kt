package com.souvik.rea

import android.content.pm.PackageManager
import com.android.tools.smali.baksmali.Baksmali
import com.android.tools.smali.baksmali.BaksmaliOptions
import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object Engine {
    fun run(pm: PackageManager, apk: File, out: File, jadx: Boolean, log: (String) -> Unit) {
        step("App info", log) { info(pm, apk, out) }
        step("Extract files", log) { unzip(apk, File(out, "raw")) }
        step("Smali code", log) { smali(apk, File(out, "smali")) }
        if (jadx) step("Java code (JADX)", log) { java(apk, File(out, "java")) }
    }

    private fun step(name: String, log: (String) -> Unit, f: () -> Unit) {
        log("> $name ...")
        try { f(); log("  ok") } catch (t: Throwable) { log("  FAILED: $t") }
    }

    @Suppress("DEPRECATION")
    private fun info(pm: PackageManager, apk: File, out: File) {
        val flags = PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or
            PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS or PackageManager.GET_PERMISSIONS
        val p = pm.getPackageArchiveInfo(apk.path, flags) ?: error("Cannot parse APK")
        val sb = StringBuilder()
        sb.appendLine("package: ${p.packageName}")
        sb.appendLine("version: ${p.versionName} (${p.versionCode})")
        p.applicationInfo?.let { sb.appendLine("minSdk: ${it.minSdkVersion}  targetSdk: ${it.targetSdkVersion}") }
        fun sec(t: String, l: List<String>?) { sb.appendLine("\n$t:"); l?.forEach { sb.appendLine("  $it") } }
        sec("permissions", p.requestedPermissions?.toList())
        sec("activities", p.activities?.map { it.name })
        sec("services", p.services?.map { it.name })
        sec("receivers", p.receivers?.map { it.name })
        sec("providers", p.providers?.map { it.name })
        out.mkdirs()
        File(out, "info.txt").writeText(sb.toString())
    }

    private fun unzip(apk: File, dest: File) {
        dest.mkdirs()
        val root = dest.canonicalPath + File.separator
        ZipFile(apk).use { z ->
            for (e in z.entries().toList()) {
                if (e.isDirectory) continue
                val f = File(dest, e.name)
                if (!f.canonicalPath.startsWith(root)) continue
                f.parentFile?.mkdirs()
                z.getInputStream(e).use { i -> f.outputStream().use { i.copyTo(it) } }
            }
        }
    }

    private fun smali(apk: File, dest: File) {
        val c = DexFileFactory.loadDexContainer(apk, Opcodes.getDefault())
        val jobs = Runtime.getRuntime().availableProcessors().coerceAtMost(4)
        for (n in c.dexEntryNames) {
            val d = c.getEntry(n)!!.dexFile
            val o = File(dest, n.removeSuffix(".dex")).apply { mkdirs() }
            Baksmali.disassembleDexFile(d, o, jobs, BaksmaliOptions())
        }
    }

    private fun java(apk: File, dest: File) {
        dest.mkdirs()
        val a = JadxArgs()
        a.setInputFiles(listOf(apk))
        a.setOutDir(dest)
        a.setSkipResources(false)
        a.setThreadsCount(2)
        a.setShowInconsistentCode(true)
        JadxDecompiler(a).use { it.load(); it.save() }
    }

    fun zipDir(dir: File, os: OutputStream) {
        ZipOutputStream(os.buffered()).use { z ->
            dir.walkTopDown().filter { it.isFile }.forEach { f ->
                z.putNextEntry(ZipEntry(dir.name + "/" + f.relativeTo(dir).path))
                f.inputStream().use { it.copyTo(z) }
                z.closeEntry()
            }
        }
    }
}
