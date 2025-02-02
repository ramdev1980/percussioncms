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

package com.percussion.delivery.utils.spring;

import org.apache.logging.log4j.LogManager;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import javax.servlet.Servlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import org.apache.logging.log4j.Logger;

public class SpringAwareGrizzlyTestContainerFactory implements TestContainerFactory {

    private Object springTarget;

    public TestContainer create(URI baseUri, DeploymentContext deploymentContext) {
        return new SpringAwareGrizzlyWebTestContainer(baseUri, deploymentContext, springTarget);
    }


    private static class SpringAwareGrizzlyWebTestContainer implements TestContainer {

        private static final Logger log = LogManager.getLogger(SpringAwareGrizzlyWebTestContainer.class.getName());
        private URI baseUri;
        private HttpServer webServer;
        private Object springTarget;
        private Servlet servletInstance;

        private SpringAwareGrizzlyWebTestContainer(URI baseUri, DeploymentContext context, Object springTarget) {
            this.springTarget = springTarget;
            this.baseUri = UriBuilder.fromUri(baseUri)
                    .path(context.getContextPath()).path(context.getContextPath())
                    .build();

            log.info("Creating Grizzly Web Container configured at the base URI " + this.baseUri);
            this.webServer = GrizzlyHttpServerFactory.createHttpServer(this.baseUri, context.getResourceConfig(), false);
        }

        public Client getClient() {

            Client client = ClientBuilder.newClient();
            return client;
        }

        @Override
        public ClientConfig getClientConfig() {
            return null;
        }

        public URI getBaseUri() {
            return baseUri;
        }

        public void start() {
            log.info("Starting the Grizzly Web Container...");

            try {
                webServer.start();
               // autoWireSpringTarget();
            } catch (IOException ex) {
                throw new TestContainerException(ex);
            }

        }

        public void stop() {
            log.info("Stopping the Grizzly Web Container...");

            webServer.shutdown();
        }

//        private void instantiateGrizzlyWebServer(DeploymentContext deploymentContext, Object springTarget) {
//            webServer = new HttpServer.createSimpleServer("",baseUri.getPort());
//            ServletAdapter sa = new ServletAdapter();
//            sa.setProperty("load-on-startup", 1);
//            servletInstance = createrServletInstance(ad.getServletClass());
//            sa.setServletInstance(servletInstance);
//
//            populateEventListeners(sa, deploymentContext.getListeners());
//            populateFilterDescriptors(sa, deploymentContext.getFilters());
//            populateContextParams(sa, ad.getContextParams());
//            populateInitParams(sa, ad.getInitParams());
//            setContextPath(sa, ad.getContextPath());
//            setServletPath(sa, ad.getServletPath());
//
//            String[] mapping = null;
//            webServer.addGrizzlyAdapter(sa, mapping);

        }

//        private void setServletPath(ServletAdapter sa, String servletPath) {
//            if ( notEmpty(servletPath)) {
//                sa.setServletPath(servletPath);
//            }
//        }
//
//        private void setContextPath(ServletAdapter sa, String contextPath) {
//            if (notEmpty(contextPath)) {
//                sa.setContextPath(ensureLeadingSlash(contextPath));
//            }
//        }

        private boolean notEmpty(String string) {
            return string != null && string.length() > 0;
        }

//        private void populateInitParams(ServletAdapter sa, Map<String, String> initParams) {
//            for (String initParamName : initParams.keySet()) {
//                sa.addInitParameter(initParamName, initParams.get(initParamName));
//            }
//
//        }
//
//        private void populateContextParams(ServletAdapter sa, Map<String, String> contextParams) {
//            for (String contextParamName : contextParams.keySet()) {
//                sa.addContextParameter(contextParamName, contextParams.get(contextParamName));
//            }
//        }
//
//        private void populateFilterDescriptors(ServletAdapter sa, List<FilterDescriptor> filters) {
//            if (filters != null) {
//                for (WebAppDescriptor.FilterDescriptor d : filters) {
//                    sa.addFilter(instantiate(d.getFilterClass()), d.getFilterName(), d.getInitParams());
//                }
//            }
//
//        }
//
//        private void populateEventListeners(ServletAdapter sa, List<Class<? extends EventListener>> listeners) {
//            for (Class<? extends EventListener> eventListener : listeners) {
//                sa.addServletListener(eventListener.getName());
//            }
//        }

        private String ensureLeadingSlash(String string) {
            return (string.startsWith("/") ? string : "/".concat(string));
        }

//        private Servlet createrServletInstance(Class<? extends HttpServlet> servletClass) {
//            return ( servletClass == null ? new SpringServlet() : instantiate(servletClass));
//        }

        private <I> I instantiate(Class<? extends I> clazz) {
            I instance = null;
            try {
                instance = clazz.newInstance();
            } catch (InstantiationException e) {
                throw new TestContainerException(e);
            } catch (IllegalAccessException e) {
                throw new TestContainerException(e);
            }
            return instance;
        }
//
//        private void autoWireSpringTarget() {
//            WebApplicationContext ctx = WebApplicationContextUtils
//                    .getRequiredWebApplicationContext(servletInstance.getServletConfig()
//                            .getServletContext());
//            AutowireCapableBeanFactory beanFactory = ctx
//                    .getAutowireCapableBeanFactory();
//            beanFactory.autowireBean(springTarget);
//        }

  //  }

}
