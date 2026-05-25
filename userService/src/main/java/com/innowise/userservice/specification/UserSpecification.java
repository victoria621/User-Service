package com.innowise.userservice.specification;

import com.innowise.userservice.entity.UserEntity;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    private UserSpecification() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static Specification<UserEntity> hasName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isEmpty()) return null;
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    public static Specification<UserEntity> hasSurname(String surname) {
        return (root, query, cb) -> {
            if (surname == null || surname.isEmpty()) return null;
            return cb.like(cb.lower(root.get("surname")), "%" + surname.toLowerCase() + "%");
        };
    }
}
