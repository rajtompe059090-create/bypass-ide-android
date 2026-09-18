package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

@Composable
fun TerminalScreen() {
    val context = LocalContext.current
    var command by remember { mutableStateOf("") }
    val history = remember { mutableStateListOf<String>(
        "[SYSTEM] Bypass IDE Root Subsystem Initialized (Mock)",
        "[WORKSPACE] Standard Mode",
        "$ "
    ) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    val projectDir = AppState.currentFile?.parentFile ?: File(context.filesDir, "BypassProjects")

    fun executeCommand(cmd: String) {
        if (cmd.isBlank()) return
        history.add("$ $cmd")
        command = ""
        coroutineScope.launch {
            try {
                val output = withContext(Dispatchers.IO) {
                    val process = ProcessBuilder()
                        .command("sh", "-c", cmd)
                        .directory(projectDir)
                        .redirectErrorStream(true)
                        .start()
                    
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    val result = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        result.append(line).append("\n")
                    }
                    process.waitFor()
                    result.toString()
                }
                if (output.isNotBlank()) {
                    history.add(output.trimEnd())
                }
            } catch (e: Exception) {
                history.add("Error: ${e.message}")
            }
            listState.animateScrollToItem(history.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {
        // Toolbar
        Row(
            modifier = Modifier.fillMaxWidth().background(SurfaceColor).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Terminal, contentDescription = "Terminal", tint = CyanAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("TERMINAL", color = PrimaryTextColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("STANDARD MODE", color = YellowWarning, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.border(1.dp, YellowWarning, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp).clickable { history.clear() }) {
                    Text("Clear", color = YellowWarning, fontSize = 12.sp)
                }
            }
        }

        // Quick Commands
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val cmds = listOf("ls -la", "pwd", "node -v", "python3 --version", "git status")
            cmds.forEach { cmd ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, SurfaceVariantColor, RoundedCornerShape(12.dp))
                        .clickable { executeCommand(cmd) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(cmd, color = CyanAccent, fontSize = 12.sp)
                }
            }
        }

        // Output Area
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
            state = listState
        ) {
            items(history) { line ->
                Text(line, color = PrimaryTextColor, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            }
        }

        // Input Area
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$", color = YellowWarning, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(16.dp))
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SurfaceVariantColor,
                    unfocusedBorderColor = SurfaceVariantColor,
                    focusedContainerColor = SurfaceColor,
                    unfocusedContainerColor = SurfaceColor,
                    focusedTextColor = PrimaryTextColor,
                    unfocusedTextColor = PrimaryTextColor
                ),
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(
                onClick = { executeCommand(command) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariantColor)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Send", tint = MutedTextColor)
            }
        }
    }
}
