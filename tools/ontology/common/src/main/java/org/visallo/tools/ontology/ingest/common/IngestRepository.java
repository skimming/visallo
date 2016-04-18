package org.visallo.tools.ontology.ingest.common;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;

import org.vertexium.Authorizations;
import org.vertexium.EdgeBuilderByVertexId;
import org.vertexium.ElementBuilder;
import org.vertexium.Graph;
import org.vertexium.Metadata;
import org.vertexium.VertexBuilder;
import org.vertexium.Visibility;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.ontology.Relationship;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.PropertyType;

public class IngestRepository {
  private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(IngestRepository.class);

  private Graph graph;
  private UserRepository userRepository;
  private VisibilityTranslator visibilityTranslator;
  private OntologyRepository ontologyRepository;

  private Set<Class> verifiedClasses = new HashSet<>();
  private Set<String> verifiedClassProperties = new HashSet<>();

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

  public boolean validate(BaseConceptBuilder builder) {
    return save(builder, false);
  }

  public boolean validate(BaseRelationshipBuilder builder) {
    return save(builder, false);
  }

  public void save(BaseEntityBuilder... builders) {
    save(Arrays.asList(builders));
  }

  public void save(Collection<BaseEntityBuilder> builders) {
    LOGGER.debug("Saving %d entities", builders.size());
    for (final BaseEntityBuilder builder : builders) {
      if (builder instanceof BaseConceptBuilder) {
        if (!save((BaseConceptBuilder) builder, false)) {
          throw new VisalloException("Concept class: " + builder.getClass().getName() + " failed validation");
        }
      } else if (builder instanceof BaseRelationshipBuilder) {
        if (!save((BaseRelationshipBuilder) builder, false)) {
          throw new VisalloException("Relationship class: " + builder.getClass().getName() + " failed validation");
        }
      } else {
        throw new VisalloException("Unexpected type: " + builder.getClass().getName());
      }
    }
    for (final BaseEntityBuilder builder : builders) {
      if (builder instanceof BaseConceptBuilder) {
        save((BaseConceptBuilder) builder, true);
      } else {
        save((BaseRelationshipBuilder) builder, true);
      }
    }
  }

  public void flush() {
    graph.flush();
  }

  private boolean save(BaseConceptBuilder conceptBuilder, boolean save) {
    boolean valid = verifyConcept(conceptBuilder, save);
    if (valid) {
      Visibility conceptVisibility = getVisibility(conceptBuilder.getVisibility());
      VertexBuilder vertexBuilder = graph.prepareVertex(conceptBuilder.getId(), conceptBuilder.getTimestamp(), getVisibility(conceptBuilder.getVisibility()));
      vertexBuilder.setProperty(VisalloProperties.CONCEPT_TYPE.getPropertyName(), conceptBuilder.getIri(), conceptVisibility);

      valid = addProperties(vertexBuilder, conceptBuilder, save);
      if (valid && save) {
        LOGGER.trace("Saving vertex: %s", vertexBuilder.getVertexId());
        vertexBuilder.save(getAuthorizations());
      }
    }
    return valid;
  }

