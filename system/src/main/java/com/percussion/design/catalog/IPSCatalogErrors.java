/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.design.catalog;


/**
 * The IPSCatalogErrors inteface is provided as a convenient mechanism
 * for accessing the various catalog related error codes. The catalog error
 * code ranges are:
 * <TABLE BORDER="1">
 * <TR><TH>Range</TH><TH>Component</TH></TR>
 * <TR><TD>4000 - 4100</TD><TD>general errors used all over</TD></TR>
 * <TR><TD>4101 - 4300</TD><TD>client-side classes</TD></TR>
 * <TR><TD>4301 - 4500</TD><TD>server-side classes</TD></TR>
 * <TR><TD>4501 - 4999</TD><TD>-unassigned-</TD></TR>
 * </TABLE>
 *
 * @author      Tas Giakouminakis
 * @version      1.0
 * @since      1.0
 */
public interface IPSCatalogErrors {

   /**
    * The required request property was not specified in the catalog call.
    * <p>
    * The arguments passed in for this message are:
    * <TABLE BORDER="1">
    * <TR><TH>Arg</TH><TH>Description</TH></TR>
    * <TR><TD>0</TD><TD>the name of the property</TD></TR>
    * </TABLE>
    */
   public static final int REQD_PROP_NOT_SPECIFIED      = 4101;

   /**
    * The request category specified does not match the expected value.
    * <p>
    * The arguments passed in for this message are:
    * <TABLE BORDER="1">
    * <TR><TH>Arg</TH><TH>Description</TH></TR>
    * <TR><TD>0</TD><TD>the value expected</TD></TR>
    * <TR><TD>1</TD><TD>the value specified</TD></TR>
    * </TABLE>
    */
   public static final int REQ_CATEGORY_INVALID         = 4102;

   /**
    * The request type specified does not match the expected value.
    * <p>
    * The arguments passed in for this message are:
    * <TABLE BORDER="1">
    * <TR><TH>Arg</TH><TH>Description</TH></TR>
    * <TR><TD>0</TD><TD>the value expected</TD></TR>
    * <TR><TD>1</TD><TD>the value specified</TD></TR>
    * </TABLE>
    */
   public static final int REQ_TYPE_INVALID            = 4103;

   /**
    * An exception occurred loading the request generator
    * for the specified category/type.
    * <p>
    * The arguments passed in for this message are:
    * <TABLE BORDER="1">
    * <TR><TH>Arg</TH><TH>Description</TH></TR>
    * <TR><TD>0</TD><TD>request category</TD></TR>
    * <TR><TD>1</TD><TD>request type</TD></TR>
    * <TR><TD>2</TD><TD>Exception.getMessage</TD></TR>
    * </TABLE>
    */
   public static final int REQ_HANDLER_EXCEPTION      = 4104;

   /**
    * The PSCataloger constructor was called with null
    * as the PSDesignerConnection object.
    * <p>
    * No arguments are passed for this message.
    */
   public static final int CONN_OBJ_NULL               = 4105;

   /**
    * a request document was not supplied with the request.
    * <p>
    * The arguments passed in for this message are:
    * <TABLE BORDER="1">
    * <TR><TH>Arg</TH><TH>Description</TH></TR>
    * <TR><TD>0</TD><TD>request category</TD></TR>
    * <TR><TD>1</TD><TD>request type</TD></TR>
    * <TR><TD>2</TD><TD>XML request document type (DTD name)</TD></TR>
    * </TABLE>
    */
   public static final int REQ_DOC_MISSING            = 4301;

   /**
    * a request document was not supplied with the request .
    */
   public static final int REQ_DOC_MISSING_GENERIC      = 4302;

   /**
    * a request document was not supplied with the request
    */
   public static final int REQ_DOC_ROOT_MISSING_GENERIC   = 4303;

   /**
    * the request document sent is not of the expected type.
    * <p>
    * The arguments passed in for this message are:
    * <TABLE BORDER="1">
    * <TR><TH>Arg</TH><TH>Description</TH></TR>
    * <TR><TD>0</TD><TD>the document type (DTD) expected</TD></TR>
    * <TR><TD>1</TD><TD>the document type (DTD) specified</TD></TR>
    * </TABLE>
    */
   public static final int REQ_DOC_INVALID_TYPE         = 4304;

   /**
    * a request handler could not be found to process the request.
    * <p>
    * The arguments passed in for this message are:
    * <TABLE BORDER="1">
    * <TR><TH>Arg</TH><TH>Description</TH></TR>
    * <TR><TD>0</TD><TD>request category</TD></TR>
    * <TR><TD>1</TD><TD>request type</TD></TR>
    * </TABLE>
    */
   public static final int NO_REQ_HANDLER_FOUND         = 4305;

   /**
    * an exception occurred trying to load the the properties file.
    * <p>
    * The arguments passed in for this message are:
    * <TABLE BORDER="1">
    * <TR><TH>Arg</TH><TH>Description</TH></TR>
    * <TR><TD>0</TD><TD>request category</TD></TR>
    * <TR><TD>1</TD><TD>request type</TD></TR>
    * <TR><TD>2</TD><TD>exception text</TD></TR>
    * </TABLE>
    */
   public static final int PROPS_LOAD_EXCEPTION         = 4306;

   /**
    * the specified exit handler class could not be loaded.
    * <p>
    * The arguments passed in for this message are:
    * <TABLE BORDER="1">
    * <TR><TH>Arg</TH><TH>Description</TH></TR>
    * <TR><TD>0</TD><TD>the class name</TD></TR>
    * </TABLE>
    */
   public static final int EXIT_HANDLER_CLASS_NOT_FOUND   = 4307;

   /**
    * the IPSExtensionHandler or IPSExtension interface is not implemented by this class
    * (all extension handlers must implement IPSExtensionHandler, and all extensions
    * must implement IPSExtension).
    * <p>
    * The arguments passed in for this message are:
    * <TABLE BORDER="1">
    * <TR><TH>Arg</TH><TH>Description</TH></TR>
    * <TR><TD>0</TD><TD>the class name</TD></TR>
    * </TABLE>
    */
   public static final int IPSEXITHANDLER_NOT_IMPLEMENTED   = 4308;

   /**
    * an exception occurred trying to load the exit handler class.
    * <p>
    * The arguments passed in for this message are:
    * <TABLE BORDER="1">
    * <TR><TH>Arg</TH><TH>Description</TH></TR>
    * <TR><TD>0</TD><TD>the class name</TD></TR>
    * <TR><TD>1</TD><TD>exceptin text</TD></TR>
    * </TABLE>
    */
   public static final int EXIT_HANDLER_CLASS_LOAD_EXCEPTION   = 4309;

   /**
    * an exception occurred trying to load the exit handler class.
    * <p>
    * The arguments passed in for this message are:
    * <TABLE BORDER="1">
    * <TR><TH>Arg</TH><TH>Description</TH></TR>
    * <TR><TD>0</TD><TD>the session id of the requestor</TD></TR>
    * <TR><TD>1</TD><TD>catalog request category</TD></TR>
    * <TR><TD>2</TD><TD>catalog request type</TD></TR>
    * </TABLE>
    */
   public static final int CATALOG_ERROR               = 4310;

   /**
    * an exception occurred while processing the catalog request.
    * <p>
    * The arguments passed in for this message are:
    * <TABLE BORDER="1">
    * <TR><TH>Arg</TH><TH>Description</TH></TR>
    * <TR><TD>0</TD><TD>exceptin text</TD></TR>
    * </TABLE>
    */
   public static final int CATALOG_EXCEPTION            = 4311;
}

