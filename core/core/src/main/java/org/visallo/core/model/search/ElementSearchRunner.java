package org.visallo.core.model.search;

import com.google.inject.Inject;
import org.json.JSONArray;
import org.vertexium.Authorizations;
import org.vertexium.ElementType;
import org.vertexium.Graph;
import org.vertexium.query.Query;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.directory.DirectoryRepository;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.EnumSet;

public class ElementSearchRunner extends ElementSearchRunnerWithRelatedBase {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ElementSearchRunner.class);
    public static final String URI = "/element/search";

    @Inject
    public ElementSearchRunner(
            OntologyRepository ontologyRepository,
            Graph graph,
            Configuration configuration,
            DirectoryRepository directoryRepository
    ) {
        super(ontologyRepository, graph, configuration, directoryRepository);
    }

    @Override
    protected EnumSet<ElementType> getResultType() {
        return EnumSet.of(ElementType.EDGE, ElementType.VERTEX);
    }

    @Override
    public String getUri() {
        return URI;
    }

    @Override
    protected QueryAndData getQuery(SearchOptions searchOptions, final Authorizations authorizations) {
        JSONArray filterJson = getFilterJson(searchOptions);
        String queryString = searchOptions.getRequiredParameter("q", String.class);
        LOGGER.debug("search %s\n%s", queryString, filterJson.toString(2));

        Query graphQuery = query(queryString, authorizations);

        return new QueryAndData(graphQuery);
    }

    private Query query(String query, Authorizations authorizations) {
        return getGraph().query(query, authorizations);
    }
}
