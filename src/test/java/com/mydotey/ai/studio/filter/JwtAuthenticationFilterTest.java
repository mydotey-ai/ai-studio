package com.mydotey.ai.studio.filter;

import com.mydotey.ai.studio.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final Long USER_ID = 123L;
    private static final String USER_ROLE = "USER";

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void testTokenExtractedFromAuthorizationHeader() throws ServletException, IOException {
        // Given: request with valid Authorization header
        request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(VALID_TOKEN)).thenReturn(USER_ID);
        when(jwtUtil.getRoleFromToken(VALID_TOKEN)).thenReturn(USER_ROLE);

        // When: filter processes the request
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then: user attributes should be set
        assertEquals(USER_ID, request.getAttribute("userId"));
        assertEquals(USER_ROLE, request.getAttribute("userRole"));

        // And: filter chain should be called
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testTokenExtractedFromQueryParameter() throws ServletException, IOException {
        // Given: request with token in query parameter (for SSE)
        request.setQueryString("token=" + VALID_TOKEN);
        request.setParameter("token", VALID_TOKEN);
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(VALID_TOKEN)).thenReturn(USER_ID);
        when(jwtUtil.getRoleFromToken(VALID_TOKEN)).thenReturn(USER_ROLE);

        // When: filter processes the request
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then: user attributes should be set
        assertEquals(USER_ID, request.getAttribute("userId"));
        assertEquals(USER_ROLE, request.getAttribute("userRole"));

        // And: filter chain should be called
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testAuthorizationHeaderTakesPrecedenceOverQueryParameter() throws ServletException, IOException {
        // Given: request with both Authorization header and query parameter
        String headerToken = "header.jwt.token";
        String queryToken = "query.jwt.token";
        request.addHeader("Authorization", "Bearer " + headerToken);
        request.setQueryString("token=" + queryToken);
        request.setParameter("token", queryToken);

        when(jwtUtil.validateToken(headerToken)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(headerToken)).thenReturn(USER_ID);
        when(jwtUtil.getRoleFromToken(headerToken)).thenReturn(USER_ROLE);

        // When: filter processes the request
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then: header token should be used (not query parameter)
        verify(jwtUtil).validateToken(headerToken);
        verify(jwtUtil, never()).validateToken(queryToken);
        assertEquals(USER_ID, request.getAttribute("userId"));
        assertEquals(USER_ROLE, request.getAttribute("userRole"));
    }

    @Test
    void testInvalidTokenDoesNotSetAttributes() throws ServletException, IOException {
        // Given: request with invalid token
        request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(false);

        // When: filter processes the request
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then: user attributes should NOT be set
        assertNull(request.getAttribute("userId"));
        assertNull(request.getAttribute("userRole"));

        // And: filter chain should still be called
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testNoTokenDoesNotSetAttributes() throws ServletException, IOException {
        // Given: request without any token
        request.setRequestURI("/api/test");

        // When: filter processes the request
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then: user attributes should NOT be set
        assertNull(request.getAttribute("userId"));
        assertNull(request.getAttribute("userRole"));

        // And: filter chain should still be called
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testEmptyQueryParameterTokenDoesNotSetAttributes() throws ServletException, IOException {
        // Given: request with empty token query parameter
        request.setQueryString("token=");
        request.setParameter("token", "");

        // When: filter processes the request
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then: user attributes should NOT be set
        assertNull(request.getAttribute("userId"));
        assertNull(request.getAttribute("userRole"));

        // And: filter chain should still be called
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testBearerTokenWithoutBearerPrefixDoesNotSetAttributes() throws ServletException, IOException {
        // Given: request with Authorization header without "Bearer " prefix
        request.addHeader("Authorization", VALID_TOKEN);

        // When: filter processes the request
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then: user attributes should NOT be set
        assertNull(request.getAttribute("userId"));
        assertNull(request.getAttribute("userRole"));

        // And: filter chain should still be called
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testNullAuthorizationHeaderDoesNotSetAttributes() throws ServletException, IOException {
        // Given: request with null Authorization header (empty string)
        request.addHeader("Authorization", "");

        // When: filter processes the request
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then: user attributes should NOT be set
        assertNull(request.getAttribute("userId"));
        assertNull(request.getAttribute("userRole"));

        // And: filter chain should still be called
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testQueryParameterWithSseEndpoint() throws ServletException, IOException {
        // Given: SSE endpoint request with token in query parameter
        request.setRequestURI("/api/chat/stream");
        request.setQueryString("token=" + VALID_TOKEN);
        request.setParameter("token", VALID_TOKEN);
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(VALID_TOKEN)).thenReturn(USER_ID);
        when(jwtUtil.getRoleFromToken(VALID_TOKEN)).thenReturn(USER_ROLE);

        // When: filter processes the request
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then: user attributes should be set
        assertEquals(USER_ID, request.getAttribute("userId"));
        assertEquals(USER_ROLE, request.getAttribute("userRole"));

        // And: filter chain should be called
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testQueryParameterWithAdditionalParameters() throws ServletException, IOException {
        // Given: request with token and other query parameters
        request.setQueryString("sessionId=abc123&token=" + VALID_TOKEN + "&format=json");
        request.setParameter("sessionId", "abc123");
        request.setParameter("token", VALID_TOKEN);
        request.setParameter("format", "json");
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(VALID_TOKEN)).thenReturn(USER_ID);
        when(jwtUtil.getRoleFromToken(VALID_TOKEN)).thenReturn(USER_ROLE);

        // When: filter processes the request
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then: user attributes should be set
        assertEquals(USER_ID, request.getAttribute("userId"));
        assertEquals(USER_ROLE, request.getAttribute("userRole"));

        // And: other parameters should still be accessible
        assertEquals("abc123", request.getParameter("sessionId"));
        assertEquals("json", request.getParameter("format"));
    }
}
