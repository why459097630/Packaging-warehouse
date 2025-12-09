@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ndjc.ui.neu.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ndjc.ui.neu.components.NDJCBusScaffold
import com.ndjc.ui.neu.theme.NDJCTheme
import com.ndjc.ui.neu.theme.ThemeMode
import com.ndjc.ui.neu.theme.NeuTokens
import com.ndjc.ui.neu.components.NDJCCard

@Composable
fun DemoScreen(modifier: Modifier = Modifier) {
    BusDemoScreen(modifier)
}

/* ------------------------------ Bus 风格页面 ------------------------------ */

@Composable
private fun BusDemoScreen(modifier: Modifier = Modifier) {
    NDJCBusScaffold(
        modifier = modifier
    ) {
        // 顶部文案：Hi + Bus
        BusTopHeader()

        Spacer(modifier = Modifier.height(72.dp))

        // 上半部分：行程信息卡（From / To）
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 20.dp,
                    vertical = 18.dp
                )
            ) {
                Text(
                    text = "FROM",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9FA3C5)
                )
                Text(
                    text = "Location 1",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF33334F)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "TO",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9FA3C5)
                )
                Text(
                    text = "Location 2",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF33334F)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 下半部分：乘客 / 日期信息卡
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 20.dp,
                    vertical = 18.dp
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "PASSENGER",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9FA3C5)
                        )
                        Text(
                            text = "01",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF33334F)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "TYPE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9FA3C5)
                        )
                        Text(
                            text = "BUS",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF33334F)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    Text(
                        text = "DEPART",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9FA3C5)
                    )
                    Text(
                        text = "Sun 3 Jun 2021",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF33334F)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 搜索按钮
        Button(
            onClick = { /* TODO: search */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF8C7BFF),
                contentColor = Color.White
            )
        ) {
            Text(text = "SEARCH")
        }

        // 底部的空白由骨架 + 模板底栏共同决定，这里只留一点缓冲
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun BusTopHeader() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Hi, 20min",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Bus",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
    }
}

/* -------------------------------- Previews -------------------------------- */

@Preview(name = "Light • Regular", showSystemUi = true)
@Composable
private fun PreviewDemoLight() {
    NDJCTheme(mode = ThemeMode.Light) {
        Surface { DemoScreen(Modifier) }
    }
}

@Preview(name = "Dark • Regular", showSystemUi = true)
@Composable
private fun PreviewDemoDark() {
    NDJCTheme(mode = ThemeMode.Dark) {
        Surface { DemoScreen(Modifier) }
    }
}

@Preview(showBackground = true)
@Composable
fun DemoScreenPreview() {
    DemoScreen()
}
