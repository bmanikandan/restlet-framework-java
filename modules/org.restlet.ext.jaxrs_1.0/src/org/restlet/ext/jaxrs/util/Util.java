/*
 * Copyright 2005-2008 Noelios Consulting.
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the "License"). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * http://www.opensource.org/licenses/cddl1.txt See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL HEADER in each file and
 * include the License file at http://www.opensource.org/licenses/cddl1.txt If
 * applicable, add the following below this CDDL HEADER, with the fields
 * enclosed by brackets "[]" replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of copyright owner]
 */

package org.restlet.ext.jaxrs.util;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.restlet.data.Form;
import org.restlet.data.Metadata;
import org.restlet.data.Parameter;
import org.restlet.data.Preference;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.StringRepresentation;
import org.restlet.util.DateUtils;
import org.restlet.util.Engine;

/**
 * This class contains utility methods.
 * 
 * @author Stephan Koops
 */
public class Util {

    /**
     * The header in the attribute map to return the HTTP headers.
     * 
     * @see #getHttpHeaders(Request)
     * @see #getHttpHeaders(Response)
     */
    public static final String ORG_RESTLET_HTTP_HEADERS = "org.restlet.http.headers";

    /**
     * Name of the Header Principal in the request attributes.
     * 
     * @see Principal
     */
    public static final String JAVA_SECURITY_HEADER = "java.security.Principal";

    /**
     * appends the given String to the StringBuilder. If convertBraces is true,
     * all "{" and "}" are converted to "%7B" and "%7D"
     * 
     * @param stb
     *                the Appendable to append on
     * @param string
     *                the CharSequence to append
     * @param convertBraces
     *                if true, all braces are converted, if false then not.
     * @throws IOException
     *                 If the Appendable have a problem
     */
    public static void append(Appendable stb, CharSequence string,
            boolean convertBraces) throws IOException {
        if (!convertBraces) {
            stb.append(string);
            return;
        }
        int l = string.length();
        for (int i = 0; i < l; i++) {
            char c = string.charAt(i);
            if (c == '{')
                stb.append("%7B");
            else if (c == '}')
                stb.append("%7D");
            else
                stb.append(c);
        }
    }

    /**
     * Checks, if the string contains characters that are reserved in URIs.
     * 
     * @see <a href="http://tools.ietf.org/html/rfc3986#section-2.2">RFC 3986,
     *      Section 2.2</a>
     * @param uriPart
     * @param index
     * @param errMessName
     * @throws IllegalArgumentException
     */
    public static void checkForInvalidUriChars(String uriPart, int index,
            String errMessName) throws IllegalArgumentException {
        // LATER Characters in variables should not be checked.
        int l = uriPart.length();
        for (int i = 0; i < l; i++) {
            char c = uriPart.charAt(i);
            switch (c) {
            case ':':
            case '/':
            case '?':
            case '#':
            case '[':
            case ']':
            case '@':
            case '!':
            case '$':
            case '&':
            case '\'':
            case '(':
            case ')':
            case '*':
            case '+':
            case ',':
            case ';':
            case '=':
                throw throwIllegalArgExc(index, errMessName, uriPart,
                        " contains at least one reservec character: " + c
                                + ". They must be encoded.");
            }
            if (c == ' ' || c < 32 || c >= 127)
                throw throwIllegalArgExc(index, errMessName, uriPart,
                        " contains at least one illegal character: " + c
                                + ". They must be encoded.");
        }
    }

    /**
     * Checks, if the String is a valid URI scheme
     * 
     * @param scheme
     *                the String to check.
     * @throws IllegalArgumentException
     *                 If the string is not a valid URI scheme.
     */
    public static void checkValidScheme(String scheme)
            throws IllegalArgumentException {
        if (scheme == null)
            throw new IllegalArgumentException("The scheme must not be null");
        int schemeLength = scheme.length();
        if (schemeLength == 0)
            throw new IllegalArgumentException(
                    "The scheme must not be an empty String");
        char c = scheme.charAt(0);
        if (!((c > 64 && c <= 90) || (c > 92 && c <= 118)))
            throw new IllegalArgumentException(
                    "The first character of a scheme must be an alphabetic character");
        for (int i = 1; i < schemeLength; i++) {
            c = scheme.charAt(i);
            if (!((c > 64 && c <= 90) || (c > 92 && c <= 118) || (c == '+')
                    || (c == '-') || (c == '.')))
                throw new IllegalArgumentException(
                        "The "
                                + i
                                + ". character of a scheme must be an alphabetic character, a number, a '+', a '-' or a '.'");
        }
    }

