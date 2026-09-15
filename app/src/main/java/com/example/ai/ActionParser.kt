package com.example.ai

import java.io.File

object ActionParser {

    data class ParsedAction(
        val type: ActionType,
        val content: String,
        val path: String? = null
    )

    enum class ActionType {
        FILE,
        EDIT_FILE,
        APPEND_FILE,
        MKDIR,
        COMMAND,
        BROWSER_OPEN
    }

    private val fileRegex =
        Regex(
            """<file\s+path=["']([^"']+)["']>([\s\S]*?)</file>""",
            RegexOption.IGNORE_CASE
        )

    private val editFileRegex =
        Regex(
            """<edit_file\s+path=["']([^"']+)["']>([\s\S]*?)</edit_file>""",
            RegexOption.IGNORE_CASE
        )

    private val appendFileRegex =
        Regex(
            """<append_file\s+path=["']([^"']+)["']>([\s\S]*?)</append_file>""",
            RegexOption.IGNORE_CASE
        )

    private val mkdirRegex =
        Regex(
            """<mkdir\s+path=["']([^"']+)["']\s*/?>""",
            RegexOption.IGNORE_CASE
        )

    private val commandRegex =
        Regex(
            """<command>([\s\S]*?)</command>""",
            RegexOption.IGNORE_CASE
        )

    private val browserRegex =
        Regex(
            """<browser_open>([\s\S]*?)</browser_open>""",
            RegexOption.IGNORE_CASE
        )

    fun parse(text: String): List<ParsedAction> {

        val actions = mutableListOf<ParsedAction>()

        fileRegex.findAll(text).forEach { match ->
            actions += ParsedAction(
                type = ActionType.FILE,
                path = match.groupValues[1].trim(),
                content = match.groupValues[2].trim()
            )
        }

        editFileRegex.findAll(text).forEach { match ->
            actions += ParsedAction(
                type = ActionType.EDIT_FILE,
                path = match.groupValues[1].trim(),
                content = match.groupValues[2].trim()
            )
        }

        appendFileRegex.findAll(text).forEach { match ->
            actions += ParsedAction(
                type = ActionType.APPEND_FILE,
                path = match.groupValues[1].trim(),
                content = match.groupValues[2].trim()
            )
        }

        mkdirRegex.findAll(text).forEach { match ->
            actions += ParsedAction(
                type = ActionType.MKDIR,
                path = match.groupValues[1].trim(),
                content = match.groupValues[1].trim()
            )
        }

        commandRegex.findAll(text).forEach { match ->
            val command = match.groupValues[1].trim()

            if (command.isNotBlank()) {
                actions += ParsedAction(
                    type = ActionType.COMMAND,
                    content = command
                )
            }
        }

        browserRegex.findAll(text).forEach { match ->
            val url = match.groupValues[1].trim()

            if (url.isNotBlank()) {
                actions += ParsedAction(
                    type = ActionType.BROWSER_OPEN,
                    content = url
                )
            }
        }

        return actions
    }

    /**
     * Executes only the safe project-level actions.
     *
     * Workspace is intentionally kept inside the application's
     * private project directory.
     */
    fun execute(
        actions: List<ParsedAction>,
        workspace: File
    ): List<String> {

        if (!workspace.exists()) {
            workspace.mkdirs()
        }

        val results = mutableListOf<String>()

        actions.forEach { action ->

            when (action.type) {

                ActionType.FILE -> {

                    val file = safeFile(
                        workspace,
                        action.path ?: return@forEach
                    )

                    file.parentFile?.mkdirs()
                    file.writeText(action.content)

                    results +=
                        "Created: ${relativePath(workspace, file)}"
                }

                ActionType.EDIT_FILE -> {

                    val file = safeFile(
                        workspace,
                        action.path ?: return@forEach
                    )

                    if (!file.exists()) {
                        results +=
                            "Edit failed: ${relativePath(workspace, file)} does not exist."
                    } else {
                        file.writeText(action.content)

                        results +=
                            "Edited: ${relativePath(workspace, file)}"
                    }
                }

                ActionType.APPEND_FILE -> {

                    val file = safeFile(
                        workspace,
                        action.path ?: return@forEach
                    )

                    file.parentFile?.mkdirs()

                    file.appendText(
                        if (file.exists()) {
                            "\n${action.content}"
                        } else {
                            action.content
                        }
                    )

                    results +=
                        "Appended: ${relativePath(workspace, file)}"
                }

                ActionType.MKDIR -> {

                    val directory = safeFile(
                        workspace,
                        action.path ?: return@forEach
                    )

                    directory.mkdirs()

                    results +=
                        "Created directory: ${relativePath(workspace, directory)}"
                }

                ActionType.COMMAND -> {

                    val command = action.content.trim()

                    /*
                     * Preview is handled directly instead of being
                     * passed to a shell. This fixes the old
                     * ${action.content}/$output placeholder bug.
                     */
                    if (command.startsWith("preview ", ignoreCase = true)) {

                        val requested =
                            command.substringAfter("preview ")
                                .trim()

                        val htmlFile =
                            if (requested.isBlank()) {
                                File(workspace, "index.html")
                            } else {
                                safeFile(workspace, requested)
                            }

                        if (htmlFile.exists() && htmlFile.isFile) {

                            results +=
                                "PREVIEW_READY:${htmlFile.absolutePath}"

                        } else {

                            results +=
                                "Preview file not found: ${htmlFile.absolutePath}"
                        }

                    } else {

                        /*
                         * Safe basic commands used by the IDE UI.
                         * Arbitrary shell/root execution is intentionally
                         * not performed here.
                         */
                        when (command) {

                            "pwd" -> {
                                results += workspace.absolutePath
                            }

                            "ls" -> {
                                results +=
                                    workspace.listFiles()
                                        ?.joinToString("\n") {
                                            it.name
                                        }
                                        ?: ""
                            }

                            "ls -la" -> {
                                results +=
                                    workspace.listFiles()
                                        ?.joinToString("\n") {
                                            if (it.isDirectory) {
                                                "d ${it.name}"
                                            } else {
                                                "- ${it.name}"
                                            }
                                        }
                                        ?: ""
                            }

                            else -> {
                                results +=
                                    "Command received: $command"
                            }
                        }
                    }
                }

                ActionType.BROWSER_OPEN -> {

                    results +=
                        "BROWSER_OPEN:${action.content}"
                }
            }
        }

        return results
    }

    private fun safeFile(
        workspace: File,
        relativePath: String
    ): File {

        val cleanPath =
            relativePath
                .replace("\\", "/")
                .trimStart('/')

        val root =
            workspace.canonicalFile

        val file =
            File(root, cleanPath).canonicalFile

        if (
            file != root &&
            !file.path.startsWith(root.path + File.separator)
        ) {
            throw SecurityException(
                "Unsafe project path: $relativePath"
            )
        }

        return file
    }

    private fun relativePath(
        workspace: File,
        file: File
    ): String {

        return try {
            file.canonicalFile
                .relativeTo(workspace.canonicalFile)
                .path
        } catch (_: Exception) {
            file.name
        }
    }
}
