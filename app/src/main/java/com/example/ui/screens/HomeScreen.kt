package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiMessage
import com.example.ai.AiSession
import com.example.ai.ActionParser
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun HomeScreen(onNavigateToBuilder: () -> Unit = {}) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val aiSession = remember { 
         AppState.aiSession ?: AiSession(context).also { AppState.aiSession = it }
    }
    
    var promptText by remember { mutableStateOf("") }
    val provider = aiSession.provider

    fun sendMessage() {
        if (promptText.isBlank()) return
        val text = promptText
        promptText = ""
        
        aiSession.chatHistory.add(AiMessage(text, isUser = true))
        aiSession.chatHistory.add(AiMessage("", isUser = false, isLoading = true))
        
        coroutineScope.launch {
            val bypassProjectsRoot = File(context.filesDir, "BypassProjects").apply { mkdirs() }
            val projectDir = AppState.currentFile?.parentFile ?: bypassProjectsRoot
            
            val projectContext = ActionParser.getProjectContext(projectDir)
            val response = provider.sendMessage(text, aiSession.chatHistory.toList(), projectContext)
            
            // Replace loading with response
            aiSession.chatHistory.removeAt(aiSession.chatHistory.size - 1)
            
            if (response.isError) {
                 aiSession.chatHistory.add(AiMessage(response.text, isUser = false, isError = true))
            } else {
                 val parsedActions = ActionParser.parseActions(response.text)
                 var actionResults = ""
                 if (parsedActions.isNotEmpty()) {
                     for (action in parsedActions) {
                         val res = ActionParser.executeAction(action, projectDir)
                         actionResults += "$res\n"
                     }
                     AppState.fileUpdateTrigger++
                 }
                 
                 val displayText = if (actionResults.isNotEmpty()) {
                     "${response.text}\n\nExecution Results:\n$actionResults"
                 } else {
                     response.text
                 }
                 aiSession.chatHistory.add(AiMessage(displayText, isUser = false))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // Chat History / Main Content
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                // Welcome Section
                Text("Welcome back, Root", color = PrimaryTextColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Dashboard Elements
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(CyanAccent).padding(16.dp).clickable { onNavigateToBuilder() }) {
                        Text("Start Coding >", color = BgColor, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Recent Projects", color = MutedTextColor, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceColor).padding(16.dp).clickable { onNavigateToBuilder() }) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("RajTest", color = PrimaryTextColor, fontWeight = FontWeight.Bold)
                        Text("2 mins ago", color = MutedTextColor, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Chat Assistant", color = MutedTextColor, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (aiSession.chatHistory.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Architect full-stack apps, write root utilities, and execute direct OS commands autonomously.",
                            color = MutedTextColor,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                items(aiSession.chatHistory) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (msg.isUser) SurfaceColor else Color.Transparent)
                                .border(1.dp, if (msg.isUser) SurfaceVariantColor else Color.Transparent, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .widthIn(max = 300.dp)
                        ) {
                            if (msg.isLoading) {
                                Text("Thinking...", color = PurpleAccent, fontSize = 14.sp)
                            } else {
                                Text(msg.text, color = PrimaryTextColor, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // Bottom Input Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .border(1.dp, SurfaceVariantColor, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(SurfaceColor)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    placeholder = { Text("Build apps, create websites, or write code...", color = MutedTextColor) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = PrimaryTextColor,
                        unfocusedTextColor = PrimaryTextColor
                    )
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { /* Voice Diagnostics UI could be triggered here */ },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceVariantColor)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice", tint = CyanAccent)
                    }
                    IconButton(
                        onClick = { sendMessage() },
                        enabled = promptText.isNotBlank(),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (promptText.isNotBlank()) CyanAccent else SurfaceVariantColor)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = if (promptText.isNotBlank()) BgColor else MutedTextColor)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgColor)
                        .border(1.dp, SurfaceVariantColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(provider.name, color = PrimaryTextColor, fontSize = 12.sp)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgColor)
                        .border(1.dp, SurfaceVariantColor, RoundedCornerShape(12.dp))
                        .clickable { onNavigateToBuilder() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("< >", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Builder", color = PrimaryTextColor, fontSize = 12.sp)
                }
            }
        }
    }
}
