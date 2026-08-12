package com.novabank.auth.mapper;

import com.novabank.auth.domain.UserCredential;
import com.novabank.auth.web.dto.CurrentUserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserCredentialMapper {
    @Mapping(target = "customerId", source = "id")
    @Mapping(target = "role", expression = "java(user.getRole().name())")
    CurrentUserResponse toCurrentUser(UserCredential user);
}
