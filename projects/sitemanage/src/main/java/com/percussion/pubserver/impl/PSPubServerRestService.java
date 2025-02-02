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
package com.percussion.pubserver.impl;

import com.amazonaws.regions.Regions;
import com.percussion.delivery.data.PSDeliveryInfo;
import com.percussion.delivery.service.IPSDeliveryInfoService;
import com.percussion.delivery.service.PSDeliveryInfoServiceLocator;
import com.percussion.delivery.service.impl.PSDeliveryInfoService;
import com.percussion.error.PSExceptionUtils;
import com.percussion.pubserver.IPSPubServerService;
import com.percussion.pubserver.data.PSPublishServerInfo;
import com.percussion.pubserver.data.PSPublishServerInfoList;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSParameterValidationUtils;
import com.percussion.share.service.exception.PSParametersValidationException;
import com.percussion.share.service.exception.PSValidationException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author leonardohildt
 * @author ignacioerro
 */
@Path("/servers")
@Component("pubServerRestService")
@Lazy
public class PSPubServerRestService
{
    private static final Logger log = LogManager.getLogger(PSPubServerRestService.class);

    private IPSPubServerService service;
    private PSDeliveryInfoService psDeliveryInfoService;
    private List<PSDeliveryInfo> psDeliveryInfoServiceList;

    @Autowired
    public PSPubServerRestService(IPSPubServerService service)
    {
        this.service = service;
    }

    /**
     * Load the server information based on the server name as the parameter.
     *
     * @param siteId the id of the site of which it is going to retrieve
     *            the information from. Cannot be empty or <code>null</code>
     * @param serverId The id of the server.
     * @return a <code>PSPublishServerInfo</code> object.
     */
    @GET
    @Path("/{siteId}/{serverId}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSPublishServerInfo getPubServer(@PathParam("siteId")
                                                    String siteId, @PathParam("serverId")
                                                    String serverId)
    {
        try {
            return service.getPubServer(siteId, serverId);
        } catch (IPSPubServerService.PSPubServerServiceException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/{siteId}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<PSPublishServerInfo> getServers(@PathParam("siteId")
                                                        String siteId)
    {
        try {
            return new PSPublishServerInfoList(service.getPubServerList(siteId));
        } catch (IPSPubServerService.PSPubServerServiceException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e);
        }
    }

    /**
     * Creates a new server with the name provided.
     *
     * @param siteId the id of the site to be created
     * @param serverName The name of the pub server
     * @param pubServerInfo the <code>PSPublishServer</code> object containing the
     *      *            server information to add to that server. Must not be empty or
     *      *            <code>null</code>
     * @return a <code>PSPublishServerInfo</code> object never empty or
     *         <code>null</code>
     */
    @POST
    @Path("/{siteId}/{serverName:.*}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSPublishServerInfo createPubServer(@PathParam("siteId")
                                                       String siteId, @PathParam("serverName")
                                                       String serverName, PSPublishServerInfo pubServerInfo) throws PSValidationException,WebApplicationException
    {
        try {
            PSParameterValidationUtils.rejectIfBlank("create", "siteId", siteId);
            PSParameterValidationUtils.rejectIfBlank("create", "serverName", serverName);
            return service.createPubServer(siteId, serverName, pubServerInfo);
        }catch(PSParametersValidationException ps){
            log.error(ps.getMessage());
            log.debug(ps.getMessage(),ps);
            throw ps;
        } catch (PSNotFoundException | IPSPubServerService.PSPubServerServiceException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e);
        }
    }

