package tech.huangsh.firstsight.service.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tech.huangsh.firstsight.data.model.Reminder
import tech.huangsh.firstsight.data.repository.ReminderRepository
import tech.huangsh.firstsight.data.repository.SettingsRepository
import tech.huangsh.firstsight.utils.ReminderTimeUtils
import tech.huangsh.firstsight.utils.VoiceAssistant
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER = "tech.huangsh.firstsight.action.REMINDER_TRIGGER"
        const val ACTION_ACK = "tech.huangsh.firstsight.action.REMINDER_ACK"
        const val ACTION_SNOOZE = "tech.huangsh.firstsight.action.REMINDER_SNOOZE"
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_SNOOZE = "reminder_snooze"
    }

    @Inject lateinit var reminderRepository: ReminderRepository

    @Inject lateinit var reminderScheduler: ReminderScheduler

    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var voiceAssistant: VoiceAssistant

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_ID, -1L)
        if (reminderId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_TRIGGER -> handleTrigger(
                        context = context,
                        reminderId = reminderId,
                        isSnooze = intent.getBooleanExtra(EXTRA_SNOOZE, false)
                    )
                    ACTION_ACK -> handleAcknowledge(context, reminderId)
                    ACTION_SNOOZE -> handleSnooze(context, reminderId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleTrigger(context: Context, reminderId: Long, isSnooze: Boolean) {
        val reminder = reminderRepository.getReminderById(reminderId) ?: return
        if (!reminder.enabled) return

        if (!isSnooze) {
            if (reminder.onceAt != null) {
                reminderRepository.setEnabled(reminder.id, false)
            } else {
                reminderScheduler.schedule(reminder)
            }
        }

        ReminderNotifier.show(context, reminder)
        speakReminder(reminder)
    }

    private suspend fun handleAcknowledge(context: Context, reminderId: Long) {
        val reminder = reminderRepository.getReminderById(reminderId) ?: return
        reminderRepository.markAcknowledged(reminderId)
        ReminderNotifier.cancel(context, reminderId)
        reminderScheduler.cancelSnooze(reminderId)
        if (reminder.onceAt != null) {
            reminderScheduler.cancel(reminderId)
            reminderRepository.setEnabled(reminderId, false)
        }
    }

    private suspend fun handleSnooze(context: Context, reminderId: Long) {
        val reminder = reminderRepository.getReminderById(reminderId) ?: return
        ReminderNotifier.cancel(context, reminderId)
        reminderScheduler.scheduleSnooze(reminder)
    }

    private suspend fun speakReminder(reminder: Reminder) {
        val settings = settingsRepository.settings.first()
        if (!settings.voiceEnabled) return
        voiceAssistant.speak(
            text = ReminderTimeUtils.buildTextForSpeech(reminder),
            volume = settings.voiceVolume
        )
    }
}
