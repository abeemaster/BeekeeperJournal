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

    // КОНСТАНТИ VOSK MODEL
    const val VOSK_MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-uk-v3-small.zip"

    // Імена файлів та директорій
    const val VOSK_MODEL_ZIP_NAME = "vosk-model-small-uk-v3-small.zip"

    const val VOSK_MODEL_DIR_NAME = "vosk-model"

    // Ключі для WorkManager
    const val WORK_KEY_MODEL_ZIP_NAME = "model_zip_name"
    // Ключ для шляху до розпакованої моделі
    const val WORK_KEY_MODEL_UNZIPPED_PATH = "model_unzipped_path"

    // Ключ для оновлення прогресу (використовується в setProgressAsync)
    const val WORK_KEY_PROGRESS = "Progress"

    // Розміри
    const val BUFFER_SIZE = 8192 // 8KB для операцій читання/запису

    // Ключ для збереження ID активного пасічного року в SharedPreferences
    const val KEY_ACTIVE_YEAR_ID = "active_beekeeping_year_id"

}

