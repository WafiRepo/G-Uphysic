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
    private String photoAnswer;
    private String dateTime;
    private String base64_5;
    private String typeData;
    private String desc;
    private int totalEdit;
    private int views;

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
        this.desc = "";
        this.isFinished = false;
        this.totalEdit = 0;
        this.views = 0;
        Date now = Calendar.getInstance().getTime();

        // Format waktu
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formatted = formatter.format(now);
        this.dateTime= formatted;
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
        totalEdit = in.readInt();
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
        dest.writeInt(totalEdit);
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
    public String getPhotoAnswer() {
        return photoAnswer;
    }

    public void setPhotoAnswer(String photoAnswer) {
        this.photoAnswer = photoAnswer;
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
}

