package net.sentinelsafeguard.app.ui

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import net.sentinelsafeguard.app.web.WebDestination
import net.sentinelsafeguard.app.web.WebNav

/**
 * The embedded browser (hunt4): the complete Command web app (or
 * the full Roundcube webmail) inside the app, "just like the
 * browser".
 *
 * Authentication here is deliberately the BROWSER path — the
 * WebView gets no device token and none of the sealed Cloudflare
 * Access service headers (those stay on the API client only).
 * Cloudflare Access asks for an email code the first time, then
 * its cookies — accepted, third-party included, and flushed to
 * disk on pause — keep the session across app restarts for as
 * long as Access allows. The default WebView user agent is used,
 * untouched.
 *
 * Downloads ride Android's DownloadManager with the WebView's own
 * cookies and user agent; uploads ride the system file picker;
 * the system Back button walks the page history before it closes
 * the screen; links to anywhere outside the Sentinel hosts (and
 * the Access sign-in domain) open in the system browser.
 */
@Composable
fun WebScreen(destination: WebDestination, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var progress by remember(destination) { mutableStateOf(0) }
    var loading by remember(destination) { mutableStateOf(true) }
    var loadError by remember(destination) { mutableStateOf<String?>(null) }
    var canGoBack by remember(destination) { mutableStateOf(false) }

    var fileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val callback = fileCallback
        fileCallback = null
        val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        callback?.onReceiveValue(uris)
    }
    val currentFilePicker = rememberUpdatedState(filePicker)
    val currentFileCallbackSetter = rememberUpdatedState { cb: ValueCallback<Array<Uri>>? ->
        fileCallback?.onReceiveValue(null)
        fileCallback = cb
    }

    val webView = remember(destination) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // Inline media (Command carries none today, Roundcube
            // previews do) should not need a tap to start.
            settings.mediaPlaybackRequiresUserGesture = false

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    if (!request.isForMainFrame) return false
                    val url = request.url?.toString() ?: return false
                    if (WebNav.staysInside(url)) return false
                    openExternally(context, url)
                    return true
                }

                override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                    loading = true
                    loadError = null
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    loading = false
                    canGoBack = view.canGoBack()
                    CookieManager.getInstance().flush()
                }

                override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                    canGoBack = view.canGoBack()
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    if (request.isForMainFrame) {
                        loading = false
                        loadError = "This page could not be loaded. Check your connection, " +
                            "then try again."
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    progress = newProgress
                }

                override fun onShowFileChooser(
                    webView: WebView,
                    filePathCallback: ValueCallback<Array<Uri>>,
                    fileChooserParams: FileChooserParams,
                ): Boolean {
                    currentFileCallbackSetter.value(filePathCallback)
                    return try {
                        currentFilePicker.value.launch(fileChooserParams.createIntent())
                        true
                    } catch (e: Exception) {
                        // Answer the page's callback with "no file"
                        // (the setter clears the stored one) and let
                        // the page carry on.
                        currentFileCallbackSetter.value(null)
                        false
                    }
                }
            }

            setDownloadListener(
                DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                    startDownload(context, url, userAgent, contentDisposition, mimeType)
                },
            )

            loadUrl(destination.startUrl)
        }
    }

    // Persist the Access/session cookies whenever the app pauses,
    // and release the WebView when the screen goes away.
    DisposableEffect(lifecycleOwner, webView) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                CookieManager.getInstance().flush()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            CookieManager.getInstance().flush()
            webView.stopLoading()
            webView.destroy()
        }
    }

    BackHandler(enabled = canGoBack) { webView.goBack() }
    BackHandler(enabled = !canGoBack, onBack = onClose)

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth()) {
            TextButton(onClick = onClose) { Text("‹ Back") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { loadError = null; webView.reload() }) { Text("Reload") }
        }
        if (loading) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (loadError != null) {
            ErrorBox(loadError!!) {
                loadError = null
                webView.reload()
            }
        }
        AndroidView(
            factory = { webView },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

/** Hand a link to the system browser (or dialer/mail app for
 *  tel:/mailto: links the policy kept out of the WebView). */
private fun openExternally(context: Context, url: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    } catch (e: Exception) {
        Toast.makeText(context, "No app on this phone can open that link.", Toast.LENGTH_SHORT).show()
    }
}

/** A WebView download through the system DownloadManager, with
 *  the page's own cookies and user agent so gated files come down
 *  as the signed-in owner. Lands in the phone's Downloads folder. */
private fun startDownload(
    context: Context,
    url: String,
    userAgent: String?,
    contentDisposition: String?,
    mimeType: String?,
) {
    try {
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val request = DownloadManager.Request(Uri.parse(url))
        val cookies = CookieManager.getInstance().getCookie(url)
        if (!cookies.isNullOrBlank()) {
            request.addRequestHeader("Cookie", cookies)
        }
        if (!userAgent.isNullOrBlank()) {
            request.addRequestHeader("User-Agent", userAgent)
        }
        if (!mimeType.isNullOrBlank()) {
            request.setMimeType(mimeType)
        }
        request.setTitle(fileName)
        request.setDescription("Sentinel App download")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(context, "Downloading $fileName", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "That download could not be started. Try again.",
            Toast.LENGTH_SHORT,
        ).show()
    }
}
