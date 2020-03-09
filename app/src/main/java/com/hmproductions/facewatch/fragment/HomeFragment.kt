package com.hmproductions.facewatch.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import com.hmproductions.facewatch.FaceWatchClient
import com.hmproductions.facewatch.R
import com.hmproductions.facewatch.adapter.PersonRecyclerAdapter
import com.hmproductions.facewatch.dagger.ContextModule
import com.hmproductions.facewatch.dagger.DaggerFaceWatchApplicationComponent
import com.hmproductions.facewatch.data.FaceWatchViewModel
import com.hmproductions.facewatch.data.Person
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class HomeFragment : Fragment(), PersonRecyclerAdapter.PersonClickListener {

    @Inject
    lateinit var client: FaceWatchClient

    private lateinit var model: FaceWatchViewModel
    private var personRecyclerAdapter: PersonRecyclerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        personRecyclerAdapter = PersonRecyclerAdapter(context, null, this)
        model = activity?.run { ViewModelProvider(this).get(FaceWatchViewModel::class.java) }
            ?: throw Exception("Invalid activity")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        DaggerFaceWatchApplicationComponent.builder().contextModule(ContextModule(context!!)).build().inject(this)
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        personRecyclerView.adapter = personRecyclerAdapter
        personRecyclerView.layoutManager = LinearLayoutManager(context)
        personRecyclerView.setHasFixedSize(false)

        val familyBitmap = BitmapFactory.decodeResource(context?.resources, R.drawable.family)

        val faceDetector = FaceDetector.Builder(context).setTrackingEnabled(false).build()
        if (!faceDetector.isOperational) {
            AlertDialog.Builder(context!!).setMessage("Could not set up the face detector!").show()
            return
        }

        val frame = Frame.Builder().setBitmap(familyBitmap).build()
        val faces = faceDetector.detect(frame)

        identifyPeopleFromFaces(faces, familyBitmap)
    }

    private fun identifyPeopleFromFaces(faces: SparseArray<Face>, familyBitmap: Bitmap) = lifecycleScope.launch {

        val personList = mutableListOf<Person>()

        for (i in 0 until faces.size()) {
            val thisFace = faces.valueAt(i)
            val x1 = thisFace.position.x.toInt()
            val y1 = thisFace.position.y.toInt()
            val currentFaceBitmap = Bitmap.createBitmap(familyBitmap, x1, y1, thisFace.width.toInt(), thisFace.height.toInt())

            val newPerson = withContext(Dispatchers.IO) { model.identifyFace(client, image) }

            if (newPerson != null) personList.add(newPerson)
        }

        personRecyclerAdapter?.swapData(personList)
    }

    override fun onPersonClicked(person: Person?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}