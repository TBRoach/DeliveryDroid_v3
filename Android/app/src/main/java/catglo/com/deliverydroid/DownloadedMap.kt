package catglo.com.deliverydroid

import android.annotation.SuppressLint
import android.content.Context
import catglo.com.deliverydroid.R.string.map
import catglo.com.deliverydroid.homeScreen.latLong
import com.google.android.gms.location.LocationServices
import org.mapsforge.core.model.BoundingBox
import java.io.*
import java.lang.IllegalStateException

interface MapReadyListener {
    fun onMapReady(map:DownloadedMap)
}

data class DownloadedMap(var title: String,
                         var mapFile: File? = null,
                         var poiFile: File? = null,
                         var bounds: BoundingBox?=null) : Serializable
{
    companion object {
        const val fileName = "mapslist"

        fun loadMapsList(context: Context) : ArrayList<DownloadedMap> {
            val file = File(context.filesDir, fileName)
            try {
                ObjectInputStream(FileInputStream(file)).use {
                    it.readObject().let {
                        if (it is ArrayList<*>) {
                            return it as ArrayList<DownloadedMap>
                        }
                    }
                }
            } catch (e: FileNotFoundException)
            {
                //ok create a new one
            }
            return ArrayList<DownloadedMap>()
        }

        fun saveMapsList(context: Context, list: ArrayList<DownloadedMap>) {
            val file = File(context.filesDir, fileName)
            ObjectOutputStream(FileOutputStream(file)).use{ it.writeObject(list) }
        }

        @SuppressLint("MissingPermission")
        fun getMapForCurrentLocation(context: Context, litener : MapReadyListener  ) {
            val list = loadMapsList(context)
            val locationProvider = LocationServices.getFusedLocationProviderClient(context)
            list.forEach { map ->
                locationProvider.lastLocation.addOnSuccessListener { location ->
                    map.bounds?.let {
                        try {
                            if (it.contains(location.latLong())) {
                                litener.onMapReady(map)
                                return@addOnSuccessListener
                            }
                        } catch (e:IllegalStateException)
                        {
                            e.printStackTrace()
                            //Not sure why this was getting reported on google play error reporting
                            //No map is better than a crash.
                        }
                    }
                }
            }
        }
    }
}