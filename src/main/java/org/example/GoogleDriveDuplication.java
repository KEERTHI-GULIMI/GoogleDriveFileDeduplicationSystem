package org.example;


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;

public class GoogleDriveDuplication {
    private static final String APPLICATION_NAME = "Google Drive File Deduplication System";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
    private static final String CREDENTIALS_FILE_PATH = "resources/credentials.json";

    public static void main(String[] args) {
        try {
            Drive service = getDriveService();
            Scanner scanner = new Scanner(System.in);

            System.out.println("Enter the folder Id you wanted to check for duplicates");

            String folderId = scanner.nextLine();             //"1BtVWBMXdh1RIOttFRz-UkXfuaT5lHCio";

            System.out.println(" Do you want to (1) Delete duplicates or (2) Move them to a backup folder?");
            System.out.print("Enter 1 for Delete or 2 for Backup: ");

            int choice = scanner.nextInt();


            if (choice == 1) {
                System.out.println("Deleting duplicates...");
                findAndRemoveDuplicates(service, folderId);
            } else if (choice == 2) {
                System.out.println("Enter the folder Id that you wanted your backup to happen");
                String backupFolderId = scanner.next();

                System.out.println("Moving duplicates to backup folder...");
                findAndBackupDuplicates(service, folderId,backupFolderId);
            } else {
                System.out.println("Invalid choice!");
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Authenticate and get Google Drive service
    private static Drive getDriveService() throws Exception {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = getCredentials(httpTransport);
        return new Drive.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private static Credential getCredentials(HttpTransport httpTransport) throws Exception {
        InputStream in = new FileInputStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    //Find duplicates and move them
    private static void findAndRemoveDuplicates(Drive service, String folderId) throws Exception {
        HashMap<String, String> hashToFileIdMap = new HashMap<>();
        List<File> files = listFilesInFolder(service, folderId);

        for (File file : files) {
            String fileId = file.getId();
            String fileName = file.getName();

            // Download file
            java.io.File tempFile = downloadFile(service, fileId, fileName);

            if (tempFile != null) {
                // Generate hash
                String fileHash = getSHA256(tempFile);

                // Check for duplicates
                if (hashToFileIdMap.containsKey(fileHash)) {
                    System.out.println("Duplicate found: " + fileName);

                    service.files().delete(fileId).execute(); // Delete from Google Drive
                    System.out.println("Deleted from Google Drive: " + fileName);
                } else {
                    hashToFileIdMap.put(fileHash, fileId);
                }
                tempFile.delete(); // Delete the temp file after processing
            }
        }
    }

    private static void findAndBackupDuplicates(Drive service, String folderId,String backupFolderId) throws Exception {
        System.out.println(" Connecting to Google Drive...");
        List<File> files = listFilesInFolder(service, folderId);

        System.out.println(" Found " + files.size() + " files in folder: " + folderId);

        if (files.isEmpty()) {
            System.out.println(" No files found in the folder. Check if the folder ID is correct!");
            return;
        }

        HashMap<String, String> hashToFileIdMap = new HashMap<>();

        for (File file : files) {
            String fileId = file.getId();
            String fileName = file.getName();
            System.out.println("Processing file: " + fileName);

            java.io.File tempFile = downloadFile(service, fileId, fileName);

            if (tempFile != null) {
                String fileHash = getSHA256(tempFile);

                if (hashToFileIdMap.containsKey(fileHash)) {
                    System.out.println("Duplicate found: " + fileName);

                    // Move duplicate file to backup folder
                    service.files().update(fileId, null)
                            .setAddParents(backupFolderId) //  Move to backup folder
                            .setRemoveParents(folderId) //  Remove from original folder
                            .execute();

                    System.out.println(" Moved to backup folder: " + fileName);

                } else {
                    hashToFileIdMap.put(fileHash, fileId);
                }

                tempFile.delete();
            }
        }
    }


    // List files in a specific Google Drive folder
    private static List<File> listFilesInFolder(Drive service, String folderId) throws Exception {
        List<File> fileList = new ArrayList<>();
        String query = "'" + folderId + "' in parents and mimeType != 'application/vnd.google-apps.folder'";
        FileList result = service.files().list().setQ(query).setFields("files(id, name)").execute();

        fileList.addAll(result.getFiles());
        return fileList;
    }

    // Download file from Google Drive
    private static java.io.File downloadFile(Drive service, String fileId, String fileName) {
        try {
            java.io.File tempFile = new java.io.File("downloads/" + fileName);
            tempFile.getParentFile().mkdirs(); // Create downloads directory if not exists
            OutputStream outputStream = new FileOutputStream(tempFile);
            service.files().get(fileId).executeMediaAndDownloadTo(outputStream);
            outputStream.close();

            return tempFile;
        } catch (IOException e) {
            System.out.println("Error downloading file: " + fileName);
            e.printStackTrace();
            return null;
        }
    }

    // Generate SHA-256 hash of a file
    private static String getSHA256(java.io.File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] byteArray = new byte[1024];
            int bytesCount;

            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }

            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IOException("Error generating hash", e);
        }
    }
}


