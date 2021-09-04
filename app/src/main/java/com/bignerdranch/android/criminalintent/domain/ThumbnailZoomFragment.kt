package com.bignerdranch.android.criminalintent.domain

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.bignerdranch.android.criminalintent.R

private lateinit var zoomedCrimePhoto: ImageView

private const val ARG_1 = "ARG_1"

class ThumbnailZoomFragment : DialogFragment() {
    companion object {
        fun newInstance(path: String): ThumbnailZoomFragment {
            val args = Bundle().apply {
                putString(ARG_1, path)
            }
            return ThumbnailZoomFragment().apply {
                arguments = args
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.zoomed_thumbnail, container)
        zoomedCrimePhoto = view.findViewById(R.id.zoomed_crime_photo) as ImageView
        val photoName = arguments?.getString(ARG_1) as String
        val photoPath = requireActivity().filesDir.path + "/" + photoName
        photoPath.run {
            val decodedBitmap = BitmapFactory.decodeFile(this)
            val rotatedBitmap = decodedBitmap.modifyOrientation(this)
            zoomedCrimePhoto.setImageBitmap(rotatedBitmap)
        }

        return view
    }
}