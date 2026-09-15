package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.io.File
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextRange

object AppState {
    var currentFile: File? by mutableStateOf(null)
    var aiSession: com.example.ai.AiSession? = null
    var fileUpdateTrigger by mutableStateOf(0)
}

@Composable
fun FilesScreen(onNavigateToEditor: () -> Unit) {
    val context = LocalContext.current
    val rootDir = remember { AppState.aiSession?.workspace ?: File(context.getExternalFilesDir(null), "BypassProjects").apply { mkdirs() } }
    var currentDir by remember { mutableStateOf(rootDir) }
    var files by remember { mutableStateOf(currentDir.listFiles()?.toList()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()) }
    
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }

    fun refreshFiles() {
        if (!currentDir.exists()) currentDir = rootDir
        files = currentDir.listFiles()?.toList()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
        android.util.Log.d("BypassIDE", "FilesScreen refreshed. Showing \${files.size} items in \${currentDir.absolutePath}")
    }

    LaunchedEffect(AppState.fileUpdateTrigger) {
        refreshFiles()
    }

    if (showNewFileDialog || showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { 
                showNewFileDialog = false
                showNewFolderDialog = false
                newItemName = "" 
            },
            title = { Text(if (showNewFileDialog) "New File" else "New Folder", color = PrimaryTextColor) },
            text = {
                OutlinedTextField(
                    value = newItemName,
                    onValueChange = { newItemName = it },
                    placeholder = { Text("Name", color = MutedTextColor) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryTextColor,
                        unfocusedTextColor = PrimaryTextColor
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newItemName.isNotBlank()) {
                        try {
                            val newFile = File(currentDir, newItemName)
                            if (showNewFileDialog) {
                                newFile.createNewFile()
                            } else {
                                newFile.mkdirs()
                            }
                            refreshFiles()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    showNewFileDialog = false
                    showNewFolderDialog = false
                    newItemName = ""
                }) {
                    Text("Create", color = CyanAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showNewFileDialog = false
                    showNewFolderDialog = false
                    newItemName = "" 
                }) {
                    Text("Cancel", color = MutedTextColor)
                }
            },
            containerColor = SurfaceColor,
            titleContentColor = PrimaryTextColor
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {
        // Top Path Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E3A3A))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentDir != rootDir) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, 
                        contentDescription = "Up", 
                        tint = CyanAccent, 
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                currentDir.parentFile?.let {
                                    if (it.absolutePath.startsWith(rootDir.absolutePath)) {
                                        currentDir = it
                                        refreshFiles()
                                    }
                                }
                            }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(currentDir.name, color = CyanAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = CyanAccent, modifier = Modifier.size(20.dp).clickable { refreshFiles() })
                Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "New File", tint = MutedTextColor, modifier = Modifier.size(20.dp).clickable { showNewFileDialog = true })
                Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", tint = MutedTextColor, modifier = Modifier.size(20.dp).clickable { showNewFolderDialog = true })
            }
        }

        Text(
            text = "Path: ${currentDir.absolutePath}",
            color = MutedTextColor,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (files.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Empty folder", color = MutedTextColor)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files) { file ->
                    val isDir = file.isDirectory
                    val ext = if (isDir) "DIR" else file.extension.uppercase().take(4).ifEmpty { "FILE" }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceColor)
                            .clickable {
                                if (isDir) {
                                    currentDir = file
                                    refreshFiles()
                                } else {
                                    AppState.currentFile = file
                                    onNavigateToEditor()
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isDir) {
                                Icon(Icons.Default.Folder, contentDescription = "Folder", tint = CyanAccent, modifier = Modifier.size(24.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .border(1.dp, MutedTextColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(ext, color = MutedTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = file.name, color = if (isDir) CyanAccent else PrimaryTextColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (!isDir) {
                                    Text("${file.length()} B", color = MutedTextColor, fontSize = 10.sp)
                                }
                            }
                        }
                        
                        var menuExpanded by remember { mutableStateOf(false) }
                        var showRenameDialog by remember { mutableStateOf(false) }
                        var showDeleteDialog by remember { mutableStateOf(false) }
                        
                        Box {
                            Icon(
                                Icons.Default.MoreVert, 
                                contentDescription = "More", 
                                tint = MutedTextColor,
                                modifier = Modifier.clickable { menuExpanded = true }.padding(8.dp)
                            )
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(SurfaceColor)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Rename", color = PrimaryTextColor) },
                                    onClick = { menuExpanded = false; showRenameDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete", color = YellowWarning) },
                                    onClick = { menuExpanded = false; showDeleteDialog = true }
                                )
                            }
                        }

                        if (showRenameDialog) {
                            var newName by remember { mutableStateOf(file.name) }
                            AlertDialog(
                                onDismissRequest = { showRenameDialog = false },
                                title = { Text("Rename", color = PrimaryTextColor) },
                                text = {
                                    OutlinedTextField(
                                        value = newName,
                                        onValueChange = { newName = it },
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PrimaryTextColor, unfocusedTextColor = PrimaryTextColor)
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        if (newName.isNotBlank() && newName != file.name) {
                                            file.renameTo(File(file.parentFile, newName))
                                            refreshFiles()
                                        }
                                        showRenameDialog = false
                                    }) { Text("Rename", color = CyanAccent) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showRenameDialog = false }) { Text("Cancel", color = MutedTextColor) }
                                },
                                containerColor = SurfaceColor,
                                titleContentColor = PrimaryTextColor
                            )
                        }

                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                title = { Text("Delete", color = YellowWarning) },
                                text = { Text("Are you sure you want to delete ${file.name}?", color = PrimaryTextColor) },
                                confirmButton = {
                                    TextButton(onClick = {
                                        if (file.isDirectory) file.deleteRecursively() else file.delete()
                                        refreshFiles()
                                        showDeleteDialog = false
                                    }) { Text("Delete", color = YellowWarning) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel", color = MutedTextColor) }
                                },
                                containerColor = SurfaceColor,
                                titleContentColor = PrimaryTextColor
                            )
                        }
                    }
                }
            }
        }
    }
}

class SyntaxHighlightTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val inputText = text.text
        val annotatedString = buildAnnotatedString {
            append(inputText)
            
            // Very basic keyword highlighting
            val keywords = listOf("val", "var", "fun", "class", "import", "package", "if", "else", "for", "while", "return", "true", "false", "null", "function", "const", "let", "<html>", "<body>", "<div>")
            
            // Colors
            val keywordColor = Color(0xFFC678DD) // Purple
            val stringColor = Color(0xFF98C379) // Green
            val numberColor = Color(0xFFD19A66) // Orange
            val commentColor = Color(0xFF5C6370) // Gray
            
            // Highlight keywords
            val keywordRegex = "\\b(${keywords.joinToString("|")})\\b".toRegex()
            keywordRegex.findAll(inputText).forEach { matchResult ->
                addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold), matchResult.range.first, matchResult.range.last + 1)
            }
            
            // Highlight strings (very basic)
            val stringRegex = "\".*?\"".toRegex()
            stringRegex.findAll(inputText).forEach { matchResult ->
                addStyle(SpanStyle(color = stringColor), matchResult.range.first, matchResult.range.last + 1)
            }
            
            // Highlight numbers
            val numberRegex = "\\b\\d+\\b".toRegex()
            numberRegex.findAll(inputText).forEach { matchResult ->
                addStyle(SpanStyle(color = numberColor), matchResult.range.first, matchResult.range.last + 1)
            }
            
            // Highlight comments //
            val commentRegex = "//.*".toRegex()
            commentRegex.findAll(inputText).forEach { matchResult ->
                addStyle(SpanStyle(color = commentColor), matchResult.range.first, matchResult.range.last + 1)
            }
        }
        
        return androidx.compose.ui.text.input.TransformedText(annotatedString, OffsetMapping.Identity)
    }
}

