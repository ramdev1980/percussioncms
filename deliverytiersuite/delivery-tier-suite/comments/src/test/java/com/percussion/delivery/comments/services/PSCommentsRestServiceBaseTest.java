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
package com.percussion.delivery.comments.services;

import com.percussion.delivery.comments.data.PSCommentCriteria;
import com.percussion.delivery.multitenant.PSTenantSecurityFilter;
import com.percussion.delivery.test.FakeRegistrant;
import com.percussion.delivery.test.PSFakeDataGenerator;
import com.percussion.delivery.utils.PSVersionHelper;
import com.percussion.delivery.utils.spring.PSConfigurableApplicationContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.request.RequestContextListener;

import javax.servlet.http.HttpServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/***
 * 
 * @author natechadwick
 *
 */
public abstract class PSCommentsRestServiceBaseTest extends JerseyTest {

    private static final Logger log = LogManager.getLogger(PSCommentsRestServiceBaseTest.class);
	
	private static String PERCUSSION_LIC="2012-07-04-12344";
	private static int NUM_TENANTS = 10;
	private List<FakeRegistrant> tenants;
	private String _appContext;

    /***
     * Takes the context file as an arg and spins up grizzly to
     * test rest methods.
     *
     * @param appContext
     */
    @Override
    protected Application configure() {
        ResourceConfig resourceConfig =  new ResourceConfig(PSCommentsService.class);
        resourceConfig.property("contextConfig", PSConfigurableApplicationContext.class);
        return resourceConfig;
    }


    @Override
    protected DeploymentContext configureDeployment(){
        return ServletDeploymentContext
                .forPackages("com.percussion.delivery.comments.services")
                .servletClass(HttpServlet.class)
                .contextPath("perc-comments-services")
                .addListener(ContextLoaderListener.class)
                .addListener(RequestContextListener.class)
                .addFilter(org.springframework.web.filter.DelegatingFilterProxy.class, "tenantAuthorizationFilter")
                .contextParam("contextConfigLocation", _appContext)
                .build();
    }



    public PSCommentsRestServiceBaseTest(){}

    @Before
    public void setup() throws Exception{
        super.setUp();
        this.tenants = PSFakeDataGenerator.getFakeRegistrations(NUM_TENANTS);
    }

    @After
    public void teardown(){
    	//add tear down code here.
    }

    private static String MOD_STATE_PUT_URL="/comment/moderation/defaultModerationState";
    private static String MOD_STATE_GET_URL = "/comment/defaultModerationState/";


    @Ignore
    @Test
    public void testModerationState() throws JSONException{


        Client client = ClientBuilder.newClient();

        WebTarget webTarget = client.target(MOD_STATE_PUT_URL);
        Invocation.Builder invocationBuilder =  webTarget.request(MediaType.APPLICATION_JSON);
       //  webResource.addFilter(new HTTPBasicAuthFilter("ps_manager", "newpassword"));

         JSONObject setState = new JSONObject();
         setState.put("site", "testsite");
         setState.put("state", "REJECTED");

        Response response = invocationBuilder.put(Entity.json(setState));
         Assert.assertNotNull(response);
         Assert.assertEquals("200",response.getStatus(), 200);

        Client client2 = ClientBuilder.newClient();

        WebTarget webTarget2 = client.target(MOD_STATE_GET_URL+ "testsite");
        Invocation.Builder invocationBuilder2 =  webTarget2.request(MediaType.APPLICATION_JSON);
        Response response2 = invocationBuilder.put(Entity.json(setState));

       //  response = webResource.path(MOD_STATE_GET_URL + "testsite").accept("application/json").type("application/json").get(ClientResponse.class);

         Assert.assertNotNull(response2);
         Assert.assertEquals("200",response2.getStatus(), 200);

         String ret = (String)response.getEntity();

         Assert.assertEquals(ret, "REJECTED");

    }

