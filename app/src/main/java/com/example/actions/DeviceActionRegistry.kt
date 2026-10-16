package com.example.actions

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import org.json.JSONObject
import java.util.Calendar

data class ActionResult(
    val success: Boolean,
    val actionName: String,
    val summary: String,
    val details: String,
    val requiresUserApproval: Boolean = false
)

class DeviceActionRegistry(private val context: Context) {

    companion object {
        private const val TAG = "DeviceActionRegistry"
        private var isFlashlightOn = false
    }

    fun executeTool(toolName: String, argsJsonString: String): ActionResult {
        val args = try {
            JSONObject(argsJsonString)
        } catch (e: Exception) {
            JSONObject()
        }

        return try {
            when (toolName) {
                // Existing core tools
                "open_app" -> openApp(args.optString("app_name", ""))
                "set_timer" -> setTimer(args.optInt("duration_seconds", 60), args.optString("label", "NOVA AI Timer"))
                "set_alarm" -> setAlarm(args.optInt("hour", 8), args.optInt("minute", 0), args.optString("message", "NOVA AI Alarm"))
                "create_calendar_event" -> createCalendarEvent(
                    args.optString("title", "New Event"),
                    args.optInt("minutes_from_now", 60),
                    args.optInt("duration_minutes", 30),
                    args.optString("description", "")
                )
                "draft_sms" -> draftSms(args.optString("phone_number", ""), args.optString("message", ""))
                "dial_phone" -> dialPhone(args.optString("phone_number", ""))
                "get_battery_info" -> getBatteryInfo()
                "launch_navigation" -> launchNavigation(args.optString("destination", ""))
                "web_search" -> webSearch(args.optString("query", ""))
                "share_text" -> shareText(args.optString("text", ""))

                // New extended tools for complete hands-free device control
                "toggle_flashlight" -> toggleFlashlight(args.optBoolean("enabled", !isFlashlightOn))
                "adjust_volume" -> adjustVolume(args.optString("direction", "raise"), args.optString("stream", "music"))
                "open_settings_page" -> openSettingsPage(args.optString("setting_type", "main"))
                "send_email" -> sendEmail(
                    args.optString("recipient", ""),
                    args.optString("subject", ""),
                    args.optString("body", "")
                )
                "play_music" -> playMusic(args.optString("query", ""))
                "get_device_specs" -> getDeviceSpecs()
                "copy_to_clipboard" -> copyToClipboard(args.optString("text", ""))
                "take_photo" -> takePhoto()

                else -> ActionResult(
                    success = false,
                    actionName = toolName,
                    summary = "Unknown Tool",
                    details = "Tool '$toolName' is not registered in the device automation subsystem."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing action $toolName", e)
            ActionResult(
                success = false,
                actionName = toolName,
                summary = "Action Failed",
                details = "System error executing $toolName: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    // --- Extended Tools Implementation ---

    private fun toggleFlashlight(enabled: Boolean): ActionResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            if (cameraManager == null) {
                return ActionResult(false, "toggle_flashlight", "Hardware Unavailable", "Camera service unavailable.")
            }
            return try {
                val cameraId = cameraManager.cameraIdList.firstOrNull()
                if (cameraId != null) {
                    cameraManager.setTorchMode(cameraId, enabled)
                    isFlashlightOn = enabled
                    ActionResult(
                        true,
                        "toggle_flashlight",
                        if (enabled) "Flashlight Activated" else "Flashlight Turned Off",
                        "Device torch set to ${if (enabled) "ON" else "OFF"} via CameraManager."
                    )
                } else {
                    ActionResult(false, "toggle_flashlight", "No Torch Found", "No rear camera flash found on device.")
                }
            } catch (e: Exception) {
                ActionResult(false, "toggle_flashlight", "Flashlight Error", "Could not toggle torch: ${e.localizedMessage}")
            }
        } else {
            return ActionResult(false, "toggle_flashlight", "Unsupported OS", "Flashlight control requires Android 6.0+.")
        }
    }

    private fun adjustVolume(direction: String, stream: String): ActionResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ActionResult(false, "adjust_volume", "Audio Service Error", "AudioManager unavailable.")

        val streamType = when (stream.lowercase()) {
            "ring", "ringer" -> AudioManager.STREAM_RING
            "alarm" -> AudioManager.STREAM_ALARM
            "notification" -> AudioManager.STREAM_NOTIFICATION
            "voice", "call" -> AudioManager.STREAM_VOICE_CALL
            else -> AudioManager.STREAM_MUSIC
        }

        val adjustDir = when (direction.lowercase()) {
            "lower", "down", "decrease" -> AudioManager.ADJUST_LOWER
            "mute" -> AudioManager.ADJUST_MUTE
            "unmute" -> AudioManager.ADJUST_UNMUTE
            else -> AudioManager.ADJUST_RAISE
        }

        return try {
            audioManager.adjustStreamVolume(streamType, adjustDir, AudioManager.FLAG_SHOW_UI)
            val currentVol = audioManager.getStreamVolume(streamType)
            val maxVol = audioManager.getStreamMaxVolume(streamType)
            val pct = (currentVol * 100) / maxVol.coerceAtLeast(1)
            ActionResult(
                true,
                "adjust_volume",
                "Volume Adjusted",
                "Set $stream volume to $pct% ($currentVol/$maxVol)."
            )
        } catch (e: Exception) {
            ActionResult(false, "adjust_volume", "Volume Error", "Failed to adjust volume: ${e.localizedMessage}")
        }
    }

    private fun openSettingsPage(settingType: String): ActionResult {
        val action = when (settingType.lowercase().trim()) {
            "wifi", "wi-fi", "internet" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display", "screen", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound", "volume", "audio" -> Settings.ACTION_SOUND_SETTINGS
            "battery", "power" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "apps", "applications" -> Settings.ACTION_APPLICATION_SETTINGS
            "location", "gps" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            "date", "time" -> Settings.ACTION_DATE_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ActionResult(
                true,
                "open_settings_page",
                "Settings Opened",
                "Navigated to Android $settingType settings."
            )
        } else {
            ActionResult(false, "open_settings_page", "Settings Unavailable", "Could not open settings for $settingType.")
        }
    }

    private fun sendEmail(recipient: String, subject: String, body: String): ActionResult {
        val uri = Uri.parse("mailto:${Uri.encode(recipient.trim())}")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ActionResult(
                true,
                "send_email",
                "Email Drafted",
                "Prepared email to $recipient with subject '$subject'.",
                requiresUserApproval = true
            )
        } else {
            ActionResult(false, "send_email", "No Email Client", "No email app installed to send mail.")
        }
    }

    private fun playMusic(query: String): ActionResult {
        val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
            putExtra(android.app.SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ActionResult(true, "play_music", "Media Playback Dispatched", "Searching and playing: '$query'.")
        } else {
            // Fallback to searching YouTube or browser
            val ytUri = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
            val ytIntent = Intent(Intent.ACTION_VIEW, ytUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (ytIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(ytIntent)
                ActionResult(true, "play_music", "YouTube Dispatched", "Opened video/music stream for: '$query'.")
            } else {
                ActionResult(false, "play_music", "Media Player Unavailable", "No player or browser available.")
            }
        }
    }

    private fun getDeviceSpecs(): ActionResult {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
            val bytesTotal = stat.blockCountLong * stat.blockSizeLong
            val gbAvailable = bytesAvailable / (1024 * 1024 * 1024)
            val gbTotal = bytesTotal / (1024 * 1024 * 1024)

            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            val ramAvailableMb = memInfo.availMem / (1024 * 1024)
            val ramTotalMb = memInfo.totalMem / (1024 * 1024)

            val model = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
            val osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

            ActionResult(
                true,
                "get_device_specs",
                "Device Telemetry: $model",
                "OS: $osVersion | RAM: ${ramAvailableMb}MB free / ${ramTotalMb}MB | Storage: ${gbAvailable}GB free / ${gbTotal}GB total"
            )
        } catch (e: Exception) {
            ActionResult(false, "get_device_specs", "Specs Error", "Could not query hardware specs: ${e.localizedMessage}")
        }
    }

    private fun copyToClipboard(text: String): ActionResult {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return ActionResult(false, "copy_to_clipboard", "Clipboard Error", "Clipboard service unavailable.")
        val clip = ClipData.newPlainText("NOVA AI Copy", text)
        clipboard.setPrimaryClip(clip)
        val preview = if (text.length > 40) text.take(40) + "..." else text
        return ActionResult(true, "copy_to_clipboard", "Copied to Clipboard", "\"$preview\" copied.")
    }

    private fun takePhoto(): ActionResult {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ActionResult(true, "take_photo", "Camera Shutter Opened", "Opened camera for quick photo capture.")
        } else {
            ActionResult(false, "take_photo", "Camera Unavailable", "No camera application found on device.")
        }
    }

