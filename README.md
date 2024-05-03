# ChatApp

ChatApp is a Java-based realtime chatting Android application built with Firebase, FCM, and Glide. It offers seamless authentication via OTP verification, leveraging Firebase Firestore for efficient data storage including user profiles, chat histories, and media content. The app features real-time user status indicators, username search functionality, and instant notifications powered by FCM for new messages.


## Features

- **Authentication with OTP:** Seamlessly verifies users' phone numbers using OTP verification for secure login.

- **Real-time Chat:** Enables users to communicate in real-time with other users, with chat histories stored efficiently in Firebase Firestore.

- **Chat Request System:** Enhances security by allowing users to initiate chats only after their request is accepted by the other user, preventing unwanted communication.

- **Message Encryption:** Ensures the security of messages by encrypting them using AES before storing them in the database, safeguarding user privacy and data integrity.

- **Media Content Handling:** Supports the storage and sharing of media content within chats, ensuring a rich messaging experience.

- **User Presence Indicators:** Displays users' online status, allowing for better communication timing and engagement.

- **User Search Functionality:** Facilitates easy discovery of other users by enabling search through usernames.

- **Instant Notifications:** Employs FCM to deliver instant notifications upon receiving new chat messages, ensuring timely responses.


## Tech Stack

**IDE:** Android Studio

**Languages:** Java, XML

**Database:** Firebase Firestore for chats, profiles etc. and Firebase Storage for Media

**Notification:** Firebase Cloud Messaging 


### Libraries

    // Country code picker
    implementation 'com.hbb20:ccp:2.5.0'

    // Handling media
    implementation 'com.github.dhaval2404:imagepicker:2.1'
    implementation 'com.github.bumptech.glide:glide:4.15.1'
    
    //Firebase cloud messaging
    implementation 'com.google.firebase:firebase-messaging:23.4.0'
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
