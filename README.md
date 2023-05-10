# ISAC: Impaired Sight Audio Companion

ISAC is a project that uses an ESP32-CAM module and an ultrasonic sensor to capture images and detect objects present in the images using TensorFlow Lite object detection library. The detected objects' names are converted to speech using text-to-speech and can be heard by visually challenged individuals through an Android application.

## Components

- ESP32-CAM module
- Ultrasonic sensor
- Firebase server
- Android application
- TensorFlow Lite object detection library

## Functionality

The system operates by continuously measuring the distance using the ultrasonic sensor. If the distance measured is less than 100, the ESP32-CAM module captures an image, which is then converted to base64 format and sent to the Firebase server as a JSON file. The Android application continuously monitors the Firebase Realtime Database and fetches the data whenever a new JSON file is added to the database. The fetched data is displayed on the texture view and then analyzed using the TensorFlow Lite object detection library to detect objects present in the image. The detected object's names are then converted to speech using text-to-speech and can be heard by visually challenged individuals through the Android application.

## Installation and Configuration

To use this project, follow these steps:

1. Connect the ESP32-CAM module and ultrasonic sensor to your development board According to the ciruit Diagram.
2. Clone the project repository and upload the code to your development board(The Code for Arduino is in the Folder Blind_Assistant.
3. Set up a Firebase account and create a new Realtime Database.
4. Configure the ESP32-CAM module to connect to the internet and the Firebase Realtime Database.
5. Install the Android application on your device and configure it to connect to the Firebase Realtime Database.
6. Install the TensorFlow Lite object detection library and ensure that it is properly configured in your Android application.

## Screenshots
### The Hardware
<img width="939" alt="Screenshot 2023-05-10 at 11 40 32 PM" src="https://github.com/AbhishekPSingh07/ISAC/assets/79076050/697f7eae-117d-441a-a8af-18863e8c9449">

### Sample Outputs from the android App
<img width="505" alt="Screenshot 2023-05-10 at 11 49 55 AM" src="https://github.com/AbhishekPSingh07/ISAC/assets/79076050/dce648dc-ab93-4c46-ac2e-8857c524d4e4">
<img width="505" alt="Screenshot 2023-05-10 at 11 49 43 AM" src="https://github.com/AbhishekPSingh07/ISAC/assets/79076050/c1d4f706-d9d6-4298-b241-8cb9035fdd9e">
<img width="505" alt="Screenshot 2023-05-10 at 11 49 25 AM" src="https://github.com/AbhishekPSingh07/ISAC/assets/79076050/f64c4f42-3aba-4492-9454-cc30df8c0a0f">




## Contributors

This project was created by [AbhishekPSingh07](https://github.com/AbhishekPSingh07),[sabu13](https://github.com/sabu13). Contributions to the project are welcome and appreciated.


