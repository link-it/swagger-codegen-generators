package io.swagger.codegen.v3.generators;

import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.CodegenModelFactory;
import io.swagger.codegen.v3.CodegenModelType;
import io.swagger.codegen.v3.CodegenProperty;
import io.swagger.codegen.v3.ISchemaHandler;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SchemaHandler implements ISchemaHandler {

    public static final String ONE_OF_PREFFIX = "OneOf";
    public static final String ANY_OF_PREFFIX = "AnyOf";
    public static final String ARRAY_ITEMS_SUFFIX = "Items";

    protected DefaultCodegenConfig codegenConfig;
    private List<CodegenModel> composedModels = new ArrayList<>();

    public SchemaHandler(DefaultCodegenConfig codegenConfig) {
        this.codegenConfig = codegenConfig;
    }

    @Override
    public void processComposedSchemas(CodegenModel codegenModel, Schema schema, Map<String, CodegenModel> allModels) {

	System.out.println("'processComposedSchemas' codegenModel ["+codegenModel.getName()+"] ["+codegenModel.getClass().getName()+"]");

        if (schema instanceof ComposedSchema) {
            System.out.println("'processComposedSchemas' codegenModel ["+codegenModel.getName()+"] ComposedSchema");
	    this.addComposedModel(this.processComposedSchema(codegenModel, (ComposedSchema) schema, allModels));
            return;
        }
        if (schema instanceof ArraySchema) {
            System.out.println("'processComposedSchemas' codegenModel ["+codegenModel.getName()+"] ArraySchema");
	    this.addComposedModel(this.processArrayItemSchema(codegenModel, (ArraySchema) schema, allModels));
            return;
        }
        final Map<String, Schema> properties = schema.getProperties();
        if (properties == null || properties.isEmpty()) {
            System.out.println("'processComposedSchemas' codegenModel ["+codegenModel.getName()+"] no properties");
	    return;
        }
        for (String name : properties.keySet()) {
	    System.out.println("'processComposedSchemas' codegenModel ["+codegenModel.getName()+"] property '"+name+"'");
            final Schema property = properties.get(name);
            final Optional<CodegenProperty> optionalCodegenProperty = codegenModel.getVars()
                .stream()
                .filter(codegenProperty -> codegenProperty.baseName.equals(name))
                .findFirst();
            if (!optionalCodegenProperty.isPresent()) {
                continue;
            }
            final CodegenProperty codegenProperty = optionalCodegenProperty.get();
            final String codegenName = codegenModel.getName() + codegenConfig.toModelName(codegenProperty.getName());
            if (property instanceof ComposedSchema) {
                this.addComposedModel(this.processComposedSchema(codegenName, codegenProperty, (ComposedSchema) property, allModels));
                continue;
            }
            if (property instanceof ArraySchema) {
                this.addComposedModel(this.processArrayItemSchema(codegenName, codegenProperty, (ArraySchema) property, allModels));
                continue;
            }
        }
    }

    @Override
    public List<CodegenModel> getModels() {
        return composedModels;
    }

    private void mkdirParentDirectory(java.io.File file) throws Exception {
	   mkdirParentDirectory(file.getAbsolutePath());
    }
    private void mkdirParentDirectory(String file) throws Exception {
	try{
		java.io.File p = new java.io.File(file);
		if(p.getParentFile()==null){
			return;
		}
		if(p.getParentFile().exists()){
			return;
		}
		mkdirParentDirectory(p.getParentFile().getAbsolutePath());
		if(p.getParentFile().mkdir()==false){
			throw new Exception("Directory ["+p.getParentFile().getAbsolutePath()+"] non esistente e creazione non riuscita");
		}
	}catch(Exception e){
		throw new Exception("mkdirParentDirectory non riuscito: "+e.getMessage(),e);
	}
     }

    protected CodegenModel processComposedSchema(CodegenModel codegenModel, ComposedSchema composedSchema, Map<String, CodegenModel> allModels) {
       
	System.out.println("'processComposedSchemas' codegenModel composed ["+codegenModel.getName()+"]");

	List<Schema> schemas = composedSchema.getOneOf();
        CodegenModel composedModel = this.createComposedModel(ONE_OF_PREFFIX + codegenModel.getName(), schemas);
        if (composedModel == null) {
            schemas = composedSchema.getAnyOf();
            composedModel = this.createComposedModel(ANY_OF_PREFFIX + codegenModel.getName(), schemas);
            if (composedModel == null) {
		schemas = composedSchema.getAllOf();
		if(schemas == null || schemas.isEmpty()){
		    System.out.println("'processComposedSchemas' codegenModel composed ["+codegenModel.getName()+"] anyOf not found");
                    return null;
		}
		else{
		    System.out.println("'processComposedSchemas' codegenModel composed ["+codegenModel.getName()+"] allOf");
		    for (Schema schema : schemas) {
			System.out.println("'processComposedSchemas' codegenModel composed ["+codegenModel.getName()+"] schema '"+schema.getName()+"' type'"+schema.getClass().getName()+"'");
			final Map<String, Schema> properties = schema.getProperties();
        		if (properties == null || properties.isEmpty()) {
            		    System.out.println("'processComposedSchemas' codegenModel composed ["+codegenModel.getName()+"] schema '"+schema.getName()+"': no properties");
        		}
			else{
       			    for (String name : properties.keySet()) {
	    		        System.out.println("'processComposedSchemas' codegenModel composed ["+codegenModel.getName()+"] schema '"+schema.getName()+"' property '"+name+"'");
            			final Schema property = properties.get(name);

				final Optional<CodegenProperty> optionalCodegenProperty = codegenModel.getVars()
				   .stream()
				   .filter(codegenProperty -> codegenProperty.baseName.equals(name))
				   .findFirst();
				if (!optionalCodegenProperty.isPresent()) {
				    continue;
				}
		    		final CodegenProperty codegenProperty = optionalCodegenProperty.get();
				final String codegenPropertyName = codegenModel.getName() + codegenConfig.toModelName(codegenProperty.getName());
            			System.out.println("'processComposedSchemas' codegenModel composed ["+codegenModel.getName()+"] schema '"+schema.getName()+"' codegenProperty '"+codegenProperty.name+"' ["+codegenProperty.datatypeWithEnum+"]");

				if(property!=null && property instanceof ComposedSchema){
					ComposedSchema composedPropertySchema = (ComposedSchema) property;

					java.io.File fInterface = null;

					List<Schema> propertySchemas = composedPropertySchema.getOneOf();
					CodegenModel composedPropertyModel = this.createComposedModel(ONE_OF_PREFFIX + codegenPropertyName, propertySchemas);
					if(composedPropertyModel!=null){
					    if(!allModels.containsKey(composedPropertyModel.getName())){
						//System.out.println("'processComposedSchema' codegenModel composed ["+codegenModel.getName()+"] schema '"+schema.getName()+"' codegenProperty '"+codegenProperty.name+"' Registered new interface '"+composedPropertyModel.getName()+"'");
						//allModels.put(composedPropertyModel.getName(),composedPropertyModel);
						System.out.println("'processComposedSchema' codegenModel composed ["+codegenModel.getName()+"] schema '"+schema.getName()+"' codegenProperty '"+codegenProperty.name+"' Registered new interface '"+composedPropertyModel.getName()+"' ["+this.codegenConfig.modelPackage()+"]");
						StringBuilder bf = new StringBuilder();
						bf.append("package ").append(this.codegenConfig.modelPackage()).append(";\n\n");
						bf.append("/**\n");
						bf.append("* ").append(composedPropertyModel.getName()).append("\n");
						bf.append("*/\n");
						bf.append("public interface ").append(composedPropertyModel.getName()).append(" {\n");
						bf.append("\n");
						bf.append("}");
						try{
							fInterface = new java.io.File(this.codegenConfig.modelFileFolder(),composedPropertyModel.getName()+".java");
							mkdirParentDirectory(fInterface);
							java.io.FileOutputStream fos =new java.io.FileOutputStream(fInterface);
							fos.write(bf.toString().getBytes());
							fos.flush();
							fos.close();
						}catch(Exception e){
							System.out.println("'processComposedSchema' codegenModel composed ["+codegenModel.getName()+"] schema '"+schema.getName()+"' codegenProperty '"+codegenProperty.name+"' Registered new interface '"+composedPropertyModel.getName()+"' ERROR [outputDir:"+this.codegenConfig.modelFileFolder()+"]: "+e.getMessage());
							e.printStackTrace(System.out);
						}
					    }
					    // Fix: la classe stessa non deve contenenere l'implements. this.addInterfaceModel(codegenModel, composedPropertyModel);
        				    this.addInterfaces(propertySchemas, composedPropertyModel, allModels);
					    codegenProperty.datatype = composedPropertyModel.getClassname();
					    codegenProperty.datatypeWithEnum = composedPropertyModel.getClassname();
					    codegenProperty.baseType = composedPropertyModel.getClassname();
					    codegenProperty.complexType = composedPropertyModel.getClassname();
					    System.out.println("'processComposedSchema' codegenModel composed ["+codegenModel.getName()+"] schema '"+schema.getName()+"' codegenProperty '"+codegenProperty.name+"' Update ["+codegenProperty.datatypeWithEnum+"]");
					}
					if(codegenProperty.datatypeWithEnum!=null && codegenProperty.datatypeWithEnum.startsWith("OneOf")){
					    System.out.println("'processComposedSchema' codegenModel composed ["+codegenModel.getName()+"] schema '"+schema.getName()+"' codegenProperty '"+codegenProperty.name+"' is oneof");
		    			    codegenProperty.vendorExtensions.put("x-is-oneof", Boolean.TRUE);
					}
					
					if (composedPropertySchema.getDiscriminator() != null) {
					    System.out.println("'processComposedSchemas' codegenModel composed ["+codegenModel.getName()+"] schema '"+schema.getName()+"' codegenProperty '"+codegenProperty.name+"' discriminator '"+composedPropertySchema.getDiscriminator().getPropertyName()+"'");
					    codegenProperty.vendorExtensions.put("x-discriminator-enable", true);
					    String discriminatorName = composedPropertySchema.getDiscriminator().getPropertyName();
					    boolean externalDiscriminator = discriminatorName.startsWith("x-external-property-");
					    if(externalDiscriminator){
						discriminatorName = discriminatorName.substring("x-external-property-".length());
					    }
					    codegenProperty.vendorExtensions.put("x-discriminator-existing-property", !externalDiscriminator);
					    codegenProperty.vendorExtensions.put("x-discriminator-external-property", externalDiscriminator);
					    codegenProperty.vendorExtensions.put("x-discriminator-name", discriminatorName);

					    String classDiscriminator = null;
	    				    Map<String, String> map = composedPropertySchema.getDiscriminator().getMapping();
					    if(map!=null && !map.isEmpty()){
						List<Map<String, String>> enumVars = new ArrayList<Map<String, String>>();
						for (String key : map.keySet()) {
						    String value = map.get(key);
						    String schemasDecl = "components/schemas/";
						    String valueWithoutSchema = value; 
					    	    if(value.contains(schemasDecl)) {
					    		int indexOf = value.indexOf(schemasDecl);
					    		valueWithoutSchema = value.substring(indexOf+schemasDecl.length());
					    	    }
						    System.out.println("'processComposedSchemas' codegenModel composed ["+codegenModel.getName()+"] schema '"+schema.getName()+"' codegenProperty '"+codegenProperty.name+"' discriminator '"+composedPropertySchema.getDiscriminator().getPropertyName()+"' key["+key+"] value["+map.get(key)+"] valueWithoutSchema["+valueWithoutSchema+"]");
						    Map<String, String> enumVar = new java.util.HashMap<String, String>();
						    enumVar.put("x-discriminator-value", key);
					    	    enumVar.put("x-discriminator-class", valueWithoutSchema);
						    if(classDiscriminator==null){
						    	classDiscriminator = valueWithoutSchema;
						    }
						    enumVars.add(enumVar);
						}
						codegenProperty.vendorExtensions.put("x-discriminator-list", enumVars);
					    }

					    if(!externalDiscriminator){
					    	String nomeMetodoExistingProperty = "get"+DefaultCodegenConfig.camelize(discriminatorName, false);
						codegenProperty.vendorExtensions.put("x-discriminator-existing-property-method-name", nomeMetodoExistingProperty);

						if(classDiscriminator!=null && allModels.containsKey(classDiscriminator)){
							CodegenModel codegenModelDiscriminatorType = allModels.get(classDiscriminator);
							final String discriminatorNameSearch = discriminatorName;
							final Optional<CodegenProperty> optionalCodegenPropertyDiscriminator = codegenModelDiscriminatorType.getVars()
							   .stream()
							   .filter(codegenPropertyDiscriminator -> codegenPropertyDiscriminator.baseName.equals(discriminatorNameSearch))
							   .findFirst();
							if (optionalCodegenPropertyDiscriminator.isPresent()) {
						    		final CodegenProperty codegenPropertyDiscriminator = optionalCodegenPropertyDiscriminator.get();
								System.out.println("'processComposedSchemas' classDiscriminator ["+classDiscriminator+"] schema '"+schema.getName()+"' codegenProperty '"+codegenPropertyDiscriminator.name+"' ["+codegenPropertyDiscriminator.datatypeWithEnum+"] ["+codegenPropertyDiscriminator.datatype+"]");
								codegenProperty.vendorExtensions.put("x-discriminator-existing-property-method-type", codegenPropertyDiscriminator.datatype);
							}
							else{
								System.out.println("'processComposedSchemas' classDiscriminator ["+classDiscriminator+"] type:string (1)");
								codegenProperty.vendorExtensions.put("x-discriminator-existing-property-method-type", "String");
							}
						}
						else{
							System.out.println("'processComposedSchemas' classDiscriminator ["+classDiscriminator+"] type:string (2)");
							codegenProperty.vendorExtensions.put("x-discriminator-existing-property-method-type", "String");
						}

						if(fInterface!=null){
							StringBuilder bf = new StringBuilder();
							bf.append("package ").append(this.codegenConfig.modelPackage()).append(";\n\n");
							bf.append("/**\n");
							bf.append("* ").append(composedPropertyModel.getName()).append("\n");
							bf.append("*/\n");
							bf.append("public interface ").append(composedPropertyModel.getName()).append(" {\n");
							bf.append("    public ");
							bf.append(codegenProperty.vendorExtensions.get("x-discriminator-existing-property-method-type"));
							bf.append(" ");
							bf.append(codegenProperty.vendorExtensions.get("x-discriminator-existing-property-method-name"));
							bf.append("();");
							bf.append("\n");
							bf.append("}");
							try{
								java.io.FileOutputStream fos =new java.io.FileOutputStream(fInterface);
								fos.write(bf.toString().getBytes());
								fos.flush();
								fos.close();
							}catch(Exception e){
								System.out.println("'processComposedSchema' codegenModel composed ["+codegenModel.getName()+"] schema '"+schema.getName()+"' codegenProperty '"+codegenProperty.name+"' Registered new interface '"+composedPropertyModel.getName()+"' ERROR [outputDir:"+this.codegenConfig.modelFileFolder()+"]: "+e.getMessage());
								e.printStackTrace(System.out);
							}	
						}
					    }
					}
				}
			    }
			}
		    }
		    return null;
		}
	    }else{
		System.out.println("'processComposedSchemas' codegenModel composed ["+codegenModel.getName()+"] is anyOf");
	    }
        }
	else{
	    System.out.println("'processComposedSchemas' codegenModel composed ["+codegenModel.getName()+"] is oneOf");
	}

	if(schemas == null || schemas.isEmpty()){
		System.out.println("'processComposedSchemas' codegenModel composed ["+codegenModel.getName()+"] no schemas");
	}
	else{
            for (Schema schema : schemas) {
	        System.out.println("'processComposedSchemas' codegenModel composed ["+codegenModel.getName()+"] schema '"+schema.getName()+"' type'"+schema.getClass().getName()+"'");
	    }
	}

        this.addInterfaceModel(codegenModel, composedModel);
        this.addInterfaces(schemas, composedModel, allModels);
        return composedModel;
    }

    protected CodegenModel processComposedSchema(String name, ComposedSchema composedSchema, Map<String, CodegenModel> allModels) {
        System.out.println("'processComposedSchema' name ["+name+"]");
        List<Schema> schemas = composedSchema.getOneOf();
        CodegenModel composedModel = this.createComposedModel(ONE_OF_PREFFIX + name, schemas);
        if (composedModel == null) {
            System.out.println("'processComposedSchema' name ["+name+"] oneOf not found");
            schemas = composedSchema.getAnyOf();
            composedModel = this.createComposedModel(ANY_OF_PREFFIX + name, schemas);
            if (composedModel == null) {
                System.out.println("'processComposedSchema' name ["+name+"] anyOf not found");
                return null;
            }
	    else{
	        System.out.println("'processComposedSchema' name ["+name+"] is anyOf");
                //codegenProperty.vendorExtensions.put("x-is-anyof", Boolean.TRUE);
	    }
        }
	else{
	    System.out.println("'processComposedSchema' name ["+name+"] is oneOf");
            //codegenProperty.vendorExtensions.put("x-is-oneof", Boolean.TRUE);
	}
        this.addInterfaces(schemas, composedModel, allModels);

        return composedModel;
    }

    protected CodegenModel processComposedSchema(String codegenModelName, CodegenProperty codegenProperty, ComposedSchema composedSchema, Map<String, CodegenModel> allModels) {
        System.out.println("'processComposedSchema' Property ["+codegenProperty.name+"] ["+codegenProperty.datatypeWithEnum+"]");
        List<Schema> schemas = composedSchema.getOneOf();
        CodegenModel composedModel = this.createComposedModel(ONE_OF_PREFFIX + codegenModelName, schemas);
        if (composedModel == null) {
	    System.out.println("'processComposedSchema' Property ["+codegenProperty.name+"] oneOf not found");
            schemas = composedSchema.getAnyOf();
            composedModel = this.createComposedModel(ANY_OF_PREFFIX + codegenModelName, schemas);
            if (composedModel == null) {
                System.out.println("'processComposedSchema' Property ["+codegenProperty.name+"] anyOf not found");
            	return null;
            }
	    else{
	        System.out.println("'processComposedSchema' Property ["+codegenProperty.name+"] is anyOf");
	    }
        }
	else{
	    System.out.println("'processComposedSchema' Property ["+codegenProperty.name+"] is oneOf");
	}

	boolean doneInterfaceDiscriminator = false;
	if (composedSchema.getDiscriminator() != null) {
            System.out.println("'processComposedSchema' Property ["+codegenProperty.name+"] discriminator '"+composedSchema.getDiscriminator().getPropertyName()+"' before add Interfaces");
	    String discriminatorName = composedSchema.getDiscriminator().getPropertyName();
	    boolean externalDiscriminator = discriminatorName.startsWith("x-external-property-");
	    if(!externalDiscriminator){
		composedModel.vendorExtensions.put("x-discriminator-existing-property", !externalDiscriminator);
		codegenProperty.vendorExtensions.put("x-discriminator-existing-property", !externalDiscriminator);
		String nomeMetodoExistingProperty = "get"+DefaultCodegenConfig.camelize(discriminatorName, false);
		composedModel.vendorExtensions.put("x-discriminator-existing-property-method-name", nomeMetodoExistingProperty);
		codegenProperty.vendorExtensions.put("x-discriminator-existing-property-method-name", nomeMetodoExistingProperty);

	    	String classDiscriminator = null;
	    	Map<String, String> map = composedSchema.getDiscriminator().getMapping();
            	if(map!=null && !map.isEmpty()){
			List<Map<String, String>> enumVars = new ArrayList<Map<String, String>>();
			for (String key : map.keySet()) {
	            		String value = map.get(key);
		    		String schemasDecl = "components/schemas/";
		    		String valueWithoutSchema = value; 
	    	    		if(value.contains(schemasDecl)) {
	    				int indexOf = value.indexOf(schemasDecl);
	    				valueWithoutSchema = value.substring(indexOf+schemasDecl.length());
	    	    		}
		    		System.out.println("'processComposedSchema' Property ["+codegenProperty.name+"] discriminator '"+composedSchema.getDiscriminator().getPropertyName()+"' key["+key+"] value["+map.get(key)+"] valueWithoutSchema["+valueWithoutSchema+"] before add Interfaces");
	    	    		if(classDiscriminator==null){
		    			classDiscriminator = valueWithoutSchema;
		    		}
	        	}
	    	}

		if(classDiscriminator!=null && allModels.containsKey(classDiscriminator)){
			CodegenModel codegenModelDiscriminatorType = allModels.get(classDiscriminator);
			final String discriminatorNameSearch = discriminatorName;
			final Optional<CodegenProperty> optionalCodegenPropertyDiscriminator = codegenModelDiscriminatorType.getVars()
			   .stream()
			   .filter(codegenPropertyDiscriminator -> codegenPropertyDiscriminator.baseName.equals(discriminatorNameSearch))
			   .findFirst();
			if (optionalCodegenPropertyDiscriminator.isPresent()) {
		    		final CodegenProperty codegenPropertyDiscriminator = optionalCodegenPropertyDiscriminator.get();
				System.out.println("'processComposedSchemas' classDiscriminator ["+classDiscriminator+"] codegenProperty '"+codegenPropertyDiscriminator.name+"' ["+codegenPropertyDiscriminator.datatypeWithEnum+"] ["+codegenPropertyDiscriminator.datatype+"]");
				composedModel.vendorExtensions.put("x-discriminator-existing-property-method-type", codegenPropertyDiscriminator.datatype);
				codegenProperty.vendorExtensions.put("x-discriminator-existing-property-method-type", codegenPropertyDiscriminator.datatype);
				doneInterfaceDiscriminator=true;
			}
			else{
				System.out.println("'processComposedSchemas' classDiscriminator ["+classDiscriminator+"] type:string (1)");
				composedModel.vendorExtensions.put("x-discriminator-existing-property-method-type", "String");	
				codegenProperty.vendorExtensions.put("x-discriminator-existing-property-method-type", "String");	
				doneInterfaceDiscriminator=true;
			}
		}
		else{
			System.out.println("'processComposedSchemas' classDiscriminator ["+classDiscriminator+"] type:string (2)");
			composedModel.vendorExtensions.put("x-discriminator-existing-property-method-type", "String");
			codegenProperty.vendorExtensions.put("x-discriminator-existing-property-method-type", "String");
		}
	    }
	}

        this.addInterfaces(schemas, composedModel, allModels);

        codegenProperty.datatype = composedModel.getClassname();
        codegenProperty.datatypeWithEnum = composedModel.getClassname();
        codegenProperty.baseType = composedModel.getClassname();
        codegenProperty.complexType = composedModel.getClassname();

	System.out.println("'processComposedSchema' Update ["+codegenProperty.name+"] ["+codegenProperty.datatypeWithEnum+"]");


	if(codegenProperty.datatypeWithEnum!=null && codegenProperty.datatypeWithEnum.startsWith("OneOf")){
	    System.out.println("'processComposedSchema' Property ["+codegenProperty.name+"] is oneof");
	    composedModel.vendorExtensions.put("x-is-oneof", Boolean.TRUE);
	    codegenProperty.vendorExtensions.put("x-is-oneof", Boolean.TRUE);
	}
	if (composedSchema.getDiscriminator() != null) {
	    System.out.println("'processComposedSchema' Property ["+codegenProperty.name+"] discriminator '"+composedSchema.getDiscriminator().getPropertyName()+"'");
	    composedModel.vendorExtensions.put("x-discriminator-enable", true);
	    codegenProperty.vendorExtensions.put("x-discriminator-enable", true);
	    String discriminatorName = composedSchema.getDiscriminator().getPropertyName();
	    boolean externalDiscriminator = discriminatorName.startsWith("x-external-property-");
	    if(externalDiscriminator){
		discriminatorName = discriminatorName.substring("x-external-property-".length());
	    }
	    composedModel.vendorExtensions.put("x-discriminator-existing-property", !externalDiscriminator);
	    codegenProperty.vendorExtensions.put("x-discriminator-existing-property", !externalDiscriminator);
	    composedModel.vendorExtensions.put("x-discriminator-external-property", externalDiscriminator);
	    codegenProperty.vendorExtensions.put("x-discriminator-external-property", externalDiscriminator);
	    composedModel.vendorExtensions.put("x-discriminator-name", discriminatorName);
	    codegenProperty.vendorExtensions.put("x-discriminator-name", discriminatorName);
	    System.out.println("'processComposedSchema' Property ["+codegenProperty.name+"] discriminator '"+composedSchema.getDiscriminator().getPropertyName()+"' externalDiscriminator:'"+externalDiscriminator+"'");

	    String classDiscriminator = null;
	    Map<String, String> map = composedSchema.getDiscriminator().getMapping();
	    if(map!=null && !map.isEmpty()){
		List<Map<String, String>> enumVars = new ArrayList<Map<String, String>>();
		for (String key : map.keySet()) {
		    String value = map.get(key);
		    String schemasDecl = "components/schemas/";
		    String valueWithoutSchema = value; 
	    	    if(value.contains(schemasDecl)) {
	    		int indexOf = value.indexOf(schemasDecl);
	    		valueWithoutSchema = value.substring(indexOf+schemasDecl.length());
	    	    }
		    System.out.println("'processComposedSchema' Property ["+codegenProperty.name+"] discriminator '"+composedSchema.getDiscriminator().getPropertyName()+"' key["+key+"] value["+map.get(key)+"] valueWithoutSchema["+valueWithoutSchema+"]");
	    	    Map<String, String> enumVar = new java.util.HashMap<String, String>();
		    enumVar.put("x-discriminator-value", key);
	    	    enumVar.put("x-discriminator-class", valueWithoutSchema);
		    if(classDiscriminator!=null){
		    	classDiscriminator = valueWithoutSchema;
		    }
		    enumVars.add(enumVar);
		}
		composedModel.vendorExtensions.put("x-discriminator-list", enumVars);
		codegenProperty.vendorExtensions.put("x-discriminator-list", enumVars);
	    }

	    if(!doneInterfaceDiscriminator){

		    if(!externalDiscriminator){

			System.out.println("'processComposedSchemas' classDiscriminator ["+classDiscriminator+"] (!externalDiscriminator) ... ");

		    	String nomeMetodoExistingProperty = "get"+DefaultCodegenConfig.camelize(discriminatorName, false);
			composedModel.vendorExtensions.put("x-discriminator-existing-property-method-name", nomeMetodoExistingProperty);
			codegenProperty.vendorExtensions.put("x-discriminator-existing-property-method-name", nomeMetodoExistingProperty);
			if(classDiscriminator!=null && allModels.containsKey(classDiscriminator)){
				CodegenModel codegenModelDiscriminatorType = allModels.get(classDiscriminator);
				final String discriminatorNameSearch = discriminatorName;
				final Optional<CodegenProperty> optionalCodegenPropertyDiscriminator = codegenModelDiscriminatorType.getVars()
				   .stream()
				   .filter(codegenPropertyDiscriminator -> codegenPropertyDiscriminator.baseName.equals(discriminatorNameSearch))
				   .findFirst();
				if (optionalCodegenPropertyDiscriminator.isPresent()) {
			    		final CodegenProperty codegenPropertyDiscriminator = optionalCodegenPropertyDiscriminator.get();
					System.out.println("'processComposedSchemas' classDiscriminator ["+classDiscriminator+"] codegenProperty '"+codegenPropertyDiscriminator.name+"' ["+codegenPropertyDiscriminator.datatypeWithEnum+"] ["+codegenPropertyDiscriminator.datatype+"]");
					composedModel.vendorExtensions.put("x-discriminator-existing-property-method-type", codegenPropertyDiscriminator.datatype);
					codegenProperty.vendorExtensions.put("x-discriminator-existing-property-method-type", codegenPropertyDiscriminator.datatype);
				}
				else{
					System.out.println("'processComposedSchemas' classDiscriminator ["+classDiscriminator+"] type:string (1)");
					composedModel.vendorExtensions.put("x-discriminator-existing-property-method-type", "String");	
					codegenProperty.vendorExtensions.put("x-discriminator-existing-property-method-type", "String");	
				}
			}
			else{
				System.out.println("'processComposedSchemas' classDiscriminator ["+classDiscriminator+"] type:string (2)");
				composedModel.vendorExtensions.put("x-discriminator-existing-property-method-type", "String");
				codegenProperty.vendorExtensions.put("x-discriminator-existing-property-method-type", "String");
			}
		    }

		    // Il file viene sovrascritto dopo
		    //if(!externalDiscriminator){
		//	java.io.File fInterface = null;
		//	try{
		//		fInterface = new java.io.File(this.codegenConfig.modelFileFolder(),codegenProperty.datatypeWithEnum+".java");
		//		if(fInterface.exists()){
		//			System.out.println("'processComposedSchema' Property ["+codegenProperty.name+"] discriminator '"+composedSchema.getDiscriminator().getPropertyName()+"'  already exists '"+fInterface.getAbsolutePath()+"'");
		//		}
		//		else{
		//			System.out.println("'processComposedSchema' Property ["+codegenProperty.name+"] discriminator '"+composedSchema.getDiscriminator().getPropertyName()+"'  not exists '"+fInterface.getAbsolutePath()+"'");
		//			mkdirParentDirectory(fInterface);
		//		}

		//		StringBuilder bf = new StringBuilder();
		//		bf.append("package ").append(this.codegenConfig.modelPackage()).append(";\n\n");
		//		bf.append("/**\n");
		//		bf.append("* ").append(codegenProperty.datatypeWithEnum).append("\n");
		//		bf.append("*/\n");
		//		bf.append("public interface ").append(codegenProperty.datatypeWithEnum).append(" {\n");
		//		bf.append("    public ");
		//		bf.append(composedModel.vendorExtensions.get("x-discriminator-existing-property-method-type"));
		//		bf.append(" ");
		//		bf.append(composedModel.vendorExtensions.get("x-discriminator-existing-property-method-name"));
		//		bf.append("();");
		//		bf.append("\n");
		//		bf.append("}");
//
//				java.io.FileOutputStream fos =new java.io.FileOutputStream(fInterface);
//				fos.write(bf.toString().getBytes());
//				fos.flush();
//				fos.close();
//
//				System.out.println("Scritto in '"+fInterface.getAbsolutePath()+"' : "+bf.toString());
//
//			}catch(Exception e){
//				System.out.println("'processComposedSchema' Property ["+codegenProperty.name+"] discriminator '"+composedSchema.getDiscriminator().getPropertyName()+"' ERROR [outputDir:"+this.codegenConfig.modelFileFolder()+"]: "+e.getMessage());
//				e.printStackTrace(System.out);
//			}
//		    }
		}
        }

        return composedModel;
    }

    protected CodegenModel processArrayItemSchema(CodegenModel codegenModel, ArraySchema arraySchema, Map<String, CodegenModel> allModels) {
        final Schema itemsSchema = arraySchema.getItems();
        if (itemsSchema instanceof ComposedSchema) {
            final CodegenModel composedModel = this.processComposedSchema(codegenModel.name + ARRAY_ITEMS_SUFFIX, (ComposedSchema) itemsSchema, allModels);
            this.updateArrayModel(codegenModel, composedModel.name, arraySchema);
            return composedModel;
        }
        return null;
    }

    protected CodegenModel processArrayItemSchema(String codegenModelName, CodegenProperty codegenProperty, ArraySchema arraySchema, Map<String, CodegenModel> allModels) {
        final Schema itemsSchema = arraySchema.getItems();
        if (itemsSchema instanceof ComposedSchema) {
            final CodegenModel composedModel = this.processComposedSchema(codegenModelName + ARRAY_ITEMS_SUFFIX, codegenProperty.items, (ComposedSchema) itemsSchema, allModels);
            if (composedModel == null) {
                return null;
            }
            this.updatePropertyDataType(codegenProperty, composedModel.name, arraySchema);
            return composedModel;
        }
        return null;
    }

    protected CodegenModel createComposedModel(String name, List<Schema> schemas) {
        if (schemas == null || schemas.isEmpty()) {
            return null;
        }
        final CodegenModel composedModel = CodegenModelFactory.newInstance(CodegenModelType.MODEL);
        composedModel.setIsComposedModel(true);
        composedModel.setInterfaces(new ArrayList<>());
        this.configureModel(composedModel, name);

        return composedModel;
    }

    protected void addInterfaceModel(CodegenModel codegenModel, CodegenModel interfaceModel) {
        if (codegenModel == null) {
            return;
        }
        if (codegenModel.getInterfaceModels() == null) {
            codegenModel.setInterfaceModels(new ArrayList<>());
        }

	boolean found = false;
	for(CodegenModel interfaceModelCheck: codegenModel.getInterfaceModels()){
		if(interfaceModelCheck.getName().equals(interfaceModel.getName())){
			found=true;
			break;
		}
	}
	if(!found){
        	codegenModel.getInterfaceModels().add(interfaceModel);
	}
    }

    protected void addInterfaces(List<Schema> schemas, CodegenModel codegenModel, Map<String, CodegenModel> allModels) {
        for (Schema interfaceSchema : schemas) {
            final String ref = interfaceSchema.get$ref();
            if (StringUtils.isBlank(ref)) {
                continue;
            }
            final String schemaName = ref.substring(ref.lastIndexOf("/") + 1);
            this.addInterfaceModel(allModels.get(codegenConfig.toModelName(schemaName)), codegenModel);
        }
    }

    protected void configureModel(CodegenModel codegenModel, String name) {
        codegenModel.name = name;
        codegenModel.classname = codegenConfig.toModelName(name);
        codegenModel.classVarName = codegenConfig.toVarName(name);
        codegenModel.classFilename = codegenConfig.toModelFilename(name);
    }

    protected boolean hasNonObjectSchema(List<Schema> schemas) {
        for  (Schema schema : schemas) {
            if (!codegenConfig.isObjectSchema(schema)) {
                return true;
            }
        }
        return false;
    }

    protected void addComposedModel(CodegenModel composedModel) {
        if (composedModel == null) {
            return;
        }
        this.composedModels.add(composedModel);
    }

    protected void updatePropertyDataType(CodegenProperty codegenProperty, String schemaName, ArraySchema arraySchema) {
        final Schema items = arraySchema.getItems();
        final Schema refSchema = new Schema();
        refSchema.set$ref("#/components/schemas/" + schemaName);
        arraySchema.setItems(refSchema);
        codegenProperty.setDatatype(this.codegenConfig.getTypeDeclaration(arraySchema));
        codegenProperty.setDatatypeWithEnum(codegenProperty.getDatatype());

        codegenProperty.defaultValue = this.codegenConfig.toDefaultValue(arraySchema);
        codegenProperty.defaultValueWithParam = this.codegenConfig.toDefaultValueWithParam(codegenProperty.baseName, arraySchema);

        arraySchema.setItems(items);
    }

    protected void updateArrayModel(CodegenModel codegenModel, String schemaName, ArraySchema arraySchema) {
        final Schema items = arraySchema.getItems();
        final Schema refSchema = new Schema();
        refSchema.set$ref("#/components/schemas/" + schemaName);
        arraySchema.setItems(refSchema);

        this.codegenConfig.addParentContainer(codegenModel, codegenModel.name, arraySchema);
        codegenModel.defaultValue = this.codegenConfig.toDefaultValue(arraySchema);
        codegenModel.arrayModelType = this.codegenConfig.fromProperty(codegenModel.name, arraySchema).complexType;

        arraySchema.setItems(items);
    }
}
