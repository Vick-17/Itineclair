package com.projectspring.api.Generic;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public abstract class GenericServiceImpl<E, I, D, R extends JpaRepository<E, I>, M extends GenericMapper<E, D>>
        implements GenericService<D, I> {
    protected R repository;
    protected M mapper;

    public GenericServiceImpl(R repository, M mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Page<D> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDto);
    }

    @Override
    public Optional<D> findById(I id) {
        return repository.findById(id).map(this::toDto);
    }

    @Override
    public D saveOrUpdate(D dto) {
        return toDto(repository.save(toEntity(dto)));
    }

    @Override
    public void deleteById(I id) {
        repository.deleteById(id);
    }

    E toEntity(D dto) {
        return mapper.toEntity(dto);
    }

    protected D toDto(E entity) {
        return mapper.toDto(entity);
    }
}
