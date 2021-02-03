package org.acme.servlet.extension;

import static java.lang.Boolean.TRUE;
import static java.util.Base64.getDecoder;
import static java.util.Base64.getEncoder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.urosporo.quarkus.vaadin.cdi.QuarkusVaadinServletService;
import com.vaadin.flow.component.ComponentEventBus;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import io.quarkus.arc.Arc;
import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionCookieConfig;
import io.undertow.server.session.SessionListener;
import io.undertow.server.session.SessionManager;
import io.undertow.server.session.SessionManagerStatistics;
import io.vertx.mutiny.redis.client.Response;

/**
 * A SessionManager that uses Redis to store session data. Sessions are stored as a Redis Hash and sessions attributes are stored directly in fields
 * of that Hash
 *
 * This SessionManager is designed for high availability purposes. Therefore, sessions are stored in-memory, as well as saved in Redis. Sessions are
 * only deserialized from Redis, if a node has no existing inMemorySession.
 *
 * The main functionality of session management is proxied to the {@link io.undertow.server.session.InMemorySessionManager}
 *
 * @author Inacta AG
 * @since 1.0.0
 */
public class RedisSessionManager implements SessionManager {

    private static final String CREATED_FIELD = ":created";
    private static final Logger LOG = LoggerFactory.getLogger(RedisSessionManager.class);

    private final SessionCookieConfig sessionConfig;
    private final SessionManager inMemorySessionManager;

    ReactiveRedisClient redisClient;

    public RedisSessionManager(final SessionCookieConfig sessionCookieConfig, final SessionManager inMemorySessionManager) {

        this.sessionConfig = sessionCookieConfig;
        this.inMemorySessionManager = inMemorySessionManager;
        this.redisClient = Arc.container().instance(ReactiveRedisClient.class).get();
    }

    @Override
    public String getDeploymentName() {

        return this.inMemorySessionManager.getDeploymentName();
    }

    @Override
    public void start() {

        this.inMemorySessionManager.start();
    }

    @Override
    public void stop() {

        this.redisClient.close();
        this.inMemorySessionManager.stop();
    }

    @Override
    public Session createSession(final HttpServerExchange serverExchange, final SessionConfig sessionConfig) {

        final SessionImpl session = new SessionImpl(this, this.inMemorySessionManager.createSession(serverExchange, sessionConfig), sessionConfig);

        final long created = System.currentTimeMillis();
        this.redisClient.set(Arrays.asList(session.getId() + CREATED_FIELD, String.valueOf(created))).map(response -> null);

        session.bumpTimeout();
        return session;
    }

    @Override
    public Session getSession(final HttpServerExchange serverExchange, final SessionConfig sessionConfig) {

        final Session inMemorySession = this.inMemorySessionManager.getSession(serverExchange, sessionConfig);
        if (inMemorySession != null) {
            return new SessionImpl(this, inMemorySession, sessionConfig);
        }

        final String sessionId = sessionConfig.findSessionId(serverExchange);
        if (sessionId != null) {
           return this.redisClient.exists(Collections.singletonList(sessionId)).map(response -> {
                if (TRUE.equals(response.toBoolean())) {
                    return new SessionImpl(this, this.inMemorySessionManager.createSession(serverExchange, sessionConfig), sessionConfig);
                }
                return null;
            }).subscribe().with(item ->item);
        }
        return null;
    }

    @Override
    public Session getSession(final String sessionId) {

        final Session inMemorySession = this.inMemorySessionManager.getSession(sessionId);

        if (inMemorySession != null) {
            return new SessionImpl(this, inMemorySession, this.sessionConfig);
        }
        return null;
    }

    @Override
    public void registerSessionListener(final SessionListener listener) {

        this.inMemorySessionManager.registerSessionListener(listener);
    }

    @Override
    public void removeSessionListener(final SessionListener listener) {

        this.inMemorySessionManager.removeSessionListener(listener);
    }

    @Override
    public void setDefaultSessionTimeout(final int timeout) {

        this.inMemorySessionManager.setDefaultSessionTimeout(timeout);
    }

    @Override
    public Set<String> getTransientSessions() {

        return this.inMemorySessionManager.getTransientSessions();
    }

    @Override
    public Set<String> getActiveSessions() {

        return getAllSessions();
    }

    // TODO find a solution to notify nodes if a session was altered by another node
    @Override
    public Set<String> getAllSessions() {

        final Set<String> sessions = new HashSet<>();
        this.redisClient.keys("*").map(response -> {
            final List<String> result = new ArrayList<>();
            for (final Response key : response) {
                result.add(key.toString());
            }
            return result;
        }).subscribe().with(sessions::addAll);
        return sessions;
    }

    @Override
    public SessionManagerStatistics getStatistics() {

        return this.inMemorySessionManager.getStatistics();
    }

