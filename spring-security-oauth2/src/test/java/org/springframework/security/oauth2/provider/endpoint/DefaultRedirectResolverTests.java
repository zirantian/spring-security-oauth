/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.security.oauth2.provider.endpoint;

import org.junit.Before;
import org.junit.Test;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.common.exceptions.RedirectMismatchException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 */
public class DefaultRedirectResolverTests {

	private DefaultRedirectResolver resolver;

	private BaseClientDetails client;

	@Before
	public void setup() {
		client = new BaseClientDetails();
		client.setAuthorizedGrantTypes(Collections.singleton("authorization_code"));
		resolver = new DefaultRedirectResolver();
	}

	@Test
	public void testRedirectMatchesRegisteredValue() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com"));
		client.setRegisteredRedirectUri(redirectUris);
		String requestedRedirect = "https://anywhere.com";
		assertEquals(requestedRedirect, resolver.resolveRedirect(requestedRedirect, client));
	}

	@Test(expected = InvalidRequestException.class)
	public void testRedirectWithNoRegisteredValue() throws Exception {
		String requestedRedirect = "https://anywhere.com/myendpoint";
		resolver.resolveRedirect(requestedRedirect, client);
	}

	// If only one redirect has been registered, then we should use it
	@Test
	public void testRedirectWithNoRequestedValue() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com"));
		client.setRegisteredRedirectUri(redirectUris);
		resolver.resolveRedirect(null, client);
	}

	// If multiple redirects registered, then we should get an exception
	@Test(expected = RedirectMismatchException.class)
	public void testRedirectWithNoRequestedValueAndMultipleRegistered() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com", "http://nowhere.com"));
		client.setRegisteredRedirectUri(redirectUris);
		resolver.resolveRedirect(null, client);
	}

	@Test(expected = InvalidGrantException.class)
	public void testNoGrantType() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com", "http://nowhere.com"));
		client.setRegisteredRedirectUri(redirectUris);
		client.setAuthorizedGrantTypes(Collections.<String>emptyList());
		resolver.resolveRedirect(null, client);
	}

	@Test(expected = InvalidGrantException.class)
	public void testWrongGrantType() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com", "http://nowhere.com"));
		client.setRegisteredRedirectUri(redirectUris);
		client.setAuthorizedGrantTypes(Collections.singleton("client_credentials"));
		resolver.resolveRedirect(null, client);
	}

	@Test(expected = InvalidGrantException.class)
	public void testWrongCustomGrantType() throws Exception {
		resolver.setRedirectGrantTypes(Collections.singleton("foo"));
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com", "http://nowhere.com"));
		client.setRegisteredRedirectUri(redirectUris);
		resolver.resolveRedirect(null, client);
	}

	@Test(expected = RedirectMismatchException.class)
	public void testRedirectNotMatching() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("http://nowhere.com"));
		String requestedRedirect = "https://anywhere.com/myendpoint";
		client.setRegisteredRedirectUri(redirectUris);
		assertEquals(redirectUris.iterator().next(), resolver.resolveRedirect(requestedRedirect, client));
	}

	@Test(expected = RedirectMismatchException.class)
	public void testRedirectNotMatchingWithTraversal() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com/foo"));
		String requestedRedirect = "https://anywhere.com/foo/../bar";
		client.setRegisteredRedirectUri(redirectUris);
		assertEquals(redirectUris.iterator().next(), resolver.resolveRedirect(requestedRedirect, client));
	}

	// gh-1331
	@Test(expected = RedirectMismatchException.class)
	public void testRedirectNotMatchingWithHexEncodedTraversal() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com/foo"));
		client.setRegisteredRedirectUri(redirectUris);
		String requestedRedirect = "https://anywhere.com/foo/%2E%2E";	// hexadecimal encoding of '..' represents '%2E%2E'
		resolver.resolveRedirect(requestedRedirect, client);
	}

	// gh-747
	@Test(expected = RedirectMismatchException.class)
	public void testRedirectNotMatchingSubdomain() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com/foo"));
		client.setRegisteredRedirectUri(redirectUris);
		resolver.resolveRedirect("http://2anywhere.com/foo", client);
	}

	// gh-747
	@Test
	public void testRedirectMatchingSubdomain() throws Exception {
		resolver.setMatchSubdomains(true);
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com/foo"));
		String requestedRedirect = "https://2.anywhere.com/foo";
		client.setRegisteredRedirectUri(redirectUris);
		assertEquals(requestedRedirect, resolver.resolveRedirect(requestedRedirect, client));
	}

	@Test(expected = RedirectMismatchException.class)
	public void testRedirectMatchSubdomainsDefaultsFalse() {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com"));
		client.setRegisteredRedirectUri(redirectUris);
		resolver.resolveRedirect("https://2.anywhere.com", client);
	}

	// gh-746
	@Test(expected = RedirectMismatchException.class)
	public void testRedirectNotMatchingPort() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com:90"));
		client.setRegisteredRedirectUri(redirectUris);
		resolver.resolveRedirect("https://anywhere.com:91/foo", client);
	}

	// gh-746
	@Test
	public void testRedirectMatchingPort() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com:90"));
		client.setRegisteredRedirectUri(redirectUris);
		String requestedRedirect = "https://anywhere.com:90";
		assertEquals(requestedRedirect, resolver.resolveRedirect(requestedRedirect, client));
	}

	// gh-746
	@Test(expected = RedirectMismatchException.class)
	public void testRedirectRegisteredPortSetRequestedPortNotSet() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com:90"));
		client.setRegisteredRedirectUri(redirectUris);
		resolver.resolveRedirect("https://anywhere.com/foo", client);
	}

	// gh-746
	@Test(expected = RedirectMismatchException.class)
	public void testRedirectRegisteredPortNotSetRequestedPortSet() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com"));
		client.setRegisteredRedirectUri(redirectUris);
		resolver.resolveRedirect("https://anywhere.com:8443/foo", client);
	}

	// gh-746
	@Test
	public void testRedirectMatchPortsFalse() throws Exception {
		resolver.setMatchPorts(false);
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com:90"));
		client.setRegisteredRedirectUri(redirectUris);
		String requestedRedirect = "https://anywhere.com:91";
		assertEquals(requestedRedirect, resolver.resolveRedirect(requestedRedirect, client));
	}

	// gh-1566
	@Test(expected = RedirectMismatchException.class)
	public void testRedirectRegisteredUserInfoNotMatching() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://userinfo@anywhere.com"));
		client.setRegisteredRedirectUri(redirectUris);
		resolver.resolveRedirect("https://otheruserinfo@anywhere.com", client);
	}

	// gh-1566
	@Test(expected = RedirectMismatchException.class)
	public void testRedirectRegisteredNoUserInfoNotMatching() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://userinfo@anywhere.com"));
		client.setRegisteredRedirectUri(redirectUris);
		resolver.resolveRedirect("https://anywhere.com", client);
	}

	// gh-1566
	@Test()
	public void testRedirectRegisteredUserInfoMatching() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://userinfo@anywhere.com"));
		client.setRegisteredRedirectUri(redirectUris);
		String requestedRedirect = "https://userinfo@anywhere.com";
		assertEquals(requestedRedirect, resolver.resolveRedirect(requestedRedirect, client));
	}

	// gh-1566
	@Test()
	public void testRedirectRegisteredFragmentIgnoredAndStripped() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://userinfo@anywhere.com/foo/bar#baz"));
		client.setRegisteredRedirectUri(redirectUris);
		String requestedRedirect = "https://userinfo@anywhere.com/foo/bar";
		assertEquals(requestedRedirect, resolver.resolveRedirect(requestedRedirect + "#bar", client));
	}

	// gh-1566
	@Test()
	public void testRedirectRegisteredQueryParamsMatching() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com/?p1=v1&p2=v2"));
		client.setRegisteredRedirectUri(redirectUris);
		String requestedRedirect = "https://anywhere.com/?p1=v1&p2=v2";
		assertEquals(requestedRedirect, resolver.resolveRedirect(requestedRedirect, client));
	}

	// gh-1566
	@Test()
	public void testRedirectRegisteredQueryParamsMatchingIgnoringAdditionalParams() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com/?p1=v1&p2=v2"));
		client.setRegisteredRedirectUri(redirectUris);
		String requestedRedirect = "https://anywhere.com/?p1=v1&p2=v2&p3=v3";
		assertEquals(requestedRedirect, resolver.resolveRedirect(requestedRedirect, client));
	}

	// gh-1566
	@Test()
	public void testRedirectRegisteredQueryParamsMatchingDifferentOrder() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com/?p1=v1&p2=v2"));
		client.setRegisteredRedirectUri(redirectUris);
		String requestedRedirect = "https://anywhere.com/?p2=v2&p1=v1";
		assertEquals(requestedRedirect, resolver.resolveRedirect(requestedRedirect, client));
	}

	// gh-1566
	@Test(expected = RedirectMismatchException.class)
	public void testRedirectRegisteredQueryParamsWithDifferentValues() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com/?p1=v1&p2=v2"));
		client.setRegisteredRedirectUri(redirectUris);
		resolver.resolveRedirect("https://anywhere.com/?p1=v1&p2=v3", client);
	}

	// gh-1566
	@Test(expected = RedirectMismatchException.class)
	public void testRedirectRegisteredQueryParamsNotMatching() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com/?p1=v1"));
		client.setRegisteredRedirectUri(redirectUris);
		resolver.resolveRedirect("https://anywhere.com/?p2=v2", client);
	}

	// gh-1566
	@Test(expected = RedirectMismatchException.class)
	public void testRedirectRegisteredQueryParamsPartiallyMatching() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com/?p1=v1&p2=v2"));
		client.setRegisteredRedirectUri(redirectUris);
		resolver.resolveRedirect("https://anywhere.com/?p2=v2&p3=v3", client);
	}

	// gh-1566
	@Test
	public void testRedirectRegisteredQueryParamsMatchingWithMultipleValuesInRegistered() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com/?p1=v11&p1=v12"));
		client.setRegisteredRedirectUri(redirectUris);
		String requestedRedirect = "https://anywhere.com/?p1=v11&p1=v12";
		assertEquals(requestedRedirect, resolver.resolveRedirect(requestedRedirect, client));
	}

	// gh-1566
	@Test
	public void testRedirectRegisteredQueryParamsMatchingWithParamWithNoValue() throws Exception {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("https://anywhere.com/?p1&p2=v2"));
		client.setRegisteredRedirectUri(redirectUris);
		String requestedRedirect = "https://anywhere.com/?p1&p2=v2";
		assertEquals(requestedRedirect, resolver.resolveRedirect(requestedRedirect, client));
	}

	// gh-1618
	@Test
	public void testRedirectNoHost() {
		Set<String> redirectUris = new HashSet<String>(Arrays.asList("scheme:/path"));
		client.setRegisteredRedirectUri(redirectUris);
		String requestedRedirect = "scheme:/path";
		assertEquals(requestedRedirect, resolver.resolveRedirect(requestedRedirect, client));
	}
}
