package com.hrd.localvoice.view.local_files

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.hrd.localvoice.AppRoomDatabase
import com.hrd.localvoice.R
import com.hrd.localvoice.adapters.ImageAdapter
import com.hrd.localvoice.databinding.ActivityMyImagesBinding
import com.hrd.localvoice.models.Configuration
import com.hrd.localvoice.models.Image
import java.io.File

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

    private fun showAlertDialogBox() {
        val dialog =
            AlertDialog.Builder(this)
                .setTitle("DELETE DESCRIBED IMAGES")
                .setMessage("Delete all images that have been described for the required number of times.")
                .setCancelable(false)
                .setNegativeButton("Cancel") { _, _ -> }.setPositiveButton("Delete") { _, _ ->
                    images?.forEach { image ->
                        if (configuraton?.maxImageDescriptionCount != null && image.descriptionCount >= configuraton!!.maxImageDescriptionCount!!) {
                            val file = image.localURl?.let { File(it) }
                            file?.delete()
                            viewModel.deleteImage(image)
                        }
                    }
                }

        dialog.create()
        dialog.show()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.my_audios_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete_uploaded ->
                showAlertDialogBox()
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }


}