@Composable
fun EditorScreen() {
    val file = AppState.currentFile
    var textValue by remember(file) {
        val initialText = if (file != null && file.exists()) {
            try { file.readText() } catch (e: Exception) { "Error reading file" }
        } else ""
        mutableStateOf(TextFieldValue(initialText))
    }
    var isDirty by remember(file) { mutableStateOf(false) }

    var undoStack by remember(file) { mutableStateOf(listOf<TextFieldValue>()) }
    var redoStack by remember(file) { mutableStateOf(listOf<TextFieldValue>()) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }

    fun updateText(newValue: TextFieldValue) {
        if (newValue.text != textValue.text) {
            undoStack = undoStack + textValue
            redoStack = emptyList()
            isDirty = true
        }
        textValue = newValue
    }

    fun findNext() {
        if (searchQuery.isNotEmpty()) {
            val idx = textValue.text.indexOf(searchQuery, textValue.selection.max, ignoreCase = true)
            if (idx >= 0) {
                textValue = textValue.copy(selection = TextRange(idx, idx + searchQuery.length))
            } else {
                val firstIdx = textValue.text.indexOf(searchQuery, 0, ignoreCase = true)
                if (firstIdx >= 0) {
                    textValue = textValue.copy(selection = TextRange(firstIdx, firstIdx + searchQuery.length))
                }
            }
        }
    }

    fun replace() {
        if (searchQuery.isNotEmpty() && textValue.selection.length == searchQuery.length) {
            val selectedText = textValue.text.substring(textValue.selection.min, textValue.selection.max)
            if (selectedText.equals(searchQuery, ignoreCase = true)) {
                val newText = textValue.text.substring(0, textValue.selection.min) + replaceQuery + textValue.text.substring(textValue.selection.max)
                updateText(TextFieldValue(newText, TextRange(textValue.selection.min, textValue.selection.min + replaceQuery.length)))
                findNext()
            }
        } else {
            findNext()
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack = redoStack + textValue
            textValue = undoStack.last()
            undoStack = undoStack.dropLast(1)
            isDirty = true
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack = undoStack + textValue
            textValue = redoStack.last()
            redoStack = redoStack.dropLast(1)
            isDirty = true
        }
    }

    fun save() {
        if (file != null) {
            try {
                file.writeText(textValue.text)
                isDirty = false
                AppState.fileUpdateTrigger++
            } catch (e: Exception) {
                // Ignore for now
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {
        // Tab bar
        Row(
            modifier = Modifier.fillMaxWidth().background(SurfaceColor).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, CyanAccent, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(if (isDirty) YellowWarning else CyanAccent))
                Spacer(modifier = Modifier.width(8.dp))
                Text("${file?.name ?: "No file"} ${if (isDirty) "*" else ""}", color = PrimaryTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Toolbar
        Row(
            modifier = Modifier.fillMaxWidth().background(SurfaceVariantColor).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val ext = file?.extension?.uppercase()?.take(6)?.ifEmpty { "FILE" } ?: "FILE"
            Text(ext, color = CyanAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = if (isSearchVisible) CyanAccent else MutedTextColor, modifier = Modifier.size(16.dp).clickable { isSearchVisible = !isSearchVisible })
                Icon(Icons.Default.Save, contentDescription = "Save", tint = if (isDirty) CyanAccent else MutedTextColor, modifier = Modifier.size(16.dp).clickable(enabled = isDirty) { save() })
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = if (undoStack.isNotEmpty()) CyanAccent else MutedTextColor, modifier = Modifier.size(16.dp).clickable(enabled = undoStack.isNotEmpty()) { undo() })
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = if (redoStack.isNotEmpty()) CyanAccent else MutedTextColor, modifier = Modifier.size(16.dp).clickable(enabled = redoStack.isNotEmpty()) { redo() })
            }
        }

        if (isSearchVisible) {
            Column(modifier = Modifier.fillMaxWidth().background(SurfaceColor).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f).height(48.dp),
                        placeholder = { Text("Find...", color = MutedTextColor, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PrimaryTextColor, unfocusedTextColor = PrimaryTextColor),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { findNext() }, modifier = Modifier.height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantColor)) {
                        Text("Find", color = CyanAccent, fontSize = 12.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = replaceQuery,
                        onValueChange = { replaceQuery = it },
                        modifier = Modifier.weight(1f).height(48.dp),
                        placeholder = { Text("Replace...", color = MutedTextColor, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PrimaryTextColor, unfocusedTextColor = PrimaryTextColor),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { replace() }, modifier = Modifier.height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantColor)) {
                        Text("Replace", color = CyanAccent, fontSize = 12.sp)
                    }
                }
            }
        }

        // Editor Content
        Row(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
            val lines = textValue.text.count { it == '\n' } + 1
            // Line numbers
            Column(modifier = Modifier.width(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.End) {
                for (i in 1..lines) {
                    Text("$i", color = MutedTextColor, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            // Code
            Box(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
                BasicTextField(
                    value = textValue,
                    onValueChange = ::updateText,
                    textStyle = TextStyle(color = PrimaryTextColor, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(CyanAccent),
                    visualTransformation = SyntaxHighlightTransformation(),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Bottom Symbol Toolbar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val symbols = listOf("TAB", "{}", "()", "[]", "<>", "\"\"", "''")
            symbols.forEach { symbol ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceVariantColor)
                        .clickable {
                            val toInsert = if (symbol == "TAB") "    " else symbol
                            val newText = textValue.text.substring(0, textValue.selection.min) + toInsert + textValue.text.substring(textValue.selection.max)
                            val newCursorPos = textValue.selection.min + (if (symbol.length == 2 && symbol != "() ") 1 else toInsert.length)
                            updateText(TextFieldValue(newText, TextRange(newCursorPos)))
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(symbol, color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Status bar
        Row(
            modifier = Modifier.fillMaxWidth().background(SurfaceColor).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val currentLine = textValue.text.substring(0, textValue.selection.start).count { it == '\n' } + 1
            val currentCol = textValue.selection.start - textValue.text.lastIndexOf('\n', textValue.selection.start - 1)
            val linesCount = textValue.text.count { it == '\n' } + 1
            Text("Ln $currentLine, Col $currentCol · $linesCount lines", color = MutedTextColor, fontSize = 10.sp)
            Text("UTF-8  Wrap: OFF  14sp", color = MutedTextColor, fontSize = 10.sp)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1E3A3A))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                val ext2 = file?.extension?.uppercase()?.take(6)?.ifEmpty { "FILE" } ?: "FILE"
                Text(ext2, color = CyanAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