    /**
     * Copies headers into a response.
     * 
     * @param jaxRsHeaders
     *                Headers of an JAX-RS-Response.
     * @param restletResponse
     *                Restlet Response to copy the headers in.
     * @param logger
     *                The logger to use
     * @see javax.ws.rs.core.Response#getMetadata()
     */
    public static void copyResponseHeaders(
            final MultivaluedMap<String, Object> jaxRsHeaders,
            Response restletResponse, Logger logger) {
        Collection<Parameter> headers = new ArrayList<Parameter>();
        for (Map.Entry<String, List<Object>> m : jaxRsHeaders.entrySet()) {
            String headerName = m.getKey();
            for (Object headerValue : m.getValue()) {
                String hValue;
                if (headerValue == null)
                    hValue = null;
                else if (headerValue instanceof Date)
                    hValue = formatDate((Date) headerValue, false);
                // TODO temporarily constant not as cookie;
                else
                    hValue = headerValue.toString();
                headers.add(new Parameter(headerName, hValue));
            }
        }
        if (restletResponse.getEntity() == null) {
            // TODO Jerome: wie bekommt man am elegantesten ne leere
            // Repräsentation?
            restletResponse.setEntity(new StringRepresentation(""));
        }
        Engine.getInstance().copyResponseHeaders(headers, restletResponse,
                logger);
    }

    /**
     * Copies the non-null components of the supplied URI to the Reference
     * replacing any existing values for those components.
     * 
     * @param uri
     *                the URI to copy components from.
     * @param reference
     *                The Reference to copy the URI data in.
     * @throws IllegalArgumentException
     *                 if uri is null
     * @see javax.ws.rs.core.UriBuilder#uri(URI)
     */
    public static void copyUriToReference(URI uri, Reference reference)
            throws IllegalArgumentException {
        if (uri == null)
            throw new IllegalArgumentException("The URI must not be null");
        if (uri.getScheme() != null)
            reference.setScheme(uri.getScheme());
        if (uri.getAuthority() != null)
            reference.setAuthority(uri.getAuthority());
        if (uri.getHost() != null)
            reference.setHostDomain(uri.getHost());
        if (uri.getUserInfo() != null)
            reference.setUserInfo(uri.getUserInfo());
        if (uri.getPort() >= 0)
            reference.setHostPort(uri.getPort());
        if (uri.getPath() != null)
            reference.setPath(uri.getPath());
        if (uri.getQuery() != null)
            reference.setQuery(uri.getQuery());
        if (uri.getFragment() != null)
            reference.setFragment(uri.getFragment());
    }

    /**
     * Creates an modifiable Collection with the given Objects in it, and no
     * other objects. nulls will be ignored.
     * 
     * @param objects
     * @param <A>
     * @return Returns the created list with the given objects in it.
     */
    public static <A> Collection<A> createColl(A... objects) {
        return createList(objects);
    }

    /**
     * Creates an modifiable List with the given Object in it, and no other
     * objects. If the given object is null, than an empty List will returned
     * 
     * @param objects
     * @param <A>
     * @return Returns the created list with the given object in it or an empty
     *         list, if the given object is null.
     */
    public static <A> List<A> createList(A... objects) {
        List<A> list = new ArrayList<A>();
        int l = objects.length;
        for (int i = 0; i < l; i++) {
            A o = objects[i];
            if (o != null)
                list.add(o);
        }
        return list;
    }

