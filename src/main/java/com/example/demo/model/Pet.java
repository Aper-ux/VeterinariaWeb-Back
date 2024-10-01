package com.example.demo.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Pet {
    private String id;
    private String name;
    private String species;
    private String breed;
    private int age;
    private String ownerId;
}