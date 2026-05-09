package com.zokomart.backend.auth;

public final class BuyerPhoneNumberNormalizer {

    private BuyerPhoneNumberNormalizer() {
    }

    public static String normalize(String rawPhoneNumber) {
        if (rawPhoneNumber == null) {
            return "";
        }

        String trimmed = rawPhoneNumber.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        boolean keepPlus = trimmed.startsWith("+");
        String digits = trimmed.replaceAll("[^\\d]", "");
        if (digits.isEmpty()) {
            return "";
        }

        if (digits.startsWith("0") && digits.length() == 10) {
            return "+233" + digits.substring(1);
        }
        if (digits.startsWith("233") && digits.length() == 12) {
            return "+" + digits;
        }
        return keepPlus ? "+" + digits : digits;
    }
}
