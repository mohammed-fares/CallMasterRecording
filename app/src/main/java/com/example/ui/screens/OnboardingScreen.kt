package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.compatibility.DeviceCompatibilityEngine
import com.example.preferences.CallMasterPreferences
import com.example.ui.theme.DarkGreen
import com.example.ui.theme.LightGold
import com.example.ui.theme.PetroleumGreen
import com.example.ui.theme.RadiantGold
import com.example.ui.theme.SurfaceDark

data class OnboardingStep(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val detailText: String
)

@Composable
fun OnboardingScreen(
    preferences: CallMasterPreferences,
    compatibilityEngine: DeviceCompatibilityEngine,
    onFinish: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions handled
    }

    val steps = listOf(
        OnboardingStep(
            titleRes = R.string.onboarding_welcome_title,
            descRes = R.string.onboarding_welcome_desc,
            icon = Icons.Default.Phone,
            detailText = "CallMaster provides professional audio management and honest recording intelligence."
        ),
        OnboardingStep(
            titleRes = R.string.onboarding_capabilities_title,
            descRes = R.string.onboarding_capabilities_desc,
            icon = Icons.Default.Mic,
            detailText = "Distinguishes reliably between two-sided calls, microphone input, and system-authorized audio."
        ),
        OnboardingStep(
            titleRes = R.string.onboarding_limitations_title,
            descRes = R.string.onboarding_limitations_desc,
            icon = Icons.Default.Security,
            detailText = "Zero fake recordings. We never pretend empty files represent successful captures."
        ),
        OnboardingStep(
            titleRes = R.string.onboarding_privacy_title,
            descRes = R.string.onboarding_privacy_desc,
            icon = Icons.Default.Lock,
            detailText = "Android microphone privacy indicators always remain visible. Legal consent is mandatory."
        ),
        OnboardingStep(
            titleRes = R.string.onboarding_storage_title,
            descRes = R.string.onboarding_storage_desc,
            icon = Icons.Default.Security,
            detailText = "Local application-private storage with hardware-backed AES-256-GCM encryption."
        ),
        OnboardingStep(
            titleRes = R.string.onboarding_check_title,
            descRes = R.string.onboarding_check_desc,
            icon = Icons.Default.Speed,
            detailText = "Audits your hardware, Android version, and VoIP policies for transparent recording capability."
        )
    )

    val step = steps[currentStep]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .testTag("onboarding_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top row with skip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CallMaster",
                style = MaterialTheme.typography.titleMedium,
                color = RadiantGold,
                fontWeight = FontWeight.Bold
            )
            if (currentStep < steps.size - 1) {
                TextButton(
                    onClick = {
                        preferences.hasCompletedOnboarding = true
                        onFinish()
                    },
                    modifier = Modifier.testTag("onboarding_skip_btn")
                ) {
                    Text(stringResource(R.string.btn_skip), color = RadiantGold)
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        // Center Animated Content
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding_step"
        ) { targetIndex ->
            val cur = steps[targetIndex]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(PetroleumGreen.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = cur.icon,
                        contentDescription = null,
                        tint = RadiantGold,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = stringResource(cur.titleRes),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(cur.descRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = cur.detailText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LightGold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // Bottom progress & navigation
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Step dots
            Row(horizontalArrangement = Arrangement.Center) {
                steps.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(if (index == currentStep) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (index == currentStep) RadiantGold else PetroleumGreen.copy(alpha = 0.5f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (currentStep < steps.size - 1) {
                Button(
                    onClick = {
                        // Request permissions when reaching step 4 (microphone privacy)
                        if (currentStep == 3) {
                            val perms = mutableListOf(
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.READ_PHONE_STATE,
                                Manifest.permission.READ_CALL_LOG,
                                Manifest.permission.READ_CONTACTS
                            )
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionLauncher.launch(perms.toTypedArray())
                        }
                        currentStep++
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RadiantGold,
                        contentColor = DarkGreen
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("onboarding_next_btn")
                ) {
                    Text(stringResource(R.string.btn_next), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Button(
                    onClick = {
                        val perms = mutableListOf(
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.READ_CALL_LOG,
                            Manifest.permission.READ_CONTACTS
                        )
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            perms.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionLauncher.launch(perms.toTypedArray())
                        preferences.hasCompletedOnboarding = true
                        onFinish()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RadiantGold,
                        contentColor = DarkGreen
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("onboarding_finish_btn")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_get_started), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
