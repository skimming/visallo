package org.visallo.tools.ontology.ingest.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import org.vertexium.*;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.ontology.*;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.PropertyType;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.*;

public class IngestRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(IngestRepository.class);

    private Graph graph;
    private UserRepository userRepository;
    private VisibilityTranslator visibilityTranslator;
    private OntologyRepository ontologyRepository;

    private Set<Class> verifiedClasses = new HashSet<>();
    private Set<String> verifiedClassProperties = new HashSet<>();

    private Map<String, Object> defaultMetadata;
    private Long defaultTimestamp;
    private Visibility defaultVisibility;

    public IngestRepository withDefaultMetadata(Map<String, Object> metdata) {
        this.defaultMetadata = metdata;
        return this;
    }

    public IngestRepository withDefaultTimestamp(Long timestamp) {
        this.defaultTimestamp = timestamp;
        return this;
    }

    public IngestRepository withDefaultVisibility(String visibility) {
        this.defaultVisibility = visibilityTranslator.toVisibility(visibility).getVisibility();
        return this;
    }

    public Map<String, Object> getDefaultMetadata() {
        return defaultMetadata;
    }

    public Long getDefaultTimestamp() {
        return defaultTimestamp;
    }

    public Visibility getDefaultVisibility() {
        return defaultVisibility;
    }

    @Inject
    public IngestRepository(
            Graph graph,
            UserRepository userRepository,
            VisibilityTranslator visibilityTranslator,
            OntologyRepository ontologyRepository
    ) throws JsonProcessingException {
        this.graph = graph;
        this.userRepository = userRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.ontologyRepository = ontologyRepository;
    }

    public List<Element> save(EntityBuilder... builders) {
        return save(Arrays.asList(builders));
    }

    public List<Element> save(Collection<EntityBuilder> builders) {
        LOGGER.debug("Saving %d entities", builders.size());

        // Validate the entities
        for (EntityBuilder builder : builders) {
            ValidationResult validationResult = (builder instanceof ConceptBuilder) ?
                    validateConceptBuilder((ConceptBuilder) builder) :
                    validateRelationshipBuilder((RelationshipBuilder)builder);
            if (!validationResult.isValid()) {
                throw new VisalloException(validationResult.getValidationError());
            }
        }

        // If we got to here, everything must be valid. Save the entities.
        List<Element> elements = new ArrayList<>();
        for (EntityBuilder builder : builders) {
            if (builder instanceof ConceptBuilder) {
                elements.add(save((ConceptBuilder) builder));
            } else {
                elements.add(save((RelationshipBuilder) builder));
            }
        }
        return elements;
    }

    public void flush() {
        graph.flush();
    }

    private Vertex save(ConceptBuilder conceptBuilder) {
        Visibility conceptVisibility = getVisibility(conceptBuilder.getVisibility());
        VertexBuilder vertexBuilder = graph.prepareVertex(conceptBuilder.getId(), getTimestamp(conceptBuilder.getTimestamp()), conceptVisibility);
        vertexBuilder.setProperty(VisalloProperties.CONCEPT_TYPE.getPropertyName(), conceptBuilder.getIri(), conceptVisibility);

        addProperties(vertexBuilder, conceptBuilder);

        LOGGER.trace("Saving vertex: %s", vertexBuilder.getVertexId());
        return vertexBuilder.save(getAuthorizations());
    }

    private Edge save(RelationshipBuilder relationshipBuilder) {
        Visibility relationshipVisibility = getVisibility(relationshipBuilder.getVisibility());
        EdgeBuilderByVertexId edgeBuilder = graph.prepareEdge(
                relationshipBuilder.getId(),
                relationshipBuilder.getOutVertexId(),
                relationshipBuilder.getInVertexId(),
                relationshipBuilder.getIri(),
                getTimestamp(relationshipBuilder.getTimestamp()),
                relationshipVisibility
        );

        addProperties(edgeBuilder, relationshipBuilder);

        LOGGER.trace("Saving edge: %s", edgeBuilder.getEdgeId());
        return edgeBuilder.save(getAuthorizations());
    }

    private void addProperties(ElementBuilder elementBuilder, EntityBuilder entityBuilder) {
        for (PropertyAddition<?> propertyAddition : entityBuilder.getPropertyAdditions()) {
            if (propertyAddition.getValue() != null) {
                elementBuilder.addPropertyValue(
                        propertyAddition.getKey(),
                        propertyAddition.getIri(),
                        propertyAddition.getValue(),
                        buildMetadata(propertyAddition.getMetadata(), propertyAddition.getVisibility()),
                        getTimestamp(propertyAddition.getTimestamp()),
                        getVisibility(propertyAddition.getVisibility())
                );
            }
        }
    }

    public ValidationResult validateConceptBuilder(ConceptBuilder builder) {
        if (!verifiedClasses.contains(builder.getClass())) {
            Concept concept = ontologyRepository.getConceptByIRI(builder.getIri());
            if (concept == null) {
                return new ValidationResult("Concept class: " + builder.getClass().getName() + " IRI: " + builder.getIri() + " is invalid");
            }

            verifiedClasses.add(builder.getClass());
        }

        return validateProperties(builder, builder.getPropertyAdditions());
    }

    public ValidationResult validateRelationshipBuilder(RelationshipBuilder builder) {
        if (verifiedClasses.contains(builder.getClass())) {
            return ValidationResult.VALID_RESULT;
        }

        Relationship relationship = ontologyRepository.getRelationshipByIRI(builder.getIri());
        if (relationship == null) {
            return new ValidationResult("Relationship class: " + builder.getClass().getName() + " IRI: " + builder.getIri() + " is invalid");
        }

        List<String> domainConceptIRIs = relationship.getDomainConceptIRIs();
        List<String> rangeConceptIRIs = relationship.getRangeConceptIRIs();

        try {
            for (Constructor constructor : builder.getClass().getConstructors()) {
                Class[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 3) {
                    String outConceptIRI = (String) parameterTypes[1].getField("IRI").get(null);
                    if (!domainConceptIRIs.contains(outConceptIRI)) {
                        return new ValidationResult("Out vertex Concept IRI: " + outConceptIRI + " is invalid");
                    }

                    String inConceptIRI = (String) parameterTypes[2].getField("IRI").get(null);
                    if (!rangeConceptIRIs.contains(inConceptIRI)) {
                        return new ValidationResult("In vertex Concept IRI: " + inConceptIRI + " is invalid");
                    }
                } else {
                    LOGGER.warn("Unsupported Constructor found: " + constructor.toString());
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new VisalloException("Failed to get IRI for in/out Concept class", e);
        }

        verifiedClasses.add(builder.getClass());

        return validateProperties(builder, builder.getPropertyAdditions());
    }

    private ValidationResult validateProperties(EntityBuilder entityBuilder, Set<PropertyAddition<?>> propertyAdditions) {
        return propertyAdditions.stream()
                .map(propertyAddition -> validateProperty(entityBuilder, propertyAddition))
                .filter(validationResult -> !validationResult.isValid())
                .findFirst().orElse(ValidationResult.VALID_RESULT);
    }

    private ValidationResult validateProperty(EntityBuilder entityBuilder, PropertyAddition propertyAddition) {
        if (!verifiedClassProperties.contains(getKey(entityBuilder, propertyAddition))) {
            HasOntologyProperties conceptOrRelationship;
            if (entityBuilder instanceof ConceptBuilder) {
                conceptOrRelationship = ontologyRepository.getConceptByIRI(entityBuilder.getIri());
            } else if (entityBuilder instanceof RelationshipBuilder) {
                conceptOrRelationship = ontologyRepository.getRelationshipByIRI(entityBuilder.getIri());
            } else {
                return new ValidationResult("Unexpected type: " + entityBuilder.getClass().getName());
            }

            if (conceptOrRelationship == null) {
                return new ValidationResult("Entity: " + entityBuilder.getIri() + " does not exist in the ontology");
            }

            OntologyProperty property = ontologyRepository.getPropertyByIRI(propertyAddition.getIri());
            if (property == null) {
                return new ValidationResult("Property: " + propertyAddition.getIri() + " does not exist in the ontology");
            }

            if (!isPropertyValidForEntity(conceptOrRelationship, property)) {
                return new ValidationResult("Property: " + propertyAddition.getIri() + " is invalid for class (or its ancestors): " + entityBuilder.getClass().getName());
            }

            if (propertyAddition.getValue() != null) {
                Class<?> valueType = propertyAddition.getValue().getClass();
                Class propertyType = PropertyType.getTypeClass(property.getDataType());
                if (propertyType.equals(BigDecimal.class)) {
                    propertyType = Double.class;
                }
                if (!valueType.isAssignableFrom(propertyType)) {
                    return new ValidationResult("Property: " + propertyAddition.getIri() + " type: " + valueType.getSimpleName() + " is invalid for Relationship class: " + entityBuilder.getClass().getName());
                }

                verifiedClassProperties.contains(getKey(entityBuilder, propertyAddition));
            }
        }

        return ValidationResult.VALID_RESULT;
    }

    private boolean isPropertyValidForEntity(HasOntologyProperties entity, OntologyProperty property) {
        if (!entity.getProperties().contains(property)) {
            if (entity instanceof Concept) {
                Concept parentConcept = ontologyRepository.getParentConcept((Concept)entity);
                if (parentConcept != null) {
                    return isPropertyValidForEntity(parentConcept, property);
                }
            }
            return false;
        }
        return true;
    }

    private String getKey(EntityBuilder entityBuilder, PropertyAddition propertyAddition) {
        return entityBuilder.getIri() + ":" + propertyAddition.getIri();
    }

    private Long getTimestamp(Long timestamp) {
        if (timestamp != null) {
            return timestamp;
        } else if (defaultTimestamp != null) {
            return defaultTimestamp;
        }
        return null;
    }

    private Visibility getVisibility(String visibilitySource) {
        if (visibilitySource != null) {
            return visibilityTranslator.toVisibility(visibilitySource).getVisibility();
        } else if (defaultVisibility != null) {
            return defaultVisibility;
        }
        return visibilityTranslator.getDefaultVisibility();
    }

    private Metadata buildMetadata(Map<String, Object> map, String visibilitySource) {
        Metadata metadata = new Metadata();

        Date now = new Date();
        Visibility defaultVisibility = visibilityTranslator.getDefaultVisibility();
        VisalloProperties.MODIFIED_DATE_METADATA.setMetadata(metadata, now, defaultVisibility);
        VisalloProperties.MODIFIED_BY_METADATA.setMetadata(metadata, getIngestUser().getUserId(), defaultVisibility);
        VisalloProperties.CONFIDENCE_METADATA.setMetadata(metadata, GraphRepository.SET_PROPERTY_CONFIDENCE, defaultVisibility);
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, new VisibilityJson(visibilitySource), defaultVisibility);

        if (defaultMetadata != null) {
            defaultMetadata.forEach((k, v) -> metadata.add(k, v, defaultVisibility));
        }

        if (map != null) {
            map.forEach((k, v) -> metadata.add(k, v, defaultVisibility));
        }

        return metadata;
    }

    private Authorizations getAuthorizations() {
        return userRepository.getAuthorizations(getIngestUser());
    }

    private User getIngestUser() {
        return userRepository.getSystemUser();
    }

    public static class ValidationResult {
        public static final ValidationResult VALID_RESULT = new ValidationResult(true, null);

        private boolean valid = false;
        private String validationError;

        public ValidationResult(String validationError) {
            this.validationError = validationError;
        }

        public ValidationResult(boolean valid, String validationError) {
            this.valid = valid;
            this.validationError = validationError;
        }

        public boolean isValid() {
            return valid;
        }

        public String getValidationError() {
            return validationError;
        }
    }
}
