package com.example.ai

import com.example.ui.screens.AppState
import android.util.Log
import java.io.File

object ActionParser {
    data class ParsedAction(
        val type: ActionType,
        val path: String?,
        val content: String?
    )

    enum class ActionType {
        CREATE_FILE, EDIT_FILE, APPEND_FILE, CREATE_DIRECTORY, READ_FILE, LIST_FILES, RUN_SAFE_COMMAND, UNKNOWN
    }

    fun parseActions(response: String): List<ParsedAction> {
        val actions = mutableListOf<ParsedAction>()
        
        // Parse <file path="...">...</file>
        val fileRegex = "<file\\s+path=\"([^\"]+)\">([\\s\\S]*?)</file>".toRegex()
        fileRegex.findAll(response).forEach { matchResult ->
            actions.add(ParsedAction(ActionType.CREATE_FILE, matchResult.groupValues[1], matchResult.groupValues[2]))
        }
        
        // Parse <edit_file path="...">...</edit_file>
        val editFileRegex = "<edit_file\\s+path=\"([^\"]+)\">([\\s\\S]*?)</edit_file>".toRegex()
        editFileRegex.findAll(response).forEach { matchResult ->
            actions.add(ParsedAction(ActionType.EDIT_FILE, matchResult.groupValues[1], matchResult.groupValues[2]))
        }
        
        // Parse <append_file path="...">...</append_file>
        val appendFileRegex = "<append_file\\s+path=\"([^\"]+)\">([\\s\\S]*?)</append_file>".toRegex()
        appendFileRegex.findAll(response).forEach { matchResult ->
            actions.add(ParsedAction(ActionType.APPEND_FILE, matchResult.groupValues[1], matchResult.groupValues[2]))
        }
        
        // Parse <mkdir path="..."/>
        val mkdirRegex = "<mkdir\\s+path=\"([^\"]+)\"\\s*/>".toRegex()
        mkdirRegex.findAll(response).forEach { matchResult ->
            actions.add(ParsedAction(ActionType.CREATE_DIRECTORY, matchResult.groupValues[1], null))
        }
        
        // Parse <command>...</command>
        val cmdRegex = "<command>([\\s\\S]*?)</command>".toRegex()
        cmdRegex.findAll(response).forEach { matchResult ->
            actions.add(ParsedAction(ActionType.RUN_SAFE_COMMAND, null, matchResult.groupValues[1].trim()))
        }
        
        return actions
    }

