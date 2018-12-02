package catglo.com.deliverydroid.shift

import androidx.appcompat.app.AlertDialog

import android.os.Bundle
import android.view.View
import catglo.com.deliveryDatabase.Shift
import catglo.com.deliveryDatabase.TipTotalData
import catglo.com.deliverydroid.DeliveryDroidBaseActivity
import catglo.com.deliverydroid.R
import catglo.com.deliverydroid.Utils
import kotlinx.android.synthetic.main.start_end_shift_form.*
import kotlinx.android.synthetic.main.shift_start_end_activity.*

import android.app.TimePickerDialog
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import catglo.com.deliverydroid.widgets.MeterView

import org.joda.time.MutableDateTime
import travel.ithaka.android.horizontalpickerlib.PickerLayoutManager
import java.lang.NumberFormatException
import java.text.DateFormatSymbols
import java.util.*

class StringViewHolder(itemView: View):RecyclerView.ViewHolder(itemView)

fun Locale.isImperial() : Boolean {
    when (Locale.getDefault().country.toUpperCase()) {
        "GB", "MM", "LR", "US" -> return true
        else -> return false
    }
}

open class ShiftStartEndActivity : DeliveryDroidBaseActivity() {

    internal var whichShift: Int = 0

    lateinit var shift: Shift

    private var tips: TipTotalData? = null

    open fun shiftStartTimeClickListener() : View.OnClickListener {
        return View.OnClickListener {
            val customView = View.inflate(this@ShiftStartEndActivity,R.layout.time_date_picker_dialog,null)
            val timePicker = customView.findViewById<TimePicker>(R.id.timePicker)
            val datePicker = customView.findViewById<DatePicker>(R.id.weekdayPicker)
            val layoutManager = PickerLayoutManager(this@ShiftStartEndActivity,PickerLayoutManager.HORIZONTAL,false)
            val symbols = DateFormatSymbols().weekdays
            val time = shift.startTime
            timePicker.hour = time.hourOfDay
            timePicker.minute = time.minuteOfHour
            datePicker.updateDate(time.year,time.monthOfYear-1,time.dayOfMonth)
            AlertDialog.Builder(this@ShiftStartEndActivity)
                .setView(customView)
                .setPositiveButton(android.R.string.ok){ _:DialogInterface, _: Int ->
                    shift.startTime.year = datePicker.year
                    shift.startTime.monthOfYear = datePicker.month+1
                    shift.startTime.dayOfMonth = datePicker.dayOfMonth
                    shift.startTime.hourOfDay = timePicker.hour
                    shift.startTime.minuteOfHour = timePicker.minute
                    dataBase.saveShift(shift)
                    updateUI()
                }
                .show()
        }
    }

    open fun shiftEndTimeClickListener() : View.OnClickListener {
        return View.OnClickListener {
            val customView = View.inflate(this@ShiftStartEndActivity,R.layout.time_date_picker_dialog,null)
            val timePicker = customView.findViewById<TimePicker>(R.id.timePicker)
            val datePicker = customView.findViewById<DatePicker>(R.id.weekdayPicker)
            val layoutManager = PickerLayoutManager(this@ShiftStartEndActivity,PickerLayoutManager.HORIZONTAL,false)
            val symbols = DateFormatSymbols().weekdays
            val time = shift.endTime
            timePicker.hour = time.hourOfDay
            timePicker.minute = time.minuteOfHour
            datePicker.updateDate(time.year,time.monthOfYear-1,time.dayOfMonth)
            AlertDialog.Builder(this@ShiftStartEndActivity)
                .setView(customView)
                .setPositiveButton(android.R.string.ok){ _:DialogInterface, _: Int ->
                    shift.endTime.year = datePicker.year
                    shift.endTime.monthOfYear = datePicker.month+1
                    shift.endTime.dayOfMonth = datePicker.dayOfMonth
                    shift.endTime.hourOfDay = timePicker.hour
                    shift.endTime.minuteOfHour = timePicker.minute
                    dataBase.saveShift(shift)
                    updateUI()
                }
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.shift_start_end_activity)

        deleteShiftClickable.setOnClickListener {
            AlertDialog.Builder(this@ShiftStartEndActivity).run {
                setTitle("Delete shift?")
                setMessage("Are you sure you want to delete this shift?")
                setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dataBase.deleteShift(whichShift)
                    dialog.dismiss()
                    updateUI()
                }
                setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
                    show()
            }

        }

        newShiftButton?.setOnClickListener {
            dataBase.setNextShift()
            updateUI()
        }


        /*Took this out along with the XML until I could get a really good time tracking code
        timeClockClickable.setOnClickListener {
            val intent = Intent(applicationContext, ShiftStartEndDayActivity::class.java)
            intent.putExtra("ID", whichShift)
            startActivity(intent)
        }*/


        val intent = intent
        val id = intent.getIntExtra("ID", -1)
        if (id == -1) {
            whichShift = dataBase.curShift
        } else {
            whichShift = id
        }


        val currentShiftNumber = findViewById<View>(R.id.currentShiftNumber) as TextView
        currentShiftNumber.text = whichShift.toString()

        shiftStartTime.setOnClickListener(shiftStartTimeClickListener())
        shiftEndTime.setOnClickListener(shiftEndTimeClickListener())

