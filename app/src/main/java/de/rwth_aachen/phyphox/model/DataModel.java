package de.rwth_aachen.phyphox.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class DataModel implements Parcelable {

    private String photo;
    private ArrayList<String> photoDraw;
    private double latitude;
    private double longitude;
    private String locationName;
    private String idCustomer;
    private String customerName;
    private String typeQuestion;
    private String photoAcceleration;
    private String valueAcceleration;
    private String topics;
    private Boolean isFinished;
    private String id;
    private String question;
    private String base64;
    private String base64_2;
    private String base64_3;
    private String base64_4;
    private String photoAnswer; // Legacy Base64 field - deprecated, use photoAnswerUrl instead
    private String photoAnswerUrl; // Firebase Storage download URL for photo answer
    private String photoAnswerPath; // Firebase Storage path for photo answer
    private String dateTime;
    private String base64_5;
    private String typeData;
    private String desc;
    private int totalEdit;
    private int views;

    // New, clear fields for question images stored in Firebase Storage (URLs + paths)
    // Prefer these over legacy base64/base64_2/... when available.
    private String questionImageUrl1;
    private String questionImageUrl2;
    private String questionImageUrl3;
    private String questionImageUrl4;
    private String questionImageUrl5;
    private String questionImagePath1;
    private String questionImagePath2;
    private String questionImagePath3;
    private String questionImagePath4;
    private String questionImagePath5;
    
    // New fields for photo documentation and storage
    private ArrayList<String> photoDocumentation; // URLs to Firebase Storage
    private ArrayList<String> photoDocumentationPaths; // Storage paths
    private ArrayList<String> notePhotos; // Note photo URLs
    private ArrayList<String> notePhotoPaths; // Note photo storage paths
    private String experimentPhotoUrl; // Main experiment photo URL
    private String experimentPhotoPath; // Main experiment photo storage path
    private String documentationNotes; // Text notes for documentation
    private String storageUserId; // User ID for storage organization
    private long lastPhotoUpdate; // Timestamp of last photo update
    
    // Photo upload fields
    private String localPhotoPath; // Local file path for photo to be uploaded
    private String localPhotoBase64; // Base64 data for photo to be uploaded
    
    // Admin tracking fields
    private String creatorName; // Name of the person who created the question
    private String dibuatOleh; // Indonesian: "Dibuat Oleh" (Created By)
    private String sourceQuestionId; // ID of the source question if this is a copy
    private String createdBy; // Alternative field for creator tracking
    
    // Versioning and history tracking fields
    private int version; // Version number for tracking changes
    private String status; // active, archived, deleted
    private long createdAt; // Timestamp saat pertama dibuat
    private long updatedAt; // Timestamp saat terakhir di-update
    private String createdAtFormatted; // Formatted date string
    private String updatedAtFormatted; // Formatted date string
    private ArrayList<java.util.Map<String, Object>> history; // History array stored in same document (max 10 entries)

    public DataModel(){
        this.photo = "";
        this.photoDraw = new ArrayList<>();
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.locationName = "";
        this.idCustomer = "";
        this.customerName = "";
        this.typeQuestion = "";
        this.photoAcceleration = "";
        this.valueAcceleration = "";
        this.topics = "";
        this.id = "";
        this.question = "";
        this.base64 = "";
        this.base64_2 = "";
        this.base64_3 = "";
        this.base64_4 = "";
        this.photoAnswer = "";
        this.photoAnswerUrl = "";
        this.photoAnswerPath = "";
        this.desc = "";
        this.isFinished = false;
        this.totalEdit = 0;
        this.views = 0;

        // Initialize question image URL/path fields
        this.questionImageUrl1 = "";
        this.questionImageUrl2 = "";
        this.questionImageUrl3 = "";
        this.questionImageUrl4 = "";
        this.questionImageUrl5 = "";
        this.questionImagePath1 = "";
        this.questionImagePath2 = "";
        this.questionImagePath3 = "";
        this.questionImagePath4 = "";
        this.questionImagePath5 = "";
        
        // Initialize new fields
        this.photoDocumentation = new ArrayList<>();
        this.photoDocumentationPaths = new ArrayList<>();
        this.notePhotos = new ArrayList<>();
        this.notePhotoPaths = new ArrayList<>();
        this.experimentPhotoUrl = "";
        this.experimentPhotoPath = "";
        this.documentationNotes = "";
        this.storageUserId = "";
        this.lastPhotoUpdate = System.currentTimeMillis();
        
        // Initialize photo upload fields
        this.localPhotoPath = "";
        this.localPhotoBase64 = "";
        
        // Initialize admin tracking fields
        this.creatorName = "";
        this.dibuatOleh = "";
        this.sourceQuestionId = "";
        this.createdBy = "";
        
        // Initialize versioning fields
        this.version = 1;
        this.status = "active";
        long currentTime = System.currentTimeMillis();
        this.createdAt = currentTime;
        this.updatedAt = currentTime;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formatted = formatter.format(new Date(currentTime));
        this.createdAtFormatted = formatted;
        this.updatedAtFormatted = formatted;
        this.history = new ArrayList<>(); // Initialize empty history array

        Date now = Calendar.getInstance().getTime();

        // Format waktu (legacy field)
        this.dateTime = formatted;
    }

    public DataModel(String photo, ArrayList<String> photoDraw, double latitude, double longitude, String locationName) {
        this.photo = photo;
        this.photoDraw = photoDraw;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationName = locationName;
    }

    protected DataModel(Parcel in) {
        photo = in.readString();
        photoDraw = in.createStringArrayList(); // read ArrayList<String>
        latitude = in.readDouble();
        longitude = in.readDouble();
        locationName = in.readString();
        idCustomer = in.readString();
        typeQuestion = in.readString();
        photoAcceleration = in.readString();
        valueAcceleration = in.readString();
        topics = in.readString();
        byte tmpIsFinished = in.readByte();
        isFinished = tmpIsFinished == 0 ? null : tmpIsFinished == 1;
        id = in.readString();
        question = in.readString();
        base64 = in.readString();
        base64_2 = in.readString();
        base64_3 = in.readString();
        base64_4 = in.readString();
        photoAnswer = in.readString();
        photoAnswerUrl = in.readString();
        photoAnswerPath = in.readString();
        totalEdit = in.readInt();
        
        // Read new fields
        dateTime = in.readString();
        base64_5 = in.readString();
        typeData = in.readString();
        desc = in.readString();
        views = in.readInt();

        // Read question image URL/path fields
        questionImageUrl1 = in.readString();
        questionImageUrl2 = in.readString();
        questionImageUrl3 = in.readString();
        questionImageUrl4 = in.readString();
        questionImageUrl5 = in.readString();
        questionImagePath1 = in.readString();
        questionImagePath2 = in.readString();
        questionImagePath3 = in.readString();
        questionImagePath4 = in.readString();
        questionImagePath5 = in.readString();
        
        // Read photo documentation fields
        photoDocumentation = in.createStringArrayList();
        photoDocumentationPaths = in.createStringArrayList();
        notePhotos = in.createStringArrayList();
        notePhotoPaths = in.createStringArrayList();
        experimentPhotoUrl = in.readString();
        experimentPhotoPath = in.readString();
        documentationNotes = in.readString();
        storageUserId = in.readString();
        lastPhotoUpdate = in.readLong();
        
        // Read photo upload fields
        localPhotoPath = in.readString();
        localPhotoBase64 = in.readString();
        
        // Read admin tracking fields
        creatorName = in.readString();
        dibuatOleh = in.readString();
        sourceQuestionId = in.readString();
        createdBy = in.readString();
        
        // Read versioning fields
        version = in.readInt();
        status = in.readString();
        createdAt = in.readLong();
        updatedAt = in.readLong();
        createdAtFormatted = in.readString();
        updatedAtFormatted = in.readString();
        // History array - initialize empty (will be loaded from Firestore)
        history = new ArrayList<>();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(photo);
        dest.writeStringList(photoDraw); // write ArrayList<String>
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(locationName);
        dest.writeString(idCustomer);
        dest.writeString(typeQuestion);
        dest.writeString(photoAcceleration);
        dest.writeString(valueAcceleration);
        dest.writeString(topics);
        dest.writeByte((byte) (isFinished == null ? 0 : isFinished ? 1 : 2));
        dest.writeString(id);
        dest.writeString(question);
        dest.writeString(base64);
        dest.writeString(base64_2);
        dest.writeString(base64_3);
        dest.writeString(base64_4);
        dest.writeString(photoAnswer);
        dest.writeString(photoAnswerUrl);
        dest.writeString(photoAnswerPath);
        dest.writeInt(totalEdit);
        
        // Write new fields
        dest.writeString(dateTime);
        dest.writeString(base64_5);
        dest.writeString(typeData);
        dest.writeString(desc);
        dest.writeInt(views);

        // Write question image URL/path fields
        dest.writeString(questionImageUrl1);
        dest.writeString(questionImageUrl2);
        dest.writeString(questionImageUrl3);
        dest.writeString(questionImageUrl4);
        dest.writeString(questionImageUrl5);
        dest.writeString(questionImagePath1);
        dest.writeString(questionImagePath2);
        dest.writeString(questionImagePath3);
        dest.writeString(questionImagePath4);
        dest.writeString(questionImagePath5);
        
        // Write photo documentation fields
        dest.writeStringList(photoDocumentation);
        dest.writeStringList(photoDocumentationPaths);
        dest.writeStringList(notePhotos);
        dest.writeStringList(notePhotoPaths);
        dest.writeString(experimentPhotoUrl);
        dest.writeString(experimentPhotoPath);
        dest.writeString(documentationNotes);
        dest.writeString(storageUserId);
        dest.writeLong(lastPhotoUpdate);
        
        // Write photo upload fields
        dest.writeString(localPhotoPath);
        dest.writeString(localPhotoBase64);
        
        // Write admin tracking fields
        dest.writeString(creatorName);
        dest.writeString(dibuatOleh);
        dest.writeString(sourceQuestionId);
        dest.writeString(createdBy);
        
        // Write versioning fields
        dest.writeInt(version);
        dest.writeString(status);
        dest.writeLong(createdAt);
        dest.writeLong(updatedAt);
        dest.writeString(createdAtFormatted);
        dest.writeString(updatedAtFormatted);
        // History array - not serialized in Parcel (loaded from Firestore)
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DataModel> CREATOR = new Creator<DataModel>() {
        @Override
        public DataModel createFromParcel(Parcel in) {
            return new DataModel(in);
        }

        @Override
        public DataModel[] newArray(int size) {
            return new DataModel[size];
        }
    };

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public ArrayList<String> getPhotoDraw() {
        return photoDraw;
    }

    public void setPhotoDraw(ArrayList<String> photoDraw) {
        this.photoDraw = photoDraw;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getPhoto() {
        return photo;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getLocationName() {
        return locationName;
    }

    public String getIdCustomer() {
        return idCustomer;
    }

    public void setIdCustomer(String idCustomer) {
        this.idCustomer = idCustomer;
    }

    public String getTypeQuestion() {
        return typeQuestion;
    }

    public void setTypeQuestion(String typeQuestion) {
        this.typeQuestion = typeQuestion;
    }

    public String getPhotoAcceleration() {
        return photoAcceleration;
    }

    public void setPhotoAcceleration(String photoAcceleration) {
        this.photoAcceleration = photoAcceleration;
    }

    public String getValueAcceleration() {
        return valueAcceleration;
    }

    public void setValueAcceleration(String valueAcceleration) {
        this.valueAcceleration = valueAcceleration;
    }

    public String getTopics() {
        return topics;
    }

    public void setTopics(String topics) {
        this.topics = topics;
    }

    public Boolean getFinished() {
        return isFinished;
    }

    public void setFinished(Boolean finished) {
        isFinished = finished;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getBase64() {
        return base64;
    }

    public void setBase64(String base64) {
        this.base64 = base64;
    }

    // --- New getters/setters for question image URLs/paths ---
    public String getQuestionImageUrl1() { return questionImageUrl1; }
    public void setQuestionImageUrl1(String v) { this.questionImageUrl1 = v; }
    public String getQuestionImageUrl2() { return questionImageUrl2; }
    public void setQuestionImageUrl2(String v) { this.questionImageUrl2 = v; }
    public String getQuestionImageUrl3() { return questionImageUrl3; }
    public void setQuestionImageUrl3(String v) { this.questionImageUrl3 = v; }
    public String getQuestionImageUrl4() { return questionImageUrl4; }
    public void setQuestionImageUrl4(String v) { this.questionImageUrl4 = v; }
    public String getQuestionImageUrl5() { return questionImageUrl5; }
    public void setQuestionImageUrl5(String v) { this.questionImageUrl5 = v; }

    public String getQuestionImagePath1() { return questionImagePath1; }
    public void setQuestionImagePath1(String v) { this.questionImagePath1 = v; }
    public String getQuestionImagePath2() { return questionImagePath2; }
    public void setQuestionImagePath2(String v) { this.questionImagePath2 = v; }
    public String getQuestionImagePath3() { return questionImagePath3; }
    public void setQuestionImagePath3(String v) { this.questionImagePath3 = v; }
    public String getQuestionImagePath4() { return questionImagePath4; }
    public void setQuestionImagePath4(String v) { this.questionImagePath4 = v; }
    public String getQuestionImagePath5() { return questionImagePath5; }
    public void setQuestionImagePath5(String v) { this.questionImagePath5 = v; }
    public String getPhotoAnswer() {
        return photoAnswer;
    }

    public void setPhotoAnswer(String photoAnswer) {
        this.photoAnswer = photoAnswer;
    }
    
    public String getPhotoAnswerUrl() {
        return photoAnswerUrl;
    }
    
    public void setPhotoAnswerUrl(String photoAnswerUrl) {
        this.photoAnswerUrl = photoAnswerUrl;
    }
    
    public String getPhotoAnswerPath() {
        return photoAnswerPath;
    }
    
    public void setPhotoAnswerPath(String photoAnswerPath) {
        this.photoAnswerPath = photoAnswerPath;
    }
    public String getBase64_2() {
        return base64_2;
    }

    public void setBase64_2(String base64) {
        this.base64_2 = base64;
    }
    public void setBase64_3(String base64) {
        this.base64_3 = base64;
    }
    public String getBase64_3() {
        return base64_3;
    }
    public void setBase64_4(String base64) {
        this.base64_4 = base64;
    }
    public String getBase64_4() {
        return base64_4;
    }
    public String getBase64_5() {
        return base64_5;
    }
    public void setBase64_5(String base64_5) {
        this.base64_5 = base64_5;
    }

    public String getCustomerName() {
        if(customerName==null ||customerName.isEmpty()){
            return "-";
        }
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }



    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getTypeData() {
        return typeData;
    }

    public void setTypeData(String typeData) {
        this.typeData = typeData;
    }

    public int getTotalEdit() {
        return totalEdit;
    }

    public void setTotalEdit(int totalEdit) {
        this.totalEdit = totalEdit;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public int getViews() {
        return views;
    }
    
    public void setViews(int views) {
        this.views = views;
    }
    
    // New getters and setters for photo documentation
    public ArrayList<String> getPhotoDocumentation() {
        return photoDocumentation;
    }
    
    public void setPhotoDocumentation(ArrayList<String> photoDocumentation) {
        this.photoDocumentation = photoDocumentation;
    }
    
    public ArrayList<String> getPhotoDocumentationPaths() {
        return photoDocumentationPaths;
    }
    
    public void setPhotoDocumentationPaths(ArrayList<String> photoDocumentationPaths) {
        this.photoDocumentationPaths = photoDocumentationPaths;
    }
    
    public ArrayList<String> getNotePhotos() {
        return notePhotos;
    }
    
    public void setNotePhotos(ArrayList<String> notePhotos) {
        this.notePhotos = notePhotos;
    }
    
    public ArrayList<String> getNotePhotoPaths() {
        return notePhotoPaths;
    }
    
    public void setNotePhotoPaths(ArrayList<String> notePhotoPaths) {
        this.notePhotoPaths = notePhotoPaths;
    }
    
    public String getExperimentPhotoUrl() {
        return experimentPhotoUrl;
    }
    
    public void setExperimentPhotoUrl(String experimentPhotoUrl) {
        this.experimentPhotoUrl = experimentPhotoUrl;
    }
    
    public String getExperimentPhotoPath() {
        return experimentPhotoPath;
    }
    
    public void setExperimentPhotoPath(String experimentPhotoPath) {
        this.experimentPhotoPath = experimentPhotoPath;
    }
    
    public String getDocumentationNotes() {
        return documentationNotes;
    }
    
    public void setDocumentationNotes(String documentationNotes) {
        this.documentationNotes = documentationNotes;
    }
    
    public String getStorageUserId() {
        return storageUserId;
    }
    
    public void setStorageUserId(String storageUserId) {
        this.storageUserId = storageUserId;
    }
    
    public long getLastPhotoUpdate() {
        return lastPhotoUpdate;
    }
    
    public void setLastPhotoUpdate(long lastPhotoUpdate) {
        this.lastPhotoUpdate = lastPhotoUpdate;
    }
    
    // Photo upload getters and setters
    public String getLocalPhotoPath() {
        return localPhotoPath;
    }

    public void setLocalPhotoPath(String localPhotoPath) {
        this.localPhotoPath = localPhotoPath;
    }

    public String getLocalPhotoBase64() {
        return localPhotoBase64;
    }

    public void setLocalPhotoBase64(String localPhotoBase64) {
        this.localPhotoBase64 = localPhotoBase64;
    }
    
    // Admin tracking getters and setters
    public String getCreatorName() {
        return creatorName;
    }
    
    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }
    
    public String getDibuatOleh() {
        return dibuatOleh;
    }
    
    public void setDibuatOleh(String dibuatOleh) {
        this.dibuatOleh = dibuatOleh;
    }
    
    public String getSourceQuestionId() {
        return sourceQuestionId;
    }
    
    public void setSourceQuestionId(String sourceQuestionId) {
        this.sourceQuestionId = sourceQuestionId;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    /**
     * Add photo documentation URL and path
     */
    public void addPhotoDocumentation(String photoUrl, String storagePath) {
        if (this.photoDocumentation == null) {
            this.photoDocumentation = new ArrayList<>();
        }
        if (this.photoDocumentationPaths == null) {
            this.photoDocumentationPaths = new ArrayList<>();
        }
        
        this.photoDocumentation.add(photoUrl);
        this.photoDocumentationPaths.add(storagePath);
        this.lastPhotoUpdate = System.currentTimeMillis();
    }
    
    /**
     * Add note photo URL and path
     */
    public void addNotePhoto(String photoUrl, String storagePath) {
        if (this.notePhotos == null) {
            this.notePhotos = new ArrayList<>();
        }
        if (this.notePhotoPaths == null) {
            this.notePhotoPaths = new ArrayList<>();
        }
        
        this.notePhotos.add(photoUrl);
        this.notePhotoPaths.add(storagePath);
        this.lastPhotoUpdate = System.currentTimeMillis();
    }
    
    /**
     * Remove photo documentation by index
     */
    public void removePhotoDocumentation(int index) {
        if (this.photoDocumentation != null && index >= 0 && index < this.photoDocumentation.size()) {
            this.photoDocumentation.remove(index);
            if (this.photoDocumentationPaths != null && index < this.photoDocumentationPaths.size()) {
                this.photoDocumentationPaths.remove(index);
            }
            this.lastPhotoUpdate = System.currentTimeMillis();
        }
    }
    
    /**
     * Remove note photo by index
     */
    public void removeNotePhoto(int index) {
        if (this.notePhotos != null && index >= 0 && index < this.notePhotos.size()) {
            this.notePhotos.remove(index);
            if (this.notePhotoPaths != null && index < this.notePhotoPaths.size()) {
                this.notePhotoPaths.remove(index);
            }
            this.lastPhotoUpdate = System.currentTimeMillis();
        }
    }
    
    /**
     * Get total photo count (documentation + notes + experiment)
     */
    public int getTotalPhotoCount() {
        int count = 0;
        if (this.photoDocumentation != null) count += this.photoDocumentation.size();
        if (this.notePhotos != null) count += this.notePhotos.size();
        if (this.experimentPhotoUrl != null && !this.experimentPhotoUrl.isEmpty()) count++;
        return count;
    }
    
    // Versioning getters and setters
    public int getVersion() {
        return version;
    }
    
    public void setVersion(int version) {
        this.version = version;
    }
    
    public String getStatus() {
        return status != null ? status : "active";
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public long getCreatedAt() {
        return createdAt > 0 ? createdAt : System.currentTimeMillis();
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getUpdatedAt() {
        return updatedAt > 0 ? updatedAt : System.currentTimeMillis();
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getCreatedAtFormatted() {
        if (createdAtFormatted == null || createdAtFormatted.isEmpty()) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            createdAtFormatted = formatter.format(new Date(getCreatedAt()));
        }
        return createdAtFormatted;
    }
    
    public void setCreatedAtFormatted(String createdAtFormatted) {
        this.createdAtFormatted = createdAtFormatted;
    }
    
    public String getUpdatedAtFormatted() {
        if (updatedAtFormatted == null || updatedAtFormatted.isEmpty()) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            updatedAtFormatted = formatter.format(new Date(getUpdatedAt()));
        }
        return updatedAtFormatted;
    }
    
    public void setUpdatedAtFormatted(String updatedAtFormatted) {
        this.updatedAtFormatted = updatedAtFormatted;
    }
    
    public ArrayList<java.util.Map<String, Object>> getHistory() {
        if (history == null) {
            history = new ArrayList<>();
        }
        return history;
    }
    
    public void setHistory(ArrayList<java.util.Map<String, Object>> history) {
        this.history = history;
    }
    
    /**
     * Add history entry to history array (max 10 entries to keep document size manageable)
     */
    public void addHistoryEntry(String changeType, String userId, String userName, java.util.Map<String, Object> changedFields) {
        if (history == null) {
            history = new ArrayList<>();
        }
        
        // Limit to max 10 history entries to prevent document from getting too large
        if (history.size() >= 10) {
            history.remove(0); // Remove oldest entry
        }
        
        long currentTime = System.currentTimeMillis();
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
        
        java.util.Map<String, Object> historyEntry = new java.util.HashMap<>();
        historyEntry.put("version", version);
        historyEntry.put("changeType", changeType);
        historyEntry.put("timestamp", currentTime);
        historyEntry.put("timestampFormatted", formatter.format(new java.util.Date(currentTime)));
        historyEntry.put("userId", userId != null ? userId : "");
        historyEntry.put("userName", userName != null ? userName : "");
        historyEntry.put("changedFields", changedFields);
        
        history.add(historyEntry);
    }
    
    /**
     * Increment version and update timestamp
     */
    public void incrementVersion() {
        this.version++;
        this.updatedAt = System.currentTimeMillis();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.updatedAtFormatted = formatter.format(new Date(this.updatedAt));
    }
    
    /**
     * Initialize versioning fields for new record
     */
    public void initializeVersioning(String userId, String userName) {
        long currentTime = System.currentTimeMillis();
        this.version = 1;
        this.status = "active";
        this.createdAt = currentTime;
        this.updatedAt = currentTime;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formatted = formatter.format(new Date(currentTime));
        this.createdAtFormatted = formatted;
        this.updatedAtFormatted = formatted;
        if (this.history == null) {
            this.history = new ArrayList<>();
        }
        if (userId != null && !userId.isEmpty()) {
            this.idCustomer = userId;
        }
        if (userName != null && !userName.isEmpty()) {
            this.customerName = userName;
            this.creatorName = userName;
            this.dibuatOleh = userName;
            this.createdBy = userName;
        }
    }
}

