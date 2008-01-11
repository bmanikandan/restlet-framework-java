/*
 * Copyright 2005-2007 Noelios Consulting.
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

package org.restlet.ext.jaxrs.wrappers;

import org.restlet.ext.jaxrs.PathRegExp;

/**
 * This interface describes a sub resource locator or a sub resource method.
 * See section 1.5 and 2.3.1 of JSR-311-Spec
 * 
 * @author Stephan Koops
 */
public interface SubResourceMethodOrLocator {

    /**
     * @return Returns the Regular Expression of the path.
     */
    public PathRegExp getPathRegExp();
}