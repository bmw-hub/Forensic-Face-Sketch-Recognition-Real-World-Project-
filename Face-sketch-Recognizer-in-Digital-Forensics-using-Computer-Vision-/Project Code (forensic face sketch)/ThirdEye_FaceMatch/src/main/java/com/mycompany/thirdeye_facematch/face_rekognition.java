/*
 * ThirdEye FaceMatch - Face Sketch Recognition using AWS Rekognition
 * Author: Himanshu Sai
 *
 * FIX FOR: Wrong face being matched (sketch matching wrong person)
 *
 * ROOT CAUSE EXPLANATION:
 * ─────────────────────────────────────────────────────────────────
 * The Rekognition COLLECTION stores indexed SUSPECT PHOTOS (real faces).
 * The SKETCH is only used as a QUERY IMAGE — it is NOT stored in the collection.
 *
 * The wrong match happened because:
 *   1. Old "test.jpg" (Shah Rukh sketch) was previously added to the COLLECTION
 *      by collection_add_image.java — it was indexed as a face in the database.
 *   2. When a new sketch is uploaded and searched, Rekognition found "test.jpg"
 *      as the closest match at 69% — but this is wrong data in your collection.
 *
 * HOW TO FIX:
 * ─────────────────────────────────────────────────────────────────
 *   STEP 1: Delete "test.jpg" face from your Rekognition collection
 *           Use collection_delete.java or AWS Console → Rekognition → Collections
 *           → MyFaceCollection → delete the face with ExternalImageId = "test.jpg"
 *
 *   STEP 2: Re-add correct suspect photos using collection_add_image.java
 *           Each photo should be a REAL PHOTO of a suspect, not a sketch.
 *           The ExternalImageId should be the person's name/ID (e.g. "john_doe.jpg")
 *
 *   STEP 3: Then upload the sketch of the suspect and click Find Match.
 *           Rekognition will compare the sketch against the real photos in collection.
 *
 * COLLECTION SHOULD CONTAIN:  Real suspect photos (added via collection_add_image.java)
 * SEARCH QUERY SHOULD BE:     Forensic sketch (uploaded via Upload Sketch button)
 *
 * ─────────────────────────────────────────────────────────────────
 * The code below is the same as the previously fixed version,
 * with added DEBUG LOGGING in find_matchActionPerformed to help
 * diagnose future wrong-match issues.
 * ─────────────────────────────────────────────────────────────────
 */

package com.mycompany.thirdeye_facematch;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.MediaTracker;
import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.amazonaws.HttpMethod;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.FaceMatch;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.rekognition.model.SearchFacesByImageRequest;
import com.amazonaws.services.rekognition.model.SearchFacesByImageResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

/**
 * Face Sketch Recognition UI using AWS Rekognition and S3.
 *
 * WORKFLOW:
 *   1. Add real suspect PHOTOS to the Rekognition collection (collection_add_image.java)
 *   2. Open a forensic SKETCH of the suspect (Open Sketch button)
 *   3. Upload the sketch to S3 (Upload Sketch button)
 *   4. Search for a matching face in the collection (Find Match button)
 */
public class face_rekognition extends javax.swing.JFrame {

    // Stores the current S3 key of the uploaded sketch
    private String currentS3Key = "";

    // ── AWS Configuration ─────────────────────────────────────────────────────
    private static final String  BUCKET_NAME    = "thirdeye-facematch-himanshu";
    private static final String  COLLECTION_ID  = "MyFaceCollection";
    private static final Regions CLIENT_REGION  = Regions.AP_SOUTH_1;

    // Minimum similarity threshold — raise this to reduce false matches
    // e.g. set to 70F to only show matches above 70% similarity
    private static final float   MIN_SIMILARITY = 50F;