    /**
     * Creates an modifiable Set with the given Object in it, and no other
     * objects. If the given object is null, than an empty Set will returned.
     * 
     * @param <A>
     * @param objects
     * @return the created Set
     */
    public static <A> Set<A> createSet(A... objects) {
        Set<A> set = new HashSet<A>();
        int l = objects.length;
        for (int i = 0; i < l; i++) {
            A o = objects[i];
            if (o != null)
                set.add(o);
        }
        return set;
    }

    /**
     * Encodes the given string, if encoding is enabled. If encoding is
     * disabled, the methods checks the validaty of the containing characters.
     * 
     * @param uriPart
     *                the string to encode or check. Must not be null; result
     *                are not defined.
     * @param index
     *                index in an array or list if necessary. If not necessary,
     *                set it lower than zero.
     * @param errMessName
     *                The name for the message
     * @param encode
     *                see {@link #encode}
     * @param encodeSlash
     *                if encode is true: if encodeSlash is true, than slashes
     *                are also converted, otherwise not. if encode is false,
     *                this is ignored.
     * @return
     * @throws IllegalArgumentException
     *                 if the char is invalid.
     */
    public static String encode(String uriPart, int index, String errMessName,
            boolean encode, boolean encodeSlash)
            throws IllegalArgumentException {
        if (uriPart == null)
            throw throwIllegalArgExc(index, errMessName, uriPart,
                    " must not be null");
        if (encode)
            return encodeNotBraces(uriPart, encodeSlash);
        else
            checkForInvalidUriChars(uriPart, index, errMessName);
        return uriPart;
    }

    /**
     * This methods encodes the given String, but doesn't encode braces.
     * 
     * @param uriPart
     *                the String to encode
     * @param encodeSlash
     *                if encodeSlash is true, than slashes are also converted,
     *                otherwise not.
     * @return the encoded String
     */
    public static String encodeNotBraces(String uriPart, boolean encodeSlash) {
        StringBuilder stb = new StringBuilder();
        int l = uriPart.length();
        for (int i = 0; i < l; i++) {
            char c = uriPart.charAt(i);
            if (c == '{' || c == '}' || (!encodeSlash && c == '/'))
                stb.append(c);
            else
                stb.append(Reference.encode(uriPart.substring(i, i + 1)));
        }
        return stb.toString();
    }

    /**
     * Ensures that the path starts wirh a string. if not, a slash will be added
     * at the beginning.
     * 
     * @param path
     * @return
     */
    public static String ensureStartSlash(String path) {
        if (path.startsWith("/"))
            return path;
        return "/" + path;
    }

    /**
     * Check if the given objects are equal. Can deal with null references. if
     * both elements are null, than the result is true.
     * 
     * @param object1
     * @param object2
     * @return
     */
    public static boolean equals(Object object1, Object object2) {
        if (object1 == null)
            return object2 == null;
        return object1.equals(object2);
    }

    /**
     * Converte the given Date into a String. Copied from
     * {@link com.noelios.restlet.HttpCall}.
     * 
     * @param date
     *                Date to format
     * @param cookie
     *                if true, using RFC 1036 format, otherwise RFC 1123 format.
     * @return
     */
    public static String formatDate(Date date, boolean cookie) {
        if (cookie) {
            return DateUtils.format(date, DateUtils.FORMAT_RFC_1036.get(0));
        } else {
            return DateUtils.format(date, DateUtils.FORMAT_RFC_1123.get(0));
        }
    }

    /**
     * @param coll
     * @param <A>
     * @return Returns the first Element of the collection
     * @throws IndexOutOfBoundsException
     *                 If the list is empty
     */
    public static <A> A getFirstElement(Collection<A> coll)
            throws IndexOutOfBoundsException {
        if (coll.isEmpty())
            throw new IndexOutOfBoundsException(
                    "The Collection is empty; you can't get the first element of it.");
        if (coll instanceof List)
            return ((List<A>) coll).get(0);
        return coll.iterator().next();
    }

