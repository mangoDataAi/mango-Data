package com.mango.test.database.model;

import lombok.Data;

@Data
public class NamingStandardConfig {
    private String mandatoryStandardId;
    private String optionalStandardId;
    private NamingPatterns patterns;
}

@Data
class NamingPatterns {
    private StandardPattern mandatory;
    private StandardPattern optional;
}

@Data
class StandardPattern {
    private TablePattern table;
    private FieldPattern field;
    private IndexPattern index;
}

@Data
class TablePattern {
    private String mode;
    private String pattern;
    private String description;
}

@Data
class FieldPattern {
    private String mode;
    private String pattern;
    private String description;
}

@Data
class IndexPattern {
    private String mode;
    private String pattern;
    private String description;
} 