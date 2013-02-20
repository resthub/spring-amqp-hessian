package org.resthub.rpc.jpa.repository;

import org.resthub.rpc.jpa.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {

}
