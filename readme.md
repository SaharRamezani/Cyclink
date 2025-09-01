# Cyclink App - Concise README

Cyclink is a mobile application designed for cyclists, offering a range of features to enhance their riding experience, facilitate team collaboration, and track their activities.

## Features

### Team Management

Cyclink allows users to create and join teams, manage team members, and view team-related activities. This includes:

*   **Team Overview:** See a ranking of team members based on their cycling statistics.

    <img src="screenshots/main_page_screenshot.jpg" width="300">

*   **Member Tracking:** View individual member details and their locations on a map.

    <img src="screenshots/map_screenshot.jpg" width="300">

### Activity Tracking & History

Track your cycling activities and review your performance over time.

*   **Ride History:** Records of past rides.

    <img src="screenshots/ride_history_screenshot.jpg" width="300">

*   **GPS Integration:** Utilize GPS to record routes and provide accurate location data.

### Communication & AI Assistance

*   **Help Chat:** Get real-time help using AI-powered chat.

    <img src="screenshots/chat_feature_screenshot.jpg" width="300">

### User Authentication & Profile

*   **Google Sign-In:** Secure and convenient authentication using Google accounts.

    <img src="screenshots/login_screenshot.jpg" width="300">

*   **User Profile:** Manage personal account information.

    <img src="screenshots/profile_screenshot.jpg" width="300">

## Setup and Configuration

To run the Cyclink app, the following configuration files and API credentials are required:

*   **`google-services.json`**: This file is essential for connecting your Android application to Firebase services, including Firebase Authentication and Firestore. It should be placed in the `app/` directory of your project.

*   **API Credentials**:
    *   **Google Gemini API Key**: Required for the AI Helper functionality within the chat feature.
    *   **Google Maps API Key**: Necessary for displaying maps and tracking locations in features like member tracking and ride history.
    *   **Web Client ID for Authentication**: Used for Google Sign-In to authenticate users.

Ensure these are correctly configured in your project for all features to function as intended.