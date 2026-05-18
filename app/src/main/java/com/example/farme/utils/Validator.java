package com.example.farme.utils;

import java.util.regex.Pattern;

public class Validator {

    private static final Pattern PHONE =
            Pattern.compile("^\\+996[0-9]{9}$");
    private static final Pattern EMAIL =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    /** +996700123456 → 996700123456@farme.kg */
    public static String phoneToEmail(String phone) {
        if (phone == null) return "";
        return phone.replace("+", "") + "@farme.kg";
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE.matcher(phone.trim()).matches();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email.trim()).matches();
    }

    public static boolean isValidPassword(String pass) {
        return pass != null && pass.length() >= 6;
    }

    public static boolean isValidName(String name) {
        return name != null && name.trim().length() >= 2;
    }

    public static boolean isValidPrice(String price) {
        if (price == null || price.trim().isEmpty()) return false;
        try { return Double.parseDouble(price.trim()) >= 0; }
        catch (NumberFormatException e) { return false; }
    }

    public static String formatPhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("996") && digits.length() == 12)
            return "+996 " + digits.substring(3, 6) + " "
                    + digits.substring(6, 9) + " " + digits.substring(9);
        return phone;
    }

    public static String formatPrice(double price) {
        if (price <= 0) return "Договорная";
        return String.format(
                        java.util.Locale.getDefault(), "%,.0f сом", price)
                .replace(",", " ");
    }
}