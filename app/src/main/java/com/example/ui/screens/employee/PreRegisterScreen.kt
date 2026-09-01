package com.example.ui.screens.employee

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.utils.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class SchedulePreset(
    val title: String,
    val isTomorrow: Boolean,
    val hourOfDay: Int,
    val minute: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreRegisterScreen(
    isLoading: Boolean,
    onSubmit: (
        visitorName: String,
        visitorMobile: String,
        visitorCompany: String,
        visitorEmail: String?,
        purpose: String,
        expectedDateTime: String,
        departmentId: Int
    ) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    var visitorName by remember { mutableStateOf("") }
    var visitorMobile by remember { mutableStateOf("") }
    var visitorCompany by remember { mutableStateOf("") }
    var visitorEmail by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("Strategic Client Meeting") }

    // Schedule Calendar State initialized to 1 hour from now
    val selectedCalendar = remember {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    var calendarUpdateTrigger by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val displayDateStr = remember(calendarUpdateTrigger) {
        val sdf = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
        sdf.format(selectedCalendar.time)
    }

    val displayTimeStr = remember(calendarUpdateTrigger) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.format(selectedCalendar.time)
    }

    val schedulePresets = remember {
        listOf(
            SchedulePreset("Today in 1h", false, -1, -1),
            SchedulePreset("Today 02:30 PM", false, 14, 30),
            SchedulePreset("Today 04:30 PM", false, 16, 30),
            SchedulePreset("Tomorrow 10:00 AM", true, 10, 0),
            SchedulePreset("Tomorrow 02:30 PM", true, 14, 30)
        )
    }

    fun applyPreset(preset: SchedulePreset) {
        val now = Calendar.getInstance()
        if (preset.hourOfDay == -1) {
            now.add(Calendar.HOUR_OF_DAY, 1)
            selectedCalendar.timeInMillis = now.timeInMillis
        } else {
            selectedCalendar.timeInMillis = now.timeInMillis
            if (preset.isTomorrow) {
                selectedCalendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            selectedCalendar.set(Calendar.HOUR_OF_DAY, preset.hourOfDay)
            selectedCalendar.set(Calendar.MINUTE, preset.minute)
            selectedCalendar.set(Calendar.SECOND, 0)
            selectedCalendar.set(Calendar.MILLISECOND, 0)
        }
        calendarUpdateTrigger = System.currentTimeMillis()
    }

    fun showCustomDatePicker() {
        val year = selectedCalendar.get(Calendar.YEAR)
        val month = selectedCalendar.get(Calendar.MONTH)
        val day = selectedCalendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(context, { _, y, m, d ->
            selectedCalendar.set(Calendar.YEAR, y)
            selectedCalendar.set(Calendar.MONTH, m)
            selectedCalendar.set(Calendar.DAY_OF_MONTH, d)
            calendarUpdateTrigger = System.currentTimeMillis()

            // Open Time Picker right after date selection
            val hour = selectedCalendar.get(Calendar.HOUR_OF_DAY)
            val min = selectedCalendar.get(Calendar.MINUTE)
            TimePickerDialog(context, { _, h, minVal ->
                selectedCalendar.set(Calendar.HOUR_OF_DAY, h)
                selectedCalendar.set(Calendar.MINUTE, minVal)
                selectedCalendar.set(Calendar.SECOND, 0)
                selectedCalendar.set(Calendar.MILLISECOND, 0)
                calendarUpdateTrigger = System.currentTimeMillis()
            }, hour, min, false).show()
        }, year, month, day).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
        }.show()
    }

    fun showCustomTimePicker() {
        val hour = selectedCalendar.get(Calendar.HOUR_OF_DAY)
        val min = selectedCalendar.get(Calendar.MINUTE)
        TimePickerDialog(context, { _, h, minVal ->
            selectedCalendar.set(Calendar.HOUR_OF_DAY, h)
            selectedCalendar.set(Calendar.MINUTE, minVal)
            selectedCalendar.set(Calendar.SECOND, 0)
            selectedCalendar.set(Calendar.MILLISECOND, 0)
            calendarUpdateTrigger = System.currentTimeMillis()
        }, hour, min, false).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pre-Register Visitor", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                        Text("Generate Secure QR & 6-Digit OTP Pass", fontSize = 11.sp, color = AccentCyanGlow)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavyDark)
            )
        },
        containerColor = SlateLightBackground,
        contentWindowInsets = WindowInsets.navigationBars
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Visitor Details Section
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SlateLightCard,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(StatusApprovedGreenContainer, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = StatusApprovedGreenText, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Visitor & Pass Information", fontWeight = FontWeight.Bold, color = DeepNavyDark, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Visitor Full Name *", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepNavyDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = visitorName,
                        onValueChange = { visitorName = it },
                        placeholder = { Text("e.g. Meera Kapoor") },
                        singleLine = true,
                        colors = vmsOutlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("preregister_name_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Mobile Number (For Pass Delivery) *", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepNavyDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = visitorMobile,
                        onValueChange = { visitorMobile = it },
                        placeholder = { Text("e.g. 9876500000") },
                        singleLine = true,
                        colors = vmsOutlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("preregister_mobile_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Organization / Client Company", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepNavyDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = visitorCompany,
                        onValueChange = { visitorCompany = it },
                        placeholder = { Text("e.g. Microsoft Azure Enterprise") },
                        singleLine = true,
                        colors = vmsOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Email Address (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepNavyDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = visitorEmail,
                        onValueChange = { visitorEmail = it },
                        placeholder = { Text("e.g. meera@microsoft.com") },
                        singleLine = true,
                        colors = vmsOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Schedule & Purpose Section
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SlateLightCard,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(NavySurface, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Event, contentDescription = null, tint = AccentCyanGlow, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Expected Schedule & Purpose", fontWeight = FontWeight.Bold, color = DeepNavyDark, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Purpose of Meeting *", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepNavyDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = purpose,
                        onValueChange = { purpose = it },
                        placeholder = { Text("e.g. Project Demo, Vendor Meeting") },
                        singleLine = true,
                        colors = vmsOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Quick Schedule Selection", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepNavyDark)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(schedulePresets) { preset ->
                            FilterChip(
                                selected = false,
                                onClick = { applyPreset(preset) },
                                label = { Text(preset.title, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SlateLightBackground,
                                    labelColor = DeepNavyDark
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Selected Date/Time Display Card with Picker Triggers
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SlateLightBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SlateLightBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("SCHEDULED ARRIVAL TIME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateLightTextSecondary, letterSpacing = 0.5.sp)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(displayDateStr, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DeepNavyDark)
                                    Text(displayTimeStr, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = AccentBlue)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { showCustomDatePicker() },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Date", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { showCustomTimePicker() },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Schedule, contentDescription = "Pick Time", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Time", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Submit Button
            Button(
                onClick = {
                    if (visitorName.isBlank() || visitorMobile.isBlank()) return@Button
                    val isoTimestamp = DateTimeUtils.toIso8601String(selectedCalendar)
                    onSubmit(
                        visitorName,
                        visitorMobile,
                        visitorCompany,
                        visitorEmail.ifBlank { null },
                        purpose,
                        isoTimestamp,
                        1
                    )
                },
                enabled = visitorName.isNotBlank() && visitorMobile.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = StatusApprovedGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_preregister_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GENERATE SECURE OTP & QR PASS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