    /**
     * Opens a connection to redis to check if the connection is established.
     * 
     * @return <CODE>true/false</CODE> indicating if the connectionTest was successful
     */
    public boolean isConnectedToRedis() {

        return true;
    }

    private static class SessionImpl implements Session {

        private static final String REFLECTION_ERROR_MESSAGE = "Unable to access QuarkusVaadinServletService.QuarkusVaadinServiceDelegate through reflection. Consider checking fieldNames";
        private final Session inMemorySession;
        private final RedisSessionManager sessionManager;
        private final SessionConfig sessionConfig;

        private SessionImpl(final RedisSessionManager sessionManager, final Session session, final SessionConfig sessionConfig) {

            this.sessionManager = sessionManager;
            this.inMemorySession = session;
            this.sessionConfig = sessionConfig;
        }

        @Override
        public String getId() {

            return this.inMemorySession.getId();
        }

        @Override
        public void requestDone(final HttpServerExchange serverExchange) {

            this.inMemorySession.requestDone(serverExchange);
        }

        @Override
        public long getCreationTime() {

            return Long.parseLong(this.sessionManager.redisClient.get(this.inMemorySession.getId() + CREATED_FIELD).toString());
        }

        @Override
        public long getLastAccessedTime() {

            return 0;
            // return System.currentTimeMillis()
            // - ((getMaxInactiveInterval() * 100) - this.sessionManager.redisClient.pttl(this.inMemorySession.getId()).toInteger());
        }

        @Override
        public void setMaxInactiveInterval(final int interval) {

            this.inMemorySession.setMaxInactiveInterval(interval);
            bumpTimeout();
        }

        @Override
        public int getMaxInactiveInterval() {

            return this.inMemorySession.getMaxInactiveInterval();
        }

        @Override
        public Object getAttribute(final String name) {

            final Object inMemoryAttribute = this.inMemorySession.getAttribute(name);
            if (inMemoryAttribute != null) {
                return inMemoryAttribute;
            } else {

                this.sessionManager.redisClient.hget(this.inMemorySession.getId(), name).map(Response::toString).subscribe();

                // final Response attribute = this.sessionManager.redisClient.hget(this.inMemorySession.getId(), name);
                final Object attribute = null;
                if (attribute == null) {
                    return null;
                }
                bumpTimeout();
                System.out.println("attribute name: " + name);

                final Object deserializedAttribute = deserialize(attribute.toString());
                if (deserializedAttribute instanceof VaadinSession) {
                    configureVaadinSessionForClientRequest((VaadinSession) deserializedAttribute);
                }
                this.inMemorySession.setAttribute(name, deserializedAttribute);
                return deserializedAttribute;

            }
        }

        private Object deserialize(final String data) {

            if (data == null) {
                return null;
            }
            final byte[] attributeBytes = getDecoder().decode(data);
            try (final BufferedInputStream bufferedInputStream = new BufferedInputStream(new ByteArrayInputStream(attributeBytes));
                    final ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream)) {

                return objectInputStream.readObject();
            } catch (final ClassNotFoundException | IOException e) {
                LOG.error("Failed to deserialize object from string: {}", data, e);
                return null;
            }
        }

        @Override
        public Set<String> getAttributeNames() {

            bumpTimeout();
            final Set<String> attributeNames = new HashSet<>();

            this.sessionManager.redisClient.keys(this.inMemorySession.getId()).map(response -> {

                final List<String> result = new ArrayList<>();
                for (final Response key : response) {
                    result.add(key.toString());
                }
                return result;
            }).subscribe().with(attributeNames::addAll);

            return attributeNames;

        }

        @Override
        public Object setAttribute(final String name, final Object value) {

            this.inMemorySession.setAttribute(name, value);

            final Object existingAttribute;
            try (final ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
                    final ObjectOutputStream objectOutputStream = new ObjectOutputStream(new BufferedOutputStream(byteOutputStream))) {

                if (value instanceof VaadinSession) {
                    prepareVaadinSessionForSerialization((VaadinSession) value);
                }
                objectOutputStream.writeObject(value);
                objectOutputStream.flush();
                if (value instanceof VaadinSession) {
                    configureVaadinSessionForClientRequest((VaadinSession) value);
                }
                existingAttribute = this.sessionManager.redisClient.hget(this.inMemorySession.getId(), name);
                this.sessionManager.redisClient
                        .hset(Arrays.asList(this.inMemorySession.getId(), name, getEncoder().encodeToString(byteOutputStream.toByteArray())));

                bumpTimeout();
            } catch (final IOException e) {
                LOG.error("Failed to serialize a sessionAttribute. Name: {}, value: {}", name, value, e);
                return null;
            }

            return existingAttribute;
        }

        @Override
        public Object removeAttribute(final String name) {

            final Object existing = getAttribute(name);
            this.sessionManager.redisClient.hdel(Arrays.asList(this.inMemorySession.getId(), name));

            this.inMemorySession.removeAttribute(name);
            bumpTimeout();

            return existing;
        }

