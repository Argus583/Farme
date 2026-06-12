# Farme — Санарип Дыкан

Digital agricultural marketplace for Kyrgyzstan connecting farmers, livestock producers, and suppliers.

## Features

- **Marketplace** — Create and browse listings for livestock, grain, vegetables, dairy, equipment, and services
- **Animal Passports** — Digital veterinary certificates with vaccine tracking and breed information
- **Map Discovery** — Google Maps integration with category-filtered markers and nearby listings
- **Real-time Chat** — Buyer-seller messaging with listing context and unread badges
- **Authentication** — Phone OTP, email/password, and Google Sign-In
- **Seller Profiles** — Ratings, reviews, verification status, and sales history
- **Favorites & Search** — Wishlist, autocomplete search, and advanced filtering by price/region/category
- **Support System** — In-app ticket chat for user and admin communication
- **Push Notifications** — Firebase Cloud Messaging for messages and listing updates
- **Multilingual** — Russian, English, and Kyrgyz (Кыргызча)

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 11 |
| Auth | Firebase Authentication (Phone, Email, Google) |
| Database | Firebase Realtime Database |
| Storage | Firebase Cloud Storage |
| Messaging | Firebase Cloud Messaging |
| Maps | Google Maps SDK |
| UI | Material Design 3, ViewBinding, Glide |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 (Android 15) |

## Setup

1. Clone the repository
   ```bash
   git clone https://github.com/Argus583/Farme.git
   ```
2. Open in Android Studio
3. Add your `google-services.json` to the `app/` directory (from Firebase Console)
4. Add your Google Maps API key to `AndroidManifest.xml`
5. Build and run

## Screenshots

_Coming soon_

## License

This project is for educational and portfolio purposes.
