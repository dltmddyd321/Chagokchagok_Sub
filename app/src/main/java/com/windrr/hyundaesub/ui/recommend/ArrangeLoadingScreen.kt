package com.windrr.hyundaesub.ui.recommend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.windrr.hyundaesub.ui.component.ExitConfirmDialog
import com.windrr.hyundaesub.ui.theme.ChagokBackgroundTop
import com.windrr.hyundaesub.ui.theme.ChagokTextPrimary
import com.windrr.hyundaesub.ui.theme.ChagokTextSecondary
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme

/**
 * 배치 계산 중 전체 화면 로딩 (짐 선택 후 처음 계산 · 새 계획 만들기로 다시 계산).
 * ✕는 홈까지 나가므로 종료 확인 다이얼로그를 거친다.
 */
@Composable
fun ArrangeLoadingScreen(onClose: () -> Unit) {
    var showExitConfirm by rememberSaveable { mutableStateOf(false) }
    if (showExitConfirm) {
        ExitConfirmDialog(
            onContinue = { showExitConfirm = false },
            onExit = {
                showExitConfirm = false
                onClose()
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChagokBackgroundTop)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CloseButtonBg)
                    .clickable { showExitConfirm = true },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "✕", color = ChagokTextSecondary, fontSize = 14.sp)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                // 시안: 화면 가운데보다 살짝 위
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                color = SpinnerNavy,
                trackColor = SpinnerTrack,
                strokeWidth = 6.dp,
                strokeCap = StrokeCap.Round,
                modifier = Modifier.size(68.dp),
            )
            Spacer(Modifier.height(28.dp))
            Text(
                text = "짐 배치 방법을\n찾고 있어요",
                color = ChagokTextPrimary,
                fontSize = 24.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "선택한 짐의 크기와 탑승 좌석을\n함께 확인하고 있어요.",
                color = ChagokTextSecondary,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val SpinnerNavy = Color(0xFF05082B)
private val SpinnerTrack = Color(0xFFDDE2EC)
private val CloseButtonBg = Color(0xFFE3E7EF)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ArrangeLoadingPreview() {
    HyundaeSubTheme { ArrangeLoadingScreen(onClose = {}) }
}
