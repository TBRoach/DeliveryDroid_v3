package catglo.com.deliverydroid.shift

import android.app.Activity
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.appcompat.app.AlertDialog
import catglo.com.deliverydroid.R
import kotlinx.android.synthetic.main.shift_activity.*
import org.joda.time.DateTime
import org.joda.time.MutableDateTime

class PastShiftActivity : ShiftActivity() {

    override fun shiftTimeClickListener(
        time: MutableDateTime,
        setter: (year:Int, month: Int, day : Int, hour : Int, minute : Int ) -> Unit
    ): View.OnClickListener {
        return View.OnClickListener {
            val customView = View.inflate(this@PastShiftActivity, R.layout.time_date_picker_dialog, null)
            val timePicker = customView.findViewById<TimePicker>(R.id.timePicker)
            val datePicker = customView.findViewById<DatePicker>(R.id.weekdayPicker)
            val nowButton = customView.findViewById<Button>(R.id.nowButton)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                timePicker.hour = time.hourOfDay
                timePicker.minute = time.minuteOfHour
            } else {
                @Suppress("DEPRECATION")
                timePicker.currentHour = time.hourOfDay
                @Suppress("DEPRECATION")
                timePicker.currentMinute = time.minuteOfHour
            }
            nowButton.setOnClickListener {
                val now = DateTime.now()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    timePicker.hour = now.hourOfDay
                    timePicker.minute = now.minuteOfHour
                } else {
                    timePicker.currentHour = now.hourOfDay
                    timePicker.currentMinute = now.minuteOfHour
                }
                datePicker.updateDate(now.year, now.monthOfYear - 1, now.dayOfMonth)
            }
            datePicker.updateDate(time.year, time.monthOfYear - 1, time.dayOfMonth)
            AlertDialog.Builder(this@PastShiftActivity)
                .setView(customView)
                .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->

                    shift.endTime.year = datePicker.year
                    shift.endTime.monthOfYear = datePicker.month + 1
                    shift.endTime.dayOfMonth = datePicker.dayOfMonth
                    var hour = 0
                    var minute = 0
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        hour = timePicker.hour
                        minute = timePicker.minute
                    } else {
                        hour = timePicker.currentHour
                        minute = timePicker.currentMinute
                    }
                    setter(datePicker.year,datePicker.month + 1,datePicker.dayOfMonth,hour,minute)
                    updateUI()
                }
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deleteShiftClickable?.visibility = View.GONE
        newShiftButton?.visibility = View.GONE
    }
}