    /**
     * Updates a new server with the name and data provided.
     *
     * @param siteId the id of the site
     * @param serverId The id of the pub server.
     * @param pubServerInfo the <code>PSPublishServerInfo</code> object to be updated
     * @return a <code>PSPublishServerInfo</code> object never empty or
     *         <code>null</code>
     */
    @PUT
    @Path("/{siteId}/{serverId:.*}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public PSPublishServerInfo updatePubServer(@PathParam("siteId")
                                                       String siteId, @PathParam("serverId")
                                                       String serverId, PSPublishServerInfo pubServerInfo) throws PSParametersValidationException {
        try {
            PSParameterValidationUtils.rejectIfBlank("update", "siteId", siteId);
            PSParameterValidationUtils.rejectIfBlank("update", "serverId", serverId);

            return service.updatePubServer(siteId, serverId, pubServerInfo);
        }catch(PSParametersValidationException ps){
            log.error(ps.getMessage());
            log.debug(ps.getMessage(),ps);
            throw ps;
        } catch (PSDataServiceException | PSNotFoundException | IPSPubServerService.PSPubServerServiceException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e);
        }
    }

    /**
     * Deletes the server with the name provided
     *
     * @param siteId the id of the site. Never <code>null</code>
     * @param serverId the id of the server. Never <code>null</code>
     * @return a list of servers updated
     */
    @DELETE
    @Path("/{siteId}/{serverId}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<PSPublishServerInfo> deleteServer(@PathParam("siteId")
                                                          String siteId, @PathParam("serverId")
                                                          String serverId)
    {
        try {
            PSParameterValidationUtils.rejectIfBlank("delete", "siteName", siteId);
            PSParameterValidationUtils.rejectIfBlank("delete", "serverId", serverId);

            return new PSPublishServerInfoList(service.deleteServer(siteId, serverId));
        } catch (IPSPubServerService.PSPubServerServiceException | PSDataServiceException | PSNotFoundException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e);
        }
    }

    @POST
    @Path("/stopPublishing/{jobId}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public void stopPublishing(@PathParam("jobId")
                                       String jobId)
    {
        try {
            PSParameterValidationUtils.rejectIfBlank("delete", "jobId", jobId);

            service.stopPublishing(jobId);
        } catch (PSValidationException | IPSPubServerService.PSPubServerServiceException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e);
        }
    }

    /**
     * Get information about drivers availability
     *
     * @return a <code>String</code> of the JSON object from a
     *         <code>Map<String, Boolean></code> (DriverName,available).
     */
    @GET
    @Path("/availableDrivers")
    @Produces(MediaType.TEXT_PLAIN)
    public String getAvailableDrivers()
    {
        return JSONObject.fromObject(service.getAvailableDrivers()).toString();
    }

    /**
     * Get EC2 Bucket Regions
     *
     * @return a List of <code>Region</code> of the JSON object.
     */
    @GET
    @Path("/availableRegions")
    @Produces(MediaType.TEXT_PLAIN)
    public String getAvailableRegions()
    {
        Regions[] regions =  Regions.values();
        if(regions !=null){
            String[] regionNames = new String[regions.length];
            for (int i=0;i<regions.length;i++){
                Regions region =regions[i];
                regionNames[i] = region.getName();
            }
            return  JSONArray.fromObject(regionNames).toString();
        }
        return  null;
    }


    @GET
    @Path("/availablePublishingServer/{publishServerType}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getAvailablePublishingServer(@PathParam("publishServerType") String publishServer) {
        psDeliveryInfoService = (PSDeliveryInfoService) PSDeliveryInfoServiceLocator.getDeliveryInfoService();
        List<String> serverList = psDeliveryInfoService.getAdminUrls(publishServer);
        serverList.add(IPSPubServerService.DEFAULT_DTS);

        return JSONArray.fromObject(serverList.toArray()).toString();

    }
    /**
     * Get if EC2 instance
     *
     * @return a boolean true, if EC2 instance else false
     */
    @GET
    @Path("/isEC2Instance")
    @Produces(MediaType.TEXT_PLAIN)
    public Boolean isEc2Instance()
    {
        return PSPubServerService.isEC2Instance();
    }

    /**
     * Determine if the default publish server that belongs to a site is
     * modified or not.
     *
     * @param siteId The id of the site that contains the publish server.
     * @return <code>true</code> if the default server was modified by the user.
     */
    @GET
    @Path("/isDefaultServerModified/{siteId}")
    @Produces(MediaType.TEXT_PLAIN)
    public Boolean isDefaultServerModified(@PathParam("siteId")
                                                   String siteId)
    {
        return service.isDefaultServerModified(siteId);
    }

    @GET
    @Path("/defaultFolderLocation/{siteId}/{publishType}/{driver}/{serverType}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getDefaultFolderLocation(@PathParam("siteId")
                                                   String siteId, @PathParam("publishType")
                                                   String publishType, @PathParam("driver")
                                                   String driver, @PathParam("serverType") String serverType)
    {
        try {
            PSParameterValidationUtils.rejectIfBlank("defaultFolderLocation", "siteId", siteId);
            PSParameterValidationUtils.rejectIfBlank("defaultFolderLocation", "publishType", publishType);
            PSParameterValidationUtils.rejectIfBlank("defaultFolderLocation", "driver", driver);

            return service.getDefaultFolderLocation(siteId, publishType, driver, serverType);
        } catch (PSValidationException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/availableDeliveryServers")
    @Produces(MediaType.TEXT_PLAIN)
    public String getAvailableDeliveryServers(){
        IPSDeliveryInfoService svc = PSDeliveryInfoServiceLocator.getDeliveryInfoService();

        return JSONObject.fromObject(svc.findAll()).toString();
    }
}
