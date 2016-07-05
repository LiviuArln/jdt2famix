package org.moosetechnology.jdt2famix.injava;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.moosetechnology.jdt2famix.Importer;
import org.moosetechnology.model.famix.*;
import org.moosetechnology.model.famix.Class;
import org.moosetechnology.model.java.JavaModel;

import ch.akuhn.fame.MetaRepository;
import ch.akuhn.fame.Repository;

public class InJavaImporter extends Importer {

	private Map<String,Namespace> namespaces;
	public Map<String,Namespace> getNamespaces() { return namespaces; }
	
	private Map<String, Type> types;
	public Map<String, Type> getTypes() { return types; }
	
	Repository repository;
	public Repository getRepository() { return repository; }
	
	/*
	 * This is a structure that keeps track of the current stack of containers
	 * It is particularly useful when we deal with inner or anonymous classes
	 */ 
	private Deque<ContainerEntity> containerStack = new ArrayDeque<ContainerEntity>();
	public Deque<ContainerEntity> getContainerStack() { return containerStack; }
	
	public InJavaImporter() {
		 MetaRepository metaRepository = new MetaRepository();
		 FAMIXModel.importInto(metaRepository);
		 JavaModel.importInto(metaRepository);
		 repository = new Repository(metaRepository);
		 
		 types = new HashMap<String, Type>();
		 namespaces = new HashMap<String, Namespace>();
	}
	
	
	@Override
	protected FileASTRequestor getRequestor() {
		return new AstRequestor(this);
	}

	public Namespace ensureNamespaceFromPackageBinding(IPackageBinding binding) {
		String packageName = binding.getName();
		if (namespaces.containsKey(packageName)) 
			return namespaces.get(packageName);
		else {
			Namespace namespace = createNamespaceNamed(packageName);
			namespaces.put(packageName, namespace);
			repository.add(namespace);
			return namespace;
		}
	}

	//NAMESPACES
	private Namespace createNamespaceNamed(String k) {
		Namespace namespace = new Namespace();
		namespace.setName(k);
		namespace.setIsStub(true);
		return namespace;
	}
	
	private ContainerEntity ensureContainerEntityForTypeBinding(ITypeBinding binding) {
		return ensureNamespaceFromPackageBinding(binding.getPackage());
	}
	
	//TYPES
	public Type ensureTypeFromTypeBinding(ITypeBinding binding) {
		String qualifiedName = binding.getQualifiedName();
		if (types.containsKey(qualifiedName)) return types.get(qualifiedName);
		Type type = createTypeFromTypeBinding(binding);
		types.put(qualifiedName, type);
		type.setName(binding.getName());
		type.setContainer(ensureContainerEntityForTypeBinding(binding));
		if (binding.getSuperclass() != null) 
			createInheritanceFromSubtypeToSuperTypeBinding(type, binding);
		for (ITypeBinding interfaceBinding : binding.getInterfaces()) {
			createInheritanceFromSubtypeToSuperTypeBinding(type, interfaceBinding);
		}
		repository.add(type);
		return type;
	}

	private Type createTypeFromTypeBinding(ITypeBinding binding) {
		Class clazz = new Class();
		clazz.setIsInterface(binding.isInterface());
		clazz.setName(binding.getName());
		clazz.setIsStub(true);
		return clazz;
	}

	//INHERITANCE
	private Inheritance createInheritanceFromSubtypeToSuperTypeBinding(Type subType,
			ITypeBinding superBinding) {
		Inheritance inheritance = new Inheritance();
		inheritance.setSuperclass(ensureTypeFromTypeBinding(superBinding)); 
		inheritance.setSubclass(subType);
		repository.add(inheritance);
		return inheritance;
	}
	
	//STACK
	public void pushOnContainerStack(ContainerEntity namespace) {
		this.containerStack.push(namespace);
	}
	public ContainerEntity popFromContainerStack() {
		return this.containerStack.pop();
	}
	public ContainerEntity topOfContainerStack() {
		return this.containerStack.peek();
	}
	
	//EXPORT
	public void exportMSE(String fileName) {
		try {
			repository.exportMSE(new FileWriter(fileName));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}