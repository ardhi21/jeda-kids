package com.jedakids.app

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {
    private var refreshKey by mutableIntStateOf(0)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        refreshKey += 1
        if (!isGranted) {
            showMessage("Notifications are required for the time-up warning.")
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TimeUpNotifier.createNotificationChannel(this)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    JedaKidsScreen(
                        refreshKey = refreshKey,
                        onEnableDeviceAdmin = ::requestDeviceAdmin,
                        onEnableExactAlarms = ::requestExactAlarmAccess,
                        onEnableNotifications = ::requestNotificationAccess,
                        onStartSession = ::startSession,
                        onCancelSession = ::cancelSession,
                        onLockNow = ::lockNowForTesting,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshKey += 1
    }

    private fun requestDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                LockController.adminComponent(this@MainActivity)
            )
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                getString(R.string.app_name) + " needs permission to show the lock screen when the timer ends."
            )
        }
        startActivity(intent)
    }

    private fun requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            refreshKey += 1
            return
        }

        val intent = Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            "package:$packageName".toUri()
        )

        runCatching {
            startActivity(intent)
        }.onFailure {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    "package:$packageName".toUri()
                )
            )
        }
    }

    private fun requestNotificationAccess() {
        val needsRuntimePermission =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED

        if (needsRuntimePermission) {
            notificationPermissionLauncher.launch(
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            return
        }

        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    private fun startSession(minutes: Int) {
        if (!LockController.isAdminActive(this)) {
            showMessage("Enable device administrator first.")
            return
        }

        if (!TimerScheduler.canScheduleExactAlarms(this)) {
            showMessage("Allow Alarms & reminder first.")
            return
        }

        val endAtMillis = System.currentTimeMillis() + TimerMath.durationMillis(minutes)

        runCatching {
            TimerPreferences(this).saveEndAtMillis(endAtMillis)
            TimeUpNotifier.cancel(this)
            TimerScheduler.schedule(this, endAtMillis)
        }.onSuccess {
            refreshKey += 1
            showMessage("Session started for $minutes minute(s).")
        }.onFailure { error ->
            TimerScheduler.cancel(this)
            TimerPreferences(this).clearSession()
            showMessage(error.message ?: "Could not start the session")
        }
    }

    private fun cancelSession() {
        TimerScheduler.cancel(this)
        TimeUpNotifier.cancel(this)
        TimerPreferences(this).clearSession()
        refreshKey += 1
        showMessage("Session cancelled.")
    }

    private fun lockNowForTesting() {
        if (!LockController.lockNow(this)) {
            showMessage("Enable device administrator first.")
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun JedaKidsScreen(
    refreshKey: Int,
    onEnableDeviceAdmin: () -> Unit,
    onEnableExactAlarms: () -> Unit,
    onEnableNotifications: () -> Unit,
    onStartSession: (Int) -> Unit,
    onCancelSession: () -> Unit,
    onLockNow: () -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember(context) { TimerPreferences(context) }
    var minutesText by rememberSaveable { mutableStateOf("1") }
    var nowMillis by remember(refreshKey) {
        mutableLongStateOf(System.currentTimeMillis())
    }

    val endAtMillis = preferences.getEndAtMillis()

    LaunchedEffect(endAtMillis, refreshKey) {
        nowMillis = System.currentTimeMillis()
        while (
            endAtMillis != TimerPreferences.NO_ACTIVE_SESSION &&
            nowMillis < endAtMillis + TimerScheduler.LOCK_GRACE_PERIOD_MILLIS
        ) {
            delay(1_000L.milliseconds)
            nowMillis = System.currentTimeMillis()
        }
    }

    val remainingMillis = TimerMath.remainingMillis(endAtMillis, nowMillis)
    val sessionExists = endAtMillis != TimerPreferences.NO_ACTIVE_SESSION
    val countdownIsRunning = sessionExists && remainingMillis > 0L
    val lockAtMillis = endAtMillis + TimerScheduler.LOCK_GRACE_PERIOD_MILLIS
    val warningIsVisible =
        sessionExists &&
                remainingMillis == 0L &&
                nowMillis < lockAtMillis
    val secondsUntilLock = ((lockAtMillis - nowMillis).coerceAtLeast(0L) + 999L) / 1_000L

    val adminIsActive = LockController.isAdminActive(context)
    val exactAlarmsAreAllowed = TimerScheduler.canScheduleExactAlarms(context)
    val notificationsAreAllowed = TimeUpNotifier.canPostNotifications(context)
    val parsedMinutes = minutesText.toIntOrNull()
    val minutesAreValid = parsedMinutes != null &&
            parsedMinutes in TimerMath.MIN_SESSION_MINUTES..TimerMath.MAX_SESSION_MINUTES

    if (warningIsVisible) {
        AlertDialog(
            onDismissRequest =  {},
            title = {
                Text("Time's up!")
            },
            text = {
                Text("This device will lock in $secondsUntilLock seconds.")
            },
            confirmButton = {},
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "A small pause healthier screen habits.",
            style = MaterialTheme.typography.bodyLarge,
        )

        StatusCard(
            title = "1. Device administrator",
            isReady = adminIsActive,
            readyText = "Ready to lock the screen",
            missingText = "Required for locking",
            buttonText = "Enable device administrator",
            onClick = onEnableDeviceAdmin
        )

        StatusCard(
            title = "2. Alarms & reminders",
            isReady = exactAlarmsAreAllowed,
            readyText = "Ready to run at the deadline",
            missingText = "Required for an exact deadline",
            buttonText = "Allow alarms & reminders",
            onClick = onEnableExactAlarms
        )

        StatusCard(
            title = "3. Notifications",
            isReady = notificationsAreAllowed,
            readyText = "Ready to show the time-up warning",
            missingText = "Required for the background warning",
            buttonText = "Allow notifications",
            onClick = onEnableNotifications
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "4. Timed session",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                if (sessionExists) {
                    if (countdownIsRunning) {
                        Text(
                            text = TimerMath.formatRemaining((remainingMillis)),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "A warning appears at zero, followed by a 10-second grace period"
                        )
                        OutlinedButton(
                            onClick = onCancelSession,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel session - development only")
                        }
                    } else {
                        Text(
                            text = "Time's up!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text("Locking in $secondsUntilLock seconds")
                    }
                } else {
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { newValue ->
                            minutesText = newValue.filter(Char::isDigit).take(3)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Session length in minutes") },
                        supportingText = {
                            Text(
                                "Enter ${TimerMath.MIN_SESSION_MINUTES} to " +
                                    "${TimerMath.MAX_SESSION_MINUTES} minutes."
                            )
                        },
                        isError = minutesText.isNotEmpty() && !minutesAreValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )

                    Button(
                        onClick = { onStartSession(requireNotNull(parsedMinutes)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled =
                            adminIsActive &&
                                    exactAlarmsAreAllowed &&
                                    notificationsAreAllowed &&
                                    minutesAreValid,
                    ) {
                        Text("Start session")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Developer tools",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Verify device-admin locking without waiting for a timer.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(
            onClick = onLockNow,
            modifier = Modifier.fillMaxWidth(),
            enabled = adminIsActive,
        ) {
            Text("Lock now - test")
        }

        Text(
            text = "Development warning: cancellation is not protected by a parent PIN yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun StatusCard(
    title: String,
    isReady: Boolean,
    readyText: String,
    missingText: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(if (isReady) "Enabled" else "Not enabled")
                Text(if (isReady) readyText else missingText)
            }

            if (!isReady) {
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(buttonText)
                }
            }
        }
    }
}
