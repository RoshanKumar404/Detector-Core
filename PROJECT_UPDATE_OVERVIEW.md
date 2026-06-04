# Detector Project Update Overview

## What Was Fixed

The project was updated across both the Android app and the Flask backend to make reporting more reliable, harder to abuse, and easier to test on a real device.

## Android App Changes

### Map and Issues UI

- Reworked the map flow to use a WebView/Leaflet-based map instead of the unstable native map approach.
- Filtered invalid map coordinates like `0.0, 0.0` so fake/default locations do not break the map view.
- Added an "All User Issues" screen that displays reports from all users with:
  - issue image
  - location
  - municipality
  - reporter name, when available
  - description/status

### Report Creation

- Added image upload from gallery.
- Added manual latitude/longitude entry for cases where:
  - GPS is unavailable
  - the user uploads an old/gallery image
- Prevented reports from being submitted with invalid coordinates.
- Added app-side validation so only reports with:
  - prediction: `waterlogged`
  - confidence: `88%` or higher
  can be submitted.

### Anti-Fraud Data Sent to Backend

Each Android report now sends:

- `device_fingerprint`: a hashed Android device ID used to detect repeated submissions from the same device.
- `location_source`: either `gps` or `manual`, depending on how the report location was provided.

### Auth Screen Styling

- Fixed text input color bugs on login and registration screens.
- Made input text, placeholder text, cursor, icons, and borders readable against the current theme.

### APK Generated

Debug APKs were generated successfully:

- `D:\KotlinAps\Detector\app\build\outputs\apk\debug\app-arm64-v8a-debug.apk`
- `D:\KotlinAps\Detector\app\build\outputs\apk\debug\app-armeabi-v7a-debug.apk`

## Backend Changes

Backend path:

`D:\Desktop\Final Year\DetectorBackSupport`

### Report Acceptance Rule

The backend now rejects reports unless:

- `prediction == "waterlogged"`
- `confidence >= 0.88`

This prevents low-confidence or non-waterlogging images from entering the system.

### Coordinated Fake Report Protection

Added hourly duplicate protection:

- One user cannot submit repeated reports for the same nearby location within one hour.
- The same device fingerprint cannot submit repeated reports for the same nearby location within one hour.

Nearby means within about `100 meters`.

### Image and Location Verification

The backend now reads EXIF metadata from uploaded images where available.

It checks:

- Image capture time must be close to submission time.
- EXIF GPS and submitted phone/manual GPS should not differ by more than `100 meters`.
- Missing or suspicious EXIF data is recorded as a fraud flag.

Reports are not always rejected for missing EXIF because many gallery/compressed images remove metadata. Instead, they are marked for verification.

### Verification Fields Added

The `Issue` model now includes:

- `verification_status`
- `verification_weight`
- `fraud_flags`
- `device_fingerprint`
- `location_source`
- `exif_captured_at`
- `exif_latitude`
- `exif_longitude`

These fields support moderation, fraud review, and future reputation/adaptive threshold logic.

### Cluster-Based Valid Report Rule

Added one more validation layer:

- A clean report is not treated as fully valid immediately.
- It stays `pending` until at least `5` clean waterlogging reports exist in the same area.
- Same area means within a `50 meter` radius.
- When the fifth nearby clean report arrives, the backend marks the whole nearby cluster as:
  - `verification_status = trusted`
  - `status = verified`

This means a single report can be saved for review, but the location becomes officially verified only after enough nearby reports support it.

### New Account Weighting

Reports from accounts less than 7 days old receive a lower verification weight:

- new account: `0.5`
- older account: `1.0`

This makes organized fake-account attacks harder to count as trusted reports.

### Migration Added

Migration file added:

`migrations/versions/8b2f9d1a6c33_add_issue_verification_fields.py`

Run this before using the updated backend database:

```bash
./venv/Scripts/python.exe -m flask db upgrade
```

If the venv was moved between folders or drives, recreate it first:

```bash
python -m venv venv
./venv/Scripts/python.exe -m pip install -r requirements.txt
./venv/Scripts/python.exe -m flask db upgrade
```

## Verification Done

The backend was checked with:

```bash
./venv/Scripts/python.exe -m py_compile app/routes/issues.py app/models/issue.py
./venv/Scripts/python.exe -c "from app import create_app; app=create_app(); print('routes loaded', len(list(app.url_map.iter_rules())))"
```

Result:

- Python compile passed.
- Flask app loaded successfully.
- Android debug build passed with `assembleDebug`.

## Still Pending / Future Work

