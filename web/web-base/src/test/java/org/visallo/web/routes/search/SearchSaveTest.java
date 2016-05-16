package org.visallo.web.routes.search;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.visallo.core.model.search.SearchRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiSaveSearchResponse;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SearchSaveTest {
    private SearchSave searchSave;

    @Mock
    private SearchRepository searchRepository;

    @Mock
    private User user;

    @Before
    public void setUp() {
        searchSave = new SearchSave(searchRepository);
    }

    @Test
    public void testHandleFirstSave() throws Exception {
        String id = null;
        String name = null;
        String url = "/vertex/search";
        JSONObject searchParameters = new JSONObject();
        String newId = "1234";

        when(searchRepository.saveSearch(eq(id), eq(name), eq(url), eq(searchParameters), eq(user))).thenReturn(newId);

        ClientApiSaveSearchResponse results = searchSave.handle(id, name, url, searchParameters, false, user);

        ClientApiSaveSearchResponse expectedResult = new ClientApiSaveSearchResponse();
        expectedResult.id = newId;
        assertEquals(expectedResult, results);
    }

    @Test
    public void testHandleExistingSave() throws Exception {
        String id = "1234";
        String name = null;
        String url = "/vertex/search";
        JSONObject searchParameters = new JSONObject();
        String newId = "1234";

        when(searchRepository.saveSearch(eq(id), eq(name), eq(url), eq(searchParameters), eq(user))).thenReturn(newId);

        ClientApiSaveSearchResponse results = searchSave.handle(id, name, url, searchParameters, false, user);

        ClientApiSaveSearchResponse expectedResult = new ClientApiSaveSearchResponse();
        expectedResult.id = newId;
        assertEquals(expectedResult, results);
    }

    @Test
    public void testHandleExistingSaveGlobal() throws Exception {
        String id = "1234";
        String name = null;
        String url = "/vertex/search";
        JSONObject searchParameters = new JSONObject();
        String newId = "1234";

        when(searchRepository.saveGlobalSearch(eq(id), eq(name), eq(url), eq(searchParameters), eq(user))).thenReturn(newId);

        ClientApiSaveSearchResponse results = searchSave.handle(id, name, url, searchParameters, true, user);

        ClientApiSaveSearchResponse expectedResult = new ClientApiSaveSearchResponse();
        expectedResult.id = newId;
        assertEquals(expectedResult, results);
    }
}