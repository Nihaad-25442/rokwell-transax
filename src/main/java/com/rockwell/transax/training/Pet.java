package com.rockwell.transax.training;

public class Pet {
    // --- Fields ---
    // each pet object stores its own name, species, age and weight
    // These fields define the "state" of a pet
    String name;
    String species;
    int age;
    double weight;
    double maxWeight;

    // --- Getter methods ---
    // Getters follow the naming pascal convention
    // They provide read access to a field's value
    public String getName() {
        return this.name;
    }
    public String getSpecies() {
        return this.species;
    }
    public int getAge() {
        return this.age;
    }
    public double getWeight() {
        return this.weight;
    }

    /// --- Constructor 1: Full constructor (all four fields) ---
    // The "this" keyword refers to the current object being created
    // We use "this.name" to distinguish the field from the parameter,
    // since they share the same name
    public Pet(String name, String species, int age, double weight) {
        this.name = name;
        this.species = species;
        this.age = age;
        this.weight =weight;

        // Set a healthy weight limit based on species
        this.maxWeight = switch (species) {
            case "Dog" -> 40.0;
            case "Cat" -> 10.0;
            default -> 1.0;
        };
    }

    /// --- Constructor 2: Without weight (defaults to 0.0) ---
    // Constructor Overloading - same name, different parameters
    // this(...) calls the full constructor above. This is "constructor chaining"
    // It avoids duplicating the field assignment code
    public Pet(String name, String species, int age) {
        this(name, species, age, 0.0);
    }

    /// --- Constructor 3: Name and species only (unknown age and weight) ---
    // Chains to Constructor 2, which chains to Constructor 1
    // The result: all three constructors ultimately run the same initialization
    public Pet(String name, String species) {
        this(name, species, 0);
    }

    // NOTE: Sine we defined our own constructors, Java no longer provides
    // a default no-argument constructor. Writing "new Pet()" would cause
    // a compiler error - You must supply at least a name and species

    // --- display method ---
    // Prints the pet's profile to the console
    // We use System.out.println() in regular classes (not IO.println(),
    // which is only available in compact source files)
    public void display() {
        System.out.println(this.name + " (" + this.species + ")");
        System.out.println(" Age: " + this.age + " years");
        System.out.println(" Weight: " + String.format("%.1f", this.weight) + "kg");
    }

    // --- feed method (no arguments) - OVERLOADED ---
    // Method overloading: same name "feed", but different parameter lists
    // This version picks a default amount based on species, then delegates
    // to the other feed method. This is the delegation pattern
    // one overloaded method calls the other to avoid duplicating logic
    public boolean feed(double amount) {
        if (amount <= 0) {
            System.out.println(this.name + ": Feed amount must be positive!");
            return false;
        }
        if (this.weight + amount > this.maxWeight) {
            System.out.println(this.name + ": Would exceed max healthy weight of "
                    + String.format("%.1f", this.maxWeight) + " kg!");
            return false;
        }
        this.weight = this.weight + amount;
        System.out.println(this.name + " ate" + String.format("%.1f", amount)
                + " kg of food. Weight: " + String.format("%.1f", this.weight) + " kg");
        return true;
    }

    // --- feed method (no arguments) - OVERLOADED ---
    // Method overloading: same name "feed", but different parameter lists
    // This version picks a default amount based on species, then delegates
    // to the other feed method. This is the delegation pattern -
    // one overloaded method calls the other to avoid duplicating logic
    public boolean feed() {
     double defaultAmount = switch (this.species) {
         case "Dog" -> 0.5;
         case "Cat" -> 0.3;
         default -> 0.2;
     };
        System.out.println("Feeding " + this.name + "the default portion for a " + this.species
                + "...");
        return feed(defaultAmount);
    }

    // --- Birthday Method ---
    // Another method that modifies state: the pet's age increases by 1
    public void birthday() {
        this.age = this.age + 1;
        System.out.println("Happy Birthday, " + this.name
                + "! Now " + this.age + " years old.");
    }

    // --- describe method ---
    // Unlike display(), this method RETURNS a String instead of printing it
    // This lets the caller decide what to do with the description
    public String describe() {
        return this.name + " is a " + this.age + "-year-old " + this.species
                + " weighing " + String.format("%.1f", this.weight) + " kg";
    }


}
