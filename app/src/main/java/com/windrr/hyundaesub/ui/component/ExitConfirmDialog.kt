package com.windrr.hyundaesub.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.windrr.hyundaesub.ui.theme.ChagokTextPrimary
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme

/**
 * 짐 등록 흐름에서 ✕로 홈에 나가기 전 확인.
 * 바깥을 누르거나 뒤로가기를 누르면 [onContinue] (계속 작성)로 본다.
 */
@Composable
fun ExitConfirmDialog(
    onContinue: () -> Unit,
    onExit: () -> Unit,
) {
    Dialog(
        onDismissRequest = onContinue,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ExitConfirmContent(onContinue = onContinue, onExit = onExit)
    }
}

@Composable
private fun ExitConfirmContent(onContinue: () -> Unit, onExit: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "작성 중인 내용을 종료할까요?",
            color = ChagokTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "화면을 나가면 작성한 내용이\n저장되지 않고 사라집니다.",
            color = BodyText,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DialogPillButton(text = "계속 작성하기", container = ContinueBg, content = Color.White, onClick = onContinue)
            DialogPillButton(text = "나가기", container = ExitBg, content = ChagokTextPrimary, onClick = onExit)
        }
    }
}

@Composable
private fun DialogPillButton(text: String, container: Color, content: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(percent = 50),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        elevation = null,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

private val BodyText = Color(0xFF2B2F38)
private val ContinueBg = Color(0xFF05082B)
private val ExitBg = Color(0xFFDFE5F0)

@Preview(showBackground = true, widthDp = 360, heightDp = 400, backgroundColor = 0xFF7A7F8A)
@Composable
private fun ExitConfirmPreview() {
    HyundaeSubTheme {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ExitConfirmContent(onContinue = {}, onExit = {})
        }
    }
}
