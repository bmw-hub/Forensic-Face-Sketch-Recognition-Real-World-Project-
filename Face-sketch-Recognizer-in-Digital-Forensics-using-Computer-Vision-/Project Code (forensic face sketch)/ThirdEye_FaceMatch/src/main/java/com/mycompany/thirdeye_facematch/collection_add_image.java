package com.mycompany.thirdeye_facematch;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.FaceRecord;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.IndexFacesRequest;
import com.amazonaws.services.rekognition.model.IndexFacesResult;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.rekognition.model.InvalidS3ObjectException;
import java.util.List;

/**
 * collection_add_image
 * This file adds the "Real" target photo to your AWS Collection.
 */
public class collection_add_image {
    
    // Ensure these three match your AWS Setup exactly
    public static final String collectionId = "MyFaceCollection";
    public static final String bucket = "thirdeye-facematch-himanshu";
    
    // IMPORTANT: This must be the EXACT name of the real photo in your S3 bucket (case-sensitive)
    public static final String photo = "a-sharukh-1.jpg"; 

    public static void main(String[] args) {

        AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();

        Image image = new Image()
                .withS3Object(new S3Object()
                .withBucket(bucket)
                .withName(photo));
        
        // We use the photo name as the ExternalImageId so we can identify who was matched later
        IndexFacesRequest indexFacesRequest = new IndexFacesRequest()
                .withImage(image)
                .withCollectionId(collectionId)
                .withExternalImageId(photo) 
                .withDetectionAttributes("ALL");

        try {
            System.out.println("Attempting to index face from: " + photo);
            IndexFacesResult indexFacesResult = rekognitionClient.indexFaces(indexFacesRequest);
            
            System.out.println("Results for " + photo);
            System.out.println("Faces indexed:");
            List<FaceRecord> faceRecords = indexFacesResult.getFaceRecords();
            
            if (faceRecords.isEmpty()) {
                System.out.println("WARNING: No faces were detected in the image. Try a clearer photo.");
            } else {
                for (FaceRecord faceRecord : faceRecords) {
                    System.out.println("  Face ID: " + faceRecord.getFace().getFaceId());
                    System.out.println("  Confidence: " + faceRecord.getFace().getConfidence());
                    System.out.println("  Location: " + faceRecord.getFaceDetail().getBoundingBox().toString());
                    System.out.println("------------------------------------------------------------");
                }
                System.out.println("SUCCESS: Person added to '" + collectionId + "'. You can now search for them using a sketch.");
            }
            
        } catch (InvalidS3ObjectException e) {
            System.err.println("ERROR: Could not find " + photo + " in bucket " + bucket);
            System.err.println("Check if the filename is correct and the case (JPG vs jpg) matches.");
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}