package com.mineral.classification;

import java.util.ArrayList;
import java.util.Collection;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a mineral specimen to be classified according to the Mineral Classification Policy.
 * Contains observable, testable, and measurable properties used for classification.
 *
 * JSON serialization enabled with Jackson annotations.
 * Computed properties are excluded from JSON output.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MineralSpecimen {
    
    // Identification
    private String specimenId;
    private String specimenName;
    
    // Chemical composition properties
    private boolean singleElement;
    private boolean containsSulfur;
    private boolean containsOxygen;
    private boolean containsPhosphorus;
    private boolean containsSiliconOxygenTetrahedra;
    private boolean containsOrganicMolecules;
    private boolean metalBondedWithOxygen;
    
    // Physical properties
    private String luster; // metallic, vitreous, etc.
    private double mohsHardness;
    private double specificGravity;
    private String taste; // salty, bitter, none
    private boolean dissolvesInWater;
    private String color; // white, pale, etc.
    private String crystalHabit; // hexagonal, prismatic, platy, flaky, fibrous
    private String cleavage; // basal, distinct, none
    private String fracture; // conchoidal, etc.
    
    // Reaction properties
    private boolean reactsWithAcid;
    private boolean producesEffervescence;
    private boolean producesHydrogenSulfideOdor;
    
    // Environmental context
    private String environment; // evaporite, arid, sedimentary, biological
    
    // Silicate structure (for silicate subclassification)
    private String silicateStructure; // isolated, sheet, chain, framework
    private boolean isolatedTetrahedra;
    private boolean sheetStructure;
    private boolean chainStructure;
    private boolean frameworkStructure;
    
    // Classification results
    private String mineralClass;
    private String silicateSubclass;
    private Collection<String> messages;
    
    // Constructors
    public MineralSpecimen() {
        this.messages = new ArrayList<>();
    }
    
    public MineralSpecimen(String specimenId, String specimenName) {
        this.specimenId = specimenId;
        this.specimenName = specimenName;
        this.messages = new ArrayList<>();
    }
    
    // Getters and Setters
    public String getSpecimenId() {
        return specimenId;
    }
    
    public void setSpecimenId(String specimenId) {
        this.specimenId = specimenId;
    }
    
    public String getSpecimenName() {
        return specimenName;
    }
    
    public void setSpecimenName(String specimenName) {
        this.specimenName = specimenName;
    }
    
    public boolean isSingleElement() {
        return singleElement;
    }
    
    public void setSingleElement(boolean singleElement) {
        this.singleElement = singleElement;
    }
    
    public boolean isContainsSulfur() {
        return containsSulfur;
    }
    
    public void setContainsSulfur(boolean containsSulfur) {
        this.containsSulfur = containsSulfur;
    }
    
    public boolean isContainsOxygen() {
        return containsOxygen;
    }
    
    public void setContainsOxygen(boolean containsOxygen) {
        this.containsOxygen = containsOxygen;
    }
    
    public boolean isContainsPhosphorus() {
        return containsPhosphorus;
    }
    
    public void setContainsPhosphorus(boolean containsPhosphorus) {
        this.containsPhosphorus = containsPhosphorus;
    }
    
    public boolean isContainsSiliconOxygenTetrahedra() {
        return containsSiliconOxygenTetrahedra;
    }
    
    public void setContainsSiliconOxygenTetrahedra(boolean containsSiliconOxygenTetrahedra) {
        this.containsSiliconOxygenTetrahedra = containsSiliconOxygenTetrahedra;
    }
    
    public boolean isContainsOrganicMolecules() {
        return containsOrganicMolecules;
    }
    
    public void setContainsOrganicMolecules(boolean containsOrganicMolecules) {
        this.containsOrganicMolecules = containsOrganicMolecules;
    }
    
    public boolean isMetalBondedWithOxygen() {
        return metalBondedWithOxygen;
    }
    
    public void setMetalBondedWithOxygen(boolean metalBondedWithOxygen) {
        this.metalBondedWithOxygen = metalBondedWithOxygen;
    }
    
    public String getLuster() {
        return luster;
    }
    
    public void setLuster(String luster) {
        this.luster = luster;
    }
    
    public double getMohsHardness() {
        return mohsHardness;
    }
    
    public void setMohsHardness(double mohsHardness) {
        this.mohsHardness = mohsHardness;
    }
    
    public double getSpecificGravity() {
        return specificGravity;
    }
    
    public void setSpecificGravity(double specificGravity) {
        this.specificGravity = specificGravity;
    }
    
    public String getTaste() {
        return taste;
    }
    
    public void setTaste(String taste) {
        this.taste = taste;
    }
    
    public boolean isDissolvesInWater() {
        return dissolvesInWater;
    }
    
    public void setDissolvesInWater(boolean dissolvesInWater) {
        this.dissolvesInWater = dissolvesInWater;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public String getCrystalHabit() {
        return crystalHabit;
    }
    
    public void setCrystalHabit(String crystalHabit) {
        this.crystalHabit = crystalHabit;
    }
    
    public String getCleavage() {
        return cleavage;
    }
    
    public void setCleavage(String cleavage) {
        this.cleavage = cleavage;
    }
    
    public String getFracture() {
        return fracture;
    }
    
    public void setFracture(String fracture) {
        this.fracture = fracture;
    }
    
    public boolean isReactsWithAcid() {
        return reactsWithAcid;
    }
    
    public void setReactsWithAcid(boolean reactsWithAcid) {
        this.reactsWithAcid = reactsWithAcid;
    }
    
    public boolean isProducesEffervescence() {
        return producesEffervescence;
    }
    
    public void setProducesEffervescence(boolean producesEffervescence) {
        this.producesEffervescence = producesEffervescence;
    }
    
    public boolean isProducesHydrogenSulfideOdor() {
        return producesHydrogenSulfideOdor;
    }
    
    public void setProducesHydrogenSulfideOdor(boolean producesHydrogenSulfideOdor) {
        this.producesHydrogenSulfideOdor = producesHydrogenSulfideOdor;
    }
    
    public String getEnvironment() {
        return environment;
    }
    
    public void setEnvironment(String environment) {
        this.environment = environment;
    }
    
    public String getSilicateStructure() {
        return silicateStructure;
    }
    
    public void setSilicateStructure(String silicateStructure) {
        this.silicateStructure = silicateStructure;
    }
    
    public boolean isIsolatedTetrahedra() {
        return isolatedTetrahedra;
    }
    
    public void setIsolatedTetrahedra(boolean isolatedTetrahedra) {
        this.isolatedTetrahedra = isolatedTetrahedra;
    }
    
    public boolean isSheetStructure() {
        return sheetStructure;
    }
    
    public void setSheetStructure(boolean sheetStructure) {
        this.sheetStructure = sheetStructure;
    }
    
    public boolean isChainStructure() {
        return chainStructure;
    }
    
    public void setChainStructure(boolean chainStructure) {
        this.chainStructure = chainStructure;
    }
    
    public boolean isFrameworkStructure() {
        return frameworkStructure;
    }
    
    public void setFrameworkStructure(boolean frameworkStructure) {
        this.frameworkStructure = frameworkStructure;
    }
    
    public String getMineralClass() {
        return mineralClass;
    }
    
    public void setMineralClass(String mineralClass) {
        this.mineralClass = mineralClass;
    }
    
    public String getSilicateSubclass() {
        return silicateSubclass;
    }
    
    public void setSilicateSubclass(String silicateSubclass) {
        this.silicateSubclass = silicateSubclass;
    }
    
    public Collection<String> getMessages() {
        return messages;
    }
    
    public void setMessages(Collection<String> messages) {
        this.messages = messages;
    }
    
    public void addMessage(String message) {
        this.messages.add(message);
    }
    
    // Computed properties for easier rule writing
    // These are excluded from JSON serialization as they are derived from other properties
    
    @JsonIgnore
    public boolean isSoftMineral() {
        return mohsHardness < 3.0;
    }
    
    @JsonIgnore
    public boolean isHardMineral() {
        return mohsHardness > 5.0;
    }
    
    @JsonIgnore
    public boolean hasHighSpecificGravity() {
        return specificGravity > 4.0;
    }
    
    @JsonIgnore
    public boolean hasSaltyOrBitterTaste() {
        return "salty".equalsIgnoreCase(taste) || "bitter".equalsIgnoreCase(taste);
    }
    
    @JsonIgnore
    public boolean isWhiteOrPaleColored() {
        return "white".equalsIgnoreCase(color) || "pale".equalsIgnoreCase(color);
    }
    
    @JsonIgnore
    public boolean isInEvaporiteEnvironment() {
        return environment != null && environment.toLowerCase().contains("evaporite");
    }
    
    @JsonIgnore
    public boolean isInAridEnvironment() {
        return "arid".equalsIgnoreCase(environment);
    }
    
    @JsonIgnore
    public boolean isInSedimentaryEnvironment() {
        return environment != null && environment.toLowerCase().contains("sedimentary");
    }
    
    @JsonIgnore
    public boolean hasHexagonalOrPrismaticHabit() {
        return "hexagonal".equalsIgnoreCase(crystalHabit) || "prismatic".equalsIgnoreCase(crystalHabit);
    }
    
    @JsonIgnore
    public boolean hasPlateOrFlakyHabit() {
        return "platy".equalsIgnoreCase(crystalHabit) || "flaky".equalsIgnoreCase(crystalHabit);
    }
    
    @JsonIgnore
    public boolean hasPrismaticOrFibrousHabit() {
        return "prismatic".equalsIgnoreCase(crystalHabit) || "fibrous".equalsIgnoreCase(crystalHabit);
    }
    
    @JsonIgnore
    public boolean hasBasalCleavage() {
        return "basal".equalsIgnoreCase(cleavage);
    }
    
    @JsonIgnore
    public boolean hasDistinctCleavage() {
        return "distinct".equalsIgnoreCase(cleavage);
    }
    
    @JsonIgnore
    public boolean hasNoCleavage() {
        return "none".equalsIgnoreCase(cleavage) || cleavage == null || cleavage.isEmpty();
    }
    
    @JsonIgnore
    public boolean hasConchoidalFracture() {
        return "conchoidal".equalsIgnoreCase(fracture);
    }
    
    @JsonIgnore
    public boolean hasMetallicLuster() {
        return "metallic".equalsIgnoreCase(luster);
    }
    
    @JsonIgnore
    public boolean hasVitreousLuster() {
        return "vitreous".equalsIgnoreCase(luster);
    }
    
    @JsonIgnore
    public boolean originatesFromBiologicalProcesses() {
        return environment != null && environment.toLowerCase().contains("biological");
    }
}

// Made with Bob
