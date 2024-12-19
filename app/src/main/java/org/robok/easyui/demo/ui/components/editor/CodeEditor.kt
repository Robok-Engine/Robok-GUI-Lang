package org.robok.easyui.demo.ui.components.editor

/*
 *  This file is part of Robok © 2024.
 *
 *  Robok is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Robok is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with Robok.  If not, see <https://www.gnu.org/licenses/>.
 */

import android.content.Context
import android.graphics.Typeface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.R as MaterialR
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.eclipse.tm4e.core.registry.IThemeSource

/*
 * A Sora Code Editor Implementation to use with compose
 * @author Vivek
 */

@Composable
fun rememberCodeEditorState(initialContent: Content = Content()) = remember {
  CodeEditorState(initialContent = initialContent)
}

@Composable
fun CodeEditor(modifier: Modifier = Modifier, state: CodeEditorState) {
  val context = LocalContext.current
  val dark = isSystemInDarkTheme()

  val editor = remember {
    setCodeEditorFactory(isDarkMode = dark, context = context, state = state)
  }
  AndroidView(
    factory = { editor },
    modifier = modifier,
    onRelease = { it.release() },
    update = { state.content = it.text },
  )
}

private fun setCodeEditorFactory(
  isDarkMode: Boolean = true,
  context: Context,
  state: CodeEditorState,
): CodeEditor {
  val editor = CodeEditor(context)
  val typeface = Typeface.createFromAsset(context.assets, "JetBrainsMono-Regular.ttf")
  val theme = if (isDarkMode) "darcula" else "quietlight"

  editor.apply {
    setText(state.content)
    typefaceText = typeface
    typefaceLineNumber = typeface
    isEditable = state.isEditable

    FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(context.assets))
    val themeRegistry = ThemeRegistry.getInstance()
    val path = "editor/textmate/$theme.json"
    themeRegistry.loadTheme(
      ThemeModel(
        IThemeSource.fromInputStream(
          FileProviderRegistry.getInstance().tryGetInputStream(path),
          path,
          null,
        ),
        theme,
      )
    )

    themeRegistry.setTheme(theme)

    ThemeRegistry.getInstance().setTheme(theme)
    GrammarRegistry.getInstance().loadGrammars("editor/textmate/languages.json")

    if (colorScheme !is TextMateColorScheme) {
      colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
      colorScheme = colorScheme
    }

    val resUtils = ResUtils(context)
    colorScheme.setColor(
      EditorColorScheme.WHOLE_BACKGROUND,
      resUtils.getAttrColor(MaterialR.attr.colorSurface),
    )
    colorScheme.setColor(
      EditorColorScheme.LINE_NUMBER_PANEL,
      resUtils.getAttrColor(MaterialR.attr.colorSurface),
    )
    colorScheme.setColor(
      EditorColorScheme.LINE_NUMBER_BACKGROUND,
      resUtils.getAttrColor(MaterialR.attr.colorSurface),
    )

    val language = TextMateLanguage.create("text.xml", true)
    setEditorLanguage(language)
  }
  state.editor = editor
  return editor
}

data class CodeEditorState(
  var editor: CodeEditor? = null,
  val initialContent: Content = Content(),
  val isEditable: Boolean = false,
) {
  var content by mutableStateOf(initialContent)
}