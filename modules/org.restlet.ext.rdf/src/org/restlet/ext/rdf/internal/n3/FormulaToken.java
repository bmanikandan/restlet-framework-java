/**
 * Copyright 2005-2009 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
 * "Licenses"). You can select the license that you prefer but you may not use
 * this file except in compliance with one of these Licenses.
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1.php
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1.php
 * 
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine
 * 
 * Restlet is a registered trademark of Noelios Technologies.
 */

package org.restlet.ext.rdf.internal.n3;

import java.io.IOException;

/**
 * Allows to parse a formula in RDF N3 notation. Please note that this kind of
 * feature is not supported yet.
 */
public class FormulaToken extends LexicalUnit {

    @Override
    public Object resolve() {
        // TODO Auto-generated method stub
        return null;
    }

    public FormulaToken(RdfN3ParsingContentHandler contentHandler, Context context)
            throws IOException {
        super(contentHandler, context);
        this.parse();
    }

    @Override
    public void parse() throws IOException {
        getContentHandler().step();
        do {
            getContentHandler().parseStatement(new Context());
        } while (getContentHandler().getChar() != RdfN3ParsingContentHandler.EOF
                && getContentHandler().getChar() != '}');
        if (getContentHandler().getChar() == '}') {
            // Set the cursor at the right of the list token.
            getContentHandler().step();
        }
    }
}