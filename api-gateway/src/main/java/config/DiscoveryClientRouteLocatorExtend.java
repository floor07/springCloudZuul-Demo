package config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.discovery.ServiceRouteMapper;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;

import java.util.Collection;
import java.util.Set;

public class DiscoveryClientRouteLocatorExtend extends DiscoveryClientRouteLocator {

    private PathMatcher pathMatcher = new AntPathMatcher();

    @Autowired
    ZuulConstantReload zuulConstantReload;

    private static final Log log = LogFactory.getLog(SimpleRouteLocatorExtend.class);


    public DiscoveryClientRouteLocatorExtend(String servletPath, DiscoveryClient discovery, ZuulProperties properties, ServiceInstance localServiceInstance) {
        super(servletPath, discovery, properties, localServiceInstance);
    }

    public DiscoveryClientRouteLocatorExtend(String servletPath, DiscoveryClient discovery, ZuulProperties properties, ServiceRouteMapper serviceRouteMapper, ServiceInstance localServiceInstance) {
        super(servletPath, discovery, properties, serviceRouteMapper, localServiceInstance);
    }

    private boolean isByPathUrl(String urlPath, Collection<String> pathWhites) {
        if(!ObjectUtils.isEmpty(pathWhites)) {
            for (String whitePath : pathWhites) {
                if (this.pathMatcher.match(whitePath, urlPath)) {
                    return true;
                }
            }
        }
        return false;
    }


    protected boolean matchesIgnoredPatterns(String path) {
        if(!isByPathUrl(path,zuulConstantReload.getByPassUrl())) {
            for (String pattern : this.getIgnoredPaths()) {
                log.debug("Matching ignored pattern:" + pattern);
                if (this.pathMatcher.match(pattern, path)) {
                    log.debug("Path " + path + " matches ignored pattern " + pattern);
                    return true;
                }
            }
        }
        return false;
    }
}
