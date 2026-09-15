package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun DevScreen() {
    Column(modifier = Modifier.fillMaxSize().background(BgColor).padding(16.dp)) {
        Text("DEVELOPER / PROFILE", color = CyanAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceColor).padding(16.dp)) {
            Column {
                Text("Verification Status", color = PrimaryTextColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Status: UNVERIFIED DEMO", color = YellowWarning)
                Text("No root cryptographically sealed profile found.", color = MutedTextColor, fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceColor).padding(16.dp)) {
            Column {
                Text("Telegram Sync", color = PrimaryTextColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Status: DISCONNECTED", color = YellowWarning)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceColor).padding(16.dp)) {
            Column {
                Text("Client Type", color = PrimaryTextColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Status: LOCAL / FREE", color = YellowWarning)
            }
        }
    }
}
