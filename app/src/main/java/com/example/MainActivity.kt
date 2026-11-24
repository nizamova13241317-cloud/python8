package com.example

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  private var pendingPyContent: String = ""

  private val createPyFileLauncher =
      registerForActivityResult(ActivityResultContracts.CreateDocument("text/x-python")) { uri: Uri? ->
        if (uri != null) {
          try {
            contentResolver.openOutputStream(uri)?.use { stream ->
              stream.write(pendingPyContent.toByteArray(Charsets.UTF_8))
            }
            Toast.makeText(this, "Файл .py збережено", Toast.LENGTH_SHORT).show()
          } catch (e: Exception) {
            Toast.makeText(this, "Не вдалося зберегти файл", Toast.LENGTH_SHORT).show()
          }
        }
      }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val bgColor = MaterialTheme.colorScheme.background
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .background(bgColor)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .imePadding(),
        ) {
          PythonSandboxWebView(
              backgroundColorArgb = bgColor.toArgb(),
              onCopy = { text ->
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Python Code", text))
              },
              onDownloadPy = { filename, content ->
                pendingPyContent = content
                createPyFileLauncher.launch(filename.ifBlank { "main.py" })
              },
          )
        }
      }
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PythonSandboxWebView(
    backgroundColorArgb: Int,
    onCopy: (String) -> Unit,
    onDownloadPy: (String, String) -> Unit,
) {
  AndroidView(
      modifier = Modifier.fillMaxSize().testTag("python_sandbox_webview"),
      factory = { context ->
        WebView(context).apply {
          setBackgroundColor(backgroundColorArgb)
          overScrollMode = View.OVER_SCROLL_NEVER
          isVerticalScrollBarEnabled = false
          isHorizontalScrollBarEnabled = false

          settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            useWideViewPort = false
            loadWithOverviewMode = false
          }

          webViewClient = WebViewClient()
          webChromeClient = WebChromeClient()

          addJavascriptInterface(
              object {
                @JavascriptInterface
                fun copyToClipboard(text: String) {
                  post { onCopy(text) }
                }

                @JavascriptInterface
                fun downloadPy(filename: String, content: String) {
                  post { onDownloadPy(filename, content) }
                }
              },
              "AndroidBridge",
          )

          loadUrl("file:///android_asset/index.html")
        }
      },
  )
}
