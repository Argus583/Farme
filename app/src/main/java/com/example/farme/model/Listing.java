package com.example.farme.model;

import java.util.List;

public class Listing {
    private String  id;
    private String  uid;
    private String  category;
    private String  title;
    private String  description;
    private double  price;
    private boolean negotiable;
    private String  region;
    private double  latitude;
    private double  longitude;
    private List<String> photos;
    private boolean active;
    private Boolean pending;
    private boolean rejected;
    private boolean sold;
    private String  rejectReason;
    private long    createdAt;
    private String  sellerName;
    private String  sellerPhone;
    private String  sellerAvatar;
    private double  sellerRating;
    private Passport passport;

    // ── Passport (цифровой паспорт животного) ──────────
    public static class Passport {
        private String  species;    // Вид: Корова, Овца...
        private String  breed;      // Порода
        private int     age;        // Возраст (лет)
        private String  sex;        // Самец/Самка
        private int     count;      // Количество голов
        private double  weight;     // Вес (кг)
        private String  condition;  // Состояние
        private String  vetCertNo;  // Номер вет. свидетельства
        private String  vetDate;    // Дата осмотра
        private List<Vaccine> vaccines;
        private boolean verified;
        private boolean rejected;
        private boolean sold;

        public static class Vaccine {
            private String name;
            private String date;
            public String getName()  { return name; }
            public String getDate()  { return date; }
            public void setName(String name) { this.name = name; }
            public void setDate(String date) { this.date = date; }
        }

        public String  getSpecies()  { return species; }
        public String  getBreed()    { return breed; }
        public int     getAge()      { return age; }
        public String  getSex()      { return sex; }
        public int     getCount()    { return count; }
        public double  getWeight()   { return weight; }
        public String  getCondition(){ return condition; }
        public String  getVetCertNo(){ return vetCertNo; }
        public String  getVetDate()  { return vetDate; }
        public List<Vaccine> getVaccines() { return vaccines; }
        public boolean isVerified()  { return verified; }
        public boolean isRejected()  { return rejected; }

        public void setSpecies(String s)   { species = s; }
        public void setBreed(String b)     { breed = b; }
        public void setAge(int a)          { age = a; }
        public void setSex(String s)       { sex = s; }
        public void setCount(int c)        { count = c; }
        public void setWeight(double w)    { weight = w; }
        public void setCondition(String c) { condition = c; }
        public void setVetCertNo(String v) { vetCertNo = v; }
        public void setVetDate(String d)   { vetDate = d; }
        public void setVaccines(List<Vaccine> v) { vaccines = v; }
        public void setVerified(boolean v) { verified = v; }
        public void setRejected(boolean r) { rejected = r; }
    }

    // ── Getters ────────────────────────────────────────
    public String  getId()           { return id; }
    public String  getUid()          { return uid; }
    public String  getCategory()     { return category; }
    public String  getTitle()        { return title; }
    public String  getDescription()  { return description; }
    public double  getPrice()        { return price; }
    public boolean isNegotiable()    { return negotiable; }
    public String  getRegion()       { return region; }
    public double  getLatitude()     { return latitude; }
    public double  getLongitude()    { return longitude; }
    public List<String> getPhotos()  { return photos; }
    public boolean isActive()        { return active; }
    public Boolean isPending()       { return pending; }
    public boolean isRejected()      { return rejected; }
    public boolean isSold()          { return sold; }
    public void    setSold(boolean s){ this.sold = s; }
    public String  getRejectReason() { return rejectReason; }
    public long    getCreatedAt()    { return createdAt; }
    public String  getSellerName()   { return sellerName; }
    public String  getSellerPhone()  { return sellerPhone; }
    public String  getSellerAvatar() { return sellerAvatar; }
    public double  getSellerRating() { return sellerRating; }
    public Passport getPassport()    { return passport; }

    // ── Setters ────────────────────────────────────────
    public void setId(String id)               { this.id = id; }
    public void setUid(String uid)             { this.uid = uid; }
    public void setCategory(String cat)        { this.category = cat; }
    public void setTitle(String title)         { this.title = title; }
    public void setDescription(String desc)    { this.description = desc; }
    public void setPrice(double price)         { this.price = price; }
    public void setNegotiable(boolean neg)     { this.negotiable = neg; }
    public void setRegion(String region)       { this.region = region; }
    public void setLatitude(double lat)        { this.latitude = lat; }
    public void setLongitude(double lng)       { this.longitude = lng; }
    public void setPhotos(List<String> photos) { this.photos = photos; }
    public void setActive(boolean active)      { this.active = active; }
    public void setPending(Boolean pending)    { this.pending = pending; }
    public void setRejected(boolean rejected)  { this.rejected = rejected; }
    public void setRejectReason(String r)      { this.rejectReason = r; }
    public void setCreatedAt(long time)        { this.createdAt = time; }
    public void setSellerName(String n)        { this.sellerName = n; }
    public void setSellerPhone(String p)       { this.sellerPhone = p; }
    public void setSellerAvatar(String a)      { this.sellerAvatar = a; }
    public void setSellerRating(double r)      { this.sellerRating = r; }
    public void setPassport(Passport p)        { this.passport = p; }

    // ── Helpers ────────────────────────────────────────
    public boolean hasPassport() {
        return passport != null && passport.isVerified();
    }
    public boolean hasPhotos() {
        return photos != null && !photos.isEmpty();
    }
    public String getFirstPhoto() {
        return hasPhotos() ? photos.get(0) : null;
    }
    public String getPriceFormatted() {
        if (price <= 0) return "Договорная";
        return String.format(
                        java.util.Locale.getDefault(), "%,.0f сом", price)
                .replace(",", " ");
    }
    public String getCategoryEmoji() {
        if (category == null) return "📦";
        switch (category) {
            case "Скот":    return "🐄";
            case "Зерно":   return "🌾";
            case "Овощи":   return "🥬";
            case "Фрукты":  return "🍎";
            case "Молоко":  return "🥛";
            case "Птица":   return "🐔";
            case "Корма":   return "🌿";
            case "Техника": return "🚜";
            case "Услуги":  return "🔧";
            default:        return "📦";
        }
    }
}