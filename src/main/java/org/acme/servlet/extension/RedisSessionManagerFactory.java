package org.acme.servlet.extension;

import static javax.servlet.DispatcherType.REQUEST;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.server.session.InMemorySessionManager;
import io.undertow.server.session.SessionCookieConfig;
import io.undertow.server.session.SessionManager;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.SessionManagerFactory;

/**
 * Creates a SessionManager to manage the HttpSession / VaadinSession. Reads the Redis-URI from the environment variable. If the provided URI for the
 * Redis-cluster is invalid, a default {@link InMemorySessionManager} will be used.
 *
 * @author Inacta AG
 * @since 1.0.0
 */
public class RedisSessionManagerFactory implements SessionManagerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(RedisSessionManagerFactory.class);
    private final int maxSessions;
    private final boolean expireOldestUnusedSessionOnMax;

    /**
     * This constructor sets the limit of sessions to be handled by the default {@link InMemorySessionManager}. If old and unused sessions should be
     * removed when reaching the maxSessions, set expireOldestUnusedSessionOnMax to <CODE>true</CODE>
     *
     * * @param maxSessions the maximum of sessions to be kept inMemory by the {@link InMemorySessionManager}
     * 
     * @param expireOldestUnusedSessionOnMax
     *            Allows the {@link InMemorySessionManager} to remove unused sessions if maxSessions is reached.
     */
    public RedisSessionManagerFactory(final int maxSessions, final boolean expireOldestUnusedSessionOnMax) {

        this.maxSessions = maxSessions;
        this.expireOldestUnusedSessionOnMax = expireOldestUnusedSessionOnMax;
    }

    @Override
    public SessionManager createSessionManager(final Deployment deployment) {

        configureVaadinSessionRewriteFilter(deployment);
        LOG.info("Configuring RedisSessionManager");
        final RedisSessionManager redisSessionManager = new RedisSessionManager(new SessionCookieConfig(), createInMemorySessionManager(deployment));

        if (redisSessionManager.isConnectedToRedis()) {
            return redisSessionManager;
        }

        return createInMemorySessionManager(deployment);

    }

    private void configureVaadinSessionRewriteFilter(final Deployment deployment) {

        final FilterInfo vaadinSessionRewriteFilter = new FilterInfo(QuarkusRedisFilter.class.getName(), QuarkusRedisFilter.class);
        deployment.getDeploymentInfo().addFilter(vaadinSessionRewriteFilter);
        deployment.getDeploymentInfo().addFilterUrlMapping(QuarkusRedisFilter.class.getName(), "/*", REQUEST);
    }

    private SessionManager createInMemorySessionManager(final Deployment deployment) {

        LOG.info("Configuring InMemorySessionManager");
        return new InMemorySessionManager(deployment.getDeploymentInfo().getSessionIdGenerator(), deployment.getDeploymentInfo().getDeploymentName(),
                this.maxSessions, this.expireOldestUnusedSessionOnMax, deployment.getDeploymentInfo().getMetricsCollector() != null);
    }

}
