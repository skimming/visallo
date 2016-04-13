package org.visallo.tools.ontology.ingest.common;

import java.lang.reflect.Field;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.hash.Hashing;
import com.google.inject.Inject;

import org.vertexium.Authorizations;
import org.vertexium.EdgeBuilderByVertexId;
import org.vertexium.Graph;
import org.vertexium.Metadata;
import org.vertexium.VertexBuilder;
import org.vertexium.Visibility;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.web.clientapi.model.ClientApiOntology;
import org.visallo.web.clientapi.util.ObjectMapperFactory;

public class IngestRepository {
  private Graph graph;
  private UserRepository userRepository;
  private VisibilityTranslator visibilityTranslator;
  private String ontologyHash;

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

    ClientApiOntology result = ontologyRepository.getClientApiObject();
    String ontologyJsonString = ObjectMapperFactory.getInstance().writeValueAsString(result);
    ontologyHash = calculateOntologyHash(ontologyJsonString);
  }

  public static String calculateOntologyHash(String ontologyJsonString) {
    return Hashing.murmur3_128().hashString(ontologyJsonString).toString();
  }

  private void verifyOntologyHash(Class klass) {
    try {
      Field f = klass.getField("ONTOLOGY_HASH");
      String klassOntologyHash = (String) f.get(null);
      if (!ontologyHash.equals(klassOntologyHash)) {
        throw new VisalloException("ONTOLOGY_HASH field in class: " + klass.getName() + " does not match destination ontology hash");
      }
    } catch (NoSuchFieldException e) {
      throw new VisalloException("ONTOLOGY_HASH field not found", e);
    } catch (IllegalAccessException e) {
      throw new VisalloException("ONTOLOGY_HASH field inaccessible", e);
    }
  }

  public void save(BaseConceptBuilder conceptBuilder) {
    verifyOntologyHash(conceptBuilder.getClass());

    Visibility conceptVisibility = getVisibility(conceptBuilder.getVisibility());
    VertexBuilder vertexBuilder = graph.prepareVertex(conceptBuilder.getId(), conceptBuilder.getTimestamp(), getVisibility(conceptBuilder.getVisibility()));
    vertexBuilder.setProperty(VisalloProperties.CONCEPT_TYPE.getPropertyName(), conceptBuilder.getIri(), conceptVisibility);
    conceptBuilder.getPropertyAdditions().forEach(propertyAddition -> {
      if (propertyAddition.getValue() != null) {
        Visibility propertyVisibility = getVisibility(propertyAddition.getVisibility());
        vertexBuilder.addPropertyValue(
            propertyAddition.getKey(),
            propertyAddition.getIri(),
            propertyAddition.getValue(),
            getMetadata(propertyAddition.getMetadata(), propertyVisibility),
            propertyAddition.getTimestamp(),
            propertyVisibility
        );
      }
    });
    vertexBuilder.save(getAuthorizations());
  }

  public void save(BaseRelationshipBuilder relationshipBuilder) {
    verifyOntologyHash(relationshipBuilder.getClass());

    Visibility relationshipVisibility = getVisibility(relationshipBuilder.getVisibility());
    EdgeBuilderByVertexId edgeBuilder = graph.prepareEdge(
        relationshipBuilder.getId(),
        relationshipBuilder.getOutVertexId(),
        relationshipBuilder.getInVertexId(),
        relationshipBuilder.getIri(),
        relationshipBuilder.getTimestamp(),
        relationshipVisibility
    );
    relationshipBuilder.getPropertyAdditions().forEach(propertyAddition -> {
      if (propertyAddition.getValue() != null) {
        Visibility propertyVisibility = getVisibility(propertyAddition.getVisibility());
        edgeBuilder.addPropertyValue(
            propertyAddition.getKey(),
            propertyAddition.getIri(),
            propertyAddition.getValue(),
            getMetadata(propertyAddition.getMetadata(), propertyVisibility),
            propertyAddition.getTimestamp(),
            propertyVisibility
        );
      }
    });
    edgeBuilder.save(getAuthorizations());
  }

  public void flush() {
    graph.flush();
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
