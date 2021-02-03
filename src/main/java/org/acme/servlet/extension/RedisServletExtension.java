package org.acme.servlet.extension;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.System.getenv;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;

/**
 * Custom Undertow Servlet extension to register the RedisSessionManagerFactory
 *
 * @author Inacta AG
 * @since 1.0.0
 */
public class RedisServletExtension implements ServletExtension {

    public static final String INVALID_REDIS_URI_ERROR_MESSAGE = "Invalid Redis URI, check value of property: quarkus.redis.hosts";

    private static final String MAX_IN_MEMORY_SESSIONS_KEY = "ch.inacta.vaadin.redisCache.inMemory.maxSessions";
    private static final String EXPIRE_OLDEST_UNUSED_IN_MEMORY_SESSION_ON_MAX_KEY = "ch.inacta.vaadin.redisCache.inMemory.expireOldestSessionOnMax";

    private static final Logger LOG = LoggerFactory.getLogger(RedisServletExtension.class);

    @Override
    public void handleDeployment(final DeploymentInfo deploymentInfo, final ServletContext servletContext) {

        final int maxSessions = getenv().containsKey(MAX_IN_MEMORY_SESSIONS_KEY) ? parseInt(getenv(MAX_IN_MEMORY_SESSIONS_KEY)) : -1;
        final boolean expireOldestSessionOnMax = getenv().containsKey(EXPIRE_OLDEST_UNUSED_IN_MEMORY_SESSION_ON_MAX_KEY)
                && parseBoolean(getenv(EXPIRE_OLDEST_UNUSED_IN_MEMORY_SESSION_ON_MAX_KEY));

        deploymentInfo.setSessionManagerFactory(new RedisSessionManagerFactory(maxSessions, expireOldestSessionOnMax));
    }

}
