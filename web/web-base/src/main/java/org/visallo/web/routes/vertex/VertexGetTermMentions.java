package org.visallo.web.routes.vertex;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.vertexium.Authorizations;
import org.vertexium.Graph;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.util.ClientApiConverter;
import org.visallo.web.clientapi.model.ClientApiTermMentionsResponse;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;

public class VertexGetTermMentions implements ParameterizedHandler {
    private final Graph graph;
    private final TermMentionRepository termMentionRepository;

    @Inject
    public VertexGetTermMentions(
            Graph graph,
            TermMentionRepository termMentionRepository
    ) {
        this.graph = graph;
        this.termMentionRepository = termMentionRepository;
    }

    @Handle
    public ClientApiTermMentionsResponse handle(
            @Required(name = "graphVertexId") String graphVertexId,
            @Required(name = "propertyKey") String propertyKey,
            @Required(name = "propertyName") String propertyName,
            @ActiveWorkspaceId String workspaceId,
            Authorizations authorizations
    ) throws Exception {
        Vertex vertex = graph.getVertex(graphVertexId, authorizations);
        if (vertex == null) {
            throw new VisalloResourceNotFoundException(String.format("vertex %s not found", graphVertexId));
        }

        Property property = vertex.getProperty(propertyKey, propertyName);
        if (property == null) {
            throw new VisalloResourceNotFoundException(String.format("property %s:%s not found on vertex %s", propertyKey, propertyName, vertex.getId()));
        }

        Iterable<Vertex> termMentions = termMentionRepository.findByOutVertexAndProperty(graphVertexId, propertyKey, propertyName, authorizations);
        return ClientApiConverter.toTermMentionsResponse(termMentions, workspaceId, authorizations);
    }
}
