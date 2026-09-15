package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.ai.AiProviderState
import com.example.ai.AiSession
import com.example.ui.theme.*

@Composable
fun SettingsScreen() {

    val context = LocalContext.current

    val aiSession = remember {
        AppState.aiSession
            ?: AiSession(context).also {
                AppState.aiSession = it
            }
    }

    var apiKey by remember {
        mutableStateOf(aiSession.apiKey)
    }

    var geminiEnabled by remember {
        mutableStateOf(
            aiSession.selectedProviderState ==
                AiProviderState.GEMINI
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(16.dp)
    ) {

        Text(
            "SETTINGS",
            color = CyanAccent,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "GEMINI AI",
            color = PrimaryTextColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    SurfaceColor,
                    RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    "Use Gemini AI",
                    color = PrimaryTextColor
                )

                Switch(
                    checked = geminiEnabled,
                    onCheckedChange = { enabled ->

                        geminiEnabled = enabled

                        if (enabled) {
                            aiSession.activateGemini(apiKey)
                        } else {
                            aiSession.activateMock()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyanAccent
                    )
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Gemini API Key",
                color = PrimaryTextColor,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    aiSession.apiKey = it

                    if (it.isNotBlank()) {
                        geminiEnabled = true
                        aiSession.selectedProviderState =
                            AiProviderState.GEMINI
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text(
                        "Paste Gemini API key",
                        color = MutedTextColor
                    )
                },
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryTextColor,
                        unfocusedTextColor = PrimaryTextColor,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor =
                            SurfaceVariantColor
                    )
            )

            Spacer(Modifier.height(8.dp))

            Text(
                if (aiSession.apiKey.isNotBlank())
                    "✓ Gemini API key saved"
                else
                    "Add your Gemini API key to enable real AI.",
                color = if (aiSession.apiKey.isNotBlank())
                    CyanAccent
                else
                    MutedTextColor,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "IDE SETTINGS",
            color = PrimaryTextColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        var reasoning by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    SurfaceColor,
                    RoundedCornerShape(10.dp)
                )
                .padding(16.dp),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Text(
                "Reasoning / Thinking",
                color = PrimaryTextColor
            )

            Switch(
                checked = reasoning,
                onCheckedChange = {
                    reasoning = it
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        var grounding by remember {
            mutableStateOf(false)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    SurfaceColor,
                    RoundedCornerShape(10.dp)
                )
                .padding(16.dp),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Text(
                "Live Web Search Grounding",
                color = PrimaryTextColor
            )

            Switch(
                checked = grounding,
                onCheckedChange = {
                    grounding = it
                }
            )
        }
    }
}
