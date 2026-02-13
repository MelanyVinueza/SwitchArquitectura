package com.bancario.msdirectorio.repositorio;

import java.util.Optional;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.bancario.msdirectorio.modelo.Institucion;

@Repository
@EnableScan
public interface InstitucionRepositorio extends CrudRepository<Institucion, String> {

    Optional<Institucion> findByCodigoBic(String codigoBic);
}
