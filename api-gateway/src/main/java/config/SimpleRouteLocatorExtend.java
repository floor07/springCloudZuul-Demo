package config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.SimpleRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;
import java.util.Collection;

public class SimpleRouteLocatorExtend extends SimpleRouteLocator {

    private PathMatcher pathMatcher = new AntPathMatcher();

    @Autowired
    ZuulConstantReload zuulConstantReload;

    private static final Log log = LogFactory.getLog(SimpleRouteLocatorExtend.class);

    public SimpleRouteLocatorExtend(String servletPath, ZuulProperties properties) {
        super(servletPath, properties);
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
