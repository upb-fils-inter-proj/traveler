package ro.upb.traveler.admin.attraction;

/** The texts of an attraction in one language. Sources are never sent to the tourist backend. */
public record Translation(String name, String description, String history, String sources) {}