The adaptive threshold system is only partially prepared.

Already added:

- verification weights
- fraud flags
- new account weighting

Still to build later:

- area-based thresholds, for example:
  - dense city: require around 10 trusted reports
  - small town/rural area: require around 3 to 5 trusted reports
- municipality dashboard logic using weighted reports
- automatic escalation/notification when a location crosses its trusted threshold

## Summary

The app now prevents weak AI detections from being submitted, supports gallery/manual-location reports, sends anti-fraud metadata, and has better issue browsing. The backend now validates report confidence, rate-limits suspicious repeated submissions, stores verification metadata, and flags image/location mismatches for review.

## How It Was Implemented

### 1. Android Report Flow

The report flow starts from the capture/gallery screen.

Main files:

- `app/src/main/java/com/example/detector/presentation/screens/capture/CaptureScreen.kt`
- `app/src/main/java/com/example/detector/presentation/screens/capture/AiResultScreen.kt`
- `app/src/main/java/com/example/detector/presentation/screens/capture/AiResultViewModel.kt`
- `app/src/main/java/com/example/detector/data/repository/IssueRepositoryImpl.kt`
- `app/src/main/java/com/example/detector/data/api/ApiService.kt`

How it works:

1. User captures an image or selects one from gallery.
2. The image is passed to the AI result screen.
3. `AiResultViewModel.runPrediction()` reads the image bytes and calls the backend prediction API.
4. The backend returns:
   - prediction name
   - confidence score
5. The UI shows the prediction result.
6. Before submit, Android checks:
   - prediction must be `waterlogged`
   - confidence must be at least `0.88`
7. If GPS is available, the app uses GPS location.
8. If GPS is unavailable or the user chooses manual mode, the app uses manually entered latitude/longitude.
9. `IssueRepositoryImpl.createIssue()` builds a multipart request with:
   - image
   - latitude
   - longitude
   - prediction
   - confidence
   - device fingerprint
   - location source
10. `ApiService.createIssue()` sends that request to:

```text
POST /api/issues/
```

### 2. Android Confidence Gate

The confidence rule is enforced in two places.

In the UI:

```kotlin
val canSubmitPrediction = prediction.lowercase() == "waterlogged" && confidence >= 0.88
```

This disables the submit button for weak/non-waterlogged predictions.

In the ViewModel:

```kotlin
if (prediction.lowercase() != "waterlogged" || confidence < 0.88) {
    _uiState.value = AiResultUiState.Error(
        "Only waterlogged results above 88% confidence can be submitted."
    )
    return@launch
}
```

This protects the submit logic even if the UI state behaves unexpectedly.

### 3. Device Fingerprint

Main file:

`app/src/main/java/com/example/detector/data/repository/SessionManager.kt`

How it works:

1. Android reads `Settings.Secure.ANDROID_ID`.
2. The app hashes it using SHA-256.
3. The hash is sent as `device_fingerprint`.

This avoids sending the raw Android ID directly while still letting the backend detect repeated submissions from the same device.

### 4. Location Source

The app sends one of these values:

- `gps`
- `manual`

This is decided inside `AiResultViewModel.submitReport()`:

```kotlin
locationSource = if (manualLocation == null) "gps" else "manual"
```

The backend stores this in `Issue.location_source`.

### 5. Backend Report Creation

Main backend file:

`app/routes/issues.py`

The main function is:

```python
@issues_bp.route('/', methods=['POST'])
@jwt_required()
def create_issue():
```

How it works:

1. Backend receives multipart form data.
2. It reads:
   - image
   - latitude
   - longitude
   - prediction
   - confidence
   - device fingerprint
   - location source
3. It validates latitude/longitude.
4. It rejects default invalid coordinates like `0.0, 0.0`.
5. It checks prediction and confidence:

```python
if normalized_prediction != "waterlogged" or confidence_score < MIN_WATERLOGGED_CONFIDENCE:
    return jsonify({
        "error": "Report rejected: only waterlogged predictions above 88% confidence are accepted",
        "prediction": normalized_prediction,
        "confidence": confidence_score,
        "required_confidence": MIN_WATERLOGGED_CONFIDENCE
    }), 422
```

6. It reads image EXIF metadata.
7. It uploads the image to Cloudinary.
8. It checks for duplicate nearby reports.
9. It creates the `Issue`.
10. It stores fraud/verification metadata with the issue.
11. It checks whether at least 5 clean waterlogging reports exist within 50 meters.
12. If yes, the nearby clean reports become verified/trusted.

### 6. Rate Limiting by User and Device

