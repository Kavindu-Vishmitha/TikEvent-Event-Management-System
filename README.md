# 🎯 TikEvent - M-Commerce Mobile Event Ticketing System

A **decentralized mobile application** designed to bridge the gap between event organizers (Sellers) and attendees (Buyers) through a real-time digital ticketing and event discovery ecosystem.

---

## 📘 Overview

**TikEvent** is a full-featured **Android M-commerce application** built using **Java** and powered by **Firebase** as a real-time backend. It offers a role-based interface inside a single application:
- **Buyer Role**: Browse upcoming events, view exact event locations using **Google Maps**, purchase digital tickets securely via **PayHere SDK**, and store QR-coded tickets in a digital vault.
- **Seller / Admin Role**: Publish and manage event listings, view real-time sales analytics, track revenue, manage ticket inventory, and verify attendee entry using an integrated **ZXing QR Scanner**.

---

## 🎯 Objectives

- 🎟️ Provide a direct, paperless M-commerce marketplace for event organizers and buyers.
- 📍 Eliminate location ambiguity by integrating interactive Google Maps navigation.
- 🔒 Prevent ticket duplication and fraud using unique, scannable QR Code verification.
- 💳 Enable seamless advance booking and digital payment processing via PayHere SDK.
- 📊 Deliver real-time sales performance and revenue analytics for event organizers.

---

## 🧰 Tech Stack

| Component | Technology |
|---|---|
| Frontend | Android SDK (Java), Material Design 3 |
| Backend & Cloud Services | Firebase Cloud Firestore, Firebase Storage, Firebase Auth, FCM |
| Payments | PayHere Android SDK |
| Maps & Location | Google Maps Android SDK, Geocoder API |
| QR Code Engine | ZXing (Zebra Crossing) Library |
| Image Caching | Glide |
| Analytics & Charts | MPAndroidChart |
| Offline Storage | SQLite (`NotificationDbHelper`), SharedPreferences (`SessionManager`) |
| IDE Used | Android Studio |
| Platform | Android (Mobile) |

---

## ⚙️ Features

### 🧑‍💼 Buyer Features
- **Event Discovery**: Browse featured, popular, and category-wise events with live search.
- **Interactive Maps**: View exact event venues on Google Maps and get real-time directions.
- **Ticket Vault**: Store purchased tickets with dynamic QR codes for offline verification.
- **Favorites & Notifications**: Save favorite events and receive real-time push/local updates.

### 🛡️ Seller / Admin Features
- **Event Management**: Full CRUD operations for event listings, pricing tiers (Standard, VIP, VVIP), and cover images.
- **Gate Scanning**: Built-in QR scanner to validate attendee tickets at entry in real-time.
- **Sales Analytics**: Live dashboards displaying revenue trends, tickets sold, and sales performance charts.
- **PDF Export**: Generate and download printable PDF sales reports.

---

## 🖥️ Interfaces

### 🖼️ Screenshots

<div align="center">

