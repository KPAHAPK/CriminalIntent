package com.bignerdranch.android.criminalintent.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.lifecycle.ViewModelProvider
import com.bignerdranch.android.criminalintent.R
import com.bignerdranch.android.criminalintent.domain.*
import com.bignerdranch.android.criminalintent.viewmodel.CrimeDetailViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private lateinit var titleField: EditText
private lateinit var dateButton: Button
private lateinit var solvedCheckBox: CheckBox
private lateinit var timeButton: Button
private lateinit var reportButton: Button
private lateinit var suspectButton: Button
private lateinit var suspectNumberButton: Button
private lateinit var photoButton: ImageButton
private lateinit var photoView: ImageView


private lateinit var pickContactContract: ActivityResultContract<Uri, Uri?>
private lateinit var pickContactCallback: ActivityResultCallback<Uri?>
private lateinit var pickContactLauncher: ActivityResultLauncher<Uri>
private lateinit var takePhotoContractLauncher: ActivityResultLauncher<Intent>

private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "DialogDate"
private const val DIALOG_TIME = "DialogTime"
private const val DIALOG_THUMBNAIL = "DialogTime"
private const val REQUEST_DATE = "RequestDate"
private const val REQUEST_TIME = "RequestTime"
private const val REQUEST_CONTACT = 1
private const val DATE_FORMAT = "EEE, MMM, dd"


