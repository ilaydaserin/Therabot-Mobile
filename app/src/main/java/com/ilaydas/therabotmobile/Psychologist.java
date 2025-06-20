package com.ilaydas.therabotmobile;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class Psychologist {
    private String id; // Firestore doküman ID'si (burada e-posta olacak)
    private String name;
    private String specialty;
    private String shortBio;
    private String fullBio;
    private String contactInfo; // Telefon veya genel iletişim bilgisi
    private String workingHours;
    private List<String> expertiseAreas;
    private String profileImageUrl;
    private boolean isVerified; // Yöneticinin onay durumu
    private Date registrationDate;
    private Map<String, String> verificationDocuments; // (diplomaUrl, identityCardUrl içerecek)

    public Psychologist() {
        // Firestore için boş yapıcı metod gerekli
    }

    public Psychologist(String id, String name, String specialty, String shortBio, String fullBio,
                        String contactInfo, String workingHours, List<String> expertiseAreas,
                        String profileImageUrl, boolean isVerified, Date registrationDate,
                        Map<String, String> verificationDocuments) {
        this.id = id;
        this.name = name;
        this.specialty = specialty;
        this.shortBio = shortBio;
        this.fullBio = fullBio;
        this.contactInfo = contactInfo;
        this.workingHours = workingHours;
        this.expertiseAreas = expertiseAreas;
        this.profileImageUrl = profileImageUrl;
        this.isVerified = isVerified;
        this.registrationDate = registrationDate;
        this.verificationDocuments = verificationDocuments;
    }

    // Getter ve Setter Metotları

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getShortBio() {
        return shortBio;
    }

    public void setShortBio(String shortBio) {
        this.shortBio = shortBio;
    }

    public String getFullBio() {
        return fullBio;
    }

    public void setFullBio(String fullBio) {
        this.fullBio = fullBio;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public String getWorkingHours() {
        return workingHours;
    }

    public void setWorkingHours(String workingHours) {
        this.workingHours = workingHours;
    }

    public List<String> getExpertiseAreas() {
        return expertiseAreas;
    }

    public void setExpertiseAreas(List<String> expertiseAreas) {
        this.expertiseAreas = expertiseAreas;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public Date getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(Date registrationDate) {
        this.registrationDate = registrationDate;
    }

    public Map<String, String> getVerificationDocuments() {
        return verificationDocuments;
    }

    public void setVerificationDocuments(Map<String, String> verificationDocuments) {
        this.verificationDocuments = verificationDocuments;
    }
}