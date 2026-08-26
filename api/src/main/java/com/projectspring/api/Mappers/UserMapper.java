package com.projectspring.api.Mappers;


import org.mapstruct.Mapper;

import com.projectspring.api.Dto.UserDto;
import com.projectspring.api.Generic.GenericMapper;
import com.projectspring.api.Models.UserEntities;

@Mapper
public interface UserMapper extends GenericMapper<UserEntities, UserDto> {
    
}
