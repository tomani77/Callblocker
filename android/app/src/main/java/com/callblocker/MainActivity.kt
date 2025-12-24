package com.callblocker

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate

class MainActivity : ReactActivity() {

  companion object {
    private const val TAG = "MainActivity"
    private const val REQUEST_ROLE_CALL_SCREENING = 1001
    private const val REQUEST_PERMISSIONS = 1002
    private val REQUIRED_PERMISSIONS = arrayOf(
      android.Manifest.permission.READ_PHONE_STATE,
      android.Manifest.permission.CALL_PHONE,
      android.Manifest.permission.READ_CALL_LOG,
      android.Manifest.permission.WRITE_CALL_LOG,
      android.Manifest.permission.ANSWER_PHONE_CALLS
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Log.d(TAG, "MainActivity onCreate() - App started")
    requestRequiredPermissions()
  }

  private fun requestRequiredPermissions() {
    Log.d(TAG, "Requesting required permissions...")
    ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS)
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    
    if (requestCode == REQUEST_PERMISSIONS) {
      val granted = grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }
      if (granted) {
        Log.d(TAG, "✓ All permissions granted")
        requestCallScreeningRole()
      } else {
        Log.w(TAG, "⚠️ Some permissions were denied")
      }
    }
  }

  private fun requestCallScreeningRole() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      try {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
          if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            Log.d(TAG, "Requesting Call Screening role")
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            startActivityForResult(intent, REQUEST_ROLE_CALL_SCREENING)
          } else {
            Log.d(TAG, "✓ Call screening role already held - App is ready to block calls")
          }
        } else {
          Log.w(TAG, "Call screening role not available on this device")
        }
      } catch (t: Throwable) {
        Log.e(TAG, "Error requesting call screening role: ${t.message}", t)
      }
    } else {
      Log.w(TAG, "Device is below Android 10 - Using CallReceiver for call blocking")
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_ROLE_CALL_SCREENING) {
      Log.d(TAG, "Call screening role request result: $resultCode")
      if (resultCode == RESULT_OK) {
        Log.d(TAG, "✓ User granted Call Screening role - App is ready")
      } else {
        Log.w(TAG, "⚠️ User denied Call Screening role")
      }
    }
  }

  /**
   * Returns the name of the main component registered from JavaScript. This is used to schedule
   * rendering of the component.
   */
  override fun getMainComponentName(): String = "Callblocker"

  /**
   * Returns the instance of the [ReactActivityDelegate]. We use [DefaultReactActivityDelegate]
   * which allows you to enable New Architecture with a single boolean flags [fabricEnabled]
   */
  override fun createReactActivityDelegate(): ReactActivityDelegate =
      DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled)
}
