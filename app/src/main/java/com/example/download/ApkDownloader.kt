package com.example.download

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class ApkDownloader(private val context: Context) {

    private val downloadManager: DownloadManager? by lazy {
        try {
            context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    // Map to link enqueued download IDs to their respective filenames for auto-installer triggers
    private val activeDownloadsMap = ConcurrentHashMap<Long, String>()

    // Cache to store the validation state (is valid check result) along with lastModified timestamp
    private val apkValidationCache = ConcurrentHashMap<String, Pair<Long, Boolean>>()

    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val action = intent.action
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId != -1L) {
                    val filename = activeDownloadsMap[downloadId]
                    if (filename != null) {
                        activeDownloadsMap.remove(downloadId)
                        // Trigger immediate package installation check & auto launch installer
                        ctx.mainExecutor.execute {
                            if (isApkDownloaded(filename)) {
                                installApk(filename)
                            } else {
                                Toast.makeText(ctx, "Download completed, but APK verification failed. Try redownloading.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    init {
        // Register download complete broadcast receiver
        try {
            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.applicationContext.registerReceiver(downloadCompleteReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.applicationContext.registerReceiver(downloadCompleteReceiver, filter)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /**
     * Enqueues a download request in the system's DownloadManager.
     * Saves files to the application sandbox's external directory which is secure and highly reliable on Android 11+.
     */
    fun downloadApk(url: String, filename: String, onDownloadEnqueued: (Long) -> Unit) {
        val manager = downloadManager
        if (manager == null) {
            Toast.makeText(context, "System Download service is unavailable.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            // Trim and sanitize URL
            val cleanUrl = url.trim()
            val uri = Uri.parse(cleanUrl)
            
            // Validate scheme
            if (uri.scheme != "http" && uri.scheme != "https") {
                Toast.makeText(context, "Invalid download URL scheme. Must be HTTP/HTTPS.", Toast.LENGTH_LONG).show()
                return
            }

            // If a previous failed / partial file exists, delete it first to prevent conflicts
            deleteDownloadedApk(filename)

            val request = DownloadManager.Request(uri).apply {
                setTitle(filename)
                setDescription("Downloading $filename from Ghost Store")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                
                // Store in our application's external files downloads directory
                // This permits easy and permissionless read/write for sharing via FileProvider
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, filename)
                
                setMimeType("application/vnd.android.package-archive")
            }

            val downloadId = manager.enqueue(request)
            activeDownloadsMap[downloadId] = filename
            onDownloadEnqueued(downloadId)
            Toast.makeText(context, "Download started: $filename", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to download Apk: ${e.localizedMessage ?: e.toString()}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Checks if the app has permission to install unknown packages.
     */
    fun canInstallApks(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Directs the user to the system settings screen to allow installing unknown apps.
     */
    fun requestInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(context, "Please enable \"Ghost Store\" to install APKs.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Triggers the Android platform package installer for the downloaded APK file.
     */
    fun installApk(filename: String) {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename)
        if (!file.exists()) {
            Toast.makeText(context, "Apk file not found locally. Please download it again.", Toast.LENGTH_SHORT).show()
            return
        }

        // Verify package install capability (Android O+)
        if (!canInstallApks()) {
            requestInstallPermission()
            return
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, file)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to launch package installer: ${e.localizedMessage ?: e.toString()}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Checks the actual status of a download in DownloadManager.
     * Returns:
     * - DownloadManager.STATUS_SUCCESSFUL: Completed successfully
     * - DownloadManager.STATUS_RUNNING / STATUS_PENDING / STATUS_PAUSED: Still downloading
     * - -1: No active/matching download record found
     */
    fun getDownloadStatus(filename: String): Int {
        val manager = downloadManager ?: return -1
        val query = DownloadManager.Query()
        val cursor = try {
            manager.query(query)
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        } ?: return -1

        try {
            val localUriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            
            while (cursor.moveToNext()) {
                val localUriStr = if (localUriIdx != -1) cursor.getString(localUriIdx) else null
                if (localUriStr != null) {
                    val localUri = Uri.parse(localUriStr)
                    val lastSegment = localUri.lastPathSegment
                    if (lastSegment == filename) {
                        if (statusIdx != -1) {
                            return cursor.getInt(statusIdx)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            cursor.close()
        }
        return -1
    }

    /**
     * Checks if the APK is currently being downloaded or queued by system's DownloadManager.
     */
    fun isApkDownloading(filename: String): Boolean {
        val status = getDownloadStatus(filename)
        return status == DownloadManager.STATUS_RUNNING || 
               status == DownloadManager.STATUS_PENDING || 
               status == DownloadManager.STATUS_PAUSED
    }

    /**
     * Checks if the file starts with the local zip file header magic bytes ("PK\u0003\u0004").
     * Validating this signature avoids passing non-ZIP/corrupt HTML error files to PackageManager
     * which triggers native error logs in the system console.
     */
    private fun isValidZipHeader(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        return try {
            file.inputStream().use { fis ->
                val bytes = ByteArray(4)
                if (fis.read(bytes) == 4) {
                    bytes[0] == 0x50.toByte() && // 'P'
                    bytes[1] == 0x4B.toByte() && // 'K'
                    bytes[2] == 0x03.toByte() &&
                    bytes[3] == 0x04.toByte()
                } else {
                    false
                }
            }
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Checks if a file is already downloaded and fully exists in local storage.
     * Also checks that the file is indeed a valid APK and a completed download.
     */
    fun isApkDownloaded(filename: String): Boolean {
        return try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return false
            val file = File(dir, filename)
            if (!file.exists() || file.length() <= 0) {
                apkValidationCache.remove(filename)
                return false
            }

            // Quick check: if already verified and modified timestamp did not change, return cached result immediately!
            val lastModified = file.lastModified()
            val cached = apkValidationCache[filename]
            if (cached != null && cached.first == lastModified) {
                return cached.second
            }

            // Check if there is an active/recent download record; if so, it must be STATUS_SUCCESSFUL
            val status = getDownloadStatus(filename)
            if (status != -1 && status != DownloadManager.STATUS_SUCCESSFUL) {
                return false
            }

            // Quick zip footer/header magic check before calling heavy package manager
            if (!isValidZipHeader(file)) {
                apkValidationCache[filename] = Pair(lastModified, false)
                if (status != DownloadManager.STATUS_RUNNING && 
                    status != DownloadManager.STATUS_PENDING && 
                    status != DownloadManager.STATUS_PAUSED) {
                    file.delete()
                }
                return false
            }

            // Verify with PackageManager's parsing utility to ensure the file is completely written and fully valid
            val info = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
            if (info != null) {
                apkValidationCache[filename] = Pair(lastModified, true)
                return true
            } else {
                apkValidationCache[filename] = Pair(lastModified, false)
                
                // If it is invalid (corrupt download/redirect), and not actively downloading,
                // delete it to prevent log spamming and release space
                if (status != DownloadManager.STATUS_RUNNING && 
                    status != DownloadManager.STATUS_PENDING && 
                    status != DownloadManager.STATUS_PAUSED) {
                    file.delete()
                }
                return false
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Deletes a local APK download.
     */
    fun deleteDownloadedApk(filename: String): Boolean {
        return try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return false
            val file = File(dir, filename)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }
}
