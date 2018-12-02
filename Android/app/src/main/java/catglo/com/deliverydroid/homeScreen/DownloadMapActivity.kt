package catglo.com.deliverydroid.homeScreen

import android.Manifest
import android.Manifest.permission.*
import android.annotation.SuppressLint
import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.download_map_activity.*
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.reader.MapFile
import java.io.*
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.view.*
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.checkSelfPermission
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import catglo.com.deliverydroid.*
import catglo.com.deliverydroid.MapDownloadOption.*
import com.frostwire.jlibtorrent.Priority
import com.frostwire.jlibtorrent.TorrentHandle
import com.frostwire.jlibtorrent.TorrentInfo
import com.masterwok.simpletorrentandroid.TorrentSession
import com.masterwok.simpletorrentandroid.TorrentSessionOptions
import com.masterwok.simpletorrentandroid.contracts.TorrentSessionListener
import com.masterwok.simpletorrentandroid.models.TorrentSessionStatus


import kotlinx.coroutines.*
import java.net.URL


fun Location.latLong(): LatLong {
    return LatLong(this.latitude, this.longitude)
}


class DownloadMapCell(view: View, map: DownloadedMap) : RecyclerView.ViewHolder(view) {
    val title = view.findViewById<TextView>(R.id.mapTitle)!!
    val hasMap = view.findViewById<ImageView>(R.id.hasMap)!!
    val hasPoi = view.findViewById<ImageView>(R.id.hasPoi)!!
    val warningIcon = view.findViewById<ImageView>(R.id.warningIcon)!!
    val warningText = view.findViewById<TextView>(R.id.warningText)!!
    val checkmark = view.findViewById<ImageView>(R.id.checkmark)!!
}

class DownloadMapActivity : DeliveryDroidBaseActivity() {

