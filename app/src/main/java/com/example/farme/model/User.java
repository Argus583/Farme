package com.example.farme.model;

public class User {
    private String  uid;
    private String  name;
    private String  phone;
    private String  email;
    private String  region;
    private String  role;
    private double  rating;
    private int     reviewCount;
    private int     listingCount;
    private String  avatar;
    private boolean banned;
    private boolean verified;
    private long    createdAt;

    public User() {}

    public String  getUid()          { return uid; }
    public String  getName()         { return name; }
    public String  getPhone()        { return phone; }
    public String  getEmail()        { return email; }
    public String  getRegion()       { return region; }
    public String  getRole()         { return role; }
    public double  getRating()       { return rating; }
    public int     getReviewCount()  { return reviewCount; }
    public int     getListingCount() { return listingCount; }
    public String  getAvatar()       { return avatar; }
    public boolean isBanned()        { return banned; }
    public boolean isVerified()      { return verified; }
    public long    getCreatedAt()    { return createdAt; }

    public void setUid(String uid)          { this.uid = uid; }
    public void setName(String name)        { this.name = name; }
    public void setPhone(String phone)      { this.phone = phone; }
    public void setEmail(String email)      { this.email = email; }
    public void setRegion(String region)    { this.region = region; }
    public void setRole(String role)        { this.role = role; }
    public void setRating(double rating)    { this.rating = rating; }
    public void setReviewCount(int c)       { this.reviewCount = c; }
    public void setListingCount(int c)      { this.listingCount = c; }
    public void setAvatar(String avatar)    { this.avatar = avatar; }
    public void setBanned(boolean banned)   { this.banned = banned; }
    public void setVerified(boolean v)      { this.verified = v; }
    public void setCreatedAt(long time)     { this.createdAt = time; }

    public boolean isAdmin() {
        return "admin".equals(role);
    }
    public String getInitials() {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2)
            return String.valueOf(parts[0].charAt(0)).toUpperCase()
                    + String.valueOf(parts[1].charAt(0)).toUpperCase();
        return String.valueOf(name.charAt(0)).toUpperCase();
    }
}