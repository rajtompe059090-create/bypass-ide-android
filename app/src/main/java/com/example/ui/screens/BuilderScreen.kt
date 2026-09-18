package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.io.File

@Composable
fun BuilderScreen() {
    val context = LocalContext.current
    val rootDir = remember { File(context.filesDir, "BypassProjects").apply { mkdirs() } }
    
    var progressText by remember { mutableStateOf("Initializing Build Workspace...\nAnalyzing project context...") }
    var generatedFiles by remember { mutableStateOf(listOf<File>()) }
    var currentPreviewFile by remember { mutableStateOf<File?>(null) }
    
    LaunchedEffect(AppState.fileUpdateTrigger) {
        val files = rootDir.listFiles()?.toList() ?: emptyList()
        generatedFiles = files
        if (currentPreviewFile == null && files.isNotEmpty()) {
            currentPreviewFile = files.firstOrNull { !it.isDirectory }
        }
        progressText += "\nDetected ${files.size} objects in workspace."
    }

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {
        // Top Toolbar
        Row(
            modifier = Modifier.fillMaxWidth().background(SurfaceColor).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("BUILDER", color = CyanAccent, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(SurfaceVariantColor).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("ACTIVE", color = GreenStatus, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { /* Run/Build */ }, colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = BgColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run", color = BgColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // File Tree
            Column(modifier = Modifier.weight(0.3f).fillMaxHeight().background(SurfaceVariantColor).padding(8.dp)) {
                Text("PROJECT FILES", color = MutedTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(generatedFiles) { file ->
                        Text(
                            text = file.name,
                            color = if (file == currentPreviewFile) CyanAccent else PrimaryTextColor,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { currentPreviewFile = file }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
            
            // Editor / Active File View
            Column(modifier = Modifier.weight(0.7f).fillMaxHeight().background(BgColor)) {
                Row(modifier = Modifier.fillMaxWidth().background(SurfaceColor).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(currentPreviewFile?.name ?: "No file selected", color = PrimaryTextColor, fontSize = 12.sp)
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
                    Text(
                        text = currentPreviewFile?.readText() ?: "Select a file to view code",
                        color = MutedTextColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
                // Terminal / Output
                Column(modifier = Modifier.height(150.dp).fillMaxWidth().background(SurfaceVariantColor).padding(8.dp)) {
                    Text("TERMINAL OUTPUT", color = MutedTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(progressText, color = GreenStatus, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }
        }
    }
}
