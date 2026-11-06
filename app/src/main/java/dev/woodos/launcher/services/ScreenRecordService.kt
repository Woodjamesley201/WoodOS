package dev.woodos.launcher.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen recorder built on MediaProjection (the only public API Android allows
 * for third-party screen recording). Saves MP4 files to
 * Movies/WoodOS on shared storage via MediaStore.
 */
class ScreenRecordService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val TAG = "WoodRecord"
        private const val CHANNEL_ID = "woodos_record"
        private const val NOTIF_ID = 41

        @Volatile var isRecording = false
            private set

        fun start(context: Context, resultCode: Int, data: Intent) {
            val i = Intent(context, ScreenRecordService::class.java)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_RESULT_DATA, data)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenRecordService::class.java))
        }
    }

    private var projection: MediaProjection? = null
    private var recorder: MediaRecorder? = null
    private var display: VirtualDisplay? = null
    private var outFile: File? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (isRecording) {
            stopRecording()
            stopSelf()
            return START_NOT_STICKY
        }
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        @Suppress("DEPRECATION")
        val data = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
        if (data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startAsForeground()

        try {
            val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = mpm.getMediaProjection(resultCode, data).also {
                it.registerCallback(object : MediaProjection.Callback() {
                    override fun onStop() = stopRecording()
                }, null)
            }
            val metrics = resources.displayMetrics
            val w = metrics.widthPixels
            val h = metrics.heightPixels
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "WoodOS").apply { mkdirs() }
            val name = "WoodOS_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".mp4"
            outFile = File(dir, name)

            @Suppress("DEPRECATION")
            val mr = MediaRecorder()
            mr.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mr.setVideoSize(w, h)
            mr.setVideoFrameRate(30)
            mr.setVideoEncodingBitRate(6_000_000)
            mr.setOutputFile(outFile!!.absolutePath)
            mr.prepare()
            display = projection?.createVirtualDisplay("WoodOS-Record", w, h, metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mr.surface, null, null)
            mr.start()
            recorder = mr
            isRecording = true
        } catch (e: Exception) {
            Log.e(TAG, "failed to start recording", e)
            stopRecording()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(dev.woodos.launcher.R.string.notification_channel_record),
                    NotificationManager.IMPORTANCE_LOW)
            )
        }
        val pi = PendingIntent.getActivity(this, 0,
            Intent(this, dev.woodos.launcher.MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notif: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("WoodOS")
            .setContentText(getString(dev.woodos.launcher.R.string.record_active))
            .setSmallIcon(dev.woodos.launcher.R.drawable.ic_launcher_foreground)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun stopRecording() {
        isRecording = false
        try { recorder?.stop() } catch (_: Exception) {}
        try { recorder?.release() } catch (_: Exception) {}
        try { display?.release() } catch (_: Exception) {}
        try { projection?.stop() } catch (_: Exception) {}
        recorder = null
        display = null
        projection = null
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }
}