    /**
     * @param map
     * @param <K>
     * @param <V>
     * @return Returns the first element, returned by the iterator over the
     *         map.entrySet()
     * 
     * @throws NoSuchElementException
     *                 If the map is empty.
     */
    public static <K, V> Map.Entry<K, V> getFirstEntry(Map<K, V> map)
            throws NoSuchElementException {
        return map.entrySet().iterator().next();
    }

    /**
     * @return Returns the first element, returned by the iterator over the
     *         map.keySet()
     * 
     * @param map
     * @param <K>
     * @param <V>
     * @throws NoSuchElementException
     *                 If the map is empty.
     */
    public static <K, V> K getFirstKey(Map<K, V> map)
            throws NoSuchElementException {
        return map.keySet().iterator().next();
    }

    /**
     * @return Returns the first element, returned by the iterator over the
     *         map.values()
     * @param map
     * @param <K>
     * @param <V>
     * @throws NoSuchElementException
     *                 If the map is empty.
     */
    public static <K, V> V getFirstValue(Map<K, V> map)
            throws NoSuchElementException {
        return map.values().iterator().next();
    }

    /**
     * @param request
     * @return Returns the HTTP-Headers-Form from the Request.
     */
    public static Form getHttpHeaders(Request request) {
        return (Form) request.getAttributes().get(ORG_RESTLET_HTTP_HEADERS);
    }

    /**
     * @param response
     *                a Restlet response
     * @return Returns the HTTP-Headers-Form from the Response.
     */
    public static Form getHttpHeaders(Response response) {
        return (Form) response.getAttributes().get(ORG_RESTLET_HTTP_HEADERS);
    }

    /**
     * @param list
     * @param <A>
     * @return Returns the last Element of the list
     * @throws IndexOutOfBoundsException
     *                 If the list is empty
     */
    public static <A> A getLastElement(List<A> list)
            throws IndexOutOfBoundsException {
        if (list instanceof LinkedList)
            return ((LinkedList<A>) list).getLast();
        return list.get(list.size() - 1);
    }

    /**
     * Returns the only element of the list, or null, if the List is null or
     * empty.
     * 
     * @param <A>
     * @param list
     *                a List with at most one element
     * @return The element of the List, or null, if there is no element.
     * @throws IllegalArgumentException
     *                 if the list contains more than one element.
     */
    public static <A> A getOnlyElement(List<A> list)
            throws IllegalArgumentException {
        if (list == null)
            return null;
        if (list.isEmpty())
            return null;
        if (list.size() > 1)
            throw new IllegalArgumentException(
                    "The list must have exactly one element");
        return list.get(0);
    }

    /**
     * Returns the Name of the only element of the list of the given Metadata.
     * Returns null, if the list is empty or null.
     * 
     * @param metadatas
     * @return the name of the Metadata
     * @see #getOnlyElement(List)
     */
    public static String getOnlyMetadataName(List<? extends Metadata> metadatas) {
        Metadata metadata = getOnlyElement(metadatas);
        if (metadata == null)
            return null;
        return metadata.getName();
    }

    /**
     * Gets the logged in user.
     * 
     * @param request
     *                The Restlet request
     * @return The Principal of the logged in user.
     * @see #setPrincipal(Principal, Request)
     */
    public static Principal getPrincipal(Request request) {
        return (Principal) request.getAttributes().get(JAVA_SECURITY_HEADER);
    }

    /**
     * Sets the logged in user.
     * 
     * @param principal
     *                The Principal of the logged in user.
     * @param request
     *                The Restlet request
     * @see #getPrincipal(Request)
     */
    public static void setPrincipal(Principal principal, Request request) {
        request.getAttributes().put(JAVA_SECURITY_HEADER, principal);
    }

    /**
     * This method throws an WebApplicationException for Exceptions where is no
     * planned handling. Logs the excption (warn Level).
     * 
     * @param e
     *                the catched Exception
     * @param logger
     *                the logger to log the messade
     * @param logMessage
     *                the message to log.
     * @return Will never return anyithing, because the generated exceptions
     *         will be thrown. You an formally thro the returned exception (e.g.
     *         in a catch block). So the compiler is sure, that the method will
     *         be left here.
     */
    public static RuntimeException handleException(Exception e, Logger logger,
            String logMessage) {
        logger.log(Level.WARNING, logMessage, e);
        throw new WebApplicationException(e, Status.SERVER_ERROR_INTERNAL
                .getCode());
    }