    @SuppressLint("SetTextI18n")
    fun mapOptionsDialog() {
        val optionsView = View.inflate(this, R.layout.download_map_options, null)
        val helpText = optionsView.findViewById<TextView>(R.id.helpText)
        val helpLink = optionsView.findViewById<TextView>(R.id.helpLink)
        when {
            Settings(this).mapDownloadOption == torrent -> optionsView.findViewById<RadioButton>(R.id.automaticDownloadRadio)
                .isChecked = true
            Settings(this).mapDownloadOption == download -> optionsView.findViewById<RadioButton>(R.id.manualDownloadRadio)
                .isChecked = true
            Settings(this).mapDownloadOption == create -> optionsView.findViewById<RadioButton>(R.id.generateMapRadio)
                .isChecked = true
        }
        optionsView.findViewById<RadioGroup>(R.id.mapDownloadRadioGroup)
            .setOnCheckedChangeListener { group, checkedId ->
                when (checkedId) {
                    R.id.automaticDownloadRadio -> {
                        helpText.setText(R.string.map_bittorrent_desc)
                        helpLink.visibility = View.GONE
                        Settings(this).mapDownloadOption = torrent
                    }
                    R.id.manualDownloadRadio -> {
                        helpText.text = getString(R.string.map_download_desc)
                        helpLink.text = "http://download.mapsforge.org/"
                        helpLink.visibility = View.VISIBLE
                        Settings(this).mapDownloadOption = download
                    }
                    R.id.generateMapRadio -> {
                        helpText.text = getString(R.string.map_create_desc)
                        helpLink.text = "https://github.com/mapsforge/mapsforge"
                        helpLink.visibility = View.VISIBLE
                        Settings(this).mapDownloadOption = create
                    }
                }
            }
        helpLink.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(helpLink.text.toString())))
        }
        AlertDialog.Builder(this)
            .setView(optionsView)
            .setPositiveButton(android.R.string.ok, null)
            .setOnDismissListener {
                if (Settings(this).mapDownloadOption == none) {
                    finish()
                }
                else {
                    scanFileSystem()
                }
            }
            .setTitle("Choose map source")
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.download_map_activity)

        setSupportActionBar(toolbar)


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.download_map_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.settings -> mapOptionsDialog()
            R.id.download -> startActivity(Intent(this,ChooseDownloadActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }


    var listOfDownloadableMaps = ArrayList<DownloadableMap>()

    override fun onResume() {
        super.onResume()

        //First we need to check for system permissions
        if (checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED
            && checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED
        ) {
            requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), 0)
            return
        }
        if (checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE), 0)
            return
        }
        //Next make sure the user has chosen his preferred way of getting maps
        if (Settings(this).mapDownloadOption == none) {
            mapOptionsDialog()
            return
        }

        //If we have all the permissions we need the next thing is to scan for files on the file system
        scanFileSystem()

    }

    private fun scanFileSystem() {
        //Scan the sd card folder for .map and .poi files
        var mapFilesList = ArrayList<DownloadedMap>()
        mapFilesList.run {
            addAll(searchForMapFiles(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)))
            addAll(searchForMapFiles(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)))
            addAll(searchForMapFiles(Environment.getExternalStorageDirectory()))

        }
        val oldList = DownloadedMap.loadMapsList(this)
        val map = HashMap<String, DownloadedMap>()
        val mapOld = HashMap<String, DownloadedMap>()
        oldList.forEach {
            if (it.mapFile != null) {
                mapOld[it.mapFile!!.nameWithoutExtension] = it
            } else if (it.poiFile != null) {
                mapOld[it.poiFile!!.nameWithoutExtension] = it
            }
        }
        mapFilesList.forEach {
            var key: String? = null
            if (it.mapFile != null) {
                key = it.mapFile!!.nameWithoutExtension
            } else if (it.poiFile != null) {
                key = it.poiFile!!.nameWithoutExtension
            }
            if (key != null) {
                if (mapOld[key] != null) {
                    mapOld[key]?.title?.let { oldTitle ->
                        it.title = oldTitle
                    }
                }
                map[key] = it
            }
        }

        mapFilesList = ArrayList(map.values)

        if (mapFilesList.size > 0) {
            mapFilesList.forEach { downloadedMap ->
                //Load the map files and verify the bounds
                if (downloadedMap.mapFile != null) {
                    val mapFile = MapFile(downloadedMap.mapFile)
                    downloadedMap.bounds = mapFile.boundingBox()
                }
            }
            DownloadedMap.saveMapsList(this, mapFilesList)
            downloadedMapList.adapter = DownloadMapAdapter(this, mapFilesList)
            downloadedMapList.visibility = View.VISIBLE
        }
    }

    fun searchForMapFiles(pathToSearch: File): ArrayList<DownloadedMap> {
        val result = ArrayList<DownloadedMap>()
        val mapFile = HashMap<String, DownloadedMap>()
        if (pathToSearch.exists() && pathToSearch.isDirectory) {
            val fileList = pathToSearch.listFiles()
            fileList?.forEach { file ->
                if (file.isDirectory && file.name=="MapsforgeMaps") {
                    //This is the torrent download folder
                    //we should search this folder for map files
                    result.addAll(searchForMapFiles((file)))
                    //and each of its subdirectories for map files
                    file.listFiles().forEach { if (it.isDirectory) result.addAll(searchForMapFiles(it)) }
                }
                else {
                    Log.i("DD", file.absolutePath)
                    if (file.extension == "map") {
                        val map = mapFile[file.nameWithoutExtension] ?: DownloadedMap(file.nameWithoutExtension)
                        map.mapFile = file
                        mapFile[file.nameWithoutExtension] = map

                    }
                    if (file.extension == "poi") {
                        val map = mapFile[file.nameWithoutExtension] ?: DownloadedMap(file.nameWithoutExtension)
                        map.poiFile = file
                        mapFile[file.nameWithoutExtension] = map
                    }
                }
            }
            result.addAll(mapFile.values)
        }
        return result
    }

}


class DownloadMapAdapter(val context: Context, val maps: ArrayList<DownloadedMap>) :
    RecyclerView.Adapter<DownloadMapCell>() {
    override fun onCreateViewHolder(view: ViewGroup, position: Int): DownloadMapCell {
        return DownloadMapCell(View.inflate(context, R.layout.download_map_cell, null), maps[position])
    }

    override fun getItemCount(): Int {
        return maps.size
    }


    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(cell: DownloadMapCell, position: Int) {
        val map = maps[position]
        cell.title.text = map.title
        if (map.poiFile == null && map.mapFile == null) {
            cell.hasPoi.visibility = View.GONE
            cell.hasMap.visibility = View.GONE
            cell.warningText.text = if (map.poiFile == null) {
                "Missing POI file"
            } else {
                "Missing MAP file"
            }
            cell.warningText.visibility = View.VISIBLE
            cell.warningIcon.visibility = View.VISIBLE
        } else {
            cell.warningText.visibility = View.GONE
            cell.warningIcon.visibility = View.GONE
            if (map.poiFile != null) {
                cell.hasPoi.visibility = View.VISIBLE
            } else {
                cell.hasPoi.visibility = View.GONE
            }
            if (map.mapFile != null) {
                cell.hasMap.visibility = View.VISIBLE
            } else {
                cell.hasMap.visibility = View.GONE
            }
        }
        cell.checkmark.visibility = View.INVISIBLE
        val locationProvider = LocationServices.getFusedLocationProviderClient(context)
        locationProvider.lastLocation.addOnSuccessListener { location ->
            if (map.bounds != null) {
                if (map.bounds!!.contains(location.latLong())) {
                    cell.checkmark.visibility = View.VISIBLE
                }
            }
        }
    }
}
