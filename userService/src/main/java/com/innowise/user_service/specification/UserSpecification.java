package com.innowise.user_service.specification;

import com.innowise.user_service.entity.UserEntity;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    private UserSpecification() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static Specification<UserEntity> hasName(String name) {
        Specification<UserEntity> spec = Specification.where(null);

        if (name != null && !name.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("name"), name));
        }

        return spec;
    }

    public static Specification<UserEntity> hasSurname(String surname) {
        Specification<UserEntity> spec = Specification.where(null);

        if (surname != null && !surname.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("surname"), surname));
        }

        return spec;
    }
}
