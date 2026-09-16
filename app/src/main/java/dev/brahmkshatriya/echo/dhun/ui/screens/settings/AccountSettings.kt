/*
 * Dhun Project Original (2026)
 * Dhun
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package dev.brahmkshatriya.echo.dhun.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.brahmkshatriya.echo.R
import dev.brahmkshatriya.echo.dhun.App
import dev.brahmkshatriya.echo.dhun.constants.AccountChannelHandleKey
import dev.brahmkshatriya.echo.dhun.constants.AccountEmailKey
import dev.brahmkshatriya.echo.dhun.constants.AccountNameKey
import dev.brahmkshatriya.echo.dhun.ui.screens.buildLoginRoute
import dev.brahmkshatriya.echo.dhun.ui.screens.YOUTUBE_LOGIN_URL
import dev.brahmkshatriya.echo.dhun.utils.rememberPreference

/**
 * The account panel rendered inside `AccountSettingsDialog`
 * (see `dhun/ui/component/Dialog.kt`) and on the "account" route.
 */
@Composable
fun AccountSettings(
    navController: NavController,
    onClose: () -> Unit,
    latestVersionName: String,
) {
    val context = LocalContext.current
    val accountName by rememberPreference(AccountNameKey, defaultValue = "")
    val accountEmail by rememberPreference(AccountEmailKey, defaultValue = "")
    val accountHandle by rememberPreference(AccountChannelHandleKey, defaultValue = "")
    val isLoggedIn = accountName.isNotBlank() || accountHandle.isNotBlank()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.dhun),
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape),
        )

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )

        Text(
            text = latestVersionName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.account),
            style = MaterialTheme.typography.titleMedium,
        )

        if (isLoggedIn) {
            Text(
                text = accountName.ifBlank { accountHandle },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            if (accountEmail.isNotBlank()) {
                Text(
                    text = accountEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Text(
                text = stringResource(R.string.x_login_required),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoggedIn) {
                OutlinedButton(
                    onClick = {
                        App.forgetAccount(context)
                        onClose()
                    },
                ) {
                    Text(stringResource(R.string.logout))
                }
            } else {
                OutlinedButton(
                    onClick = {
                        onClose()
                        navController.navigate(buildLoginRoute(YOUTUBE_LOGIN_URL))
                    },
                ) {
                    Text(stringResource(R.string.login))
                }
            }

            TextButton(onClick = onClose) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    }
}
