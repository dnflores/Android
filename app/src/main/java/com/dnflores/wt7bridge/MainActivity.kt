package com.dnflores.wt7bridge

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.quvii.p2pv2.QvP2PV2Api

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val status = findViewById<TextView>(R.id.status)

        try {
            // Prova principal desta versão:
            // carregar as bibliotecas oficiais em um app independente.
            QvP2PV2Api.setDefaultServiceMask(768)
            status.text = "Bibliotecas oficiais carregadas com sucesso.\n" +
                    "Próxima etapa: preencher createP2PClient() com os campos da nuvem."
        } catch (t: Throwable) {
            status.text = "Falha ao carregar biblioteca: ${t::class.java.name}\n${t.message}"
        }
    }
}
