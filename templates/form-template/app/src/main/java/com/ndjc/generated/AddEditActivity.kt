package com.ndjc.generated

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class AddEditActivity : AppCompatActivity() {
    companion object { const val EXTRA_MODEL = "model" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit)

        val name = findViewById<EditText>(R.id.edt_name)
        val years = findViewById<EditText>(R.id.edt_years)
        val engine = findViewById<EditText>(R.id.edt_engine)
        val decade = findViewById<EditText>(R.id.edt_decade)
        val summary = findViewById<EditText>(R.id.edt_summary)

        (intent.getSerializableExtra(EXTRA_MODEL) as? Model)?.let {
            name.setText(it.name); years.setText(it.years)
            engine.setText(it.engine); decade.setText(it.decade)
            summary.setText(it.summary)
        }

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            val m = Model(
                name = name.text.toString().trim(),
                years = years.text.toString().trim(),
                engine = engine.text.toString().trim(),
                decade = decade.text.toString().trim(),
                summary = summary.text.toString().trim()
            )
            setResult(RESULT_OK, Intent().putExtra(EXTRA_MODEL, m))
            finish()
        }
    }
}