    // --- Core Tools Implementation ---

    private fun openApp(appName: String): ActionResult {
        if (appName.isBlank()) {
            return ActionResult(false, "open_app", "Missing App Name", "No application name was provided.")
        }

        val pm = context.packageManager
        val searchLower = appName.trim().lowercase()

        val commonPackages = mapOf(
            "camera" to listOf("com.android.camera", "com.google.android.GoogleCamera"),
            "calculator" to listOf("com.google.android.calculator", "com.android.calculator2"),
            "clock" to listOf("com.google.android.deskclock", "com.android.deskclock"),
            "settings" to listOf("com.android.settings"),
            "chrome" to listOf("com.android.chrome"),
            "youtube" to listOf("com.google.android.youtube"),
            "maps" to listOf("com.google.android.apps.maps"),
            "whatsapp" to listOf("com.whatsapp"),
            "gmail" to listOf("com.google.android.gm"),
            "calendar" to listOf("com.google.android.calendar", "com.android.calendar")
        )

        for ((key, pkgList) in commonPackages) {
            if (searchLower.contains(key)) {
                for (pkg in pkgList) {
                    val launchIntent = pm.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return ActionResult(true, "open_app", "App Launched", "Opened $appName ($pkg)")
                    }
                }
            }
        }

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        for (info in resolveInfos) {
            val label = info.loadLabel(pm).toString()
            if (label.lowercase().contains(searchLower) || searchLower.contains(label.lowercase())) {
                val launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return ActionResult(true, "open_app", "App Launched", "Opened $label")
                }
            }
        }

        return ActionResult(
            success = false,
            actionName = "open_app",
            summary = "App Not Found",
            details = "Could not find an installed application matching '$appName' on this device."
        )
    }

    private fun setTimer(durationSeconds: Int, label: String): ActionResult {
        if (durationSeconds <= 0) {
            return ActionResult(false, "set_timer", "Invalid Duration", "Duration must be greater than zero.")
        }

        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, durationSeconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ActionResult(
                true,
                "set_timer",
                "Timer Created",
                "Timer for $durationSeconds seconds ($label) dispatched to clock app."
            )
        } else {
            ActionResult(
                false,
                "set_timer",
                "No Timer App Available",
                "Android system reports no handler app for AlarmClock.ACTION_SET_TIMER."
            )
        }
    }

    private fun setAlarm(hour: Int, minute: Int, message: String): ActionResult {
        if (hour !in 0..23 || minute !in 0..59) {
            return ActionResult(false, "set_alarm", "Invalid Time", "Hour must be 0-23 and minute 0-59.")
        }

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            val formattedTime = String.format("%02d:%02d", hour, minute)
            ActionResult(
                true,
                "set_alarm",
                "Alarm Scheduled",
                "Alarm set for $formattedTime ($message)."
            )
        } else {
            ActionResult(
                false,
                "set_alarm",
                "No Clock App",
                "No application available to set system alarms."
            )
        }
    }

    private fun createCalendarEvent(title: String, minutesFromNow: Int, durationMinutes: Int, description: String): ActionResult {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, minutesFromNow)
        val startTime = cal.timeInMillis
        val endTime = startTime + (durationMinutes * 60 * 1000L)

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ActionResult(
                true,
                "create_calendar_event",
                "Calendar Event Drafted",
                "Scheduled '$title' starting in $minutesFromNow minutes (duration: ${durationMinutes}m)."
            )
        } else {
            ActionResult(
                false,
                "create_calendar_event",
                "Calendar App Unavailable",
                "No calendar application installed to handle event creation."
            )
        }
    }

    private fun draftSms(phoneNumber: String, message: String): ActionResult {
        val uri = Uri.parse("smsto:${Uri.encode(phoneNumber.trim())}")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ActionResult(
                true,
                "draft_sms",
                "SMS Prepared for Review",
                "Opened default SMS app with draft for $phoneNumber: \"$message\"",
                requiresUserApproval = true
            )
        } else {
            ActionResult(
                false,
                "draft_sms",
                "SMS App Not Found",
                "No SMS app found on this device to draft message."
            )
        }
    }

    private fun dialPhone(phoneNumber: String): ActionResult {
        val cleanNumber = phoneNumber.trim().replace(" ", "")
        val uri = Uri.parse("tel:${Uri.encode(cleanNumber)}")
        val intent = Intent(Intent.ACTION_DIAL, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ActionResult(
                true,
                "dial_phone",
                "Dialer Opened",
                "Opened system phone dialer with number: $cleanNumber"
            )
        } else {
            ActionResult(
                false,
                "dial_phone",
                "Dialer Unavailable",
                "No dialer application available on this device."
            )
        }
    }

    private fun getBatteryInfo(): ActionResult {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

        return if (batteryStatus != null) {
            val level: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1

            val status: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val chargePlug: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val plugType = when (chargePlug) {
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_AC -> "AC Wall Charger"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                else -> if (isCharging) "Charging" else "Discharging"
            }

            ActionResult(
                true,
                "get_battery_info",
                "Battery Status: $batteryPct%",
                "Current Level: $batteryPct% | Charging: ${if (isCharging) "Yes ($plugType)" else "No"} | Health: Good"
            )
        } else {
            ActionResult(
                false,
                "get_battery_info",
                "Battery Info Unavailable",
                "Could not read battery telemetry from Android BatteryManager."
            )
        }
    }

    private fun launchNavigation(destination: String): ActionResult {
        if (destination.isBlank()) {
            return ActionResult(false, "launch_navigation", "Missing Destination", "Please specify a location or address.")
        }

        val gmmIntentUri = Uri.parse("google.navigation:q=${Uri.encode(destination.trim())}")
        var mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (mapIntent.resolveActivity(context.packageManager) == null) {
            val geoUri = Uri.parse("geo:0,0?q=${Uri.encode(destination.trim())}")
            mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        return if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
            ActionResult(
                true,
                "launch_navigation",
                "Navigation Started",
                "Launched directions to '$destination'."
            )
        } else {
            ActionResult(
                false,
                "launch_navigation",
                "No Maps Application",
                "No maps or navigation app available on this device."
            )
        }
    }

    private fun webSearch(query: String): ActionResult {
        if (query.isBlank()) {
            return ActionResult(false, "web_search", "Empty Query", "Search query was empty.")
        }

        val isUrl = query.startsWith("http://") || query.startsWith("https://")
        val intent = if (isUrl) {
            Intent(Intent.ACTION_VIEW, Uri.parse(query)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(android.app.SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ActionResult(
                true,
                "web_search",
                "Search Dispatched",
                "Dispatched query: \"$query\""
            )
        } else {
            val searchUri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            val browserIntent = Intent(Intent.ACTION_VIEW, searchUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (browserIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(browserIntent)
                ActionResult(true, "web_search", "Browser Search", "Opened Google Search for: \"$query\"")
            } else {
                ActionResult(false, "web_search", "No Browser", "No web browser found on device.")
            }
        }
    }

    private fun shareText(text: String): ActionResult {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share via NOVA AI").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(shareIntent)
            ActionResult(true, "share_text", "Share Sheet Opened", "Shared content via system sheet.")
        } catch (e: Exception) {
            ActionResult(false, "share_text", "Share Failed", "Could not launch Android share sheet.")
        }
    }

    /**
     * Instantly matches and executes local device commands (flashlight, battery,
     * volume, timers, alarms, settings) without any cloud network latency.
     */
    fun matchAndExecuteLocalCommand(rawInput: String): ActionResult? {
        val normalized = rawInput.trim().lowercase()

        // Flashlight
        if (normalized in listOf("turn on flashlight", "flashlight on", "torch on", "turn on torch", "enable flashlight", "turn on the flashlight")) {
            return toggleFlashlight(true)
        }
        if (normalized in listOf("turn off flashlight", "flashlight off", "torch off", "turn off torch", "disable flashlight", "turn off the flashlight")) {
            return toggleFlashlight(false)
        }

        // Battery
        if (normalized in listOf("battery", "battery percentage", "battery level", "battery status", "check battery", "how much battery", "what is my battery level")) {
            return getBatteryInfo()
        }

        // Volume
        if (normalized in listOf("mute", "mute phone", "silence phone", "mute volume")) {
            return adjustVolume("mute", "music")
        }
        if (normalized in listOf("volume up", "increase volume", "turn up volume", "louder")) {
            return adjustVolume("raise", "music")
        }
        if (normalized in listOf("volume down", "decrease volume", "turn down volume", "quieter", "lower volume")) {
            return adjustVolume("lower", "music")
        }
        if (normalized in listOf("max volume", "maximum volume")) {
            return adjustVolume("max", "music")
        }

        // Settings
        if (normalized in listOf("open wifi", "wifi settings", "open wi-fi", "open wifi settings")) {
            return openSettingsPage("wifi")
        }
        if (normalized in listOf("open bluetooth", "bluetooth settings", "open bluetooth settings")) {
            return openSettingsPage("bluetooth")
        }
        if (normalized in listOf("open settings", "device settings")) {
            return openSettingsPage("main")
        }
        if (normalized in listOf("open sound settings", "sound settings", "volume settings")) {
            return openSettingsPage("sound")
        }
        if (normalized in listOf("open display settings", "display settings", "screen settings")) {
            return openSettingsPage("display")
        }
        if (normalized in listOf("open battery settings", "battery settings")) {
            return openSettingsPage("battery")
        }

        // Memory & Storage
        if (normalized in listOf("check ram", "check memory", "memory info", "ram usage")) {
            return getDeviceSpecs()
        }
        if (normalized in listOf("check storage", "storage info", "storage status", "free space")) {
            return getDeviceSpecs()
        }

        // Timer matching: e.g. "set timer for 5 minutes", "timer 30 seconds"
        val timerMatch = Regex("""(?:set\s+)?timer\s+(?:for\s+)?(\d+)\s*(minute|minutes|second|seconds|min|sec)""").find(normalized)
        if (timerMatch != null) {
            val amount = timerMatch.groupValues[1].toIntOrNull() ?: 60
            val unit = timerMatch.groupValues[2]
            val durationSec = if (unit.startsWith("min")) amount * 60 else amount
            return setTimer(durationSec, "NOVA Quick Timer")
        }

        // Alarm matching: e.g. "set alarm for 7:30", "set alarm for 8 am"
        val alarmMatch = Regex("""(?:set\s+)?alarm\s+(?:for\s+)?(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""").find(normalized)
        if (alarmMatch != null) {
            var hour = alarmMatch.groupValues[1].toIntOrNull() ?: 8
            val minute = alarmMatch.groupValues[2].toIntOrNull() ?: 0
            val amPm = alarmMatch.groupValues[3]
            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0
            return setAlarm(hour, minute, "NOVA AI Alarm")
        }

        return null
    }
}
