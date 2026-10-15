package com.example.ui.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.TravelAssistantService
import kotlinx.coroutines.launch

data class ChatMessage(
    val sender: String, // "USER" or "ASSISTANT"
    val text: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelAssistantScreen(
    currentRouteContext: String = "",
    onBackClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                sender = "ASSISTANT",
                text = "Hello! I am your TollMate Highway Assistant. Ask me about toll rates, FASTag guidelines, nearest EV chargers, food malls, or emergency assistance along Indian national highways."
            )
        )
    }

    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }

    val suggestedQuestions = listOf(
        "Toll rates on Mumbai-Pune Expressway",
        "Nearest EV charging plaza",
        "What is the NHAI emergency number?",
        "What should I do if my tyre is punctured?",
        "Speed limit on 6-lane expressways"
    )

    fun sendUserMessage(query: String) {
        if (query.isBlank() || isThinking) return
        val userMsg = query.trim()
        messages.add(ChatMessage(sender = "USER", text = userMsg))
        inputText = ""
        isThinking = true

        coroutineScope.launch {
            val response = TravelAssistantService.askAssistant(
                userPrompt = userMsg,
                contextInfo = currentRouteContext
            )
            isThinking = false
            messages.add(ChatMessage(sender = "ASSISTANT", text = response))
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("AI Travel Assistant", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Smart Highway Intelligence", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("assistant_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(messages) { msg ->
                    val isUser = msg.sender == "USER"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isUser) 16.dp else 4.dp,
                                        bottomEnd = if (isUser) 4.dp else 16.dp
                                    )
                                )
                                .background(
                                    if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(12.dp)
                                .fillMaxWidth(0.85f)
                        ) {
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (isThinking) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Assistant is thinking...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Quick suggestion chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestedQuestions) { prompt ->
                    FilterChip(
                        selected = false,
                        onClick = { sendUserMessage(prompt) },
                        label = { Text(prompt, fontSize = 12.sp) }
                    )
                }
            }

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask route, toll rates, or facilities...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("assistant_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { sendUserMessage(inputText) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("assistant_send_button"),
                    enabled = inputText.isNotBlank() && !isThinking
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
