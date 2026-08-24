package com.resonote.feature.risk.impl

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resonote.core.designsystem.component.ResonoteButton
import com.resonote.core.designsystem.component.ResonoteTopAppBar
import com.resonote.core.model.RiskChallengeHandle
import com.resonote.core.navigation.RiskVerificationNavKey
import org.json.JSONObject

@Composable
fun RiskVerificationRoute(
    key: RiskVerificationNavKey,
    onBack: () -> Unit,
    onVerified: () -> Unit,
    viewModel: RiskVerificationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(key.challengeHandle) { viewModel.load(RiskChallengeHandle(key.challengeHandle)) }
    LaunchedEffect(viewModel) { viewModel.verified.collect { onVerified() } }
    RiskVerificationScreen(
        state = state,
        onBack = onBack,
        onSmsCodeChanged = viewModel::updateSmsCode,
        onSubmitSms = viewModel::submitSms,
        onTencentProof = viewModel::submitTencent,
        onTencentFailure = viewModel::reportTencentFailure,
        onRetry = viewModel::retry,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun RiskVerificationScreen(
    state: RiskVerificationUiState,
    onBack: () -> Unit,
    onSmsCodeChanged: (String) -> Unit,
    onSubmitSms: () -> Unit,
    onTencentProof: (String, String, String) -> Unit,
    onTencentFailure: () -> Unit,
    onRetry: () -> Unit,
) {
    BackHandler(onBack = onBack)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ResonoteTopAppBar(
                title = { Text(stringResource(R.string.feature_risk_impl_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.feature_risk_impl_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.feature_risk_impl_context),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                when (state) {
                    RiskVerificationUiState.Loading -> CenteredMessage(R.string.feature_risk_impl_loading, true)
                    is RiskVerificationUiState.Tencent -> TencentCaptcha(
                        applicationId = state.applicationId,
                        enabled = !state.submitting,
                        onProof = onTencentProof,
                        onFailure = onTencentFailure,
                    )
                    is RiskVerificationUiState.Sms -> SmsVerification(state, onSmsCodeChanged, onSubmitSms)
                    is RiskVerificationUiState.Failed -> Failure(onRetry, R.string.feature_risk_impl_error)
                    is RiskVerificationUiState.Unsupported -> Failure(onRetry, R.string.feature_risk_impl_unsupported)
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(message: Int, progress: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (progress) CircularProgressIndicator()
            Text(stringResource(message), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SmsVerification(state: RiskVerificationUiState.Sms, onCodeChanged: (String) -> Unit, onSubmit: () -> Unit) {
    Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        OutlinedTextField(
            value = state.code,
            onValueChange = onCodeChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.feature_risk_impl_sms_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            isError = state.error != null,
        )
        ResonoteButton(
            label = stringResource(R.string.feature_risk_impl_submit),
            loadingLabel = stringResource(R.string.feature_risk_impl_submitting),
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.code.isNotBlank(),
            loading = state.submitting,
        )
    }
}

@Composable
private fun Failure(onRetry: () -> Unit, message: Int) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(message), color = MaterialTheme.colorScheme.onSurfaceVariant)
        ResonoteButton(label = stringResource(R.string.feature_risk_impl_retry), onClick = onRetry)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun TencentCaptcha(
    applicationId: String,
    enabled: Boolean,
    onProof: (String, String, String) -> Unit,
    onFailure: () -> Unit,
) {
    val context = LocalContext.current
    val webView = androidx.compose.runtime.remember(applicationId) {
        WebView(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                    handleCaptchaUri(request.url, applicationId, onProof, onFailure)
            }
            loadDataWithBaseURL(
                "https://turing.captcha.qcloud.com/",
                captchaHtml(applicationId),
                "text/html",
                "UTF-8",
                null,
            )
        }
    }
    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }
    AndroidView(
        factory = { webView },
        modifier = Modifier.fillMaxWidth().heightIn(min = 420.dp).padding(horizontal = 16.dp),
        update = { it.isEnabled = enabled },
    )
}

private fun handleCaptchaUri(
    uri: Uri,
    applicationId: String,
    onProof: (String, String, String) -> Unit,
    onFailure: () -> Unit,
): Boolean {
    if (uri.scheme != "resonote-captcha") return false
    when (uri.host) {
        "success" -> {
            val ticket = uri.getQueryParameter("ticket").orEmpty()
            val randomString = uri.getQueryParameter("randstr").orEmpty()
            if (ticket.isBlank() ||
                randomString.isBlank()
            ) {
                onFailure()
            } else {
                onProof(ticket, randomString, applicationId)
            }
        }
        "error" -> onFailure()
    }
    return true
}

private fun captchaHtml(applicationId: String): String {
    val appId = JSONObject.quote(applicationId)
    return """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
        <style>html,body{margin:0;background:transparent;height:100%;font-family:sans-serif}#status{padding:24px;color:#666;text-align:center}</style>
        <script src="https://turing.captcha.qcloud.com/TCaptcha.js"></script></head>
        <body><div id="status">正在准备安全验证…</div><script>
        window.onload=function(){try{var captcha=new TencentCaptcha($appId,function(res){
          if(res&&res.ret===0&&res.ticket&&res.randstr){location.href='resonote-captcha://success?ticket='+encodeURIComponent(res.ticket)+'&randstr='+encodeURIComponent(res.randstr)}
          else if(res&&res.ret!==2){location.href='resonote-captcha://error'}
        },{type:'',showHeader:false});document.getElementById('status').style.display='none';captcha.show()}
        catch(e){location.href='resonote-captcha://error'}};
        </script></body></html>
    """.trimIndent()
}
