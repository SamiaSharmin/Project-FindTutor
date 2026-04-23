package com.example.project_findtutor

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RoleRegisterActivity : AppCompatActivity() {

    lateinit var cardTutor: CardView
    lateinit var cardStudent: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_role_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cardTutor = findViewById<CardView>(R.id.cardTutor)
        cardStudent = findViewById<CardView>(R.id.cardStudent)

        cardTutor.setOnClickListener {
            val intent = Intent(this, TutorRegisterActivity::class.java)
            startActivity(intent)
        }
        cardStudent.setOnClickListener {
            val intent = Intent(this, StudentRegisterActivity::class.java)
            startActivity(intent)
        }


    }
}