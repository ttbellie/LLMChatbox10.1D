# LLM ChatBot 10.1D

This project is an improved LLM-enhanced learning assistant app developed for **SIT305 Task 10.1D**. The app extends the previous LLM chatbot by adding new screens and extra features, including quiz history, profile sharing, and account upgrading.

## App Overview

The app is a mobile learning assistant that allows users to chat with an AI assistant, generate quiz questions, answer multiple-choice questions, and track their learning progress. It also includes a profile page, history page, QR code sharing feature, and upgrade account screen.

The main goal of the app is to provide a simple and interactive learning experience using an LLM-based chatbot.

## Main Features

### 1. Login Screen

Users can enter their username before accessing the app.  
The username is passed to other screens and used to save user-specific data.

### 2. Chat Screen

The Chat screen allows users to send messages to the learning assistant.

Main functions include:

- Sending user messages
- Receiving AI responses from Gemini
- Saving chat messages locally
- Generating multiple-choice quiz questions
- Checking user answers
- Saving quiz results into local history

If the Gemini API reaches a temporary quota limit, the app includes a local fallback quiz so that the History feature can still be tested during the demo.

### 3. History Screen

The History screen displays saved quiz attempts.

Each history item includes:

- Quiz question
- User answer
- Correct answer
- Correct or incorrect result

This data is saved using Room database, so quiz history can remain available after reopening the app.

### 4. Profile Screen

The Profile screen shows the user’s learning statistics, including:

- Total questions answered
- Correct answers
- Incorrect answers
- Current account plan

The statistics are calculated from the saved quiz attempts in the local database.

### 5. QR Code Sharing

The Share button generates a QR code using the user’s public profile data.

The QR code includes:

- Username
- Current plan
- Total questions
- Correct answers
- Incorrect answers

Users can scan the QR code or share it as an image through other apps.

### 6. Upgrade Account Screen

The Upgrade Account screen provides three upgrade options:

- Starter
- Intermediate
- Advanced

The app uses Google Pay test mode and a demo confirmation flow for emulator testing. After a plan is selected, the upgraded plan is saved to the user profile in Room database.

## Technologies Used

- Kotlin
- Android Studio
- XML layouts
- Room Database
- Retrofit
- Gson Converter
- Kotlin Coroutines
- ViewBinding
- Google Gemini API
- ZXing QR Code library
- Google Pay / Wallet API test mode

## Project Structure

```text
app/src/main/java/com/example/llmchatbot/
│
├── data/
│   ├── AppDatabase.kt
│   ├── Message.kt
│   ├── MessageDao.kt
│   ├── QuizAttempt.kt
│   ├── QuizAttemptDao.kt
│   ├── UserProfile.kt
│   └── UserProfileDao.kt
│
├── network/
│   ├── ChatRepository.kt
│   ├── GeminiApi.kt
│   ├── GeminiClient.kt
│   └── GeminiModels.kt
│
└── ui/
    ├── LoginActivity.kt
    ├── ChatActivity.kt
    ├── HistoryActivity.kt
    ├── ProfileActivity.kt
    ├── UpgradeActivity.kt
    └── MessageAdapter.kt