  private boolean save(BaseRelationshipBuilder relationshipBuilder, boolean save) {
    boolean valid = verifyRelationship(relationshipBuilder, save);
    if (valid) {
      Visibility relationshipVisibility = getVisibility(relationshipBuilder.getVisibility());
      EdgeBuilderByVertexId edgeBuilder = graph.prepareEdge(
          relationshipBuilder.getId(),
          relationshipBuilder.getOutVertexId(),
          relationshipBuilder.getInVertexId(),
          relationshipBuilder.getIri(),
          relationshipBuilder.getTimestamp(),
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

  private boolean addProperties(ElementBuilder elementBuilder, BaseEntityBuilder entityBuilder, boolean save) {
    for (BaseEntityBuilder.PropertyAddition<?> propertyAddition : entityBuilder.getPropertyAdditions()) {
      if (propertyAddition.getValue() != null) {
        boolean valid = verifyClassProperty(entityBuilder, propertyAddition, save);
        if (!valid) return false;

        Visibility propertyVisibility = getVisibility(propertyAddition.getVisibility());
        elementBuilder.addPropertyValue(
            propertyAddition.getKey(),
            propertyAddition.getIri(),
            propertyAddition.getValue(),
            getMetadata(propertyAddition.getMetadata(), propertyVisibility),
            propertyAddition.getTimestamp(),
            propertyVisibility
        );
      }
    }
    return true;
  }

  private boolean verifyConcept(BaseConceptBuilder builder, boolean save) {
    if (verifiedClasses.contains(builder.getClass())) return true;

    Concept concept = ontologyRepository.getConceptByIRI(builder.getIri());
    if (concept == null) {
      if (save) {
        throw new VisalloException("Concept class: " + builder.getClass().getName() + " IRI: " + builder.getIri() + " is invalid");
      } else {
        return false;
      }
    }

    verifiedClasses.add(builder.getClass());
    return true;
  }

  private boolean verifyRelationship(BaseRelationshipBuilder builder, boolean save) {
    if (verifiedClasses.contains(builder.getClass())) return true;

    try {
      Relationship relationship = ontologyRepository.getRelationshipByIRI(builder.getIri());
      if (relationship == null) {
        if (save) {
          throw new VisalloException("Relationship class: " + builder.getClass().getName() + " IRI: " + builder.getIri() + " is invalid");
        } else {
          return false;
        }
      }
      List<String> domainConceptIRIs = relationship.getDomainConceptIRIs();
      List<String> rangeConceptIRIs = relationship.getRangeConceptIRIs();

      for (Constructor constructor : builder.getClass().getConstructors()) {
        Class[] parameterTypes = constructor.getParameterTypes();
        if (parameterTypes.length == 3) {
          String outConceptIRI = (String) parameterTypes[1].getField("IRI").get(null);
          if (!domainConceptIRIs.contains(outConceptIRI)) {
            if (save) {
              throw new VisalloException("Out vertex Concept IRI: " + outConceptIRI + " is invalid");
            } else {
              return false;
            }
          }
          String inConceptIRI = (String) parameterTypes[2].getField("IRI").get(null);
          if (!rangeConceptIRIs.contains(inConceptIRI)) {
            if (save) {
              throw new VisalloException("In vertex Concept IRI: " + inConceptIRI + " is invalid");
            } else {
              return false;
            }
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

  private boolean verifyClassProperty(BaseEntityBuilder entityBuilder, BaseEntityBuilder.PropertyAddition propertyAddition, boolean save) {
    if (verifiedClassProperties.contains(getKey(entityBuilder, propertyAddition))) return true;

    OntologyProperty property = ontologyRepository.getPropertyByIRI(propertyAddition.getIri());
    Class propertyType = PropertyType.getTypeClass(property.getDataType());
    if (propertyType.equals(BigDecimal.class)) {
      propertyType = Double.class;
    }
    Class<?> valueType = propertyAddition.getValue().getClass();

    if (entityBuilder instanceof BaseConceptBuilder) {
      Concept concept = ontologyRepository.getConceptByIRI(entityBuilder.getIri());
      if (!isPropertyValidForConcept(concept, property)) {
        if (save) {
          throw new VisalloException("Property: " + propertyAddition.getIri() + " is invalid for Concept class (or its ancestors): " + entityBuilder.getClass().getName());
        } else {
          return false;
        }
      }
      if (!valueType.isAssignableFrom(propertyType)) {
        if (save) {
          throw new VisalloException("Property: " + propertyAddition.getIri() + " type: " + valueType.getSimpleName() + " is invalid for Concept class: " + entityBuilder.getClass().getName());
        } else {
          return false;
        }
      }
      verifiedClassProperties.add(getKey(entityBuilder, propertyAddition));
      return true;

    } else if (entityBuilder instanceof BaseRelationshipBuilder) {
      Relationship relationship = ontologyRepository.getRelationshipByIRI(entityBuilder.getIri());
      if (!relationship.getProperties().contains(property)) {
        if (save) {
          throw new VisalloException("Property: " + propertyAddition.getIri() + " is invalid for Relationship class: " + entityBuilder.getClass().getName());
        } else {
          return false;
        }
      }
      if (!valueType.isAssignableFrom(propertyType)) {
        if (save) {
          throw new VisalloException("Property: " + propertyAddition.getIri() + " type: " + valueType.getSimpleName() + " is invalid for Relationship class: " + entityBuilder.getClass().getName());
        } else {
          return false;
        }
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

  private String getKey(BaseEntityBuilder entityBuilder, BaseEntityBuilder.PropertyAddition propertyAddition) {
    return entityBuilder.getIri() + ":" + propertyAddition.getIri();
  }

  private Visibility getVisibility(String visibilitySource) {
    return visibilitySource == null ? visibilityTranslator.getDefaultVisibility() : visibilityTranslator.toVisibility(visibilitySource).getVisibility();
  }

  private Metadata getMetadata(Map<String, Object> map, Visibility visibility) {
    if (map != null) {
      Metadata metadata = new Metadata();
      map.forEach((k, v) -> metadata.add(k, v, visibility));
      return metadata;
    }
    return null;
  }

  private Authorizations getAuthorizations() {
    return userRepository.getAuthorizations(userRepository.getSystemUser());
  }
}
