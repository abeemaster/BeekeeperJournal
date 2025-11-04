
package com.beemaster.beekeeperjournal.utils


interface VoskSearchListener {
    fun performSearchFromVosk(query: String)
}

/**
 * Призначення VoskSearchListener
 * Цей інтерфейс виконує роль зворотного виклику (Callback), або слухача (Listener), і є ключовим елементом,
 * що забезпечує архітектурно чисте спілкування між класом VoiceManager та тими Activity, які повинні реагувати на розпізнане мовлення.
 *
 * Роль у Проєкті
 * Розділення Відповідальності (SRP): Він дозволяє VoiceManager (який займається лише розпізнаванням голосу) відправляти розпізнаний текст,
 * не знаючи, що саме з цим текстом буде робити Activity (наприклад, SearchActivity).
 *
 * Абстракція: Інтерфейс створює контракт: будь-яка Activity, яка реалізує VoskSearchListener,
 * обов'язково матиме функцію performSearchFromVosk(query: String).
 *
 * Використання у VoiceManager:
 *
 * У VoiceManager оголошено поле private var searchListener: VoskSearchListener? = null.
 *
 * У методі init() ви перевіряєте, чи є поточна Activity цим слухачем: this.searchListener = activity as? VoskSearchListener.
 *
 * У методі insertTextIntoTargetInput() ви викликаєте його, якщо він існує: searchListener?.performSearchFromVosk(newText).
 *
 * Функція performSearchFromVosk(query: String)
 * Єдина функція в інтерфейсі призначена для того, щоб:
 *
 * Прийняти фінальний розпізнаний текст (query: String), який надсилає VoiceManager.
 *
 * Запустити логіку пошуку всередині відповідної Activity (наприклад, SearchActivity), оскільки голос,
 * ймовірно, використовувався саме для пошукового запиту.
 */