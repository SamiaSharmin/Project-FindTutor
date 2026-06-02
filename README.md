# Project-FindTutor

Project-FindTutor is a native Android tuition-management app that connects students with tutors. Students can create tuition posts, tutors can browse available tuition requests and show interest, and admins can monitor users, posts, meetings, reports, and platform notifications.

The app is built with **Kotlin**, **Android XML layouts**, **Firebase Authentication**, and **Firebase Realtime Database**.

---

## Table of Contents

- [Features](#features)
- [User Roles](#user-roles)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Firebase Setup](#firebase-setup)
- [Installation and Run](#installation-and-run)
- [Database Structure](#database-structure)
- [Main App Flow](#main-app-flow)
- [Build Commands](#build-commands)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Notes for Future Development](#notes-for-future-development)

---

## Features

### Authentication

- Email/password login using Firebase Authentication.
- Student registration.
- Tutor registration.
- Role-based dashboard redirection after login.
- Forgot-password screen for password reset.

### Student Features

- Register and log in as a student.
- Create tuition posts with details such as title, location, class, subjects, salary, weekly days, student gender, preferred tutor gender, and description.
- View own tuition posts.
- Edit or delete tuition posts.
- Receive tutor-interest notifications.
- View tutor details from notifications.
- Set meetings with interested tutors.
- Submit reviews and ratings after meetings.
- Edit student profile.
- Report problems to admin.
- Delete account data from the app database.

### Tutor Features

- Register and log in as a tutor.
- Browse available tuition posts.
- Mark interest in tuition posts.
- Receive student/admin notifications.
- Accept or reject meeting requests.
- View meeting reminders.
- Edit tutor profile including qualification and preferred areas.
- Report problems to admin.
- Delete account data from the app database.

### Admin Features

- Admin dashboard with platform metrics.
- View users by role/status.
- Open user detail pages.
- Activate, deactivate, suspend, or mark users pending.
- Manage tuition posts and update post status.
- View and update meeting status.
- View problem reports and review moderation items.
- Update report/review status such as pending, in review, resolved, or dismissed.
- Receive admin notifications for new registrations and new posts.
- Audit log entries are written for major admin actions.

---

## User Roles

The app uses the `Users/{uid}/role` value in Firebase Realtime Database to redirect users after login.

Supported roles:

| Role | Dashboard |
|---|---|
| `student` | `StudentDashboard` |
| `tutor` | `TutorDashboard` |
| `admin` | `AdminDashboard` |

> There is no public admin registration screen in the current project. To create an admin user, create the account in Firebase Authentication and add/update the user record manually in Realtime Database with `role: "admin"`.

Example admin record:

```json
{
  "Users": {
    "ADMIN_FIREBASE_UID": {
      "userId": "ADMIN_FIREBASE_UID",
      "name": "Admin",
      "email": "admin@example.com",
      "role": "admin",
      "status": "active"
    }
  }
}
```

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Android XML layouts
- **Architecture:** Activity + Fragment based Android app
- **Authentication:** Firebase Authentication
- **Database:** Firebase Realtime Database
- **Build System:** Gradle Kotlin DSL
- **Material UI:** Material Components for Android
- **Minimum SDK:** 24
- **Target SDK:** 36
- **Compile SDK:** 36
- **Java/Kotlin JVM Target:** Java 11 / JVM 11

Key dependency versions used in this project:

| Tool / Library | Version |
|---|---:|
| Android Gradle Plugin | 8.12.3 |
| Kotlin | 2.0.21 |
| AndroidX Core KTX | 1.18.0 |
| AppCompat | 1.7.1 |
| Material Components | 1.13.0 |
| ConstraintLayout | 2.2.1 |
| Firebase BoM | 32.7.0 |
| Firebase Realtime Database | 22.0.1 |

---

## Project Structure

```text
Project-FindTutor/
├── app/
│   ├── build.gradle.kts
│   ├── google-services.json
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/project_findtutor/
│       │   │   ├── MainActivity.kt
│       │   │   ├── StudentDashboard.kt
│       │   │   ├── TutorDashboard.kt
│       │   │   ├── AdminDashboard.kt
│       │   │   ├── *Fragment.kt
│       │   │   ├── *Adapter.kt
│       │   │   └── data model classes
│       │   └── res/
│       │       ├── layout/
│       │       ├── drawable/
│       │       ├── menu/
│       │       ├── values/
│       │       └── xml/
│       ├── androidTest/
│       └── test/
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
└── gradlew.bat
```

Important files:

| File | Purpose |
|---|---|
| `MainActivity.kt` | Login screen and role-based routing |
| `RoleRegisterActivity.kt` | Registration role selection |
| `StudentRegisterActivity.kt` | Student signup |
| `TutorRegisterActivity.kt` | Tutor signup |
| `StudentDashboard.kt` | Student bottom navigation container |
| `TutorDashboard.kt` | Tutor bottom navigation container |
| `AdminDashboard.kt` | Admin bottom navigation container |
| `CreatePostActivity.kt` | Student tuition post creation |
| `EditPostActivity.kt` | Student tuition post editing |
| `AdminManageUsersFragment.kt` | Admin user management |
| `AdminManagePostsFragment.kt` | Admin post management |
| `AdminUserMeetingsFragment.kt` | Admin meeting management |
| `AdminUserReportsFragment.kt` | Admin reports and review moderation |
| `AdminNotificationHelper.kt` | Helper for writing admin notifications |

---

## Prerequisites

Before running the project, install:

- Android Studio
- JDK 11 or newer
- Android SDK with API level 36 installed
- Firebase project with Authentication and Realtime Database enabled
- Android device or emulator

---

## Firebase Setup

### 1. Create a Firebase Project

Create a new Firebase project from the Firebase Console.

### 2. Add Android App to Firebase

Use the application ID from the project:

```text
com.example.project_findtutor
```

### 3. Download Firebase Config

Download the generated `google-services.json` file and place it here:

```text
app/google-services.json
```

> Keep `google-services.json` private for production projects. Do not expose production Firebase credentials in public repositories.

### 4. Enable Authentication

In Firebase Console:

1. Go to **Authentication**.
2. Open **Sign-in method**.
3. Enable **Email/Password**.

### 5. Enable Realtime Database

In Firebase Console:

1. Go to **Realtime Database**.
2. Create a database.
3. Select the region.
4. Start with suitable development rules, then secure them before production.

Example development-only rules:

```json
{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null"
  }
}
```

> These rules are only for local development/testing. For production, use role-based security rules so students, tutors, and admins can only access the data they are allowed to manage.

---

## Installation and Run

### 1. Clone or Extract the Project

```bash
git clone <repository-url>
cd Project-FindTutor
```

Or extract the project ZIP and open the root folder in Android Studio.

### 2. Add Firebase Config

Make sure this file exists:

```text
app/google-services.json
```

### 3. Sync Gradle

Open the project in Android Studio and click **Sync Now**.

Or run from terminal:

```bash
./gradlew clean
./gradlew build
```

On Windows:

```bat
gradlew.bat clean
gradlew.bat build
```

### 4. Run the App

Use Android Studio:

1. Select an emulator or physical Android device.
2. Click **Run**.

Or run:

```bash
./gradlew installDebug
```

---

## Database Structure

The app uses Firebase Realtime Database nodes similar to the following:

```text
Users/
Students/
Tutors/
Posts/
Meetings/
Notifications/
AdminNotifications/
ProblemReports/
Reviews/
AuditLogs/
jobCounter
```

### `Users`

Stores common user information and role.

```json
{
  "Users": {
    "uid": {
      "userId": "uid",
      "name": "User Name",
      "email": "user@example.com",
      "role": "student",
      "status": "active"
    }
  }
}
```

### `Students`

Stores student profile data.

```json
{
  "Students": {
    "uid": {
      "userId": "uid",
      "name": "Student Name",
      "email": "student@example.com",
      "phoneNumber": "01XXXXXXXXX",
      "rating": 0
    }
  }
}
```

### `Tutors`

Stores tutor profile data.

```json
{
  "Tutors": {
    "uid": {
      "userId": "uid",
      "name": "Tutor Name",
      "email": "tutor@example.com",
      "phoneNumber": "01XXXXXXXXX",
      "qualification": "BSc in CSE",
      "preferedAreas": "Dhanmondi, Mirpur",
      "rating": 0,
      "totalReview": 0
    }
  }
}
```

### `Posts`

Stores tuition posts created by students.

```json
{
  "Posts": {
    "1001": {
      "postId": "1001",
      "jobId": 1001,
      "userId": "studentUid",
      "title": "Need Math Tutor",
      "location": "Dhanmondi",
      "studentClass": "Class 8",
      "time": "6 PM",
      "subjects": "Math, Science",
      "salary": 5000,
      "days": 3,
      "studentGender": "Male",
      "tutorGender": "Any",
      "description": "Need an experienced tutor.",
      "postedDate": "01/06/2026",
      "status": "open"
    }
  }
}
```

### `Meetings`

Stores meeting requests between students and tutors.

```json
{
  "Meetings": {
    "meetingId": {
      "meetingId": "meetingId",
      "jobId": 1001,
      "studentId": "studentUid",
      "studentName": "Student Name",
      "studentPhoneNumber": "01XXXXXXXXX",
      "tutorId": "tutorUid",
      "date": "05/06/2026",
      "time": "6 PM",
      "location": "Dhanmondi",
      "status": "pending",
      "createdAt": 1710000000000,
      "reviewSubmitted": false,
      "reviewRating": 0,
      "reviewText": ""
    }
  }
}
```

### `Notifications`

Stores user-specific notifications.

```json
{
  "Notifications": {
    "userUid": {
      "notificationId": {
        "jobId": 1001,
        "tutorId": "tutorUid",
        "tutorName": "Tutor Name",
        "message": "Tutor is interested in your post.",
        "type": "interest",
        "timestamp": 1710000000000,
        "isRead": false
      }
    }
  }
}
```

### `AdminNotifications`

Stores admin-facing platform activity notifications.

```json
{
  "AdminNotifications": {
    "notificationId": {
      "notificationId": "notificationId",
      "title": "New student registered",
      "message": "A new student registered.",
      "type": "new_user",
      "userId": "uid",
      "userRole": "student",
      "userName": "Student Name",
      "relatedId": "uid",
      "relatedNode": "Students",
      "timestamp": 1710000000000,
      "isRead": false
    }
  }
}
```

### `ProblemReports`

Stores reports submitted by students or tutors.

```json
{
  "ProblemReports": {
    "reportId": {
      "reportId": "reportId",
      "userId": "uid",
      "userRole": "student",
      "userName": "User Name",
      "userEmail": "user@example.com",
      "userPhoneNumber": "01XXXXXXXXX",
      "description": "Problem description",
      "status": "pending",
      "createdAt": 1710000000000
    }
  }
}
```

### `Reviews`

Stores tutor reviews submitted by students.

```json
{
  "Reviews": {
    "reviewId": {
      "reviewId": "reviewId",
      "studentId": "studentUid",
      "studentName": "Student Name",
      "tutorId": "tutorUid",
      "meetingId": "meetingId",
      "rating": 5,
      "reviewText": "Excellent tutor.",
      "timestamp": 1710000000000,
      "moderationStatus": "pending"
    }
  }
}
```

### `AuditLogs`

Stores admin action logs for important changes.

```json
{
  "AuditLogs": {
    "logId": {
      "action": "update_status",
      "targetNode": "Users",
      "targetId": "uid",
      "oldStatus": "pending",
      "newStatus": "active",
      "timestamp": 1710000000000
    }
  }
}
```

### `jobCounter`

Used to generate numeric job IDs for tuition posts. The app initializes the counter from `1000` and increments it for new posts.

---

## Main App Flow

### Student Flow

```text
Register/Login
→ Student Dashboard
→ Create tuition post
→ Tutor shows interest
→ Student receives notification
→ Student schedules meeting
→ Tutor accepts/rejects meeting
→ Student reviews tutor after completion
```

### Tutor Flow

```text
Register/Login
→ Tutor Dashboard
→ Browse tuition posts
→ Mark interested
→ Receive meeting request
→ Accept/reject meeting
→ Maintain profile and preferred areas
```

### Admin Flow

```text
Admin Login
→ Admin Dashboard
→ Manage users/posts/meetings/reports
→ Update statuses
→ Review audit logs and notifications
```

---

## Build Commands

Clean project:

```bash
./gradlew clean
```

Build debug APK:

```bash
./gradlew assembleDebug
```

Build release APK:

```bash
./gradlew assembleRelease
```

Install debug build on connected device:

```bash
./gradlew installDebug
```

Run unit tests:

```bash
./gradlew test
```

Run Android instrumentation tests:

```bash
./gradlew connectedAndroidTest
```

---

## Testing

Recommended manual test checklist:

- Register a student account.
- Register a tutor account.
- Create an admin account manually in Firebase.
- Log in as student, tutor, and admin.
- Create a tuition post as a student.
- Browse and show interest as a tutor.
- Confirm student receives interest notification.
- Schedule a meeting as a student.
- Accept/reject meeting as tutor.
- Confirm notifications update correctly.
- Submit a tutor review as student.
- Submit a problem report.
- Review reports and users from admin dashboard.
- Change post/user/meeting/report statuses from admin panel.

---

## Troubleshooting

### Gradle sync fails

Try:

```bash
./gradlew clean
./gradlew build
```

Also confirm Android Studio has SDK 36 installed.

### Firebase login or database read/write fails

Check:

- `app/google-services.json` exists.
- Firebase package name matches `com.example.project_findtutor`.
- Email/password authentication is enabled.
- Realtime Database is created.
- Database rules allow authenticated users during development.
- Device/emulator has internet access.

### User logs in but dashboard does not open

Check the database path:

```text
Users/{uid}/role
```

The role value must be one of:

```text
student
tutor
admin
```

### Admin login does not work

Create an authenticated Firebase user first, then manually add/update:

```text
Users/{adminUid}/role = "admin"
```

### Posts do not create correctly

Check that `jobCounter` exists or allow the app to initialize it automatically. Also confirm the logged-in user has a valid student profile under:

```text
Students/{uid}
```

---

## Notes for Future Development

Suggested improvements:

- Add secure Firebase Realtime Database rules by role.
- Add a proper admin creation/management process.
- Move hardcoded locations into Firebase or local resources.
- Add input validation for email format and numeric fields.
- Add loading states for long Firebase operations.
- Add ViewModel/Repository layers for cleaner architecture.
- Add offline caching strategy where suitable.
- Add unit tests for validation logic.
- Add UI tests for registration, login, post creation, and meeting flow.
- Add push notifications with Firebase Cloud Messaging.
- Add image/profile upload using Firebase Storage.
- Add pagination or query filtering for large database nodes.

---

## License

No license file is included in the current project. Add a license before publishing or distributing the project.

---

## Author

Project name: **Project-FindTutor**

Package name: `com.example.project_findtutor`