    public face_rekognition() {
        initComponents();
        sketch_path.setVisible(false);
        match_path.setVisible(false);
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    @SuppressWarnings("unchecked")
    private void initComponents() {

        sketch           = new javax.swing.JLabel();
        match            = new javax.swing.JLabel();
        jSeparator1      = new javax.swing.JSeparator();
        open_sketch      = new javax.swing.JButton();
        upload_sketch    = new javax.swing.JButton();
        find_match       = new javax.swing.JButton();
        jScrollPane1     = new javax.swing.JScrollPane();
        match_properties = new javax.swing.JTextArea();
        match_similarity = new javax.swing.JLabel();
        jSeparator2      = new javax.swing.JSeparator();
        sketch_path      = new javax.swing.JTextField();
        match_path       = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("ThirdEye — Forensic Face Sketch Matcher");

        sketch.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        match.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        open_sketch.setText("OPEN SKETCH");
        open_sketch.addActionListener(evt -> open_sketchActionPerformed(evt));

        upload_sketch.setText("UPLOAD SKETCH");
        upload_sketch.addActionListener(evt -> upload_sketchActionPerformed(evt));

        find_match.setText("FIND MATCH");
        find_match.addActionListener(evt -> find_matchActionPerformed(evt));

        match_properties.setColumns(20);
        match_properties.setRows(5);
        jScrollPane1.setViewportView(match_properties);

        match_similarity.setFont(new java.awt.Font("Roboto", Font.BOLD, 18));
        match_similarity.setForeground(new java.awt.Color(0, 204, 0));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);

        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(50)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jSeparator2)
                    .addComponent(jSeparator1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(sketch, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(29)
                        .addComponent(match,  javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(open_sketch,    javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18)
                        .addComponent(upload_sketch,  javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18)
                        .addComponent(find_match,     javax.swing.GroupLayout.DEFAULT_SIZE,   139, Short.MAX_VALUE))
                    .addComponent(match_similarity, javax.swing.GroupLayout.Alignment.TRAILING,
                            javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(32)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 243, Short.MAX_VALUE)
                    .addComponent(sketch_path)
                    .addComponent(match_path))
                .addContainerGap(53, Short.MAX_VALUE))
        );

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(42)
                .addComponent(match_similarity, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(1)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(sketch, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(match,  javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1))
                .addGap(18)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(sketch_path, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(7)
                        .addComponent(match_path, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 13, Short.MAX_VALUE))
                    .addComponent(open_sketch,   javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(upload_sketch, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(find_match,    javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(42))
        );

        pack();
        setLocationRelativeTo(null);
    }
    // </editor-fold>

    // ─────────────────────────────────────────────────────────────────────────
    // OPEN SKETCH — Browse and display the sketch image locally
    // ─────────────────────────────────────────────────────────────────────────
    private void open_sketchActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser fileChooser = new JFileChooser(System.getProperty("user.home"));
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Image Files (jpg, png, jpeg, gif)", "jpg", "png", "jpeg", "gif");
        fileChooser.setFileFilter(filter);

        int selected = fileChooser.showOpenDialog(null);
        if (selected == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            sketch_path.setText(file.getAbsolutePath());

            // Clear any previous match results when a new sketch is opened
            clearMatchResults();
            currentS3Key = "";

            // Scale and display the sketch
            ImageIcon icon = new ImageIcon(file.getAbsolutePath());
            Image scaled   = icon.getImage()
                    .getScaledInstance(sketch.getWidth(), sketch.getHeight(), Image.SCALE_SMOOTH);
            sketch.setIcon(new ImageIcon(scaled));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPLOAD SKETCH — Upload the sketch to S3 as a temporary query image
    // NOTE: This does NOT add the sketch to the Rekognition collection.
    //       It only stores it in S3 so Rekognition can read it for comparison.
    // ─────────────────────────────────────────────────────────────────────────
    private void upload_sketchActionPerformed(java.awt.event.ActionEvent evt) {
        if (sketch_path.getText().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Please open a sketch first before uploading.",
                    "No Sketch Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final String fileName       = sketch_path.getText();
        final String fileObjKeyName = "sketch_" + System.currentTimeMillis() + ".jpg";

        setButtonsEnabled(false);
        upload_sketch.setText("UPLOADING...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                        .withRegion(CLIENT_REGION).build();

                PutObjectRequest request = new PutObjectRequest(
                        BUCKET_NAME, fileObjKeyName, new File(fileName));
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType("image/jpeg");
                request.setMetadata(metadata);
                s3Client.putObject(request);
                return null;
            }

            @Override
            protected void done() {
                setButtonsEnabled(true);
                upload_sketch.setText("UPLOAD SKETCH");
                try {
                    get(); // rethrows exception if upload failed
                    currentS3Key = fileObjKeyName;
                    clearMatchResults();
                    JOptionPane.showMessageDialog(null,
                            "Sketch uploaded successfully!\nNow click FIND MATCH.",
                            "Upload Successful", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    Logger.getLogger(face_rekognition.class.getName())
                          .log(Level.SEVERE, "Upload failed", e);
                    JOptionPane.showMessageDialog(null,
                            "Upload failed.\nError: " + e.getMessage(),
                            "Upload Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FIND MATCH — Query Rekognition collection with the uploaded sketch
    //
    // IMPORTANT: The Rekognition collection must contain REAL SUSPECT PHOTOS,
    // added using collection_add_image.java BEFORE using this feature.
    // If the collection contains wrong/old data, delete those faces first.
    // ─────────────────────────────────────────────────────────────────────────
    private void find_matchActionPerformed(java.awt.event.ActionEvent evt) {
        if (currentS3Key.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Please upload a sketch first before searching.",
                    "No Sketch Uploaded", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setButtonsEnabled(false);
        find_match.setText("SEARCHING...");
        match_properties.setText("Searching database, please wait...");
        match_similarity.setText("");
        match.setIcon(null);

        final String s3KeyToSearch = currentS3Key;

        SwingWorker<List<FaceMatch>, Void> worker = new SwingWorker<>() {

            @Override
            protected List<FaceMatch> doInBackground() throws Exception {
                AmazonRekognition rekClient = AmazonRekognitionClientBuilder.standard()
                        .withRegion(CLIENT_REGION).build();

                com.amazonaws.services.rekognition.model.Image queryImage =
                        new com.amazonaws.services.rekognition.model.Image()
                                .withS3Object(new S3Object()
                                        .withBucket(BUCKET_NAME)
                                        .withName(s3KeyToSearch));

                SearchFacesByImageRequest searchRequest = new SearchFacesByImageRequest()
                        .withCollectionId(COLLECTION_ID)
                        .withImage(queryImage)
                        .withFaceMatchThreshold(MIN_SIMILARITY)
                        .withMaxFaces(1); // Only return the single best match

                SearchFacesByImageResult result = rekClient.searchFacesByImage(searchRequest);

                // ── DEBUG: Print all matches to console ──────────────────────
                System.out.println("═══════════════════════════════════════");
                System.out.println("COLLECTION SEARCHED: " + COLLECTION_ID);
                System.out.println("QUERY IMAGE (sketch key): " + s3KeyToSearch);
                System.out.println("TOTAL MATCHES FOUND: " + result.getFaceMatches().size());
                result.getFaceMatches().forEach(face -> {
                    System.out.println("  ► Match: " + face.getFace().getExternalImageId()
                            + " | Similarity: "  + face.getSimilarity()
                            + "% | Confidence: " + face.getFace().getConfidence() + "%");
                });
                System.out.println("═══════════════════════════════════════");

                return result.getFaceMatches();
            }

            @Override
            protected void done() {
                setButtonsEnabled(true);
                find_match.setText("FIND MATCH");

                try {
                    List<FaceMatch> matches = get();

                    if (matches == null || matches.isEmpty()) {
                        clearMatchResults();
                        match_properties.setText(
                            "No match found in the database.\n\n" +
                            "POSSIBLE REASONS:\n" +
                            "• The suspect's photo is not in the collection.\n" +
                            "• The sketch quality is too low.\n" +
                            "• Try lowering MIN_SIMILARITY threshold (currently " + MIN_SIMILARITY + "%)."
                        );
                        match_similarity.setText("NO MATCH FOUND");
                        match_similarity.setForeground(Color.RED);
                        return;
                    }

                    // Reset color to green for a successful match
                    match_similarity.setForeground(new Color(0, 153, 0));

                    FaceMatch best          = matches.get(0);
                    String externalImageId  = best.getFace().getExternalImageId();
                    float  similarity       = best.getSimilarity();
                    float  confidence       = best.getFace().getConfidence();

                    // Update UI text
                    match_path.setText(externalImageId);
                    match_similarity.setText("SIMILARITY : " + String.format("%.2f", similarity) + "%");
                    match_properties.setText(
                            "********************************************\n" +
                            "FACE MATCHED\n"                                  +
                            "********************************************\n\n"+
                            "Name in database: " + externalImageId  + "\n\n" +
                            "Similarity:       " + String.format("%.4f", similarity)  + "%\n\n" +
                            "Confidence:       " + String.format("%.4f", confidence)  + "%\n"
                    );

                    // ── Load the matched suspect photo from S3 via Pre-signed URL ──
                    loadMatchedImage(externalImageId);

                } catch (Exception e) {
                    Logger.getLogger(face_rekognition.class.getName())
                          .log(Level.SEVERE, "Search failed", e);
                    match_properties.setText("Search failed.\n\nError: " + e.getMessage() +
                            "\n\nCheck that your AWS credentials are configured correctly.");
                    JOptionPane.showMessageDialog(null,
                            "Search error: " + e.getMessage(),
                            "Search Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Load matched suspect photo from S3 using a Pre-signed URL
    // Pre-signed URL bypasses S3 public access restrictions
    // ─────────────────────────────────────────────────────────────────────────
    private void loadMatchedImage(String externalImageId) {
        SwingWorker<ImageIcon, Void> imageWorker = new SwingWorker<>() {

            @Override
            protected ImageIcon doInBackground() throws Exception {
                AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                        .withRegion(CLIENT_REGION).build();

                // Generate a pre-signed URL valid for 1 hour
                Date expiration = new Date(System.currentTimeMillis() + 3_600_000L);

                GeneratePresignedUrlRequest presignedRequest =
                        new GeneratePresignedUrlRequest(BUCKET_NAME, externalImageId)
                                .withMethod(HttpMethod.GET)
                                .withExpiration(expiration);

                URL url = s3Client.generatePresignedUrl(presignedRequest);
                System.out.println("Loading matched image from pre-signed URL: " + url);

                // Load image from URL
                ImageIcon rawIcon = new ImageIcon(url);

                // MediaTracker: wait for full image load before returning
                MediaTracker tracker = new MediaTracker(match);
                tracker.addImage(rawIcon.getImage(), 0);
                try {
                    tracker.waitForAll();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }

                // Check if image loaded successfully
                if (tracker.isErrorAny()) {
                    throw new Exception("Image failed to load from S3. " +
                            "Check that '" + externalImageId + "' exists in bucket '" + BUCKET_NAME + "'.");
                }

                return rawIcon;
            }

            @Override
            protected void done() {
                try {
                    ImageIcon rawIcon = get();

                    // Scale to fit the match label — same treatment as the sketch
                    Image scaled = rawIcon.getImage()
                            .getScaledInstance(match.getWidth(), match.getHeight(), Image.SCALE_SMOOTH);
                    match.setIcon(new ImageIcon(scaled));

                } catch (Exception e) {
                    Logger.getLogger(face_rekognition.class.getName())
                          .log(Level.SEVERE, "Image load failed", e);
                    match.setIcon(null);

                    // Show the match details but warn about the missing image
                    match_properties.setText(match_properties.getText() +
                            "\n\n⚠ Could not load suspect photo:\n" + e.getMessage() +
                            "\n\nVerify that '" + externalImageId +
                            "' exists in the S3 bucket and was added to the collection correctly.");

                    JOptionPane.showMessageDialog(null,
                            "Match found but image could not be loaded.\n" + e.getMessage(),
                            "Image Load Error", JOptionPane.WARNING_MESSAGE);
                }
            }
        };

        imageWorker.execute();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper — Clear all match-related UI fields
    // ─────────────────────────────────────────────────────────────────────────
    private void clearMatchResults() {
        match_path.setText("");
        match.setIcon(null);
        match_similarity.setText("");
        match_properties.setText("");
        match_similarity.setForeground(new Color(0, 153, 0));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper — Enable or disable all action buttons at once
    // ─────────────────────────────────────────────────────────────────────────
    private void setButtonsEnabled(boolean enabled) {
        open_sketch.setEnabled(enabled);
        upload_sketch.setEnabled(enabled);
        find_match.setEnabled(enabled);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAIN
    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(face_rekognition.class.getName())
                  .log(Level.WARNING, "Look and feel error", ex);
        }

        java.awt.EventQueue.invokeLater(() -> new face_rekognition().setVisible(true));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Variables declaration
    // ─────────────────────────────────────────────────────────────────────────
    private javax.swing.JButton    find_match;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JLabel     match;
    private javax.swing.JTextField match_path;
    private javax.swing.JTextArea  match_properties;
    private javax.swing.JLabel     match_similarity;
    private javax.swing.JButton    open_sketch;
    private javax.swing.JLabel     sketch;
    private javax.swing.JTextField sketch_path;
    private javax.swing.JButton    upload_sketch;
}
