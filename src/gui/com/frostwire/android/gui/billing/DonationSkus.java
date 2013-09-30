package com.frostwire.android.gui.billing;

public interface DonationSkus {
    public enum DonationSkuType {
        SKU_01_DOLLARS,
        SKU_05_DOLLARS,
        SKU_10_DOLLARS,
        SKU_25_DOLLARS
    }
    
    public String getSku(DonationSkuType type);
    
    static final class DefaultDonationSkus implements DonationSkus {

        @Override
        public String getSku(DonationSkuType type) {
            switch (type) {
            case SKU_01_DOLLARS:
                return "frostwire.donation.one";
            case SKU_05_DOLLARS:
                return "frostwire.donation.five";
            case SKU_10_DOLLARS:
                return "frostwire.donation.ten";
            case SKU_25_DOLLARS:
                return "frostwire.donation.twentyfive";
            default:
                return "frostwire.donation.twentyfive";
            }
        }
    }
    
    static final class OuyaDonationSkus implements DonationSkus {
        @Override
        public String getSku(DonationSkuType type) {
            switch (type) {
            case SKU_01_DOLLARS:
                return "ouya-donation-1";
            case SKU_05_DOLLARS:
                return "ouya-donation-5";
            case SKU_10_DOLLARS:
                return "ouya-donation-10";
            case SKU_25_DOLLARS:
                return "ouya-donation-25";
            default:
                return "ouya-donation-25";
            }
        }
    }
}
