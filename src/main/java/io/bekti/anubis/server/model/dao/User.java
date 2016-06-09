package io.bekti.anubis.server.model.dao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.*;

@Entity
@Table(name = "users", indexes = { @Index(name = "userNameIndex", columnList = "name", unique = true) })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private int id;

    @NotBlank
    @Column(name = "name", unique = true)
    private String name;

    @NotBlank
    @Column(name = "password")
    @JsonIgnore
    private String password;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}