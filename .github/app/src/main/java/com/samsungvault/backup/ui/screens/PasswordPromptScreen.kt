package com.samsungvault.backup.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.samsungvault.backup.R

/** شاشة واحدة تُستخدم سواء لتعيين كلمة مرور عند النسخ، أو لإدخالها عند الاستعادة. */
@Composable
fun PasswordPromptScreen(
    requireConfirmation: Boolean,
    onConfirmed: (CharArray) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.password_set_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password_set_hint)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (requireConfirmation) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it },
                label = { Text(stringResource(R.string.password_confirm_hint)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                when {
                    password.length < 6 -> error = "كلمة المرور قصيرة جدًا (6 أحرف على الأقل)"
                    requireConfirmation && password != confirm -> error = "كلمتا المرور غير متطابقتين"
                    else -> onConfirmed(password.toCharArray())
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("متابعة")
        }
    }
}
