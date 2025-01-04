package com.ethereal.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Table(name = "users")
@Entity
public class User implements UserDetails {
    @Id
    @GeneratedValue(generator = "users_id_seq", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "users_id_seq", sequenceName = "users_id_seq", allocationSize = 1)
    private long id;
    private String uuid;
    private String login;
    private String email;
    private String password;
    @Enumerated(EnumType.STRING)
    private Role role;
    @Column(name = "islock")
    private boolean isLock;
    @Column(name = "isenabled")
    private boolean isEnabled;
    @Column(name = "firstname")
    private String firstName;
    @Column(name = "lastname")
    private String lastName;
    @Column(name = "phone")
    private String phone;
    @Column(name = "city")
    private String city;
    @Column(name = "street")
    private String street;
    @Column(name = "number")
    private String apartmentNumber;
    @Column(name = "postalcode")
    private String postalCode;
    @Column(name = "iscompany")
    private boolean isCompany;
    @Column(name = "companyname")
    private String companyName;
    @Column(name = "nip")
    private String nip;
    @ElementCollection
    @CollectionTable(
            name = "user_favourites",
            joinColumns = @JoinColumn(name = "user_uuid", referencedColumnName = "uuid")
    )
    @Column(name = "product_uuid", nullable = false)
    private List<String> favouriteProductUids = new ArrayList<>();


    public User() {
        generateUuid();
    }

    public User(long id, String uuid, String login, String email, String password, Role role, boolean isLock, boolean isEnabled) {
        this.id = id;
        this.uuid = uuid;
        this.login = login;
        this.email = email;
        this.password = password;
        this.role = role;
        this.isLock = isLock;
        this.isEnabled = isEnabled;
        generateUuid();
    }

    private long getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return login;
    }

    @Override
    public boolean isAccountNonExpired() {
        return false;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !isLock;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    private void generateUuid() {
        if (uuid == null || uuid.isEmpty()) {
            setUuid(UUID.randomUUID().toString());
        }
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", uuid='" + uuid + '\'' +
                ", login='" + login + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", isLock=" + isLock +
                ", isEnabled=" + isEnabled +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phone='" + phone + '\'' +
                ", city='" + city + '\'' +
                ", street='" + street + '\'' +
                ", apartmentNumber='" + apartmentNumber + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", isCompany=" + isCompany +
                ", companyName='" + companyName + '\'' +
                ", nip='" + nip + '\'' +
                ", favouriteProductUids=" + (favouriteProductUids.size() > 5
                ? favouriteProductUids.subList(0, 5) + " (+" + (favouriteProductUids.size() - 5) + " more)"
                : favouriteProductUids) +
                '}';
    }

}
