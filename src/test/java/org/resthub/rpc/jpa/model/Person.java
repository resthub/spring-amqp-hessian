package org.resthub.rpc.jpa.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Entity
public class Person implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Long id;
	private List<Address> addresses;
	private Set<Address> addressesSet;
	
	public Person(){
		
	}
	
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	@OneToMany(mappedBy = "person", fetch = FetchType.LAZY, cascade=CascadeType.ALL)
	public List<Address> getAddresses() {
		if (this.addresses == null){
			this.addresses = new ArrayList<Address>();
		}
		return addresses;
	}
	public void setAddresses(List<Address> addresses) {
		this.addresses = addresses;
	}


	@OneToMany(mappedBy = "personSet", fetch = FetchType.LAZY, cascade=CascadeType.ALL)
	public Set<Address> getAddressesSet() {
		if (this.addressesSet == null){
			this.addressesSet = new HashSet<Address>();
		}
		return addressesSet;
	}


	public void setAddressesSet(Set<Address> addressesSet) {
		this.addressesSet = addressesSet;
	}
	
	
	
}
