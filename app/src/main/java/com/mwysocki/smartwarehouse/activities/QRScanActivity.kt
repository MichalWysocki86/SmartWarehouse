package com.mwysocki.smartwarehouse.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.mwysocki.smartwarehouse.viewmodels.PackagesViewModel

class QRScanActivity : ComponentActivity() {
    // Assuming viewModel is correctly initialized
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IntentIntegrator(this).initiateScan()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result.contents != null) {
            val scannedQRCode = result.contents
            val returnIntent = Intent()
            returnIntent.putExtra("SCANNED_QR", scannedQRCode)
            setResult(Activity.RESULT_OK, returnIntent)
            finish() // Finish this activity to return to the previous screen
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}