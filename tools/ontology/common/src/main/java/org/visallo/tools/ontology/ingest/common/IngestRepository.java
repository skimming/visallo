package org.visallo.tools.ontology.ingest.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import org.vertexium.*;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.graph.GraphRepository;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.ontology.Relationship;
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

    public boolean validate(ConceptBuilder builder) {
        return save(builder, false);
    }

    public boolean validate(RelationshipBuilder builder) {
        return save(builder, false);
    }

    public void save(EntityBuilder... builders) {
        save(Arrays.asList(builders));
    }

    public void save(Collection<EntityBuilder> builders) {
        LOGGER.debug("Saving %d entities", builders.size());
        for (EntityBuilder builder : builders) {
            if (builder instanceof ConceptBuilder) {
                if (!save((ConceptBuilder) builder, false)) {
                    throw new VisalloException("Concept class: " + builder.getClass().getName() + " failed validation");
                }
            } else if (builder instanceof RelationshipBuilder) {
                if (!save((RelationshipBuilder) builder, false)) {
                    throw new VisalloException("Relationship class: " + builder.getClass().getName() + " failed validation");
                }
            } else {
                throw new VisalloException("Unexpected type: " + builder.getClass().getName());
            }
        }
        for (EntityBuilder builder : builders) {
            if (builder instanceof ConceptBuilder) {
                save((ConceptBuilder) builder, true);
            } else {
                save((RelationshipBuilder) builder, true);
            }
        }
    }

    public void flush() {
        graph.flush();
    }

    private boolean save(ConceptBuilder conceptBuilder, boolean save) {
        boolean valid = verifyConcept(conceptBuilder, save);
        if (valid) {
            Visibility conceptVisibility = getVisibility(conceptBuilder.getVisibility());
            VertexBuilder vertexBuilder = graph.prepareVertex(conceptBuilder.getId(), getTimestamp(conceptBuilder.getTimestamp()), conceptVisibility);
            vertexBuilder.setProperty(VisalloProperties.CONCEPT_TYPE.getPropertyName(), conceptBuilder.getIri(), conceptVisibility);

            valid = addProperties(vertexBuilder, conceptBuilder, save);
            if (valid && save) {
                LOGGER.trace("Saving vertex: %s", vertexBuilder.getVertexId());
                vertexBuilder.save(getAuthorizations());
            }
        }
        return valid;
    }

    private boolean save(RelationshipBuilder relationshipBuilder, boolean save) {
        boolean valid = verifyRelationship(relationshipBuilder, save);
        if (valid) {
            Visibility relationshipVisibility = getVisibility(relationshipBuilder.getVisibility());
            EdgeBuilderByVertexId edgeBuilder = graph.prepareEdge(
                    relationshipBuilder.getId(),
                    relationshipBuilder.getOutVertexId(),
                    relationshipBuilder.getInVertexId(),
                    relationshipBuilder.getIri(),
                    getTimestamp(relationshipBuilder.getTimestamp()),
                    relationshipVisibility
            );

            valid = addProperties(edgeBuilder, relationshipBuilder, save);
            if (valid && save) {
                LOGGER.trace("Saving edge: %s", edgeBuilder.getEdgeId());
                edgeBuilder.save(getAuthorizations());
            }
        }
        return valid;
    }

    private boolean addProperties(ElementBuilder elementBuilder, EntityBuilder entityBuilder, boolean save) {
        for (PropertyAddition<?> propertyAddition : entityBuilder.getPropertyAdditions()) {
            if (propertyAddition.getValue() != null) {
                boolean valid = verifyClassProperty(entityBuilder, propertyAddition, save);
                if (!valid) {
                    return false;
                }

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
        return true;
    }

    private boolean verifyConcept(ConceptBuilder builder, boolean save) {
        if (verifiedClasses.contains(builder.getClass())) {
            return true;
        }

        Concept concept = ontologyRepository.getConceptByIRI(builder.getIri());
        if (concept == null) {
            logOrThrowError(save, "Concept class: " + builder.getClass().getName() + " IRI: " + builder.getIri() + " is invalid");
            return false;
        }

        verifiedClasses.add(builder.getClass());
        return true;
    }

    private boolean verifyRelationship(RelationshipBuilder builder, boolean save) {
        if (verifiedClasses.contains(builder.getClass())) {
            return true;
        }

        try {
            Relationship relationship = ontologyRepository.getRelationshipByIRI(builder.getIri());
            if (relationship == null) {
                logOrThrowError(save, "Relationship class: " + builder.getClass().getName() + " IRI: " + builder.getIri() + " is invalid");
                return false;
            }
            List<String> domainConceptIRIs = relationship.getDomainConceptIRIs();
            List<String> rangeConceptIRIs = relationship.getRangeConceptIRIs();

            for (Constructor constructor : builder.getClass().getConstructors()) {
                Class[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 3) {
                    String outConceptIRI = (String) parameterTypes[1].getField("IRI").get(null);
                    if (!domainConceptIRIs.contains(outConceptIRI)) {
                        logOrThrowError(save, "Out vertex Concept IRI: " + outConceptIRI + " is invalid");
                        return false;
                    }
                    String inConceptIRI = (String) parameterTypes[2].getField("IRI").get(null);
                    if (!rangeConceptIRIs.contains(inConceptIRI)) {
                        logOrThrowError(save, "In vertex Concept IRI: " + inConceptIRI + " is invalid");
                        return false;
                    }
                } else {
                    LOGGER.warn("Unsupported Constructor found: " + constructor.toString());
                }
            }

            verifiedClasses.add(builder.getClass());
            return true;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new VisalloException("Failed to get IRI for in/out Concept class", e);
        }
    }

    private boolean verifyClassProperty(EntityBuilder entityBuilder, PropertyAddition propertyAddition, boolean save) {
        if (verifiedClassProperties.contains(getKey(entityBuilder, propertyAddition))) {
            return true;
        }

        OntologyProperty property = ontologyRepository.getPropertyByIRI(propertyAddition.getIri());
        Class propertyType = PropertyType.getTypeClass(property.getDataType());
        if (propertyType.equals(BigDecimal.class)) {
            propertyType = Double.class;
        }
        Class<?> valueType = propertyAddition.getValue().getClass();

        if (entityBuilder instanceof ConceptBuilder) {
            Concept concept = ontologyRepository.getConceptByIRI(entityBuilder.getIri());
            if (!isPropertyValidForConcept(concept, property)) {
                logOrThrowError(save, "Property: " + propertyAddition.getIri() + " is invalid for Concept class (or its ancestors): " + entityBuilder.getClass().getName());
                return false;
            }
            if (!valueType.isAssignableFrom(propertyType)) {
                logOrThrowError(save, "Property: " + propertyAddition.getIri() + " type: " + valueType.getSimpleName() + " is invalid for Concept class: " + entityBuilder.getClass().getName());
                return false;
            }
            verifiedClassProperties.add(getKey(entityBuilder, propertyAddition));
            return true;

        } else if (entityBuilder instanceof RelationshipBuilder) {
            Relationship relationship = ontologyRepository.getRelationshipByIRI(entityBuilder.getIri());
            if (!relationship.getProperties().contains(property)) {
                logOrThrowError(save, "Property: " + propertyAddition.getIri() + " is invalid for Relationship class: " + entityBuilder.getClass().getName());
                return false;
            }
            if (!valueType.isAssignableFrom(propertyType)) {
                logOrThrowError(save, "Property: " + propertyAddition.getIri() + " type: " + valueType.getSimpleName() + " is invalid for Relationship class: " + entityBuilder.getClass().getName());
                return false;
            }
            verifiedClassProperties.add(getKey(entityBuilder, propertyAddition));
            return true;

        } else {
            throw new VisalloException("Unexpected type: " + entityBuilder.getClass().getName());
        }
    }

    private boolean isPropertyValidForConcept(Concept concept, OntologyProperty property) {
        if (!concept.getProperties().contains(property)) {
            Concept parentConcept = ontologyRepository.getParentConcept(concept);
            return parentConcept != null && isPropertyValidForConcept(parentConcept, property);
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

    private void logOrThrowError(boolean save, String message) {
        if (save) {
            throw new VisalloException(message);
        }
        LOGGER.error(message);
    }

    private Authorizations getAuthorizations() {
        return userRepository.getAuthorizations(getIngestUser());
    }

    private User getIngestUser() {
        return userRepository.getSystemUser();
    }
}