    /**
     * Checks, if the list is empty.
     * 
     * @param list
     * @return true, if the list is empty or null, or false, if the list
     *         contains elements.
     * @see #isEmpty(Object[])
     */
    public static boolean isEmpty(List<?> list) {
        return (list == null || list.isEmpty());
    }

    /**
     * Tests, if the given array is empty. Will not throw a
     * NullPointerException.
     * 
     * @param array
     * @return Returns true, if the given array ist null or has zero elements,
     *         otherwise false.
     * @see #isEmpty(List)
     */
    public static boolean isEmpty(Object[] array) {
        if (array == null || array.length == 0)
            return true;
        return false;
    }

    /**
     * Tests, if the given String is empty or "/". Will not throw a
     * NullPointerException.
     * 
     * @param string
     * @return Returns true, if the given string ist null, empty or equals "/"
     */
    public static boolean isEmptyOrSlash(String string) {
        return string == null || string.length() == 0 || string.equals("/");
    }

    /**
     * Checks, if the list contains elements.
     * 
     * @param list
     * @return true, if the list contains elements, or false, if the list is
     *         empty or null.
     */
    public static boolean isNotEmpty(List<?> list) {
        return (list != null && !list.isEmpty());
    }

    /**
     * Sorts the Metadata by it's quality into the Collections. The list is
     * ordered by the qualities, most wanted Metadata at first.
     * 
     * @param preferences
     * 
     * @return Returns a List of collections of Metadata
     */
    public static List<Collection<? extends Metadata>> sortMetadataList(
            Collection<Preference<Metadata>> preferences) {
        SortedMap<Float, Collection<Metadata>> map = new TreeMap<Float, Collection<Metadata>>(
                Collections.reverseOrder());
        for (Preference<Metadata> preference : preferences) {
            Float quality = preference.getQuality();
            Collection<Metadata> metadatas = map.get(quality);
            if (metadatas == null) {
                metadatas = new ArrayList<Metadata>(2);
                map.put(quality, metadatas);
            }
            metadatas.add(preference.getMetadata());
        }
        return new ArrayList<Collection<? extends Metadata>>(map.values());
    }

    /**
     * 
     * @param index
     *                index, starting with zero.
     * @param errMessName
     *                the name of the string with illegal characters
     * @param illegalString
     *                the illegal String
     * @param messageEnd
     * @return
     */
    private static IllegalArgumentException throwIllegalArgExc(int index,
            String errMessName, String illegalString, String messageEnd) {
        StringBuilder stb = new StringBuilder();
        stb.append("The ");
        if (index >= 0) {
            stb.append(index);
            stb.append(". ");
        }
        stb.append(errMessName);
        stb.append(" (");
        stb.append(illegalString);
        stb.append(")");
        stb.append(messageEnd);
        if (index >= 0)
            stb.append(" (index starting with 0)");
        throw new IllegalArgumentException(stb.toString());
    }

    /**
     * Creates a JAX-RS-MediaType.
     * @param type main type of the MediaType
     * @param subtype subtype of the MediaType
     * @param keysAndValues parameters (optional)
     * @return the created MediaType
     */
    public static MediaType createMediaType(String type, String subtype, String... keysAndValues)
    {
        return new MediaType(type, subtype, Util.createMap(keysAndValues));
    }
    
    /**
     * Creates a map with the given keys and values.
     * @param keysAndValues first element is key1, second element value1, third element key2, forth element value2 and so on.
     * @return
     */
    public static Map<String, String> createMap(String... keysAndValues) {
        Map<String, String> map = new HashMap<String, String>();
        for(int i=0; i<keysAndValues.length; i+=2)
            map.put(keysAndValues[i], keysAndValues[i+1]);
        return map;
    }
}