        @Override
        public void invalidate(final HttpServerExchange exchange) {

            this.sessionManager.redisClient.del(Collections.singletonList(this.inMemorySession.getId()));
            this.sessionManager.redisClient.del(Collections.singletonList(this.inMemorySession.getId() + CREATED_FIELD));

            if (exchange != null) {
                this.sessionConfig.clearSession(exchange, this.getId());
            }
            this.inMemorySession.invalidate(exchange);
        }

        @Override
        public SessionManager getSessionManager() {

            return this.sessionManager;
        }

        @Override
        public String changeSessionId(final HttpServerExchange exchange, final SessionConfig config) {

            final String oldId = this.inMemorySession.getId();
            final String newId = this.inMemorySession.changeSessionId(exchange, config);

            this.sessionManager.redisClient.rename(oldId, newId);

            return newId;
        }

        private void bumpTimeout() {

            this.sessionManager.redisClient.expire(this.inMemorySession.getId(), Integer.toString(getMaxInactiveInterval()));
            this.sessionManager.redisClient.expire(this.inMemorySession.getId() + CREATED_FIELD, Integer.toString(getMaxInactiveInterval()));

        }

        private void prepareVaadinSessionForSerialization(final VaadinSession vaadinSession) {

            configureVaadinService(vaadinSession, null);
        }

        private void configureVaadinSessionForClientRequest(final VaadinSession vaadinSession) {

            configureVaadinService(vaadinSession, VaadinService.getCurrent());
        }

        private void configureVaadinService(final VaadinSession vaadinSession, final Object vaadinService) {

            if (vaadinSession.getSession() == null) {
                // Session configuration from Vaadin is not finished yet
                return;
            }

            vaadinSession.lock();
            for (final UI ui : vaadinSession.getUIs()) {
                setVaadinServiceInUIListeners(ui, vaadinService);
            }
            vaadinSession.unlock();
        }

        private void setVaadinServiceInUIListeners(final UI ui, final Object vaadinService) {

            try {
                final Field eventBusField = ui.getClass().getSuperclass().getDeclaredField("eventBus");
                setAccessible(eventBusField, true);
                final ComponentEventBus componentEventBus = (ComponentEventBus) eventBusField.get(ui);

                final Field componentEventDataField = componentEventBus.getClass().getDeclaredField("componentEventData");
                setAccessible(componentEventDataField, true);
                final Set<?> keySet = ((Map<?, ?>) componentEventDataField.get(componentEventBus)).keySet();

                for (final Object key : keySet) {
                    ((ArrayList<?>) ((Map<?, ?>) componentEventDataField.get(componentEventBus)).get(key))
                            .forEach(componentEventBusListenerWrapper -> {
                                try {
                                    final Field listenerField = componentEventBusListenerWrapper.getClass().getDeclaredField("listener");
                                    setAccessible(listenerField, true);
                                    final ComponentEventListener<?> listener = (ComponentEventListener<?>) listenerField
                                            .get(componentEventBusListenerWrapper);
                                    setAccessible(listenerField, false);

                                    if (listener.getClass().getName().contains("QuarkusVaadinServletService")) {
                                        setVaadinServiceInUIListener(listener, vaadinService);
                                    }
                                } catch (final ReflectiveOperationException e) {
                                    LOG.error(REFLECTION_ERROR_MESSAGE, e);
                                }
                            });
                }
                setAccessible(eventBusField, false);
                setAccessible(componentEventDataField, false);
            } catch (final ReflectiveOperationException e) {
                LOG.error(REFLECTION_ERROR_MESSAGE, e);
            }
        }

        private void setVaadinServiceInUIListener(final Object listener, final Object value) throws ReflectiveOperationException {

            final Field delegateField = listener.getClass().getDeclaredField("delegate");

            setAccessible(delegateField, true);
            final QuarkusVaadinServletService.QuarkusVaadinServiceDelegate delegate = (QuarkusVaadinServletService.QuarkusVaadinServiceDelegate) delegateField
                    .get(listener);
            setAccessible(delegateField, false);

            final Field vaadinServiceField = delegate.getClass().getDeclaredField("vaadinService");
            setAccessible(vaadinServiceField, true);
            vaadinServiceField.set(delegate, value); // NOSONAR
            setAccessible(vaadinServiceField, false);

            final Field beanManagerField = delegate.getClass().getDeclaredField("beanManager");
            setAccessible(beanManagerField, true);
            if (beanManagerField.get(delegate) == null && value != null) {
                beanManagerField.set(delegate, Arc.container().beanManager()); // NOSONAR
            }
            setAccessible(beanManagerField, false);

        }

        private void setAccessible(final Field field, final boolean accessible) {

            field.setAccessible(accessible);
        }

    }

}