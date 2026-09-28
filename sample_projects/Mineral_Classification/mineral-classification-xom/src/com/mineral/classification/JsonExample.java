package com.mineral.classification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Example demonstrating JSON serialization of MineralSpecimen objects.
 * 
 * This class shows how to:
 * - Serialize a MineralSpecimen to JSON
 * - Deserialize JSON back to a MineralSpecimen object
 * - Handle pretty-printed JSON output
 */
public class JsonExample {
    
    public static void main(String[] args) {
        try {
            // Create ObjectMapper for JSON operations
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            
            // Create a sample mineral specimen
            MineralSpecimen specimen = new MineralSpecimen("SPEC-001", "Quartz Sample");
            
            // Set properties for a typical quartz specimen
            specimen.setContainsSiliconOxygenTetrahedra(true);
            specimen.setMohsHardness(7.0);
            specimen.setSpecificGravity(2.65);
            specimen.setLuster("vitreous");
            specimen.setColor("clear");
            specimen.setCrystalHabit("hexagonal");
            specimen.setCleavage("none");
            specimen.setFracture("conchoidal");
            specimen.setReactsWithAcid(false);
            specimen.setFrameworkStructure(true);
            
            // Serialize to JSON
            String json = mapper.writeValueAsString(specimen);
            System.out.println("Serialized MineralSpecimen to JSON:");
            System.out.println(json);
            System.out.println();
            
            // Deserialize from JSON
            MineralSpecimen deserialized = mapper.readValue(json, MineralSpecimen.class);
            System.out.println("Deserialized MineralSpecimen:");
            System.out.println("  ID: " + deserialized.getSpecimenId());
            System.out.println("  Name: " + deserialized.getSpecimenName());
            System.out.println("  Mohs Hardness: " + deserialized.getMohsHardness());
            System.out.println("  Luster: " + deserialized.getLuster());
            System.out.println();
            
            // Demonstrate that computed properties are excluded from JSON
            System.out.println("Computed properties (not in JSON):");
            System.out.println("  Is Hard Mineral: " + deserialized.isHardMineral());
            System.out.println("  Has Vitreous Luster: " + deserialized.hasVitreousLuster());
            System.out.println("  Has No Cleavage: " + deserialized.hasNoCleavage());
            System.out.println("  Has Conchoidal Fracture: " + deserialized.hasConchoidalFracture());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

// Made with Bob
