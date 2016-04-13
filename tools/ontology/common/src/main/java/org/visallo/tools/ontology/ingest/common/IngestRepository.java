package org.visallo.tools.ontology.ingest.common;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;

import org.vertexium.Authorizations;
import org.vertexium.EdgeBuilderByVertexId;
import org.vertexium.Graph;
import org.vertexium.Metadata;
import org.vertexium.VertexBuilder;
import org.vertexium.Visibility;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.ontology.Relationship;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.security.VisibilityTranslator;

public class IngestRepository {
  private Graph graph;
  private UserRepository userRepository;
  private VisibilityTranslator visibilityTranslator;
  private OntologyRepository ontologyRepository;

  private Set<Class> verifiedClasses = new HashSet<>();

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

  private void verifyOntology(BaseConceptBuilder builder) {
    if (verifiedClasses.contains(builder.getClass())) return;

    boolean valid = true;

    Concept concept = ontologyRepository.getConceptByIRI(builder.getIri());
    // TODO:

    if (valid) {
      verifiedClasses.add(builder.getClass());
    } else {
      throw new VisalloException("Concept class " + builder.getClass().getName() + " is invalid for the destination ontology");
    }
  }

  private void verifyOntology(BaseRelationshipBuilder builder) {
    if (verifiedClasses.contains(builder.getClass())) return;

    boolean valid = true;

    Relationship relationship = ontologyRepository.getRelationshipByIRI(builder.getIri());
    // TODO:

    if (valid) {
      verifiedClasses.add(builder.getClass());
    } else {
      throw new VisalloException("Relationship class " + builder.getClass().getName() + " is invalid for the destination ontology");
    }
  }

  public void save(BaseConceptBuilder conceptBuilder) {
    verifyOntology(conceptBuilder);

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
    verifyOntology(relationshipBuilder);

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
