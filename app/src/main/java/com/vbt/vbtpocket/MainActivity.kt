package com.vbt.vbtpocket

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vbt.vbtpocket.ui.screens.*
import com.vbt.vbtpocket.ui.theme.VBTPocketTheme
import com.vbt.vbtpocket.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        ActivityCompat.requestPermissions(this, permissions, 1)

        setContent {
            VBTPocketTheme {
                val vbtViewModel: MainViewModel = viewModel()
                var selectedTab by remember { mutableIntStateOf(0) }

                var isTrainingActive by remember { mutableStateOf(false) }
                var showReadiness by remember { mutableStateOf(false) }
                var activeSessionId by remember { mutableIntStateOf(0) }
                var activeWorkoutTitle by remember { mutableStateOf("") }

                if (showReadiness) {
                    ReadinessScreen(vbtViewModel) { showReadiness = false; isTrainingActive = true }
                } else if (isTrainingActive) {
                    ActiveWorkoutScreen(activeSessionId, activeWorkoutTitle, vbtViewModel) { isTrainingActive = false }
                } else {
                    Scaffold(
                        bottomBar = {
                            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    label = { Text("Calendario") },
                                    icon = { Icon(Icons.Default.DateRange, null) }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    label = { Text("Biblioteca") },
                                    icon = { Icon(Icons.Default.List, null) }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 },
                                    label = { Text("Rendimiento") },
                                    icon = { Icon(Icons.Default.TrendingUp, null) }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Surface(modifier = Modifier.padding(innerPadding)) {
                            when (selectedTab) {
                                0 -> CalendarScreen(vbtViewModel) { id, title ->
                                    activeSessionId = id
                                    activeWorkoutTitle = title
                                    showReadiness = true
                                }
                                1 -> WorkoutListScreen(vbtViewModel)
                                2 -> PerformanceDashboardScreen(vbtViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}