package tech.huangsh.firstsight.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.huangsh.firstsight.data.model.Reminder
import tech.huangsh.firstsight.data.repository.ReminderRepository
import tech.huangsh.firstsight.service.reminder.ReminderNotifier
import tech.huangsh.firstsight.service.reminder.ReminderScheduler
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val reminders = reminderRepository.allReminders.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    fun saveReminder(reminder: Reminder, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            if (reminder.id != 0L) {
                ReminderNotifier.cancel(appContext, reminder.id)
            }
            val savedId = if (reminder.id == 0L) {
                reminderRepository.insertReminder(reminder)
            } else {
                reminderRepository.updateReminder(reminder)
                reminder.id
            }
            val saved = reminderRepository.getReminderById(savedId) ?: reminder.copy(id = savedId)
            if (saved.enabled) {
                reminderScheduler.schedule(saved)
            } else {
                reminderScheduler.cancel(saved.id)
            }
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderScheduler.cancel(reminder.id)
            ReminderNotifier.cancel(appContext, reminder.id)
            reminderRepository.deleteReminder(reminder)
        }
    }

    fun setEnabled(reminder: Reminder, enabled: Boolean) {
        viewModelScope.launch {
            val updated = reminder.copy(enabled = enabled)
            reminderRepository.updateReminder(updated)
            if (enabled) {
                reminderScheduler.schedule(updated)
            } else {
                reminderScheduler.cancel(updated.id)
                ReminderNotifier.cancel(appContext, updated.id)
            }
        }
    }
}
