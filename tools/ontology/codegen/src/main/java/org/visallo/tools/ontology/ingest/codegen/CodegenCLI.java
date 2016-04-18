package org.visallo.tools.ontology.ingest.codegen;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;
import com.google.common.base.Strings;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.visallo.core.cmdline.CommandLineTool;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.tools.ontology.ingest.common.BaseConceptBuilder;
import org.visallo.tools.ontology.ingest.common.BaseEntityBuilder;
import org.visallo.tools.ontology.ingest.common.BaseRelationshipBuilder;
import org.visallo.web.clientapi.JsonUtil;
import org.visallo.web.clientapi.UserNameAndPasswordVisalloApi;
import org.visallo.web.clientapi.VisalloApi;
import org.visallo.web.clientapi.model.ClientApiOntology;
import org.visallo.web.clientapi.model.PropertyType;

@Parameters(commandDescription = "Generate model classes based on a Visallo ontology.")
public class CodegenCLI extends CommandLineTool {
  private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(CodegenCLI.class);
  private static final Pattern IRI_FORMAT = Pattern.compile("^http://(.+)#(.+)$");
  private static final Pattern CLASSNAME_PART_MATCHER = Pattern.compile("^[a-zA-Z0-9]+$");
  private static final Class PROPERTY_ADDITION_CLASS = BaseEntityBuilder.PropertyAddition.class;

  @Parameter(names = {"--inputJsonFile", "-f"}, arity = 1, converter = FileConverter.class, description = "The path to a local json file containing the Visallo ontology.")
  private File inputJsonFile;

  @Parameter(names = {"--visalloUrl", "-url"}, arity = 1, description = "The root URL of the Visallo instance from which to download the ontology.")
  private String visalloUrl;

  @Parameter(names = {"--visalloUsername", "-u"}, arity = 1, description = "The username to authenticate as when downloading the ontology from the Visallo instance.")
  private String visalloUsername;

  @Parameter(names = {"--visalloPassword", "-p"}, arity = 1, description = "The password to authenticate with when downloading the ontology from the Visallo instance.")
  private String visalloPassword;

  @Parameter(names = {"--outputDirectory", "-o"}, arity = 1, required = true, description = "The path to the output directory for the class files. If it does not exist, it will be created.")
  private String outputDirectory;

  public static void main(String[] args) throws Exception {
    CommandLineTool.main(new CodegenCLI(), args, false);
  }

  @Override
  protected int run() throws Exception {
    String ontologyJsonString;
    if (inputJsonFile != null) {
      ontologyJsonString = new String(Files.readAllBytes(inputJsonFile.toPath()), Charset.forName("UTF-8"));
    } else if (!Strings.isNullOrEmpty(visalloUrl) && !Strings.isNullOrEmpty(visalloUsername) && !Strings.isNullOrEmpty(visalloPassword)) {
      VisalloApi visalloApi = new UserNameAndPasswordVisalloApi(visalloUrl, visalloUsername,  visalloPassword, true);
      ontologyJsonString = visalloApi.invokeAPI("/ontology", "GET", null, null, null, null, "application/json");
      visalloApi.logout();
    } else {
      throw new VisalloException("inputJsonFile or visalloUrl, visalloUsername, and visalloPassword parameters are required");
    }

    ClientApiOntology ontology = JsonUtil.getJsonMapper().readValue(ontologyJsonString, ClientApiOntology.class);

    Map<String, ClientApiOntology.Property> propertyMap = new HashMap<>();
    ontology.getProperties().forEach(property -> propertyMap.put(property.getTitle(), property));

    ontology.getConcepts().forEach(concept -> {
      try {
        List<ClientApiOntology.Property> conceptProperties = findPropertiesByIri(propertyMap, concept.getProperties());
        createConceptClass(concept, conceptProperties);
      } catch (IOException ioe) {
        throw new VisalloException("Unable to create concept class", ioe);
      }
    });

    ontology.getRelationships().forEach(relationship -> {
      try {
        List<ClientApiOntology.Property> relationshipProperties = findPropertiesByIri(propertyMap, relationship.getProperties());
        createRelationshipClass(relationship, relationshipProperties);
      } catch (IOException ioe) {
        throw new VisalloException("Unable to create concept class", ioe);
      }
    });

    return 0;
  }

