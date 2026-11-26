package com.bellatrixos.app_settings.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.runtime.snapshotFlow
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bellatrixos.App
import com.bellatrixos.R
import com.bellatrixos.Runtime
import com.bellatrixos.net.LocalNetwork
import com.bellatrixos.storage.PhoneStorage
import com.bellatrixos.storage.showingStorage
import com.bellatrixos.system.hasPermission
import com.bellatrixos.ui.AskPermission
import com.bellatrixos.ui.appDetails
import com.bellatrixos.widgets.cards.UISettingsCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Every permission the launcher has, as a Bellatrix Settings page of native cards. Android keeps the
 * switches: turning one on asks with its dialog or opens its page, turning one off opens the phone's
 * settings. Replaces the Omalauncher-style permissions screen.
 */
class UIPermissions : Fragment() {
    private lateinit var runtime: Runtime
    private lateinit var list: LinearLayout
    private lateinit var asks: Map<String, AskPermission>
    private var foldersContainer: LinearLayout? = null

    private val pickFolder =
        registerForActivityResult(
            object : ActivityResultContracts.OpenDocumentTree() {
                override fun createIntent(
                    context: Context,
                    input: Uri?,
                ): Intent = super.createIntent(context, input).showingStorage()
            },
        ) { tree ->
            val context = context ?: return@registerForActivityResult
            if (tree != null && ::runtime.isInitialized) {
                if (runCatching { runtime.phoneFolders.add(tree) }.isFailure) {
                    Toast.makeText(context, R.string.permissions_folders_cant_keep, Toast.LENGTH_SHORT).show()
                }
            }
        }

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        asks =
            mapOf(
                MIC to AskPermission(this, Manifest.permission.RECORD_AUDIO, orSettings = true),
                LOCATION to AskPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION, orSettings = true),
                CALENDAR to AskPermission(this, Manifest.permission.READ_CALENDAR, orSettings = true),
                BLUETOOTH to AskPermission(this, Manifest.permission.BLUETOOTH_CONNECT, orSettings = true),
                NOTIFICATIONS to AskPermission(this, Manifest.permission.POST_NOTIFICATIONS, orSettings = true),
                LOCAL_NETWORK to AskPermission(this, LocalNetwork.PERMISSION, orSettings = true),
            )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.settings_permissions, container, false)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        runtime = (requireContext().applicationContext as App).runtime
        list = view.findViewById(R.id.permissions_list)
        viewLifecycleOwner.lifecycleScope.launch {
            snapshotFlow { runtime.phoneFolders.changes }.collect { if (::list.isInitialized) renderFolders() }
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun changed() {
        if (!::runtime.isInitialized) return
        runtime.location.recheck()
        runtime.calendar.recheck()
        runtime.widgets.airpods.recheck()
        render()
    }

    private fun ask(key: String) {
        asks[key]?.ask { changed() }
    }

    private fun openAppDetails() {
        runCatching { startActivity(appDetails(requireContext())) }
    }

    private class Row(
        @StringRes val title: Int,
        @StringRes val uses: Int,
        val granted: Boolean,
        val turnOn: () -> Unit,
        val turnOff: () -> Unit,
    )

    @SuppressLint("InlinedApi")
    private fun render() {
        if (!::list.isInitialized) return
        val context = requireContext()
        val sdk = Build.VERSION.SDK_INT
        val notifications = context.getSystemService(NotificationManager::class.java)
        val rows =
            listOfNotNull(
                Row(R.string.permissions_mic, R.string.permissions_mic_uses, context.hasPermission(Manifest.permission.RECORD_AUDIO), { ask(MIC) }, ::openAppDetails),
                Row(R.string.permissions_location, R.string.permissions_location_uses, context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION), { ask(LOCATION) }, ::openAppDetails),
                Row(R.string.permissions_calendar, R.string.permissions_calendar_uses, context.hasPermission(Manifest.permission.READ_CALENDAR), { ask(CALENDAR) }, ::openAppDetails),
                // Before Android 12, Bluetooth is an install-time permission without a switch.
                if (sdk >= Build.VERSION_CODES.S) {
                    Row(R.string.permissions_bluetooth, R.string.permissions_bluetooth_uses, context.hasPermission(Manifest.permission.BLUETOOTH_CONNECT), { ask(BLUETOOTH) }, ::openAppDetails)
                } else {
                    null
                },
                // Before Android 13 there's no dialog, only the launcher's notification page.
                Row(
                    R.string.permissions_notifications,
                    R.string.permissions_notifications_uses,
                    notifications.areNotificationsEnabled(),
                    {
                        if (sdk >= Build.VERSION_CODES.TIRAMISU && !context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                            ask(NOTIFICATIONS)
                        } else {
                            openNotificationSettings()
                        }
                    },
                    ::openNotificationSettings,
                ),
                if (sdk >= LocalNetwork.MIN_SDK) {
                    Row(R.string.settings_local_network, R.string.settings_local_network_subtitle, !LocalNetwork.missing(context), { ask(LOCAL_NETWORK) }, ::openAppDetails)
                } else {
                    null
                },
                Row(
                    R.string.permissions_notification_access,
                    R.string.permissions_notification_access_uses,
                    runtime.widgets.media.granted,
                    runtime.widgets.media::openAccessSettings,
                    runtime.widgets.media::openAccessSettings,
                ),
            )
        foldersContainer = null
        list.removeAllViews()
        for (row in rows) list.addView(permissionCard(row))
        // For Files and Linux, so only with Linux there.
        if (runtime.bootstrap.wanted) addFolders()
    }

    private fun openNotificationSettings() {
        val intent =
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }
    }

    private fun permissionCard(row: Row): UISettingsCard {
        val card = UISettingsCard(requireContext())
        card.layoutParams = cardParams(4)
        card.spreadText()
        card.setImage(R.drawable.ic_lock_closed_24_regular)
        card.setTitle(getString(row.title))
        card.setSubtitle(getString(if (row.granted) R.string.permissions_on else R.string.permissions_off, getString(row.uses)))
        card.setOnCardClickListener { if (row.granted) row.turnOff() else row.turnOn() }
        return card
    }

    private fun addFolders() {
        val context = requireContext()
        list.addView(
            TextView(context).apply {
                text = getString(R.string.permissions_folders_title)
                textSize = 15f
                setTextColor(resources.getColor(R.color.text_default, null))
                layoutParams = cardParams(10)
            },
        )
        list.addView(
            TextView(context).apply {
                text = getString(R.string.permissions_folders_about)
                textSize = 12f
                setTextColor(resources.getColor(R.color.text_muted, null))
                layoutParams = cardParams()
            },
        )
        if (PhoneStorage.supported()) {
            list.addView(
                TextView(context).apply {
                    text = getString(R.string.permissions_folders_sdcard, PhoneStorage.GUEST)
                    textSize = 12f
                    setTextColor(resources.getColor(R.color.text_muted, null))
                    layoutParams = cardParams()
                },
            )
        }
        foldersContainer =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = cardParams()
            }
        list.addView(foldersContainer)
        renderFolders()
    }

    private fun renderFolders() {
        val container = foldersContainer ?: return
        container.removeAllViews()
        val context = requireContext()
        viewLifecycleOwner.lifecycleScope.launch {
            val folders = withContext(Dispatchers.IO) { runCatching { runtime.phoneFolders.folders() }.getOrDefault(emptyList()) }
            if (foldersContainer !== container || !isAdded) return@launch
            for (folder in folders) {
                val card = UISettingsCard(context)
                card.layoutParams = cardParams(2)
                card.setImage(R.drawable.ic_info_26_regular)
                card.setTitle(folder.name)
                card.setSubtitle(null)
                card.setOnCardClickListener { runtime.phoneFolders.remove(folder) }
                container.addView(card)
            }
            val add = UISettingsCard(context)
            add.layoutParams = cardParams()
            add.setImage(R.drawable.img_app_installer)
            add.setTitle(getString(R.string.permissions_folders_add))
            add.setSubtitle(null)
            add.setOnCardClickListener {
                try {
                    pickFolder.launch(null)
                } catch (_: android.content.ActivityNotFoundException) {
                    Toast.makeText(context, R.string.permissions_folders_no_picker, Toast.LENGTH_SHORT).show()
                }
            }
            container.addView(add)
        }
    }

    private fun cardParams(topMarginDp: Int = 0) =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = (topMarginDp * resources.displayMetrics.density).toInt() }

    private companion object {
        const val MIC = "mic"
        const val LOCATION = "location"
        const val CALENDAR = "calendar"
        const val BLUETOOTH = "bluetooth"
        const val NOTIFICATIONS = "notifications"
        const val LOCAL_NETWORK = "local_network"
    }
}
