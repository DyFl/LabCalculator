package com.example.labcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.labcalculator.ui.LabCalculatorApp
import com.example.labcalculator.ui.theme.LabCalculatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LabCalculatorTheme {
                LabCalculatorApp()
            }
        }
    }
}