    @Test
    @Ignore
    public void testComment() throws JSONException{


    	//Load up 1 comment per tenant.
    	int i=1;
    	for(FakeRegistrant p : this.tenants){

            Client client = ClientBuilder.newClient();
            WebTarget webTarget = client.target("/comment");
            Invocation.Builder invocationBuilder =  webTarget.request(MediaType.APPLICATION_FORM_URLENCODED)
                    .header(PSTenantSecurityFilter.TENANTID_PARAM_NAME, p.getGUID());


            webTarget.queryParam(IPSCommentRestService.FORM_PARAM_EMAIL,p.getEmailAddress() );
            webTarget.queryParam(IPSCommentRestService.FORM_PARAM_PAGEPATH, "/index");
            webTarget.queryParam(IPSCommentRestService.FORM_PARAM_SITE, "www." + p.getDomain());
            webTarget.queryParam(IPSCommentRestService.FORM_PARAM_TAGS, "sametag");
            webTarget.queryParam(IPSCommentRestService.FORM_PARAM_TEXT, "Tenant " + p.getGUID() + " was born " + p.getBirthday());
            webTarget.queryParam(IPSCommentRestService.FORM_PARAM_TITLE, "Tenant " + i + "tests");
            webTarget.queryParam(IPSCommentRestService.FORM_PARAM_URL, "http://www." + p.getDomain() + "/index");
            webTarget.queryParam(IPSCommentRestService.FORM_PARAM_USERNAME, p.getUsername());
	    	log.info("Tenant ID: {}", p.getGUID());

	    //   response = webResource.path("/comment").entity(queryParams).type("application/x-www-form-urlencoded").header(PSTenantSecurityFilter.TENANTID_PARAM_NAME, p.getGUID()).post(ClientResponse.class);
            Response response = invocationBuilder.get();
	       Assert.assertNotNull(response);

	       log.info(response.getEntity());
	       //Assert.assertEquals(200, response.getStatus());


	       PSCommentCriteria crit = new PSCommentCriteria();

		   JSONObject postJson = new JSONObject();
		   postJson.put("site", "www." + p.getDomain());
		   postJson.put("pagepath", "/index");

//	      //Now we want to Approve the comments.
//	      response = webResource.path("/comment/moderation/asmoderator").entity(postJson.toString()).type("application/json").accept("application/json").header(PSTenantSecurityFilter.TENANTID_PARAM_NAME, p.getGUID()).post(ClientResponse.class);
//
//	      Assert.assertNotNull(response);
	      ///Assert.assertEquals(200,response.getStatus());
	     //TODO: Fix me - this is not working.@see PSCommentService in site manage and figure out how they are doing it.

	      i++;
    	}


    	//Now that each tenant has some comments in the db
    	//we want to try and access any of the other guys content
    	//when logged in as a different tenant.
    	//TODO: Finish me


    }

    @Test
    @Ignore
    public void testAddCommentWithGoodLicense(){

    	FakeRegistrant p = tenants.get(0);

        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target("/comment");
        Invocation.Builder invocationBuilder =  webTarget.request(MediaType.APPLICATION_FORM_URLENCODED)
                .header(PSTenantSecurityFilter.TENANTID_PARAM_NAME, PERCUSSION_LIC);


        webTarget.queryParam(IPSCommentRestService.FORM_PARAM_EMAIL,p.getEmailAddress() );
        webTarget.queryParam(IPSCommentRestService.FORM_PARAM_PAGEPATH, "/index");
        webTarget.queryParam(IPSCommentRestService.FORM_PARAM_SITE, "www." + p.getDomain());
        webTarget.queryParam(IPSCommentRestService.FORM_PARAM_TAGS, "sametag");
        webTarget.queryParam(IPSCommentRestService.FORM_PARAM_TEXT, "Tenant " + p.getGUID() + " was born " + p.getBirthday());
        webTarget.queryParam(IPSCommentRestService.FORM_PARAM_TITLE, "Good license tests");
        webTarget.queryParam(IPSCommentRestService.FORM_PARAM_URL, "http://www." + p.getDomain() + "/index");
        webTarget.queryParam(IPSCommentRestService.FORM_PARAM_USERNAME, p.getUsername());
    	log.info("Tenant ID: {}", PERCUSSION_LIC);
        Response response = invocationBuilder.get();
      // response = webResource.path("/comment").entity(queryParams).type("application/x-www-form-urlencoded").header(PSTenantSecurityFilter.TENANTID_PARAM_NAME, PERCUSSION_LIC).post(ClientResponse.class);

       Assert.assertNotNull(response);


    }

    @Test
    @Ignore
	public void testGetRestVersion(){

        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target("/comment/version");
        Invocation.Builder invocationBuilder =  webTarget.request(MediaType.APPLICATION_JSON);
        Response response = invocationBuilder.get();

        Assert.assertNotNull(response);
        Assert.assertEquals(200,response.getStatus());
        Assert.assertEquals(testGetVersion(), response.getEntity());
	}


	private String testGetVersion(){
		String version = PSVersionHelper.getVersion(this.getClass());
		Assert.assertNotNull(version);
		System.out.print(version);
		return version;
	}
    @Override
    protected TestContainerFactory getTestContainerFactory() {
        return new GrizzlyWebTestContainerFactory();
    }


}
