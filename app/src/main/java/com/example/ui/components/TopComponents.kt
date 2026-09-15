package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun BypassTopBar(
    onSettingsClick: () -> Unit,
    onRunClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Placeholder for logo
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PurpleAccent)
            ) {
                Text(
                    text = "B",
                    color = PrimaryTextColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "BYPASS IDE",
                color = PrimaryTextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariantColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "$", color = MutedTextColor, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "SHELL", color = MutedTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E3A3A)) // slightly green tint background
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(GreenStatus))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = ": 8080", color = GreenStatus, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onRunClick, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = CyanAccent)
            }
            IconButton(onClick = onSettingsClick, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MutedTextColor)
            }
        }
    }
}

@Composable
fun RootStatusBanner(
    onGrantRoot: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF332B00)) // Yellow-ish dark background
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Warning", tint = YellowWarning, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Standard mode active. Grant root for full POSIX & Termux execution.",
                color = YellowWarning,
                fontSize = 12.sp
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Grant Root",
                color = CyanAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onGrantRoot() }
                    .padding(horizontal = 8.dp)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(16.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MutedTextColor)
            }
        }
    }
}