class CrimeFragment : Fragment(), FragmentResultListener {

    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
    }

    companion object {
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                this.putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                this.arguments = args
            }
        }
    }


    private lateinit var crime: Crime
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private var widthPhotoView: Int = 0
    private var heightPhotoView: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeDetailViewModel.loadCrime(crimeId)

        pickContactContract = object : ActivityResultContract<Uri, Uri?>() {
            override fun createIntent(context: Context, input: Uri?): Intent {
                Log.d(TAG, "createIntent() called")
                return Intent(Intent.ACTION_PICK, input)
            }

            override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
                Log.d(TAG, "parseResult() called")
                if (resultCode != Activity.RESULT_OK || intent == null) {
                    return null
                } else {
                    var contactID: String? = null
                    val contactUri: Uri? = intent.data
                    val queryFields = arrayOf(
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.Contacts._ID
                    )
                    val cursor = contactUri?.let {
                        requireActivity().contentResolver.query(
                            it,
                            queryFields,
                            null,
                            null,
                            null
                        )
                    }
                    cursor?.use {
                        if (it.count == 0) {
                            return null
                        }
                        it.moveToFirst()
                        val suspect = it.getString(0)
                        contactID = it.getString(1)
                        crime.suspect = suspect
                        suspectButton.text = suspect
                    }

                    val phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                    val phoneQueryFields = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val phoneWhereClause =
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
                    val phoneQueryParameters = arrayOf(contactID)
                    val phoneCursor = requireActivity().contentResolver.query(
                        phoneUri,
                        phoneQueryFields,
                        phoneWhereClause,
                        phoneQueryParameters,
                        null
                    )

                    var phoneNumber: String
                    val allNumbers: ArrayList<String> = arrayListOf()
                    allNumbers.clear()

                    phoneCursor?.use {
                        it.moveToFirst()
                        while (!it.isAfterLast) {
                            phoneNumber = it.getString(0)
                            allNumbers.add(phoneNumber)
                            it.moveToNext()
                        }
                    }
                    val items = allNumbers.toTypedArray()

                    var selectedNumber: String

                    val builder = AlertDialog.Builder(context)
                        .setTitle("Choose a Number:")
                        .setItems(items) { _, which ->
                            selectedNumber = allNumbers[which].replace("_", "")
                            crime.suspectPhoneNumber = selectedNumber
                            suspectNumberButton.text = crime.suspectPhoneNumber
                        }
                    val alert = builder.create()
                    when {
                        allNumbers.size > 1 -> alert.show()
                        allNumbers[0].isNotEmpty() -> {
                            selectedNumber = allNumbers[0].replace("-", "")
                            crime.suspectPhoneNumber = selectedNumber
                            suspectNumberButton.text = crime.suspectPhoneNumber
                        }
                        else -> {
                            suspectNumberButton.text = "no phone number found"
                            crime.suspectPhoneNumber = ""
                        }
                    }
                    crimeDetailViewModel.saveCrime(crime)
                }
                return intent.data
            }
        }
        pickContactCallback = ActivityResultCallback { contactUri ->
            Log.d(TAG, "OnActivityResult() called with result: $contactUri")
        }
        pickContactLauncher = registerForActivityResult(pickContactContract, pickContactCallback)

        takePhotoContractLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    requireActivity().revokeUriPermission(
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    updatePhotoView()
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)
        titleField = view.findViewById(R.id.crime_title) as EditText
        dateButton = view.findViewById(R.id.crime_date) as Button
        solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox
        timeButton = view.findViewById(R.id.crime_time) as Button
        reportButton = view.findViewById(R.id.crime_report) as Button
        suspectButton = view.findViewById(R.id.crime_suspect) as Button
        suspectNumberButton = view.findViewById(R.id.crime_suspect_number) as Button
        photoButton = view.findViewById(R.id.crime_camera) as ImageButton
        photoView = view.findViewById(R.id.crime_photo) as ImageView
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(viewLifecycleOwner,
            { crime ->
                crime?.let {
                    this.crime = crime
                    photoFile = crimeDetailViewModel.getPhotoFile(crime)
                    photoUri = FileProvider.getUriForFile(
                        requireActivity(),
                        "com.bignerdranch.android.criminalintent.fileprovider",
                        photoFile
                    )
                    updateUI()
                }
            })
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(
                sequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                crime.title = sequence.toString()
            }

            override fun afterTextChanged(sequence: Editable?) {
            }
        }
        titleField.addTextChangedListener((titleWatcher))

        solvedCheckBox.apply {
            this.setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
            }
        }

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date, REQUEST_DATE).apply {
                show(this@CrimeFragment.childFragmentManager, DIALOG_DATE)
            }
        }
        childFragmentManager.setFragmentResultListener(REQUEST_DATE, viewLifecycleOwner, this)

        timeButton.setOnClickListener {
            TimePickerFragment.newInstance(crime.date, REQUEST_TIME).apply {
                show(this@CrimeFragment.childFragmentManager, DIALOG_TIME)
            }
        }
        childFragmentManager.setFragmentResultListener(REQUEST_TIME, viewLifecycleOwner, this)

        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
            }.also { intent ->
                val chooserIntent = Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }

        suspectButton.apply {
            val pickContactIntent =
                pickContactContract.createIntent(
                    requireContext(),
                    ContactsContract.Contacts.CONTENT_URI
                )
            setOnClickListener {
                pickContactLauncher.launch(ContactsContract.Contacts.CONTENT_URI)
            }
            if (checkPackage(pickContactIntent)) {
                isEnabled = false
            }
        }

        suspectNumberButton.apply {
            val chooserIntent: Intent
            val dialNumberIntent = Intent(Intent.ACTION_DIAL).apply {
                val phone = crime.suspectPhoneNumber
                data = Uri.parse("tel:$phone")
            }
                .also {
                    chooserIntent = Intent.createChooser(it, "Call via:")
                }
            setOnClickListener {
                startActivity(chooserIntent)
            }
            if (checkPackage(dialNumberIntent)) {
                isEnabled = false
            }
        }

        photoButton.apply {
            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val packageManager = requireActivity().packageManager
            val resolveActivity: ResolveInfo? =
                packageManager.resolveActivity(captureImage, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveActivity == null) {
                isEnabled = false
            }
            setOnClickListener {
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                val cameraActivities: List<ResolveInfo> = packageManager.queryIntentActivities(
                    captureImage,
                    PackageManager.MATCH_DEFAULT_ONLY
                )
                for (cameraActivity in cameraActivities) {
                    requireActivity().grantUriPermission(
                        cameraActivity.activityInfo.packageName,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
                takePhotoContractLauncher.launch(captureImage)
            }
        }

        photoView.apply {
            setOnClickListener {
                ThumbnailZoomFragment.newInstance(crime.photoFileName).apply {
                    show(this@CrimeFragment.childFragmentManager, DIALOG_THUMBNAIL)
                }
            }
            viewTreeObserver.also {
                if (it.isAlive) {
                    it.addOnGlobalLayoutListener {
                        widthPhotoView = this.width
                        heightPhotoView = this.height
                    }
                }
            }
        }
    }

    private fun checkPackage(intent: Intent): Boolean {
        val packageManager: PackageManager = requireActivity().packageManager
        val resolveActivity: ResolveInfo? =
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveActivity == null
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }


    private fun updateUI() {
        val datePattern = "EEEE, MM dd yyyy"
        val timePattern = "HH:mm"

        setCalendarBtnText(datePattern, dateButton)
        setCalendarBtnText(timePattern, timeButton)

        titleField.setText(crime.title)

        solvedCheckBox.apply {
            this.isChecked = crime.isSolved
            this.jumpDrawablesToCurrentState()
        }
        if (crime.suspect.isNotEmpty()) {
            suspectButton.text = crime.suspect
        }
        if (crime.suspectPhoneNumber.isNotEmpty()) {
            suspectNumberButton.text = crime.suspectPhoneNumber
        }

        updatePhotoView()
    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
//           val bitmap = getScaledBitmap(photoFile.path, requireActivity())
            val bitmap = getScaledBitmap(photoFile.path, widthPhotoView, heightPhotoView)
            photoView.setImageBitmap(bitmap)
        } else {
            photoView.setImageDrawable(null)
        }
    }

    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }
        val datePattern = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        val dateString = datePattern.format(crime.date).toString()
        val suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect)
        }
        return getString(R.string.crime_report, crime.title, dateString, solvedString, suspect)
    }

    private fun setCalendarBtnText(
        pattern: String,
        btn: Button
    ) {
        val simpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())
        btn.text = simpleDateFormat.format(crime.date)
        btn.jumpDrawablesToCurrentState()
    }

    override fun onFragmentResult(requestKey: String, result: Bundle) {
        when (requestKey) {
            REQUEST_DATE -> crime.date = DatePickerFragment.getSelectedDate(result)
            REQUEST_TIME -> crime.date = TimePickerFragment.getSelectedTime(result)
        }
        updateUI()
    }


}