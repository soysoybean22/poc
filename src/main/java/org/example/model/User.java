package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    private Long id;
    private String name;
    private String email;
    private int age;
    private Address address;

    @JsonProperty("phone_numbers")
    private List<String> phoneNumbers;

    public User() {}

    public User(Long id, String name, String email, int age, Address address, List<String> phoneNumbers) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
        this.address = address;
        this.phoneNumbers = phoneNumbers;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }

    public List<String> getPhoneNumbers() { return phoneNumbers; }
    public void setPhoneNumbers(List<String> phoneNumbers) { this.phoneNumbers = phoneNumbers; }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', email='" + email +
               "', age=" + age + ", address=" + address + ", phoneNumbers=" + phoneNumbers + "}";
    }
}