  protected void createConceptClass(ClientApiOntology.Concept concept, List<ClientApiOntology.Property> conceptProperties) throws IOException {
    String conceptPackage = packageNameFromIri(concept.getId());
    if (conceptPackage != null) {
      String conceptClassName = classNameFromIri(concept.getId());

      // Don't expose the visallo internal concepts to the generated code
      if (conceptPackage.startsWith("org.visallo") && !conceptClassName.equals("Root")) {
        return;
      }

      LOGGER.debug("Create concept %s.%s", conceptPackage, conceptClassName);

      try (PrintWriter writer = createWriter(conceptPackage, conceptClassName)) {
        String parentClass = BaseConceptBuilder.class.getSimpleName();
        if (!Strings.isNullOrEmpty(concept.getParentConcept())) {
          parentClass = packageNameFromIri(concept.getParentConcept()) + "." + classNameFromIri(concept.getParentConcept());
        }

        writeClass(writer, conceptPackage, conceptClassName, parentClass, concept.getId(), conceptProperties, null);
      }
    }
  }

  protected void createRelationshipClass(ClientApiOntology.Relationship relationship, List<ClientApiOntology.Property> relationshipProperties) throws IOException {
    String relationshipPackage = packageNameFromIri(relationship.getTitle());
    if (relationshipPackage != null) {
      String relationshipClassName = classNameFromIri(relationship.getTitle());

      // Don't expose the visallo internal relationships to the generated code
      if (relationshipPackage.startsWith("org.visallo")) {
        return;
      }

      LOGGER.debug("Create relationship %s.%s", relationshipPackage, relationshipClassName);

      try (PrintWriter writer = createWriter(relationshipPackage, relationshipClassName)) {
        Consumer<PrintWriter> constructorWriter = methodWriter -> {
          relationship.getDomainConceptIris().forEach(outConceptIri -> {
            String outVertexClassName = packageNameFromIri(outConceptIri) + "." + classNameFromIri(outConceptIri);
            relationship.getRangeConceptIris().forEach(inConceptIri -> {
              String inVertexClassName = packageNameFromIri(inConceptIri) + "." + classNameFromIri(inConceptIri);
              methodWriter.println();
              methodWriter.println("  public " + relationshipClassName + "(String id, " + outVertexClassName + " outVertex, " + inVertexClassName + " inVertex) {");
              methodWriter.println("    super(id);");
              methodWriter.println("    this.setOutVertexId(outVertex.getId());");
              methodWriter.println("    this.setInVertexId(inVertex.getId());");
              methodWriter.println("  } ");
            });
          });
        };

        writeClass(writer, relationshipPackage, relationshipClassName, BaseRelationshipBuilder.class.getName(), relationship.getTitle(), relationshipProperties, constructorWriter);
      }
    }
  }

  protected PrintWriter createWriter(String packageName, String className) throws IOException {
    Path packagePath = Paths.get(outputDirectory, packageName.replaceAll("\\.", "/"));
    Files.createDirectories(packagePath);

    Path javaFileName = packagePath.resolve(className + ".java");
    return new PrintWriter(Files.newBufferedWriter(javaFileName, Charset.forName("UTF-8")));
  }

  protected void writeClass(
      PrintWriter writer,
      String packageName,
      String className,
      String parentClass,
      String iri,
      List<ClientApiOntology.Property> properties,
      Consumer<PrintWriter> constructorProvider) {

    writer.println("package " + packageName + ";");
    writer.println();

    // A little heavy handed to include all of these,
    // but it's simpler than trying to track which ones are actually needed
    // TODO: Only included the required imports
    writer.println("import java.math.BigDecimal;");
    writer.println("import java.text.SimpleDateFormat;");
    writer.println("import java.util.Date;");
    writer.println("import java.util.Set;");
    writer.println("import java.util.HashSet;");
    writer.println("import org.vertexium.type.GeoPoint;");
    writer.println("import org.visallo.core.model.properties.types.*;");
    writer.println("import " + BaseEntityBuilder.class.getName() + ";");
    writer.println("import " + BaseConceptBuilder.class.getName() + ";");
    writer.println("import " + BaseRelationshipBuilder.class.getName() + ";");
    writer.println("import " + BaseEntityBuilder.class.getName() + "." + PROPERTY_ADDITION_CLASS.getSimpleName() + ";");
    writer.println();

    writer.println("public class " + className + " extends " + parentClass + " {");
    writer.println("  public static final String IRI = \"" + iri + "\";");
    writer.println();
    if (constructorProvider != null) {
      constructorProvider.accept(writer);
    } else {
      writer.println("  public " + className + "(String id) { super(id); }");
    }
    writer.println();
    writer.println("  public String getIri() { return IRI; }");

    writePropertyMethods(writer, properties);

    writer.println('}');
  }

