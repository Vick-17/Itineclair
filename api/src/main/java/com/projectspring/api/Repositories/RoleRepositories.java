package com.projectspring.api.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projectspring.api.Models.RoleEntities;

public interface RoleRepositories extends JpaRepository<RoleEntities, Integer> {
    RoleEntities findByName(String name);
    
}
