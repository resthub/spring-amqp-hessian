package org.resthub.rpc.jpa.service;

import org.resthub.common.service.CrudService;
import org.resthub.rpc.jpa.model.Person;

public interface PersonService extends CrudService<Person, Long> {
	
	public Person findByIdWithAddress(Long id);

}
