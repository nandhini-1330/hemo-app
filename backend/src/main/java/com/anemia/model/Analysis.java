package com.anemia.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "analyses")
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Double hemoglobin;
    private Double hematocrit;
    private Double mcv;
    private Double mch;
    private Double mchc;
    private Double rbc;
    private Double rdw;
    private Double serumIron;
    private Double ferritin;
    private Double transferrinSaturation;
    private Integer age;
    private String sex;
    private boolean anemic;
    private String anemiaType;
    private Double confidence;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Analysis() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Double getHemoglobin() { return hemoglobin; }
    public void setHemoglobin(Double hemoglobin) { this.hemoglobin = hemoglobin; }
    public Double getHematocrit() { return hematocrit; }
    public void setHematocrit(Double hematocrit) { this.hematocrit = hematocrit; }
    public Double getMcv() { return mcv; }
    public void setMcv(Double mcv) { this.mcv = mcv; }
    public Double getMch() { return mch; }
    public void setMch(Double mch) { this.mch = mch; }
    public Double getMchc() { return mchc; }
    public void setMchc(Double mchc) { this.mchc = mchc; }
    public Double getRbc() { return rbc; }
    public void setRbc(Double rbc) { this.rbc = rbc; }
    public Double getRdw() { return rdw; }
    public void setRdw(Double rdw) { this.rdw = rdw; }
    public Double getSerumIron() { return serumIron; }
    public void setSerumIron(Double serumIron) { this.serumIron = serumIron; }
    public Double getFerritin() { return ferritin; }
    public void setFerritin(Double ferritin) { this.ferritin = ferritin; }
    public Double getTransferrinSaturation() { return transferrinSaturation; }
    public void setTransferrinSaturation(Double transferrinSaturation) { this.transferrinSaturation = transferrinSaturation; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
    public boolean isAnemic() { return anemic; }
    public void setAnemic(boolean anemic) { this.anemic = anemic; }
    public String getAnemiaType() { return anemiaType; }
    public void setAnemiaType(String anemiaType) { this.anemiaType = anemiaType; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}