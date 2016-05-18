package org.visallo.core.security;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.Edge;
import org.vertexium.Element;
import org.vertexium.Property;
import org.vertexium.Vertex;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyProperty;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.ontology.Relationship;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiElementAcl;
import org.visallo.web.clientapi.model.ClientApiObject;
import org.visallo.web.clientapi.model.ClientApiPropertyAcl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ACLProviderTest {
    @Mock private OntologyRepository ontologyRepository;
    @Mock private OntologyProperty ontologyProperty1;
    @Mock private OntologyProperty ontologyProperty2;
    @Mock private OntologyProperty ontologyProperty3;
    @Mock private OntologyProperty ontologyProperty4;
    @Mock private Concept vertexConcept;
    @Mock private Concept parentConcept;
    @Mock private Vertex vertex;
    @Mock private Edge edge;
    @Mock private Relationship edgeRelationship;
    @Mock private Property elementProperty1;
    @Mock private Property elementProperty2a;
    @Mock private Property elementProperty2b;
    @Mock private Property elementProperty3;
    @Mock private User user;

    private ACLProvider aclProvider;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        // mock ACLProvider abstract methods, but call implemented methods
        aclProvider = mock(ACLProvider.class);
        when(aclProvider.elementACL(any(Element.class), any(User.class), any(OntologyRepository.class)))
                .thenCallRealMethod();
        when(aclProvider.appendACL(any(ClientApiObject.class), any(User.class))).thenCallRealMethod();
        doCallRealMethod().when(aclProvider).appendACL(any(Collection.class), any(User.class));

        when(ontologyRepository.getConceptByIRI("vertex")).thenReturn(vertexConcept);
        when(ontologyRepository.getConceptByIRI("parent")).thenReturn(parentConcept);
        when(ontologyRepository.getRelationshipByIRI("edge")).thenReturn(edgeRelationship);

        when(vertexConcept.getParentConceptIRI()).thenReturn("parent");
        when(vertexConcept.getProperties()).thenReturn(
                ImmutableList.of(ontologyProperty1, ontologyProperty2, ontologyProperty4));

        when(parentConcept.getParentConceptIRI()).thenReturn(null);
        when(parentConcept.getProperties()).thenReturn(ImmutableList.of(ontologyProperty3));

        when(edgeRelationship.getProperties()).thenReturn(
                ImmutableList.of(ontologyProperty1, ontologyProperty2, ontologyProperty3, ontologyProperty4));

        when(ontologyProperty1.getTitle()).thenReturn("prop1");
        when(ontologyProperty2.getTitle()).thenReturn("prop2");
        when(ontologyProperty3.getTitle()).thenReturn("prop3");
        when(ontologyProperty4.getTitle()).thenReturn("prop4");

        when(vertex.getPropertyValue(VisalloProperties.CONCEPT_TYPE.getPropertyName())).thenReturn("vertex");
        when(vertex.getProperties("prop1")).thenReturn(ImmutableList.of(elementProperty1));
        when(vertex.getProperties("prop2")).thenReturn(ImmutableList.of(elementProperty2a, elementProperty2b));
        when(vertex.getProperties("prop3")).thenReturn(ImmutableList.of(elementProperty3));
        when(vertex.getProperties("prop4")).thenReturn(Collections.emptyList());

        when(edge.getLabel()).thenReturn("edge");
        when(edge.getProperties("prop1")).thenReturn(ImmutableList.of(elementProperty1));
        when(edge.getProperties("prop2")).thenReturn(ImmutableList.of(elementProperty2a, elementProperty2b));
        when(edge.getProperties("prop3")).thenReturn(ImmutableList.of(elementProperty3));
        when(edge.getProperties("prop4")).thenReturn(Collections.emptyList());

        when(elementProperty1.getName()).thenReturn("prop1");
        when(elementProperty1.getKey()).thenReturn("keyA");

        when(elementProperty2a.getName()).thenReturn("prop2");
        when(elementProperty2a.getKey()).thenReturn("keyA");

        when(elementProperty2b.getName()).thenReturn("prop2");
        when(elementProperty2b.getKey()).thenReturn("keyB");

        when(elementProperty3.getName()).thenReturn("prop3");
        when(elementProperty3.getKey()).thenReturn("keyA");
    }

    @Test
    public void vertexAclShouldPopulateClientApiElementAcl() {
        elementAclShouldPopulateClientApiElementAcl(vertex);
    }

    @Test
    public void edgeAclShouldPopulateClientApiElementAcl() {
        elementAclShouldPopulateClientApiElementAcl(edge);
    }

    private void elementAclShouldPopulateClientApiElementAcl(Element element) {
        when(aclProvider.canUpdateElement(element, user)).thenReturn(true);
        when(aclProvider.canDeleteElement(element, user)).thenReturn(true);

        when(aclProvider.canAddProperty(element, "keyA", "prop1", user)).thenReturn(true);
        when(aclProvider.canUpdateProperty(element, "keyA", "prop1", user)).thenReturn(false);
        when(aclProvider.canDeleteProperty(element, "keyA", "prop1", user)).thenReturn(true);

        when(aclProvider.canAddProperty(element, "keyA", "prop2", user)).thenReturn(false);
        when(aclProvider.canUpdateProperty(element, "keyA", "prop2", user)).thenReturn(true);
        when(aclProvider.canDeleteProperty(element, "keyA", "prop2", user)).thenReturn(false);

        when(aclProvider.canAddProperty(element, "keyB", "prop2", user)).thenReturn(true);
        when(aclProvider.canUpdateProperty(element, "keyB", "prop2", user)).thenReturn(false);
        when(aclProvider.canDeleteProperty(element, "keyB", "prop2", user)).thenReturn(true);

        when(aclProvider.canAddProperty(element, "keyA", "prop3", user)).thenReturn(false);
        when(aclProvider.canUpdateProperty(element, "keyA", "prop3", user)).thenReturn(true);
        when(aclProvider.canDeleteProperty(element, "keyA", "prop3", user)).thenReturn(false);

        when(aclProvider.canAddProperty(element, null, "prop4", user)).thenReturn(false);
        when(aclProvider.canUpdateProperty(element, null, "prop4", user)).thenReturn(true);
        when(aclProvider.canDeleteProperty(element, null, "prop4", user)).thenReturn(true);

        ClientApiElementAcl elementAcl = aclProvider.elementACL(element, user, ontologyRepository);

        assertThat(elementAcl.isAddable(), equalTo(true));
        assertThat(elementAcl.isUpdateable(), equalTo(true));
        assertThat(elementAcl.isDeleteable(), equalTo(true));

        List<ClientApiPropertyAcl> propertyAcls = elementAcl.getPropertyAcls();
        assertThat(propertyAcls.size(), equalTo(5));

        ClientApiPropertyAcl propertyAcl = findSinglePropertyAcl(propertyAcls, "prop1");
        assertThat(propertyAcl.getName(), equalTo("prop1"));
        assertThat(propertyAcl.getKey(), equalTo("keyA"));
        assertThat(propertyAcl.isAddable(), equalTo(true));
        assertThat(propertyAcl.isUpdateable(), equalTo(false));
        assertThat(propertyAcl.isDeleteable(), equalTo(true));

        propertyAcl = findMultiplePropertyAcls(propertyAcls, "prop2").get(0);
        assertThat(propertyAcl.getName(), equalTo("prop2"));
        assertThat(propertyAcl.getKey(), equalTo("keyA"));
        assertThat(propertyAcl.isAddable(), equalTo(false));
        assertThat(propertyAcl.isUpdateable(), equalTo(true));
        assertThat(propertyAcl.isDeleteable(), equalTo(false));

        propertyAcl = findMultiplePropertyAcls(propertyAcls, "prop2").get(1);
        assertThat(propertyAcl.getName(), equalTo("prop2"));
        assertThat(propertyAcl.getKey(), equalTo("keyB"));
        assertThat(propertyAcl.isAddable(), equalTo(true));
        assertThat(propertyAcl.isUpdateable(), equalTo(false));
        assertThat(propertyAcl.isDeleteable(), equalTo(true));

        propertyAcl = findSinglePropertyAcl(propertyAcls, "prop3");
        assertThat(propertyAcl.getName(), equalTo("prop3"));
        assertThat(propertyAcl.getKey(), equalTo("keyA"));
        assertThat(propertyAcl.isAddable(), equalTo(false));
        assertThat(propertyAcl.isUpdateable(), equalTo(true));
        assertThat(propertyAcl.isDeleteable(), equalTo(false));

        propertyAcl = findSinglePropertyAcl(propertyAcls, "prop4");
        assertThat(propertyAcl.getName(), equalTo("prop4"));
        assertThat(propertyAcl.getKey(), nullValue());
        assertThat(propertyAcl.isAddable(), equalTo(false));
        assertThat(propertyAcl.isUpdateable(), equalTo(true));
        assertThat(propertyAcl.isDeleteable(), equalTo(true));
    }

    private List<ClientApiPropertyAcl> findMultiplePropertyAcls(
            List<ClientApiPropertyAcl> propertyAcls, String propertyName) {
        return propertyAcls.stream().filter(pa -> pa.getName().equals(propertyName)).collect(Collectors.toList());
    }

    private ClientApiPropertyAcl findSinglePropertyAcl(List<ClientApiPropertyAcl> propertyAcls, String propertyName) {
        List<ClientApiPropertyAcl> matches = findMultiplePropertyAcls(propertyAcls, propertyName);
        assertThat(matches.size(), equalTo(1));
        return matches.get(0);
    }
}
