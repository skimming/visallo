package org.visallo.web.routes;

import com.v5analytics.webster.HandlerChain;
import org.json.JSONArray;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.vertexium.Graph;
import org.vertexium.inmemory.InMemoryGraph;
import org.visallo.core.config.Configuration;
import org.visallo.core.config.ConfigurationLoader;
import org.visallo.core.config.HashMapConfigurationLoader;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.model.termMention.TermMentionRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workspace.WorkspaceHelper;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.ProxyUser;
import org.visallo.core.user.User;
import org.visallo.vertexium.model.user.InMemoryUser;
import org.visallo.web.CurrentUser;
import org.visallo.web.SessionUser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public abstract class RouteTestBase {
    public static final String WORKSPACE_ID = "WORKSPACE_12345";
    public static final String USER_ID = "USER_123";

    @Mock
    protected UserRepository userRepository;

    @Mock
    protected HttpServletRequest request;

    @Mock
    protected HttpServletResponse response;

    @Mock
    protected HandlerChain chain;

    @Mock
    protected OntologyRepository ontologyRepository;

    @Mock
    protected WorkspaceRepository workspaceRepository;

    @Mock
    protected TermMentionRepository termMentionRepository;

    @Mock
    protected WorkspaceHelper workspaceHelper;

    protected Configuration configuration;

    protected Graph graph;

    @Mock
    protected HttpSession httpSession;

    protected SessionUser sessionUser;

    protected ProxyUser user;

    protected User nonProxiedUser;

    private ByteArrayOutputStream responseByteArrayOutputStream;

    private HashMap<String, String[]> requestParameters;

    private HashMap<String, Object> attributes;

    protected void before() throws IOException {
        requestParameters = new HashMap<>();
        attributes = new HashMap<>();
        Map config = new HashMap();
        ConfigurationLoader hashMapConfigurationLoader = new HashMapConfigurationLoader(config);
        configuration = new Configuration(hashMapConfigurationLoader, new HashMap<>());

        graph = InMemoryGraph.create(getGraphConfiguration());

        Set<String> privileges = new HashSet<>();
        String[] authorizations = new String[0];
        String currentWorkspaceId = null;
        nonProxiedUser = new InMemoryUser("jdoe", "Jane Doe", "jane.doe@email.com", privileges, authorizations, currentWorkspaceId);
        when(userRepository.findById(eq(USER_ID))).thenReturn(nonProxiedUser);

        sessionUser = new SessionUser(USER_ID);
        user = new ProxyUser(USER_ID, userRepository);

        when(request.getSession()).thenReturn(httpSession);
        when(httpSession.getAttribute(eq(CurrentUser.SESSIONUSER_ATTRIBUTE_NAME))).thenReturn(sessionUser);

        when(request.getAttribute(eq("workspaceId"))).thenReturn(WORKSPACE_ID);

        when(workspaceRepository.hasReadPermissions(eq(WORKSPACE_ID), eq(user))).thenReturn(true);

        responseByteArrayOutputStream = new ByteArrayOutputStream();
        when(response.getWriter()).thenReturn(new PrintWriter(responseByteArrayOutputStream));

        when(request.getParameterNames()).thenAnswer(
                (Answer<Enumeration<String>>) invocationOnMock -> Collections.enumeration(requestParameters.keySet())
        );
        when(request.getParameterValues(any(String.class))).thenAnswer(
                (Answer<String[]>) invocationOnMock -> {
                    Object key = invocationOnMock.getArguments()[0];
                    return (String[]) requestParameters.get(key);
                }
        );
        when(request.getParameter(any(String.class))).thenAnswer(
                (Answer<String>) invocationOnMock -> {
                    Object key = invocationOnMock.getArguments()[0];
                    String[] value = requestParameters.get(key);
                    if (value == null) {
                        return null;
                    }
                    if (value.length != 1) {
                        throw new VisalloException("Unexpected number of values. Expected 1 found " + value.length);
                    }
                    return value[0];
                }
        );
        when(request.getParameterMap()).thenReturn(requestParameters);

        when(request.getAttributeNames()).thenAnswer(
                (Answer<Enumeration<String>>) invocationOnMock -> Collections.enumeration(attributes.keySet())
        );
    }

    protected Map<String, Object> getGraphConfiguration() {
        return new HashMap<>();
    }

    protected byte[] getResponse() {
        return responseByteArrayOutputStream.toByteArray();
    }

    protected void setArrayParameter(String parameterName, String[] values) {
        requestParameters.put(parameterName, values);
    }

    protected void setParameter(String parameterName, JSONArray json) {
        setParameter(parameterName, json.toString());
    }

    protected void setParameter(String parameterName, String value) {
        requestParameters.put(parameterName, new String[]{value});
    }
}
