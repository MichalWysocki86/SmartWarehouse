package com.mwysocki.smartwarehouse.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.mwysocki.smartwarehouse.viewmodels.PackagesViewModel

class QRScanActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IntentIntegrator(this).initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result.contents != null) {
            val scannedQRCode = result.contents
            Log.d("QRScanActivity", "Scanned QR Code: $scannedQRCode")

            val returnIntent = Intent()
            returnIntent.putExtra("SCANNED_QR", scannedQRCode)
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Call finish() without setting a result to return to the previous screen
        finish()
    }
}