package dev.hungrymonkey.careercompass.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.emoji2.emojipicker.EmojiPickerView
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import dev.hungrymonkey.careercompass.models.Subtask
import dev.hungrymonkey.careercompass.models.Goal
import dev.hungrymonkey.careercompass.models.ReminderSettings
import androidx.compose.material3.MenuAnchorType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalSheet(
    goalToEdit: Goal? = null,
    onSave: (Goal) -> Unit,
    onCancel: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    var title by remember { mutableStateOf(TextFieldValue(goalToEdit?.title ?: "")) }
    var targetDate by remember { mutableStateOf(goalToEdit?.targetDate ?: "") }
    var selectedEmoji by remember { mutableStateOf(goalToEdit?.icon ?: "🎯") }
    var showEmojiPicker by remember { mutableStateOf(false) }

    val priorityOptions = listOf("High", "Medium", "Low")
    var priorityExpanded by remember { mutableStateOf(false) }
    var priority by remember { mutableStateOf(goalToEdit?.priority ?: "Medium") }
    var subtasks = remember { 
        mutableStateListOf<Subtask>().apply {
            if (goalToEdit != null && goalToEdit.subtasks.isNotEmpty()) {
                addAll(goalToEdit.subtasks)
            }
        }
    }
    
    var reminderEnabled by remember { mutableStateOf(goalToEdit?.reminderSettings?.isEnabled ?: false) }
    var reminderDays by remember { mutableIntStateOf(goalToEdit?.reminderSettings?.daysBefore ?: 3) }
    var reminderExpanded by remember { mutableStateOf(false) }
    val reminderOptions = listOf(1, 3, 7, 14)


    val storageFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy") 
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val picked = LocalDate.of(year, month + 1, day)
                targetDate = picked.format(storageFormatter)
            },
            LocalDate.now().year,
            LocalDate.now().monthValue - 1,
            LocalDate.now().dayOfMonth
        )
    }


    LaunchedEffect(Unit) {
        scope.launch { sheetState.show() }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = {
            scope.launch { sheetState.hide() }.invokeOnCompletion { onCancel() }
        },
        dragHandle = null
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(screenHeight - 150.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (goalToEdit != null) "Edit Goal" else "Add Goal",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onCancel() }
                    }
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Goal Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = targetDate,
                onValueChange = { targetDate = it },
                label = { Text("Target Date") },
                placeholder = { Text("MM/DD/YYYY") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Pick date")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = priorityExpanded,
                onExpandedChange = { priorityExpanded = !priorityExpanded }
            ) {
                OutlinedTextField(
                    value = priority,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Priority") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = priorityExpanded,
                    onDismissRequest = { priorityExpanded = false }
                ) {
                    priorityOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                priority = option
                                priorityExpanded = false
                            }
                        )
                    }
                }
            }

            Text("Goal Icon", style = MaterialTheme.typography.titleMedium)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEmojiPicker = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = selectedEmoji,
                            fontSize = 20.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tap to select emoji",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Choose an icon that represents your goal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Text("Reminder Settings", style = MaterialTheme.typography.titleMedium)
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (reminderEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = if (reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Enable Reminder",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Get notified before the due date",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { enabled ->
                                reminderEnabled = enabled
                            }
                        )
                    }
                    
                    if (reminderEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ExposedDropdownMenuBox(
                            expanded = reminderExpanded,
                            onExpandedChange = { reminderExpanded = !reminderExpanded }
                        ) {
                            OutlinedTextField(
                                value = "$reminderDays ${if (reminderDays == 1) "day" else "days"} before",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Remind me") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = reminderExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = reminderExpanded,
                                onDismissRequest = { reminderExpanded = false }
                            ) {
                                reminderOptions.forEach { days ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text("$days ${if (days == 1) "day" else "days"} before")
                                        },
                                        onClick = {
                                            reminderDays = days
                                            reminderExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (subtasks.isNotEmpty()) {
                Text("Sub-tasks", style = MaterialTheme.typography.titleMedium)
                subtasks.forEachIndexed { idx, sub ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(8.dp)
                                .clickable { 
                                    subtasks[idx] = subtasks[idx].copy(isCompleted = !sub.isCompleted)
                                }
                        ) {
                            Checkbox(
                                checked = sub.isCompleted,
                                onCheckedChange = { isChecked ->
                                    subtasks[idx] = subtasks[idx].copy(isCompleted = isChecked)
                                }
                            )
                            OutlinedTextField(
                                value = sub.title,
                                onValueChange = { newTitle ->
                                    subtasks[idx] = subtasks[idx].copy(title = newTitle)
                                },
                                placeholder = { Text("Enter subtask...") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                isError = sub.title.isBlank() && subtasks.size > 1,
                                supportingText = if (sub.title.isBlank() && subtasks.size > 1) {
                                    { Text("Subtask cannot be empty", color = MaterialTheme.colorScheme.error) }
                                } else null
                            )
                            if (subtasks.size > 1) {
                                IconButton(
                                    onClick = {
                                        subtasks.removeAt(idx)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete, 
                                        contentDescription = "Remove subtask",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { 
                    subtasks.add(Subtask("", false))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = subtasks.isEmpty() || subtasks.all { it.title.isNotBlank() }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add Sub-task")
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp), 
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onCancel() }
                },
                modifier = Modifier.weight(1f)
            ) { 
                Text("Cancel") 
            }
            
            Button(
                onClick = {
                        if (title.text.isNotBlank() && targetDate.isNotBlank()) {
                            val validSubtasks = subtasks.filter { it.title.isNotBlank() }

                            val raw = targetDate
                            val valid = Regex("""\d{2}/\d{2}/\d{4}""").matches(raw)

                            if (title.text.isNotBlank() && valid){
                                val goalId = goalToEdit?.id ?: ""
                                val notificationId = if (goalId.isNotEmpty()) {
                                    goalId.hashCode()
                                } else {
                                    (goalToEdit?.reminderSettings?.notificationId 
                                        ?: System.currentTimeMillis().toInt())
                                }
                                
                                val updatedGoal = Goal(
                                    id = goalToEdit?.id ?: "",
                                    title = title.text,
                                    targetDate = raw,
                                    subtasks = validSubtasks,
                                    icon = selectedEmoji,
                                    priority = priority,
                                    reminderSettings = ReminderSettings(
                                        isEnabled = reminderEnabled,
                                        daysBefore = reminderDays,
                                        notificationId = notificationId
                                    )
                                )
                                onSave(updatedGoal)
                                scope.launch { sheetState.hide() }   
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { 
                    Text(if (goalToEdit != null) "Update" else "Save") 
                }
            }
        }
    }
    
    if (showEmojiPicker) {
        OfficialEmojiPickerDialog(
            onEmojiSelected = { emoji ->
                selectedEmoji = emoji
                showEmojiPicker = false
            },
            onDismiss = { showEmojiPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficialEmojiPickerDialog(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var isEmojiPickerReady by remember { mutableStateOf(false) }
    
    fun configureEmojiPicker(emojiPickerView: EmojiPickerView) {
        emojiPickerView.setOnEmojiPickedListener { emojiViewItem ->
            val emoji = emojiViewItem.emoji
            onEmojiSelected(emoji)
            scope.launch { sheetState.hide() }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch { sheetState.show() }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = {
            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Choose Goal Icon",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
            ) {
                if (!isEmojiPickerReady) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                "Loading emojis...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                AndroidView(
                    factory = { context ->
                        EmojiPickerView(context).apply {
                            configureEmojiPicker(this)
                            
                            post { isEmojiPickerReady = true }
                        }
                    },
                    update = { emojiPickerView ->
                        configureEmojiPicker(emojiPickerView)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}