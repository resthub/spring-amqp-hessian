package org.resthub.rpc.jpa.model;

import java.io.Serializable;
import java.util.*;

import javax.persistence.*;

@Entity
public class Person implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Long id;
	private List<Address> addresses;
	private Set<Address> addressesSet;
    private Map<String, Address> myAdresses;
	
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

    @ElementCollection
    @CollectionTable(name="my_addresses")
    @MapKeyColumn (name = "address")
    @Column(name = "myaddresses")
    public Map<String, Address> getMyAdresses() {
        if(this.myAdresses == null){
            this.myAdresses = new HashMap<String, Address>();
        }
        return myAdresses;
    }

    public void setMyAdresses(Map<String, Address> myAdresses) {
        this.myAdresses = myAdresses;
    }
}