| **Splash** | **Sign In** | **Sign Up** |
|:---:|:---:|:---:|
| ![Splash Screen](https://github.com/Kavindu-Vishmitha/TikEvent-Event-Management-System/blob/ac41390cbff68cabac917721b42c84054ce5f6ca/Screenshot/Splash%20Screen.png) | ![Sign In Screen](https://github.com/Kavindu-Vishmitha/TikEvent-Event-Management-System/blob/625ed8b3b82cb054c20986cf14fe44c0b1774ea3/Screenshot/SignIn%20Fragment.png) | ![Sign Up Screen](https://github.com/Kavindu-Vishmitha/TikEvent-Event-Management-System/blob/625ed8b3b82cb054c20986cf14fe44c0b1774ea3/Screenshot/Registration%20Fragment.png) |

| **Forgot Password** | **Buyer Home** | **Categories** |
|:---:|:---:|:---:|
| ![Forgot Password](https://github.com/Kavindu-Vishmitha/TikEvent-Event-Management-System/blob/6f0ceea839d39009d093b356fb1f749b1b223090/Screenshot/Forgot%20Password%20Fragment.png) | ![Buyer Home](https://github.com/Kavindu-Vishmitha/TikEvent-Event-Management-System/blob/6f0ceea839d39009d093b356fb1f749b1b223090/Screenshot/Home%20Fragment.png) | ![Categories](https://github.com/Kavindu-Vishmitha/TikEvent-Event-Management-System/blob/6f0ceea839d39009d093b356fb1f749b1b223090/Screenshot/Categories%20Fragment.png) |

| **Single View** | **My Ticket** | **Ticket View** |
|:---:|:---:|:---:|
| ![Single View](https://github.com/Kavindu-Vishmitha/TikEvent-Event-Management-System/blob/0f1847078e4f596ff74118cff4785ee4441a5aae/Screenshot/Single%20View%20Fragment.png) | ![My Ticket](https://github.com/Kavindu-Vishmitha/TikEvent-Event-Management-System/blob/0f1847078e4f596ff74118cff4785ee4441a5aae/Screenshot/My%20Ticket%20Fragment.png) | ![Ticket View](https://github.com/Kavindu-Vishmitha/TikEvent-Event-Management-System/blob/0f1847078e4f596ff74118cff4785ee4441a5aae/Screenshot/Ticket%20View%20Fragment.png) |

| **Ticket** | **Favorites** | **Account** |
|:---:|:---:|:---:|
| ![Ticket](https://github.com/Kavindu-Vishmitha/TikEvent-Event-Management-System/blob/f2ac7837f53193c702b00ef3baf431e16628dce2/Screenshot/Ticket.jpeg) | ![Favorites](https://github.com/Kavindu-Vishmitha/TikEvent-Event-Management-System/blob/93abf9c54de830fabfa3dcabe711784b63c06414/Screenshot/Favorite%20Fragment.png) | ![Account](https://github.com/Kavindu-Vishmitha/TikEvent-Event-Management-System/blob/93abf9c54de830fabfa3dcabe711784b63c06414/Screenshot/Account%20Fragment.png) |

| **Admin Dashboard** | **Create Event** | **Manage Events** |
|:---:|:---:|:---:|
| ![Admin Dashboard](https://github.com/Kavindu-Vishmitha/TikEvent-Android/blob/main/screenshot/AdminDashboard.jpg?raw=true) | ![Create Event](https://github.com/Kavindu-Vishmitha/TikEvent-Android/blob/main/screenshot/CreateEvent.jpg?raw=true) | ![Manage Events](https://github.com/Kavindu-Vishmitha/TikEvent-Android/blob/main/screenshot/ManageEvents.jpg?raw=true) |

| **Edit Event** | **Event Location Picker** | **All Event Locations Map** |
|:---:|:---:|:---:|
| ![Edit Event](https://github.com/Kavindu-Vishmitha/TikEvent-Android/blob/main/screenshot/EditEvent.jpg?raw=true) | ![Location Picker](https://github.com/Kavindu-Vishmitha/TikEvent-Android/blob/main/screenshot/LocationPicker.jpg?raw=true) | ![All Locations Map](https://github.com/Kavindu-Vishmitha/TikEvent-Android/blob/main/screenshot/AllLocationsMap.jpg?raw=true) |

| **Ticket Management** | **Event Attendance List** | **Gate QR Scanner** |
|:---:|:---:|:---:|
| ![Ticket Management](https://github.com/Kavindu-Vishmitha/TikEvent-Android/blob/main/screenshot/TicketManagement.jpg?raw=true) | ![Attendance List](https://github.com/Kavindu-Vishmitha/TikEvent-Android/blob/main/screenshot/AttendanceList.jpg?raw=true) | ![Gate QR Scanner](https://github.com/Kavindu-Vishmitha/TikEvent-Android/blob/main/screenshot/QRScanner.jpg?raw=true) |

| **Sales & Revenue Reports** | **PDF Export View** | **Custom Dialog / Alert** |
|:---:|:---:|:---:|
| ![Sales Reports](https://github.com/Kavindu-Vishmitha/TikEvent-Android/blob/main/screenshot/Reports.jpg?raw=true) | ![PDF Export View](https://github.com/Kavindu-Vishmitha/TikEvent-Android/blob/main/screenshot/PDFExport.jpg?raw=true) | ![Custom Dialog](https://github.com/Kavindu-Vishmitha/TikEvent-Android/blob/main/screenshot/CustomDialog.jpg?raw=true) |

| **Side Navigation Drawer** | **PayHere Gateway View** |
|:---:|:---:|
| ![Side Drawer](https://github.com/Kavindu-Vishmitha/TikEvent-Android/blob/main/screenshot/SideDrawer.jpg?raw=true) | ![PayHere Gateway](https://github.com/Kavindu-Vishmitha/TikEvent-Android/blob/main/screenshot/PayHereGateway.jpg?raw=true) | ![Custom Dialog](https://github.com/Kavindu-Vishmitha/TikEvent-Android/blob/main/screenshot/CustomDialog.jpg?raw=true) |

</div>

---

## 📝 License

This project is for **academic and educational purposes only**.

Any form of **commercial use, redistribution, or modification** for profit is **strictly prohibited** without **explicit written permission** from the original author.