  protected void writePropertyMethods(PrintWriter writer, List<ClientApiOntology.Property> properties) {
    properties.forEach(property -> {
      String upperCamelCasePropertyName = classNameFromIri(property.getTitle());
      String constantName = constantNameFromClassName(upperCamelCasePropertyName);

      String propertyType = PropertyType.getTypeClass(property.getDataType()).getSimpleName();
      if (propertyType.equals("BigDecimal")) {
        propertyType = "Double";
      }

      String propertyAdditionType = PROPERTY_ADDITION_CLASS.getSimpleName() + "<" + propertyType + ">";
      String visalloPropertyType = (propertyType.equals("byte[]") ? "ByteArray" : propertyType) + "VisalloProperty";
      String helperMethodName = propertyType.equals("byte[]") ? "addByteArrayProperty" : "add" + propertyType + "Property";

      writer.println();
      writer.println("  public static final " + visalloPropertyType + " " + constantName + " = new " + visalloPropertyType + "(\"" + property.getTitle() + "\");");
      if (propertyType.equals("Date")) {
        writer.println("  public " + propertyAdditionType +
            " set" + upperCamelCasePropertyName + "(Object value, SimpleDateFormat dateFormat) { return add" + upperCamelCasePropertyName + "(\"\", value, dateFormat); }");
        writer.println("  public " + propertyAdditionType +
            " add" + upperCamelCasePropertyName + "(String key, Object value, SimpleDateFormat dateFormat)" +
            " { return " + helperMethodName + "(" + constantName + ".getPropertyName(), key, value, dateFormat); }");
      } else {
        writer.println("  public " + propertyAdditionType +
            " set" + upperCamelCasePropertyName + "(Object value) { return add" + upperCamelCasePropertyName + "(\"\", value); }");
        writer.println("  public " + propertyAdditionType +
            " add" + upperCamelCasePropertyName + "(String key, Object value)" +
            " { return " + helperMethodName + "(" + constantName + ".getPropertyName(), key, value); }");
      }

      LOGGER.debug("  %s property %s", propertyType, upperCamelCasePropertyName);
    });
  }

  private List<ClientApiOntology.Property> findPropertiesByIri(Map<String, ClientApiOntology.Property> propertyMap, List<String> propertyIris) {
    return propertyIris.stream().sorted()
        .map(propertyIri -> {
          ClientApiOntology.Property property = propertyMap.get(propertyIri);
          if (property == null) throw new VisalloException("Unable to locate property for iri: " + propertyIri);
          return property;
        })
        .collect(Collectors.toList());
  }

  private String constantNameFromClassName(String className) {
    String[] classNameParts = StringUtils.splitByCharacterTypeCamelCase(className);
    return Arrays.stream(classNameParts)
        .map(String::toUpperCase)
        .collect(Collectors.joining("_"));
  }


  private String classNameFromIri(String iri) {
    Matcher matcher = IRI_FORMAT.matcher(iri);
    if (matcher.matches()) {
      String[] classNameParts = StringUtils.splitByCharacterTypeCamelCase(matcher.group(2));
      return Arrays.stream(classNameParts)
          .filter(classNamePart -> CLASSNAME_PART_MATCHER.matcher(classNamePart).matches())
          .map(StringUtils::capitalize)
          .collect(Collectors.joining(""));
    } else {
      LOGGER.error("Unsupported iri pattern %s", iri);
    }
    return null;
  }

  private String packageNameFromIri(String iri) {
    Matcher matcher = IRI_FORMAT.matcher(iri);
    if (matcher.matches()) {
      String[] baseIriParts = matcher.group(1).split("/", -1);

      String[] packageParts = baseIriParts[0].split("\\.", -1);
      ArrayUtils.reverse(packageParts);
      if (baseIriParts.length > 1) {
        packageParts = (String[]) ArrayUtils.addAll(packageParts, ArrayUtils.subarray(baseIriParts, 1, baseIriParts.length));
      }

      String packageName = String.join(".", packageParts);
      if (packageName.toLowerCase().equals("org.w3.www.2002.07.owl")) {
        packageName = "org.w3.www.owl";
      }
      return packageName;
    } else {
      LOGGER.error("Unsupported iri pattern %s", iri);
    }
    return null;
  }
}
