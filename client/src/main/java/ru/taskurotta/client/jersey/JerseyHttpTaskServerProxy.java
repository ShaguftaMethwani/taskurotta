package ru.taskurotta.client.jersey;

import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.ApacheHttpClientHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import ru.taskurotta.server.TaskServerResource;
import ru.taskurotta.transport.utils.TransportUtils;

import javax.annotation.PostConstruct;

/**
 * TaskServer implementation as a jersey client proxy. Uses embedded apache HTTP client for connection pooling
 */
public class JerseyHttpTaskServerProxy extends BaseTaskProxy {

    private int maxConnectionsPerHost;

    @PostConstruct
    public void init() {
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();

        if (connectTimeout > 0) {
            connectionManager.getParams().setConnectionTimeout((int) connectTimeout);
        }
        if (readTimeout > 0) {
            connectionManager.getParams().setSoTimeout((int) readTimeout);
        }
        if (threadPoolSize > 0) {
            connectionManager.getParams().setMaxTotalConnections((int) threadPoolSize);
        }
        if (maxConnectionsPerHost > 0) {
            connectionManager.getParams().setDefaultMaxConnectionsPerHost(maxConnectionsPerHost);
        }

        HttpClient httpClient = new HttpClient(connectionManager);
        ApacheHttpClientHandler httpClientHandler = new ApacheHttpClientHandler(httpClient);
        ApacheHttpClient contentServerClient = new ApacheHttpClient(httpClientHandler);
        contentServerClient.setConnectTimeout((int) connectTimeout);
        contentServerClient.setReadTimeout((int) readTimeout);

        startResource = contentServerClient.resource(TransportUtils.getRestPath(endpoint, TaskServerResource.START));
        pullResource = contentServerClient.resource(TransportUtils.getRestPath(endpoint, TaskServerResource.POLL));
        releaseResource = contentServerClient.resource(TransportUtils.getRestPath(endpoint, TaskServerResource.RELEASE));
    }

    public void setMaxConnectionsPerHost(int maxConnectionsPerHost) {
        this.maxConnectionsPerHost = maxConnectionsPerHost;
    }
}