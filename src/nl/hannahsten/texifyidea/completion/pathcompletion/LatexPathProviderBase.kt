package nl.hannahsten.texifyidea.completion.pathcompletion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import nl.hannahsten.texifyidea.TexifyIcons
import nl.hannahsten.texifyidea.completion.handlers.CompositeHandler
import nl.hannahsten.texifyidea.completion.handlers.FileNameInsertionHandler
import nl.hannahsten.texifyidea.completion.handlers.LatexReferenceInsertHandler
import nl.hannahsten.texifyidea.lang.RequiredFileArgument
import nl.hannahsten.texifyidea.lang.RequiredPicturePathArgument
import nl.hannahsten.texifyidea.util.files.findRootFile
import nl.hannahsten.texifyidea.util.files.isLatexFile
import java.io.File
import java.util.regex.Pattern

/**
 * @author Lukas Heiligenbrunner
 */
abstract class LatexPathProviderBase : CompletionProvider<CompletionParameters>() {
    private var parameters: CompletionParameters? = null
    private var resultSet: CompletionResultSet? = null
    private var validExtensions: Set<String>? = null
    private var absolutePathSupport = true

    companion object {
        private val TRIM_SLASH = Pattern.compile("/[^/]*$")
        private val EXTENSION = Pattern.compile("\\..*")
    }


    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        this.parameters = parameters

        // We create a result set with the correct autocomplete text as prefix, which may be different when multiple LaTeX parameters (comma separated) are present
        val autocompleteText = processAutocompleteText(parameters.position.text)
        resultSet = result.withPrefixMatcher(autocompleteText)

        val parentCommand = context.get("type")
        if (parentCommand is RequiredFileArgument) {
            validExtensions = parentCommand.supportedExtensions
            absolutePathSupport = parentCommand.isAbsolutePathSupported
        }

