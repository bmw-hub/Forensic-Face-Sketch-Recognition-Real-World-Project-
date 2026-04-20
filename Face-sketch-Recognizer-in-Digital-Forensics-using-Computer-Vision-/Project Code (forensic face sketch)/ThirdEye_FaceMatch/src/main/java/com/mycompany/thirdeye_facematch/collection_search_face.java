package com.mycompany.thirdeye_facematch;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.*;
import java.util.List;

public class collection_search_face {
    public static final String collectionId = "MyFaceCollection";
    public static final String bucket = "thirdeye-facematch-himanshu";
    public static final String photo = "sketch_upload.jpg"; // Matches the GUI upload key
      
    public static void main(String[] args) throws Exception {
        AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();
        
        Image image = new Image()
              .withS3Object(new S3Object()
                      .withBucket(bucket)
                      .withName(photo));
      
        SearchFacesByImageRequest request = new SearchFacesByImageRequest()
              .withCollectionId(collectionId)
              .withImage(image)
              .withFaceMatchThreshold(30F) // Lowered for forensic sketches
              .withMaxFaces(2);
      
        try {
            SearchFacesByImageResult result = rekognitionClient.searchFacesByImage(request);
            List<FaceMatch> faceImageMatches = result.getFaceMatches();
            
            if(faceImageMatches.isEmpty()){
                System.out.println("NO MATCH FOUND");
            } else {
                for (FaceMatch face: faceImageMatches) {
                   System.out.println("Match Found!");
                   System.out.println("External Image ID: " + face.getFace().getExternalImageId());
                   System.out.println("Similarity: " + face.getSimilarity() + "%");
                }
            }
        } catch (InvalidS3ObjectException e) {
            System.out.println("ERROR: The file '" + photo + "' was not found in bucket '" + bucket + "'. Please upload it via the GUI first!");
        }
    }
}