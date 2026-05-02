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
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction

class CreatePostActivity : AppCompatActivity() {

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
    lateinit var btnSubmit: Button

    lateinit var auth: FirebaseAuth
    lateinit var db: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_post)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().reference

        etTitle = findViewById<EditText>(R.id.etTitle)
        spLocation = findViewById<Spinner>(R.id.spLocation)
        etClass = findViewById<EditText>(R.id.etClass)
        etTime = findViewById<EditText>(R.id.etTime)
        etSubjects = findViewById<EditText>(R.id.etSubjects)
        etSalary = findViewById<EditText>(R.id.etSalary)
        etDays = findViewById<EditText>(R.id.etDays)
        rgStudentGender = findViewById<RadioGroup>(R.id.rgStudentGender)
        rgTutorGender = findViewById<RadioGroup>(R.id.rgTutorGender)
        etDescription = findViewById<EditText>(R.id.etDescription)
        btnSubmit = findViewById<Button>(R.id.btnSubmit)

        val location = listOf("Select Location",
            "Dhanmondi", "Gulshan", "Banani", "Mirpur", "Uttara",
            "Mohammadpur", "Bashundhara", "Rampura", "Badda",
            "Malibagh", "Shyamoli", "Farmgate", "Tejgaon")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, location)
        spLocation.adapter = adapter

        btnSubmit.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val studentClass = etClass.text.toString().trim()
            val time = etTime.text.toString().trim()
            val subjects = etSubjects.text.toString().trim()
            val salaryInput = etSalary.text.toString().trim()
            val daysInput = etDays.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val location = spLocation.selectedItem.toString()

            val studentGender = when (rgStudentGender.checkedRadioButtonId) {
                R.id.rbStudentMale -> "Male"
                R.id.rbStudentFemale -> "Female"
                else -> ""
            }

            val tutorGender = when (rgTutorGender.checkedRadioButtonId) {
                R.id.rbTutorMale -> "Male"
                R.id.rbTutorFemale -> "Female"
                R.id.rbTutorAny -> "Any"
                else -> ""
            }

            if(title.isEmpty()){
                etTitle.error = "Title is required"
                return@setOnClickListener
            }

            if(location == "Select Location"){
                Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(studentClass.isEmpty()){
                etClass.error = "Class is required"
                return@setOnClickListener
            }

            if(time.isEmpty()){
                etTime.error = "Time is required"
                return@setOnClickListener
            }

            if(subjects.isEmpty()){
                etSubjects.error = "Subjects are required"
                return@setOnClickListener
            }

            if(salaryInput.isEmpty()){
                etSalary.error = "Salary is required"
                return@setOnClickListener
            }

            if(daysInput.isEmpty()){
                etDays.error = "Days/Week is required"
                return@setOnClickListener
            }

            if(studentGender.isEmpty()){
                Toast.makeText(this, "Please select a student gender", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(tutorGender.isEmpty()){
                Toast.makeText(this, "Please select a tutor gender", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val salary = salaryInput.toInt()
            val days = daysInput.toInt()

            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            generateJobIdAndSave(
                userId,
                title,
                location,
                studentClass,
                time,
                subjects,
                salary,
                days,
                studentGender,
                tutorGender,
                description
            )

        }

    }

    fun generateJobIdAndSave(userId: String, title: String, location: String, studentClass: String, time: String, subjects: String, salary: Int, days: Int, studentGender: String, tutorGender: String, description: String=""){

        val counterRef = db.child("jobCounter")

        counterRef.runTransaction(object: Transaction.Handler{
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                var currentValue = currentData.getValue(Int::class.java) ?: 0
                if(currentValue == 0){
                    currentValue = 1000
                }
                val newValue = currentValue + 1
                currentData.value = newValue
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if(committed && currentData != null){
                    val jobId = currentData.getValue(Int::class.java) ?: 0
                    val postedDate = getCurrentDate()
                    savePost(jobId, userId, title, location, studentClass, time, subjects, salary, days, studentGender, tutorGender, description,postedDate)
                }else{
                    Toast.makeText(this@CreatePostActivity, "Error: ${error?.message}", Toast.LENGTH_SHORT).show()
                }
            }

        })

    }

    fun savePost(jobId: Int, userId: String, title: String, location: String, studentClass: String, time: String, subjects: String, salary: Int, days: Int, studentGender: String, tutorGender: String, description: String="",postedDate: String){
        val postId = jobId.toString()
        val post = Post(postId,jobId, userId, title, location, studentClass, time, subjects, salary, days, studentGender, tutorGender, description,postedDate)
        db.child("Posts").child(jobId.toString()).setValue(post)
            .addOnSuccessListener {
                Toast.makeText(this, "Post created successfully! Job ID: $jobId", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun getCurrentDate(): String{
        val date = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        return date.format(java.util.Date())
    }

}