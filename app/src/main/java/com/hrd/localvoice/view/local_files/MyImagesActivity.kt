package com.hrd.localvoice.view.local_files

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.adapters.ImageAdapter
import com.hrd.localvoice.databinding.ActivityMyImagesBinding
import com.hrd.localvoice.models.Configuration
import com.hrd.localvoice.models.Image

class MyImagesActivity : AppCompatActivity() {
    private lateinit var adapter: ImageAdapter
    private lateinit var binding: ActivityMyImagesBinding
    private lateinit var viewModel: MyAudiosActivityViewModel
    private var images: List<Image>? = null
    private var configuraton: Configuration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyImagesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = "My Images"
        setContentView(binding.root)

        // Show back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setup()
        loadImages()

        // Swipe to refresh
        binding.swiperefresh.setOnRefreshListener {
            loadImages()
        }

        // Load configuration
        AppRoomDatabase.databaseWriteExecutor.execute {
            configuraton = AppRoomDatabase.INSTANCE?.ConfigurationDao()?.getConfiguration()
        }
    }

    private fun setup() {
        viewModel = ViewModelProvider(this)[MyAudiosActivityViewModel::class.java]
        adapter = ImageAdapter(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun loadImages() {
        viewModel.getImages()?.observe(this) {
            images = it
            adapter.setData(it)
            binding.swiperefresh.isRefreshing = false

            if (it.isEmpty()) {
                binding.infoText.visibility = View.VISIBLE
            } else {
                binding.infoText.visibility = View.GONE
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}