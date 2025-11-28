package com.beemaster.beekeeperjournal.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import android.util.Log

object NotificationPermissionHelper {
    private const val TAG = "NotificationPermHelper"

    /**
     * Перевіряє та запитує дозвіл POST_NOTIFICATIONS, якщо необхідно.
     * * @param activity Контекст Activity (для доступу до системи дозволів).
     * @param requestPermissionLauncher ActivityResultLauncher для обробки результату запиту.
     */
    fun requestNotificationPermission(
        activity: Activity,
        requestPermissionLauncher: ActivityResultLauncher<String>
    ) {
        // Дозвіл POST_NOTIFICATIONS потрібен лише для Android 13 (API 33) і вище.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU = API 33 (Android 13)

            val permission = Manifest.permission.POST_NOTIFICATIONS

            if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
                // Дозвіл вже надано. Все добре.
                Log.i(TAG, "POST_NOTIFICATIONS permission already granted.")
            } else if (activity.shouldShowRequestPermissionRationale(permission)) {
                // Користувач відмовив раніше. Покажіть пояснення, чому цей дозвіл потрібен.
                // Наприклад, діалогове вікно, що пояснює, що завантаження буде невидимим без сповіщень.
                Log.w(TAG, "Showing rationale for POST_NOTIFICATIONS permission.")
                // Тут ви можете викликати AlertDialog або інший UI для пояснення
                requestPermissionLauncher.launch(permission)
            } else {
                // Запит дозволу вперше.
                Log.i(TAG, "Requesting POST_NOTIFICATIONS permission.")
                requestPermissionLauncher.launch(permission)
            }
        } else {
            // Для старих версій Android дозвіл не потрібен (він надається автоматично).
            Log.i(TAG, "POST_NOTIFICATIONS permission not required for API < 33.")
        }
    }
}