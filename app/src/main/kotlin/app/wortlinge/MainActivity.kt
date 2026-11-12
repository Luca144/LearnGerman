package app.wortlinge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.wortlinge.ui.list.PetListScreen
import app.wortlinge.ui.theme.WortlingeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WortlingeTheme {
                PetListScreen()
            }
        }
    }
}
