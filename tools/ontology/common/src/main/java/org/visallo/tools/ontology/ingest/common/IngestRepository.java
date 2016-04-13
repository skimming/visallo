package org.visallo.tools.ontology.ingest.common;

import java.util.Map;

import com.google.inject.Inject;

import org.vertexium.Authorizations;
import org.vertexium.EdgeBuilderByVertexId;
import org.vertexium.Graph;
import org.vertexium.Metadata;
import org.vertexium.VertexBuilder;
import org.vertexium.Visibility;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.security.VisibilityTranslator;

public class IngestRepository {
  private Graph graph;
  private UserRepository userRepository;
  private VisibilityTranslator visibilityTranslator;

  @Inject
  public IngestRepository(Graph graph, UserRepository userRepository, VisibilityTranslator visibilityTranslator) {
    this.graph = graph;
    this.userRepository = userRepository;
    this.visibilityTranslator = visibilityTranslator;
  }

  public void save(BaseConceptBuilder conceptBuilder) {
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
