package com.despesas.gestor

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.despesas.gestor.ui.AppGate
import com.despesas.gestor.ui.AppRoot
import com.despesas.gestor.ui.theme.GestorTheme

// FragmentActivity é necessário para o BiometricPrompt (bloqueio da app).
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GestorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppGate {
                        AppRoot()
                    }
                }
            }
        }
    }
}
