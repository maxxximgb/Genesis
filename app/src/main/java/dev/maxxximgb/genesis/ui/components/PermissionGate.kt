package dev.maxxximgb.genesis.ui.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.maxxximgb.genesis.R
import dev.maxxximgb.genesis.ui.theme.Sizes
import dev.maxxximgb.genesis.ui.theme.Spacing

private const val PERMISSION = Manifest.permission.READ_MEDIA_AUDIO

enum class PermissionStatus { Granted, NotRequested, Denied, DeniedPermanently }

@Composable
fun PermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity

    var status by remember { mutableStateOf(currentStatus(context, activity, false)) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            status = if (granted) {
                PermissionStatus.Granted
            } else {
                val showRationale =
                    activity?.shouldShowRequestPermissionRationale(PERMISSION) == true
                if (showRationale) PermissionStatus.Denied else PermissionStatus.DeniedPermanently
            }
        },
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                status = currentStatus(context, activity, status == PermissionStatus.DeniedPermanently)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when (status) {
        PermissionStatus.Granted -> content()
        PermissionStatus.NotRequested,
        PermissionStatus.Denied,
        PermissionStatus.DeniedPermanently -> PermissionRationale(
            status = status,
            onGrant = { launcher.launch(PERMISSION) },
            onOpenSettings = { context.openAppSettings() },
        )
    }
}

private fun currentStatus(
    context: android.content.Context,
    activity: Activity?,
    wasPermanentlyDenied: Boolean,
): PermissionStatus {
    val granted = ContextCompat.checkSelfPermission(context, PERMISSION) ==
        PackageManager.PERMISSION_GRANTED
    if (granted) return PermissionStatus.Granted
    if (wasPermanentlyDenied) return PermissionStatus.DeniedPermanently
    val showRationale = activity?.shouldShowRequestPermissionRationale(PERMISSION) == true
    return if (showRationale) PermissionStatus.Denied else PermissionStatus.NotRequested
}

private fun android.content.Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}

@Composable
private fun PermissionRationale(
    status: PermissionStatus,
    onGrant: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.LibraryMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = Spacing.lg),
            )
            Text(
                text = stringResource(R.string.permission_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = Spacing.md),
            )
            Text(
                text = stringResource(
                    if (status == PermissionStatus.DeniedPermanently)
                        R.string.permission_permanently_denied
                    else R.string.permission_message
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = Spacing.xl),
            )
            if (status == PermissionStatus.DeniedPermanently) {
                Button(onClick = onOpenSettings) {
                    Text(stringResource(R.string.permission_open_settings))
                }
            } else {
                Button(onClick = onGrant) {
                    Text(stringResource(R.string.permission_grant))
                }
            }
        }
    }
}