    fun executeAction(action: ParsedAction, projectRoot: File): String {
        Log.d("BypassIDE", "executeAction called: \${action.type} in root \${projectRoot.absolutePath}")
        return try {
            when (action.type) {
                ActionType.CREATE_FILE, ActionType.EDIT_FILE -> {
                    if (action.path != null && action.content != null) {
                        Log.d("BypassIDE", "AI action path requested: \${action.path}")
                        var safePath = action.path.trim()
                        if (safePath.startsWith("/")) safePath = safePath.removePrefix("/")
                        if (safePath.startsWith("app/")) safePath = safePath.removePrefix("app/")
                        
                        val file = File(projectRoot, safePath).canonicalFile
                        Log.d("BypassIDE", "Resolved physical file path: \${file.absolutePath}")
                        if (!file.path.startsWith(projectRoot.canonicalPath)) {
                            Log.d("BypassIDE", "Failed: Path escapes project root.")
                            return "Failed: Path escapes project root. Attempted: \${file.path}"
                        }
                        file.parentFile?.mkdirs()
                        file.writeText(action.content)
                        Log.d("BypassIDE", "File creation result: Success")
                        if (action.type == ActionType.CREATE_FILE) "Created file: \$safePath (at \${file.absolutePath})" else "Edited file: \$safePath (at \${file.absolutePath})"
                    } else {
                        Log.d("BypassIDE", "Failed to create/edit file: Missing path or content")
                        "Failed to create/edit file: Missing path or content"
                    }
                }
                ActionType.APPEND_FILE -> {
                    if (action.path != null && action.content != null) {
                        Log.d("BypassIDE", "AI action path requested: \${action.path}")
                        var safePath = action.path.trim()
                        if (safePath.startsWith("/")) safePath = safePath.removePrefix("/")
                        if (safePath.startsWith("app/")) safePath = safePath.removePrefix("app/")
                        
                        val file = File(projectRoot, safePath).canonicalFile
                        Log.d("BypassIDE", "Resolved physical file path: \${file.absolutePath}")
                        if (!file.path.startsWith(projectRoot.canonicalPath)) return "Failed: Path escapes project root. Attempted: \${file.path}"
                        file.parentFile?.mkdirs()
                        file.appendText(action.content)
                        "Appended to file: \$safePath (at \${file.absolutePath})"
                    } else "Failed to append to file: Missing path or content"
                }
                ActionType.CREATE_DIRECTORY -> {
                    if (action.path != null) {
                        Log.d("BypassIDE", "AI action path requested: \${action.path}")
                        var safePath = action.path.trim()
                        if (safePath.startsWith("/")) safePath = safePath.removePrefix("/")
                        if (safePath.startsWith("app/")) safePath = safePath.removePrefix("app/")
                        
                        val dir = File(projectRoot, safePath).canonicalFile
                        Log.d("BypassIDE", "Resolved physical dir path: \${dir.absolutePath}")
                        if (!dir.path.startsWith(projectRoot.canonicalPath)) return "Failed: Path escapes project root. Attempted: \${dir.path}"
                        dir.mkdirs()
                        "Created directory: \$safePath (at \${dir.absolutePath})"
                    } else "Failed to create directory: Missing path"
                }
                ActionType.RUN_SAFE_COMMAND -> {
                    if (action.content != null) {
                        val process = ProcessBuilder()
                            .command("sh", "-c", action.content)
                            .directory(projectRoot)
                            .redirectErrorStream(true)
                            .start()
                        
                        val output = process.inputStream.bufferedReader().readText()
                        process.waitFor()
                        "Executed: \${action.content}\nOutput: \$output"
                    } else "Failed to execute command: Missing content"
                }
                else -> "Unknown or unsupported action type: \${action.type}"
            }
        } catch (e: Exception) {
            "Error executing action \${action.type}: \${e.message}"
        }
    }

    fun getProjectContext(projectRoot: File?): String {
        if (projectRoot == null || !projectRoot.exists()) return "No project loaded."
        
        val sb = java.lang.StringBuilder()
        sb.append("Current Project Root: \${projectRoot.absolutePath}\\n\\n")
        sb.append("File Structure:\\n")
        
        val allowedExtensions = listOf("kt", "java", "xml", "html", "css", "js", "json", "md", "txt", "gradle", "kts", "toml", "properties")
        
        fun walk(dir: File, indent: String) {
            dir.listFiles()?.sortedBy { it.name }?.forEach { file ->
                if (file.name == "build" || file.name == ".gradle" || file.name == ".idea" || file.name == "node_modules") return@forEach
                if (file.isDirectory) {
                    sb.append("\$indent\${file.name}/\\n")
                    walk(file, "$indent  ")
                } else {
                    sb.append("\$indent\${file.name}\\n")
                }
            }
        }
        walk(projectRoot, "")
        
        sb.append("\\n\\nFile Contents (limited):\\n")
        var filesRead = 0
        projectRoot.walkTopDown()
            .onEnter { it.name != "build" && it.name != ".gradle" && it.name != ".idea" && it.name != "node_modules" }
            .filter { it.isFile && allowedExtensions.contains(it.extension) }
            .take(10) // Limit to 10 files
            .forEach { file ->
                if (filesRead > 10) return@forEach
                val relativePath = file.relativeTo(projectRoot).path
                sb.append("--- \$relativePath ---\\n")
                val content = file.readText().take(2000) // Limit content size
                sb.append(content)
                if (file.length() > 2000) sb.append("\\n... (truncated)\\n")
                sb.append("\\n\\n")
                filesRead++
            }
            
        return sb.toString()
    }
}
