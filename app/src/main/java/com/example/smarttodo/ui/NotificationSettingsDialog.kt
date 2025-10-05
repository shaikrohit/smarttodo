package com.example.smarttodo.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.example.smarttodo.R
import com.example.smarttodo.databinding.DialogNotificationSettingsBinding
import com.example.smarttodo.util.SoundMode
import com.example.smarttodo.utils.NotificationHelper

class NotificationSettingsDialog : DialogFragment() {
    private var _binding: DialogNotificationSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var notificationHelper: NotificationHelper

    private val soundPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                @Suppress("DEPRECATION")
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
                } else {
                    intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                }
                notificationHelper.saveNotificationSettings(SoundMode.CUSTOM, uri)
                updateCurrentSoundText()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogNotificationSettingsBinding.inflate(inflater, container, false)
        notificationHelper = NotificationHelper(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadInitialSettings()
        setupClickListeners()
        setupRadioGroupListener()
        updateCurrentSoundText()

        // Handle Save button click
        binding.buttonSaveSettings.setOnClickListener {
            val selectedMode = when (binding.radioGroupSoundMode.checkedRadioButtonId) {
                R.id.radioButtonDefaultSound -> SoundMode.DEFAULT
                R.id.radioButtonCustomSound -> SoundMode.CUSTOM
                R.id.radioButtonSilent -> SoundMode.SILENT
                else -> SoundMode.DEFAULT
            }

            notificationHelper.saveNotificationSettings(selectedMode, null)
            Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun loadInitialSettings() {
        // Load sound mode settings
        val (currentMode, _) = notificationHelper.getNotificationSoundSettings()
        when (currentMode) {
            SoundMode.DEFAULT -> binding.radioGroupSoundMode.check(R.id.radioButtonDefaultSound)
            SoundMode.CUSTOM -> binding.radioGroupSoundMode.check(R.id.radioButtonCustomSound)
            SoundMode.SILENT -> binding.radioGroupSoundMode.check(R.id.radioButtonSilent)
        }
        binding.buttonSelectSound.isEnabled = (currentMode == SoundMode.CUSTOM)

        // Load snooze duration
        val currentSnoozeDuration = notificationHelper.getSnoozeDuration()
        binding.editTextSnoozeDuration.setText(currentSnoozeDuration.toString())
    }

    private fun setupRadioGroupListener() {
        binding.radioGroupSoundMode.setOnCheckedChangeListener { _, checkedId ->
            val selectedMode = when (checkedId) {
                R.id.radioButtonDefaultSound -> SoundMode.DEFAULT
                R.id.radioButtonCustomSound -> SoundMode.CUSTOM
                R.id.radioButtonSilent -> SoundMode.SILENT
                else -> SoundMode.DEFAULT
            }

            binding.buttonSelectSound.isEnabled = (selectedMode == SoundMode.CUSTOM)

            if (selectedMode == SoundMode.CUSTOM) {
                val (_, customUri) = notificationHelper.getNotificationSoundSettings()
                if (customUri == null) {
                    openSoundPicker()
                } else {
                    notificationHelper.saveNotificationSettings(SoundMode.CUSTOM, customUri)
                }
            } else {
                notificationHelper.saveNotificationSettings(selectedMode, null)
            }
            updateCurrentSoundText()
        }
    }

    private fun setupClickListeners() {
        binding.buttonSelectSound.setOnClickListener {
            if (binding.radioGroupSoundMode.checkedRadioButtonId == R.id.radioButtonCustomSound) {
                openSoundPicker()
            }
        }

        binding.buttonClose.setOnClickListener {
            if (saveSnoozeDuration()) {
                dismiss()
            }
        }
    }

    private fun saveSnoozeDuration(): Boolean {
        val snoozeText = binding.editTextSnoozeDuration.text.toString()
        if (snoozeText.isBlank()) {
            notificationHelper.saveSnoozeDuration(NotificationHelper.DEFAULT_SNOOZE_MINUTES)
            Toast.makeText(context, getString(R.string.snooze_duration_empty_default), Toast.LENGTH_SHORT).show()
            return true
        }
        return try {
            val snoozeMinutes = snoozeText.toInt()
            if (snoozeMinutes > 0) {
                notificationHelper.saveSnoozeDuration(snoozeMinutes)
                true
            } else {
                Toast.makeText(context, getString(R.string.snooze_duration_invalid_default), Toast.LENGTH_SHORT).show()
                false
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(context, getString(R.string.snooze_duration_invalid_format), Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun openSoundPicker() {
        val (_, currentCustomUri) = notificationHelper.getNotificationSoundSettings()
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.notification_select_custom_sound))
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentCustomUri)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        }
        soundPickerLauncher.launch(intent)
    }

    private fun updateCurrentSoundText() {
        val (currentMode, customSoundUri) = notificationHelper.getNotificationSoundSettings()
        val currentSoundText = when (currentMode) {
            SoundMode.DEFAULT -> getString(R.string.notification_sound_display_default)
            SoundMode.SILENT -> getString(R.string.notification_sound_display_silent)
            SoundMode.CUSTOM -> {
                val ringtoneName = if (customSoundUri != null) {
                    RingtoneManager.getRingtone(context, customSoundUri)?.getTitle(context)
                        ?: getString(R.string.notification_sound_display_unknown)
                } else {
                    getString(R.string.notification_sound_display_unknown) + " (Please select a sound)"
                }
                getString(R.string.notification_sound_display_custom_prefix) + ringtoneName
            }
        }
        binding.textCurrentSound.text = getString(R.string.notification_current_sound_prefix) + currentSoundText
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "NotificationSettingsDialog"
    }
}
