package com.example.responderapp.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.responderapp.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pager Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(pagerState.pageCount) { iteration ->
                        val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.LightGray
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(color)
                                .size(10.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < 3) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            viewModel.completeOnboarding(onOnboardingComplete)
                        }
                    },
                    modifier = Modifier.height(50.dp)
                ) {
                    if (pagerState.currentPage < 3) {
                        Text("Next")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    } else {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Get Started")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> OnboardingPage(
                        title = "Welcome to Lifeline",
                        description = "Your companion for emergency response and patient care tracking.",
                        iconRes = R.drawable.ic_launcher_foreground // Placeholder
                    )
                    1 -> OnboardingPage(
                        title = "Patient Management",
                        description = "View your assigned patients, update their status, and provide critical care updates in real-time.",
                        iconRes = R.drawable.ic_launcher_foreground
                    )
                    2 -> OnboardingPage(
                        title = "Real-time Tracking",
                        description = "Your location helps supervisors coordinate response efforts efficiently.",
                        iconRes = R.drawable.ic_launcher_foreground
                    )
                    3 -> ProfileVerificationPage(
                        uiState = uiState,
                        onNameChange = { viewModel.onFullNameChange(it) },
                        onPhoneChange = { viewModel.onPhoneChange(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingPage(
    title: String,
    description: String,
    iconRes: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Placeholder for illustration
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(Color(0xFFE0E0E0), CircleShape),
            contentAlignment = Alignment.Center
        ) {
             Icon(
                 painter = painterResource(id = iconRes),
                 contentDescription = null,
                 modifier = Modifier.size(100.dp),
                 tint = MaterialTheme.colorScheme.primary
             )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
    }
}

@Composable
fun ProfileVerificationPage(
    uiState: OnboardingUiState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Verify Your Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Profile Pic Placeholder
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Color.LightGray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
             Icon(Icons.Default.Check, contentDescription = null) // Placeholder
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = uiState.fullName,
            onValueChange = onNameChange,
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = uiState.email, // Read only
            onValueChange = {},
            label = { Text("Email") },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = uiState.phone,
            onValueChange = onPhoneChange,
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
