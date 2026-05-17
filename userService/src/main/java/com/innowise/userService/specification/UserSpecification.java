package com.innowise.userService.specification;

import com.innowise.userService.entity.UserEntity;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

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
