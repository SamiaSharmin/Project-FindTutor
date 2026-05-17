package com.example.project_findtutor

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class AdminSettingsFragment : Fragment(R.layout.fragment_admin_settings) {

    private lateinit var auth: FirebaseAuth

    private lateinit var tvAdminEmail: TextView
    private lateinit var btnChangeAdminEmail: Button
    private lateinit var btnChangePassword: Button
    private lateinit var btnAppPolicy: Button
    private lateinit var btnAboutApp: Button
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()

        tvAdminEmail = view.findViewById(R.id.tvAdminEmail)
        btnChangeAdminEmail = view.findViewById(R.id.btnChangeAdminEmail)
        btnChangePassword = view.findViewById(R.id.btnChangePassword)
        btnAppPolicy = view.findViewById(R.id.btnAppPolicy)
        btnAboutApp = view.findViewById(R.id.btnAboutApp)
        btnLogout = view.findViewById(R.id.btnLogout)

        loadAdminEmail()

        btnChangeAdminEmail.setOnClickListener {
            showChangeEmailDialog()
        }

        btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        btnAppPolicy.setOnClickListener {
            showAppPolicyDialog()
        }

        btnAboutApp.setOnClickListener {
            showAboutAppDialog()
        }

        btnLogout.setOnClickListener {
            logoutAdmin()
        }
    }

    override fun onResume() {
        super.onResume()
        loadAdminEmail()
    }

    private fun loadAdminEmail() {
        val currentEmail = auth.currentUser?.email

        tvAdminEmail.text = if (currentEmail.isNullOrEmpty()) {
            "Email: No email found"
        } else {
            "Email: $currentEmail"
        }
    }

    private fun showChangeEmailDialog() {
        val currentPasswordInput = createInput(
            hintText = "Enter current password",
            inputTypeValue = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        )

        val newEmailInput = createInput(
            hintText = "Enter new email",
            inputTypeValue = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        )

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 0)

            addView(createLabel("Current Password"))
            addView(currentPasswordInput)

            addView(createLabel("New Email"))
            addView(newEmailInput)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Change Admin Email")
            .setMessage("A verification email will be sent to the new email address.")
            .setView(container)
            .setPositiveButton("Update", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val currentPassword = currentPasswordInput.text.toString().trim()
                val newEmail = newEmailInput.text.toString().trim()

                if (currentPassword.isEmpty()) {
                    currentPasswordInput.error = "Current password is required"
                    return@setOnClickListener
                }

                if (newEmail.isEmpty()) {
                    newEmailInput.error = "New email is required"
                    return@setOnClickListener
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                    newEmailInput.error = "Enter a valid email address"
                    return@setOnClickListener
                }

                changeAdminEmail(currentPassword, newEmail)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun changeAdminEmail(currentPassword: String, newEmail: String) {
        reauthenticateAdmin(currentPassword) {
            val user = auth.currentUser

            if (user == null) {
                Toast.makeText(requireContext(), "Admin not logged in", Toast.LENGTH_SHORT).show()
                return@reauthenticateAdmin
            }

            user.verifyBeforeUpdateEmail(newEmail)
                .addOnSuccessListener {
                    Toast.makeText(
                        requireContext(),
                        "Verification email sent. Verify the new email to complete the change.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        requireContext(),
                        "Failed to change email: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun showChangePasswordDialog() {
        val currentPasswordInput = createInput(
            hintText = "Enter current password",
            inputTypeValue = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        )

        val newPasswordInput = createInput(
            hintText = "Enter new password",
            inputTypeValue = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        )

        val confirmPasswordInput = createInput(
            hintText = "Confirm new password",
            inputTypeValue = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        )

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 0)

            addView(createLabel("Current Password"))
            addView(currentPasswordInput)

            addView(createLabel("New Password"))
            addView(newPasswordInput)

            addView(createLabel("Confirm Password"))
            addView(confirmPasswordInput)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Change Password")
            .setView(container)
            .setPositiveButton("Update", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val currentPassword = currentPasswordInput.text.toString().trim()
                val newPassword = newPasswordInput.text.toString().trim()
                val confirmPassword = confirmPasswordInput.text.toString().trim()

                if (currentPassword.isEmpty()) {
                    currentPasswordInput.error = "Current password is required"
                    return@setOnClickListener
                }

                if (newPassword.isEmpty()) {
                    newPasswordInput.error = "New password is required"
                    return@setOnClickListener
                }

                if (newPassword.length < 6) {
                    newPasswordInput.error = "Password must be at least 6 characters"
                    return@setOnClickListener
                }

                if (confirmPassword.isEmpty()) {
                    confirmPasswordInput.error = "Please confirm your new password"
                    return@setOnClickListener
                }

                if (newPassword != confirmPassword) {
                    confirmPasswordInput.error = "Passwords do not match"
                    return@setOnClickListener
                }

                changeAdminPassword(currentPassword, newPassword)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun changeAdminPassword(currentPassword: String, newPassword: String) {
        reauthenticateAdmin(currentPassword) {
            val user = auth.currentUser

            if (user == null) {
                Toast.makeText(requireContext(), "Admin not logged in", Toast.LENGTH_SHORT).show()
                return@reauthenticateAdmin
            }

            user.updatePassword(newPassword)
                .addOnSuccessListener {
                    Toast.makeText(
                        requireContext(),
                        "Password changed successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        requireContext(),
                        "Failed to change password: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun reauthenticateAdmin(
        currentPassword: String,
        onSuccess: () -> Unit
    ) {
        val user = auth.currentUser
        val email = user?.email

        if (user == null || email.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Admin not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val credential = EmailAuthProvider.getCredential(email, currentPassword)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    requireContext(),
                    "Re-authentication failed: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun showAppPolicyDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("App Policy")
            .setMessage(
                "FindTutor Policy\n\n" +
                        "1. Students and tutors must provide accurate information.\n\n" +
                        "2. Users must not post fake tuition requests or misleading tutor information.\n\n" +
                        "3. Meetings should be arranged only for educational purposes.\n\n" +
                        "4. Reviews must be honest and respectful.\n\n" +
                        "5. Reports will be reviewed by the admin team, and necessary action may be taken.\n\n" +
                        "6. Any abusive, fraudulent, or inappropriate activity may lead to account restriction."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAboutAppDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("About FindTutor")
            .setMessage(
                "FindTutor is a tuition management platform that helps students find tutors and allows tutors to connect with students.\n\n" +
                        "Main Features:\n\n" +
                        "• Student and tutor registration\n" +
                        "• Tutor interest system\n" +
                        "• Meeting setup\n" +
                        "• Reviews and ratings\n" +
                        "• Problem reporting\n" +
                        "• Admin investigation and notification system\n\n" +
                        "Version: 1.0"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun logoutAdmin() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                auth.signOut()
                Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()

                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun createInput(hintText: String, inputTypeValue: Int): EditText {
        return EditText(requireContext()).apply {
            hint = hintText
            inputType = inputTypeValue
            setPadding(20, 12, 20, 12)
        }
    }

    private fun createLabel(labelText: String): TextView {
        return TextView(requireContext()).apply {
            text = labelText
            textSize = 14f
            setPadding(0, 16, 0, 4)
        }
    }

}