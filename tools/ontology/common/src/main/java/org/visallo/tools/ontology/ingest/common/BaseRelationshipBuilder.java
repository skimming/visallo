package org.visallo.tools.ontology.ingest.common;

public abstract class BaseRelationshipBuilder extends BaseEntityBuilder {
    private String inVertexId;
    private String outVertexId;

    public BaseRelationshipBuilder(final String id) {
        super(id);
    }

    public String getInVertexId() {
        return inVertexId;
    }

    public void setInVertexId(final String inVertexId) {
        this.inVertexId = inVertexId;
    }

    public String getOutVertexId() {
        return outVertexId;
    }

    public void setOutVertexId(final String outVertexId) {
        this.outVertexId = outVertexId;
    }
}
