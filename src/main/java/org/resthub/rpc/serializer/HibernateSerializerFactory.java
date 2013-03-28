package org.resthub.rpc.serializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.caucho.hessian.io.*;
import org.hibernate.Hibernate;
import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.collection.internal.PersistentMap;

public class HibernateSerializerFactory extends SerializerFactory {
	
	private HibernateListSerializer listSerializer = new HibernateListSerializer();
	private HibernateMapSerializer mapSerializer = new HibernateMapSerializer();

	@SuppressWarnings("rawtypes")
	public Serializer getSerializer(Class cl) throws HessianProtocolException {
       if(PersistentMap.class.isAssignableFrom(cl)){
            return mapSerializer;
       }else if (AbstractPersistentCollection.class.isAssignableFrom(cl)){
            return listSerializer;
       }
		return super.getSerializer(cl);
	}
	
	private static class HibernateListSerializer implements Serializer {
		
		private CollectionSerializer delegate = new CollectionSerializer();

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public void writeObject(Object obj, AbstractHessianOutput out)
				throws IOException {
			if (Hibernate.isInitialized(obj)){
				delegate.writeObject(new ArrayList((Collection) obj), out);
			}
			else {
				delegate.writeObject(new ArrayList(), out);
			}
		}
		
	}

	private static class HibernateMapSerializer implements Serializer {

		private MapSerializer delegate = new MapSerializer();

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public void writeObject(Object obj, AbstractHessianOutput out)
				throws IOException {
			if (Hibernate.isInitialized(obj)){
				delegate.writeObject(new HashMap((Map) obj), out);
			}
			else {
				delegate.writeObject(new HashMap(), out);
			}
		}

	}
}
