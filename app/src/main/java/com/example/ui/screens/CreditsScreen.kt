package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.SertaoObsidian
import com.example.ui.theme.SolarGold

@Composable
fun CreditsScreen(
    heroName: String,
    isEpilogue: Boolean,
    onReturnToMenu: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.img_hero_sertao),
            contentDescription = "Alvorada sobre o Sertão",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SertaoObsidian.copy(alpha = 0.72f),
                            SertaoObsidian.copy(alpha = 0.94f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.widthIn(max = 600.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isEpilogue) "EPÍLOGO DA CAMPANHA" else "CRÉDITOS FINAIS",
                    style = MaterialTheme.typography.labelLarge,
                    color = SolarGold
                )
                Text(
                    text = "A Lenda dos Caçadores do Sertão",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )

                if (isEpilogue) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("O Novo Amanhecer", style = MaterialTheme.typography.titleLarge, color = SolarGold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Com a queda do Coronel Umbra na Fortaleza do Eclipse, o céu do sertão voltou a clarear. $heroName retornou à Vila das Almas ao lado de Inaiá, Mestre Severino, Tainá e Mateus, honrando o sacrifício de Antônio Brasa e reconstruindo a nova geração dos Guardiões da Chama.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                CreditBlock(
                    title = "Criação e Direção do Projeto",
                    lines = listOf(
                        "Projeto idealizado pelo Criador do Universo 'Caçadores do Sertão'",
                        "Motor 3D Cel-Shaded em OpenGL ES + Jetpack Compose",
                        "Síntese Musical Nordestina (Viola, Rabeca, Zabumba, Triângulo) em Tempo Real",
                        "Dublagem PT-BR via Android TextToSpeech"
                    )
                )

                CreditBlock(
                    title = "Tecnologias e Licenças de Terceiros",
                    lines = listOf(
                        "Android Jetpack Compose, Material 3 & Credential Manager — Apache License 2.0",
                        "Firebase Authentication & Cloud Firestore — Apache License 2.0",
                        "Google Fonts: Cinzel & Plus Jakarta Sans — SIL Open Font License (OFL)",
                        "Ícones Material Symbols — Apache License 2.0"
                    )
                )

                CreditBlock(
                    title = "Agradecimentos e Versão",
                    lines = listOf(
                        "Inspirado pela cultura, música e resistência do povo nordestino.",
                        "Versão do Jogo: 1.0.0"
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onReturnToMenu,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("credits_return_menu_button")
                ) {
                    Text("Retornar ao Menu Inicial", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun CreditBlock(title: String, lines: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = SolarGold)
            Spacer(modifier = Modifier.height(6.dp))
            lines.forEach { line ->
                Text(
                    text = "• $line",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
