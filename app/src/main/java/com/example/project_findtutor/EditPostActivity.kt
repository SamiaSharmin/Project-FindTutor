package com.example.project_findtutor

import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class EditPostActivity : AppCompatActivity() {

    lateinit var db: DatabaseReference
    lateinit var auth: FirebaseAuth
    lateinit var etTitle: EditText
    lateinit var spLocation: Spinner
    lateinit var etClass: EditText
    lateinit var etTime: EditText
    lateinit var etSubjects: EditText
    lateinit var etSalary: EditText
    lateinit var etDays: EditText
    lateinit var rgStudentGender: RadioGroup
    lateinit var rgTutorGender: RadioGroup
    lateinit var etDescription: EditText
    lateinit var btnSave: Button

    var postId: String = ""
    var currentPost: Post? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_post)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()
        etTitle = findViewById(R.id.etTitle)
        spLocation = findViewById(R.id.spLocation)
        etClass = findViewById(R.id.etClass)
        etTime = findViewById(R.id.etTime)
        etSubjects = findViewById(R.id.etSubjects)
        etSalary = findViewById(R.id.etSalary)
        etDays = findViewById(R.id.etDays)
        rgStudentGender = findViewById(R.id.rgStudentGender)
        rgTutorGender = findViewById(R.id.rgTutorGender)
        etDescription = findViewById(R.id.etDescription)
        btnSave = findViewById(R.id.btnSave)


        val locations = listOf("Select Location",
            "Dhanmondi", "Gulshan", "Banani", "Mirpur", "Uttara",
            "Mohammadpur", "Bashundhara", "Rampura", "Badda",
            "Malibagh", "Shyamoli", "Farmgate", "Tejgaon")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, locations)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spLocation.adapter = adapter

        postId = intent.getStringExtra("postId") ?: ""

        if (postId.isEmpty()) {
            Toast.makeText(this, "Invalid Post", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadPostData(locations)

        btnSave.setOnClickListener {
            updatePost()
        }

    }

    fun loadPostData(locations: List<String>){
        db.child("Posts").child(postId).addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val post = snapshot.getValue(Post::class.java)
                if(post != null){
                    currentPost = post
                    etTitle.setText(post.title)
                    val locationIndex = locations.indexOf(post.location)
                    if(locationIndex != -1){
                        spLocation.setSelection(locationIndex)
                    }
                    etClass.setText(post.studentClass)
                    etTime.setText(post.time)
                    etSubjects.setText(post.subjects)
                    etSalary.setText(post.salary.toString())
                    etDays.setText(post.days.toString())
                    when(post.studentGender){
                        "Male" -> rgStudentGender.check(R.id.rbStudentMale)
                        "Female" -> rgStudentGender.check(R.id.rbStudentFemale)
                    }
                    when(post.tutorGender){
                        "Male" -> rgTutorGender.check(R.id.rbTutorMale)
                        "Female" -> rgTutorGender.check(R.id.rbTutorFemale)
                        "Any" -> rgTutorGender.check(R.id.rbTutorAny)
                    }
                    etDescription.setText(post.description)

                }else{
                    Toast.makeText(this@EditPostActivity, "Post not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@EditPostActivity, "Failed to load post", Toast.LENGTH_SHORT).show()
            }
        })

    }

    fun updatePost(){
        val title = etTitle.text.toString().trim()
        val location = spLocation.selectedItem.toString()
        val studentClass = etClass.text.toString().trim()
        val time = etTime.text.toString().trim()
        val subjects = etSubjects.text.toString().trim()
        val salary = etSalary.text.toString().trim()
        val days = etDays.text.toString().trim()
        val studentGender = when(rgStudentGender.checkedRadioButtonId){
            R.id.rbStudentMale -> "Male"
            R.id.rbStudentFemale -> "Female"
            else -> ""
        }
        val tutorGender = when(rgTutorGender.checkedRadioButtonId){
            R.id.rbTutorMale -> "Male"
            R.id.rbTutorFemale -> "Female"
            R.id.rbTutorAny -> "Any"
            else -> ""
        }
        val description = etDescription.text.toString().trim()

        if(title.isEmpty() || location.isEmpty() || studentClass.isEmpty() || time.isEmpty() ||
            subjects.isEmpty() || salary.isEmpty() || days.isEmpty() || studentGender.isEmpty() ||
            tutorGender.isEmpty() || description.isEmpty()){
            Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
            return
        }

        val salaryInt = salary.toInt()
        val dayInt = days.toInt()
        val oldPost = currentPost?:return
        val updatedPost = Post(
            postId = oldPost.postId,
            userId = oldPost.userId,
            title = title,
            location = location,
            studentClass = studentClass,
            time = time,
            subjects = subjects,
            salary = salaryInt,
            days = dayInt,
            studentGender = studentGender,
            tutorGender = tutorGender,
            description = description,
            postedDate = getCurrDate()
        )

        db.child("Posts").child(postId).setValue(updatedPost)
            .addOnSuccessListener {
                Toast.makeText(this, "Post updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update post", Toast.LENGTH_SHORT).show()
            }
    }

    fun getCurrDate(): String{
        val date = java.util.Date()
        val format = java.text.SimpleDateFormat("dd-MM-yyyy")
        return format.format(date)

    }
}