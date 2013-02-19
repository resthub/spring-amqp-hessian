package org.resthub.rpc.jpa.model;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class Address implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Long id;
	private String wayNumber;
	private String wayName;
	private String postalCode;
	private String city;
	private Person person;
	private Person personSet;
	
	public Address(){
		
	}
	
	
	
	public Address(String wayNumber, String wayName,
			String postalCode, String city) {
		super();
		this.wayNumber = wayNumber;
		this.wayName = wayName;
		this.postalCode = postalCode;
		this.city = city;
	}



	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getWayNumber() {
		return wayNumber;
	}
	public void setWayNumber(String wayNumber) {
		this.wayNumber = wayNumber;
	}
	public String getWayName() {
		return wayName;
	}
	public void setWayName(String wayName) {
		this.wayName = wayName;
	}
	public String getPostalCode() {
		return postalCode;
	}
	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	
	@ManyToOne
	public Person getPerson() {
		return person;
	}
	public void setPerson(Person person) {
		this.person = person;
	}

	@ManyToOne
	public Person getPersonSet() {
		return personSet;
	}

	public void setPersonSet(Person personSet) {
		this.personSet = personSet;
	}
	
	
}
