package org.resthub.rpc.jpa.service;

import javax.inject.Inject;
import javax.inject.Named;

import org.resthub.common.service.CrudServiceImpl;
import org.resthub.rpc.jpa.model.Address;
import org.resthub.rpc.jpa.model.Person;
import org.resthub.rpc.jpa.repository.PersonRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Named("personService")
public class PersonServiceImpl extends
		CrudServiceImpl<Person, Long, PersonRepository> implements
		PersonService {

	@Override
	@Inject
	@Named("personRepository")
	public void setRepository(PersonRepository personRepository) {
		super.setRepository(personRepository);
	}

	@Transactional
	public Person findByIdWithAddress(Long id) {
		Person result = this.repository.findOne(id);
		// initialize addresses
		for (Address a : result.getAddresses());
		for (Address a : result.getAddressesSet());
        for (Map.Entry entry : result.getMyAdresses().entrySet());
        return result;
	}
}
