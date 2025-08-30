package com.ndjc.generated

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.ndjc.generated.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), ModelsAdapter.Listener {

    private lateinit var binding: ActivityMainBinding
    private val data = mutableListOf<Model>()
    private lateinit var adapter: ModelsAdapter

    private val addLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { r ->
        if (r.resultCode == RESULT_OK && r.data != null) {
            (r.data!!.getSerializableExtra(AddEditActivity.EXTRA_MODEL) as? Model)?.let {
                data.add(0, it)
                adapter.notifyItemInserted(0)
                Persistence.saveModels(this, data)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recycler.layoutManager = LinearLayoutManager(this)
        adapter = ModelsAdapter(data, this)
        binding.recycler.adapter = adapter

        // 加载/首次复制用户数据
        data.clear()
        data.addAll(Persistence.loadModels(this))
        adapter.notifyDataSetChanged()

        binding.fab.setOnClickListener {
            addLauncher.launch(Intent(this, AddEditActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_reset) {
            data.clear()
            data.addAll(Persistence.loadModels(this))
            adapter.notifyDataSetChanged()
            Persistence.saveModels(this, data)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemClick(position: Int) {
        val it = Intent(this, AddEditActivity::class.java)
            .putExtra(AddEditActivity.EXTRA_MODEL, data[position])
        addLauncher.launch(it)
    }

    override fun onItemLongClick(position: Int) {
        AlertDialog.Builder(this)
            .setMessage("删除该条目？")
            .setPositiveButton("删除") { _, _ ->
                data.removeAt(position)
                adapter.notifyItemRemoved(position)
                Persistence.saveModels(this, data)
            }.setNegativeButton("取消", null).show()
    }
}
