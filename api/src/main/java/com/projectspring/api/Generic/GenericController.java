package com.projectspring.api.Generic;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

public class GenericController<E, I, S extends GenericService<E, I>> {
    protected S service;

    public GenericController(S service) {
        this.service = service;
    }

    @GetMapping
    public Page<E> findAll(Pageable pageable) {
        return service.findAll(pageable);
    }

    @PostMapping
    public E saveOrUpdate(@RequestBody E entity) {
        return service.saveOrUpdate(entity);
    }

    @GetMapping("{id}")
    public Optional<E> findById(@PathVariable I id) {
        return service.findById(id);
    }

    @DeleteMapping("{id}")
    public void deleteById(@PathVariable I id) {
        service.deleteById(id);
    }
}