        selectScanRoots(parameters.originalFile).forEach {
            addByDirectory(it, autocompleteText)
        }
    }

    /**
     * return a List of Paths to be searched in
     * eg. project root
     * eg. \includegraphics roots
     */
    abstract fun selectScanRoots(file: PsiFile): ArrayList<VirtualFile>

    /**
     * enable folder search?
     */
    abstract fun searchFolders(): Boolean

    /**
     * enable file search?
     */
    abstract fun searchFiles(): Boolean


    /**
     * scan directory for completions
     */
    private fun addByDirectory(baseDirectory: VirtualFile, autoCompleteText: String) {
        // Check if path is relative or absolute
        if (File(autoCompleteText).isAbsolute) {
            if (absolutePathSupport) {
                // Split text in path and completion text
                val pathOffset = trimAutocompleteText(autoCompleteText)
                addAbsolutePathCompletion(pathOffset)
            }
        }
        else {
            val pathOffset = trimAutocompleteText(autoCompleteText)
            addRelativePathCompletion(baseDirectory, pathOffset)
        }
    }

    /**
     * add completion entries for relative path
     */
    private fun addRelativePathCompletion(projectDir: VirtualFile, pathOffset: String) {
        LocalFileSystem.getInstance().findFileByPath(projectDir.path + "/" + pathOffset)?.let { baseDir ->
            if (searchFolders()) {
                addFolderNavigations(pathOffset)
                getContents(baseDir, true).forEach {
                    addDirectoryCompletion(pathOffset, it)
                }
            }

            if (searchFiles()) getContents(baseDir, false).forEach {
                addFileCompletion(pathOffset, it)
            }
        }
    }

    /**
     * add completion entries for absolute path
     */
    private fun addAbsolutePathCompletion(baseDir: String) {
        LocalFileSystem.getInstance().findFileByPath(baseDir)?.let { dirFile ->
            if (searchFolders()) {
                addFolderNavigations(baseDir)
                getContents(dirFile, true).forEach {
                    addDirectoryCompletion(baseDir, it)
                }
            }

            if (searchFiles()) getContents(dirFile, false).forEach {
                addFileCompletion(baseDir, it)
            }
        }
    }

    /**
     * add basic folder navigation options such as ../ and ./
     */
    private fun addFolderNavigations(baseDir: String) {
        // Add current directory.
        resultSet?.addElement(
                LookupElementBuilder.create("$baseDir./")
                        .withPresentableText(".")
                        .withIcon(PlatformIcons.PACKAGE_ICON)
        )

        // Add return directory.
        resultSet?.addElement(
                LookupElementBuilder.create("$baseDir../")
                        .withPresentableText("..")
                        .withIcon(PlatformIcons.PACKAGE_ICON)
        )
    }

    /**
     * add Directory to autocompletion dialog
     */
    private fun addDirectoryCompletion(baseDir: String, foundFile: VirtualFile) {
        resultSet?.addElement(
                LookupElementBuilder.create(baseDir + foundFile.name + "/")
                        .withPresentableText(foundFile.presentableName)
                        .withIcon(PlatformIcons.PACKAGE_ICON)
        )
    }

    /**
     * add file to autocompletion dialog
     */
    private fun addFileCompletion(baseDir: String, foundFile: VirtualFile) {
        if (validExtensions != null) {
            if (validExtensions!!.contains(foundFile.extension).not()) return
        }

        val icon = TexifyIcons.getIconFromExtension(foundFile.extension)
        resultSet?.addElement(
                LookupElementBuilder.create(baseDir + foundFile.name)
                        .withPresentableText(foundFile.presentableName)
                        .withInsertHandler(CompositeHandler<LookupElement>(
                                LatexReferenceInsertHandler(),
                                FileNameInsertionHandler()
                        ))
                        .withIcon(icon)
        )
    }


    /**
     * Get project roots
     * @return all Project Root directories as VirtualFile
     */
    fun getProjectRoots(): ArrayList<VirtualFile> {
        val resultList = ArrayList<VirtualFile>()
        if (parameters == null) return resultList
        // Get base data.
        val baseFile = parameters!!.originalFile.virtualFile

        if (parameters!!.originalFile.isLatexFile()) {
            resultList.add(parameters!!.originalFile.findRootFile().containingDirectory.virtualFile)
        }
        else resultList.add(baseFile.parent)

        val rootManager = ProjectRootManager.getInstance(parameters!!.originalFile.project)
        rootManager.contentSourceRoots.asSequence()
                .filter { it != resultList.first() }
                .toSet()
                .forEach { resultList.add(it) }

        return resultList
    }


    /**
     * @param autoCompleteText full path (relative or absolute) including the completion offset
     * @return path without autocompletion text including forward slash at end
     */
    private fun trimAutocompleteText(autoCompleteText: String): String {
        return if (!autoCompleteText.contains("/")) {
            ""
        }
        else TRIM_SLASH.matcher(autoCompleteText).replaceAll("/")
        // delete last subpath occurence
    }

    private fun getFileExtension(fileName: String): String {
        val matcher = EXTENSION.matcher(fileName)
        if (matcher.find()) return matcher.group()
        else return ""
    }

    /**
     * prepare auto-complete text before searching for files
     */
    private fun processAutocompleteText(autocompleteText: String): String {
        var result = if (autocompleteText.endsWith("}")) {
            autocompleteText.substring(0, autocompleteText.length - 1)
        }
        else autocompleteText

        // When the last parameter is autocompleted, parameters before that may also be present in
        // autocompleteText so we split on commas and take the last one. If it is not the last
        // parameter, no commas will be present so the split will do nothing.
        result = result.replace("IntellijIdeaRulezzz", "")
                .split(",").last()

        // Prevent double ./
        if (result.startsWith("./")) {
            result = result.substring(2)
        }

        // Prevent double /
        if (result.startsWith("//")) {
            result = result.substring(1)
        }

        return result
    }

    /**
     * search in given path for subfiles or directories
     */
    private fun getContents(base: VirtualFile?, directory: Boolean): List<VirtualFile> {
        val contents = java.util.ArrayList<VirtualFile>()

        if (base == null) {
            return contents
        }

        for (file in base.children) {
            if (file.isDirectory == directory) {
                contents.add(file)
            }
        }

        return contents
    }
}