Main helper:

```python
find_recent_nearby_report(query, latitude, longitude)
```

How it works:

1. Backend looks for reports created within the last hour.
2. It calculates distance between the old report and the new report.
3. Distance is calculated using the Haversine formula.
4. If the same user already reported within `100 meters`, the backend returns `429`.
5. If the same device fingerprint already reported within `100 meters`, the backend also returns `429`.

This handles the "10 friends sitting at home" loophole partially by stopping rapid repeated reports from the same user/device/location.

### 7. EXIF Validation

Main helper:

```python
extract_image_metadata(image_bytes)
```

How it works:

1. Backend opens the uploaded image using Pillow.
2. It tries to read EXIF metadata.
3. It checks image capture timestamp.
4. It checks EXIF GPS coordinates if available.
5. If image timestamp is too far from submission time, it adds a fraud flag.
6. If EXIF GPS and submitted GPS differ by more than `100 meters`, it adds a fraud flag.
7. If EXIF data is missing, it also records that as a flag instead of directly rejecting the report.

This is important because many gallery images lose EXIF metadata after compression or sharing.

### 8. Verification Status and Weight

Main model:

`app/models/issue.py`

New fields were added:

```python
verification_status = db.Column(db.String(20), default='pending')
verification_weight = db.Column(db.Float, default=1.0)
fraud_flags = db.Column(db.Text, nullable=True)
device_fingerprint = db.Column(db.String(255), nullable=True)
location_source = db.Column(db.String(30), nullable=True)
exif_captured_at = db.Column(db.DateTime, nullable=True)
exif_latitude = db.Column(db.Float, nullable=True)
exif_longitude = db.Column(db.Float, nullable=True)
```

How status is decided:

- If fraud flags exist, report becomes `flagged`.
- If no fraud flags exist but fewer than 5 nearby clean reports exist, report stays `pending`.
- If 5 or more clean reports exist within 50 meters, the cluster becomes `trusted` and `verified`.

How weight is decided:

```python
if account_age < 7 days:
    weight = 0.5
else:
    weight = 1.0
```

This means new accounts count less than older accounts.

### 8.1 Cluster Validation

Main helpers:

```python
find_nearby_cluster_issues(latitude, longitude, radius_meters=50)
apply_cluster_validation(latitude, longitude, fraud_flags)
```

How it works:

1. After the backend saves the new report, it searches for nearby waterlogging reports.
2. The search radius is `50 meters`.
3. Flagged reports are ignored.
4. If the nearby clean report count is less than `5`, the new report remains pending.
5. If the count is `5` or more, every clean report in that nearby cluster is updated:

```python
issue.verification_status = "trusted"
issue.status = "verified"
```

The response also includes:

- `cluster_count`
- `required_cluster_reports`
- `cluster_radius_meters`

### 9. Database Migration

Main migration:

`migrations/versions/8b2f9d1a6c33_add_issue_verification_fields.py`

How it works:

The migration adds the new verification columns to the `issues` table.

Run it with:

```bash
./venv/Scripts/python.exe -m flask db upgrade
```

This updates the database schema so the backend can save the new fields.

### 10. All Issues Screen

Main files:

- `app/src/main/java/com/example/detector/presentation/screens/issues/AllIssuesScreen.kt`
- `app/src/main/java/com/example/detector/presentation/screens/issues/AllIssuesViewModel.kt`
- `app/src/main/java/com/example/detector/data/repository/IssueRepositoryImpl.kt`
- `app/src/main/java/com/example/detector/data/api/ApiService.kt`

How it works:

1. `AllIssuesViewModel` asks the repository for global issues.
2. Repository calls:

```text
GET /api/issues/global
```

3. Backend returns issues from all users.
4. Android maps the API DTO into the app's `Issue` model.
5. `AllIssuesScreen` renders each issue in a card with image, municipality, location, status, and user name.

### 11. Map Screen

Main files:

- `app/src/main/java/com/example/detector/presentation/screens/map/MapScreen.kt`
- `app/src/main/java/com/example/detector/presentation/screens/map/MapViewModel.kt`
- `app/src/main/assets/leaflet_map.html`

How it works:

1. `MapViewModel` loads map issues from the backend.
2. Android passes issue data into the Leaflet WebView.
3. Leaflet renders markers/circles using latitude and longitude.
4. Invalid coordinates like `0.0, 0.0` are ignored.
5. Valid issue locations are shown on the map.

The WebView/Leaflet approach was used because native map libraries were crashing or behaving inconsistently on the real device.
