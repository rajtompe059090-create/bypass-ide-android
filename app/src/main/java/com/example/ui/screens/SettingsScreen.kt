package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ai.AiSession
import com.example.ai.AiProviderState

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val aiSession = remember { 
         AppState.aiSession ?: AiSession(context).also { AppState.aiSession = it }
    }
    
    var apiKeyInput by remember { mutableStateOf(aiSession.apiKey) }
    var selectedProviderState by remember { mutableStateOf(aiSession.selectedProviderState) }

    Column(modifier = Modifier.fillMaxSize().background(BgColor).padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("SETTINGS", color = CyanAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        // AI Model
        Text("AI Configuration", color = PrimaryTextColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceColor).padding(16.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedProviderState == AiProviderState.MOCK,
                        onClick = {
                            selectedProviderState = AiProviderState.MOCK
                            aiSession.selectedProviderState = AiProviderState.MOCK
                        },
                        colors = RadioButtonDefaults.colors(selectedColor = CyanAccent)
                    )
                    Text("Mock AI Provider (Offline)", color = PrimaryTextColor)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedProviderState == AiProviderState.GEMINI,
                        onClick = {
                            selectedProviderState = AiProviderState.GEMINI
                            aiSession.selectedProviderState = AiProviderState.GEMINI
                        },
                        colors = RadioButtonDefaults.colors(selectedColor = CyanAccent)
                    )
                    Text("GEMINI 3.8 FLASH HIGH (UNLIMITED)", color = PrimaryTextColor, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Gemini API Key:", color = PrimaryTextColor)
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { 
                         apiKeyInput = it
                         aiSession.apiKey = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PrimaryTextColor, unfocusedTextColor = PrimaryTextColor)
                )
                Text("A real provider requires connecting a valid API key.", color = MutedTextColor, fontSize = 12.sp)
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Reasoning:", color = PrimaryTextColor, fontWeight = FontWeight.Bold)
                Text("High — 12k\nMedium — 6k\nLow — 2k\nOff — 0k", color = MutedTextColor, fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // IDE Settings
        Text("IDE Settings", color = PrimaryTextColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        var dynamicIsland by remember { mutableStateOf(false) }
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceColor).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Dynamic Island Top Capsule", color = PrimaryTextColor)
            Switch(checked = dynamicIsland, onCheckedChange = { dynamicIsland = it }, colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent, checkedTrackColor = SurfaceVariantColor))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        var backgroundService by remember { mutableStateOf(false) }
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceColor).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Background Assistant Service", color = PrimaryTextColor)
            Switch(checked = backgroundService, onCheckedChange = { backgroundService = it }, colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent, checkedTrackColor = SurfaceVariantColor))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        var searchGrounding by remember { mutableStateOf(false) }
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceColor).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Live Web Search Grounding", color = PrimaryTextColor)
            Switch(checked = searchGrounding, onCheckedChange = { searchGrounding = it }, colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent, checkedTrackColor = SurfaceVariantColor))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("System", color = PrimaryTextColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceColor).padding(16.dp)) {
            Text("IP & NETWORK RESET", color = PrimaryTextColor)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { /* mock reset */ }, colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantColor)) {
                Text("Reset IP", color = CyanAccent)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("VOICE DIAGNOSTICS & ERROR LOGS", color = PrimaryTextColor)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { /* mock reset */ }, colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantColor)) {
                Text("View Logs", color = CyanAccent)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
