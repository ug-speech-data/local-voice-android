package com.hrd.localvoice

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.*
import androidx.work.testing.WorkManagerTestInitHelper
import com.hrd.localvoice.workers.UploadWorker
import org.hamcrest.Matchers.`is`
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class InstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.hrd.localvoice", appContext.packageName)
    }

    @Test
    @Throws(Exception::class)
    fun testPeriodicWork() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        // Define input data
        val input = workDataOf("KEY_1" to 1, "KEY_2" to 2)

        // Create request
//        val request = PeriodicWorkRequestBuilder<UploadWorker>(15, TimeUnit.MINUTES)
//            .setInputData(input)
//            .build()

        // Schedule audio upload.
        val constraints = Constraints.Builder().apply {
            setRequiredNetworkType(NetworkType.CONNECTED)
            setRequiresBatteryNotLow(true)
        }.build()

        val request = PeriodicWorkRequest.Builder(
            UploadWorker::class.java, 15, TimeUnit.MINUTES
        ).setInputData(input)
            .setConstraints(constraints)
            .build()

        val workManager = WorkManager.getInstance(appContext)
        val testDriver = WorkManagerTestInitHelper.getTestDriver(appContext)

        // Enqueue and wait for result.
        workManager.enqueue(request).result.get()

        // Tells the testing framework the period delay is met
        testDriver?.setAllConstraintsMet(request.id)
        testDriver?.setPeriodDelayMet(request.id)

        // Get WorkInfo and outputData
        val workInfo = workManager.getWorkInfoById(request.id).get()

        // Assert
        assertThat(workInfo.state, `is`(WorkInfo.State.ENQUEUED))
    }

}