package catglo.com.deliverydroid.backup


import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import catglo.com.deliveryDatabase.DataBase
import catglo.com.deliverydroid.BuildConfig
import catglo.com.deliverydroid.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.drive.*
import com.google.android.gms.drive.query.Filters
import com.google.android.gms.drive.query.SearchableField
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import kotlinx.android.synthetic.main.backup_to_google_drive_activity.*
import java.io.*

class GoogleDriveBackupRestoreActivity : AppCompatActivity() {
    private var mDriveClient: DriveClient? = null
    private var mDriveResourceClient: DriveResourceClient? = null

    private var mOpenItemTaskSource: TaskCompletionSource<DriveId>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dbFile = if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("DatabaseOnSdcard", false)) {
            File(Environment.getExternalStorageDirectory(), BuildConfig.DATABASE_NAME)
        } else {
            File(applicationContext.filesDir, BuildConfig.DATABASE_NAME)
        }

        setContentView(R.layout.backup_to_google_drive_activity)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        signIn()
        backup?.setOnClickListener { saveFileToDrive() }
        restore?.setOnClickListener { restoreBackup() }
    }

    private fun signIn() {
        Log.i("DD", "Start sign in")
        val googleSignInClient = buildGoogleSignInClient()
        startActivityForResult(googleSignInClient.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    private fun buildGoogleSignInClient(): GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Drive.SCOPE_FILE)
            .build()
        return GoogleSignIn.getClient(this, signInOptions)
    }

    var dbFile : File? = null



    private fun saveFileToDrive() {
        mDriveResourceClient!!
            .createContents()
            .continueWithTask { task -> createFileIntentSender(task.result!!, dbFile!!) }
            .addOnFailureListener { e -> Log.w("DD", "Failed to create new contents.", e) }
    }


    private fun createFileIntentSender(driveContents: DriveContents, dbFileToBackup: File): Task<Void>? {
        try {
            Log.i("DD", "New contents created.")

            val outputStream = driveContents.outputStream
            val input = FileInputStream(dbFileToBackup)

            input.use { fin ->
                outputStream.use { fout ->
                    fin.copyTo(fout)
                }
            }
            input.close()

            val metadataChangeSet = MetadataChangeSet.Builder()
                .setMimeType("delivery/mysql")
                .setTitle("Delivery Droid Database Backup")
                .build()
            // Set up options to configure and display the create file activity.
            val createFileActivityOptions = CreateFileActivityOptions.Builder()
                .setInitialMetadata(metadataChangeSet)
                .setInitialDriveContents(driveContents)
                .build()

            return mDriveClient!!
                .newCreateFileActivityIntentSender(createFileActivityOptions)
                .continueWith { task ->
                    startIntentSenderForResult(task.result, REQUEST_CODE_CREATOR, null, 0, 0, 0)
                    null
                }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SIGN_IN -> {
                Log.i("DD", "Sign in request code")
                // Called after user is signed in.
                if (resultCode == Activity.RESULT_OK) {
                    Log.i("DD", "Signed in successfully.")
                    // Use the last signed in account here since it already have a Drive scope.
                    mDriveClient = Drive.getDriveClient(this, GoogleSignIn.getLastSignedInAccount(this)!!)
                    // Build a drive resource client.
                    mDriveResourceClient =
                            Drive.getDriveResourceClient(this, GoogleSignIn.getLastSignedInAccount(this)!!)
                }
                else
                {
                    //Display sign in error
                }
            }
        }
    }

    protected fun restoreBackup() {
        val openOptions = OpenFileActivityOptions.Builder()
            .setSelectionFilter(Filters.eq(SearchableField.MIME_TYPE, "delivery/mysql"))
            .setActivityTitle(getString(R.string.BackupRestoreData))
            .build()

        mOpenItemTaskSource = TaskCompletionSource()
        mDriveClient!!
            .newOpenFileActivityIntentSender(openOptions)
            .continueWith { task -> startIntentSenderForResult(task.result, REQUEST_CODE_OPEN_ITEM, null, 0, 0, 0)}
        var pickItemTask =  mOpenItemTaskSource!!.task
        pickItemTask
            .addOnSuccessListener { driveId -> getDatabaseFile(driveId.asDriveFile()) }
            .addOnFailureListener { Toast.makeText(this,R.string.something_went_wrong, Toast.LENGTH_LONG).show() }
    }

    private fun getDatabaseFile(file: DriveFile) {
        // [START drive_android_open_file]
        val openFileTask = mDriveResourceClient!!.openFile(file, DriveFile.MODE_READ_ONLY)
        // [END drive_android_open_file]
        // [START drive_android_read_contents]
        openFileTask
            .continueWithTask { task ->
                val contents = task.result
                val output = FileOutputStream(dbFile)

                contents?.inputStream.use { input ->
                    output.use { fileOut ->
                        input?.copyTo(fileOut)
                    }
                }

                val discardTask = mDriveResourceClient!!.discardContents(contents!!)

                discardTask
            }
            .addOnFailureListener {
                Toast.makeText(this,R.string.something_went_wrong, Toast.LENGTH_LONG).show()
            }

    }

    companion object {
        private val REQUEST_CODE_CREATOR = 1
        private val REQUEST_CODE_SIGN_IN = 2
        private val REQUEST_CODE_OPEN_ITEM = 3
    }

}
