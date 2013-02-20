package org.resthub.rpc.serializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.Hibernate;
import org.hibernate.collection.internal.AbstractPersistentCollection;

import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.CollectionSerializer;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.hessian.io.Serializer;
import com.caucho.hessian.io.SerializerFactory;

public class HibernateSerializerFactory extends SerializerFactory {
	
	private HibernateListSerializer listSerializer = new HibernateListSerializer();

	@SuppressWarnings("rawtypes")
	public Serializer getSerializer(Class cl) throws HessianProtocolException {
		return (Serializer) (AbstractPersistentCollection.class
				.isAssignableFrom(cl) ? listSerializer : super.getSerializer(cl));
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
}
