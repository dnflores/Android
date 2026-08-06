package com.dnflores.wt7bridge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var store: ConfigStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ConfigStore(this)

        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                10,
            )
        }

        val config = store.load()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        fun field(label: String, value: String, password: Boolean = false): EditText {
            layout.addView(TextView(this).apply { text = label })
            return EditText(this).apply {
                setText(value)
                if (password) inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                layout.addView(this)
            }
        }

        val email = field("E-mail Allo Plus", config.email)
        val password = field("Senha Allo Plus", config.password, true)
        val pin = field("PIN de abertura", config.pin, true)
        val device = field("Nome do dispositivo", config.deviceName)
        val token = field("Token da API", config.apiToken)
        val port = field("Porta HTTP", config.listenPort.toString())

        val info = TextView(this).apply {
            text = """
                Endpoints:
                GET  /health
                POST /open/1
                POST /open/2

                Cabeçalho obrigatório:
                Authorization: Bearer TOKEN

                O Allo Plus precisa estar aberto na imagem ao vivo para criar o túnel P2P.
            """.trimIndent()
        }
        layout.addView(info)

        layout.addView(Button(this).apply {
            text = "Salvar e iniciar bridge"
            setOnClickListener {
                store.save(
                    BridgeConfig(
                        email = email.text.toString().trim(),
                        password = password.text.toString(),
                        pin = pin.text.toString(),
                        deviceName = device.text.toString().trim(),
                        apiToken = token.text.toString().trim(),
                        listenPort = port.text.toString().toIntOrNull() ?: 8765,
                    )
                )
                ContextCompat.startForegroundService(
                    this@MainActivity,
                    Intent(this@MainActivity, BridgeService::class.java),
                )
                info.text = "Bridge iniciada na porta ${port.text}. Token salvo."
            }
        })

        layout.addView(Button(this).apply {
            text = "Parar bridge"
            setOnClickListener {
                stopService(Intent(this@MainActivity, BridgeService::class.java))
                info.text = "Bridge parada."
            }
        })

        setContentView(ScrollView(this).apply { addView(layout) })
    }
}
