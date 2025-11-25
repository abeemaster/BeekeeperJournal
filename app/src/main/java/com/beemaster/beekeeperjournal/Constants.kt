// Constants.kt файл з усіма константами.

package com.beemaster.beekeeperjournal

object Constants {

    const val TYPE_HIVE = "hive"
    const val TYPE_EXPENSE = "expense"
    const val TYPE_INCOME = "income"

    // КОНСТАНТИ ДОЗВОЛІВ
    const val REQUEST_RECORD_AUDIO_PERMISSION = 200

    // НАЛАШТУВАННЯ (Settings)
    const val SETTINGS_PREFS_NAME = "app_settings"

    // Ключі для налаштувань
    const val KEY_SPEECH_ENGINE = "speech_engine"

    // Значення для рушіїв
    const val ENGINE_GOOGLE = "google"
    const val ENGINE_VOSK = "vosk"

    // Значення за замовчуванням
    const val DEFAULT_SPEECH_ENGINE = ENGINE_GOOGLE
    const val EXTRA_HIVE_ID = "com.beemaster.beekeeperjournal.HIVE_ID"
    const val EXTRA_HIVE_NUMBER = "com.beemaster.beekeeperjournal.HIVE_NUMBER"
    const val EXTRA_HIVE_COLOR = "com.beemaster.beekeeperjournal.HIVE_COLOR"
    const val EXTRA_HIVE_SECONDARY_COLOR = "com.beemaster.beekeeperjournal.HIVE_SECONDARY_COLOR"
    const val EXTRA_ENTRY_TYPE = "com.beemaster.beekeeperjournal.ENTRY_TYPE"
    const val EXTRA_NOTE_ID = "com.beemaster.beekeeperjournal.NOTE_ID"
    const val EXTRA_ORIGINAL_NOTE_TEXT = "com.beemaster.beekeeperjournal.ORIGINAL_NOTE_TEXT"
    const val EXTRA_START_VOICE_INPUT = "com.beemaster.beekeeperjournal.START_VOICE_INPUT"

    // КОНСТАНТИ VOSK MODEL (ДЛЯ WORK MANAGER ТА ЗБЕРІГАННЯ)
    // URL для завантаження Vosk моделі
    const val VOSK_MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-uk-v3-small.zip"

    // Імена файлів та директорій
    const val VOSK_MODEL_ZIP_NAME = "vosk-model-small-uk-v3-small.zip"
    const val VOSK_MODEL_UNPACKED_NAME = "vosk-model-uk-small-0.22"

    // Теги та ключі WorkManager
    const val WORK_TAG_MODEL_SETUP = "vosk_model_setup"
    const val WORK_KEY_MODEL_ZIP_NAME = "model_zip_file_name"
    const val WORK_KEY_MODEL_UNPACKED_NAME = "model_unpacked_dir_name"
    const val WORK_KEY_MODEL_DOWNLOAD_URL = "model_download_url"

    // Ідентифікатор сповіщення для завантаження моделі
    const val NOTIFICATION_CHANNEL_ID_MODEL = "model_download_channel"
    const val NOTIFICATION_ID_MODEL = 101
}
