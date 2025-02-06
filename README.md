## Google Drive File Deduplication System  

### Overview  
The **Google Drive File Deduplication System** is a Java-based tool designed to scan a Google Drive folder, detect duplicate files based on their content (SHA-256 hashing), and either delete or move them to a backup folder. This ensures efficient storage management by eliminating redundant files.  

### Features  
- **Content-Based Deduplication**: Uses SHA-256 hashing to detect duplicates based on file content rather than just filenames.  
- **Google Drive Integration**: Interacts with Google Drive API to list, download, delete, and move files.  
- **User-Controlled Actions**: Allows users to either delete duplicates or move them to a backup folder.  
- **Secure Authentication**: Utilizes OAuth 2.0 for secure access to Google Drive.  
- **Error Handling**: Manages file operations with appropriate logging and error messages.  

### Prerequisites  
Before using the system, ensure you have:  
- **Java 21 or later** installed  
- **Maven** for dependency management  
- A **Google Cloud Project** with Google Drive API enabled  
- A **OAuth 2.0 credentials.json** file (downloaded from the Google Cloud Console)  
- An active **Google Drive account**  

### Installation  
1. Clone this repository:  
   ```sh
   git clone https://github.com/your-repo/google-drive-deduplication.git
   cd google-drive-deduplication
   ```  
2. Install dependencies using Maven:  
   ```sh
   mvn clean install
   ```  
3. Place your `credentials.json` file inside the `resources/` directory.  

### Setup  
1. Run the program:  
   ```sh
   java -jar target/google-drive-deduplication.jar
   ```  
2. Authenticate with Google when prompted.  
3. Enter the **Google Drive folder ID** to scan for duplicates.  
4. Choose an action:  
   - **Delete duplicates**  
   - **Move duplicates to a backup folder**  

### Usage  
- **Deleting Duplicates**: The program scans the selected folder, identifies duplicate files, and removes them.  
- **Moving to Backup Folder**: Duplicates are moved to a specified backup folder instead of being deleted.  

### Conclusion  
This tool helps in efficient Google Drive management by eliminating redundant files, freeing up space, and maintaining a well-organized storage structure. Contributions and improvements are welcome! 🚀