        startingOdometer.setOnClickListener {
            val meterLayout = View.inflate(this@ShiftStartEndActivity,R.layout.meter_vew_wrapper,null)
            val meterView = meterLayout.findViewById<MeterView>(R.id.meterView)
            val milesDriven = meterLayout.findViewById<EditText>(R.id.milesDriven)
            meterView.setOnValueChangedListener {
                try {
                    if (milesDriven.text.toString().toInt() != it) {
                        milesDriven.setText("$it")
                    }
                } catch (e:NumberFormatException) {
                    milesDriven.setText("$it")
                }
            }
            milesDriven.addTextChangedListener(object:TextWatcher{
                override fun afterTextChanged(s: Editable?) { }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    try {
                        if (s.toString().toInt() != meterView.value) {
                            meterView.value = s.toString().toInt()
                        }
                    } catch (e:NumberFormatException){ }
                }
            })
            milesDriven.setText(shift.odometerAtShiftStart.toString())
            meterView.value = shift.odometerAtShiftStart
            AlertDialog.Builder(this@ShiftStartEndActivity)
                .setTitle(R.string.Starting_odometer)
                .setView(meterLayout)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (meterView.value<shift.odometerAtShiftStart)
                    {
                        Toast.makeText(this@ShiftStartEndActivity,"Distance going backwards ",Toast.LENGTH_LONG).show()
                    }
                    shift.odometerAtShiftStart = meterView.value
                    dataBase.saveShift(shift)
                    updateUI()
                }
                .setNegativeButton(android.R.string.cancel,null)
                .show()
        }

        endingOdometer.setOnClickListener {
            val meterLayout = View.inflate(this@ShiftStartEndActivity,R.layout.meter_vew_wrapper,null)
            val meterView = meterLayout.findViewById<MeterView>(R.id.meterView)
            val milesDriven = meterLayout.findViewById<EditText>(R.id.milesDriven)
            meterView.setOnValueChangedListener {
                try {
                    if (milesDriven.text.toString().toInt() != it) {
                        milesDriven.setText("$it")
                    }
                } catch (e:NumberFormatException) {
                    milesDriven.setText("$it")
                }
            }
            milesDriven.addTextChangedListener(object:TextWatcher{
                override fun afterTextChanged(s: Editable?) { }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    try {
                        if (s.toString().toInt() != meterView.value) {
                            meterView.value = s.toString().toInt()
                        }
                    } catch (e:NumberFormatException){ }
                }
            })
            meterView.value = shift.odometerAtShiftEnd
            milesDriven.setText("${shift.odometerAtShiftEnd}")
            if (meterView.value<shift.odometerAtShiftStart){
                meterView.value = shift.odometerAtShiftStart+1
            }
            AlertDialog.Builder(this@ShiftStartEndActivity)
                .setTitle(R.string.End_odometer)
                .setView(meterLayout)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (meterView.value<shift.odometerAtShiftStart)
                    {
                        Toast.makeText(this@ShiftStartEndActivity,"Distance does not go backwards",Toast.LENGTH_LONG).show()
                        endingOdometer.performClick()
                    }
                    else {
                        shift.odometerAtShiftEnd = meterView.value
                        dataBase.saveShift(shift)
                        updateUI()
                    }
                }
                .setNegativeButton(android.R.string.cancel,null)
                .show()
        }




        //OK Button
        val doneClickListener = View.OnClickListener { finish() }
        doneButton.setOnClickListener(doneClickListener)
        backButton.setOnClickListener(doneClickListener)
    }

    public override fun onResume() {
        super.onResume()
        updateUI()
    }

    override fun onPause() {
        super.onPause()



        dataBase.saveShift(shift);
    }

    fun updateUI() {
        shift = dataBase.getShift(whichShift)

        if (shift.endTime.millis == 0L && shift.startTime.millis == 0L) {
            dataBase.estimateShiftTimes(shift)
        }

        tips = dataBase.getTipTotal(
            applicationContext, "Payed != -1 AND Shift=$whichShift",
            "WHERE shifts.ID=$whichShift"
        )

        shift.startTime.toGregorianCalendar().let { shiftStartTime.setText(String.format("%tl:%tM %tp %ta", it, it, it, it)) }
        shift.endTime.toGregorianCalendar().let {  shiftEndTime.setText(String.format("%tl:%tM %tp %ta", it, it, it, it)) }

        deliveries.text = tips?.deliveries.toString()

        val t1 = shift.endTime.millis.toFloat()
        val t2 = shift.startTime.millis.toFloat()
        var total = t1 - t2
        total /= 3600000f
        hoursWorked.text = String.format("%.2f", total)

        val recentOdometerValue = dataBase.mostRecientOdomenterValue
        if (shift.odometerAtShiftStart == 0) {
            shift.odometerAtShiftStart = recentOdometerValue
        }
        if (shift.odometerAtShiftEnd < shift.odometerAtShiftStart) {
            shift.odometerAtShiftEnd = shift.odometerAtShiftStart
        }

        if (Locale.getDefault().isImperial()) {
            startingOdometer.setText("${shift.odometerAtShiftStart.toString()}mi")
            endingOdometer.setText("${shift.odometerAtShiftEnd.toString()}mi")
            totalMilesDriven.text = (shift.odometerAtShiftEnd - shift.odometerAtShiftStart).toString()+"mi"
        } else {
            startingOdometer.setText("${shift.odometerAtShiftStart.toString()}km")
            endingOdometer.setText("${shift.odometerAtShiftEnd.toString()}km")
            totalMilesDriven.text = (shift.odometerAtShiftEnd - shift.odometerAtShiftStart).toString()+"km"
        }
        moneyCollected.text = Utils.getFormattedCurrency(tips?.payed)



            //  val minutesWorked = hoursWorked

     ///   hoursWorked.setText((shift.endTime.hourOfDay.toInt()-shift.startTime.hourOfDay.toInt()).toString())

        //TODO: Figure out what this endThisShift view was about
        /*   endThisShift.s = if (tips.deliveries == 0) { GONE } else { VISIBLE }

           if (dataBase.isTodaysShift(shift) == false) {
               endThisShift.visibility = GONE
           }*/
    }




}
