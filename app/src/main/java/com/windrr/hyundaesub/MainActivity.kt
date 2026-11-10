package com.windrr.hyundaesub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.windrr.hyundaesub.measure.ArMeasureScreen
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HyundaeSubTheme {
                var measuring by rememberSaveable { mutableStateOf(false) }
                if (measuring) {
                    ArMeasureScreen(onBack = { measuring = false })
                } else {
                    HomeScreen(onMeasureClick = { measuring = true })
                }
            }
        }
    }
}

@Composable
fun HomeScreen(onMeasureClick: () -> Unit) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("내 차에 실릴까?", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("물건 크기를 카메라로 재 보세요", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(32.dp))
            Button(onClick = onMeasureClick, modifier = Modifier.fillMaxWidth()) {
                Text("AR로 물건 크기 재기")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HyundaeSubTheme {
        HomeScreen(onMeasureClick = {})
    }
}
