package com.anemia.service;

import org.springframework.stereotype.Service;

@Service
public class AnemiaDetectionService {

    public DetectionResult detect(Double hemoglobin, Double hematocrit,
            Double mcv, Double mch, Double mchc, Double rbc, Double rdw,
            Double serumIron, Double ferritin, Double transferrinSaturation,
            Integer age, String sex) {

        boolean isFemale = sex != null && sex.equalsIgnoreCase("female");
        double score = 0.0;

        if (hemoglobin != null) {
            double threshold = isFemale ? 12.0 : 13.5;
            if (hemoglobin < threshold) score += 0.40 * (threshold - hemoglobin) / threshold;
        }
        if (hematocrit != null) {
            double threshold = isFemale ? 36.0 : 41.0;
            if (hematocrit < threshold) score += 0.15;
        }
        if (rbc != null) {
            double threshold = isFemale ? 4.1 : 4.5;
            if (rbc < threshold) score += 0.15;
        }
        if (ferritin != null) {
            double threshold = isFemale ? 11.0 : 24.0;
            if (ferritin < threshold) score += 0.10;
        }
        if (mcv != null && mcv < 80) score += 0.10;
        if (serumIron != null && serumIron < 60) score += 0.05;
        if (transferrinSaturation != null && transferrinSaturation < 20) score += 0.05;

        boolean anemic = score >= 0.20;
        double confidence = Math.min(0.55 + (score * 0.42), 0.97);

        String anemiaType = "Normal";
        if (anemic) {
            if (mcv != null && mcv < 80) anemiaType = "Iron-Deficiency Anemia";
            else if (mcv != null && mcv > 100) anemiaType = "Macrocytic Anemia";
            else anemiaType = "Normocytic Anemia";
        }

        return new DetectionResult(anemic, anemiaType, confidence);
    }

    public static class DetectionResult {
        private final boolean anemic;
        private final String anemiaType;
        private final double confidence;

        public DetectionResult(boolean anemic, String anemiaType, double confidence) {
            this.anemic = anemic;
            this.anemiaType = anemiaType;
            this.confidence = confidence;
        }

        public boolean isAnemic() { return anemic; }
        public String getAnemiaType() { return anemiaType; }
        public double getConfidence() { return confidence; }
    }
}