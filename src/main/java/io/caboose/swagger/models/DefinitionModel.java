package io.caboose.swagger.models;

/**
 * Model that represents swagger defs
 */
public class DefinitionModel {
    String javaPackage;
    String name;

    /**
     * Create def
     * @param javaPackage package
     * @param name def name
     */
    public DefinitionModel(String javaPackage, String name) {
        this.javaPackage = javaPackage;
        this.name = name;
    }

    public String getJavaPackage() {
        return javaPackage;
    }

    public void setJavaPackage(String javaPackage) {
        this.javaPackage = javaPackage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
