package com.cloudplay.app

import android.content.Context
import android.os.Build
import android.os.Environment
import android.app.ActivityManager
import android.content.pm.PackageManager
import java.io.File
import java.util.Locale

/**
 * Device preflight checks only. These checks do not install or initialize Wine/Box64.
 */
object RuntimeDiagnostics {
    data class Report(
        val abi: String,
        val freeStorageGb: String,
        val ramGb: String,
        val vulkan: Boolean,
        val gpuHint: String,
        val notes: List<String>
    )

    fun inspect(context: Context): Report {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "desconhecida"
        val usableBytes = context.filesDir.usableSpace
        val memory = ActivityManager.MemoryInfo().also {
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(it)
        }
        val totalRam = memory.totalMem
        val hasVulkan = context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL) ||
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
        val gpuHint = listOf(Build.HARDWARE, Build.BOARD, Build.SOC_MODEL, Build.MANUFACTURER, Build.MODEL)
            .filterNotNull().joinToString(" ").lowercase(Locale.ROOT)
        val notes = mutableListOf<String>()
        if (!abi.contains("arm64")) notes += "ABI ARM64 não detectada; Box64 normalmente precisa de arm64-v8a."
        if (usableBytes < 3L * 1024 * 1024 * 1024) notes += "Pouco espaço livre para preparar um rootfs grande (menos de 3 GB)."
        if (totalRam < 4L * 1024 * 1024 * 1024) notes += "RAM total abaixo de 4 GB; jogos 3D podem ter limitações."
        if (!hasVulkan) notes += "O Android não informou suporte Vulkan via recursos do sistema; validar OpenGL/Vulkan no dispositivo."
        if (gpuHint.contains("mali")) notes += "GPU Mali detectada: não presumir compatibilidade com drivers Turnip, voltados a Adreno."
        if (notes.isEmpty()) notes += "Pré-checagens básicas passaram; isso não confirma compatibilidade com jogos."
        return Report(
            abi = abi,
            freeStorageGb = String.format(Locale.US, "%.1f", usableBytes / (1024.0 * 1024 * 1024)),
            ramGb = String.format(Locale.US, "%.1f", totalRam / (1024.0 * 1024 * 1024)),
            vulkan = hasVulkan,
            gpuHint = gpuHint.ifBlank { "não identificada" },
            notes = notes
        )
    }
}
