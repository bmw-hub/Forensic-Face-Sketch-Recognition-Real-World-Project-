# ThirdEye — Forensic Face Sketch Recognition (JavaFX + AWS Rekognition)

**ThirdEye** is a two-part forensic prototype that helps investigators **create a facial composite sketch** and then **search for the closest match** against a suspect photo database using **AWS Rekognition**.

- **Part A — `ThirdEye v2` (JavaFX / NetBeans Ant project)**: login + OTP flow and a sketch/composite workflow UI.
- **Part B — `ThirdEye_FaceMatch` (Maven / Swing UI)**: uploads a sketch image to **S3** and queries **Rekognition Collections** to retrieve the **best matching suspect photo**.

> **Ethics & legality**: This project is for educational/research use. If you use it with real biometric data, ensure consent, data minimization, secure storage, and compliance with local laws/policy.

---

## Highlights

- **Sketch → match workflow**: treat the sketch as the *query image*; keep **real suspect photos** in the Rekognition collection.
- **S3 + Rekognition integration**: upload sketch to S3, then `SearchFacesByImage` against a collection.
- **Configurable similarity threshold**: tune match strictness (reduce false positives).
- **Offline-friendly UI** for the “sketching” side; cloud only required for matching.

---

## Repository layout

```
Face-sketch-Recognizer-in-Digital-Forensics-using-Computer-Vision-/
  README.md                                  # legacy upstream instructions
  Project Code (forensic face sketch)/
    ThirdEye v2/                             # JavaFX (NetBeans/Ant)
    ThirdEye_FaceMatch/                      # Maven project (AWS Rekognition)
```

---

## Requirements

### For `ThirdEye v2` (JavaFX)
- **Java JDK 8** (this project is typically run with NetBeans + JavaFX tooling)
- **Apache NetBeans** + **Scene Builder** (for editing `.fxml`)
- **SQLite** (local DB file `login.sqlite`)

### For `ThirdEye_FaceMatch` (AWS matching)
- **Java 13+** (per `pom.xml`)
- **Maven**
- **AWS account** (Free Tier works) with:
  - **S3** bucket access
  - **Rekognition** access (Collections + SearchFacesByImage)
- AWS credentials configured via **AWS CLI** or environment variables (recommended: `aws configure`)

---

## Quick start (recommended order)

### 1) Clone and open

```bash
git clone https://github.com/bmw-hub/Forensic-Face-Sketch-Recognition-Real-World-Project-.git
cd Forensic-Face-Sketch-Recognition-Real-World-Project-
```

### 2) Run the JavaFX app (`ThirdEye v2`)

- Open NetBeans
- `File → Open Project…` and select:
  - `Face-sketch-Recognizer-in-Digital-Forensics-using-Computer-Vision-/Project Code (forensic face sketch)/ThirdEye v2`
- Add libraries if NetBeans didn’t resolve them automatically:
  - In NetBeans: `ThirdEye v2 → Libraries → Add JAR/Folder…`
  - Add jars from `ThirdEye v2/lib`
- Run (NetBeans **F6**)

**Login + OTP**
- The app reads users from `ThirdEye v2/login.sqlite`
- OTP sending is implemented in `Login_screenController.java` and requires valid SMTP credentials.

> Tip: For Gmail, use an **App Password** (recommended) rather than “less secure apps”.

### 3) Configure AWS for matching (`ThirdEye_FaceMatch`)

1. Create an S3 bucket (same region as your client).
2. Create a Rekognition collection (once).
3. Add suspect **real photos** (not sketches) into the collection.
4. Run the FaceMatch UI and search using a sketch.

---

## AWS setup (minimal)

### IAM permissions
Your IAM user/role should allow (at minimum):
- Rekognition: `CreateCollection`, `IndexFaces`, `SearchFacesByImage`, `ListCollections`, `DeleteFaces`
- S3: `PutObject`, `GetObject`, `ListBucket`

### Configure credentials locally

```bash
aws configure
```

This project uses the AWS SDK default credential chain (CLI profile / env vars / instance role).

---

## How matching works (important)

**Correct data model**
- **Rekognition collection contains**: *real suspect photos*
- **Sketch is used as**: *query image only* (uploaded to S3 temporarily)

If your collection contains old test images, you may get wrong matches. The FaceMatch UI includes guidance in:
- `ThirdEye_FaceMatch/src/main/java/.../face_rekognition.java`

---

## Build & run

### `ThirdEye_FaceMatch` (Maven)

From:
`Face-sketch-Recognizer-in-Digital-Forensics-using-Computer-Vision-/Project Code (forensic face sketch)/ThirdEye_FaceMatch`

```bash
mvn clean package
```

Then run your main class (example):

```bash
mvn -q exec:java -Dexec.mainClass="com.mycompany.thirdeye_facematch.face_rekognition"
```

> If `exec-maven-plugin` isn’t configured, you can run from NetBeans/IDEA by selecting the main class.

---

## Large files & Git LFS

This repository uses **Git LFS** for large binaries such as `*.jar` and `*.psd` (see `.gitattributes`).  
If you clone without LFS, install it and pull LFS objects:

```bash
git lfs install
git lfs pull
```

---

## Security notes (read before publishing)

- **Do not commit credentials** (AWS keys, SMTP passwords, tokens). This repo is configured to ignore common `credentials` files.
- Prefer:
  - **AWS CLI profiles** (`aws configure`)
  - **Environment variables** / instance roles
  - **App Passwords** (for Gmail SMTP)
- If GitHub blocks a push for secrets, remove them from history and rotate the leaked keys immediately.

---

## Troubleshooting

- **GitHub rejects push due to file size**: ensure Git LFS is installed and large binaries are tracked.
- **No match found**:
  - ensure suspect photos are indexed into your Rekognition collection
  - try adjusting similarity threshold
  - ensure sketch image quality is sufficient
- **S3 object not found**:
  - confirm the uploaded sketch key and bucket name match your configuration
- **OTP email fails**:
  - verify SMTP credentials and network access
  - prefer Gmail App Passwords and TLS settings

---

## Roadmap (ideas)

- Replace hard-coded bucket/collection with a **config screen** or config file template.
- Add **audit logging** (who searched what, when) with redaction.
- Add **local pre-check** (face detection quality) before calling Rekognition.
- Package into an installer with a clean first-run wizard.

---

## License

If you plan to make this public/production-ready, add a license file (MIT/Apache-2.0/etc.) and document dataset/model provenance.

